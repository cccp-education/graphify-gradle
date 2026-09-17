package graphify.steps

import graphify.fingerprint.FileExtraction
import graphify.fingerprint.FileFingerprint
import graphify.fingerprint.ScanCache
import graphify.fingerprint.ScanCacheStore
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat
import java.nio.file.Files

class IncrementalScanSteps : En {

    private var cache: ScanCache = ScanCache()
    private var fastPath: Boolean = false
    private var extractionPresent: Boolean = false
    private var loadedCache: ScanCache? = null
    private var cacheFile: java.io.File? = null

    init {

        Given("an empty scan cache") {
            cache = ScanCache()
            loadedCache = null
        }

        Given("a scan cache recording {string} with size {int} and mtime {int}") { path: String, size: Int, mtime: Int ->
            cache = cache.record(
                path = path,
                fingerprint = FileFingerprint(size = size.toLong(), mtime = mtime.toLong(), sha256 = "hash-$path"),
                extraction = FileExtraction(imports = listOf("com.example.From$path"))
            )
        }

        Given("a cache file written by schema version {int}") { version: Int ->
            val dir = Files.createTempDirectory("graphify-cache")
            val file = dir.resolve("fingerprints.json").toFile()
            file.writeText("""{"version":$version,"fingerprints":{},"extractions":{}}""")
            cacheFile = file
        }

        When("the fast path is checked for {string} with size {int} and mtime {int}") { path: String, size: Int, mtime: Int ->
            fastPath = cache.isFastUnchanged(path, size.toLong(), mtime.toLong())
        }

        When("a file with the same content is recorded under {string} with a newer mtime") { path: String ->
            val existing = cache.fingerprints.values.first()
            cache = cache.record(
                path = path,
                fingerprint = existing.copy(mtime = existing.mtime + 5_000),
                extraction = cache.extraction(existing.sha256)!!
            )
        }

        When("the cache is pruned to the live paths {string}") { live: String ->
            cache = cache.prunedTo(setOf(live))
        }

        When("the cache is saved and loaded again") {
            val dir = Files.createTempDirectory("graphify-cache-roundtrip")
            val file = dir.resolve("fingerprints.json").toFile()
            ScanCacheStore.save(file, cache)
            loadedCache = ScanCacheStore.load(file)
        }

        When("the cache file is loaded") {
            loadedCache = ScanCacheStore.load(cacheFile!!)
        }

        Then("the fast path is available") {
            assertThat(fastPath).isTrue()
        }

        Then("the fast path is not available") {
            assertThat(fastPath).isFalse()
        }

        Then("the extraction for {string} is present") { path: String ->
            val fingerprint = cache.fingerprints[path]
            assertThat(fingerprint).isNotNull()
            assertThat(cache.extractionFor(path, fingerprint!!.size, fingerprint.mtime)).isNotNull()
        }

        Then("the extraction for {string} is absent") { path: String ->
            assertThat(cache.extractionFor(path, size = 10, mtime = 1_000)).isNull()
        }

        Then("only one extraction is stored for that content") {
            assertThat(cache.extractions).hasSize(1)
        }

        Then("the cache holds only the path {string}") { path: String ->
            assertThat(cache.fingerprints).containsOnlyKeys(path)
        }

        Then("the extraction of the vanished path is dropped") {
            assertThat(cache.extractions).doesNotContainKey("hash-src/Gone.kt")
        }

        Then("the loaded cache equals the original cache") {
            assertThat(loadedCache).isEqualTo(cache)
        }

        Then("the loaded cache is empty") {
            assertThat(loadedCache).isEqualTo(ScanCache())
        }
    }
}
