package br.com.claus.mcpregressionplatform.domain.security.injection

enum class InjectionCategory {
    INSTRUCTION_OVERRIDE,
    SYSTEM_PROMPT_EXFILTRATION,
    PRIVILEGE_ESCALATION,
    UNRESTRICTED_EXECUTION,
    CREDENTIAL_EXFILTRATION,
    EXTERNAL_INSTRUCTION_SOURCE
}

enum class InjectionRisk {
    NONE,
    LOW,
    HIGH
}

data class InjectionSignal(
    val category: InjectionCategory,
    val evidence: String
)

data class InjectionVerdict(
    val risk: InjectionRisk,
    val signals: List<InjectionSignal>
) {
    val rejected: Boolean get() = risk == InjectionRisk.HIGH
}

class PromptInjectionDetector(private val rules: List<InjectionRule> = DEFAULT_RULES) {

    fun inspect(content: String): InjectionVerdict {
        if (content.isBlank()) {
            return InjectionVerdict(InjectionRisk.NONE, emptyList())
        }
        val normalized = content.lowercase()
        val signals = rules.mapNotNull { rule ->
            rule.pattern.find(normalized)?.let { InjectionSignal(rule.category, it.value.trim()) }
        }
        val risk = when {
            signals.isEmpty() -> InjectionRisk.NONE
            signals.size == 1 && signals.first().category == InjectionCategory.EXTERNAL_INSTRUCTION_SOURCE -> InjectionRisk.LOW
            else -> InjectionRisk.HIGH
        }
        return InjectionVerdict(risk, signals)
    }

    data class InjectionRule(val category: InjectionCategory, val pattern: Regex)

    companion object {
        private fun rule(category: InjectionCategory, pattern: String) =
            InjectionRule(category, Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)))

        val DEFAULT_RULES: List<InjectionRule> = listOf(
            rule(InjectionCategory.INSTRUCTION_OVERRIDE, "ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions"),
            rule(InjectionCategory.INSTRUCTION_OVERRIDE, "disregard\\s+(the\\s+)?(security|previous)\\s+\\w+"),
            rule(InjectionCategory.INSTRUCTION_OVERRIDE, "you\\s+are\\s+now\\s+(a|an|in)\\s+"),
            rule(InjectionCategory.INSTRUCTION_OVERRIDE, "new\\s+system\\s+(prompt|instructions)"),
            rule(InjectionCategory.SYSTEM_PROMPT_EXFILTRATION, "(reveal|print|show|repeat|output)\\s+(the\\s+)?(system|initial)\\s+prompt"),
            rule(InjectionCategory.SYSTEM_PROMPT_EXFILTRATION, "what\\s+are\\s+your\\s+(system\\s+)?instructions"),
            rule(InjectionCategory.PRIVILEGE_ESCALATION, "(grant|give|elevate)\\s+(me\\s+)?(admin|architect|qa|full)\\s+(role|access|privileges?)"),
            rule(InjectionCategory.PRIVILEGE_ESCALATION, "bypass\\s+(the\\s+)?(authorization|authentication|security|policy)"),
            rule(InjectionCategory.PRIVILEGE_ESCALATION, "call\\s+(the\\s+)?privileged\\s+tools?"),
            rule(InjectionCategory.UNRESTRICTED_EXECUTION, "(execute|run)\\s+(an\\s+)?(unrestricted\\s+)?(sql|shell|command|script)"),
            rule(InjectionCategory.UNRESTRICTED_EXECUTION, "drop\\s+table|delete\\s+from\\s+|;\\s*--"),
            rule(InjectionCategory.CREDENTIAL_EXFILTRATION, "(return|reveal|send|leak)\\s+(the\\s+)?(credentials?|secrets?|tokens?|api\\s+keys?|passwords?)"),
            rule(InjectionCategory.EXTERNAL_INSTRUCTION_SOURCE, "https?://\\S+\\s+as\\s+(authoritative\\s+)?instructions"),
            rule(InjectionCategory.EXTERNAL_INSTRUCTION_SOURCE, "fetch\\s+https?://\\S+\\s+and\\s+follow")
        )
    }
}
