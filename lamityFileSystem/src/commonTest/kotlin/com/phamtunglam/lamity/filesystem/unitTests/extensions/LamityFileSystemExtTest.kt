package com.phamtunglam.lamity.filesystem.unitTests.extensions

import com.phamtunglam.lamity.filesystem.LamityFileSystem
import com.phamtunglam.lamity.filesystem.extensions.isDirectory
import com.phamtunglam.lamity.filesystem.extensions.isRegularFile
import com.phamtunglam.lamity.filesystem.extensions.listOrEmpty
import com.phamtunglam.lamity.filesystem.extensions.readTextOrNull
import com.phamtunglam.lamity.filesystem.extensions.sizeOrNull
import com.phamtunglam.lamity.filesystem.models.LamityFileMetadata
import com.phamtunglam.lamity.filesystem.models.LamityFileOperation
import com.phamtunglam.lamity.filesystem.models.LamityFileSystemException
import com.phamtunglam.lamity.filesystem.models.LamityPath
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class LamityFileSystemExtTest : BehaviorSpec({

    val fs = mock<LamityFileSystem>()

    afterEach {
        resetAnswers(fs)
        resetCalls(fs)
    }

    fun fileMeta(size: Long = 0L) =
        LamityFileMetadata(isRegularFile = true, isDirectory = false, sizeBytes = size, lastModifiedEpochMillis = null)

    fun dirMeta() =
        LamityFileMetadata(isRegularFile = false, isDirectory = true, sizeBytes = 0L, lastModifiedEpochMillis = null)

    Given("type predicates") {
        When("the entry is a regular file") {
            Then("isRegularFile is true and isDirectory is false") {
                every { fs.metadataOrNull(LamityPath("/a")) } returns fileMeta()
                fs.isRegularFile(LamityPath("/a")) shouldBe true
                fs.isDirectory(LamityPath("/a")) shouldBe false
            }
        }

        When("the entry is a directory") {
            Then("isDirectory is true and isRegularFile is false") {
                every { fs.metadataOrNull(LamityPath("/a")) } returns dirMeta()
                fs.isDirectory(LamityPath("/a")) shouldBe true
                fs.isRegularFile(LamityPath("/a")) shouldBe false
            }
        }

        When("nothing exists at the path") {
            Then("both predicates are false") {
                every { fs.metadataOrNull(LamityPath("/a")) } returns null
                fs.isRegularFile(LamityPath("/a")) shouldBe false
                fs.isDirectory(LamityPath("/a")) shouldBe false
            }
        }
    }

    Given("sizeOrNull") {
        When("the entry exists") {
            Then("it reports the size") {
                every { fs.metadataOrNull(LamityPath("/a")) } returns fileMeta(size = 42L)
                fs.sizeOrNull(LamityPath("/a")) shouldBe 42L
            }
        }

        When("the entry is missing") {
            Then("it is null") {
                every { fs.metadataOrNull(LamityPath("/a")) } returns null
                fs.sizeOrNull(LamityPath("/a")).shouldBeNull()
            }
        }
    }

    Given("readTextOrNull") {
        When("the file exists and reads") {
            Then("it returns the text") {
                every { fs.exists(LamityPath("/a")) } returns true
                every { fs.readText(LamityPath("/a")) } returns "hello"
                fs.readTextOrNull(LamityPath("/a")) shouldBe "hello"
            }
        }

        When("the file is missing") {
            Then("it returns null") {
                every { fs.exists(LamityPath("/a")) } returns false
                fs.readTextOrNull(LamityPath("/a")).shouldBeNull()
            }
        }

        When("the read throws") {
            Then("it swallows the failure and returns null") {
                every { fs.exists(LamityPath("/a")) } returns true
                every { fs.readText(LamityPath("/a")) } throws
                    LamityFileSystemException(LamityPath("/a"), LamityFileOperation.Read)
                fs.readTextOrNull(LamityPath("/a")).shouldBeNull()
            }
        }
    }

    Given("listOrEmpty") {
        When("the path is a directory") {
            Then("it returns the children") {
                every { fs.metadataOrNull(LamityPath("/d")) } returns dirMeta()
                every { fs.list(LamityPath("/d")) } returns listOf(LamityPath("/d/a"), LamityPath("/d/b"))
                fs.listOrEmpty(LamityPath("/d")) shouldContainExactly listOf(LamityPath("/d/a"), LamityPath("/d/b"))
            }
        }

        When("the path is not a directory") {
            Then("it returns an empty list") {
                every { fs.metadataOrNull(LamityPath("/d")) } returns null
                fs.listOrEmpty(LamityPath("/d")) shouldBe emptyList()
            }
        }
    }
})
