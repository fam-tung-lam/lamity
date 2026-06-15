package com.phamtunglam.lamity.llm.native

import kotlinx.cinterop.StableRef

/** Carries the per-turn [TurnCallback] across the C streaming boundary via a [StableRef]. */
internal class IosTurnContext(val callback: TurnCallback)
