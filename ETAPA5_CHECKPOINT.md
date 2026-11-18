# SIMPLE — Etapa 5 checkpoint de inicio y brechas

## Estado de inicio (17-11-2026)

- Rama local: `etapa4`. El árbol de trabajo ya contenía cambios locales de implementación/documentación; se preservan. No se hizo commit ni push en este checkpoint.
- Etapa 4.6 validada localmente: suite backend `BUILD SUCCESSFUL in 1m 10s`; Android `:app:assembleDebug` `BUILD SUCCESSFUL in 53s`; instalación APK `Success`; la persona usuaria confirmó la revisión de solo lectura del feed anónimo, lista propia `USER` y bandeja interna institucional.
- No se modificó ni probó con escrituras la base `simple_db`. Las pruebas backend se ejecutaron con configuración H2 de test.
- Arquitectura aún no equivale a despacho ni publicación oficiales. `INSTITUTION`/`ADMIN` siguen siendo moderadores globales; no hay ámbito por agencia/territorio ni captura GPS cliente.

## Revisión de brechas de etapa 5

| Dato/requisito | Estado actual | Consecuencia y decisión |
|---|---|---|
| Lista institucional de reportes | Parcialmente implementada: `ReportsScreen` muestra lista de alertas y resumen global; el origen es `/api/alerts`, aún no existe entidad/end-point separado de `Report`. | Conservar compatibilidad en este corte; no migrar modelo ni crear tablas sin necesidad definida. |
| Creador/última gestión | Brecha encontrada en la auditoría; el avance 5.1 ya añade los IDs al DTO institucional. | ID de cuenta solo para `INSTITUTION`/`ADMIN`; el DTO de `USER` los deja nulos. No nombres/correos ni otra información personal. |
| Activación/resolución/vencimiento | Brecha encontrada en la auditoría; el avance 5.1 ya añade marcas de tiempo opcionales al DTO y a la tarjeta interna. | Mostrar solo fechas registradas; no inferir expiración ni cambiar estados automáticamente. Pendiente de compilar y validar. |
| Nivel | Nuevos registros tienen `SIN_CLASIFICAR`; niveles históricos permanecen. | Presentar como clasificación interna/no oficial. No recalcular legado. |
| Ubicación | Modelo/API permiten coordenadas y backend valida pares/rangos; el cliente siempre envía `null`. Manifest declara permisos, pero no se pide permiso ni se obtiene posición. | Próxima subfase de ubicación optativa en primer plano, con permiso y explicación en el momento del reporte, alternativa sin ubicación y sin seguimiento de fondo. |
| Resumen y conteos | `GET /api/reports/summary` restringido a `INSTITUTION`/`ADMIN`, conteos globales desde las alertas existentes. | Mantener lectura únicamente y documentar que no hay segmentación institucional/territorial. |

## Subfases

1. **5.1 Ciclo de vida y actor — implementada localmente, validación pendiente:** `AlertDto` expone `createdBy`, `updatedBy`, `updatedAt`, `activatedAt`, `resolvedAt`, `expiresAt`; el backend oculta los IDs a `USER`, la tarjeta de Reportes institucionales muestra IDs/fechas existentes, y se agregó cobertura a `AlertApiTest`. Falta ejecutar la suite/backend y el build Android, instalar el APK y revisar las tarjetas. No exponer correo/nombre del reportante.
2. **5.2 Ubicación optativa — pendiente:** obtener ubicación solo al crear un reporte, con permiso en primer plano y explicación; permitir enviar sin ubicación y mostrarlo explícitamente. Validar permiso denegado, proveedor apagado y ubicación desactualizada. No integrar seguimiento en segundo plano.
3. **5.3 Coherencia y privacidad — pendiente:** comprobar visibilidad por rol, fechas tras los cambios de estado y consistencia del resumen, con H2/datos sintéticos. No escribir pruebas en `simple_db`.

## Regla de continuación

Actualizar este checkpoint con archivos, decisiones, pruebas/resultados y pendientes por subfase. No preparar commit/push hasta completar validaciones de etapa 5 y revisar el diff. Si aparece una diferencia en datos, detener escrituras y registrar evidencia antes de continuar.

## Avance 5.1 (17-11-2026)

- Cambios locales: `backend/src/main/kotlin/cl/simple/api/AlertController.kt`, `android/app/src/main/java/cl/simple/app/data/Api.kt`, `android/app/src/main/java/cl/simple/app/MainActivity.kt` y `backend/src/test/kotlin/cl/simple/AlertApiTest.kt`.
- El rol institucional recibe IDs de cuenta y marcas de tiempo ya almacenadas; `USER` no recibe IDs del creador/gestor. La tarjeta no infiere vencimiento ni publica registros.
- Pruebas, compilación y revisión visual de este avance aún no ejecutadas. No hubo cambios de esquema ni escrituras de base de datos.
