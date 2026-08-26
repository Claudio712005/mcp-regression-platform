package br.com.claus.mcpregressionplatform.domain.regression

import br.com.claus.mcpregressionplatform.domain.contract.ContractComparison
import br.com.claus.mcpregressionplatform.domain.dependency.BffDefinition
import br.com.claus.mcpregressionplatform.domain.dependency.HealthCheckResult
import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeSearchResult
import br.com.claus.mcpregressionplatform.domain.smoke.SmokeTestSuiteResult

enum class RegressionStage {
    DISCOVER_DEPENDENCIES,
    CHECK_HEALTH,
    VALIDATE_SECURITY,
    VALIDATE_CONTRACT,
    RUN_SMOKE_TEST,
    RETRIEVE_KNOWLEDGE,
    ANALYZE
}

enum class ReadinessStatus {
    READY_FOR_REGRESSION,
    WARNING,
    BLOCKED
}

enum class EvidenceSeverity {
    INFO,
    WARNING,
    BLOCKER
}

data class Evidence(
    val stage: RegressionStage,
    val subject: String,
    val summary: String,
    val severity: EvidenceSeverity
)

enum class IntegrationSecurityState {
    VALID,
    REJECTED,
    NOT_APPLICABLE
}

data class IntegrationSecurityCheck(
    val dependency: String,
    val mechanism: String,
    val state: IntegrationSecurityState,
    val detail: String
)

data class RegressionAssessment(
    val bff: BffDefinition,
    val health: List<HealthCheckResult>,
    val integrationSecurity: List<IntegrationSecurityCheck>,
    val contract: ContractComparison?,
    val smoke: SmokeTestSuiteResult?,
    val knowledge: KnowledgeSearchResult?,
    val evidence: List<Evidence>,
    val status: ReadinessStatus,
    val stagesExecuted: List<RegressionStage>,
    val stagesSkipped: List<RegressionStage>
) {
    val blockers: List<Evidence> get() = evidence.filter { it.severity == EvidenceSeverity.BLOCKER }
    val warnings: List<Evidence> get() = evidence.filter { it.severity == EvidenceSeverity.WARNING }
}

data class RegressionAnalysis(
    val assessment: RegressionAssessment,
    val narrative: String,
    val narrativeSource: NarrativeSource
)

enum class NarrativeSource {
    LANGUAGE_MODEL,
    DETERMINISTIC_TEMPLATE
}
