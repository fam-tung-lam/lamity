package com.phamtunglam.lamity.filesystem.models

import kotlin.jvm.JvmInline

/**
 * A file system location, expressed as a POSIX-style path string.
 *
 * Wrapping the raw [value] keeps paths distinct from arbitrary strings at call
 * sites and hangs the common path operations (joining, [name], [parent],
 * [extension]) off a single type. It is a zero-cost `value class`, so it adds
 * no allocation over passing a `String` around.
 *
 * Paths are treated lexically — no entry needs to exist on disk to inspect or
 * derive one — and the segment separator is always `/` (both Android's
 * app-private directories and iOS's `NSDocumentDirectory`/`NSCachesDirectory`
 * hand back absolute POSIX paths).
 */
@JvmInline
value class LamityPath(val value: String) {

    /** The final segment (file or directory name), ignoring any trailing slash. */
    val name: String get() = value.trimEnd('/').substringAfterLast('/')

    /** [name] with its extension removed, e.g. `model.litertlm` -> `model`. */
    val nameWithoutExtension: String get() = name.substringBeforeLast('.')

    /** The extension without the dot, e.g. `model.litertlm` -> `litertlm`; empty when there is none. */
    val extension: String get() = name.substringAfterLast('.', "")

    /** Whether this path is rooted (starts with `/`). */
    val isAbsolute: Boolean get() = value.startsWith('/')

    /**
     * The enclosing directory, or `null` when this path has no separator (a bare
     * name). The parent of a top-level entry such as `/models` is the root `/`.
     */
    val parent: LamityPath?
        get() {
            val trimmed = value.trimEnd('/')
            val separator = trimmed.lastIndexOf('/')
            return when {
                separator < 0 -> null
                separator == 0 -> LamityPath("/")
                else -> LamityPath(trimmed.substring(0, separator))
            }
        }

    /**
     * Resolves [child] against this path, inserting a single separator. Leading
     * slashes on [child] are ignored so the result always stays under this path
     * (`a` / `/b` is `a/b`, not `/b`).
     */
    operator fun div(child: String): LamityPath {
        val segment = child.trimStart('/')
        if (segment.isEmpty()) return this
        return when {
            value.isEmpty() -> LamityPath(segment)
            value.endsWith('/') -> LamityPath(value + segment)
            else -> LamityPath("$value/$segment")
        }
    }

    override fun toString(): String = value
}

/** Wraps this string as a [LamityPath]. */
fun String.toLamityPath(): LamityPath = LamityPath(this)
