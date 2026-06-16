package com.phamtunglam.lamity.downloader.models

import com.phamtunglam.lamity.downloader.fixtures.fakeDownloadRequest
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class DownloadRequestTest :
    BehaviorSpec({

        Given("a download request with a bearer token") {
            When("redirect hosts are checked for auth trust") {
                Then("it trusts the configured host and its subdomains") {
                    val request = fakeDownloadRequest()

                    request.isAuthTrustedHost("huggingface.co").shouldBeTrue()
                    request.isAuthTrustedHost("cdn.huggingface.co").shouldBeTrue()
                }
                Then("it rejects lookalike and unrelated hosts") {
                    val request = fakeDownloadRequest()

                    request.isAuthTrustedHost("evil-huggingface.co").shouldBeFalse()
                    request.isAuthTrustedHost("s3.amazonaws.com").shouldBeFalse()
                    request.isAuthTrustedHost(null).shouldBeFalse()
                }
                Then("it falls back to the request's own host when no trusted hosts are configured") {
                    val request =
                        fakeDownloadRequest(
                            url = "https://user@host.example.com:8443/path",
                            trustedAuthHosts = emptySet(),
                        )

                    request.isAuthTrustedHost("host.example.com").shouldBeTrue()
                    request.isAuthTrustedHost("s3.amazonaws.com").shouldBeFalse()
                }
            }
        }
    })
