package br.com.claus.mcpregressionplatform.infrastructure.api

import br.com.claus.mcpregressionplatform.application.dependency.CheckDependencyHealthUseCase
import br.com.claus.mcpregressionplatform.application.dependency.DiscoverDependenciesUseCase
import br.com.claus.mcpregressionplatform.application.regression.RunRegressionAnalysisUseCase
import br.com.claus.mcpregressionplatform.infrastructure.mcp.security.ToolInputValidator
import br.com.claus.mcpregressionplatform.infrastructure.mcp.tool.BffDependenciesResponse
import br.com.claus.mcpregressionplatform.infrastructure.mcp.tool.HealthCheckResponse
import br.com.claus.mcpregressionplatform.infrastructure.mcp.tool.RegressionAnalysisResponse
import br.com.claus.mcpregressionplatform.infrastructure.mcp.tool.ToolViewMapper
import br.com.claus.mcpregressionplatform.infrastructure.observability.PlatformMetrics
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/regression")
class RegressionController(
    private val discoverDependencies: DiscoverDependenciesUseCase,
    private val checkHealth: CheckDependencyHealthUseCase,
    private val analysis: RunRegressionAnalysisUseCase,
    private val mapper: ToolViewMapper,
    private val validator: ToolInputValidator,
    private val renderer: ConsoleReportRenderer,
    private val metrics: PlatformMetrics
) {

    @GetMapping("/bffs")
    @PreAuthorize("hasAnyRole('DEV', 'QA', 'ARCHITECT')")
    fun bffs(): List<BffDependenciesResponse> = discoverDependencies.listBffs().map(mapper::dependencies)

    @GetMapping("/{bff}/health")
    @PreAuthorize("hasAnyRole('DEV', 'QA', 'ARCHITECT')")
    fun health(@PathVariable bff: String): HealthCheckResponse {
        val name = validator.identifier("bff", bff)
        val results = checkHealth.executeForBff(name)
        metrics.publishHealth(results)
        return mapper.health(name, results)
    }

    @GetMapping("/{bff}/readiness", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('QA', 'ARCHITECT')")
    fun readiness(@PathVariable bff: String): RegressionAnalysisResponse {
        val result = metrics.timeRegression { analysis.execute(validator.identifier("bff", bff)) }
        return RegressionAnalysisResponse(
            status = result.assessment.status.name,
            narrative = result.narrative,
            narrativeSource = result.narrativeSource.name,
            assessment = mapper.assessment(result.assessment)
        )
    }

    @GetMapping("/{bff}/readiness", produces = [MediaType.TEXT_PLAIN_VALUE])
    @PreAuthorize("hasAnyRole('QA', 'ARCHITECT')")
    fun readinessReport(
        @PathVariable bff: String,
        @RequestParam(defaultValue = "true") color: Boolean
    ): String {
        val result = metrics.timeRegression { analysis.execute(validator.identifier("bff", bff)) }
        return renderer.render(result, color)
    }
}
