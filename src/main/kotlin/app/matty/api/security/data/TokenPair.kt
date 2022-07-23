package app.matty.api.security.data

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)