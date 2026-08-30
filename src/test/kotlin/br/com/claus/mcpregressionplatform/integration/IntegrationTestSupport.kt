package br.com.claus.mcpregressionplatform.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
abstract class IntegrationTestSupport {

    @LocalServerPort
    protected var port: Int = 0

    protected val client: RestClient by lazy {
        RestClient.builder()
            .requestFactory(
                ClientHttpRequestFactoryBuilder.jdk().build(
                    HttpClientSettings.defaults().withTimeouts(Duration.ofSeconds(10), Duration.ofSeconds(90))
                )
            )
            .baseUrl(baseUrl())
            .defaultStatusHandler({ true }, { _, _ -> })
            .build()
    }

    @BeforeEach
    fun resetScenario() {
        switchScenario("healthy")
    }

    protected fun baseUrl(): String = "http://localhost:$port"

    protected fun token(username: String, password: String): String {
        val response = client.post()
            .uri("/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"username":"$username","password":"$password"}""")
            .retrieve()
            .toEntity(Map::class.java)
        return response.body?.get("accessToken")?.toString()
            ?: throw IllegalStateException("token request failed with ${response.statusCode}")
    }

    protected fun architectToken(): String = token("architect", "architect-password")

    protected fun qaToken(): String = token("qa", "qa-password")

    protected fun devToken(): String = token("dev", "dev-password")

    protected fun get(path: String, token: String, accept: MediaType = MediaType.APPLICATION_JSON): ResponseEntity<String> =
        client.get()
            .uri(path)
            .header("Authorization", "Bearer $token")
            .accept(accept)
            .retrieve()
            .toEntity(String::class.java)

    protected fun switchScenario(scenario: String) {
        client.put()
            .uri("/internal/demo/scenario/$scenario")
            .header("Authorization", "Bearer ${architectToken()}")
            .retrieve()
            .toEntity(String::class.java)
    }

    companion object {
        private val postgres = PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres")
        ).withDatabaseName("regression").withUsername("regression").withPassword("regression")

        private val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)

        private val wireMock = WireMockServer(
            WireMockConfiguration.options().dynamicPort().usingFilesUnderDirectory("wiremock")
        )

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            startAll()
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
            registry.add("platform.integrations.account-service.base-url") { wireMock.baseUrl() }
        }

        private fun startAll() {
            if (!postgres.isRunning) {
                postgres.start()
            }
            if (!redis.isRunning) {
                redis.start()
            }
            if (!wireMock.isRunning) {
                wireMock.start()
                Runtime.getRuntime().addShutdownHook(Thread { wireMock.stop() })
            }
        }
    }
}
