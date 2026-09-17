package graphify

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Functional (TestKit) coverage for EPIC GF-FINGERPRINT.
 *
 * Drives the real plugin end-to-end and proves the incremental scan is a pure
 * optimisation: the second run must reproduce the first graph byte for byte,
 * while genuine workspace changes must still be re-analysed.
 */
class IncrementalFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private val mapper = ObjectMapper()

    @Test
    fun `incremental second run is byte-identical to the first and writes a cache`() {
        writeProject(incremental = true)
        writeFile("my-app/.git/HEAD", "ref: refs/heads/main")
        writeFile("my-app/build.gradle.kts", "plugins { java }")
        writeFile(
            "my-app/src/main/kotlin/com/example/App.kt",
            "package com.example\nimport com.example.Util\nclass App"
        )
        writeFile("my-app/src/main/kotlin/com/example/Util.kt", "package com.example\nclass Util")
        writeFile("docs/book.adoc", "= Book\n== Chapter\nSee `docs/other.adoc`.")
        writeFile("docs/other.adoc", "= Other\n== Section")

        run()
        val first = readGraphText()

        projectDir.resolve("graph.json").delete()
        run()
        val second = readGraphText()

        assertThat(second).isEqualTo(first)
        assertThat(cacheFile()).exists()
    }

    @Test
    fun `cache hit keeps the canonical contract across runs`() {
        writeProject(incremental = true)
        writeFile("my-app/.git/HEAD", "ref: refs/heads/main")
        writeFile("my-app/build.gradle.kts", "plugins { java }")
        writeFile("docs/book.adoc", "= Book\n== Chapter")

        run()
        run()

        val graph = readGraph()
        val nodeTypes = graph.get("nodes").map { it.get("type").asText() }.toSet()
        assertThat(nodeTypes).isSubsetOf(setOf("project", "directory", "file", "section"))
    }

    @Test
    fun `a modified file is re-analysed and its new section appears`() {
        writeProject(incremental = true)
        writeFile("m/.git/HEAD", "ref: refs/heads/main")
        writeFile("docs/book.adoc", "= Book\n== First")

        run()
        assertThat(sectionLabels()).contains("First").doesNotContain("Second")

        writeFile("docs/book.adoc", "= Book\n== First\n== Second")
        run()

        assertThat(sectionLabels()).contains("First", "Second")
    }

    @Test
    fun `a deleted file leaves no node behind`() {
        writeProject(incremental = true)
        writeFile("m/.git/HEAD", "ref: refs/heads/main")
        writeFile("docs/keep.adoc", "= Keep")
        writeFile("docs/gone.adoc", "= Gone")

        run()
        assertThat(nodeIds()).contains("docs/gone.adoc")

        projectDir.resolve("docs/gone.adoc").delete()
        run()

        assertThat(nodeIds()).doesNotContain("docs/gone.adoc")
        assertThat(nodeIds()).contains("docs/keep.adoc")
    }

    @Test
    fun `a corrupted cache degrades to a full scan without failing`() {
        writeProject(incremental = true)
        writeFile("m/.git/HEAD", "ref: refs/heads/main")
        writeFile("docs/book.adoc", "= Book\n== Chapter")

        run()
        cacheFile().writeText("{ corrupted json")

        val result = run()

        assertThat(result.task(":collectFromWorkspace")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(sectionLabels()).contains("Chapter")
    }

    @Test
    fun `incremental scan is opt-in and stays off by default`() {
        writeProject(incremental = false)
        writeFile("m/.git/HEAD", "ref: refs/heads/main")
        writeFile("docs/book.adoc", "= Book\n== Chapter")

        run()

        assertThat(cacheFile()).doesNotExist()
    }

    private fun sectionLabels(): List<String> =
        readGraph().get("nodes")
            .filter { it.get("type").asText() == "section" }
            .map { it.get("label").asText() }

    private fun nodeIds(): List<String> =
        readGraph().get("nodes").map { it.get("id").asText() }

    private fun run() = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments("collectFromWorkspace", "--stacktrace")
        .withPluginClasspath()
        .build()

    private fun cacheFile(): File = projectDir.resolve("build/graphify/fingerprints.json")

    private fun readGraph(): JsonNode {
        val graphFile = projectDir.resolve("graph.json")
        assertThat(graphFile).exists()
        return mapper.readTree(graphFile)
    }

    private fun readGraphText(): String = projectDir.resolve("graph.json").readText()

    private fun writeProject(incremental: Boolean) {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"incremental-ft\"\n")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("education.cccp.graphify")
            }

            graphify {
                rootDir.set(layout.projectDirectory.asFile)
                outputFile.set(layout.projectDirectory.file("graph.json").asFile)
                incremental.set($incremental)
            }
            """.trimIndent()
        )
    }

    private fun writeFile(relativePath: String, content: String) {
        val file = projectDir.resolve(relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }
}
