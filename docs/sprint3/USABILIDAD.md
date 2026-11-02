# Sprint 3: preparación y ejecución de usabilidad

Responsable de entorno y datos: Julián. Coordinación de sesiones: confirmar con el equipo.
Preparado el 1 de noviembre de 2026. **Estado: protocolo complementado; sesiones con usuarios pendientes.**

Este complemento conserva las cinco hipótesis y los umbrales de la sección 8 de la
[Wiki del Sprint 2](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/wiki/Sprint-2).
Atiende la retroalimentación del profesor: equipos y contextualización explícitos,
preguntas antes/después de cada tarea y separación de las dos tareas de H-US-04.
El equipo debe acordar esta versión antes de comenzar las sesiones.

## Logística y preparación

Sesiones previstas para el **4 y 5 de noviembre de 2026**, hora de Colombia; confirmar
lugar, hora y responsable de cada cita en la tabla. No son sesiones realizadas.

| Participante | Perfil requerido | Fecha/hora/lugar | Facilitador | Observador |
|---|---|---|---|---|
| P01 | Cliente potencial / huésped | Por confirmar | Por confirmar | Por confirmar |
| P02 | Cliente potencial / huésped | Por confirmar | Por confirmar | Por confirmar |
| P03 | Cliente potencial / huésped | Por confirmar | Por confirmar | Por confirmar |
| P04 | Persona representativa del personal operativo | Por confirmar | Por confirmar | Por confirmar |
| P05 | Persona representativa de administración hotelera | Por confirmar | Por confirmar | Por confirmar |

Validar el perfil real y su experiencia con reservas y aplicaciones. Los desarrolladores
pueden hacer un ensayo técnico, pero ese ensayo no cuenta como resultado de usuarios finales.
Registrar solo el código de participante en la matriz pública.

Equipos: portátil con navegador actualizado, mouse o panel táctil, conexión a internet,
cargador, cronómetro, hoja de observación y equipo de grabación de pantalla/audio si
el participante acepta. Tener el entorno local aislado como respaldo; registrar si se
usó el sitio desplegado o el respaldo. No mezclar ambos sin identificar el entorno.

Secuencia por sesión: 3 minutos de bienvenida/contexto, 2 de perfil y consentimiento,
10–18 de tareas según el rol, 5 de entrevista/cierre. El observador mide por tarea;
el facilitador lee el escenario sin explicar qué botones usar. No evalúa a la persona.

Texto inicial: «Estamos evaluando la aplicación de Hotel Lindomar. Usa estos datos
ficticios como si estuvieras reservando o trabajando en el hotel. Puedes pensar en voz
alta y detenerte cuando quieras. Si algo no es claro, nos interesa observarlo. No hay
respuestas correctas sobre tu experiencia. La grabación es opcional».

Solicitar autorización antes de grabar. Registrar duración, permiso y ubicación de la
evidencia; evitar nombres reales y credenciales en capturas compartidas.

## Datos reproducibles

Usar una base **de pruebas**, separada de producción. Con la aplicación local en marcha:

```powershell
$env:USABILITY_SESSION = 'P01'
node scripts/seed-usability.mjs
```

El script usa `http://127.0.0.1:8080/api/`, solo permite direcciones locales y no elimina
registros. Se autentica con el administrador de demostración; si cambió, definir
`USABILITY_ADMIN_EMAIL` y `USABILITY_ADMIN_PASSWORD` en el entorno local. No publicarlos.
Repetirlo sin consumir los escenarios no duplica los mismos registros. Después de
cancelar/pagar/completar, usar otro código de sesión para preparar datos nuevos.

Para cada código crea:

- Huésped `us-p01-huesped@example.test` y empleado `us-p01-empleado@example.test`
  (sustituir `p01` por el código en minúsculas); contraseña ficticia `UsabilidadS3-2026`,
  configurable con `USABILITY_PASSWORD`.
- Habitaciones `US-P01-401` y `402` disponibles, `403` en mantenimiento, `404` fuera
  de servicio y `405` reservada operativamente.
- Reserva confirmada del huésped en `401`, llegada D+7 y salida D+9, dos personas.
- Tarea pendiente asignada al empleado y recordatorio pendiente de $25.000 COP.

El script entrega los identificadores y fechas exactos. Guardar esa salida con la
evidencia de la sesión. D es la fecha de preparación en `America/Bogota`; si se preparó
el 1 de noviembre, la reserva será del 8 al 10 de noviembre. Verificar otra vez las
fechas antes de las citas. Confirmar que no hay datos personales reales.

## Tareas, preguntas y criterios

Leer únicamente el escenario al participante. Los resultados esperados son guía del
observador. Medir desde que se termina de leer hasta que se logra el resultado o la
persona abandona; registrar tiempo real aun si supera el umbral.

| Tarea / hipótesis | Escenario para la persona | Pregunta previa | Preguntas posteriores | Éxito y umbral |
|---|---|---|---|---|
| T01 / H-US-01 | «Busca una habitación para dos personas entre D+14 y D+16. Revisa sus características y reserva la opción que cumpla lo que necesitas». | «¿Qué información necesitas revisar antes de reservar?» | «¿Qué habitación y fechas reservaste? ¿Qué te costó encontrar? ¿Qué tan fácil fue, de 1 a 5?» | Reserva nueva confirmada con las fechas/capacidad correctas, sin asistencia, ≤5 min. Al menos 2/3 huéspedes. |
| T02 / H-US-02 | «Tu viaje de la reserva preparada en 401 se canceló. Cancela esa estadía y verifica qué ocurrió». | «¿Qué esperarías que pase con una reserva y su pago al cancelarla?» | «¿Cómo sabes que quedó cancelada? ¿Qué entendiste de la política y el reembolso? Facilidad 1–5». | Reserva preparada cancelada; sin error crítico, ≤3 min. Al menos 2/3 huéspedes. |
| T03 / H-US-03 | «Encuentra tu tarea de preparación de habitación, informa que la empezaste y después que la terminaste». | «¿Cómo informarías al administrador que una actividad está en curso o terminada?» | «¿Qué estados viste? ¿Supiste cómo agregar evidencia opcional? Facilidad 1–5». | Pendiente → En progreso → Completada, sin asistencia ni transición inválida, ≤4 min. Foto opcional. |
| T04-A / H-US-04 | «Asigna al empleado ficticio una revisión del minibar, con prioridad alta y entrega mañana». | «¿Qué información necesitas para asignar una actividad?» | «¿A quién quedó asignada y con qué fecha/prioridad? Facilidad 1–5». | Tarea persistida para EMPLOYEE, título/fecha/prioridad correctos, sin asistencia, ≤6 min. |
| T04-B / H-US-04 | «La cuenta ficticia de huésped de esta sesión ahora será parte del personal. Dale el rol de empleado y revisa qué funciones podrá usar». | «¿Qué diferencia esperas entre el acceso de un huésped y el de un empleado?» | «¿Qué acceso acabas de conceder? ¿Podrías cambiar tu propio rol? Facilidad 1–5». | Cambia el rol de la cuenta indicada, distingue permisos y no intenta una operación no autorizada, sin asistencia, ≤6 min. |
| Cierre / H-US-05 | Pregunta a cada una de las cinco personas al finalizar sus tareas. | «Antes de empezar, ¿qué experiencia tienes con aplicaciones similares?» (contexto, no calificación final). | «En general, ¿qué tan fácil fue usar Hotel Lindomar, de 1=muy difícil a 5=muy fácil? ¿Qué cambiarías primero?» | Al menos 4/5 califican 4 o 5. Mantener separado el resultado general de la valoración por tarea. |

Para T04-B usar una cuenta de la sesión del administrador que no necesite volver a
ejecutar T01/T02. H-US-04 solo se considera confirmada si T04-A **y** T04-B cumplen;
informar resultados de ambas por separado. No dar por fallida toda la aplicación
porque un usuario dude: registrar lo observado y aplicar el criterio definido.

## Registro de resultados (por diligenciar)

| Participante | Tarea | Entorno / commit | Inicio/fin | Segundos | Completada | Errores y pasos | Ayuda solicitada/dada | Facilidad 1–5 | Observación literal | Evidencia |
|---|---|---|---|---|---|---|---|---|---|---|
| Pendiente | Pendiente | Pendiente | — | — | — | — | — | — | — | — |

Error crítico: requiere intervención o reinicio. Asistencia: cualquier guía más allá
de leer el escenario. Registrar cantidad y descripción, incluyendo errores recuperables.
No calcular porcentajes ni promedios sobre celdas sin observación.

Después de ejecutar: calcular por tarea éxito sin ayuda / intentos, tiempos de los
intentos exitosos, errores y facilidad. Informar abandonos, datos faltantes y tamaño
de muestra. Comparar cada resultado con el umbral original; clasificar confirmado,
parcialmente confirmado o no confirmado con su evidencia. No confundir pruebas
automáticas o capturas del manual con resultados de participantes.

## Hallazgos y correcciones

| Hallazgo | Participante/tarea/evidencia | Severidad e impacto | Cambio / responsable | PR/commit | Reprueba | Decisión |
|---|---|---|---|---|---|---|
| Pendiente de sesiones | — | — | — | — | — | — |

Julián atiende los hallazgos de lógica/backend de su alcance (J7). Cambios sencillos:
corregir y volver a verificar antes de la entrega. Cambios de alta complejidad:
documentar historia para un hipotético Sprint 4, impacto y justificación. No cerrar J7
ni afirmar éxito de H-US-01…05 hasta contar con sesiones reales y revisión del equipo.
