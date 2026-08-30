package br.com.claus.mcpregressionplatform.infrastructure.rag

import br.com.claus.mcpregressionplatform.application.port.KnowledgeDocumentSource
import br.com.claus.mcpregressionplatform.application.port.KnowledgeIndexPort
import br.com.claus.mcpregressionplatform.infrastructure.configuration.PlatformProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KnowledgeIngestionRunner {

    private val log = LoggerFactory.getLogger(KnowledgeIngestionRunner::class.java)

    @Bean
    fun knowledgeIngestion(
        source: KnowledgeDocumentSource,
        chunker: DocumentChunker,
        index: KnowledgeIndexPort,
        properties: PlatformProperties
    ) = ApplicationRunner {
        if (!properties.knowledge.ingestOnStartup) {
            return@ApplicationRunner
        }
        if (index.count() > 0) {
            log.info("Knowledge base already populated with {} chunks", index.count())
            return@ApplicationRunner
        }
        val chunks = source.load().flatMap(chunker::chunk)
        index.index(chunks)
        log.info("Ingested {} knowledge chunks", chunks.size)
    }
}
