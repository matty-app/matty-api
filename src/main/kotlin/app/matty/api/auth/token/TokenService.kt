package app.matty.api.auth.token

import app.matty.api.auth.token.data.RefreshTokenRepository
import app.matty.api.auth.token.data.TokenPair
import app.matty.api.auth.token.exception.InvalidTokenException
import app.matty.api.auth.token.exception.TokenServiceException
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val log = LoggerFactory.getLogger(TokenService::class.java)

@Service
class TokenService(
    private val jwtGenerator: JwtGenerator,
    private val refreshJwtDecoder: JwtDecoder,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository
) {
    fun emitTokens(user: User): TokenPair {
        if (user.id == null) {
            throwTokenServiceException("User.id can't be null!")
        }
        val tokenPair = TokenPair(
            accessToken = jwtGenerator.generateAccessToken(
                userId = user.id, claims = mapOf(
                    EMAIL_CLAIM to user.email,
                    FULL_NAME_CLAIM to user.fullName
                )
            ), refreshToken = jwtGenerator.generateRefreshToken(user.id)
        )
        refreshTokenRepository.insert(tokenPair.refreshToken, user.id)
        return tokenPair
    }

    fun refreshTokens(refreshToken: String): TokenPair {
        val userId = try {
            refreshJwtDecoder.decode(refreshToken).subject
        } catch (e: InvalidTokenException) {
            throwTokenServiceException("Invalid refresh token", e)
        }

        if (!refreshTokenRepository.exists(refreshToken)) {
            throwTokenServiceException("Refresh token not found in storage!")
        }
        val user = userRepository.findById(userId).orElseThrow {
            throwTokenServiceException("User does not exist!")
        }

        refreshTokenRepository.delete(refreshToken)

        return emitTokens(user)
    }

    private fun throwTokenServiceException(message: String, cause: Throwable? = null): Nothing {
        log.error(message)
        throw TokenServiceException(message, cause)
    }
}

private const val EMAIL_CLAIM = "email"
private const val FULL_NAME_CLAIM = "fullName"
