package app.matty.api.auth.data

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)
