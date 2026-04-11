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
- The screenshot pipeline currently still fails overall, but now for actionable per-test/runtime errors (not early startup deadlock).

What Was Fixed In This Pass
---------------------------

1. Screenshot report pipeline no longer aborts immediately on `CN1SS:ERR` lines.
   - File: `scripts/run-javascript-screenshot-tests.sh`
   - Root cause:
     - `cn1ss_print_log "$LOG_FILE"` was called under `set -e`.
     - `Cn1ssChunkTools check` exits non-zero when `CN1SS:ERR` lines are present.
     - This stopped decode/report/comment stages, so CI artifact had no screenshots/comment update.
   - Fix:
     - Continue after `cn1ss_print_log` failure with explicit warning.
   - Verified locally:
     - Script now decodes and copies many PNGs from the same failing browser log and reaches compare/comment stages.

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

Known Failing Symptoms (Latest CI Logs/Artifacts)
-------------------------------------------------

- Screenshot suite finishes but many tests fail during `runTest`.
- Repeated deterministic blockers in browser log:
  - `TabsScreenshotTest`: `cn1_com_codename1_ui_Button_initLaf_com_codename1_ui_plaf_UIManager is not defined`
  - `OrientationLockScreenshotTest`: `document is not defined`
  - Multiple tests: `TypeError: Cannot read properties of null (reading '__classDef')`
    - Seen in `ValidatorLightweightPickerScreenshotTest`, `InPlaceEditViewTest`, `StreamApiTest`, `TimeApiTest`.
- Timeout-only tests still timeout by design/behavior (`MediaPlayback...`, `BytecodeTranslatorRegression...`, selected API tests).
- `default` CN1SS stream decode can still fail in some runs while named streams decode successfully.

Other CI Signal
---------------

- `JavascriptCn1CoreCompletenessTest.executesMeaningfulCodenameOneCoreSliceInWorkerRuntime` has reported flaky `expected: <7> but was: <3>` in CI.
- Local single-method repetition passed in this workspace after the runtime hardening above, but CI confirmation is still required.

Priority Next Steps
-------------------

1. Fix per-test null receiver/init path (`__classDef` null) at first failing stack, not via broad fallbacks.
2. Fix missing `Button.initLaf(UIManager)` symbol resolution in worker runtime path.
3. Fix worker-mode orientation lock path so DOM access is host-bridge mediated (no direct `document` access in worker).
4. Stabilize/diagnose `JavascriptCn1CoreCompletenessTest` in CI with focused logging around JSON parse/map key lookup.
5. After runtime errors above are fixed, restore expected screenshot count/contents (target remains full set, currently partial/wrong).

Files Touched In This Pass
--------------------------

- `scripts/run-javascript-screenshot-tests.sh`
- `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`
- `Ports/JavaScriptPort/STATUS.md`
