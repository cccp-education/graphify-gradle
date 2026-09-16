package graphify.schema

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GraphSchemaTest {

    @Nested
    inner class CanonicalNodeTypes {

        @Test
        fun `should expose the four produced node types as canonical values`() {
            assertThat(NodeType.entries.map { it.wire })
                .containsExactlyInAnyOrder("project", "directory", "file", "section")
        }

        @Test
        fun `should return canonical node type unchanged`() {
            for (type in NodeType.entries) {
                assertThat(GraphSchema.canonicalizeNodeType(type.wire)).isEqualTo(type.wire)
            }
        }

        @Test
        fun `should resolve plural alias to canonical singular node type`() {
            assertThat(GraphSchema.canonicalizeNodeType("dir")).isEqualTo("directory")
            assertThat(GraphSchema.canonicalizeNodeType("directories")).isEqualTo("directory")
            assertThat(GraphSchema.canonicalizeNodeType("file")).isEqualTo("file")
            assertThat(GraphSchema.canonicalizeNodeType("project")).isEqualTo("project")
        }

        @Test
        fun `should resolve the phantom module type to project`() {
            assertThat(GraphSchema.canonicalizeNodeType("module")).isEqualTo("project")
        }

        @Test
        fun `should preserve unknown node type as-is`() {
            assertThat(GraphSchema.canonicalizeNodeType("domain")).isEqualTo("domain")
            assertThat(GraphSchema.canonicalizeNodeType("totally-unknown")).isEqualTo("totally-unknown")
        }
    }

    @Nested
    inner class CanonicalEdgeTypes {

        @Test
        fun `should expose the six produced edge types as canonical values`() {
            assertThat(EdgeType.entries.map { it.wire })
                .containsExactlyInAnyOrder(
                    "contains", "import", "reference", "agent_reference",
                    "has_section", "subsection"
                )
        }

        @Test
        fun `should return canonical edge type unchanged`() {
            for (type in EdgeType.entries) {
                assertThat(GraphSchema.canonicalizeEdgeType(type.wire)).isEqualTo(type.wire)
            }
        }

        @Test
        fun `should resolve plural aliases to canonical singular edge types`() {
            assertThat(GraphSchema.canonicalizeEdgeType("imports")).isEqualTo("import")
            assertThat(GraphSchema.canonicalizeEdgeType("references")).isEqualTo("reference")
            assertThat(GraphSchema.canonicalizeEdgeType("agent_references")).isEqualTo("agent_reference")
            assertThat(GraphSchema.canonicalizeEdgeType("contains")).isEqualTo("contains")
        }

        @Test
        fun `should resolve underscore and dash spelling variants`() {
            assertThat(GraphSchema.canonicalizeEdgeType("agent-reference")).isEqualTo("agent_reference")
            assertThat(GraphSchema.canonicalizeEdgeType("has-section")).isEqualTo("has_section")
        }

        @Test
        fun `should preserve unknown edge type as-is`() {
            assertThat(GraphSchema.canonicalizeEdgeType("depends_on")).isEqualTo("depends_on")
            assertThat(GraphSchema.canonicalizeEdgeType("totally-unknown")).isEqualTo("totally-unknown")
        }
    }

    @Nested
    inner class Idempotence {

        @Test
        fun `node canonicalization should be idempotent`() {
            val samples = listOf("dir", "module", "file", "unknown-x", "project")
            for (raw in samples) {
                val once = GraphSchema.canonicalizeNodeType(raw)
                assertThat(GraphSchema.canonicalizeNodeType(once)).isEqualTo(once)
            }
        }

        @Test
        fun `edge canonicalization should be idempotent`() {
            val samples = listOf("imports", "references", "has-section", "unknown-x", "contains")
            for (raw in samples) {
                val once = GraphSchema.canonicalizeEdgeType(raw)
                assertThat(GraphSchema.canonicalizeEdgeType(once)).isEqualTo(once)
            }
        }
    }

    @Nested
    inner class ReservedVocabulary {

        @Test
        fun `should declare reserved node types that are known but not produced`() {
            assertThat(GraphSchema.reservedNodeTypes)
                .contains("domain", "flow", "step", "service", "endpoint", "config", "concept")
            assertThat(GraphSchema.reservedNodeTypes)
                .doesNotContainAnyElementsOf(NodeType.entries.map { it.wire })
        }

        @Test
        fun `should declare reserved edge types that are known but not produced`() {
            assertThat(GraphSchema.reservedEdgeTypes)
                .contains("depends_on", "deploys", "documents", "similar_to")
            assertThat(GraphSchema.reservedEdgeTypes)
                .doesNotContainAnyElementsOf(EdgeType.entries.map { it.wire })
        }

        @Test
        fun `reserved vocabulary must be preserved by canonicalization`() {
            for (type in GraphSchema.reservedNodeTypes) {
                assertThat(GraphSchema.canonicalizeNodeType(type)).isEqualTo(type)
            }
            for (type in GraphSchema.reservedEdgeTypes) {
                assertThat(GraphSchema.canonicalizeEdgeType(type)).isEqualTo(type)
            }
        }
    }
}
