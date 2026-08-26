package br.com.claus.mcpregressionplatform.domain.security

class CapabilityPolicy {

    fun decide(principal: AuthenticatedPrincipal?, requirement: CapabilityRequirement?): AuthorizationDecision {
        if (requirement == null) {
            return AuthorizationDecision.Denied(
                DenialReason.UNKNOWN_TOOL,
                "Tool is not registered in the capability registry"
            )
        }
        if (principal == null) {
            return AuthorizationDecision.Denied(
                DenialReason.MISSING_AUTHENTICATION,
                "No authenticated principal bound to the MCP request"
            )
        }
        if (!principal.holds(requirement.capability)) {
            return AuthorizationDecision.Denied(
                DenialReason.MISSING_CAPABILITY,
                "Capability ${requirement.capability} is required by ${requirement.toolName} " +
                    "and is not granted to roles ${principal.roles.joinToString { it.name }}"
            )
        }
        return AuthorizationDecision.Granted(principal, requirement.capability)
    }
}

data class CapabilityRequirement(
    val toolName: String,
    val capability: Capability,
    val classification: ToolClassification
)
