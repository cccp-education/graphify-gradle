import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    signing
    `java-library`
    `maven-publish`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
}

group = "education.cccp"
version = libs.plugins.graphify.get().version
kotlin.jvmToolchain(23)

repositories {
    mavenCentral()
    gradlePluginPortal()
}

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
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    outputs.cacheIf { true }
}

gradlePlugin {
    plugins {
        create("graphify") {
            id = "education.cccp.graphify"
            implementationClass = "com.cheroliv.graphify.GraphifyPlugin"
            displayName = "Graphify Plugin"
            description = "Gradle plugin for knowledge graph extraction across a workspace."
            tags.set(listOf("knowledge-graph", "workspace", "dependency-analysis", "graphify"))
        }
    }
    website = "https://cheroliv.com"
    vcsUrl = "https://github.com/cheroliv/graphify-gradle.git"
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        withType<MavenPublication> {
            if (name == "pluginMaven") {
                pom {
                    name.set(gradlePlugin.plugins.getByName("graphify").displayName)
                    description.set(gradlePlugin.plugins.getByName("graphify").description)
                    url.set(gradlePlugin.website.get())
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("cccp-education")
                            name.set("CCCP Education")
                            email.set("cccp.education@gmail.com")
                        }
                    }
                    scm {
                        connection.set(gradlePlugin.vcsUrl.get())
                        developerConnection.set(gradlePlugin.vcsUrl.get())
                        url.set(gradlePlugin.vcsUrl.get())
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            url = if (version.toString().endsWith("-SNAPSHOT")) {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            } else {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
            credentials {
                username = project.findProperty("ossrhUsername") as? String
                password = project.findProperty("ossrhPassword") as? String
            }
        }
        mavenCentral()
    }
}

signing {
    if (System.getenv("CI") != "true" && !version.toString().endsWith("-SNAPSHOT")) {
        sign(publishing.publications)
    }
    useGpgCmd()
}
