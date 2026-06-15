package com.phamtunglam.lamity.core.data.db.converters

import androidx.room3.TypeConverter
import com.phamtunglam.lamity.feature.models.domain.LlmBackend

/**
 * Persists the [LlmBackend] enum as its name string. Unknown values fall back to [LlmBackend.GPU] so
 * a stale or corrupt row never crashes a read.
 */
class BackendConverter {
    @TypeConverter
    fun backendToString(backend: LlmBackend): String = backend.name

    @TypeConverter
    fun backendFromString(value: String): LlmBackend =
        runCatching { LlmBackend.valueOf(value) }.getOrDefault(LlmBackend.GPU)
}
