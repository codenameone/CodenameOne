# Triaging Production Crashes from an Agent

Crash Protection files every crash from a shipped build as a GitHub issue. The build cloud has
already symbolicated it (dSYM on Apple, debug info on Linux and Win32, the R8/ProGuard
`mapping.txt` on Android, the hardening map on a hardened build), so the issue holds a readable
stack trace with your own class names and line numbers. Nothing else stores the crash: the issue
tracker is the crash database, so you read it with the same GitHub tools you already use for code.

Read this when the developer asks why the app crashes, what broke in the last release, or to fix
crashes users hit.

## Is it set up?

Crash Protection needs a Pro or Enterprise plan (`whoami` on the BuildCloud MCP server reports
the plan, see `cloud-agent.md`). The project needs three things. If one is missing, add it and tell
the developer. Crashes only arrive from release builds made after the change.

1. `codename1.arg.crashProtection.enabled=true` in `codenameone_settings.properties` (`common/` in Maven, the root in Gradle).
   Without it the build uploads no symbols, and the cloud drops that build's reports.
2. `CrashProtection.install()` in `Lifecycle.init`, plus `CrashProtection.setEnabled(true)`
   once the user has agreed. Reports are kept on the device until then, so they aren't lost.
3. A repository mapping from the app's package to a GitHub repository, set in the console under
   **Repo mappings**. Installing the Crash Protection GitHub App is done in the developer's
   browser, so ask them to do it. To check the mapping from the agent, call
   `list_api_endpoints` with `crash` and read the result. The issues may land in a different
   repository, or a different organization, from the source code.

## Read the crashes

Use `gh` (or the GitHub MCP server) against the mapped repository:

```bash
gh issue list --repo <owner>/<crash-repo> --state open --limit 50
gh issue view <number> --repo <owner>/<crash-repo>
```

Each issue has one crash fingerprint. Its header gives the package, app version, platform and
OS version, first and last seen dates, and an occurrence count. A repeat of the same crash raises
the count on the existing issue instead of opening a new one. So:

- **Rank by impact:** occurrence count first, then how recent "last seen" is. A crash that's
  only on an old app version may already be fixed.
- **Check the version:** compare the issue's app version with `codename1.version` in the
  settings. If it's older than the current release, check whether the frame still exists in the
  code before you change anything.
- **Read from your own frames:** start with the first frame in the app's package, not the top of
  the trace. The frames above it are usually Codename One or the OS reacting to what your code
  did. For a `NativeCrash` (a signal such as `SIGSEGV`), look for the native interface call near
  your frames. See `native-interfaces.md`.
- **Messages are scrubbed:** emails, tokens and similar values are masked on the device before
  sending. Don't treat a masked value as the cause.

## Fix and close the loop

1. Reproduce it if you can. Drive the same flow in the simulator (`mcp-agent-control.md`), or
   in a debug build on a device for a platform-specific crash. Add a JUnit or `cn1:test`
   test that fails the same way (`junit-testing.md`).
2. Fix it in the source and run the tests. Before reporting it done, go through the sanity-check
   loop in `SKILL.md`.
3. Reference the issue in the commit or PR (`Fixes <owner>/<crash-repo>#<number>`) so the fix
   and the crash are linked. If the crash repository differs from the code repository, use the
   full `owner/repo#n` form.
4. Ship the fix through the cloud (`cloud-agent.md`): build, then submit to `BETA` first.
5. Don't close the issue just because the fix merged. Leave a comment naming the version that
   carries the fix. When that version is live, a new occurrence means the fix didn't hold: it
   comes back on the same issue with a newer app version.

## Rules

- Don't paste stack traces or device details from the issues into public places. The crash
  repository may be private for a reason.
- Don't turn on `crashProtection` for the simulator or debug builds to "test" it. `install()`
  does nothing on the simulator, and debug builds have no uploaded symbols.
- Ask before changing the user-consent flow (`setEnabled`). The developer owns that decision.
