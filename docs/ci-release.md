# CI & release

Three GitHub Actions workflows run the pipeline:

| Workflow | Trigger | What it does |
|---|---|---|
| [ci.yml](../.github/workflows/ci.yml) | PR + push to `main` | Compile Android + iOS Kotlin, assemble Android debug, run unit tests |
| [cut-release.yml](../.github/workflows/cut-release.yml) | Manual (`workflow_dispatch`) from `main` | Bumps `versions.properties` + iOS `MARKETING_VERSION`, commits, tags `vX.Y.Z`, pushes |
| [release.yml](../.github/workflows/release.yml) | Push of tag `v*` | Builds signed Android AAB, uploads iOS build to TestFlight, creates GitHub Release with artifacts attached |

## Cutting a release

1. Go to **Actions → Cut release → Run workflow**.
2. Pick the `main` branch, enter a bump type (`patch`, `minor`, `major`, or explicit `1.2.3`), click Run.
3. The workflow bumps versions, commits to main, creates tag `vX.Y.Z`, pushes. The tag push triggers the Release workflow.
4. Monitor Actions → Release. When both Android + iOS jobs finish, a GitHub Release is published with the AAB, APK, and IPA attached.
5. **Promote to production** manually from App Store Connect (TestFlight → App Store) and Play Console (internal → closed/open/production). The pipeline only pushes to internal/beta tracks.

## Required GitHub secrets

### iOS (already configured)
- `APPLE_TEAM_ID`
- `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8_BASE64`

### Android
Generated once with `keytool`. The keystore file itself lives in `~/.hiittimer-release/` on the maintainer's machine and in 1Password — **if you lose both copies, the app can never be updated on Play Store.** The only thing in GitHub is the base64 encoding of it.

- `ANDROID_KEYSTORE_BASE64` — `base64 -i hiittimer-upload.p12` output
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS` (currently `hiittimer-release`)
- `ANDROID_KEY_PASSWORD` (same as `ANDROID_KEYSTORE_PASSWORD` for PKCS12)

### Not yet configured
- `PLAY_SERVICE_ACCOUNT_JSON_BASE64` — Play Console service account. The Android release job currently stops after producing the signed AAB; when you set up Play Console, add this secret and extend the Android job to upload to the internal track (see `r0adkll/upload-google-play-action`).

## Building a signed release locally

The app signs release builds with the debug keystore by default, so `./gradlew :apps:compose:assembleRelease` Just Works for smoke-testing on your device. To build with the real upload keystore (e.g. to verify what CI will produce), add the following to `local.properties`:

```
android.keystore.path=/absolute/path/to/hiittimer-upload.p12
android.keystore.password=...
android.key.alias=hiittimer-release
android.key.password=...
```

Then:

```
./gradlew :apps:compose:assembleRelease -Prelease.signing=true
```

The signing config is defined in [build-logic/.../Signing.kt](../build-logic/src/main/java/com/hiittimer/util/Signing.kt).

## Version bumping script

[scripts/increment-version.main.kts](../scripts/increment-version.main.kts) is the single source of truth. It:

- Reads `versions.properties` and the iOS `Config.xcconfig`
- Bumps `versionName` per input (`patch` | `minor` | `major` | explicit `x.y.z`)
- Increments `versionCode` and `buildNumber` by 1 each
- Mirrors `versionName` to iOS `MARKETING_VERSION`
- Writes `version=`, `versionCode=`, `buildNumber=` to `$GITHUB_OUTPUT` when run under Actions

Run it locally with `kotlin scripts/increment-version.main.kts patch` to dry-run. Revert with `git checkout versions.properties apps/ios/Configuration/Config.xcconfig` if you don't want to keep the bump.
