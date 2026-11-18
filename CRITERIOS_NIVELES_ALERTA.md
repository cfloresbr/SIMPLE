# Criterios de nivelación por tipo de alerta

## Estado del análisis

Revisión de fuentes oficiales realizada el 15 de noviembre de 2026. La persona usuaria confirmó que SIMPLE incluirá incendios **forestales y estructurales**. Este documento define criterios de dominio y registra la adaptación inicial de fase 4.6. La clasificación debe separar lo que observa una persona, la prioridad interna de SIMPLE y una declaración oficial de autoridad competente.

## Hallazgo de diseño

El modelo actual reutiliza `INFORMATIVA`, `PREVENTIVA`, `AMARILLA` y `ROJA` como un mismo campo para todo tipo de alerta y permite que quien reporta elija el valor antes de la revisión institucional. Las fuentes oficiales usan escalas distintas según el fenómeno. El estado del proceso (`PENDING`, `ACTIVE`, `REJECTED`, etc.) tampoco equivale al nivel de amenaza.

La recomendación es separar cinco conceptos:

1. **Estado del reporte/flujo:** recibido, en revisión, aprobado, rechazado, resuelto.
2. **Subtipo de incendio:** forestal o estructural; “incendio” por sí solo no identifica la fuente de clasificación adecuada.
3. **Clasificación oficial externa:** se registra como oficial solo al referenciar una declaración vigente de CONAF, SENAPRED, DIRECTEMAR, SHOA u otra autoridad competente. No la crea SIMPLE por analogía de colores.
4. **Prioridad interna de gestión:** si el producto necesita ordenar revisión o atención, debe nombrarse explícitamente como interna y no como alerta oficial.
5. **Observación del usuario:** descripción y efectos percibidos, sin presentarla como nivel oficial.

## Línea base previa al cambio de fase 4.6

- Android muestra el tipo general `INCENDIO`; no solicita subtipo forestal/estructural.
- `AlertReq` requiere siempre `level`; el backend acepta el enum común `PREVENTIVA`, `AMARILLA`, `ROJA`, `INFORMATIVA` para todos los tipos.
- Las cuentas `USER` pueden reportar `INCENDIO` o `TERREMOTO`; el reporte de incendio entra `PENDING`. La aprobación no admite una clasificación nueva.
- Por tanto, no basta con cambiar etiquetas del selector: el dato del subtipo, su actor, fuente, compatibilidad histórica y permisos deben definirse juntos.

## Implementación transitoria de fase 4.6

- Android distingue `INCENDIO_FORESTAL` e `INCENDIO_ESTRUCTURAL`, elimina el selector genérico de nivel y no envía una clasificación oficial.
- El backend conserva temporalmente el subtipo dentro de `alerts.type` y fuerza `SIN_CLASIFICAR` para los registros nuevos, aunque un cliente antiguo envíe `level`. Los registros históricos no se reinterpretan.
- `alerts.level` se representa como `VARCHAR` (`String` Kotlin), por lo que no se requiere cambio de esquema para aceptar esta etiqueta. Esto no crea aún campos de organismo, boletín, cobertura o vigencia.
- `INSTITUTION` sigue siendo un moderador interno; no se ha integrado una institución real, despacho a Bomberos ni publicación oficial. Los estados actuales de SIMPLE no equivalen a una clasificación de autoridad.
- Esta adaptación es provisional hasta que se implemente el modelo separado de reportes/eventos/alertas públicas descrito en `ARQUITECTURA_ALERTAMIENTO_TEMPRANO.md`.

## Propuesta por fenómeno

| Tipo | Qué mide/publica la fuente oficial | Propuesta para SIMPLE | Decisión pendiente |
|---|---|---|---|
| Incendio forestal | CONAF distingue Alerta Técnica Temprana Preventiva (verde), Amarilla y Roja. El glosario técnico describe criterios de comportamiento del fuego, recursos, amenaza y cobertura. SENAPRED publica alertas territoriales por eventos, incluidas las originadas por incendio forestal. Son actos de las autoridades y sistemas distintos. | `USER` selecciona subtipo forestal y describe el hecho, sin asignar nivel oficial. La institución revisa el reporte y gestiona su flujo. La app solo muestra nivel CONAF/SENAPRED como oficial cuando se asocia a la declaración/boletín vigente, organismo, cobertura y fecha. Si SIMPLE requiere prioridad propia, etiquetarla “interna”. | Definir si SIMPLE solo mostrará referencias oficiales o permitirá una prioridad interna, separada del boletín. La institución de SIMPLE no debe presumirse autorizada para declarar niveles CONAF/SENAPRED. |
| Incendio estructural | SENAPRED define como incendio en casas, edificios y locales comerciales, y recomienda llamar a Bomberos 132. La página de Bomberos consultada describe una “segunda alarma” en una respuesta local con despliegue de compañías/unidades. En las fuentes revisadas no aparece una escala nacional de severidad pública interoperable equivalente a los niveles CONAF. | `USER` selecciona subtipo estructural y describe el hecho; la app debe indicar que ante un incendio activo se llame a Bomberos (132), sin esperar validación en SIMPLE. No reutilizar niveles forestales ni colores SENAPRED como una escala propia. “Segunda alarma” no debe tratarse como nivel de riesgo SIMPLE: es una señal operativa del cuerpo local. | Definir si se necesita una prioridad interna de revisión para incendio estructural; si se incorpora, identificarla explícitamente como interna y quién la puede asignar. |
| Terremoto/sismo | El CSN distingue magnitud (una medición del tamaño del evento) e intensidad Mercalli modificada (I–XII), observada por localidad y efectos. Los sismos no tienen el mismo flujo de alerta preventiva amarilla/roja antes de ocurrir. | El reporte manual público no debe escoger `AMARILLA`. Mientras no existan sensores, la percepción de personas se registra como observación no oficial. Magnitud/Intensidad solo se mostrarían con fuente técnica identificada. | Definir más adelante si SIMPLE recibe percepción humana además de sensores; fijar fuentes, validación y umbrales antes de notificar. |
| Marejadas | DIRECTEMAR publicó código visual para distinguir Avisos Especiales de Marejadas (amarillo) y Marejadas Anormales (rojo), según intensidad/riesgo; entrega avisos y recomendaciones marítimas. No es una escala de tsunami. | Si la institución registra una clasificación manual, ofrecer nombres explícitos del aviso DIRECTEMAR asociados a sus colores. No usar “preventiva” como sustituto de los avisos marítimos. Para automatizar se requeriría integrar aviso oficial, área/periodo/altura/dirección y referencia del boletín. | Confirmar si la institución solo crea una alerta interna o si el sistema reflejará avisos DIRECTEMAR. Definir cómo se identificará el aviso y su vigencia. |
| Tsunami (distinto de marejada) | El SHOA/SNAM difunde estados de amenaza propios (informativo, precaución, alerta, alarma y cancelación) y evalúa información sísmica y del nivel del mar. | No mezclar los estados SHOA con el tipo `MAREJADAS` ni con los estados internos de trabajo. Si se incorpora tsunami como tipo, modelarlo con fuente, cobertura/bloques, vigencia y estado de cancelación propios. | Fuera del tipo de alerta marejadas actualmente acordado; requiere alcance y datos oficiales definidos. |

## Cambio de flujo recomendado antes de endurecer niveles

- Mantener la regla validada: reporte público de incendio entra como `PENDING`; institución decide aceptar/rechazar.
- Distinguir forestal/estructural en los reportes nuevos; los registros existentes no se deben clasificar retroactivamente sin evidencia y podrían requerir subtipo `LEGACY/NO_ESPECIFICADO` en el modelo futuro.
- Quitar del formulario `USER` la capacidad de elegir un nivel oficial. La descripción del usuario no debe almacenarse como clasificación oficial.
- La institución puede gestionar el flujo y, si se aprueba una prioridad local, debe estar identificada como “prioridad interna”. Registrar una clasificación externa oficial requiere autoridad, referencia, cobertura y vigencia verificables.
- Para incendio estructural, mostrar orientación para llamar a Bomberos (132) ante un incendio activo; SIMPLE es un canal complementario de reporte y no un servicio de despacho.
- La base actual hace `level` obligatorio y la ruta de aprobación no recibe un nivel. La adaptación transitoria conserva esa columna y sus valores históricos; el cambio al modelo separado requiere diseñar compatibilidad del modelo/API/Android y datos históricos en conjunto. No hacer migración parcial.
- Mantener actor, fuente, fecha/hora y valor originales auditables; estado de flujo, subtipo, observación y clasificación deben ser campos/conceptos separados.
- No activar alertas públicas automáticas desde sensores hasta definir umbrales, falsos positivos, permisos y procedimiento de validación.

## Acuerdo de alcance y propuesta operativa

- Alcance confirmado por la persona usuaria: SIMPLE incluirá reportes de incendio **forestal y estructural**.
- Decisión confirmada por la persona usuaria: el rol `INSTITUTION` será por ahora un **moderador interno de SIMPLE**, pensado como posible encargado dentro de una institución, pero no vinculado aún a una autoridad concreta.
- En este alcance, `INSTITUTION` puede revisar, aprobar/rechazar y gestionar estados; **no declara por sí mismo niveles oficiales de CONAF, SENAPRED, Bomberos o DIRECTEMAR**.
- Recomendación inicial: no añadir todavía prioridad interna; el flujo `PENDING/ACTIVE/RESOLVED/REJECTED` ya organiza la gestión. Si la persona usuaria decide que el moderador necesite priorizar la cola, definirlo como campo separado claramente interno, nunca como semáforo oficial.
- Clasificaciones oficiales forestales o de marejadas solo se registrarán junto con organismo, identificador/enlace del aviso, cobertura y vigencia; para incendio estructural se conserva el estado de flujo y orientación de emergencia, sin inventar una escala común.
- Futuro confirmado como dirección de producto: entidades oficiales podrían operar SIMPLE y organizaciones de estudio podrían acceder a datos para análisis. Esto se planificará en una etapa de interoperabilidad/gobernanza, definiendo propósito, acceso por rol, minimización, desidentificación/agregación, retención y trazabilidad antes de compartir o exportar datos.
- Diseño actual: capturar el subtipo forestal/estructural y preservar observaciones; la fase 4.6 aplica `SIN_CLASIFICAR` a registros nuevos sin alterar datos anteriores. Pendiente validar compilación, suite y comportamiento visual antes de cerrar la fase.

## Evidencia oficial consultada

- [SENAPRED: estados de alerta](https://www.senapred.cl/alertas/) — Alerta Temprana Preventiva (verde), Amarilla y Roja.
- [CONAF: situación y pronóstico de incendios](https://www.conaf.cl/incendios/situacion-actual-y-pronostico-de-incendios/) — probabilidad de ignición y humedad del combustible como indicadores.
- [CONAF: Glosario de Protección contra Incendios Forestales 2025–2026](https://www.conaf.cl/centro-documental/glosario/) — alerta técnica forestal, cobertura, peligrosidad y niveles de alerta verde/amarilla/roja.
- [SENAPRED: recomendaciones ante incendios forestales](https://www.senapred.cl/incendios-forestales/) — prevención, evacuación, fuentes oficiales y teléfonos de emergencia.
- [SENAPRED: incendios estructurales](https://senapred.cl/incendios-estructurales/) — define incendio estructural y recomienda llamar a Bomberos (132).
- [Bomberos de Chile: segunda alarma en incendio estructural](https://www.bomberos.cl/contenidos/segunda-alarma-de-incendio-en-cuerpo-de-bomberos-de-los-andes) — ejemplo de alarma y despliegue operativo de un cuerpo local; no es una escala nacional de alertamiento público. Esta última conclusión es una inferencia basada en las fuentes consultadas.
- [CSN: glosario sismológico](https://www.csn.uchile.cl/sismologia/glosario/) — magnitud e intensidad Mercalli modificada I–XII.
- [SENAPRED: recomendaciones ante sismos](https://www.senapred.cl/sismos/) — preparación y conducta durante/después de un sismo.
- [DIRECTEMAR: código de colores para avisos de marejadas](https://prontus.directemar.cl/directemar/sin-titulo-121370982) — aviso especial amarillo y marejada anormal roja.
- [DIRECTEMAR: recomendaciones ante marejadas](https://www.directemar.cl/temporada-estival-2024-2025-autoridad-maritima-llama-a-la-prevencion-y) — evitar roqueríos y rompientes, respetar avisos y restricciones.
- [SHOA/SNAM: guía de referencia](https://www.armada.cl/armada/site/docs/20230309/20230309122928/guia_referencia_snam.pdf) — misión técnica, evaluación y estados propios de amenaza de tsunami.

## Trazabilidad

Este análisis deriva de los requisitos SIMPLE registrados en `HOJA_DE_RUTA_SIMPLE.md`. Alcance aprobado: incendios forestales y estructurales; por ahora `INSTITUTION` es moderador interno. La prioridad interna adicional aún no se ha acordado y queda sin implementar. Para el flujo de reportes, despacho/validación pública y sensores, consultar `ARQUITECTURA_ALERTAMIENTO_TEMPRANO.md`.
