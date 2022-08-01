package app.matty.api.interest.data

import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.stereotype.Repository

@Repository
class InterestRepository(private val mongoOperations: MongoOperations) {
    fun addAll(interests: Collection<Interest>) {
        mongoOperations.insert(
            interests,
            DB_COLLECTION_NAME
        )
    }

    fun isEmpty(): Boolean {
        return mongoOperations.estimatedCount(DB_COLLECTION_NAME) == 0L
    }

    fun findAll(): List<Interest> = mongoOperations.findAll(Interest::class.java, DB_COLLECTION_NAME)
}

private const val DB_COLLECTION_NAME = "interests"

