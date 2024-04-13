import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems.jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://maven.tryformation.com/releases") {
        content {
            includeGroup("com.jillesvangurp")
            includeGroup("com.github.jillesvangurp")
            includeGroup("com.tryformation")
            includeGroup("com.tryformation.fritz2")
        }
    }
}

dependencies {
    implementation("com.jillesvangurp:rankquest-core:_")
    implementation("com.github.ajalt.clikt:clikt:_")
    implementation("io.github.microutils:kotlin-logging:_")
    implementation("com.github.ajalt.mordant:mordant:_")
    implementation("org.slf4j:slf4j-api:_")
    implementation("org.slf4j:jcl-over-slf4j:_")
    implementation("org.slf4j:log4j-over-slf4j:_")
    implementation("org.slf4j:jul-to-slf4j:_")
    implementation("org.apache.logging.log4j:log4j-to-slf4j:_") // es seems to insist on log4j2
    implementation("ch.qos.logback:logback-classic:_")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging.exceptionFormat = TestExceptionFormat.FULL
    testLogging.events = setOf(
        TestLogEvent.FAILED,
        TestLogEvent.PASSED,
        TestLogEvent.SKIPPED,
        TestLogEvent.STANDARD_ERROR,
        TestLogEvent.STANDARD_OUT
    )
}

application {
    mainClass.set("com.jillesvangurp.rankquest.RankquestCliKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.jillesvangurp.rankquest.RankquestCliKt"
    }
    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all of the dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}


publishing {
    repositories {
        maven {
            // GOOGLE_APPLICATION_CREDENTIALS env var must be set for this to work
            // public repository is at https://maven.tryformation.com/releases
            url = uri("gcs://mvn-public-tryformation/releases")
            name = "FormationPublic"
        }
    }
}

