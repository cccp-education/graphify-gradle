package graphify.fingerprint

/**
 * Pure, content-only extraction of the facts a scan can cache per file.
 *
 * Every parser here mirrors the producer's historical regexes exactly; the
 * extraction is deliberately kept free of any sibling-file knowledge so its
 * result is a function of `(name, content)` alone and can be memoised by
 * content hash.
 */
object FileExtractor {

    private val kotlinImportRegex = Regex("""^import\s+([\w.]+)""", RegexOption.MULTILINE)

    private val adocLinkRegex = Regex("""(?:link|include|xref|image):([^\[\]\s]+)\[""")

    private val adocPathRegex = Regex("""`([\w./-]+\.(?:adoc|ad|kt|kts|yml|yaml|json|java|md))`""")

    private val tocCellRegex = Regex("""\|\s*([\w./-]+\.(?:adoc|ad))\s*(?:\||$)""", RegexOption.MULTILINE)

    private val agentReferenceRegex = Regex("""[\w.-]+/[\w.-]+(?:/[\w.-]+)*""")

    private val headingRegex = Regex("""^(=+)\s+(.+?)\s*$""")

    private val kotlinExtensions = setOf("kt", "kts")
    private val adocExtensions = setOf("adoc", "ad")

    fun extract(fileName: String, content: String): FileExtraction {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        val isKotlin = extension in kotlinExtensions
        val isAdoc = extension in adocExtensions

        return FileExtraction(
            imports = if (isKotlin) kotlinImports(content) else emptyList(),
            adocReferences = if (isAdoc) adocReferences(content) else emptyList(),
            tocReferences = if (isAdoc) tocReferences(content) else emptyList(),
            agentReferences = if (fileName == "INDEX.adoc") agentReferences(content) else emptyList(),
            sections = if (isAdoc) sections(content) else emptyList()
        )
    }

    fun kotlinImports(content: String): List<String> =
        kotlinImportRegex.findAll(content).map { it.groupValues[1] }.toList()

    fun adocReferences(content: String): List<String> {
        val refs = mutableListOf<String>()
        refs.addAll(adocLinkRegex.findAll(content).map { it.groupValues[1] }.toList())
        refs.addAll(adocPathRegex.findAll(content).map { it.groupValues[1] }.toList())
        return refs
    }

    fun tocReferences(content: String): List<String> =
        tocCellRegex.findAll(content).map { it.groupValues[1] }.toList()

    fun agentReferences(content: String): List<String> =
        agentReferenceRegex.findAll(content).map { it.value }.filter { it.contains("/") }.toList()

    fun sections(content: String): List<SectionExtraction> {
        val sections = mutableListOf<SectionExtraction>()
        var lineNumber = 0
        for (line in content.lines()) {
            lineNumber++
            val match = headingRegex.find(line) ?: continue
            sections.add(
                SectionExtraction(
                    title = match.groupValues[2],
                    level = match.groupValues[1].length,
                    line = lineNumber
                )
            )
        }
        return sections
    }
}
