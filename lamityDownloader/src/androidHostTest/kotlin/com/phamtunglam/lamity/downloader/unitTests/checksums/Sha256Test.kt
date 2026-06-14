package com.phamtunglam.lamity.downloader.unitTests.checksums

import com.phamtunglam.lamity.downloader.checksums.Sha256
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.File
import okio.Path.Companion.toPath

class Sha256Test : BehaviorSpec({

    Given("a file with known content") {
        When("its digest is computed") {
            Then("it matches the published SHA-256 vector as lowercase hex") {
                val file = File.createTempFile("sha256", ".bin").apply { deleteOnExit() }
                file.writeText("abc")

                Sha256.of(file.absolutePath.toPath()) shouldBe
                    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
            }
        }
    }
})
