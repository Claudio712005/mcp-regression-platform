package br.com.claus.mcpregressionplatform.domain

import br.com.claus.mcpregressionplatform.domain.contract.ApiContract
import br.com.claus.mcpregressionplatform.domain.contract.ContractComparator
import br.com.claus.mcpregressionplatform.domain.contract.ContractHttpMethod
import br.com.claus.mcpregressionplatform.domain.contract.ContractOperation
import br.com.claus.mcpregressionplatform.domain.contract.ContractViolationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ContractComparatorTest {

    private val comparator = ContractComparator()

    private val expected = ApiContract(
        service = "fintech-srv-account",
        version = "1.4.0",
        operations = listOf(
            ContractOperation("/accounts/{accountId}", ContractHttpMethod.GET, setOf("accountId"), 200, setOf("id", "balance")),
            ContractOperation("/accounts/{accountId}/status", ContractHttpMethod.GET, setOf("accountId"), 200, setOf("status"))
        )
    )

    @Test
    fun `reports compatibility when the published contract matches`() {
        val comparison = comparator.compare(expected, expected.copy(version = "1.4.1"))

        assertThat(comparison.compatible).isTrue()
    }

    @Test
    fun `reports a missing endpoint`() {
        val actual = expected.copy(operations = expected.operations.take(1))

        val comparison = comparator.compare(expected, actual)

        assertThat(comparison.compatible).isFalse()
        assertThat(comparison.violations.map { it.type }).contains(ContractViolationType.MISSING_ENDPOINT)
    }

    @Test
    fun `reports an incompatible method`() {
        val actual = expected.copy(
            operations = listOf(
                expected.operations[0].copy(method = ContractHttpMethod.POST),
                expected.operations[1]
            )
        )

        val comparison = comparator.compare(expected, actual)

        assertThat(comparison.violations.map { it.type }).contains(ContractViolationType.METHOD_NOT_SUPPORTED)
    }

    @Test
    fun `reports a missing required parameter`() {
        val actual = expected.copy(
            operations = listOf(
                expected.operations[0].copy(requiredParameters = emptySet()),
                expected.operations[1]
            )
        )

        val comparison = comparator.compare(expected, actual)

        assertThat(comparison.violations.map { it.type }).contains(ContractViolationType.MISSING_REQUIRED_PARAMETER)
    }

    @Test
    fun `reports an incompatible response payload`() {
        val actual = expected.copy(
            operations = listOf(
                expected.operations[0].copy(responseFields = setOf("id")),
                expected.operations[1]
            )
        )

        val comparison = comparator.compare(expected, actual)

        assertThat(comparison.violations.map { it.type }).contains(ContractViolationType.INCOMPATIBLE_RESPONSE)
    }

    @Test
    fun `reports a missing contract when nothing is published`() {
        val comparison = comparator.compare(expected, null)

        assertThat(comparison.compatible).isFalse()
        assertThat(comparison.actualVersion).isNull()
    }
}
