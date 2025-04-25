import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegen
import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegenPlugin
import org.gradle.api.tasks.Copy

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("net.pwall.json:json-kotlin-gradle:0.120")
    }
}

dependencies {
    implementation("net.pwall.json:json-kotlin-schema:0.51")
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

val majorVersion = Regex("^(\\d+)").find(project.version.toString())?.value ?: "X"

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
    into(layout.buildDirectory.dir("generated-sources/kotlin/no/domstol/esas/kontrakterV${majorVersion}"))
}

//Copy example files
tasks.register<Copy>("copyExampleFiles") {
    from("kontrakter") {
        include("**/eksempelfiler/*.json")
    }
    into(layout.buildDirectory.dir("generated-sources/kotlin/no/domstol/esas/kontrakterV${majorVersion}/"))

    includeEmptyDirs = false
}

// Copy validator
tasks.register<Copy>("copyValidator") {
    from("validation") {
        include("JSONSchemaValidator.kt")
    }
    into(layout.buildDirectory.dir("generated-sources/kotlin/no/domstol/esas"))
}

tasks.withType<Jar> {
    dependsOn("copySchemaFiles")
    dependsOn("copyValidator")
    dependsOn("copyExampleFiles")
    from("src/main/kotlin")  // Include main source set
    from(layout.buildDirectory.dir("generated-sources/kotlin")) {
        include("**/*.schema.json")  // Include schema files
        include("**/*.kt")  // Include Kotlin files
    }
    // Include example files
    from(layout.buildDirectory.dir(
        "generated-sources/kotlin/no/domstol/esas/kontrakterV${majorVersion}/eksempelfiler")) {
        include("**/*.json")
    }
}

// Make sure the copy task is executed before compileKotlin
tasks.named("compileKotlin") {
    dependsOn("copySchemaFiles", "copyExampleFiles", "copyValidator")
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
            artifact(tasks.jar)
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