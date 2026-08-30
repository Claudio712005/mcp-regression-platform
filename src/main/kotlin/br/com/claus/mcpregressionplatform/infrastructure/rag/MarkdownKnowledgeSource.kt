package br.com.claus.mcpregressionplatform.infrastructure.rag

import br.com.claus.mcpregressionplatform.application.port.KnowledgeDocumentSource
import br.com.claus.mcpregressionplatform.domain.knowledge.KnowledgeDocument
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component

@Component
class MarkdownKnowledgeSource(private val properties: PlatformProperties) : KnowledgeDocumentSource {

    override fun load(): List<KnowledgeDocument> {
        val resolver = PathMatchingResourcePatternResolver()
        return resolver.getResources(properties.knowledge.location + "**/*.md").map { resource ->
            val content = resource.inputStream.readAllBytes().decodeToString()
            val path = resource.url.toString().substringAfter("/knowledge/")
            KnowledgeDocument(
                id = path,
                title = titleOf(content, path),
                category = path.substringBefore('/', "general"),
                source = "knowledge/$path",
                content = content
            )
        }
    }

    private fun titleOf(content: String, path: String): String =
        content.lineSequence()
            .firstOrNull { it.startsWith("# ") }
            ?.removePrefix("# ")
            ?.trim()
            ?: path.substringAfterLast('/').removeSuffix(".md")
}
