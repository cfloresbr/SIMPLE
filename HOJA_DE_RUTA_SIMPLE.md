# SIMPLE — Hoja de ruta del proyecto

## Propósito y regla de seguimiento

Este documento conserva el orden de trabajo acordado para SIMPLE y permite retomar el desarrollo sin repetir validaciones ya cerradas ni atribuir al proyecto funciones que todavía no se han implementado o probado.

Antes de comenzar cada etapa:

1. Revisar rama, commit base y archivos modificados.
2. Leer el checkpoint de la etapa anterior y esta hoja de ruta.
3. Comparar el alcance con el código, API, base de datos y pruebas actuales. Marcar cada requisito como **existente y validado**, **existente sin validar**, **parcial** o **pendiente**.
4. Continuar desde la primera brecha pendiente. No volver a implementar ni repetir pruebas aprobadas salvo que un cambio las afecte.
5. Registrar alcance, decisiones, pruebas, evidencia, fallos, correcciones y pendientes en el checkpoint de la etapa.
6. Si aparece una modificación inesperada de datos, archivos dañados o resultados fuera del alcance, detener las escrituras, preservar evidencia y retomar desde un estado verificable.

Las etapas 1 a 3 están cerradas según sus documentos de checkpoint. La secuencia posterior ordena los objetivos que la persona usuaria definió en el chat compartido del 8 de octubre de 2026. Es una planificación del proyecto; cada función futura requiere análisis de brechas, criterios de aceptación y validación antes de declararse terminada.

## Registro de etapas

| Etapa | Alcance | Estado |
|---|---|---|
| 1. Reglas de backend y trazabilidad | Estados de alerta, auditoría, permisos por rol, persistencia y resumen institucional. | Cerrada y publicada; ver `ETAPA1_BACKEND.md`. |
| 2. Seguridad y flujo de estados | JWT/permisos, matriz de transiciones, coherencia del resumen con MySQL y validación de interfaz por rol. | Cerrada y publicada; ver `ETAPA2_PLAN.md`. |
| 3. Entradas, errores y recuperación | Validación de coordenadas, errores de API, desconexión/reintento y regresión backend/Android. | Cerrada y publicada; ver `ETAPA3_CHECKPOINT.md` y `ETAPA3_PLAN.md`. |
| 4. Flujo principal completo y brechas de producto | Corroborar lo existente y cerrar brechas del flujo de alertas por tipo y rol; para `USER`, mostrar primero información preventiva, luego Alertas y Configuración. Para institución, conservar gestión, Reportes y Configuración. | Fases 4.1–4.6 cerradas y validadas localmente el 17-11-2026. Sin publicación oficial ni despacho externo. Ver `ETAPA4_CHECKPOINT.md`, `CRITERIOS_NIVELES_ALERTA.md` y `ARQUITECTURA_ALERTAMIENTO_TEMPRANO.md`. |
| 5. Reportes, ubicación y datos de análisis | Consulta institucional de reportes en la app; ubicación, duración, nivel y actor institucional en alertas; consistencia de los datos relacionados. | En curso. Auditoría 5.1: backend conserva metadatos de actor y ciclo de vida, pero la API/UI no los presentan; ubicación del cliente siempre nula. Primer corte: exponer tiempos y actor sin datos personales adicionales. Captura GPS optativa queda en subfase separada. Ver `ETAPA5_CHECKPOINT.md`. |
| 6. Notificaciones y preferencias | Avisos internos de recepción/revisión separados de alertas públicas validadas; entrega, acuse, reintentos, expiración, recuperación y preferencias. | Pendiente; no confundir registro local con entrega a Bomberos ni con alerta pública oficial. Requiere canal y destinatario acordados. |
| 7. Cuenta y formularios | Edición de correo/contraseña y estandarización de campos, controles de contraseña y selectores. | Pendiente. |
| 8. Reportes y derivación temprana de incendios | Reporte ciudadano forestal/estructural, entrega temprana al operador/destinatario competente, correlación sin fusionar reportes y publicación pública solo validada. | Futuro; no esperar un conteo fijo para mostrar el reporte al operador. Definir convenio, destinatario, geografía, acuse, guardia y canal de respaldo antes del despacho real. |
| 9. Terremoto con sensores | Detección temprana distribuida con acelerómetros, clasificación local, correlación espacio-temporal en servidor y estimación de evento. | Futuro; modo sombra y comparación con CSN antes de alertar públicamente. Umbrales deben calibrarse con teléfonos/densidad y datos chilenos, no copiarse de Sismo Detector/MyShake. |
| 10. Alertas costeras oficiales | Marejadas basadas en DIRECTEMAR; tsunami según SHOA/SNAM y protocolos de SENAPRED. Periféricos se evalúan solo con especificación y fuente de señal. | Futuro; autoridades y escalas separadas, referencia oficial, cobertura, vigencia, actualización y cancelación. SIMPLE no sustituye SAE/SNAM. |
| 11. Nuevos tipos de alerta e identidad | Temporales, cortes, vías u otras alertas; evaluar utilidad de RUT y fuentes autorizadas. | Versión posterior; referencias y análisis legal/técnico pendientes. |
| 12. Operación institucional e interoperabilidad analítica | Evolucionar desde moderación interna hacia posible uso por entidades oficiales y análisis por organismos de estudio. | Futuro; definir organizaciones/roles, finalidad, permisos, minimización y desidentificación, exportación, retención, trazabilidad y revisión normativa antes de compartir datos. No implementar durante la etapa 4. |

La arquitectura objetivo, límites de cada rol, flujo de validación y salvaguardas están definidos en `ARQUITECTURA_ALERTAMIENTO_TEMPRANO.md`.

## Etapa 4 — Flujo principal completo y brechas de producto

### Objetivo

Consolidar una aplicación utilizable según los roles y tipos de alerta definidos, partiendo de las funciones actuales. Esta etapa empieza con una revisión de brechas para evitar repetir funciones existentes. Solo se implementan las brechas confirmadas.

### Flujo funcional acordado

- **Incendio:** el usuario público envía un reporte no verificado; el moderador lo revisa/deriva. La integración de despacho y publicación pública validada quedan en etapas 6/8.
- **Marejadas:** el rol institucional actual solo gestiona el flujo interno. La publicación oficial requiere aviso DIRECTEMAR y permiso de una autoridad; ver etapa 10.
- **Terremoto:** la activación automática basada en sensores queda para la etapa 9. No simular ni anunciar detección automática antes de implementarla y validarla.
- **Otros eventos:** quedan fuera del alcance inicial hasta recibir el documento de referencia.
- **Navegación:** organizar el acceso por rol en Inicio (crear/gestionar/ver alertas), Reportes y Configuración. La visibilidad de acciones y datos debe respetar permisos del backend.

### Fases

1. **4.1 Revisión de brechas:** inspeccionar pantallas Android, endpoints, reglas de rol, tablas y pruebas. Registrar qué partes del flujo anterior ya existen y qué falta. Revisar la rama y cambios locales antes de tocar código.
2. **4.2 Flujo por tipo y rol:** verificar y completar, si hace falta, envío de incendio por `USER`, aprobación/rechazo por `INSTITUTION`, y creación institucional de marejadas. Definir casos para estados, errores y auditoría. No duplicar permisos ya cubiertos.
3. **4.3 Navegación por rol:** verificar que Inicio, Reportes y Configuración sean accesibles de forma clara y que cada rol vea solo acciones autorizadas. La revisión de pantallas institucionales existentes se conserva como evidencia; se amplía solo donde haya brechas.
4. **4.4 Inicio educativo para `USER` — validada:** presentar información preventiva basada en fuentes institucionales al entrar; dejar Alertas como segunda sección y Configuración como tercera. La sección educativa debe citar/abrir las fuentes y distinguir recomendaciones de alertas oficiales. La compilación `assembleDebug` fue exitosa, el APK se instaló y la persona usuaria confirmó la revisión visual de ambos roles.
5. **4.5 Persistencia y regresión — cerrada:** 54 pruebas backend aprobadas, incluida persistencia real de alerta e historial contra H2 aislado. Se toman como evidencia complementaria las validaciones MySQL, conteos, auditoría y actualización visual ya registradas en etapas 1 y 2; no se repitieron escrituras en `simple_db`.
6. **4.6 Clasificación y separación reporte/aviso — cerrada el 17-11-2026:** incendios forestales/estructurales se distinguen; nadie asigna nivel oficial al crear. Registros nuevos `SIN_CLASIFICAR` y `PENDING`; `ACTIVE` solo flujo interno. `USER` consulta reportes propios e historial propio; `INSTITUTION`/`ADMIN` mantienen bandeja global interna. El feed anónimo `/api/alerts/public` declara `NOT_CONNECTED` sin exponer reportes. Android separa avisos públicos y reportes propios, con acuse de recepción por SIMPLE. Suite backend y build Android exitosos; APK instalado y revisión manual de los tres accesos confirmada. Sin publicación, notificación externa, alcance por agencia ni migración MySQL.

### Criterios de cierre

- Matriz de requisitos por rol/tipo con estado antes/después y casos de prueba reproducibles.
- La población puede consultar avisos públicos vigentes sin crear cuenta; `USER` ve además sus propios reportes. El moderador recibe la bandeja interna; futuras agencias reciben solo reportes por competencia/área aplicada en backend.
- La recepción por SIMPLE, la derivación/acuse de un organismo y la publicación a la población son estados y mensajes distintos; nunca confirmar un paso externo sin acuse.
- `USER` solo puede consultar sus propios reportes e historiales; el rol de moderación no se interpreta como agencia oficial. La publicación requiere una fuente/permiso explícitos.
- El flujo de incendio individual funciona de extremo a extremo y la institución puede aprobar/rechazar.
- La marejada solo puede activarse con el rol institucional.
- Menú y vistas respetan permisos; Reportes y Configuración tienen alcance y estado documentados, aunque una función se programe en una etapa posterior.
- El rol `USER` aterriza en contenido preventivo con fuente institucional; puede abrir alertas como segunda sección y configuración como tercera. El contenido no se presenta como alertamiento oficial.
- Persistencia e historial concuerdan para los casos validados; las pruebas no alteran alertas ajenas a las elegidas.
- Compilaciones y pruebas aplicables registradas; checkpoint actualizado antes del commit/push.

## Etapa 5 — Reportes, ubicación y datos de análisis

1. **5.1 Auditoría de campos y exposición — completada:** `alerts` persiste creador/último actor, `activatedAt`, `resolvedAt`, `expiresAt`, nivel y coordenadas. El DTO actual solo expone tipo, nivel, mensaje, estado, fecha de creación y coordenadas. Android siempre envía coordenadas `null`. `ReportsScreen` ya lista reportes y agrega resumen institucional; el resumen es global para `INSTITUTION`/`ADMIN`.
2. **5.2 Ciclo de vida y actor — en desarrollo:** exponer campos existentes, presentar el ID de cuenta responsable a moderadores sin correo, nombre ni ubicación personal adicional. Mostrar activación/resolución/vencimiento solo cuando estén registrados; no insinuar expiración automática.
3. **5.3 Ubicación reportada por la persona — pendiente:** diseñar captura en primer plano, optativa y solicitada solo al enviar un reporte; explicar el uso, permitir continuar sin ubicación y mostrar el resultado antes del envío. Validar permiso denegado, proveedor apagado y ubicación desactualizada. No integrar seguimiento en segundo plano.
4. **5.4 Coherencia y privacidad — pendiente:** comprobar visibilidad por rol, fechas tras los cambios de estado y consistencia de conteos; usar H2/datos sintéticos, no escrituras de prueba en `simple_db`.

## Requisitos transversales de seguridad de datos

- No ejecutar concurrencia, repetición ni fallos inducidos contra la base compartida `simple_db`; usar una base aislada con datos sintéticos.
- Antes de cualquier prueba que escriba, identificar respaldo, base efectiva, IDs objetivo y conteos iniciales. No restaurar ni ejecutar scripts que seleccionen `simple_db` sin revisar el destino.
- Comparar IDs, estados, historial y conteos al terminar. Si aparece una diferencia no explicada, detener escrituras y registrar el incidente antes de repetir.
- La atomicidad, idempotencia, outbox/reintentos y correlación entre reportes se abordan antes de integrar entrega crítica externa; no introducir microservicios sin necesidad medida. Los reportes se vinculan a eventos candidatos, nunca se fusionan destructivamente.
- No guardar claves, tokens, datos personales innecesarios ni credenciales en Git o en checkpoints.
- No habilitar analítica para terceros ni exportaciones con datos personales hasta definir finalidad, permisos, minimización/desidentificación, retención, trazabilidad y revisión de requisitos aplicables en la etapa 12.

## Exclusiones explícitas hasta sus etapas

Notificaciones push completas, detección sísmica por sensores, radio/periféricos, umbral de múltiples reportantes, nuevas alertas e integración de RUT no se considerarán implementadas por aparecer aquí. Cada una requiere su propio diseño, evaluación de factibilidad, protección de datos y validación.

## Fuentes de seguimiento

- Requisitos funcionales: chat compartido de SIMPLE, actualizado el 8 de octubre de 2026: `https://chatgpt.com/share/6ae7f716-d46c-83e8-b016-f96b1e11e63d`.
- Criterios propuestos de niveles y fuentes oficiales: `CRITERIOS_NIVELES_ALERTA.md`.
- Evidencia de etapas completadas: `ETAPA1_BACKEND.md`, `ETAPA2_PLAN.md`, `ETAPA3_PLAN.md` y `ETAPA3_CHECKPOINT.md`.
- El Informe Maestro del 20 de octubre de 2026 sirve como fuente complementaria para criterios de pruebas y trazabilidad; no reemplaza los requisitos funcionales priorizados por el usuario.

## Actualización obligatoria por etapa

Al cerrar una etapa, actualizar la tabla de estado y el checkpoint de esa etapa con rama/commit, requisitos cubiertos, pruebas ejecutadas, resultados, evidencia, cambios realizados y asuntos diferidos. Al iniciar la siguiente, actualizar primero el checkpoint de inicio con el resultado de la revisión de brechas.
