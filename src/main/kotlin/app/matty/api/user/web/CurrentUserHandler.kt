package app.matty.api.user.web

import app.matty.api.common.getUserIdOrThrow
import app.matty.api.common.web.ApiHandler
import app.matty.api.user.data.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.ServerResponse.ok

@ApiHandler
class CurrentUserHandler(
    private val userRepository: UserRepository
) {
    fun me(request: ServerRequest): ServerResponse {
        val userId = getUserIdOrThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED) }
        val user = userRepository.findById(userId).orElseThrow {
            throw ResponseStatusException(INTERNAL_SERVER_ERROR)
        }
        return ok().body(user)
    }
}
