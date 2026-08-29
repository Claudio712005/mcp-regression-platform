package br.com.claus.mcpregressionplatform.infrastructure.configuration

import br.com.claus.mcpregressionplatform.domain.contract.ContractComparator
import br.com.claus.mcpregressionplatform.domain.dependency.HealthClassifier
import br.com.claus.mcpregressionplatform.domain.dependency.LatencyPolicy
import br.com.claus.mcpregressionplatform.domain.regression.ReadinessPolicy
import br.com.claus.mcpregressionplatform.domain.security.CapabilityPolicy
import br.com.claus.mcpregressionplatform.domain.security.injection.PromptInjectionDetector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainConfiguration {

    @Bean
    fun latencyPolicy(properties: PlatformProperties) =
        LatencyPolicy(properties.health.warnAbove, properties.health.failAbove)

    @Bean
    fun healthClassifier(latencyPolicy: LatencyPolicy) = HealthClassifier(latencyPolicy)

    @Bean
    fun contractComparator() = ContractComparator()

    @Bean
    fun readinessPolicy() = ReadinessPolicy()

    @Bean
    fun capabilityPolicy() = CapabilityPolicy()

    @Bean
    fun promptInjectionDetector() = PromptInjectionDetector()
}
