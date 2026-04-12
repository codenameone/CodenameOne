<!-- Licensed under the PolyForm Noncommercial License 1.0.0 -->

JavaScript Port Status (ParparVM)
=================================

Last updated: 2026-04-12

Latest Investigation Snapshot (this round)
------------------------------------------

- Input artifacts analyzed:
  - `~/Downloads/job-logs.txt`
  - `~/Downloads/javascript-ui-tests/browser.log`
- Confirmed in that artifact set:
  - Suite completes (`CN1SS:SUITE:FINISHED`) and 32 named streams decode.
  - Most PNGs are still identical/white (`canvasSig=7263bb45` repeated).
  - Screenshot helper repeatedly falls back due:
    - `PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:originalInvokeErr=Error: Missing JS member get for host receiver`
- New code changes in workspace (not CI-validated yet):
  1. Host/runtime JSO bridge now supports array-like indexed fallback for `get(index)`/`set(index,value)` when no callable member exists.
  2. Host bridge attempts to avoid per-pixel host RPC by cloning typed-array payloads.
  3. Host bridge has explicit `getter:data` fast-return clone path.
  4. BaseTest on-show lambda shim now guards missing `target.__classDef` by resolving class metadata from `target.__class`.
  5. Screenshot fallback now logs short `originalInvokeStack` for first-failure localization.
- Important caveat from local replay:
  - Replaying patched runtime against the previously archived CI zip can regress to timeout and show `originalInvokeErr=Cannot read properties of null (reading '__classDef')` during per-test screenshot emission.
  - This indicates the first blocker has shifted from missing host `.get` to a null receiver/class-init path in translated screenshot execution.

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
- New patch (not yet CI-validated in this document revision): native rebinding now preserves overwritten translated JS method functions in `jvm.translatedMethods` for targeted fallback/original resolution.
- New patch (not yet CI-validated in this document revision): canvas visual scoring now samples multiple regions (not only center), to better detect non-white drawn content.

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

11. Added translated-method preservation during native rebinding.
   - Files:
     - `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`
     - `Ports/JavaScriptPort/src/main/webapp/port.js`
   - Changes:
     - Runtime now records pre-override translated functions in `jvm.translatedMethods` when installing/reinstalling natives.
     - `Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot...` fallback resolution now checks `jvm.translatedMethods` before globals/class tables.
     - CI fallback wrappers are tagged (`__cn1CiFallbackSymbol`) so resolution can skip recursive self-selection.
   - Motivation:
     - `originalMissing=1` persisted even though translated helper symbols exist in `translated_app.js`; rebinding order was losing discoverability of original translated handlers.

12. Improved screenshot canvas scoring robustness.
   - File:
     - `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
   - Changes:
     - Replaced single-center 48x48 sample with multi-region sampling (center, corners, and edge midpoints).
     - Score still prefers non-white/opaque content, but now reflects content distribution across the frame.
   - Motivation:
     - White-frame capture can occur when center-only sampling misses rendered content (e.g., content concentrated away from center).

13. Fixed screenshot-helper recursion regression that collapsed streams to `default`.
   - File:
     - `Ports/JavaScriptPort/src/main/webapp/port.js`
   - CI symptom:
     - `run-javascript-screenshot-tests` reported:
       - `ERROR: No meaningful screenshots decoded (only default/bootstrap streams were present)`
     - Browser logs showed repeated:
       - `originalResolved=translated:...emitCurrentFormScreenshot...` followed by
       - `originalInvokeErr=Maximum call stack size exceeded`
     - Then only `CN1SS:default` chunks were emitted.
   - Changes:
     - Prefer translated `__impl` method resolution over non-impl wrapper.
     - Added re-entry guard (`cn1ssEmitCurrentFormScreenshotInvokeDepth`) so recursive original calls are bypassed into deterministic host fallback instead of stack overflow.
   - Expected effect:
     - Restore named screenshot stream emission (`CN1SS:<test>`) instead of collapsing to `default`-only stream.

14. Added JSO bridge indexed-access compatibility for array-like receivers.
   - Files:
     - `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
     - `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`
   - Changes:
     - If a bridged `method` call has member `get`/`set` and receiver is array-like (`length`), map to indexed element access when no callable JS member exists.
   - Motivation:
     - Fixes deterministic screenshot-path failure:
       - `Missing JS member get for host receiver`
     - Seen when translated code accesses pixel buffers via `get(index)`.

15. Added host bridge typed-array transfer fast paths.
   - File:
     - `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
   - Changes:
     - `hostResult()` now clones/returns typed-array and `ArrayBuffer` values directly instead of always creating host refs.
     - Added explicit `getter:data` direct-clone return path in `__cn1_jso_bridge__`.
   - Motivation:
     - Prevents pathological per-element host RPC loops during pixel-buffer reads.
   - Status:
     - Local replay indicates this removes the old `Missing JS member get...` failure, but uncovers a later null `__classDef` path that still needs repair.

16. Hardened BaseTest on-show lambda shim for missing class definition.
   - File:
     - `Ports/JavaScriptPort/src/main/webapp/port.js`
   - Changes:
     - `target.__classDef` access now falls back to `jvm.classes[target.__class]` and exits safely with diagnostic if unresolved.
   - Motivation:
     - Targets recurring per-test failure signature:
       - `Cannot read properties of null (reading '__classDef')`
     - This is now the highest-priority blocker after `.get` bridge repair.

17. Fixed Worker-to-main-thread console forwarding for CN1SS output and System.out.println.
   - Files:
     - `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`
     - `Ports/JavaScriptPort/src/main/webapp/port.js`
     - `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
     - `scripts/run-javascript-headless-browser.mjs`
   - Root cause:
     - `System.out.println()` in the VM Worker maps to `printToConsole()` which only calls `console.log()` in the Worker context.
     - Playwright `page.on('console')` does not reliably capture Web Worker console messages emitted during async VM execution (only synchronous module-load-time messages appear).
     - Consequently, `CN1SS:SUITE:FINISHED` and all test chunk data never reach the log file, causing the shell harness to time out.
   - Changes:
     - `printToConsole()` now also calls `emitVmMessage({ type: 'log', message })` to forward `System.out.println` output to the main thread via `postMessage`.
     - `emitDiagLine()` in port.js now also calls `postMessage({ type: 'log', message })` to forward CN1SS chunk data and diagnostic lines.
     - `browser_bridge.js` detects app lifecycle start from worker log messages and sets `window.cn1Started = true` on the main thread.
     - `run-javascript-headless-browser.mjs` now detects `CN1SS:SUITE:FINISHED` in console output and exits early instead of running to its full timeout.
   - Expected effect:
     - `CN1SS:SUITE:FINISHED` reliably appears in the browser log, resolving the CI timeout.
     - All CN1SS chunk data reaches Playwright, enabling screenshot extraction.
     - Playwright exits promptly after suite completion, saving CI time.

18. Fixed screenshot hang caused by canvasToBlob async callback across worker boundary.
    - File: `Ports/JavaScriptPort/src/main/webapp/port.js`
    - Root cause:
      - The translated screenshot method calls `ImageIO.save()` which calls
        `BlobUtil.canvasToBlob()`.  That method uses the async
        `HTMLCanvasElement.toBlob(BlobCallback)` browser API.  In the worker
        architecture the BlobCallback is a Java object that cannot be invoked
        from the host thread, so `canvasToBlob()` hangs forever in
        `while (!complete) { lock.wait(200); }`.
    - Fix:
      - `emitCurrentFormScreenshotDom` now always uses the DOM-based host
        bridge capture path (`__cn1_capture_canvas_png__`) instead of the
        translated screenshot method.  This avoids async callbacks entirely.
    - Also added `Uint8ClampedArray` to the JSO `inferFn` for proper type
      recognition when wrapping typed arrays received from the host.

Known Failing Symptoms (Latest CI Logs/Artifacts)
-------------------------------------------------

- Screenshot suite finishes but many tests fail during `runTest`.
- Latest primary blocker progression:
  - Earlier blocker in CI artifacts: `Missing JS member get for host receiver`.
  - After bridge compatibility work in local replay: blocker shifts to `Cannot read properties of null (reading '__classDef')` in translated screenshot helper path.
- Repeated deterministic blockers in browser log:
  - `TabsScreenshotTest`: `cn1_com_codename1_ui_Button_initLaf_com_codename1_ui_plaf_UIManager is not defined`
  - `OrientationLockScreenshotTest`: `document is not defined`
  - Multiple tests: `TypeError: Cannot read properties of null (reading '__classDef')`
    - Seen in `ValidatorLightweightPickerScreenshotTest`, `InPlaceEditViewTest`, `StreamApiTest`, `TimeApiTest`.
- Timeout-only tests still timeout by design/behavior (`MediaPlayback...`, `BytecodeTranslatorRegression...`, selected API tests).
- Screenshot pixels are still wrong in CI (host-canvas fallback path remains dominant in logs).
- Local note from current workspace revalidation:
  - Fresh locally built bundle (`/tmp/hellocodenameone-javascript-port-local.zip`) currently emits only `bootstrap_placeholder` as a named stream.
  - Browser diagnostics in that run show repeated:
    - `PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:originalMissing=1`
    - `PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:noCanvas=1:test=...`
  - This local symptom differs from the latest CI artifacts (which still emit 32 screenshots, mostly white), so CI confirmation is required before accepting/rejecting the current translated-method resolution patch.

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
3. Validate no `originalInvokeErr=Maximum call stack size exceeded` in CI browser log for screenshot helper path.
4. Validate `CN1SS` named test streams are emitted again (not only `default/bootstrap`).
5. Validate `originalResolved=translated:...__impl` (or equivalent non-recursive path) in CI browser log after translated-method preservation patch.
6. If white-frame reuse persists, capture and compare per-test `settleSig`/`canvasSig`/`canvasSource` to identify whether paint is not happening or capture target is still wrong.
7. Fix per-test null receiver/init path (`__classDef` null) at first failing stack in translated screenshot/helper execution (no new broad fallbacks).
8. Fix missing `Button.initLaf(UIManager)` symbol resolution in worker runtime path.
9. Fix worker-mode orientation lock path so DOM access is host-bridge mediated (no direct `document` access in worker).
10. Confirm VM completeness stability in CI with parser/runtime patches (`expected 7` consistently).

Files Touched In This Pass
--------------------------

- `vm/ByteCodeTranslator/src/javascript/browser_bridge.js`
- `Ports/JavaScriptPort/src/main/webapp/port.js`
- `Ports/JavaScriptPort/STATUS.md`
