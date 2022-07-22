package app.matty.api.verification

import app.matty.api.common.MattyApiException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

private val log = LoggerFactory.getLogger(VerificationService::class.java)

@Service
class VerificationService(
    @Value("\${app.verification-code.ttl}")
    private val ttl: Long,
    private val sender: VerificationCodeSender,
    private val repository: VerificationCodeRepository
) {
    fun generateAndSend(destination: String) {
        if (repository.isActiveCodeExist(destination)) {
            log.error("Verification code for destination '$destination' is already exist!")
            throw ActiveCodeAlreadyExists()
        }
        val code = codesRange.random().toString()
        val expiresAt = Instant.now().plusMillis(ttl)
        val verificationCode = VerificationCode(code, destination, expiresAt, submitted = false)
        repository.insert(verificationCode)
        log.debug("Generated verification code: $verificationCode")
        sender.send(code, destination)
    }

    fun submitCode(code: String, destination: String): Boolean {
        log.debug("Trying to submit verification code: $code (destination $destination)")
        val now = Instant.now()
        val verificationCode = repository.findOneByCodeAndDestination(code, destination)

        if (verificationCode == null) {
            log.debug("Verification code not found")
            return false
        }
        if (verificationCode.submitted) {
            log.debug("Verification code already used")
            return true
        }
        if (verificationCode.expiresAt.isAfter(now)) {
            log.debug("Verification code has expired")
            return false
        }

        repository.update(verificationCode.copy(submitted = true))

        return false
    }

    private val codesRange = (100000..999999)
}

class ActiveCodeAlreadyExists : MattyApiException()