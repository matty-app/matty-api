package app.matty.api.security.config

import com.auth0.jwt.algorithms.Algorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
data class TokensConfiguration(
    @Value("\${app.tokens.refresh-token-ttl}")
    val refreshTokenTtlInMillis: Long,
    @Value("\${app.tokens.access-token-ttl}")
    val accessTokenTtlInMillis: Long,
    @Value("\${app.tokens.access-token-ttl}")
    val refreshTokenSecret: String,
    @Value("\${app.tokens.access-token-ttl}")
    val accessTokenSecret: String,
) {
    val refreshTokenAlgorithm: Algorithm by lazy { Algorithm.HMAC256(refreshTokenSecret) }
    val accessTokenAlgorithm: Algorithm by lazy { Algorithm.HMAC256(accessTokenSecret) }
}