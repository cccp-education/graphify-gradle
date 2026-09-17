package graphify

import graphify.catalog.PublicationHygiene
import graphify.catalog.VersionCatalogToml
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * MEM-CAT-ROLLOUT-6 (S-214, cross-borough MEMPHIS) — publication hygiene guard.
 *
 * D3: the plugin self version is derived from the published workspace catalog
 * (`ws.versions.graphify.plugin.get()`) — the ws catalog is the cross-borough
 * source of truth.
 * D4: the borough pins the catalog once in settings.gradle.kts.
 * D5 hygiene: the local toml self version and the ws catalog version must agree.
 *
 * D5R-2 (S-029): the *published* ws catalog version is injected by Gradle as the
 * `graphify.publishedCatalog.graphifyVersion` system property. The guard never
 * reads a neighbour repository's working tree — that was racy between sessions
 * (S-028 collision).
 */
class GraphifyPluginPublicationTest {
    private val pluginDir = File(System.getProperty("user.dir")).absoluteFile

    private val publishedVersion: String
        get() =
            System.getProperty("graphify.publishedCatalog.graphifyVersion")
                ?: error(
                    "graphify.publishedCatalog.graphifyVersion is not set — run through Gradle " +
                        "(build.gradle.kts injects the published ws catalog version)"
                )

    private val selfVersionKeys = listOf("graphify-plugin", "graphify")

    @Test
    fun `plugin version matches ws catalog version`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)
        val versionLine =
            buildScript
                .lineSequence()
                .first { it.trimStart().startsWith("version =") }

        // MEM-CAT-ROLLOUT-6 (D3) — self version derived from the published workspace catalog.
        assertThat(versionLine)
            .withFailMessage("build.gradle.kts version must derive from the published workspace catalog (ws.versions.graphify.plugin)")
            .contains("ws.versions.graphify.plugin.get()")

        // Hygiene (D5): local toml self version must match the published ws catalog version.
        val localVersion =
            VersionCatalogToml.versionOf(
                pluginDir.resolve("gradle/libs.versions.toml").readText(UTF_8),
                selfVersionKeys
            )

        val verdict = PublicationHygiene.check(localVersion, publishedVersion)

        assertThat(verdict.consistent)
            .withFailMessage(verdict.message)
            .isTrue()
    }

    @Test
    fun `settings pins the workspace catalog`() {
        val settings = pluginDir.resolve("settings.gradle.kts").readText(UTF_8)

        // MEM-CAT-ROLLOUT-6 (D4) — single pin per borough, published workspace catalog.
        assertThat(settings)
            .withFailMessage("settings.gradle.kts must pin the published workspace catalog (education.cccp:workspace-catalog:0.0.46)")
            .contains("""from("education.cccp:workspace-catalog:0.0.46")""")
    }

    @Test
    fun `plugin group and id are stable for publication`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)

        // The plugin id is declared inline in the gradlePlugin block (gradlePlugin.plugins graphify).
        val idLine =
            buildScript
                .lineSequence()
                .first { it.trimStart().startsWith("id = ") && it.contains("education.cccp.graphify") }

        assertThat(buildScript).contains("group = \"education.cccp\"")
        assertThat(idLine.substringAfter("\"").substringBefore("\"")).isEqualTo("education.cccp.graphify")
    }
}
