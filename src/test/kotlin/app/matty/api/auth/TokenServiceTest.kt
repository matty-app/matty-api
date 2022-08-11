package app.matty.api.auth

import app.matty.api.auth.token.JwtDecoder
import app.matty.api.auth.token.JwtGenerator
import app.matty.api.auth.token.TokenService
import app.matty.api.auth.token.data.RefreshTokenRepository
import app.matty.api.auth.token.data.TokenData
import app.matty.api.auth.token.exception.InvalidTokenException
import app.matty.api.auth.token.exception.TokenServiceException
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
            jwtGenerator = mockk(),
            refreshJwtDecoder = mockk(),
            refreshTokenRepository = mockk(),
            userRepository = mockk()
        )
        val invalidUser = user.copy(id = null)

        assertThrows<TokenServiceException> {
            tokenService.emitTokens(invalidUser)
        }
    }

    @Test
    fun `should emmit tokens`() {
        val tokenGenerator = mockk<JwtGenerator> {
            every { generateAccessToken(user.id!!, any()) } returns accessToken
            every { generateRefreshToken(user.id!!) } returns refreshToken
        }
        val refreshTokenRepository = mockk<RefreshTokenRepository>(relaxed = true)
        val tokenService = TokenService(
            jwtGenerator = tokenGenerator,
            refreshJwtDecoder = mockk(),
            refreshTokenRepository = refreshTokenRepository,
            userRepository = mockk()
        )

        val tokenPair = tokenService.emitTokens(user)

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
            jwtGenerator = mockk(),
            refreshJwtDecoder = mockk(relaxed = true),
            refreshTokenRepository = refreshTokenRepository,
            userRepository = mockk()
        )

        assertThrows<TokenServiceException> {
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
        val refreshJwtDecoder = mockk<JwtDecoder> {
            every { decode(any()) } returns TokenData(subject = user.id!!)
        }
        val tokenService = TokenService(
            jwtGenerator = mockk(),
            refreshJwtDecoder = refreshJwtDecoder,
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
        val refreshJwtDecoder = mockk<JwtDecoder> {
            every { decode(refreshToken) } throws InvalidTokenException("Invalid refresh token")
        }
        val tokenService = TokenService(
            jwtGenerator = mockk(),
            refreshJwtDecoder = refreshJwtDecoder,
            refreshTokenRepository = refreshTokenRepository,
            userRepository = userRepository
        )

        assertThrows<TokenServiceException> {
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
        val refreshJwtDecoder = mockk<JwtDecoder> {
            every { decode(refreshToken) } returns TokenData(subject = user.id!!)
        }
        val tokenService = TokenService(
            jwtGenerator = mockk(relaxed = true),
            refreshJwtDecoder = refreshJwtDecoder,
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
