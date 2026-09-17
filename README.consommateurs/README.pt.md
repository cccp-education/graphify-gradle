<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — Guia do Consumidor

> Plugin Gradle para extrair um grafo de conhecimento (nós, arestas, comunidades) num workspace.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **Versão**: `0.0.2` · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.graphify`
- **Build**: `./gradlew build` · **Testes**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **Estado**: Liquidado, inativo — o grafo extraído alimenta os consumidores downstream.

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## O que faz

`graphify-gradle` percorre o sistema de ficheiros de um workspace, deteta projetos
(Gradle/Maven/Node), ficheiros, diretórios e relações intra-workspace, depois emite um
`graph.json` normalizado que descreve **nós**, **arestas** e **comunidades**. As comunidades
são derivadas dos repositórios Git que as contêm (um ancestral `.git` = uma comunidade).

O `graph.json` emitido é consumido no ecossistema CCCP Education por:

```
graphify-gradle (graph.json) → plantuml-gradle (diagramas)
                             → bakery-gradle     (vista de grafo do site estático)
                             → runner-gradle     (percurso do workspace)
```

## Início rápido

### 1. Aplicar o plugin

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. Configurar a extensão

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
        // …outros boroughs…
    ))
}
```

### 3. Gerar o grafo

```bash
./gradlew collectFromWorkspace      # percorrer FS → graph.json
./gradlew verifyDagAcyclic          # aplicar a estratificação N0→N3
./gradlew collectAndVerify          # cadeia: collectFromWorkspace + verifyDagAcyclic
```

Formato do `graph.json` produzido:

```json
{
  "schemaVersion": 1,
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file|section", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference|has_section|subsection", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

`schemaVersion` é um inteiro que descreve o *esquema de transmissão*, independente da versão do plugin. `graphify.model.GraphModel` também carrega `@JsonIgnoreProperties(ignoreUnknown = true)`, de modo que um consumidor estrito que atualiza a sua dependência tolera campos futuros sem qualquer alteração de código, e um `graph.json` legado sem `schemaVersion` ainda é lido com o valor predefinido.

## Tarefas disponíveis

| Tarefa | Grupo | Descrição |
|------|-------|-------------|
| `collectFromWorkspace` | collect   | Percorrer o workspace → emitir `graph.json` (nós, arestas, comunidades). |
| `verifyDagAcyclic`     | verify    | Aplicar os `dagLevels` declarados — um projeto não pode depender de um nível N superior. |
| `collectAndVerify`     | collect   | Cadeia de conveniência: `collectFromWorkspace` seguido de `verifyDagAcyclic` (finalizedBy). |

## DSL da extensão

```gradle
graphify {
    // Raiz do percurso do sistema de ficheiros (OBRIGATÓRIO).
    rootDir.set(file("/home/cheroliv/workspace"))

    // Onde escrever graph.json (OBRIGATÓRIO).
    outputFile.set(file("graph.json"))

    // Diretório que contém os projetos borough — analisado para detetar violações de níveis DAG.
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // Mapa nome do projeto → nível N (usado por verifyDagAcyclic).
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // Padrões glob de exclusão (opcional — predefinições abaixo).
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))

    // Rescan incremental opt-in (predefinição: false → análise completa).
    incremental.set(true)

    // Onde o cache de impressões digitais é armazenado (predefinição: build/graphify/fingerprints.json).
    cacheFile.set(layout.buildDirectory.file("graphify/fingerprints.json").get().asFile)
}
```

## Varredura incremental

`collectFromWorkspace` varre toda a árvore em ambos os modos; é a
varredura que deteta os ficheiros adicionados e removidos. Com `incremental.set(true)`, a
*análise por ficheiro* é memoizada:

- um ficheiro cujo tamanho e mtime não mudaram reutiliza a sua extração em cache (sem leitura);
- um ficheiro cujo mtime mudou mas cujo conteúdo não mudou é detetado por SHA-256
  e reutiliza a extração de um ficheiro idêntico (por exemplo, após `git checkout`);
- apenas o conteúdo genuinamente novo é reanalisado.

A cache é um documento JSON sob `build/` (nunca commitado) e é podada para
os ficheiros presentes em cada varredura. É apenas uma otimização: uma cache ausente, corrompida
ou de versão desconhecida degrada silenciosamente para uma varredura completa, e o `graph.json`
produzido é byte-idêntico a uma execução não incremental.

## Pré-requisitos

- **Java** 23+ (toolchain Kotlin 2.2.20)
- **Gradle** 9.x (wrapper fornecido)
- Acesso de leitura ao workspace a analisar

## Build e testes

```bash
./gradlew build                      # build completo (compila + testes)
./gradlew build -x test              # apenas compilar
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # publicação local
```

## Resolução de problemas

| Sintoma | Solução |
|---------|-----|
| `graph.json` vazio              | Confirme que `rootDir` resolve e é legível; verifique `excludePatterns`. |
| `DAG VIOLATIONS DETECTED`       | Um borough importa um plugin de um nível N superior — ajuste `dagLevels` ou a importação. |
| Análise lenta em monorepos grandes | Restrinja `rootDir`, amplie `excludePatterns`, evite analisar subárvores não relacionadas. |
| Avisos de caminho inacessível   | Inofensivos — o walker ignora caminhos ilegíveis e continua com resultados parciais. |

## Licença

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`.