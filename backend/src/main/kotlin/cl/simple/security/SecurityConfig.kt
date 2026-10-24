package cl.simple.security

import cl.simple.repo.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

@Configuration
class SecurityConfig {
    @Bean
    fun filterChain(
        http: HttpSecurity,
        jwtFilter: JwtFilter
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Autenticación requerida"
                    )
                }
            }
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/**").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )
            .build()

}

@Component
class JwtFilter(private val jwt: JwtService, private val users: UserRepository): OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val header = req.getHeader("Authorization")
        if (header?.startsWith("Bearer ") == true) {
            try {
                val email = jwt.email(header.removePrefix("Bearer "))
                val user = users.findByEmail(email)
                if (user != null) {
                    val auth = UsernamePasswordAuthenticationToken(
                        org.springframework.security.core.userdetails.User(
                            user.email, user.passwordHash, listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                        ),
                        null, listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                    )
                    org.springframework.security.core.context.SecurityContextHolder.getContext().authentication = auth
                }
            } catch (_: Exception) {}
        }
        chain.doFilter(req,res)
    }
}
