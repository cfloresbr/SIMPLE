# SIMPLE Etapa 2 Plan de validación y robustez

## Estado del documento

Plan de validación de la rama `etapa2`, creada desde `10e25e1` (`final etapa 1`) y publicada en GitHub. Las fases 2.1, 2.2 y 2.3 están validadas; la fase 2.4 fue revisada manualmente para `USER` e `INSTITUTION`. El alcance se basa en el plan de pruebas del Informe Maestro SIMPLE con fecha 20 de octubre de 2026.

## Objetivo

Completar la matriz reproducible de autenticación, autorización, transiciones de alertas y coherencia de reportes entre API, MySQL y Android. La etapa prioriza reglas de negocio y seguridad antes de incorporar funcionalidades nuevas.

## Fases propuestas

### Fase 2.1 Autenticación JWT y permisos

- Probar solicitudes sin token, con token malformado, expirado y válido.
- Verificar `401` cuando no existe autenticación válida.
- Verificar `403` cuando una cuenta autenticada `USER` intenta usar resumen o acciones institucionales.
- Verificar accesos permitidos para `INSTITUTION` y `ADMIN` donde corresponda.
- Usar tokens reales de la aplicación en las pruebas de integración; no depender solo de usuarios simulados en el contexto de Spring Security.

### Fase 2.2 Matriz de transiciones de alertas

- Documentar y probar transiciones válidas e inválidas para `PENDING`, `VALIDATED`, `ACTIVE`, `REJECTED`, `RESOLVED` y `EXPIRED`.
- Verificar aprobación, validación, activación, rechazo y resolución con roles permitidos y no permitidos.
- Comprobar código HTTP, estado final, actor, comentario e historial para cada cambio.
- Confirmar que una transición rechazada no modifica la alerta ni agrega historial.

### Fase 2.3 Resumen y coherencia con MySQL

- Comparar `GET /api/reports/summary` con consultas SQL que usen el mismo conjunto de alertas.
- Confirmar conteos de cada estado después de crear y gestionar alertas de prueba.
- Repetir la consulta del resumen en la app institucional y contrastar los totales.
- Conservar el respaldo antes de cualquier ejecución que pueda actualizar el esquema; no registrar contraseñas ni tokens en el repositorio o informe.

### Fase 2.4 Validación Android de regresión

- Confirmar que las acciones disponibles cambian según rol y estado.
- Comprobar refresco de listas y resumen después de cada operación.
- Revisar respuestas 4xx/5xx y backend inaccesible para asegurar mensajes claros y que la aplicación no se cierre.
- Registrar dispositivo/emulador, versión, cuenta/rol y resultado observado.

## Criterios de aceptación

1. Existe una matriz automatizada de casos con rol, token, endpoint, entrada, resultado HTTP, estado esperado e historial esperado.
2. Los casos críticos de autenticación, permisos y transiciones cuentan con pruebas automatizadas reproducibles.
3. La respuesta de resumen coincide con la base MySQL y con el panel Android para el mismo conjunto de datos.
4. Las pruebas manuales de MySQL se ejecutan con datos identificados como pruebas y con respaldo previo.
5. Backend y Android compilan; se conservan los comandos, resultados, fallos y correcciones.
6. El informe de checkpoint registra rama, commit base, casos ejecutados, evidencia, pendientes y SHA final antes del push.

## Fuera de alcance de esta etapa

- Notificaciones push reales.
- Mapas y geolocalización GPS en funcionamiento completo.
- Operación offline y sincronización posterior.
- Integraciones con SENAPRED, SHOA, CONAF, municipios, sensores o redes radiales.
- Certificación de seguridad o preparación para producción.

Estas funcionalidades requieren etapas y criterios propios; su existencia no debe inferirse del trabajo de validación descrito aquí.

## Checkpoint de inicio

- Etapa 1 publicada: commit `10e25e1` (`final etapa 1`) en `origin/etapa1`.
- La rama `etapa2` parte de ese commit.
- El árbol de trabajo estaba limpio al crear la rama.
- La rama fue publicada con el commit `699e605bcbd37baa30c0ea267337e35857ad2101` (`etapa 2`).
- Los cambios de etapa 2 hasta ahora son el plan y suites de pruebas; no se modificó código de producción.

## Seguimiento de fase 2.1

- Estado: validada en la suite automatizada.
- Comando ejecutado desde `backend`: `.\gradlew.bat test --no-daemon --console=plain`.
- Resultado vigente de la suite completa: `BUILD SUCCESSFUL`; 42 pruebas, 0 fallos, 0 errores y 0 omitidas (4 en `AlertApiTest`, 30 en `AlertTransitionMatrixTest`, 8 en `JwtSecurityTest`).
- Casos JWT: token ausente, malformado y expirado devuelven `401`; token válido de cuenta eliminada devuelve `401`; `USER` puede listar alertas y recibe `403` para resumen y aprobación; `INSTITUTION` y `ADMIN` acceden al resumen.
- Las solicitudes atraviesan el filtro JWT real y usan tokens firmados por `JwtService`; los repositorios se simulan. La suite no valida conexión/persistencia MySQL ni prueba la acción de aprobación con `ADMIN`, que pertenece a la fase 2.2.
- No se requirió cambiar el código de producción.

## Seguimiento de fase 2.2

- Estado: matriz automatizada validada en la suite backend.
- Se cubrieron 7 transiciones permitidas: `PENDING → VALIDATED`, `PENDING/VALIDATED → ACTIVE` por aprobación, `VALIDATED → ACTIVE` por activación, `PENDING/VALIDATED → REJECTED` y `ACTIVE → RESOLVED`.
- Se cubrieron 23 combinaciones inválidas entre las cinco acciones y todos los estados existentes. Cada una devuelve `409` y no guarda la alerta ni un evento de historial.
- En las transiciones permitidas se verifican la respuesta/estado final, marcas de actualización/activación/resolución, actor, estado anterior/nuevo y comentario de auditoría.
- Ejecución completa posterior a estos cambios: `BUILD SUCCESSFUL`, 42 pruebas, 0 fallos, 0 errores y 0 omitidas (4 `AlertApiTest`, 30 `AlertTransitionMatrixTest`, 8 `JwtSecurityTest`).
- Esta matriz prueba controladores con repositorios simulados. La persistencia real aprobada para la transición de aprobación está documentada en el cierre de etapa 1; la matriz exhaustiva aún no recorre cada combinación contra MySQL.

## Seguimiento de fase 2.3

- Estado: conteos SQL, endpoint de resumen y panel institucional confirmados por la persona usuaria como coincidentes.
- Consulta: `SELECT COUNT(*)` y sumas por cada estado de `alerts`.
- Conteo inicial MySQL: total 9; `PENDING` 1; `VALIDATED` 0; `ACTIVE` 4; `REJECTED` 1; `RESOLVED` 3; `EXPIRED` 0.
- El conteo inicial coincide con el fixture de resumen probado en `JwtSecurityTest` (9; 1; 0; 4; 1; 3; 0).
- La persona usuaria confirmó que los conteos mostrados son correctos. No se modificaron datos durante la consulta SQL.
- La prueba HTTP automatizada del resumen usa repositorios simulados; la confirmación manual del endpoint y la pantalla cubre la comparación con el entorno local.

## Seguimiento de fase 2.4

- Estado: validada manualmente para los roles `USER` e `INSTITUTION`.
- Revisada la lógica de `MainActivity.kt`: las acciones solo se muestran a `INSTITUTION` y `ADMIN`; `PENDING` muestra aprobar/rechazar, `VALIDATED` activar/rechazar, `ACTIVE` marcar como resuelta y los estados finales no tienen acciones de gestión.
- El botón `Actualizar` llama al refresco del panel; el resumen se presenta para cuentas institucionales/administrativas.
- La persona usuaria revisó la vista con ambos roles y confirmó que se comporta correctamente.
- La persona usuaria ejecutó en Android una transición de una alerta `ACTIVE` a `RESOLVED` y confirmó que, al pulsar `Actualizar`, la pantalla quedó correcta.
- Conteo SQL posterior a esa operación: total 9; `PENDING` 1; `VALIDATED` 0; `ACTIVE` 3; `REJECTED` 1; `RESOLVED` 4; `EXPIRED` 0. Se verificó con consulta agregada de solo lectura.
- `VALIDATED` y `EXPIRED` no estaban presentes en los datos observados; la visibilidad de sus acciones se revisó en el código y en pruebas automatizadas de la matriz de transiciones.
- No se registró una prueba manual del rol `ADMIN`.

## Cierre de etapa 2

- Fases 2.1, 2.2 y 2.3 validadas; fase 2.4 validada manualmente para `USER` e `INSTITUTION`.
- Backend: `BUILD SUCCESSFUL`, 42 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Android: revisión manual de roles, transición `ACTIVE → RESOLVED` y actualización posterior confirmadas por la persona usuaria.
- Estado Git: rama `etapa2` publicada en GitHub. Commit de cierre `699e605bcbd37baa30c0ea267337e35857ad2101` (`etapa 2`), basado en `10e25e17eb966ef01ecd8c786dc696de9b74164d`.

