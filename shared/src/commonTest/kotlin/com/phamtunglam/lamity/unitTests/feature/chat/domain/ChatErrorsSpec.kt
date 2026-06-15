package com.phamtunglam.lamity.unitTests.feature.chat.domain

import com.phamtunglam.lamity.feature.chat.domain.ChatError
import com.phamtunglam.lamity.feature.chat.domain.ChatErrorKind
import com.phamtunglam.lamity.feature.chat.domain.classifyLoadError
import com.phamtunglam.lamity.feature.chat.domain.isRecoverableGpuFailure
import com.phamtunglam.lamity.feature.chat.domain.loadError
import com.phamtunglam.lamity.feature.chat.domain.shouldFallBackToCpu
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private const val EXECUTOR_ERROR =
    "INTERNAL: ERROR: third_party/odml/litert_lm/runtime/executor/" +
        "llm_litert_compiled_model_executor.cc:755"

class ChatErrorsSpec :
    BehaviorSpec({

        Given("a native GPU compiled-model-executor failure") {
            When("classified") {
                Then("it is a recoverable GPU failure mapped to GPU_LOAD_FAILED") {
                    isRecoverableGpuFailure(EXECUTOR_ERROR) shouldBe true
                    classifyLoadError(EXECUTOR_ERROR) shouldBe ChatErrorKind.GPU_LOAD_FAILED
                }
                Then("a GPU model should fall back to CPU but a CPU model should not") {
                    shouldFallBackToCpu(LlmBackend.GPU, EXECUTOR_ERROR) shouldBe true
                    shouldFallBackToCpu(LlmBackend.CPU, EXECUTOR_ERROR) shouldBe false
                }
            }
        }

        Given("other recoverable GPU delegate failures") {
            Then("RESOURCE_EXHAUSTED, clEnqueueNDRangeKernel and prepare failures are recoverable") {
                isRecoverableGpuFailure(
                    "RESOURCE_EXHAUSTED: Requested allocation size - 2097414144 bytes.",
                ) shouldBe true
                isRecoverableGpuFailure(
                    "Failed to clEnqueueNDRangeKernel - Invalid command queue",
                ) shouldBe true
                isRecoverableGpuFailure(
                    "Node number 0 (BATCH_MATMUL) failed to prepare.",
                ) shouldBe true
            }
        }

        Given("a GPU-only model rejected on the CPU backend") {
            val message = "INVALID_ARGUMENT: main backend section_backend_constraint: gpu"
            Then("it is NOT recoverable on CPU and maps to MODEL_UNSUPPORTED_ON_DEVICE") {
                isRecoverableGpuFailure(message) shouldBe false
                shouldFallBackToCpu(LlmBackend.GPU, message) shouldBe false
                classifyLoadError(message) shouldBe ChatErrorKind.MODEL_UNSUPPORTED_ON_DEVICE
            }
        }

        Given("an unrelated native error") {
            val message = "Conversation is not active"
            Then("it is not recoverable, has no known kind, and is surfaced verbatim") {
                isRecoverableGpuFailure(message) shouldBe false
                classifyLoadError(message) shouldBe null
                loadError(message) shouldBe ChatError.Raw(message)
            }
        }

        Given("a null error message") {
            Then("nothing is recoverable and loadError yields a default raw message") {
                isRecoverableGpuFailure(null) shouldBe false
                classifyLoadError(null) shouldBe null
                loadError(null) shouldBe ChatError.Raw("Could not load model")
            }
        }

        Given("loadError over known kinds") {
            Then("executor errors map to GPU_LOAD_FAILED and backend constraints to unsupported") {
                loadError(EXECUTOR_ERROR) shouldBe ChatError.Known(ChatErrorKind.GPU_LOAD_FAILED)
                loadError("section_backend_constraint: gpu") shouldBe
                    ChatError.Known(ChatErrorKind.MODEL_UNSUPPORTED_ON_DEVICE)
            }
        }
    })
