<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — प्लगइन आंतरिक

> `graphify-plugin` Gradle प्लगइन के लिए डेवलपर और योगदानकर्ता गाइड।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=tests&message=58%2F58%20PASS&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **संस्करण**: `0.0.2` · **समूह**: `education.cccp` · **प्लगइन आईडी**: `education.cccp.graphify`
- **टूलचेन**: Java 23 · Kotlin 2.2.20 · Gradle 9.x (wrapper)
- **बिल्ड**: `./gradlew build -x test` · **परीक्षण**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **स्थिति**: पूर्ण, निष्क्रिय।

🌐 Languages: [English](README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## मॉड्यूल लेआउट

```
graphify-gradle/
├── build.gradle.kts                     # स्वयं graphify-plugin का उपभोग (dogfooding)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── .github/workflows/test.yml
└── graphify-plugin/
    └── src/main/kotlin/graphify/
        ├── GraphifyPlugin.kt            # प्लगइन प्रवेश बिंदु — कार्य पंजीकृत करता है
        ├── GraphifyExtension.kt         # DSL: rootDir, outputFile, foundryDir, dagLevels, excludePatterns
        ├── ScanWorkspaceTask.kt         # FS चलना → graph.json (नोड्स, एजेज़, कम्युनिटीज़)
        ├── VerifyDagAcyclicTask.kt      # boroughs में dagLevels लागू करता है
        └── model/
            └── GraphModel.kt            # GraphNode, GraphEdge, GraphCommunity, GraphModel
```

## N0 स्थिति

`graphify-gradle` वर्कस्पेस DAG में **N0** पर स्थित है। यह साझा `graph.json` आर्टिफैक्ट
उत्पन्न करता है (graphify-plugin N0 स्तर पर प्रकाशित, codebase-gradle N1 है और इसे उपभोग करता है)।
`build.gradle.kts` में **कोई `workspace-bom` (MEMPHIS) N0 अनुबंध निर्भरताएँ** घोषित नहीं हैं
— graphify स्व-निहित है: Kotlin stdlib + Jackson।

## प्रमुख निर्भरताएँ

`gradle/libs.versions.toml` से:

- **Kotlin** 2.2.20 — `org.jetbrains.kotlin.jvm`, `kotlin-stdlib-jdk8`
- **Jackson** 2.18.3 — `jackson-module-kotlin`, `jackson-dataformat-yaml` (`api` के रूप में बंडल)
- **JUnit Jupiter** 5.10.1
- **AssertJ** 3.25.3 — परीक्षण अभिकथन
- **SLF4J** 2.0.17 / **Logback** 1.5.26 — परीक्षण रनटाइम लॉगिंग
- **Gradle TestKit** — एकीकरण परीक्षण
- **com.gradleup.gratatouille** 0.1.4 — प्रकाशन प्लगइन (Plugin Portal)

कोई koog, कोई langchain4j, कोई pgvector नहीं — graphify एक शुद्ध फाइलसिस्टम विश्लेषक है।

## कार्य और एक्सटेंशन

`GraphifyPlugin` (`graphify.GraphifyPlugin`) द्वारा पंजीकृत:

| कार्य | क्लास | समूह |
|------|-------|-------|
| `collectFromWorkspace` | `ScanWorkspaceTask`     | collect |
| `verifyDagAcyclic`     | `VerifyDagAcyclicTask`  | verify  |
| `collectAndVerify`     | `DefaultTask` (depends + finalizedBy) | collect |

एक्सटेंशन नाम: `graphify` (`GraphifyExtension`)। गुण:
`rootDir`, `outputFile`, `foundryDir`, `dagLevels`, `excludePatterns`।

दोनों scan/verify कार्यों पर `@DisableCachingByDefault` है (फाइलसिस्टम / वर्कस्पेस-स्थिति
पर निर्भर); `collectFromWorkspace` अतिरिक्त रूप से `doNotTrackState(...)` कॉल करता है।

## स्कैन अर्थशास्त्र (`ScanWorkspaceTask`)

- `rootDir` को `Files.walkFileTree` से चलता है, किसी भी सबट्री को छोड़ देता है जो
  `excludePatterns` glob या अंतर्निहित नामों (`build`, `node_modules`, `.gradle`,
  `.git`, `.idea`, `target`) से मेल खाती हो।
- मार्कर फाइलों द्वारा प्रोजेक्ट सनाक्त करता है: `build.gradle.kts`, `build.gradle`,
  `pom.xml`, `package.json`।
- तीन एज प्रकार उत्सर्जित करता है:
  - `contains` — डायरेक्टरी → बाल फाइल/डायरेक्टरी
  - `import` — Kotlin `.kt`/`.kts` imports जो सिबलिंग प्रोजेक्ट डायरेक्टरीज़ में हल होते हैं
  - `reference` — AsciiDoc `link:`/`include:`/`xref:`/`image:` और बैकटिक पथ
  - `agent_reference` — `INDEX.adoc` स्लैश-विभाजित पथ (एजेंट पॉइंटर्स)
- कम्युनिटीज़ `buildRepoMap` से आती हैं: `.git` डायरेक्टरी या फाइल रखने वाली निकटतम संलग्न डायरेक्टरी
  कम्युनिटी id बन जाती है।
- आउटपुट Jackson `ObjectMapper` द्वारा क्रमबद्ध (NON_NULL inclusion, pretty-print)।

## DAG सत्यापन (`VerifyDagAcyclicTask`)

- `foundryDir` के सब-डायरेक्टरीज़ पर पुनरावृत्ति, प्रत्येक `build.gradle.kts` पढ़ता है, और regex द्वारा
  `id("…") version "…"` प्लगइन imports निकालता है।
- प्रोजेक्ट और निर्भरता नाम दोनों `dagLevels` के विरुद्ध हल करता है, प्रत्यय सामान्यीकरण रूपांतर
  (`-gradle`, `-plugin`, `_`↔`-`) लागू करते हुए।
- जब स्तर N का borough ऐसा प्लगइन import करता है जिसका N अपने से बड़ा है तो उल्लंघन उठाया जाता है
  — `GradleException` से बिल्ड विफल।

## परीक्षण मैट्रिक्स

| सूट फाइल | क्षेत्र |
|------------|-------|
| `ScanWorkspaceTaskTest.kt`             | यूनिट — नोड/एज/कम्युनिटी निष्कर्ष |
| `VerifyDagAcyclicTask.kt`             | यूनिट — DAG स्तर रिज़ॉल्यूशन और उल्लंघन रिपोर्टिंग |
| `ScanAndVerifyIntegrationTest.kt`     | एकीकरण — शृंखलित `collectAndVerify` |
| `ScanWorkspaceIntegrationTest.kt`     | एकीकरण — वास्तविक-वर्कस्पेस स्कैन (Gradle TestKit) |
| `model/GraphModelTest.kt`             | यूनिट — डेटा क्लासेज़ और क्रमांकन |

कुल: **58/58 PASS**। JUnit5 प्लेटफ़ॉर्म, AssertJ अभिकथन, एकीकरण सूट्स के लिए Gradle TestKit।

## JVM ट्यूनिंग

कोई विशेष GC फ्लैग आवश्यक नहीं — graphify एक अल्पकालिक बैच स्कैन है। बहुत बड़े
वर्कस्पेस के लिए, हीप बढ़ाएँ:

```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew collectFromWorkspace
```

## बिल्ड कमांड

```bash
./gradlew build                      # पूर्ण बिल्ड (कंपाइल + परीक्षण)
./gradlew build -x test              # केवल कंपाइल
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # स्थानीय प्रकाशन
```

## CI पाइपलाइन

`.github/workflows/test.yml` `ubuntu-latest` पर चलता है, **JDK 24** (Temurin), 15 मिनट
timeout। चरण:

1. Checkout
2. `setup-java@v4` — Java 24
3. `gradle/actions/setup-gradle@v4`
4. `cd graphify-plugin && ./gradlew build`
5. `cd graphify-plugin && ./gradlew publishToMavenLocal`
6. `./gradlew collectAndVerify` — लाइव वर्कस्पेस पर एकीकृत चेन

नोट: CI JDK 24 से बिल्ड करता है यद्यपि टूलचेन Java 23 को लक्षित करता है; दोनों में से कोई भी
Kotlin 2.2.20 टूलचेन आवश्यकता को संतुष्ट करता है।

## प्रकाशन

प्लगइन आर्टिफैक्ट हस्ताक्षरित और Maven Central पर **`com.gradleup.gratatouille`**
प्लगइन (`alias(libs.plugins.publish)`, संस्करण 0.1.4 — *नहीं* `nmcp`) द्वारा प्रकाशित।

`graphify-plugin/build.gradle.kts` से:

- `group = "education.cccp"`, `version = "0.0.2"`
- `gradlePlugin { … }` `graphify` प्लगइन घोषित करता है (id `education.cccp.graphify`,
  impl `graphify.GraphifyPlugin`, tags `knowledge-graph`, `workspace`,
  `dependency-analysis`, `graphify`), वेबसाइट `https://cccp.education/`,
  vcs `https://github.com/cccp-education/graphify-gradle.git`।
- `java { withJavadocJar(); withSourcesJar() }`।
- `publishing.repositories { mavenCentral() }` — सीधे Central Portal पर प्रकाशित।
- `signing { useGpgCmd() }` — प्रत्येक non-SNAPSHOT, non-CI प्रकाशन पर हस्ताक्षर।
- POM Apache 2.0, डेवलपर `cccp-education` (`cccp.edu@gmail.com`),
  `github.com/cccp-education/graphify-gradle` की ओर SCM घोषित करता है।
- वैकल्पिक `relocationGroup` प्रोजेक्ट गुण POM XML में `<distributionManagement>`
  `<relocation>` ब्लॉक इंजेक्ट करता है (समूह पुनर्स्थापना आवश्यक होने पर उपयोगी)।

प्रकाशन कमांड (जब क्रेडेंशियल + GPG उपलब्ध हों):

```bash
./gradlew publishToMavenLocal                                 # स्थानीय जाँच
# Central Portal पर रिलीज़ के लिए ~/.gradle/gradle.properties में पोर्टल क्रेडेंशियल चाहिए
```

## आर्किटेक्चर दस्तावेज़

- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs और शासन (GF-0…GF-6, PUB)
- [BACKLOG.adoc](../BACKLOG.adoc) — शेष आइटम
- [LICENSE](../LICENSE) — Apache 2.0

## योगदान

1. बिल्ड कंपाइल होता है: `./gradlew build -x test`
2. परीक्षण हरे: `./gradlew test` (58/58)
3. प्लगइन को स्व-निहित रखें — N0 अनुबंधों पर कोई LLM/RAG/db निर्भरता नहीं
4. scan/verify के लिए `@DisableCachingByDefault` और `doNotTrackState` अर्थशास्त्र का पालन करें

## लाइसेंस

Apache License 2.0 — [LICENSE](../LICENSE) देखें।

---

_CCCP Education पारिस्थितिकी तंत्र का हिस्सा — `groupId: education.cccp`।