# SIMPLE — Etapa 1: reglas de backend y trazabilidad

## Cambios incluidos

1. Se ampliaron los estados de alerta: `PENDING`, `VALIDATED`, `ACTIVE`, `REJECTED`, `RESOLVED` y `EXPIRED`.
2. Se incorporaron campos para registrar última modificación, persona que modificó, activación, resolución y vencimiento.
3. Se añadió `alert_status_history` para auditar cambios de estado.
4. Se protegieron las acciones institucionales: validar, aprobar, activar, rechazar y resolver requieren `INSTITUTION` o `ADMIN`.
5. El registro público no permite elegir el rol: las cuentas nuevas se crean como `USER`.
6. `GET /api/reports/summary` ahora está restringido a `INSTITUTION` y `ADMIN`.
7. Se conservó la forma de `AlertDto` para reducir cambios necesarios en Android.

## Endpoints

- `GET /api/alerts`: listar alertas; requiere autenticación.
- `POST /api/alerts`: crear un reporte o alerta, con reglas por rol.
- `POST /api/alerts/{id}/validate`: pasar `PENDING` a `VALIDATED`.
- `POST /api/alerts/{id}/approve`: pasar `PENDING`/`VALIDATED` a `ACTIVE`.
- `POST /api/alerts/{id}/activate`: pasar `VALIDATED` a `ACTIVE`.
- `POST /api/alerts/{id}/reject`: pasar `PENDING`/`VALIDATED` a `REJECTED`.
- `POST /api/alerts/{id}/resolve`: pasar `ACTIVE` a `RESOLVED`.
- `GET /api/alerts/{id}/history`: consultar trazabilidad.
- `GET /api/reports/summary`: resumen institucional.

Las acciones de cambio de estado aceptan opcionalmente un JSON como `{"comment":"Revisado por la institución"}`.

## Importante para la base de datos

En instalaciones existentes, realizar una copia de seguridad de `simple_db` antes de iniciar la nueva versión. El backend está configurado con `spring.jpa.hibernate.ddl-auto=update`, que intenta incorporar las columnas y la tabla de historial al iniciar. `database/schema.sql` describe el esquema para instalaciones nuevas.

## Validación ejecutada

### Pruebas automatizadas de backend

Comando ejecutado desde `backend`:

```powershell
.\gradlew.bat test --no-daemon --console=plain
```

Resultado: `BUILD SUCCESSFUL` (Gradle 9.6.0; 5 tareas, 2 ejecutadas y 3 actualizadas). `AlertApiTest` ejecutó 4 pruebas, con 0 fallos, 0 errores y 0 omitidas:

1. Una cuenta `USER` no puede aprobar una alerta (`403`) y no se llama a los repositorios de persistencia para cambiarla.
2. Una cuenta `USER` no puede consultar el resumen institucional (`403`).
3. Una cuenta `INSTITUTION` puede aprobar una alerta `PENDING`; la respuesta queda `ACTIVE`, se registra `activatedAt` y se verifica el evento de historial con estado anterior, nuevo estado, actor y comentario.
4. Una cuenta `INSTITUTION` no puede resolver directamente una alerta `PENDING` (`409`), y no se guarda ni la alerta ni un evento de historial.

Estas pruebas usan repositorios simulados y validan las rutas/controladores; por sí solas no prueban persistencia ni migraciones de MySQL.

### Compilaciones y validaciones manuales registradas

- Backend: el usuario informó `BUILD SUCCESSFUL` para `gradlew.bat test` antes de agregar estas pruebas de controlador.
- Android: el usuario informó `BUILD SUCCESSFUL` para `:app:assembleDebug`.
- Validación manual reportada: `GET /api/alerts` como `USER` respondió `200`; `GET /api/reports/summary` como `USER` respondió `403`; `POST /api/alerts/8/approve` como `USER` respondió `403`.
- Revisión visual reportada: el rol público no ve controles institucionales; el rol institucional muestra resumen y acciones de gestión.
- Validación MySQL ejecutada el 2026-11-02: se creó el respaldo `C:\Users\cflor\Documents\SIMPLE-backups\simple_db-20261102-011144.sql` (9,302 bytes), antes de iniciar el backend con su configuración de etapa 1.
- Se creó desde la app el reporte de prueba `alerts.id=9` como `PENDING` y se aprobó como `ACTIVE`. MySQL registró los eventos `alert_status_history.id=17` (`NULL` → `PENDING`) e `id=18` (`PENDING` → `ACTIVE`), con actores y comentarios correspondientes.
- Después de reiniciar el backend, la consulta volvió a mostrar ambas filas (17 y 18); esto confirma que el estado y su historial quedaron persistidos en MySQL tras el reinicio.
- `SHOW COLUMNS FROM alerts;` confirmó en MySQL los campos `updated_at`, `updated_by`, `activated_at`, `resolved_at` y `expires_at`, junto con las columnas esperadas de alerta.

Las compilaciones y respuestas manuales anteriores se consignan como resultados reportados por el usuario, no como ejecuciones repetidas en este checkpoint.

## Cierre de la validación de etapa 1

- Después de reiniciar el backend, el usuario confirmó en la app como `INSTITUTION` que la alerta 9 aparece `ACTIVE` y el resumen institucional refleja el total actualizado.
- Combinada con las cuatro pruebas automatizadas, las validaciones HTTP por rol y la comprobación de esquema e historial persistido en MySQL, queda cerrado el alcance de validación de etapa 1 definido en este checkpoint.
- Cobertura adicional para etapas posteriores: tokens ausentes/inválidos/expirados, pruebas completas del rol `ADMIN`, errores de red y notificaciones/geolocalización. No forma parte de los criterios de cierre funcional de esta etapa.

La conexión local a MySQL se confirmó en `127.0.0.1:3306`. Para esta validación no se registraron credenciales en el repositorio ni en el informe.

## Preparar una cuenta institucional de pruebas

Por seguridad, el endpoint público de registro no permite elegir `INSTITUTION` ni `ADMIN`. Para pruebas locales, registra una cuenta normal desde SIMPLE y luego ejecuta en MySQL, reemplazando el correo por el de esa cuenta:

```sql
UPDATE users SET role = 'INSTITUTION' WHERE email = 'institucion.prueba@ejemplo.cl';
```

Cierra sesión e inicia sesión de nuevo para obtener un token nuevo. La autorización también consulta el rol actual en la base de datos.
