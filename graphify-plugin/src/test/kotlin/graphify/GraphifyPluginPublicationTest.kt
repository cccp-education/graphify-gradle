package graphify

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
 */
class GraphifyPluginPublicationTest {
    private val pluginDir = File(System.getProperty("user.dir")).absoluteFile

    private val rootDir =
        pluginDir.parentFile
            ?: throw IllegalStateException("Cannot resolve repo root from plugin dir")

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

        // Hygiene (D5): local toml self version must match the ws catalog version —
        // the ws catalog (workspace-bom repo) is the cross-borough source of truth.
        val pluginCatalogVersion = graphifyVersionFrom(pluginDir.resolve("gradle/libs.versions.toml").readText(UTF_8))
        val wsCatalogVersion = graphifyVersionFrom(wsCatalogToml())

        assertThat(pluginCatalogVersion)
            .withFailMessage("plugin catalog graphify version ($pluginCatalogVersion) must match ws catalog graphify-plugin version ($wsCatalogVersion)")
            .isEqualTo(wsCatalogVersion)
    }

    @Test
    fun `settings pins the workspace catalog`() {
        val settings = pluginDir.resolve("settings.gradle.kts").readText(UTF_8)

        // MEM-CAT-ROLLOUT-6 (D4) — single pin per borough, published workspace catalog.
        assertThat(settings)
            .withFailMessage("settings.gradle.kts must pin the published workspace catalog (education.cccp:workspace-catalog:0.0.32)")
            .contains("""from("education.cccp:workspace-catalog:0.0.33")""")
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

    /**
     * Reads the `ws` catalog toml and extracts the `graphify-plugin` version.
     * Fallback: parse the local MEMPHIS repo toml (same source file as the
     * published catalog).
     */
    private fun wsCatalogToml(): String {
        val wsRepoToml = rootDir.parentFile
            ?.resolve("workspace-bom/gradle/libs.versions.toml")
        if (wsRepoToml != null && wsRepoToml.exists()) return wsRepoToml.readText(UTF_8)
        error("ws catalog toml introuvable — résolution ws impossible pour l'hygiène")
    }

    private fun graphifyVersionFrom(content: String): String =
        content
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .first { it.startsWith("graphify-plugin =") || it.startsWith("graphify =") }
            .substringAfter("\"")
            .substringBefore("\"")
}