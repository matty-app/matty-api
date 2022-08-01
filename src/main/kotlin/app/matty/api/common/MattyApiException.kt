package app.matty.api.common

abstract class MattyApiException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
