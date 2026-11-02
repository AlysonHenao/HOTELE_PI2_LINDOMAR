# Hotel Lindomar — ejecutar y probar desde cero

Esta guía permite descargar la versión de `main`, iniciar la aplicación y probar las funciones de huésped, empleado y administrador. Los comandos están escritos para **PowerShell en Windows**.

## 1. Preparar el equipo

Para la opción con Docker necesitas:

- [Git](https://git-scm.com/downloads).
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) con el motor Linux activo. En Windows debe estar habilitada la virtualización y configurado WSL 2.
- Conexión a Internet para descargar el proyecto, las imágenes y las dependencias.

Abre Docker Desktop. Después, abre PowerShell y comprueba:

```powershell
git --version
docker --version
docker compose version
docker info
```

**Resultado esperado:** `docker info` muestra información del servidor. Si solo muestra el cliente y un error, el motor de Docker todavía no está disponible. Resuelve eso antes del siguiente paso; no necesitas instalar Java, Maven, Node ni PostgreSQL para ejecutar con Docker.

## 2. Descargar la última versión

Desde la carpeta donde quieras guardar el proyecto:

```powershell
git clone https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR.git
cd HOTELE_PI2_LINDOMAR
git switch main
git pull --ff-only origin main
git log -1 --oneline
```

Si ya lo tienes descargado, entra en la carpeta que contiene `docker-compose.yml` y ejecuta solo los tres últimos comandos. Guarda tus cambios pendientes antes de cambiar de rama. No clones el repositorio dentro de otra copia existente.

## 3. Iniciar la aplicación con Docker

La guía usa el nombre de proyecto `lindomar-pruebas`, para mantener sus contenedores y datos separados de otras instancias.

```powershell
$env:FRONTEND_PORT = '3000'
$env:BACKEND_PORT = '8080'
docker compose -p lindomar-pruebas up --build -d --wait
docker compose -p lindomar-pruebas ps
```

La primera construcción puede tardar varios minutos. Espera a que termine.

**Resultado esperado:** los servicios `postgres`, `backend` y `frontend` están en ejecución y aparecen como `healthy`.

Abre:

- **Aplicación:** [http://localhost:3000](http://localhost:3000).
- **Consulta pública de habitaciones:** [http://localhost:8080/api/rooms](http://localhost:8080/api/rooms).

Si los puertos están ocupados, elige otros antes de iniciar:

```powershell
$env:FRONTEND_PORT = '3002'
$env:BACKEND_PORT = '8082'
docker compose -p lindomar-pruebas up --build -d --wait
```

En ese caso abre [http://localhost:3002](http://localhost:3002). Si tienes otra versión abierta en ese puerto, usa uno libre, por ejemplo `3004` y `8084`. El puerto elegido debe coincidir con el enlace que abres.

## 4. Cuentas de demostración

Las cuentas se crean automáticamente en una base de datos nueva.

| Perfil | Correo | Contraseña |
|---|---|---|
| Huésped | `huesped@lindomar.co` | `demo123` |
| Empleado | `empleado@lindomar.co` | `demo123` |
| Administrador | `admin@lindomar.co` | `demo123` |

Usa **Cerrar sesión** para cambiar de perfil. Los botones de demostración completan las credenciales; pulsa **Ingresar** para acceder. Si reutilizas una base existente, conserva sus cuentas, reservas y cambios de roles.

## 5. Recorrido de prueba del Sprint 2

Utiliza fechas de **mañana y pasado mañana**. Así la prueba de cancelación no depende de la hora en que la realices. Anota el número de habitación, las fechas y el identificador de la reserva.

### Prueba 1 — Buscar y consultar detalles (HU-06)

1. Ingresa como huésped y abre **Habitaciones**.
2. Selecciona mañana como llegada y pasado mañana como salida. Pulsa **Buscar**.
3. Prueba los filtros de tipo, personas, precio, balcón o mascotas.
4. Abre **Ver detalles** en una habitación disponible.
5. Revisa número, tipo, piso, capacidad, servicios, tarifa por noche, noches, total y política de cancelación.
6. Pulsa **Continuar con la reserva** y confirma una reserva de prueba.

**Resultado esperado:** la información corresponde a la habitación elegida y la reserva aparece confirmada en **Mis reservas**. Para una noche, el total coincide con la tarifa por noche.

Una habitación sin fotografía registrada muestra **Fotografía no disponible**. Para comprobar una fotografía, el administrador puede guardar una URL HTTPS de una imagen accesible en la edición de esa habitación.

### Prueba 2 — Evitar inconsistencias al cambiar estados (bug #59)

Haz esta prueba **antes de cancelar la reserva anterior**:

1. Cierra sesión e ingresa como administrador.
2. Abre **Habitaciones** y localiza la habitación reservada.
3. Intenta cambiarla a **Mantenimiento** y luego a **Fuera de servicio**.
4. Prueba también el cambio mediante la edición completa de la habitación.

**Resultado esperado:** el sistema rechaza ambos cambios e informa que hay reservas activas. La habitación conserva su estado anterior y la reserva sigue registrada. La operación no cancela la reserva automáticamente.

### Prueba 3 — Cancelar y liberar las fechas (HU-11)

1. Vuelve a ingresar como el huésped propietario.
2. En **Mis reservas**, revisa el plazo de cancelación.
3. Pulsa **Cancelar** y confirma el aviso de reembolso.
4. Verifica que la reserva permanece en el historial con estado **Cancelada**.
5. Busca la misma habitación en las mismas fechas.
6. Como administrador, abre **Finanzas** y localiza el ingreso y el reembolso relacionados con esa reserva.

**Resultado esperado:** la cancelación libera las fechas si no existe otro bloqueo. Se registra **un único reembolso completo** del importe original. El reembolso es un movimiento contable del prototipo; no hace un pago bancario real.

La política actual permite cancelar **antes de las 13:00 del día de llegada**, dos horas antes del check-in de las 15:00, en horario de Colombia. En el límite exacto ya se rechaza. Una reserva alojada o finalizada no se puede cancelar. Para probar los segundos alrededor del límite, usa las pruebas automáticas; no cambies el reloj del equipo.

### Prueba 4 — Gestionar roles y restringir acceso (HU-24)

1. Registra una cuenta de prueba como huésped, usando un correo distinto al de las cuentas de demostración.
2. Ingresa como administrador y abre **Equipo y usuarios**.
3. Revisa los permisos de cada rol.
4. Cambia la cuenta de prueba a **Empleado** y confirma.
5. Vuelve a ingresar con esa cuenta.
6. Restaura su rol a **Huésped** al terminar.

**Resultado esperado:** las sesiones anteriores de la cuenta afectada quedan revocadas. Al ingresar como empleado solo se muestra su navegación correspondiente. El administrador activo tiene deshabilitado el cambio de su propio rol.

No uses el cambio de rol del administrador de demo para probar esta función.

### Prueba 5 — Asignar, iniciar y completar tareas (HU-25 a HU-28)

1. Como administrador, entra en **Tareas** y crea una tarea para el empleado de demo.
2. Completa título, descripción, prioridad y una fecha límite válida.
3. Ingresa como ese empleado y abre **Mis tareas**.
4. Localiza la tarea y pásala de **Pendiente** a **En progreso**.
5. Complétala, opcionalmente adjuntando una imagen de hasta 5 MB.
6. Como administrador, verifica el estado y la evidencia, si adjuntaste una fotografía.

**Resultado esperado:** solo el empleado asignado puede iniciar/completar su tarea. No se puede completar una tarea pendiente, ajena o ya finalizada. La fecha de finalización queda registrada.

### Prueba 6 — Tareas automáticas y ocupación (HU-34)

1. Crea una nueva reserva como huésped.
2. En administración, verifica que se creó una tarea **Preparar habitación** para la fecha de llegada.
3. Revisa el resumen de ocupación: distingue disponibles, ocupadas, reservadas, mantenimiento y fuera de servicio.
4. Al finalizar una estadía y liberar realmente una habitación ocupada, verifica la tarea **Acondicionar habitación**.

**Resultado esperado:** las tareas se asignan al empleado con menos tareas abiertas. Si no hay empleados, se conservan sin responsable y se pueden asignar posteriormente desde administración. Una reserva futura no debe contarse automáticamente como una estadía ya alojada.

Para probar el checkout completo se requiere una reserva vigente con ingreso realizado; no se permite hacer check-in de una reserva de mañana durante el día de hoy.

## 6. Comprobaciones automáticas

Desde la raíz del repositorio, con Docker disponible:

```powershell
docker run --rm -e TZ=America/Bogota -v "${PWD}/backend:/app" -v lindomar-maven-cache:/root/.m2 -w /app maven:3.9.9-eclipse-temurin-21 mvn --batch-mode --no-transfer-progress verify
```

**Resultado esperado en esta versión:** `Tests run: 83, Failures: 0, Errors: 0, Skipped: 0`, `0 Checkstyle violations` y `BUILD SUCCESS`. El número puede aumentar con cambios posteriores.

Para comprobar el frontend:

```powershell
docker run --rm -v "${PWD}/frontend:/app" -w /app node:22-alpine sh -c 'npm ci && npm run lint && npm run build'
```

Si tienes Java 21, Maven y Node.js 22 instalados, puedes ejecutar directamente:

```powershell
cd backend
mvn verify
cd ../frontend
npm ci
npm run lint
npm run build
cd ..
```

## 7. Detener, volver a iniciar y actualizar

Detener sin borrar los datos:

```powershell
docker compose -p lindomar-pruebas down
```

Volver a iniciar:

```powershell
docker compose -p lindomar-pruebas up -d --wait
```

Actualizar a los últimos cambios de `main` y reconstruir:

```powershell
git pull --ff-only origin main
docker compose -p lindomar-pruebas up --build -d --wait
```

Ejecuta los comandos desde la misma carpeta y conserva las variables de puertos. Al abrir una PowerShell nueva, vuelve a definirlas si usaste puertos diferentes a 3000/8080.

Para repetir las pruebas con datos adicionales, registra otro huésped y usa otras fechas. **No necesitas borrar la base de datos.**

Solo si deseas eliminar todos los datos de esta instancia de pruebas y regenerar las cuentas iniciales:

```powershell
# Borra permanentemente las reservas, usuarios y movimientos de lindomar-pruebas.
docker compose -p lindomar-pruebas down -v
docker compose -p lindomar-pruebas up --build -d --wait
```

## 8. Resolver problemas comunes

| Problema | Qué revisar |
|---|---|
| `localhost` no abre | Comprueba el puerto elegido y `docker compose -p lindomar-pruebas ps`. La página solo funciona mientras la app esté ejecutándose. |
| Error `dockerDesktopLinuxEngine` | Abre Docker Desktop y comprueba `docker info`. Si Docker muestra un error de inicio, hay que resolver el motor; reconstruir el proyecto no lo soluciona. |
| Puerto ocupado | Usa otros valores de `FRONTEND_PORT` y `BACKEND_PORT` y vuelve a iniciar. No cierres procesos de otros proyectos sin identificarlos. |
| Servicio `unhealthy` o API sin responder | Ejecuta `docker compose -p lindomar-pruebas logs --tail=100 backend frontend postgres`. |
| Aparece la interfaz anterior | Confirma la rama/commit con `git log -1 --oneline`, reconstruye y recarga con `Ctrl + F5`. |
| No entra una cuenta de demo | Revisa que la API esté disponible. Una base reutilizada puede conservar cambios previos de contraseña o rol. |
| No hay habitaciones en la búsqueda | Prueba otras fechas y elimina filtros. Revisa los estados operativos y las reservas existentes. |
| La cancelación no está disponible | Comprueba propietario, estado y límite horario de esa reserva. |
| Prettier avisa de finales de línea en Windows | Ejecuta `npm run format` dentro de `frontend` y repite `npm run lint`. Revisa `git diff` antes de guardar cambios. |

## 9. Alternativa sin Docker

Si el motor Docker no funciona, instala **Java 21**, **Maven 3.9**, **Node.js 22** y **PostgreSQL 16**. Esta opción requiere configurar los servicios manualmente.

En PostgreSQL, con una cuenta administradora, crea una base y un usuario propios para la demo (omite este paso si ya existen):

```sql
CREATE USER lindomar WITH PASSWORD 'lindomar';
CREATE DATABASE lindomar OWNER lindomar;
```

En una PowerShell, desde `backend`, ejecuta:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/lindomar'
$env:SPRING_DATASOURCE_USERNAME = 'lindomar'
$env:SPRING_DATASOURCE_PASSWORD = 'lindomar'
$env:SERVER_PORT = '8080'
mvn spring-boot:run
```

Deja esa terminal abierta. En otra PowerShell, desde `frontend`:

```powershell
npm ci
npm run dev -- --port 3000 --strictPort
```

Abre [http://localhost:3000](http://localhost:3000). El servidor de desarrollo envía las peticiones `/api` al backend en 8080. Si PostgreSQL usa otro puerto, ajusta la URL de conexión. Detén cada servidor con `Ctrl + C` cuando termines; conserva el directorio de datos de PostgreSQL.

La instancia local que se haya levantado temporalmente en 3002/8082 puede tener una configuración diferente: no es necesario reproducir las rutas privadas de herramientas de otra persona para seguir esta guía.
