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
    val message: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Deprecated compatibility field. New alerts are always saved as unclassified.
    val level: String? = null
)
data class AlertDto(
    val id: Long,
    val type: String,
    val level: String,
    val message: String,
    val status: String,
    val createdAt: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdBy: Long?,
    val updatedAt: String?,
    val updatedBy: Long?,
    val activatedAt: String?,
    val resolvedAt: String?,
    val expiresAt: String?
)
data class PublicAlertDto(
    val id: Long,
    val type: String,
    val title: String,
    val message: String,
    val source: String,
    val area: String,
    val publishedAt: String,
    val validUntil: String?
)
data class PublicAlertFeedDto(
    val state: String,
    val message: String,
    val alerts: List<PublicAlertDto>
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
    fun list(): List<AlertDto> {
        val actor = authenticatedUser()
        val visibleReports = when (actor.role) {
            Role.USER -> alerts.findAllByCreatedByOrderByCreatedAtDesc(actor.id)
            Role.INSTITUTION, Role.ADMIN -> alerts.findAllByOrderByCreatedAtDesc()
        }
        val includeActorIds = actor.role in setOf(Role.INSTITUTION, Role.ADMIN)
        return visibleReports.map { dto(it, includeActorIds) }
    }

    /** Public notices are intentionally empty until an authorized official source is connected. */
    @GetMapping("/public")
    fun publicFeed(): PublicAlertFeedDto {
        return PublicAlertFeedDto(
            state = "NOT_CONNECTED",
            message = "SIMPLE aún no está conectado a fuentes oficiales de alertas. Ante una emergencia, usa los canales oficiales.",
            alerts = emptyList()
        )
    }

    /**
     * Public accounts can submit a fire report. Earthquake reports remain unverified
     * submissions only; this endpoint does not implement sensor-based earthquake detection.
     * INSTITUTION is currently an internal moderator role, not a verified public authority.
     * Its records do not carry an official hazard classification or imply external dispatch.
     */
    @PostMapping
    @Transactional
    fun create(@RequestBody r: AlertReq): AlertDto {
        val actor = authenticatedUser()
        val type = normalizeType(r.type)
        if (type.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de alerta es obligatorio")
        val supportedTypes = setOf(
            "INCENDIO",
            "INCENDIO_FORESTAL",
            "INCENDIO_ESTRUCTURAL",
            "TERREMOTO",
            "MAREJADAS"
        )
        if (type !in supportedTypes) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de alerta no admitido")
        }
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
        if (isPublicReport && type !in setOf("INCENDIO", "INCENDIO_FORESTAL", "INCENDIO_ESTRUCTURAL", "TERREMOTO")) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta pública solo puede reportar incendios o enviar reportes no verificados de terremoto"
            )
        }
        if (type == "MAREJADAS" && actor.role !in setOf(Role.INSTITUTION, Role.ADMIN)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el rol institucional puede registrar marejadas en la bandeja interna")
        }

        // INSTITUTION is an internal moderator role, not an authority that may
        // assign an official hazard classification. Preserve existing levels,
        // but leave every new record explicitly unclassified.
        val level = AlertLevel.SIN_CLASIFICAR.name
        val now = LocalDateTime.now()
        // Every submission starts as an internal report. ACTIVE is not a public publication state.
        val initialStatus = AlertStatus.PENDING
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
                comment = if (isPublicReport) "Reporte recibido por SIMPLE; requiere revisión interna" else "Registro interno creado; requiere revisión y no constituye aviso público",
                changedAt = now
            )
        )
        return dto(saved, includeActorIds = actor.role in setOf(Role.INSTITUTION, Role.ADMIN))
    }

    // Internal moderation workflow only; these transitions do not publish a public notice.
    @PostMapping("/{id}/validate")
    @Transactional
    fun validate(@PathVariable id: Long, @RequestBody(required = false) req: AlertActionReq?): AlertDto =
        changeStatus(id, AlertStatus.VALIDATED, req?.comment ?: "Reporte revisado en la bandeja interna", setOf(AlertStatus.PENDING))

    // Internal approval changes the report workflow state, not the public alert feed.
    @PostMapping("/{id}/approve")
    @Transactional
    fun approve(@PathVariable id: Long, @RequestBody(required = false) req: AlertActionReq?): AlertDto =
        changeStatus(id, AlertStatus.ACTIVE, req?.comment ?: "Reporte aprobado para gestión interna", setOf(AlertStatus.PENDING, AlertStatus.VALIDATED))

    @PostMapping("/{id}/activate")
    @Transactional
    fun activate(@PathVariable id: Long, @RequestBody(required = false) req: AlertActionReq?): AlertDto =
        changeStatus(id, AlertStatus.ACTIVE, req?.comment ?: "Reporte activado en el flujo interno", setOf(AlertStatus.VALIDATED))

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
        val actor = authenticatedUser()
        val alert = alerts.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado")
        }
        if (actor.role == Role.USER && alert.createdBy != actor.id) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado")
        }
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
        return dto(saved, includeActorIds = true)
    }

    private fun authenticatedUser(): User {
        val email = currentEmail() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión")
        return users.findByEmail(email) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no encontrada")
    }

    private fun normalizeType(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .uppercase()
        .replace(' ', '_')

    private fun dto(a: Alert, includeActorIds: Boolean = true) = AlertDto(
        id = a.id,
        type = a.type,
        level = a.level,
        message = a.message,
        status = a.status.name,
        createdAt = a.createdAt.toString(),
        latitude = a.latitude,
        longitude = a.longitude,
        createdBy = a.createdBy.takeIf { includeActorIds },
        updatedAt = a.updatedAt?.toString(),
        updatedBy = a.updatedBy.takeIf { includeActorIds },
        activatedAt = a.activatedAt?.toString(),
        resolvedAt = a.resolvedAt?.toString(),
        expiresAt = a.expiresAt?.toString()
    )
}
