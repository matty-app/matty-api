package app.matty.api.verification.sender

import app.matty.api.verification.data.VerificationCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(EmailVerificationCodeSender::class.java)

@Component
class EmailVerificationCodeSender : VerificationCodeSender {
    override suspend fun send(verificationCode: VerificationCode) {
        log.info("Sending email verification code $verificationCode")
    }
}
