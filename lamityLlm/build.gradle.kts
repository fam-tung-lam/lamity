import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        // Kotlin/Native cinterop to the CLiteRTLM C API. Bindings only — the symbols live in the
        // dynamic CLiteRTLM.framework and resolve at app launch (see shared/build.gradle.kts).
        iosTarget.compilations.getByName("main").cinterops.create("litertlm") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/litertlm.def"))
            includeDirs(project.file("src/nativeInterop/cinterop/litertlm"))
        }
        // The only native binaries this library produces are the unit-test executables. They link
        // the iosMain runtime, which references the CLiteRTLM symbols that are otherwise resolved at
        // app launch (see shared/build.gradle.kts). Resolve them dynamically so the test binary
        // links; the common tests never call native code, so the lookup is never exercised at runtime.
        iosTarget.binaries.all {
            linkerOpts("-undefined", "dynamic_lookup")
        }
    }

    androidLibrary {
        namespace = "com.phamtunglam.lamity.llm"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest { }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.litertlm.android)
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
