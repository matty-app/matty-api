package app.matty.api.user.web

import app.matty.api.auth.token.TokenService
import app.matty.api.common.web.ApiHandler
import app.matty.api.common.web.requireParam
import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import app.matty.api.user.web.RegistrationErrorCode.USER_EXISTS
import app.matty.api.user.web.RegistrationErrorCode.VERIFICATION_CODE_EXISTS
import app.matty.api.user.web.RegistrationErrorCode.VERIFICATION_CODE_INVALID
import app.matty.api.user.web.RegistrationResponseMessage.Error
import app.matty.api.user.web.RegistrationResponseMessage.Success
import app.matty.api.verification.ActiveCodeAlreadyExists
import app.matty.api.verification.VerificationService
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.ServerResponse.badRequest
import org.springframework.web.servlet.function.ServerResponse.ok
import org.springframework.web.servlet.function.ServerResponse.status
import org.springframework.web.servlet.function.body
import java.time.Instant

@ApiHandler
class RegistrationHandler(
    private val verificationService: VerificationService,
    private val userRepository: UserRepository,
    private val tokenService: TokenService
) {
    fun getVerificationCode(request: ServerRequest): ServerResponse {
        val email = request.requireParam("email")
        //TODO validate email
        if (userRepository.existsByEmail(email)) {
            return status(HttpStatus.CONFLICT).body(Error(USER_EXISTS))
        }
        val verificationCode = try {
            verificationService.generateAndSend(email)
        } catch (e: ActiveCodeAlreadyExists) {
            return badRequest().body(Error(VERIFICATION_CODE_EXISTS))
        }
        return ok().body(VerificationCodeResponse(expiresAt = verificationCode.expiresAt))
    }

    fun register(request: ServerRequest): ServerResponse {
        val (fullName, email, verificationCode) = request.body<RegistrationRequest>()
        if (userRepository.existsByEmail(email)) {
            return status(HttpStatus.CONFLICT).body(Error(USER_EXISTS))
        }
        if (!verificationService.acceptCode(verificationCode, email)) {
            return status(HttpStatus.UNAUTHORIZED).body(Error(VERIFICATION_CODE_INVALID))
        }
        val newUser = userRepository.insert(
            User(
                fullName = fullName, email = email, interests = emptyList(), id = null
            )
        )
        val tokens = tokenService.emitTokens(newUser)
        return ok().body(
            Success(
                user = newUser,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken
            )
        )
    }
}

private data class RegistrationRequest(val fullName: String, val email: String, val verificationCode: String)

private data class VerificationCodeResponse(val expiresAt: Instant) : RegistrationResponseMessage()

private sealed class RegistrationResponseMessage {
    data class Error(val error: RegistrationErrorCode) : RegistrationResponseMessage()
    data class Success(
        val user: User,
        val accessToken: String,
        val refreshToken: String
    ) : RegistrationResponseMessage()
}

enum class RegistrationErrorCode {
    USER_EXISTS, VERIFICATION_CODE_EXISTS, VERIFICATION_CODE_INVALID
}
