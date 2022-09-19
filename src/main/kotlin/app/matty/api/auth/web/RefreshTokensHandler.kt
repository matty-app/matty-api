package app.matty.api.auth.web

import app.matty.api.auth.token.TokenService
import app.matty.api.auth.token.exception.TokenServiceException
import app.matty.api.common.web.ApiHandler
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.ServerResponse.ok
import org.springframework.web.servlet.function.ServerResponse.status

@ApiHandler
class RefreshTokensHandler(
    private val tokenService: TokenService
) {
    fun refreshAuthTokens(request: ServerRequest): ServerResponse {
        val refreshToken = request.param("refresh_token").orElseThrow { ResponseStatusException(BAD_REQUEST) }
        return try {
            val tokenPair = tokenService.refreshTokens(refreshToken)
            ok().body(tokenPair)
        } catch (e: TokenServiceException) {
            status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}
