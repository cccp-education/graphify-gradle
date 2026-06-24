<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — پلگ ان کی اندرونیات

> Gradle پلگ ان `graphify-plugin` کے لیے ڈویلپر اور معاون رہنمائی۔

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **ورژن**: `0.0.2` · **گروپ**: `education.cccp` · **پلگ ان ID**: `education.cccp.graphify`
- **ٹول چین**: Java 23 · Kotlin 2.2.20 · Gradle 9.x (wrapper)
- **بلڈ**: `./gradlew build -x test` · **ٹیسٹ**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **حالت**: مکمل، غیر فعال۔

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## ماڈیول ترتیب

```
graphify-gradle/
├── build.gradle.kts                     # خود graphify-plugin استعمال کرتا ہے (dogfooding)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # پلگ ان داخلہ نقطہ — ٹاسکس رجسٹر کرتا ہے
        ├── GraphifyExtension.kt         # DSL: rootDir, outputFile, foundryDir, dagLevels, excludePatterns
        ├── ScanWorkspaceTask.kt         # FS چلنا → graph.json (نوڈز، ایجز، کمیونٹیز)
        ├── VerifyDagAcyclicTask.kt      # boroughs میں dagLevels لاگو کرتا ہے
        └── model/
            └── GraphModel.kt            # GraphNode, GraphEdge, GraphCommunity, GraphModel
```

## N0 مقام

`graphify-gradle` ورک اسپیس DAG میں **N0** پر واقع ہے۔ یہ اشتراک کردہ `graph.json`
آرٹیفیکٹ پیدا کرتا ہے (graphify-plugin N0 سطح پر شائع ہوتا ہے، codebase-gradle N1 ہے
اور اسے استعمال کرتا ہے)۔ `build.gradle.kts` میں **کوئی `workspace-bom` (MEMPHIS)
N0 معاہدہ_dependencies** مُعلَن نہیں — graphify خودکفیل ہے: Kotlin stdlib + Jackson۔

## اہم dependencies

`gradle/libs.versions.toml` سے:

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`, `kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`, `jackson-dataformat-yaml` (`api` کے طور پر بندل)
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — ٹیسٹ دعوے
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — ٹیسٹ runtime لاگنگ
- **Gradle TestKit** — انضمام ٹیسٹ
- **com.gradleup.gratatouille** 0.1.4 — اشاعت پلگ ان (Plugin Portal)

کوئی koog، کوئی langchain4j، کوئی pgvector نہیں — graphify ایک خالص فائل سسٹم تجزیہ کار ہے۔

## ٹاسکس اور ایکسٹینشن

`GraphifyPlugin` (`graphify.GraphifyPlugin`) کے ذریعے رجسٹرڈ:

| ٹاسک | کلاس | گروپ |
|------|-------|-------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask` (depends + finalizedBy) | collect |

ایکسٹینشن نام: `graphify` (`GraphifyExtension`)۔ خصوصیات:
`rootDir`, `outputFile`, `foundryDir`, `dagLevels`, `excludePatterns`۔

دونوں scan/verify ٹاسکس پر `@DisableCachingByDefault` ہے (FS / ورک اسپیس حالت پر
منحصر); `collectFromWorkspace` مزید `doNotTrackState(...)` کال کرتا ہے۔

## سکین سیمنٹکس (`ScanWorkspaceTask`)

- `rootDir` کو `Files.walkFileTree` کے ساتھ چلتا ہے، کسی بھی ذیلی درخت کو چھوڑ دیتا ہے جو
  `excludePatterns` glob یا بلٹ-ان ناموں (`build`, `node_modules`, `.gradle`,
  `.git`, `.idea`, `target`) سے ملتا ہو۔
- مارکر فائلوں کے ذریعے پروجیکٹس کا پتہ لگاتا ہے: `build.gradle.kts`, `build.gradle`,
  `pom.xml`, `package.json`۔
- تین ایج اقسام خارج کرتا ہے:
  - `contains` — ڈائریکٹری → چائلڈ فائل/ڈائریکٹری
  - `import` — Kotlin ‎`.kt`/`.kts`‎ imports جو سبلنگ پروجیکٹ ڈائریکٹریز میں حل ہوتے ہیں
  - `reference` — AsciiDoc ‎`link:`/`include:`/`xref:`/`image:`‎ اور backtick راستے
  - `agent_reference` — `INDEX.adoc` میں سلیش سے الگ شدہ راستے (ایجنٹ پوائنٹرز)
- کمیونٹیز `buildRepoMap` سے آتی ہیں: `.git` ڈائریکٹری یا فائل رکھنے والی قریب ترین
  احاطہ بند ڈائریکٹری کمیونٹی id بن جاتی ہے۔
- آؤٹ پُٹ Jackson `ObjectMapper` سے سیریلائز (NON_NULL inclusion, pretty-print)۔

## DAG تصدیق (`VerifyDagAcyclicTask`)

- `foundryDir` کی ذیلی ڈائریکٹریز پر اعادہ، ہر `build.gradle.kts` پڑھتا ہے، اور regex
  کے ذریعے `id("…") version "…"` پلگ ان imports نکالتا ہے۔
- projیکٹ اور dependency دونوں نام `dagLevels` کے خلاف حل کرتا ہے، suffix normalization
  variants (`-gradle`, `-plugin`, ‎`_`↔`-`‎) لاگو کرتے ہوئے۔
- جب N سطح کا borough ایسا پلگ ان import کرتا ہے جس کا N اس سے بڑا ہو تو خلاف ورزی اٹھائی
  جاتی ہے — `GradleException` کے ساتھ بلڈ ناکام۔

## ٹیسٹ میٹرکس

| suite فائل | دائرہ |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | یونٹ — نوڈ/ایج/کمیونٹی نکالنا |
| `VerifyDagAcyclicTask.kt`             | یونٹ — DAG سطح حل اور خلاف ورزی رپورٹنگ |
| `ScanAndVerifyIntegrationTest.kt`     | انضمام — چینڈ `collectAndVerify` |
| `ScanWorkspaceIntegrationTest.kt`     | انضمام — حقیقی ورک اسپیس سکین (Gradle TestKit) |
| `model/GraphModelTest.kt`             | یونٹ — data classes اور سیریلائزیشن |

کل: **58/58 PASS**۔ JUnit5 پلیٹ فارم، AssertJ دعوے، انضمام suites کے لیے
Gradle TestKit۔

## JVM ٹیوننگ

کوئی خاص GC flags ضروری نہیں — graphify ایک مختصر batch سکین ہے۔ بہت بڑے
ورک اسپیسز کے لیے، heap بڑھائیں:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## بلڈ کمانڈز

```bash
./gradlew build                      # مکمل بلڈ (کمپائل + ٹیسٹ)
./gradlew build -x test              # صرف کمپائل
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # مقامی اشاعت
```

## CI پائپ لائن

`.github/workflows/test.yml` `ubuntu-latest` پر چلتا ہے، **JDK 24** (Temurin), 15 منٹ
timeout۔ مراحل:

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — لائیو ورک اسپیس پر مربوط چین

نوٹ: CI JDK 24 سے بلڈ کرتا ہے حالانکہ ٹول چین Java 23 کو نشانہ بناتا ہے؛ دونوں میں سے کوئی بھی
Kotlin 2.2.20 ٹول چین ضرورت پوری کرتا ہے۔

## اشاعت

پلگ ان آرٹیفیکٹ **`com.gradleup.gratatouille`** پلگ ان
(`alias(libs.plugins.publish)`, ورژن 0.1.4 — *نہیں* `nmcp`) کے ذریعے دستخط شدہ اور
Maven Central پر شائع ہوتا ہے۔

`graphify-plugin/build.gradle.kts` سے:

- `group = "education.cccp"`, `version = "0.0.2"`
- `gradlePlugin { … }` `graphify` پلگ ان مُعلَن کرتا ہے (id `education.cccp.graphify`,
  impl `graphify.GraphifyPlugin`, tags `knowledge-graph`, `workspace`,
  `dependency-analysis`, `graphify`), ویب سائٹ `https://cccp.education/`,
  vcs `https://github.com/cccp-education/graphify-gradle.git`۔
- `java { withJavadocJar(); withSourcesJar() }`۔
- `publishing.repositories { mavenCentral() }` — براہ راست Central Portal پر اشاعت۔
- `signing { useGpgCmd() }` — ہر non-SNAPSHOT، non-CI اشاعت پر دستخط۔
- POM Apache 2.0، ڈویلپر `cccp-education` (`cccp.edu@gmail.com`),
  `github.com/cccp-education/graphify-gradle` کی طرف SCM مُعلَن کرتا ہے۔
- اختیاری `relocationGroup` projیکٹ خاصیت POM XML میں `<distributionManagement>`
  `<relocation>` بلاک داخل کرتی ہے (گروپ منتقلی ضروری ہونے پر استعمال)۔

اشاعت کمانڈ (جب credentials + GPG دستیاب ہوں):

```bash
./gradlew publishToMavenLocal                                 # مقامی جانچ
# Central Portal پر ریلیز کے لیے ~/.gradle/gradle.properties میں portal credentials درکار
```

## فن تعمیر دستاویزات

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs اور گورننس (GF-0…GF-6, PUB)
- [BACKLOG.adoc](../BACKLOG.adoc) — زیر التواء اشیاء
- [LICENSE](../LICENSE) — Apache 2.0

## شراکت

1. بلڈ کمپائل ہوتا ہے: `./gradlew build -x test`
2. ٹیسٹ سبز: `./gradlew test` (58/58)
3. پلگ ان کو خودکفیل رکھیں — N0 معاہدوں پر کوئی LLM/RAG/db dependencies نہیں
4. scan/verify کے لیے `@DisableCachingByDefault` اور `doNotTrackState` سیمنٹکس کا احترام کریں

## لائسنس

Apache License 2.0 — [LICENSE](../LICENSE) دیکھیں۔

---

_CCCP Education ماحولیاتی نظام کا حصہ — `groupId: education.cccp`۔