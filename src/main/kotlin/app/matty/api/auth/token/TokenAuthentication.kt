package app.matty.api.auth.token

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority

class TokenAuthentication(
    val token: String,
    val userId: String? = null
) : Authentication {
    override fun getName() = null

    override fun getAuthorities() = emptyList<SimpleGrantedAuthority>()

    override fun getCredentials() = null

    override fun getDetails() = null

    override fun getPrincipal() = userId

    override fun isAuthenticated() = userId != null

    override fun setAuthenticated(isAuthenticated: Boolean) {}
}
