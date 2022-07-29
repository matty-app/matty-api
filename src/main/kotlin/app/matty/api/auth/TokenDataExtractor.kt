package app.matty.api.auth

import app.matty.api.auth.config.TokensConfiguration
import app.matty.api.auth.exc.InvalidTokenException
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(TokenDataExtractor::class.java)

@Component
class TokenDataExtractor(configuration: TokensConfiguration) {
    private val refreshTokenVerifier = JWT.require(configuration.refreshTokenAlgorithm).build()
    private val accessTokenVerifier = JWT.require(configuration.accessTokenAlgorithm).build()

    fun getUserIdFromRefreshToken(token: String): String? = refreshTokenVerifier.getFromToken(token) { subject }

    fun getUserIdFromAccessToken(token: String): String? = accessTokenVerifier.getFromToken(token) { subject }

    private inline fun <T> JWTVerifier.getFromToken(token: String, dataExtractor: DecodedJWT.() -> T): T {
        return try {
            verify(token).run(dataExtractor)
        } catch (e: JWTVerificationException) {
            log.error("Token '${token}' is invalid!", e)
            throw InvalidTokenException("Token '${token}' is invalid!")
        }
    }
}