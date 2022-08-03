package app.matty.api.verification

import app.matty.api.common.MattyApiException
import app.matty.api.verification.data.VerificationCodeRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

private val log = LoggerFactory.getLogger(VerificationCodeIssuer::class.java)

@Component
class VerificationCodeIssuer(
    @Value("\${app.verification-code.ttl}")
    private val ttl: Long,
    private val codeRepository: VerificationCodeRepository
) {
    private val codesRange = (1000..9999)

    fun issueCode(destination: String, purpose: Purpose, transportType: TransportType): VerificationCode {
        if (codeRepository.isActiveCodeExist(destination)) {
            log.error("Verification code {destination: '$destination')} is already exist!")
            throw ActiveCodeAlreadyExists()
        }
        val code = codesRange.random().toString()
        val expiresAt = Instant.now().plusMillis(ttl)
        val verificationCode = VerificationCode(
            code,
            destination,
            transportType,
            purpose,
            expiresAt,
            accepted = false,
            id = UUID.randomUUID().toString()
        )
        codeRepository.add(verificationCode)
        log.debug("Generated verification code: $verificationCode")
        return verificationCode
    }
}

class ActiveCodeAlreadyExists : MattyApiException()
