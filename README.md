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

Este prototipo usa autenticación simplificada para demostración; antes de producción debe incorporarse Spring Security, contraseñas cifradas, permisos del lado servidor y validaciones de negocio más estrictas.

