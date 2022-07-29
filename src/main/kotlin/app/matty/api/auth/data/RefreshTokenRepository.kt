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
            ), DB_COLLECTION_NAME
        )
    }

    fun exists(token: String): Boolean {
        return mongoOperations.exists(
            Query.query(Criteria.where("_id").`is`(token)),
            DB_COLLECTION_NAME
        )
    }

    fun delete(token: String) {
        mongoOperations.remove(
            Query.query(Criteria.where("_id").`is`(token)),
            DB_COLLECTION_NAME
        )
    }
}

private const val DB_COLLECTION_NAME = "refresh_tokens"
