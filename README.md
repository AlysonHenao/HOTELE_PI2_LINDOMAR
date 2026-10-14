# LINDOMAR Prototipo hotelero

MVP de gestión hotelera con tres experiencias: huésped, empleado y administrador.

## Ejecutar con Docker

Docker Compose levanta PostgreSQL, la API de Spring Boot y el frontend. No es
necesario instalar Java, Maven, Node.js ni PostgreSQL en el equipo.

### Requisitos

1. Instalar [Docker Desktop](https://www.docker.com/products/docker-desktop/).
2. En Windows, comprobar que la virtualización esté habilitada en BIOS/UEFI y
   que Docker Desktop use WSL 2.
3. Abrir Docker Desktop y esperar hasta que indique que el motor está activo.

Puede verificar la instalación desde PowerShell:

```powershell
docker --version
docker compose version
docker info
```

`docker info` debe mostrar información del servidor, no solamente del cliente.

### Paso a paso

1. Cambiar a la rama del Sprint 2 y actualizarla:

   ```powershell
   git switch Sprint2/Julian
   git pull --ff-only origin Sprint2/Julian
   ```

2. Desde la carpeta raíz del repositorio, construir e iniciar todo. El comando
   espera automáticamente a que la base de datos, la API y el frontend estén
   saludables:

   ```powershell
   docker compose up --build -d --wait
   ```

3. Confirmar que los tres contenedores aparezcan como `healthy`:

   ```powershell
   docker compose ps
   ```

4. Abrir la aplicación en http://localhost:3000.

La API también queda disponible en http://localhost:8080. La base de datos se
guarda en el volumen `lindomar_data`, por lo que conserva la información entre
reinicios.

### Comandos útiles

Ver los registros de todos los servicios:

```powershell
docker compose logs -f
```

Detener la aplicación sin borrar los datos:

```powershell
docker compose down
```

Volver a iniciarla sin reconstruir las imágenes:

```powershell
docker compose up -d
```

Recrear todo desde cero, incluida la base de datos (borra los datos locales):

```powershell
docker compose down -v
docker compose up --build -d
```

Si Docker muestra que no puede conectarse a
`dockerDesktopLinuxEngine`, primero abra Docker Desktop. Si Docker Desktop
informa que la virtualización no está disponible, habilite Intel VT-x o AMD-V
en BIOS/UEFI y reinicie Windows.

## Cuentas de demostración

| Perfil | Correo | Contraseña |
|---|---|---|
| Huésped | huesped@lindomar.co | demo123 |
| Empleado | empleado@lindomar.co | demo123 |
| Administrador | admin@lindomar.co | demo123 |

## Alcance del prototipo

- **Huésped:** registro de cuenta, búsqueda y filtros, mapa de disponibilidad por pisos, creación/cancelación de reservas, solicitudes de atención a la habitación y asistente de información.
- **Empleado:** consulta de tareas con actualización de progreso y bandeja de solicitudes de los huéspedes.
- **Administrador:** indicadores operativos, habitaciones (crear/editar/eliminar), reservas, solicitudes, usuarios, asignación de tareas, movimientos financieros y recordatorios de gastos programados.

### Novedades de esta versión (PROTOTIPO_2)

- Rediseño completo de la interfaz: paleta costera, tipografía Playfair Display + DM Sans, y **modo claro / oscuro**.
- **Registro de huéspedes** (`POST /api/auth/register`), antes solo existían cuentas precargadas.
- **Bandeja de solicitudes para el personal**: el botón de atención del huésped ahora tiene quién lo atienda y se puede resolver.
- **Ingreso automático al balance** al confirmar una reserva, con su reembolso correspondiente al cancelarla.
- **Recordatorios de gastos programados**, con aviso de los próximos a vencer y registro del gasto en un clic.
- El mapa de habitaciones ahora se construye a partir de las habitaciones reales del hotel.

Este prototipo usa sesiones opacas y contraseñas protegidas con BCrypt. La autorización se aplica a operaciones concretas; todavía requiere una revisión integral de permisos y reglas de negocio antes de producción.

### Sprint 1 — HU-01, HU-04 y HU-18

- **HU-01:** registro validado, correo normalizado y único, contraseñas BCrypt y cuentas siempre de huésped. Al iniciar se convierten las contraseñas en texto plano de bases del prototipo; las que ya tienen BCrypt se conservan.
- **HU-04:** la búsqueda rechaza fechas incompletas, pasadas o con salida igual/anterior a la llegada. Conserva el catálogo sin fechas para los consumidores existentes. Los periodos se interpretan como `[llegada, salida)`: otra estadía puede empezar el día de salida. Las reservas canceladas no bloquean disponibilidad.
- **HU-18:** estados `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE` y `OUT_OF_SERVICE`. El administrador puede cambiarlos desde el inventario o la edición. `PATCH /api/rooms/{id}/status` acepta `{"status":"RESERVED"}` y conserva las demás propiedades. Solo `AVAILABLE` aparece como reservable. `RESERVED` es un bloqueo operativo manual; las reservas con fechas siguen controlando sus propios cruces y no bloquean periodos futuros sin solapamiento.
- El inicio actualiza la restricción de estados de PostgreSQL de volúmenes existentes, sin borrar sus datos. Habitaciones, usuarios, reservas administrativas, finanzas, recordatorios y dashboard requieren una sesión de administrador. Los huéspedes solo consultan/cancelan sus propias reservas y crean/consultan sus propias solicitudes; los empleados solo acceden a sus tareas.

### Sprint 2 — HU-25, HU-26, HU-27, HU-28 y HU-34

- El administrador asigna tareas únicamente a cuentas de empleado, con campos y fecha límite validados.
- Cada empleado consulta solo sus tareas e inicia una tarea pendiente antes de poder completarla.
- La finalización registra la fecha y permite adjuntar una fotografía; no se puede completar una tarea ajena, pendiente o ya finalizada.
- El tablero administrativo separa habitaciones disponibles, ocupadas, reservadas, en mantenimiento y fuera de servicio, y calcula una ocupación segura incluso sin habitaciones registradas.

Calidad: `cd backend && mvn verify`. Frontend: `cd frontend && npm ci && npm run lint && npm run build`.

### Sprint 2 — Julián Jiménez: HU-06, HU-11 y HU-24

- **HU-06:** pantalla de detalles antes de reservar, con servicios, capacidad, piso, precio por noche, total y disponibilidad consultada para las fechas elegidas. Fotografía configurable desde administración y aviso cuando no está disponible.
- **HU-11:** cancelación hasta antes del límite de **2 horas previas al check-in de las 15:00**, en `America/Bogota` (antes de las 13:00 del día de llegada). Solo reservas confirmadas, por su huésped o un administrador. Reembolso completo del importe registrado, una sola vez, conservando el historial. Las fechas se liberan sin quitar bloqueos operativos ni la ocupación de otra estadía.
- **HU-24:** asignación de los roles huésped, empleado y administrador, con sus permisos visibles. Solo un administrador puede asignarlos; no puede cambiar su propio rol. Un cambio revoca las sesiones anteriores del usuario y los permisos se validan en el backend.
- **Tareas automáticas:** cada reserva asigna la preparación de la habitación al empleado con menor carga abierta; cada transición real de ocupada a disponible asigna una tarea prioritaria de limpieza e inspección. Los eventos son idempotentes y no duplican tareas.

Consulta [reglas, endpoints, pruebas y guion de demo del Sprint 2](docs/SPRINT2.md).

