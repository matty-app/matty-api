package app.matty.api.user.web

import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import app.matty.api.auth.TokenAuthentication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/user")
class CurrentUserController(
    private val userRepository: UserRepository
) {
    @GetMapping("/me")
    fun me(): ResponseEntity<User> {
        val userId = (SecurityContextHolder.getContext().authentication as? TokenAuthentication)?.userId
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val user = userRepository.findById(userId).orElseThrow {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return ResponseEntity.ok(user)
    }
}