package br.com.claus.mcpregressionplatform.infrastructure.security

import br.com.claus.mcpregressionplatform.domain.security.Role
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Instant

class InvalidCredentialsException : RuntimeException("Invalid credentials")

data class IssuedToken(
    val token: String,
    val expiresAt: Instant,
    val roles: List<String>
)

@Component
class PlatformTokenIssuer(
    private val encoder: JwtEncoder,
    private val properties: PlatformProperties,
    private val passwordEncoder: PasswordEncoder
) {

    fun issue(username: String, password: String): IssuedToken {
        val user = properties.security.demoUsers.firstOrNull { it.username == username }
            ?: throw InvalidCredentialsException()
        if (user.password.isBlank() || password.isBlank()) {
            throw InvalidCredentialsException()
        }
        if (!matches(password, user.password)) {
            throw InvalidCredentialsException()
        }
        val roles = user.roles.mapNotNull { Role.from(it) }
        if (roles.isEmpty()) {
            throw InvalidCredentialsException()
        }
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(properties.security.tokenTtl)
        val claims = JwtClaimsSet.builder()
            .issuer(properties.security.issuer)
            .audience(listOf(properties.security.audience))
            .subject(username)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim(ROLES_CLAIM, roles.map { it.name })
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return IssuedToken(token, expiresAt, roles.map { it.name })
    }

    private fun matches(raw: String, stored: String): Boolean =
        if (stored.startsWith(ENCODED_PREFIX)) passwordEncoder.matches(raw, stored) else raw == stored

    companion object {
        const val ROLES_CLAIM = "roles"
        private const val ENCODED_PREFIX = "{bcrypt}"
    }
}
