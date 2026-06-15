// swift-tools-version: 5.9
// Local mirror of https://github.com/google-ai-edge/LiteRT-LM at v0.13.1
// (commit a0afb5a56acd106b23a2b2385b8469834dc268c0), Apache-2.0 — see LICENSE.
//
// This package does ONE job: fetch the checksum-pinned CLiteRTLM.xcframework (a *dynamic*
// framework) and vend it to the iOS app. The LiteRT-LM runtime is consumed as a pure
// Kotlin/Native cinterop port in lamityLlm (src/nativeInterop/cinterop/litertlm.def); there is
// no Swift wrapper. Because the framework is dynamic, dyld loads it at launch and binds its
// `litert_lm_*` exports to the Kotlin `Shared` framework's `-undefined dynamic_lookup`
// references — no `-all_load` force-load is needed (that only affects static archives).

import PackageDescription

let package = Package(
  name: "LiteRTLM",
  platforms: [
    .iOS(.v15)
  ],
  // Product stays named `LiteRTLM` so the app's existing package product dependency
  // (iosApp.xcodeproj) resolves unchanged; it now maps straight to the binary target.
  products: [
    .library(
      name: "LiteRTLM",
      targets: ["CLiteRTLM"]
    )
  ],
  targets: [
    .binaryTarget(
      name: "CLiteRTLM",
      url: "https://github.com/google-ai-edge/LiteRT-LM/releases/download/v0.13.1/CLiteRTLM.xcframework.zip",
      checksum: "7ff01c42106b754748b5dd3036a4a57161b25ebf523e705bebc1219061852362"
    )
  ]
)
