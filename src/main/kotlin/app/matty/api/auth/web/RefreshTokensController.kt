package app.matty.api.auth.web

import app.matty.api.auth.token.TokenService
import app.matty.api.auth.token.data.TokenPair
import app.matty.api.auth.token.exception.TokenServiceException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/refresh")
class RefreshTokensController(
    private val tokenService: TokenService
) {
    @GetMapping
    fun refreshAuthTokens(@RequestParam("refresh_token") refreshToken: String): ResponseEntity<TokenPair> {
        return try {
            val tokenPair = tokenService.refreshTokens(refreshToken)
            ResponseEntity.ok(tokenPair)
        } catch (e: TokenServiceException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}
