<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — 用户指南

> 用于跨工作区提取知识图谱（节点、边、社区）的 Gradle 插件。

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **版本**：`0.0.2` · **Group**：`education.cccp` · **插件 ID**：`education.cccp.graphify`
- **构建**：`./gradlew build` · **测试**：`./gradlew test`（JUnit5 — 58/58 PASS）
- **状态**：已结项，休眠中 —— 提取的图谱为下游消费者提供支撑。

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 功能简介

`graphify-gradle` 遍历工作区文件系统，检测项目（Gradle/Maven/Node）、文件、目录以及工作区内部关系，然后输出一个标准化的 `graph.json`，描述**节点**、**边**和**社区**。社区由所归属的 Git 仓库推导而来（一个 `.git` 祖先 = 一个社区）。

输出的 `graph.json` 在 CCCP Education 生态系统中被以下项目消费：

```
graphify-gradle (graph.json) → plantuml-gradle (图表)
                             → bakery-gradle     (静态站点图谱视图)
                             → runner-gradle     (工作区巡览)
```

## 快速开始

### 1. 应用插件

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. 配置扩展

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
        // …其他 boroughs…
    ))
}
```

### 3. 生成图谱

```bash
./gradlew collectFromWorkspace      # 扫描文件系统 → graph.json
./gradlew verifyDagAcyclic          # 强制 N0→N3 DAG 分层
./gradlew collectAndVerify          # 链式：collectFromWorkspace + verifyDagAcyclic
```

输出的 `graph.json` 结构：

```json
{
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

## 可用任务

| 任务 | 组 | 说明 |
|------|-------|-------------|
| `collectFromWorkspace` | collect   | 遍历工作区 → 输出 `graph.json`（节点、边、社区）。 |
| `verifyDagAcyclic`     | verify    | 强制执行已声明的 `dagLevels` —— 一个项目不得依赖更高 N 层级。 |
| `collectAndVerify`     | collect   | 便捷链：先 `collectFromWorkspace` 再 `verifyDagAcyclic`（finalizedBy）。 |

## 扩展 DSL

```gradle
graphify {
    // 文件系统遍历的根目录（必填）。
    rootDir.set(file("/home/cheroliv/workspace"))

    // graph.json 的写入位置（必填）。
    outputFile.set(file("graph.json"))

    // 包含 borough 项目的目录 —— 用于扫描 DAG 层级违规。
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // 项目名 → N 层级的映射（由 verifyDagAcyclic 使用）。
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // glob 排除模式（可选 —— 以下为默认值）。
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))
}
```

## 前置条件

- **Java** 23+（Kotlin 2.2.20 工具链）
- **Gradle** 9.x（已提供 wrapper）
- 对待扫描的工作区具备读取权限

## 构建与测试

```bash
./gradlew build                      # 完整构建（编译 + 测试）
./gradlew build -x test              # 仅编译
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # 本地发布
```

## 故障排查

| 症状 | 解决方法 |
|---------|-----|
| `graph.json` 为空              | 确认 `rootDir` 可解析且可读；检查 `excludePatterns`。 |
| `DAG VIOLATIONS DETECTED`       | 某个 borough 导入了更高 N 层级的插件 —— 调整 `dagLevels` 或该导入。 |
| 在大型 monorepo 上扫描缓慢    | 缩小 `rootDir`，扩展 `excludePatterns`，避免扫描无关子树。 |
| 不可访问路径警告              | 无害 —— 遍历器会跳过不可读路径并以部分结果继续。 |

## 许可证

Apache License 2.0 —— 详见 [LICENSE](../LICENSE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。