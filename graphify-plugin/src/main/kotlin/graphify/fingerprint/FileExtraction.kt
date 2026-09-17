package graphify.fingerprint

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * The *file-local*, resolvable-without-the-world extraction of a source file.
 *
 * Only content-derived facts live here. Anything that depends on sibling
 * files (import → directory, reference → existing file, section dedup and
 * subsection hierarchy) is resolved by the producer at assembly time and is
 * deliberately absent from the cache.
 */
data class FileExtraction(
    val imports: List<String> = emptyList(),
    val adocReferences: List<String> = emptyList(),
    val tocReferences: List<String> = emptyList(),
    val agentReferences: List<String> = emptyList(),
    val sections: List<SectionExtraction> = emptyList()
) {
    @get:JsonIgnore
    val isEmpty: Boolean
        get() = imports.isEmpty() &&
                adocReferences.isEmpty() &&
                tocReferences.isEmpty() &&
                agentReferences.isEmpty() &&
                sections.isEmpty()

    companion object {
        val EMPTY = FileExtraction()
    }
}

data class SectionExtraction(
    val title: String,
    val level: Int,
    val line: Int
)
