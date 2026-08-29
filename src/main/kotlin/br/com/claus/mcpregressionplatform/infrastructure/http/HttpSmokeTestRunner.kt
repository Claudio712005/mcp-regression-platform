package br.com.claus.mcpregressionplatform.infrastructure.http

import br.com.claus.mcpregressionplatform.application.port.SmokeTestRunner
import br.com.claus.mcpregressionplatform.domain.dependency.BffDefinition
import br.com.claus.mcpregressionplatform.domain.smoke.SmokeTestOutcome
import br.com.claus.mcpregressionplatform.domain.smoke.SmokeTestSuiteResult
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import br.com.claus.mcpregressionplatform.infrastructure.configuration.SmokeTestProperties
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class HttpSmokeTestRunner(
    private val client: AccountServiceClient,
    private val properties: PlatformProperties,
    private val jsonMapper: JsonMapper
) : SmokeTestRunner {

    override fun run(bff: BffDefinition): SmokeTestSuiteResult =
        SmokeTestSuiteResult(bff.name, properties.smoke.tests.map(::execute))

    private fun execute(definition: SmokeTestProperties): SmokeTestOutcome {
        val exchange = client.get(definition.path)
        if (exchange.status != definition.expectedStatus) {
            return SmokeTestOutcome(
                id = definition.id,
                name = definition.name,
                passed = false,
                detail = "expected HTTP ${definition.expectedStatus}, received ${exchange.status ?: exchange.transportError}",
                duration = exchange.latency
            )
        }
        val missing = missingFields(exchange.body, definition.expectedFields)
        if (missing.isNotEmpty()) {
            return SmokeTestOutcome(
                id = definition.id,
                name = definition.name,
                passed = false,
                detail = "response is missing fields: ${missing.joinToString()}",
                duration = exchange.latency
            )
        }
        return SmokeTestOutcome(
            id = definition.id,
            name = definition.name,
            passed = true,
            detail = "HTTP ${exchange.status} in ${exchange.latency.toMillis()}ms",
            duration = exchange.latency
        )
    }

    private fun missingFields(body: String?, expected: List<String>): List<String> {
        if (expected.isEmpty()) {
            return emptyList()
        }
        if (body.isNullOrBlank()) {
            return expected
        }
        val root = runCatching { jsonMapper.readTree(body) }.getOrNull() ?: return expected
        return expected.filter { root.path(it).isMissingNode }
    }
}
