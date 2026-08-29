package br.com.claus.mcpregressionplatform.infrastructure.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class SecurityConfiguration {

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val authorities = JwtGrantedAuthoritiesConverter()
        authorities.setAuthoritiesClaimName(PlatformTokenIssuer.ROLES_CLAIM)
        authorities.setAuthorityPrefix(ROLE_PREFIX)
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(authorities)
        return converter
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity, converter: JwtAuthenticationConverter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/token").permitAll()
                it.requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                it.requestMatchers("/actuator/**").hasAuthority("${ROLE_PREFIX}ARCHITECT")
                it.requestMatchers("/internal/demo/**").hasAuthority("${ROLE_PREFIX}ARCHITECT")
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { jwt -> jwt.jwtAuthenticationConverter(converter) }
            }
        return http.build()
    }

    companion object {
        const val ROLE_PREFIX = "ROLE_"
    }
}
