Sentry triage routine.

Scheduled Claude run. Pull the most impactful unresolved Sentry issues for HIIT Timer, pick the ones fixable from app code, open one pull request per fix.

Environment:
- SENTRY_ORG, SENTRY_PROJECT, SENTRY_AUTH_TOKEN for Sentry API access.
- GH_TOKEN for GitHub API via gh.
- MAX_ISSUES cap on PRs opened per run, default 5.
- DRY_RUN when true, report without opening PRs.

Procedure:

1. Pull top unresolved production issues over the last 7 days, sorted by frequency. GET sentry.io/api/0/projects/$SENTRY_ORG/$SENTRY_PROJECT/issues with query "is:unresolved environment:production", sort freq, statsPeriod 7d, limit MAX_ISSUES. The `environment:production` filter is mandatory — debug/preview crashes do not ship to users and must not be fixed by this routine.

2. Fetch the latest event for each issue from sentry.io/api/0/issues/$ISSUE_ID/events/latest/ to get stack trace and tags.

3. Filter out issues where the top frame is in a third-party SDK, system framework, or vendored library you cannot edit. Also skip user-environment fingerprints (network timeout, disk full, bare cancelled coroutine). Skip issues with an open PR already referencing them (gh pr list --search ISSUE_ID). Skip issues already resolved on main (git log --all --grep ISSUE_ID). For skipped issues, open a tracking GitHub issue so they stay visible.

4. For each triageable issue:
   - Create branch ai/sentry-SHORT_ID.
   - Read relevant source. Form a root-cause hypothesis.
   - Apply the smallest fix plausible. Prefer defensive checks at the failure site over rewrites.
   - Add a regression test only if reproducible from a unit test.
   - Verify build with gradlew compileDebugKotlinAndroid and compileKotlinIosSimulatorArm64, plus testDebugUnitTest if tests were added.
   - If the build fails and cannot be fixed in one more attempt, abandon the branch and open a tracking issue.

5. Open one PR per fix:
   - Title in the form fix: terse description (conventional commit — release-please uses this for the changelog).
   - Body contains a Sentry link, a 1-2 sentence hypothesis, and what changed.
   - Labels ai-autofix and sentry. The ai-autofix label triggers auto-merge when CI is green.

6. Write a GITHUB_STEP_SUMMARY entry listing issues seen, PRs opened, and skipped issues with reasons.

Hard limits:
- Do not modify anything under .github/workflows.
- Do not bump dependency versions. If a dep is at fault, open a tracking issue.
- Do not edit versions.properties, CHANGELOG.md, or Config.xcconfig — release-please owns them.
- Do not force-push, rebase published branches, or delete branches you did not create.
- If DRY_RUN is true, do everything except gh pr create — print the diff and intended PR body instead.
