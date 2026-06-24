<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — প্লাগইন অভ্যন্তরীণ

> `graphify-plugin` Gradle প্লাগইনের জন্য ডেভেলপার ও অবদানকারী গাইড।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **সংস্করণ**: `0.0.2` · **গ্রুপ**: `education.cccp` · **প্লাগইন আইডি**: `education.cccp.graphify`
- **টুলচেইন**: Java 23 · Kotlin 2.2.20 · Gradle 9.x (wrapper)
- **বিল্ড**: `./gradlew build -x test` · **পরীক্ষা**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **অবস্থা**: সমাপ্ত, নিষ্ক্রিয়।

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## মডিউল বিন্যাস

```
graphify-gradle/
├── build.gradle.kts                     # নিজেই graphify-plugin ব্যবহার করে (dogfooding)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # প্লাগইন এন্ট্রি পয়েন্ট — টাস্ক নিবন্ধন করে
        ├── GraphifyExtension.kt         # DSL: rootDir, outputFile, foundryDir, dagLevels, excludePatterns
        ├── ScanWorkspaceTask.kt         # FS পার করা → graph.json (নোড, এজ, কমিউনিটি)
        ├── VerifyDagAcyclicTask.kt      # boroughs-এ dagLevels প্রয়োগ করে
        └── model/
            └── GraphModel.kt            # GraphNode, GraphEdge, GraphCommunity, GraphModel
```

## N0 অবস্থান

`graphify-gradle` ওয়ার্কস্পেস DAG-এ **N0**-তে অবস্থিত। এটি শেয়ার্ড `graph.json` আর্টিফ্যাক্ট
উৎপন্ন করে (graphify-plugin N0 স্তরে প্রকাশিত, codebase-gradle হলো N1 এবং এটি ব্যবহার করে)।
`build.gradle.kts`-এ **কোনো `workspace-bom` (MEMPHIS) N0 চুক্তি নির্ভরতা** ঘোষণা করা নেই —
graphify স্বয়ংসম্পূর্ণ: Kotlin stdlib + Jackson।

## মূল নির্ভরতাসমূহ

`gradle/libs.versions.toml` থেকে:

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`, `kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`, `jackson-dataformat-yaml` (`api` হিসেবে বান্ডেল)
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — পরীক্ষার অভিকথন
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — পরীক্ষা রানটাইম লগিং
- **Gradle TestKit** — ইন্টিগ্রেশন পরীক্ষা
- **com.gradleup.gratatouille** 0.1.4 — প্রকাশনা প্লাগইন (Plugin Portal)

কোনো koog, কোনো langchain4j, কোনো pgvector নেই — graphify একটি শুদ্ধ ফাইলসিস্টেম বিশ্লেষক।

## টাস্ক ও এক্সটেনশন

`GraphifyPlugin` (`graphify.GraphifyPlugin`) দ্বারা নিবন্ধিত:

| টাস্ক | ক্লাস | গ্রুপ |
|------|-------|-------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask` (depends + finalizedBy) | collect |

এক্সটেনশন নাম: `graphify` (`GraphifyExtension`)। বৈশিষ্ট্য:
`rootDir`, `outputFile`, `foundryDir`, `dagLevels`, `excludePatterns`।

দুটি scan/verify টাস্কই `@DisableCachingByDefault` বহন করে (FS / ওয়ার্কস্পেস-স্টেট
নির্ভর); `collectFromWorkspace` অতিরিক্তভাবে `doNotTrackState(...)` কল করে।

## স্ক্যান সিম্যান্টিক্স (`ScanWorkspaceTask`)

- `rootDir` কে `Files.walkFileTree` দিয়ে পার করে, `excludePatterns` glob বা বিল্ট-ইন নামের
  (`build`, `node_modules`, `.gradle`, `.git`, `.idea`, `target`) সাথে মিল এমন
  যেকোনো সাবট্রি এড়িয়ে যায়।
- মার্কার ফাইলের মাধ্যমে প্রজেক্ট সনাক্ত করে: `build.gradle.kts`, `build.gradle`,
  `pom.xml`, `package.json`।
- তিন ধরনের এজ নির্গত করে:
  - `contains` — ডাইরেক্টরি → চাইল্ড ফাইল/ডাইরেক্টরি
  - `import` — Kotlin ‎`.kt`/`.kts`‎ import যা সিবলিং প্রজেক্ট ডাইরেক্টরিতে সমাধান করা হয়
  - `reference` — AsciiDoc ‎`link:`/`include:`/`xref:`/`image:`‎ এবং backtick পথ
  - `agent_reference` — `INDEX.adoc` স্ল্যাশ-বিভক্ত পথ (এজেন্ট পয়েন্টার)
- কমিউনিটি `buildRepoMap` থেকে আসে: `.git` ডাইরেক্টরি বা ফাইল ধারণকারী নিকটতম আবদ্ধ
  ডাইরেক্টরি কমিউনিটি id হয়ে যায়।
- আউটপুট Jackson `ObjectMapper` দ্বারা সিরিয়ালাইজ করা (NON_NULL inclusion, pretty-print)।

## DAG যাচাই (`VerifyDagAcyclicTask`)

- `foundryDir`-এর সাব-ডাইরেক্টরি পুনরাবৃত্তি করে, প্রতিটি `build.gradle.kts` পড়ে, এবং regex
  দ্বারা `id("…") version "…"` প্লাগইন import বের করে।
- প্রজেক্ট এবং নির্ভরতা নাম উভয়ই `dagLevels`-এর বিপরীতে সমাধান করে, সাফিক্স নর্মালাইজেশন
  ভ্যারিয়েন্ট (`-gradle`, `-plugin`, ‎`_`↔`-`‎) প্রয়োগ করে।
- N স্তরের borough যখন এমন প্লাগইন import করে যার N তার নিজের চেয়ে বড় তখন একটি লঙ্ঘন ওঠে —
  `GradleException` দিয়ে বিল্ড ব্যর্থ।

## পরীক্ষা ম্যাট্রিক্স

| suite ফাইল | পরিসর |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | ইউনিট — নোড/এজ/কমিউনিটি নিষ্কাশন |
| `VerifyDagAcyclicTask.kt`             | ইউনিট — DAG স্তর রিজ়লিউশন ও লঙ্ঘন রিপোর্টিং |
| `ScanAndVerifyIntegrationTest.kt`     | ইন্টিগ্রেশন — চেইনড `collectAndVerify` |
| `ScanWorkspaceIntegrationTest.kt`     | ইন্টিগ্রেশন — প্রকৃত-ওয়ার্কস্পেস স্ক্যান (Gradle TestKit) |
| `model/GraphModelTest.kt`             | ইউনিট — data class ও সিরিয়ালাইজেশন |

মোট: **58/58 PASS**। JUnit5 প্ল্যাটফর্ম, AssertJ অভিকথন, ইন্টিগ্রেশন suite-এর জন্য
Gradle TestKit।

## JVM টিউনিং

কোনো বিশেষ GC ফ্ল্যাগের প্রয়োজন নেই — graphify একটি স্বল্পকালীন ব্যাচ স্ক্যান। খুব বড়
ওয়ার্কস্পেসের জন্য, heap বাড়ান:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## বিল্ড কমান্ড

```bash
./gradlew build                      # সম্পূর্ণ বিল্ড (কম্পাইল + পরীক্ষা)
./gradlew build -x test              # শুধু কম্পাইল
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # স্থানীয় প্রকাশনা
```

## CI পাইপলাইন

`.github/workflows/test.yml` `ubuntu-latest`-এ চলে, **JDK 24** (Temurin), 15 মিনিট
timeout। ধাপ:

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — লাইভ ওয়ার্কস্পেসে ইন্টিগ্রেটেড চেইন

নোট: CI JDK 24 দিয়ে বিল্ড করে যদিও টুলচেইন Java 23 টার্গেট করে; যেকোনো একটি
Kotlin 2.2.20 টুলচেইন প্রয়োজন পূরণ করে।

## প্রকাশনা

প্লাগইন আর্টিফ্যাক্ট স্বাক্ষরিত ও Maven Central-এ **`com.gradleup.gratatouille`**
প্লাগইন (`alias(libs.plugins.publish)`, সংস্করণ 0.1.4 — *নয়* `nmcp`) দ্বারা প্রকাশিত।

`graphify-plugin/build.gradle.kts` থেকে:

- `group = "education.cccp"`, `version = "0.0.2"`
- `gradlePlugin { … }` `graphify` প্লাগইন ঘোষণা করে (id `education.cccp.graphify`,
  impl `graphify.GraphifyPlugin`, tags `knowledge-graph`, `workspace`,
  `dependency-analysis`, `graphify`), ওয়েবসাইট `https://cccp.education/`,
  vcs `https://github.com/cccp-education/graphify-gradle.git`।
- `java { withJavadocJar(); withSourcesJar() }`।
- `publishing.repositories { mavenCentral() }` — সরাসরি Central Portal-এ প্রকাশ।
- `signing { useGpgCmd() }` — প্রতিটি non-SNAPSHOT, non-CI প্রকাশনায় স্বাক্ষর।
- POM Apache 2.0, ডেভেলপার `cccp-education` (`cccp.edu@gmail.com`),
  `github.com/cccp-education/graphify-gradle`-কে নির্দেশকারী SCM ঘোষণা করে।
- ঐচ্ছিক `relocationGroup` প্রজেক্ট বৈশিষ্ট্য POM XML-এ `<distributionManagement>`
  `<relocation>` ব্লক ইনজেক্ট করে (গ্রুপ পুনর্বিন্যাস প্রয়োজন হলে ব্যবহৃত)।

প্রকাশনা কমান্ড (যখন ক্রেডেনশিয়াল + GPG উপলব্ধ):

```bash
./gradlew publishToMavenLocal                                 # স্থানীয় স্যানিটি
# Central Portal-এ রিলিজের জন্য ~/.gradle/gradle.properties-এ portal ক্রেডেনশিয়াল প্রয়োজন
```

## আর্কিটেকচার ডকুমেন্টেশন

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs ও গভর্ন্যান্স (GF-0…GF-6, PUB)
- [BACKLOG.adoc](../BACKLOG.adoc) — বকেয়া আইটেম
- [LICENSE](../LICENSE) — Apache 2.0

## অবদান

1. বিল্ড কম্পাইল হয়: `./gradlew build -x test`
2. পরীক্ষা সবুজ: `./gradlew test` (58/58)
3. প্লাগইন স্বয়ংসম্পূর্ণ রাখুন — N0 চুক্তিতে কোনো LLM/RAG/db নির্ভরতা নেই
4. scan/verify-এর জন্য `@DisableCachingByDefault` এবং `doNotTrackState` সিম্যান্টিক্স মেনে চলুন

## লাইসেন্স

Apache License 2.0 — [LICENSE](../LICENSE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।