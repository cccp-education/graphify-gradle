package graphify.fingerprint

/**
 * In-memory view of a previous scan, persisted by [ScanCacheStore].
 *
 * Two lookups coexist on purpose: [fingerprints] is keyed by path (answers
 * "did this file change?") while [extractions] is keyed by content hash
 * (answers "has this exact content already been parsed?"). The indirection
 * lets an untouched file reuse a parse it never produced, and lets identical
 * content be parsed once regardless of how many paths hold it.
 */
data class ScanCache(
    val fingerprints: Map<String, FileFingerprint> = emptyMap(),
    val extractions: Map<String, FileExtraction> = emptyMap()
) {

    fun record(path: String, fingerprint: FileFingerprint, extraction: FileExtraction): ScanCache =
        copy(
            fingerprints = fingerprints + (path to fingerprint),
            extractions = extractions + (fingerprint.sha256 to extraction)
        )

    fun isFastUnchanged(path: String, size: Long, mtime: Long): Boolean =
        fingerprints[path]?.matches(size, mtime) == true

    /**
     * Extraction reuse through the fast path: the path is known *and* its
     * fingerprint still holds, so the cached content hash is trusted.
     */
    fun extractionFor(path: String, size: Long, mtime: Long): FileExtraction? {
        val fingerprint = fingerprints[path] ?: return null
        if (!fingerprint.matches(size, mtime)) return null
        return extraction(fingerprint.sha256)
    }

    fun extraction(sha256: String): FileExtraction? = extractions[sha256]

    /**
     * Drops fingerprints of paths that no longer exist and every extraction
     * no surviving fingerprint refers to, so the cache never grows without
     * bound across renames and deletions.
     */
    fun prunedTo(livePaths: Set<String>): ScanCache {
        val liveFingerprints = fingerprints.filterKeys { it in livePaths }
        val liveHashes = liveFingerprints.values.map { it.sha256 }.toSet()
        return ScanCache(
            fingerprints = liveFingerprints,
            extractions = extractions.filterKeys { it in liveHashes }
        )
    }
}
