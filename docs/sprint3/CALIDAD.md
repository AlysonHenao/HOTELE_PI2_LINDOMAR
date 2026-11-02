# Sprint 3: implementación, pruebas y evidencias

Validación local del 1 de noviembre de 2026, rama `Julian/Sprint3`, basada en
`main` (`d8fd722`). Los cambios aún deben publicarse por PR y verificarse en CI.
No atribuir estos resultados a un run de GitHub ni al entorno AWS.

## Reglas implementadas

| Alcance | Comportamiento |
|---|---|
| HU-30/31 | `POST /api/finance` requiere ADMIN, tipo válido, concepto no vacío (hasta 255 caracteres) e importe positivo, hasta 12 enteros y dos decimales. Fecha omitida = hoy en Colombia; fecha futura se rechaza. El cliente no elige el ID persistido. |
| HU-32 | El balance del dashboard continúa siendo ingresos menos gastos; un recordatorio pendiente no afecta ese balance. |
| Recordatorios, HU-36 | Creación validada y siempre pendiente, sin aceptar identidad de pago del cliente. Fecha nueva desde hoy. Edición de campos válidos solo mientras está pendiente. |
| Pago de recordatorio | `POST /api/reminders/{id}/pay` bloquea el registro dentro de una transacción y vincula `paymentEntryId`. Reintentos devuelven el mismo movimiento. Pago y estado se confirman juntos. |
| Historial | Recordatorios pagados no pueden reabrirse, modificarse ni borrarse. DONE heredado sin movimiento vinculado responde 409 para evitar otro gasto; revisar manualmente el historial. |
| Datos heredados | Un recordatorio pendiente con concepto ausente o valor no positivo no se paga: debe corregirse primero. No se modificaron históricos ni se eliminaron pagos anteriores. |
| Reservas / J2 | Observaciones de más de 255 caracteres responden 400 antes de crear ingreso/tareas; texto normalizado. Las reglas existentes de disponibilidad y cancelación conservan sus pruebas. |
| Interfaz | Confirmación de pago, botones bloqueados durante envío, errores visibles, formularios conservados tras fallo y decimales monetarios visibles. Botón Limpiar de reservas con contraste visible. |
| AWS | Demo desactivada en Compose público; una BD nueva exige credenciales de bootstrap y almacena la contraseña con BCrypt. |

Archivos principales: `FinanceRequests.java`, `HotelController.java`,
`ReminderRepository.java`, `Domain.java`, `AdminBootstrapConfig.java` y
`frontend/src/main.jsx`. El cambio de esquema agrega una columna nullable y única
`payment_entry_id` en recordatorios; no rellena automáticamente los DONE heredados.
Probar en una copia antes de aplicarlo a una BD que contenga datos reales.

## Verificación ejecutada

Backend: Java 21, Maven `spotless:apply verify`, H2 en modo PostgreSQL para la
suite automatizada. Resultado final: **106 invocaciones, 0 fallos, 0 errores**;
Checkstyle sin infracciones y comprobación Spotless aprobada.

| Clase | Invocaciones |
|---|---:|
| CancellationPolicyTest | 4 |
| HotelStoriesIntegrationTest | 2 |
| Sprint1IntegrationTest | 37 |
| Sprint2IntegrationTest | 33 |
| StaffSprint2IntegrationTest | 7 |
| FinanceSprint3IntegrationTest | 20 |
| AdminBootstrapConfigTest | 3 |
| **Total** | **106** |

La suite nueva prueba valores nulos, cero, negativos, precisión, campos inválidos,
fechas futuras, permisos, ID inyectado, balance, pago repetido y simultáneo, edición,
historial heredado y notas de reserva demasiado largas. La prueba previa de permisos
ahora envía datos financieros válidos para seguir comprobando 403 con el contrato nuevo.

JaCoCo del conjunto completo: **867/1.059 líneas cubiertas (81,87 %)** y
**286/442 ramas (64,71 %)**. No es cobertura exclusiva de las funciones nuevas ni
una medición del frontend. La Wiki del Sprint 2 describe 153 invocaciones de otra
ejecución/versión; no se reutiliza esa cifra para esta rama.

Comprobación adicional real contra PostgreSQL en una base aislada: dos pagos
simultáneos devolvieron 200 y el mismo ID; se agregó un solo movimiento y el balance
bajó exactamente COP 25.000,50. Un importe negativo respondió 400 y reabrir el
recordatorio pagado respondió 409. No se ejecutó contra una base del hotel.

Frontend: `npm run lint` y `npm run build` aprobados. Prettier admite los finales de
línea de Windows/Linux, sin reformatear masivamente el proyecto. Navegador Chrome:
recorridos de administrador y formularios; un error 400 simulado permanece visible
y conserva el formulario; sin errores de JavaScript en ese recorrido. Capturas en
1440 px y Finanzas en 390 px. Esto es QA técnico, no una prueba con usuarios finales.

Despliegue: validación de YAML, `docker compose config --quiet`, sintaxis Bash y
generación del payload SSM. Docker Engine local no estaba disponible: **no se
ejecutaron imágenes/contenedores de producción ni el despliegue AWS** en esta revisión.

## Reproducir

```powershell
cd backend
mvn --batch-mode --no-transfer-progress verify
# O solo las nuevas pruebas:
mvn --batch-mode --no-transfer-progress '-Dtest=FinanceSprint3IntegrationTest,AdminBootstrapConfigTest' test
cd ../frontend
npm ci
npm run lint
npm run build
```

Para la evidencia principal ejecutar `verify` sobre toda la suite, no solo las clases
nuevas. Abrir `backend/target/site/jacoco/index.html` localmente. El CSV y el resumen
de esta ejecución se conservan en [evidence](evidence/quality-summary.json), junto
a [las invocaciones](evidence/test-cases.csv). Los informes completos se regeneran
con los comandos anteriores y se adjuntan automáticamente en CI:

- `jacoco-coverage-report`: coverage HTML/XML/CSV.
- `backend-quality-evidence-SHA`: Surefire y análisis estático, incluso si falla un job.

## Publicación y evidencia pendiente

Al abrir el PR, registrar su URL y el commit final. Tras ejecutar CI, registrar URL
del run y descargar sus artefactos; comprobarlos desde una sesión con los permisos
que tendrá el profesor. Verificar enlaces de Wiki, README, código, pruebas y coverage.
No asumir protección de rama ni bloqueo de merge sin comprobar la configuración
real de GitHub. No se han cambiado las reglas de protección de `main`.

Para demo: mostrar validación de importe, pago una vez, reintento sin duplicado y
balance; después abrir pruebas, run CI y coverage del mismo SHA. Agregar despliegue
y hallazgos de usuarios cuando existan. No exponer credenciales ni grabar sesiones reales
en los artefactos de calidad.
