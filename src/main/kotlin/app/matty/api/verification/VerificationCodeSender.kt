package app.matty.api.verification

interface VerificationCodeSender {
    fun send(code: String, destination: String)
}