package br.com.claus.mcpregressionplatform.infrastructure.mcp.tool

data class DependencyView(
    val name: String,
    val type: String,
    val criticality: String,
    val description: String
)

data class BffDependenciesResponse(
    val bff: String,
    val description: String,
    val dependencies: List<DependencyView>
)

data class HealthCheckView(
    val dependency: String,
    val type: String,
    val criticality: String,
    val state: String,
    val httpStatus: Int?,
    val latencyMillis: Long,
    val detail: String,
    val blocksRegression: Boolean
)

data class HealthCheckResponse(
    val bff: String,
    val checks: List<HealthCheckView>
)

data class ContractViolationView(
    val type: String,
    val operation: String,
    val detail: String
)

data class ContractValidationResponse(
    val service: String,
    val expectedVersion: String,
    val publishedVersion: String?,
    val compatible: Boolean,
    val violations: List<ContractViolationView>
)

data class SmokeTestView(
    val id: String,
    val name: String,
    val passed: Boolean,
    val detail: String,
    val durationMillis: Long
)

data class SmokeTestResponse(
    val target: String,
    val passed: Int,
    val total: Int,
    val allPassed: Boolean,
    val tests: List<SmokeTestView>
)

data class KnowledgePassageView(
    val title: String,
    val category: String,
    val source: String,
    val score: Double,
    val trustLevel: String,
    val quarantined: Boolean,
    val quarantineReason: String?,
    val text: String?
)

data class KnowledgeSearchResponse(
    val query: String,
    val trustNotice: String,
    val passages: List<KnowledgePassageView>
)

data class EvidenceView(
    val stage: String,
    val subject: String,
    val severity: String,
    val summary: String
)

data class RegressionStatusResponse(
    val bff: String,
    val status: String,
    val stagesExecuted: List<String>,
    val stagesSkipped: List<String>,
    val evidence: List<EvidenceView>,
    val health: List<HealthCheckView>,
    val contract: ContractValidationResponse?,
    val smoke: SmokeTestResponse?
)

data class RegressionAnalysisResponse(
    val status: String,
    val narrative: String,
    val narrativeSource: String,
    val assessment: RegressionStatusResponse
)
