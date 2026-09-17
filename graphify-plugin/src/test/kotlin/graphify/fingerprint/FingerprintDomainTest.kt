package graphify.fingerprint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class FileFingerprintTest {

    @Nested
    inner class FastComparison {

        @Test
        fun `should match when size and mtime are unchanged`() {
            val fingerprint = FileFingerprint(size = 120, mtime = 1_700_000_000_000L, sha256 = "abc")
            assertThat(fingerprint.matches(size = 120, mtime = 1_700_000_000_000L)).isTrue()
        }

        @Test
        fun `should not match when size differs`() {
            val fingerprint = FileFingerprint(size = 120, mtime = 1_700_000_000_000L, sha256 = "abc")
            assertThat(fingerprint.matches(size = 121, mtime = 1_700_000_000_000L)).isFalse()
        }

        @Test
        fun `should not match when mtime differs`() {
            val fingerprint = FileFingerprint(size = 120, mtime = 1_700_000_000_000L, sha256 = "abc")
            assertThat(fingerprint.matches(size = 120, mtime = 1_700_000_001_000L)).isFalse()
        }
    }
}

class FingerprinterTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should produce identical sha256 for identical content`() {
        val a = write("a.txt", "same content")
        val b = write("b.txt", "same content")

        assertThat(Fingerprinter.sha256(a)).isEqualTo(Fingerprinter.sha256(b))
    }

    @Test
    fun `should produce distinct sha256 for distinct content`() {
        val a = write("a.txt", "one")
        val b = write("b.txt", "two")

        assertThat(Fingerprinter.sha256(a)).isNotEqualTo(Fingerprinter.sha256(b))
    }

    @Test
    fun `should capture size and mtime in the fingerprint`() {
        val file = write("a.txt", "hello")

        val fingerprint = Fingerprinter.fingerprint(file)

        assertThat(fingerprint.size).isEqualTo(5L)
        assertThat(fingerprint.mtime).isPositive()
        assertThat(fingerprint.sha256).hasSize(64)
    }

    @Test
    fun `should be stable across two fingerprints of an untouched file`() {
        val file = write("a.txt", "stable")

        assertThat(Fingerprinter.fingerprint(file)).isEqualTo(Fingerprinter.fingerprint(file))
    }

    private fun write(name: String, content: String): Path {
        val file = tempDir.resolve(name)
        file.parent?.createDirectories()
        file.writeText(content)
        return file
    }
}

class ScanCacheTest {

    private val extraction = FileExtraction(
        imports = listOf("com.example.Util"),
        sections = listOf(SectionExtraction(title = "Chapter", level = 2, line = 3))
    )

    private fun cacheWith(path: String = "src/App.kt"): ScanCache =
        ScanCache().record(
            path = path,
            fingerprint = FileFingerprint(size = 10, mtime = 1_000L, sha256 = "hash-1"),
            extraction = extraction
        )

    @Nested
    inner class Recording {

        @Test
        fun `should start empty`() {
            val cache = ScanCache()
            assertThat(cache.fingerprints).isEmpty()
            assertThat(cache.extractions).isEmpty()
        }

        @Test
        fun `should record a fingerprint keyed by path and an extraction keyed by sha256`() {
            val cache = cacheWith()
            assertThat(cache.fingerprints).containsOnlyKeys("src/App.kt")
            assertThat(cache.extractions).containsOnlyKeys("hash-1")
        }

        @Test
        fun `should replace the fingerprint of a re-recorded path`() {
            val updated = cacheWith().record(
                path = "src/App.kt",
                fingerprint = FileFingerprint(size = 20, mtime = 2_000L, sha256 = "hash-2"),
                extraction = FileExtraction(imports = listOf("com.example.Other"))
            )

            assertThat(updated.fingerprints.getValue("src/App.kt").sha256).isEqualTo("hash-2")
            assertThat(updated.extractions).containsKeys("hash-1", "hash-2")
        }
    }

    @Nested
    inner class FastPath {

        @Test
        fun `should report fast-unchanged when size and mtime match`() {
            assertThat(cacheWith().isFastUnchanged("src/App.kt", size = 10, mtime = 1_000L)).isTrue()
        }

        @Test
        fun `should not report fast-unchanged for an unknown path`() {
            assertThat(cacheWith().isFastUnchanged("src/Gone.kt", size = 10, mtime = 1_000L)).isFalse()
        }

        @Test
        fun `should not report fast-unchanged when size or mtime drift`() {
            assertThat(cacheWith().isFastUnchanged("src/App.kt", size = 11, mtime = 1_000L)).isFalse()
            assertThat(cacheWith().isFastUnchanged("src/App.kt", size = 10, mtime = 2_000L)).isFalse()
        }

        @Test
        fun `should reuse the extraction through the fast path`() {
            assertThat(cacheWith().extractionFor("src/App.kt", size = 10, mtime = 1_000L)).isEqualTo(extraction)
        }

        @Test
        fun `should return no extraction through the fast path when the fingerprint drifted`() {
            assertThat(cacheWith().extractionFor("src/App.kt", size = 99, mtime = 1_000L)).isNull()
        }
    }

    @Nested
    inner class ContentHashPath {

        @Test
        fun `should reuse the extraction by sha256 when only the mtime changed`() {
            assertThat(cacheWith().extraction("hash-1")).isEqualTo(extraction)
        }

        @Test
        fun `should return no extraction for an unknown sha256`() {
            assertThat(cacheWith().extraction("unknown")).isNull()
        }

        @Test
        fun `should share one extraction between two paths holding identical content`() {
            val shared = cacheWith(path = "a.txt").record(
                path = "b.txt",
                fingerprint = FileFingerprint(size = 10, mtime = 3_000L, sha256 = "hash-1"),
                extraction = extraction
            )

            assertThat(shared.fingerprints).containsOnlyKeys("a.txt", "b.txt")
            assertThat(shared.extractions).containsOnlyKeys("hash-1")
        }
    }

    @Nested
    inner class Pruning {

        @Test
        fun `should keep only the fingerprints of live paths`() {
            val pruned = cacheWith(path = "a.txt")
                .record(
                    path = "b.txt",
                    fingerprint = FileFingerprint(size = 5, mtime = 2_000L, sha256 = "hash-2"),
                    extraction = FileExtraction()
                )
                .prunedTo(setOf("a.txt"))

            assertThat(pruned.fingerprints).containsOnlyKeys("a.txt")
        }

        @Test
        fun `should drop extractions no surviving fingerprint references`() {
            val pruned = cacheWith(path = "a.txt")
                .record(
                    path = "b.txt",
                    fingerprint = FileFingerprint(size = 5, mtime = 2_000L, sha256 = "hash-2"),
                    extraction = FileExtraction()
                )
                .prunedTo(setOf("a.txt"))

            assertThat(pruned.extractions).containsOnlyKeys("hash-1")
        }

        @Test
        fun `should keep a shared extraction while one live path references it`() {
            val pruned = cacheWith(path = "a.txt")
                .record(
                    path = "b.txt",
                    fingerprint = FileFingerprint(size = 10, mtime = 4_000L, sha256 = "hash-1"),
                    extraction = extraction
                )
                .prunedTo(setOf("b.txt"))

            assertThat(pruned.extractions).containsOnlyKeys("hash-1")
            assertThat(pruned.fingerprints).containsOnlyKeys("b.txt")
        }

        @Test
        fun `should be a no-op when every path is live`() {
            val cache = cacheWith()
            assertThat(cache.prunedTo(setOf("src/App.kt"))).isEqualTo(cache)
        }

        @Test
        fun `should empty the cache when no path is live`() {
            val pruned = cacheWith().prunedTo(emptySet())
            assertThat(pruned.fingerprints).isEmpty()
            assertThat(pruned.extractions).isEmpty()
        }
    }
}

class ScanCacheStoreTest {

    @TempDir
    lateinit var tempDir: Path

    private val cache = ScanCache()
        .record(
            path = "src/App.kt",
            fingerprint = FileFingerprint(size = 42, mtime = 1_000L, sha256 = "deadbeef"),
            extraction = FileExtraction(
                imports = listOf("com.example.Util"),
                adocReferences = listOf("docs/other.adoc"),
                tocReferences = listOf("docs/toc.adoc"),
                agentReferences = listOf(".agents/INDEX.adoc"),
                sections = listOf(SectionExtraction(title = "Chapter", level = 2, line = 7))
            )
        )

    @Test
    fun `should round-trip a cache through disk`() {
        val file = tempDir.resolve("cache/fingerprints.json").toFile()

        ScanCacheStore.save(file, cache)

        assertThat(ScanCacheStore.load(file)).isEqualTo(cache)
    }

    @Test
    fun `should create the parent directory when saving`() {
        val file = tempDir.resolve("nested/dir/fingerprints.json").toFile()

        ScanCacheStore.save(file, cache)

        assertThat(file).exists()
    }

    @Test
    fun `should return an empty cache when the file is missing`() {
        assertThat(ScanCacheStore.load(tempDir.resolve("absent.json").toFile())).isEqualTo(ScanCache())
    }

    @Test
    fun `should return an empty cache when the file is corrupted`() {
        val file = tempDir.resolve("corrupted.json")
        file.writeText("{ not json")

        assertThat(ScanCacheStore.load(file.toFile())).isEqualTo(ScanCache())
    }

    @Test
    fun `should return an empty cache when the version is unknown`() {
        val file = tempDir.resolve("future.json")
        file.writeText("""{"version":99,"fingerprints":{},"extractions":{}}""")

        assertThat(ScanCacheStore.load(file.toFile())).isEqualTo(ScanCache())
    }

    @Test
    fun `should preserve a version marker in the written document`() {
        val file = tempDir.resolve("fingerprints.json").toFile()

        ScanCacheStore.save(file, cache)

        assertThat(file.readText()).contains("\"version\"")
    }
}
