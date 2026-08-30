package br.com.claus.mcpregressionplatform.infrastructure.mcp.security

import br.com.claus.mcpregressionplatform.domain.security.Capability
import br.com.claus.mcpregressionplatform.domain.security.CapabilityRequirement
import br.com.claus.mcpregressionplatform.domain.security.ToolClassification
import org.springframework.stereotype.Component

object McpToolNames {
    const val GET_BFF_DEPENDENCIES = "get_bff_dependencies"
    const val CHECK_DEPENDENCY_HEALTH = "check_dependency_health"
    const val VALIDATE_SERVICE_CONTRACT = "validate_service_contract"
    const val RUN_SMOKE_TEST = "run_smoke_test"
    const val SEARCH_REGRESSION_KNOWLEDGE = "search_regression_knowledge"
    const val GET_REGRESSION_STATUS = "get_regression_status"
    const val RUN_REGRESSION_ANALYSIS = "run_regression_analysis"
}

object McpResourceNames {
    const val BFF_CATALOG = "regression-bff-catalog"
    const val BFF_DETAIL = "regression-bff-detail"
    const val DEPENDENCY_DETAIL = "regression-dependency-detail"
    const val SERVICE_CONTRACT = "regression-service-contract"
    const val RUNBOOK = "regression-runbook"
    const val ARCHITECTURE_NOTE = "regression-architecture-note"
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GuardedTool(val name: String)

@Component
class McpCapabilityRegistry {

    private val tools: Map<String, CapabilityRequirement> = listOf(
        CapabilityRequirement(McpToolNames.GET_BFF_DEPENDENCIES, Capability.READ_DEPENDENCIES, ToolClassification.READ),
        CapabilityRequirement(McpToolNames.CHECK_DEPENDENCY_HEALTH, Capability.CHECK_HEALTH, ToolClassification.READ),
        CapabilityRequirement(McpToolNames.VALIDATE_SERVICE_CONTRACT, Capability.VALIDATE_CONTRACT, ToolClassification.VALIDATION),
        CapabilityRequirement(McpToolNames.RUN_SMOKE_TEST, Capability.RUN_SMOKE_TEST, ToolClassification.EXECUTION),
        CapabilityRequirement(McpToolNames.SEARCH_REGRESSION_KNOWLEDGE, Capability.SEARCH_KNOWLEDGE, ToolClassification.READ),
        CapabilityRequirement(McpToolNames.GET_REGRESSION_STATUS, Capability.RUN_REGRESSION, ToolClassification.EXECUTION),
        CapabilityRequirement(McpToolNames.RUN_REGRESSION_ANALYSIS, Capability.RUN_REGRESSION, ToolClassification.EXECUTION)
    ).associateBy { it.toolName }

    private val resources: Map<String, CapabilityRequirement> = listOf(
        CapabilityRequirement(McpResourceNames.BFF_CATALOG, Capability.READ_DEPENDENCIES, ToolClassification.READ),
        CapabilityRequirement(McpResourceNames.BFF_DETAIL, Capability.READ_DEPENDENCIES, ToolClassification.READ),
        CapabilityRequirement(McpResourceNames.DEPENDENCY_DETAIL, Capability.READ_DEPENDENCIES, ToolClassification.READ),
        CapabilityRequirement(McpResourceNames.SERVICE_CONTRACT, Capability.VALIDATE_CONTRACT, ToolClassification.READ),
        CapabilityRequirement(McpResourceNames.RUNBOOK, Capability.SEARCH_KNOWLEDGE, ToolClassification.READ),
        CapabilityRequirement(McpResourceNames.ARCHITECTURE_NOTE, Capability.READ_ARCHITECTURE, ToolClassification.READ)
    ).associateBy { it.toolName }

    fun tool(name: String): CapabilityRequirement? = tools[name]

    fun resource(name: String): CapabilityRequirement? = resources[name]

    fun declaredTools(): List<CapabilityRequirement> = tools.values.sortedBy { it.toolName }

    fun declaredResources(): List<CapabilityRequirement> = resources.values.sortedBy { it.toolName }
}
