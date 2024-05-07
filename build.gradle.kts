import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegen
import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegenPlugin
import org.gradle.api.tasks.Copy

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("net.pwall.json:json-kotlin-gradle:0.96.1")
    }
}

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    `maven-publish`
}

repositories {
    mavenCentral()
}

val GITHUB_USER: String by project
val GITHUB_TOKEN: String by project
val ARTIFACT_VARIANT: String = project.findProperty("ARTIFACT_VARIANT") as String? ?: "x"

sourceSets.main.configure {
    kotlin.srcDirs(layout.buildDirectory.dir("generated-sources/kotlin"))
}

val version = project.version.toString()
val majorVersion = Regex("^(\\d+)").find(version)?.value ?: "X"

apply<JSONSchemaCodegenPlugin>()

configure<JSONSchemaCodegen> {
    packageName.set("no.domstol.esas.kontrakterV${majorVersion}")
    inputs {
        inputFile(file("kontrakter"))
    }
    outputDir.set(file("build/generated-sources/kotlin"))
}

// Copy .schema.json files
tasks.register<Copy>("copySchemaFiles") {
    from("kontrakter") {
        include("**/*.schema.json")
    }
    into("build/generated-sources/kotlin/no/domstol/esas/kontrakterV${majorVersion}")
}

tasks.withType<Jar> {
    from("${layout.buildDirectory}/generated-sources/kotlin")
}

// Make sure the copy task is executed before compileKotlin
tasks.named("compileKotlin") {
    dependsOn("copySchemaFiles")
}

publishing {
    repositories {
        maven {
            name = project.name
            url = uri("https://maven.pkg.github.com/domstolene/esas-kontrakt")
            credentials {
                username = GITHUB_USER
                password = GITHUB_TOKEN
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = "no.domstol"
            artifactId = "${project.name}-v$ARTIFACT_VARIANT"
            from(components["java"])
            pom {
                licenses {
                    license {
                        name.set("GNU Lesser General Public License version 3 or later")
                        url.set("https://www.gnu.org/licenses/lgpl-3.0.txt")
                    }
                }
            }
        }
    }
}