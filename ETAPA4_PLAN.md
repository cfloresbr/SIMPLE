# SIMPLE Etapa 4 — borrador anterior de integridad de alertas e historial

> **Estado:** este documento es un borrador local de análisis técnico, no define la hoja de ruta oficial ni el alcance vigente de la etapa 4. La referencia principal es [HOJA_DE_RUTA_SIMPLE.md](HOJA_DE_RUTA_SIMPLE.md), que prioriza revisar y completar el flujo funcional por roles y tipos de alerta. El análisis de integridad aquí descrito queda como riesgo diferido y solo se retoma después de estabilizar el flujo principal, o si una anomalía reproducible exige investigarlo. No ejecutar este plan contra la base compartida.

## Estado

Borrador de planificación en la rama local `etapa4`, creada desde `origin/etapa3` en el commit `2ab9353` (`check etapa 3`). No hay cambios de código de producción ni cambios de base de datos en esta fase. El plan prepara pruebas en una base de datos aislada antes de considerar cualquier corrección.

## Motivo y prioridad

La fase 3 estabilizó el flujo principal de consultas, errores y reintentos. El riesgo de que una operación concurrente o repetida afecte alertas o historiales ajenos quedó explícitamente diferido en `ETAPA3_PLAN.md`; ahora se puede revisar sin interrumpir la estabilización.

El Informe Maestro del 20 de octubre prioriza la persistencia, la trazabilidad del historial y la verificación del estado final en la base de datos. Su plan de siguiente ciclo también propone respaldo antes de las pruebas y comparación de datos antes/después.

## Revisión inicial del código

Observaciones de lectura, no defectos reproducidos:

- `AlertController.changeStatus` lee la alerta, valida el estado actual, guarda el estado y agrega historial dentro de una transacción.
- No se encontraron anotaciones de bloqueo JPA ni campo de versión (`@Version`) en los modelos/repositorios revisados. Las pruebas actuales cubren transiciones secuenciales con repositorios simulados, no solicitudes concurrentes contra MySQL.
- `POST /api/alerts` no muestra un mecanismo de idempotencia en el cliente o servidor. Una repetición después de una respuesta perdida podría crear más de una alerta; hay que reproducir y definir el comportamiento esperado antes de proponer una solución.
- `database/schema.sql` define claves foráneas de historial a alerta/usuario, pero debe verificarse la instalación real de la base de pruebas antes de depender de ellas.

## Regla de seguridad de pruebas

1. No ejecutar pruebas de concurrencia, repetición ni rollback contra `simple_db` compartida.
2. Crear un esquema aislado, por ejemplo `simple_stage4_test`, con usuarios y alertas sintéticos. No copiar datos personales sin necesidad.
3. Revisar cada comando SQL del esquema antes de ejecutarlo. `database/schema.sql` selecciona `simple_db`; no ejecutarlo tal cual para esta fase.
4. Antes de arrancar el backend, confirmar que `SPRING_DATASOURCE_URL` apunta al esquema aislado y consultar `SELECT DATABASE()` con las mismas credenciales.
5. Guardar respaldo verificable del esquema de pruebas, IDs de alertas objetivo y de control, conteos iniciales y logs sin tokens/contraseñas.
6. Si cambia cualquier fila fuera del conjunto objetivo o el resultado es ambiguo, detener escrituras, conservar el esquema y los logs, comparar IDs/conteos y no repetir la operación hasta explicar la diferencia.
7. No modificar código de producción hasta reproducir el riesgo y registrar una política de negocio aprobada.

## Fases de trabajo

### Fase 4.1 Preparación del entorno aislado

- Revisar el respaldo vigente sin restaurarlo sobre `simple_db`.
- Crear `simple_stage4_test` con un esquema controlado y datos sintéticos: dos alertas objetivo, varias alertas de control, un usuario `USER` y una cuenta `INSTITUTION`.
- Verificar claves foráneas, índices, estado inicial y conexión efectiva del backend a la base de pruebas.
- Registrar el commit base, versión Java/Gradle, fecha, URL de base sin credenciales y resultado de `SELECT DATABASE()`.

### Fase 4.2 Repetición de creación y reintentos

- Enviar una creación válida una vez en la base de pruebas y registrar ID/estado/historial.
- Simular pérdida de respuesta desde el cliente y repetir la misma solicitud.
- Medir cuántas alertas e historiales se crean y documentar la respuesta HTTP de cada intento.
- Antes de implementar idempotencia, acordar si una repetición debe devolver la alerta original, responder conflicto u otra respuesta definida.

### Fase 4.3 Transiciones simultáneas de la misma alerta

- Con una alerta sintética `PENDING`, iniciar en paralelo aprobación y rechazo con la cuenta institucional.
- Repetir con dos solicitudes simultáneas de aprobación sobre la misma alerta.
- Verificar que el estado final corresponda a una sola transición aceptada y que el historial no tenga eventos duplicados o incompatibles.
- Registrar códigos HTTP, hora/orden de solicitudes, estado final y filas exactas de historial.

### Fase 4.4 Aislamiento entre alertas e historial

- Capturar antes/después IDs, estados y conteos de alertas e historial para objetivos y controles.
- Ejecutar operaciones secuenciales, repetidas y concurrentes solo sobre IDs sintéticos elegidos.
- Confirmar que las alertas de control y sus historiales no cambien.
- Confirmar la correspondencia de cada evento con `alert_id`, actor, estado anterior/nuevo, comentario y fecha.

### Fase 4.5 Atomicidad del cambio y su historial

- Diseñar una falla controlada exclusivamente en el esquema de pruebas para comprobar que estado e historial se confirman o revierten juntos.
- Verificar el resultado por consulta SQL antes/después.
- Evitar inyectar fallas en MySQL compartido o interrumpir su servicio.

## Evidencia requerida por caso

Registrar ID, versión/commit, base efectiva, rol, precondición, payload sin secretos, secuencia/tiempo, respuesta HTTP, estado de alerta, filas de historial, alertas de control antes/después y resultado (aprobado/fallido/bloqueado). Adjuntar logs y consultas SQL sin credenciales ni JWT.

## Criterios de cierre

- Ninguna prueba modificó `simple_db` compartida.
- Las repeticiones tienen un comportamiento observado y una política de negocio definida.
- Las transiciones concurrentes no producen dos cambios incompatibles ni eventos de historial duplicados.
- Las operaciones solo cambian las alertas objetivo y el historial correspondiente.
- El cambio de estado y su evento de historial son atómicos.
- Cualquier brecha reproducida tiene evidencia reproducible y una propuesta revisable antes de modificar producción.

## Alcance excluido

Esta fase no implementa notificaciones, mapas/GPS, modo offline ni integraciones externas. Esas capacidades se priorizarán después de completar y documentar la integridad del flujo actual.
