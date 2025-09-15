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
    api("net.pwall.json:json-kotlin-schema:0.56")
}

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.1.20"
    `maven-publish`
}

repositories {
    mavenCentral()
}

val GITHUB_USER: String by project
val GITHUB_TOKEN: String by project
val ARTIFACT_VARIANT: String = project.findProperty("ARTIFACT_VARIANT") as String? ?: "x"

java {
    withSourcesJar()
}

sourceSets.main.configure {
    kotlin.srcDirs(layout.buildDirectory.dir("generated-sources/kotlin"))
}

val majorVersion = Regex("^(\\d+)").find(project.version.toString())?.value ?: "X"

apply<JSONSchemaCodegenPlugin>()

configure<JSONSchemaCodegen> {
    packageName.set("no.domstol.esas.kontrakter.V${majorVersion}")
    inputs {
        inputFile(file("kontrakter"))
    }
    outputDir.set(file("build/generated-sources/kotlin"))
}

// Copy .schema.json files
tasks.register<Copy>("copySchemaFiles") {
    from("kontrakter") {
        include("**/*.schema.json")
        include("**/eksempelfiler/*.json")
    }
    into(layout.buildDirectory.dir("generated-sources/kotlin/no/domstol/esas/kontrakter/V${majorVersion}"))
}

tasks.withType<Jar> {
    dependsOn("copySchemaFiles")
    from(layout.buildDirectory.dir("generated-sources/kotlin")) {
        include("**/*.schema.json")  // Include schema files
        include("**/*.json") // Include example files
    }
}

tasks.named<Jar>("sourcesJar") {
    dependsOn("generate", "copySchemaFiles")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
            from(components["java"])
            groupId = "no.domstol"
            artifactId = "${project.name}-v$ARTIFACT_VARIANT"
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
