package cl.simple.api

import cl.simple.model.User
import cl.simple.repo.UserRepository
import cl.simple.security.JwtService
import cl.simple.security.Passwords
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

data class LoginReq(val email:String, val password:String)
data class RegisterReq(val name:String, val email:String, val password:String, val age:Int?=null, val disability:String?=null, val education:String?=null)
data class AuthRes(val token:String, val user:UserDto)
data class UserDto(val id:Long, val name:String, val email:String, val role:String)

@RestController
@RequestMapping("/api/auth")
class AuthController(private val users:UserRepository, private val jwt:JwtService) {
    @PostMapping("/register")
    fun register(@RequestBody r:RegisterReq):AuthRes {
        if (users.findByEmail(r.email) != null) throw ResponseStatusException(HttpStatus.CONFLICT,"El correo ya está registrado")
        val u = users.save(User(name=r.name,email=r.email,passwordHash=Passwords.encoder.encode(r.password),
            age=r.age,disability=r.disability,education=r.education))
        return AuthRes(jwt.token(u),UserDto(u.id,u.name,u.email,u.role.name))
    }

    @PostMapping("/login")
    fun login(@RequestBody r:LoginReq):AuthRes {
        val u = users.findByEmail(r.email) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas")
        if (!Passwords.encoder.matches(r.password,u.passwordHash))
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas")
        return AuthRes(jwt.token(u),UserDto(u.id,u.name,u.email,u.role.name))
    }
}
