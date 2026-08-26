package br.com.claus.mcpregressionplatform.domain.regression

import br.com.claus.mcpregressionplatform.domain.contract.ContractComparison
import br.com.claus.mcpregressionplatform.domain.dependency.Criticality
import br.com.claus.mcpregressionplatform.domain.dependency.HealthCheckResult
import br.com.claus.mcpregressionplatform.domain.dependency.HealthState
import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeSearchResult
import br.com.claus.mcpregressionplatform.domain.smoke.SmokeTestSuiteResult

class ReadinessPolicy {

    fun evaluateHealth(results: List<HealthCheckResult>): List<Evidence> = results.map { result ->
        val severity = when {
            result.state.blocksRegression() && result.dependency.criticality == Criticality.CRITICAL ->
                EvidenceSeverity.BLOCKER
            result.state.blocksRegression() -> EvidenceSeverity.WARNING
            result.state == HealthState.DEGRADED -> EvidenceSeverity.WARNING
            else -> EvidenceSeverity.INFO
        }
        Evidence(
            stage = RegressionStage.CHECK_HEALTH,
            subject = result.dependency.name,
            summary = buildString {
                append(result.state.name)
                result.httpStatus?.let { append(" (HTTP $it)") }
                append(" in ${result.latency.toMillis()}ms: ${result.detail}")
            },
            severity = severity
        )
    }

    fun evaluateIntegrationSecurity(checks: List<IntegrationSecurityCheck>): List<Evidence> = checks.map { check ->
        Evidence(
            stage = RegressionStage.VALIDATE_SECURITY,
            subject = check.dependency,
            summary = "${check.mechanism}: ${check.state.name} - ${check.detail}",
            severity = if (check.state == IntegrationSecurityState.REJECTED) {
                EvidenceSeverity.BLOCKER
            } else {
                EvidenceSeverity.INFO
            }
        )
    }

    fun evaluateContract(comparison: ContractComparison): List<Evidence> {
        if (comparison.compatible) {
            return listOf(
                Evidence(
                    RegressionStage.VALIDATE_CONTRACT,
                    comparison.service,
                    "Contract ${comparison.expectedVersion} is compatible with the published contract",
                    EvidenceSeverity.INFO
                )
            )
        }
        return comparison.violations.map {
            Evidence(
                RegressionStage.VALIDATE_CONTRACT,
                comparison.service,
                "${it.type.name} on ${it.operation}: ${it.detail}",
                EvidenceSeverity.BLOCKER
            )
        }
    }

    fun evaluateSmoke(result: SmokeTestSuiteResult): List<Evidence> {
        val header = Evidence(
            RegressionStage.RUN_SMOKE_TEST,
            result.target,
            "${result.passed}/${result.total} smoke tests passed",
            if (result.allPassed) EvidenceSeverity.INFO else EvidenceSeverity.BLOCKER
        )
        return listOf(header) + result.failures.map {
            Evidence(
                RegressionStage.RUN_SMOKE_TEST,
                it.name,
                it.detail,
                EvidenceSeverity.BLOCKER
            )
        }
    }

    fun evaluateKnowledge(result: KnowledgeSearchResult): List<Evidence> {
        val quarantined = result.passages.count { it.quarantined }
        val found = Evidence(
            RegressionStage.RETRIEVE_KNOWLEDGE,
            "knowledge-base",
            if (result.usable.isEmpty()) {
                "No runbook was retrieved for the regression context"
            } else {
                "${result.usable.size} passages retrieved: ${result.usable.joinToString { it.title }}"
            },
            if (result.usable.isEmpty()) EvidenceSeverity.WARNING else EvidenceSeverity.INFO
        )
        if (quarantined == 0) {
            return listOf(found)
        }
        return listOf(
            found,
            Evidence(
                RegressionStage.RETRIEVE_KNOWLEDGE,
                "knowledge-base",
                "$quarantined passages quarantined by the prompt injection filter",
                EvidenceSeverity.WARNING
            )
        )
    }

    fun classify(evidence: List<Evidence>): ReadinessStatus = when {
        evidence.any { it.severity == EvidenceSeverity.BLOCKER } -> ReadinessStatus.BLOCKED
        evidence.any { it.severity == EvidenceSeverity.WARNING } -> ReadinessStatus.WARNING
        else -> ReadinessStatus.READY_FOR_REGRESSION
    }
}
