# TODOs

Planned improvements to the [`shared`](shared) module. None are blocking — the app
works today; these make the codebase cleaner and easier to follow.

## Architecture

- **Adopt one consistent layering everywhere:** `UI → ViewModel → UseCase → Repository → DataStore/DAOs/Platform APIs and so on`.
  Some features skip layers today; align them all on the same flow so the codebase
  reads the same way end to end.

- **Make repositories expose `Flow`s directly.** Today repositories manage their
  own state internally and force callers to wait via an `awaitLoaded()` method — see
  [`SettingsRepository`](shared/src/commonMain/kotlin/com/phamtunglam/lamity/feature/settings/data/SettingsRepository.kt).
  Returning a `Flow` instead lets callers observe data reactively and drops the
  `awaitLoaded()` workaround.

- **Group files by role within each feature** — e.g. `models/`, `components/`,
  `useCases/`, `utils/`. Makes the structure easier to scan at a glance.

- Maybe simplifying the architecture by dropping repository layer and used it only when there are multiple data sources.

## Logging

- **Raise log coverage** across modules.

## Testing

- **Raise test coverage** across modules.
