package br.com.claus.mcpregressionplatform.infrastructure.api

import br.com.claus.mcpregressionplatform.domain.dependency.HealthState
import br.com.claus.mcpregressionplatform.domain.regression.EvidenceSeverity
import br.com.claus.mcpregressionplatform.domain.regression.ReadinessStatus
import br.com.claus.mcpregressionplatform.domain.regression.RegressionAnalysis
import br.com.claus.mcpregressionplatform.domain.regression.RegressionStage
import br.com.claus.mcpregressionplatform.domain.regression.IntegrationSecurityState
import org.springframework.stereotype.Component

@Component
class ConsoleReportRenderer {

    fun render(analysis: RegressionAnalysis, color: Boolean): String {
        val palette = if (color) AnsiPalette.ENABLED else AnsiPalette.DISABLED
        val assessment = analysis.assessment
        return buildString {
            appendLine(palette.bold("MCP REGRESSION PLATFORM"))
            appendLine()
            appendLine(palette.dim("BFF"))
            appendLine(assessment.bff.name)
            appendLine()
            appendLine(palette.dim("Discovering dependencies..."))
            assessment.bff.dependencies.forEach { appendLine("${palette.ok("+")} ${it.name} (${it.type.name})") }
            appendLine()
            appendLine(palette.dim("Running health checks..."))
            assessment.health.forEach {
                val marker = when (it.state) {
                    HealthState.HEALTHY -> palette.ok("+")
                    HealthState.DEGRADED -> palette.warn("!")
                    else -> palette.fail("x")
                }
                appendLine("$marker ${it.dependency.name}: ${it.state.name} (${it.latency.toMillis()}ms) ${it.detail}")
            }
            appendLine()
            appendLine(palette.dim("Validating integration security..."))
            assessment.integrationSecurity.forEach {
                val marker = statusMarker(it.state == IntegrationSecurityState.VALID, palette)
                appendLine("$marker ${it.dependency}: ${it.mechanism} ${it.state.name}")
            }
            appendLine()
            appendLine(palette.dim("Validating contracts..."))
            val contract = assessment.contract
            if (contract == null) {
                appendLine("${palette.warn("-")} skipped by the planner")
            } else {
                appendLine("${statusMarker(contract.compatible, palette)} ${contract.service} ${contract.expectedVersion}")
                contract.violations.forEach {
                    appendLine("  ${palette.fail("x")} ${it.type.name} ${it.operation}: ${it.detail}")
                }
            }
            appendLine()
            appendLine(palette.dim("Running smoke tests..."))
            val smoke = assessment.smoke
            if (smoke == null) {
                appendLine("${palette.warn("-")} skipped by the planner")
            } else {
                smoke.outcomes.forEach {
                    appendLine("${statusMarker(it.passed, palette)} ${it.name}: ${it.detail}")
                }
                appendLine("${statusMarker(smoke.allPassed, palette)} ${smoke.passed}/${smoke.total}")
            }
            appendLine()
            appendLine(palette.dim("Retrieving regression knowledge..."))
            val knowledge = assessment.knowledge
            if (knowledge == null || knowledge.usable.isEmpty()) {
                appendLine("${palette.warn("-")} no knowledge retrieved")
            } else {
                knowledge.usable.forEach { appendLine("${palette.ok("+")} ${it.title} (${it.source})") }
            }
            knowledge?.passages?.filter { it.quarantined }?.forEach {
                appendLine("${palette.warn("!")} quarantined: ${it.title} - ${it.quarantineReason}")
            }
            appendLine()
            appendLine(palette.dim("Stages"))
            RegressionStage.entries.forEach { stage ->
                val marker = when (stage) {
                    in assessment.stagesExecuted -> palette.ok("+")
                    in assessment.stagesSkipped -> palette.warn("-")
                    else -> palette.dim("o")
                }
                appendLine("$marker ${stage.name}")
            }
            appendLine()
            appendLine(palette.bold("Regression readiness: ${statusLabel(assessment.status, palette)}"))
            appendLine()
            appendLine(palette.dim("Analysis (${analysis.narrativeSource.name})"))
            appendLine(analysis.narrative)
            val relevant = assessment.evidence.filter { it.severity != EvidenceSeverity.INFO }
            if (relevant.isNotEmpty()) {
                appendLine()
                appendLine(palette.dim("Evidence"))
                relevant.forEach { appendLine("- [${it.severity.name}] ${it.stage.name} ${it.subject}: ${it.summary}") }
            }
        }
    }

    private fun statusMarker(success: Boolean, palette: AnsiPalette) =
        if (success) palette.ok("+") else palette.fail("x")

    private fun statusLabel(status: ReadinessStatus, palette: AnsiPalette): String = when (status) {
        ReadinessStatus.READY_FOR_REGRESSION -> palette.ok("READY")
        ReadinessStatus.WARNING -> palette.warn("WARNING")
        ReadinessStatus.BLOCKED -> palette.fail("BLOCKED")
    }
}

class AnsiPalette(private val enabled: Boolean) {

    fun ok(text: String) = wrap(text, GREEN)

    fun warn(text: String) = wrap(text, YELLOW)

    fun fail(text: String) = wrap(text, RED)

    fun bold(text: String) = wrap(text, BOLD)

    fun dim(text: String) = wrap(text, DIM)

    private fun wrap(text: String, code: String) = if (enabled) code + text + RESET else text

    companion object {
        val ENABLED = AnsiPalette(true)
        val DISABLED = AnsiPalette(false)
        private const val GREEN = "\u001B[32m"
        private const val YELLOW = "\u001B[33m"
        private const val RED = "\u001B[31m"
        private const val BOLD = "\u001B[1m"
        private const val DIM = "\u001B[2m"
        private const val RESET = "\u001B[0m"
    }
}
