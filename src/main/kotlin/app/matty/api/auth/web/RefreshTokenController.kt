package app.matty.api.auth.web

import app.matty.api.auth.TokenAuthentication
import app.matty.api.auth.TokenService
import app.matty.api.auth.data.TokenPair
import app.matty.api.auth.exc.TokenServiceException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/auth/refresh")
class RefreshTokenController(
    private val tokenService: TokenService
) {
    @GetMapping
    fun refreshAuthTokens(): ResponseEntity<TokenPair> {
        val refreshToken = (SecurityContextHolder.getContext().authentication as? TokenAuthentication)?.token
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return try {
            val tokenPair = tokenService.refreshTokens(refreshToken)
            ResponseEntity.ok(tokenPair)
        } catch (e: TokenServiceException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}