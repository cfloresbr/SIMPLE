package cl.simple.api

import cl.simple.model.AlertStatus
import cl.simple.model.Role
import cl.simple.repo.AlertRepository
import cl.simple.repo.UserRepository
import cl.simple.security.currentEmail
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val alerts: AlertRepository,
    private val users: UserRepository
) {

    @GetMapping("/summary")
    fun summary(): ResponseEntity<Any> {
        val email = currentEmail()
        val user = email?.let { users.findByEmail(it) }

        if (user == null) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Cuenta no encontrada"))
        }

        if (user.role !in setOf(Role.INSTITUTION, Role.ADMIN)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                    mapOf(
                        "message" to
                                "El resumen institucional requiere permisos institucionales"
                    )
                )
        }

        val all = alerts.findAll()

        val summary: Map<String, Any> = mapOf(
            "totalAlerts" to all.size,
            "pending" to all.count {
                it.status == AlertStatus.PENDING
            },
            "validated" to all.count {
                it.status == AlertStatus.VALIDATED
            },
            "active" to all.count {
                it.status == AlertStatus.ACTIVE
            },
            "rejected" to all.count {
                it.status == AlertStatus.REJECTED
            },
            "resolved" to all.count {
                it.status == AlertStatus.RESOLVED
            },
            "expired" to all.count {
                it.status == AlertStatus.EXPIRED
            }
        )

        return ResponseEntity.ok(summary)
    }
}

