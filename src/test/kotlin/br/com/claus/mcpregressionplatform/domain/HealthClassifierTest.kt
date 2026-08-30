package br.com.claus.mcpregressionplatform.domain

import br.com.claus.mcpregressionplatform.domain.dependency.Criticality
import br.com.claus.mcpregressionplatform.domain.dependency.DependencyType
import br.com.claus.mcpregressionplatform.domain.dependency.HealthClassifier
import br.com.claus.mcpregressionplatform.domain.dependency.HealthProbeOutcome
import br.com.claus.mcpregressionplatform.domain.dependency.HealthState
import br.com.claus.mcpregressionplatform.domain.dependency.LatencyPolicy
import br.com.claus.mcpregressionplatform.domain.dependency.ServiceDependency
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class HealthClassifierTest {

    private val dependency = ServiceDependency(
        "fintech-srv-account",
        DependencyType.HTTP_SERVICE,
        Criticality.CRITICAL,
        "account service"
    )

    private val classifier = HealthClassifier(
        LatencyPolicy(warnAbove = Duration.ofMillis(800), failAbove = Duration.ofSeconds(5))
    )

    @Test
    fun `classifies a fast successful response as healthy`() {
        val result = classifier.classify(dependency, outcome(status = 200, latency = Duration.ofMillis(120)))

        assertThat(result.state).isEqualTo(HealthState.HEALTHY)
        assertThat(result.state.blocksRegression()).isFalse()
    }

    @Test
    fun `classifies server errors as unavailable`() {
        val result = classifier.classify(dependency, outcome(status = 503, latency = Duration.ofMillis(30)))

        assertThat(result.state).isEqualTo(HealthState.UNAVAILABLE)
        assertThat(result.state.blocksRegression()).isTrue()
    }

    @Test
    fun `classifies credential rejection as authentication failure`() {
        val result = classifier.classify(dependency, outcome(status = 401, latency = Duration.ofMillis(30)))

        assertThat(result.state).isEqualTo(HealthState.AUTHENTICATION_FAILURE)
    }

    @Test
    fun `classifies read timeout as timeout regardless of latency`() {
        val result = classifier.classify(
            dependency,
            HealthProbeOutcome(false, null, Duration.ofSeconds(4), true, false, "timeout")
        )

        assertThat(result.state).isEqualTo(HealthState.TIMEOUT)
    }

    @Test
    fun `classifies latency above the warning threshold as degraded`() {
        val result = classifier.classify(dependency, outcome(status = 200, latency = Duration.ofMillis(1500)))

        assertThat(result.state).isEqualTo(HealthState.DEGRADED)
        assertThat(result.state.blocksRegression()).isFalse()
    }

    @Test
    fun `classifies latency above the failure threshold as unavailable`() {
        val result = classifier.classify(dependency, outcome(status = 200, latency = Duration.ofSeconds(6)))

        assertThat(result.state).isEqualTo(HealthState.UNAVAILABLE)
    }

    private fun outcome(status: Int, latency: Duration) = HealthProbeOutcome(
        reachable = true,
        httpStatus = status,
        latency = latency,
        timedOut = false,
        authenticationRejected = false,
        detail = "responded with HTTP $status"
    )
}
