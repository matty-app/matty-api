package app.matty.api.auth

import app.matty.api.auth.data.RefreshTokenRepository
import app.matty.api.auth.exc.InvalidTokenException
import app.matty.api.auth.exc.TokenServiceException
import app.matty.api.auth.exc.UnidentifiedRefreshToken
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional

class TokenServiceTest {
    private val accessToken = "access-token-#"
    private val refreshToken = "refresh-token-#"
    private val user = User(
        fullName = "john doe",
        email = "mail@mail.com",
        interests = emptyList(),
        id = "userid"
    )

    @Test
    fun `should not emmit tokens if userid is null`() {
        val tokenService = TokenService(
            tokenGenerator = mockk(),
            tokenDataExtractor = mockk(),
            refreshTokenRepository = mockk(),
            userRepository = mockk()
        )
        val invalidUser = user.copy(id = null)

        assertThrows<TokenServiceException> {
            tokenService.emmitTokens(invalidUser)
        }
    }

    @Test
    fun `should emmit tokens`() {
        val tokenGenerator = mockk<TokenGenerator> {
            every { generateAccessToken(user.id!!, any()) } returns accessToken
            every { generateRefreshToken(user.id!!) } returns refreshToken
        }
        val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxed = true)
        val tokenService = TokenService(
            tokenGenerator = tokenGenerator,
            tokenDataExtractor = mockk(),
            refreshTokenRepository = refreshTokenRepository,
            userRepository = mockk()
        )

        val tokenPair = tokenService.emmitTokens(user)

        assertEquals(accessToken, tokenPair.accessToken, "Access token")
        assertEquals(refreshToken, tokenPair.refreshToken, "Refresh token")
        verify {
            refreshTokenRepository.insert(refreshToken, user.id!!)
        }

    }

    @Test
    fun `should not refresh tokens if RT is not exist`() {
        val refreshTokenRepository = mockk<RefreshTokenRepository> {
            every { exists(refreshToken) } returns false
        }
        val tokenService = TokenService(
            tokenGenerator = mockk(),
            tokenDataExtractor = mockk(relaxed = true),
            refreshTokenRepository = refreshTokenRepository,
            userRepository = mockk()
        )

        assertThrows<UnidentifiedRefreshToken> {
            tokenService.refreshTokens(refreshToken)
        }
    }

    @Test
    fun `should not refresh tokens if user is not exist`() {
        val refreshTokenRepository = mockk<RefreshTokenRepository> {
            every { exists(refreshToken) } returns true
        }
        val userRepository = mockk<UserRepository> {
            every { findById(user.id!!) } returns Optional.empty()
        }
        val tokenDataExtractor = mockk<TokenDataExtractor>() {
            every { getUserIdFromRefreshToken(refreshToken) } returns user.id
        }
        val tokenService = TokenService(
            tokenGenerator = mockk(),
            tokenDataExtractor = tokenDataExtractor,
            refreshTokenRepository = refreshTokenRepository,
            userRepository = userRepository
        )

        assertThrows<TokenServiceException> {
            tokenService.refreshTokens(refreshToken)
        }
    }

    @Test
    fun `should not refresh tokens if RT is invalid`() {
        val user = createUser()
        val refreshTokenRepository = mockk<RefreshTokenRepository> {
            every { exists(refreshToken) } returns true
        }
        val userRepository = mockk<UserRepository> {
            every { findById(user.id!!) } returns Optional.of(user)
        }
        val tokenDataExtractor = mockk<TokenDataExtractor> {
            every { getUserIdFromRefreshToken(refreshToken) } throws InvalidTokenException("Invalid refresh token")
        }
        val tokenService = TokenService(
            tokenGenerator = mockk(),
            tokenDataExtractor = tokenDataExtractor,
            refreshTokenRepository = refreshTokenRepository,
            userRepository = userRepository
        )

        assertThrows<InvalidTokenException> {
            tokenService.refreshTokens(refreshToken)
        }
    }

    @Test
    fun `should delete prev refresh token`() {
        val refreshToken = "refresh-token-#"
        val user = createUser()
        val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxed = true) {
            every { exists(refreshToken) } returns true
        }
        val userRepository = mockk<UserRepository> {
            every { findById(user.id!!) } returns Optional.of(user)
        }
        val tokenDataExtractor = mockk<TokenDataExtractor> {
            every { getUserIdFromRefreshToken(refreshToken) } returns user.id
        }
        val tokenService = TokenService(
            tokenGenerator = mockk(relaxed = true),
            tokenDataExtractor = tokenDataExtractor,
            refreshTokenRepository = refreshTokenRepository,
            userRepository = userRepository
        )

        tokenService.refreshTokens(refreshToken)

        verify(exactly = 1) {
            refreshTokenRepository.delete(refreshToken)
        }
    }

    private fun createUser(id: String? = "userid") = User(
        fullName = "john doe",
        email = "mail@mail.com",
        interests = emptyList(),
        id = id
    )
}
