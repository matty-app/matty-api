package app.matty.api.verification.sender

import app.matty.api.verification.data.VerificationCode

fun interface VerificationCodeSender {
    suspend fun send(verificationCode: VerificationCode)
}
