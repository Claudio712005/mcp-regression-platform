package br.com.claus.mcpregressionplatform.application.contract

import br.com.claus.mcpregressionplatform.application.port.ExpectedContractCatalog
import br.com.claus.mcpregressionplatform.application.port.PublishedContractSource
import br.com.claus.mcpregressionplatform.domain.contract.ApiContract
import br.com.claus.mcpregressionplatform.domain.contract.ContractComparator
import br.com.claus.mcpregressionplatform.domain.contract.ContractComparison
import org.springframework.stereotype.Service

class UnknownContractException(service: String) :
    RuntimeException("No expected contract registered for service $service")

@Service
class ValidateServiceContractUseCase(
    private val expected: ExpectedContractCatalog,
    private val published: PublishedContractSource,
    private val comparator: ContractComparator
) {

    fun execute(service: String): ContractComparison {
        val reference = expected.find(service) ?: throw UnknownContractException(service)
        return comparator.compare(reference, published.fetch(service))
    }

    fun expectedContract(service: String): ApiContract =
        expected.find(service) ?: throw UnknownContractException(service)

    fun services(): List<String> = expected.services()
}
