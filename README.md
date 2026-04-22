# Rounds

A Kotlin Multiplatform HIIT / interval timer app, built with Compose Multiplatform, Room, and a unidirectional-data-flow ViewModel base class.

## Build & Run

First time in a fresh clone:

```shell
./scripts/install_hooks.sh   # enables the Conventional Commits commit-msg hook
```

Then:

```shell
# Android
./gradlew :apps:compose:assembleDebug

# iOS - compile Kotlin framework
./gradlew :apps:compose:compileKotlinIosSimulatorArm64

# iOS - or open in Xcode
open apps/ios/iosApp.xcodeproj
```

The first Gradle build will fail with a remediation message if you skip the hook install.

## Project Structure

```
apps/compose/          # KMP entry point (Android + iOS)
apps/ios/              # Swift/Xcode wrapper
features/<name>/       # Routes and public API
features/<name>/impl/  # Screens and ViewModels
libraries/<name>/      # Interfaces
libraries/<name>/impl/ # Implementations
```

### Architecture Rules

- **api / impl split**: `features/<x>` holds routes + public interfaces; `features/<x>/impl` holds screens, ViewModels, wiring.
- **Cross-feature talk goes api → impl**: `features/<x>/impl` can depend on `features/<y>` (api). Never on `features/<y>/impl`.
- **No api-to-api edges**: `features/<x>` must not depend on another feature's api. That path leads to cycles the first time someone adds the reverse.
- **Only `apps/compose` depends on impls** — it's the DI glue. Anything else reaching into an impl breaks the interface contract.
- **Libraries follow the same rules.** Shared code lives in libraries, not in feature apis.

### Creating New Modules

```shell
./scripts/create_module
```

| Plugin | Use Case |
|--------|----------|
| `hiittimer.kotlin.multiplatform` | Pure Kotlin modules |
| `hiittimer.compose.multiplatform` | Kotlin + Compose UI |
| `hiittimer.feature` | Feature modules |

## Architecture Patterns

### ViewModel (Unidirectional Data Flow)

ViewModels extend `SEAViewModel` which enforces **State-Event-Action** unidirectional data flow:

```kotlin
class MyViewModel : SEAViewModel<State, Event, Action>(initialStateArg = State()) {
    override suspend fun handleAction(action: Action) {
        when (action) {
            is Action.Load -> action.updateState { it.copy(loading = true) }
            is Action.Submit -> {
                // Do work, then send one-shot event
                sendEvent(Event.NavigateBack)
            }
        }
    }
}
```

- **State**: Immutable data class representing UI state
- **Event**: One-shot side effects (navigation, toasts, etc.)
- **Action**: The only way to mutate state via `action.updateState { }`

### Dependency Injection

Uses [kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil):

```kotlin
// Bind implementation to interface
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class MyRepositoryImpl : MyRepository

// Multibinding for feature entry points
@ContributesBinding(AppScope::class, multibinding = true)
class MyFeatureEntryPoint : FeatureEntryPoint
```

### Navigation

Routes are `@Serializable` data classes extending `Route`:

```kotlin
@Serializable
data class ProfileRoute(val userId: String) : Route

// Register in FeatureEntryPoint.buildNavGraph()
screen<ProfileRoute> { backStackEntry -> 
    ProfileScreen(userId = backStackEntry.toRoute<ProfileRoute>().userId)
}
```

Supports `screen`, `bottomSheet`, and `dialog` destinations.

## iOS Integration

The iOS app embeds a Kotlin framework compiled from `apps/compose`. Swift types can be passed into Kotlin's DI graph via `IosAppComponentFactory.create(...)`.

When exposing Kotlin types to Swift, use `@ObjCName` for stable naming:

```kotlin
@ObjCName("MyType", exact = true)
interface MyType { ... }
```

See [Swift-Kotlin Communication Patterns](docs/swift-kotlin-communication-patterns.md) for detailed guidance.

## Coding Guidelines

- Use `Catching { }` from `libraries/core` instead of `runCatching`
- Custom UI components go in `libraries/ui`—avoid using Material components directly

## Releases

Cutting a release is merging one PR. release-please maintains an open `chore(main): release vX.Y.Z` PR with the version bump + changelog; merging it tags, builds both platforms, and submits to the stores. PR titles must be conventional commits (`fix:`, `feat:`, `feat!:`) — the type picks the version bump.

See [docs/release-automation.md](docs/release-automation.md) for the full pipeline (CI, Sentry triage, rollout guard, secrets) and [AGENTS.md](AGENTS.md#conventional-commits-required) for the commit convention.

## Key Files

| Purpose | Path |
|---------|------|
| App DI Component | `apps/compose/src/.../AppComponent.kt` |
| Base ViewModel | `libraries/flowroutines/src/.../SEAViewModel.kt` |
| iOS Entry Point | `apps/ios/iosApp/iOSApp.swift` |

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
