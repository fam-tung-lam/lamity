package com.phamtunglam.lamity.filesystem.unitTests.models

import com.phamtunglam.lamity.filesystem.models.LamityPath
import com.phamtunglam.lamity.filesystem.models.toLamityPath
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class LamityPathTest : BehaviorSpec({

    Given("a path joined with a child segment") {
        When("the base has no trailing slash") {
            Then("a single separator is inserted") {
                (LamityPath("/models") / "m1.litertlm").value shouldBe "/models/m1.litertlm"
            }
        }

        When("the base has a trailing slash") {
            Then("the separator is not doubled") {
                (LamityPath("/models/") / "m1.litertlm").value shouldBe "/models/m1.litertlm"
            }
        }

        When("the base is the root") {
            Then("the leading slash is preserved") {
                (LamityPath("/") / "models").value shouldBe "/models"
            }
        }

        When("the child carries a leading slash") {
            Then("the child stays relative to the base") {
                (LamityPath("/a") / "/b").value shouldBe "/a/b"
            }
        }

        When("the base is empty") {
            Then("the result is just the child") {
                (LamityPath("") / "file.txt").value shouldBe "file.txt"
            }
        }

        When("the child is empty") {
            Then("the base is returned unchanged") {
                (LamityPath("/a") / "").value shouldBe "/a"
            }
        }
    }

    Given("a path's name") {
        When("the path is a nested file") {
            Then("it is the last segment") {
                LamityPath("/models/m1.litertlm").name shouldBe "m1.litertlm"
            }
        }

        When("the path is a bare name") {
            Then("it is the whole value") {
                LamityPath("m1.litertlm").name shouldBe "m1.litertlm"
            }
        }

        When("the path has a trailing slash") {
            Then("the trailing slash is ignored") {
                LamityPath("/models/").name shouldBe "models"
            }
        }
    }

    Given("a path's parent") {
        When("the path is nested") {
            Then("it is the enclosing directory") {
                LamityPath("/models/m1.litertlm").parent shouldBe LamityPath("/models")
            }
        }

        When("the path is a top-level entry") {
            Then("the parent is the root") {
                LamityPath("/models").parent shouldBe LamityPath("/")
            }
        }

        When("the path is a bare name") {
            Then("there is no parent") {
                LamityPath("m1.litertlm").parent.shouldBeNull()
            }
        }
    }

    Given("a path's extension and stem") {
        When("the name has a single extension") {
            Then("the extension and stem split on the last dot") {
                LamityPath("/models/m1.litertlm").extension shouldBe "litertlm"
                LamityPath("/models/m1.litertlm").nameWithoutExtension shouldBe "m1"
            }
        }

        When("the name has multiple dots") {
            Then("only the final segment is the extension") {
                LamityPath("/a/archive.tar.gz").extension shouldBe "gz"
                LamityPath("/a/archive.tar.gz").nameWithoutExtension shouldBe "archive.tar"
            }
        }

        When("the name has no extension") {
            Then("the extension is empty and the stem is the whole name") {
                LamityPath("/a/README").extension shouldBe ""
                LamityPath("/a/README").nameWithoutExtension shouldBe "README"
            }
        }
    }

    Given("a path's absoluteness") {
        When("it starts with a slash") {
            Then("it is absolute") {
                LamityPath("/a/b").isAbsolute shouldBe true
            }
        }

        When("it does not start with a slash") {
            Then("it is relative") {
                LamityPath("a/b").isAbsolute shouldBe false
            }
        }
    }

    Given("string interop") {
        When("a string is wrapped and rendered") {
            Then("it round-trips through value and toString") {
                "/models/m1.litertlm".toLamityPath() shouldBe LamityPath("/models/m1.litertlm")
                LamityPath("/models/m1.litertlm").toString() shouldBe "/models/m1.litertlm"
            }
        }
    }
})
