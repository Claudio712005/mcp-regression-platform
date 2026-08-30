package br.com.claus.mcpregressionplatform.infrastructure.mcp.security

import br.com.claus.mcpregressionplatform.domain.security.injection.InjectionRisk
import br.com.claus.mcpregressionplatform.domain.security.injection.PromptInjectionDetector
import org.springframework.stereotype.Component

class InvalidToolInputException(message: String) : RuntimeException(message)

@Component
class ToolInputValidator(private val detector: PromptInjectionDetector) {

    fun identifier(field: String, value: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            throw InvalidToolInputException("Parameter $field is required")
        }
        if (trimmed.length > MAX_IDENTIFIER_LENGTH) {
            throw InvalidToolInputException("Parameter $field exceeds $MAX_IDENTIFIER_LENGTH characters")
        }
        if (!IDENTIFIER_PATTERN.matches(trimmed)) {
            throw InvalidToolInputException(
                "Parameter $field accepts only letters, digits, dots, dashes and underscores"
            )
        }
        return trimmed
    }

    fun freeText(field: String, value: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            throw InvalidToolInputException("Parameter $field is required")
        }
        if (trimmed.length > MAX_TEXT_LENGTH) {
            throw InvalidToolInputException("Parameter $field exceeds $MAX_TEXT_LENGTH characters")
        }
        val verdict = detector.inspect(trimmed)
        if (verdict.risk == InjectionRisk.HIGH) {
            throw InvalidToolInputException(
                "Parameter $field was rejected by the prompt injection filter: " +
                    verdict.signals.joinToString { it.category.name }
            )
        }
        return trimmed
    }

    fun boundedInt(field: String, value: Int?, default: Int, minimum: Int, maximum: Int): Int {
        val resolved = value ?: default
        if (resolved < minimum || resolved > maximum) {
            throw InvalidToolInputException("Parameter $field must be between $minimum and $maximum")
        }
        return resolved
    }

    private companion object {
        const val MAX_IDENTIFIER_LENGTH = 120
        const val MAX_TEXT_LENGTH = 400
        val IDENTIFIER_PATTERN = Regex("[A-Za-z0-9._-]+")
    }
}
