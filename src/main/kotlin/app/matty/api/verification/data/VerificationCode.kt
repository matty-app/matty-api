package app.matty.api.verification

import java.time.Instant

data class VerificationCode(
    val code: String,
    val destination: String,
    val transport: TransportType,
    val purpose: Purpose,
    val expiresAt: Instant,
    val accepted: Boolean,
    val id: String?
)

enum class TransportType {
    SMS,
    EMAIL
}

enum class Purpose {
    REGISTRATION,
    LOGIN
}
