package app.matty.api.interest.web

import app.matty.api.common.web.ApiHandler
import app.matty.api.interest.data.InterestRepository
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.ServerResponse.ok

@ApiHandler
class InterestsHandler(
    private val interestRepository: InterestRepository,
) {
    fun getAll(request: ServerRequest): ServerResponse = interestRepository.findAll().let { ok().body(it) }
}
