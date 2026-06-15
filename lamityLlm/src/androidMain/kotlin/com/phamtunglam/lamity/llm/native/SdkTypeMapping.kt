package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.Backend as SdkBackend
import com.google.ai.edge.litertlm.Content as SdkContent
import com.google.ai.edge.litertlm.Contents as SdkContents
import com.google.ai.edge.litertlm.Message as SdkMessage
import com.google.ai.edge.litertlm.Role as SdkRole
import com.google.ai.edge.litertlm.ToolCall as SdkToolCall
import com.phamtunglam.lamity.llm.model.Backend
import com.phamtunglam.lamity.llm.model.Content
import com.phamtunglam.lamity.llm.model.Contents
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.Role
import com.phamtunglam.lamity.llm.model.ToolCall

internal fun Backend.toSdk(): SdkBackend =
    when (this) {
        is Backend.Gpu -> SdkBackend.GPU()
        is Backend.Cpu -> SdkBackend.CPU(threadCount)
    }

internal fun Role.toSdk(): SdkRole =
    when (this) {
        Role.System -> SdkRole.SYSTEM
        Role.User -> SdkRole.USER
        Role.Model -> SdkRole.MODEL
        Role.Tool -> SdkRole.TOOL
    }

internal fun SdkRole.toCommon(): Role =
    when (this) {
        SdkRole.SYSTEM -> Role.System
        SdkRole.USER -> Role.User
        SdkRole.MODEL -> Role.Model
        SdkRole.TOOL -> Role.Tool
    }

internal fun Contents.toSdk(): SdkContents = SdkContents.of(values.map { it.toSdk() })

internal fun Content.toSdk(): SdkContent =
    when (this) {
        is Content.Text -> SdkContent.Text(text)
        is Content.ImageBytes -> SdkContent.ImageBytes(bytes)
        is Content.ImageFile -> SdkContent.ImageFile(path)
        is Content.AudioBytes -> SdkContent.AudioBytes(bytes)
        is Content.AudioFile -> SdkContent.AudioFile(path)
        is Content.ToolResponse -> SdkContent.ToolResponse(name, jsonElementToAny(response))
    }

internal fun Message.toSdk(): SdkMessage {
    val sdkContents = contents.toSdk()
    return when (role) {
        Role.System -> {
            SdkMessage.system(sdkContents)
        }

        Role.User -> {
            SdkMessage.user(sdkContents)
        }

        Role.Tool -> {
            SdkMessage.tool(sdkContents)
        }

        Role.Model -> {
            SdkMessage.model(
                sdkContents,
                toolCalls.map { SdkToolCall(it.name, jsonObjectToMap(it.arguments)) },
                channels,
            )
        }
    }
}

internal fun SdkMessage.toCommon(): Message =
    Message(
        role = role.toCommon(),
        contents = Contents(contents.contents.mapNotNull { it.toCommonOrNull() }),
        toolCalls = toolCalls.map { ToolCall(it.name, mapToJsonObject(it.arguments)) },
        channels = channels,
    )

internal fun SdkContent.toCommonOrNull(): Content? =
    when (this) {
        is SdkContent.Text -> Content.Text(text)
        is SdkContent.ToolResponse -> Content.ToolResponse(name, anyToJsonElement(response))
        else -> null
    }
