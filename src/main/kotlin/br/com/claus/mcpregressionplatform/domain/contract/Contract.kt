package br.com.claus.mcpregressionplatform.domain.contract

enum class ContractHttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE
}

data class ContractOperation(
    val path: String,
    val method: ContractHttpMethod,
    val requiredParameters: Set<String>,
    val successStatus: Int,
    val responseFields: Set<String>
) {
    val signature: String get() = "${method.name} $path"
}

data class ApiContract(
    val service: String,
    val version: String,
    val operations: List<ContractOperation>
) {
    fun operation(path: String, method: ContractHttpMethod): ContractOperation? =
        operations.firstOrNull { it.path == path && it.method == method }

    fun operationsForPath(path: String): List<ContractOperation> = operations.filter { it.path == path }
}

enum class ContractViolationType {
    MISSING_ENDPOINT,
    METHOD_NOT_SUPPORTED,
    MISSING_REQUIRED_PARAMETER,
    INCOMPATIBLE_SUCCESS_STATUS,
    INCOMPATIBLE_RESPONSE
}

data class ContractViolation(
    val type: ContractViolationType,
    val operation: String,
    val detail: String
)

data class ContractComparison(
    val service: String,
    val expectedVersion: String,
    val actualVersion: String?,
    val violations: List<ContractViolation>
) {
    val compatible: Boolean get() = violations.isEmpty()
}
