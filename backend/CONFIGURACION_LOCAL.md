# SIMPLE — Configuración local segura del backend (IntelliJ IDEA)

## Objetivo

La configuración de Spring Boot ya no almacena contraseñas ni la clave JWT en `application.properties`. IntelliJ IDEA entrega esos valores al proceso mediante variables de entorno. No se necesita cambiar el código Android ni ejecutar scripts sobre MySQL para aplicar este ajuste.

## 1. Variables requeridas

En IntelliJ IDEA, abre el proyecto `backend` y ve a **Run > Edit Configurations...**. Selecciona la configuración de Spring Boot/Gradle que utilizas para iniciar el backend. En **Environment variables**, agrega:

- `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/simple_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
- `SPRING_DATASOURCE_USERNAME=` el nombre de usuario MySQL que ya funciona en tu instalación local.
- `SPRING_DATASOURCE_PASSWORD=` la contraseña actual de ese usuario MySQL.
- `APP_JWT_SECRET=` una clave aleatoria nueva, generada como se explica abajo.
- `APP_JWT_EXPIRATION_MS=86400000` (opcional; equivale a 24 horas).

No incluyas comillas alrededor de los valores salvo que el editor de IntelliJ las requiera. No pegues contraseñas ni la clave JWT en el código, en `README.md`, en capturas, ni en el chat.

## 2. Generar una clave JWT aleatoria en Windows

Abre PowerShell y ejecuta:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Copia el resultado directamente al valor de `APP_JWT_SECRET` en la configuración de ejecución de IntelliJ. No lo guardes en este archivo ni en el repositorio. Esta clave debe mantenerse privada.

> Al cambiar la clave JWT, cualquier token emitido con la clave anterior dejará de validarse. Es normal que sea necesario iniciar sesión nuevamente. Esto no modifica ni elimina cuentas o alertas almacenadas en MySQL.

## 3. Ejecutar el backend

1. Confirma que el servicio MySQL esté iniciado y que la base `simple_db` exista.
2. Usa las credenciales MySQL locales que ya funcionan; no cambies el usuario ni la contraseña de MySQL como parte de este paso.
3. Guarda la configuración de ejecución en IntelliJ. No compartas ni publiques una configuración que contenga valores secretos.
4. Ejecuta el backend de la forma habitual. La API debe escuchar en `http://localhost:8080`.

También puedes iniciar desde PowerShell con las variables definidas únicamente en esa ventana de terminal, usando `gradlew.bat bootRun`. Cierra la terminal al terminar para descartar esas variables de sesión.

## 4. Verificación segura

- Revisa `backend/src/main/resources/application.properties`: debe contener referencias `${...}` y no credenciales reales.
- Compila el backend sin iniciar el servidor con `gradlew.bat compileKotlin` desde `backend`.
- Luego inicia desde IntelliJ y revisa el log de arranque. Si aparece un error de conexión, comprueba que el nombre de usuario, contraseña, URL y servicio MySQL coincidan con la configuración local.
- Una vez iniciado, prueba el inicio de sesión y una consulta de alertas desde la app o Postman. No ejecutes scripts de creación/eliminación de tablas como parte de esta verificación.

## 5. No versionar secretos

- No agregues valores reales a `application.properties`.
- No subas capturas de pantalla que muestren el campo de variables de entorno.
- Si el proyecto se guarda en Git, revisa el historial antes de compartirlo: quitar una contraseña del archivo actual no la elimina de commits anteriores.
- Si una clave real se compartió públicamente, considera rotarla. Cambiar la clave JWT invalida los tokens existentes; cambiar la contraseña MySQL requiere actualizar también la configuración local de MySQL y la variable correspondiente.

## Nota sobre IntelliJ

Las variables guardadas en una configuración de ejecución local pueden permanecer en los archivos de configuración del IDE o en la configuración de usuario, según la opción utilizada. No marques la configuración como compartida ni la incluyas en un commit si contiene secretos. Para equipos, utiliza un gestor de secretos o variables de entorno configuradas fuera del repositorio.
