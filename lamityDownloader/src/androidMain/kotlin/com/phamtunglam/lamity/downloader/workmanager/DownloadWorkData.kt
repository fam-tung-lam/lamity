package com.phamtunglam.lamity.downloader.workmanager

import com.phamtunglam.lamity.downloader.models.DownloadRequest
import java.io.File

internal object DownloadWorkData {

    /** Extension of the in-flight file next to the destination path. */
    const val PARTIAL_FILE_EXT = "part"

    object Keys {
        const val ID = "id"
        const val STATE = "state"
        const val DOWNLOADED_BYTES = "downloaded_bytes"
        const val TOTAL_BYTES = "total_bytes"
        const val BYTES_PER_SECOND = "bytes_per_second"
        const val ETA_MILLIS = "eta_millis"
        const val ERROR = "error"
    }

    object Tags {
        const val ALL = "lamity_download"
    }
}

internal fun DownloadRequest.partialFile(): File =
    File("$destinationPath.${DownloadWorkData.PARTIAL_FILE_EXT}")
