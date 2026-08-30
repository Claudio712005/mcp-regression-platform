package br.com.claus.mcpregressionplatform.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.json.JsonMapper

class RegressionScenarioIntegrationTest : IntegrationTestSupport() {

    private val jsonMapper = JsonMapper.builder().build()

    @Test
    fun `discovers the declared dependencies of the bff`() {
        val response = get("/api/regression/$BFF/health", devToken())

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val checks = jsonMapper.readTree(response.body).path("checks")
        assertThat(checks.size()).isEqualTo(2)
    }

    @Test
    fun `reports the environment as ready in the healthy scenario`() {
        switchScenario("healthy")

        val readiness = readiness()

        assertThat(readiness.path("status").asString()).isEqualTo("READY_FOR_REGRESSION")
        assertThat(readiness.path("assessment").path("smoke").path("allPassed").asBoolean()).isTrue()
        assertThat(readiness.path("assessment").path("contract").path("compatible").asBoolean()).isTrue()
    }

    @Test
    fun `blocks the regression when the dependency answers 503`() {
        switchScenario("service-down")

        val readiness = readiness()

        assertThat(readiness.path("status").asString()).isEqualTo("BLOCKED")
        val health = readiness.path("assessment").path("health")
        assertThat(health.any { it.path("state").asString() == "UNAVAILABLE" }).isTrue()
        assertThat(readiness.path("assessment").path("stagesSkipped").toString()).contains("RUN_SMOKE_TEST")
    }

    @Test
    fun `blocks the regression when the published contract drifted`() {
        switchScenario("contract-mismatch")

        val readiness = readiness()

        assertThat(readiness.path("status").asString()).isEqualTo("BLOCKED")
        val violations = readiness.path("assessment").path("contract").path("violations")
        assertThat(violations.size()).isGreaterThan(0)
        assertThat(violations.toString()).contains("MISSING_ENDPOINT")
    }

    @Test
    fun `blocks the regression when the integration credentials are rejected`() {
        switchScenario("authentication-failure")

        val readiness = readiness()

        assertThat(readiness.path("status").asString()).isEqualTo("BLOCKED")
        val health = readiness.path("assessment").path("health")
        assertThat(health.any { it.path("state").asString() == "AUTHENTICATION_FAILURE" }).isTrue()
    }

    @Test
    fun `warns instead of blocking when the dependency is slow`() {
        switchScenario("high-latency")

        val readiness = readiness()

        assertThat(readiness.path("status").asString()).isEqualTo("WARNING")
        val health = readiness.path("assessment").path("health")
        assertThat(health.any { it.path("state").asString() == "DEGRADED" }).isTrue()
    }

    @Test
    fun `blocks the regression when the dependency does not answer in time`() {
        switchScenario("timeout")

        val readiness = readiness()

        assertThat(readiness.path("status").asString()).isEqualTo("BLOCKED")
        val health = readiness.path("assessment").path("health")
        assertThat(health.any { it.path("state").asString() == "TIMEOUT" }).isTrue()
    }

    @Test
    fun `renders a console report for the demo`() {
        switchScenario("service-down")

        val response = get("/api/regression/$BFF/readiness", qaToken(), MediaType.TEXT_PLAIN)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("MCP REGRESSION PLATFORM")
        assertThat(response.body).contains("BLOCKED")
    }

    private fun readiness() = jsonMapper.readTree(get("/api/regression/$BFF/readiness", qaToken()).body)

    private companion object {
        const val BFF = "fintech-bff-account"
    }
}
