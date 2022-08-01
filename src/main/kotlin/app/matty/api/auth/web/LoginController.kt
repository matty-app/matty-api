package app.matty.api.auth.web

import app.matty.api.auth.TokenService
import app.matty.api.auth.web.LoginErrorCode.USER_NOT_FOUND
import app.matty.api.auth.web.LoginErrorCode.VERIFICATION_CODE_EXISTS
import app.matty.api.auth.web.LoginErrorCode.VERIFICATION_CODE_INVALID
import app.matty.api.auth.web.LoginResponseMessage.Error
import app.matty.api.auth.web.LoginResponseMessage.Success
import app.matty.api.auth.web.LoginResponseMessage.VerificationCode
import app.matty.api.user.data.UserRepository
import app.matty.api.verification.ActiveCodeAlreadyExists
import app.matty.api.verification.VerificationService
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/login")
class LoginController(
    val verificationService: VerificationService,
    val userRepository: UserRepository,
    val tokenService: TokenService
) {
    @GetMapping("/code")
    fun sendLoginCode(
        @RequestParam("email", required = true) email: String
    ): ResponseEntity<LoginResponseMessage> {
        if (!userRepository.existsByEmail(email)) {
            return ResponseEntity.status(NOT_FOUND)
                .body(Error(USER_NOT_FOUND))
        }
        val verificationCode = try {
            verificationService.generateAndSend(email)
        } catch (e: ActiveCodeAlreadyExists) {
            return ResponseEntity.badRequest().body(Error(VERIFICATION_CODE_EXISTS))
        }
        return ResponseEntity.ok(VerificationCode(expiresAt = verificationCode.expiresAt))
    }

    @PostMapping
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<LoginResponseMessage> {
        val (email, verificationCode) = loginRequest
        val user =
            userRepository.findByEmail(email) ?: return ResponseEntity.status(NOT_FOUND).body(Error(USER_NOT_FOUND))
        if (!verificationService.acceptCode(verificationCode, email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Error(VERIFICATION_CODE_INVALID))
        }
        val tokens = tokenService.emitTokens(user)
        return ResponseEntity.ok(
            Success(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken
            )
        )
    }
}

data class LoginRequest(val email: String, val verificationCode: String)

sealed class LoginResponseMessage {
    data class Error(val error: LoginErrorCode) : LoginResponseMessage()
    data class Success(val accessToken: String, val refreshToken: String) : LoginResponseMessage()
    data class VerificationCode(val expiresAt: Instant) : LoginResponseMessage()
}

enum class LoginErrorCode {
    USER_NOT_FOUND,
    VERIFICATION_CODE_INVALID,
    VERIFICATION_CODE_EXISTS
}
