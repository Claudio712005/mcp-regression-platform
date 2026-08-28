package br.com.claus.mcpregressionplatform.application.agent

import br.com.claus.mcpregressionplatform.domain.regression.EvidenceSeverity
import br.com.claus.mcpregressionplatform.domain.regression.ReadinessStatus
import br.com.claus.mcpregressionplatform.domain.regression.RegressionAssessment
import org.springframework.stereotype.Component

@Component
class EvidenceRenderer {

    fun evidenceBlock(assessment: RegressionAssessment): String = buildString {
        appendLine("bff: ${assessment.bff.name}")
        appendLine("status: ${assessment.status.name}")
        appendLine("stages_executed: ${assessment.stagesExecuted.joinToString { it.name }}")
        if (assessment.stagesSkipped.isNotEmpty()) {
            appendLine("stages_skipped: ${assessment.stagesSkipped.joinToString { it.name }}")
        }
        assessment.evidence.forEach {
            appendLine("- [${it.severity.name}] ${it.stage.name} ${it.subject}: ${it.summary}")
        }
    }

    fun knowledgeBlock(assessment: RegressionAssessment): String {
        val knowledge = assessment.knowledge ?: return "no knowledge retrieved"
        if (knowledge.usable.isEmpty()) {
            return "no usable knowledge retrieved"
        }
        return knowledge.usable.joinToString(separator = "\n\n") {
            "source=${it.source} title=${it.title} score=${"%.3f".format(it.score)}\n${it.text}"
        }
    }

    fun deterministicNarrative(assessment: RegressionAssessment): String = buildString {
        appendLine(
            when (assessment.status) {
                ReadinessStatus.READY_FOR_REGRESSION ->
                    "The environment is ready for regression of ${assessment.bff.name}."
                ReadinessStatus.WARNING ->
                    "The environment can run a regression of ${assessment.bff.name}, but degraded conditions were detected."
                ReadinessStatus.BLOCKED ->
                    "The regression of ${assessment.bff.name} is blocked."
            }
        )
        val blockers = assessment.evidence.filter { it.severity == EvidenceSeverity.BLOCKER }
        if (blockers.isNotEmpty()) {
            appendLine()
            appendLine("Blocking findings:")
            blockers.forEach { appendLine("- ${it.subject}: ${it.summary}") }
        }
        val warnings = assessment.evidence.filter { it.severity == EvidenceSeverity.WARNING }
        if (warnings.isNotEmpty()) {
            appendLine()
            appendLine("Warnings:")
            warnings.forEach { appendLine("- ${it.subject}: ${it.summary}") }
        }
        val runbooks = assessment.knowledge?.usable.orEmpty()
        if (runbooks.isNotEmpty()) {
            appendLine()
            appendLine("Related knowledge: ${runbooks.joinToString { it.title }}")
        }
    }.trim()
}
