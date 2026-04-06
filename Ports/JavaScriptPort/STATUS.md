JavaScript Port Status (ParparVM)
=================================

Last updated: 2026-04-06

Current CI State
----------------

- JavaScript screenshot pipeline now reaches completion:
  - `CN1SS:SUITE:FINISHED` is present.
  - `TOP_BLOCKER=none|none|none` is present.
  - 5 screenshots are emitted as artifacts.
- Major remaining problem: screenshots are still visually wrong (black/flat/patterned output in key tests).

What Is Working
---------------

- End-to-end browser harness execution is stable enough to complete.
- Screenshot chunk emission/decoding path works in CI and local reproduction.
- Earlier deterministic VM blockers were resolved (including missing virtual array clone handling).
- Local and CI diagnostics are actionable (`PARPAR:DIAG:*`, fallback markers, blocker classifier output).

Latest High-Value Change
------------------------

- Implemented `INVOKESPECIAL` owner resolution in JS code generation:
  - File: `vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptMethodGenerator.java`
  - Change: for non-constructor/non-clinit `INVOKESPECIAL`, resolve actual method owner using:
    - `Util.resolveInvokeSpecialOwner(owner, name, desc)`
  - Applied in both straight-line and interpreter invoke emission paths.
- Expanded `Form` constructor fallback diagnostics in `port.js`:
  - `formCtorLayout` and `formCtorTitleLayout` bypass logs now include throwable detail and receiver/arg class metadata.

Why this matters:
- The generated JS previously emitted direct calls against unresolved intermediate owners in inherited/super-call paths.
- This contributed to runtime dependence on emergency owner-delegate fallbacks.

Observed Effect Of Latest Change
--------------------------------

- Local run still finishes and emits screenshots.
- In new local artifacts, these fallback keys remain enabled but are no longer hit:
  - `FALLBACK:Container.missing.internalPaintImpl_com_codename1_ui_Graphics_boolean`
  - `FALLBACK:Container.missing.paintBackground_com_codename1_ui_Graphics`
  - `FALLBACK:Label.paintComponentBackgroundMissing`
- Screenshot hashes changed for some tests (`graphics-draw-line`, `graphics-draw-rect`) but are still incorrect overall.
- `MainActivity.png` and `kotlin.png` remain black and identical.

Primary Remaining Blocker
-------------------------

- Form initialization still repeatedly throws and is bypassed by CI fallbacks:
  - `PARPAR:DIAG:FORM_INIT_LAYOUT:error=java_lang_IllegalStateException`
  - `FALLBACK:Form.layoutCtorIllegalStateBypass:HIT`
  - `FALLBACK:Form.titleLayoutCtorIllegalStateBypass:HIT`
  - `FALLBACK:Form.initLafNullUiManagerBridge:HIT`

Assessment:
- This is currently the highest-probability root cause for wrong UI state at screenshot time.
- The constructor bypasses prevent hard failure but likely leave form/layout/laf state partially initialized.

Priority Order (Do Next)
------------------------

1. Eliminate `Form(..., Layout)`/`Form(String, Layout)` illegal-state bypass dependence.
   - Instrument and fix root cause so constructors complete without fallback catch-and-return.
   - Exit condition: no `FORM_INIT_LAYOUT:error=java_lang_IllegalStateException` for screenshot scenario.

2. Re-validate rendering once form init is clean.
   - Confirm black/flat screenshots are resolved or significantly improved.
   - Re-check whether paint-path fallbacks remain unused.

3. Tighten fallback surface after rendering is correct.
   - Keep only minimal required fallbacks.
   - Remove stale fallback entries and document each retained one with rationale.

4. Final CI hardening.
   - Repeat-run stability checks.
   - Ensure artifacts and logs remain deterministic.

Known Important Context
-----------------------

- This file supersedes older status notes that referenced initial JSBody/static-wrapper bootstrap issues as the primary blocker.
- Current bottleneck is no longer “suite timeout”; it is “suite passes but screenshots are wrong”.
- Existing local tree also includes ongoing debug-oriented changes in:
  - `Ports/JavaScriptPort/src/main/webapp/port.js`
  - `Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/HTML5Implementation.java`
  These are part of the active CI recovery/debugging workflow.
