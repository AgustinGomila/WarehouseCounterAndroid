buildscript {
    val androidGradlePluginVersion = libs.versions.agp.get()
    val kotlinVersion = libs.versions.kotlinVersion.get()
    val roomVersion = libs.versions.roomVersion.get()
    val jacocoVersion = libs.versions.jacocoVersion.get()

    dependencies {
        classpath("com.android.tools.build:gradle:$androidGradlePluginVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jacoco:org.jacoco.core:$jacocoVersion")
        classpath("androidx.room:room-gradle-plugin:$roomVersion")
    }

    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.ALL
}