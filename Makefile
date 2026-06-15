# ============================================================================
# Variables
# ============================================================================

GRADLE             = ./gradlew
RERUN              = --rerun
RERUN_TASKS        = --rerun-tasks

ANDROID_APP        = :androidApp
SHARED             = :shared
LAMITY_DB          = :lamityDb
LAMITY_DOWNLOADER  = :lamityDownloader
LAMITY_LLM         = :lamityLlm
LAMITY_LOGGER      = :lamityLogger
LAMITY_CRASH       = :lamityCrashReporter

# ============================================================================
# Static analysis
# ============================================================================

# KMP modules carry the Detekt plugin (applied to org.jetbrains.kotlin.multiplatform projects).
LINT_KMP_CHECKS = $(SHARED):check $(LAMITY_DB):check $(LAMITY_DOWNLOADER):check \
        $(LAMITY_LLM):check $(LAMITY_LOGGER):check $(LAMITY_CRASH):check

# Keep `make lint` focused on static analysis: skip the slower unit-test tasks.
LINT_TEST_EXCLUDES = -x testAndroidHostTest -x iosSimulatorArm64Test

lint-ktlint:
	$(GRADLE) ktlintCheck

lint-ktlint-format:
	$(GRADLE) ktlintFormat

lint-detekt:
	$(GRADLE) $(LINT_KMP_CHECKS) $(LINT_TEST_EXCLUDES) -x ktlintCheck $(RERUN_TASKS)

lint:
	$(GRADLE) ktlintCheck $(LINT_KMP_CHECKS) $(LINT_TEST_EXCLUDES) $(RERUN_TASKS)

# ============================================================================
# Tests
# ============================================================================

# These KMP modules opt into unit tests (withHostTest + commonTest sources);
# only lamityDb has no test sources, so it is excluded here.
TEST_KMP_ANDROID = $(SHARED):testAndroidHostTest $(LAMITY_DOWNLOADER):testAndroidHostTest \
        $(LAMITY_LOGGER):testAndroidHostTest $(LAMITY_CRASH):testAndroidHostTest \
        $(LAMITY_LLM):testAndroidHostTest
TEST_KMP_IOS = $(SHARED):iosSimulatorArm64Test $(LAMITY_DOWNLOADER):iosSimulatorArm64Test \
        $(LAMITY_LOGGER):iosSimulatorArm64Test $(LAMITY_CRASH):iosSimulatorArm64Test \
        $(LAMITY_LLM):iosSimulatorArm64Test

# ---- Compile-only fast feedback ----

# Typecheck shared production code (commonMain) across every KMP module.
test-typecheck-common:
	$(GRADLE) $(SHARED):compileCommonMainKotlinMetadata $(LAMITY_DB):compileCommonMainKotlinMetadata \
        $(LAMITY_DOWNLOADER):compileCommonMainKotlinMetadata $(LAMITY_LLM):compileCommonMainKotlinMetadata \
        $(LAMITY_LOGGER):compileCommonMainKotlinMetadata $(LAMITY_CRASH):compileCommonMainKotlinMetadata $(RERUN_TASKS)

# Typecheck Android unit-test sources (app + host-test KMP modules).
test-typecheck-android:
	$(GRADLE) $(ANDROID_APP):compileDebugUnitTestKotlin $(SHARED):compileAndroidHostTest \
        $(LAMITY_DOWNLOADER):compileAndroidHostTest $(LAMITY_LOGGER):compileAndroidHostTest \
        $(LAMITY_CRASH):compileAndroidHostTest $(LAMITY_LLM):compileAndroidHostTest $(RERUN_TASKS)

# Typecheck iOS unit-test sources.
test-typecheck-ios:
	$(GRADLE) $(SHARED):compileTestKotlinIosSimulatorArm64 $(LAMITY_DOWNLOADER):compileTestKotlinIosSimulatorArm64 \
        $(LAMITY_LOGGER):compileTestKotlinIosSimulatorArm64 $(LAMITY_CRASH):compileTestKotlinIosSimulatorArm64 \
        $(LAMITY_LLM):compileTestKotlinIosSimulatorArm64 $(RERUN_TASKS)

# ---- Unit tests ----

test-android:
	$(GRADLE) $(ANDROID_APP):testDebugUnitTest $(TEST_KMP_ANDROID) $(RERUN)

test-ios:
	$(GRADLE) $(TEST_KMP_IOS) $(RERUN)

test-all:
	$(GRADLE) $(ANDROID_APP):testDebugUnitTest $(TEST_KMP_ANDROID) $(TEST_KMP_IOS) $(RERUN)

# ---- Per-module tests ----
#
# `test-<module>` runs that module's Android + iOS unit tests in one Gradle
# invocation. The `-common` / `-android` / `-ios` variants scope to one source
# set: `-android` and `-ios` RUN tests (each also compiles + runs commonTest),
# while `-common` is a typecheck of the module's commonMain
# (compileCommonMainKotlinMetadata) — common test code has no standalone runner,
# it executes under both platforms.

# shared
test-shared:
	$(GRADLE) $(SHARED):testAndroidHostTest $(SHARED):iosSimulatorArm64Test $(RERUN)
test-shared-common:
	$(GRADLE) $(SHARED):compileCommonMainKotlinMetadata $(RERUN_TASKS)
test-shared-android:
	$(GRADLE) $(SHARED):testAndroidHostTest $(RERUN)
test-shared-ios:
	$(GRADLE) $(SHARED):iosSimulatorArm64Test $(RERUN)

# lamityDownloader
test-downloader:
	$(GRADLE) $(LAMITY_DOWNLOADER):testAndroidHostTest $(LAMITY_DOWNLOADER):iosSimulatorArm64Test $(RERUN)
test-downloader-common:
	$(GRADLE) $(LAMITY_DOWNLOADER):compileCommonMainKotlinMetadata $(RERUN_TASKS)
test-downloader-android:
	$(GRADLE) $(LAMITY_DOWNLOADER):testAndroidHostTest $(RERUN)
test-downloader-ios:
	$(GRADLE) $(LAMITY_DOWNLOADER):iosSimulatorArm64Test $(RERUN)

# lamityLogger
test-logger:
	$(GRADLE) $(LAMITY_LOGGER):testAndroidHostTest $(LAMITY_LOGGER):iosSimulatorArm64Test $(RERUN)
test-logger-common:
	$(GRADLE) $(LAMITY_LOGGER):compileCommonMainKotlinMetadata $(RERUN_TASKS)
test-logger-android:
	$(GRADLE) $(LAMITY_LOGGER):testAndroidHostTest $(RERUN)
test-logger-ios:
	$(GRADLE) $(LAMITY_LOGGER):iosSimulatorArm64Test $(RERUN)

# lamityCrashReporter
test-crash:
	$(GRADLE) $(LAMITY_CRASH):testAndroidHostTest $(LAMITY_CRASH):iosSimulatorArm64Test $(RERUN)
test-crash-common:
	$(GRADLE) $(LAMITY_CRASH):compileCommonMainKotlinMetadata $(RERUN_TASKS)
test-crash-android:
	$(GRADLE) $(LAMITY_CRASH):testAndroidHostTest $(RERUN)
test-crash-ios:
	$(GRADLE) $(LAMITY_CRASH):iosSimulatorArm64Test $(RERUN)

# lamityLlm
test-llm:
	$(GRADLE) $(LAMITY_LLM):testAndroidHostTest $(LAMITY_LLM):iosSimulatorArm64Test $(RERUN)
test-llm-common:
	$(GRADLE) $(LAMITY_LLM):compileCommonMainKotlinMetadata $(RERUN_TASKS)
test-llm-android:
	$(GRADLE) $(LAMITY_LLM):testAndroidHostTest $(RERUN)
test-llm-ios:
	$(GRADLE) $(LAMITY_LLM):iosSimulatorArm64Test $(RERUN)

# lamityDb has no unit tests (no host-test/commonTest sources);
# only the commonMain typecheck applies.
test-db-common:
	$(GRADLE) $(LAMITY_DB):compileCommonMainKotlinMetadata $(RERUN_TASKS)

# androidApp is an Android application module — JVM unit tests only.
test-app:
	$(GRADLE) $(ANDROID_APP):testDebugUnitTest $(RERUN)

.PHONY: lint lint-ktlint lint-ktlint-format lint-detekt \
        test-typecheck-common test-typecheck-android test-typecheck-ios \
        test-android test-ios test-all \
        test-shared test-shared-common test-shared-android test-shared-ios \
        test-downloader test-downloader-common test-downloader-android test-downloader-ios \
        test-logger test-logger-common test-logger-android test-logger-ios \
        test-crash test-crash-common test-crash-android test-crash-ios \
        test-llm test-llm-common test-llm-android test-llm-ios \
        test-db-common test-app
