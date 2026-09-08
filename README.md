# LINDOMAR Prototipo hotelero

MVP de gestión hotelera con tres experiencias: huésped, empleado y administrador.

## Ejecutar

Requisito: Docker Desktop encendido.

```powershell
docker compose up --build
```

Abrir http://localhost:3000

## Cuentas de demostración

| Perfil | Correo | Contraseña |
|---|---|---|
| Huésped | huesped@lindomar.co | demo123 |
| Empleado | empleado@lindomar.co | demo123 |
| Administrador | admin@lindomar.co | demo123 |

Los datos quedan guardados en PostgreSQL mediante un volumen de Docker. Para detener el sistema use `docker compose down`.

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

Pruebas: `cd backend && mvn verify`. Compilación frontend: `cd frontend && npm ci && npm run build`.

