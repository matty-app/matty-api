package app.matty.api.security

import app.matty.api.account.data.User
import app.matty.api.account.data.UserRepository
import app.matty.api.security.data.RefreshToken
import app.matty.api.security.data.TokenPair
import app.matty.api.security.exc.InvalidTokenException
import app.matty.api.security.exc.TokenServiceException
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class TokenService(
    private val tokenGenerator: TokenGenerator,
    private val tokenDataExtractor: TokenDataExtractor,
    private val mongoOperations: MongoOperations,
    private val userRepository: UserRepository
) {
    fun createTokens(user: User): TokenPair {
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
        storeRefreshToken(user.id, tokenPair.refreshToken)
        return tokenPair
    }

    fun refreshTokens(refreshToken: String): TokenPair {
        val userId = tokenDataExtractor.getUserIdFromRefreshToken(refreshToken)
            ?: throw InvalidTokenException("Invalid refresh token!")

        val query = Query.query(Criteria.where("_id").`is`(refreshToken))
        if (!mongoOperations.exists(query, DB_REFRESH_TOKENS_COLLECTION)) {
            throw TokenServiceException("Invalid refresh token!")
        }

        mongoOperations.remove(query)

        val user = userRepository.findById(userId).orElseThrow {
            throw TokenServiceException("User not found!")
        }

        return createTokens(user)
    }

    private fun storeRefreshToken(userId: String, token: String) {
        mongoOperations.insert(
            RefreshToken(
                id = token,
                userId = userId
            ),
            DB_REFRESH_TOKENS_COLLECTION
        )
    }
}

private const val EMAIL_CLAIM = "email"
private const val FULL_NAME_CLAIM = "fullName"
private const val DB_REFRESH_TOKENS_COLLECTION = "REFRESH_TOKENS"