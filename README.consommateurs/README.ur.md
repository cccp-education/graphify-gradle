<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — صارف کی رہنمائی

> کسی ورک اسپیس میں نالج گراف (نوڈز، ایجز، کمیونٹیز) نکالنے کے لیے Gradle پلگ ان۔

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **ورژن**: `0.0.2` · **گروپ**: `education.cccp` · **پلگ ان ID**: `education.cccp.graphify`
- **بلڈ**: `./gradlew build` · **ٹیسٹ**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **حالت**: مکمل، غیر فعال — نکالا گیا گراف ڈاؤن اسٹریم صارفین کو فیض دیتا ہے۔

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## یہ کیا کرتا ہے

`graphify-gradle` ایک ورک اسپیس فائل سسٹم پر چلتا ہے، پروجیکٹس (Gradle/Maven/Node)،
فائلوں، ڈائریکٹریز اور انٹرا-ورک اسپیس تعلقات کا پتہ لگاتا ہے، پھر ایک معیاری
`graph.json` خارج کرتا ہے جو **نوڈز**، **ایجز** اور **کمیونٹیز** کی وضاحت کرتا ہے۔ کمیونٹیز
احاطہ بند Git ریپوزٹریز سے حاصل ہوتی ہیں (ایک `.git` پیش رو = ایک کمیونٹی)۔

خارج کردہ `graph.json` CCCP Education کے ماحولیاتی نظام میں استعمال ہوتا ہے:

```
graphify-gradle (graph.json) → plantuml-gradle (ڈایاگرام)
                             → bakery-gradle     (اسٹیٹک سائٹ گراف ویو)
                             → runner-gradle     (ورک اسپیس ٹورنگ)
```

## جلد آغاز

### 1. پلگ ان لگائیں

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. ایکسٹینشن ترتیب دیں

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
        // …دیگر boroughs…
    ))
}
```

### 3. گراف بنائیں

```bash
./gradlew collectFromWorkspace      # FS سکین → graph.json
./gradlew verifyDagAcyclic          # N0→N3 DAG پرت بندی لاگو
./gradlew collectAndVerify          # چین: collectFromWorkspace + verifyDagAcyclic
```

خارج کردہ `graph.json` کا خاکہ:

```json
{
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

## دستیاب ٹاسکس

| ٹاسک | گروپ | تفصیل |
|------|-------|-------------|
| `collectFromWorkspace` | collect   | ورک اسپیس چلیں → `graph.json` خارج کریں (نوڈز، ایجز، کمیونٹیز)۔ |
| `verifyDagAcyclic`     | verify    | اعلان کردہ `dagLevels` لاگو — کوئی پروجیکٹ اعلیٰ N سطح پر منحصر نہیں ہو سکتا۔ |
| `collectAndVerify`     | collect   | سہولت چین: پہلے `collectFromWorkspace` پھر `verifyDagAcyclic` (finalizedBy)۔ |

## ایکسٹینشن DSL

```gradle
graphify {
    // فائل سسٹم چلنے کی جڑ (ضروری)۔
    rootDir.set(file("/home/cheroliv/workspace"))

    // graph.json لکھنے کی جگہ (ضروری)۔
    outputFile.set(file("graph.json"))

    // borough پروجیکٹس رکھنے والی ڈائریکٹری — DAG سطحی خلاف ورزیوں کے لیے سکین کی جاتی ہے۔
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // پروجیکٹ نام → N سطح میپ (verifyDagAcyclic استعمال کرتا ہے)۔
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // glob اخراج پیٹرن (اختیاری — نیچے دیفالٹ)۔
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))
}
```

## پیش شرائط

- **Java** 23+ (Kotlin 2.2.20 ٹول چین)
- **Gradle** 9.x (wrapper فراہم کردہ)
- سکین کیے جانے والے ورک اسپیس تک پڑھنے کی رسائی

## بلڈ اور ٹیسٹ

```bash
./gradlew build                      # مکمل بلڈ (کمپائل + ٹیسٹ)
./gradlew build -x test              # صرف کمپائل
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # مقامی اشاعت
```

## مسئلہ حل

| علامت | حل |
|---------|-----|
| `graph.json` خالی              | یقینی کریں کہ `rootDir` حل ہوتا ہے اور پڑھنے کے قابل ہے; `excludePatterns` چیک کریں۔ |
| `DAG VIOLATIONS DETECTED`       | کوئی borough اعلیٰ N سطح سے پلگ ان درآمد کرتا ہے — `dagLevels` یا درآمد کو ایڈجسٹ کریں۔ |
| بڑے monorepo پر سست سکین        | `rootDir` تنگ کریں، `excludePatterns` وسیع کریں، غیر متعلقہ ذیلی درخت سکین کرنے سے گریز کریں۔ |
| ناقابل رسائی پاتھ وارننگز       | بے ضرر — واکر ناقابلِ قراءت پاتھ کو چھوڑ دیتا ہے اور جزوی نتائج کے ساتھ جاری رہتا ہے۔ |

## لائسنس

Apache License 2.0 — [LICENSE](../LICENSE) دیکھیں۔

---

_CCCP Education ماحولیاتی نظام کا حصہ — `groupId: education.cccp`۔