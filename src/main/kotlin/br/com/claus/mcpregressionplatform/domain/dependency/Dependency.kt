package br.com.claus.mcpregressionplatform.domain.dependency

import java.time.Duration

enum class DependencyType {
    HTTP_SERVICE,
    DATABASE
}

enum class Criticality {
    CRITICAL,
    NON_CRITICAL
}

data class ServiceDependency(
    val name: String,
    val type: DependencyType,
    val criticality: Criticality,
    val description: String
)

data class BffDefinition(
    val name: String,
    val description: String,
    val dependencies: List<ServiceDependency>
) {
    fun dependency(name: String): ServiceDependency? = dependencies.firstOrNull { it.name == name }
}

enum class HealthState {
    HEALTHY,
    DEGRADED,
    UNAVAILABLE,
    TIMEOUT,
    AUTHENTICATION_FAILURE,
    UNKNOWN;

    fun blocksRegression(): Boolean = this in BLOCKING

    companion object {
        private val BLOCKING = setOf(UNAVAILABLE, TIMEOUT, AUTHENTICATION_FAILURE, UNKNOWN)
    }
}

data class LatencyPolicy(
    val warnAbove: Duration,
    val failAbove: Duration
)

data class HealthProbeOutcome(
    val reachable: Boolean,
    val httpStatus: Int?,
    val latency: Duration,
    val timedOut: Boolean,
    val authenticationRejected: Boolean,
    val detail: String
)

data class HealthCheckResult(
    val dependency: ServiceDependency,
    val state: HealthState,
    val latency: Duration,
    val httpStatus: Int?,
    val detail: String
)
