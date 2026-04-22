# AGENTS.md

Guidelines for AI agents working in the Rounds codebase (repo name `HIITTimer` is historical and stays for continuity with CI, bundle IDs, and paths — the user-facing product is `Rounds`).

## Overview

KMP (Kotlin Multiplatform) app with Compose Multiplatform. Modular architecture with Room database, navigation, and SEAViewModel pattern.

This is **Kotlin Multiplatform**—most code is shared, but some platform features (permissions, sensors, native APIs) require platform-specific implementations. When implementing something not inherently cross-platform, follow the patterns in `docs/swift-kotlin-communication-patterns.md`.

## Build Commands

```shell
./gradlew :apps:compose:assembleDebug          # Android
./gradlew :apps:compose:compileKotlinIosSimulatorArm64  # iOS Kotlin
xcodebuild -project apps/ios/iosApp.xcodeproj -scheme iOS -sdk iphonesimulator  # iOS full
```

## Module Structure

```
apps/compose/          # KMP entry point (Android + iOS)
apps/ios/              # Swift wrapper
features/<name>/       # Routes, public API
features/<name>/impl/  # Screens, ViewModels
libraries/<name>/      # Interfaces
libraries/<name>/impl/ # Implementations
```

**Rules:** Features never depend on features. Shared code → libraries. Main modules expose interfaces only; impl modules contain implementations.

**Storage exception:** `libraries/storage/impl` may depend on any submodule named `storage`, whether under `libraries/*` or `features/*` (e.g. `features/timers/storage`). This lets features colocate their Room entities/DAOs while still registering them with the single global `AppDatabase`. Nothing else should depend on a feature's storage submodule.

## Convention Plugins

| Plugin | Use |
|--------|-----|
| `hiittimer.kotlin.multiplatform` | Pure Kotlin |
| `hiittimer.compose.multiplatform` | Kotlin + Compose |
| `hiittimer.feature` | Feature modules |
| `hiittimer.application` | apps:compose only |

Use `/scripts/create_module` for new modules.

## DI (kotlin-inject-anvil)

```kotlin
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class MyImpl : MyInterface

// Multibinding for FeatureEntryPoints
@ContributesBinding(AppScope::class, multibinding = true)
```

No expect/actual for platform impls—bind different implementations per platform. iOS impls written in Swift get passed into the DI graph via `IosAppComponentFactory.create(...)`.

## SEAViewModel Pattern

```kotlin
class MyViewModel : SEAViewModel<State, Event, Action>(initialStateArg = State()) {
    override suspend fun handleAction(action: Action) {
        when (action) {
            is Action.Load -> action.updateState { it.copy(loading = true) }
        }
    }
}
```

- **State**: Immutable data class for UI
- **Event**: One-shot side effects (navigation, toasts)
- **Action**: Only way to mutate state via `action.updateState { }`

## Navigation

Routes are `@Serializable` data classes extending `Route`. Register in `FeatureEntryPoint.buildNavGraph()`:

```kotlin
screen<MyRoute> { backStackEntry -> MyScreen(...) }
bottomSheet<SheetRoute> { backStackEntry, sheetState -> ... }
dialog<DialogRoute> { backStackEntry, dialogState -> ... }
```

## Coding Guidelines

- Code like a staff engineer
- Use `Catching { }` from libraries/core instead of `runCatching`
- No comments in code
- Custom UI components in libraries/ui—avoid Material directly
- Check `ComposeApp.h` for Swift names of Kotlin types before using in Swift

## Conventional commits (required)

Every PR title must be a conventional commit. PRs are squash-merged, so the title becomes the commit message, and [release-please](https://github.com/googleapis/release-please) derives the next version bump from commit types.

| Prefix | Version bump | Use for |
|---|---|---|
| `fix:` | patch (0.1.0 → 0.1.1) | Bug fixes |
| `feat:` | minor (0.1.0 → 0.2.0) | New user-facing features |
| `feat!:` or trailer `BREAKING CHANGE:` | major (0.1.0 → 1.0.0) | Breaking changes |
| `perf:` | patch | Performance improvements |
| `refactor:`, `docs:`, `style:`, `test:`, `build:`, `ci:`, `chore:` | none | Everything else (still allowed) |

Subject starts with a lowercase letter and describes the end-user effect, not the implementation. Enforced in two places:
- **Locally** via `.githooks/commit-msg` — run `./scripts/install_hooks.sh` once per clone. The first Gradle build will fail until the hook is installed.
- **On PRs** via the `commitlint.yml` workflow (squash-merged PR titles).

## Automation routines

Background automation runs in GitHub Actions. Any AI agent opening PRs should match these conventions so the pipeline handles them correctly.

- **`ai-autofix` label** on a PR → auto-merge once CI is green. Use for Sentry-triage fixes and similar low-risk patches.
- **Sentry-triage cron** ([`sentry-triage.yml`](.github/workflows/sentry-triage.yml)) runs every Monday with [`scripts/prompts/sentry-triage.md`](scripts/prompts/sentry-triage.md) as the prompt. Edit that file — not the workflow — to change behavior.
- **release-please** owns `versions.properties`, `apps/ios/Configuration/Config.xcconfig` (MARKETING_VERSION), and `CHANGELOG.md`. Do not touch them in feature PRs.
- **Never edit `.github/workflows/*`** from an autofix PR — CI changes are out of scope for the triage routine and need a human.

See [`docs/release-automation.md`](docs/release-automation.md) for the full release pipeline.

## iOS Notes

- iOS framework compiled from `apps/compose`, embedded as `ComposeApp.xcframework`
- Swift types passed to Kotlin via `IosAppComponentFactory.create(...)`
- Reference `apps/compose/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework/Headers/ComposeApp.h` for generated Swift interfaces
- **Use `@ObjCName("TypeName", exact = true)` on Kotlin types used from Swift** to give stable names that won't change when project is renamed:
  ```kotlin
  @file:OptIn(ExperimentalObjCName::class)
  import kotlin.experimental.ExperimentalObjCName
  import kotlin.native.ObjCName
  
  @ObjCName("MyType", exact = true)
  interface MyType { ... }
  ```
  Note: The `exact = true` parameter prevents module prefixes from being added. Without it, the Swift name would be `<ModuleName><ObjCName>` (e.g., `KmptemplateMyType`).

## Key Files

| Purpose | Path |
|---------|------|
| User model | `libraries/hiittimer/src/.../User.kt` |
| SEAViewModel | `libraries/flowroutines/src/.../SEAViewModel.kt` |
| App DI | `apps/compose/src/.../AppComponent.kt` |
| iOS entry | `apps/ios/iosApp/iOSApp.swift` |
| Swift↔Kotlin patterns | `docs/swift-kotlin-communication-patterns.md` |

