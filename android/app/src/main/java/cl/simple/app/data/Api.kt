package cl.simple.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String, val age: Int?, val disability: String?, val education: String?)
data class AuthResponse(val token: String, val user: UserDto)
data class UserDto(val id: Long, val name: String, val email: String, val role: String)
data class AlertRequest(
    val type: String,
    val message: String,
    val latitude: Double?,
    val longitude: Double?
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
    val createdBy: Long? = null,
    val updatedAt: String? = null,
    val updatedBy: Long? = null,
    val activatedAt: String? = null,
    val resolvedAt: String? = null,
    val expiresAt: String? = null
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


data class AlertActionRequest(
    val comment: String? = null
)

data class AlertHistoryDto(
    val id: Long,
    val previousStatus: String?,
    val newStatus: String,
    val changedBy: Long,
    val comment: String,
    val changedAt: String
)

data class ReportSummaryDto(
    val totalAlerts: Int,
    val pending: Int,
    val validated: Int,
    val active: Int,
    val rejected: Int,
    val resolved: Int,
    val expired: Int
)

interface SimpleApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @GET("api/alerts")
    suspend fun alerts(@Header("Authorization") token: String): List<AlertDto>

    @GET("api/alerts/public")
    suspend fun publicAlerts(): PublicAlertFeedDto

    @POST("api/alerts")
    suspend fun createAlert(@Header("Authorization") token: String, @Body body: AlertRequest): AlertDto

    @POST("api/alerts/{id}/validate")
    suspend fun validateAlert(@Header("Authorization") token: String, @Path("id") id: Long): AlertDto

    @POST("api/alerts/{id}/approve")
    suspend fun approveAlert(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body body: AlertActionRequest
    ): AlertDto

    @POST("api/alerts/{id}/activate")
    suspend fun activateAlert(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body body: AlertActionRequest
    ): AlertDto

    @POST("api/alerts/{id}/reject")
    suspend fun rejectAlert(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body body: AlertActionRequest
    ): AlertDto

    @POST("api/alerts/{id}/resolve")
    suspend fun resolveAlert(
        @Header("Authorization") token: String,
        @Path("id") id: Long,
        @Body body: AlertActionRequest
    ): AlertDto

    @GET("api/alerts/{id}/history")
    suspend fun alertHistory(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): List<AlertHistoryDto>

    @GET("api/reports/summary")
    suspend fun summary(@Header("Authorization") token: String): ReportSummaryDto
}

object ApiClient {
    // Emulator -> host machine. For a physical phone replace with the PC's LAN IP.
    private const val BASE_URL = "http://10.0.2.2:8080/"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: SimpleApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SimpleApi::class.java)
}
