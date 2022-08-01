package app.matty.api.auth

import app.matty.api.auth.data.RefreshTokenRepository
import app.matty.api.auth.data.TokenPair
import app.matty.api.auth.exc.InvalidTokenException
import app.matty.api.auth.exc.TokenServiceException
import app.matty.api.auth.exc.UnidentifiedRefreshToken
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import org.springframework.stereotype.Service

@Service
class TokenService(
    private val tokenGenerator: TokenGenerator,
    private val tokenDataExtractor: TokenDataExtractor,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository
) {
    fun emitTokens(user: User): TokenPair {
        if (user.id == null) {
            throw TokenServiceException("User.id can't be null!")
        }
        val tokenPair = TokenPair(
            accessToken = tokenGenerator.generateAccessToken(
                userId = user.id,
                claims = mapOf(
                    EMAIL_CLAIM to user.email,
                    FULL_NAME_CLAIM to user.fullName
                )
            ), refreshToken = tokenGenerator.generateRefreshToken(user.id)
        )
        refreshTokenRepository.insert(tokenPair.refreshToken, user.id)
        return tokenPair
    }

    fun refreshTokens(refreshToken: String): TokenPair {
        val userId = tokenDataExtractor.getUserIdFromRefreshToken(refreshToken)
            ?: throw InvalidTokenException("Invalid refresh token!")

        if (!refreshTokenRepository.exists(refreshToken)) {
            throw UnidentifiedRefreshToken()
        }
        val user = userRepository.findById(userId).orElseThrow {
            throw TokenServiceException("User does not exist!")
        }

        refreshTokenRepository.delete(refreshToken)

        return emitTokens(user)
    }
}

private const val EMAIL_CLAIM = "email"
private const val FULL_NAME_CLAIM = "fullName"
