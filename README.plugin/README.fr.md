<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — Internes du Plugin

> Guide développeur et contributeur pour le plugin Gradle `graphify-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=Tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Couverture](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![Licence](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.2` · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.graphify`
- **Toolchain** : Java 23 · Kotlin 2.2.20 · Gradle 9.x (wrapper)
- **Build** : `./gradlew build -x test` · **Tests** : `./gradlew test` (JUnit5 — 58/58 PASS)
- **Statut** : Liquidé, dormant.

🌐 Langues : [English](README.md) | **Français**

---

## Disposition des modules

```
graphify-gradle/
├── build.gradle.kts                     # consomme lui-même graphify-plugin (dogfooding)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # Point d'entrée — enregistre les tâches
        ├── GraphifyExtension.kt         # DSL : rootDir, outputFile, foundryDir, dagLevels, excludePatterns
        ├── ScanWorkspaceTask.kt         # Parcours FS → graph.json (nœuds, arêtes, communautés)
        ├── VerifyDagAcyclicTask.kt      # Applique les dagLevels sur les boroughs
        └── model/
            └── GraphModel.kt            # GraphNode, GraphEdge, GraphCommunity, GraphModel
```

## Position N0

`graphify-gradle` se situe au **N0** dans le DAG du workspace. Il produit l'artefact
partagé `graph.json` (graphify-plugin est publié au niveau N0, codebase-gradle est N1
et le consomme). Aucune **dépendance de contrat N0 de `workspace-bom` (MEMPHIS)** n'est
déclarée dans `build.gradle.kts` — graphify est autonome : Kotlin stdlib + Jackson.

## Dépendances clés

Depuis `gradle/libs.versions.toml` :

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`, `kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`, `jackson-dataformat-yaml` (bundlés en `api`)
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — assertions de test
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — logging runtime de test
- **Gradle TestKit** — tests d'intégration
- **com.gradleup.gratatouille** 0.1.4 — plugin de publication (Plugin Portal)

Pas de koog, pas de langchain4j, pas de pgvector — graphify est un analyseur pur du système de fichiers.

## Tâches & extension

Enregistrées par `GraphifyPlugin` (`graphify.GraphifyPlugin`) :

| Tâche | Classe | Groupe |
|------|--------|--------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask` (depends + finalizedBy) | collect |

Nom de l'extension : `graphify` (`GraphifyExtension`). Propriétés :
`rootDir`, `outputFile`, `foundryDir`, `dagLevels`, `excludePatterns`.

Les deux tâches scan/verify portent `@DisableCachingByDefault` (dépendent du FS / état
du workspace) ; `collectFromWorkspace` appelle en plus `doNotTrackState(...)`.

## Sémantique du scan (`ScanWorkspaceTask`)

- Parcourt `rootDir` avec `Files.walkFileTree`, en ignorant tout sous-arbre
  correspondant aux globs `excludePatterns` ou aux noms intégrés (`build`,
  `node_modules`, `.gradle`, `.git`, `.idea`, `target`).
- Détecte les projets via fichiers marqueurs : `build.gradle.kts`, `build.gradle`,
  `pom.xml`, `package.json`.
- Émet trois types d'arêtes :
  - `contains` — répertoire → fichier/sous-répertoire enfant
  - `import` — imports Kotlin `.kt`/`.kts` résolus vers des répertoires de projets frères
  - `reference` — `link:`/`include:`/`xref:`/`image:` AsciiDoc et chemins entre backticks
  - `agent_reference` — chemins slash-séparés dans `INDEX.adoc` (pointeurs d'agents)
- Les communautés proviennent de `buildRepoMap` : le répertoire englobant le plus proche
  contenant un dossier ou fichier `.git` devient l'id de communauté.
- La sortie est sérialisée via Jackson `ObjectMapper` (inclusion NON_NULL, pretty-print).

## Vérification du DAG (`VerifyDagAcyclicTask`)

- Itère les sous-répertoires de `foundryDir`, lit chaque `build.gradle.kts`, et extrait
  les imports de plugins `id("…") version "…"` via regex.
- Résout les noms de projet ET de dépendance contre `dagLevels`, en appliquant des
  variantes de normalisation de suffixe (`-gradle`, `-plugin`, `_`↔`-`).
- Une violation est levée quand un borough de niveau N importe un plugin dont le niveau N
  est supérieur au sien — échec du build avec `GradleException`.

## Matrice de tests

| Fichier de suite | Portée |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | Unit — extraction nœuds/arêtes/communautés |
| `VerifyDagAcyclicTask.kt`             | Unit — résolution de niveaux DAG et reporting de violations |
| `ScanAndVerifyIntegrationTest.kt`     | Intégration — chaîne `collectAndVerify` |
| `ScanWorkspaceIntegrationTest.kt`     | Intégration — scan réel du workspace (Gradle TestKit) |
| `model/GraphModelTest.kt`             | Unit — data classes et sérialisation |

Total : **58/58 PASS**. Platform JUnit5, assertions AssertJ, Gradle TestKit pour les
suites d'intégration.

## Réglage JVM

Aucun flag GC particulier n'est requis — graphify est un scan batch court. Pour les très
gros workspaces, augmentez le tas :

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## Commandes de build

```bash
./gradlew build                      # build complet (compilation + tests)
./gradlew build -x test              # compilation seule
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # publication locale
```

## Pipeline CI

`.github/workflows/test.yml` s'exécute sur `ubuntu-latest`, **JDK 24** (Temurin),
timeout 15 min. Étapes :

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — chaîne intégrée sur le workspace réel

Note : la CI build avec JDK 24 bien que la toolchain cible Java 23 ; l'un ou l'autre
satisfait à l'exigence de toolchain Kotlin 2.2.20.

## Publication

L'artefact plugin est signé et publié sur Maven Central via le plugin
**`com.gradleup.gratatouille`** (`alias(libs.plugins.publish)`, version 0.1.4 —
*pas* `nmcp`).

Depuis `graphify-plugin/build.gradle.kts` :

- `group = "education.cccp"`, `version = "0.0.2"`
- `gradlePlugin { … }` déclare le plugin `graphify` (id `education.cccp.graphify`,
  impl `graphify.GraphifyPlugin`, tags `knowledge-graph`, `workspace`,
  `dependency-analysis`, `graphify`), site `https://cccp.education/`,
  vcs `https://github.com/cccp-education/graphify-gradle.git`.
- `java { withJavadocJar(); withSourcesJar() }`.
- `publishing.repositories { mavenCentral() }` — publication directe sur le portail Central.
- `signing { useGpgCmd() }` — signe toute publication non-SNAPSHOT, non-CI.
- Le POM déclare Apache 2.0, développeur `cccp-education` (`cccp.edu@gmail.com`),
  SCM pointant vers `github.com/cccp-education/graphify-gradle`.
- La propriété optionnelle `relocationGroup` du projet injecte un bloc
  `<distributionManagement>` `<relocation>` dans le XML du POM (utilisé en cas de
  relocalisation de groupe).

Commande de publication (quand identifiants + GPG sont disponibles) :

```bash
./gradlew publishToMavenLocal                                 # contrôle local
# la release vers le portail Central nécessite des identifiants portail dans ~/.gradle/gradle.properties
```

## Documentation d'architecture

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs et gouvernance (GF-0…GF-6, PUB)
- [BACKLOG.adoc](../BACKLOG.adoc) — items restants
- [LICENSE](../LICENSE) — Apache 2.0

## Contribution

1. Le build compile : `./gradlew build -x test`
2. Tests au vert : `./gradlew test` (58/58)
3. Gardez le plugin autonome — aucune dépendance LLM/RAG/db sur les contrats N0
4. Respectez la sémantique `@DisableCachingByDefault` et `doNotTrackState` pour scan/verify

## Licence

Apache License 2.0 — voir [LICENSE](../LICENSE).

---

_Partie de l'écosystème CCCP Education — `groupId: education.cccp`._