package br.com.claus.mcpregressionplatform.infrastructure.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "platform")
data class PlatformProperties(
    val demo: DemoProperties = DemoProperties(),
    val health: HealthProperties = HealthProperties(),
    val integrations: IntegrationsProperties = IntegrationsProperties(),
    val contracts: ContractsProperties = ContractsProperties(),
    val smoke: SmokeProperties = SmokeProperties(),
    val knowledge: KnowledgeProperties = KnowledgeProperties(),
    val ai: AiProperties = AiProperties(),
    val security: SecurityProperties = SecurityProperties()
)

data class DemoProperties(
    val scenario: String = "healthy",
    val scenarioHeader: String = "X-Demo-Scenario"
)

data class HealthProperties(
    val warnAbove: Duration = Duration.ofMillis(800),
    val failAbove: Duration = Duration.ofSeconds(5)
)

data class IntegrationsProperties(
    val accountService: AccountServiceProperties = AccountServiceProperties(),
    val database: DatabaseProbeProperties = DatabaseProbeProperties()
)

data class AccountServiceProperties(
    val name: String = "fintech-srv-account",
    val baseUrl: String = "http://localhost:8081",
    val healthPath: String = "/actuator/health",
    val contractPath: String = "/v3/api-docs",
    val apiKeyHeader: String = "X-Service-Api-Key",
    val apiKey: String = "",
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val readTimeout: Duration = Duration.ofSeconds(4)
)

data class DatabaseProbeProperties(
    val name: String = "fintech-db",
    val probeQuery: String = "select 1",
    val timeout: Duration = Duration.ofSeconds(2)
)

data class ContractsProperties(
    val location: String = "classpath:contracts/"
)

data class SmokeProperties(
    val tests: List<SmokeTestProperties> = emptyList()
)

data class SmokeTestProperties(
    val id: String = "",
    val name: String = "",
    val path: String = "",
    val expectedStatus: Int = 200,
    val expectedFields: List<String> = emptyList()
)

data class KnowledgeProperties(
    val location: String = "classpath:knowledge/",
    val chunkSize: Int = 700,
    val chunkOverlap: Int = 120,
    val ingestOnStartup: Boolean = true,
    val minimumScore: Double = 0.10
)

data class AiProperties(
    val reasoningEnabled: Boolean = false,
    val embeddingDimensions: Int = 384
)

data class SecurityProperties(
    val issuer: String = "mcp-regression-platform",
    val audience: String = "mcp-regression-platform",
    val signingKey: String = "",
    val stdioToken: String = "",
    val tokenTtl: Duration = Duration.ofHours(1),
    val demoUsers: List<DemoUserProperties> = emptyList()
)

data class DemoUserProperties(
    val username: String = "",
    val password: String = "",
    val roles: List<String> = emptyList()
)
