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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.Optional
import java.util.stream.Stream

data class TransitionCase(
    val label: String,
    val path: String,
    val initial: AlertStatus,
    val expected: AlertStatus
)

@WebMvcTest(
    controllers = [AlertController::class, ReportController::class],
    properties = [
        "app.jwt.secret=test-only-secret-that-is-long-enough-for-hmac256",
        "app.jwt.expiration-ms=3600000"
    ]
)
@Import(SecurityConfig::class, JwtService::class)
class AlertTransitionMatrixTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @MockitoBean
    private lateinit var alerts: AlertRepository

    @MockitoBean
    private lateinit var history: AlertStatusHistoryRepository

    @MockitoBean
    private lateinit var users: UserRepository

    @org.junit.jupiter.api.BeforeEach
    fun prepareInstitution() {
        `when`(users.findByEmail(ACTOR_EMAIL))
            .thenReturn(User(id = ACTOR_ID, email = ACTOR_EMAIL, role = Role.INSTITUTION))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allowedTransitions")
    @WithMockUser(username = ACTOR_EMAIL, roles = ["INSTITUTION"])
    fun `allowed transition updates alert and writes audit history`(case: TransitionCase) {
        val alert = alert(case.initial)
        `when`(alerts.findById(ALERT_ID)).thenReturn(Optional.of(alert))
        `when`(alerts.save(any(Alert::class.java))).thenAnswer { it.getArgument<Alert>(0) }
        `when`(history.save(any(AlertStatusHistory::class.java))).thenAnswer { it.getArgument<AlertStatusHistory>(0) }

        mvc.perform(
            post(case.path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"matriz fase 2.2\"}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value(case.expected.name))

        assertEquals(case.expected, alert.status)
        assertNotNull(alert.updatedAt)
        assertEquals(ACTOR_ID, alert.updatedBy)
        if (case.expected == AlertStatus.ACTIVE) assertNotNull(alert.activatedAt)
        if (case.expected == AlertStatus.RESOLVED) assertNotNull(alert.resolvedAt)

        val captor = ArgumentCaptor.forClass(AlertStatusHistory::class.java)
        verify(history).save(captor.capture())
        assertEquals(case.initial.name, captor.value.previousStatus)
        assertEquals(case.expected.name, captor.value.newStatus)
        assertEquals(ACTOR_ID, captor.value.changedBy)
        assertEquals("matriz fase 2.2", captor.value.comment)
        verify(alerts).save(alert)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTransitions")
    @WithMockUser(username = ACTOR_EMAIL, roles = ["INSTITUTION"])
    fun `invalid transition returns conflict without writing alert or history`(case: TransitionCase) {
        `when`(alerts.findById(ALERT_ID)).thenReturn(Optional.of(alert(case.initial)))

        mvc.perform(
            post(case.path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"no debe guardarse\"}")
        ).andExpect(status().isConflict)

        verify(alerts, never()).save(any(Alert::class.java))
        verify(history, never()).save(any(AlertStatusHistory::class.java))
    }

    private fun alert(status: AlertStatus) = Alert(
        id = ALERT_ID,
        type = "INCENDIO",
        level = AlertLevel.PREVENTIVA,
        message = "Alerta de prueba de transiciones",
        status = status,
        createdAt = LocalDateTime.parse("2026-11-02T10:00:00"),
        createdBy = ACTOR_ID
    )

    companion object {
        private const val ACTOR_EMAIL = "institution.matrix@example.test"
        private const val ACTOR_ID = 17L
        private const val ALERT_ID = 92L

        @JvmStatic
        fun allowedTransitions(): Stream<Arguments> = Stream.of(
            Arguments.of(TransitionCase("PENDING -> VALIDATED", "/api/alerts/92/validate", AlertStatus.PENDING, AlertStatus.VALIDATED)),
            Arguments.of(TransitionCase("PENDING -> ACTIVE by approve", "/api/alerts/92/approve", AlertStatus.PENDING, AlertStatus.ACTIVE)),
            Arguments.of(TransitionCase("VALIDATED -> ACTIVE by approve", "/api/alerts/92/approve", AlertStatus.VALIDATED, AlertStatus.ACTIVE)),
            Arguments.of(TransitionCase("VALIDATED -> ACTIVE by activate", "/api/alerts/92/activate", AlertStatus.VALIDATED, AlertStatus.ACTIVE)),
            Arguments.of(TransitionCase("PENDING -> REJECTED", "/api/alerts/92/reject", AlertStatus.PENDING, AlertStatus.REJECTED)),
            Arguments.of(TransitionCase("VALIDATED -> REJECTED", "/api/alerts/92/reject", AlertStatus.VALIDATED, AlertStatus.REJECTED)),
            Arguments.of(TransitionCase("ACTIVE -> RESOLVED", "/api/alerts/92/resolve", AlertStatus.ACTIVE, AlertStatus.RESOLVED))
        )

        @JvmStatic
        fun invalidTransitions(): Stream<Arguments> {
            val routes = listOf(
                "/validate" to ("/api/alerts/92/validate" to setOf(AlertStatus.PENDING)),
                "/approve" to ("/api/alerts/92/approve" to setOf(AlertStatus.PENDING, AlertStatus.VALIDATED)),
                "/activate" to ("/api/alerts/92/activate" to setOf(AlertStatus.VALIDATED)),
                "/reject" to ("/api/alerts/92/reject" to setOf(AlertStatus.PENDING, AlertStatus.VALIDATED)),
                "/resolve" to ("/api/alerts/92/resolve" to setOf(AlertStatus.ACTIVE))
            )
            return routes.flatMap { (action, routeAndAllowed) ->
                val (path, allowedStates) = routeAndAllowed
                AlertStatus.values()
                    .filterNot { it in allowedStates }
                    .map { state -> Arguments.of(TransitionCase("$action rejects $state", path, state, state)) }
            }.stream()
        }
    }
}
