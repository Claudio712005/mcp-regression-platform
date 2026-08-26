package br.com.claus.mcpregressionplatform.domain.knowledge

enum class TrustLevel {
    TRUSTED,
    UNTRUSTED
}

data class KnowledgeDocument(
    val id: String,
    val title: String,
    val category: String,
    val source: String,
    val content: String
)

data class KnowledgeChunk(
    val documentId: String,
    val title: String,
    val category: String,
    val source: String,
    val ordinal: Int,
    val text: String
)

data class RetrievedPassage(
    val title: String,
    val category: String,
    val source: String,
    val text: String,
    val score: Double,
    val trustLevel: TrustLevel = TrustLevel.UNTRUSTED,
    val quarantined: Boolean = false,
    val quarantineReason: String? = null
)

data class KnowledgeSearchResult(
    val query: String,
    val passages: List<RetrievedPassage>
) {
    val usable: List<RetrievedPassage> get() = passages.filterNot { it.quarantined }
}
