# TestFlight + Claude CI setup

The `ios-testflight` workflow builds a Release `.ipa` on a `macos-14` runner, signs it with your Apple Developer team via automatic signing, and uploads it to TestFlight. The `claude-playground` workflow spawns Claude Code in CI on `@claude` mentions and pushes changes to a `playground/**` branch, which then triggers the TestFlight workflow.

## One-time: Apple side

1. **Apple Developer Program membership.** $99/year, required for TestFlight. The free personal team does not work with CI.
2. **Team ID.** Find it at <https://developer.apple.com/account> → Membership → Team ID (10-character alphanumeric).
3. **Bundle ID.** The app uses `com.dangerfield.hiittimer.HIITTimer`. Register it at <https://developer.apple.com/account/resources/identifiers/list>.
4. **App Store Connect app record.** Create the app at <https://appstoreconnect.apple.com/apps> with that bundle ID, a name (`HIIT Timer`), and SKU. This is what TestFlight attaches builds to.
5. **App Store Connect API key.** At <https://appstoreconnect.apple.com/access/integrations/api> create a new key with **App Manager** role.
   - Save the `.p8` file (you only get one download).
   - Note the **Key ID** (10-char) and **Issuer ID** (UUID).

## One-time: GitHub secrets

Set these in repo **Settings → Secrets and variables → Actions**.

| Secret | What it is |
|---|---|
| `APPLE_TEAM_ID` | Your 10-character Apple team ID. |
| `ASC_KEY_ID` | App Store Connect API Key ID. |
| `ASC_ISSUER_ID` | App Store Connect API Issuer ID (UUID). |
| `ASC_KEY_P8_BASE64` | The `.p8` private key, base64-encoded. On macOS: `base64 -i AuthKey_ABC123DEF4.p8 \| pbcopy`. Paste as-is. |
| `CLAUDE_CODE_OAUTH_TOKEN` | Run `claude setup-token` on your local machine (with your Claude subscription signed in). Paste the resulting token. This lets the Action draw from your Claude subscription instead of API credits. If you'd rather use the Anthropic API, rename to `ANTHROPIC_API_KEY` in both `.github/workflows/claude-playground.yml` and Settings and put your API key there instead. |

## Verify the TestFlight workflow

1. Trigger manually: **Actions → iOS TestFlight → Run workflow** on `main`.
2. First successful run takes ~15–20 minutes (cold Gradle cache, `pod install`, `xcodebuild archive`, upload).
3. Subsequent runs on the same branch should be ~7–10 minutes thanks to cache.
4. Build shows up in TestFlight within ~5 minutes of the upload step finishing. On your phone, open TestFlight → **HIIT Timer** → Install.
5. The first time you upload, Apple will take longer (10–30 min) to process the build. Later uploads are faster.

## Verify the Claude workflow

1. Open an issue titled something like `@claude tighten the Runner header padding`.
2. Within ~30 seconds the Action should start, Claude replies in the issue comments, then commits to `playground/<issue-number>` and opens a PR.
3. Because the branch matches `playground/**`, `ios-testflight` fires, builds the iPhone app, and ships to TestFlight.
4. Install the new TestFlight build on your phone, try the change, comment `@claude ...` again to iterate, or merge the PR when you're happy.

## Cost watchouts

- GitHub Actions macOS runners bill at 10× the rate of Linux. Personal account free tier is 2,000 minutes/month, which maps to ~200 macOS minutes ≈ ~20 TestFlight builds/month. Watch usage under **Settings → Billing**.
- The Claude Action uses your Claude subscription via `CLAUDE_CODE_OAUTH_TOKEN` (no API spend), subject to your plan's limits. If you switch to an API key, set a monthly cap in the Anthropic console.
- The Anthropic Action runs on Linux (cheap). Only the TestFlight workflow is on macOS.

## Triggers summary

| Event | Triggers |
|---|---|
| Push to `main` | `ios-testflight` |
| Push to `playground/**` | `ios-testflight` |
| Manual dispatch | `ios-testflight` (with optional changelog input) |
| Comment `@claude ...` on issue/PR | `claude-playground` |
| Open issue with `@claude` in title/body | `claude-playground` |

## Files in the repo

- `.github/workflows/ios-testflight.yml` — the build + upload workflow.
- `.github/workflows/claude-playground.yml` — the Claude mention handler.
- `apps/ios/Gemfile` — pins Fastlane.
- `apps/ios/fastlane/Fastfile` — the `beta` lane (build + upload) and a local-only `build_only` lane.
- `apps/ios/fastlane/Appfile` — bundle ID + team ID wiring.

## Running fastlane locally

Useful to smoke-test the Fastfile without touching CI:

```sh
cd apps/ios
bundle install
APPLE_TEAM_ID=XXXXXXXXXX bundle exec fastlane build_only
```

For a full upload locally you'd also need to export the four ASC/team env vars listed above.
