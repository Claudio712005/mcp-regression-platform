package br.com.claus.mcpregressionplatform.domain.security

data class AuthenticatedPrincipal(
    val subject: String,
    val roles: Set<Role>
) {
    val capabilities: Set<Capability> = roles.flatMapTo(mutableSetOf()) { it.capabilities }

    fun holds(capability: Capability): Boolean = capabilities.contains(capability)
}

sealed interface AuthorizationDecision {

    data class Granted(val principal: AuthenticatedPrincipal, val capability: Capability) : AuthorizationDecision

    data class Denied(val reason: DenialReason, val message: String) : AuthorizationDecision
}

enum class DenialReason {
    MISSING_AUTHENTICATION,
    INVALID_TOKEN,
    MISSING_CAPABILITY,
    UNKNOWN_TOOL
}

class AuthorizationDeniedException(val reason: DenialReason, message: String) : RuntimeException(message)
