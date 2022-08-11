package app.matty.api.auth.token

import app.matty.api.auth.token.data.TokenData
import app.matty.api.auth.token.exception.InvalidTokenException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(JwtDecoder::class.java)

class JwtDecoder(
    private val jwtVerifier: JWTVerifier
) {
    fun decode(token: String): TokenData {
        log.debug("Decoding token: $token")
        val jwt = try {
            jwtVerifier.verify(token)
        } catch (e: JWTVerificationException) {
            log.debug("The token has not been verified!", e)
            throw InvalidTokenException("Token '${token}' is invalid!", e)
        }
        return TokenData(subject = jwt.subject)
    }
}
