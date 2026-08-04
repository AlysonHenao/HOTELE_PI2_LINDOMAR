# LINDOMAR · Prototipo hotelero

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

- Huésped: búsqueda y filtros, mapa de disponibilidad, creación/cancelación de reservas, solicitudes de atención y asistente FAQ.
- Empleado: consulta de tareas y actualización de su progreso.
- Administrador: indicadores, habitaciones, reservas, usuarios, asignación de tareas y movimientos financieros.

Este prototipo usa autenticación simplificada para demostración; antes de producción debe incorporarse Spring Security, contraseñas cifradas, permisos del lado servidor y validaciones de negocio más estrictas.

