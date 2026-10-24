package cl.simple.security

import cl.simple.model.User
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*

@Service
class JwtService(@Value("\${app.jwt.secret}") secret: String,
                 @Value("\${app.jwt.expiration-ms}") private val expiration: Long) {
    private val algorithm = Algorithm.HMAC256(secret)
    fun token(user: User): String = JWT.create()
        .withSubject(user.email)
        .withClaim("role", user.role.name)
        .withExpiresAt(Date(System.currentTimeMillis()+expiration))
        .sign(algorithm)
    fun email(token: String): String = JWT.require(algorithm).build().verify(token).subject
}

object Passwords {
    val encoder = BCryptPasswordEncoder()
}

fun currentEmail(): String? {
    val a = SecurityContextHolder.getContext().authentication
    return if (a?.principal is UserDetails) (a.principal as UserDetails).username else a?.name
}
