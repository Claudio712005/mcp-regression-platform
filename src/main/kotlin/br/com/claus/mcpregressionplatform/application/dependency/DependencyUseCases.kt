package br.com.claus.mcpregressionplatform.application.dependency

import br.com.claus.mcpregressionplatform.application.port.BffRegistry
import br.com.claus.mcpregressionplatform.application.port.DependencyHealthProbe
import br.com.claus.mcpregressionplatform.application.port.IntegrationSecurityInspector
import br.com.claus.mcpregressionplatform.domain.dependency.BffDefinition
import br.com.claus.mcpregressionplatform.domain.dependency.HealthCheckResult
import br.com.claus.mcpregressionplatform.domain.dependency.HealthClassifier
import br.com.claus.mcpregressionplatform.domain.dependency.ServiceDependency
import br.com.claus.mcpregressionplatform.domain.regression.IntegrationSecurityCheck
import org.springframework.stereotype.Service

class UnknownBffException(name: String) : RuntimeException("Unknown BFF: $name")

class UnknownDependencyException(bff: String, dependency: String) :
    RuntimeException("Dependency $dependency is not declared for BFF $bff")

@Service
class DiscoverDependenciesUseCase(private val registry: BffRegistry) {

    fun listBffs(): List<BffDefinition> = registry.findAll()

    fun execute(bffName: String): BffDefinition =
        registry.findByName(bffName) ?: throw UnknownBffException(bffName)
}

@Service
class CheckDependencyHealthUseCase(
    private val registry: BffRegistry,
    private val probes: List<DependencyHealthProbe>,
    private val classifier: HealthClassifier
) {

    fun executeForBff(bffName: String): List<HealthCheckResult> {
        val bff = registry.findByName(bffName) ?: throw UnknownBffException(bffName)
        return bff.dependencies.map { check(it) }
    }

    fun executeForDependency(bffName: String, dependencyName: String): HealthCheckResult {
        val bff = registry.findByName(bffName) ?: throw UnknownBffException(bffName)
        val dependency = bff.dependency(dependencyName)
            ?: throw UnknownDependencyException(bffName, dependencyName)
        return check(dependency)
    }

    private fun check(dependency: ServiceDependency): HealthCheckResult {
        val probe = probes.firstOrNull { it.supports(dependency) }
            ?: throw IllegalStateException("No probe registered for dependency type ${dependency.type}")
        return classifier.classify(dependency, probe.probe(dependency))
    }
}

@Service
class ValidateIntegrationSecurityUseCase(
    private val registry: BffRegistry,
    private val inspectors: List<IntegrationSecurityInspector>
) {

    fun execute(bffName: String): List<IntegrationSecurityCheck> {
        val bff = registry.findByName(bffName) ?: throw UnknownBffException(bffName)
        return bff.dependencies.flatMap { dependency ->
            inspectors.map { it.inspect(dependency) }
        }
    }
}
