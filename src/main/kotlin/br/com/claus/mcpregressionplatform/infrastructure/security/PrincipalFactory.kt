package br.com.claus.mcpregressionplatform.infrastructure.security

import br.com.claus.mcpregressionplatform.domain.security.AuthenticatedPrincipal
import br.com.claus.mcpregressionplatform.domain.security.Role
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class PrincipalFactory {

    fun from(jwt: Jwt): AuthenticatedPrincipal? {
        val roles = jwt.getClaimAsStringList(PlatformTokenIssuer.ROLES_CLAIM)
            .orEmpty()
            .mapNotNull { Role.from(it) }
            .toSet()
        if (roles.isEmpty()) {
            return null
        }
        return AuthenticatedPrincipal(jwt.subject ?: return null, roles)
    }
}
