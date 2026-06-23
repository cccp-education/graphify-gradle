<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — داخليات الإضافة

> دليل المطوّر والمساهم لإضافة Gradle `graphify-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **الإصدار**: `0.0.2` · **المجموعة**: `education.cccp` · **معرّف الإضافة**: `education.cccp.graphify`
- **سلسلة الأدوات**: Java 23 · Kotlin 2.2.20 · Gradle 9.x (wrapper)
- **البناء**: `./gradlew build -x test` · **الاختبارات**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **الحالة**: منتهٍ، خامل.

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## تخطيط الوحدات

```
graphify-gradle/
├── build.gradle.kts                     # يستهلك graphify-plugin نفسه (dogfooding)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # نقطة دخول الإضافة — تسجّل المهام
        ├── GraphifyExtension.kt         # DSL: rootDir, outputFile, foundryDir, dagLevels, excludePatterns
        ├── ScanWorkspaceTask.kt         # اجتياز FS → graph.json (عُقد، حواف، مجتمعات)
        ├── VerifyDagAcyclicTask.kt      # يفرض dagLevels عبر boroughs
        └── model/
            └── GraphModel.kt            # GraphNode, GraphEdge, GraphCommunity, GraphModel
```

## موقع N0

`graphify-gradle` يقع عند **N0** في DAG مساحة العمل. يُنتج الأثر المشترك
`graph.json` (تُنشر graphify-plugin عند مستوى N0، codebase-gradle هو N1 ويستهلكه). لا توجد
**تبعيات عقد N0 من `workspace-bom` (MEMPHIS)** مُعلنة في `build.gradle.kts` —
graphify مكتفٍ ذاتيًا: Kotlin stdlib + Jackson.

## التبعيات الرئيسية

من `gradle/libs.versions.toml`:

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`, `kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`, `jackson-dataformat-yaml` (مُحزَّم كـ `api`)
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — تأكيدات الاختبار
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — تسجيل وقت تشغيل الاختبار
- **Gradle TestKit** — اختبارات تكاملية
- **com.gradleup.gratatouille** 0.1.4 — إضافة النشر (Plugin Portal)

لا koog، لا langchain4j، لا pgvector — graphify هو محلِّل نقي لنظام الملفات.

## المهام والامتداد

مُسجّلة بواسطة `GraphifyPlugin` (`graphify.GraphifyPlugin`):

| المهمة | الصنف | المجموعة |
|------|-------|-------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask` (depends + finalizedBy) | collect |

اسم الامتداد: `graphify` (`GraphifyExtension`). الخصائص:
`rootDir`, `outputFile`, `foundryDir`, `dagLevels`, `excludePatterns`.

تحمل مهمتا scan/verify كلاهما `@DisableCachingByDefault` (تعتمدان على FS / حالة
مساحة العمل)؛ `collectFromWorkspace` يستدعي إضافيًا `doNotTrackState(...)`.

## دلالات المسح (`ScanWorkspaceTask`)

- يجتاز `rootDir` عبر `Files.walkFileTree`، متخطيًا أي شجرة فرعية تطابق
  glob لـ `excludePatterns` أو الأسماء المدمجة (`build`, `node_modules`, `.gradle`,
  `.git`, `.idea`, `target`).
- يكتشف المشاريع عبر ملفات وسم: `build.gradle.kts`, `build.gradle`,
  `pom.xml`, `package.json`.
- يُصدر ثلاثة أنواع من الحواف:
  - `contains` — دليل → ملف/دليل فرعي
  - `import` — استيرادات Kotlin ‎`.kt`/`.kts`‎ محلولة إلى أدلة مشاريع شقيقة
  - `reference` — ‎`link:`/`include:`/`xref:`/`image:`‎ في AsciiDoc ومسارات backtick
  - `agent_reference` — مسارات مفصولة بشرطة مائلة في `INDEX.adoc` (مؤشرات وكلاء)
- المجتمعات تأتي من `buildRepoMap`: أقرب دليل حاوي يحوي دليلًا أو ملف `.git`
  يصبح معرّف المجتمع.
- المخرجات تُسلسل عبر Jackson `ObjectMapper` (إدراج NON_NULL, pretty-print).

## التحقق من DAG (`VerifyDagAcyclicTask`)

- يكرر عبر الأدلة الفرعية لـ `foundryDir`، يقرأ كل `build.gradle.kts`، ويستخرج
  استيرادات الإضافات ‎`id("…") version "…"`‎ عبر regex.
- يحلّ أسماء المشروع والتبعية معًا مقابل `dagLevels`، مع تطبيق متغيرات تطبيع
  اللاحقة (`-gradle`, `-plugin`, ‎`_`↔`-`‎).
- يُرفع انتهاك حين يستورد borough في مستوى N إضافة قيمة N لها أكبر من مستواه —
  يفشل البناء بـ `GradleException`.

## مصفوفة الاختبارات

| ملف suite | النطاق |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | وحدة — استخراج عُقد/حواف/مجتمعات |
| `VerifyDagAcyclicTask.kt`             | وحدة — حل مستويات DAG والإبلاغ عن الانتهاكات |
| `ScanAndVerifyIntegrationTest.kt`     | تكامل — سلسلة `collectAndVerify` |
| `ScanWorkspaceIntegrationTest.kt`     | تكامل — مسح حقيقي لمساحة العمل (Gradle TestKit) |
| `model/GraphModelTest.kt`             | وحدة — data classes والتسلسل |

المجموع: **58/58 PASS**. منصة JUnit5، تأكيدات AssertJ، Gradle TestKit لمجموعات
التكامل.

## ضبط JVM

لا حاجة لأي علم GC خاص — graphify مسح دفعي قصير. لمساحات عمل كبيرة جدًا،
زد الـ heap:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## أوامر البناء

```bash
./gradlew build                      # بناء كامل (ترجمة + اختبارات)
./gradlew build -x test              # ترجمة فقط
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # نشر محلي
```

## خط أنابيب CI

يُشغّل ‎`.github/workflows/test.yml`‎ على `ubuntu-latest`, **JDK 24** (Temurin), انتهاء
15 دقيقة. الخطوات:

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — سلسلة متكاملة على مساحة العمل الحية

ملاحظة: CI يبني بـ JDK 24 رغم أن سلسلة الأدوات تستهدف Java 23؛ أيٌّ منهما يلبّي
متطلب سلسلة أدوات Kotlin 2.2.20.

## النشر

أثر الإضافة موقّع ويُنشَر على Maven Central عبر إضافة
**`com.gradleup.gratatouille`** (`alias(libs.plugins.publish)`, إصدار 0.1.4 —
*ليس* `nmcp`).

من `graphify-plugin/build.gradle.kts`:

- `group = "education.cccp"`, `version = "0.0.2"`
- `gradlePlugin { … }` يعرّف إضافة `graphify` (id `education.cccp.graphify`,
  impl `graphify.GraphifyPlugin`, tags `knowledge-graph`, `workspace`,
  `dependency-analysis`, `graphify`), موقع `https://cccp.education/`,
  vcs `https://github.com/cccp-education/graphify-gradle.git`.
- `java { withJavadocJar(); withSourcesJar() }`.
- `publishing.repositories { mavenCentral() }` — نشر مباشر إلى Central Portal.
- `signing { useGpgCmd() }` — يوقّع كل نشر غير-SNAPSHOT وغير-CI.
- POM يعرّف Apache 2.0, مطوّر `cccp-education` (`cccp.edu@gmail.com`),
  SCM يشير إلى `github.com/cccp-education/graphify-gradle`.
- خاصية المشروع الاختيارية `relocationGroup` تُحقن كتلة
  `<distributionManagement>` `<relocation>` في XML الـ POM (تُستخدم عند الحاجة
  إلى نقل المجموعة).

أمر النشر (حين تكون بيانات الاعتماد + GPG متاحة):

```bash
./gradlew publishToMavenLocal                                 # فحص محلي
# الإطلاق إلى Central Portal يتطلب بيانات اعتماد portal في ~/.gradle/gradle.properties
```

## وثائق البنية

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs وحوكمة (GF-0…GF-6, PUB)
- [BACKLOG.adoc](../BACKLOG.adoc) — بنود معلّقة
- [LICENSE](../LICENSE) — Apache 2.0

## المساهمة

1. البناء يترجم: `./gradlew build -x test`
2. الاختبارات خضراء: `./gradlew test` (58/58)
3. أبقِ الإضافة مكتفية ذاتيًا — دون تبعيات LLM/RAG/db على عقود N0
4. احترم دلالات `@DisableCachingByDefault` و`doNotTrackState` لـ scan/verify

## الترخيص

Apache License 2.0 — راجع [LICENSE](../LICENSE).

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`.