package app.matty.api.auth.token

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class TokenAuthenticationProvider(
    private val accessJwtDecoder: JwtDecoder
) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication {
        val token = ((authentication as? TokenAuthentication)
            ?: throw IllegalArgumentException("Must be of type TokenAuthentication!")).token
        val tokenData = accessJwtDecoder.decode(token)
        return TokenAuthentication(token, userId = tokenData.subject)
    }

    override fun supports(authentication: Class<*>?): Boolean = authentication == TokenAuthentication::class.java
}
