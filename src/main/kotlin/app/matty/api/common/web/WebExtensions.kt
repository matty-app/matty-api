package app.matty.api.common.web

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.function.ServerRequest

fun ServerRequest.requireParam(name: String): String = param(name).orElseThrow {
    ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter $name is mandatory!")
}
