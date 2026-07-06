plugins {
    `java-library`
    alias(libs.plugins.publish)
    id("education.cccp.build.gradle-plugin") version "0.0.2"
    id("education.cccp.build.publishing") version "0.0.2"
}

group = "education.cccp"
version = "0.0.2"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api(libs.bundles.jackson)

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.logback.classic)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
}

tasks.withType<Test> {
    outputs.cacheIf { true }
}

gradlePlugin {
    plugins {
        create("graphify") {
            id = "education.cccp.graphify"
            implementationClass = "graphify.GraphifyPlugin"
            displayName = "Graphify Plugin"
            description = "Gradle plugin for knowledge graph extraction across a workspace."
            tags.set(listOf("knowledge-graph", "workspace", "dependency-analysis", "graphify"))
        }
    }
    website = "https://cccp.education/"
    vcsUrl = "https://github.com/cccp-education/graphify-gradle.git"
}

publishingConventions {
    publicationType = "PLUGIN"
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