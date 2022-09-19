package app.matty.api.auth.web

import app.matty.api.auth.token.TokenService
import app.matty.api.auth.web.LoginErrorCode.USER_NOT_FOUND
import app.matty.api.auth.web.LoginErrorCode.VERIFICATION_CODE_EXISTS
import app.matty.api.auth.web.LoginErrorCode.VERIFICATION_CODE_INVALID
import app.matty.api.auth.web.LoginResponseMessage.Error
import app.matty.api.auth.web.LoginResponseMessage.Success
import app.matty.api.common.web.ApiHandler
import app.matty.api.common.web.requireParam
import app.matty.api.user.data.UserRepository
import app.matty.api.verification.ActiveCodeAlreadyExists
import app.matty.api.verification.VerificationService
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.ServerResponse.badRequest
import org.springframework.web.servlet.function.ServerResponse.ok
import org.springframework.web.servlet.function.ServerResponse.status
import org.springframework.web.servlet.function.body
import java.time.Instant

@ApiHandler
class LoginHandler(
    private val verificationService: VerificationService,
    private val userRepository: UserRepository,
    private val tokenService: TokenService
) {
    fun getVerificationCode(
        request: ServerRequest
    ): ServerResponse {
        val email = request.requireParam("email")
        if (!userRepository.existsByEmail(email)) {
            return status(NOT_FOUND).body(Error(USER_NOT_FOUND))
        }
        val verificationCode = try {
            verificationService.generateAndSend(email)
        } catch (e: ActiveCodeAlreadyExists) {
            return badRequest().body(Error(VERIFICATION_CODE_EXISTS))
        }
        return ok().body(VerificationCodeResponse(expiresAt = verificationCode.expiresAt))
    }

    fun login(request: ServerRequest): ServerResponse {
        val (email, verificationCode) = request.body<LoginRequest>()
        val user =
            userRepository.findByEmail(email) ?: return status(NOT_FOUND).body(Error(USER_NOT_FOUND))
        if (!verificationService.acceptCode(verificationCode, email)) {
            return status(HttpStatus.UNAUTHORIZED).body(Error(VERIFICATION_CODE_INVALID))
        }
        val tokens = tokenService.emitTokens(user)
        return ok().body(Success(accessToken = tokens.accessToken, refreshToken = tokens.refreshToken))
    }
}

data class LoginRequest(val email: String, val verificationCode: String)

enum class LoginErrorCode {
    USER_NOT_FOUND,
    VERIFICATION_CODE_INVALID,
    VERIFICATION_CODE_EXISTS
}

private data class VerificationCodeResponse(val expiresAt: Instant)

private sealed class LoginResponseMessage {
    data class Error(val error: LoginErrorCode) : LoginResponseMessage()
    data class Success(val accessToken: String, val refreshToken: String) : LoginResponseMessage()
}
