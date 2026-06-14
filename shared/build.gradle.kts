import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.sentryKmpPlugin)
}

// Links the Shared framework against Sentry Cocoa (resolved by Xcode from the
// sentry-cocoa Swift package; Sentry-Dynamic is required for dynamic frameworks).
sentryKmp {
    autoInstall.commonMain.enabled.set(false) // dependency lives in :lamityCrashReporter
    linker.xcodeprojPath.set("$rootDir/iosApp/iosApp.xcodeproj")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            // Dynamic: the LiteRT-LM Swift package links the app with -all_load,
            // which would force-load duplicate objects from a static Kotlin framework.
            isStatic = false
            // The Swift side implements NativeLlmBridge and
            // LamityDownloaderBridge, so those modules' APIs must be visible
            // in the framework header.
            export(projects.lamityLlm)
            export(projects.lamityDownloader)
        }
    }

    androidLibrary {
       namespace = "com.phamtunglam.lamity.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            api(projects.lamityLlm)
            api(projects.lamityDownloader)
            implementation(projects.lamityCrashReporter)
            implementation(projects.lamityLogger)
            implementation(projects.lamityDb)
            implementation(projects.lamityFileSystem)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.materialIconsCore)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotest.runner.junit5)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
