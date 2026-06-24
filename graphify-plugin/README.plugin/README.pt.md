<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — Internos do Plugin

> Guia de desenvolvimento e contribuição para o plugin Gradle `graphify-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **Versão**: `0.0.2` · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.graphify`
- **Toolchain**: Java 23 · Kotlin 2.2.20 · Gradle 9.x (wrapper)
- **Build**: `./gradlew build -x test` · **Testes**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **Estado**: Liquidado, inativo.

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Disposição de módulos

```
graphify-gradle/
├── build.gradle.kts                     # consome graphify-plugin a si próprio (dogfooding)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # Ponto de entrada — regista as tarefas
        ├── GraphifyExtension.kt         # DSL: rootDir, outputFile, foundryDir, dagLevels, excludePatterns
        ├── ScanWorkspaceTask.kt         # Percurso de FS → graph.json (nós, arestas, comunidades)
        ├── VerifyDagAcyclicTask.kt      # Aplica dagLevels entre boroughs
        └── model/
            └── GraphModel.kt            # GraphNode, GraphEdge, GraphCommunity, GraphModel
```

## Posição N0

`graphify-gradle` situa-se em **N0** no DAG do workspace. Produz o artefacto partilhado
`graph.json` (graphify-plugin é publicado no nível N0, codebase-gradle é N1 e consome-o).
**Não há dependências de contrato N0 de `workspace-bom` (MEMPHIS)** declaradas em
`build.gradle.kts` — graphify é autónomo: Kotlin stdlib + Jackson.

## Dependências principais

De `gradle/libs.versions.toml`:

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`, `kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`, `jackson-dataformat-yaml` (empacotado como `api`)
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — asserções de teste
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — logging de runtime de teste
- **Gradle TestKit** — testes de integração
- **com.gradleup.gratatouille** 0.1.4 — plugin de publicação (Plugin Portal)

Sem koog, sem langchain4j, sem pgvector — graphify é um analisador puro do sistema de ficheiros.

## Tarefas e extensão

Registadas por `GraphifyPlugin` (`graphify.GraphifyPlugin`):

| Tarefa | Classe | Grupo |
|------|-------|-------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask` (depends + finalizedBy) | collect |

Nome da extensão: `graphify` (`GraphifyExtension`). Propriedades:
`rootDir`, `outputFile`, `foundryDir`, `dagLevels`, `excludePatterns`.

Ambas as tarefas scan/verify têm `@DisableCachingByDefault` (dependem do FS / estado do
workspace); `collectFromWorkspace` chama ainda `doNotTrackState(...)`.

## Semântica do escaneamento (`ScanWorkspaceTask`)

- Percorre `rootDir` com `Files.walkFileTree`, ignorando qualquer subárvore que coincida
  com globs `excludePatterns` ou nomes integrados (`build`, `node_modules`, `.gradle`,
  `.git`, `.idea`, `target`).
- Deteta projetos via ficheiros marcador: `build.gradle.kts`, `build.gradle`,
  `pom.xml`, `package.json`.
- Emite três tipos de arestas:
  - `contains` — diretório → ficheiro/subdiretório filho
  - `import` — imports Kotlin `.kt`/`.kts` resolvidos para diretórios de projetos irmãos
  - `reference` — `link:`/`include:`/`xref:`/`image:` de AsciiDoc e caminhos entre backticks
  - `agent_reference` — caminhos separados por barras em `INDEX.adoc` (ponteiros de agentes)
- As comunidades provêm de `buildRepoMap`: o diretório ancestral mais próximo que contém
  um diretório ou ficheiro `.git` torna-se o id de comunidade.
- A saída é serializada com Jackson `ObjectMapper` (inclusão NON_NULL, pretty-print).

## Verificação do DAG (`VerifyDagAcyclicTask`)

- Itera os subdiretórios de `foundryDir`, lê cada `build.gradle.kts`, e extrai
  imports de plugins `id("…") version "…"` via regex.
- Resolve tanto o nome do projeto como o da dependência contra `dagLevels`, aplicando
  variantes de normalização de sufixo (`-gradle`, `-plugin`, `_`↔`-`).
- É levantada uma violação quando um borough no nível N importa um plugin cujo N é maior
  que o seu — falha o build com `GradleException`.

## Matriz de testes

| Ficheiro de suite | Âmbito |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | Unit — extração de nós/arestas/comunidades |
| `VerifyDagAcyclicTask.kt`             | Unit — resolução de níveis DAG e relato de violações |
| `ScanAndVerifyIntegrationTest.kt`     | Integração — `collectAndVerify` encadeado |
| `ScanWorkspaceIntegrationTest.kt`     | Integração — escaneamento real do workspace (Gradle TestKit) |
| `model/GraphModelTest.kt`             | Unit — data classes e serialização |

Total: **58/58 PASS**. Plataforma JUnit5, asserções AssertJ, Gradle TestKit para as
suites de integração.

## Afinação JVM

Não são necessárias flags GC especiais — graphify é um escaneamento batch curto. Para
workspaces muito grandes, aumente o heap:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## Comandos de build

```bash
./gradlew build                      # build completo (compila + testes)
./gradlew build -x test              # apenas compilar
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # publicação local
```

## Pipeline CI

`.github/workflows/test.yml` corre em `ubuntu-latest`, **JDK 24** (Temurin),
timeout 15 min. Passos:

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — cadeia integrada no workspace ativo

Nota: a CI constrói com JDK 24 embora a toolchain tenha como alvo Java 23; qualquer dos dois
satisfaz o requisito de toolchain do Kotlin 2.2.20.

## Publicação

O artefacto do plugin é assinado e publicado no Maven Central via plugin
**`com.gradleup.gratatouille`** (`alias(libs.plugins.publish)`, versão 0.1.4 —
*não* `nmcp`).

De `graphify-plugin/build.gradle.kts`:

- `group = "education.cccp"`, `version = "0.0.2"`
- `gradlePlugin { … }` declara o plugin `graphify` (id `education.cccp.graphify`,
  impl `graphify.GraphifyPlugin`, tags `knowledge-graph`, `workspace`,
  `dependency-analysis`, `graphify`), site `https://cccp.education/`,
  vcs `https://github.com/cccp-education/graphify-gradle.git`.
- `java { withJavadocJar(); withSourcesJar() }`.
- `publishing.repositories { mavenCentral() }` — publica diretamente para o Central Portal.
- `signing { useGpgCmd() }` — assina todas as publicações não-SNAPSHOT, não-CI.
- O POM declara Apache 2.0, programador `cccp-education` (`cccp.edu@gmail.com`),
  SCM a apontar para `github.com/cccp-education/graphify-gradle`.
- A propriedade de projeto opcional `relocationGroup` injeta um bloco
  `<distributionManagement>` `<relocation>` no XML do POM (usado se for necessária
  relocalização de grupo).

Comando de publicação (quando credenciais + GPG disponíveis):

```bash
./gradlew publishToMavenLocal                                 # sanity local
# release para o Central Portal requer credenciais do portal em ~/.gradle/gradle.properties
```

## Documentação de arquitetura

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs e governação (GF-0…GF-6, PUB)
- [BACKLOG.adoc](../BACKLOG.adoc) — itens pendentes
- [LICENSE](../LICENSE) — Apache 2.0

## Contribuição

1. O build compila: `./gradlew build -x test`
2. Testes no verde: `./gradlew test` (58/58)
3. Mantenha o plugin autónomo — sem dependências LLM/RAG/db sobre contratos N0
4. Respeite a semântica `@DisableCachingByDefault` e `doNotTrackState` para scan/verify

## Licença

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`.