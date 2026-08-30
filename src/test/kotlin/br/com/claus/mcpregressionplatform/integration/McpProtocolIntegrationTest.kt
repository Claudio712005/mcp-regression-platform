package br.com.claus.mcpregressionplatform.integration

import io.modelcontextprotocol.spec.McpSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class McpProtocolIntegrationTest : IntegrationTestSupport() {

    @Test
    fun `exposes the declared tools through streamable http`() {
        McpClientSupport.client(baseUrl(), qaToken()).use { client ->
            val tools = client.listTools().tools().map { it.name() }

            assertThat(tools).containsExactlyInAnyOrder(
                "get_bff_dependencies",
                "check_dependency_health",
                "validate_service_contract",
                "run_smoke_test",
                "search_regression_knowledge",
                "get_regression_status",
                "run_regression_analysis"
            )
        }
    }

    @Test
    fun `invokes a tool and returns domain data`() {
        McpClientSupport.client(baseUrl(), qaToken()).use { client ->
            val result = client.callTool(
                McpSchema.CallToolRequest("get_bff_dependencies", mapOf("bff" to "fintech-bff-account"))
            )

            assertThat(result.isError()).isNotEqualTo(true)
            assertThat(textOf(result)).contains("fintech-srv-account", "fintech-db")
        }
    }

    @Test
    fun `runs the regression workflow through a tool`() {
        switchScenario("service-down")

        McpClientSupport.client(baseUrl(), qaToken()).use { client ->
            val result = client.callTool(
                McpSchema.CallToolRequest("get_regression_status", mapOf("bff" to "fintech-bff-account"))
            )

            assertThat(textOf(result)).contains("BLOCKED")
        }
    }

    @Test
    fun `exposes static resources and resource templates`() {
        McpClientSupport.client(baseUrl(), qaToken()).use { client ->
            val resources = client.listResources().resources().map { it.uri() }
            val templates = client.listResourceTemplates().resourceTemplates().map { it.uriTemplate() }

            assertThat(resources).contains("regression://bffs")
            assertThat(templates).contains(
                "regression://bff/{name}",
                "regression://dependencies/{service}",
                "regression://contracts/{service}",
                "regression://runbooks/{name}"
            )
        }
    }

    @Test
    fun `reads a resource`() {
        McpClientSupport.client(baseUrl(), qaToken()).use { client ->
            val result = client.readResource(McpSchema.ReadResourceRequest("regression://bffs"))

            val contents = result.contents().filterIsInstance<McpSchema.TextResourceContents>()
            assertThat(contents.first().text()).contains("fintech-bff-account")
        }
    }

    @Test
    fun `exposes reusable prompts`() {
        McpClientSupport.client(baseUrl(), qaToken()).use { client ->
            val prompts = client.listPrompts().prompts().map { it.name() }

            assertThat(prompts).containsExactlyInAnyOrder(
                "regression-readiness-analysis",
                "dependency-diagnosis",
                "contract-risk-analysis",
                "incident-analysis"
            )
        }
    }

    @Test
    fun `renders a prompt with its arguments`() {
        McpClientSupport.client(baseUrl(), qaToken()).use { client ->
            val prompt = client.getPrompt(
                McpSchema.GetPromptRequest(
                    "regression-readiness-analysis",
                    mapOf("bff" to "fintech-bff-account")
                )
            )

            val message = prompt.messages().first().content() as McpSchema.TextContent
            assertThat(message.text()).contains("WORKFLOW: REGRESSION READINESS ANALYSIS")
            assertThat(message.text()).contains("fintech-bff-account")
        }
    }

    private fun textOf(result: McpSchema.CallToolResult): String =
        result.content().filterIsInstance<McpSchema.TextContent>().joinToString { it.text() }
}
