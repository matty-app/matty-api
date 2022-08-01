package app.matty.api.verification

fun interface VerificationCodeSender {
    fun send(code: String, destination: String)
}
