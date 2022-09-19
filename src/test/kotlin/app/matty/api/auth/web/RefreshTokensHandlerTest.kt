package app.matty.api.auth.web

import app.matty.api.BaseApiIntegrationTest
import app.matty.api.auth.token.JwtGenerator
import app.matty.api.auth.token.data.RefreshTokenRepository
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokensHandlerTest : BaseApiIntegrationTest() {

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var jwtGenerator: JwtGenerator

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    private val userId = "userId"
    private val user = User(fullName = "John Doe", email = "johndoe@test.com", interests = emptyList(), id = userId)
    private var refreshToken: String? = null

    @BeforeEach
    fun setUp() {
        refreshToken = jwtGenerator.generateRefreshToken(userId)
        refreshTokenRepository.insert(refreshToken!!, userId)
        userRepository.insert(user)
    }

    @AfterEach
    fun tearDown() {
        refreshTokenRepository.delete(refreshToken!!)
        userRepository.delete(user)
    }

    @Test
    fun `refresh tokens`() {
        mockMvc.perform(get("/auth/refresh?refresh_token=$refreshToken"))
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.refreshToken").isNotEmpty)
    }

    @Test
    fun `refresh tokens with invalid access token in header`() {
        mockMvc.perform(
            get("/auth/refresh?refresh_token=$refreshToken").header(
                "Authorization",
                "Bearer invalid_token"
            )
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken").isNotEmpty)
            .andExpect(MockMvcResultMatchers.jsonPath("$.refreshToken").isNotEmpty)
    }

    @Test
    fun `invalid refresh token`() {
        mockMvc.perform(get("/auth/refresh?refresh_token=invalid_token"))
            .andExpect(status().isUnauthorized)
    }
}
