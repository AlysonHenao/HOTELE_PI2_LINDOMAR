# Sprint 2 — Julián Jiménez

Implementación sobre `main` (`215eb386`), en la rama `Sprint2`.

## Historias y reglas

### HU-06 — Ver detalles de habitación

[Historia #6](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/6).

Desde los resultados o el mapa, **Ver detalles** abre una vista independiente del formulario de reserva. Consulta nuevamente la habitación y su disponibilidad para las fechas buscadas. Muestra número, tipo, piso, capacidad, balcón, admisión de mascotas, servicios, precio por noche, noches y total. Incluye la política de cancelación y permite continuar si hay disponibilidad.

La fotografía se configura en **Administración → Habitaciones → Editar → Fotografía (URL HTTPS)**. Se muestra un aviso si falta o no carga; las cuentas iniciales no incluyen fotografías reales. No se inventan imágenes del hotel. La consulta sin fechas muestra los detalles sin afirmar disponibilidad para un periodo no seleccionado.

Errores de consulta, habitación inexistente o fechas inválidas se muestran al usuario. El servidor vuelve a validar capacidad, estado y cruces de reservas al confirmar.

### HU-11 — Cancelar reserva

[Historia #11](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/11).

Política elegida por Julián para esta versión: **2 horas antes del check-in**. Se usa el horario de check-in ya anunciado por la aplicación, **15:00 de Colombia**. El plazo vence a las **13:00 del día de llegada**: a las 12:59:59 se permite; desde las 13:00 se rechaza. La comparación se hace en el servidor con su reloj y zona horaria, no con la hora del navegador.

- Solo el huésped propietario o un administrador pueden cancelar. Un empleado no puede hacerlo, incluso si anteriormente fue dueño de la reserva como huésped.
- Solo se admite el estado `CONFIRMED` y un plazo vigente. `CHECKED_IN` y `COMPLETED` no admiten cancelación.
- Se solicita confirmación en la interfaz mostrando reserva, habitación e importe del reembolso.
- La reserva pasa a `CANCELLED` y permanece en el historial. Se registra un gasto por reembolso del **100 % del importe guardado**; no se procesa un pago externo.
- Las reservas canceladas dejan de bloquear sus fechas. Se conservan otros cruces y los estados manuales `RESERVED`, `MAINTENANCE` y `OUT_OF_SERVICE`. Cancelar una reserva futura no desocupa una estadía actual.
- La cancelación y el asiento financiero son una transacción. Un bloqueo de base de datos serializa cancelaciones simultáneas de la misma reserva. Repetir la solicitud devuelve éxito sin repetir el reembolso.
- El cambio administrativo a `CANCELLED` pasa por las mismas reglas. No se pueden reactivar reservas canceladas/finalizadas ni cambiar fechas mediante el flujo de estados para evadir el plazo. La modificación integral de fechas corresponde a HU-10.
- No se permite eliminar una habitación que tiene historial de reservas, evitando dejar reservas sin habitación. Para retirarla del inventario reservable se usa `OUT_OF_SERVICE`.

Configuración mediante variables de entorno, también admitidas por Docker Compose:

| Variable | Valor inicial | Significado |
|---|---|---|
| `HOTEL_TIME_ZONE` | `America/Bogota` | Zona del reloj del hotel |
| `HOTEL_CHECK_IN_TIME` | `15:00` | Hora local de entrada |
| `HOTEL_CANCELLATION_ADVANCE_HOURS` | `2` | Horas de anticipación para cancelar |

### HU-24 — Gestionar roles y permisos

[Historia #24](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/24).

La pantalla **Equipo y usuarios** permite al administrador asignar roles y muestra los permisos asociados. Se conservan los tres roles del proyecto; los permisos se asignan por rol, sin permisos personalizados por usuario.

| Recurso/operación | Huésped | Empleado | Administrador |
|---|---|---|---|
| Catálogo, detalles y disponibilidad | Consulta pública | Consulta pública | Consulta pública |
| Crear reservas y consultar mis reservas | Propias | No | No |
| Cancelar reservas | Propias, según política | No | Todas, según política |
| Inventario, estados y reservas administrativas | No | No | Sí |
| Solicitudes de atención | Crea y consulta propias | Consulta y atiende | Consulta y atiende |
| Tareas | No | Consulta y actualiza asignadas | Gestiona todas |
| Usuarios y roles | No | No | Sí |
| Finanzas, recordatorios e indicadores | No | No | Sí |

El registro público siempre crea huéspedes. El backend verifica sesión, vencimiento, rol y propiedad; cambiar el menú o el almacenamiento del navegador no concede permisos. No se acepta un rol desconocido o nulo en la asignación. Un administrador no puede cambiar su propio rol y debe quedar al menos un administrador. Las asignaciones se serializan en base de datos para conservar esa regla.

Al cambiar un rol se revocan **todas** las sesiones del usuario afectado; debe ingresar nuevamente. El frontend comprueba la sesión al recuperar el foco y cada 30 segundos, y vuelve al acceso al recibir un 401. Las peticiones posteriores ya se rechazan en el servidor aunque el navegador conserve una pantalla abierta.

## Endpoints

| Método y ruta | Resultado |
|---|---|
| `GET /api/rooms/{id}?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD` | Habitación, disponibilidad, noches, total y política |
| `DELETE /api/reservations/{id}` | Cancela con respuesta 204; repetición autorizada también devuelve 204 |
| `PUT /api/reservations/{id}` con `status: CANCELLED` | Misma cancelación; devuelve la reserva actualizada |
| `GET /api/roles` | Permisos de cada rol; solo administrador |
| `PATCH /api/users/{id}/role` con `role` | Asigna un rol; solo administrador |
| `PUT /api/users/{id}` | Conserva edición de nombre/teléfono y aplica las mismas reglas si cambia rol |

Las vistas de reservas incluyen `cancellable`, `cancellationReason`, `cancellationDeadline` (fecha/hora con desplazamiento) y `cancellationPolicy`. Respuestas de error: 400 datos inválidos, 401 sesión requerida/vencida, 403 acceso denegado, 404 recurso inexistente y 409 conflicto de política o estado.

## Validación

```powershell
cd backend
mvn --batch-mode --no-transfer-progress verify
cd ../frontend
npm ci
npm run build
```

Alternativa con Docker, desde la raíz:

```powershell
docker run --rm -e TZ=America/Bogota -v "${PWD}/backend:/app" -v maven-sprint2-cache:/root/.m2 -w /app maven:3.9.9-eclipse-temurin-21 mvn --batch-mode --no-transfer-progress verify
docker run --rm -v "${PWD}/frontend:/app" -w /app node:22-alpine sh -c 'npm ci && npm run build'
```

`Sprint2IntegrationTest` cubre detalles y totales, estados operativos, disponibilidad, fechas inválidas, cancelación y nueva reserva de las mismas fechas, liberación el día de llegada, preservación de bloqueos/ocupación, historial, importe original, reembolso único bajo concurrencia, propietario/rol/sesión, cancelación por ambas rutas, estados terminales, prevención de sobreescritura por ID, checkout, protección del historial al eliminar habitaciones, asignación/revocación de roles y acceso directo a rutas administrativas. `CancellationPolicyTest` prueba el segundo anterior al límite, el límite exacto y momentos posteriores con reloj fijo en Colombia. Se ejecutan también las pruebas de los Sprints anteriores.

## Demo

1. Ingresar con `huesped@lindomar.co` / `demo123`, buscar una habitación y abrir **Ver detalles**. Revisar características, precio y fechas. Continuar y confirmar una reserva para mañana.
2. Abrir **Mis reservas**, revisar el plazo y cancelar. Confirmar el aviso, observar el estado cancelado y buscar nuevamente las mismas fechas: vuelven a estar disponibles si la habitación no tiene otro bloqueo.
3. Ingresar con `admin@lindomar.co` / `demo123`. En **Finanzas**, verificar un ingreso y un único reembolso por la reserva. En **Reservas**, comprobar que permanece cancelada.
4. En **Equipo y usuarios**, cambiar una cuenta de prueba de huésped a empleado. Volver a ingresar con ella: verá tareas/solicitudes y carecerá de acceso administrativo. Restaurar su rol para reutilizar la cuenta de demo.
5. Intentar cambiar el rol de la cuenta administradora activa: el control permanece deshabilitado. Probar los casos de límite horario y peticiones manipuladas con la suite automatizada.

Para ejecutar una instancia separada si los puertos habituales están ocupados:

```powershell
$env:FRONTEND_PORT = '3002'
$env:BACKEND_PORT = '8082'
docker compose -p lindomar-sprint2 up --build -d --wait
```

Abrir `http://localhost:3002`. Esta instancia usa su propio volumen de PostgreSQL. Los puertos predeterminados siguen siendo 3000 y 8080.
