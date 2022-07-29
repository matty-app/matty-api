package app.matty.api.auth.data

import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class RefreshTokenRepository(private val mongoOperations: MongoOperations) {
    fun insert(token: String, userId: String) {
        mongoOperations.insert(
            mapOf(
                "_id" to token,
                "userId" to userId
            ), REFRESH_TOKENS_COLLECTION
        )
    }

    fun exists(token: String): Boolean {
        return mongoOperations.exists(
            Query.query(Criteria.where("_id").`is`(token)),
            REFRESH_TOKENS_COLLECTION
        )
    }

    fun delete(token: String) {
        mongoOperations.remove(
            Query.query(Criteria.where("_id").`is`(token)),
            REFRESH_TOKENS_COLLECTION
        )
    }
}

private const val REFRESH_TOKENS_COLLECTION = "refresh_tokens"