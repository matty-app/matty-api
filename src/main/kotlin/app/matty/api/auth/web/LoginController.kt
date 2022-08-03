package app.matty.api.auth.web

import app.matty.api.auth.TokenService
import app.matty.api.auth.web.LoginErrorCode.EMAIL_INVALID
import app.matty.api.auth.web.LoginErrorCode.USER_NOT_FOUND
import app.matty.api.auth.web.LoginErrorCode.VERIFICATION_CODE_EXISTS
import app.matty.api.auth.web.LoginErrorCode.VERIFICATION_CODE_INVALID
import app.matty.api.auth.web.LoginResponse.Error
import app.matty.api.auth.web.LoginResponse.Success
import app.matty.api.auth.web.LoginResponse.VerificationCode
import app.matty.api.common.isEmailNotValid
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import app.matty.api.verification.ActiveCodeAlreadyExists
import app.matty.api.verification.CodeAcceptanceResult.Accepted
import app.matty.api.verification.CodeAcceptanceResult.NotAccepted
import app.matty.api.verification.Purpose.LOGIN
import app.matty.api.verification.TransportType
import app.matty.api.verification.TransportType.EMAIL
import app.matty.api.verification.TransportType.SMS
import app.matty.api.verification.VerificationService
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.http.ResponseEntity.ok
import org.springframework.http.ResponseEntity.status
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
    val verificationService: VerificationService, val userRepository: UserRepository, val tokenService: TokenService
) {
    @GetMapping("/email/code")
    fun getEmailVerificationCode(
        @RequestParam("email", required = true) email: String,
    ): ResponseEntity<LoginResponse> {
        if (isEmailNotValid(email)) {
            return badRequest().body(Error(EMAIL_INVALID))
        }
        return handleVerificationCodeRequest(
            destination = email,
            userExistentPredicate = userRepository::existsByEmail,
            verificationTransport = EMAIL
        )
    }

    @PostMapping("/email")
    fun loginByEmail(@RequestBody loginRequest: LoginRequest) = handleLogin(
        loginRequest = loginRequest,
        loadUserByCodeDestination = userRepository::findByEmail,
        verificationTransport = EMAIL
    )

    @PostMapping("/phone")
    fun loginByPhone(@RequestBody loginRequest: LoginRequest) = handleLogin(
        loginRequest = loginRequest,
        loadUserByCodeDestination = userRepository::findByPhone,
        verificationTransport = SMS
    )

    @GetMapping("/phone/code")
    fun getSmsVerificationCode(
        @RequestParam("phone", required = false) phone: String
    ): ResponseEntity<LoginResponse> = handleVerificationCodeRequest(
        destination = phone,
        userExistentPredicate = userRepository::existsByPhone,
        verificationTransport = SMS
    )

    private inline fun handleLogin(
        loginRequest: LoginRequest, loadUserByCodeDestination: (String) -> User?, verificationTransport: TransportType
    ): ResponseEntity<LoginResponse> {
        val (code, codeId) = loginRequest
        return when (
            val codeAcceptanceResult = verificationService.acceptCode(code, codeId, LOGIN, verificationTransport)
        ) {
            is NotAccepted -> status(UNAUTHORIZED).body(Error(VERIFICATION_CODE_INVALID))
            is Accepted -> {
                loadUserByCodeDestination(codeAcceptanceResult.destination)?.let { user ->
                    val tokens = tokenService.emitTokens(user)
                    ok().body(
                        Success(
                            accessToken = tokens.accessToken,
                            refreshToken = tokens.refreshToken
                        )
                    )
                } ?: badRequest().body(Error(USER_NOT_FOUND))
            }
        }
    }

    private inline fun handleVerificationCodeRequest(
        destination: String, userExistentPredicate: (String) -> Boolean, verificationTransport: TransportType
    ): ResponseEntity<LoginResponse> {
        if (!userExistentPredicate(destination)) {
            return status(UNAUTHORIZED).body(Error(USER_NOT_FOUND))
        }
        val verificationCode = try {
            verificationService.sendLoginCode(destination, verificationTransport)
        } catch (e: ActiveCodeAlreadyExists) {
            return badRequest().body(Error(VERIFICATION_CODE_EXISTS))
        }
        return ok(
            VerificationCode(
                expiresAt = verificationCode.expiresAt, verificationId = verificationCode.id!!
            )
        )
    }
}


data class LoginRequest(val verificationCode: String, val verificationId: String)

sealed class LoginResponse {
    data class Error(val error: LoginErrorCode) : LoginResponse()
    data class Success(val accessToken: String, val refreshToken: String) : LoginResponse()
    data class VerificationCode(val expiresAt: Instant, val verificationId: String) : LoginResponse()
}

enum class LoginErrorCode {
    USER_NOT_FOUND,
    VERIFICATION_CODE_INVALID,
    VERIFICATION_CODE_EXISTS,
    EMAIL_INVALID
}
