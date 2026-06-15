import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Secrets are injected from local.properties (never checked in) into the app's
// generated BuildConfig; LamityBuildConfig reads them back reflectively at runtime.
private val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.isFile) {
            localPropertiesFile.inputStream().use(::load)
        }
    }

// SENTRY_DNS is accepted as a legacy spelling fallback for SENTRY_DSN.
private fun Properties.sentryDsnBuildConfigString(): String =
    (getProperty("SENTRY_DSN") ?: getProperty("SENTRY_DNS")).orEmpty().toBuildConfigStringLiteral()

private fun Properties.hfTokenBuildConfigString(): String =
    getProperty("HF_TOKEN").orEmpty().toBuildConfigStringLiteral()

// buildConfigField takes a raw Java literal, so the value must be a quoted,
// escaped string. Values already wrapped in quotes are passed through as-is.
private fun String.toBuildConfigStringLiteral(): String {
    val trimmed = trim()
    if (trimmed.length >= 2 && trimmed.first() == '"' && trimmed.last() == '"') return trimmed
    return "\"" + trimmed.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.phamtunglam.lamity"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.phamtunglam.lamity"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SENTRY_DSN", localProperties.sentryDsnBuildConfigString())
        buildConfigField("String", "HF_TOKEN", localProperties.hfTokenBuildConfigString())
    }
    buildFeatures {
        // shared/core/LamityBuildConfig.android.kt reads this BuildConfig (versions +
        // the injected SENTRY_DSN/HF_TOKEN secrets) reflectively.
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
