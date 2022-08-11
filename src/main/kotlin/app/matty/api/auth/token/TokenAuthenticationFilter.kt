package app.matty.api.auth.token

import app.matty.api.auth.token.exception.InvalidTokenException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class TokenAuthenticationFilter(
    private val authManager: AuthenticationManager
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        request.resolveTokenFromHeader()?.let { token ->
            val tokenAuthentication = TokenAuthentication(token, userId = null)
            try {
                val authResult = authManager.authenticate(tokenAuthentication)
                if (authResult.isAuthenticated) {
                    val securityContext = SecurityContextHolder.createEmptyContext()
                    securityContext.authentication = authResult
                    SecurityContextHolder.setContext(securityContext)
                }
            } catch (e: InvalidTokenException) {
                SecurityContextHolder.clearContext()
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.resolveTokenFromHeader(): String? {
        val authHeader = getHeader(AUTH_HEADER_NAME) ?: return null
        if (authHeader.isNotEmpty() && authHeader.startsWith(AUTH_TOKEN_PREFIX, ignoreCase = true)) {
            return authHeader.substring(AUTH_TOKEN_PREFIX.length)
        }
        return null
    }
}

private const val AUTH_HEADER_NAME = "Authorization"
private const val AUTH_TOKEN_PREFIX = "Bearer "
