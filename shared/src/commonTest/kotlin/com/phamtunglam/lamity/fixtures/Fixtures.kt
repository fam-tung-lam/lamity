package com.phamtunglam.lamity.fixtures

import com.phamtunglam.lamity.feature.chat.domain.Conversation
import com.phamtunglam.lamity.feature.llmModels.domain.LlmModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.TestCoroutineScheduler

internal fun fakeLlmModel(
    id: String = "model-1",
    url: String = "https://huggingface.co/litert-community/m1/resolve/main/m1.litertlm",
    fileName: String = "m1.litertlm",
    sizeBytes: Long = 1_000,
    requiresAuth: Boolean = false,
) = LlmModel(
    id = id,
    name = "Model $id",
    url = url,
    fileName = fileName,
    sizeBytes = sizeBytes,
    requiresAuth = requiresAuth,
)

internal fun fakeConversation(id: String = "conv-1", title: String = "First chat", updatedAt: Long = 2) =
    Conversation(
        id = id,
        title = title,
        createdAt = 1,
        updatedAt = updatedAt,
    )

/** Scope sharing the test's virtual-time dispatcher, for scope-taking SUTs. */
internal suspend fun testScope(): CoroutineScope = CoroutineScope(currentCoroutineContext())

/**
 * Like [testScope] but with its own [Job], for SUTs that launch never-ending
 * work (eager shareIn/stateIn) the test must not wait for.
 */
internal suspend fun detachedTestScope(): CoroutineScope = CoroutineScope(currentCoroutineContext() + Job())

/** Runs all coroutines queued on the test's virtual-time scheduler. */
internal suspend fun advanceUntilIdle() {
    currentCoroutineContext()[TestCoroutineScheduler]?.advanceUntilIdle()
}
