package app.matty.api.verification

import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class VerificationCodeRepository(
    private val mongoOperations: MongoOperations
) {
    fun insert(verificationCode: VerificationCode): VerificationCode {
        return mongoOperations.insert(verificationCode, DB_COLLECTION_NAME)
    }

    fun update(verificationCode: VerificationCode): VerificationCode {
        return mongoOperations.save(verificationCode, DB_COLLECTION_NAME)
    }

    fun isActiveCodeExist(destination: String): Boolean {
        val query = Query.query(
            Criteria
                .where("destination").`is`(destination)
                .and("submitted").ne(true)
                .and("expiresAt").gt(Instant.now())
        )
        return mongoOperations.exists(query, DB_COLLECTION_NAME)
    }

    fun findOneByCodeAndDestination(code: String, destination: String): VerificationCode? {
        val query = Query.query(
            Criteria
                .where("code").`is`(code)
                .and("destination").`is`(destination)
        )
        return mongoOperations.findOne(query, VerificationCode::class.java, DB_COLLECTION_NAME)
    }
}

private const val DB_COLLECTION_NAME = "verification_codes"