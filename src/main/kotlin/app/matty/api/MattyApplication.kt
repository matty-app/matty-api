package app.matty.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MattyApplication

fun main(args: Array<String>) {
	runApplication<MattyApplication>(*args)
}
