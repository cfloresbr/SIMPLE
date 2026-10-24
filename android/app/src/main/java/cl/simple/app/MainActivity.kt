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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.simple.app.data.*
import kotlinx.coroutines.launch


class SimpleViewModel : ViewModel() {
    var token by mutableStateOf<String?>(null)
    var user by mutableStateOf<UserDto?>(null)
    var alerts by mutableStateOf<List<AlertDto>>(emptyList())
    var history by mutableStateOf<List<AlertHistoryDto>>(emptyList())
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)

    fun login(email: String, password: String) = viewModelScope.launch {
        loading = true
        error = null

        runCatching {
            ApiClient.api.login(LoginRequest(email, password))
        }.onSuccess {
            token = it.token
            user = it.user
            loadAlerts()
        }.onFailure {
            error = it.message ?: "No fue posible iniciar sesión"
        }

        loading = false
    }

    fun register(name: String, email: String, password: String) =
        viewModelScope.launch {
            loading = true
            error = null

            runCatching {
                ApiClient.api.register(
                    RegisterRequest(name, email, password, null, null, null)
                )
            }.onSuccess {
                token = it.token
                user = it.user
                loadAlerts()
            }.onFailure {
                error = it.message ?: "No fue posible registrar la cuenta"
            }

            loading = false
        }

    fun loadAlerts() = viewModelScope.launch {
        val currentToken = token ?: return@launch

        runCatching {
            ApiClient.api.alerts("Bearer $currentToken")
        }.onSuccess {
            alerts = it
            error = null
        }.onFailure {
            error = it.message ?: "No fue posible cargar las alertas"
        }
    }

    fun createAlert(type: String, level: String, message: String) =
        viewModelScope.launch {
            val currentToken = token ?: return@launch

            loading = true
            error = null

            runCatching {
                ApiClient.api.createAlert(
                    "Bearer $currentToken",
                    AlertRequest(type, level, message, null, null)
                )
            }.onSuccess {
                loadAlerts()
            }.onFailure {
                error = it.message ?: "No fue posible crear el reporte"
            }

            loading = false
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

        runCatching {
            action(
                "Bearer $currentToken",
                id,
                AlertActionRequest()
            )
        }.onSuccess {
            loadAlerts()
        }.onFailure {
            error = it.message ?: "No fue posible actualizar la alerta"
        }

        loading = false
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

        runCatching {
            ApiClient.api.alertHistory("Bearer $currentToken", id)
        }.onSuccess {
            history = it
            error = null
        }.onFailure {
            error = it.message ?: "No fue posible consultar el historial"
        }
    }

    fun logout() {
        token = null
        user = null
        alerts = emptyList()
        history = emptyList()
        error = null
    }
}

@Composable
fun SimpleApp(vm: SimpleViewModel) {
    MaterialTheme {
        if (vm.token == null) LoginScreen(vm) else HomeScreen(vm)
    }
}

@Composable
fun LoginScreen(vm: SimpleViewModel) {
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
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (vm.loading) CircularProgressIndicator()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: SimpleViewModel) {
    var showCreate by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    var type by remember { mutableStateOf("Incendio") }
    var level by remember { mutableStateOf("Preventiva") }
    var message by remember { mutableStateOf("") }

    val role = vm.user?.role?.uppercase()
    val canManageAlerts = role == "INSTITUTION" || role == "ADMIN"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIMPLE") },
                actions = {
                    TextButton(onClick = { vm.loadAlerts() }) {
                        Text("Actualizar")
                    }
                    TextButton(onClick = vm::logout) {
                        Text("Salir")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true }
            ) {
                Text("+")
            }
        }
    ) { pad ->
        LazyColumn(
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
                    text = "Alertas registradas: ${vm.alerts.size}",
                    style = MaterialTheme.typography.titleMedium
                )

                if (vm.loading) {
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
                            text = "${alert.type} · ${alert.level}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(alert.message)

                        Text(
                            text = "Estado: ${alert.status}",
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
                                        enabled = !vm.loading
                                    ) {
                                        Text("Aprobar alerta")
                                    }

                                    TextButton(
                                        onClick = { vm.rejectAlert(alert.id) },
                                        enabled = !vm.loading
                                    ) {
                                        Text("Rechazar alerta")
                                    }
                                }

                                "VALIDATED" -> {
                                    TextButton(
                                        onClick = { vm.activateAlert(alert.id) },
                                        enabled = !vm.loading
                                    ) {
                                        Text("Activar alerta")
                                    }

                                    TextButton(
                                        onClick = { vm.rejectAlert(alert.id) },
                                        enabled = !vm.loading
                                    ) {
                                        Text("Rechazar alerta")
                                    }
                                }

                                "ACTIVE" -> {
                                    TextButton(
                                        onClick = { vm.resolveAlert(alert.id) },
                                        enabled = !vm.loading
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
                                    showHistory = true
                                    vm.loadAlertHistory(alert.id)
                                },
                                enabled = !vm.loading
                            ) {
                                Text("Consultar historial")
                            }
                        }

                    }
                }
            }
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

                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Tipo de emergencia") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = level,
                        onValueChange = { level = it },
                        label = { Text("Nivel") },
                        singleLine = true
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
                        if (
                            type.isNotBlank() &&
                            level.isNotBlank() &&
                            message.isNotBlank()
                        ) {
                            vm.createAlert(
                                type.trim(),
                                level.trim(),
                                message.trim()
                            )
                            showCreate = false
                            message = ""
                        }
                    },
                    enabled = !vm.loading &&
                            type.isNotBlank() &&
                            level.isNotBlank() &&
                            message.isNotBlank()
                ) {
                    Text("Enviar reporte")
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
                if (vm.history.isEmpty()) {
                    Text(
                        "No hay registros para mostrar o el historial todavía no se ha cargado."
                    )
                } else {
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SimpleApp(SimpleViewModel()) }
    }
}
