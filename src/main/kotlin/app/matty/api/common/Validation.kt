package app.matty.api.common

import java.util.regex.Pattern

private val VALID_EMAIL_PREDICATE = Pattern.compile(
    "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})\$"
).asPredicate()

fun isEmailNotValid(email: String): Boolean = !VALID_EMAIL_PREDICATE.test(email)
