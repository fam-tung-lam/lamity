@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.phamtunglam.lamity.filesystem

import com.phamtunglam.lamity.filesystem.models.LamityFileMetadata
import com.phamtunglam.lamity.filesystem.models.LamityFileOperation
import com.phamtunglam.lamity.filesystem.models.LamityFileSystemException
import com.phamtunglam.lamity.filesystem.models.LamityPath
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory
import platform.Foundation.NSFileTypeRegular
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.posix.memcpy

/** [LamityFileSystem] backed by Foundation's [NSFileManager]. */
internal class IosLamityFileSystem : LamityFileSystem {

    private val fm: NSFileManager get() = NSFileManager.defaultManager

    override fun exists(path: LamityPath): Boolean = fm.fileExistsAtPath(path.value)

    override fun metadataOrNull(path: LamityPath): LamityFileMetadata? {
        val attributes = fm.attributesOfItemAtPath(path.value, null) ?: return null
        val type = attributes[NSFileType] as? String
        val modified = (attributes[NSFileModificationDate] as? NSDate)
            ?.timeIntervalSince1970
            ?.let { (it * 1000.0).toLong() }
        return LamityFileMetadata(
            isRegularFile = type == NSFileTypeRegular,
            isDirectory = type == NSFileTypeDirectory,
            sizeBytes = (attributes[NSFileSize] as? NSNumber)?.longLongValue ?: 0L,
            lastModifiedEpochMillis = modified,
        )
    }

    override fun list(path: LamityPath): List<LamityPath> {
        val names = fm.contentsOfDirectoryAtPath(path.value, null)
            ?: throw LamityFileSystemException(path, LamityFileOperation.List, "not a directory or could not be read")
        return names.mapNotNull { (it as? String)?.let { name -> path / name } }
    }

    override fun createDirectories(path: LamityPath) {
        if (isDirectoryAt(path.value)) return
        val created = fm.createDirectoryAtPath(
            path.value,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        if (!created && !isDirectoryAt(path.value)) {
            throw LamityFileSystemException(path, LamityFileOperation.CreateDirectories, "could not create directory")
        }
    }

    override fun readBytes(path: LamityPath): ByteArray =
        (NSData.dataWithContentsOfFile(path.value)
            ?: throw LamityFileSystemException(path, LamityFileOperation.Read, "could not read file"))
            .toByteArray()

    override fun readText(path: LamityPath): String =
        NSString.stringWithContentsOfFile(path.value, NSUTF8StringEncoding, null)
            ?: throw LamityFileSystemException(path, LamityFileOperation.Read, "could not read file as UTF-8")

    override fun writeBytes(path: LamityPath, bytes: ByteArray, atomically: Boolean) {
        ensureParentExists(path)
        val ok = bytes.toNSData().writeToFile(path.value, atomically = atomically)
        if (!ok) throw LamityFileSystemException(path, LamityFileOperation.Write, "could not write file")
    }

    override fun writeText(path: LamityPath, text: String, atomically: Boolean) {
        ensureParentExists(path)
        @Suppress("CAST_NEVER_SUCCEEDS")
        val ok = (text as NSString).writeToFile(
            path.value,
            atomically = atomically,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        if (!ok) throw LamityFileSystemException(path, LamityFileOperation.Write, "could not write file")
    }

    override fun delete(path: LamityPath, mustExist: Boolean) {
        if (!fm.fileExistsAtPath(path.value)) {
            if (mustExist) throw LamityFileSystemException(path, LamityFileOperation.Delete, "no such file or directory")
            return
        }
        // removeItemAtPath also removes a directory's contents.
        if (!fm.removeItemAtPath(path.value, null)) {
            throw LamityFileSystemException(path, LamityFileOperation.Delete, "could not delete")
        }
    }

    override fun deleteRecursively(path: LamityPath, mustExist: Boolean) = delete(path, mustExist)

    override fun move(source: LamityPath, destination: LamityPath, overwrite: Boolean) {
        if (!fm.fileExistsAtPath(source.value)) {
            throw LamityFileSystemException(source, LamityFileOperation.Move, "source does not exist")
        }
        ensureParentExists(destination)
        if (fm.fileExistsAtPath(destination.value)) {
            if (!overwrite) {
                throw LamityFileSystemException(destination, LamityFileOperation.Move, "destination already exists")
            }
            fm.removeItemAtPath(destination.value, null)
        }
        if (!fm.moveItemAtPath(source.value, toPath = destination.value, error = null)) {
            throw LamityFileSystemException(source, LamityFileOperation.Move, "could not move to '${destination.value}'")
        }
    }

    override fun copy(source: LamityPath, destination: LamityPath, overwrite: Boolean) {
        if (!fm.fileExistsAtPath(source.value)) {
            throw LamityFileSystemException(source, LamityFileOperation.Copy, "source does not exist")
        }
        ensureParentExists(destination)
        if (fm.fileExistsAtPath(destination.value)) {
            if (!overwrite) {
                throw LamityFileSystemException(destination, LamityFileOperation.Copy, "destination already exists")
            }
            fm.removeItemAtPath(destination.value, null)
        }
        if (!fm.copyItemAtPath(source.value, toPath = destination.value, error = null)) {
            throw LamityFileSystemException(source, LamityFileOperation.Copy, "could not copy to '${destination.value}'")
        }
    }

    private fun ensureParentExists(path: LamityPath) {
        val parent = path.parent ?: return
        fm.createDirectoryAtPath(parent.value, withIntermediateDirectories = true, attributes = null, error = null)
    }

    private fun isDirectoryAt(path: String): Boolean =
        (fm.attributesOfItemAtPath(path, null)?.get(NSFileType) as? String) == NSFileTypeDirectory
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}

private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.convert())
    }
}
