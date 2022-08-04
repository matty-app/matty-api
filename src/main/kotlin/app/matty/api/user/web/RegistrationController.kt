package app.matty.api.user.web

import app.matty.api.auth.TokenService
import app.matty.api.common.isEmailNotValid
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import app.matty.api.user.web.RegistrationErrorCode.INVALID_EMAIL
import app.matty.api.user.web.RegistrationErrorCode.USER_EXISTS
import app.matty.api.user.web.RegistrationErrorCode.VERIFICATION_CODE_EXISTS
import app.matty.api.user.web.RegistrationErrorCode.INVALID_VERIFICATION_CODE
import app.matty.api.user.web.RegistrationResponse.Error
import app.matty.api.user.web.RegistrationResponse.Success
import app.matty.api.user.web.RegistrationResponse.VerificationCode
import app.matty.api.verification.ActiveCodeAlreadyExists
import app.matty.api.verification.CodeAcceptanceResult.Accepted
import app.matty.api.verification.CodeAcceptanceResult.NotAccepted
import app.matty.api.verification.data.Purpose.REGISTRATION
import app.matty.api.verification.data.ChannelType
import app.matty.api.verification.data.ChannelType.EMAIL
import app.matty.api.verification.data.ChannelType.SMS
import app.matty.api.verification.VerificationService
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.http.ResponseEntity.ok
import org.springframework.http.ResponseEntity.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
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
    @GetMapping("/phone/code")
    fun getSmsVerificationCode(
        @RequestParam("phone", required = true) phone: String
    ): ResponseEntity<RegistrationResponse> = handleVerificationCodeRequest(
        destination = phone,
        userExistentPredicate = userRepository::existsByPhone,
        verificationChannel = SMS
    )


    @GetMapping("/email/code")
    fun getEmailVerificationCode(
        @RequestParam("email", required = true) email: String
    ): ResponseEntity<RegistrationResponse> {
        if (isEmailNotValid(email)) {
            return badRequest().body(Error(INVALID_EMAIL))
        }
        return handleVerificationCodeRequest(
            destination = email,
            userExistentPredicate = userRepository::existsByEmail,
            verificationChannel = EMAIL
        )
    }


    @PostMapping("/phone")
    fun registerByPhone(registrationRequest: RegistrationRequest): ResponseEntity<RegistrationResponse> {
        return handleRegistrationRequest(
            registrationRequest = registrationRequest,
            extractEmail = { destination },
            extractPhone = { null },
            userExistentPredicate = userRepository::existsByEmail,
            verificationChannel = SMS
        )
    }

    @PostMapping("/email")
    fun registerByEmail(registrationRequest: RegistrationRequest): ResponseEntity<RegistrationResponse> {
        return handleRegistrationRequest(
            registrationRequest = registrationRequest,
            extractEmail = { destination },
            extractPhone = { null },
            userExistentPredicate = userRepository::existsByEmail,
            verificationChannel = EMAIL
        )
    }

    private inline fun handleRegistrationRequest(
        registrationRequest: RegistrationRequest,
        extractEmail: Accepted.() -> String?,
        extractPhone: (Accepted.() -> String?),
        userExistentPredicate: (String) -> Boolean,
        verificationChannel: ChannelType
    ): ResponseEntity<RegistrationResponse> {
        val (fullName, code, codeId) = registrationRequest
        val newUser = when (val codeAcceptanceResult =
            verificationService.acceptCode(code, codeId, REGISTRATION, verificationChannel)) {
            is NotAccepted -> return status(BAD_REQUEST).body(Error(INVALID_VERIFICATION_CODE))
            is Accepted -> {
                val destination = codeAcceptanceResult.destination
                if (userExistentPredicate(destination)) {
                    return badRequest().body(Error(USER_EXISTS))
                }
                val user = User(
                    fullName = fullName,
                    email = codeAcceptanceResult.extractEmail(),
                    phone = codeAcceptanceResult.extractPhone(),
                    interests = emptyList(),
                    id = null
                )
                userRepository.insert(user)
            }
        }
        val tokens = tokenService.emitTokens(newUser)
        return ok(
            Success(
                user = newUser,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken
            )
        )
    }

    private inline fun handleVerificationCodeRequest(
        destination: String,
        userExistentPredicate: (String) -> Boolean,
        verificationChannel: ChannelType
    ): ResponseEntity<RegistrationResponse> {
        if (userExistentPredicate(destination)) {
            return status(HttpStatus.UNAUTHORIZED).body(Error(USER_EXISTS))
        }
        val verificationCode = try {
            verificationService.sendRegistrationCode(destination, verificationChannel)
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

data class RegistrationRequest(val fullName: String, val verificationCode: String, val verificationId: String)

sealed class RegistrationResponse {
    data class Error(val error: RegistrationErrorCode) : RegistrationResponse()
    data class Success(
        val user: User,
        val accessToken: String,
        val refreshToken: String
    ) : RegistrationResponse()

    data class VerificationCode(val expiresAt: Instant, val verificationId: String) : RegistrationResponse()
}

enum class RegistrationErrorCode {
    USER_EXISTS,
    VERIFICATION_CODE_EXISTS,
    INVALID_VERIFICATION_CODE,
    INVALID_EMAIL
}
