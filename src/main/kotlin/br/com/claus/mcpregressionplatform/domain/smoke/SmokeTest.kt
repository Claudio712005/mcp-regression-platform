package br.com.claus.mcpregressionplatform.domain.smoke

import java.time.Duration

data class SmokeTestOutcome(
    val id: String,
    val name: String,
    val passed: Boolean,
    val detail: String,
    val duration: Duration
)

data class SmokeTestSuiteResult(
    val target: String,
    val outcomes: List<SmokeTestOutcome>
) {
    val total: Int get() = outcomes.size
    val passed: Int get() = outcomes.count { it.passed }
    val allPassed: Boolean get() = outcomes.isNotEmpty() && passed == total
    val failures: List<SmokeTestOutcome> get() = outcomes.filterNot { it.passed }
}
