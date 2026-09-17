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
 * Functional (TestKit) coverage for EPIC GF-SCHEMA.
 *
 * Drives the real plugin end-to-end against a throw-away project and asserts on
 * the *wire contract* (`graph.json`), exactly what the N1/N2 consumers read:
 * canonical type literals only, and a top-level shape a strict mapper accepts.
 */
class SchemaFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private val mapper = ObjectMapper()

    private val canonicalNodeTypes =
        setOf("project", "directory", "file", "section")
    private val canonicalEdgeTypes =
        setOf("contains", "import", "reference", "agent_reference", "has_section", "subsection")

    @Test
    fun `collectFromWorkspace produces a canonical graph contract`() {
        writeProject()
        writeFile("my-app/.git/HEAD", "ref: refs/heads/main")
        writeFile("my-app/build.gradle.kts", "plugins { java }")
        writeFile(
            "my-app/src/main/kotlin/com/example/App.kt",
            "package com.example\nimport com.example.Util\nclass App"
        )
        writeFile("my-app/src/main/kotlin/com/example/Util.kt", "package com.example\nclass Util")
        writeFile("docs/book.adoc", "= Book\n== Chapter\nSee `docs/other.adoc`.")
        writeFile("docs/other.adoc", "= Other\n== Section")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("collectFromWorkspace", "--stacktrace")
            .withPluginClasspath()
            .build()

        assertThat(result.task(":collectFromWorkspace")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val graph = readGraph()
        val nodeTypes = graph.get("nodes").map { it.get("type").asText() }.toSet()
        val edgeTypes = graph.get("edges").map { it.get("type").asText() }.toSet()

        assertThat(nodeTypes).isSubsetOf(canonicalNodeTypes)
        assertThat(edgeTypes).isSubsetOf(canonicalEdgeTypes)
        assertThat(nodeTypes).contains("project", "file", "directory", "section")
        assertThat(edgeTypes).contains("contains", "import", "reference", "has_section", "subsection")
    }

    @Test
    fun `produced graph keeps the four-field top-level shape a strict consumer expects`() {
        writeProject()
        writeFile("my-app/.git/HEAD", "ref: refs/heads/main")
        writeFile("my-app/README.adoc", "= Readme\n== Intro")

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("collectFromWorkspace")
            .withPluginClasspath()
            .build()

        val graph = readGraph()
        assertThat(graph.fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("schemaVersion", "nodes", "edges", "communities")
        assertThat(graph.get("schemaVersion").asInt()).isEqualTo(1)
        assertThat(graph.get("nodes")).isNotEmpty
        assertThat(graph.get("communities")).isNotEmpty
    }

    @Test
    fun `produces byte-identical graph across two runs on an unchanged workspace`() {
        writeProject()
        writeFile("my-app/.git/HEAD", "ref: refs/heads/main")
        writeFile("my-app/build.gradle.kts", "plugins { java }")
        writeFile("my-app/src/main/kotlin/com/example/App.kt", "package com.example\nclass App")

        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("collectFromWorkspace")
            .withPluginClasspath()
            .build()
        val first = projectDir.resolve("graph.json").readText()

        projectDir.resolve("graph.json").delete()
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("collectFromWorkspace")
            .withPluginClasspath()
            .build()
        val second = projectDir.resolve("graph.json").readText()

        assertThat(second).isEqualTo(first)
    }

    private fun readGraph(): JsonNode {
        val graphFile = projectDir.resolve("graph.json")
        assertThat(graphFile).exists()
        return mapper.readTree(graphFile)
    }

    private fun writeProject() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"schema-ft\"\n")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("education.cccp.graphify")
            }

            graphify {
                rootDir.set(layout.projectDirectory.asFile)
                outputFile.set(layout.projectDirectory.file("graph.json").asFile)
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
