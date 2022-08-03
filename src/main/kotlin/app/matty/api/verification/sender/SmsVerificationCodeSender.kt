package app.matty.api.verification.sender

import app.matty.api.verification.VerificationCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(EmailVerificationCodeSender::class.java)

@Component
class SmsVerificationCodeSender : VerificationCodeSender {
    override fun send(verificationCode: VerificationCode) {
        log.info("Sending verification code $verificationCode")
    }
}
