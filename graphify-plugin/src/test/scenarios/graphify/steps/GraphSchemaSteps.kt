package graphify.steps

import graphify.model.GraphCommunity
import graphify.model.GraphEdge
import graphify.model.GraphModel
import graphify.model.GraphNode
import graphify.schema.GraphSchema
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

class GraphSchemaSteps : En {

    private var nodeResult: String = ""
    private var edgeResult: String = ""
    private var legacyGraph: GraphModel? = null
    private var canonicalGraph: GraphModel? = null

    init {

        Given("the graph schema vocabulary") {
            nodeResult = ""
            edgeResult = ""
        }

        Given("a legacy graph with a {string} node and an {string} edge") { nodeType: String, edgeType: String ->
            legacyGraph = GraphModel(
                nodes = listOf(
                    GraphNode(
                        id = "n1",
                        label = "kept-label",
                        type = nodeType,
                        community = "kept-community",
                        metadata = mapOf("extension" to "kt")
                    )
                ),
                edges = listOf(
                    GraphEdge(source = "n1", target = "n2", type = edgeType, label = "kept-label-edge")
                ),
                communities = listOf(GraphCommunity(id = "kept-community", label = "kept-community", size = 1))
            )
        }

        When("the node type {string} is canonicalised") { raw: String ->
            nodeResult = GraphSchema.canonicalizeNodeType(raw)
        }

        When("the edge type {string} is canonicalised") { raw: String ->
            edgeResult = GraphSchema.canonicalizeEdgeType(raw)
        }

        When("the node type result is canonicalised again") {
            nodeResult = GraphSchema.canonicalizeNodeType(nodeResult)
        }

        When("the graph is canonicalised") {
            canonicalGraph = GraphSchema.canonicalize(legacyGraph!!)
        }

        Then("the canonical node type is {string}") { expected: String ->
            assertThat(nodeResult).isEqualTo(expected)
        }

        Then("the canonical edge type is {string}") { expected: String ->
            assertThat(edgeResult).isEqualTo(expected)
        }

        Then("the node type becomes {string}") { expected: String ->
            assertThat(canonicalGraph!!.nodes.first().type).isEqualTo(expected)
        }

        Then("the edge type becomes {string}") { expected: String ->
            assertThat(canonicalGraph!!.edges.first().type).isEqualTo(expected)
        }

        Then("every non-type field is preserved") {
            val node = canonicalGraph!!.nodes.first()
            val edge = canonicalGraph!!.edges.first()
            assertThat(node.id).isEqualTo("n1")
            assertThat(node.label).isEqualTo("kept-label")
            assertThat(node.community).isEqualTo("kept-community")
            assertThat(node.metadata["extension"]).isEqualTo("kt")
            assertThat(edge.source).isEqualTo("n1")
            assertThat(edge.target).isEqualTo("n2")
            assertThat(edge.label).isEqualTo("kept-label-edge")
            assertThat(canonicalGraph!!.communities).hasSize(1)
        }
    }
}
