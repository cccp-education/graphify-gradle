package graphify.schema

enum class EdgeType(val wire: String) {
    CONTAINS("contains"),
    IMPORT("import"),
    REFERENCE("reference"),
    AGENT_REFERENCE("agent_reference"),
    HAS_SECTION("has_section"),
    SUBSECTION("subsection")
}
