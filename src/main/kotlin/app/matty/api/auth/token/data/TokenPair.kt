package app.matty.api.auth.token.data

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)
