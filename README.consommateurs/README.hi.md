<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — उपभोक्ता गाइड

> किसी वर्कस्पेस में नॉलेज ग्राफ (नोड्स, एजेज़, कम्युनिटीज़) निकालने वाला Gradle प्लगइन।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **संस्करण**: `0.0.2` · **समूह**: `education.cccp` · **प्लगइन आईडी**: `education.cccp.graphify`
- **बिल्ड**: `./gradlew build` · **परीक्षण**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **स्थिति**: पूर्ण, निष्क्रिय — निकाला गया ग्राफ डाउनस्ट्रीम उपभोक्ताओं को शक्ति देता है।

🌐 Languages: [English](README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## यह क्या करता है

`graphify-gradle` वर्कस्पेस फाइलसिस्टम पर चलता है, प्रोजेक्ट्स (Gradle/Maven/Node), फाइलों, डायरेक्टरीज़ और इंट्रा-वर्कस्पेस संबंधों का पता लगाता है, फिर एक मानकीकृत `graph.json` उत्सर्जित करता है जो **नोड्स**, **एजेज़** और **कम्युनिटीज़** का वर्णन करता है। कम्युनिटीज़ संलग्न Git रिपॉज़िटरीज़ से प्राप्त होती हैं (एक `.git` पूर्वज = एक कम्युनिटी)।

उत्सर्जित `graph.json` CCCP Education पारिस्थितिकी तंत्र में निम्न द्वारा उपभोग किया जाता है:

```
graphify-gradle (graph.json) → plantuml-gradle (आरेख)
                             → bakery-gradle     (स्टेटिक साइट ग्राफ दृश्य)
                             → runner-gradle     (वर्कस्पेस टूरिंग)
```

## त्वरित आरंभ

### 1. प्लगइन लागू करें

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. एक्सटेंशन कॉन्फ़िगर करें

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
        // …अन्य boroughs…
    ))
}
```

### 3. ग्राफ जनरेट करें

```bash
./gradlew collectFromWorkspace      # फाइलसिस्टम स्कैन → graph.json
./gradlew verifyDagAcyclic          # N0→N3 DAG स्तरण लागू करें
./gradlew collectAndVerify          # शृंखला: collectFromWorkspace + verifyDagAcyclic
```

उत्सर्जित `graph.json` का आकार:

```json
{
  "schemaVersion": 1,
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file|section", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference|has_section|subsection", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

`schemaVersion` एक पूर्णांक है जो *वायर स्कीमा* का वर्णन करता है, जो प्लगइन संस्करण से स्वतंत्र है। `graphify.model.GraphModel` भी `@JsonIgnoreProperties(ignoreUnknown = true)` धारण करता है, इसलिए अपनी निर्भरता अपडेट करने वाला सख्त उपभोक्ता बिना किसी कोड परिवर्तन के भविष्य के फ़ील्ड सहन करता है, और `schemaVersion` रहित विरासती `graph.json` अब भी डिफ़ॉल्ट मान के साथ पढ़ा जाता है।

## उपलब्ध कार्य

| कार्य | समूह | विवरण |
|------|-------|-------------|
| `collectFromWorkspace` | collect   | वर्कस्पेस चलें → `graph.json` उत्सर्जित करें (नोड्स, एजेज़, कम्युनिटीज़)। |
| `verifyDagAcyclic`     | verify    | घोषित `dagLevels` लागू करें — कोई प्रोजेक्ट उच्च N स्तर पर निर्भर नहीं हो सकता। |
| `collectAndVerify`     | collect   | सुविधा शृंखला: पहले `collectFromWorkspace` फिर `verifyDagAcyclic` (finalizedBy)। |

## एक्सटेंशन DSL

```gradle
graphify {
    // फाइलसिस्टम चलने का मूल (आवश्यक)।
    rootDir.set(file("/home/cheroliv/workspace"))

    // graph.json लिखने का स्थान (आवश्यक)।
    outputFile.set(file("graph.json"))

    // borough प्रोजेक्ट्स वाली डायरेक्टरी — DAG स्तर उल्लंघनों के लिए स्कैन की जाती है।
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // प्रोजेक्ट नाम → N स्तर मैप (verifyDagAcyclic द्वारा उपयोग)।
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // glob बहिष्करण पैटर्न (वैकल्पिक — नीचे डिफ़ॉल्ट)।
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))

    // ऑप्ट-इन इंक्रीमेंटल पुनःस्कैन (डिफ़ॉल्ट: false → पूर्ण स्कैन)।
    incremental.set(true)

    // फ़िंगरप्रिंट कैश कहाँ संग्रहीत है (डिफ़ॉल्ट: build/graphify/fingerprints.json)।
    cacheFile.set(layout.buildDirectory.file("graphify/fingerprints.json").get().asFile)
}
```

## इंक्रीमेंटल स्कैन

`collectFromWorkspace` दोनों मोड में पूरे ट्री को स्कैन करता है; वॉक ही
जोड़ी गई और हटाई गई फ़ाइलों का पता लगाता है। `incremental.set(true)` के साथ, *प्रति-फ़ाइल
पार्सिंग* मेमोइज़ की जाती है:

- जिस फ़ाइल का आकार और mtime अपरिवर्तित है, वह अपना कैश किया गया एक्सट्रैक्शन पुनः उपयोग करती है (कोई पठन नहीं);
- जिस फ़ाइल का mtime बदला है परंतु सामग्री अपरिवर्तित है, उसे SHA-256 द्वारा पहचाना जाता है
  और किसी समरूप फ़ाइल का एक्सट्रैक्शन पुनः उपयोग करती है (जैसे `git checkout` के बाद);
- केवल वास्तव में नई सामग्री ही पुनः पार्स की जाती है।

कैश `build/` के अंतर्गत एक JSON दस्तावेज़ है (कभी कमिट नहीं किया जाता) और प्रत्येक
स्कैन पर उपस्थित फ़ाइलों तक छाँट लिया जाता है। यह केवल एक अनुकूलन है: अनुपस्थित, करप्ट
या अज्ञात-संस्करण कैश चुपचाप पूर्ण स्कैन में बदल जाता है, और उत्पन्न
`graph.json` एक गैर-इंक्रीमेंटल रन के समान बाइट-अनुरूप होता है।

## पूर्वापेक्षाएँ

- **Java** 23+ (Kotlin 2.2.20 टूलचेन)
- **Gradle** 9.x (wrapper प्रदान किया गया)
- स्कैन किए जाने वाले वर्कस्पेस तक पढ़ने की पहुँच

## बिल्ड और परीक्षण

```bash
./gradlew build                      # पूर्ण बिल्ड (कंपाइल + परीक्षण)
./gradlew build -x test              # केवल कंपाइल
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # स्थानीय प्रकाशन
```

## समस्या निवारण

| लक्षण | समाधान |
|---------|-----|
| `graph.json` खाली              | पुष्टि करें कि `rootDir` हल हो जाता है और पठनीय है; `excludePatterns` जाँचें। |
| `DAG VIOLATIONS DETECTED`       | कोई borough उच्च N स्तर से प्लगइन आयात करता है — `dagLevels` या आयात समायोजित करें। |
| बड़े monorepo पर स्कैन धीमा    | `rootDir` सीमित करें, `excludePatterns` विस्तारित करें, असंबंधित उप-वृक्षों को स्कैन करने से बचें। |
| अप्राप्य पथ चेतावनियाँ         | हानिरहित — वॉकर अपठनीय पथों को छोड़ देता है और आंशिक परिणामों के साथ जारी रहता है। |

## लाइसेंस

Apache License 2.0 — [LICENSE](../LICENSE) देखें।

---

_CCCP Education पारिस्थितिकी तंत्र का हिस्सा — `groupId: education.cccp`।