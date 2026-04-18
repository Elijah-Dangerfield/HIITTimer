# HIIT Timer

A Kotlin Multiplatform template with a clean, modular architecture using Compose Multiplatform, Room database, and a base ViewModel that encourages unidirectional data flow.

## Build & Run

```shell
# Android
./gradlew :apps:compose:assembleDebug

# iOS - compile Kotlin framework
./gradlew :apps:compose:compileKotlinIosSimulatorArm64

# iOS - or open in Xcode
open apps/ios/iosApp.xcodeproj
```

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

- **Features cannot depend on other features** — keeps dependency graph acyclic
- **Shared code belongs in libraries** — extract common functionality up
- **Main modules expose interfaces only** — `impl` modules contain implementations

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

## Key Files

| Purpose | Path |
|---------|------|
| App DI Component | `apps/compose/src/.../AppComponent.kt` |
| Base ViewModel | `libraries/flowroutines/src/.../SEAViewModel.kt` |
| iOS Entry Point | `apps/ios/iosApp/iOSApp.swift` |

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
