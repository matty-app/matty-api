package app.matty.api.user.web

import app.matty.api.auth.getUserIdOrThrow
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
        val userId = getUserIdOrThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED) }
        val user = userRepository.findById(userId).orElseThrow {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return ResponseEntity.ok(user)
    }
}
