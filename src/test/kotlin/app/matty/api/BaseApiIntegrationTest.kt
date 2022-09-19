package app.matty.api

import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Testcontainers

@ContextConfiguration(
    initializers = [
        BaseApiIntegrationTest.Companion.MongoTestInitializer::class,
        ApiRouterTestInitializer::class
    ],
)
@Testcontainers
abstract class BaseApiIntegrationTest {
    companion object {
        private var mongoDBContainer = MongoDBContainer("mongo").withReuse(true)

        @Suppress("unused")
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mongoDBContainer.start()
        }

        class MongoTestInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
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

class ApiRouterTestInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(applicationContext: GenericApplicationContext) {
        apiRouter.initialize(applicationContext)
    }
}
