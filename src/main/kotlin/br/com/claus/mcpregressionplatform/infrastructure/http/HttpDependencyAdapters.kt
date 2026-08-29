package br.com.claus.mcpregressionplatform.infrastructure.http

import br.com.claus.mcpregressionplatform.application.port.DependencyHealthProbe
import br.com.claus.mcpregressionplatform.application.port.IntegrationSecurityInspector
import br.com.claus.mcpregressionplatform.domain.dependency.DependencyType
import br.com.claus.mcpregressionplatform.domain.dependency.HealthProbeOutcome
import br.com.claus.mcpregressionplatform.domain.dependency.ServiceDependency
import br.com.claus.mcpregressionplatform.domain.regression.IntegrationSecurityCheck
import br.com.claus.mcpregressionplatform.domain.regression.IntegrationSecurityState
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class HttpHealthProbeAdapter(
    private val client: AccountServiceClient,
    private val properties: PlatformProperties
) : DependencyHealthProbe {

    override fun supports(dependency: ServiceDependency): Boolean = dependency.type == DependencyType.HTTP_SERVICE

    @Cacheable(cacheNames = ["dependencyHealth"], key = "#dependency.name")
    override fun probe(dependency: ServiceDependency): HealthProbeOutcome {
        val exchange = client.get(properties.integrations.accountService.healthPath)
        return HealthProbeOutcome(
            reachable = exchange.status != null,
            httpStatus = exchange.status,
            latency = exchange.latency,
            timedOut = exchange.timedOut,
            authenticationRejected = exchange.status == 401 || exchange.status == 403,
            detail = describe(exchange)
        )
    }

    private fun describe(exchange: HttpExchange): String = when {
        exchange.timedOut -> "read timeout while calling the dependency"
        exchange.transportError != null -> "transport failure: ${exchange.transportError}"
        else -> "responded with HTTP ${exchange.status}"
    }
}

@Component
class HttpIntegrationSecurityInspector(
    private val client: AccountServiceClient,
    private val properties: PlatformProperties
) : IntegrationSecurityInspector {

    override fun inspect(dependency: ServiceDependency): IntegrationSecurityCheck {
        if (dependency.type != DependencyType.HTTP_SERVICE) {
            return IntegrationSecurityCheck(
                dependency = dependency.name,
                mechanism = "database-credentials",
                state = IntegrationSecurityState.VALID,
                detail = "connection pool authenticated by the datasource configuration"
            )
        }
        if (properties.integrations.accountService.apiKey.isBlank()) {
            return IntegrationSecurityCheck(
                dependency = dependency.name,
                mechanism = "service-api-key",
                state = IntegrationSecurityState.REJECTED,
                detail = "no outbound API key configured for the integration"
            )
        }
        val exchange = client.get(properties.integrations.accountService.healthPath)
        val state = when (exchange.status) {
            401, 403 -> IntegrationSecurityState.REJECTED
            null -> IntegrationSecurityState.NOT_APPLICABLE
            else -> IntegrationSecurityState.VALID
        }
        return IntegrationSecurityCheck(
            dependency = dependency.name,
            mechanism = "service-api-key",
            state = state,
            detail = when (state) {
                IntegrationSecurityState.REJECTED -> "dependency rejected the platform credentials with HTTP ${exchange.status}"
                IntegrationSecurityState.NOT_APPLICABLE -> "dependency unreachable, credentials could not be validated"
                IntegrationSecurityState.VALID -> "dependency accepted the platform credentials"
            }
        )
    }
}
