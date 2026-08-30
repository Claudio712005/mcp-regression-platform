package br.com.claus.mcpregressionplatform.infrastructure.llm

import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.math.sqrt

@Configuration
class EmbeddingConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel::class)
    fun lexicalEmbeddingModel(properties: PlatformProperties): EmbeddingModel =
        LexicalHashingEmbeddingModel(properties.ai.embeddingDimensions)
}

class LexicalHashingEmbeddingModel(private val dimensions: Int) : EmbeddingModel {

    override fun call(request: EmbeddingRequest): EmbeddingResponse =
        EmbeddingResponse(request.instructions.mapIndexed { index, text -> Embedding(vectorOf(text), index) })

    override fun embed(document: Document): FloatArray = vectorOf(document.text.orEmpty())

    override fun dimensions(): Int = dimensions

    private fun vectorOf(text: String): FloatArray {
        val vector = FloatArray(dimensions)
        TOKEN_PATTERN.findAll(text.lowercase()).forEach { match ->
            val token = match.value
            if (token.length < 3) {
                return@forEach
            }
            val bucket = Math.floorMod(token.hashCode(), dimensions)
            vector[bucket] += 1f
            val bigramBucket = Math.floorMod(token.hashCode() * 31 + token.length, dimensions)
            vector[bigramBucket] += 0.5f
        }
        val norm = sqrt(vector.fold(0.0) { acc, value -> acc + value * value }).toFloat()
        if (norm == 0f) {
            vector[0] = 1f
            return vector
        }
        for (index in vector.indices) {
            vector[index] = vector[index] / norm
        }
        return vector
    }

    private companion object {
        val TOKEN_PATTERN = Regex("[a-z0-9-]+")
    }
}
