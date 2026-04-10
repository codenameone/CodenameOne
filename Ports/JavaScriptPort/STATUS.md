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
- CI timeout root cause shifted:
  - We no longer stall on missing lambda virtual method resolution for `Cn1ssDeviceRunner_lambda_runNextTest_*`.
  - Lambda fallback now handles both method signatures:
    - `..._java_lang_String_com_codenameone_examples_hellocodenameone_tests_BaseTest_int`
    - `..._java_lang_String_int_com_codenameone_examples_hellocodenameone_tests_BaseTest`
- Local end-to-end harness run now reaches:
  - `CN1SS:SUITE:FINISHED`
  - `TOP_BLOCKER=none|none|none`
- Remaining blocker class is now screenshot/runtime correctness, not suite completion:
  - Most tests fail during `prepare`/`runTest` path with
    - `TypeError: Cannot read properties of null (reading '__classDef')`
  - Browser logs show per-test failures (not missing virtual dispatch) and only fallback/default image streams in current artifacts.
- Lambda bridge progression hardening now includes:
  - Forced advance path when `finalizeTest()`/`awaitTestCompletion()` fails.
  - Forced next-index carryover (`__cn1ForcedNextIndex`) to prevent stale lambda-capture loops.
  - Additional run-phase diagnostics (`prepare` vs `runTest`) and stack snippets for next-stage root-cause analysis.

Next Steps
----------

1. Root-cause `TypeError: ... __classDef` in test execution path:
   - Use new lambda bridge phase diagnostics to determine whether first fault is in `BaseTest.prepare` or `BaseTest.runTest`.
   - Capture first failing stack and identify null receiver origin (most likely missing virtual owner delegate or missing init path returning null component/form/object).
2. Fix runtime object/dispatch correctness for startup drawing/form tests:
   - Prioritize first deterministic failure (`KotlinUiTest`, then `MainScreenScreenshotTest`, then graphics tests).
   - Remove reliance on broad force-advance once root cause is fixed.
3. Validate screenshot stream emission per test:
   - Ensure each test emits a named stream chunk (not just `default`) so screenshot parser can materialize expected artifacts.
4. Continue worker-only boot validation:
   - Required markers: `PARPAR:worker-mode`, `PARPAR:DIAG:BOOT:bridgeMode=worker`.
   - Any `main-thread-mode` marker now indicates stale artifact or wrong bundle.
5. Confirm worker native rebind fix is present in produced bundle:
   - In generated `worker.js`, ensure `__parparInstallNativeBindings()` is invoked after imports and before `start`.
   - This must eliminate `Window.current()` null stubs from startup execution.
6. Separate VM/EDT execution from main-thread host services cleanly:
   - Keep VM/EDT scheduling in worker.
   - Ensure main-thread browser APIs are reached through explicit host-call handlers rather than direct worker DOM access.
7. Restore full screenshot count and correctness:
   - Exit gate remains `CN1SS:SUITE:FINISHED` with expected screenshot artifacts and no `BROWSER:PARPAR_ERROR`.

Important Notes
---------------

- In this Codex environment, local browser harness runs require out-of-sandbox execution due socket bind restrictions.
- With elevated execution, local run is now useful for fast iteration and reproduces the same class of runtime failures seen in CI.

Known Important Context
-----------------------

- Useful diagnostics to grep:
  - `PARPAR:worker-mode`
  - `PARPAR:DIAG:BOOT:bridgeMode=worker`
  - `PARPAR:DIAG:FIRST_FAILURE:category=worker_missing`
  - `PARPAR:DIAG:FALLBACK:lambdaBridge:capturedTest=...:capturedIndex=...`
  - `PARPAR:DIAG:FALLBACK:lambdaBridge:runError:phase=prepare|runTest`
  - `PARPAR:DIAG:FALLBACK:lambdaBridge:runErrorStack=...`
  - `PARPAR:DIAG:FALLBACK:formCtorLayout:bypassIllegalState=1`
  - `PARPAR:DIAG:FALLBACK:formCtorLayout:recoverApplied=1`
  - `CN1SS:INFO:suite starting test=...`
  - `CN1SS:SUITE:FINISHED`
- Current local patch set touches:
  - `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
  - `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`
  - `Ports/JavaScriptPort/src/main/webapp/port.js`
  - `scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.java`
