package com.phamtunglam.lamity.llm.native

/** Carries a [SessionStreamCallback] across the C streaming boundary via a `StableRef`. */
internal class IosSessionContext(val callback: SessionStreamCallback)
