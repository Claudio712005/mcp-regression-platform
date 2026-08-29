package br.com.claus.mcpregressionplatform.infrastructure.demo

import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

enum class DemoScenario(val id: String) {
    HEALTHY("healthy"),
    SERVICE_DOWN("service-down"),
    CONTRACT_MISMATCH("contract-mismatch"),
    AUTHENTICATION_FAILURE("authentication-failure"),
    HIGH_LATENCY("high-latency"),
    TIMEOUT("timeout");

    companion object {
        fun from(value: String): DemoScenario =
            entries.firstOrNull { it.id.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown demo scenario: $value")
    }
}

@Component
class DemoScenarioHolder(
    properties: PlatformProperties,
    private val cacheManager: CacheManager
) {

    private val current = AtomicReference(DemoScenario.from(properties.demo.scenario))

    fun current(): DemoScenario = current.get()

    fun switch(scenario: DemoScenario): DemoScenario {
        current.set(scenario)
        cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
        return scenario
    }
}
