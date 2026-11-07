package cl.simple.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        exception: ResponseStatusException
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(exception.statusCode)
            .body(mapOf("message" to (exception.reason ?: "Error de solicitud")))
    }
}