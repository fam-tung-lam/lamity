package com.phamtunglam.lamity.logger.unitTests

import com.phamtunglam.lamity.logger.LamityLogger
import com.phamtunglam.lamity.logger.logWriters.LamityLogWriter
import com.phamtunglam.lamity.logger.models.LamityLogSeverity
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private const val TAG = "TAG"
private const val LOG_MESSAGE = "log message"

class LamityLoggerTest :
    BehaviorSpec({

        // A writer that accepts every record. The backend calls both isLoggable and
        // log during dispatch, so both must be stubbed on the strict mock.
        fun createMockedLogWriter(): LamityLogWriter =
            mock<LamityLogWriter>().also { writer ->
                every { writer.isLoggable(any(), any()) } returns true
                every { writer.log(any(), any(), any(), any()) } returns Unit
            }

        // LamityLogger is a process-wide singleton; wipe whatever a test registered
        // so the next one starts from an empty (and therefore disabled) writer set.
        beforeEach {
            LamityLogger.setLogWriters(emptyList())
        }

        Given("a registered writer") {

            When("a record is logged") {
                Then("the writer receives it with severity translated and a null throwable") {
                    // Arrange
                    val writer = createMockedLogWriter()
                    LamityLogger.addWriter(writer)

                    // Act
                    LamityLogger.i(TAG) { LOG_MESSAGE }

                    // Assert
                    verify(VerifyMode.exactly(1)) {
                        writer.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null)
                    }
                }
            }

            When("a record carries a throwable") {
                Then("the writer receives the same throwable instance") {
                    // Arrange
                    val failure = IllegalStateException("boom")
                    val writer = createMockedLogWriter()
                    LamityLogger.addWriter(writer)

                    // Act
                    LamityLogger.e(TAG, failure) { LOG_MESSAGE }

                    // Assert
                    verify(VerifyMode.exactly(1)) {
                        writer.log(LamityLogSeverity.Error, LOG_MESSAGE, TAG, failure)
                    }
                }
            }

            When("each severity-specific function is invoked") {
                Then("every record carries the matching Lamity severity") {
                    // Arrange
                    val cases: List<Pair<(String, Throwable?, () -> String) -> Unit, LamityLogSeverity>> =
                        listOf(
                            LamityLogger::v to LamityLogSeverity.Verbose,
                            LamityLogger::d to LamityLogSeverity.Debug,
                            LamityLogger::i to LamityLogSeverity.Info,
                            LamityLogger::w to LamityLogSeverity.Warn,
                            LamityLogger::e to LamityLogSeverity.Error,
                        )
                    val writer = createMockedLogWriter()
                    LamityLogger.addWriter(writer)

                    // Act
                    cases.forEach { (logFn, _) -> logFn(TAG, null) { LOG_MESSAGE } }

                    // Assert
                    cases.forEach { (_, severity) ->
                        verify(VerifyMode.exactly(1)) {
                            writer.log(severity, LOG_MESSAGE, TAG, null)
                        }
                    }
                }
            }

            When("the same writer is registered twice") {
                Then("it is registered once and still receives each record only once") {
                    // Arrange
                    val writer = createMockedLogWriter()
                    LamityLogger.addWriter(writer)
                    LamityLogger.addWriter(writer)

                    // Act
                    LamityLogger.i(TAG) { LOG_MESSAGE }

                    // Assert
                    LamityLogger.writers shouldBe listOf(writer)
                    verify(VerifyMode.exactly(1)) {
                        writer.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null)
                    }
                }
            }

            When("several writers are registered") {
                Then("each one receives the record") {
                    // Arrange
                    val firstLogWriter = createMockedLogWriter()
                    val secondLogWriter = createMockedLogWriter()
                    LamityLogger.addWriter(firstLogWriter)
                    LamityLogger.addWriter(secondLogWriter)

                    // Act
                    LamityLogger.i(TAG) { LOG_MESSAGE }

                    // Assert
                    verify(VerifyMode.exactly(1)) { firstLogWriter.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null) }
                    verify(
                        VerifyMode.exactly(1),
                    ) { secondLogWriter.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null) }
                }
            }
        }

        Given("writer removal") {

            When("one of several writers is removed") {
                Then("the removed writer stops receiving while the rest keep working") {
                    // Arrange
                    val keptLogWriter = createMockedLogWriter()
                    val removedLogWriter = createMockedLogWriter()
                    LamityLogger.addWriter(keptLogWriter)
                    LamityLogger.addWriter(removedLogWriter)

                    // Act
                    LamityLogger.removeWriter(removedLogWriter)
                    LamityLogger.i(TAG) { LOG_MESSAGE }

                    // Assert
                    verify(VerifyMode.exactly(1)) { keptLogWriter.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null) }
                    verify(VerifyMode.exactly(0)) { removedLogWriter.log(any(), any(), any(), any()) }
                }
            }
        }

        Given("no writers are registered") {

            When("a record is logged") {
                Then("logging is disabled: the message lambda is never evaluated") {
                    // Arrange
                    var evaluated = false

                    // Act
                    LamityLogger.i(TAG) {
                        evaluated = true
                        LOG_MESSAGE
                    }

                    // Assert
                    evaluated shouldBe false
                }
            }
        }
    })
