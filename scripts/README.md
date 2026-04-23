# scripts/

Utility scripts for the Rounds project.

| Script | Purpose |
|---|---|
| [install_hooks.sh](install_hooks.sh) | One-time per clone: wire `.githooks/` as Git's hooks directory so `commit-msg` enforces Conventional Commits. Gradle builds fail until this is run. |
| [create_module.main.kts](create_module.main.kts) | Scaffold a new KMP module (feature or library) with the right convention plugin, source sets, and wiring in `settings.gradle.kts` + `apps/compose/build.gradle.kts`. |
| [generate_sounds.py](generate_sounds.py) | Regenerate the bundled audio cue files under `features/timers/impl/src/commonMain/composeResources/files/sounds/`. |
| [cleanup.sh](cleanup.sh) | Nuke build artifacts and Gradle caches. |
| [prompts/](prompts/) | Prompts used by scheduled Claude workflows. Currently: [sentry-triage.md](prompts/sentry-triage.md). |

## create_module.main.kts

### Interactive

```sh
./scripts/create_module.main.kts
```

### Non-interactive

```sh
./scripts/create_module.main.kts <type> <name>
# type: feature | library
# name: bare (my-feature) or nested (user:preferences)
```

Examples:

```sh
./scripts/create_module.main.kts feature messaging
./scripts/create_module.main.kts library analytics
./scripts/create_module.main.kts library user:preferences
```

Applies `hiittimer.feature` to feature modules and `hiittimer.kotlin.multiplatform` to libraries. Libraries follow the public-interface / `impl` split automatically.
