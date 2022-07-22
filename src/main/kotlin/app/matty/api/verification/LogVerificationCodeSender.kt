package app.matty.api.verification

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(LogVerificationCodeSender::class.java)

@Component
class LogVerificationCodeSender : VerificationCodeSender {
    override fun send(code: String, destination: String) {
        log.info("Sending verification code '$code' to '$destination'")
    }
}