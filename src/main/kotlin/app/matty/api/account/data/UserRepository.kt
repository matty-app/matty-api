package app.matty.api.account.data

import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, String> {
    fun existsByEmail(email: String): Boolean
}