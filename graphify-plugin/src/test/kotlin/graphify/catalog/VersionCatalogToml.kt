package graphify.catalog

import contracts.catalog.VersionCatalogToml as CatalogVersionToml

/**
 * Deprecated facade — the implementation moved to the N0 shared contract
 * `contracts.catalog.VersionCatalogToml` (MEMPHIS `catalog-contracts`, S-331).
 *
 * Kept temporarily so the historical call sites (`GraphifyPluginPublicationTest`,
 * `PublicationHygieneSteps`) keep compiling while the deletion is confirmed
 * file by file (AGENT.adoc governance gate). Every call delegates to N0: there
 * is a single implementation, never two.
 */
@Deprecated(
    "Moved to the N0 contract contracts.catalog.VersionCatalogToml (MEMPHIS catalog-contracts).",
    ReplaceWith("contracts.catalog.VersionCatalogToml", "contracts.catalog.VersionCatalogToml"),
)
object VersionCatalogToml {
    fun versionOf(
        content: String,
        key: String,
    ): String? = CatalogVersionToml.versionOf(content, key)

    fun versionOf(
        content: String,
        keys: List<String>,
    ): String? = CatalogVersionToml.versionOf(content, keys)
}
