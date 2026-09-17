<!-- translated from README.md rev 0.0.2 -->
# graphify-gradle — دليل المستهلك

> إضافة Gradle لاستخراج رسم بياني للمعرفة (عُقد، حواف، مجتمعات) عبر مساحة عمل.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/graphify-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/graphify-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.graphify.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.graphify)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/graphify-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/graphify-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/graphify-gradle?label=License)](../LICENSE)

- **الإصدار**: `0.0.2` · **المجموعة**: `education.cccp` · **معرّف الإضافة**: `education.cccp.graphify`
- **البناء**: `./gradlew build` · **الاختبارات**: `./gradlew test` (JUnit5 — 58/58 PASS)
- **الحالة**: منتهٍ، خامل — الرسم البياني المستخرج يُغذّي المستهلكين下游.

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## ماذا يفعل

`graphify-gradle` يجتاز نظام ملفات مساحة العمل، يكتشف المشاريع (Gradle/Maven/Node)،
الملفات، الأدلة، والعلاقات داخل مساحة العمل، ثم يُصدر `graph.json` معياريًا يصف
**العُقد** و**الحواف** و**المجتمعات**. تُشتق المجتمعات من مستودعات Git المحيطة
(سلف `.git` واحد = مجتمع واحد).

يُستهلك `graph.json` المُصدَر في منظومة CCCP Education بواسطة:

```
graphify-gradle (graph.json) → plantuml-gradle (المخططات)
                             → bakery-gradle     (عرض الرسم البياني للموقع الثابت)
                             → runner-gradle     (جولة مساحة العمل)
```

## البداية السريعة

### 1. تطبيق الإضافة

```gradle
plugins {
    id("education.cccp.graphify") version "0.0.2"
}
```

### 2. تهيئة الامتداد

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
        // …boroughs أخرى…
    ))
}
```

### 3. توليد الرسم البياني

```bash
./gradlew collectFromWorkspace      # مسح نظام الملفات → graph.json
./gradlew verifyDagAcyclic          # فرض طبقات N0→N3 DAG
./gradlew collectAndVerify          # سلسلة: collectFromWorkspace + verifyDagAcyclic
```

شكل `graph.json` الناتج:

```json
{
  "schemaVersion": 1,
  "nodes":       [{ "id": "...", "label": "...", "type": "project|directory|file|section", "community": "...", "metadata": {} }],
  "edges":       [{ "source": "...", "target": "...", "type": "contains|import|reference|agent_reference|has_section|subsection", "label": null }],
  "communities": [{ "id": "...", "label": "...", "size": 0 }]
}
```

`schemaVersion` عدد صحيح يصف *مخطط النقل* (wire schema)، مستقل عن إصدار الإضافة. يحمل `graphify.model.GraphModel` أيضًا `@JsonIgnoreProperties(ignoreUnknown = true)`، لذا فإن المستهلك الصارم الذي يحدّث تبعيته يتسامح مع الحقول المستقبلية دون أي تغيير في الكود، ويظل `graph.json` القديم الذي يفتقر إلى `schemaVersion` يُقرأ بالقيمة الافتراضية.

## المهام المتاحة

| المهمة | المجموعة | الوصف |
|------|-------|-------------|
| `collectFromWorkspace` | collect   | اجتياز مساحة العمل → إصدار `graph.json` (عُقد، حواف، مجتمعات). |
| `verifyDagAcyclic`     | verify    | فرض `dagLevels` المُعلنة — لا يجوز لمشروع أن يعتمد على مستوى N أعلى. |
| `collectAndVerify`     | collect   | سلسلة ملائمة: `collectFromWorkspace` ثم `verifyDagAcyclic` (finalizedBy). |

## امتداد DSL

```gradle
graphify {
    // جذر اجتياز نظام الملفات (مطلوب).
    rootDir.set(file("/home/cheroliv/workspace"))

    // مكان كتابة graph.json (مطلوب).
    outputFile.set(file("graph.json"))

    // الدليل الحاوي لمشاريع borough — يُمسح للكشف عن انتهاكات مستويات DAG.
    foundryDir.set(file("/home/cheroliv/workspace/foundry/public"))

    // خريطة اسم المشروع → مستوى N (يستخدمه verifyDagAcyclic).
    dagLevels.set(mapOf("graphify-gradle" to 0, "codebase-gradle" to 1 /* … */))

    // أنماط استبعاد glob (اختياري — افتراضي أدناه).
    excludePatterns.set(listOf(
        "**/build/**", "**/node_modules/**", "**/.gradle/**",
        "**/.git/**", "**/.idea/**", "**/target/**"
    ))

    // إعادة مسح تدريجية اختيارية (افتراضي: false → مسح كامل).
    incremental.set(true)

    // مكان تخزين ذاكرة التبصيم المؤقتة (افتراضي: build/graphify/fingerprints.json).
    cacheFile.set(layout.buildDirectory.file("graphify/fingerprints.json").get().asFile)
}
```

## المسح التدريجي

`collectFromWorkspace` يمسح الشجرة بالكامل في كلا الوضعين؛ فالاجتياز هو ما
يكتشف الملفات المضافة والمحذوفة. مع `incremental.set(true)`، يتم تخزين
*تحليل كل ملف* مؤقتًا:

- الملف الذي لم تتغير مساحته و mtime الخاص به يعيد استخدام استخراجه المخزَّن (بدون قراءة)؛
- الملف الذي تغيّر mtime الخاص به لكن محتواه لم يتغير يُكتشف عبر SHA-256
  ويعيد استخدام استخراج ملف مطابق (مثلًا بعد `git checkout`)؛
- المحتوى الجديد فعليًا فقط يُعاد تحليله.

ذاكرة التخزين المؤقتة هي مستند JSON تحت `build/` (لا يُودَع في المستودع أبدًا) وتُقلَّم
لتشمل الملفات الموجودة عند كل مسح. إنها تحسين فقط: ذاكرة تخزين مؤقتة مفقودة أو تالفة
أو بإصدار غير معروف تتدهور بصمت إلى مسح كامل، ويكون `graph.json` الناتج
مطابقًا بايتيًا لتشغيل غير تدريجي.

## المتطلبات المسبقة

- **Java** 23+ (سلسلة أدوات Kotlin 2.2.20)
- **Gradle** 9.x (موفّر wrapper)
- وصول قراءة إلى مساحة العمل المراد مسحها

## البناء والاختبار

```bash
./gradlew build                      # بناء كامل (ترجمة + اختبارات)
./gradlew build -x test              # ترجمة فقط
./gradlew test                       # JUnit5 — 58/58 PASS
./gradlew publishToMavenLocal        # نشر محلي
```

## استكشاف الأخطاء

| العَرض | الإصلاح |
|---------|-----|
| `graph.json` فارغ              | تأكد من أن `rootDir` يُحل وقابل للقراءة؛ تحقق من `excludePatterns`. |
| `DAG VIOLATIONS DETECTED`       | borough يستورد إضافة من مستوى N أعلى — اضبط `dagLevels` أو الاستيراد. |
| مسح بطيء على monorepos كبيرة   | ضيّق `rootDir`، وسّع `excludePatterns`، تجنب مسح أشجار فرعية غير ذات صلة. |
| تحذيرات مسار غير قابل للوصول   | غير ضارة — الـwalker يتخطى المسارات غير القابلة للقراءة ويستمر بنتائج جزئية. |

## الترخيص

Apache License 2.0 — راجع [LICENSE](../LICENSE).

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`.