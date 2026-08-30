package br.com.claus.mcpregressionplatform.infrastructure.mcp.prompt

import br.com.claus.mcpregressionplatform.application.port.PromptCatalog
import br.com.claus.mcpregressionplatform.infrastructure.mcp.security.ToolInputValidator
import io.modelcontextprotocol.spec.McpSchema
import org.springframework.ai.mcp.annotation.McpArg
import org.springframework.ai.mcp.annotation.McpPrompt
import org.springframework.stereotype.Component

@Component
class RegressionPrompts(
    private val catalog: PromptCatalog,
    private val validator: ToolInputValidator
) {

    @McpPrompt(
        name = "regression-readiness-analysis",
        description = "Investigate whether an environment is ready to run a regression of a BFF"
    )
    fun regressionReadinessAnalysis(
        @McpArg(name = "bff", description = "Registered BFF name", required = true) bff: String
    ): McpSchema.GetPromptResult = result(
        description = "Regression readiness investigation for $bff",
        body = catalog.load("workflows/regression-analysis"),
        task = "Investigate the regression readiness of ${validator.identifier("bff", bff)}."
    )

    @McpPrompt(
        name = "dependency-diagnosis",
        description = "Diagnose why a dependency is not healthy"
    )
    fun dependencyDiagnosis(
        @McpArg(name = "bff", description = "Registered BFF name", required = true) bff: String,
        @McpArg(name = "dependency", description = "Dependency name", required = true) dependency: String
    ): McpSchema.GetPromptResult = result(
        description = "Dependency diagnosis for $dependency",
        body = catalog.load("workflows/dependency-analysis"),
        task = "Diagnose the dependency ${validator.identifier("dependency", dependency)} " +
            "of ${validator.identifier("bff", bff)}."
    )

    @McpPrompt(
        name = "contract-risk-analysis",
        description = "Assess the regression risk introduced by contract drift"
    )
    fun contractRiskAnalysis(
        @McpArg(name = "service", description = "Service that publishes the contract", required = true) service: String
    ): McpSchema.GetPromptResult = result(
        description = "Contract risk analysis for $service",
        body = catalog.load("workflows/contract-analysis"),
        task = "Assess the contract compatibility risk of ${validator.identifier("service", service)}."
    )

    @McpPrompt(
        name = "incident-analysis",
        description = "Correlate regression evidence with the knowledge base during an incident"
    )
    fun incidentAnalysis(
        @McpArg(name = "bff", description = "Registered BFF name", required = true) bff: String,
        @McpArg(name = "symptom", description = "Observed symptom", required = true) symptom: String
    ): McpSchema.GetPromptResult = result(
        description = "Incident analysis for $bff",
        body = catalog.load("workflows/incident-analysis"),
        task = "Investigate the symptom \"${validator.freeText("symptom", symptom)}\" " +
            "reported for ${validator.identifier("bff", bff)}."
    )

    private fun result(description: String, body: String, task: String): McpSchema.GetPromptResult =
        McpSchema.GetPromptResult(
            description,
            listOf(
                McpSchema.PromptMessage(
                    McpSchema.Role.USER,
                    McpSchema.TextContent("$body\n\n## TASK\n$task")
                )
            )
        )
}
