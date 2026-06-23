<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — Guía del Consumidor

> Plugin de Gradle para extraer un grafo de conocimiento (nodos, aristas, comunidades) en un workspace.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **Versión**: `0.0.2` · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.graphify`
- **Build**: `./gradlew build` · **Tests**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **Estado**: Liquidado, inactivo — el grafo extraído alimenta a los consumidores descendientes.

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Qué hace

`graphify-gradle` recorre el sistema de archivos de un workspace, detecta proyectos (Gradle/Maven/Node), archivos, directorios y relaciones intra-workspace, y luego emite un `graph.json` estandarizado que describe **nodos**, **aristas** y **comunidades**. Las comunidades se derivan de los repositorios Git que las contienen (un ancestro `.git` = una comunidad).

El `graph.json` emitido se consume en el ecosistema CCCP Education por:

```
graphify-gradle (graph.json) → plantuml-gradle (diagramas)
                             → bakery-gradle     (vista de grafo del sitio estático)
                             → runner-gradle     (recorrido del workspace)
```

## Inicio rápido

### 1. Aplicar el plugin

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. Configurar la extensión

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
        // …otros boroughs…
    ))
}
```

### 3. Generar el grafo

```bash
./gradlew collectFromWorkspace      # recorrer FS → graph.json
./gradlew verifyDagAcyclic          # aplicar la estratificación N0→N3
./gradlew collectAndVerify          # cadena: collectFromWorkspace + verifyDagAcyclic
```

Forma del `graph.json` generado:

```json
{
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

## Tareas disponibles

| Tarea | Grupo | Descripción |
|------|-------|-------------|
| `collectFromWorkspace` | collect   | Recorrer el workspace → emitir `graph.json` (nodos, aristas, comunidades). |
| `verifyDagAcyclic`     | verify    | Aplicar los `dagLevels` declarados — un proyecto no puede depender de un nivel N superior. |
| `collectAndVerify`     | collect   | Cadena de conveniencia: `collectFromWorkspace` luego `verifyDagAcyclic` (finalizedBy). |

## DSL de extensión

```gradle
graphify {
    // Raíz del recorrido del sistema de archivos (OBLIGATORIO).
    rootDir.set(file("/home/cheroliv/workspace"))

    // Dónde escribir graph.json (OBLIGATORIO).
    outputFile.set(file("graph.json"))

    // Directorio que contiene los proyectos borough — se escanea para detectar violaciones de niveles DAG.
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // Mapa nombre de proyecto → nivel N (usado por verifyDagAcyclic).
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // Patrones glob de exclusión (opcional — valores por defecto abajo).
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))
}
```

## Requisitos previos

- **Java** 23+ (toolchain Kotlin 2.2.20)
- **Gradle** 9.x (wrapper incluido)
- Acceso de lectura al workspace que se va a escanear

## Build y tests

```bash
./gradlew build                      # build completo (compila + tests)
./gradlew build -x test              # solo compilar
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # publicación local
```

## Solución de problemas

| Síntoma | Solución |
|---------|-----|
| `graph.json` vacío              | Confirme que `rootDir` se resuelve y es legible; revise `excludePatterns`. |
| `DAG VIOLATIONS DETECTED`       | Un borough importa un plugin de un nivel N superior — ajuste `dagLevels` o la importación. |
| Escaneo lento en monorepos grandes | Restrinja `rootDir`, amplíe `excludePatterns`, evite escanear subárboles no relacionados. |
| Advertencias de ruta inaccesible | Inofensivas — el walker omite rutas ilegibles y continúa con resultados parciales. |

## Licencia

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`.