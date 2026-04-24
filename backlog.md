# Backlog

Ideas worth building, not scheduled. Grouped loosely: user-facing
features, reliability / observability fixes, CI and release
infrastructure.

## Stats, badges, and challenges page

A dedicated screen opened from a shield/badge icon in the top bar of the
timer list. Makes the app feel like a real fitness product and gives users a
reason to keep coming back beyond the timer itself.

**Top-bar entry point:**
- Shield/badge glyph, top-right of the timer list (settings moves into an
  overflow or a profile sheet).
- Subtle unread dot when an un-viewed badge is available.

**Stats section:**
- Total workout time (lifetime + this month).
- Number of sessions (lifetime + this month).
- Longest single session.
- Current streak / longest streak (days in a row with at least one session).
- GitHub-style contribution grid of the last 12–16 weeks. Each square = a
  day; intensity shaded by total minutes that day. Tap a square → day detail.
- Per-timer breakdown: which timers get used most.

**Badges — Apple Fitness style:**
- Circular / hex medallion art, each with a unique color + glyph.
- Grid on the stats page: earned badges in color, locked ones as greyed-out
  silhouettes (hidden badges show as "?" with no hint).
- Tap a badge → detail sheet with title, description, date earned, and (for
  repeatable ones) count.
- Most badges are **hidden**. User only discovers them by earning them. A
  small subset are visible-goals (e.g. "First Workout", "10 Sessions") so
  there's something to aim at out of the gate.

**Challenge ideas** (starter list, keep growing):

*Milestones (visible)*
- First Workout
- 10 / 50 / 100 / 500 / 1000 sessions lifetime
- 10 / 60 / 600 / 6000 total minutes lifetime

*Session length (hidden)*
- 10, 20, 30, 45, 60, 90 minute single session
- "Marathon" — 2 hour single session

*Frequency (hidden)*
- 2 sessions in one day
- 3 sessions in one day
- 5 sessions in one day
- 5 workouts in a month
- 10 workouts in a month
- 20 workouts in a month
- Every day for a week (7-day streak)
- 14-day streak
- 30-day streak
- 100-day streak
- Perfect month (every day)

*Time of day (hidden)*
- Early bird — finish before 7am
- Night owl — start after 10pm
- Lunch break — session between 12–1pm
- Midnight session — between midnight and 4am

*Day of week (hidden)*
- Weekend warrior — session on both Saturday and Sunday
- Monday motivator — first session of the week on a Monday
- Tabata Tuesday — a Tabata on a Tuesday (tie to timer template type)

*Behavior (hidden)*
- Comeback — return after a 7+ day gap
- Architect — create 10 custom timers
- Explorer — run 5 different timers
- Minimalist — complete a session under 5 minutes
- Unshaken — finish a 30+ min session without pausing
- Iron will — finish a 60+ min session without pausing
- Perfectionist — complete a session with zero skipped intervals

*Calendar (hidden)*
- New Year's Day session
- Birthday session (needs user DOB — skip for v1)
- Holiday sessions (Thanksgiving, Christmas, etc.)

**Finished-screen integration:**
- When a session ends, run badge evaluation.
- If any new badges were earned, show them on the finished screen with a
  reveal animation (confetti, shimmer, whatever fits the theme) before the
  rest of the stats.
- Stack multiple badges if earned in the same session — one after the other,
  tappable to dismiss.
- Earned badges appear on a "recently earned" shelf at the top of the stats
  page for ~7 days.

**Data model:**
- New storage submodule `features/stats/storage` with a `Session` entity
  (timerId, startedAt, completedAt, totalMs, pauses, etc.) and a
  `BadgeEarned` entity (badgeId, earnedAt, sessionId).
- Badges defined as a sealed hierarchy in code with a `evaluate(context)`
  predicate — no runtime config needed.
- Session is written when runner completes. Badge evaluator runs on completion
  and returns newly-earned badges to hand to the finished screen.

**Effort:** 1–2 weeks. Split as: data model + session persistence (2 days),
stats page UI (3 days), badge system + ~30 challenges (4 days), finished-screen
reveal animation (1 day).

## Apple Watch / Wear OS companion

Run timers from the wrist. Major differentiator for a fitness app.

**Rough shape:**
- Start from Apple Watch first (standalone watchOS app that talks to the
  phone via WatchConnectivity).
- Minimum viable version: pick a timer on the phone, "send to watch", run
  the interval from the watch with haptics on transitions.
- Stretch: create/edit timers on the watch, sync session history back to
  phone, standalone operation without phone nearby.
- Wear OS port after the watchOS version is stable.

**Effort:** 2–3 weeks for a minimum Apple Watch version. Wear OS is a
separate chunk.

## Image share card on the finished screen

After a workout, let the user share a designed "just finished" image — timer
name, total time, rounds, block strip — to Messages, iOS Photos, X, Instagram,
etc. via the system share sheet.

**Rough shape:**
- `expect/actual ShareService` with `shareImage(bytes, title)`. Android:
  `Intent.ACTION_SEND` with a FileProvider URI. iOS: `UIActivityViewController`.
- Compose-to-bitmap: `GraphicsLayer.toImageBitmap()` on Android (Compose 1.6+);
  `UIGraphicsImageRenderer` on the hosting UIView on iOS.
- One aspect ratio to start — 1080×1080 PNG lands well in Messages, X, and IG
  feed. Instagram Stories wants 9:16 + its own `instagram-stories://` deep
  link, treat as a stretch goal.

**Effort:** 1–2 days. Bulk of the work is the platform glue for Compose →
image, not the design of the card.

## Multiple rounds per workout

Today a workout has one implicit "cycle group" — blocks tagged `Cycle` repeat
together `cycleCount` times, wrapped by optional warmup/cooldown blocks.
Allow users to create multiple rounds, each with its own block list and
repeat count. Example: Round 1 (squat, push-up, plank × 3) then Round 2
(burpee, rest × 5).

**Data model:**
- Introduce a `Round` (id, name, repeatCount, List<Block>). Timer becomes
  `List<Round>`. Drop `BlockRole` or keep only as a UI hint.
- Room migration: each existing timer's `Cycle` blocks wrap into one Round
  with `repeatCount = cycleCount`; warmup/cooldown become Rounds with
  `repeatCount = 1`. Once-only, non-reversible — schema version bump.

**Decisions to make before coding:**
- **Unify warmup/cooldown into rounds** (warmup = round with `repeatCount 1`)
  vs **keep separate**. Unifying is more flexible (any round can be
  non-repeating, any position); separate preserves the current 3-zone mental
  model. Lean unify.
- **Runner screen cognitive load.** Multi-round adds a layer to the progress
  display ("Round 2 of 3, block 3 of 5, rep 2 of 4"). Decide whether to show
  full nesting or collapse to "current block + overall %". Must feel calm
  mid-workout.
- **Reordering within vs across rounds.** Drag-reorder blocks across round
  boundaries is fiddly UX — sketch before building.

**UX — progressive disclosure:**
- Today's experience is unchanged for 1-round workouts. One round shown, no
  extra chrome.
- Below the blocks, a `+ Add round` affordance. Tapping creates a new round
  section with its own repeat counter.
- Round name defaults to "Round 1", "Round 2", renameable. Names surface as
  runner-screen cues.
- Soft caps (5 rounds / 20 blocks per round) to keep the list usable.
- First time a user opens the edit screen post-update, show a dismissible
  one-liner ("You can now add multiple rounds"). No modal, no tutorial video.

**Also update:**
- `StarterTimersSeeder` — ship at least one multi-round starter template to
  showcase the feature day one.
- Total-duration math across list/detail/share cards.
- "What's New" copy for the release that ships this.

**Effort:** ~1 focused day. Data model + migration is rote; the time goes
into the nested-list edit UX and the runner-screen redesign.

## Release CI — further speedups beyond the DerivedData cache

The DerivedData cache + `clean: false` + pre-built KMP framework already
gets iOS release down to ~10 min on warm runs. The next layer of wins is
about not rebuilding shared work across jobs and not rebuilding the KMP
framework at all when Kotlin sources haven't changed.

**Extract KMP framework into its own job.**
- New job `build-framework`, runs on macOS, produces
  `ComposeApp.xcframework` (or the single-arch `Framework` for device)
  as a GitHub Actions artifact.
- Keyed on a hash of `apps/compose/src/**`, `libraries/**/src/**`,
  `features/**/src/**`, and `gradle/libs.versions.toml`. If that hash
  matches a previous run's cached output, the job skips and the
  artifact is reused.
- `ios` job downloads the artifact instead of running
  `linkReleaseFrameworkIosArm64` itself, then proceeds straight to the
  Xcode archive. Makes iOS near-instant for pure-CI-config or pure-Swift
  changes.
- Complication: `embedAndSignAppleFrameworkForXcode` today is invoked
  by an Xcode build phase that calls `./gradlew`. Would need to either
  replace that build phase with a copy-artifact step when the
  `PREBUILT_FRAMEWORK_PATH` env var is set, or add a conditional in the
  phase's shell script.

**Cross-runner Gradle build cache.**
- Android (Linux) and iOS (macOS) runners don't share Gradle build cache
  today. Tasks like `compileCommonMainKotlinMetadata`,
  `compileKotlinCommonMain`, KSP runs on `commonMain`, and dependency
  resolution produce platform-independent outputs that Gradle's build
  cache could share across runners.
- Options: (a) use `gradle/actions/setup-gradle`'s cache in a shared
  cache key scheme keyed on `hashFiles('**/*.gradle*', 'gradle/**')`
  across both jobs — simple, modest win; (b) stand up a remote build
  cache (Gradle Enterprise/Develocity, or a self-hosted S3-backed
  one). Remote build cache gives us read/write across all runners and
  also benefits local dev, but it's operational overhead for a solo
  project.
- Realistic win from (a): 30–60s per run. Not transformative on its own
  — mainly useful combined with framework extraction above so the
  common metadata tasks the framework build needs are already cached.

**Reconsider `macos-15-xlarge` when not on free tier.**
- xlarge is 2–3× faster for iOS work but 2× the minute multiplier on
  paid plans. On the free tier the multiplier eats allowance too fast.
  If the project ever moves to a paid account or a public repo (where
  macOS minutes are free-ish), revisit — iOS release could drop to
  3–5 min.

**Make release.yml idempotent / re-runnable.**
- Today, if `upload_to_testflight` uploads the binary successfully but
  then a subsequent step fails (group distribution, deliver, Sentry),
  re-running the release.yml run will re-archive + re-upload. ASC
  rejects the second upload as duplicate build number, so the retry
  just fails.
- Option: detect in the ios job whether a build with the current
  `CURRENT_PROJECT_VERSION` is already in ASC, and skip the archive +
  upload step in that case. Proceed straight to group assignment and
  `deliver`. This turns "iOS failure" into "just re-run the job" in
  most cases.

**Frontload iOS signals before shipping Android.**
- Android job already `needs: ios` so it can't ship alone, but iOS
  still takes ~10 min warm / ~20 min cold before we know if there's a
  fastlane/ASC problem. A cheap smoke test — `build_only` lane on a
  PR-to-main or on the release-please PR — would catch icon/signing/
  provisioning issues before the tag is cut. Effort: a small workflow
  that builds the iOS archive without uploading, run on the
  release-please PR so broken releases never get merged.

## Release pipeline — smooth the retag dance

See `docs/branching-and-release-strategy.md` for why we stay on trunk
and retag on Apple rejections. These items reduce that ritual from
three manual steps to one.

**One-shot `retag-release` workflow_dispatch.**
- New workflow (or new inputs on release.yml) that takes a version
  string, does `gh release delete --cleanup-tag`, creates the tag at
  the current `origin/main`, and fires release.yml with
  `skip_play_store: true`. Replaces: manual delete + manual retag +
  cancel auto-triggered run + redispatch.
- Effort: ~30 lines of YAML + a shell step. Needs `contents: write`
  permission.

**Make `skip_play_store` respected on tag-push runs.**
- Today, only `workflow_dispatch` sees `skip_play_store`. Tag-push
  always runs Android + Play. That's why a retag needs the cancel +
  redispatch dance.
- Option A: read a repo variable (e.g. `vars.SKIP_PLAY_STORE_NEXT`) in
  the android job's `if:` — set before tag push, unset after.
- Option B: tag-name convention — `v1.0.1-ios` skips Play, `v1.0.1`
  ships both. Ugly but no manual toggle.
- Option C: annotated-tag message convention — parse the tag message
  for `skip-play-store` and branch on it.
- Lean A. Smallest footprint, signal lives in repo state rather than
  in tag soup. Combined with the retag action above, the ritual
  becomes: click one button in Actions.

**Release-please dry-run action.**
- `workflow_dispatch` that runs release-please with `--dry-run` and
  posts the planned version + changelog as a comment on the workflow
  run. Catches "oh, it's going to propose 1.0.2 when I wanted a 1.0.1
  resubmit" before merging the release-please PR.
- Effort: one workflow step + a comment poster.

## Reliability / observability

**Audio cues die when another app takes over audio in the background (iOS + Android).**

Repro: start timer → background app → start music. Cues stop playing.
If music was already playing before the timer started, cues keep
working normally. Reproduced on both iOS and Android.

Root cause on iOS (high confidence, from reading
`IosAudioCuePlayerFactory.kt` + `IosRunnerForegroundController.kt`):

- Session is `.playback + .mixWithOthers`, kept alive by a silent
  `AVAudioEngine` loop so iOS doesn't suspend the process
  mid-workout.
- Per-cue ducking calls `setCategory(.playback, mode, mix | duck)`
  and resets to `mix` after a delay.
- No subscription to `AVAudioSession.interruptionNotification` or
  `routeChangeNotification`.
- Every `setActive` / `setCategory` call passes `null` for the error
  pointer — any failure is swallowed silently.

When another audio app takes primary in the background, iOS fires an
interruption. The silent engine can get stopped, the session can be
implicitly deactivated, and the next cue's `player.play()` becomes a
silent no-op. iOS then fully suspends the process because no audible
output is happening. Nothing in our code detects or recovers from
this.

Android has the same shape of problem via `AudioFocus` — transient
focus loss without a reacquire makes the next `MediaPlayer.start()`
a silent no-op.

**Fix plan:**
1. Subscribe to `AVAudioSession.interruptionNotification`. On `.ended`
   with `.shouldResume`, call `setActive(true)` and restart the silent
   engine if it's stopped.
2. Subscribe to `routeChangeNotification` — reactivate on
   `.oldDeviceUnavailable` / `.categoryChange`.
3. Stop toggling the session category per cue. Set
   `.playback + .mixWithOthers + .duckOthers` once for the life of
   the session. If the "unduck between cues" feel matters, use
   `AVAudioPlayer.volume` envelope, not `setCategory`.
4. Before every `player.play()`, check session state; reactivate if
   inactive (and log that we had to).
5. Pass real error pointers to every `setCategory` / `setActive` call.
6. Mirror the pattern on Android — `AudioFocusRequest` with a change
   listener, reacquire on transient loss, log on denial.

**Telemetry plan (even if the fix isn't in yet, this gives
visibility):**

Sentry breadcrumbs for every audio-lifecycle event:
- Session activation / deactivation.
- Category change with before/after options.
- Interruption began / ended.
- Route change with reason code.
- `player.play()` called → on the next runloop tick, check
  `player.isPlaying` and record a breadcrumb with the result.

Sentry errors (non-fatal) for:
- Any `setActive(true, ...)` that returns false.
- Any `setCategory(...)` that errors out.
- Any `player.play()` where `player.isPlaying` is false 100 ms later
  (the cue was requested but nothing played).
- On Android: `AudioManager.requestAudioFocus(...)` returning anything
  other than `AUDIOFOCUS_REQUEST_GRANTED`, or `onAudioFocusChange`
  receiving `LOSS_TRANSIENT*` without a subsequent `GAIN`.

With this, Sentry gets an issue each time the bug happens in the
field, breadcrumbs show the sequence (background → other audio
started → interruption → no recovery). No user reports needed.

**Effort:** 1–2 days. Most time goes into the interruption + route
change handlers and verifying the recovery across Music / Spotify /
Podcasts / Phone call scenarios on device.
