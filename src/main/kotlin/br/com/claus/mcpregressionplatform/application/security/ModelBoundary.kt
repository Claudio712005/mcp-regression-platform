package br.com.claus.mcpregressionplatform.application.security

import br.com.claus.mcpregressionplatform.domain.security.injection.InjectionRisk
import br.com.claus.mcpregressionplatform.domain.security.injection.PromptInjectionDetector
import org.springframework.stereotype.Component

object UntrustedContentEnvelope {

    const val OPEN = "<untrusted-data>"
    const val CLOSE = "</untrusted-data>"

    fun wrap(content: String): String = buildString {
        appendLine(OPEN)
        appendLine(content.replace(OPEN, "&lt;untrusted-data&gt;").replace(CLOSE, "&lt;/untrusted-data&gt;"))
        append(CLOSE)
    }
}

@Component
class ModelOutputGuard(private val detector: PromptInjectionDetector) {

    fun sanitize(output: String, expectedStatus: String): String? {
        if (output.isBlank()) {
            return null
        }
        if (output.length > MAX_OUTPUT_LENGTH) {
            return null
        }
        if (detector.inspect(output).risk == InjectionRisk.HIGH) {
            return null
        }
        if (LEAKED_MARKERS.any { output.contains(it, ignoreCase = true) }) {
            return null
        }
        val contradicts = FORBIDDEN_STATUS_CLAIMS
            .filterNot { it == expectedStatus }
            .any { output.contains(it, ignoreCase = false) }
        if (contradicts) {
            return null
        }
        return output.trim()
    }

    companion object {
        const val MAX_OUTPUT_LENGTH = 8000
        private val LEAKED_MARKERS = listOf(
            "SECURITY POLICY (SYSTEM)",
            "AGENT IDENTITY (SYSTEM)",
            UntrustedContentEnvelope.OPEN
        )
        private val FORBIDDEN_STATUS_CLAIMS = listOf("READY_FOR_REGRESSION", "BLOCKED", "WARNING")
    }
}
