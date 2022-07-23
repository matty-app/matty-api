package app.matty.api.security

import app.matty.api.security.config.TokensConfiguration
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TokenGenerator(
    private val tokensConfiguration: TokensConfiguration
) {
    fun generateAccessToken(userId: String, claims: Map<String, String>): String = generateToken(
        userId = userId,
        claims = claims,
        ttlInMillis = tokensConfiguration.accessTokenTtlInMillis,
        signAlgorithm = tokensConfiguration.accessTokenAlgorithm
    )

    fun generateRefreshToken(userId: String) = generateToken(
        userId = userId,
        claims = emptyMap(),
        ttlInMillis = tokensConfiguration.refreshTokenTtlInMillis,
        signAlgorithm = tokensConfiguration.refreshTokenAlgorithm
    )

    private fun generateToken(
        userId: String,
        claims: Map<String, String>,
        ttlInMillis: Long,
        signAlgorithm: Algorithm
    ): String {
        val expiresAt = Instant.now().plusMillis(ttlInMillis) //1 month
        return JWT.create()
            .withSubject(userId)
            .withExpiresAt(expiresAt)
            .apply {
                claims.forEach { (name, value) -> withClaim(name, value) }
            }
            .sign(signAlgorithm)
    }
}