<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — Guide Consommateur

> Plugin Gradle pour extraire un graphe de connaissance (nœuds, arêtes, communautés) d'un workspace.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=Tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Licence](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.2` · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.graphify`
- **Build** : `./gradlew build` · **Tests** : `./gradlew test` (JUnit5 — 58/58 PASS)
- **Statut** : Liquidé, dormant — le graphe extrait alimente les consommateurs en aval.

🌐 Langues : [English](README.md) | **Français**

---

## Ce que ça fait

`graphify-gradle` parcourt le système de fichiers d'un workspace, détecte les projets
(Gradle/Maven/Node), fichiers, répertoires, et les relations intra-workspace, puis émet
un `graph.json` normalisé décrivant des **nœuds**, des **arêtes** et des **communautés**.
Les communautés sont déduites des dépôts Git englobants (un ancêtre `.git` = une communauté).

Le `graph.json` produit est consommé dans l'écosystème CCCP Education par :

```
graphify-gradle (graph.json) → plantuml-gradle (diagrammes)
                            → bakery-gradle     (vue graphe du site statique)
                            → runner-gradle     (parcours du workspace)
```

## Démarrage rapide

### 1. Appliquer le plugin

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. Configurer l'extension

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
        // …autres boroughs…
    ))
}
```

### 3. Générer le graphe

```bash
./gradlew collectFromWorkspace      # parcours FS → graph.json
./gradlew verifyDagAcyclic          # applique la stratification N0→N3
./gradlew collectAndVerify          # chaîne : collectFromWorkspace + verifyDagAcyclic
```

Structure du `graph.json` produit :

```json
{
  "schemaVersion": 1,
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file|section", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference|has_section|subsection", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

`schemaVersion` est un entier décrivant le *schéma de transport*, indépendant de la version du plugin. `graphify.model.GraphModel` porte aussi `@JsonIgnoreProperties(ignoreUnknown = true)`, de sorte qu'un consommateur strict qui met à jour sa dépendance tolère les futurs champs sans aucune modification de code, et qu'un `graph.json` hérité dépourvu de `schemaVersion` se lit toujours avec la valeur par défaut.

## Tâches disponibles

| Tâche | Groupe | Description |
|------|--------|-------------|
| `collectFromWorkspace` | collect   | Parcourt le workspace → émet `graph.json` (nœuds, arêtes, communautés). |
| `verifyDagAcyclic`     | verify    | Applique les `dagLevels` déclarés — un projet ne peut dépendre d'un niveau N supérieur. |
| `collectAndVerify`     | collect   | Chaîne pratique : `collectFromWorkspace` puis `verifyDagAcyclic` (finalizedBy). |

## DSL d'extension

```gradle
graphify {
    // Racine du parcours système de fichiers (REQUIS).
    rootDir.set(file("/home/cheroliv/workspace"))

    // Destination du graph.json (REQUIS).
    outputFile.set(file("graph.json"))

    // Répertoire contenant les projets boroughs — analysé pour les violations de niveaux DAG.
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // Map nom de projet → niveau N (utilisé par verifyDagAcyclic).
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // Patterns glob d'exclusion (optionnel — défaut ci-dessous).
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))

    // Rescan incrémental opt-in (défaut : false → scan complet).
    incremental.set(true)

    // Où le cache d'empreintes est stocké (défaut : build/graphify/fingerprints.json).
    cacheFile.set(layout.buildDirectory.file("graphify/fingerprints.json").get().asFile)
}
```

## Scan incrémental

`collectFromWorkspace` parcourt l'arborescence entière dans les deux modes ; c'est le
parcours qui détecte les fichiers ajoutés et supprimés. Avec `incremental.set(true)`, le
*parsing par fichier* est mémoïsé :

- un fichier dont la taille et le mtime sont inchangés réutilise son extraction en cache (aucune lecture) ;
- un fichier dont le mtime a changé mais dont le contenu est inchangé est détecté par SHA-256
  et réutilise l'extraction d'un fichier identique (par ex. après `git checkout`) ;
- seul le contenu réellement nouveau est re-parsé.

Le cache est un document JSON sous `build/` (jamais commité) et est élagué aux
fichiers présents à chaque scan. Ce n'est qu'une optimisation : un cache manquant, corrompu
ou de version inconnue se dégrade silencieusement en un scan complet, et le `graph.json`
produit est byte-identique à une exécution non incrémentale.

## Prérequis

- **Java** 23+ (toolchain Kotlin 2.2.20)
- **Gradle** 9.x (wrapper fourni)
- Accès en lecture au workspace à scanner

## Build et tests

```bash
./gradlew build                      # build complet (compilation + tests)
./gradlew build -x test              # compilation seule
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # publication locale
```

## Dépannage

| Symptôme | Solution |
|----------|----------|
| `graph.json` vide              | Vérifiez que `rootDir` est lisible ; contrôlez `excludePatterns`. |
| `DAG VIOLATIONS DETECTED`     | Un borough importe un plugin d'un niveau N supérieur — ajustez `dagLevels` ou l'import. |
| Scan lent sur un gros monorepo | Réduisez `rootDir`, étendez `excludePatterns`, évitez de scanner des sous-arborescences sans rapport. |
| Avertissements « inaccessible path » | Bénins — le walker ignore les chemins illisibles et continue avec des résultats partiels. |

## Licence

Apache License 2.0 — voir [LICENSE](../LICENSE).

---

_Partie de l'écosystème CCCP Education — `groupId: education.cccp`._