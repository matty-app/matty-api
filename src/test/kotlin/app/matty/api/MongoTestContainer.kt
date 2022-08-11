package app.matty.api

import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Testcontainers

@ContextConfiguration(
    initializers = [MongoTestContainer.Companion.Initializer::class],
)
@Testcontainers
abstract class MongoTestContainer {
    companion object {
        private var mongoDBContainer = MongoDBContainer("mongo").withReuse(true)

        @Suppress("unused")
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mongoDBContainer.start()
        }

        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(applicationContext: ConfigurableApplicationContext) {
                TestPropertyValues.of(
                    mapOf(
                        "spring.data.mongodb.uri" to mongoDBContainer.replicaSetUrl
                    )
                ).applyTo(applicationContext)
            }
        }
    }
}
