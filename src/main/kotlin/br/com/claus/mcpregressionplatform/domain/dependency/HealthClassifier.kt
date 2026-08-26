package br.com.claus.mcpregressionplatform.domain.dependency

class HealthClassifier(private val latencyPolicy: LatencyPolicy) {

    fun classify(dependency: ServiceDependency, outcome: HealthProbeOutcome): HealthCheckResult {
        val state = resolveState(outcome)
        return HealthCheckResult(
            dependency = dependency,
            state = state,
            latency = outcome.latency,
            httpStatus = outcome.httpStatus,
            detail = outcome.detail
        )
    }

    private fun resolveState(outcome: HealthProbeOutcome): HealthState {
        if (outcome.timedOut) {
            return HealthState.TIMEOUT
        }
        if (outcome.authenticationRejected) {
            return HealthState.AUTHENTICATION_FAILURE
        }
        if (!outcome.reachable) {
            return HealthState.UNAVAILABLE
        }
        val status = outcome.httpStatus
        if (status != null) {
            if (status == 401 || status == 403) {
                return HealthState.AUTHENTICATION_FAILURE
            }
            if (status >= 500) {
                return HealthState.UNAVAILABLE
            }
            if (status >= 400) {
                return HealthState.DEGRADED
            }
        }
        if (outcome.latency >= latencyPolicy.failAbove) {
            return HealthState.UNAVAILABLE
        }
        if (outcome.latency >= latencyPolicy.warnAbove) {
            return HealthState.DEGRADED
        }
        return HealthState.HEALTHY
    }
}
