package br.com.claus.mcpregressionplatform.infrastructure.contract

import br.com.claus.mcpregressionplatform.application.port.ExpectedContractCatalog
import br.com.claus.mcpregressionplatform.domain.contract.ApiContract
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class ClasspathExpectedContractCatalog(
    private val properties: PlatformProperties,
    private val parser: OpenApiContractParser
) : ExpectedContractCatalog {

    private val contracts: Map<String, ApiContract> by lazy { load() }

    override fun find(service: String): ApiContract? = contracts[service]

    override fun services(): List<String> = contracts.keys.sorted()

    private fun load(): Map<String, ApiContract> {
        val resolver = PathMatchingResourcePatternResolver()
        return resolver.getResources(properties.contracts.location + "*.json")
            .mapNotNull { resource ->
                val service = resource.filename?.removeSuffix(".json") ?: return@mapNotNull null
                service to parser.parse(service, resource.inputStream.readAllBytes().decodeToString())
            }
            .toMap()
    }
}
