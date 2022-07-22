package app.matty.api.account.data

data class User(
    val fullName: String,
    val email: String,
    val interests: List<String>,
    val id: String?
)