package graphify.fingerprint

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

/**
 * Persists a [ScanCache] as a versioned JSON document.
 *
 * The cache is an *optimisation*, never a dependency: a missing, unreadable or
 * corrupted file — or one written by an unknown schema version — degrades to an
 * empty cache so the caller silently falls back to a full scan.
 */
object ScanCacheStore {

    const val VERSION = 1

    private val mapper: ObjectMapper = ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerKotlinModule()

    fun load(file: File): ScanCache {
        if (!file.isFile) return ScanCache()
        return try {
            val document = mapper.readValue<ScanCacheDocument>(file)
            if (document.version != VERSION) ScanCache()
            else ScanCache(fingerprints = document.fingerprints, extractions = document.extractions)
        } catch (_: Exception) {
            ScanCache()
        }
    }

    fun save(file: File, cache: ScanCache) {
        file.parentFile?.mkdirs()
        val document = ScanCacheDocument(
            version = VERSION,
            fingerprints = cache.fingerprints,
            extractions = cache.extractions
        )
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, document)
    }

    data class ScanCacheDocument(
        val version: Int = VERSION,
        val fingerprints: Map<String, FileFingerprint> = emptyMap(),
        val extractions: Map<String, FileExtraction> = emptyMap()
    )
}
