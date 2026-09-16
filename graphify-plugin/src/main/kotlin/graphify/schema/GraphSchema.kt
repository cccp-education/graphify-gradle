package graphify.schema

import graphify.model.GraphModel

/**
 * Single source of truth for the `graph.json` node and edge vocabulary.
 *
 * Canonicalisation is *non destructive*: an unknown type is always preserved
 * as-is so the schema tolerates future producers without ever dropping data.
 * Recognised aliases (plural, dash spelling, legacy phantom types) collapse
 * onto the canonical value; canonical values are idempotent.
 *
 * The canonical direction is the singular spelling (`import` / `reference` /
 * `agent_reference`) because the N1/N2 consumers (bakery, codebase) hard-code
 * those literals. Switching to the Understand-Anything plural spelling later
 * is a one-table change here, with no impact on the producers.
 */
object GraphSchema {

    private val nodeAliases: Map<String, String> = mapOf(
        "dir" to NodeType.DIRECTORY.wire,
        "directories" to NodeType.DIRECTORY.wire,
        "folder" to NodeType.DIRECTORY.wire,
        "folders" to NodeType.DIRECTORY.wire,
        "modules" to NodeType.PROJECT.wire,
        "module" to NodeType.PROJECT.wire,
        "projects" to NodeType.PROJECT.wire,
        "files" to NodeType.FILE.wire,
        "sections" to NodeType.SECTION.wire
    )

    private val edgeAliases: Map<String, String> = mapOf(
        "imports" to EdgeType.IMPORT.wire,
        "references" to EdgeType.REFERENCE.wire,
        "agent_references" to EdgeType.AGENT_REFERENCE.wire,
        "has_sections" to EdgeType.HAS_SECTION.wire,
        "subsections" to EdgeType.SUBSECTION.wire,
        "contains_file" to EdgeType.CONTAINS.wire
    )

    val reservedNodeTypes: Set<String> = setOf(
        "domain", "flow", "step", "service", "endpoint", "config", "concept"
    )

    val reservedEdgeTypes: Set<String> = setOf(
        "depends_on", "deploys", "documents", "similar_to"
    )

    private val canonicalNodeWires: Set<String> = NodeType.entries.map { it.wire }.toSet()
    private val canonicalEdgeWires: Set<String> = EdgeType.entries.map { it.wire }.toSet()

    fun canonicalizeNodeType(raw: String): String {
        if (raw in canonicalNodeWires) return raw
        val normalized = normalize(raw)
        if (normalized in canonicalNodeWires) return normalized
        return nodeAliases[normalized] ?: raw
    }

    fun canonicalizeEdgeType(raw: String): String {
        if (raw in canonicalEdgeWires) return raw
        val normalized = normalize(raw)
        if (normalized in canonicalEdgeWires) return normalized
        return edgeAliases[normalized] ?: raw
    }

    /**
     * Canonicalises a whole model — used by consumers reading a *foreign*
     * `graph.json` (legacy spelling) so every type is normalised in one pass.
     * Unknown types and every non-type field are preserved untouched.
     */
    fun canonicalize(model: GraphModel): GraphModel = model.copy(
        nodes = model.nodes.map { node ->
            node.copy(type = canonicalizeNodeType(node.type))
        },
        edges = model.edges.map { edge ->
            edge.copy(type = canonicalizeEdgeType(edge.type))
        }
    )

    private fun normalize(raw: String): String = raw.replace('-', '_')
}
