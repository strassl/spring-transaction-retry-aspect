import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.4.31"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}


allprojects {
    group = "at.sigmoid"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }

    dependencies {
        implementation(group = "org.jetbrains.kotlin", name = "kotlin-reflect", version = "1.4.31")
        testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = "5.7.1")
        testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = "5.7.1")
        testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test", version = "1.4.31")
        testImplementation(group = "com.natpryce", name = "hamkrest", version = "1.8.0.1")
        testImplementation(group = "org.mockito.kotlin", name = "mockito-kotlin", version = "2.2.8")
    }
}