package app.matty.api.auth.web

import app.matty.api.BaseApiIntegrationTest
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import app.matty.api.verification.VerificationCode
import app.matty.api.verification.VerificationCodeRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class LoginHandlerTest : BaseApiIntegrationTest() {
    private val user = User(
        fullName = "john doe",
        email = "johndoe@matty.dev",
        interests = emptyList(),
        id = UUID.randomUUID().toString()
    )

    private val verificationCode = VerificationCode(
        code = "1234",
        destination = user.email,
        expiresAt = Instant.now().plusSeconds(60),
        submitted = false
    )

    @Autowired
    private lateinit var verificationCodeRepository: VerificationCodeRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        userRepository.insert(user)
        verificationCodeRepository.add(verificationCode)
    }

    @AfterEach
    fun tearDown() {
        userRepository.delete(user)
        verificationCodeRepository.remove(verificationCode)
    }

    @Test
    fun `should not generate verification code if user not found`() {
        mockMvc.perform(get("/login/code?email=usernotfound@matty.dev"))
            .andExpect(status().is4xxClientError)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.USER_NOT_FOUND.name)))
    }

    @Test
    fun `should not generate verification code if active code already exists`() {
        mockMvc.perform(get("/login/code?email=${user.email}"))
            .andExpect(status().is4xxClientError)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.VERIFICATION_CODE_EXISTS.name)))
    }

    @Test
    fun `should generate verification code`() {
        verificationCodeRepository.remove(verificationCode)

        mockMvc.perform(get("/login/code?email=${user.email}"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.expiresAt").isNotEmpty)
    }

    @Test
    fun `should not login if user not found`() {
        val email = "usernotfound@matty.dev"
        val requestBody = objectMapper.writeValueAsString(
            LoginRequest(
                email = email,
                verificationCode = verificationCode.code
            )
        )

        mockMvc.perform(
            post("/login").content(requestBody).header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        ).andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.USER_NOT_FOUND.name)))
    }

    @Test
    fun `should not login if verification code has already been submitted`() {
        val requestBody = objectMapper.writeValueAsString(
            LoginRequest(
                email = user.email,
                verificationCode = verificationCode.code
            )
        )

        verificationCodeRepository.update(verificationCode.copy(submitted = true))

        mockMvc.perform(
            post("/login").content(requestBody).header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        ).andExpect(status().is4xxClientError)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.VERIFICATION_CODE_INVALID.name)))
    }

    @Test
    fun `should not login if verification code does not exist`() {
        val requestBody = objectMapper.writeValueAsString(
            LoginRequest(
                email = user.email,
                verificationCode = "non-existent verification code"
            )
        )

        mockMvc.perform(
            post("/login").content(requestBody).header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        ).andExpect(status().is4xxClientError)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.VERIFICATION_CODE_INVALID.name)))
    }


    @Test
    fun `should login`() {
        val requestBody = objectMapper.writeValueAsString(
            LoginRequest(
                email = user.email,
                verificationCode = verificationCode.code
            )
        )

        mockMvc.perform(
            post("/login").header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).content(requestBody)
        ).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
    }
}
