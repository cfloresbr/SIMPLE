# SIMPLE — Arquitectura objetivo para alertamiento temprano

## Propósito y estado

Este documento registra el flujo objetivo solicitado para SIMPLE: recibir reportes ciudadanos tempranos, derivarlos al organismo que corresponde y difundir a la población solo alertas con respaldo suficiente y atribución clara.

Estado al 17 de noviembre de 2026: **arquitectura objetivo en diseño; mitigación local de fase 4.6 compilada, probada y revisada manualmente**. El rol `INSTITUTION` actual es un moderador interno. SIMPLE no está conectado a despacho de Bomberos, CONAF, DIRECTEMAR, SHOA/SNAM, SENAPRED, SAE ni a sensores sísmicos. No afirmar al usuario que un reporte fue despachado o que una alerta oficial fue emitida sin confirmación verificable.

## Auditoría del flujo y roles actuales (16 de noviembre de 2026)

Sí faltaba cerrar el significado de “éxito” y el alcance de visibilidad antes de compilar y validar el flujo como completo. La auditoría del código encontró estas diferencias respecto del sistema objetivo:

| Área | Comportamiento actual observado | Implicancia |
|---|---|---|
| Confirmación al reportante | `POST /api/alerts` devuelve el registro guardado. Android cierra el formulario y actualiza las listas, pero no muestra una confirmación de recepción diferenciada. | Un reporte aceptado por la API se puede confundir con una alerta oficial o un aviso recibido por el organismo. |
| Feed público | `GET /api/alerts` devuelve todas las filas para cualquier usuario autenticado; el cliente público representa esa misma lista sin filtrar `PENDING`, `REJECTED`, `RESOLVED` o `EXPIRED`. | Datos de reportes no verificados y estados internos pueden mostrarse a población que no debe recibirlos como alertas públicas. La API también entrega todos los historiales a cualquier sesión autenticada que conozca el ID. |
| Rol institucional | Solo existen `USER`, `INSTITUTION` y `ADMIN`. `INSTITUTION` puede revisar todos los reportes, crear tipos de todos los fenómenos y los registros que crea quedan `ACTIVE` inmediatamente, aunque `INSTITUTION` sea un moderador interno no afiliado a una autoridad. | El estado interno `ACTIVE` no debe interpretarse como “alerta oficial publicada”. El rol genérico no sirve como autorización final de publicación por organismo. |
| Derivación | No se encontró envío a CONAF/Bomberos ni acuse de una entidad receptora. La app construye reportes con latitud y longitud `null`; solo está declarada la permission `POST_NOTIFICATIONS`, sin servicio de entrega FCM visible. | SIMPLE hoy registra y gestiona dentro de la app; todavía no deriva, moviliza instituciones ni garantiza difusión temprana. |
| Alcance institucional | No hay entidad/organismo asociado al usuario, especialidad, territorio asignado ni permisos por amenaza. | En un piloto sectorial, cada equipo debe recibir solo casos de su competencia/área; la coordinación intersectorial requiere un rol/canal distinto. |

### Matriz objetivo por actor

| Actor futuro | Debe recibir/gestionar | No debe ocurrir por defecto |
|---|---|---|
| `USER` / comunidad | Prevención; creación de un reporte; estado propio de recepción/derivación; alertas públicas vigentes para su zona. | Ver reportes ajenos, la cola `PENDING`, rechazos, comentarios internos o información precisa de terceros. |
| Moderador interno SIMPLE | Revisar reportes del entorno de prueba, detectar faltantes y derivarlos por un procedimiento acordado; no emitir declaraciones oficiales. | Ser tratado como CONAF, Bomberos, DIRECTEMAR, SHOA o SENAPRED por tener rol `INSTITUTION`. |
| Operador CONAF | Reportes forestales asignados a sus territorios y cola/acuse dentro del convenio que se acuerde. | Recibir o tener que gestionar por defecto marejadas o toda emergencia nacional. |
| Operador Bomberos | Reportes de incendios estructurales y los forestales que correspondan al despacho/área convenida. | Que SIMPLE marque “despachado” o “controlado” sin acuse/procedimiento de Bomberos. |
| Autoridad marítima / fuente oficial | Avisos oficiales de marejadas con zona, inicio, término y recomendaciones de DIRECTEMAR. | Mezclar marejadas con tsunami, o hacer que el moderador interno cree por sí solo un aviso oficial. |
| SHOA/SNAM y SENAPRED | Integración según sus competencias y protocolos para amenaza de tsunami, coordinación y difusión. | Tratar una denuncia ciudadana de sismo o un registro de marejadas como alerta de tsunami. |
| Administrador SIMPLE | Gestionar cuentas, organizaciones, permisos y auditoría técnica. | Publicar una advertencia pública solo por ser administrador técnico. |

Esta matriz es objetivo de diseño, no significa que estas cuentas o integraciones ya existan. SENAPRED describe SINAPRED como coordinación entre actores públicos, privados y comunitarios, y DIRECTEMAR publica avisos marítimos con zona y periodo; la app debe respetar esas competencias y vigencias, no reemplazarlas. Referencias: [SINAPRED/SENAPRED](https://www.senapred.cl/sinapred/), [aviso de marejadas DIRECTEMAR](https://www.directemar.cl/aviso-de-marejadas-0), [CONAF: aviso inmediato de incendios por 130 y 132](https://www.conaf.cl/por-retorno-de-veraneantes-conaf-llama-a-la-prevencion-de-incendios-forestales/).

### Confirmaciones correctas por etapa

1. Después de persistir un reporte ciudadano: **“Reporte recibido por SIMPLE; pendiente de revisión.”** Indicar el tipo y el identificador/estado cuando exista. Para incendio activo, añadir el canal telefónico oficial pertinente; la recomendación de llamada no espera a que SIMPLE responda.
2. Mostrar **“Enviado/recibido por [organismo]”** solo si el canal está integrado y su estado de entrega registra el acuse correspondiente. Si está en cola, decir “Envío pendiente”; si falla, dar el fallback telefónico.
3. Decir **“Alerta pública publicada”** solo después de autorización, fuente/alcance/validez registrados y resultado del canal de difusión. No usar una confirmación genérica “alerta creada con éxito” para los tres casos.

La auditoría reveló que el estado interno `ACTIVE` no puede definir por sí solo lo que se publica. La separación se implementa primero en backend; ocultar controles solo en Android no es una barrera de acceso.

### Mitigación local implementada en código (17 de noviembre)

- `GET /api/alerts`: `USER` recibe solo filas cuyo `created_by` coincide con su ID; `INSTITUTION`/`ADMIN` conservan la bandeja interna completa. `GET /api/alerts/{id}/history` oculta con 404 el historial de otro usuario.
- `GET /api/alerts/public`: lectura sin iniciar sesión; devuelve un feed explícito `NOT_CONNECTED` y vacío hasta conectar una fuente/publicador autorizado. No convierte los estados internos `ACTIVE` en avisos públicos. Android expone esta consulta desde la pantalla de acceso, sin pedir una cuenta.
- Toda nueva solicitud de `USER` o `INSTITUTION`/`ADMIN` comienza `PENDING`; `ACTIVE` queda como transición del flujo interno, no publicación pública. No se reescriben filas históricas.
- Android separa “Avisos públicos” de “Mis reportes” y muestra acuse con folio solo después de guardar en SIMPLE. El acuse aclara que el reporte de incendio no se envió a organismos externos. El moderador recibe “Registro interno”; no “alerta publicada”.
- No se añadió tabla de aviso público ni función para publicarlo: esa publicación espera organización, permisos, fuente, cobertura y vigencia acordadas. No se inició backend ni se ejecutó migración/escritura en MySQL.

Límite restante: el único rol `INSTITUTION` aún puede consultar toda la bandeja interna. No representa a CONAF/Bomberos ni permite aislamiento por organismo o territorio. No habilitar cuentas de agencias reales hasta agregar organización/área/capacidades y aplicar sus permisos en backend.

## Decisión central: separar reporte, evento y alerta pública

No tratar como el mismo registro las observaciones de usuarios, un evento corroborado y una alerta pública. En la evolución de datos, mantener tres objetos relacionados pero independientes:

1. **Reporte ciudadano:** evidencia original e inmutable de una persona, con ID, hora, ubicación, subtipo, descripción y estado de entrega/revisión.
2. **Evento candidato:** agrupación revisable de reportes posiblemente relacionados. Cada reporte conserva su ID y se vincula al candidato; agruparlos nunca debe sobrescribir reportes ni modificar otros eventos.
3. **Alerta pública:** instrucción difundida a una zona por un operador competente o por una integración oficial autorizada. Tiene fuente, cobertura, vigencia, texto/instrucciones y auditoría propia.

Los estados de flujo, la prioridad interna y la clasificación de una autoridad son campos separados. Los reportes no verificados no aparecen como alerta oficial. Cambios de estado son auditables e idempotentes; correcciones/cancelaciones se registran como eventos nuevos, no borrando evidencia previa.

### Vigencia sísmica y retención

No aplicar una caducidad automática al reporte ciudadano pendiente: debe permanecer en la bandeja institucional y conservarse en el historial aunque deje de ser reciente. Una observación sísmica no demuestra por sí sola que el sismo siga ocurriendo.

La vigencia de una alerta pública debe ser distinta de la retención del reporte que la originó. USGS da como referencia que el movimiento fuerte de sismos moderados a grandes suele durar unos 10–30 segundos; un evento muy grande puede sentirse desde segundos hasta un par de minutos, según magnitud, distancia y terreno. Las réplicas son eventos separados y pueden continuar durante días, meses o más. Por eso no existe una duración única que SIMPLE deba inferir para todos los reportes.

Antes de automatizar `EXPIRED`, el modelo separado de alertas públicas debe definir una ventana de vigencia corta y configurable, iniciada al activar/publicar la alerta, guardar `expiresAt`, registrar la transición automática en el historial y mantener el reporte original para revisión/analítica según las reglas de retención. Una réplica se reporta/correlaciona como otro evento, no como prolongación indefinida de la alerta anterior. El `expiresAt` nullable y el estado `EXPIRED` del esquema actual aún no tienen lógica automática; no se deben tratar como si ya funcionaran.

Fuentes técnicas de referencia: [USGS, duración típica del movimiento fuerte](https://pubs.usgs.gov/of/1992/0249/report.pdf), [USGS, duración del movimiento sentido](https://www.usgs.gov/faqs/what-was-duration-earthquake-why-dont-you-report-duration-each-earthquake-how-does-duration) y [USGS, duración de las réplicas](https://www.usgs.gov/faqs/what-difference-between-aftershocks-and-swarms).

### Contrato lógico objetivo para el primer corte completo

Este es un diseño para implementar por etapas. La mitigación local ya separa lecturas y mensajes, pero aún no agrega las entidades de reporte/evento/aviso público ni entrega externa.

Para conservar compatibilidad en la fase 4.6 actual, el subtipo se codifica provisionalmente como el valor de `alerts.type` (`INCENDIO_FORESTAL` o `INCENDIO_ESTRUCTURAL`), y el campo textual existente `level` queda `SIN_CLASIFICAR` en registros nuevos. Los valores antiguos permanecen sin reinterpretarse. La ruta separada de reportes/eventos/alertas públicas y los estados de entrega se implementarán antes de conectar canales externos, no mediante una falsa notificación desde el flujo actual.

- `POST /api/reports` recibiría `category` (`INCENDIO_FORESTAL`, `INCENDIO_ESTRUCTURAL` o `TERREMOTO_OBSERVADO`), descripción, latitud/longitud opcionales como par, `clientRequestId` para idempotencia y hora del dispositivo opcional claramente separada de `receivedAt` del servidor. No recibe un nivel oficial del usuario.
- La respuesta devolvería `reportId`, `RECEIVED_UNVERIFIED`, `receivedAt`, `needsLocation`, `deliveryState` e `officialAlertIssued=false`. Esto permite a la app distinguir “recibido por SIMPLE” de “derivado al destinatario” y evita mensajes falsos de éxito.
- `GET /api/reports/inbox` sería la ruta explícita futura para reportes del moderador interno. Actualmente el mismo rol obtiene esa bandeja desde `GET /api/alerts`, por compatibilidad. Una acción de revisión cambia solo ese reporte y registra actor, hora, comentario y destino. Hasta que exista integración, el destino puede quedar como derivación manual pendiente y la interfaz mantiene el fallback telefónico.
- Un evento candidato relaciona IDs de reportes. No se fusiona el texto, no se cambia el creador y no se elimina ningún reporte al agrupar.
- Una alerta pública usa un ID distinto y solo se publica con validación registrada: organización responsable, operador autorizado, método de verificación, hora, cobertura y vigencia; se enlaza al evento/reportes origen. Si existe boletín oficial, también se registra su identificador o URL. El rol genérico `INSTITUTION` actual no satisface por sí solo esta autorización futura.
- Las entidades actuales de `alerts` y su API se mantienen durante la transición. Las filas históricas sin subtipo/fuente permanecen como legado desconocido; no asignarles subtipo ni atribuirles validez retroactiva.

Dependencia observada: `SimpleViewModel.createAlert` envía actualmente latitud y longitud como `null`. Antes de derivar un incendio a un organismo, la etapa de ubicación debe resolver captura/autorización o exigir una ubicación escrita suficiente; de lo contrario el reporte se conserva, se marca `needsLocation` y no se asegura despacho.

## Flujos por amenaza

### Incendio forestal o estructural

1. `USER` elige subtipo forestal o estructural y envía un reporte con la ubicación y observaciones disponibles. El sistema lo marca como **no verificado**.
2. El backend guarda el reporte y lo muestra/notifica rápidamente a la cola de revisión. Los avisos internos y el eventual envío a un cuerpo de Bomberos/CONAF son canales distintos, con estados y acuse de recepción propios.
3. El moderador interno revisa y deriva el reporte. La agrupación de otros reportes ayuda a identificar que describen el mismo foco, pero un umbral de reportantes no debe bloquear la recepción o derivación inicial de un incendio.
4. La publicación para la población se realiza solo tras la decisión de un organismo competente o una integración formal autorizada, con ubicación/cobertura, fuente, vigencia e instrucciones verificables.
5. Si falla la conexión o el destinatario no confirma recepción, la app debe decirlo con claridad y ofrecer el canal oficial de emergencia. No mostrar “enviado a Bomberos” solo porque SIMPLE guardó el reporte.

SENAPRED indica que ante incendio forestal se avise a CONAF (130) o Bomberos (132), y que no se asuma que otra persona ya llamó. Ante incendio estructural recomienda llamar a Bomberos (132). Por eso SIMPLE debe ser complementaria y no sustituir la llamada de emergencia mientras no exista un canal oficial integrado y operativo.

### Marejadas y tsunamis

- **Marejadas:** el usuario no declara una alerta oficial. La operación futura debe basarse en el aviso marítimo de DIRECTEMAR y estar a cargo de la institución/autoridad correspondiente. La alerta SIMPLE debe conservar identificador/fuente, zona, vigencia y actualizaciones/cancelación.
- **Tsunami:** es una amenaza distinta. SHOA/SNAM emite boletines y estados técnicos; SENAPRED gestiona instrucciones de evacuación y canales como SAE según sus protocolos. SIMPLE puede complementar o reflejar mensajes oficiales si se acuerda la integración; no reemplaza SNAM ni SAE y no inventa estados propios equivalentes.
- El moderador interno actual puede preparar/revisar información, pero no adjudicar carácter oficial a una instrucción sin autorización y fuente enlazada.

### Terremoto y sensores, en una etapa futura

El reporte manual actual permanece informativo/no verificado. Un sistema de detección temprana basado en teléfonos no predice el sismo ni se valida porque varios teléfonos “se muevan hacia la misma dirección”. Requiere detecciones de aceleración con marca temporal, ubicación/calidad del dispositivo y coherencia espacial y temporal de la señal; el servidor correlaciona múltiples activaciones, estima origen/magnitud con incertidumbre y actualiza la estimación mientras llegan datos. La orientación física de cada teléfono varía y la actividad cotidiana produce falsas detecciones.

El trabajo publicado de MyShake describe un clasificador en el teléfono para distinguir sismo de movimiento humano y una detección de red en servidor que confirma el evento y estima ubicación/tiempo/magnitud a partir de múltiples activaciones. Sus umbrales y simulaciones pertenecen a ese estudio, no se deben copiar directamente a Chile/SIMPLE: necesitan calibración con cobertura local, registros del CSN, densidad y calidad de teléfonos y pruebas de falsos positivos/omisiones.

Android restringe sensores continuos en segundo plano desde Android 9 salvo ejecución apropiada (por ejemplo, foreground service) y limita la tasa de algunos sensores en Android 12+. Por lo tanto, la etapa sísmica debe diseñar consentimiento, servicio visible, consumo de batería, permisos, estado de monitoreo y recolección mínima antes de programar el clasificador. La alerta automática requiere criterios de confianza medidos y una vía de corrección/cancelación; nunca prometer certeza absoluta.

## Requisitos transversales de confiabilidad

- Separar `report_id`, `event_id` y `public_alert_id`; no fusionar filas ni reutilizar IDs por similitud geográfica.
- Cada transición guarda actor, hora, motivo y fuente; cada aviso externo tiene ID de entrega, reintentos limitados y acuse. Los reintentos no crean eventos ni alertas duplicadas.
- Para notificaciones críticas futuras, guardar el cambio de estado y el mensaje pendiente de entrega de forma consistente (por ejemplo, outbox transaccional); reintentar entrega sin duplicar. No implementar colas o microservicios mientras no haya integración y carga que lo justifique.
- Definir expiración, actualización y cancelación de alertas, y qué ve el usuario cuando está sin red, el backend cae o el destinatario no responde.
- Distinguir telemetría del dispositivo, identidad del reportante y datos analíticos. No exponer datos personales o ubicaciones precisas a terceros de investigación por defecto.
- Cualquier publicación pública, despacho real, prueba de sensores en vivo o intercambio de datos requiere entorno, destinatarios, autorizaciones y protocolo verificados. No probar contra `simple_db`.

## Secuencia recomendada

1. **Cerrar fase 4.6:** documentar tipos/subtipos y el rol del moderador; acordar que el público no asigna clasificación oficial. Mantener nivel oficial asociado a una fuente externa, no a un color genérico.
2. **Completar flujo base de incendios:** contrato independiente de reporte, subtipo, estado no verificado, idempotencia, ubicación, moderación, historial y mensajes honestos de estado/errores. En pruebas, usar H2 o entorno local aislado. No anunciar despacho ni alertamiento mientras solo exista el registro interno.
3. **Integrar notificación institucional:** cuando exista organismo destinatario autorizado, acordar enrutamiento territorial, formato, autenticación, acuse, tiempos máximos, reintentos, guardia y fallback telefónico.
4. **Publicar alertas verificadas:** definir alcance geográfico, población objetivo, mensajes, evidencia/fuente oficial, expiración, actualización, cancelación y permisos de operadores autorizados. La primera implementación es un canal complementario a los oficiales.
5. **Marejadas/tsunami:** conectar sistemas y autoridades por separado; no mapear una escala a otra.
6. **Piloto sísmico:** primero recolectar/probar en modo sombra con datos sintéticos y, si se autoriza, instrumentación local no pública; comparar estimaciones con fuentes CSN y medir sensibilidad, precisión, latencia, cobertura y consumo. Solo después evaluar alertas operativas y autorización oficial.
7. **Operación oficial y analítica:** definir organizaciones, ámbitos, finalidades, privacidad, acceso, agregación/desidentificación, retención y auditoría antes de habilitar datos para análisis externo.

## Criterio realista de éxito

“Validación exacta” sin falsos positivos ni omisiones no es una garantía técnicamente honesta. El objetivo medible será reducir errores y latencia: registrar evidencia y confianza, controlar quién puede publicar, medir falsos positivos/negativos y tiempo de entrega, comunicar incertidumbre, mantener trazabilidad y permitir corrección/cancelación rápida.

## Fuentes oficiales y técnicas

- [SENAPRED: incendios forestales](https://www.senapred.cl/incendios-forestales/) — aviso a CONAF 130/Bomberos 132 y respuesta rápida.
- [SENAPRED: incendios estructurales](https://www.senapred.cl/incendios-estructurales/) — orientación y llamada a Bomberos 132.
- [CONAF: Glosario de Protección contra Incendios Forestales](https://www.conaf.cl/centro-documental/glosario/) — alertas técnicas forestales y criterios técnicos.
- [DIRECTEMAR](https://www.directemar.cl/) — autoridad marítima y avisos de marejadas.
- [Armada/SHOA: guía SNAM](https://www.armada.cl/armada/site/docs/20230309/20230309122928/guia_referencia_snam.pdf) — operación y estados del Sistema Nacional de Alarma de Maremotos; coordinación con SENAPRED.
- [Kong et al., MyShake, Science Advances](https://pmc.ncbi.nlm.nih.gov/articles/PMC4758737/) — detección de teléfono y correlación espacio-temporal en servidor; umbrales del estudio no son valores de SIMPLE.
- [Android Developers: sensores](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview) — límites de sensores en segundo plano y tasa de muestreo.

## Trazabilidad

Decisiones solicitadas por la persona usuaria el 15 de noviembre de 2026: reporte ciudadano de incendios para derivar tempranamente a Bomberos/organismo correspondiente; alertamiento público de incendios solo tras validación suficiente; marejadas/tsunami gestionadas por instituciones competentes; terremoto manual se mantiene como está y detección por sensores se difiere; uso futuro por entidades oficiales y analítica de organismos de estudio. Este documento ordena la arquitectura objetivo sin afirmar que esas integraciones ya existan.
