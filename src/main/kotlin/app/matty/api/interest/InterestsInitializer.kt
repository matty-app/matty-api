package app.matty.api.interest

import app.matty.api.interest.data.Interest
import app.matty.api.interest.data.InterestRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(InterestsInitializer::class.java)

@Component
class InterestsInitializer(
    private val interestRepository: InterestRepository,
    private val objectMapper: ObjectMapper
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        if (interestRepository.isEmpty()) {
            log.info("Initializing interests collection...")
            val interests = loadInterests()
            interestRepository.addAll(interests)
            log.info("Added ${interests.size} interests")
        }
    }

    private fun loadInterests(): List<Interest> {
        val file = javaClass.classLoader.getResource("data/interests.json")
        requireNotNull(file) {
            "Cant find URL of the required resource 'interests.json'!"
        }
        return objectMapper.readValue(file)
    }
}
