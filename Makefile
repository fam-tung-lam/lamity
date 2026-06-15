# ============================================================================
# Static analysis
# ============================================================================

GRADLE             = ./gradlew
RERUN_TASKS        = --rerun-tasks

SHARED             = :shared
LAMITY_DB          = :lamityDb
LAMITY_DOWNLOADER  = :lamityDownloader
LAMITY_LLM         = :lamityLlm
LAMITY_LOGGER      = :lamityLogger
LAMITY_CRASH       = :lamityCrashReporter

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

.PHONY: lint lint-ktlint lint-ktlint-format lint-detekt
