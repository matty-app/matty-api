package app.matty.api.auth.web

import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import app.matty.api.verification.data.Purpose.LOGIN
import app.matty.api.verification.data.ChannelType.EMAIL
import app.matty.api.verification.data.VerificationCode
import app.matty.api.verification.data.VerificationCodeRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(
    initializers = [LoginControllerTest.Companion.Initializer::class],
)
class LoginControllerTest {
    companion object {
        @Container
        var mongoDBContainer = MongoDBContainer("mongo")

        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(applicationContext: ConfigurableApplicationContext) {
                TestPropertyValues.of(
                    mapOf(
                        "spring.data.mongodb.uri" to mongoDBContainer.replicaSetUrl
                    )
                ).applyTo(applicationContext)
            }
        }
    }

    private val user = User(
        fullName = "john doe",
        email = "johndoe@matty.dev",
        phone = "12345678",
        interests = emptyList(),
        id = UUID.randomUUID().toString()
    )

    private val validCode = VerificationCode(
        code = "1234",
        destination = user.email!!,
        expiresAt = Instant.now().plusSeconds(60),
        channel = EMAIL,
        purpose = LOGIN,
        accepted = false,
        id = "validVerificationCode"
    )

    private val nonExistentUserCode = VerificationCode(
        code = "1234",
        destination = "user-not-found",
        expiresAt = Instant.now().plusSeconds(60),
        channel = EMAIL,
        purpose = LOGIN,
        accepted = false,
        id = "undefinedUserVerificationCode"
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
        verificationCodeRepository.add(validCode)
        verificationCodeRepository.add(nonExistentUserCode)
    }

    @AfterEach
    fun tearDown() {
        userRepository.delete(user)
        verificationCodeRepository.remove(validCode)
        verificationCodeRepository.remove(nonExistentUserCode)
    }

    @Test
    fun `should not generate email verification code if user not found`() {
        mockMvc.perform(get("/login/email/code?email=usernotfound@matty.dev"))
            .andExpect(status().is4xxClientError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.USER_NOT_FOUND.name)))
    }


    @Test
    fun `should not generate phone verification code if user not found`() {
        mockMvc.perform(get("/login/phone/code?phone=404"))
            .andExpect(status().is4xxClientError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.USER_NOT_FOUND.name)))
    }

    @Test
    fun `should not generate email verification code if active code already exists`() {
        mockMvc.perform(get("/login/email/code?email=${user.email}"))
            .andExpect(status().is4xxClientError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.VERIFICATION_CODE_EXISTS.name)))
    }

    @Test
    fun `should generate email verification code`() {
        verificationCodeRepository.remove(validCode)

        mockMvc.perform(get("/login/email/code?email=${user.email}"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.expiresAt").isNotEmpty)
            .andExpect(jsonPath("$.verificationId").isNotEmpty)
    }

    @Test
    fun `should not login by email if user not found`() {
        val requestBody = objectMapper.writeValueAsString(
            LoginRequest(
                verificationId = nonExistentUserCode.id!!,
                verificationCode = nonExistentUserCode.code
            )
        )

        mockMvc.perform(
            post("/login/email").content(requestBody).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.USER_NOT_FOUND.name)))
    }

    @Test
    fun `should not login if email verification code has already been submitted`() {
        val requestBody = objectMapper.writeValueAsString(
            LoginRequest(
                verificationId = validCode.id!!,
                verificationCode = validCode.code
            )
        )

        verificationCodeRepository.update(validCode.copy(accepted = true))

        mockMvc.perform(
            post("/login/email").content(requestBody).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.INVALID_VERIFICATION_CODE.name)))
    }

    @Test
    fun `should not login if verification code does not exist`() {
        val requestBody = objectMapper.writeValueAsString(
            LoginRequest(
                verificationId = validCode.id!!,
                verificationCode = "non-existent verification code"
            )
        )

        mockMvc.perform(
            post("/login/email").content(requestBody).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        ).andExpect(status().is4xxClientError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", `is`(LoginErrorCode.INVALID_VERIFICATION_CODE.name)))
    }


    @Test
    fun `should login`() {
        val requestBody = objectMapper.writeValueAsString(
            LoginRequest(
                verificationId = validCode.id!!,
                verificationCode = validCode.code
            )
        )

        mockMvc.perform(
            post("/login/email").header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).content(requestBody)
        ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
    }
}
