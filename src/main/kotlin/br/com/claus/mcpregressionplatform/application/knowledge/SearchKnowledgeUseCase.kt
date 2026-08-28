package br.com.claus.mcpregressionplatform.application.knowledge

import br.com.claus.mcpregressionplatform.application.port.KnowledgeSearchPort
import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeSearchResult
import br.com.claus.mcpregressionplatform.domain.knowledge.RetrievedPassage
import br.com.claus.mcpregressionplatform.domain.security.injection.InjectionRisk
import br.com.claus.mcpregressionplatform.domain.security.injection.PromptInjectionDetector
import org.springframework.stereotype.Service

@Service
class SearchKnowledgeUseCase(
    private val search: KnowledgeSearchPort,
    private val detector: PromptInjectionDetector
) {

    fun execute(query: String, topK: Int): KnowledgeSearchResult {
        val effectiveTopK = topK.coerceIn(1, MAX_TOP_K)
        val passages = search.search(query, effectiveTopK).map(::screen)
        return KnowledgeSearchResult(query, passages)
    }

    private fun screen(passage: RetrievedPassage): RetrievedPassage {
        val verdict = detector.inspect(passage.text)
        if (verdict.risk != InjectionRisk.HIGH) {
            return passage
        }
        return passage.copy(
            quarantined = true,
            quarantineReason = "Retrieved content contains injection signals: " +
                verdict.signals.joinToString { it.category.name }
        )
    }

    companion object {
        const val MAX_TOP_K = 10
    }
}
