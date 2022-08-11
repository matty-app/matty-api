package app.matty.api.auth.config

import app.matty.api.auth.token.JwtDecoder
import app.matty.api.auth.token.TokenAuthenticationFilter
import com.auth0.jwt.JWT
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class WebSecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtTokenFilter: TokenAuthenticationFilter): SecurityFilterChain {
        http {
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeRequests {
                authorize("/registration/**", permitAll)
                authorize("/login/**", permitAll)
                authorize("/auth/refresh/**", permitAll)
                authorize(anyRequest, authenticated)
            }
            csrf { disable() }
            logout { disable() }
            httpBasic { disable() }
            addFilterAfter<UsernamePasswordAuthenticationFilter>(jwtTokenFilter)
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(UNAUTHORIZED) //TODO WWW-Authenticate
            }
        }
        return http.build()
    }

    @Bean
    fun authManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }

    @Bean
    fun accessJwtDecoder(tokensConfig: TokensConfig): JwtDecoder {
        val jwtVerifier = JWT.require(tokensConfig.accessTokenAlgorithm).build()
        return JwtDecoder(jwtVerifier)
    }

    @Bean
    fun refreshJwtDecoder(tokensConfig: TokensConfig): JwtDecoder {
        val jwtVerifier = JWT.require(tokensConfig.refreshTokenAlgorithm).build()
        return JwtDecoder(jwtVerifier)
    }
}
