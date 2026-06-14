package com.phamtunglam.lamity.filesystem.unitTests

import com.phamtunglam.lamity.filesystem.extensions.isDirectory
import com.phamtunglam.lamity.filesystem.extensions.readTextOrNull
import com.phamtunglam.lamity.filesystem.lamityFileSystem
import com.phamtunglam.lamity.filesystem.models.LamityFileSystemException
import com.phamtunglam.lamity.filesystem.models.LamityPath
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files

/** Exercises the real [com.phamtunglam.lamity.filesystem.AndroidLamityFileSystem] against a JVM temp directory. */
class AndroidLamityFileSystemTest : BehaviorSpec({

    val fs = lamityFileSystem()
    lateinit var tempDir: File
    var root = LamityPath("")

    beforeEach {
        tempDir = Files.createTempDirectory("lamityfs").toFile()
        root = LamityPath(tempDir.absolutePath)
    }

    afterEach {
        tempDir.deleteRecursively()
    }

    Given("text round-tripping") {
        When("text is written and read back") {
            Then("the content matches") {
                val file = root / "notes.txt"
                fs.writeText(file, "hello world")
                fs.readText(file) shouldBe "hello world"
            }
        }

        When("an atomic write replaces an existing file") {
            Then("the new content wins and no temp file is left behind") {
                val file = root / "notes.txt"
                fs.writeText(file, "first")
                fs.writeText(file, "second")
                fs.readText(file) shouldBe "second"
                fs.exists(LamityPath(file.value + ".tmp")) shouldBe false
            }
        }
    }

    Given("byte round-tripping") {
        When("bytes are written and read back") {
            Then("the content matches") {
                val file = root / "blob.bin"
                val bytes = byteArrayOf(0, 1, 2, 3, 127, -1, -128)
                fs.writeBytes(file, bytes)
                fs.readBytes(file).toList() shouldBe bytes.toList()
            }
        }

        When("an empty byte array is written") {
            Then("an empty file is read back") {
                val file = root / "empty.bin"
                fs.writeBytes(file, ByteArray(0))
                fs.readBytes(file).size shouldBe 0
            }
        }
    }

    Given("metadata and existence") {
        When("a file is written") {
            Then("metadata reports a regular file with the right size") {
                val file = root / "sized.bin"
                fs.writeBytes(file, ByteArray(10))
                fs.exists(file) shouldBe true
                val meta = fs.metadataOrNull(file)
                meta?.isRegularFile shouldBe true
                meta?.isDirectory shouldBe false
                meta?.sizeBytes shouldBe 10L
            }
        }

        When("nothing exists at the path") {
            Then("metadata and best-effort reads are null") {
                val missing = root / "ghost"
                fs.exists(missing) shouldBe false
                fs.metadataOrNull(missing).shouldBeNull()
                fs.readTextOrNull(missing).shouldBeNull()
            }
        }
    }

    Given("directories") {
        When("nested directories are created") {
            Then("the leaf is a directory") {
                val nested = root / "a" / "b" / "c"
                fs.createDirectories(nested)
                fs.isDirectory(nested) shouldBe true
            }
        }

        When("a directory is listed") {
            Then("its immediate children are returned") {
                fs.writeText(root / "a.txt", "a")
                fs.writeText(root / "b.txt", "b")
                fs.createDirectories(root / "sub")
                fs.list(root).map { it.name } shouldContainExactlyInAnyOrder listOf("a.txt", "b.txt", "sub")
            }
        }

        When("listing a non-directory") {
            Then("it throws") {
                val file = root / "a.txt"
                fs.writeText(file, "a")
                shouldThrow<LamityFileSystemException> { fs.list(file) }
            }
        }
    }

    Given("move and copy") {
        When("a file is moved into a new subdirectory") {
            Then("it appears at the destination and not the source") {
                val src = root / "src.txt"
                val dst = root / "moved" / "dst.txt"
                fs.writeText(src, "payload")
                fs.move(src, dst)
                fs.exists(src) shouldBe false
                fs.readText(dst) shouldBe "payload"
            }
        }

        When("a copy targets an existing file without overwrite") {
            Then("it throws and leaves the destination intact") {
                val src = root / "src.txt"
                val dst = root / "dst.txt"
                fs.writeText(src, "new")
                fs.writeText(dst, "old")
                shouldThrow<LamityFileSystemException> { fs.copy(src, dst, overwrite = false) }
                fs.readText(dst) shouldBe "old"
            }
        }
    }

    Given("deletion") {
        When("a file is deleted") {
            Then("it no longer exists") {
                val file = root / "doomed.txt"
                fs.writeText(file, "x")
                fs.delete(file)
                fs.exists(file) shouldBe false
            }
        }

        When("a missing path is deleted with mustExist") {
            Then("it throws") {
                shouldThrow<LamityFileSystemException> { fs.delete(root / "ghost", mustExist = true) }
            }
        }

        When("a non-empty tree is deleted recursively") {
            Then("the whole tree is gone") {
                fs.writeText(root / "tree" / "leaf.txt", "x")
                fs.deleteRecursively(root / "tree")
                fs.exists(root / "tree") shouldBe false
            }
        }
    }
})
