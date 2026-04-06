<!-- Licensed under the PolyForm Noncommercial License 1.0.0 -->

JavaScript Port Status (ParparVM)
=================================

Last updated: 2026-04-06

Current State
-------------

- Screenshot CI reaches suite completion and emits screenshots, but output images are still incorrect.
- The separate ParparVM Java test pipelines that were failing in CI (`job-logs2.txt`, `job-logs3.txt`) are now reproduced and fixed locally.

What Was Fixed In This Round
----------------------------

1. Restored native categorization for JavaScript translation.
   - Reintroduced `vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptNativeRegistry.java`.
   - Restored registry-driven behavior in `JavascriptMethodGenerator.appendNativeStubIfNeeded(...)`:
     - runtime-implemented natives: no generic "Missing javascript native method" stubs.
     - host-hook natives: generated `jvm.invokeHostNative(...)` stubs.
     - unsupported natives: explicit unsupported error reasons.

2. Expanded host-hook coverage to the JavaScript port boundary.
   - Host-hook prefix now covers `cn1_com_codename1_impl_platform_js_`.
   - This fixes `JavaScriptPortHost` bridge symbols used by JavaScript port smoke tests.

3. Fixed `String.format(...)` runtime coercion bug.
   - File: `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js`.
   - Changed format token conversion to use `runtimeFormatTokenValue(...)`.
   - This resolves the deterministic `JsJavaApiCoverageApp` mismatch (`expected 511, got 495`; missing bit `16`).

4. Added explicit PolyForm header text to missing JavaScriptPort boundary files.
   - Added `"PolyForm Noncommercial License 1.0.0"` markers to all Java/Markdown files previously flagged by `JavaScriptPortSmokeIntegrationTest`.

5. Reverted risky JS `INVOKESPECIAL` remap in codegen.
   - Removed the recent `Util.resolveInvokeSpecialOwner(...)` injection from JS invoke emission paths for now.
   - The broader regressions observed in CI are fixed without that change.

Validated Locally
-----------------

- `mvn -pl tests -am "-Dtest=JavascriptRuntimeSemanticsTest,JavascriptTargetIntegrationTest,JavaScriptPortSmokeIntegrationTest" -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
- Result: `Tests run: 313, Failures: 0, Errors: 0, Skipped: 0` (BUILD SUCCESS, finished 2026-04-06 10:47:59 +03:00).

Primary Remaining Work (Screenshot Correctness)
-----------------------------------------------

1. Remove/replace `Form` constructor IllegalState fallbacks in runtime path.
   - Current evidence still points to `FORM_INIT_LAYOUT:error=java_lang_IllegalStateException` as the highest-value screenshot blocker.

2. Validate form-show lifecycle correctness before rendering.
   - Ensure no constructor-bypass fallback is hit in the screenshot scenario.
   - Confirm `show()` path reaches expected layout/paint readiness markers.

3. Re-run screenshot CI and compare artifact diffs.
   - Goal is not just "suite finished", but visually correct screenshots.

Important Notes
---------------

- The CI breakage in the ParparVM Java test pipelines was real and not just screenshot-noise; it is now addressed by the translator/runtime fixes above.
- The screenshot problem remains a separate rendering/lifecycle issue after startup/protocol recovery.

Known Important Context
-----------------------

- This file supersedes older status notes that referenced initial JSBody/static-wrapper bootstrap issues as the primary blocker.
- Current bottleneck is no longer “suite timeout”; it is “suite passes but screenshots are wrong”.
- Existing local tree also includes ongoing debug-oriented changes in:
  - `Ports/JavaScriptPort/src/main/webapp/port.js`
  - `Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/HTML5Implementation.java`
  These are part of the active CI recovery/debugging workflow.
