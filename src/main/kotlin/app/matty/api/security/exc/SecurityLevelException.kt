package app.matty.api.security.exc

import app.matty.api.common.MattyApiException

abstract class SecurityLevelException(message: String?) : MattyApiException(message)