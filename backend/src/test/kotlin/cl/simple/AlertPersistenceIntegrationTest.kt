package cl.simple

import cl.simple.model.Alert
import cl.simple.model.AlertLevel
import cl.simple.model.AlertStatus
import cl.simple.model.AlertStatusHistory
import cl.simple.model.Role
import cl.simple.model.User
import cl.simple.repo.AlertRepository
import cl.simple.repo.AlertStatusHistoryRepository
import cl.simple.repo.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class AlertPersistenceIntegrationTest @Autowired constructor(
    private val users: UserRepository,
    private val alerts: AlertRepository,
    private val history: AlertStatusHistoryRepository,
    private val entityManager: EntityManager
) {
    @Test
    @Transactional
    fun `alert and status history are stored and can be reloaded`() {
        val user = users.saveAndFlush(
            User(
                name = "Institutional test",
                email = "institution-${UUID.randomUUID()}@example.test",
                passwordHash = "test-only-hash",
                role = Role.INSTITUTION
            )
        )
        val createdAt = LocalDateTime.now().withNano(0)
        val alert = alerts.saveAndFlush(
            Alert(
                type = "INCENDIO",
                level = AlertLevel.PREVENTIVA.name,
                message = "Synthetic persistence test",
                status = AlertStatus.PENDING,
                createdAt = createdAt,
                createdBy = user.id
            )
        )
        val changedAt = LocalDateTime.now().withNano(0)
        history.saveAndFlush(
            AlertStatusHistory(
                alertId = alert.id,
                previousStatus = null,
                newStatus = AlertStatus.PENDING.name,
                changedBy = user.id,
                comment = "Isolated integration test",
                changedAt = changedAt
            )
        )

        entityManager.clear()

        val persistedAlert = alerts.findById(alert.id).orElseThrow()
        val persistedHistory = history.findAllByAlertIdOrderByChangedAtDesc(alert.id).single()

        assertEquals(AlertStatus.PENDING, persistedAlert.status)
        assertEquals("Synthetic persistence test", persistedAlert.message)
        assertEquals(user.id, persistedAlert.createdBy)
        assertEquals(alert.id, persistedHistory.alertId)
        assertEquals(AlertStatus.PENDING.name, persistedHistory.newStatus)
        assertEquals(user.id, persistedHistory.changedBy)
        assertNotNull(persistedHistory.changedAt)
    }
}
