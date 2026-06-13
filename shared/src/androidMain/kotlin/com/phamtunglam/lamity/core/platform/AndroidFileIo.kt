package com.phamtunglam.lamity.core.platform

import java.io.File

class AndroidFileIo : FileIo {

    override fun readText(path: String): String? {
        val file = File(path)
        return if (file.isFile) runCatching { file.readText() }.getOrNull() else null
    }

    override fun writeTextAtomic(path: String, text: String): Boolean = runCatching {
        val target = File(path)
        target.parentFile?.mkdirs()
        val tmp = File("$path.tmp")
        tmp.writeText(text)
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }.getOrDefault(false)

    override fun delete(path: String) {
        runCatching { File(path).delete() }
    }

    override fun exists(path: String): Boolean = File(path).exists()

    override fun fileSize(path: String): Long = File(path).length()

    override fun mkdirs(path: String) {
        File(path).mkdirs()
    }

    override fun moveFile(from: String, to: String): Boolean = runCatching {
        val src = File(from)
        val dst = File(to)
        dst.parentFile?.mkdirs()
        if (dst.exists()) dst.delete()
        if (src.renameTo(dst)) true
        else {
            src.copyTo(dst, overwrite = true)
            src.delete()
            true
        }
    }.getOrDefault(false)
}
