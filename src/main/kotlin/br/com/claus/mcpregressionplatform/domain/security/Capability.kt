package br.com.claus.mcpregressionplatform.domain.security

enum class Capability {
    READ_DEPENDENCIES,
    CHECK_HEALTH,
    VALIDATE_CONTRACT,
    RUN_SMOKE_TEST,
    SEARCH_KNOWLEDGE,
    RUN_REGRESSION,
    READ_ARCHITECTURE,
    ADVANCED_ANALYSIS
}

enum class ToolClassification {
    READ,
    VALIDATION,
    EXECUTION
}

enum class Role(val capabilities: Set<Capability>) {
    DEV(
        setOf(
            Capability.READ_DEPENDENCIES,
            Capability.CHECK_HEALTH,
            Capability.VALIDATE_CONTRACT,
            Capability.RUN_SMOKE_TEST,
            Capability.SEARCH_KNOWLEDGE
        )
    ),
    QA(
        DEV.capabilities + setOf(Capability.RUN_REGRESSION)
    ),
    ARCHITECT(
        DEV.capabilities + setOf(Capability.READ_ARCHITECTURE, Capability.ADVANCED_ANALYSIS)
    );

    companion object {
        fun from(value: String): Role? = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
