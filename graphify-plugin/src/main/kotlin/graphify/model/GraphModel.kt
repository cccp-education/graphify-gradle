package graphify.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * The `graph.json` contract.
 *
 * [schemaVersion] is the version of the *schema*, independent of the plugin
 * artefact version: it only ever changes when the wire shape changes in a way
 * consumers must be aware of.
 *
 * [JsonIgnoreProperties] is the crucial part: it makes every consumer tolerant
 * of unknown fields *from the shared class*, so a new field added here can
 * never break a strict mapper (bakery, codebase, …) that reads this model.
 * Tolerance is delivered by bumping the dependency, not by editing consumers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphModel(
    val schemaVersion: Int = SCHEMA_VERSION,
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val communities: List<GraphCommunity> = emptyList()
) {
    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphNode(
    val id: String,
    val label: String,
    val type: String,
    val community: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphEdge(
    val source: String,
    val target: String,
    val type: String,
    val label: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphCommunity(
    val id: String,
    val label: String,
    val size: Int
)
