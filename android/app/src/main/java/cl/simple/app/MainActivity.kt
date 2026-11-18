package cl.simple.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.simple.app.data.*
import java.util.concurrent.CancellationException
import kotlinx.coroutines.launch


class SimpleViewModel : ViewModel() {
    var token by mutableStateOf<String?>(null)
    var user by mutableStateOf<UserDto?>(null)
    var alerts by mutableStateOf<List<AlertDto>>(emptyList())
    var publicFeed by mutableStateOf<PublicAlertFeedDto?>(null)
    var history by mutableStateOf<List<AlertHistoryDto>>(emptyList())
    var summary by mutableStateOf<ReportSummaryDto?>(null)
    var error by mutableStateOf<String?>(null)
    var alertsError by mutableStateOf<String?>(null)
    var summaryError by mutableStateOf<String?>(null)
    var historyError by mutableStateOf<String?>(null)
    var createError by mutableStateOf<String?>(null)
    var createSuccess by mutableStateOf<String?>(null)
    var publicFeedError by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    var alertsLoading by mutableStateOf(false)
    var publicFeedLoading by mutableStateOf(false)
    var summaryLoading by mutableStateOf(false)
    var historyLoading by mutableStateOf(false)

    val dashboardLoading: Boolean
        get() = alertsLoading || publicFeedLoading || summaryLoading

    val isBusy: Boolean
        get() = loading || dashboardLoading || historyLoading

    fun login(email: String, password: String) = viewModelScope.launch {
        loading = true
        error = null

        try {
            safeApiCall {
                ApiClient.api.login(LoginRequest(email, password))
            }.onSuccess {
                token = it.token
                user = it.user
                refreshDashboard()
            }.onFailure {
                error = it.toSimpleMessage(
                    fallback = "No fue posible iniciar sesión.",
                    unauthorizedMessage = "El correo o la contraseña no son correctos."
                )
            }
        } finally {
            loading = false
        }
    }

    fun register(name: String, email: String, password: String) =
        viewModelScope.launch {
            loading = true
            error = null

            try {
                safeApiCall {
                    ApiClient.api.register(
                        RegisterRequest(name, email, password, null, null, null)
                    )
                }.onSuccess {
                    token = it.token
                    user = it.user
                    refreshDashboard()
                }.onFailure {
                    error = it.toSimpleMessage("No fue posible registrar la cuenta.")
                }
            } finally {
                loading = false
            }
        }

    fun loadAlerts() = viewModelScope.launch {
        val currentToken = token ?: return@launch
        if (alertsLoading) return@launch
        alertsLoading = true

        try {
            safeApiCall {
                ApiClient.api.alerts("Bearer $currentToken")
            }.onSuccess {
                alerts = it
                alertsError = null
            }.onFailure {
                alertsError = it.toSimpleMessage("No fue posible cargar las alertas.")
            }
        } finally {
            alertsLoading = false
        }
    }

    fun loadSummary() = viewModelScope.launch {
        val currentToken = token ?: return@launch
        if (summaryLoading) return@launch
        val role = user?.role?.uppercase()
        if (role != "INSTITUTION" && role != "ADMIN") {
            summary = null
            summaryError = null
            return@launch
        }
        summaryLoading = true

        try {
            safeApiCall {
                ApiClient.api.summary("Bearer $currentToken")
            }.onSuccess {
                summary = it
                summaryError = null
            }.onFailure {
                summaryError = it.toSimpleMessage(
                    "No fue posible cargar el resumen institucional."
                )
            }
        } finally {
            summaryLoading = false
        }
    }

    fun loadPublicFeed() = viewModelScope.launch {
        if (publicFeedLoading) return@launch
        publicFeedLoading = true

        try {
            safeApiCall {
                ApiClient.api.publicAlerts()
            }.onSuccess {
                publicFeed = it
                publicFeedError = null
            }.onFailure {
                publicFeedError = it.toSimpleMessage("No fue posible consultar los avisos públicos.")
            }
        } finally {
            publicFeedLoading = false
        }
    }

    fun refreshDashboard() {
        loadAlerts()
        loadSummary()
        loadPublicFeed()
    }

    fun clearCreateSuccess() {
        createSuccess = null
    }

    fun createAlert(
        type: String,
        message: String,
        onSuccess: () -> Unit
    ) =
        viewModelScope.launch {
            val currentToken = token ?: return@launch

            loading = true
            createError = null

            try {
                safeApiCall {
                    ApiClient.api.createAlert(
                        "Bearer $currentToken",
                        AlertRequest(type, message, null, null)
                    )
                }.onSuccess {
                    val actorRole = user?.role?.uppercase()
                    createSuccess = if (actorRole == "USER") {
                        when (it.type.uppercase()) {
                            "TERREMOTO" -> "Reporte #${it.id} recibido por SIMPLE; aún no está verificado."
                            "INCENDIO_FORESTAL", "INCENDIO_ESTRUCTURAL", "INCENDIO" ->
                                "Reporte #${it.id} recibido por SIMPLE; pendiente de revisión. No se ha enviado a organismos de emergencia."
                            else -> "Reporte #${it.id} recibido por SIMPLE; pendiente de revisión."
                        }
                    } else {
                        "Registro interno #${it.id} guardado. No es un aviso público."
                    }
                    onSuccess()
                    refreshDashboard()
                }.onFailure {
                    createError = it.toSimpleMessage("No fue posible crear el reporte.")
                }
            } finally {
                loading = false
            }
        }

    private fun runAlertAction(
        id: Long,
        action: suspend (
            String,
            Long,
            AlertActionRequest
        ) -> AlertDto
    ) = viewModelScope.launch {
        val currentToken = token ?: return@launch

        loading = true
        error = null

        try {
            safeApiCall {
                action(
                    "Bearer $currentToken",
                    id,
                    AlertActionRequest()
                )
            }.onSuccess {
                refreshDashboard()
            }.onFailure {
                error = it.toSimpleMessage("No fue posible actualizar la alerta.")
            }
        } finally {
            loading = false
        }
    }

    fun approveAlert(id: Long) {
        runAlertAction(id) { auth, alertId, body ->
            ApiClient.api.approveAlert(auth, alertId, body)
        }
    }

    fun activateAlert(id: Long) {
        runAlertAction(id) { auth, alertId, body ->
            ApiClient.api.activateAlert(auth, alertId, body)
        }
    }

    fun rejectAlert(id: Long) {
        runAlertAction(id) { auth, alertId, body ->
            ApiClient.api.rejectAlert(auth, alertId, body)
        }
    }

    fun resolveAlert(id: Long) {
        runAlertAction(id) { auth, alertId, body ->
            ApiClient.api.resolveAlert(auth, alertId, body)
        }
    }

    fun loadAlertHistory(id: Long) = viewModelScope.launch {
        val currentToken = token ?: return@launch
        historyLoading = true

        try {
            safeApiCall {
                ApiClient.api.alertHistory("Bearer $currentToken", id)
            }.onSuccess {
                history = it
                historyError = null
            }.onFailure {
                historyError = it.toSimpleMessage("No fue posible consultar el historial.")
            }
        } finally {
            historyLoading = false
        }
    }

    private suspend fun <T> safeApiCall(request: suspend () -> T): Result<T> = try {
        Result.success(request())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    fun logout() {
        token = null
        user = null
        alerts = emptyList()
        publicFeed = null
        history = emptyList()
        summary = null
        error = null
        createError = null
        createSuccess = null
        publicFeedError = null
        alertsError = null
        summaryError = null
        historyError = null
    }
}

@Composable
fun SimpleApp(vm: SimpleViewModel) {
    var showPublicFeed by remember { mutableStateOf(false) }
    MaterialTheme {
        when {
            vm.token != null -> HomeScreen(vm)
            showPublicFeed -> {
                LaunchedEffect(Unit) { vm.loadPublicFeed() }
                PublicAlertsScreen(
                    vm = vm,
                    showMyReports = false,
                    onBack = { showPublicFeed = false }
                )
            }
            else -> LoginScreen(vm, onPublicFeed = { showPublicFeed = true })
        }
    }
}

@Composable
fun LoginScreen(vm: SimpleViewModel, onPublicFeed: () -> Unit) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SIMPLE", style = MaterialTheme.typography.displaySmall)
        Text("Sistema de Información y Monitoreo de Prevención y Emergencias")
        if (register) OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { if (register) vm.register(name, email, password) else vm.login(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (register) "Crear cuenta" else "Ingresar") }
        TextButton(onClick = { register = !register }) {
            Text(if (register) "Ya tengo una cuenta" else "Crear cuenta")
        }
        TextButton(onClick = onPublicFeed) {
            Text("Consultar avisos públicos")
        }
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (vm.loading) CircularProgressIndicator()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: SimpleViewModel) {
    var selectedSection by remember { mutableStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var selectedHistoryAlertId by remember { mutableStateOf<Long?>(null) }

    var type by remember { mutableStateOf("Incendio forestal") }
    var message by remember { mutableStateOf("") }

    val role = vm.user?.role?.uppercase()
    val canManageAlerts = role == "INSTITUTION" || role == "ADMIN"
    val isPublicUser = role == "USER"
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.createSuccess) {
        val successMessage = vm.createSuccess ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = successMessage,
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
        vm.clearCreateSuccess()
    }
    val alertTypes = if (canManageAlerts) {
        listOf("Incendio forestal", "Incendio estructural", "Terremoto", "Marejadas")
    } else {
        listOf("Incendio forestal", "Incendio estructural", "Terremoto")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            selectedSection == 2 -> "Configuración"
                            selectedSection == 1 && isPublicUser -> "Alertas"
                            selectedSection == 1 -> "Reportes"
                            isPublicUser -> "Información"
                            else -> "SIMPLE"
                        }
                    )
                },
                actions = {
                    TextButton(
                        onClick = vm::refreshDashboard,
                        enabled = !vm.dashboardLoading
                    ) {
                        Text("Actualizar")
                    }
                    TextButton(onClick = vm::logout) {
                        Text("Salir")
                    }
                }
            )
        },
        floatingActionButton = {
            val canCreateOnThisSection =
                (canManageAlerts && selectedSection == 0) ||
                    (isPublicUser && selectedSection == 0)
            if (canCreateOnThisSection) {
                FloatingActionButton(
                    onClick = {
                        vm.createError = null
                        vm.clearCreateSuccess()
                        showCreate = true
                    }
                ) {
                    Text("+")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    icon = { Text(if (isPublicUser) "i" else "⌂") },
                    label = { Text(if (isPublicUser) "Información" else "Inicio") }
                )
                NavigationBarItem(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    icon = { Text("▤") },
                    label = { Text(if (isPublicUser) "Alertas" else "Reportes") }
                )
                NavigationBarItem(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    icon = { Text("⚙") },
                    label = { Text("Configuración") }
                )
            }
        }
    ) { pad ->
        when (selectedSection) {
            0 -> if (isPublicUser) {
                PublicInformationScreen(Modifier.padding(pad))
            } else LazyColumn(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                Text(
                    text = "Hola, ${vm.user?.name ?: "persona usuaria"}",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (role) {
                        "ADMIN" -> "Panel de administración"
                        "INSTITUTION" -> "Panel institucional de emergencias"
                        else -> "Reporte y consulta de emergencias"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Reportes internos: ${vm.alerts.size}",
                    style = MaterialTheme.typography.titleMedium
                )

                if (vm.isBusy) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                vm.error?.let { errorMessage ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                vm.alertsError?.let { alertsError ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(alertsError, color = MaterialTheme.colorScheme.error)
                    TextButton(
                        onClick = { vm.loadAlerts() },
                        enabled = !vm.alertsLoading
                    ) {
                        Text("Reintentar carga de alertas")
                    }
                }
            }

            if (canManageAlerts) {
                vm.summaryError?.let { summaryError ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    summaryError,
                                    color = MaterialTheme.colorScheme.error
                                )
                                TextButton(
                                    onClick = { vm.loadSummary() },
                                    enabled = !vm.summaryLoading
                                ) {
                                    Text("Reintentar resumen")
                                }
                            }
                        }
                    }
                }

                vm.summary?.let { report ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Resumen institucional", style = MaterialTheme.typography.titleMedium)
                                Text("Total de alertas: ${report.totalAlerts}")
                                Text("Pendientes: ${report.pending} · Validadas: ${report.validated}")
                                Text("Activas internamente: ${report.active} · Rechazadas: ${report.rejected}")
                                Text("Resueltas: ${report.resolved} · Vencidas: ${report.expired}")
                            }
                        }
                    }
                }
            }

            if (vm.alerts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No hay alertas para mostrar.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(vm.alerts) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${alertTypeLabel(alert.type)} · ${alertClassificationLabel(alert.level)}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(alert.message)

                        Text(
                            text = "Estado interno: ${reportStatusLabel(alert.status)}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "Fecha: ${alert.createdAt}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (
                            alert.latitude != null &&
                            alert.longitude != null
                        ) {
                            Text(
                                text = "Ubicación: ${alert.latitude}, ${alert.longitude}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }


                        if (canManageAlerts) {
                            val status = alert.status
                                .trim()
                                .uppercase()

                            HorizontalDivider()

                            Text(
                                text = "Gestión institucional",
                                style = MaterialTheme.typography.titleSmall
                            )

                            when (status) {
                                "PENDING" -> {
                                    TextButton(
                                        onClick = { vm.approveAlert(alert.id) },
                                        enabled = !vm.isBusy
                                    ) {
                                        Text("Aprobar para gestión interna")
                                    }

                                    TextButton(
                                        onClick = { vm.rejectAlert(alert.id) },
                                        enabled = !vm.isBusy
                                    ) {
                                        Text("Rechazar reporte")
                                    }
                                }

                                "VALIDATED" -> {
                                    TextButton(
                                        onClick = { vm.activateAlert(alert.id) },
                                        enabled = !vm.isBusy
                                    ) {
                                        Text("Marcar activa internamente")
                                    }

                                    TextButton(
                                        onClick = { vm.rejectAlert(alert.id) },
                                        enabled = !vm.isBusy
                                    ) {
                                        Text("Rechazar reporte")
                                    }
                                }

                                "ACTIVE" -> {
                                    TextButton(
                                        onClick = { vm.resolveAlert(alert.id) },
                                        enabled = !vm.isBusy
                                    ) {
                                        Text("Marcar como resuelta")
                                    }
                                }

                                "REJECTED", "RESOLVED", "EXPIRED" -> {
                                    // Estados finales: no mostrar acciones adicionales.
                                }

                                else -> {
                                    Text(
                                        text = "Estado sin acciones disponibles",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            // El historial queda disponible en todos los estados.
                            TextButton(
                                onClick = {
                                    vm.history = emptyList()
                                    vm.historyError = null
                                    selectedHistoryAlertId = alert.id
                                    showHistory = true
                                    vm.loadAlertHistory(alert.id)
                                },
                                enabled = !vm.isBusy
                            ) {
                                Text("Consultar historial")
                            }
                        }

                    }
                }
            }
            }
            1 -> if (isPublicUser) {
                PublicAlertsScreen(vm, Modifier.padding(pad))
            } else {
                ReportsScreen(vm, canManageAlerts, Modifier.padding(pad))
            }
            else -> SettingsScreen(vm, Modifier.padding(pad))
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Reportar emergencia") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Describe la emergencia para que pueda ser revisada."
                    )

                    vm.createError?.let { createError ->
                        Text(
                            createError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    SelectField(
                        label = "Tipo de emergencia",
                        selected = type,
                        options = alertTypes,
                        onSelected = { type = it }
                    )

                    Text(
                        text = when (type) {
                            "Incendio estructural" ->
                                "SIMPLE aún no avisa a Bomberos. Si hay fuego activo, llama al 132."
                            "Incendio forestal" ->
                                "SIMPLE aún no envía avisos. Para un foco, llama al 130 o al 132."
                            "Terremoto" ->
                                "Reporte manual; SIMPLE no detecta sismos por sensores."
                            "Marejadas" ->
                                "Registro interno; no es un aviso oficial."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Descripción") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (type.isNotBlank() && message.isNotBlank()) {
                            vm.createAlert(
                                type.trim(),
                                message.trim()
                            ) {
                                showCreate = false
                                message = ""
                            }
                        }
                    },
                    enabled = !vm.loading &&
                            type.isNotBlank() &&
                            message.isNotBlank()
                ) {
                    Text(if (vm.createError == null) "Enviar a SIMPLE" else "Reintentar registro")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreate = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("Historial de la alerta") },
            text = {
                when {
                    vm.historyLoading -> CircularProgressIndicator()
                    vm.historyError != null -> Column {
                        Text(
                            vm.historyError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = {
                                selectedHistoryAlertId?.let { alertId ->
                                    vm.loadAlertHistory(alertId)
                                }
                            },
                            enabled = !vm.historyLoading
                        ) {
                            Text("Reintentar")
                        }
                    }
                    vm.history.isEmpty() -> {
                        Text(
                            "No hay registros en el historial de esta alerta."
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(vm.history) { entry ->
                                Column(
                                    verticalArrangement =
                                        Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${entry.previousStatus ?: "Sin estado previo"} → ${entry.newStatus}",
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    Text("Responsable: ${entry.changedBy}")
                                    Text("Fecha: ${entry.changedAt}")

                                    if (entry.comment.isNotBlank()) {
                                        Text("Comentario: ${entry.comment}")
                                    }

                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHistory = false }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

private data class SafetyArticle(
    val title: String,
    val introduction: String,
    val recommendations: List<String>,
    val sourceName: String,
    val sourceUrl: String
)

private val safetyArticles = listOf(
    SafetyArticle(
        title = "Sismos: protégete durante el movimiento",
        introduction = "Identifica de antemano un lugar de protección sísmica y mantén la calma.",
        recommendations = listOf(
            "Protégete y afírmate debajo de un elemento firme; si no puedes, ubícate junto a él.",
            "En la calle, aléjate de edificios, postes y cables eléctricos.",
            "Si estás en la costa y el sismo te dificulta mantenerte en pie, evacúa de inmediato hacia un punto de encuentro o área de seguridad."
        ),
        sourceName = "SENAPRED · Sismos",
        sourceUrl = "https://www.senapred.cl/sismos/"
    ),
    SafetyArticle(
        title = "Incendios forestales: prevén y aléjate del fuego",
        introduction = "El fuego puede propagarse rápidamente. No intentes exponerte para combatirlo.",
        recommendations = listOf(
            "Aléjate del avance del fuego y del humo, y sigue las instrucciones de las autoridades.",
            "Si debes evacuar, SENAPRED recomienda hacerlo a pie cuando sea posible.",
            "Ante un foco, avisa a CONAF (130), Bomberos (132) o Carabineros (133); no supongas que otra persona ya llamó."
        ),
        sourceName = "SENAPRED · Incendios forestales",
        sourceUrl = "https://www.senapred.cl/incendios-forestales/"
    ),
    SafetyArticle(
        title = "Marejadas: mantén distancia del borde costero",
        introduction = "El oleaje y las rompientes pueden alcanzar roqueríos, playas y paseos costeros.",
        recommendations = listOf(
            "No te acerques a roqueríos ni interactúes con las rompientes.",
            "No ingreses al mar ni realices actividades náuticas o deportivas sin autorización.",
            "Infórmate por los avisos de la Autoridad Marítima y respeta cierres y señalizaciones."
        ),
        sourceName = "DIRECTEMAR · Seguridad ante marejadas",
        sourceUrl = "https://www.directemar.cl/temporada-estival-2024-2025-autoridad-maritima-llama-a-la-prevencion-y"
    ),
    SafetyArticle(
        title = "Prepara a tu familia antes de una emergencia",
        introduction = "Un plan acordado ayuda a que cada persona sepa qué hacer y dónde reunirse.",
        recommendations = listOf(
            "Reconoce las amenazas de tu entorno y define zonas seguras y rutas de evacuación.",
            "Acuerda roles, contactos y un punto de encuentro familiar.",
            "Prepara un kit de emergencia y practica el plan periódicamente."
        ),
        sourceName = "SENAPRED · Familia Preparada",
        sourceUrl = "https://www.senapred.cl/familia-preparada/"
    )
)

@Composable
private fun PublicInformationScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Infórmate y prepárate", style = MaterialTheme.typography.headlineSmall)
            Text("Recomendaciones para actuar antes y durante una emergencia.")
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Este contenido es educativo y preventivo; no constituye una alerta oficial. En una emergencia, sigue las instrucciones de las autoridades y consulta sus canales vigentes.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        items(safetyArticles) { article ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(article.title, style = MaterialTheme.typography.titleMedium)
                    Text(article.introduction)
                    article.recommendations.forEach { recommendation ->
                        Text("• $recommendation")
                    }
                    HorizontalDivider()
                    Text(article.sourceName, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { uriHandler.openUri(article.sourceUrl) }) {
                        Text("Consultar fuente oficial")
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicAlertsScreen(
    vm: SimpleViewModel,
    modifier: Modifier = Modifier,
    showMyReports: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            onBack?.let { back ->
                TextButton(onClick = back) { Text("Volver") }
            }
            Text("Avisos públicos", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (showMyReports) "Los avisos públicos y tus reportes tienen estados distintos."
                else "Avisos oficiales vigentes; no necesitas iniciar sesión para consultarlos."
            )
            TextButton(
                onClick = vm::loadPublicFeed,
                enabled = !vm.publicFeedLoading
            ) { Text("Actualizar avisos") }
        }

        if (vm.publicFeedLoading) item { CircularProgressIndicator() }

        vm.publicFeedError?.let { error ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(
                            onClick = vm::loadPublicFeed,
                            enabled = !vm.publicFeedLoading
                        ) { Text("Reintentar avisos públicos") }
                    }
                }
            }
        }

        vm.publicFeed?.let { feed ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (feed.state == "NOT_CONNECTED") {
                            Text("Fuentes oficiales no conectadas")
                        } else if (feed.alerts.isEmpty()) {
                            Text("No hay avisos públicos vigentes.")
                        }
                        Text(feed.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        vm.publicFeed?.alerts?.let { notices ->
            items(notices) { notice ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("${alertTypeLabel(notice.type)} · ${notice.title}", style = MaterialTheme.typography.titleMedium)
                        Text(notice.message)
                        Text("Fuente: ${notice.source}", style = MaterialTheme.typography.bodySmall)
                        Text("Cobertura: ${notice.area}", style = MaterialTheme.typography.bodySmall)
                        Text("Publicado: ${notice.publishedAt}", style = MaterialTheme.typography.bodySmall)
                        notice.validUntil?.let { Text("Vigente hasta: $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }

        if (showMyReports) {
            item {
                HorizontalDivider()
                Text("Mis reportes", style = MaterialTheme.typography.titleLarge)
                Text("Solo tú y el equipo interno autorizado pueden ver estos reportes.")
            }

            if (vm.alertsLoading) item { CircularProgressIndicator() }

            vm.alertsError?.let { error ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            TextButton(
                                onClick = vm::loadAlerts,
                                enabled = !vm.alertsLoading
                            ) { Text("Reintentar mis reportes") }
                        }
                    }
                }
            }

            if (vm.alerts.isEmpty() && !vm.alertsLoading) {
                item { Text("Aún no has enviado reportes.") }
            }

            items(vm.alerts) { alert ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("${alertTypeLabel(alert.type)} · ${alertClassificationLabel(alert.level)}", style = MaterialTheme.typography.titleMedium)
                        Text("Estado del reporte: ${reportStatusLabel(alert.status)}")
                        Text("Fecha: ${alert.createdAt}", style = MaterialTheme.typography.bodySmall)
                        Text(alert.message)
                        if (alert.latitude != null && alert.longitude != null) {
                            Text("Ubicación: ${alert.latitude}, ${alert.longitude}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen(
    vm: SimpleViewModel,
    canManageAlerts: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Reportes de emergencias", style = MaterialTheme.typography.headlineSmall)
            Text("Reportes internos: ${vm.alerts.size}")
        }

        if (canManageAlerts) {
            vm.summaryError?.let { error ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            TextButton(
                                onClick = vm::loadSummary,
                                enabled = !vm.summaryLoading
                            ) { Text("Reintentar resumen") }
                        }
                    }
                }
            }

            vm.summary?.let { summary ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Resumen institucional", style = MaterialTheme.typography.titleMedium)
                            Text("Pendientes: ${summary.pending} · Validadas: ${summary.validated}")
                            Text("Activas internamente: ${summary.active} · Rechazadas: ${summary.rejected}")
                            Text("Resueltas: ${summary.resolved} · Vencidas: ${summary.expired}")
                            Text("Total: ${summary.totalAlerts}")
                        }
                    }
                }
            }
        }

        if (vm.alertsLoading) {
            item { CircularProgressIndicator() }
        }

        vm.alertsError?.let { error ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(
                            onClick = vm::loadAlerts,
                            enabled = !vm.alertsLoading
                        ) { Text("Reintentar carga de reportes") }
                    }
                }
            }
        }

        if (vm.alerts.isEmpty() && !vm.alertsLoading) {
            item { Text("No hay reportes disponibles.") }
        }

        items(vm.alerts) { alert ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("${alertTypeLabel(alert.type)} · ${alertClassificationLabel(alert.level)}", style = MaterialTheme.typography.titleMedium)
                    Text("Estado interno: ${reportStatusLabel(alert.status)}")
                    Text("Fecha: ${alert.createdAt}", style = MaterialTheme.typography.bodySmall)
                        alert.createdBy?.let {
                            Text("Registrado por cuenta #$it", style = MaterialTheme.typography.bodySmall)
                        }
                        if (alert.updatedBy != null && alert.updatedBy != alert.createdBy) {
                            Text("Última gestión por cuenta #${alert.updatedBy}", style = MaterialTheme.typography.bodySmall)
                        }
                        alert.updatedAt?.takeIf { it != alert.createdAt }?.let {
                            Text("Última actualización: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        alert.activatedAt?.let {
                            Text("Activada en flujo interno: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        alert.resolvedAt?.let {
                            Text("Resuelta: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        alert.expiresAt?.let {
                            Text("Vigencia registrada hasta: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    Text(alert.message)
                    if (alert.latitude != null && alert.longitude != null) {
                        Text("Ubicación: ${alert.latitude}, ${alert.longitude}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: SimpleViewModel, modifier: Modifier = Modifier) {
    val user = vm.user
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Configuración", style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Cuenta", style = MaterialTheme.typography.titleMedium)
                Text("Nombre: ${user?.name.orEmpty()}")
                Text("Correo: ${user?.email.orEmpty()}")
                Text(
                    "Rol: ${when (user?.role?.uppercase()) {
                        "INSTITUTION" -> "Institución"
                        "ADMIN" -> "Administración"
                        "USER" -> "Público"
                        else -> "No disponible"
                    }}"
                )
            }
        }
        Text(
            "La edición de datos de cuenta y las preferencias de notificación se incorporarán en sus etapas correspondientes.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun alertTypeLabel(type: String): String = when (type.trim().uppercase()) {
    "INCENDIO" -> "Incendio (subtipo no especificado)"
    "INCENDIO_FORESTAL" -> "Incendio forestal"
    "INCENDIO_ESTRUCTURAL" -> "Incendio estructural"
    "TERREMOTO" -> "Terremoto"
    "MAREJADAS" -> "Marejadas"
    else -> type.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun alertClassificationLabel(level: String): String = when (level.trim().uppercase()) {
    "SIN_CLASIFICAR" -> "Sin clasificación oficial"
    else -> "Nivel legado: ${level.replace('_', ' ').lowercase()} (fuente no verificada)"
}

private fun reportStatusLabel(status: String): String = when (status.trim().uppercase()) {
    "PENDING" -> "Pendiente de revisión interna"
    "VALIDATED" -> "Revisado en SIMPLE"
    "ACTIVE" -> "Activo en flujo interno; no publicado"
    "REJECTED" -> "Rechazado en SIMPLE"
    "RESOLVED" -> "Cerrado en SIMPLE"
    "EXPIRED" -> "Vencido en SIMPLE; registro conservado"
    else -> status
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SimpleApp(SimpleViewModel()) }
    }
}
