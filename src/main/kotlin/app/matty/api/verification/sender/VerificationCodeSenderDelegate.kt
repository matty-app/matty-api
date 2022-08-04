package app.matty.api.verification.sender

import app.matty.api.verification.data.ChannelType.EMAIL
import app.matty.api.verification.data.ChannelType.SMS
import app.matty.api.verification.data.VerificationCode
import org.springframework.stereotype.Component

@Component
class VerificationCodeSenderDelegate(
    private val emailVerificationCodeSender: VerificationCodeSender,
    private val smsVerificationCodeSender: VerificationCodeSender
) : VerificationCodeSender {
    override suspend fun send(verificationCode: VerificationCode) {
        val codeSender = when (verificationCode.channel) {
            EMAIL -> emailVerificationCodeSender
            SMS -> smsVerificationCodeSender
        }
        codeSender.send(verificationCode)
    }
}
