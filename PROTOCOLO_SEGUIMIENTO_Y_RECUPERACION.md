# Protocolo de seguimiento y recuperación de SIMPLE

## Objetivo

Mantener una ruta verificable del desarrollo entre sesiones, ramas y herramientas, detectar trabajo realizado fuera del checkout activo y poder retomar después de una interrupción sin sobrescribir cambios ni repetir verificaciones que siguen siendo válidas.

Este protocolo aplica a todas las etapas y a los cambios hechos desde Android Studio, IntelliJ, GitHub Desktop, Vento, otra copia del proyecto o una sesión de Codex.

## Regla de continuidad

Antes de modificar archivos, antes de cerrar una fase y antes de preparar un commit, comparar el estado actual con el último checkpoint. Identificar rama, SHA de `HEAD`, upstream, archivos modificados, staged, untracked, worktrees y commits locales/remotos. Si rama, SHA y estado de archivos no cambiaron, reutilizar la evidencia vigente y no repetir pasos de inspección o pruebas sin motivo. Si algo cambió, revisar solo los archivos y validaciones afectados, además de las pruebas de regresión necesarias.

No editar, restaurar, limpiar, mover ni descartar cambios cuyo origen o propósito no esté claro. Primero preservarlos y clasificarlos; después decidir cómo continuar.

## Checkpoint al iniciar o retomar trabajo

1. Leer el plan y el último checkpoint de la etapa activa.
2. Registrar fecha/hora local, ruta del checkout, rama actual, SHA completo de `HEAD`, upstream y estado de sincronización.
3. Ejecutar `git status --short --branch --untracked-files=all`, `git branch -vv`, `git worktree list` y revisar commits desde el último checkpoint.
4. Separar los cambios en: modificados rastreados, staged, untracked de trabajo, artefactos generados y archivos sospechosos/dañados.
5. Comparar cada cambio con el último checkpoint, con `HEAD` y con las ramas/worktrees pertinentes. Revisar también cambios publicados en remoto si el acceso está disponible; no hacer `pull`, `reset`, `checkout`, `clean` ni `stash` como parte de una inspección.
6. Si una herramienta o copia externa contiene trabajo que no aparece en el checkout, anotarlo como ubicación pendiente y no asumir que está perdido ni volver a implementarlo.

## Seguimiento durante la etapa

Mantener dos registros coordinados:

- **Ruta técnica:** fase, objetivo, criterios de aceptación, archivos y pruebas requeridos en `ETAPA3_PLAN.md` o el plan de la etapa activa.
- **Estado comprobado:** rama, SHA, cambios identificados, validaciones ejecutadas, evidencia, límites, incidentes y siguiente paso en el checkpoint de la etapa.

Actualizar el checkpoint después de cada fase o corrección importante, y antes de pausar. Indicar qué evidencia corresponde a qué SHA; una prueba ejecutada en un SHA anterior no se atribuye automáticamente a cambios posteriores.

Para evitar redundancias, conservar comandos y resultados exactos. No volver a ejecutar una prueba si el código/configuración relevante y el entorno no cambiaron, salvo que sea una regresión requerida para cerrar la fase. Diferenciar entre resultado informado por la persona usuaria, resultado observado directamente y resultado inferido.

## Protocolo para interrupciones y archivos dañados

1. **Detener cambios.** No sobrescribir ni borrar el archivo afectado ni archivos cercanos.
2. **Tomar inventario de solo lectura.** Anotar ruta, tamaño, fecha, estado Git, SHA-256 y mensaje de error; guardar la salida relevante del log.
3. **Preservar una copia byte a byte fuera de Git** con nombre que incluya fecha y hora. Verificar que el hash de la copia coincida con el original. Para una base de datos, realizar y verificar un respaldo antes de pruebas que puedan modificar datos.
4. **Comparar con fuente conocida.** Revisar la versión en `HEAD`, commits anteriores, respaldo y demás worktrees. Identificar si el archivo es del usuario, una salida generada o un artefacto de fallo.
5. **Recuperar en una copia de trabajo**, no sobre el original. Aplicar cambios mínimos y verificar el hash/diff, compilación o prueba que corresponda.
6. **Documentar el incidente y la recuperación** en el checkpoint: causa confirmada o hipótesis, archivo afectado, copia preservada, versión recuperada, validaciones y riesgos restantes.

### Archivos generados y registros de fallo

Tratar `build/`, `.gradle/`, `hs_err_pid*.log`, `replay_pid*.log`, heap dumps, volcados y archivos temporales como artefactos, no como código. No agregarlos al commit por defecto. No borrarlos antes de registrar su ruta, fecha, tamaño y causa probable; conservarlos fuera de Git si pueden ayudar al diagnóstico. Un artefacto inesperado, como `android/hs_err_pid26640.log`, se reporta como pendiente de clasificación y se excluye del commit hasta resolverlo.

### Salvaguarda para cambios de alertas y base de datos

- En pruebas manuales, limitar cada operación a IDs de alertas identificados y a datos marcados como prueba. No ejecutar actualizaciones o eliminaciones amplias ni repetir una acción cuyo resultado sea incierto.
- Antes de una prueba que modifique MySQL, guardar un respaldo verificado y registrar conteos por estado y los IDs objetivo. Después, comparar exactamente esos mismos datos.
- Si una acción modifica más alertas o historiales de los esperados, detener el flujo. No refrescar/reintentar operaciones de escritura ni “limpiar” los datos hasta conservar logs, IDs afectados, conteos y un respaldo del estado observado.
- Tratar la posible interacción entre alertas como riesgo diferido. No rediseñar ahora el modelo ni añadir mecanismos de concurrencia que no se necesitan para cerrar el flujo principal; primero estabilizar creación, consulta, gestión y recuperación ante errores.
- Una vez estable el flujo principal, abrir una validación específica de integridad entre alertas: alcance por ID, atomicidad entre alerta e historial, repetición de solicitudes y concurrencia de cambios. Definir entonces controles y pruebas en una base aislada antes de modificar datos reales.

## Puerta previa a commit y push

Antes de crear un commit:

1. Confirmar que el trabajo está en la rama de etapa prevista y que los cambios de otras herramientas/cuentas están incorporados de forma intencional.
2. Revisar el diff completo y el diff staged; verificar cada archivo nuevo. Confirmar que no incluye secretos, respaldos, registros de fallo, build outputs ni cambios ajenos a la fase.
3. Ejecutar las validaciones que exige el criterio de aceptación para ese cambio. Registrar el resultado y el SHA probado.
4. Confirmar que plan y checkpoint describen el mismo alcance y que no hay pasos marcados como pendientes que ya se completaron ni pruebas reportadas como hechas sin evidencia.
5. Revisar el mensaje y contenido del commit. Después del commit, registrar su SHA; después del push, confirmar la rama y el SHA remoto.

Una revisión de diff es obligatoria antes de cada commit. Si el estado local no puede verificarse, hay conflictos, un archivo dañado, secretos potenciales o evidencia crítica desalineada, no recomendar ni realizar commit/push hasta resolver y documentar el bloqueo.

## Formato breve de checkpoint

```text
Fecha y zona horaria:
Checkout:
Etapa/fase:
Rama / upstream:
HEAD antes → después:
Estado Git: modified / staged / untracked / limpio
Cambios externos o interrupciones detectadas:
Archivos relevantes y clasificación:
Pruebas: comando, resultado, SHA probado, quién ejecutó
Validación manual y evidencia:
Datos/backups afectados:
Pendientes/riesgos:
Siguiente acción no redundante:
```

## Estado inicial de aplicación

Al adoptar este protocolo en la etapa 3, el checkout inspeccionado estaba en `etapa3`, con `HEAD` `0a18b1a278ff725e37de4277e340a42152ecaf2b`, alineado con `origin/etapa3`. No había modificaciones rastreadas ni staged; estaba presente el archivo no rastreado `android/hs_err_pid26640.log`, de 99.779 bytes, con diagnóstico de fallo de asignación de memoria nativa de JVM del 7 de noviembre de 2026. Se mantiene sin borrar y fuera de commits hasta su clasificación y resolución.
