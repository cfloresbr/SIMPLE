package cl.simple.api

import cl.simple.model.*
import cl.simple.repo.AlertRepository
import cl.simple.repo.AlertStatusHistoryRepository
import cl.simple.repo.UserRepository
import cl.simple.security.currentEmail
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.text.Normalizer

// Keep the response shape compatible with the current Android client.
data class AlertReq(
    val type: String,
    val level: String,
    val message: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)
data class AlertDto(
    val id: Long,
    val type: String,
    val level: String,
    val message: String,
    val status: String,
    val createdAt: String,
    val latitude: Double?,
    val longitude: Double?
)
data class AlertHistoryDto(
    val id: Long,
    val previousStatus: String?,
    val newStatus: String,
    val changedBy: Long,
    val comment: String,
    val changedAt: String
)
data class AlertActionReq(val comment: String? = null)

@RestController
@RequestMapping("/api/alerts")
class AlertController(
    private val alerts: AlertRepository,
    private val users: UserRepository,
    private val history: AlertStatusHistoryRepository
) {
    @GetMapping
    fun list(): List<AlertDto> = alerts.findAllByOrderByCreatedAtDesc().map(::dto)

    /**
     * Public accounts can submit a fire report. Earthquake reports remain unverified
     * submissions only; this endpoint does not implement sensor-based earthquake detection.
     * Institutional accounts can create institutional alerts, including marejadas.
     */
    @PostMapping
    @Transactional
    fun create(@RequestBody r: AlertReq): AlertDto {
        val actor = authenticatedUser()
        val type = normalizeType(r.type)
        if (type.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de alerta es obligatorio")
        if (r.message.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje es obligatorio")
        if (r.message.length > 1000) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje supera los 1000 caracteres")
        if ((r.latitude == null) != (r.longitude == null)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar latitud y longitud juntas")
        }
        if (r.latitude != null && r.longitude != null) {
            if (r.latitude !in -90.0..90.0 || r.longitude !in -180.0..180.0) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las coordenadas no son válidas"
                )
            }
        }

        val isPublicReport = actor.role == Role.USER
        if (isPublicReport && type !in setOf("INCENDIO", "TERREMOTO")) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta pública solo puede reportar incendios o enviar reportes no verificados de terremoto"
            )
        }
        if (type == "MAREJADAS" && actor.role !in setOf(Role.INSTITUTION, Role.ADMIN)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo una institución autorizada puede activar alertas de marejadas")
        }

        val level = try {
            AlertLevel.valueOf(normalizeType(r.level))
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel inválido. Usa PREVENTIVA, AMARILLA, ROJA o INFORMATIVA")
        }
        val now = LocalDateTime.now()
        val initialStatus = if (isPublicReport) AlertStatus.PENDING else AlertStatus.ACTIVE
        val saved = alerts.save(
            Alert(
                type = type,
                level = level,
                message = r.message.trim(),
                status = initialStatus,
                latitude = r.latitude,
                longitude = r.longitude,
                createdAt = now,
                createdBy = actor.id,
                updatedAt = now,
                updatedBy = actor.id,
                activatedAt = if (initialStatus == AlertStatus.ACTIVE) now else null
            )
        )
        history.save(
            AlertStatusHistory(
                alertId = saved.id,
                previousStatus = null,
                newStatus = saved.status.name,
                changedBy = actor.id,
                comment = if (isPublicReport) "Reporte recibido; requiere revisión institucional" else "Alerta creada por cuenta institucional/administrativa",
                changedAt = now
            )
        )
        return dto(saved)
    }

    // Backward-compatible route: keeps VALIDATED as a review state, not public activation.
    @PostMapping("/{id}/validate")
    @Transactional
    fun validate(@PathVariable id: Long, @RequestBody(required = false) req: AlertActionReq?): AlertDto =
        changeStatus(id, AlertStatus.VALIDATED, req?.comment ?: "Reporte validado por institución", setOf(AlertStatus.PENDING))

    // Institutional approval of a public report activates it.
    @PostMapping("/{id}/approve")
    @Transactional
    fun approve(@PathVariable id: Long, @RequestBody(required = false) req: AlertActionReq?): AlertDto =
        changeStatus(id, AlertStatus.ACTIVE, req?.comment ?: "Alerta aprobada y activada por institución", setOf(AlertStatus.PENDING, AlertStatus.VALIDATED))

    @PostMapping("/{id}/activate")
    @Transactional
    fun activate(@PathVariable id: Long, @RequestBody(required = false) req: AlertActionReq?): AlertDto =
        changeStatus(id, AlertStatus.ACTIVE, req?.comment ?: "Alerta activada por institución", setOf(AlertStatus.VALIDATED))

    @PostMapping("/{id}/reject")
    @Transactional
    fun reject(@PathVariable id: Long, @RequestBody(required = false) req: AlertActionReq?): AlertDto =
        changeStatus(id, AlertStatus.REJECTED, req?.comment ?: "Reporte rechazado por institución", setOf(AlertStatus.PENDING, AlertStatus.VALIDATED))

    @PostMapping("/{id}/resolve")
    @Transactional
    fun resolve(@PathVariable id: Long, @RequestBody(required = false) req: AlertActionReq?): AlertDto =
        changeStatus(id, AlertStatus.RESOLVED, req?.comment ?: "Emergencia marcada como resuelta", setOf(AlertStatus.ACTIVE))

    @GetMapping("/{id}/history")
    fun getHistory(@PathVariable id: Long): List<AlertHistoryDto> {
        authenticatedUser()
        if (!alerts.existsById(id)) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta no encontrada")
        return history.findAllByAlertIdOrderByChangedAtDesc(id).map {
            AlertHistoryDto(it.id, it.previousStatus, it.newStatus, it.changedBy, it.comment, it.changedAt.toString())
        }
    }

    private fun changeStatus(
        id: Long,
        target: AlertStatus,
        comment: String,
        allowedCurrent: Set<AlertStatus>
    ): AlertDto {
        val actor = authenticatedUser()
        if (actor.role !in setOf(Role.INSTITUTION, Role.ADMIN)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Esta acción requiere permisos institucionales")
        }
        val alert = alerts.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta no encontrada")
        }
        if (alert.status !in allowedCurrent) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar ${alert.status} a $target")
        }
        val previous = alert.status
        val now = LocalDateTime.now()
        alert.status = target
        alert.updatedAt = now
        alert.updatedBy = actor.id
        if (target == AlertStatus.ACTIVE) alert.activatedAt = now
        if (target == AlertStatus.RESOLVED) alert.resolvedAt = now
        val saved = alerts.save(alert)
        history.save(
            AlertStatusHistory(
                alertId = saved.id,
                previousStatus = previous.name,
                newStatus = target.name,
                changedBy = actor.id,
                comment = comment.take(500),
                changedAt = now
            )
        )
        return dto(saved)
    }

    private fun authenticatedUser(): User {
        val email = currentEmail() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión")
        return users.findByEmail(email) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no encontrada")
    }

    private fun normalizeType(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .uppercase()
        .replace(' ', '_')

    private fun dto(a: Alert) = AlertDto(
        a.id, a.type, a.level.name, a.message, a.status.name, a.createdAt.toString(), a.latitude, a.longitude
    )
}
