// swift-tools-version: 5.9
// Local mirror of https://github.com/google-ai-edge/LiteRT-LM at v0.13.1
// (commit a0afb5a56acd106b23a2b2385b8469834dc268c0), Apache-2.0 — see LICENSE.
//
// Vendored as a local package because the upstream manifest uses
// `unsafeFlags(["-Xlinker", "-all_load"])`, which Swift Package Manager
// rejects for remote (versioned or revision-pinned) dependencies. Local
// packages are exempt. The heavy native code still comes from the official
// prebuilt binary artifact below; only the thin Swift wrapper sources in
// ./swift are copied.

import PackageDescription

let package = Package(
  name: "LiteRTLM",
  platforms: [
    .iOS(.v15)
  ],
  products: [
    .library(
      name: "LiteRTLM",
      targets: ["LiteRTLM"]
    )
  ],
  targets: [
    // The official prebuilt binary for iOS.
    .binaryTarget(
      name: "CLiteRTLM",
      url: "https://github.com/google-ai-edge/LiteRT-LM/releases/download/v0.13.1/CLiteRTLM.xcframework.zip",
      checksum: "7ff01c42106b754748b5dd3036a4a57161b25ebf523e705bebc1219061852362"
    ),
    // The Swift wrapper around the binary.
    .target(
      name: "LiteRTLM",
      dependencies: [
        .target(name: "CLiteRTLM", condition: .when(platforms: [.iOS]))
      ],
      path: "swift",
      linkerSettings: [
        .unsafeFlags(["-Xlinker", "-all_load"])
      ]
    ),
  ]
)
