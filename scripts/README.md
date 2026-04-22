# scripts/

Utility scripts for the Rounds project.

| Script | Purpose |
|---|---|
| [create_module.main.kts](create_module.main.kts) | Scaffold a new KMP module (feature or library) with the right convention plugin, source sets, and wiring in `settings.gradle.kts` + `apps/compose/build.gradle.kts`. |
| [rotate_apple_sign_in_token.main.kts](rotate_apple_sign_in_token.main.kts) | Rotate the Apple Sign In backend token. Run when the key expires. |
| [generate_sounds.py](generate_sounds.py) | Regenerate the bundled audio cue files under `features/timers/impl/src/commonMain/composeResources/files/sounds/`. |
| [halt_asc_phased_release.sh](halt_asc_phased_release.sh) | Pause an in-progress App Store phased release via the ASC API. Invoked by the rollout-guard workflow; runnable locally with `APPLE_TEAM_ID`, `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8_BASE64` set. |
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
