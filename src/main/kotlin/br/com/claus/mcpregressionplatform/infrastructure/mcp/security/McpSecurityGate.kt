package br.com.claus.mcpregressionplatform.infrastructure.mcp.security

import br.com.claus.mcpregressionplatform.domain.security.AuthenticatedPrincipal
import br.com.claus.mcpregressionplatform.domain.security.AuthorizationDecision
import br.com.claus.mcpregressionplatform.domain.security.AuthorizationDeniedException
import br.com.claus.mcpregressionplatform.domain.security.CapabilityPolicy
import br.com.claus.mcpregressionplatform.domain.security.CapabilityRequirement
import br.com.claus.mcpregressionplatform.domain.security.DenialReason
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import br.com.claus.mcpregressionplatform.infrastructure.observability.PlatformMetrics
import br.com.claus.mcpregressionplatform.infrastructure.security.PrincipalFactory
import io.modelcontextprotocol.common.McpTransportContext
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.annotation.context.McpSyncRequestContext
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component

@Component
class McpSecurityGate(
    private val decoder: JwtDecoder,
    private val principalFactory: PrincipalFactory,
    private val registry: McpCapabilityRegistry,
    private val policy: CapabilityPolicy,
    private val properties: PlatformProperties,
    private val metrics: PlatformMetrics
) {

    private val log = LoggerFactory.getLogger(McpSecurityGate::class.java)

    fun <T> execute(context: McpSyncRequestContext?, toolName: String, action: (AuthenticatedPrincipal) -> T): T =
        authorize(context?.transportContext(), registry.tool(toolName), toolName) { principal ->
            metrics.timeTool(toolName) { action(principal) }
        }

    fun <T> readResource(
        context: McpTransportContext?,
        resourceName: String,
        action: (AuthenticatedPrincipal) -> T
    ): T = authorize(context, registry.resource(resourceName), resourceName, action)

    private fun <T> authorize(
        transportContext: McpTransportContext?,
        requirement: CapabilityRequirement?,
        capabilityName: String,
        action: (AuthenticatedPrincipal) -> T
    ): T {
        val principal = resolvePrincipal(transportContext)
        return when (val decision = policy.decide(principal, requirement)) {
            is AuthorizationDecision.Denied -> {
                metrics.denied(capabilityName, decision.reason.name)
                log.warn("MCP capability {} denied: {} - {}", capabilityName, decision.reason, decision.message)
                throw AuthorizationDeniedException(decision.reason, decision.message)
            }

            is AuthorizationDecision.Granted -> action(decision.principal)
        }
    }

    private fun resolvePrincipal(context: McpTransportContext?): AuthenticatedPrincipal? {
        val token = bearerToken(context) ?: return null
        val jwt = runCatching { decoder.decode(token) }.getOrElse {
            log.warn("MCP request presented an invalid token: {}", it.message)
            throw AuthorizationDeniedException(DenialReason.INVALID_TOKEN, "The presented token is not valid")
        }
        return principalFactory.from(jwt)
    }

    private fun bearerToken(context: McpTransportContext?): String? {
        val header = context?.get(AUTHORIZATION_KEY)?.toString()
        if (!header.isNullOrBlank()) {
            return header.removePrefix(BEARER_PREFIX).trim().ifBlank { null }
        }
        return properties.security.stdioToken.ifBlank { null }
    }

    companion object {
        const val AUTHORIZATION_KEY = "authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
