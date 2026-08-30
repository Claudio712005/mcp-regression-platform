package br.com.claus.mcpregressionplatform.application

import br.com.claus.mcpregressionplatform.application.agent.RegressionPlanner
import br.com.claus.mcpregressionplatform.domain.dependency.Criticality
import br.com.claus.mcpregressionplatform.domain.dependency.DependencyType
import br.com.claus.mcpregressionplatform.domain.dependency.HealthCheckResult
import br.com.claus.mcpregressionplatform.domain.dependency.HealthState
import br.com.claus.mcpregressionplatform.domain.dependency.ServiceDependency
import br.com.claus.mcpregressionplatform.domain.regression.IntegrationSecurityCheck
import br.com.claus.mcpregressionplatform.domain.regression.IntegrationSecurityState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class RegressionPlannerTest {

    private val planner = RegressionPlanner()

    private val dependency = ServiceDependency(
        "fintech-srv-account",
        DependencyType.HTTP_SERVICE,
        Criticality.CRITICAL,
        "account service"
    )

    @Test
    fun `runs every stage when the environment is healthy`() {
        val plan = planner.plan(listOf(health(HealthState.HEALTHY)), listOf(security(IntegrationSecurityState.VALID)))

        assertThat(plan.contractValidation).isTrue()
        assertThat(plan.smokeTests).isTrue()
    }

    @Test
    fun `skips contract and smoke stages when a critical dependency is unavailable`() {
        val plan = planner.plan(listOf(health(HealthState.UNAVAILABLE)), listOf(security(IntegrationSecurityState.VALID)))

        assertThat(plan.contractValidation).isFalse()
        assertThat(plan.smokeTests).isFalse()
        assertThat(plan.reason).contains("UNAVAILABLE")
    }

    @Test
    fun `skips downstream stages when the integration credentials are rejected`() {
        val plan = planner.plan(listOf(health(HealthState.HEALTHY)), listOf(security(IntegrationSecurityState.REJECTED)))

        assertThat(plan.contractValidation).isFalse()
        assertThat(plan.reason).contains("authentication rejected")
    }

    private fun health(state: HealthState) =
        HealthCheckResult(dependency, state, Duration.ofMillis(10), null, "detail")

    private fun security(state: IntegrationSecurityState) =
        IntegrationSecurityCheck(dependency.name, "service-api-key", state, "detail")
}
