<!-- Licensed under the PolyForm Noncommercial License 1.0.0 -->

JavaScript Port Status (ParparVM)
=================================

Last updated: 2026-04-11

Current State
-------------

- Worker-only architecture is active:
  - VM/EDT runs in `worker.js`.
  - Browser/DOM access routes through host bridge (`browser_bridge.js` + host-call messages).
- CI/local logs now reach:
  - `CN1SS:SUITE:FINISHED`
  - `TOP_BLOCKER=none|none|none`
- The screenshot pipeline now decodes/report-generates reliably from logs, but screenshot content is still mostly wrong (white-frame capture path is still being used in CI artifacts).
- New patch (not yet CI-validated in this document revision): worker-side fallback screenshot path now waits for a host-side UI-settle barrier before ready-callback dispatch and before canvas capture, to avoid pre-paint frame capture.
- Note: a short-lived `forceShow()` experiment in `BaseTest.registerReadyCallback` was reverted because it caused re-entrant callback loops and prevented `CN1SS:SUITE:FINISHED`.
- New patch (not yet CI-validated in this document revision): host screenshot capture now tracks the real draw target canvas and includes off-DOM canvases reachable via host refs.
- New patch (not yet CI-validated in this document revision): screenshot candidate selection now prioritizes near-screen-sized canvases to prevent tiny offscreen buffers from being selected (fixes unexpected `120x80`/`4x4` outputs).

What Was Fixed In This Pass
---------------------------

1. Screenshot report pipeline no longer aborts immediately on `CN1SS:ERR` lines, and synthetic `default` stream no longer causes fatal decode failure when named streams exist.
   - File: `scripts/run-javascript-screenshot-tests.sh`
   - Fixes:
     - Continue after `cn1ss_print_log` non-zero status (report generation still runs).
     - Filter out synthetic `default` stream when named streams are present.
     - Treat residual synthetic `default` decode failures as non-fatal in multi-stream runs.
   - Verified locally:
     - Replaying latest `javascript-device-runner.log` now exits `0`, writes compare/comment artifacts, and does not fail on `default`.

2. Runtime key/identity robustness hardening in ParparVM.
   - File: `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`
   - Changes:
     - `System.identityHashCode(Object)` now assigns stable ids for non-VM JS objects using direct `__id` when possible and `WeakMap` fallback otherwise.
     - `HashMap.areEqualKeys(Object,Object)` now handles Java-string/JS-string comparison safely before virtual dispatch.
   - Motivation:
     - Addresses suspected object-identity/equality instability contributing to map lookup anomalies and flaky behavior.

3. Host screenshot canvas selection switched from "largest canvas only" to content-aware selection.
   - File: `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
   - Changes:
     - Added canvas visual scoring (opaque/non-white sampled content) and area tie-break.
     - Added diagnostics for selected canvas (`canvasCount`, `canvasPick`, `canvasArea`, `canvasScore`, `pngLen`).
   - Motivation:
     - Current CI artifacts show 29/32 screenshots with identical hash (white/blank frame reuse), indicating we were repeatedly capturing the wrong canvas.

4. Added LinkedHashMap non-null-key native lookup override.
   - Files:
     - `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`
     - `vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptNativeRegistry.java`
   - Changes:
     - `cn1_java_util_LinkedHashMap_findNonNullKeyEntry_java_lang_Object_int_int_R_java_util_HashMap_Entry`
       now delegates to the runtime HashMap lookup native.
   - Motivation:
     - CI flake in `JavascriptCn1CoreCompletenessTest` (`expected 7 got 3`) points to JSON map key lookup path instability on worker runtime.

5. Fixed `Object.getClass()` null handling to throw Java NPE instead of raw JS TypeError.
   - File: `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`
   - Fix:
     - `cn1_java_lang_Object_getClass*` native now throws `java_lang_NullPointerException` when receiver is null.
   - Motivation:
     - Avoids leaking JS `Cannot read properties of null (reading '__classDef')` from core object/class paths.

6. Hardened worker harness result parsing for VM tests.
   - File: `vm/tests/src/test/java/com/codename1/tools/translator/JavascriptRuntimeSemanticsTest.java`
   - Fix:
     - Extract JSON fields from the last JSON object line containing `"type"` instead of scanning entire stdout blindly.
   - Motivation:
     - Reduces false extraction of intermediate `"result"` fields in noisy worker output.
   - Local check:
     - `JavascriptCn1CoreCompletenessTest#executesMeaningfulCodenameOneCoreSliceInWorkerRuntime` passes locally after this update.

7. Added explicit host-side UI settle barrier and wired it into screenshot readiness/capture flow.
   - Files:
     - `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
     - `Ports/JavaScriptPort/src/main/webapp/port.js`
   - Changes:
     - New host native `__cn1_wait_for_ui_settle__` waits across RAF frames and monitors canvas signature/score stability.
     - `BaseTest.registerReadyCallbackImmediate` now waits on host UI settle before invoking callback.
     - `Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshotDom` now waits on host UI settle before `__cn1_capture_canvas_png__`.
     - Added diagnostics:
       - `PARPAR:DIAG:SCREENSHOT_START:settleReason=...`
       - `PARPAR:DIAG:SCREENSHOT_START:settleChanged=...`
       - `PARPAR:DIAG:FALLBACK:baseTestRegisterReady:afterUiSettle=1:...`
   - Motivation:
     - CI showed near-identical screenshot payloads (same hash/size) indicating repeated capture of a stale frame rather than per-test painted UI.

8. Reverted force-show experiment due re-entrancy regression.
   - File:
     - `Ports/JavaScriptPort/src/main/webapp/port.js`
   - Observation:
     - Calling `Form.show()` inside `BaseTest.registerReadyCallbackImmediate` caused repeated `registerReadyCallback`/settle cycles on the same test and CI timeout before suite completion.
   - Action:
     - Removed `forceShow()` from this fallback path.
     - Kept the host-side settle barrier and diagnostics (non-recursive).

9. Added draw-target aware screenshot capture in browser bridge.
   - File:
     - `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
   - Changes:
     - Host JSO bridge now records the last canvas that received draw operations.
     - Screenshot candidate selection now includes:
       - DOM canvases
       - last draw target canvas
       - canvases reachable through host refs (including context `.canvas`)
     - Added diagnostic:
       - `PARPAR:DIAG:SCREENSHOT_START:canvasSource=...` (`dom`, `lastDraw`, `hostRef`, `hostRefCanvas`)
   - Motivation:
     - Current logs show active drawing calls while DOM-canvas snapshots remain static/white; this addresses likely off-DOM/back-buffer capture misses.

10. Added large-canvas gating for screenshot candidate selection.
   - File:
     - `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
   - Changes:
     - Evaluate all candidate canvases, compute max area, and prefer candidates with area >= 45% of max (with floor at 65536).
     - Only fall back to tiny-canvas pool if no large candidates exist.
     - Added diagnostics:
       - `PARPAR:DIAG:SCREENSHOT_START:canvasConsidered=...`
       - `PARPAR:DIAG:SCREENSHOT_START:canvasLargeCount=...`
       - `PARPAR:DIAG:SCREENSHOT_START:canvasMinLargeArea=...`
   - Motivation:
     - Latest artifacts showed mixed dimensions (`120x80`, `4x4`) and colored tiny captures, indicating offscreen utility buffers were being selected.

Known Failing Symptoms (Latest CI Logs/Artifacts)
-------------------------------------------------

- Screenshot suite finishes but many tests fail during `runTest`.
- Repeated deterministic blockers in browser log:
  - `TabsScreenshotTest`: `cn1_com_codename1_ui_Button_initLaf_com_codename1_ui_plaf_UIManager is not defined`
  - `OrientationLockScreenshotTest`: `document is not defined`
  - Multiple tests: `TypeError: Cannot read properties of null (reading '__classDef')`
    - Seen in `ValidatorLightweightPickerScreenshotTest`, `InPlaceEditViewTest`, `StreamApiTest`, `TimeApiTest`.
- Timeout-only tests still timeout by design/behavior (`MediaPlayback...`, `BytecodeTranslatorRegression...`, selected API tests).
- Screenshot pixels are still wrong in CI (host-canvas fallback path remains dominant in logs).

Other CI Signal
---------------

- `JavascriptCn1CoreCompletenessTest.executesMeaningfulCodenameOneCoreSliceInWorkerRuntime` has reported flaky `expected: <7> but was: <3>` in CI.
- Local single-method repetition passed in this workspace after the runtime hardening above, but CI confirmation is still required.

Priority Next Steps
-------------------

1. Validate CI output after UI-settle barrier:
   - Expect non-identical PNG hashes for distinct tests.
   - Confirm `settleChanged`, `canvasSig`, and `canvasSource` diagnostics vary across tests.
2. Validate size normalization after large-canvas gating:
   - Expect screenshot dimensions to remain consistent at app target size (no `120x80`/`4x4` non-bootstrap outputs).
3. If white-frame reuse persists, capture and compare per-test `settleSig`/`canvasSig`/`canvasSource` to identify whether paint is not happening or capture target is still wrong.
3. Fix per-test null receiver/init path (`__classDef` null) at first failing stack, not via broad fallbacks.
4. Fix missing `Button.initLaf(UIManager)` symbol resolution in worker runtime path.
5. Fix worker-mode orientation lock path so DOM access is host-bridge mediated (no direct `document` access in worker).
6. Confirm VM completeness stability in CI with parser/runtime patches (`expected 7` consistently).

Files Touched In This Pass
--------------------------

- `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
- `Ports/JavaScriptPort/src/main/webapp/port.js`
- `Ports/JavaScriptPort/STATUS.md`
