package app.matty.api.verification

import app.matty.api.verification.CodeAcceptanceResult.Accepted
import app.matty.api.verification.CodeAcceptanceResult.NotAccepted
import app.matty.api.verification.data.ChannelType
import app.matty.api.verification.data.Purpose
import app.matty.api.verification.data.Purpose.LOGIN
import app.matty.api.verification.data.Purpose.REGISTRATION
import app.matty.api.verification.data.VerificationCode
import app.matty.api.verification.data.VerificationCodeRepository
import app.matty.api.verification.sender.VerificationCodeSenderDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun sendLoginCode(destination: String, channel: ChannelType): VerificationCode {
        return generateAndSend(destination, channel, LOGIN)
    }

    fun sendRegistrationCode(destination: String, channel: ChannelType): VerificationCode {
        return generateAndSend(destination, channel, REGISTRATION)
    }

    fun acceptCode(
        code: String,
        codeId: String,
        purpose: Purpose,
        channel: ChannelType
    ): CodeAcceptanceResult {
        log.debug("Trying to accept verification code: $code, purpose: $purpose, id: $codeId, channel: $channel")

        val now = Instant.now()
        val verificationCode = codeRepository.findById(codeId)

        if (verificationCode == null) {
            log.debug("Verification code not found")
            return NotAccepted
        }
        if (verificationCode.channel != channel) {
            log.debug("Type of channel: '$channel' does not match expected: '${verificationCode.channel}'")
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

        return Accepted(verificationCode.destination, verificationCode.channel)
    }

    private fun generateAndSend(
        destination: String,
        channel: ChannelType,
        purpose: Purpose
    ): VerificationCode {
        val verificationCode = codeIssuer.issueCode(destination, purpose, channel)
        coroutineScope.launch {
            codeSenderDelegate.send(verificationCode)
        }
        return verificationCode
    }
}

sealed class CodeAcceptanceResult {
    object NotAccepted : CodeAcceptanceResult()
    data class Accepted(val destination: String, val channelType: ChannelType) : CodeAcceptanceResult()
}
