<!-- Licensed under the PolyForm Noncommercial License 1.0.0 -->

JavaScript Port Status (ParparVM)
=================================

Last updated: 2026-04-10

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
- Added explicit worker-native bindings for initial DOM bridge path:
  - `Window.getDocument()`
  - `HTMLDocument.createElement(String)`
  - `HTMLDocument.getBody()`
  - `HTMLDocument.getElementById(String)`
- Worker/main-thread JSO bridge introduced:
  - `parparvm_runtime.js` now routes JSO calls on host refs through `host-call` (`__cn1_jso_bridge__`).
  - `browser_bridge.js` now resolves host refs and executes getter/setter/method operations on main-thread DOM objects.
  - `Window.current()` now requests host window in worker mode (instead of using `self.window` shim).
- Host-ref class metadata added:
  - host markers include `__cn1HostClass`, and runtime class inference consumes it.
  - this moved startup failure from null receiver to later bridge/cast and callback transport issues.
- Latest local repro first blocker moved again:
  - `DataCloneError: Failed to execute 'postMessage' ... function(...) could not be cloned`
  - cause: non-cloneable callback/function payload crossing worker host-call boundary.
  - mitigation added: runtime host-call argument sanitization (`toHostTransferArg`) before `emitVmMessage`.
- Latest CI artifact (`~/Downloads/javascript-ui-tests/browser.log`) first blocker is:
  - `Error: Missing host receiver for JSO bridge`
  - stack points to worker runtime host-callback error after `__cn1_jso_bridge__`.
- Additional fix applied (not yet CI-verified):
  - worker JSO host-call request now includes `receiverClass` hint.
  - host bridge now attempts class-based receiver fallback (`Window`/`Document` and `JSOImplementations_*` classes) before throwing.
  - missing-receiver diagnostics now also emit `hostReceiverClass`.
- Existing form-constructor recovery diagnostics remain active in `port.js` and are still relevant while migrating.

Next Steps
----------

1. Validate latest JSO receiver rehydration fix in CI artifacts:
   - Check whether first failure moved off `Missing host receiver for JSO bridge`.
   - Confirm new diagnostics include `hostReceiverClass` when missing.
   - If still present, capture exact failing symbol/member/class and add targeted fallback for that class.
2. Continue worker-only boot validation:
   - Required markers: `PARPAR:worker-mode`, `PARPAR:DIAG:BOOT:bridgeMode=worker`.
   - Any `main-thread-mode` marker now indicates stale artifact or wrong bundle.
3. Confirm worker native rebind fix is present in produced bundle:
   - In generated `worker.js`, ensure `__parparInstallNativeBindings()` is invoked after imports and before `start`.
   - This must eliminate `Window.current()` null stubs from startup execution.
4. Separate VM/EDT execution from main-thread host services cleanly:
   - Keep VM/EDT scheduling in worker.
   - Ensure main-thread browser APIs are reached through explicit host-call handlers rather than direct worker DOM access.
5. Re-triage screenshot correctness in worker mode only:
   - Re-run screenshot suite and classify first blocker using the existing `TOP_BLOCKER` output.
   - Prioritize deterministic runtime failures before throughput tuning.
6. Restore full screenshot count and correctness:
   - Exit gate remains `CN1SS:SUITE:FINISHED` with expected screenshot artifacts and no `BROWSER:PARPAR_ERROR`.

Important Notes
---------------

- This Codex environment currently cannot run the local browser harness end-to-end due sandbox socket restrictions:
  - `PermissionError: [Errno 1] Operation not permitted` from `javascript_browser_harness.py` bind.
  - CI artifacts remain the source of truth for runtime progression.

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
  - `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`
  - `Ports/JavaScriptPort/src/main/webapp/port.js`
  - `scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.java`
