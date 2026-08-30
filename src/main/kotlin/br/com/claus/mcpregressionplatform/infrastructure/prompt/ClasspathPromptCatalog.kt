package br.com.claus.mcpregressionplatform.infrastructure.prompt

import br.com.claus.mcpregressionplatform.application.port.PromptCatalog
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component

class UnknownPromptException(reference: String) : RuntimeException("Unknown internal prompt: $reference")

@Component
class ClasspathPromptCatalog : PromptCatalog {

    private val prompts: Map<String, String> by lazy { load() }

    override fun load(reference: String): String =
        prompts[reference.removeSuffix(".md")] ?: throw UnknownPromptException(reference)

    override fun references(): List<String> = prompts.keys.sorted()

    private fun load(): Map<String, String> {
        val resolver = PathMatchingResourcePatternResolver()
        return resolver.getResources("$ROOT/**/*.md").associate { resource ->
            val uri = resource.url.toString()
            val reference = uri.substringAfter("/prompts/").removeSuffix(".md")
            reference to resource.inputStream.readAllBytes().decodeToString().trim()
        }
    }

    private companion object {
        const val ROOT = "classpath*:prompts"
    }
}
