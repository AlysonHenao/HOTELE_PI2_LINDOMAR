# Sprint 3: trabajo de Julián

Rama de trabajo: `Julian/Sprint3`. Base: `main` / `d8fd722`. Inicio: 1 de noviembre
de 2026. Esta carpeta conserva documentación revisable junto al código; las secciones
se preparan también en la copia local de la Wiki. La publicación en GitHub sigue pendiente.

## Alcance y estado

| ID | Trabajo | Estado y evidencia |
|---|---|---|
| J1 | HU-30/31/32: finanzas y pago idempotente | Implementado y verificado localmente. [Pruebas](CALIDAD.md). |
| J2 | HU-10 si estaba priorizada; en su defecto correcciones de reservas | Se sigue la alternativa: validación de observaciones y regresión de reservas. HU-10 abierta, etiqueta Should y sin milestone al consultar; no se presenta como comprometida. |
| J3 | Despliegue automático | [Preparado para AWS EC2](DESPLIEGUE-AWS.md); faltan infraestructura, permisos, dominio y ejecución real. |
| J4 | Caso de negocio §4: operación | [Estimación con costos fijos y variables](PRESUPUESTO-OPERACION.md), por validar al elegir instancia. |
| J5 | Manual administrador | [Preparado con capturas reales de la app local](MANUAL-ADMINISTRADOR.md); pendiente revisión con el cliente y de estilo por Alyson. |
| J6 | Entorno/datos de usabilidad | Seed reproducible ejecutado en PostgreSQL local aislado. [Preparación y protocolo complementario](USABILIDAD.md). |
| J7 | Hallazgos de lógica/backend de usabilidad | Preparación de registro; faltan sesiones y hallazgos reales. No se declaran corregidos anticipadamente. |
| J8 | Revisión de PRs | [Registro inicial](REVISION-PRS.md); actividad continua, requiere revisión entre integrantes. |

Consulta del backlog: [HU-10](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/10),
[HU-30](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/30),
[HU-31](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/31),
[HU-32](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/32) y
[HU-36](https://github.com/AlysonHenao/HOTELE_PI2_LINDOMAR/issues/36).
Los recordatorios aparecen como HU-15 en comentarios históricos del código, pero
el backlog de GitHub los identifica como HU-36. Usar la referencia del backlog al reportar.

## Retroalimentación del profesor incorporada

| Observación Sprint 2 | Respuesta en este trabajo |
|---|---|
| No se evidencian preguntas pre/post por tarea | Tabla de escenarios con preguntas previas y posteriores en Usabilidad. |
| Faltan equipos y contextualización detallada | Logística, equipos y guion inicial explícitos; citas y participantes aún por confirmar. |
| Separar mejor tareas H-US-04 | T04-A asigna una tarea; T04-B cambia un rol. Se miden por separado. |
| Código/PRs/ramas no verificables y enlaces técnicos inaccesibles | Pruebas reproducibles, evidencia local versionable y artefactos de CI. Completar enlaces al PR y al run reales al publicar. |
| Hipótesis y tareas poco claras en la presentación | Conservar H-US-01…05, explicar criterio por tarea y mostrar mediciones reales en la sustentación. |

## Criterios de entrega del equipo

La rúbrica reparte: caso de negocio 15 %, usabilidad 20 %, manual 15 %, despliegue
20 %, desarrollo 20 % y presentación 10 %. La entrega se evalúa en la herramienta
de gestión/Wiki. La carpeta local no reemplaza esa publicación.

Antes de cerrar el sprint, publicar las secciones revisadas en la Wiki, cerrar backlog
y ceremonias con evidencias, completar resultados de usuarios, verificar URL pública
en README/Wiki e integrar el manual de los tres perfiles. El resto del caso de negocio
incluye riesgos, RACI, mitigación, viabilidad e informe de cierre: coordinar con sus
responsables; esta contribución cubre presupuesto operativo.

Todos deben participar en la sustentación. El documento maestro indica nota 0,0 del
sprint para quien no asista. Coordinar las intervenciones con A10 y preparar una
demostración breve de las reglas financieras, el manual y el despliegue cuando exista.

No cerrar J3 por tener un YAML, J7 por tener una tabla vacía ni usabilidad por pasar
pruebas automáticas. Los estados anteriores distinguen lo implementado de lo pendiente.
