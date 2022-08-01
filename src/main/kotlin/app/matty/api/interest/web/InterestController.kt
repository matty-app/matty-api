package app.matty.api.interest.web

import app.matty.api.interest.data.InterestRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/interests")
class InterestController(
    val interestRepository: InterestRepository
) {
    @GetMapping
    fun getAll() = interestRepository.findAll()
}
