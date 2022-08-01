package app.matty.api.interest

import app.matty.api.interest.data.InterestRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class InterestsInitializerTest {
    @Test
    fun `should initialize collection of interests if empty`() {
        val interestRepository = mockk<InterestRepository>(relaxed = true) {
            every { isEmpty() } returns true
        }
        val interestsInitializer = InterestsInitializer(
            interestRepository = interestRepository,
            objectMapper = jacksonObjectMapper()
        )

        interestsInitializer.run(null)

        verify(exactly = 1) {
            interestRepository.addAll(match { it.isNotEmpty() })
        }
    }

    @Test
    fun `should not initialize collection of interests if not empty`() {
        val interestRepository = mockk<InterestRepository>(relaxed = true) {
            every { isEmpty() } returns false
        }
        val interestsInitializer = InterestsInitializer(
            interestRepository = interestRepository,
            objectMapper = mockk()
        )

        interestsInitializer.run(null)

        verify(exactly = 0) {
            interestRepository.addAll(match { it.isNotEmpty() })
        }
    }
}
