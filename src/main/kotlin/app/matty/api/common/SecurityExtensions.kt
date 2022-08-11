package app.matty.api.auth

import app.matty.api.auth.token.TokenAuthentication
import org.springframework.security.core.context.SecurityContextHolder

inline fun getUserIdOrThrow(supplier: () -> Throwable) =
    (SecurityContextHolder.getContext().authentication as? TokenAuthentication)?.userId ?: throw supplier()
