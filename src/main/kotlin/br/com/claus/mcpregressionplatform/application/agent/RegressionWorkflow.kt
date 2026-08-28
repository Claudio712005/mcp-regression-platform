package br.com.claus.mcpregressionplatform.application.agent

import br.com.claus.mcpregressionplatform.application.contract.UnknownContractException
import br.com.claus.mcpregressionplatform.application.contract.ValidateServiceContractUseCase
import br.com.claus.mcpregressionplatform.application.dependency.CheckDependencyHealthUseCase
import br.com.claus.mcpregressionplatform.application.dependency.DiscoverDependenciesUseCase
import br.com.claus.mcpregressionplatform.application.dependency.ValidateIntegrationSecurityUseCase
import br.com.claus.mcpregressionplatform.application.knowledge.SearchKnowledgeUseCase
import br.com.claus.mcpregressionplatform.application.smoke.RunSmokeTestsUseCase
import br.com.claus.mcpregressionplatform.domain.contract.ContractComparison
import br.com.claus.mcpregressionplatform.domain.dependency.DependencyType
import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeSearchResult
import br.com.claus.mcpregressionplatform.domain.regression.Evidence
import br.com.claus.mcpregressionplatform.domain.regression.EvidenceSeverity
import br.com.claus.mcpregressionplatform.domain.regression.ReadinessPolicy
import br.com.claus.mcpregressionplatform.domain.regression.RegressionAssessment
import br.com.claus.mcpregressionplatform.domain.regression.RegressionStage
import br.com.claus.mcpregressionplatform.domain.smoke.SmokeTestSuiteResult
import org.springframework.stereotype.Service

@Service
class RegressionWorkflow(
    private val discoverDependencies: DiscoverDependenciesUseCase,
    private val checkHealth: CheckDependencyHealthUseCase,
    private val validateIntegrationSecurity: ValidateIntegrationSecurityUseCase,
    private val validateContract: ValidateServiceContractUseCase,
    private val runSmokeTests: RunSmokeTestsUseCase,
    private val searchKnowledge: SearchKnowledgeUseCase,
    private val planner: RegressionPlanner,
    private val policy: ReadinessPolicy
) {

    fun execute(bffName: String): RegressionAssessment {
        val executed = mutableListOf<RegressionStage>()
        val skipped = mutableListOf<RegressionStage>()
        val evidence = mutableListOf<Evidence>()

        val bff = discoverDependencies.execute(bffName)
        executed.add(RegressionStage.DISCOVER_DEPENDENCIES)
        evidence.add(
            Evidence(
                RegressionStage.DISCOVER_DEPENDENCIES,
                bff.name,
                "${bff.dependencies.size} declared dependencies: ${bff.dependencies.joinToString { it.name }}",
                EvidenceSeverity.INFO
            )
        )

        val health = checkHealth.executeForBff(bffName)
        executed.add(RegressionStage.CHECK_HEALTH)
        evidence.addAll(policy.evaluateHealth(health))

        val security = validateIntegrationSecurity.execute(bffName)
        executed.add(RegressionStage.VALIDATE_SECURITY)
        evidence.addAll(policy.evaluateIntegrationSecurity(security))

        val plan = planner.plan(health, security)

        var contract: ContractComparison? = null
        if (plan.contractValidation) {
            contract = validateHttpContracts(bff.dependencies.filter { it.type == DependencyType.HTTP_SERVICE }
                .map { it.name })
            executed.add(RegressionStage.VALIDATE_CONTRACT)
            contract?.let { evidence.addAll(policy.evaluateContract(it)) }
        } else {
            skipped.add(RegressionStage.VALIDATE_CONTRACT)
            evidence.add(skippedEvidence(RegressionStage.VALIDATE_CONTRACT, plan.reason))
        }

        var smoke: SmokeTestSuiteResult? = null
        if (plan.smokeTests) {
            smoke = runSmokeTests.execute(bffName)
            executed.add(RegressionStage.RUN_SMOKE_TEST)
            evidence.addAll(policy.evaluateSmoke(smoke))
        } else {
            skipped.add(RegressionStage.RUN_SMOKE_TEST)
            evidence.add(skippedEvidence(RegressionStage.RUN_SMOKE_TEST, plan.reason))
        }

        val knowledge: KnowledgeSearchResult = searchKnowledge.execute(planner.knowledgeQuery(bff.name, evidence), KNOWLEDGE_TOP_K)
        executed.add(RegressionStage.RETRIEVE_KNOWLEDGE)
        evidence.addAll(policy.evaluateKnowledge(knowledge))

        return RegressionAssessment(
            bff = bff,
            health = health,
            integrationSecurity = security,
            contract = contract,
            smoke = smoke,
            knowledge = knowledge,
            evidence = evidence,
            status = policy.classify(evidence),
            stagesExecuted = executed,
            stagesSkipped = skipped
        )
    }

    private fun validateHttpContracts(services: List<String>): ContractComparison? =
        services.firstNotNullOfOrNull { service ->
            runCatching { validateContract.execute(service) }
                .getOrElse { error -> if (error is UnknownContractException) null else throw error }
        }

    private fun skippedEvidence(stage: RegressionStage, reason: String) = Evidence(
        stage = stage,
        subject = "workflow",
        summary = "Stage skipped by the planner: $reason",
        severity = EvidenceSeverity.WARNING
    )

    companion object {
        const val KNOWLEDGE_TOP_K = 4
    }
}
