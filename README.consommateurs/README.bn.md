<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — ভোক্তা গাইড

> একটি ওয়ার্কস্পেস জুড়ে নলেজ গ্রাফ (নোড, এজ, কমিউনিটি) বের করার জন্য Gradle প্লাগইন।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **সংস্করণ**: `0.0.2` · **গ্রুপ**: `education.cccp` · **প্লাগইন আইডি**: `education.cccp.graphify`
- **বিল্ড**: `./gradlew build` · **পরীক্ষা**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **অবস্থা**: সমাপ্ত, নিষ্ক্রিয় — বের করা গ্রাফ ডাউনস্ট্রিম ভোক্তাদের পরিচালিত করে।

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## এটি কী করে

`graphify-gradle` একটি ওয়ার্কস্পেস ফাইলসিস্টেম পার করে, প্রজেক্ট (Gradle/Maven/Node),
ফাইল, ডাইরেক্টরি এবং ইন্ট্রা-ওয়ার্কস্পেস সম্পর্ক সনাক্ত করে, তারপর একটি মানক
`graph.json` নির্গত করে যা **নোড**, **এজ** এবং **কমিউনিটি** বর্ণনা করে। কমিউনিটিগুলি
ঘেরাও করা Git রিপোজিটরি থেকে উদ্ভূত (একটি `.git` পূর্বপুরুষ = একটি কমিউনিটি)।

নির্গত `graph.json` CCCP Education ইকোসিস্টেমে নিম্নলিখিত দ্বারা ব্যবহৃত হয়:

```
graphify-gradle (graph.json) → plantuml-gradle (চিত্র)
                             → bakery-gradle     (স্ট্যাটিক সাইট গ্রাফ ভিউ)
                             → runner-gradle     (ওয়ার্কস্পেস ট্যুরিং)
```

## দ্রুত শুরু

### 1. প্লাগইন প্রয়োগ করুন

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. এক্সটেনশন কনফিগার করুন

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
        // …অন্যান্য boroughs…
    ))
}
```

### 3. গ্রাফ তৈরি করুন

```bash
./gradlew collectFromWorkspace      # FS স্ক্যান → graph.json
./gradlew verifyDagAcyclic          # N0→N3 DAG স্তরবিন্যাস প্রয়োগ
./gradlew collectAndVerify          # চেইন: collectFromWorkspace + verifyDagAcyclic
```

নির্গত `graph.json` এর রূপ:

```json
{
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

## উপলব্ধ টাস্ক

| টাস্ক | গ্রুপ | বিবরণ |
|------|-------|-------------|
| `collectFromWorkspace` | collect   | ওয়ার্কস্পেস পার করুন → `graph.json` নির্গত করুন (নোড, এজ, কমিউনিটি)। |
| `verifyDagAcyclic`     | verify    | ঘোষিত `dagLevels` প্রয়োগ — একটি প্রজেক্ট উচ্চতর N স্তরের উপর নির্ভর করতে পারে না। |
| `collectAndVerify`     | collect   | সুবিধার চেইন: প্রথমে `collectFromWorkspace` তারপর `verifyDagAcyclic` (finalizedBy)। |

## এক্সটেনশন DSL

```gradle
graphify {
    // ফাইলসিস্টেম পার করার রুট (আবশ্যক)।
    rootDir.set(file("/home/cheroliv/workspace"))

    // graph.json লেখার স্থান (আবশ্যক)।
    outputFile.set(file("graph.json"))

    // borough প্রজেক্ট ধারণকারী ডাইরেক্টরি — DAG স্তর লঙ্ঘনের জন্য স্ক্যান করা হয়।
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // প্রজেক্ট নাম → N স্তর ম্যাপ (verifyDagAcyclic দ্বারা ব্যবহৃত)।
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // glob বহিষ্কার প্যাটার্ন (ঐচ্ছিক — নিচে ডিফল্ট)।
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))
}
```

## পূর্বশর্ত

- **Java** 23+ (Kotlin 2.2.20 টুলচেইন)
- **Gradle** 9.x (wrapper সরবরাহিত)
- স্ক্যান করার উদ্দেশ্যে ওয়ার্কস্পেসে পড়ার অ্যাক্সেস

## বিল্ড ও পরীক্ষা

```bash
./gradlew build                      # সম্পূর্ণ বিল্ড (কম্পাইল + পরীক্ষা)
./gradlew build -x test              # শুধু কম্পাইল
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # স্থানীয় প্রকাশনা
```

## সমস্যা সমাধান

| লক্ষণ | সমাধান |
|---------|-----|
| `graph.json` খালি              | নিশ্চিত করুন যে `rootDir` সমাধানযোগ্য এবং পঠনযোগ্য; `excludePatterns` যাচাই করুন। |
| `DAG VIOLATIONS DETECTED`       | একটি borough উচ্চতর N স্তর থেকে প্লাগইন আমদানি করে — `dagLevels` বা আমদানি সমন্বয় করুন। |
| বড় monorepo-তে ধীর স্ক্যান     | `rootDir` সংকুচিত করুন, `excludePatterns` প্রসারিত করুন, অসম্পর্কিত সাবট্রি স্ক্যান এড়িয়ে চলুন। |
| অ্যাক্সেসযোগ্য নয় এমন পথে সতর্কতা | ক্ষতিকারক নয় — walker অপঠনযোগ্য পথ এড়িয়ে যায় এবং আংশিক ফলাফলে চলতে থাকে। |

## লাইসেন্স

Apache License 2.0 — [LICENSE](../LICENSE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।