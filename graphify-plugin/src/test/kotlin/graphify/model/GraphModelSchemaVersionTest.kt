package graphify.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * GF-SCHEMA-5 — Schema versioning + consumer tolerance from the shared model.
 *
 * The tolerance lives on [GraphModel] itself so every consumer (bakery,
 * codebase, future) becomes tolerant by bumping its dependency, without
 * touching a line of its own code.
 */
class GraphModelSchemaVersionTest {

    private val strictMapper: ObjectMapper = ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerKotlinModule()

    @Nested
    inner class SchemaVersion {

        @Test
        fun `should expose the current schema version`() {
            assertThat(GraphModel.SCHEMA_VERSION).isEqualTo(1)
        }

        @Test
        fun `should default the schema version to the current one`() {
            assertThat(GraphModel().schemaVersion).isEqualTo(GraphModel.SCHEMA_VERSION)
        }

        @Test
        fun `should serialize the schema version as a top-level field`() {
            val json = strictMapper.writeValueAsString(GraphModel())
            assertThat(json).contains("\"schemaVersion\":1")
        }

        @Test
        fun `should round-trip the schema version`() {
            val restored = strictMapper.readValue<GraphModel>(
                """{"schemaVersion":1,"nodes":[],"edges":[],"communities":[]}"""
            )
            assertThat(restored.schemaVersion).isEqualTo(1)
        }

        @Test
        fun `should read a legacy graph without a schema version using the default`() {
            val restored = strictMapper.readValue<GraphModel>(
                """{"nodes":[],"edges":[],"communities":[]}"""
            )
            assertThat(restored.schemaVersion).isEqualTo(GraphModel.SCHEMA_VERSION)
        }
    }

    @Nested
    inner class StrictConsumerTolerance {

        @Test
        fun `should ignore an unknown top-level field on a strict mapper`() {
            val restored = strictMapper.readValue<GraphModel>(
                """{"nodes":[],"edges":[],"communities":[],"futureField":"ignored"}"""
            )
            assertThat(restored.nodes).isEmpty()
        }

        @Test
        fun `should ignore an unknown node field on a strict mapper`() {
            val restored = strictMapper.readValue<GraphModel>(
                """
                {
                  "schemaVersion": 1,
                  "nodes": [{"id":"a.kt","label":"a.kt","type":"file","futureField":42}],
                  "edges": [],
                  "communities": []
                }
                """.trimIndent()
            )
            assertThat(restored.nodes).hasSize(1)
            assertThat(restored.nodes.first().id).isEqualTo("a.kt")
        }
    }
}
