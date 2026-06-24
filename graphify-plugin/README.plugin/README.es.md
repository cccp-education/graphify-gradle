<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — Internos del Plugin

> Guía para desarrolladores y contribuyentes del plugin Gradle `graphify-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **Versión**: `0.0.2` · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.graphify`
- **Toolchain**: Java 23 · Kotlin 2.2.20 · Gradle 9.x (wrapper)
- **Build**: `./gradlew build -x test` · **Tests**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **Estado**: Liquidado, inactivo.

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Disposición de módulos

```
graphify-gradle/
├── build.gradle.kts                     # consume graphify-plugin a sí mismo (dogfooding)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # Punto de entrada — registra las tareas
        ├── GraphifyExtension.kt         # DSL: rootDir, outputFile, foundryDir, dagLevels, excludePatterns
        ├── ScanWorkspaceTask.kt         # Recorrido de FS → graph.json (nodos, aristas, comunidades)
        ├── VerifyDagAcyclicTask.kt      # Aplica dagLevels entre los boroughs
        └── model/
            └── GraphModel.kt            # GraphNode, GraphEdge, GraphCommunity, GraphModel
```

## Posición N0

`graphify-gradle` se sitúa en **N0** en el DAG del workspace. Produce el artefacto
compartido `graph.json` (graphify-plugin se publica en el nivel N0, codebase-gradle es N1
y lo consume). **No hay dependencias de contrato N0 de `workspace-bom` (MEMPHIS)**
declaradas en `build.gradle.kts` — graphify es autónomo: Kotlin stdlib + Jackson.

## Dependencias clave

Desde `gradle/libs.versions.toml`:

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`, `kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`, `jackson-dataformat-yaml` (empaquetado como `api`)
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — aserciones de test
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — logging de runtime de test
- **Gradle TestKit** — tests de integración
- **com.gradleup.gratatouille** 0.1.4 — plugin de publicación (Plugin Portal)

Sin koog, sin langchain4j, sin pgvector — graphify es un analizador puro del sistema de archivos.

## Tareas y extensión

Registradas por `GraphifyPlugin` (`graphify.GraphifyPlugin`):

| Tarea | Clase | Grupo |
|------|-------|-------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask` (depends + finalizedBy) | collect |

Nombre de la extensión: `graphify` (`GraphifyExtension`). Propiedades:
`rootDir`, `outputFile`, `foundryDir`, `dagLevels`, `excludePatterns`.

Ambas tareas scan/verify llevan `@DisableCachingByDefault` (dependen del FS / estado del
workspace); `collectFromWorkspace` además llama `doNotTrackState(...)`.

## Semántica del escaneo (`ScanWorkspaceTask`)

- Recorre `rootDir` con `Files.walkFileTree`, saltando cualquier subárbol que coincida
  con globs `excludePatterns` o nombres integrados (`build`, `node_modules`, `.gradle`,
  `.git`, `.idea`, `target`).
- Detecta proyectos mediante archivos marcador: `build.gradle.kts`, `build.gradle`,
  `pom.xml`, `package.json`.
- Emite tres tipos de aristas:
  - `contains` — directorio → archivo/subdirectorio hijo
  - `import` — imports Kotlin `.kt`/`.kts` resueltos a directorios de proyectos hermanos
  - `reference` — `link:`/`include:`/`xref:`/`image:` de AsciiDoc y rutas entre backticks
  - `agent_reference` — rutas separadas por barras en `INDEX.adoc` (punteros de agentes)
- Las comunidades provienen de `buildRepoMap`: el directorio ancestro más cercano que
  contiene un directorio o archivo `.git` se convierte en el id de comunidad.
- La salida se serializa con Jackson `ObjectMapper` (inclusión NON_NULL, pretty-print).

## Verificación del DAG (`VerifyDagAcyclicTask`)

- Itera los subdirectorios de `foundryDir`, lee cada `build.gradle.kts`, y extrae
  los imports de plugins `id("…") version "…"` mediante regex.
- Resuelve tanto el nombre del proyecto como el de la dependencia contra `dagLevels`,
  aplicando variantes de normalización de sufijo (`-gradle`, `-plugin`, `_`↔`-`).
- Se lanza una violación cuando un borough en nivel N importa un plugin cuyo N es mayor
  que el suyo — falla el build con `GradleException`.

## Matriz de tests

| Archivo de suite | Alcance |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | Unit — extracción de nodos/aristas/comunidades |
| `VerifyDagAcyclicTask.kt`             | Unit — resolución de niveles DAG e informe de violaciones |
| `ScanAndVerifyIntegrationTest.kt`     | Integración — cadena `collectAndVerify` |
| `ScanWorkspaceIntegrationTest.kt`     | Integración — escaneo real del workspace (Gradle TestKit) |
| `model/GraphModelTest.kt`             | Unit — data classes y serialización |

Total: **58/58 PASS**. Plataforma JUnit5, aserciones AssertJ, Gradle TestKit para las
suites de integración.

## Ajuste JVM

No se requieren flags GC especiales — graphify es un escaneo por lotes de corta duración.
Para workspaces muy grandes, aumente el heap:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## Comandos de build

```bash
./gradlew build                      # build completo (compila + tests)
./gradlew build -x test              # solo compilar
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # publicación local
```

## Pipeline CI

`.github/workflows/test.yml` se ejecuta en `ubuntu-latest`, **JDK 24** (Temurin),
timeout 15 min. Pasos:

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — cadena integrada en el workspace en vivo

Nota: la CI construye con JDK 24 aunque la toolchain apunta a Java 23; cualquiera de los dos
satisface el requisito de toolchain de Kotlin 2.2.20.

## Publicación

El artefacto del plugin se firma y publica en Maven Central mediante el plugin
**`com.gradleup.gratatouille`** (`alias(libs.plugins.publish)`, versión 0.1.4 —
*no* `nmcp`).

Desde `graphify-plugin/build.gradle.kts`:

- `group = "education.cccp"`, `version = "0.0.2"`
- `gradlePlugin { … }` declara el plugin `graphify` (id `education.cccp.graphify`,
  impl `graphify.GraphifyPlugin`, tags `knowledge-graph`, `workspace`,
  `dependency-analysis`, `graphify`), sitio `https://cccp.education/`,
  vcs `https://github.com/cccp-education/graphify-gradle.git`.
- `java { withJavadocJar(); withSourcesJar() }`.
- `publishing.repositories { mavenCentral() }` — publica directamente al Central Portal.
- `signing { useGpgCmd() }` — firma toda publicación non-SNAPSHOT, non-CI.
- El POM declara Apache 2.0, desarrollador `cccp-education` (`cccp.edu@gmail.com`),
  SCM apuntando a `github.com/cccp-education/graphify-gradle`.
- La propiedad de proyecto opcional `relocationGroup` inyecta un bloque
  `<distributionManagement>` `<relocation>` en el XML del POM (usado si se necesita
  una reubicación de grupo).

Comando de publicación (cuando credenciales + GPG están disponibles):

```bash
./gradlew publishToMavenLocal                                 # sanity local
# la release al Central Portal requiere credenciales del portal en ~/.gradle/gradle.properties
```

## Documentación de arquitectura

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs y gobernanza (GF-0…GF-6, PUB)
- [BACKLOG.adoc](../BACKLOG.adoc) — items pendientes
- [LICENSE](../LICENSE) — Apache 2.0

## Contribución

1. El build compila: `./gradlew build -x test`
2. Tests en verde: `./gradlew test` (58/58)
3. Mantenga el plugin autónomo — sin dependencias LLM/RAG/db sobre contratos N0
4. Respete la semántica `@DisableCachingByDefault` y `doNotTrackState` para scan/verify

## Licencia

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`.