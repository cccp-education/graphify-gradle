<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — Руководство потребителя

> Плагин Gradle для извлечения графа знаний (узлы, рёбра, сообщества) в рабочем пространстве.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **Версия**: `0.0.2` · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.graphify`
- **Сборка**: `./gradlew build` · **Тесты**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **Статус**: Завершён, в спящем режиме — извлечённый граф питает нижестоящих потребителей.

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Что делает

`graphify-gradle` обходит файловую систему рабочего пространства, обнаруживает проекты
(Gradle/Maven/Node), файлы, каталоги и внутрипространственные отношения, затем выдаёт
стандартизованный `graph.json`, описывающий **узлы**, **рёбра** и **сообщества**. Сообщества
производятся от охватывающих Git-репозиториев (один предок `.git` = одно сообщество).

Выданный `graph.json` используется в экосистеме CCCP Education:

```
graphify-gradle (graph.json) → plantuml-gradle (диаграммы)
                             → bakery-gradle     (представление графа статического сайта)
                             → runner-gradle     (обзор рабочего пространства)
```

## Быстрый старт

### 1. Применить плагин

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. Настроить расширение

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
        // …другие boroughs…
    ))
}
```

### 3. Сгенерировать граф

```bash
./gradlew collectFromWorkspace      # обход FS → graph.json
./gradlew verifyDagAcyclic          # применить N0→N3 DAG-расслоение
./gradlew collectAndVerify          # цепочка: collectFromWorkspace + verifyDagAcyclic
```

Структура выходного `graph.json`:

```json
{
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

## Доступные задачи

| Задача | Группа | Описание |
|------|-------|-------------|
| `collectFromWorkspace` | collect   | Обойти рабочее пространство → выдать `graph.json` (узлы, рёбра, сообщества). |
| `verifyDagAcyclic`     | verify    | Применить объявленные `dagLevels` — проект не может зависеть от более высокого уровня N. |
| `collectAndVerify`     | collect   | Удобная цепочка: сначала `collectFromWorkspace`, затем `verifyDagAcyclic` (finalizedBy). |

## DSL расширения

```gradle
graphify {
    // Корень обхода файловой системы (ОБЯЗАТЕЛЬНО).
    rootDir.set(file("/home/cheroliv/workspace"))

    // Куда писать graph.json (ОБЯЗАТЕЛЬНО).
    outputFile.set(file("graph.json"))

    // Каталог с проектами borough — сканируется на нарушения уровней DAG.
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // Отображение имя проекта → уровень N (используется verifyDagAcyclic).
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // Glob-шаблоны исключения (опционально — значения по умолчанию ниже).
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))
}
```

## Предварительные требования

- **Java** 23+ (тулчейн Kotlin 2.2.20)
- **Gradle** 9.x (wrapper прилагается)
- Доступ на чтение к сканируемому рабочему пространству

## Сборка и тесты

```bash
./gradlew build                      # полная сборка (компиляция + тесты)
./gradlew build -x test              # только компиляция
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # локальная публикация
```

## Устранение неполадок

| Симптом | Решение |
|---------|-----|
| `graph.json` пуст              | Убедитесь, что `rootDir` разрешим и читаем; проверьте `excludePatterns`. |
| `DAG VIOLATIONS DETECTED`       | Borough импортирует плагин более высокого уровня N — настройте `dagLevels` или импорт. |
| Медленное сканирование больших монорепозиториев | Сузьте `rootDir`, расширьте `excludePatterns`, избегайте сканирования нерелевантных поддеревьев. |
| Предупреждения о недоступных путях | Безопасны — обходчик пропускает нечитаемые пути и продолжает с частичными результатами. |

## Лицензия

Apache License 2.0 — см. [LICENSE](../LICENSE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`.