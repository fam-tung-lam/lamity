package com.phamtunglam.lamity.filesystem

import com.phamtunglam.lamity.filesystem.models.LamityFileMetadata
import com.phamtunglam.lamity.filesystem.models.LamityFileOperation
import com.phamtunglam.lamity.filesystem.models.LamityFileSystemException
import com.phamtunglam.lamity.filesystem.models.LamityPath
import java.io.File
import java.io.IOException

/** [LamityFileSystem] backed by [java.io.File]. */
internal class AndroidLamityFileSystem : LamityFileSystem {

    override fun exists(path: LamityPath): Boolean = path.file.exists()

    override fun metadataOrNull(path: LamityPath): LamityFileMetadata? {
        val file = path.file
        if (!file.exists()) return null
        return LamityFileMetadata(
            isRegularFile = file.isFile,
            isDirectory = file.isDirectory,
            sizeBytes = file.length(),
            lastModifiedEpochMillis = file.lastModified().takeIf { it > 0L },
        )
    }

    override fun list(path: LamityPath): List<LamityPath> {
        val file = path.file
        val children = file.listFiles()
            ?: throw LamityFileSystemException(
                path,
                LamityFileOperation.List,
                if (file.isDirectory) "directory could not be read" else "not a directory",
            )
        return children.map { LamityPath(it.path) }
    }

    override fun createDirectories(path: LamityPath) {
        val file = path.file
        if (file.isDirectory) return
        if (!file.mkdirs() && !file.isDirectory) {
            throw LamityFileSystemException(path, LamityFileOperation.CreateDirectories, "could not create directory")
        }
    }

    override fun readBytes(path: LamityPath): ByteArray =
        ioCatching(path, LamityFileOperation.Read) { path.file.readBytes() }

    override fun readText(path: LamityPath): String =
        ioCatching(path, LamityFileOperation.Read) { path.file.readText() }

    override fun writeBytes(path: LamityPath, bytes: ByteArray, atomically: Boolean) =
        write(path, atomically) { it.writeBytes(bytes) }

    override fun writeText(path: LamityPath, text: String, atomically: Boolean) =
        write(path, atomically) { it.writeText(text) }

    override fun delete(path: LamityPath, mustExist: Boolean) {
        val file = path.file
        if (!file.exists()) {
            if (mustExist) throw LamityFileSystemException(path, LamityFileOperation.Delete, "no such file or directory")
            return
        }
        if (!file.delete()) {
            throw LamityFileSystemException(
                path,
                LamityFileOperation.Delete,
                if (file.isDirectory) "directory is not empty" else "could not delete file",
            )
        }
    }

    override fun deleteRecursively(path: LamityPath, mustExist: Boolean) {
        val file = path.file
        if (!file.exists()) {
            if (mustExist) throw LamityFileSystemException(path, LamityFileOperation.Delete, "no such file or directory")
            return
        }
        if (!file.deleteRecursively()) {
            throw LamityFileSystemException(path, LamityFileOperation.Delete, "could not delete tree")
        }
    }

    override fun move(source: LamityPath, destination: LamityPath, overwrite: Boolean) {
        val src = source.file
        val dst = destination.file
        if (!src.exists()) {
            throw LamityFileSystemException(source, LamityFileOperation.Move, "source does not exist")
        }
        dst.parentFile?.mkdirs()
        if (dst.exists()) {
            if (!overwrite) {
                throw LamityFileSystemException(destination, LamityFileOperation.Move, "destination already exists")
            }
            if (!dst.deleteRecursively()) {
                throw LamityFileSystemException(destination, LamityFileOperation.Move, "could not replace destination")
            }
        }
        if (src.renameTo(dst)) return
        // renameTo fails across mount points; fall back to copy-then-delete.
        ioCatching(source, LamityFileOperation.Move) {
            src.copyTo(dst, overwrite = true)
            if (!src.deleteRecursively()) throw IOException("could not remove source after copy")
        }
    }

    override fun copy(source: LamityPath, destination: LamityPath, overwrite: Boolean) {
        val src = source.file
        val dst = destination.file
        if (!src.exists()) {
            throw LamityFileSystemException(source, LamityFileOperation.Copy, "source does not exist")
        }
        if (dst.exists() && !overwrite) {
            throw LamityFileSystemException(destination, LamityFileOperation.Copy, "destination already exists")
        }
        dst.parentFile?.mkdirs()
        ioCatching(source, LamityFileOperation.Copy) { src.copyTo(dst, overwrite = overwrite) }
    }

    private inline fun write(path: LamityPath, atomically: Boolean, body: (File) -> Unit) {
        val target = path.file
        ioCatching(path, LamityFileOperation.Write) {
            target.parentFile?.mkdirs()
            if (!atomically) {
                body(target)
                return@ioCatching
            }
            val tmp = File(target.path + ".tmp")
            body(tmp)
            if (target.exists() && !target.delete()) {
                tmp.delete()
                throw IOException("could not replace existing file")
            }
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw IOException("could not commit atomic write")
            }
        }
    }

    /** Runs [body], re-wrapping any non-Lamity failure as a [LamityFileSystemException]. */
    private inline fun <T> ioCatching(path: LamityPath, operation: LamityFileOperation, body: () -> T): T =
        try {
            body()
        } catch (e: LamityFileSystemException) {
            throw e
        } catch (e: Exception) {
            throw LamityFileSystemException(path, operation, e.message, e)
        }

    private val LamityPath.file: File get() = File(value)
}
