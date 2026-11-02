# Despliegue de Hotel Lindomar en AWS EC2

**Estado: configuración preparada; despliegue real pendiente.** El equipo confirmó
EC2, pero todavía no ha elegido instancia, región ni dominio. Esta guía no acredita
que exista una aplicación pública. Completar la evidencia al conectar AWS.

## Arquitectura y archivos

GitHub Actions verifica `main`, publica dos imágenes por SHA en ECR y ejecuta el
despliegue por Systems Manager (SSM). Una instancia EC2 Linux x86_64 ejecuta
PostgreSQL, Spring Boot, Nginx/React y Caddy con Docker Compose. Caddy atiende HTTPS;
la API y PostgreSQL no exponen puertos del host. Los datos y certificados persisten
en volúmenes. Es una instalación de un solo nodo, con interrupciones breves durante
actualizaciones; no promete alta disponibilidad ni escalado horizontal de la base.

| Archivo | Función |
|---|---|
| `.github/workflows/ci.yml` | Pruebas, análisis, coverage y build; PR hacia main y push a main. |
| `.github/workflows/deploy-aws.yml` | Solo despliega tras CI exitoso en push a main; ejecución manual también valida el candidato. |
| `deploy/aws/compose.yml` | Servicios, persistencia y healthchecks. |
| `deploy/aws/Caddyfile` | HTTPS para el dominio configurado y proxy al frontend. |
| `deploy/aws/deploy.sh` | Obtiene parámetros, descarga imágenes, respalda la BD y verifica salud. |
| `deploy/aws/send-deploy.py` | Empaqueta los tres archivos revisados para SSM sin secretos. |

## Preparación de infraestructura (equipo AWS)

1. Elegir región, tipo de instancia **x86_64** y capacidad de disco. Validar memoria
   para Java y PostgreSQL juntos; medir con la carga de las sesiones antes de escoger
   el tamaño final. La construcción se realiza en Actions, no en EC2.
2. Crear EC2 con un Linux mantenido, volumen EBS cifrado, conectividad saliente y SSM
   Agent conectado. Asignar un instance profile con `AmazonSSMManagedInstanceCore`,
   lectura de los dos repositorios ECR y lectura de los parámetros indicados abajo
   (y `kms:Decrypt` si se utiliza una clave KMS propia).
3. Instalar Docker Engine, Docker Compose v2 con `--wait`, AWS CLI v2, `curl`, `tar`,
   `base64`, `bash` y `flock`. Verificar mediante SSM `docker version`,
   `docker compose version` y `aws sts get-caller-identity`.
4. Asociar IP pública estable y apuntar un registro DNS A del dominio hacia ella.
   Si existe AAAA, debe resolver también a esta instancia. Abrir entrada TCP 80 y 443;
   mantener cerrados 5432 y 8080. SSM evita requerir SSH entrante.
5. Crear dos repositorios ECR privados: por ejemplo `lindomar/backend` y
   `lindomar/frontend`. Las etiquetas usan SHA del commit. Mantener etiquetas
   mutables para permitir repetir el mismo despliegue; activar escaneo y una política
   de retención que conserve imágenes utilizadas y al menos la versión anterior.
6. Crear parámetros **SecureString**, por ejemplo bajo `/lindomar/production`:
   `db-password`, `admin-email` y `admin-password`. Generar contraseñas aleatorias
   con letras, números, `_` y `-`: BD entre 24 y 72 caracteres; admin entre 12 y 72.
   El script exige ese formato para escribir `.env` de forma segura. No usar valores
   de demostración ni registrar secretos en la Wiki o en Git.
7. Crear el proveedor OIDC `https://token.actions.githubusercontent.com`, audiencia
   `sts.amazonaws.com`, y un rol para Actions. Su confianza debe restringirse a:

   ```json
   {
     "StringEquals": {
       "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
       "token.actions.githubusercontent.com:sub": "repo:AlysonHenao/HOTELE_PI2_LINDOMAR:environment:aws-production"
     }
   }
   ```

   Integrar esas condiciones en el statement de `sts:AssumeRoleWithWebIdentity`, con
   el ARN del proveedor OIDC de la cuenta. No es una política IAM completa por sí sola.
   El rol de Actions necesita autorización ECR para publicar en ambos repositorios,
   `ecr:GetAuthorizationToken` y permisos SSM para ejecutar `AWS-RunShellScript` sobre
   **la instancia elegida**, más `ssm:GetCommandInvocation`. Actions no necesita leer
   las contraseñas. Restringir recursos y región con los ARN reales.
8. Crear el environment de GitHub **`aws-production`**, permitir despliegues solo
   desde `main` y configurar las variables de la tabla siguiente. Revisar las reglas
   de protección de `main`: los jobs de CI deben ser obligatorios antes del merge.
   Un archivo YAML no activa esas reglas en GitHub por sí solo.

| Variable de GitHub (no secreto) | Valor que debe aportar el equipo |
|---|---|
| `AWS_REGION` | Región de EC2, ECR y Parameter Store. |
| `AWS_ROLE_ARN` | ARN del rol OIDC de Actions. |
| `EC2_INSTANCE_ID` | ID real de la instancia registrada en SSM. |
| `ECR_BACKEND_REPOSITORY` | Nombre del repositorio ECR backend. |
| `ECR_FRONTEND_REPOSITORY` | Nombre del repositorio ECR frontend. |
| `PUBLIC_HOST` | Solo nombre DNS, sin `https://` ni rutas. |
| `SSM_PARAMETER_PREFIX` | Prefijo de parámetros sin `/` al final. |

## Primer despliegue y actualizaciones

Después de integrar esta rama por PR en `main`, CI ejecuta Maven verify, lint/build
y construcción Docker. Solo una ejecución exitosa de CI por push a main activa
Deploy AWS EC2. Un PR, incluido uno de un fork, no obtiene credenciales de AWS.
También se puede lanzar manualmente desde main: vuelve a ejecutar las validaciones.
El workflow comprueba que el SHA siga siendo el actual de main antes de desplegar.

La instancia toma un bloqueo de despliegue, lee los secretos con su propio rol,
descarga las imágenes y guarda un `pg_dump` previo. Luego actualiza los servicios,
espera healthchecks y comprueba `https://DOMINIO/api/rooms`. Conserva el release en
`/opt/lindomar/releases/SHA` y el enlace `current`. El summary de Actions incluye el
comando SSM y el SHA. Nunca imprimir el contenido de los `.env` o de Parameter Store.

La configuración pública desactiva el seed de demostración. Una BD nueva crea solo
el administrador configurado en Parameter Store, con BCrypt. No reinicializa ni
cambia la clave en cada reinicio. Modificar el parámetro de bootstrap **no** cambia
una contraseña ya almacenada. Usar una base nueva para AWS; desactivar el seed no
borra cuentas demo que ya existan en una base reutilizada.

## Fallos, recuperación y mantenimiento

Si falla salud/HTTPS, el script intenta levantar las imágenes del release anterior
y conserva un error de despliegue. En la primera ejecución no existe versión previa.
Hibernate usa `ddl-auto=update`: volver a imágenes anteriores no revierte cambios de
esquema. Revisar compatibilidad y la copia previa antes de restaurar una BD; nunca
restaurarla automáticamente sobre datos más recientes.

Desde SSM, inspeccionar sin mostrar secretos:

```bash
cd /opt/lindomar/current
docker compose --project-name lindomar-production --env-file .env -f compose.yml ps
docker compose --project-name lindomar-production --env-file .env -f compose.yml logs --tail=100 backend
```

No ejecutar `docker compose down -v`. El backup previo reside en el mismo nodo y
protege frente a fallos de actualización, no frente a perder EBS/EC2. Antes de uso
real, configurar copia externa cifrada y probar restauración. Definir retención de
releases, logs y dumps para no agotar disco. Una rotación de la clave PostgreSQL
requiere cambiar también el rol dentro de PostgreSQL; no basta con cambiar el parámetro.

## Evidencia requerida para cerrar J3

| Evidencia | Estado |
|---|---|
| Cuenta, región, instancia, dominio y permisos | Pendientes de definición/configuración. |
| URL pública final en README y Wiki | Pendiente; no sustituir por localhost. |
| Run CI y run Deploy, con SHA y fecha | Pendientes de ejecución en GitHub. |
| Comando SSM exitoso y servicios saludables | Pendientes de ejecución en EC2. |
| Acceso externo HTTPS y recorrido por rol | Pendientes. |
| Prueba de actualización y recuperación | Pendiente en el entorno elegido. |

J3 queda preparado, no terminado, hasta adjuntar estas evidencias. El equipo debe
definir cómo escalar (p. ej. BD administrada y más de una instancia) si la carga lo requiere.

Referencias de implementación: [OIDC en AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws),
[SSM Run Command](https://docs.aws.amazon.com/systems-manager/latest/userguide/walkthrough-cli.html),
[autenticación y publicación ECR](https://docs.aws.amazon.com/AmazonECR/latest/userguide/getting-started-cli.html).
