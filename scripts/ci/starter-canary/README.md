# Cloud starter canary

Nightly black-box check that a **new user can still reach a first cloud build**.
It signs in to production, downloads the personalised starter from the console,
runs the launcher the README tells the user to run, and requires the cloud build
to finish — on Linux and Windows.

## Why it exists

Between 2026-07 and 2026-09 the first-build path was broken on every operating
system, and each break was invisible for weeks because nothing exercised the
artefact a user actually receives:

| Fix | Break |
|---|---|
| BuildCloud #139 (09-05) | the starter `pom.xml` declared **no `<repositories>`**; once Codename One left Maven Central at 7.0.268 a pinned starter resolved nothing |
| BuildCloud #144 (09-12) | the ZIP shipped `build.sh`/`run.sh`/`mvnw` **without the execute bit** — `permission denied` on the first documented command |
| BuildCloud #146 (09-13) | `build.bat` **fell through to a local jar** when given no target: no cloud build, no message |

Production telemetry for the 30 days to 2026-09-15 shows what that cost: of 71
real signups, 41 never reached the console and, of the 30 who did, **12
downloaded the starter and never attempted a build** — the single largest
drop-off bucket. Zero attempts failed. Nothing was wrong with building; people
could not get that far.

## Why here and not in BuildCloud

BuildCloud's Actions minutes are metered (3,000/month, shared with BuildDaemon),
so a nightly job would eat them. Public repositories get free minutes.

Nothing here touches BuildCloud source, its database or its secrets — it is a
pure HTTP client against public production endpoints, so there is no IP to move.

It **complements** BuildCloud's `scripts/tests/starter-launchers.py` rather than
replacing it. That test stubs Maven out with a recorder and reads the template
straight from git, so it is a good cheap PR check of launcher mechanics — but it
resolves no dependency and submits no build, so it could not have caught #139,
and it never sees the generator's output, so it could not have caught #144.

## Setup (one time, needs a human)

1. **Create a dedicated canary account** on cloud.codenameone.com.
   Give it **GOD tier**. Every admin funnel query filters `type >= USER_TYPE_GOD`,
   so a GOD account cannot inflate the activation numbers this canary exists to
   protect. The trade-off is deliberate and worth stating: GOD skips the
   free-tier credit and one-concurrent-build gates, so the canary covers
   starter/toolchain breakage, not billing gates.
2. **Get its long-lived build token** — the value `cn1:set-user-token` would
   store (`user_obj.app_token`).
3. Add three repository secrets:

   | Secret | Value |
   |---|---|
   | `CN1_CANARY_EMAIL` | the canary account's email |
   | `CN1_CANARY_PASSWORD` | its console password (for the session-cookie starter download) |
   | `CN1_CANARY_TOKEN` | its build token (for headless build-client auth) |

   Optionally set the `STARTER_CANARY_ASSIGNEE` repository *variable* to change
   who gets assigned the alert issue (defaults to `shai-almog`).
4. Run it once by hand — **Actions → Cloud starter canary → Run workflow** —
   before trusting the schedule.

Until the secrets exist the scheduled run fails with a clear message and opens
the tracking issue, which is the correct behaviour: an unconfigured canary is
not a passing one.

## Running it locally

```bash
export CN1_CANARY_EMAIL=... CN1_CANARY_PASSWORD=... CN1_CANARY_TOKEN=...

# artefact checks only, no build credit spent
python3 scripts/ci/starter-canary/starter_canary.py --skip-build

# the full journey
python3 scripts/ci/starter-canary/starter_canary.py --target javascript
```

**Never point `--target` at `iphone*` or `macos`.** Those cost 8 credits per
build against a 100/month allowance; the script refuses them outright.

## The assertions

`test_starter_canary.py` runs on every PR that touches this directory and proves
each assertion still fires for the regression it was written for. A canary that
degrades into a green check that verifies nothing is the exact failure mode this
whole thing exists to end.

```bash
python3 scripts/ci/starter-canary/test_starter_canary.py
```

## When it fails

The alert job opens (or updates) a single issue titled *"Cloud starter is broken
for new users"*, assigns it, and re-comments at most once a day while it stays
broken. It closes the issue automatically on recovery.

The fix almost always belongs in **BuildCloud**, not here — `StarterProjectService`,
the vendored starter template under `src/main/resources/onboarding/starter-template/`,
or the console sign-in redirect.
