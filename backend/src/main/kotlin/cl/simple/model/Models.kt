package cl.simple.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class Role { USER, INSTITUTION, ADMIN }
enum class AlertStatus { PENDING, VALIDATED, ACTIVE, REJECTED, RESOLVED, EXPIRED }
enum class AlertLevel { PREVENTIVA, AMARILLA, ROJA, INFORMATIVA }

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(nullable = false) var name: String = "",
    @Column(nullable = false, unique = true) var email: String = "",
    @Column(nullable = false) var passwordHash: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var role: Role = Role.USER,
    var age: Int? = null,
    var disability: String? = null,
    var education: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null
)

@Entity
@Table(name = "alerts")
class Alert(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(nullable = false, length = 100) var type: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var level: AlertLevel = AlertLevel.INFORMATIVA,
    @Column(nullable = false, length = 1000) var message: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: AlertStatus = AlertStatus.PENDING,
    var latitude: Double? = null,
    var longitude: Double? = null,
    @Column(nullable = false) var createdAt: LocalDateTime = LocalDateTime.now(),
    var createdBy: Long? = null,
    var updatedAt: LocalDateTime? = null,
    var updatedBy: Long? = null,
    var activatedAt: LocalDateTime? = null,
    var resolvedAt: LocalDateTime? = null,
    var expiresAt: LocalDateTime? = null
)

@Entity
@Table(name = "alert_status_history", indexes = [Index(name = "idx_alert_history_alert_id", columnList = "alert_id")])
class AlertStatusHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(name = "alert_id", nullable = false) var alertId: Long = 0,
    @Column(name = "previous_status", length = 30) var previousStatus: String? = null,
    @Column(name = "new_status", nullable = false, length = 30) var newStatus: String = "",
    @Column(name = "changed_by", nullable = false) var changedBy: Long = 0,
    @Column(nullable = false, length = 500) var comment: String = "",
    @Column(name = "changed_at", nullable = false) var changedAt: LocalDateTime = LocalDateTime.now()
)
