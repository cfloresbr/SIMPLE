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

## Validación pendiente

No se pudo ejecutar Gradle en el entorno de trabajo porque el wrapper intenta descargar Gradle 9.6.0 y no hay conectividad de red disponible. Por tanto, estos cambios necesitan compilarse y probarse localmente en el equipo de desarrollo antes de considerarlos validados. No se han probado con una instancia MySQL real.

## Preparar una cuenta institucional de pruebas

Por seguridad, el endpoint público de registro no permite elegir `INSTITUTION` ni `ADMIN`. Para pruebas locales, registra una cuenta normal desde SIMPLE y luego ejecuta en MySQL, reemplazando el correo por el de esa cuenta:

```sql
UPDATE users SET role = 'INSTITUTION' WHERE email = 'institucion.prueba@ejemplo.cl';
```

Cierra sesión e inicia sesión de nuevo para obtener un token nuevo. La autorización también consulta el rol actual en la base de datos.
