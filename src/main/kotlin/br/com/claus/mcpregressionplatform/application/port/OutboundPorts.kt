package br.com.claus.mcpregressionplatform.application.port

import br.com.claus.mcpregressionplatform.domain.contract.ApiContract
import br.com.claus.mcpregressionplatform.domain.dependency.BffDefinition
import br.com.claus.mcpregressionplatform.domain.dependency.HealthProbeOutcome
import br.com.claus.mcpregressionplatform.domain.dependency.ServiceDependency
import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeChunk
import br.com.claus.mcpregressionplatform.domain.knowledge.RetrievedPassage
import br.com.claus.mcpregressionplatform.domain.regression.IntegrationSecurityCheck
import br.com.claus.mcpregressionplatform.domain.smoke.SmokeTestSuiteResult

interface BffRegistry {
    fun findAll(): List<BffDefinition>
    fun findByName(name: String): BffDefinition?
}

interface DependencyHealthProbe {
    fun supports(dependency: ServiceDependency): Boolean
    fun probe(dependency: ServiceDependency): HealthProbeOutcome
}

interface IntegrationSecurityInspector {
    fun inspect(dependency: ServiceDependency): IntegrationSecurityCheck
}

interface PublishedContractSource {
    fun fetch(service: String): ApiContract?
}

interface ExpectedContractCatalog {
    fun find(service: String): ApiContract?
    fun services(): List<String>
}

interface SmokeTestRunner {
    fun run(bff: BffDefinition): SmokeTestSuiteResult
}

interface KnowledgeSearchPort {
    fun search(query: String, topK: Int): List<RetrievedPassage>
}

interface KnowledgeIndexPort {
    fun index(chunks: List<KnowledgeChunk>)
    fun count(): Long
}

interface KnowledgeDocumentSource {
    fun load(): List<br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeDocument>
}

interface ReasoningModel {
    fun available(): Boolean
    fun reason(systemInstructions: String, userMessage: String): String
}

interface PromptCatalog {
    fun load(reference: String): String
    fun references(): List<String>
}
