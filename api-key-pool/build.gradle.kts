import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    signing
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

group = "education.cccp"
version = libs.versions.api.key.pool.get()
kotlin.jvmToolchain(JavaVersion.VERSION_24.ordinal)

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.logback.classic)
    testImplementation(libs.assertj)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("ApiKeyPool")
                description.set("N0 shared library — LLM API key pool with rotation, quota tracking, and audit logging.")
                url.set("https://github.com/cheroliv/graphify-gradle")
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
                    connection.set("https://github.com/cheroliv/graphify-gradle.git")
                    developerConnection.set("https://github.com/cheroliv/graphify-gradle.git")
                    url.set("https://github.com/cheroliv/graphify-gradle")
                }
                // RELOCATION : prépare la migration du groupId éducation.cccp →
                // <futur-domaine>. Activer avec -Prem relocationGroup="io.github.cccp-education"
                // Effet : injecte <distributionManagement><relocation><groupId>...</groupId></relocation>
                // dans le POM publié. Les consommateurs existants seront redirigés automatiquement
                // vers le nouveau groupId lors de la prochaine màj de dépendance.
                project.findProperty("relocationGroup")?.let { targetGroup ->
                    withXml {
                        val pom = asElement()
                        val doc = pom.ownerDocument
                        val distMgmt = doc.createElement("distributionManagement")
                        val relocation = doc.createElement("relocation")
                        relocation.appendChild(doc.createElement("groupId")).also { it.textContent = targetGroup.toString() }
                        relocation.appendChild(doc.createElement("artifactId")).also { it.textContent = project.name }
                        distMgmt.appendChild(relocation)
                        pom.appendChild(distMgmt)
                    }
                }
            }
        }
    }
    repositories {
        mavenCentral()
    }
}

signing {
    if (System.getenv("CI") != "true" && !version.toString().endsWith("-SNAPSHOT")) {
        sign(publishing.publications)
    }
    useGpgCmd()
}
