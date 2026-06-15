package com.phamtunglam.lamity.core.data.db.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.phamtunglam.lamity.core.data.db.entities.ConversationEntity
import com.phamtunglam.lamity.core.data.db.entities.MessageEntity

/** A conversation with its message log (one-to-many: a conversation has many messages). */
data class ConversationWithMessages(
    @Embedded val conversation: ConversationEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["conversation_id"])
    val messages: List<MessageEntity>,
)
