package cl.simple.app.data

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

fun Throwable.toSimpleMessage(
    fallback: String,
    unauthorizedMessage: String = "Tu sesión expiró. Inicia sesión nuevamente."
): String {
    if (this is HttpException) {
        serverMessage()?.let { return it }

        return when (code()) {
            400 -> "Los datos enviados no son válidos. Revisa la información e inténtalo nuevamente."
            401 -> unauthorizedMessage
            403 -> "No tienes permiso para realizar esta acción."
            408 -> "La solicitud tardó demasiado. Inténtalo nuevamente."
            429 -> "Hay demasiadas solicitudes. Espera un momento e inténtalo nuevamente."
            in 500..599 -> "El servicio presenta problemas. Inténtalo nuevamente más tarde."
            else -> fallback
        }
    }

    return when (this) {
        is SocketTimeoutException ->
            "La conexión tardó demasiado. Revisa tu conexión e inténtalo nuevamente."

        is IOException ->
            "No fue posible conectar con el servidor. Revisa tu conexión e inténtalo nuevamente."

        else -> fallback
    }
}

private fun HttpException.serverMessage(): String? {
    return try {
        val body = response()?.errorBody()?.string()
        if (body.isNullOrBlank()) return null

        val json = JsonParser.parseString(body)
        if (!json.isJsonObject) return null

        json.asJsonObject.get("message")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString
            ?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}
