package br.com.claus.mcpregressionplatform.infrastructure.persistence

import br.com.claus.mcpregressionplatform.application.port.DependencyHealthProbe
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import br.com.claus.mcpregressionplatform.domain.dependency.DependencyType
import br.com.claus.mcpregressionplatform.domain.dependency.HealthProbeOutcome
import br.com.claus.mcpregressionplatform.domain.dependency.ServiceDependency
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.system.measureNanoTime

@Component
class DatabaseHealthProbeAdapter(
    private val jdbcClient: JdbcClient,
    private val properties: PlatformProperties
) : DependencyHealthProbe {

    override fun supports(dependency: ServiceDependency): Boolean = dependency.type == DependencyType.DATABASE

    override fun probe(dependency: ServiceDependency): HealthProbeOutcome {
        var failure: Throwable? = null
        val elapsed = measureNanoTime {
            failure = runCatching {
                jdbcClient.sql(properties.integrations.database.probeQuery).query(Int::class.java).single()
            }.exceptionOrNull()
        }
        val latency = Duration.ofNanos(elapsed)
        val error = failure
        return if (error == null) {
            HealthProbeOutcome(
                reachable = true,
                httpStatus = null,
                latency = latency,
                timedOut = false,
                authenticationRejected = false,
                detail = "probe query executed successfully"
            )
        } else {
            HealthProbeOutcome(
                reachable = false,
                httpStatus = null,
                latency = latency,
                timedOut = error is java.sql.SQLTimeoutException,
                authenticationRejected = false,
                detail = error.message ?: error::class.simpleName.orEmpty()
            )
        }
    }
}
