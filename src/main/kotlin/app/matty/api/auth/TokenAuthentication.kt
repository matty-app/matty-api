package app.matty.api.auth

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority

class TokenAuthentication(
    val token: String,
    val userId: String
) : Authentication {
    override fun getName() = null

    override fun getAuthorities() = emptyList<SimpleGrantedAuthority>()

    override fun getCredentials() = null

    override fun getDetails() = null

    override fun getPrincipal() = userId

    override fun isAuthenticated() = true

    override fun setAuthenticated(isAuthenticated: Boolean) {}
}
