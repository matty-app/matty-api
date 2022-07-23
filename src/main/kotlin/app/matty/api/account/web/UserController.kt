package app.matty.api.account.web

import app.matty.api.account.data.User
import app.matty.api.account.data.UserRepository
import app.matty.api.security.TokenAuthentication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/user")
class UserController(
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