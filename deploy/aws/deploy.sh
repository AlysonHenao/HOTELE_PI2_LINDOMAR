#!/usr/bin/env bash
# Runs as root through SSM on an already configured EC2 instance.
set -Eeuo pipefail
umask 077
: "${AWS_REGION:?}" "${ECR_REGISTRY:?}" "${ECR_BACKEND_REPOSITORY:?}"
: "${ECR_FRONTEND_REPOSITORY:?}" "${IMAGE_TAG:?}" "${PUBLIC_HOST:?}" "${SSM_PARAMETER_PREFIX:?}"
[[ "$IMAGE_TAG" =~ ^[a-f0-9]{40}$ ]] || { echo 'Invalid commit SHA'; exit 1; }
[[ "$PUBLIC_HOST" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]+$ ]] || { echo 'Invalid hostname'; exit 1; }
task_dir=/opt/lindomar
install -d -m 700 "$task_dir"
exec 9>"$task_dir/deploy.lock"
flock -n 9 || { echo 'Another deployment is running'; exit 1; }
cd "$task_dir"

# These credentials are read on the instance, never sent through Actions or SSM command logs.
parameter() {
  aws ssm get-parameter --region "$AWS_REGION" --name "${SSM_PARAMETER_PREFIX}/$1" \
    --with-decryption --query Parameter.Value --output text
}
DB_PASSWORD=$(parameter db-password)
ADMIN_EMAIL=$(parameter admin-email)
ADMIN_PASSWORD=$(parameter admin-password)
export DB_PASSWORD ADMIN_EMAIL ADMIN_PASSWORD
# Restrict passwords to dotenv-safe characters. Use random letters, numbers, underscore and hyphen.
[[ "$DB_PASSWORD" =~ ^[a-zA-Z0-9_-]{24,72}$ ]] || { echo 'Invalid database password format'; exit 1; }
[[ "$ADMIN_PASSWORD" =~ ^[a-zA-Z0-9_-]{12,72}$ ]] || { echo 'Invalid administrator password format'; exit 1; }
[[ "$ADMIN_EMAIL" =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]] || exit 1
export AWS_REGION ECR_REGISTRY ECR_BACKEND_REPOSITORY ECR_FRONTEND_REPOSITORY IMAGE_TAG PUBLIC_HOST
aws ecr get-login-password --region "$AWS_REGION" | \
  docker login --username AWS --password-stdin "$ECR_REGISTRY"

release="$task_dir/releases/$IMAGE_TAG"
install -d -m 700 "$release"
# Workflow places the reviewed compose/Caddy files beside this script.
cp "$SOURCE_DIR/compose.yml" "$SOURCE_DIR/Caddyfile" "$release/"
for name in ECR_REGISTRY ECR_BACKEND_REPOSITORY ECR_FRONTEND_REPOSITORY IMAGE_TAG PUBLIC_HOST DB_PASSWORD ADMIN_EMAIL ADMIN_PASSWORD; do
  printf '%s=%s\n' "$name" "${!name}"
done > "$release/.env"
compose() { docker compose --project-name lindomar-production --env-file "$1/.env" -f "$1/compose.yml" "${@:2}"; }
previous=$(readlink -f "$task_dir/current" || true)
compose "$release" config --quiet
compose "$release" pull
# Never recreate healthy PostgreSQL during routine application deployments.
compose "$release" up -d --wait --wait-timeout 240 postgres
# A database backup is required before new application code starts (DDL auto-update).
install -d -m 700 "$task_dir/backups"
compose "$release" exec -T postgres pg_dump -U lindomar -d lindomar -Fc > "$task_dir/backups/pre-$IMAGE_TAG-$(date +%s).dump"

rollback() {
  echo 'Deployment failed; restoring previous application images when available.'
  if [[ -n "$previous" && -f "$previous/.env" ]]; then
    compose "$previous" up -d --wait --wait-timeout 240 backend frontend https || true
  fi
  echo 'Database was not restored automatically. Inspect logs and the pre-deployment backup.'
}
trap rollback ERR
compose "$release" up -d --wait --wait-timeout 240 backend frontend https
curl --fail --silent --show-error --retry 12 --retry-all-errors --retry-delay 5 \
  "https://$PUBLIC_HOST/api/rooms" > /dev/null
ln -sfn "$release" "$task_dir/current"
trap - ERR
echo "Deployment healthy: $IMAGE_TAG"
