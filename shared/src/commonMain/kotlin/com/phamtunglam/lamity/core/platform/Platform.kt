package com.phamtunglam.lamity.core.platform

/** Static facts about the device, used by the device_info tool and the About section. */
data class PlatformInfo(
    val platform: String,
    val osVersion: String,
    val deviceModel: String,
)

/** Absolute directories the app may write to. */
data class AppDirs(
    /** App-private data files live here. */
    val dataDir: String,
    /** Downloaded .litertlm model files live here. */
    val modelsDir: String,
    /** Scratch dir handed to the LiteRT-LM engine compilation cache. */
    val cacheDir: String,
)

/** Minimal file system abstraction implemented per platform. */
interface FileIo {
    fun readText(path: String): String?
    fun writeTextAtomic(path: String, text: String): Boolean
    fun delete(path: String)
    fun exists(path: String): Boolean
    fun fileSize(path: String): Long
    fun mkdirs(path: String)
    fun moveFile(from: String, to: String): Boolean
}

fun pathJoin(dir: String, name: String): String = dir.trimEnd('/') + "/" + name
