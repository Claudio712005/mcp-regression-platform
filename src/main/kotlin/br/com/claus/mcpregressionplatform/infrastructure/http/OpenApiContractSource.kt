package br.com.claus.mcpregressionplatform.infrastructure.http

import br.com.claus.mcpregressionplatform.application.port.PublishedContractSource
import br.com.claus.mcpregressionplatform.domain.contract.ApiContract
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import br.com.claus.mcpregressionplatform.infrastructure.contract.OpenApiContractParser
import org.springframework.stereotype.Component

@Component
class OpenApiContractSource(
    private val client: AccountServiceClient,
    private val properties: PlatformProperties,
    private val parser: OpenApiContractParser
) : PublishedContractSource {

    override fun fetch(service: String): ApiContract? {
        if (service != properties.integrations.accountService.name) {
            return null
        }
        val exchange = client.get(properties.integrations.accountService.contractPath)
        val body = exchange.body
        if (exchange.status != 200 || body.isNullOrBlank()) {
            return null
        }
        return runCatching { parser.parse(service, body) }.getOrNull()
    }
}
