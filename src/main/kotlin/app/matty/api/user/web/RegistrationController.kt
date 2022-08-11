package app.matty.api.user.web

import app.matty.api.auth.token.TokenService
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import app.matty.api.user.web.RegistrationErrorCode.USER_EXISTS
import app.matty.api.user.web.RegistrationErrorCode.VERIFICATION_CODE_EXISTS
import app.matty.api.user.web.RegistrationErrorCode.VERIFICATION_CODE_INVALID
import app.matty.api.user.web.RegistrationResponseMessage.Error
import app.matty.api.user.web.RegistrationResponseMessage.Success
import app.matty.api.user.web.RegistrationResponseMessage.VerificationCode
import app.matty.api.verification.ActiveCodeAlreadyExists
import app.matty.api.verification.VerificationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/registration")
class RegistrationController(
    private val verificationService: VerificationService,
    private val userRepository: UserRepository,
    private val tokenService: TokenService
) {
    @GetMapping("/code")
    fun sendVerificationCode(
        @RequestParam("email", required = true) email: String
    ): ResponseEntity<RegistrationResponseMessage> {
        //TODO validate email
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Error(USER_EXISTS))
        }
        val verificationCode = try {
            verificationService.generateAndSend(email)
        } catch (e: ActiveCodeAlreadyExists) {
            return ResponseEntity.badRequest().body(Error(VERIFICATION_CODE_EXISTS))
        }
        return ResponseEntity.ok(VerificationCode(expiresAt = verificationCode.expiresAt))
    }

    @PostMapping
    fun register(@RequestBody regRequest: RegistrationRequest): ResponseEntity<RegistrationResponseMessage> {
        val (fullName, email, verificationCode) = regRequest
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Error(USER_EXISTS))
        }
        if (!verificationService.acceptCode(verificationCode, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Error(VERIFICATION_CODE_INVALID))
        }
        val newUser = userRepository.insert(
            User(
                fullName = fullName, email = email, interests = emptyList(), id = null
            )
        )
        val tokens = tokenService.emitTokens(newUser)
        return ResponseEntity.ok(
            Success(
                user = newUser,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken
            )
        )
    }
}

data class RegistrationRequest(val fullName: String, val email: String, val verificationCode: String)

sealed class RegistrationResponseMessage {
    data class Error(val error: RegistrationErrorCode) : RegistrationResponseMessage()
    data class Success(
        val user: User,
        val accessToken: String,
        val refreshToken: String
    ) : RegistrationResponseMessage()

    data class VerificationCode(val expiresAt: Instant) : RegistrationResponseMessage()
}

enum class RegistrationErrorCode {
    USER_EXISTS, VERIFICATION_CODE_EXISTS, VERIFICATION_CODE_INVALID
}
