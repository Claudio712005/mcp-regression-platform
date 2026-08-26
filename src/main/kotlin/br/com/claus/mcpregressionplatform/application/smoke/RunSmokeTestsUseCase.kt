package br.com.claus.mcpregressionplatform.application.smoke

import br.com.claus.mcpregressionplatform.application.dependency.UnknownBffException
import br.com.claus.mcpregressionplatform.application.port.BffRegistry
import br.com.claus.mcpregressionplatform.application.port.SmokeTestRunner
import br.com.claus.mcpregressionplatform.domain.smoke.SmokeTestSuiteResult
import org.springframework.stereotype.Service

@Service
class RunSmokeTestsUseCase(
    private val registry: BffRegistry,
    private val runner: SmokeTestRunner
) {

    fun execute(bffName: String): SmokeTestSuiteResult {
        val bff = registry.findByName(bffName) ?: throw UnknownBffException(bffName)
        return runner.run(bff)
    }
}
