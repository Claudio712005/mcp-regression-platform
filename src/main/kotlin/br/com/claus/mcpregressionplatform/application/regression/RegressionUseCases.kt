package br.com.claus.mcpregressionplatform.application.regression

import br.com.claus.mcpregressionplatform.application.agent.EvidenceNarrator
import br.com.claus.mcpregressionplatform.application.agent.RegressionWorkflow
import br.com.claus.mcpregressionplatform.domain.regression.RegressionAnalysis
import br.com.claus.mcpregressionplatform.domain.regression.RegressionAssessment
import org.springframework.stereotype.Service

@Service
class GetRegressionStatusUseCase(private val workflow: RegressionWorkflow) {

    fun execute(bffName: String): RegressionAssessment = workflow.execute(bffName)
}

@Service
class RunRegressionAnalysisUseCase(
    private val workflow: RegressionWorkflow,
    private val narrator: EvidenceNarrator
) {

    fun execute(bffName: String): RegressionAnalysis = narrator.narrate(workflow.execute(bffName))
}
