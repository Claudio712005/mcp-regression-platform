package br.com.claus.mcpregressionplatform.infrastructure.mcp.resource

import br.com.claus.mcpregressionplatform.application.contract.ValidateServiceContractUseCase
import br.com.claus.mcpregressionplatform.application.dependency.DiscoverDependenciesUseCase
import br.com.claus.mcpregressionplatform.application.dependency.UnknownDependencyException
import br.com.claus.mcpregressionplatform.application.port.KnowledgeDocumentSource
import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeDocument
import br.com.claus.mcpregressionplatform.infrastructure.mcp.security.McpResourceNames
import br.com.claus.mcpregressionplatform.infrastructure.mcp.security.McpSecurityGate
import br.com.claus.mcpregressionplatform.infrastructure.mcp.security.ToolInputValidator
import io.modelcontextprotocol.common.McpTransportContext
import org.springframework.ai.mcp.annotation.McpResource
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

class UnknownKnowledgeResourceException(name: String) : RuntimeException("Unknown knowledge resource: $name")

@Component
class RegressionResources(
    private val gate: McpSecurityGate,
    private val validator: ToolInputValidator,
    private val jsonMapper: JsonMapper,
    private val discoverDependencies: DiscoverDependenciesUseCase,
    private val validateContract: ValidateServiceContractUseCase,
    private val knowledgeSource: KnowledgeDocumentSource
) {

    @McpResource(
        name = McpResourceNames.BFF_CATALOG,
        uri = "regression://bffs",
        description = "Catalog of the BFFs registered in the regression platform",
        mimeType = MIME_JSON
    )
    fun bffCatalog(context: McpTransportContext): String =
        gate.readResource(context, McpResourceNames.BFF_CATALOG) {
            jsonMapper.writeValueAsString(
                discoverDependencies.listBffs().map {
                    mapOf(
                        "name" to it.name,
                        "description" to it.description,
                        "uri" to "regression://bff/${it.name}",
                        "dependencies" to it.dependencies.map { dependency -> dependency.name }
                    )
                }
            )
        }

    @McpResource(
        name = McpResourceNames.BFF_DETAIL,
        uri = "regression://bff/{name}",
        description = "Declared definition of a single BFF",
        mimeType = MIME_JSON
    )
    fun bffDetail(context: McpTransportContext, name: String): String =
        gate.readResource(context, McpResourceNames.BFF_DETAIL) {
            val bff = discoverDependencies.execute(validator.identifier("name", name))
            jsonMapper.writeValueAsString(
                mapOf(
                    "name" to bff.name,
                    "description" to bff.description,
                    "dependencies" to bff.dependencies.map { dependency ->
                        mapOf(
                            "name" to dependency.name,
                            "type" to dependency.type.name,
                            "criticality" to dependency.criticality.name,
                            "description" to dependency.description,
                            "uri" to "regression://dependencies/${dependency.name}"
                        )
                    }
                )
            )
        }

    @McpResource(
        name = McpResourceNames.DEPENDENCY_DETAIL,
        uri = "regression://dependencies/{service}",
        description = "Declared metadata of a dependency and the BFFs that consume it",
        mimeType = MIME_JSON
    )
    fun dependencyDetail(context: McpTransportContext, service: String): String =
        gate.readResource(context, McpResourceNames.DEPENDENCY_DETAIL) {
            val serviceName = validator.identifier("service", service)
            val consumers = discoverDependencies.listBffs().filter { it.dependency(serviceName) != null }
            val declared = consumers.firstNotNullOfOrNull { it.dependency(serviceName) }
                ?: throw UnknownDependencyException("any", serviceName)
            jsonMapper.writeValueAsString(
                mapOf(
                    "name" to declared.name,
                    "type" to declared.type.name,
                    "criticality" to declared.criticality.name,
                    "description" to declared.description,
                    "consumers" to consumers.map { it.name }
                )
            )
        }

    @McpResource(
        name = McpResourceNames.SERVICE_CONTRACT,
        uri = "regression://contracts/{service}",
        description = "Expected API contract that the platform validates for a service",
        mimeType = MIME_JSON
    )
    fun serviceContract(context: McpTransportContext, service: String): String =
        gate.readResource(context, McpResourceNames.SERVICE_CONTRACT) {
            val contract = validateContract.expectedContract(validator.identifier("service", service))
            jsonMapper.writeValueAsString(
                mapOf(
                    "service" to contract.service,
                    "version" to contract.version,
                    "operations" to contract.operations.map {
                        mapOf(
                            "path" to it.path,
                            "method" to it.method.name,
                            "requiredParameters" to it.requiredParameters.sorted(),
                            "successStatus" to it.successStatus,
                            "responseFields" to it.responseFields.sorted()
                        )
                    }
                )
            )
        }

    @McpResource(
        name = McpResourceNames.RUNBOOK,
        uri = "regression://runbooks/{name}",
        description = "Regression runbook stored in the knowledge base",
        mimeType = MIME_MARKDOWN
    )
    fun runbook(context: McpTransportContext, name: String): String =
        gate.readResource(context, McpResourceNames.RUNBOOK) {
            documentOf("runbooks", validator.identifier("name", name)).content
        }

    @McpResource(
        name = McpResourceNames.ARCHITECTURE_NOTE,
        uri = "regression://architecture/{name}",
        description = "Architecture note stored in the knowledge base",
        mimeType = MIME_MARKDOWN
    )
    fun architectureNote(context: McpTransportContext, name: String): String =
        gate.readResource(context, McpResourceNames.ARCHITECTURE_NOTE) {
            documentOf("architecture", validator.identifier("name", name)).content
        }

    private fun documentOf(category: String, name: String): KnowledgeDocument =
        knowledgeSource.load().firstOrNull {
            it.category == category && it.id.substringAfterLast('/').removeSuffix(".md") == name
        } ?: throw UnknownKnowledgeResourceException("$category/$name")

    private companion object {
        const val MIME_JSON = "application/json"
        const val MIME_MARKDOWN = "text/markdown"
    }
}
