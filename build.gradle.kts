import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm") version "2.0.0"
    application
}

group = "me.sedwi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.12")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

application {
    mainClass.set("com.maedjyukghoti.tictactoe.MainKt")
}

// By default, the system.in of your Gradle build is not wired up with the system.in of the run (JavaExec) task.
// https://stackoverflow.com/a/13172566
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}