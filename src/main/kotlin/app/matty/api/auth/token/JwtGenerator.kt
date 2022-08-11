package app.matty.api.auth.token

import app.matty.api.auth.config.TokensConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class JwtGenerator(
    private val tokensConfiguration: TokensConfig
) {
    fun generateAccessToken(userId: String, claims: Map<String, String>): String = generateToken(
        userId = userId,
        claims = claims,
        ttlInMillis = tokensConfiguration.accessTokenTtl,
        signAlgorithm = tokensConfiguration.accessTokenAlgorithm
    )

    fun generateRefreshToken(userId: String) = generateToken(
        userId = userId,
        claims = emptyMap(),
        ttlInMillis = tokensConfiguration.refreshTokenTtl,
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
