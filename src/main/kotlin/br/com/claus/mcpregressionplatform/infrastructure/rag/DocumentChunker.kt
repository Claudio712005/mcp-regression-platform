package br.com.claus.mcpregressionplatform.infrastructure.rag

import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeChunk
import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeDocument
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.stereotype.Component

@Component
class DocumentChunker(private val properties: PlatformProperties) {

    fun chunk(document: KnowledgeDocument): List<KnowledgeChunk> {
        val size = properties.knowledge.chunkSize
        val overlap = properties.knowledge.chunkOverlap.coerceAtMost(size / 2)
        val paragraphs = document.content.split(Regex("\\n{2,}")).map { it.trim() }.filter { it.isNotEmpty() }
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        paragraphs.forEach { paragraph ->
            if (current.isNotEmpty() && current.length + paragraph.length > size) {
                chunks.add(current.toString().trim())
                val tail = current.toString().takeLast(overlap)
                current.setLength(0)
                current.append(tail).append("\n\n")
            }
            current.append(paragraph).append("\n\n")
        }
        if (current.isNotBlank()) {
            chunks.add(current.toString().trim())
        }
        return chunks.mapIndexed { index, text ->
            KnowledgeChunk(
                documentId = document.id,
                title = document.title,
                category = document.category,
                source = document.source,
                ordinal = index,
                text = text
            )
        }
    }
}
