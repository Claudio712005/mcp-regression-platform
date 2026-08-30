package br.com.claus.mcpregressionplatform.infrastructure.mcp

import br.com.claus.mcpregressionplatform.infrastructure.mcp.security.McpSecurityGate
import io.modelcontextprotocol.common.McpTransportContext
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

@Configuration
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = ["stdio"], havingValue = "false", matchIfMissing = true)
class McpTransportConfiguration {

    @Bean
    fun webMvcStreamableServerTransportProvider(
        @Qualifier("mcpServerJsonMapper") jsonMapper: JsonMapper,
        properties: McpServerStreamableHttpProperties
    ): WebMvcStreamableServerTransportProvider =
        WebMvcStreamableServerTransportProvider.builder()
            .jsonMapper(JacksonMcpJsonMapper(jsonMapper))
            .mcpEndpoint(properties.mcpEndpoint)
            .keepAliveInterval(properties.keepAliveInterval)
            .disallowDelete(properties.isDisallowDelete)
            .contextExtractor { request ->
                McpTransportContext.create(
                    buildMap {
                        request.headers().firstHeader(AUTHORIZATION_HEADER)?.let {
                            put(McpSecurityGate.AUTHORIZATION_KEY, it)
                        }
                    }
                )
            }
            .build()

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
