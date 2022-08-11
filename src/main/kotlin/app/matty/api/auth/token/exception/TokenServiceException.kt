package app.matty.api.auth.token.exception

import app.matty.api.common.MattyApiException

class TokenServiceException(message: String, cause: Throwable? = null) : MattyApiException(message, cause)
