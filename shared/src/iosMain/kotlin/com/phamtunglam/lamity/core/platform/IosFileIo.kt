@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

class IosFileIo : FileIo {

    private val fm get() = NSFileManager.defaultManager

    override fun readText(path: String): String? =
        NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)

    override fun writeTextAtomic(path: String, text: String): Boolean {
        @Suppress("CAST_NEVER_SUCCEEDS")
        return (text as NSString).writeToFile(
            path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
    }

    override fun delete(path: String) {
        fm.removeItemAtPath(path, null)
    }

    override fun exists(path: String): Boolean = fm.fileExistsAtPath(path)

    override fun fileSize(path: String): Long {
        val attributes = fm.attributesOfItemAtPath(path, null) ?: return 0L
        return (attributes[NSFileSize] as? NSNumber)?.longLongValue ?: 0L
    }

    override fun mkdirs(path: String) {
        fm.createDirectoryAtPath(
            path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }

    override fun moveFile(from: String, to: String): Boolean {
        fm.removeItemAtPath(to, null)
        return fm.moveItemAtPath(from, toPath = to, error = null)
    }
}
