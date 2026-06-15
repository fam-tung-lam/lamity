import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.kotest) apply false
    alias(libs.plugins.mokkery) apply false
    alias(libs.plugins.sentryKmpPlugin) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint)
}

// ktlint runs against the bundled engine version (decoupled from the Gradle plugin).
val ktlintVersion: String =
    libs.versions.ktlint.engine
        .get()

extensions.configure<KtlintExtension> {
    version.set(ktlintVersion)
    android.set(true)
    outputToConsole.set(true)
    filter {
        exclude { treeElement ->
            treeElement.file.absolutePath.contains("/build/")
        }
    }
}

// Aggregate verification entry point at the root (e.g. ./gradlew check).
tasks.register("check") {
    group = "verification"
    description = "Runs root and subproject verification tasks."
    dependsOn(tasks.named("ktlintCheck"))
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // Kotest specs run on the JVM host-test tasks through the JUnit Platform;
    // configured once here instead of in every module.
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }

    // Detekt only targets KMP modules; the android application module is linted by ktlint.
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "dev.detekt")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            parallel = true
            basePath.set(rootProject.projectDir)
        }

        tasks.withType<Detekt>().configureEach {
            jvmTarget.set("11")
            exclude { treeElement ->
                treeElement.file.absolutePath.contains("/build/generated/")
            }
            reports {
                checkstyle.required.set(true)
                html.required.set(true)
                sarif.required.set(true)
                markdown.required.set(false)
            }
        }

        tasks.withType<DetektCreateBaselineTask>().configureEach {
            jvmTarget.set("11")
            exclude { treeElement ->
                treeElement.file.absolutePath.contains("/build/generated/")
            }
        }

        tasks.matching { it.name == "check" }.configureEach {
            dependsOn(tasks.withType<Detekt>().matching { it.name.endsWith("SourceSet") })
        }
    }

    extensions.configure<KtlintExtension> {
        version.set(ktlintVersion)
        android.set(true)
        outputToConsole.set(true)
        filter {
            exclude { treeElement ->
                treeElement.file.absolutePath.contains("/build/")
            }
            include("**/src/**")
        }
    }
}
