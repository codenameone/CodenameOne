<!-- Licensed under the PolyForm Noncommercial License 1.0.0 -->

JavaScript Port Status (ParparVM)
=================================

Last updated: 2026-04-09

Current State
-------------

- Startup/protocol is no longer the primary blocker. Main remaining blockers are in screenshot correctness and screenshot pipeline throughput.
- `ensureDisplayEdt()` and diagnostics remain active in `port.js`.
- Form constructor `IllegalStateException` bypass now attempts recovery instead of returning `null`:
  - calls default `Form` constructor
  - reapplies layout
  - reapplies title (for title+layout constructor)
  - emits `PARPAR:DIAG:FALLBACK:formCtor*:recoverApplied=1` markers
- CI artifact behavior and fresh local rebuild behavior are currently diverged (details below).

Next Steps
----------

1. Unify build provenance first:
   - Confirm CI bundle contains expected translated `Cn1ssDeviceRunner` variant.
   - In generated `translated_app.js`, verify whether runner uses `TEST_CLASSES` (old list path) or `DEFAULT_TEST_CLASSES/prependedTest` (new array path).
   - This directly changes failure mode and must be deterministic before further triage.
2. Reduce EDT starvation in screenshot emission:
   - Current fresh local run advances to 11 tests, then stalls after `DrawImage` due very large `CN1SS:<name>:<chunk>` emissions.
   - Add bounded chunk/preview strategy for extremely large screenshots and move heavy conversion off hot EDT path where possible.
3. Keep form-constructor fallback bounded:
   - `IllegalStateException` still occurs frequently.
   - Recovery now preserves object state better, but we need to reduce recursive/looping constructor retry behavior and make one-shot recovery per form instance.
4. After throughput fix, run 3 repeated local runs and one CI run:
   - Exit gate is `CN1SS:SUITE:FINISHED` + 33 screenshot streams + no repeated stream collapse.

Important Notes
--------------

- Fresh local rebuild (`/tmp/cn1-js-fresh*.zip`) now translates current runner code and shows 11 unique tests before timeout.
- Existing CI artifact in `javascript-ui-tests/HelloCodenameOne-js/translated_app.js` still shows old list-based runner (`TEST_CLASSES`) and produces only 5 screenshot streams with suite completion.
- This means there are at least two active failure modes:
  1. old-runner path: suite finishes with only 5 streams (wrong collapse)
  2. new-runner path: progresses further but times out during heavy screenshot emission

Known Important Context
-----------------------

- Useful diagnostics to grep:
  - `PARPAR:DIAG:FALLBACK:lambdaBridge:capturedTest=...:capturedIndex=...`
  - `PARPAR:DIAG:FALLBACK:formCtorLayout:bypassIllegalState=1`
  - `PARPAR:DIAG:FALLBACK:formCtorLayout:recoverApplied=1`
  - `CN1SS:INFO:suite starting test=...`
  - `CN1SS:SUITE:FINISHED`
- Current local patch set touches:
  - `Ports/JavaScriptPort/src/main/webapp/port.js`
  - `scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.java`
