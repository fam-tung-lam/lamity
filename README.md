# Lamity AI

A Kotlin Multiplatform (Android + iOS) on-device AI chat app built on
[Google LiteRT-LM](https://ai.google.dev/edge/litert-lm), modeled after the
[Google AI Edge Gallery](https://github.com/google-ai-edge/gallery). All UI and
business logic is shared with Compose Multiplatform; inference runs fully
on-device through the LiteRT-LM Kotlin API (Android) and Swift API (iOS).

## Features

1. **Model catalog** — curated `.litertlm` models from the HuggingFace
   [litert-community](https://huggingface.co/litert-community) (Qwen2.5 1.5B,
   Gemma 3 1B, Gemma 4 E2B/E4B/12B, DeepSeek-R1 Distill 1.5B with visible
   thinking, FunctionGemma 270M), plus custom models by URL.
2. **Background downloads** — WorkManager (Android) / background URLSession
   (iOS) transfers that survive app death, with pause/resume (HTTP Range /
   resume data), live speed + ETA, optional SHA-256 verification, a progress
   notification on Android, an optional Wi-Fi-only restriction, and
   HuggingFace token support for gated models (the token is only ever sent to
   trusted hosts, never to redirect targets).
3. **Chat** — streaming responses, stop generation, per-message stats
   (≈ tok/s), thinking panel for reasoning models, inline tool-call cards.
4. **Built-in tools** (global on/off switches): `get_current_time`,
   `calculate`, `set_theme`, `set_language` (en/vi/es — changes the app UI
   live), `random_number`, `device_info`.
5. **Skills** — create/edit/delete/enable/disable named instruction sets.
   Skills use Claude-style progressive disclosure: the system prompt only
   advertises name + description and the model pulls full instructions through
   the implicit `load_skill` tool.
6. **Agents** — name, description, system prompt, plus many-to-many
   attachments of tools and skills (sample agents/skills are seeded).
7. **Model configuration** — per-model backend (CPU/GPU), max tokens, top-K,
   top-P, temperature.
8. **Conversation history** — Room-persisted on-device, resume/rename/delete.
   Resumed chats are replayed natively via `initialMessages` on both platforms.
9. **Crash reporting** — Sentry KMP behind a `CrashReporter` facade; Kermit
   logs become breadcrumbs. Blank DSN = fully disabled.

## Module map

```
androidApp/            Android entry point (Application, MainActivity, manifest)
iosApp/                Xcode project (SwiftUI entry, Swift bridges, vendored LiteRT-LM package)
  iosApp/Llm/            LiteRT-LM Swift bridge + tool structs
  iosApp/Downloader/     Background-URLSession downloader bridge + AppDelegate wiring
shared/                App logic + Compose UI (builds the iOS "Shared" framework)
lamityLlm/             LiteRT-LM abstraction (NativeLlmBridge contract, ModelRuntime)
lamityDownloader/      Background file downloads (WorkManager / URLSession bridge)
lamityDb/              Room KMP database (entities/, daos/)
lamityCrashReporter/   Sentry KMP facade (CrashReporter, breadcrumb log writer)
lamityLogger/          Kermit re-export so every module logs the same way
```

### Inside `shared`

```
com/phamtunglam/lamity/
  App.kt               Compose root: theme + strings + bottom-tab navigation
  LamityConfig.kt      App-level config (Sentry DSN, crash-reporter setup)
  navigation/          Navigation 3 keys + saved-state registration
  di/                  Koin modules (appModule + expect/actual platformModule)
  core/
    designsystem/      theme/ (Color, Theme, Shape, Type, CustomColors) + components/ + Formatters
    i18n/              Strings model + per-language bundles (en/vi/es), runtime-switchable
    platform/          FileIo, AppDirs, PlatformInfo, BuildInfo, ids/time (expect/actual)
    tools/             Built-in tool specs, registry and dispatcher
  feature/
    chat/              data/ (conversations repo) domain/ (session manager) presentation/ (+components/)
    models/            data/ (download manager, status mapper) domain/ (catalog) presentation/ (+components/)
    history/           presentation/ (conversation list)
    studio/            data/ domain/ presentation/ (agents, skills, tool switches)
    settings/          data/ domain/ presentation/
```

Each feature follows the same `data / domain / presentation` split; reusable
UI lives in `core/designsystem`, screen-specific composables in the feature's
`presentation/components/`.

### Platform boundaries

Two Kotlin interfaces are implemented in Swift (exported in the `Shared`
framework header); everything else is common code:

```
ChatSessionManager ─> ModelRuntime ─> NativeLlmBridge (callbacks)
                                        ├─ androidMain: AndroidLlmBridge → litertlm-android
                                        └─ iosApp/Llm:  SwiftLlmBridge   → LiteRT-LM Swift (SPM)

ModelDownloadManager ─> Downloader
                          ├─ androidMain: AndroidDownloader → WorkManager worker
                          └─ iosMain: IosDownloader ─> LamityDownloaderBridge
                                        └─ iosApp/Downloader: background URLSession

Model tool calls ─> ToolExecutor → shared ToolDispatcher → ToolRegistry (common executors)
```

Tool calls made by the model on either platform are executed by **shared
Kotlin code** (`ToolDispatcher`), so theme/language switches and tool-event
chat cards behave identically on both OSes.

## Build & run

### Android

```bash
./gradlew :androidApp:assembleDebug      # or installDebug with a device attached
```

Requires JDK 17+ (Android Studio's JBR works; with sdkman:
`JAVA_HOME=~/.sdkman/candidates/java/21.0.6-tem ./gradlew ...`). Open the repo
root in Android Studio to run from the IDE. The manifest already declares the
OpenCL native libraries needed by the GPU backend and the foreground-service
type used by download notifications.

### iOS

1. `open iosApp/iosApp.xcodeproj`
2. Set your team in `iosApp/Configuration/Config.xcconfig` (`TEAM_ID=...`)
3. Xcode builds the Kotlin framework via the "Compile Kotlin Framework"
   phase and resolves LiteRT-LM from the **vendored local package** in
   `iosApp/LiteRTLM` (Swift wrapper sources at v0.13.1 + the official
   prebuilt `CLiteRTLM.xcframework` binary, fetched on first build).
   It is vendored because the upstream manifest uses `unsafeFlags`, which
   SPM rejects for remote dependencies — local packages are exempt.
4. Run on a **physical device** for GPU (Metal) inference. On the simulator
   prefer small models and set the model's backend to **CPU** in
   Models → Configure.

### Tests

```bash
./gradlew testAndroidHostTest            # Kotest BehaviorSpecs on the JVM
```

Specs live under `<module>/src/commonTest/.../unitTests/` (mirroring the main
source path) with fixtures in `fixtures/`; Android-only code is tested from
`androidHostTest`.

## First chat

1. **Models** tab → download *Qwen2.5 1.5B Instruct* (no token needed; good
   tool calling) or *FunctionGemma 270M* for a quick 290 MB test. Downloads
   continue in the background; pause/resume them freely.
2. For *Gemma 3 1B*: accept the license on HuggingFace, create an access token
   (read scope) at `huggingface.co/settings/tokens`, paste it in
   **Settings → HuggingFace token**, then download.
3. **Chat** tab → pick agent *Lami* and your model, then try:
   - "What time is it in Paris?" (get_current_time)
   - "What is 12.5% of 2,348?" (calculate)
   - "Switch the app to dark mode" / "Đổi ngôn ngữ sang tiếng Việt"
     (set_theme / set_language — UI updates immediately)
   - "Answer as a haiku: why is the sky blue?" (load_skill → Haiku Mode)
4. **Studio** tab → manage agents, skills and global tool switches.

## Notes & known limits

- Tool calling quality depends on the model; Qwen2.5 1.5B is the most
  reliable of the seeded set. FunctionGemma 270M is a function-calling demo,
  not a general chatter.
- The "thinking" channel (`message.channels["thought"]`) is surfaced on both
  platforms.
- LiteRT-LM is pinned to **0.13.1 on both platforms** (`litertlm-android` on
  Maven; the vendored Swift wrapper on iOS). If you bump either, the two
  bridge files are the only integration points to touch.
- Engine load can take ~10 s for larger models on first run; subsequent loads
  are faster thanks to the compilation cache directory.
- On iOS, downloads with a HuggingFace token resolve the redirect chain up
  front (background sessions follow redirects without consulting the app), so
  the token is never forwarded to the CDN.
