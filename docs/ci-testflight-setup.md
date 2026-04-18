# TestFlight CI setup

The `ios-testflight` workflow builds a Release `.ipa` on a `macos-14` runner, signs it with your Apple Developer team via automatic signing, and uploads it to TestFlight.

## One-time: Apple side

1. **Apple Developer Program membership.** $99/year, required for TestFlight. The free personal team does not work with CI.
2. **Team ID.** Find it at <https://developer.apple.com/account> → Membership → Team ID (10-character alphanumeric).
3. **Bundle ID.** The app uses `com.dangerfield.hiittimer.HIITTimer`. Register it at <https://developer.apple.com/account/resources/identifiers/list>.
4. **App Store Connect app record.** Create the app at <https://appstoreconnect.apple.com/apps> with that bundle ID, a name (`HIIT Timer`), and SKU. This is what TestFlight attaches builds to.
5. **App Store Connect API key.** At <https://appstoreconnect.apple.com/access/integrations/api> create a new key with **App Manager** role.
   - Save the `.p8` file (you only get one download).
   - Note the **Key ID** (10-char) and **Issuer ID** (UUID at the top of the Keys tab).

## One-time: GitHub secrets

Set these in repo **Settings → Secrets and variables → Actions**.

| Secret | What it is |
|---|---|
| `APPLE_TEAM_ID` | Your 10-character Apple team ID. |
| `ASC_KEY_ID` | App Store Connect API Key ID. |
| `ASC_ISSUER_ID` | App Store Connect API Issuer ID (UUID). |
| `ASC_KEY_P8_BASE64` | The `.p8` private key, base64-encoded. On macOS: `base64 -i AuthKey_ABC123DEF4.p8 \| pbcopy`. |

## Verify

1. Trigger manually: **Actions → iOS TestFlight → Run workflow** on `main`.
2. First successful run takes ~15–20 minutes (cold Gradle cache, `xcodebuild archive`, upload).
3. Subsequent runs should be ~7–10 minutes thanks to cache.
4. Build shows up in TestFlight within ~5 minutes of the upload step finishing. On your phone, open TestFlight → **HIIT Timer** → Install.
5. The first build Apple processes will take longer (10–30 min). Later uploads are faster.

## Cost watchouts

- GitHub Actions macOS runners bill at 10× the Linux rate. Personal account free tier is 2,000 minutes/month, which maps to ~200 macOS minutes ≈ ~20 TestFlight builds/month. Watch usage under **Settings → Billing**.

## Triggers

| Event | Triggers |
|---|---|
| Push to `main` | `ios-testflight` |
| Push to `playground/**` | `ios-testflight` |
| Manual dispatch | `ios-testflight` (with optional changelog input) |

## Files

- `.github/workflows/ios-testflight.yml` — the build + upload workflow.
- `apps/ios/Gemfile` — pins Fastlane.
- `apps/ios/fastlane/Fastfile` — the `beta` lane (build + upload) and a local-only `build_only` lane.
- `apps/ios/fastlane/Appfile` — bundle ID + team ID wiring.

## Running fastlane locally

Smoke-test the Fastfile without CI:

```sh
cd apps/ios
bundle install
APPLE_TEAM_ID=XXXXXXXXXX bundle exec fastlane build_only
```

For a full upload locally, also export `ASC_KEY_ID`, `ASC_ISSUER_ID`, and `ASC_KEY_P8_BASE64`.
