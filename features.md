# Future features

Ideas worth building, not scheduled.

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
