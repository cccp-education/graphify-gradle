package graphify.steps

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import graphify.model.GraphModel
import io.cucumber.java8.En
import org.assertj.core.api.Assertions.assertThat

class SchemaVersionSteps : En {

    private val strictMapper: ObjectMapper = ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerKotlinModule()

    private var model: GraphModel? = null
    private var json: String = ""
    private var readBack: GraphModel? = null
    private var readError: Exception? = null

    init {

        Given("a graph model built in memory") {
            model = GraphModel()
            json = ""
            readBack = null
            readError = null
        }

        Given("a legacy graph JSON without a {string} field") { _: String ->
            json = """
                {
                  "nodes": [{"id":"a.kt","label":"a.kt","type":"file"}],
                  "edges": [],
                  "communities": []
                }
            """.trimIndent()
        }

        Given("a graph JSON carrying an unknown top-level field") {
            json = """
                {
                  "nodes": [],
                  "edges": [],
                  "communities": [],
                  "futureField": "ignored"
                }
            """.trimIndent()
        }

        Given("a graph JSON carrying an unknown node field") {
            json = """
                {
                  "schemaVersion": 1,
                  "nodes": [{"id":"a.kt","label":"a.kt","type":"file","futureField":42}],
                  "edges": [],
                  "communities": []
                }
            """.trimIndent()
        }

        When("the model is serialized") {
            json = strictMapper.writeValueAsString(model!!)
        }

        When("the model is serialized and read back") {
            json = strictMapper.writeValueAsString(model!!)
            readBack = strictMapper.readValue<GraphModel>(json)
        }

        When("the legacy graph is read by a strict reader") {
            readBack = strictMapper.readValue<GraphModel>(json)
        }

        When("the graph is read by a strict reader") {
            readError = null
            readBack = try {
                strictMapper.readValue<GraphModel>(json)
            } catch (e: Exception) {
                readError = e
                null
            }
        }

        Then("the schema version is the current one") {
            assertThat(model!!.schemaVersion).isEqualTo(GraphModel.SCHEMA_VERSION)
        }

        Then("the JSON exposes a top-level {string} field equal to {int}") { field: String, expected: Int ->
            val tree = strictMapper.readTree(json)
            assertThat(tree.has(field)).isTrue()
            assertThat(tree.get(field).asInt()).isEqualTo(expected)
        }

        Then("the read-back schema version is the current one") {
            assertThat(readBack!!.schemaVersion).isEqualTo(GraphModel.SCHEMA_VERSION)
        }

        Then("the graph is read without error") {
            assertThat(readError).isNull()
            assertThat(readBack).isNotNull()
        }

        Then("the node keeps its known fields") {
            val node = readBack!!.nodes.first()
            assertThat(node.id).isEqualTo("a.kt")
            assertThat(node.label).isEqualTo("a.kt")
            assertThat(node.type).isEqualTo("file")
        }
    }
}
