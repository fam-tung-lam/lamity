package com.phamtunglam.lamity.logger

import com.phamtunglam.lamity.logger.logWriters.LamityLogWriter
import com.phamtunglam.lamity.logger.models.LamityLogSeverity
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private const val TAG = "TAG"
private const val LOG_MESSAGE = "log message"

class LamityLoggerTest :
    BehaviorSpec({

        // The backend queries isLoggable and then calls log during dispatch, so both are
        // stubbed on this strict mock for every leaf that registers it.
        val writer = mock<LamityLogWriter>()

        beforeEach {
            // LamityLogger is a process-wide singleton; start each test from an empty
            // (and therefore disabled) writer set.
            LamityLogger.setLogWriters(emptyList())
            every { writer.isLoggable(any(), any()) } returns true
            every { writer.log(any(), any(), any(), any()) } returns Unit
        }

        afterEach {
            resetAnswers(writer)
            resetCalls(writer)
        }

        Given("a registered writer") {
            When("a record is logged") {
                Then("the writer receives it with the severity translated and a null throwable") {
                    LamityLogger.addWriter(writer)

                    LamityLogger.i(TAG) { LOG_MESSAGE }

                    verify(VerifyMode.exactly(1)) { writer.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null) }
                }
            }

            When("a record carries a throwable") {
                Then("the writer receives the same throwable instance") {
                    val failure = IllegalStateException("boom")
                    LamityLogger.addWriter(writer)

                    LamityLogger.e(TAG, failure) { LOG_MESSAGE }

                    verify(VerifyMode.exactly(1)) { writer.log(LamityLogSeverity.Error, LOG_MESSAGE, TAG, failure) }
                }
            }

            When("each severity-specific function is invoked") {
                Then("every record carries the matching Lamity severity") {
                    val cases: List<Pair<(String, Throwable?, () -> String) -> Unit, LamityLogSeverity>> =
                        listOf(
                            LamityLogger::v to LamityLogSeverity.Verbose,
                            LamityLogger::d to LamityLogSeverity.Debug,
                            LamityLogger::i to LamityLogSeverity.Info,
                            LamityLogger::w to LamityLogSeverity.Warn,
                            LamityLogger::e to LamityLogSeverity.Error,
                        )
                    LamityLogger.addWriter(writer)

                    cases.forEach { (logFn, _) -> logFn(TAG, null) { LOG_MESSAGE } }

                    cases.forEach { (_, severity) ->
                        verify(VerifyMode.exactly(1)) { writer.log(severity, LOG_MESSAGE, TAG, null) }
                    }
                }
            }

            When("the same writer is registered twice") {
                Then("it is registered once and still receives each record only once") {
                    LamityLogger.addWriter(writer)
                    LamityLogger.addWriter(writer)

                    LamityLogger.i(TAG) { LOG_MESSAGE }

                    LamityLogger.writers shouldBe listOf(writer)
                    verify(VerifyMode.exactly(1)) { writer.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null) }
                }
            }

            When("several writers are registered") {
                Then("each one receives the record") {
                    val secondWriter = mock<LamityLogWriter>()
                    every { secondWriter.isLoggable(any(), any()) } returns true
                    every { secondWriter.log(any(), any(), any(), any()) } returns Unit
                    LamityLogger.addWriter(writer)
                    LamityLogger.addWriter(secondWriter)

                    LamityLogger.i(TAG) { LOG_MESSAGE }

                    verify(VerifyMode.exactly(1)) { writer.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null) }
                    verify(VerifyMode.exactly(1)) { secondWriter.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null) }
                }
            }
        }

        Given("a removed writer") {
            When("one of several writers is removed") {
                Then("the removed writer stops receiving while the rest keep working") {
                    val removedWriter = mock<LamityLogWriter>()
                    every { removedWriter.isLoggable(any(), any()) } returns true
                    every { removedWriter.log(any(), any(), any(), any()) } returns Unit
                    LamityLogger.addWriter(writer)
                    LamityLogger.addWriter(removedWriter)

                    LamityLogger.removeWriter(removedWriter)
                    LamityLogger.i(TAG) { LOG_MESSAGE }

                    verify(VerifyMode.exactly(1)) { writer.log(LamityLogSeverity.Info, LOG_MESSAGE, TAG, null) }
                    verify(VerifyMode.exactly(0)) { removedWriter.log(any(), any(), any(), any()) }
                }
            }
        }

        Given("no registered writers") {
            When("a record is logged") {
                Then("logging is disabled and the message lambda is never evaluated") {
                    var evaluated = false

                    LamityLogger.i(TAG) {
                        evaluated = true
                        LOG_MESSAGE
                    }

                    evaluated shouldBe false
                }
            }
        }
    })
