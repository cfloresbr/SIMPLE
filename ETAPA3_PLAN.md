# SIMPLE Etapa 3 Plan de validación de entradas y errores

## Estado

Rama `etapa3`, creada desde `98b3fff` (`corrección etapa 2`). Este plan fija el alcance inicial; no incluye todavía cambios al código de Android o backend.

## Objetivo

Completar la validación de coordenadas y comprobar que la aplicación responde de forma clara y recuperable cuando la API rechaza una solicitud, el backend no está disponible o la conexión falla. El alcance prioriza brechas P1 del Informe Maestro SIMPLE de 20 de octubre de 2026.

## Fase 3.1 Contrato de coordenadas

Comportamiento actual observado en `AlertController.kt`: se permiten coordenadas omitidas en conjunto, se rechaza una coordenada parcial y se validan los rangos de latitud y longitud.

Casos por verificar:

- `latitude=null` y `longitude=null`: aceptar y crear la alerta sin ubicación.
- Solo una coordenada informada: responder `400` con mensaje entendible y no guardar la alerta.
- Latitud en los límites `-90` y `90`, longitud en `-180` y `180`: aceptar.
- Valores fuera de esos límites: responder `400` y no guardar la alerta.
- Confirmar la respuesta HTTP y el estado persistido en MySQL para los casos aceptados y rechazados.

Criterio de cierre: cada caso tiene resultado esperado/real, respuesta HTTP y evidencia de persistencia. Las solicitudes inválidas no generan alertas.

## Fase 3.2 Errores de API y conectividad en Android

Comprobar, al menos, estos escenarios:

- Backend detenido o inaccesible.
- Tiempo de espera agotado.
- Respuestas `400`, `401`, `403` y `5xx`.
- Falla de una consulta durante el refresco del panel mientras otra consulta sí responde.
- Reintento después de recuperar el backend o corregir la solicitud.

Criterio de cierre: la app no se cierra; muestra un mensaje claro en español, termina el indicador de carga, conserva un estado visible coherente y permite reintentar. Una solicitud exitosa no debe borrar por accidente un error relevante de otra solicitud del panel.

## Fase 3.3 Regresión y evidencia

- Compilar backend y Android después de los cambios.
- Repetir en Android los casos de envío sin ubicación, ubicación válida y rechazo de ubicación parcial/fuera de rango, cuando la interfaz permita introducir coordenadas.
- Probar disponibilidad de backend, error recuperable y reintento con los roles `USER` e `INSTITUTION`.
- Registrar dispositivo/emulador, versión, rol, pasos, respuesta o mensaje observado y resultado.
- Usar alertas de prueba identificables y registrar conteos antes/después si alguna prueba modifica MySQL.

## Criterios de aceptación de etapa

1. El contrato de ubicación queda explícito y cubierto por casos válidos e inválidos.
2. Las solicitudes inválidas no persisten datos.
3. Los errores de API y conectividad se comunican claramente sin cierre inesperado.
4. La aplicación vuelve a operar después de reintentar con el backend disponible.
5. Backend y Android compilan; el informe de checkpoint conserva comandos, resultados, pendientes y SHA de la rama antes de publicarla.

## Límites

- Esta etapa no incorpora GPS, mapas, notificaciones, operación offline ni integración con servicios externos.
- No se cambiará la versión de Gradle/AndroidX sin reproducir un problema que lo requiera.
- La validación manual con MySQL debe preservar el respaldo vigente y usar datos reconocibles como prueba.

## Revisión de cobertura antes de implementar

- La rama base contiene tres suites backend: `AlertApiTest`, `AlertTransitionMatrixTest` y `JwtSecurityTest`. No aparecen suites Android bajo rutas `src/test`.
- `AlertApiTest` cubre permisos, aprobación y rechazo de transiciones, pero no contiene casos para coordenadas. Por eso la fase 3.1 agrega cobertura nueva.
- `AlertController.kt` permite ambas coordenadas nulas, rechaza coordenadas parciales con `400` y acepta los extremos inclusivos `-90/90` para latitud y `-180/180` para longitud.
- `SimpleViewModel` captura fallos con `runCatching` y presenta `Throwable.message` como error; `refreshDashboard()` dispara en paralelo la carga de alertas y resumen. Estos comportamientos se deben probar para evaluar claridad del mensaje, estado de carga y errores concurrentes; no se presuponen defectos hasta observar resultados.

## Checkpoint inicial

- Rama base publicada: `etapa2`, commit `98b3fff2010e529a46169238f14ded5089ca5c64`.
- Rama de trabajo: `etapa3`.
- Código de producción aún sin cambios en esta etapa.
- Siguiente paso: revisar los tests existentes de backend/Android y preparar la matriz de casos de la fase 3.1 antes de implementar.
