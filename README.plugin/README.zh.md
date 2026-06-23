<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — 插件内部机制

> `graphify-plugin` Gradle 插件的开发者与贡献者指南。

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **版本**：`0.0.2` · **Group**：`education.cccp` · **插件 ID**：`education.cccp.graphify`
- **工具链**：Java 23 · Kotlin 2.2.20 · Gradle 9.x（wrapper）
- **构建**：`./gradlew build -x test` · **测试**：`./gradlew test`（JUnit5 — 58/58 PASS）
- **状态**：已结项，休眠中。

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 模块布局

```
graphify-gradle/
├── build.gradle.kts                     # 自身消费 graphify-plugin（dogfooding）
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # 插件入口点 —— 注册任务
        ├── GraphifyExtension.kt         # DSL：rootDir、outputFile、foundryDir、dagLevels、excludePatterns
        ├── ScanWorkspaceTask.kt         # 文件系统遍历 → graph.json（节点、边、社区）
        ├── VerifyDagAcyclicTask.kt      # 在 borough 间强制执行 dagLevels
        └── model/
            └── GraphModel.kt            # GraphNode、GraphEdge、GraphCommunity、GraphModel
```

## N0 位置

`graphify-gradle` 位于工作区 DAG 的 **N0** 层。它产出共享的 `graph.json` 制品
（graphify-plugin 发布于 N0 层，codebase-gradle 为 N1 并消费它）。`build.gradle.kts` 中
**没有声明任何 `workspace-bom`（MEMPHIS）的 N0 契约依赖** —— graphify 是自包含的：
Kotlin stdlib + Jackson。

## 关键依赖

来自 `gradle/libs.versions.toml`：

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`、`kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`、`jackson-dataformat-yaml`（以 `api` 打包）
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — 测试断言
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — 测试运行时日志
- **Gradle TestKit** — 集成测试
- **com.gradleup.gratatouille** 0.1.4 — 发布插件（Plugin Portal）

无 koog、无 langchain4j、无 pgvector —— graphify 是纯粹的文件系统分析器。

## 任务与扩展

由 `GraphifyPlugin`（`graphify.GraphifyPlugin`）注册：

| 任务 | 类 | 组 |
|------|-------|-------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask`（depends + finalizedBy） | collect |

扩展名：`graphify`（`GraphifyExtension`）。属性：
`rootDir`、`outputFile`、`foundryDir`、`dagLevels`、`excludePatterns`。

两个 scan/verify 任务都标注了 `@DisableCachingByDefault`（依赖文件系统 / 工作区状态）；
`collectFromWorkspace` 额外调用了 `doNotTrackState(...)`。

## 扫描语义（`ScanWorkspaceTask`）

- 使用 `Files.walkFileTree` 遍历 `rootDir`，跳过任何匹配
  `excludePatterns` glob 或内置名称（`build`、`node_modules`、`.gradle`、
  `.git`、`.idea`、`target`）的子树。
- 通过标记文件检测项目：`build.gradle.kts`、`build.gradle`、`pom.xml`、`package.json`。
- 输出三种边类型：
  - `contains` —— 目录 → 子文件/子目录
  - `import` —— Kotlin `.kt`/`.kts` import 解析到同级项目目录
  - `reference` —— AsciiDoc `link:`/`include:`/`xref:`/`image:` 及反引号路径
  - `agent_reference` —— `INDEX.adoc` 中斜杠分隔的路径（agent 指针）
- 社区来源于 `buildRepoMap`：最近的包含 `.git` 目录或文件的祖先目录成为社区 id。
- 输出由 Jackson `ObjectMapper` 序列化（NON_NULL 包含，pretty-print）。

## DAG 验证（`VerifyDagAcyclicTask`）

- 遍历 `foundryDir` 的子目录，读取每个 `build.gradle.kts`，通过正则提取
  `id("…") version "…"` 插件导入。
- 同时解析项目名和依赖名对照 `dagLevels`，应用后缀规范化变体
  （`-gradle`、`-plugin`、`_`↔`-`）。
- 当处于 N 层的 borough 导入了一个 N 值大于自身的插件时即触发违规 ——
  以 `GradleException` 使构建失败。

## 测试矩阵

| 测试套件文件 | 范围 |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | 单元 —— 节点/边/社区提取 |
| `VerifyDagAcyclicTask.kt`             | 单元 —— DAG 层级解析与违规报告 |
| `ScanAndVerifyIntegrationTest.kt`     | 集成 —— 链式 `collectAndVerify` |
| `ScanWorkspaceIntegrationTest.kt`     | 集成 —— 真实工作区扫描（Gradle TestKit） |
| `model/GraphModelTest.kt`             | 单元 —— 数据类与序列化 |

总计：**58/58 PASS**。JUnit5 平台、AssertJ 断言，Gradle TestKit 用于集成套件。

## JVM 调优

无需特殊 GC 标志 —— graphify 是一个短时批处理扫描。对于非常大的工作区，增加堆：

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## 构建命令

```bash
./gradlew build                      # 完整构建（编译 + 测试）
./gradlew build -x test              # 仅编译
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # 本地发布
```

## CI 流水线

`.github/workflows/test.yml` 运行于 `ubuntu-latest`，**JDK 24**（Temurin），超时 15 分钟。步骤：

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — 在活动工作区上的集成链

注意：CI 使用 JDK 24 构建，尽管工具链目标为 Java 23；两者均满足
Kotlin 2.2.20 工具链要求。

## 发布

插件制品通过 **`com.gradleup.gratatouille`** 插件（`alias(libs.plugins.publish)`，
版本 0.1.4 —— *不是* `nmcp`）签名并发布到 Maven Central。

来自 `graphify-plugin/build.gradle.kts`：

- `group = "education.cccp"`、`version = "0.0.2"`
- `gradlePlugin { … }` 声明 `graphify` 插件（id `education.cccp.graphify`、
  实现 `graphify.GraphifyPlugin`、标签 `knowledge-graph`、`workspace`、
  `dependency-analysis`、`graphify`），网站 `https://cccp.education/`、
  vcs `https://github.com/cccp-education/graphify-gradle.git`。
- `java { withJavadocJar(); withSourcesJar() }`。
- `publishing.repositories { mavenCentral() }` —— 直接发布到 Central Portal。
- `signing { useGpgCmd() }` —— 为每个非 SNAPSHOT、非 CI 发布签名。
- POM 声明 Apache 2.0、开发者 `cccp-education`（`cccp.edu@gmail.com`）、
  指向 `github.com/cccp-education/graphify-gradle` 的 SCM。
- 可选的 `relocationGroup` 项目属性向 POM XML 注入 `<distributionManagement>`
  `<relocation>` 块（用于需要组迁移的场景）。

发布命令（凭据 + GPG 可用时）：

```bash
./gradlew publishToMavenLocal                                 # 本地健全性检查
# 发布到 Central Portal 需在 ~/.gradle/gradle.properties 中配置 portal 凭据
```

## 架构文档

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPIC 与治理（GF-0…GF-6、PUB）
- [BACKLOG.adoc](../BACKLOG.adoc) — 待办事项
- [LICENSE](../LICENSE) — Apache 2.0

## 贡献

1. 构建通过编译：`./gradlew build -x test`
2. 测试通过：`./gradlew test`（58/58）
3. 保持插件自包含 —— 对 N0 契约无 LLM/RAG/db 依赖
4. 遵守 scan/verify 的 `@DisableCachingByDefault` 和 `doNotTrackState` 语义

## 许可证

Apache License 2.0 —— 详见 [LICENSE](../LICENSE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。