"""Build an SSM payload from reviewed local files, without copying application secrets."""

import base64
import io
import json
import os
from pathlib import Path
import shlex
import tarfile

source = Path(__file__).resolve().parent
payload = io.BytesIO()
with tarfile.open(fileobj=payload, mode="w:gz") as archive:
    for filename in ("compose.yml", "Caddyfile", "deploy.sh"):
        archive.add(source / filename, arcname=filename, recursive=False)
encoded = base64.b64encode(payload.getvalue()).decode("ascii")
variables = (
    "AWS_REGION", "ECR_REGISTRY", "ECR_BACKEND_REPOSITORY",
    "ECR_FRONTEND_REPOSITORY", "IMAGE_TAG", "PUBLIC_HOST", "SSM_PARAMETER_PREFIX",
)
exports = "\n".join(f"export {key}={shlex.quote(os.environ[key])}" for key in variables)
command = f"""set -eu
task_temp=$(mktemp -d)
trap 'rm -rf -- "$task_temp"' EXIT
printf '%s' '{encoded}' | base64 -d | tar -xz -C "$task_temp"
export SOURCE_DIR="$task_temp"
{exports}
bash "$task_temp/deploy.sh"
"""
print(json.dumps({"commands": [command], "executionTimeout": ["1200"]}))
