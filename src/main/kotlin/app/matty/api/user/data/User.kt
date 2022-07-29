package app.matty.api.user.data

data class User(
    val fullName: String,
    val email: String,
    val interests: List<String>,
    val id: String?
)