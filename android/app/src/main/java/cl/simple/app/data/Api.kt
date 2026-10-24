package cl.simple.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String, val age: Int?, val disability: String?, val education: String?)
data class AuthResponse(val token: String, val user: UserDto)
data class UserDto(val id: Long, val name: String, val email: String, val role: String)
data class AlertRequest(val type: String, val level: String, val message: String, val latitude: Double?, val longitude: Double?)
data class AlertDto(val id: Long, val type: String, val level: String, val message: String, val status: String, val createdAt: String, val latitude: Double?, val longitude: Double?)


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

interface SimpleApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @GET("api/alerts")
    suspend fun alerts(@Header("Authorization") token: String): List<AlertDto>

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
    suspend fun summary(@Header("Authorization") token: String): Map<String, Any>
}

object ApiClient {
    // Emulator -> host machine. For a physical phone replace with the PC's LAN IP.
    private const val BASE_URL = "http://10.0.2.2:8080/"
    val api: SimpleApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SimpleApi::class.java)
}
