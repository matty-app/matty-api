package app.matty.api.auth.exc

import app.matty.api.common.MattyApiException

abstract class SecurityLevelException(message: String?) : MattyApiException(message)
