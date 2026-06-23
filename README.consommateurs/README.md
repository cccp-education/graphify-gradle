<!-- master source — other languages are translations of this file -->
# graphify-gradle — Consumer Guide

> Gradle plugin for extracting a knowledge graph (nodes, edges, communities) across a workspace.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **Version**: `0.0.2` · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.graphify`
- **Build**: `./gradlew build` · **Tests**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **Status**: Liquidé, dormant — extracted graph powers downstream consumers.

🌐 Languages: **EN** | [Français](README.fr.md)

---

## What it does

`graphify-gradle` walks a workspace filesystem, detects projects (Gradle/Maven/Node),
files, directories, and intra-workspace relationships, then emits a standardized
`graph.json` describing **nodes**, **edges**, and **communities**. Communities are
derived from enclosing Git repositories (one `.git` ancestor = one community).

The emitted `graph.json` is consumed across the CCCP Education ecosystem by:

```
graphify-gradle (graph.json) → plantuml-gradle (diagrams)
                            → bakery-gradle     (static site graph view)
                            → runner-gradle     (workspace touring)
```

## Quick Start

### 1. Apply the plugin

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. Configure the extension

```gradle
graphify {
    rootDir.set(file("/home/cheroliv/workspace"))
    outputFile.set(file("graph.json"))
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))
    dagLevels.set(mapOf(
        "graphify-gradle"  to 0,
        "codebase-gradle"  to 1,
        "bakery-gradle"    to 2,
        "codex-gradle"     to 2,
        "planner-gradle"   to 2,
        "plantuml-gradle"  to 2,
        "runner-gradle"    to 3
        // …other boroughs…
    ))
}
```

### 3. Generate the graph

```bash
./gradlew collectFromWorkspace      # scan filesystem → graph.json
./gradlew verifyDagAcyclic          # enforce N0→N3 DAG layering
./gradlew collectAndVerify          # chain: collectFromWorkspace + verifyDagAcyclic
```

The output `graph.json` shape:

```json
{
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

## Available tasks

| Task | Group | Description |
|------|-------|-------------|
| `collectFromWorkspace` | collect   | Walk the workspace → emit `graph.json` (nodes, edges, communities). |
| `verifyDagAcyclic`     | verify    | Enforce declared `dagLevels` — a project may not depend on a higher N level. |
| `collectAndVerify`     | collect   | Convenience chain: `collectFromWorkspace` then `verifyDagAcyclic` (finalizedBy). |

## Extension DSL

```gradle
graphify {
    // Root of the filesystem walk (REQUIRED).
    rootDir.set(file("/home/cheroliv/workspace"))

    // Where to write graph.json (REQUIRED).
    outputFile.set(file("graph.json"))

    // Directory holding the borough projects — scanned for DAG level violations.
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // Map project name → N level (used by verifyDagAcyclic).
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // Glob exclude patterns (optional — defaults below).
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))
}
```

## Prerequisites

- **Java** 23+ (Kotlin 2.2.20 toolchain)
- **Gradle** 9.x (wrapper provided)
- Read access to the workspace you intend to scan

## Build & test

```bash
./gradlew build                      # full build (compiles + tests)
./gradlew build -x test              # compile only
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # local publish
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `graph.json` empty              | Confirm `rootDir` resolves and is readable; check `excludePatterns`. |
| `DAG VIOLATIONS DETECTED`       | A borough imports a plugin from a higher N level — adjust `dagLevels` or the import. |
| Scan slow on large monorepos    | Narrow `rootDir`, extend `excludePatterns`, avoid scanning unrelated subtrees. |
| Inaccessible path warnings      | Harmless — the walker skips unreadable paths and continues with partial results. |

## License

Apache License 2.0 — see [LICENSE](../LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._