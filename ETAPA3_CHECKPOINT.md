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