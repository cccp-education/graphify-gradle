package graphify.schema

import graphify.model.GraphCommunity
import graphify.model.GraphEdge
import graphify.model.GraphModel
import graphify.model.GraphNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GraphSchemaModelTest {

    @Nested
    inner class CanonicalizeWholeModel {

        @Test
        fun `should canonicalize aliased node and edge types`() {
            val legacy = GraphModel(
                nodes = listOf(
                    GraphNode(id = "a", label = "a", type = "dir"),
                    GraphNode(id = "b", label = "b", type = "module")
                ),
                edges = listOf(
                    GraphEdge(source = "a", target = "b", type = "imports"),
                    GraphEdge(source = "b", target = "a", type = "references")
                )
            )

            val result = GraphSchema.canonicalize(legacy)

            assertThat(result.nodes.map { it.type }).containsExactly("directory", "project")
            assertThat(result.edges.map { it.type }).containsExactly("import", "reference")
        }

        @Test
        fun `should preserve unknown types and every other field`() {
            val legacy = GraphModel(
                nodes = listOf(
                    GraphNode(
                        id = "n1",
                        label = "label-1",
                        type = "domain",
                        community = "comm",
                        metadata = mapOf("extension" to "kt")
                    )
                ),
                edges = listOf(
                    GraphEdge(source = "n1", target = "n2", type = "depends_on", label = "keep")
                ),
                communities = listOf(GraphCommunity(id = "comm", label = "comm", size = 1))
            )

            val result = GraphSchema.canonicalize(legacy)

            assertThat(result.nodes.first().type).isEqualTo("domain")
            assertThat(result.nodes.first().label).isEqualTo("label-1")
            assertThat(result.nodes.first().community).isEqualTo("comm")
            assertThat(result.nodes.first().metadata["extension"]).isEqualTo("kt")
            assertThat(result.edges.first().type).isEqualTo("depends_on")
            assertThat(result.edges.first().label).isEqualTo("keep")
            assertThat(result.communities).hasSize(1)
        }

        @Test
        fun `should be idempotent on an already canonical model`() {
            val canonical = GraphModel(
                nodes = listOf(GraphNode(id = "a", label = "a", type = "file")),
                edges = listOf(GraphEdge(source = "a", target = "a", type = "contains"))
            )

            assertThat(GraphSchema.canonicalize(GraphSchema.canonicalize(canonical)))
                .isEqualTo(GraphSchema.canonicalize(canonical))
        }

        @Test
        fun `should return an equal model when everything is already canonical`() {
            val canonical = GraphModel(
                nodes = listOf(
                    GraphNode(id = "a", label = "a", type = "file"),
                    GraphNode(id = "b", label = "b", type = "section")
                ),
                edges = listOf(
                    GraphEdge(source = "a", target = "b", type = "has_section")
                ),
                communities = listOf(GraphCommunity(id = "c", label = "c", size = 2))
            )

            assertThat(GraphSchema.canonicalize(canonical)).isEqualTo(canonical)
        }
    }

    @Nested
    inner class ProducerConformance {

        @Test
        fun `every produced node type literal is canonical`() {
            val canonical = NodeType.entries.map { it.wire }.toSet()
            assertThat(canonical).containsExactlyInAnyOrder("project", "directory", "file", "section")
        }

        @Test
        fun `every produced edge type literal is canonical`() {
            val canonical = EdgeType.entries.map { it.wire }.toSet()
            assertThat(canonical).containsExactlyInAnyOrder(
                "contains", "import", "reference", "agent_reference", "has_section", "subsection"
            )
        }
    }
}
