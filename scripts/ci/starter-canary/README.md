# Cloud starter canary

Nightly end-to-end check that a new user can still reach a first cloud build:
sign in, download the personalised starter from the console, run the shipped
launcher with a real Maven, and require the cloud build to finish — on Linux
and Windows.

## Why it runs against the live service

The starter is generated and personalised server-side, then resolves its
dependencies over the network on the user's own machine. Neither of those is
visible to a check that reads the template out of a repository or replaces
Maven with a stub — and both have broken in ways that made a first build
impossible while every offline check stayed green: a generated pom with no
dependency repository, launchers shipped without the execute bit, and a batch
launcher that quietly produced a local jar instead of submitting a build.

## Setup

1. Create a dedicated build account for the canary.
2. Add three repository secrets:

   | Secret | Value |
   |---|---|
   | `CN1_CANARY_EMAIL` | the account's email |
   | `CN1_CANARY_PASSWORD` | its console password, for the starter download |
   | `CN1_CANARY_TOKEN` | its build token, for headless build-client auth |

   Optionally set the `STARTER_CANARY_ASSIGNEE` repository *variable* to change
   who gets assigned the alert issue.
3. Run it once by hand — **Actions → Cloud starter canary → Run workflow** —
   before trusting the schedule.

Until the secrets exist the scheduled run fails and opens the tracking issue.
An unconfigured canary is not a passing one.

## Running it locally

```bash
export CN1_CANARY_EMAIL=... CN1_CANARY_PASSWORD=... CN1_CANARY_TOKEN=...

# artefact checks only, no build submitted
python3 scripts/ci/starter-canary/starter_canary.py --skip-build

# the full journey
python3 scripts/ci/starter-canary/starter_canary.py --target javascript
```

Apple targets cost several times a normal build and the script refuses them.

## The assertions

`test_starter_canary.py` runs on every PR touching this directory and proves
each assertion still fires for the breakage it was written for. A canary that
decays into a green check verifying nothing is the failure mode it exists to
end.

```bash
python3 scripts/ci/starter-canary/test_starter_canary.py
```

## When it fails

The alert job opens or updates a single issue, assigns it, and re-comments at
most once a day while it stays broken, closing it automatically on recovery.
The cause is usually server-side: the starter generator, the vendored starter
template, or the console sign-in redirect.
