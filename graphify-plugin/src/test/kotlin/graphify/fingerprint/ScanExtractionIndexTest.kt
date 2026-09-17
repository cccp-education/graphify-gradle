package graphify.fingerprint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class ScanExtractionIndexTest {

    @TempDir
    lateinit var tempDir: Path

    private fun write(name: String, content: String): Path {
        val file = tempDir.resolve(name)
        file.parent?.let { Files.createDirectories(it) }
        file.writeText(content)
        return file
    }

    @Test
    fun `should parse the content when no cache is supplied`() {
        val file = write("App.kt", "import com.example.Util")
        val index = ScanExtractionIndex()

        val extraction = index.extraction(file, "App.kt")

        assertThat(extraction.imports).containsExactly("com.example.Util")
    }

    @Test
    fun `should parse each path only once`() {
        val file = write("App.kt", "import com.example.Util")
        val index = ScanExtractionIndex()

        val first = index.extraction(file, "App.kt")
        val second = index.extraction(file, "App.kt")

        assertThat(second).isSameAs(first)
    }

    @Test
    fun `should not read a file the fast check trusts`() {
        val file = write("App.kt", "import com.example.Util")
        val stat = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes::class.java)
        val cached = ScanCache().record(
            path = "App.kt",
            fingerprint = FileFingerprint(
                size = stat.size(),
                mtime = stat.lastModifiedTime().toMillis(),
                sha256 = "cached-hash"
            ),
            extraction = FileExtraction(imports = listOf("com.example.FromCache"))
        )
        val index = ScanExtractionIndex(previous = cached, incremental = true)

        val extraction = index.extraction(file, "App.kt")

        assertThat(extraction.imports).containsExactly("com.example.FromCache")
    }

    @Test
    fun `should reuse the cached extraction by content hash when only the mtime drifted`() {
        val file = write("App.kt", "import com.example.Util")
        val contentHash = Fingerprinter.sha256(file)
        val cached = ScanCache()
            .record(
                path = "App.kt",
                fingerprint = FileFingerprint(size = 0, mtime = 1L, sha256 = contentHash),
                extraction = FileExtraction(imports = listOf("com.example.ByHash"))
            )
        val index = ScanExtractionIndex(previous = cached, incremental = true)

        val extraction = index.extraction(file, "App.kt")

        assertThat(extraction.imports).containsExactly("com.example.ByHash")
    }

    @Test
    fun `should re-parse when the previous extraction is unknown`() {
        val file = write("App.kt", "import com.example.Util")
        val cached = ScanCache().record(
            path = "App.kt",
            fingerprint = FileFingerprint(size = 0, mtime = 1L, sha256 = "stale"),
            extraction = FileExtraction()
        )
        val index = ScanExtractionIndex(previous = cached, incremental = true)

        val extraction = index.extraction(file, "App.kt")

        assertThat(extraction.imports).containsExactly("com.example.Util")
    }

    @Test
    fun `should ignore the cache when incremental is disabled`() {
        val file = write("App.kt", "import com.example.Util")
        val stat = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes::class.java)
        val cached = ScanCache().record(
            path = "App.kt",
            fingerprint = FileFingerprint(stat.size(), stat.lastModifiedTime().toMillis(), "hash"),
            extraction = FileExtraction(imports = listOf("com.example.FromCache"))
        )
        val index = ScanExtractionIndex(previous = cached, incremental = false)

        val extraction = index.extraction(file, "App.kt")

        assertThat(extraction.imports).containsExactly("com.example.Util")
    }

    @Test
    fun `should expose a cache holding the fingerprints of the scanned paths`() {
        val file = write("App.kt", "import com.example.Util")
        val index = ScanExtractionIndex(incremental = true)

        index.extraction(file, "App.kt")
        val cache = index.updatedCache(setOf("App.kt"))

        assertThat(cache.fingerprints).containsOnlyKeys("App.kt")
        assertThat(cache.extraction(cache.fingerprints.getValue("App.kt").sha256)).isNotNull()
    }

    @Test
    fun `should drop the fingerprints of vanished paths from the updated cache`() {
        val file = write("App.kt", "import com.example.Util")
        val index = ScanExtractionIndex(incremental = true)

        index.extraction(file, "App.kt")
        val cache = index.updatedCache(emptySet())

        assertThat(cache.fingerprints).isEmpty()
        assertThat(cache.extractions).isEmpty()
    }

    @Test
    fun `should report how many files were parsed, reused and trusted`() {
        val fast = write("Fast.kt", "import a.B")
        val hashed = write("Hashed.kt", "import b.C")
        val fresh = write("Fresh.kt", "import c.D")
        val fastStat = Files.readAttributes(fast, java.nio.file.attribute.BasicFileAttributes::class.java)
        val hashedHash = Fingerprinter.sha256(hashed)
        val cached = ScanCache()
            .record(
                path = "Fast.kt",
                fingerprint = FileFingerprint(fastStat.size(), fastStat.lastModifiedTime().toMillis(), "fast-hash"),
                extraction = FileExtraction(imports = listOf("a.B"))
            )
            .record(
                path = "Hashed.kt",
                fingerprint = FileFingerprint(0, 1L, hashedHash),
                extraction = FileExtraction(imports = listOf("b.C"))
            )
        val index = ScanExtractionIndex(previous = cached, incremental = true)

        index.extraction(fast, "Fast.kt")
        index.extraction(hashed, "Hashed.kt")
        index.extraction(fresh, "Fresh.kt")

        assertThat(index.stats.trusted).isEqualTo(1)
        assertThat(index.stats.reusedByHash).isEqualTo(1)
        assertThat(index.stats.parsed).isEqualTo(1)
    }
}
