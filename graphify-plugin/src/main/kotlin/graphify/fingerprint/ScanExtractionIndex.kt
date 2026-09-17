package graphify.fingerprint

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

/**
 * Per-scan memoisation of file extractions.
 *
 * A file is parsed at most once per scan. When a previous [ScanCache] is
 * available and incremental mode is on, the index first tries the fast path
 * (size + mtime unchanged → trust the cached hash) and then the content-hash
 * path (a cheap SHA-256 that hits the extraction of an identical file, even one
 * under another path). Only a genuinely new content reaches [FileExtractor].
 *
 * The index owns no policy beyond reuse: assembling the graph from the cached
 * extractions stays the producer's job.
 */
class ScanExtractionIndex(
    private val previous: ScanCache = ScanCache(),
    private val incremental: Boolean = false
) {

    data class Stats(
        val trusted: Int = 0,
        val reusedByHash: Int = 0,
        val parsed: Int = 0
    ) {
        val reused: Int get() = trusted + reusedByHash
        val total: Int get() = trusted + reusedByHash + parsed
    }

    private data class Entry(val fingerprint: FileFingerprint, val extraction: FileExtraction)

    private val entries = LinkedHashMap<String, Entry>()
    private var trustedCount = 0
    private var reusedByHashCount = 0
    private var parsedCount = 0

    private var current: Stats = Stats()

    val stats: Stats get() = current

    fun extraction(file: Path, relativePath: String): FileExtraction {
        entries[relativePath]?.let { return it.extraction }

        val attributes = Files.readAttributes(file, BasicFileAttributes::class.java)
        val size = attributes.size()
        val mtime = attributes.lastModifiedTime().toMillis()

        val (fingerprint, extraction) = resolve(file, relativePath, size, mtime)
        entries[relativePath] = Entry(fingerprint, extraction)
        current = Stats(trusted = trustedCount, reusedByHash = reusedByHashCount, parsed = parsedCount)
        return extraction
    }

    private fun resolve(
        file: Path,
        relativePath: String,
        size: Long,
        mtime: Long
    ): Pair<FileFingerprint, FileExtraction> {
        if (!incremental) return parse(file, relativePath, size, mtime)

        previous.extractionFor(relativePath, size, mtime)?.let { cached ->
            trustedCount++
            return previous.fingerprints.getValue(relativePath) to cached
        }

        val content = Files.readString(file)
        val sha256 = Fingerprinter.digest(content.toByteArray())
        val fingerprint = FileFingerprint(size = size, mtime = mtime, sha256 = sha256)

        previous.extraction(sha256)?.let { cached ->
            reusedByHashCount++
            return fingerprint to cached
        }

        parsedCount++
        return fingerprint to FileExtractor.extract(file.fileName.toString(), content)
    }

    private fun parse(
        file: Path,
        relativePath: String,
        size: Long,
        mtime: Long
    ): Pair<FileFingerprint, FileExtraction> {
        val content = Files.readString(file)
        val fingerprint = FileFingerprint(size = size, mtime = mtime, sha256 = Fingerprinter.digest(content.toByteArray()))
        parsedCount++
        return fingerprint to FileExtractor.extract(file.fileName.toString(), content)
    }

    fun updatedCache(livePaths: Set<String>): ScanCache {
        var cache = ScanCache()
        for ((path, entry) in entries) {
            cache = cache.record(path, entry.fingerprint, entry.extraction)
        }
        return cache.prunedTo(livePaths)
    }
}
