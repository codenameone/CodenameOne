<!-- Licensed under the PolyForm Noncommercial License 1.0.0 -->

JavaScript Port Status (ParparVM)
=================================

Last updated: 2026-04-09

Current State
-------------

- Architecture direction has been explicitly shifted to worker-first execution for ParparVM (EDT and VM scheduler in worker, browser UI/native host on main thread).
- `browser_bridge.js` is now worker-only. Main-thread VM mode and mode toggles were removed.
- If worker support is missing, runtime now fails explicitly with:
  - `PARPAR:worker-mode-required`
  - `PARPAR:DIAG:FIRST_FAILURE:category=worker_missing`
- Worker host-call misses now emit explicit first-failure diagnostics:
  - `PARPAR:DIAG:FIRST_FAILURE:category=host_call_unhandled`
  - `PARPAR:DIAG:FIRST_FAILURE:symbol=<nativeSymbol>`
- Worker native-rebind bug identified and fixed:
  - Cause: `translated_app.js` redefined native stubs after `port.js` bind phase in worker startup order.
  - Fix: `worker.js` now calls `__parparInstallNativeBindings()` after imports, before handling `start`.
- Bundle generation ordering bug identified and fixed for worker mode:
  - Cause: `worker.js` was generated before `port.js` was copied into the output bundle, so worker never imported JavaScriptPort natives.
  - Fix: copy JavaScriptPort assets before `worker.js` generation and keep service-worker/shell scripts excluded from worker imports.
- Latest CI artifact confirms `worker.js` now imports `port.js`, but startup still fails in `HTML5Implementation.__init`:
  - first failure remains `TypeError: Cannot read properties of null (reading '__classDef')`
  - crash site moved to `HTMLDocument.createElement(...)` invocation with null document target.
- Added explicit worker-native bindings for initial DOM bridge path:
  - `Window.getDocument()`
  - `HTMLDocument.createElement(String)`
  - `HTMLDocument.getBody()`
  - `HTMLDocument.getElementById(String)`
- Latest CI artifacts now run worker mode but fail early before suite start with:
  - `PARPAR:DIAG:FIRST_FAILURE:category=runtime_error`
  - `TypeError: Cannot read properties of null (reading '__classDef')`
  - stack rooted in `HTML5Implementation.__init` after `Window.current()` returned `null`.
- Existing form-constructor recovery diagnostics remain active in `port.js` and are still relevant while migrating.

Next Steps
----------

1. Validate worker-only boot in CI and local:
   - Required markers: `PARPAR:worker-mode`, `PARPAR:DIAG:BOOT:bridgeMode=worker`.
   - Any `main-thread-mode` marker now indicates stale artifact or wrong bundle.
2. Confirm worker native rebind fix is present in produced bundle:
   - In generated `worker.js`, ensure `__parparInstallNativeBindings()` is invoked after imports and before `start`.
   - This must eliminate `Window.current()` null stubs from startup execution.
3. Separate VM/EDT execution from main-thread host services cleanly:
   - Keep VM/EDT scheduling in worker.
   - Ensure main-thread browser APIs are reached through explicit host-call handlers rather than direct worker DOM access.
4. Re-triage screenshot correctness in worker mode only:
   - Re-run screenshot suite and classify first blocker using the existing `TOP_BLOCKER` output.
   - Prioritize deterministic runtime failures before throughput tuning.
5. Restore full screenshot count and correctness:
   - Exit gate remains `CN1SS:SUITE:FINISHED` with expected screenshot artifacts and no `BROWSER:PARPAR_ERROR`.

Important Notes
--------------

- Current CI artifact (`~/Downloads/javascript-ui-tests/browser.log`) shows:
  - `PARPAR:worker-mode`
  - `PARPAR:DIAG:BOOT:bridgeMode=worker`
  - `TOP_BLOCKER=runtime_error|none|none`
  - first crash in `HTML5Implementation.__init` due null window wrapper.
- This is consistent with native rebind order being a primary startup blocker in worker mode.

Known Important Context
-----------------------

- Useful diagnostics to grep:
  - `PARPAR:worker-mode`
  - `PARPAR:DIAG:BOOT:bridgeMode=worker`
  - `PARPAR:DIAG:FIRST_FAILURE:category=worker_missing`
  - `PARPAR:DIAG:FALLBACK:lambdaBridge:capturedTest=...:capturedIndex=...`
  - `PARPAR:DIAG:FALLBACK:formCtorLayout:bypassIllegalState=1`
  - `PARPAR:DIAG:FALLBACK:formCtorLayout:recoverApplied=1`
  - `CN1SS:INFO:suite starting test=...`
  - `CN1SS:SUITE:FINISHED`
- Current local patch set touches:
  - `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
  - `Ports/JavaScriptPort/src/main/webapp/port.js`
  - `scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.java`
