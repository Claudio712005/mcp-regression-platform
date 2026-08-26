package br.com.claus.mcpregressionplatform.domain.contract

class ContractComparator {

    fun compare(expected: ApiContract, actual: ApiContract?): ContractComparison {
        if (actual == null) {
            return ContractComparison(
                service = expected.service,
                expectedVersion = expected.version,
                actualVersion = null,
                violations = listOf(
                    ContractViolation(
                        type = ContractViolationType.MISSING_ENDPOINT,
                        operation = expected.service,
                        detail = "No contract published by the service"
                    )
                )
            )
        }
        val violations = expected.operations.flatMap { compareOperation(it, actual) }
        return ContractComparison(
            service = expected.service,
            expectedVersion = expected.version,
            actualVersion = actual.version,
            violations = violations
        )
    }

    private fun compareOperation(expected: ContractOperation, actual: ApiContract): List<ContractViolation> {
        val sameMethod = actual.operation(expected.path, expected.method)
        if (sameMethod == null) {
            val samePath = actual.operationsForPath(expected.path)
            return if (samePath.isEmpty()) {
                listOf(
                    ContractViolation(
                        ContractViolationType.MISSING_ENDPOINT,
                        expected.signature,
                        "Endpoint missing from the published contract"
                    )
                )
            } else {
                listOf(
                    ContractViolation(
                        ContractViolationType.METHOD_NOT_SUPPORTED,
                        expected.signature,
                        "Available methods: ${samePath.joinToString { it.method.name }}"
                    )
                )
            }
        }
        val violations = mutableListOf<ContractViolation>()
        val missingParameters = expected.requiredParameters - sameMethod.requiredParameters
        if (missingParameters.isNotEmpty()) {
            violations.add(
                ContractViolation(
                    ContractViolationType.MISSING_REQUIRED_PARAMETER,
                    expected.signature,
                    "Missing required parameters: ${missingParameters.sorted().joinToString()}"
                )
            )
        }
        if (expected.successStatus != sameMethod.successStatus) {
            violations.add(
                ContractViolation(
                    ContractViolationType.INCOMPATIBLE_SUCCESS_STATUS,
                    expected.signature,
                    "Expected ${expected.successStatus}, published ${sameMethod.successStatus}"
                )
            )
        }
        val missingFields = expected.responseFields - sameMethod.responseFields
        if (missingFields.isNotEmpty()) {
            violations.add(
                ContractViolation(
                    ContractViolationType.INCOMPATIBLE_RESPONSE,
                    expected.signature,
                    "Missing response fields: ${missingFields.sorted().joinToString()}"
                )
            )
        }
        return violations
    }
}
