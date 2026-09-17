package graphify.catalog

/**
 * Minimal, section-scoped reader for a Gradle version catalog `[versions]` block.
 *
 * The publication hygiene guard needs one thing from the published workspace
 * catalog — the self version — and nothing else. Rather than pull a TOML parser
 * or read a neighbour repository's working tree (racy between sessions), this
 * reads the version *from the published catalog content* with one rule: a key
 * only resolves inside its own section.
 *
 * Scoping is the point: in a catalog the alias `graphify-plugin` also appears
 * in `[libraries]` and `[plugins]` (as `version.ref`), and `graphify` appears
 * as a plugin alias. Only `[versions]` defines the version, so only `[versions]`
 * is consulted.
 */
object VersionCatalogToml {

    private val sectionPattern = Regex("""^\[([^\]]+)]""")
    private val assignmentPattern = Regex("""^\s*([A-Za-z0-9._-]+)\s*=\s*"([^"]*)"""")

    fun versionOf(content: String, key: String): String? = versionOf(content, listOf(key))

    /**
     * Returns the value of the first candidate key found in the `[versions]`
     * section, or `null` when none is declared.
     */
    fun versionOf(content: String, keys: List<String>): String? {
        val versions = versionsSection(content)
        return keys.firstNotNullOfOrNull { versions[it] }
    }

    private fun versionsSection(content: String): Map<String, String> {
        val versions = LinkedHashMap<String, String>()
        var currentSection: String? = null

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            sectionPattern.find(line)?.let { match ->
                currentSection = match.groupValues[1].trim()
                return@forEach
            }

            if (currentSection != "versions") return@forEach

            assignmentPattern.find(rawLine)?.let { match ->
                versions[match.groupValues[1]] = match.groupValues[2]
            }
        }

        return versions
    }
}
