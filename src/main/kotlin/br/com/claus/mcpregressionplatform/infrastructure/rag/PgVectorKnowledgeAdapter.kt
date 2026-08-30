package br.com.claus.mcpregressionplatform.infrastructure.rag

import br.com.claus.mcpregressionplatform.application.port.KnowledgeIndexPort
import br.com.claus.mcpregressionplatform.application.port.KnowledgeSearchPort
import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeChunk
import br.com.claus.mcpregressionplatform.domain.knowledge.RetrievedPassage
import br.com.claus.mcpregressionplatform.domain.knowledge.TrustLevel
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PgVectorKnowledgeAdapter(
    private val vectorStore: VectorStore,
    private val jdbcClient: JdbcClient,
    private val properties: PlatformProperties
) : KnowledgeSearchPort, KnowledgeIndexPort {

    override fun index(chunks: List<KnowledgeChunk>) {
        if (chunks.isEmpty()) {
            return
        }
        vectorStore.add(
            chunks.map { chunk ->
                Document.builder()
                    .id(deterministicId(chunk))
                    .text(embeddableText(chunk))
                    .metadata(
                        mapOf(
                            "title" to chunk.title,
                            "category" to chunk.category,
                            "source" to chunk.source,
                            "ordinal" to chunk.ordinal
                        )
                    )
                    .build()
            }
        )
    }

    private fun embeddableText(chunk: KnowledgeChunk): String =
        chunk.title + " | " + chunk.category + " | " + chunk.source + "\n\n" + chunk.text

    private fun deterministicId(chunk: KnowledgeChunk): String =
        UUID.nameUUIDFromBytes((chunk.documentId + "#" + chunk.ordinal).toByteArray()).toString()

    override fun count(): Long =
        jdbcClient.sql("select count(*) from vector_store").query(Long::class.java).single()

    override fun search(query: String, topK: Int): List<RetrievedPassage> {
        val request = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(properties.knowledge.minimumScore)
            .build()
        return vectorStore.similaritySearch(request).orEmpty().map { document ->
            RetrievedPassage(
                title = document.metadata["title"]?.toString() ?: "unknown",
                category = document.metadata["category"]?.toString() ?: "general",
                source = document.metadata["source"]?.toString() ?: "unknown",
                text = document.text.orEmpty(),
                score = document.score ?: 0.0,
                trustLevel = TrustLevel.UNTRUSTED
            )
        }
    }
}
