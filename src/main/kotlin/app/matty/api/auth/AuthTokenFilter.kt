package app.matty.api.auth

import app.matty.api.auth.exc.InvalidTokenException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val AUTH_HEADER_NAME = "Authorization"
private const val AUTH_TOKEN_PREFIX = "Bearer "

@Component
class AuthTokenFilter(
    private val tokenDataExtractor: TokenDataExtractor
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        request.getAuthToken()?.let { token ->
            val userId = try {
                tokenDataExtractor.getUserIdFromAccessToken(token)
            } catch (e: InvalidTokenException) {
                null
            }
            if (userId != null) {
                val securityContext = SecurityContextHolder.createEmptyContext()
                securityContext.authentication = TokenAuthentication(token, userId)
                SecurityContextHolder.setContext(securityContext)
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.getAuthToken(): String? {
        val authHeader = getHeader(AUTH_HEADER_NAME) ?: return null
        if (authHeader.isNotEmpty() && authHeader.startsWith(AUTH_TOKEN_PREFIX)) {
            return authHeader.substring(AUTH_TOKEN_PREFIX.length)
        }
        return null
    }
}
