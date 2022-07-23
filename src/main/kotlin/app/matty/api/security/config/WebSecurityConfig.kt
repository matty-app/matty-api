package app.matty.api.security.config

import app.matty.api.security.AuthTokenFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class WebSecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtTokenFilter: AuthTokenFilter): SecurityFilterChain {
        return http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/registration/**", "/login", "/login/code").permitAll()
            .anyRequest().authenticated()
            .and()
            .csrf().disable()
            .formLogin().disable()
            .addFilterAfter(jwtTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}