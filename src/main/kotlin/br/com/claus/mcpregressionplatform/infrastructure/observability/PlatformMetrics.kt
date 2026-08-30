package br.com.claus.mcpregressionplatform.infrastructure.observability

import br.com.claus.mcpregressionplatform.domain.dependency.HealthCheckResult
import br.com.claus.mcpregressionplatform.domain.dependency.HealthState
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

@Component
class PlatformMetrics(private val registry: MeterRegistry) {

    private val dependencyHealth = MultiGauge.builder("dependency_health_status").register(registry)

    fun <T> timeTool(toolName: String, action: () -> T): T {
        registry.counter("mcp_tool_calls_total", "tool", toolName).increment()
        val sample = Timer.start(registry)
        return try {
            action()
        } catch (error: Throwable) {
            registry.counter("mcp_tool_errors_total", "tool", toolName, "error", error::class.simpleName ?: "unknown")
                .increment()
            throw error
        } finally {
            sample.stop(registry.timer("mcp_tool_duration", "tool", toolName))
        }
    }

    fun denied(capability: String, reason: String) {
        registry.counter("mcp_authorization_denied_total", "capability", capability, "reason", reason).increment()
    }

    fun <T> timeRegression(action: () -> T): T = registry.timer("regression_execution_duration").recordCallable(action)!!

    fun <T> timeAgent(action: () -> T): T = registry.timer("agent_execution_duration").recordCallable(action)!!

    fun <T> timeRetrieval(action: () -> T): T = registry.timer("rag_retrieval_duration").recordCallable(action)!!

    fun publishHealth(results: List<HealthCheckResult>) {
        dependencyHealth.register(
            results.map { result ->
                MultiGauge.Row.of(
                    Tags.of("dependency", result.dependency.name, "type", result.dependency.type.name),
                    healthValue(result.state)
                )
            },
            true
        )
    }

    private fun healthValue(state: HealthState): Double = when (state) {
        HealthState.HEALTHY -> 1.0
        HealthState.DEGRADED -> 0.5
        else -> 0.0
    }
}
