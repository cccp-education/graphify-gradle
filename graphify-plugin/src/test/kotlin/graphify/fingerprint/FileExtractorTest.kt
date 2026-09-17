package graphify.fingerprint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FileExtractorTest {

    @Nested
    inner class KotlinImports {

        @Test
        fun `should extract plain imports`() {
            val extraction = FileExtractor.extract(
                "App.kt",
                """
                package com.example

                import com.example.Util
                import java.io.File

                class App
                """.trimIndent()
            )

            assertThat(extraction.imports).containsExactly("com.example.Util", "java.io.File")
        }

        @Test
        fun `should ignore indented or commented import-like lines`() {
            val extraction = FileExtractor.extract(
                "App.kt",
                """
                // import com.example.Commented
                class App {
                    val x = "import com.example.StringLiteral"
                }
                """.trimIndent()
            )

            assertThat(extraction.imports).isEmpty()
        }

        @Test
        fun `should ignore non-kotlin files`() {
            val extraction = FileExtractor.extract("App.java", "import java.util.List;")

            assertThat(extraction.imports).isEmpty()
        }
    }

    @Nested
    inner class AdocReferences {

        @Test
        fun `should extract link include xref and image targets`() {
            val extraction = FileExtractor.extract(
                "book.adoc",
                """
                See link:other.adoc[Other].
                include:chapter.adoc[]
                xref:appendix.adoc[Appendix]
                image:diagram.png[Diagram]
                """.trimIndent()
            )

            assertThat(extraction.adocReferences)
                .containsExactly("other.adoc", "chapter.adoc", "appendix.adoc", "diagram.png")
        }

        @Test
        fun `should extract backticked file paths`() {
            val extraction = FileExtractor.extract(
                "book.adoc",
                "See `docs/other.adoc` and `src/App.kt` for details."
            )

            assertThat(extraction.adocReferences).containsExactly("docs/other.adoc", "src/App.kt")
        }
    }

    @Nested
    inner class TocReferences {

        @Test
        fun `should extract adoc file cells from a table`() {
            val extraction = FileExtractor.extract(
                "INDEX.adoc",
                """
                |===
                | Chapter | File
                | Intro | intro.adoc
                | Setup | docs/setup.adoc
                |===
                """.trimIndent()
            )

            assertThat(extraction.tocReferences).containsExactly("intro.adoc", "docs/setup.adoc")
        }
    }

    @Nested
    inner class AgentReferences {

        @Test
        fun `should extract path-like references`() {
            val extraction = FileExtractor.extract(
                "INDEX.adoc",
                "Voir `.agents/INDEX.adoc` et `foundry/public/graphify-gradle`."
            )

            assertThat(extraction.agentReferences)
                .containsExactly(".agents/INDEX.adoc", "foundry/public/graphify-gradle")
        }
    }

    @Nested
    inner class Sections {

        @Test
        fun `should extract headings with level and line`() {
            val extraction = FileExtractor.extract(
                "book.adoc",
                """
                = Book
                Preamble.

                == Chapter
                Body.

                === Sub
                More.
                """.trimIndent()
            )

            assertThat(extraction.sections).containsExactly(
                SectionExtraction(title = "Book", level = 1, line = 1),
                SectionExtraction(title = "Chapter", level = 2, line = 4),
                SectionExtraction(title = "Sub", level = 3, line = 7)
            )
        }

        @Test
        fun `should ignore block delimiter lines`() {
            val extraction = FileExtractor.extract(
                "book.adoc",
                """
                = Book

                ====
                A literal block
                ====

                == Chapter
                """.trimIndent()
            )

            assertThat(extraction.sections)
                .containsExactly(
                    SectionExtraction(title = "Book", level = 1, line = 1),
                    SectionExtraction(title = "Chapter", level = 2, line = 7)
                )
        }

        @Test
        fun `should ignore headings in non-adoc files`() {
            val extraction = FileExtractor.extract("notes.md", "= Not a section")

            assertThat(extraction.sections).isEmpty()
        }
    }

    @Nested
    inner class Dispatch {

        @Test
        fun `should return an empty extraction for an unknown extension`() {
            assertThat(FileExtractor.extract("data.json", "{ \"import com.x\": true }")).isEqualTo(FileExtraction.EMPTY)
        }

        @Test
        fun `should extract both imports and sections for a kotlin script with headings`() {
            val extraction = FileExtractor.extract("build.gradle.kts", "import org.gradle.api.Project")

            assertThat(extraction.imports).containsExactly("org.gradle.api.Project")
            assertThat(extraction.sections).isEmpty()
        }

        @Test
        fun `should be pure — identical content yields identical extraction`() {
            val content = "import com.example.Util"

            assertThat(FileExtractor.extract("a.kt", content))
                .isEqualTo(FileExtractor.extract("b.kt", content))
        }
    }
}
