package graphify.steps

import graphify.catalog.PublicationHygiene
import graphify.catalog.VersionCatalogToml
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

/**
 * BDD steps for the publication hygiene guard (`publication_hygiene.feature`).
 *
 * Steps use "local catalog" / "publication hygiene" vocabulary to stay
 * collision-free with other feature glue in the shared `graphify.steps` package
 * (pattern S-088).
 */
class PublicationHygieneSteps : En {

    private var localCatalog: String = ""
    private var publishedVersion: String? = null
    private var localVersion: String? = null
    private var verdict: PublicationHygiene.Verdict? = null

    init {

        Given("a local catalog declaring graphify-plugin {string}") { version: String ->
            localCatalog =
                """
                [versions]
                graphify-plugin = "$version"
                """.trimIndent()
            resetLookup()
        }

        Given("a local catalog with only a commented graphify-plugin {string}") { version: String ->
            localCatalog =
                """
                [versions]
                # graphify-plugin = "$version"
                """.trimIndent()
            resetLookup()
        }

        Given("a local catalog whose versions section is empty") {
            localCatalog =
                """
                [versions]
                bakery-plugin = "0.0.12"
                """.trimIndent()
            resetLookup()
        }

        Given("the alias graphify declares {string} outside the versions section") { version: String ->
            localCatalog +=
                """

                [plugins]
                graphify = "$version"
                """.trimIndent()
        }

        Given("a published workspace catalog version {string}") { version: String ->
            publishedVersion = version
        }

        Given("no published workspace catalog version") {
            publishedVersion = null
        }

        When("the publication hygiene is checked") {
            val resolvedLocal = VersionCatalogToml.versionOf(localCatalog, listOf("graphify-plugin", "graphify"))
            verdict = PublicationHygiene.check(resolvedLocal, publishedVersion)
        }

        When("the local version is looked up") {
            localVersion = VersionCatalogToml.versionOf(localCatalog, listOf("graphify-plugin", "graphify"))
        }

        Then("the hygiene verdict is consistent") {
            assertThat(verdict!!.consistent)
                .withFailMessage(verdict!!.message)
                .isTrue()
        }

        Then("the hygiene verdict is a violation") {
            assertThat(verdict!!.consistent).isFalse()
        }

        Then("the message names both the local and the published version") {
            assertThat(verdict!!.message).contains("0.0.6").contains("0.0.5")
        }

        Then("the message names the published version as missing") {
            assertThat(verdict!!.message).contains("published")
        }

        Then("the local version is {string}") { expected: String ->
            assertThat(localVersion).isEqualTo(expected)
        }

        Then("the local version is absent") {
            assertThat(localVersion).isNull()
        }
    }

    private fun resetLookup() {
        publishedVersion = null
        localVersion = null
        verdict = null
    }
}
