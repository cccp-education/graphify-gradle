<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — Внутреннее устройство плагина

> Руководство разработчика и контрибьютора для плагина Gradle `graphify-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **Версия**: `0.0.2` · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.graphify`
- **Тулчейн**: Java 23 · Kotlin 2.2.20 · Gradle 9.x (wrapper)
- **Сборка**: `./gradlew build -x test` · **Тесты**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **Статус**: Завершён, в спящем режиме.

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Расположение модулей

```
graphify-gradle/
├── build.gradle.kts                     # сам использует graphify-plugin (dogfooding)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # точка входа плагина — регистрирует задачи
        ├── GraphifyExtension.kt         # DSL: rootDir, outputFile, foundryDir, dagLevels, excludePatterns
        ├── ScanWorkspaceTask.kt         # обход FS → graph.json (узлы, рёбра, сообщества)
        ├── VerifyDagAcyclicTask.kt      # применяет dagLevels среди boroughs
        └── model/
            └── GraphModel.kt            # GraphNode, GraphEdge, GraphCommunity, GraphModel
```

## Позиция N0

`graphify-gradle` находится на **N0** в DAG рабочего пространства. Он производит общий
артефакт `graph.json` (graphify-plugin публикуется на уровне N0, codebase-gradle — N1
и потребляет его). В `build.gradle.kts` **нет зависимостей от N0-контрактов
`workspace-bom` (MEMPHIS)** — graphify самодостаточен: Kotlin stdlib + Jackson.

## Ключевые зависимости

Из `gradle/libs.versions.toml`:

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`, `kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`, `jackson-dataformat-yaml` (поставляется как `api`)
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — утверждения тестов
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — логирование runtime тестов
- **Gradle TestKit** — интеграционные тесты
- **com.gradleup.gratatouille** 0.1.4 — плагин публикации (Plugin Portal)

Без koog, без langchain4j, без pgvector — graphify — чистый анализатор файловой системы.

## Задачи и расширение

Регистрируются `GraphifyPlugin` (`graphify.GraphifyPlugin`):

| Задача | Класс | Группа |
|------|-------|-------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask` (depends + finalizedBy) | collect |

Имя расширения: `graphify` (`GraphifyExtension`). Свойства:
`rootDir`, `outputFile`, `foundryDir`, `dagLevels`, `excludePatterns`.

Обе задачи scan/verify помечены `@DisableCachingByDefault` (зависят от FS / состояния
рабочего пространства); `collectFromWorkspace` дополнительно вызывает `doNotTrackState(...)`.

## Семантика сканирования (`ScanWorkspaceTask`)

- Обходит `rootDir` через `Files.walkFileTree`, пропуская любые поддеревья, совпадающие
  с glob-шаблонами `excludePatterns` или встроенными именами (`build`, `node_modules`,
  `.gradle`, `.git`, `.idea`, `target`).
- Обнаруживает проекты по маркерным файлам: `build.gradle.kts`, `build.gradle`,
  `pom.xml`, `package.json`.
- Выдаёт три типа рёбер:
  - `contains` — каталог → дочерний файл/каталог
  - `import` — импорты Kotlin `.kt`/`.kts`, разрешённые к каталогам родственных проектов
  - `reference` — `link:`/`include:`/`xref:`/`image:` AsciiDoc и пути в обратных кавычках
  - `agent_reference` — пути через слеш в `INDEX.adoc` (указатели агентов)
- Сообщества берутся из `buildRepoMap`: ближайший объемлющий каталог, содержащий
  каталог или файл `.git`, становится id сообщества.
- Вывод сериализуется через Jackson `ObjectMapper` (включение NON_NULL, pretty-print).

## Проверка DAG (`VerifyDagAcyclicTask`)

- Итерирует подкаталоги `foundryDir`, читает каждый `build.gradle.kts` и через regex
  извлекает импорты плагинов `id("…") version "…"`.
- Разрешает как имена проектов, так и зависимости против `dagLevels`, применяя варианты
  нормализации суффиксов (`-gradle`, `-plugin`, `_`↔`-`).
- Нарушение возникает, когда borough на уровне N импортирует плагин, чей N больше
  его собственного — сборка падает с `GradleException`.

## Матрица тестов

| Файл suite | Область |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | Unit — извлечение узлов/рёбер/сообществ |
| `VerifyDagAcyclicTask.kt`             | Unit — разрешение уровней DAG и отчёт о нарушениях |
| `ScanAndVerifyIntegrationTest.kt`     | Интеграция — цепочка `collectAndVerify` |
| `ScanWorkspaceIntegrationTest.kt`     | Интеграция — реальное сканирование рабочего пространства (Gradle TestKit) |
| `model/GraphModelTest.kt`             | Unit — data-классы и сериализация |

Итого: **58/58 PASS**. Платформа JUnit5, утверждения AssertJ, Gradle TestKit для
интеграционных suite.

## Настройка JVM

Специальные GC-флаги не требуются — graphify — краткосрочное пакетное сканирование.
Для очень больших рабочих пространств увеличьте кучу:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## Команды сборки

```bash
./gradlew build                      # полная сборка (компиляция + тесты)
./gradlew build -x test              # только компиляция
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # локальная публикация
```

## CI-конвейер

`.github/workflows/test.yml` выполняется на `ubuntu-latest`, **JDK 24** (Temurin),
таймаут 15 мин. Шаги:

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — интегрированная цепочка на живом рабочем пространстве

Примечание: CI собирает с JDK 24, хотя тулчейн нацелен на Java 23; любой из них
удовлетворяет требованию тулчейна Kotlin 2.2.20.

## Публикация

Артефакт плагина подписывается и публикуется в Maven Central через плагин
**`com.gradleup.gratatouille`** (`alias(libs.plugins.publish)`, версия 0.1.4 —
*не* `nmcp`).

Из `graphify-plugin/build.gradle.kts`:

- `group = "education.cccp"`, `version = "0.0.2"`
- `gradlePlugin { … }` объявляет плагин `graphify` (id `education.cccp.graphify`,
  impl `graphify.GraphifyPlugin`, теги `knowledge-graph`, `workspace`,
  `dependency-analysis`, `graphify`), сайт `https://cccp.education/`,
  vcs `https://github.com/cccp-education/graphify-gradle.git`.
- `java { withJavadocJar(); withSourcesJar() }`.
- `publishing.repositories { mavenCentral() }` — публикация напрямую в Central Portal.
- `signing { useGpgCmd() }` — подписывает каждую публикацию non-SNAPSHOT, non-CI.
- POM объявляет Apache 2.0, разработчик `cccp-education` (`cccp.edu@gmail.com`),
  SCM указывает на `github.com/cccp-education/graphify-gradle`.
- Опциональное свойство проекта `relocationGroup` внедряет блок
  `<distributionManagement>` `<relocation>` в XML POM (используется при необходимости
  перемещения группы).

Команда публикации (когда есть учётные данные + GPG):

```bash
./gradlew publishToMavenLocal                                 # локальная проверка
# релиз в Central Portal требует учётных данных портала в ~/.gradle/gradle.properties
```

## Документация по архитектуре

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPIC и управление (GF-0…GF-6, PUB)
- [BACKLOG.adoc](../BACKLOG.adoc) — невыполненные пункты
- [LICENSE](../LICENSE) — Apache 2.0

## Контрибьютинг

1. Сборка компилируется: `./gradlew build -x test`
2. Тесты зелёные: `./gradlew test` (58/58)
3. Держите плагин самодостаточным — без зависимостей LLM/RAG/db от N0-контрактов
4. Соблюдайте семантику `@DisableCachingByDefault` и `doNotTrackState` для scan/verify

## Лицензия

Apache License 2.0 — см. [LICENSE](../LICENSE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`.