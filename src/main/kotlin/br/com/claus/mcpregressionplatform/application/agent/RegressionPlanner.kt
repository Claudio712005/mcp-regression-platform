package br.com.claus.mcpregressionplatform.application.agent

import br.com.claus.mcpregressionplatform.domain.dependency.Criticality
import br.com.claus.mcpregressionplatform.domain.dependency.HealthCheckResult
import br.com.claus.mcpregressionplatform.domain.regression.Evidence
import br.com.claus.mcpregressionplatform.domain.regression.EvidenceSeverity
import br.com.claus.mcpregressionplatform.domain.regression.IntegrationSecurityCheck
import br.com.claus.mcpregressionplatform.domain.regression.IntegrationSecurityState
import org.springframework.stereotype.Component

data class RegressionPlan(
    val contractValidation: Boolean,
    val smokeTests: Boolean,
    val reason: String
)

@Component
class RegressionPlanner {

    fun plan(health: List<HealthCheckResult>, security: List<IntegrationSecurityCheck>): RegressionPlan {
        val blockedDependency = health.firstOrNull {
            it.state.blocksRegression() && it.dependency.criticality == Criticality.CRITICAL
        }
        if (blockedDependency != null) {
            return RegressionPlan(
                contractValidation = false,
                smokeTests = false,
                reason = "critical dependency ${blockedDependency.dependency.name} is ${blockedDependency.state.name}"
            )
        }
        val rejectedIntegration = security.firstOrNull { it.state == IntegrationSecurityState.REJECTED }
        if (rejectedIntegration != null) {
            return RegressionPlan(
                contractValidation = false,
                smokeTests = false,
                reason = "integration authentication rejected for ${rejectedIntegration.dependency}"
            )
        }
        return RegressionPlan(contractValidation = true, smokeTests = true, reason = "all critical dependencies reachable")
    }

    fun knowledgeQuery(bff: String, evidence: List<Evidence>): String {
        val problems = evidence.filter { it.severity != EvidenceSeverity.INFO }
        if (problems.isEmpty()) {
            return "regression runbook and readiness checklist for $bff"
        }
        return "regression troubleshooting for $bff: " + problems.joinToString(separator = "; ") { "${it.stage.name} ${it.subject}" }
    }
}
