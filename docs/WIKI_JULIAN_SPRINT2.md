# Sprint 2 - Aporte individual de Julián Jiménez

## 1. Identificación y alcance

| Campo | Información |
|---|---|
| Integrante | **Julián Jiménez** |
| Rol | Software Developer |
| Sprint | Sprint 2 - MVP Versión 2 |
| Responsabilidad | Desarrollo fullstack y validación funcional de HU-06, HU-11 y HU-24 |
| Rama de trabajo | [`Sprint2/Julian`](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/tree/Sprint2/Julian) |
| Comparación con `main` | [Ver cambios](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/compare/main...Sprint2/Julian) |

Esta página documenta exclusivamente el aporte asignado a Julián Jiménez. La evidencia se organiza de acuerdo con los dos aspectos del Documento maestro del Sprint 2 que aplican directamente a su responsabilidad: **cumplimiento funcional de las historias planeadas** y **pruebas automáticas coherentes con los criterios de aceptación**, incluyendo como mínimo un camino feliz y un flujo alternativo por funcionalidad.

## 2. Historias de usuario asignadas

| Historia | Descripción | Prioridad / tamaño | Alcance de Julián |
|---|---|---|---|
| [HU-06](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/6) | Ver detalles de habitación antes de reservar | Must / S | Refinamiento, frontend, backend y pruebas |
| [HU-11](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/11) | Cancelar una reserva cuando la política lo permita | Should / M | Lógica de negocio, liberación de disponibilidad, reembolso, seguridad y pruebas |
| [HU-24](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/24) | Gestionar roles y permisos de los usuarios | Must / L | Seguridad backend, administración frontend, sesiones y pruebas |

---

## 3. HU-06 - Ver detalles de habitación

### 3.1 Historia de usuario

> Como huésped, quiero consultar los detalles de una habitación para conocer sus características antes de reservarla.

### 3.2 Necesidad atendida

En la versión anterior el detalle se presentaba de forma incompleta y se había registrado el hallazgo [#59](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/59) por ausencia de fotografías. Para el Sprint 2 se convirtió el detalle en un paso independiente, previo a la reserva, y se añadió una fuente de fotografía configurable. No se incluyeron imágenes ficticias del hotel: si la URL no existe o falla, la interfaz informa que la fotografía no está disponible.

### 3.3 Criterios de aceptación refinados

1. Desde la lista o el mapa, el huésped puede seleccionar una habitación y abrir su detalle sin crear todavía una reserva.
2. El sistema muestra número, tipo, piso, capacidad, balcón, política de mascotas, servicios y tarifa por noche.
3. Si se seleccionaron fechas válidas, el servidor calcula noches, total y disponibilidad actual del período.
4. La disponibilidad se vuelve a consultar en el servidor y considera tanto el estado operativo de la habitación como los cruces con reservas no canceladas.
5. Una reserva que termina el día de llegada de otra no genera cruce, porque el período se interpreta como `[entrada, salida)`.
6. La fotografía se obtiene de una URL HTTPS configurada por administración. Una URL ausente o una imagen que no carga produce un aviso explícito.
7. El botón para continuar a la reserva solo está habilitado cuando la habitación está disponible para las fechas consultadas.
8. Una habitación inexistente o unas fechas incompletas, inválidas o invertidas producen un error explícito.

### 3.4 Implementación realizada

**Frontend**

- Vista de detalle independiente antes del formulario de confirmación.
- Presentación de características, servicios, fechas, noches, tarifa y total.
- Estados visibles de disponibilidad y fotografía no disponible.
- Continuación desde el detalle hacia la reserva conservando habitación y fechas.
- Campo administrativo para una fotografía mediante URL HTTPS.

**Backend**

- Consulta `GET /api/rooms/{id}?checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD`.
- Cálculo del total con la tarifa vigente y el número de noches.
- Validación de formato y orden de fechas.
- Revisión de disponibilidad por estado operativo y solapamiento de reservas.
- Validación de URL HTTPS, longitud, host y ausencia de credenciales embebidas.
- Revalidación de capacidad, estado y fechas al confirmar la reserva, evitando confiar únicamente en la interfaz.

### 3.5 Casos de prueba diseñados y ejecutados

| ID | Escenario | Tipo | Resultado esperado | Resultado |
|---|---|---|---|---|
| CP-S2-JJ-06-01 | Abrir una habitación disponible con fechas válidas | Camino feliz | Mostrar toda la información, noches, total y disponibilidad | **Pasó** |
| CP-S2-JJ-06-02 | Cambiar la tarifa y consultar nuevamente | Alternativo | Recalcular el total con el precio actual | **Pasó** |
| CP-S2-JJ-06-03 | Consultar fechas ocupadas y luego un período no solapado | Alternativo | Marcar el primer período no disponible y el segundo disponible | **Pasó** |
| CP-S2-JJ-06-04 | Probar todos los estados operativos | Parametrizado | Solo `AVAILABLE` permite continuar | **Pasó** |
| CP-S2-JJ-06-05 | Consultar habitación inexistente | Error | Responder 404 | **Pasó** |
| CP-S2-JJ-06-06 | Enviar fechas incompletas, inválidas o iguales | Error | Responder 400 con mensaje explícito | **Pasó** |
| CP-S2-JJ-06-07 | Habitación sin fotografía configurada | Interfaz | Mostrar “Fotografía no disponible” sin romper el flujo | **Pasó** |

**Evidencia automática principal:**

- `roomDetailsContainCurrentInformationAndPeriodPrice`
- `detailsRespectEveryOperationalStatus`
- `missingRoomAndInvalidDatesHaveExplicitErrors`

---

## 4. HU-11 - Cancelación de reserva

### 4.1 Historia de usuario

> Como huésped, quiero cancelar una reserva cuando las políticas del hotel lo permitan para no conservar una reserva que ya no necesito.

### 4.2 Política definida para el prototipo

La política acordada permite cancelar **hasta antes de dos horas del check-in**. Como la entrada del hotel es a las `15:00`, el límite es:

- Zona horaria: `America/Bogota`.
- Límite: `13:00` del día de llegada.
- A las `12:59:59` se permite cancelar.
- Desde las `13:00:00` se rechaza la cancelación.
- El cálculo se realiza con el reloj del servidor, no con la hora del navegador.

### 4.3 Criterios de aceptación refinados

1. Solo se puede cancelar una reserva en estado `CONFIRMED` y antes del límite.
2. El huésped solo puede cancelar sus propias reservas; un administrador puede cancelar cualquier reserva bajo la misma política.
3. Un empleado no puede cancelar reservas.
4. La reserva cambia a `CANCELLED`, pero permanece en el historial.
5. Se registra un reembolso completo por el importe almacenado en la reserva. El prototipo registra un asiento financiero; no procesa un pago externo.
6. Las fechas canceladas vuelven a estar disponibles.
7. La cancelación de una reserva futura no libera una habitación ocupada por otra estadía ni elimina bloqueos `RESERVED`, `MAINTENANCE` u `OUT_OF_SERVICE`.
8. Una repetición o dos cancelaciones simultáneas producen un solo reembolso.
9. La ruta administrativa usa la misma política y no permite reactivar reservas canceladas/finalizadas ni cambiar fechas para evadir el límite.
10. No se puede eliminar una habitación con historial de reservas.

### 4.4 Implementación realizada

**Frontend**

- Estado, importe, política y fecha límite visibles en “Mis reservas”.
- Botón de cancelación únicamente cuando la reserva es cancelable.
- Confirmación previa con número de reserva, habitación e importe del reembolso.
- Mensaje de éxito y conservación de la reserva cancelada en el historial.

**Backend**

- `DELETE /api/reservations/{id}` para huésped propietario o administrador.
- `PUT /api/reservations/{id}` con `status: CANCELLED` reutiliza la misma política.
- Servicio `CancellationPolicy` con reloj, zona, check-in y horas de anticipación configurables.
- Transacción única para cambio de estado, liberación y registro financiero.
- Bloqueo pesimista de la reserva para serializar cancelaciones concurrentes.
- Reembolso calculado con el importe guardado, aunque la tarifa de la habitación cambie después.
- Conservación de estados operativos y ocupaciones ajenas.

### 4.5 Casos de prueba diseñados y ejecutados

| ID | Escenario | Tipo | Resultado esperado | Resultado |
|---|---|---|---|---|
| CP-S2-JJ-11-01 | Propietario cancela una reserva confirmada antes del límite | Camino feliz | Estado cancelado, fechas liberadas, historial conservado y reembolso completo | **Pasó** |
| CP-S2-JJ-11-02 | Volver a reservar las fechas liberadas | Integración | Permitir una nueva reserva | **Pasó** |
| CP-S2-JJ-11-03 | Cancelar a las `12:59:59`, `13:00:00` y `13:00:01` | Límite | Permitir solo el primer instante | **Pasó** |
| CP-S2-JJ-11-04 | Cancelar una estadía iniciada o finalizada | Alternativo | Rechazar incluso si actúa un administrador | **Pasó** |
| CP-S2-JJ-11-05 | Cancelar sin sesión, con token falso, como empleado o como otro huésped | Seguridad | Responder 401 o 403 y no crear reembolso | **Pasó** |
| CP-S2-JJ-11-06 | Cancelar una reserva futura con habitación ocupada o bloqueada | Integración | Conservar ocupación y bloqueos manuales | **Pasó** |
| CP-S2-JJ-11-07 | Repetir la cancelación y ejecutar dos solicitudes simultáneas | Concurrencia | Conservar un único reembolso | **Pasó** |
| CP-S2-JJ-11-08 | Intentar reactivar, mover fechas o cancelar fuera de plazo por la ruta administrativa | Regla de negocio | Rechazar la transición | **Pasó** |
| CP-S2-JJ-11-09 | Finalizar una estadía el día de salida | Integración | Liberar habitación sin generar reembolso | **Pasó** |
| CP-S2-JJ-11-10 | Intentar eliminar una habitación con reservas | Integridad | Rechazar la eliminación y conservar el historial | **Pasó** |

**Evidencia automática principal:**

- `CancellationPolicyTest.twoHourDeadlineUsesHotelTimezoneAndRejectsAtTheCutoff`
- `cancellationReleasesDatesPreservesHistoryAndRefundsOriginalAmountOnce`
- `cancelTodayBeforeOnePmReleasesItsRoom`
- `cancellingFutureStayNeverClearsCurrentOccupancyOrManualBlocks`
- `startedAndCompletedStaysCannotBeCancelledEvenByAdmin`
- `cancellationRequiresOwnershipRoleAndValidSession`
- `adminUpdateUsesSamePolicyAndCannotResurrectOrMoveAnExpiredReservation`
- `simultaneousCancellationsCreateExactlyOneRefund`

---

## 5. HU-24 - Gestión de roles y permisos

### 5.1 Historia de usuario

> Como administrador, quiero asignar roles y permisos a los usuarios para controlar el acceso según sus responsabilidades.

### 5.2 Modelo de autorización

| Recurso u operación | Huésped | Empleado | Administrador |
|---|---:|---:|---:|
| Consultar catálogo, detalles y disponibilidad | Sí | Sí | Sí |
| Crear reservas y consultar reservas propias | Sí, propias | No | No |
| Cancelar reservas | Propias y según política | No | Todas y según política |
| Gestionar inventario, estados y reservas | No | No | Sí |
| Crear y consultar solicitudes de atención | Propias | Consulta y atiende | Consulta y atiende |
| Consultar y actualizar tareas | No | Solo asignadas | Todas |
| Gestionar usuarios y roles | No | No | Sí |
| Consultar finanzas, recordatorios e indicadores | No | No | Sí |

### 5.3 Criterios de aceptación refinados

1. Un administrador puede consultar los permisos y asignar `GUEST`, `EMPLOYEE` o `ADMIN`.
2. La autorización se verifica en el backend con sesión, vencimiento, rol y propiedad del recurso.
3. Ocultar o modificar opciones en el navegador no concede permisos.
4. El registro público siempre crea cuentas `GUEST`.
5. No se acepta un rol nulo o desconocido.
6. Un administrador no puede cambiar su propio rol.
7. El sistema no puede quedar sin administradores.
8. Cambiar un rol revoca todas las sesiones anteriores del usuario afectado.
9. Una sesión vencida o un usuario sin rol no obtiene acceso.
10. El frontend vuelve al acceso cuando recibe 401 y también revalida la sesión al recuperar el foco y periódicamente.

### 5.4 Implementación realizada

**Frontend**

- Vista “Equipo y usuarios” con cuentas, rol actual y permisos de cada perfil.
- Selector de rol disponible únicamente para administración.
- Bloqueo visual del cambio del rol de la cuenta administradora activa.
- Navegación distinta para huésped, empleado y administrador.
- Revalidación de sesión y cierre local ante una respuesta 401.

**Backend**

- `GET /api/roles` devuelve la matriz de permisos solo a administradores.
- `PATCH /api/users/{id}/role` aplica las reglas de asignación.
- `PUT /api/users/{id}` conserva la edición de datos y reutiliza las mismas reglas si cambia el rol.
- Sesiones opacas persistidas y verificadas en cada operación protegida.
- Bloqueo de cuentas durante el cambio para proteger la regla del último administrador.
- Revocación de todas las sesiones del usuario cuyo rol cambia.
- Comprobaciones de propiedad para reservas, solicitudes y tareas.

### 5.5 Casos de prueba diseñados y ejecutados

| ID | Escenario | Tipo | Resultado esperado | Resultado |
|---|---|---|---|---|
| CP-S2-JJ-24-01 | Administrador cambia huésped a empleado | Camino feliz | Rol actualizado y acceso a tareas, sin acceso a reservas propias ni usuarios | **Pasó** |
| CP-S2-JJ-24-02 | Consultar permisos por rol | Camino feliz | Devolver la matriz `GUEST`, `EMPLOYEE` y `ADMIN` | **Pasó** |
| CP-S2-JJ-24-03 | Usar sesiones anteriores después de cambiar el rol | Seguridad | Rechazar todos los tokens anteriores | **Pasó** |
| CP-S2-JJ-24-04 | Administrador intenta cambiar su propio rol | Alternativo | Rechazar con 409 | **Pasó** |
| CP-S2-JJ-24-05 | Enviar rol nulo, vacío o desconocido | Validación | Rechazar con 400 | **Pasó** |
| CP-S2-JJ-24-06 | Huésped o empleado accede directamente a recursos administrativos | Seguridad | Rechazar con 403, aunque manipule la ruta | **Pasó** |
| CP-S2-JJ-24-07 | Usar sesión vencida o cuenta sin rol | Seguridad | Rechazar el acceso | **Pasó** |
| CP-S2-JJ-24-08 | Asignar el mismo rol que ya tiene el usuario | Alternativo | Mantener las sesiones, al no existir cambio efectivo | **Pasó** |
| CP-S2-JJ-24-09 | Promover y luego degradar un administrador | Integración | Aplicar permisos nuevos y revocar sesiones en cada cambio real | **Pasó** |

**Evidencia automática principal:**

- `adminCanAssignRolesAndAllOldSessionsAreRevoked`
- `roleValidationProtectsSelfAndRejectsUnknownRolesOrUsers`
- `expiredSessionAndMissingRoleNeverGrantAccess`
- `demotionRevokesAdminAccessAndNoOpRoleChangeKeepsSessions`
- `restrictedRolesCannotReadOrWriteAdministrativeResources`

---

## 6. Estrategia de pruebas automáticas del aporte

La estrategia combina pruebas unitarias, de integración, de seguridad y de concurrencia. Se utiliza un reloj fijo para que los límites horarios sean reproducibles y una base H2 en modo PostgreSQL para probar repositorios y transacciones sin depender de datos manuales. Esta automatización complementa, pero no reemplaza, la validación visual en navegador.

| Funcionalidad | Tipo de prueba | Justificación | Camino feliz | Flujo alternativo |
|---|---|---|---|---|
| HU-06 - Detalle y disponibilidad | Integración API/repositorio | Debe validar datos, cálculo y cruces con el estado persistido | Habitación disponible con total correcto | Habitación ocupada, estado no disponible, fechas inválidas o recurso inexistente |
| HU-11 - Política horaria | Unitaria parametrizada | El límite exacto es una regla pura sensible a zona horaria | Un segundo antes del límite | Límite exacto y momentos posteriores |
| HU-11 - Cancelación integral | Integración transaccional | Cambia reserva, habitación y finanzas en una misma operación | Cancelación del propietario con reembolso | Estado no cancelable, actor no autorizado, bloqueo manual o ruta administrativa inválida |
| HU-11 - Idempotencia | Integración concurrente | Dos solicitudes pueden llegar simultáneamente | Primera cancelación | Repetición y dos cancelaciones concurrentes con un solo reembolso |
| HU-24 - Asignación de roles | Integración de seguridad | Debe comprobar persistencia, sesiones y permisos reales | Administrador asigna un rol válido | Autoasignación, rol inválido, usuario inexistente o último administrador |
| HU-24 - Control de acceso | Integración parametrizada | Verifica que la seguridad no dependa del menú | Cada rol usa sus recursos permitidos | Acceso directo de huésped/empleado a endpoints administrativos |

### Resultado de la ejecución

| Métrica | Resultado |
|---|---:|
| Pruebas automáticas directas para HU-06, HU-11 y HU-24 | 31 |
| Total de la suite ejecutada para descartar regresiones | **72** |
| Fallos | **0** |
| Errores | **0** |
| Omitidas | **0** |
| Resultado Maven | **BUILD SUCCESS** |

Comando ejecutado:

```powershell
cd backend
mvn --batch-mode --no-transfer-progress verify
```

La compilación del frontend también terminó correctamente:

```powershell
cd frontend
npm ci
npm audit --audit-level=high
npm run build
```

Resultado: compilación Vite exitosa y **0 vulnerabilidades conocidas reportadas por `npm audit`** después de actualizar `source-map-js` a `1.2.2`.

## 7. Validación del MVP desplegado

Se construyó una instancia aislada, sin eliminar volúmenes:

```powershell
$env:FRONTEND_PORT = '3002'
$env:BACKEND_PORT = '8082'
docker compose -p lindomar-sprint2 up --build -d --wait
docker compose -p lindomar-sprint2 ps
```

Resultado final:

- PostgreSQL: `healthy`.
- Backend: `healthy` y respuesta HTTP 200 en `http://localhost:8082/api/rooms`.
- Frontend: `healthy` y respuesta HTTP 200 en `http://localhost:3002`.
- Se corrigió el `healthcheck` del frontend para consultar `127.0.0.1`; dentro del contenedor `localhost` resolvía a `::1`, donde Nginx no estaba escuchando.

### Recorrido manual en navegador

1. Inicio de sesión como huésped.
2. Búsqueda de habitación con fechas válidas.
3. Apertura del detalle con características, servicios, tarifa, total y disponibilidad.
4. Continuación a la reserva y confirmación.
5. Consulta en “Mis reservas”.
6. Cancelación con confirmación del reembolso.
7. Verificación del estado `CANCELLED` y del mensaje de fechas liberadas.
8. Inicio de sesión como administrador.
9. Consulta de “Equipo y usuarios”, permisos y selectores de roles.
10. Verificación visual de que el administrador activo no puede cambiar su propio rol.

## 8. Evidencias para adjuntar en la Wiki

Antes de publicar, cargar las capturas en GitHub y sustituir cada marcador por la URL generada:

```markdown
### Evidencia HU-06 - Detalle de habitación

![HU-06 - Detalle de habitación](PEGAR_AQUI_URL_DE_LA_CAPTURA_HU06)

### Evidencia HU-11 - Reserva cancelada

![HU-11 - Reserva cancelada y reembolso registrado](PEGAR_AQUI_URL_DE_LA_CAPTURA_HU11)

### Evidencia HU-24 - Roles y permisos

![HU-24 - Administración de roles y permisos](PEGAR_AQUI_URL_DE_LA_CAPTURA_HU24)

### Evidencia técnica - Pruebas automáticas

![72 pruebas sin fallos](PEGAR_AQUI_URL_DEL_BUILD_SUCCESS)

### Evidencia técnica - Docker Compose

![Servicios saludables](PEGAR_AQUI_URL_DE_DOCKER_PS)
```

## 9. Trazabilidad de cumplimiento

| Historia | Criterios cubiertos | Evidencia de código | Evidencia de pruebas | Estado |
|---|---|---|---|---|
| HU-06 | Información completa, fotografía, disponibilidad, total y continuación | `frontend/src/main.jsx`, `frontend/src/styles.css`, `HotelController.java`, `Domain.java` | Casos CP-S2-JJ-06-01 a 07 | **Cumple** |
| HU-11 | Política, propiedad, estados, reembolso, historial, liberación e idempotencia | `CancellationPolicy.java`, `HotelController.java`, repositorios de reservas y finanzas | Casos CP-S2-JJ-11-01 a 10 | **Cumple** |
| HU-24 | Asignación, matriz de permisos, seguridad backend y revocación de sesiones | `HotelController.java`, `UserRepository.java`, `AuthSessionRepository.java`, `frontend/src/main.jsx` | Casos CP-S2-JJ-24-01 a 09 | **Cumple** |

## 10. Guion individual de sustentación

1. **HU-06:** buscar una habitación, abrir “Ver detalles” y señalar fotografía/aviso, características, disponibilidad, tarifa, noches y total. Continuar a la reserva.
2. **HU-11:** confirmar una reserva para una fecha cancelable, abrir “Mis reservas”, mostrar la fecha límite y cancelar. Explicar la conservación del historial, el reembolso único y la liberación de fechas.
3. **HU-24:** ingresar como administrador, abrir “Equipo y usuarios”, explicar la matriz de permisos y mostrar la restricción sobre el propio rol. Cambiar una cuenta de prueba a empleado, volver a iniciar sesión y comprobar su navegación restringida. Restaurar el rol al finalizar.
4. **Pruebas:** mostrar el resumen `Tests run: 72, Failures: 0, Errors: 0, Skipped: 0` y mencionar los límites horarios, permisos y cancelaciones simultáneas.
5. **Cierre:** mostrar `docker compose ps` con los tres servicios saludables y declarar que la versión fue validada de extremo a extremo.

## 11. Conclusión individual

Las tres historias asignadas a Julián Jiménez quedaron implementadas y verificadas en backend y frontend. HU-06 completa la información previa a la reserva y resuelve el hallazgo de fotografía sin inventar contenido; HU-11 aplica una política horaria consistente, conserva la trazabilidad financiera y evita duplicados bajo concurrencia; HU-24 aplica los permisos en el servidor, protege la administración y revoca sesiones ante cambios de rol. La suite completa y el recorrido sobre Docker Compose confirman el cumplimiento funcional del aporte para el MVP Versión 2.
