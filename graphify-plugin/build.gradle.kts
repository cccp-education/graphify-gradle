import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-library`
    alias(libs.plugins.publish)
    id("education.cccp.build.gradle-plugin") version "0.0.14"
    id("education.cccp.build.publishing") version "0.0.14"
    id("education.cccp.build.lint") version "0.0.14"
    id("education.cccp.build.functional-test") version "0.0.14"
    id("education.cccp.build.cucumber") version "0.0.14"
}

group = "education.cccp"
version =
    ws.versions.graphify.plugin
        .get()

repositories {
    // CNV-16 — JitPack mirror for the public artefacts (Central suspended until
    // 2026-12-01). Required here because the conventions plugin declares
    // project repositories, so the settings-level repositories are ignored.
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(libs.bundles.jackson)

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.logback.classic)
    testImplementation(libs.assertj)
    // N0 contract — the section-scoped version catalog reader (MEMPHIS
    // catalog-contracts), consumed only by the publication hygiene guard.
    testImplementation(libs.catalog.contracts)
    testImplementation(gradleTestKit())
}

tasks.withType<Test> {
    outputs.cacheIf { true }

    // D5R-2 — inject the *published* workspace catalog version resolved by Gradle
    // (from the published catalog pin). The publication hygiene guard must never
    // read a neighbour repository's working tree: that is racy between sessions.
    systemProperty(
        "graphify.publishedCatalog.graphifyVersion",
        ws.versions.graphify.plugin
            .get(),
    )
}

// Exclude the Cucumber runner + step glue from the unit `test` task.
// The convention plugin excludes `*.scenarios.*` by default, but this project
// uses the `graphify.steps` package, so we add a matching filter.
tasks.named<Test>("test") {
    filter {
        excludeTestsMatching("graphify.steps.**")
    }
}

gradlePlugin {
    plugins {
        create("graphify") {
            id = "education.cccp.graphify"
            implementationClass = "graphify.GraphifyPlugin"
            displayName = "Graphify Plugin"
            description = "Gradle plugin for knowledge graph extraction across a workspace."
            tags.set(listOf("knowledge-graph", "workspace", "dependency-analysis", "graphify"))
            compatibility {
                features {
                    configurationCache.set(false)
                    isolatedProjects.set(false)
                }
            }
        }
    }
    website = "https://cccp.education/"
    vcsUrl = "https://github.com/cccp-education/graphify-gradle.git"
}

publishingConventions {
    publicationType = "LIBRARY"
    // Sonatype 130791 — the POM url/scm must point to the exact public
    // repository, never to the organization root (classified "unreachable_repo").
    website = "https://github.com/cccp-education/graphify-gradle"
    vcsUrl = "https://github.com/cccp-education/graphify-gradle.git"
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("Graphify Gradle Plugin")
            description.set("Gradle plugin for knowledge graph extraction across a workspace.")
        }
    }
    repositories {
        mavenCentral()
    }
}
