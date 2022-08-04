package app.matty.api.verification.data

import java.time.Instant

data class VerificationCode(
    val code: String,
    val destination: String,
    val channel: ChannelType,
    val purpose: Purpose,
    val expiresAt: Instant,
    val accepted: Boolean,
    val id: String?
)

enum class ChannelType {
    SMS,
    EMAIL
}

enum class Purpose {
    REGISTRATION,
    LOGIN
}
