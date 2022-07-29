package app.matty.api.verification

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class VerificationServiceTest {
    private val destination = "matty-dev@mail.com"
    private val codeTtl = 60000L
    private val code = "123456"

    @Test
    fun `should throw exception when active code exists`() {
        val repository = repositoryMock(activeExists = true)
        val verificationService = VerificationService(
            codeTtl,
            sender = { _, _ -> },
            repository = repository
        )

        assertThrows<ActiveCodeAlreadyExists> { verificationService.generateAndSend(destination) }
    }

    @Test
    fun `should store verification code`() {
        val repository = repositoryMock(activeExists = false)
        val verificationService = VerificationService(
            codeTtl,
            sender = { _, _ -> },
            repository = repository
        )

        verificationService.generateAndSend(destination)

        verify(exactly = 1) {
            repository.add(match { it.destination == destination && !it.submitted })
        }
    }

    @Test
    fun `should send verification code`() {
        val repository = repositoryMock(activeExists = false)
        val sender = mockk<VerificationCodeSender>(relaxed = true)
        val verificationService = VerificationService(codeTtl, sender, repository)

        verificationService.generateAndSend(destination)

        verify(exactly = 1) {
            sender.send(any(), destination)
        }
    }

    @Test
    fun `should generate verification code with ttl`() {
        val repository = repositoryMock(activeExists = false)
        val verificationService = VerificationService(
            codeTtl,
            sender = { _, _ -> },
            repository = repository
        )
        val fixedTime = Instant.now()
        val expectedExpiration = fixedTime.plusMillis(codeTtl)

        mockkStatic(Instant::class)
        every { Instant.now() } returns fixedTime
        val verificationCode = verificationService.generateAndSend(destination)

        assertEquals(expectedExpiration, verificationCode.expiresAt)
    }

    @Test
    fun `should reject the code if it is not found in db`() {
        val repository = repositoryMock(codeSearchResult = null)
        val verificationService = VerificationService(
            codeTtl,
            sender = { _, _ -> },
            repository = repository
        )

        val isCodeValid = verificationService.acceptCode(code, destination)

        assertFalse(isCodeValid)
    }

    @Test
    fun `should reject expired code`() {
        val repository = repositoryMock(
            codeSearchResult = verificationCode(expired = true, submitted = false)

        )
        val verificationService = VerificationService(
            codeTtl,
            sender = { _, _ -> },
            repository = repository
        )

        val isCodeValid = verificationService.acceptCode(code, destination)

        assertFalse(isCodeValid)
    }

    @Test
    fun `should reject already accepted code`() {
        val repository = repositoryMock(
            codeSearchResult = verificationCode(expired = false, submitted = true)
        )
        val verificationService = VerificationService(
            codeTtl,
            sender = { _, _ -> },
            repository = repository
        )

        val isCodeValid = verificationService.acceptCode(code, destination)

        assertFalse(isCodeValid)
    }

    @Test
    fun `should accept valid code`() {
        val repository = repositoryMock(
            codeSearchResult = verificationCode(expired = false, submitted = false)
        )
        val verificationService = VerificationService(
            codeTtl,
            sender = { _, _ -> },
            repository = repository
        )

        val isCodeValid = verificationService.acceptCode(code, destination)

        assertTrue(isCodeValid)
    }

    private fun verificationCode(expired: Boolean = false, submitted: Boolean = false) = VerificationCode(
        code,
        destination,
        expiresAt = if (expired) Instant.now().minusMillis(codeTtl) else Instant.now().plusMillis(codeTtl),
        submitted = submitted
    )

    private fun repositoryMock(
        activeExists: Boolean = false,
        codeSearchResult: VerificationCode? = null
    ) = mockk<VerificationCodeRepository>(relaxed = true) {
        every {
            isActiveCodeExist(destination)
        } returns activeExists

        every {
            findOneByCodeAndDestination(code, destination)
        } returns codeSearchResult
    }
}
