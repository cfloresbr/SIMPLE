# SIMPLE — Checkpoint etapa 3.1

## Fecha

2026-11-06

## Rama y commit

- Rama: `etapa3`
- Commit verificado: `40ffbaf138108505e2307a8f9f664e2682763f17`
- Commit anterior de referencia: `98b3fff2010e529a46169238f14ded5089ca5c64`

## Alcance realizado

Se completó la fase 3.1 de validación de coordenadas y respuestas de error de la API.

Cambios incluidos:

- Se agregaron pruebas para solicitudes sin coordenadas.
- Se agregaron pruebas para coordenadas válidas en los límites:
  - Latitud: `-90` y `90`.
  - Longitud: `-180` y `180`.
- Se agregaron pruebas para coordenadas parciales.
- Se agregaron pruebas para coordenadas fuera de rango.
- Se verificó que las solicitudes inválidas no guarden alertas ni historial.
Se agregó ApiExceptionHandler para devolver errores con un campo message en la respuesta JSON.

Pruebas ejecutadas

Comando utilizado desde backend:


.\gradlew.bat test


Resultado:


BUILD SUCCESSFUL in 17s


5 actionable tasks: 1 executed, 4 up-to-date


La suite completa del backend terminó correctamente.

## Cobertura verificada

- Alertas sin ubicación: correcto.
- Coordenadas en límites válidos: correcto.
- Latitud sin longitud: responde `400` y no persiste datos.
- Longitud sin latitud: responde `400` y no persiste datos.
- Coordenadas fuera de rango: responde `400` y no persiste datos.
- Mensajes de error: disponibles en el campo JSON `message`.

## Archivos modificados

- `backend/src/main/kotlin/cl/simple/api/ApiExceptionHandler.kt`
- `backend/src/test/kotlin/cl/simple/AlertApiTest.kt`

## Pendientes

- Ejecutar y documentar la compilación de Android.
- Probar desde Android Studio los mensajes de error.
- Probar backend detenido o inaccesible.
- Probar respuestas `400`, `401`, `403` y `5xx`.
- Verificar reintento después de recuperar el backend.
- Completar la fase 3.2 y la regresión de la fase 3.3.
- Confirmar manualmente la persistencia en MySQL cuando corresponda.

## Observaciones

Los avisos de Mockito, Java y Gradle no provocaron fallos en la suite.

La validación de esta fase fue realizada con pruebas de backend y Mockito. La prueba manual desde Android y la comprobación directa de MySQL quedan pendientes.

## Seguimiento de fase 3.2

### Estado al 11 de noviembre de 2026

Implementación Android iniciada en `etapa3`, sobre `HEAD` `0a18b1a278ff725e37de4277e340a42152ecaf2b`. La fase permanece abierta y no se ha creado commit para estos cambios.

### Cambios locales en desarrollo

- `MainActivity.kt`: separa los errores y estados de carga de alertas, resumen, historial y envío. Una respuesta exitosa ya no borra el error de otra consulta del panel. Los errores de refresco conservan los datos previamente cargados y ofrecen reintento.
- `ApiErrors.kt`: traduce timeouts, falta de conexión y respuestas HTTP comunes a mensajes en español; si el backend entrega `message`, la app puede mostrarlo sin exponer mensajes internos de excepciones desconocidas.
- `Api.kt`: fija límites de conexión, lectura, escritura y duración total de la solicitud.
- El formulario de reporte conserva los datos y permanece abierto cuando falla el envío para que se pueda reintentar.

### Validación y pendientes

- `git diff --check`: sin errores de espacios en el diff.
- No se añadieron ni ejecutaron pruebas en esta iteración.
- En esta sesión, los intentos de compilación quedaron sin salida y se cancelaron. Después, la persona usuaria ejecutó desde `android` `.\gradlew.bat :app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain`: `BUILD SUCCESSFUL` en 51 s; 37 tareas (11 ejecutadas, 26 al día). El comando probó el código local de etapa 3.2 sobre `HEAD` `0a18b1a278ff725e37de4277e340a42152ecaf2b`. Evidencia informada por la persona usuaria; no equivale a ejecutar las pruebas automatizadas.
- La desconexión y recuperación manual del backend se validó el 14 de noviembre de 2026; los demás casos pendientes de fase 3.2 están detallados en la actualización al final de este documento.
- El log `android/hs_err_pid26640.log` ya estaba presente antes de esta implementación y sigue sin rastrear; no incluirlo en un commit.
- Riesgo diferido: una acción podría afectar alertas o historiales ajenos a la alerta objetivo. No se ha investigado ni modificado; se revisará tras estabilizar el flujo principal. Si una prueba actual muestra cambios fuera del alcance esperado, detener escrituras, preservar IDs/logs/respaldo y registrar el incidente antes de reintentar.

### Actualización del checkpoint — 14 de noviembre de 2026

- La persona usuaria confirmó la prueba manual de backend inaccesible: con SIMPLE abierta y el backend realmente detenido, la aplicación conservó las alertas visibles y presentó un mensaje de conexión fallida con opciones para reintentar la carga de alertas y el resumen.
- Tras iniciar el backend una sola vez, la persona usuaria confirmó que ambos reintentos recuperaron la conexión y que los datos y mensajes quedaron correctos.
- Se identificó una instancia anterior del backend (PID `5028`) que seguía activa durante un primer intento de inicio de sesión sin backend. Un segundo `bootRun` falló por puerto `8080` ocupado; no se considera una prueba de desconexión. Al detener la instancia anterior, la app mostró el estado de desconexión esperado.
- El backend arrancó con MySQL después de reiniciar Windows y limitar la memoria de Gradle. Los intentos previos habían generado un fallo de memoria nativa de Java.
- Durante la prueba de desconexión/recuperación no se usaron acciones para crear, aprobar, rechazar o resolver alertas; no se buscó modificar registros de alertas.
- Resultado: conectividad caída, conservación visual de datos, mensaje recuperable y recuperación mediante reintento: **validado manualmente por la persona usuaria**. No equivale a la validación de timeout, `400`, `401`, `403`, `5xx` o fallo parcial de una consulta.
- `git status` mostró todos los cambios de etapa 3.2 sin preparar en `etapa3`; aún no hay commit para ellos. `git diff --check` no encontró errores de espacios. Git avisó que normalizará LF a CRLF al tocar dos archivos Kotlin.
- Logs de diagnóstico sin seguimiento: `android/hs_err_pid26640.log`, `backend/hs_err_pid24904.log` y `backend/replay_pid24904.log`. Conservarlos para referencia y excluirlos del commit.

### Pendientes vigentes de etapa 3.2

- Las pruebas manuales de errores descritas en la actualización del 15 de noviembre se completaron. La implementación de etapa 3.2 está en el commit `4d11110` de `etapa3`.
- Pendiente vigente: ejecutar y registrar la regresión de etapa 3.3, incluyendo compilación/backend, comprobaciones por rol y evidencia reproducible.

### Actualización del checkpoint — 15 de noviembre de 2026

- `400`: una descripción de 1001 caracteres fue rechazada; la app mostró el mensaje de longitud máxima y mantuvo abierto el formulario. La validación del backend ocurre antes de guardar la alerta.
- `401`: una contraseña incorrecta mostró credenciales inválidas; el inicio de sesión correcto posterior funcionó.
- `403`: el rol `USER` intentó enviar `MAREJADAS`; apareció el error de permiso, el formulario quedó abierto y el contador no cambió. No se creó la alerta.
- `500` y fallo parcial: con `SimpleApiMockServer.java`, fuera del repositorio y sin conexión a MySQL, el endpoint de alertas devolvió una alerta identificada como simulada y el resumen devolvió `500`. La app mantuvo la sección de alertas y mostró el error del servicio para el resumen.
- Timeout: el mock demoró la respuesta de alertas más que el timeout de lectura; la app mostró que la conexión tardó demasiado.
- Recuperación del mock: al volver el mock al modo normal y reintentar, la persona usuaria confirmó que todo funcionó correctamente.
- El mock se compiló con JDK 21. El código fuente auxiliar está en el workspace de Codex, fuera de `SIMPLE`; no se incluye en commits del proyecto.
- No se usaron acciones de gestión sobre la alerta simulada y el mock no consulta ni modifica MySQL.
- Resultado de etapa 3.2: escenarios manuales de desconexión, `400`, `401`, `403`, `500` parcial, timeout y reintento: **validados por la persona usuaria**. La captura/evidencia de dispositivo y versión no se adjuntó.

### Siguiente checkpoint

- Iniciar fase 3.3 de regresión; registrar comandos, resultados, roles y datos de dispositivo/emulador.
- Mantener los registros `android/hs_err_pid26640.log`, `backend/hs_err_pid24904.log` y `backend/replay_pid24904.log` fuera de Git.
