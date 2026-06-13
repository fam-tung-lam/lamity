package com.phamtunglam.lamity.downloader.fixtures

import com.phamtunglam.lamity.downloader.models.DownloadRequest

internal fun fakeDownloadRequest(
    id: String = "model-1",
    url: String = "https://huggingface.co/litert-community/m1/resolve/main/m1.litertlm",
    destinationPath: String = "/models/m1.litertlm",
    bearerToken: String? = "token",
    trustedAuthHosts: Set<String> = setOf("huggingface.co"),
    expectedSizeBytes: Long = 0,
    sha256: String? = null,
    requireUnmetered: Boolean = false,
) = DownloadRequest(
    id = id,
    url = url,
    destinationPath = destinationPath,
    bearerToken = bearerToken,
    trustedAuthHosts = trustedAuthHosts,
    expectedSizeBytes = expectedSizeBytes,
    sha256 = sha256,
    requireUnmetered = requireUnmetered,
)
