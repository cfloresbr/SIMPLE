package cl.simple

import cl.simple.api.AlertController
import cl.simple.api.ReportController
import cl.simple.model.Alert
import cl.simple.model.AlertLevel
import cl.simple.model.AlertStatus
import cl.simple.model.AlertStatusHistory
import cl.simple.model.Role
import cl.simple.model.User
import cl.simple.repo.AlertRepository
import cl.simple.repo.AlertStatusHistoryRepository
import cl.simple.repo.UserRepository
import cl.simple.security.JwtService
import cl.simple.security.SecurityConfig
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(
    controllers = [AlertController::class, ReportController::class],
    properties = [
        "app.jwt.secret=test-only-secret-that-is-long-enough-for-hmac256",
        "app.jwt.expiration-ms=3600000"
    ]
)
@Import(SecurityConfig::class, JwtService::class)
class AlertApiTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @MockitoBean
    private lateinit var alerts: AlertRepository

    @MockitoBean
    private lateinit var history: AlertStatusHistoryRepository

    @MockitoBean
    private lateinit var users: UserRepository

    @BeforeEach
    fun prepareUsers() {
        `when`(users.findByEmail("user@example.test"))
            .thenReturn(User(id = 7, email = "user@example.test", role = Role.USER))
        `when`(users.findByEmail("institution@example.test"))
            .thenReturn(User(id = 12, email = "institution@example.test", role = Role.INSTITUTION))
    }

    @Test
    @WithMockUser(username = "user@example.test", roles = ["USER"])
    fun `public user cannot approve an alert`() {
        mvc.perform(
            post("/api/alerts/41/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"revisado\"}")
        ).andExpect(status().isForbidden)

        verify(alerts, never()).findById(41)
        verify(history, never()).save(any(AlertStatusHistory::class.java))
    }

    @Test
    @WithMockUser(username = "user@example.test", roles = ["USER"])
    fun `public user cannot read institutional summary`() {
        mvc.perform(get("/api/reports/summary"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @WithMockUser(username = "institution@example.test", roles = ["INSTITUTION"])
    fun `institution approves pending alert and records history`() {
        val pending = alert(AlertStatus.PENDING)
        `when`(alerts.findById(41)).thenReturn(Optional.of(pending))
        `when`(alerts.save(any(Alert::class.java))).thenAnswer { it.getArgument<Alert>(0) }
        `when`(history.save(any(AlertStatusHistory::class.java))).thenAnswer { it.getArgument<AlertStatusHistory>(0) }

        mvc.perform(
            post("/api/alerts/41/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Validado para activación\"}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        assertEquals(AlertStatus.ACTIVE, pending.status)
        assertNotNull(pending.activatedAt)
        val historyCaptor = ArgumentCaptor.forClass(AlertStatusHistory::class.java)
        verify(history).save(historyCaptor.capture())
        assertEquals("PENDING", historyCaptor.value.previousStatus)
        assertEquals("ACTIVE", historyCaptor.value.newStatus)
        assertEquals(12L, historyCaptor.value.changedBy)
        assertEquals("Validado para activación", historyCaptor.value.comment)
    }

    @Test
    @WithMockUser(username = "institution@example.test", roles = ["INSTITUTION"])
    fun `institution cannot resolve a pending alert`() {
        `when`(alerts.findById(41)).thenReturn(Optional.of(alert(AlertStatus.PENDING)))

        mvc.perform(
            post("/api/alerts/41/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isConflict)

        verify(alerts, never()).save(any(Alert::class.java))
        verify(history, never()).save(any(AlertStatusHistory::class.java))
    }

    @Test
    @WithMockUser(username = "user@example.test", roles = ["USER"])
    fun `public user can create alert without coordinates`() {
        stubAlertCreation()

        mvc.perform(
            post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertRequestJson("null", "null"))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.latitude").value(nullValue()))
            .andExpect(jsonPath("$.longitude").value(nullValue()))

        val savedCaptor = ArgumentCaptor.forClass(Alert::class.java)
        verify(alerts).save(savedCaptor.capture())
        assertNull(savedCaptor.value.latitude)
        assertNull(savedCaptor.value.longitude)
        verify(history).save(any(AlertStatusHistory::class.java))
    }

    @ParameterizedTest
    @CsvSource(
        "-90.0, -180.0",
        "-90.0, 180.0",
        "90.0, -180.0",
        "90.0, 180.0"
    )
    @WithMockUser(username = "user@example.test", roles = ["USER"])
    fun `public user can create alert at inclusive coordinate boundaries`(
        latitude: Double,
        longitude: Double
    ) {
        stubAlertCreation()

        mvc.perform(
            post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertRequestJson(latitude.toString(), longitude.toString()))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.latitude").value(latitude))
            .andExpect(jsonPath("$.longitude").value(longitude))

        val savedCaptor = ArgumentCaptor.forClass(Alert::class.java)
        verify(alerts).save(savedCaptor.capture())
        assertEquals(latitude, savedCaptor.value.latitude)
        assertEquals(longitude, savedCaptor.value.longitude)
        verify(history).save(any(AlertStatusHistory::class.java))
    }

    @Test
    @WithMockUser(username = "user@example.test", roles = ["USER"])
    fun `public user cannot create alert with latitude only`() {
        mvc.perform(
            post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertRequestJson("-33.4", "null"))
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Debes enviar latitud y longitud juntas"))

        verify(alerts, never()).save(any(Alert::class.java))
        verify(history, never()).save(any(AlertStatusHistory::class.java))
    }

    @Test
    @WithMockUser(username = "user@example.test", roles = ["USER"])
    fun `public user cannot create alert with longitude only`() {
        mvc.perform(
            post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertRequestJson("null", "-70.6"))
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Debes enviar latitud y longitud juntas"))

        verify(alerts, never()).save(any(Alert::class.java))
        verify(history, never()).save(any(AlertStatusHistory::class.java))
    }

    @ParameterizedTest
    @CsvSource(
        "-90.0001, 0.0",
        "90.0001, 0.0",
        "0.0, -180.0001",
        "0.0, 180.0001"
    )
    @WithMockUser(username = "user@example.test", roles = ["USER"])
    fun `public user cannot create alert outside coordinate boundaries`(
        latitude: Double,
        longitude: Double
    ) {
        mvc.perform(
            post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(alertRequestJson(latitude.toString(), longitude.toString()))
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Las coordenadas no son válidas"))

        verify(alerts, never()).save(any(Alert::class.java))
        verify(history, never()).save(any(AlertStatusHistory::class.java))
    }

    private fun alertRequestJson(latitude: String, longitude: String) =
        """{"type":"INCENDIO","level":"PREVENTIVA","message":"Reporte de prueba","latitude":$latitude,"longitude":$longitude}"""

    private fun stubAlertCreation() {
        `when`(alerts.save(any(Alert::class.java))).thenAnswer { invocation ->
            invocation.getArgument<Alert>(0).apply { id = 42L }
        }
        `when`(history.save(any(AlertStatusHistory::class.java)))
            .thenAnswer { invocation -> invocation.getArgument<AlertStatusHistory>(0) }
    }

    private fun alert(status: AlertStatus) = Alert(
        id = 41,
        type = "INCENDIO",
        level = AlertLevel.PREVENTIVA,
        message = "Reporte de prueba",
        status = status,
        createdAt = LocalDateTime.parse("2026-11-02T10:00:00"),
        createdBy = 7
    )
}
