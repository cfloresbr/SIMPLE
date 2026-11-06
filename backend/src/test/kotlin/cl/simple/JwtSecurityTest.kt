package cl.simple

import cl.simple.api.AlertController
import cl.simple.api.ReportController
import cl.simple.model.Alert
import cl.simple.model.AlertLevel
import cl.simple.model.AlertStatus
import cl.simple.model.Role
import cl.simple.model.User
import cl.simple.repo.AlertRepository
import cl.simple.repo.AlertStatusHistoryRepository
import cl.simple.repo.UserRepository
import cl.simple.security.JwtService
import cl.simple.security.SecurityConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date

@WebMvcTest(
    controllers = [AlertController::class, ReportController::class],
    properties = [
        "app.jwt.secret=test-only-secret-that-is-long-enough-for-hmac256",
        "app.jwt.expiration-ms=3600000"
    ]
)
@Import(SecurityConfig::class, JwtService::class)
class JwtSecurityTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var jwt: JwtService

    @MockitoBean
    private lateinit var alerts: AlertRepository

    @MockitoBean
    private lateinit var history: AlertStatusHistoryRepository

    @MockitoBean
    private lateinit var users: UserRepository

    private val user = User(id = 5, name = "Usuario prueba", email = "user.jwt@example.test", role = Role.USER)
    private val institution = User(id = 7, name = "Institución prueba", email = "institution.jwt@example.test", role = Role.INSTITUTION)
    private val admin = User(id = 9, name = "Admin prueba", email = "admin.jwt@example.test", role = Role.ADMIN)

    @BeforeEach
    fun prepareUsers() {
        `when`(users.findByEmail(user.email)).thenReturn(user)
        `when`(users.findByEmail(institution.email)).thenReturn(institution)
        `when`(users.findByEmail(admin.email)).thenReturn(admin)
        `when`(alerts.findAll()).thenReturn(emptyList())
        `when`(alerts.findAllByOrderByCreatedAtDesc()).thenReturn(emptyList())
    }

    @Test
    fun `request without bearer token returns unauthorized`() {
        mvc.perform(get("/api/alerts"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `malformed bearer token returns unauthorized`() {
        mvc.perform(get("/api/alerts").header(HttpHeaders.AUTHORIZATION, "Bearer malformed.token.value"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `expired bearer token returns unauthorized`() {
        val expired = JWT.create()
            .withSubject(user.email)
            .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
            .sign(Algorithm.HMAC256(TEST_JWT_SECRET))

        mvc.perform(get("/api/alerts").header(HttpHeaders.AUTHORIZATION, "Bearer $expired"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `valid user token can list alerts but cannot use institutional endpoints`() {
        val bearer = "Bearer ${jwt.token(user)}"

        mvc.perform(get("/api/alerts").header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isOk)
        mvc.perform(get("/api/reports/summary").header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isForbidden)
        mvc.perform(
            post("/api/alerts/41/approve")
                .header(HttpHeaders.AUTHORIZATION, bearer)
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `valid institution token can access institutional summary`() {
        mvc.perform(
            get("/api/reports/summary")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwt.token(institution)}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalAlerts").value(0))
    }

    @Test
    fun `institution summary counts each alert status from repository results`() {
        val statuses = listOf(
            AlertStatus.PENDING,
            AlertStatus.ACTIVE,
            AlertStatus.ACTIVE,
            AlertStatus.ACTIVE,
            AlertStatus.ACTIVE,
            AlertStatus.REJECTED,
            AlertStatus.RESOLVED,
            AlertStatus.RESOLVED,
            AlertStatus.RESOLVED
        )
        `when`(alerts.findAll()).thenReturn(statuses.mapIndexed { index, state -> summaryAlert(index + 1L, state) })

        mvc.perform(
            get("/api/reports/summary")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwt.token(institution)}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalAlerts").value(9))
            .andExpect(jsonPath("$.pending").value(1))
            .andExpect(jsonPath("$.validated").value(0))
            .andExpect(jsonPath("$.active").value(4))
            .andExpect(jsonPath("$.rejected").value(1))
            .andExpect(jsonPath("$.resolved").value(3))
            .andExpect(jsonPath("$.expired").value(0))
    }

    @Test
    fun `valid admin token can access institutional summary`() {
        mvc.perform(
            get("/api/reports/summary")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwt.token(admin)}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalAlerts").value(0))
    }

    @Test
    fun `valid token for deleted account returns unauthorized`() {
        val token = jwt.token(User(email = "deleted.jwt@example.test"))
        `when`(users.findByEmail("deleted.jwt@example.test")).thenReturn(null)

        mvc.perform(get("/api/alerts").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isUnauthorized)
    }

    private fun summaryAlert(id: Long, status: AlertStatus) = Alert(
        id = id,
        type = "INCENDIO",
        level = AlertLevel.PREVENTIVA,
        message = "Alerta de resumen $id",
        status = status,
        createdAt = LocalDateTime.parse("2026-11-02T10:00:00")
    )

    companion object {
        private const val TEST_JWT_SECRET = "test-only-secret-that-is-long-enough-for-hmac256"
    }
}
