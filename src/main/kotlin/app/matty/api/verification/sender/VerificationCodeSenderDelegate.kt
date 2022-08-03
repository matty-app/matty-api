package app.matty.api.verification.sender

import app.matty.api.verification.TransportType.EMAIL
import app.matty.api.verification.TransportType.SMS
import app.matty.api.verification.VerificationCode
import org.springframework.stereotype.Component

@Component
class VerificationCodeSenderDelegate(
    private val emailVerificationCodeSender: VerificationCodeSender,
    private val smsVerificationCodeSender: VerificationCodeSender
) : VerificationCodeSender {
    override fun send(verificationCode: VerificationCode) {
        val codeSender = when (verificationCode.transport) {
            EMAIL -> emailVerificationCodeSender
            SMS -> smsVerificationCodeSender
        }
        codeSender.send(verificationCode)
    }
}
