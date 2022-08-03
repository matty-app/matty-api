package app.matty.api.verification.sender

import app.matty.api.verification.VerificationCode

fun interface VerificationCodeSender {
    fun send(verificationCode: VerificationCode)
}
