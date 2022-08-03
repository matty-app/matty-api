package app.matty.api.verification

import app.matty.api.verification.CodeAcceptanceResult.Accepted
import app.matty.api.verification.CodeAcceptanceResult.NotAccepted
import app.matty.api.verification.Purpose.LOGIN
import app.matty.api.verification.Purpose.REGISTRATION
import app.matty.api.verification.data.VerificationCodeRepository
import app.matty.api.verification.sender.VerificationCodeSenderDelegate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

private val log = LoggerFactory.getLogger(VerificationService::class.java)

@Component
class VerificationService(
    private val codeIssuer: VerificationCodeIssuer,
    private val codeRepository: VerificationCodeRepository,
    private val codeSenderDelegate: VerificationCodeSenderDelegate
) {
    fun sendLoginCode(destination: String, transportType: TransportType): VerificationCode {
        return generateAndSend(destination, transportType, LOGIN)
    }

    fun sendRegistrationCode(destination: String, transportType: TransportType): VerificationCode {
        return generateAndSend(destination, transportType, REGISTRATION)
    }

    fun acceptCode(
        code: String,
        codeId: String,
        purpose: Purpose,
        transport: TransportType
    ): CodeAcceptanceResult {
        log.debug("Trying to accept verification code: $code, purpose: $purpose, id: $codeId, transport: $transport")

        val now = Instant.now()
        val verificationCode = codeRepository.findById(codeId)

        if (verificationCode == null) {
            log.debug("Verification code not found")
            return NotAccepted
        }
        if (verificationCode.transport != transport) {
            log.debug("Type of transport: '$transport' does not match expected: '${verificationCode.transport}'")
            return NotAccepted
        }
        if (verificationCode.accepted) {
            log.debug("Verification code already used")
            return NotAccepted
        }
        if (verificationCode.purpose != purpose) {
            log.debug("Verification code: $verificationCode doesnt match purpose $purpose")
            return NotAccepted
        }
        if (verificationCode.code != code) {
            log.debug("Verification code: '$code' doesnt match ")
            return NotAccepted
        }
        if (now.isAfter(verificationCode.expiresAt)) {
            log.debug("Verification code has expired")
            return NotAccepted
        }

        codeRepository.update(verificationCode.copy(accepted = true))

        return Accepted(verificationCode.destination, verificationCode.transport)
    }

    private fun generateAndSend(
        destination: String,
        transportType: TransportType,
        purpose: Purpose
    ): VerificationCode {
        val verificationCode = codeIssuer.issueCode(destination, purpose, transportType)
        codeSenderDelegate.send(verificationCode)
        return verificationCode
    }
}

sealed class CodeAcceptanceResult {
    object NotAccepted : CodeAcceptanceResult()
    data class Accepted(val destination: String, val transportType: TransportType) : CodeAcceptanceResult()
}
