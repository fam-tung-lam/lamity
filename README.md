# Lamity

**An open-source reference implementation for integrating an on-device LLM into a
Kotlin Multiplatform app — the proper way.**

Lamity is a complete, production-shaped Android + iOS chat app whose inference runs
**fully on-device** through [Google LiteRT-LM](https://developers.google.com/edge/litert-lm/),
with all UI and business logic shared via Compose Multiplatform. It is published as a
learning resource: a worked example you can read, copy, and adapt — not a product.

Licensed under the [MIT License](LICENSE).

|               |                                                                          |
|---------------|--------------------------------------------------------------------------|
| **Platforms** | Android (Kotlin/JVM) · iOS (Kotlin/Native)                               |
| **UI**        | Compose Multiplatform (100% shared)                                      |
| **Inference** | LiteRT-LM `0.13.1`, fully on-device — no server, no network at chat time |
| **Language**  | Kotlin `2.4.0`                                                           |

---

## What "the proper way" looks like here

These are the decisions worth copying — each is demonstrated end-to-end in the codebase:

- **One idiomatic Kotlin API over the native runtime.** [`lamityLlm`](lamityLlm/) wraps
  LiteRT-LM behind coroutines and `Flow` — no native callbacks, no platform types leak to
  callers. The *same* `commonMain` code drives Android (the `litertlm` AAR) and iOS
  (Kotlin/Native cinterop straight to the CLiteRTLM C API). **This module is the heart of
  the project — [start with its README](lamityLlm/README.md).**
- **The chat logic lives in common Kotlin.** The multi-turn tool-calling loop, message
  merging, and JSON wire format are platform-agnostic. The native runtimes only surface
  one streamed turn at a time; they never execute tools. So a `set_theme` tool call
  changes the UI identically on both OSes.
- **Streaming with real cancellation.** Generation is a cold `Flow`; cancelling the
  collecting coroutine tears down the native turn through structured concurrency.
- **Downloads that survive app death.** Multi-gigabyte model files transfer via
  WorkManager (Android) and a background `URLSession` (iOS) — with pause/resume, live
  speed/ETA, optional SHA-256 verification, and a Wi-Fi-only switch. See
  [`lamityDownloader`](lamityDownloader/README.md).
- **Strict module boundaries and a `data / domain / presentation` split** per feature,
  wired with Koin, persisted with Room KMP, logged through one Kermit facade, and reported
  through a single crash-reporter facade.

---

## What it does

A focused chat app — four screens (Chats → Chat → Models → Settings), stack navigation:

1. **On-device chat** — streaming responses, stop-generation, per-message throughput
   (≈ tok/s), a thinking panel for reasoning models, and inline tool-call cards. Resumed
   conversations are replayed natively via LiteRT-LM `initialMessages`.
2. **Model catalog** — curated `.litertlm` models from the HuggingFace
   [litert-community](https://huggingface.co/litert-community):
    - *Qwen2.5 1.5B Instruct* — solid all-rounder, good tool calling, no token needed
    - *Gemma 3 1B IT* — small & fast, gated (HuggingFace token required)
    - *Gemma 4 E2B / E4B / 12B* — increasing quality, increasing device demands
    - *DeepSeek R1 Distill 1.5B* — streams its reasoning before answering
3. **Background downloads** — survive app death; pause/resume (HTTP Range / resume data),
   live speed + ETA, optional SHA-256 verification, Android progress notification, Wi-Fi-only
   restriction, and HuggingFace token support for gated models (the token is only sent to
   trusted hosts, never to redirect targets).
4. **Built-in tools** (per-chat on/off toggles): `get_current_time`, `calculate`,
   `set_theme`, `set_language` (en/vi/es — changes the app UI live), `random_number`,
   `device_info`.
5. **Built-in skills** with Claude-style progressive disclosure — the system prompt only
   advertises name + description; the model pulls full instructions through the implicit
   `load_skill` tool. Ships *Haiku Mode* and *Step-by-step Math*, toggled per chat.
6. **Per-chat customization** — inference parameters (backend, max tokens, top-K, top-P,
   temperature), an optional system prompt, and tool/skill toggles, held in-memory for that
   chat.
7. **Conversation history** — Room-persisted on-device; resume, rename, delete.
8. **Runtime i18n & theming** — English / Tiếng Việt / Español and light/dark/system,
   switchable live (including by the model via `set_language` / `set_theme`).
9. **Crash reporting** — Sentry KMP behind a `CrashReporter` facade; Kermit logs become
   breadcrumbs. A blank DSN fully disables it.

---

## Architecture

### Modules

```
androidApp/            Android entry point (Application, MainActivity, manifest)
iosApp/                Xcode project (SwiftUI entry + background-download Swift bridge)
  iosApp/Downloader/     Background-URLSession downloader bridge + AppDelegate wiring
  iosApp/LiteRTLM/       Vendored SPM package — pins the prebuilt CLiteRTLM.xcframework (v0.13.1)
shared/                All app logic + Compose UI (also builds the iOS "Shared" framework)
lamityLlm/             Idiomatic KMP wrapper over LiteRT-LM — the integration reference (own README)
lamityDownloader/      Background file downloads: WorkManager / URLSession (own README)
lamityCrashReporter/   Sentry KMP facade (CrashReporter + breadcrumb log writer)
lamityLogger/          Kermit re-export so every module logs the same way
```

### Inside `shared`

```
com/phamtunglam/lamity/
  App.kt                 Compose root: theme + locale + Navigation 3 stack
  core/
    data/                Room KMP DB (db/: entities, daos, relations) + PreferenceDataStore + logging
    di/                  Koin modules (appModule + db + expect/actual platform)
    domain/platform/     AppDirs, PlatformInfo, time/ids (expect/actual)
    presentation/        designSystem/ (theme + reusable components) + navigation/ (Nav3 keys)
    LamityBuildConfig.kt App-level build config (e.g. crash-reporter DSN, default HF token)
  feature/
    chat/                data / domain (session factory + state) / presentation (+components/)
    history/             domain / presentation — the chats list, which is the app home
    models/              data / domain (catalog + download use cases) / presentation (+components/)
    settings/            data / domain / presentation
    localization/        data / domain / presentation — runtime en/vi/es switching
    skills/              domain — code-defined BuiltinSkills, per-chat toggles
    tools/               domain (built-in tools + Calculator) + di
```

Each feature keeps the same `data / domain / presentation` split; cross-feature UI lives in
`core/presentation/designSystem`, screen-specific composables in each feature's
`presentation/components/`.

### Platform boundaries

Almost everything is common code. The LLM runtime is bound **in Kotlin on both platforms**,
so the only remaining Kotlin↔Swift bridge is the iOS background downloader:

```
ChatSession ─> lamityLlm Engine / Conversation        (commonMain — chat + tool loop)
                 ├─ androidMain: binds com.google.ai.edge.litertlm (AAR)
                 └─ iosMain:     Kotlin/Native cinterop → CLiteRTLM C API
                                  (iosApp/LiteRTLM vends the prebuilt xcframework)

ModelDownloadManager ─> Downloader                    (lamityDownloader, commonMain)
                          ├─ androidMain: AndroidDownloader → WorkManager worker
                          └─ iosMain:     IosDownloader → LamityDownloaderBridge
                                           └─ iosApp/Downloader: background URLSession

Tool calls ─────────────> executed by shared Kotlin (lamityLlm's in-conversation tool loop
                          runs the app's built-in tools), so theme/language switches and
                          tool-event chat cards behave identically on both OSes.
```

---

## A note on `lamityLlm` (and why it isn't a separate library)

`lamityLlm` is the part most people will want to lift into their own project, and it is
written to make that easy: it is self-contained, depends only on coroutines + serialization
(and the logger facade), and ships its own [library-grade README](lamityLlm/README.md) with
a full API reference.

It is **deliberately not published as a standalone artifact or repo.** Maintaining a
separate library is an ongoing commitment — releases, versioning, keeping the vendored
CLiteRTLM headers in sync — that this project will not take on. Keeping it in-tree also
serves the reference goal better: you can read the integration end-to-end (download →
persist → chat → native runtime) in one place, and copy the module wholesale when you want
it. Treat it as reference source to adapt.

---

## Build & run

### Configuration (local secrets)

Both secrets below are **optional** — leave them blank and the app still runs. A HuggingFace token
only unlocks gated models; a Sentry DSN only turns on crash reporting. Neither file is checked into
version control; the values are read at build time and surfaced to shared code via `LamityBuildConfig`.

- **Android** — copy [`local.properties.example`](local.properties.example) to `local.properties` and set:
    - `HF_TOKEN` — a read-scope [HuggingFace access token](https://huggingface.co/settings/tokens),
      needed only for gated models (e.g. *Gemma 3 1B IT*). Blank disables authenticated downloads.
    - `SENTRY_DSN` — your Sentry DSN. Blank disables crash reporting.
- **iOS** — copy [`iosApp/Configuration/BuildConfig.xcconfig.example`](iosApp/Configuration/BuildConfig.xcconfig.example)
  to `BuildConfig.xcconfig` and set the same `HF_TOKEN` / `SENTRY_DSN` (note the `$()` trick in the
  file that keeps the DSN's `//` from being read as a comment).

### Android

```bash
./gradlew :androidApp:assembleDebug      # or installDebug with a device attached
```

Requires JDK 17+ (Android Studio's bundled JBR works). With sdkman, point `JAVA_HOME` at a
JDK explicitly:

```bash
JAVA_HOME=~/.sdkman/candidates/java/21.0.6-tem ./gradlew :androidApp:assembleDebug
```

The manifest already declares the OpenCL native libraries the GPU backend needs and the
foreground-service type used by the download notification.

### iOS

1. `open iosApp/iosApp.xcodeproj`
2. Set your team in `iosApp/Configuration/Config.xcconfig` (`TEAM_ID=...`).
3. Xcode compiles the Kotlin `Shared` framework (a build phase) and resolves the **vendored
   local package** in `iosApp/LiteRTLM`, which pins and fetches the prebuilt
   `CLiteRTLM.xcframework` (v0.13.1) on first build. LiteRT-LM itself is consumed as a pure
   Kotlin/Native cinterop port in `lamityLlm` — there is no Swift wrapper; the package's
   only job is to vend the dynamic framework binary.
4. Run on a **physical device** for GPU (Metal) inference. On the simulator, prefer small
   models and set the model's backend to **CPU** (Chat → customize sheet, or Models).

### Tests & static analysis

A `Makefile` wraps the common Gradle invocations:

```bash
make test-all        # Android + iOS unit tests across every module
make test-android    # JVM/Android host tests only
make test-ios        # iOS simulator tests only
make test-llm        # one module, both platforms (also: test-shared, test-downloader, …)
make lint            # ktlint + Detekt
```

---

## Try it (first chat)

1. Launch the app → **Chats** (the home screen) → start a **new chat**.
2. Open **Models** and download *Qwen2.5 1.5B Instruct* (no token; good tool calling). Downloads continue in the background —
   pause/resume them freely.
3. For *Gemma 3 1B*: accept the license on HuggingFace, create a read-scope access token at
   `huggingface.co/settings/tokens`, set it as `HF_TOKEN` (see
   [Configuration](#configuration-local-secrets)), then rebuild and download.
4. Back in the chat, with the model selected, try:
    - "What time is it in Paris?" → `get_current_time`
    - "What is 12.5% of 2,348?" → `calculate`
    - "Switch the app to dark mode" / "Đổi ngôn ngữ sang tiếng Việt" → `set_theme` /
      `set_language` (the UI updates immediately)
    - Enable **Haiku Mode** in the chat's customize sheet, then "Why is the sky blue?" →
      `load_skill`
5. Open a chat's **customize sheet** to tweak inference parameters, set a system prompt, and
   toggle tools/skills for that conversation.

---

## Notes & known limits

- **Tool-calling quality depends on the model.** Qwen2.5 1.5B is the most reliable of the
  seeded set; DeepSeek R1 streams reasoning but does not do tool calls.
- **LiteRT-LM is pinned to `0.13.1` on both platforms** (`litertlm-android` on Maven; the
  vendored CLiteRTLM xcframework + cinterop `.def` on iOS). If you bump it, the Android AAR
  coordinate and the iOS cinterop headers are the integration points to touch — see
  [`lamityLlm/README.md`](lamityLlm/README.md).
- **Engine load can take ~5 s** for larger models on first run; later loads are faster
  thanks to the compilation cache directory.
- The "thinking" channel (`message.channels["thought"]`) is surfaced on both platforms.
- On iOS, a download with a HuggingFace token resolves the redirect chain up front (a
  background session follows redirects without consulting the app), so the token is never
  forwarded to the CDN.

---

## Acknowledgements

- [Google LiteRT-LM](https://developers.google.com/edge/litert-lm/android) — the on-device
  runtime (Apache-2.0). The iOS `CLiteRTLM.xcframework` is its official prebuilt binary.
- [litert-community](https://huggingface.co/litert-community) — the `.litertlm` model files.

## License

[MIT](LICENSE) © 2026 Pham Tung Lam. The bundled LiteRT-LM runtime and the catalog models
are distributed under their own licenses (Apache-2.0 and per-model terms, respectively).
