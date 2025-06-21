import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.serialization)
    alias(libs.plugins.jacoco)
    alias(libs.plugins.parcelize)
}

android {
    namespace = "com.dacosys.warehouseCounter"
    compileSdk = libs.versions.androidTargetSdk.get().toInt()

    // Manejo de versiones
    val versionPropsFile = file("version.properties")
    val versionProps = Properties()
    if (!versionPropsFile.exists()) {
        versionPropsFile.createNewFile()
        versionProps["VERSION_PATCH"] = "0"
        versionProps["VERSION_NUMBER"] = "0"
        versionProps["VERSION_BUILD"] = "-1"
        versionPropsFile.writer().use { versionProps.store(it, null) }
    }

    versionProps.load(FileInputStream(versionPropsFile))
    val value = if (gradle.startParameter.taskNames.any {
            it.contains("assembleRelease", ignoreCase = true) ||
                    it.contains("bundleRelease", ignoreCase = true)
        }) 1 else 0

    val versionMajor = 12
    val versionMinor = 0
    val versionPatch = versionProps["VERSION_PATCH"].toString().toInt() + value
    val versionBuild = versionProps["VERSION_BUILD"].toString().toInt() + 1
    val versionNumber = versionProps["VERSION_NUMBER"].toString().toInt() + value

    versionProps["VERSION_PATCH"] = versionPatch.toString()
    versionProps["VERSION_BUILD"] = versionBuild.toString()
    versionProps["VERSION_NUMBER"] = versionNumber.toString()
    versionPropsFile.writer().use { versionProps.store(it, null) }

    defaultConfig {
        applicationId = "com.dacosys.warehouseCounterM12"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = versionNumber
        versionName = "$versionMajor.$versionMinor.$versionPatch ($versionBuild)"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            proguardFile("proguard-rules.pro")
            proguardFile("proguard-ktor.pro")
        }
    }

    applicationVariants.all {
        sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmCompatibility.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmCompatibility.get())
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmCompatibility.get()
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources.excludes.add("META-INF/*")
        resources.excludes.add("META-INF/LICENSE.md")
        resources.excludes.add("META-INF/LICENSE-notice.md")
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/services/javax.annotation.processing.Processor")
        resources.excludes.add("META-INF/services/org.xmlpull.v1.XmlPullParserFactory")
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable.add("MissingTranslation")
    }

    testOptions {
        unitTests.all {
            it.extensions.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
        unitTests.isReturnDefaultValues = true
    }

    buildToolsVersion = libs.versions.buildToolsVer.get()

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    // Android
    implementation(libs.multidex)
    implementation(libs.constraintlayout)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.swiperefreshlayout)
    implementation(libs.play.services.basement)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.percentlayout)

    // Kotlin
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    coreLibraryDesugaring(libs.desugar)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // KSP
    ksp(libs.ksp.symbol)

    // Koin
    implementation(libs.koin.android)

    // KTor
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.json)
    implementation(libs.ktor.auth)

    // Utilidades
    implementation(libs.keyboardvisibility)
    implementation(libs.commons.net) {
        exclude(group = "xmlpull", module = "xmlpull")
    }
    implementation(libs.async.http) {
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
        exclude(group = "xmlpull", module = "xmlpull")
    }
    implementation(libs.dotenv)
    implementation(libs.javafaker)
    implementation(libs.free.reflection)
    debugImplementation(libs.leakcanary)

    implementation(libs.paging.common)
    implementation(libs.paging.runtime)
    implementation(libs.viewpager2)
    implementation(libs.arrows)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.test)
    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.koin.test)

    // Dacosys
    implementation(libs.imagecontrol)
    implementation(libs.easyfloat)
    implementation(libs.zxingandroid)
    implementation(libs.honeywelllibraryandroid)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmCompatibility.get()))
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all",
                "-Xskip-prerelease-check"
            )
        )
    }
}