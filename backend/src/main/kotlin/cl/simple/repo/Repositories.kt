package cl.simple.repo

import cl.simple.model.*
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}

interface AlertRepository : JpaRepository<Alert, Long> {
    fun findAllByOrderByCreatedAtDesc(): List<Alert>
}

interface AlertStatusHistoryRepository : JpaRepository<AlertStatusHistory, Long> {
    fun findAllByAlertIdOrderByChangedAtDesc(alertId: Long): List<AlertStatusHistory>
}
