package com.phamtunglam.lamity.filesystem.unitTests.models

import com.phamtunglam.lamity.filesystem.models.LamityFileOperation
import com.phamtunglam.lamity.filesystem.models.LamityFileSystemException
import com.phamtunglam.lamity.filesystem.models.LamityPath
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class LamityFileSystemExceptionTest : BehaviorSpec({

    Given("a failure with a detail and cause") {
        val cause = IllegalStateException("disk full")
        val exception = LamityFileSystemException(
            path = LamityPath("/models/m1.litertlm"),
            operation = LamityFileOperation.Write,
            detail = "could not write file",
            cause = cause,
        )

        When("its message is read") {
            Then("it names the operation, path and detail") {
                exception.message shouldContain "Write"
                exception.message shouldContain "/models/m1.litertlm"
                exception.message shouldContain "could not write file"
            }
        }

        When("its fields are inspected") {
            Then("they carry the operation, path and cause") {
                exception.operation shouldBe LamityFileOperation.Write
                exception.path shouldBe LamityPath("/models/m1.litertlm")
                exception.cause shouldBe cause
            }
        }
    }

    Given("a failure without a detail") {
        val exception = LamityFileSystemException(
            path = LamityPath("/tmp/x"),
            operation = LamityFileOperation.Delete,
        )

        When("its message is read") {
            Then("it still names the operation and path") {
                exception.message shouldContain "Delete"
                exception.message shouldContain "/tmp/x"
            }
        }
    }
})
