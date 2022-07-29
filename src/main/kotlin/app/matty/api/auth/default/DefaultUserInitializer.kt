package app.matty.api.auth.default

import app.matty.api.user.data.User
import app.matty.api.user.data.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(DefaultUserInitializer::class.java)

@Component
@Profile("dev")
class DefaultUserInitializer(
    private val userRepository: UserRepository
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        val user = User(
            fullName = "Piter Parker",
            email = "spider@marvel.com",
            interests = emptyList(),
            id = null
        )
        if (userRepository.count() == 0L) {
            val newUser = userRepository.save(user)
            log.info("Default user created: $newUser")
        }
    }
}
