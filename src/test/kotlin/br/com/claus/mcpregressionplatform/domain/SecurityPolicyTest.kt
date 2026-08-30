package br.com.claus.mcpregressionplatform.domain

import br.com.claus.mcpregressionplatform.domain.security.AuthenticatedPrincipal
import br.com.claus.mcpregressionplatform.domain.security.AuthorizationDecision
import br.com.claus.mcpregressionplatform.domain.security.Capability
import br.com.claus.mcpregressionplatform.domain.security.CapabilityPolicy
import br.com.claus.mcpregressionplatform.domain.security.CapabilityRequirement
import br.com.claus.mcpregressionplatform.domain.security.DenialReason
import br.com.claus.mcpregressionplatform.domain.security.Role
import br.com.claus.mcpregressionplatform.domain.security.ToolClassification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SecurityPolicyTest {

    private val policy = CapabilityPolicy()

    private val runRegression = CapabilityRequirement(
        "run_regression_analysis",
        Capability.RUN_REGRESSION,
        ToolClassification.EXECUTION
    )

    @Test
    fun `grants a capability held by the role`() {
        val decision = policy.decide(AuthenticatedPrincipal("qa", setOf(Role.QA)), runRegression)

        assertThat(decision).isInstanceOf(AuthorizationDecision.Granted::class.java)
    }

    @Test
    fun `denies a capability that the role does not hold`() {
        val decision = policy.decide(AuthenticatedPrincipal("dev", setOf(Role.DEV)), runRegression)

        assertThat(decision).isInstanceOf(AuthorizationDecision.Denied::class.java)
        assertThat((decision as AuthorizationDecision.Denied).reason).isEqualTo(DenialReason.MISSING_CAPABILITY)
    }

    @Test
    fun `denies an unauthenticated request`() {
        val decision = policy.decide(null, runRegression)

        assertThat((decision as AuthorizationDecision.Denied).reason).isEqualTo(DenialReason.MISSING_AUTHENTICATION)
    }

    @Test
    fun `denies a capability that is not declared in the registry`() {
        val decision = policy.decide(AuthenticatedPrincipal("qa", setOf(Role.QA)), null)

        assertThat((decision as AuthorizationDecision.Denied).reason).isEqualTo(DenialReason.UNKNOWN_TOOL)
    }

    @Test
    fun `derives capabilities from roles and never from the request`() {
        val architect = AuthenticatedPrincipal("architect", setOf(Role.ARCHITECT))

        assertThat(architect.capabilities).contains(Capability.READ_ARCHITECTURE, Capability.ADVANCED_ANALYSIS)
        assertThat(architect.holds(Capability.RUN_REGRESSION)).isFalse()
    }
}
