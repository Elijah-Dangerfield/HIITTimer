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

**Rules:**

- `features/<x>/impl` may depend on another feature's **api** (`features/<y>`), never on its `impl`. This is how features talk to each other without creating implementation cycles.
- `features/<x>` (api) must **not** depend on another feature's api — api-to-api edges become cycles the moment someone adds the reverse dependency. Keep api modules leaf-ish.
- Only `apps/compose` depends on `impl` modules. Impls are the DI wiring the app composes; anything else reaching into an impl breaks the public-interface contract.
- Libraries follow the same api/impl split. Shared code belongs in libraries, not in feature apis.

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

### Decision: which type?

**The question to ask: will a user see the effect of this change if they update the app?**

| Answer | Type | Version bump | Release notes |
|---|---|---|---|
| Yes, it's a new capability | `feat:` | minor | "Features" |
| Yes, and it's breaking | `feat!:` or `BREAKING CHANGE:` trailer | major (capped at minor pre-1.0) | prominently |
| Yes, it's a bug in the shipped app | `fix:` | patch | "Bug Fixes" |
| Yes, it's faster/smoother | `perf:` | patch | "Performance" |
| No — CI workflow change | `ci:` | none | omitted |
| No — build config, Gradle plugin, R8, fastlane, versions | `build:` | none | omitted |
| No — repo housekeeping, doc edits, test changes, internal refactors | `chore:`, `docs:`, `test:`, `refactor:`, `style:` | none | omitted |

### Worked examples

| Change | Right type | Why |
|---|---|---|
| Fix crash when user taps "Start" with no blocks | `fix:` | User-visible bug |
| Add Apple Watch companion | `feat:` | New user capability |
| Swap internal architecture of timer engine, no behavior change | `refactor:` | User sees nothing |
| Fix Play Console upload path in release.yml | `ci:` | CI plumbing; users don't see it |
| Bump Kotlin compiler version | `build:` | Build system; no runtime effect |
| Enable R8 minification on release builds | `build:` | Build config; invisible to user (unless something breaks, in which case hotfix with `fix:`) |
| Rewrite README | `docs:` | Documentation |
| Add a new Sentry triage prompt | `ci:` | Automation config |

### Enforcement (two gates, both automatic)

- **Locally**: `.githooks/commit-msg` rejects non-Conventional-Commit messages. Also **warns** when a `fix:`/`feat:`/`perf:` commit only touched CI/build/docs files (likely a typing mistake). Run `./scripts/install_hooks.sh` once per clone — the first Gradle build fails until the hook is installed.
- **After each commit**, `.githooks/post-commit` prints a one-line recap of what release-please will do with it (e.g. `» fix: patch bump. Goes under 'Bug Fixes' in release notes.`).
- **On PRs**: the `commitlint.yml` workflow re-validates the squash-merge commit title.

## Automation routines

Background automation runs in GitHub Actions. Any AI agent opening PRs should match these conventions so the pipeline handles them correctly.

- **`ai-autofix` label** on a PR → auto-merge once CI is green. Use for Sentry-triage fixes and similar low-risk patches.
- **Sentry triage** runs as a Claude Code routine on the maintainer's machine (not a CI job) using [`scripts/prompts/sentry-triage.md`](scripts/prompts/sentry-triage.md) as the prompt. Edit that file to change behavior.
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

