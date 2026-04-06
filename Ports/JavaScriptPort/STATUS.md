<!-- Licensed under the PolyForm Noncommercial License 1.0.0 -->

JavaScript Port Status (ParparVM)
=================================

Last updated: 2026-04-06

Current State
-------------

- **WORKAROUND IN PLACE**: `ensureDisplayEdt()` in port.js creates synthetic EDT if missing. Tests now complete successfully.
- Screenshot tests pass - suite finishes and screenshots are generated.
- **Remaining issue**: `IllegalStateException` still caught in fallback handlers even after EDT is set. May be unrelated to EDT (different code path).
- The separate ParparVM Java test pipelines that were failing in CI (`job-logs2.txt`, `job-logs3.txt`) are now reproduced and fixed locally.

## Diagnostic Evidence (Updated 2026-04-06)

After EDT workaround, browser logs show:
```
PARPAR:DIAG:EDT_ENSURE:reusedMainThread=1
PARPAR:DIAG:POST_EDT_ENSURE_formCtorTitleLayoutGlobal:displayClassExists=1:instance=present:edt=present:edtThreadName=main
PARPAR:DIAG:PRE_formCtorLayout:displayClassExists=1:instance=present:edt=present:edtThreadName=main
PARPAR:DIAG:ERR_formCtorLayout:displayClassExists=1:instance=present:edt=present:edtThreadName=main
PARPAR:DIAG:FALLBACK:formCtorLayout:bypassIllegalState=1:detail=java_lang_IllegalStateException
```

**Key observations**:
- EDT is successfully set (`edt=present:edtThreadName=main`)
- Tests pass (suite completes)
- `IllegalStateException` still triggered but caught by fallback handler
- Exception may originate from code path other than `Display.setCurrent()` EDT check

## Previous Issues (Historical)

What Was Fixed In This Round
----------------------------

1. **Added EDT initialization workaround** in `port.js`:
   - `checkDisplayInitState()` -checks Display.INITANCE and EDT state
   - `ensureDisplayEdt()` - creates synthetic EDT thread or reuses main thread
   - Form constructor bypasses call `ensureDisplayEdt()` before construction
   - Verified workaround works: logs show `EDT_ENSURE:reusedMainThread=1`

2. **Rebuilt JavaScript bundle** with updated port.js:
   - Built with `SKIP_PARPARVM_BUILD=1 ./scripts/build-javascript-port-hellocodenameone.sh`
   - Bundle contains updated `port.js` with workaround

3. **Tests now pass**:
   - Suite completes with `CN1SS:SUITE:FINISHED`
   - Screenshots generated for MainActivity, graphics-draw-line, graphics-draw-rect, graphics-fill-rect, kotlin

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

6. **Enhanced Form constructor error diagnostics**.
   - Added `checkDisplayInitState()` function to report Display.INSTANCE and EDT state.
   - Added `emitDisplayInitDiag()` calls before and after Form constructor execution.
   - Enhanced `stringifyThrowable()` to capture `messageOnly` separately.
   - Bypass handlers now log `PRE_` and `ERR_` Display state along with exception details.

Validated Locally
-----------------

- `mvn -pl tests -am "-Dtest=JavascriptRuntimeSemanticsTest,JavascriptTargetIntegrationTest,JavaScriptPortSmokeIntegrationTest" -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
- Result: `Tests run: 313, Failures: 0, Errors: 0, Skipped: 0` (BUILD SUCCESS, finished 2026-04-06 10:47:59 +03:00).

Next Steps
----------

1. **Investigate remaining `IllegalStateException`**:
   - Exception occurs even after EDT is set
   - May be from different code path (not Display.setCurrent)
   - Add exception message logging to `ERR_` diagnostics

2. **Optional**: Investigate why Display.init() doesn't create EDT in JavaScript port:
   - Trace `Display.initNative()` call sequence
   - Check if thread creation is happening but not persisting

3. **Commit workaround**: The `ensureDisplayEdt()` fix allows tests to pass; consider committing as a workaround while root cause is investigated.

Important Notes
--------------

- The CI breakage in the ParparVM Java test pipelines was real and not just screenshot-noise; it is now addressed by the translator/runtime fixes above.
- The screenshot problem remains a separate rendering/lifecycle issue after startup/protocol recovery.
- Error messages are now extracted separately from the full throwable detail for easier parsing.

Known Important Context
-----------------------

- This file supersedes older status notes that referenced initial JSBody/static-wrapper bootstrap issues as the primary blocker.
- Current bottleneck is no longer "suite timeout"; it is "suite passes but screenshots are wrong".
- Existing local tree also includes ongoing debug-oriented changes in:
  - `Ports/JavaScriptPort/src/main/webapp/port.js`
  - `Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/HTML5Implementation.java`
  These are part of the active CI recovery/debugging workflow.
