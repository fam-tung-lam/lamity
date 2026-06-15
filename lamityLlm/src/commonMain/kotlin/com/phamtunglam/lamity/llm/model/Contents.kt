package com.phamtunglam.lamity.llm.model

/** An ordered collection of [Content]. */
class Contents(val values: List<Content>) {
    val isEmpty: Boolean get() = values.isEmpty()

    /** Concatenated text of all [Content.Text] items, space separated. */
    val text: String get() = values.filterIsInstance<Content.Text>().joinToString(" ") { it.text }

    companion object {
        val empty: Contents = Contents(emptyList())

        fun text(text: String): Contents = Contents(listOf(Content.Text(text)))

        fun of(vararg contents: Content): Contents = Contents(contents.toList())
    }
}
