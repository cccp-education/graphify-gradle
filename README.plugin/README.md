<!-- master source — other languages are translations of this file -->
# graphify-gradle — Plugin Internals

> Developer & contributor guide for the `graphify-plugin` Gradle plugin.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **Version**: `0.0.2` · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.graphify`
- **Toolchain**: Java 23 · Kotlin 2.2.20 · Gradle 9.x (wrapper)
- **Build**: `./gradlew build -x test` · **Tests**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **Status**: Liquidé, dormant.

🌐 Languages: **EN** | [Français](README.fr.md)

---

## Module layout

```
graphify-gradle/
├── build.gradle.kts                     # consumes graphify-plugin itself (dogfooding)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # Plugin entry point — registers tasks
        ├── GraphifyExtension.kt         # DSL: rootDir, outputFile, foundryDir, dagLevels, excludePatterns
        ├── ScanWorkspaceTask.kt         # Filesystem walk → graph.json (nodes, edges, communities)
        ├── VerifyDagAcyclicTask.kt      # Enforces dagLevels across foundry boroughs
        └── model/
            └── GraphModel.kt            # GraphNode, GraphEdge, GraphCommunity, GraphModel
```

## N0 position

`graphify-gradle` sits at **N0** in the workspace DAG. It produces the shared
`graph.json` artifact (graphify-plugin is published at level N0, codebase-gradle is N1
and consumes it). There are **no `workspace-bom` (MEMPHIS) N0 contract dependencies**
declared in `build.gradle.kts` — graphify is self-contained: Kotlin stdlib + Jackson.

## Key dependencies

From `gradle/libs.versions.toml`:

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`, `kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`, `jackson-dataformat-yaml` (bundled as `api`)
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — test assertions
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — test runtime logging
- **Gradle TestKit** — integration tests
- **com.gradleup.gratatouille** 0.1.4 — publish plugin (Plugin Portal)

No koog, no langchain4j, no pgvector — graphify is a pure filesystem analyzer.

## Tasks & extension

Registered by `GraphifyPlugin` (`graphify.GraphifyPlugin`):

| Task | Class | Group |
|------|-------|-------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask` (depends + finalizedBy) | collect |

Extension name: `graphify` (`GraphifyExtension`). Properties:
`rootDir`, `outputFile`, `foundryDir`, `dagLevels`, `excludePatterns`.

Both scan/verify tasks carry `@DisableCachingByDefault` (filesystem / workspace-state
dependent); `collectFromWorkspace` additionally calls `doNotTrackState(...)`.

## Scan semantics (`ScanWorkspaceTask`)

- Walks `rootDir` with `Files.walkFileTree`, skipping any subtree matching
  `excludePatterns` globs or built-in names (`build`, `node_modules`, `.gradle`,
  `.git`, `.idea`, `target`).
- Detects projects via marker files: `build.gradle.kts`, `build.gradle`, `pom.xml`,
  `package.json`.
- Emits three edge kinds:
  - `contains` — directory → child file/dir
  - `import` — Kotlin `.kt`/`.kts` imports resolved to sibling project dirs
  - `reference` — AsciiDoc `link:`/`include:`/`xref:`/`image:` and backtick paths
  - `agent_reference` — `INDEX.adoc` slash-separated paths (agent pointers)
- Communities come from `buildRepoMap`: the nearest enclosing directory containing
  a `.git` directory or file becomes the community id.
- Output is serialized with Jackson `ObjectMapper` (NON_NULL inclusion, pretty-print).

## DAG verification (`VerifyDagAcyclicTask`)

- Iterates `foundryDir`'s subdirectories, reads each `build.gradle.kts`, and extracts
  `id("…") version "…"` plugin imports via regex.
- Resolves both project and dependency names against `dagLevels`, applying suffix
  normalization variants (`-gradle`, `-plugin`, `_`↔`-`).
- A violation is raised when a borough at level N imports a plugin whose N is greater
  than its own — fails the build with `GradleException`.

## Test matrix

| Suite file | Scope |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | Unit — node/edge/community extraction |
| `VerifyDagAcyclicTask.kt`             | Unit — DAG level resolution & violation reporting |
| `ScanAndVerifyIntegrationTest.kt`     | Integration — chained `collectAndVerify` |
| `ScanWorkspaceIntegrationTest.kt`     | Integration — real-workspace scan (Gradle TestKit) |
| `model/GraphModelTest.kt`             | Unit — data classes & serialization |

Total: **58/58 PASS**. JUnit5 platform, AssertJ assertions, Gradle TestKit for the
integration suites.

## JVM tuning

No special GC flags are required — graphify is a short-lived batch scan. For very large
workspaces, increase heap:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## Build commands

```bash
./gradlew build                      # full build (compiles + tests)
./gradlew build -x test              # compile only
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # local publish
```

## CI pipeline

`.github/workflows/test.yml` runs on `ubuntu-latest`, **JDK 24** (Temurin), 15 min
timeout. Steps:

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — integrated chain on the live workspace

Note: CI builds with JDK 24 although the toolchain targets Java 23; either satisfies
the Kotlin 2.2.20 toolchain requirement.

## Publication

Plugin artifact is signed & published to Maven Central via the **`com.gradleup.gratatouille`**
plugin (`alias(libs.plugins.publish)`, version 0.1.4 — *not* `nmcp`).

From `graphify-plugin/build.gradle.kts`:

- `group = "education.cccp"`, `version = "0.0.2"`
- `gradlePlugin { … }` declares the `graphify` plugin (id `education.cccp.graphify`,
  impl `graphify.GraphifyPlugin`, tags `knowledge-graph`, `workspace`,
  `dependency-analysis`, `graphify`), website `https://cccp.education/`,
  vcs `https://github.com/cccp-education/graphify-gradle.git`.
- `java { withJavadocJar(); withSourcesJar() }`.
- `publishing.repositories { mavenCentral() }` — publishes straight to Central Portal.
- `signing { useGpgCmd() }` — signs every non-SNAPSHOT, non-CI publication.
- POM declares Apache 2.0, developer `cccp-education` (`cccp.edu@gmail.com`),
  SCM pointing to `github.com/cccp-education/graphify-gradle`.
- Optional `relocationGroup` project property injects a `<distributionManagement>`
  `<relocation>` block into the POM XML (used if a group relocation is needed).

Publication command (when credentials + GPG are available):

```bash
./gradlew publishToMavenLocal                                 # local sanity
# release to Central Portal requires portal credentials in ~/.gradle/gradle.properties
```

## Architecture docs

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs & governance (GF-0…GF-6, PUB)
- [BACKLOG.adoc](../BACKLOG.adoc) — outstanding items
- [LICENSE](../LICENSE) — Apache 2.0

## Contributing

1. Build compiles: `./gradlew build -x test`
2. Tests green: `./gradlew test` (58/58)
3. Keep the plugin self-contained — no LLM/RAG/db dependencies on N0 contracts
4. Respect `@DisableCachingByDefault` and `doNotTrackState` semantics for scan/verify

## License

Apache License 2.0 — see [LICENSE](../LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._