package app.matty.api.auth

import app.matty.api.auth.config.TokensConfig
import app.matty.api.auth.token.JwtGenerator
import com.auth0.jwt.JWT
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class TokenGeneratorTest {
    private val config = TokensConfig(
        refreshTokenTtl = TEN_MINUTES,
        accessTokenTtl = FIVE_MINUTES,
        refreshTokenSecret = TOKEN_SECRET,
        accessTokenSecret = TOKEN_SECRET
    )
    private val tokenGenerator = JwtGenerator(config)
    private val userId = "userid"
    private val fixedTime = Instant.now()

    @BeforeEach
    fun setUp() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns fixedTime
    }

    @Test
    fun `should generate valid access JWT`() {
        val claims = mapOf(
            "fullName" to "Alexander B.", "team" to "Matty dev"
        )
        val expectedExpiration = fixedTime.plusMillis(config.accessTokenTtl)

        val token = tokenGenerator.generateAccessToken(userId, claims)

        assertTrue(token::isNotEmpty, "Token must not be empty!")

        val decodedToken = JWT.decode(token)

        assertEquals(userId, decodedToken.subject, "Token subject")
        assertEquals(claims["fullName"], decodedToken.claims["fullName"]?.asString(), "Token claims")
        assertEquals(claims["team"], decodedToken.claims["team"]?.asString(), "Token claims")
        //NumericDate has precision in seconds https://www.rfc-editor.org/rfc/rfc7519#section-4.1.4 https://www.rfc-editor.org/rfc/rfc7519#section-2
        assertEquals(
            expectedExpiration.epochSecond, decodedToken.expiresAtAsInstant.epochSecond, "Token expiration"
        )
    }


    @Test
    fun `should generate valid refresh JWT`() {
        val expectedExpiration = fixedTime.plusMillis(config.refreshTokenTtl)

        val token = tokenGenerator.generateRefreshToken(userId)

        assertTrue(token::isNotEmpty, "Token must not be empty!")

        val decodedToken = JWT.decode(token)

        assertEquals(userId, decodedToken.subject, "Token subject")
        assertEquals(expectedExpiration.epochSecond, decodedToken.expiresAtAsInstant.epochSecond, "Token expiration")
    }
}

private const val TOKEN_SECRET = "super secret string"
private const val FIVE_MINUTES = 300_000L
private const val TEN_MINUTES = FIVE_MINUTES * 2
