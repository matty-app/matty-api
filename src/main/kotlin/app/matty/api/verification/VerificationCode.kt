package app.matty.api.verification

import java.time.Instant

data class VerificationCode(
    val code: String,
    val destination: String,
    val expiresAt: Instant,
    val submitted: Boolean,
    private val id: String = "${code}_${destination}_${expiresAt}"
)
