# Registro de revisión de PRs — J8

Consulta inicial del 1 de noviembre de 2026. No se publicaron aprobaciones ni comentarios.

## Inventario consultado

[PR #84](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/pull/84),
`fix: restaurar diseño de detalles y usuarios`: abierto, autor JulianJ07,
base `d8fd722`, head `4883317c62eb28a71dff5fc30e9e93f632079af6`.
Se consultaron metadatos y diff completo de sus dos archivos.
Es un PR del mismo responsable: esta lectura es una autoinspección y no sustituye
una revisión independiente de un compañero. No fue integrado en la rama Sprint 3.

Observación estática a verificar en ese PR: al agregar `header` dentro de las tarjetas
de permisos, también se aplica la regla existente `.app-shell header`, que añade
altura de 84 px, padding lateral, fondo y borde de la cabecera global. La regla nueva
`.permission-card header` cambia posición y alineación pero no restablece altura,
padding, fondo ni borde. Puede comprimir el contenido de la tarjeta, especialmente
en móvil. Antes de integrar, comprobar con nombres/permisos largos y ajustar el
selector de la cabecera global a su hijo directo o resetear esos estilos en las tarjetas.
Este hallazgo no se presenta como reproducción visual del head: la verificación visual
de este trabajo se hizo sobre `Julian/Sprint3`, que aún no incluye #84.

Los otros PR abiertos consultados (#85–#92) son actualizaciones de Dependabot
(JaCoCo, ESLint, Lucide, React, Vite, Spring Security y Spring Boot). No se aprobaron:
los cambios mayores de Spring requieren revisar compatibilidad y correr CI sobre su
head; no corresponde aprobarlos solo por tratarse de una actualización automática.

## Registro para revisión entre integrantes

| PR / head SHA | Revisor | Alcance leído | Pruebas verificadas en ese head | Hallazgo | Resolución / reprueba |
|---|---|---|---|---|---|
| Pendiente de PR del equipo | Julián | — | — | — | — |

Antes de aprobar: confirmar head actualizado y base, revisar reglas de negocio y
permisos, ejecutar los casos pertinentes, comprobar CI y enlaces de evidencia, y
registrar observaciones concretas con archivo/línea. Después de nuevos commits,
revisar otra vez los cambios afectados. No declarar probado el head de un PR con
resultados obtenidos en otra rama. J8 continúa durante el sprint.
