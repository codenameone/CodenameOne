<!-- Licensed under the PolyForm Noncommercial License 1.0.0 -->

JavaScript Port Status (ParparVM)
=================================

The JavaScript port compiles Java bytecode to JavaScript via ParparVM
(`vm/ByteCodeTranslator/`) and runs the app in a Web Worker against a host
bridge on the main thread for DOM / Canvas access.

**Current production status:** Initializr and the website use the legacy
TeaVM cloud build (`scripts/initializr/build.sh javascript`,
`scripts/website/build.sh build_initializr_for_site`). The ParparVM JS port
is shipped in tree for screenshot CI and as an opt-in
`javascript_parparvm` build, but is not the default until the
`canvasContextWipe` Heisenbug below is resolved.

This document is the handoff for whoever picks up the branch next. Read it
before you re-attempt switching initializr over.

Build
-----

```
mvn -B -f maven/pom.xml -pl parparvm -am -DskipTests package
SKIP_PARPARVM_BUILD=1 ./scripts/build-javascript-port-hellocodenameone.sh \
    /tmp/hellocodenameone-js.zip
```

For initializr:

```
./scripts/build-javascript-port-initializr.sh
# or via the wrapper
./scripts/initializr/build.sh javascript_parparvm
```

Test
----

```
ARTIFACTS_DIR=/tmp/js-verify \
    CN1_JS_TIMEOUT_SECONDS=300 CN1_JS_BROWSER_LIFETIME_SECONDS=280 \
    BROWSER_CMD='node scripts/run-javascript-headless-browser.mjs' \
    ./scripts/run-javascript-browser-tests.sh /tmp/hellocodenameone-js.zip \
    scripts/javascript/screenshots
```

CI runs at 375×667 viewport with Playwright `deviceScaleFactor: 2` so the
density picker lands on `DENSITY_VERY_HIGH` and screenshots come out 750×1334
— phone-proportional to the iOS/Android baselines in `scripts/ios/screenshots`
and `scripts/android/screenshots`.

Screenshot baselines
--------------------

`scripts/javascript/screenshots/` holds the curated JS-port PNGs. They are
platform-specific (iOS/Android references use the native themes and physical
device DPIs; the JS port rasterises through `iOS7Theme.res` at headless
Chromium). Regenerate via a full-suite run — artifacts under
`$ARTIFACTS_DIR/*.png`.

**Current matched count: 67 stable** (was 61 at branch start; +6 net). Drops
to 65 ~30% of runs when the `canvasContextWipe` Heisenbug fires for
LWPicker/Validator/SheetSlide — the bug doesn't hang the suite anymore
(targeted no-op recovery handles it) but the affected tests fail to emit
PNGs on those runs.

Branch-resident JS goldens (in tree, ready to count when the bug is
properly fixed): chart-doughnut, chart-radar, chart-time,
LightweightPickerButtons, ValidatorLightweightPicker, ToastBarTopPosition,
SheetSlideUpAnimationScreenshotTest.

Lasting fixes shipped on this branch
------------------------------------

1. **`wrapJsObject` class-preserve** (`08b12489d`,
   `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js:1751-1796`):
   the cached-wrapper path used to do `wrapper.__class = resolvedClass`
   unconditionally — a null `resolvedClass` would wipe the cached class
   and break every subsequent `cn1_iv*` dispatch on that wrapper. Fix
   preserves the existing class when the new resolution is null. This is
   the root-cause fix for the `chartDocumentStaleness` cascade (task #43).

2. **Defensive `__cn1CachedDocWrapper` invalidation** (`5dce6a24a`,
   `Ports/JavaScriptPort/src/main/webapp/port.js:1132-1149`): if the
   cached document wrapper has lost its `__class` (the original
   chartDocumentStaleness symptom), invalidate the cache and re-fetch
   from the host bridge.

3. **Targeted `{}` no-op recovery** (`64bc97115` → `9c5d233cb`,
   `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js:3105-3160`):
   when `cn1_ivResolve`'s target is a literal `{}` (Object.prototype, no
   own props) AND the method is one of a whitelisted Canvas2D op
   (save/restore/beginPath/closePath/stroke/fill/clip/resetTransform/
   `setTransform_6double`/`rect_4double`) or matches a prefix
   (`setFillStyle_`/`setStrokeStyle_`/`drawImage_`/`createElement_`),
   substitute a no-op generator. Prevents the worker-side scheduler from
   busy-looping on `VIRTUAL_FAIL`. A broader unconditional form was
   tried (`3062f310d`) but breaks early boot — keep it targeted.

4. **Number-receiver → null substitution at the bridge boundary**
   (`990d60be1`, `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js:1334-1362`):
   when `invokeJsoBridge`'s `hostResult` is a JS Number but the
   `bridge.returnClass` is an object type, substitute `null` before
   `wrapJsResult`. Downstream code's standard null-check then fires a
   clean NPE instead of busy-looping on `VIRTUAL_FAIL`.

5. **`AbstractAnimationScreenshotTest.captureAndEmit` hardened**
   (`efc9bdb67`): wrap each step in try/catch so that even when both
   `buildScreenshot` and the grey-placeholder `Image.createImage` throw,
   `done()` is still invoked. A single broken animation test can no longer
   stall the whole suite via the per-test timeout fallback.

6. **6 new JS goldens** (see "Screenshot baselines" above).

7. **Comprehensive diagnostic instrumentation in tree** (rate-limited,
   all log-only):
   - `NULL_RECEIVER` with `mid/hasJsValue/hostClass/hostRef/keys/allProps/isLiteral/protoName/typeof/value/callerFrames`
     (`vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js:3109-3160`)
   - `EMPTY_HOST_RESULT` — hostResult is a literal `{}` from
     `invokeJsoBridge`
   - `EMPTY_UNWRAP` — `unwrapJsValue` produced a literal `{}` with
     `Object.prototype` proto
   - `NUMBER_FOR_OBJECT` — `invokeJsoBridge` returned a Number for an
     object return class; logs `recovery=substituted-null` when the new
     fix kicks in
   - `NUMBER_LEAK` — host-side bridge handler (`browser_bridge.js:670`)
     about to return a Number for `document` / `getContext`
   - `CLASS_WIPE` — cached wrapper class about to be overwritten with
     null

8. **`StringBuilder.append(Object)` virtual toString dispatch**
   (`vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js:5257`):
   the native used the synchronous `jvm.toNativeString(obj)`, whose
   last-resort fallback is `"" + value` -- for any heap object (boxed
   Integer/Long/Double included) that yields the JS
   `"[object Object]"` instead of the Java `toString()`. JSONWriter
   emits numbers via `sb.append(o)`, so every serialized number became
   `[object Object]` -- the `[` opened a phantom JSON array on re-parse
   and mangled the whole document (the `surfacesJsonRoundTrip` /
   `surfacesRasterizerNpe` park reasons, plus the previously-failing
   SurfacesTimelineLogicTest). The append(Object) native is now a
   generator: strings keep the fast sync path, everything else goes
   through `runtimeToNativeString`'s virtual `toString()` dispatch.
   All three surfaces tests are un-parked; A/B full-suite runs (old vs
   new runtime, same bundle) delivered byte-identical PNGs for all 135
   other tests.

9. **Initializr "Generate" end-to-end** (`dfff3e809` and the followups):
   5 layered workarounds for build-script path, HTTP-status check in
   `getArrayBufferInputStream`, custom `DownloadNative`/`InflateNative`
   bypasses for broken LocalForage and zipme codepaths, manual STORED
   zip writer in `GeneratorModel` (int-only CRC32 because the JS port
   doesn't sign-extend `(long) negativeInt`).

The unresolved bug: `canvasContextWipe`
---------------------------------------

**Symptom**: Random late-suite tests (Sheet, SheetSlide, Toast,
CssGradients, theme tests, ChartCombined/Transform/Rotated) hang the suite
when un-parked. NULL_RECEIVER fires on `cn1_s_save` / `cn1_s_setTransform`
/ etc. — the receiver of these Canvas2D method calls is wrong.

**What the receiver actually is**: A JS `Number` with value **667** — the
viewport height. Not a literal `{}` as earlier guesswork suggested.
`typeof target === "number"`, `Object.getPrototypeOf(target) === Number.prototype`,
`Object.getOwnPropertyNames(target).length === 0`. Confirmed in CI runs
with the enhanced `NULL_RECEIVER` diag.

**Where the Number enters**: The `NUMBER_FOR_OBJECT` diag in
`invokeJsoBridge` fires consistently for two specific methodIds:
- `cn1_s_getDocument_R_HTMLDocument` on a Window receiver
- `cn1_s_getContext_..._CanvasRenderingContext2D` on an HTMLCanvasElement receiver

Both should return DOM objects; instead `hostResult` is the number 667.

**Where it does NOT come from**: The host-side `NUMBER_LEAK` diag
(`browser_bridge.js:670`, fires when `receiver[member]` is about to return
a Number) consistently shows **0 fires**. So the host bridge handler
never sees a Number — meaning the Number is produced on the **worker side**
of `invokeJsoBridge`. Specifically the worker-side fallback path at
`parparvm_runtime.js:1383+` is the suspect: when `receiver.__cn1HostRef`
is undefined, the worker does `receiver[bridge.member]` directly.

**The puzzle**: For the worker-side path to produce 667, `receiver` must
have a `member` property of 667. In the worker, `self.window = self` (via
`worker.js`), so `receiver` would be `self`, but `self.document` is
undefined — `wrapJsResult(undefined, ...)` should return null, not 667.
So either `receiver` is something other than `self`, or there's a code
path that converts undefined to 667 somewhere. Neither is yet identified.

**Why not just fix it with the existing recovery**: The Number→null
substitution at `990d60be1` does convert the symptom into a clean NPE,
but the affected tests catch the NPE in their render loop and retry
forever, hanging the suite differently. The recovery prevents the
`VIRTUAL_FAIL` busy-loop but doesn't actually let the tests pass —
they still need the canvas context to render.

**Why not just track every `self.*` mutation**: I tried. There's no
`self.document = ...` anywhere in the worker bundle, the runtime, or
port.js. The diag's stack trace points to `createCanvas` /
`drainPendingDisplayFrame`, both of which read `window.getDocument()` or
`outputCanvas.getContext("2d")` — but the bridge dispatch for those
should never produce 667. It happens **intermittently** (about 30% of
CI runs) which strongly suggests a state-dependent race in either the
host-bridge round-trip or the worker scheduler.

Tools I tried and what they showed
----------------------------------

- **Worker-side diagnostics** captured the value (667) and the method ids
  but couldn't trace which Java code held the broken receiver — JS stack
  traces only show generator frames, not the source-level Java method
  that set the field.

- **Host-side diagnostics** (`NUMBER_LEAK` at `browser_bridge.js`)
  confirmed the host bridge isn't the source. The Number enters via the
  worker-side fallback in `invokeJsoBridge`.

- **Playwright CDP attach to web worker** doesn't work: `page.context().
  newCDPSession(worker)` throws *"page: expected Page or Frame"*.
  `Target.setAutoAttach({flatten: true})` exposes worker events on the
  main session but Playwright's `CDPSession.send` API doesn't accept a
  sessionId, so you can't set worker-side breakpoints
  programmatically. Workarounds for the next session:
  - **Puppeteer** has a real Worker.detach API and supports per-worker
    `Page.target().createCDPSession()`. Switch the headless harness to
    puppeteer for breakpoint debugging.
  - **Raw CDP over WebSocket** (manually connect to the browser's
    `ws://...` endpoint, send `Target.attachToTarget`, then
    `Debugger.setBreakpoint` on the worker scriptId).
  - **Headed Chrome with manual DevTools** — launch the test bundle in
    a real Chrome window, open DevTools, switch to the worker context in
    the dropdown, set a breakpoint in `parparvm_runtime.js` at the
    `NULL_RECEIVER` block, step backwards from the call frame.

- **Local Mac iteration** is ~3× slower than CI Linux for the full suite
  (`/tmp/hcc/` hot-swap approach reaches ~test 60 in 20 minutes vs CI's
  ~110 tests in 15). Not a viable iteration loop. Targeted single-test
  runs would help; the device runner's `prependedTest` mechanism is the
  hook (`Cn1ssDeviceRunner.java`).

- **ParparVM source maps**: not generated by the translator. The user
  mentioned source maps as a debugging avenue, but a quick grep through
  `vm/ByteCodeTranslator/src/com/codename1/tools/translator/Javascript*`
  showed no `sourceMap` / `SourceMap` / `sourceMappingURL` infrastructure.
  Adding source-map emission would be a separate sub-project.

Diagnostic dump locations
-------------------------

When the screenshot CI run fails or finishes, the artifact
`javascript-ui-tests` contains `browser.log` with all `PARPAR:DIAG:*`
lines. Grep for `NUMBER_FOR_OBJECT`, `NUMBER_LEAK`, `NULL_RECEIVER`,
`EMPTY_UNWRAP`, `CLASS_WIPE` to see when the cascade fires and what
the receiver looked like at that point.

Locally:

```bash
# Hot-swap the runtime/port.js into a pre-built bundle and run via the
# headless playwright harness.
cd /tmp && rm -rf hcc && mkdir hcc && cd hcc
unzip -q /path/to/HelloCodenameOne-js.zip
cp /Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/webapp/port.js \
   HelloCodenameOne-js/port.js
cp /Users/shai/dev/cn1/vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js \
   HelloCodenameOne-js/parparvm_runtime.js
cp /Users/shai/dev/cn1/vm/ByteCodeTranslator/src/javascript/browser_bridge.js \
   HelloCodenameOne-js/browser_bridge.js
cd /Users/shai/dev/cn1
BROWSER_CMD="node $(pwd)/scripts/run-javascript-headless-browser.mjs" \
  CN1_JS_TIMEOUT_SECONDS=1500 \
  ./scripts/run-javascript-browser-tests.sh /tmp/hcc/HelloCodenameOne-js \
  2>&1 | tail -10
grep -E "NUMBER_FOR_OBJECT|NULL_RECEIVER:value" \
  artifacts/javascript-browser-tests/browser.log | head -30
```

A `cdp-debug-empty-receiver.mjs` Playwright debugger script is in
`scripts/` — it logs scriptParsed/paused events but the worker-attach
path doesn't fire (Playwright limitation noted above). Kept for the
next session as a starting point for puppeteer/raw-CDP migration.

What was deliberately reverted from this branch
-----------------------------------------------

- **Initializr is back on TeaVM cloud build.** `scripts/initializr/build.sh
  javascript` and `scripts/website/build.sh build_initializr_for_site`
  use the original `mvn package -Dcodename1.platform=javascript` path
  that ships through the CN1 build server.

- The new ParparVM build path stays as an opt-in:
  `scripts/initializr/build.sh javascript_parparvm` →
  `scripts/build-javascript-port-initializr.sh`. The build script itself,
  its DownloadNative/InflateNative natives, the manual STORED zip writer,
  and the HTTP-status check in `getArrayBufferInputStream` all remain
  in tree — they're useful when the underlying bug is fixed and we can
  retry the migration.

Tests parked on this branch
---------------------------

`Ports/JavaScriptPort/src/main/webapp/port.js`
`cn1ssForcedTimeoutTestClasses` + `cn1ssForcedTimeoutTestNames` carry
the parking decisions. Reasons:

- `chartCombinedXyCapture` — `ChartCombinedXY`, `ChartTransform`,
  `ChartRotated`: distinct capture hang inside `__cn1_capture_canvas_png__`,
  unrelated to `canvasContextWipe`.
- `canvasContextWipe` — `ToastBar`, `Sheet`, `CssGradients`, `SheetSlide`:
  the Heisenbug above. Goldens are baselined; they re-enable trivially
  once the bug is fixed.
- `sheetTearDownLeak` — `TextAreaAlignment`: Sheet's overlay isn't torn
  down before the next test; captured underneath a leftover dim/blur
  layer.
- `themeScreenshot` — 14 theme tests: parked because they sit at suite
  index 90+ where `canvasContextWipe` fires. Likely unblock once the
  bug is fixed; need baselines.
- `simdLargeAllocaCorrupt` — `SimdLargeAllocaTest`: corrupts shared
  HTML5Implementation state (VIRTUAL_FAIL on `paintDirty` / `flushGraphics`)
  late in the suite. Separate bug.
- `synchronizedBlocksAdmitContendedEntrantsInFifoOrder` —
  `JavascriptRuntimeSemanticsTest` in `vm/tests`: `@Disabled` on this
  branch. All 11 CompilerConfig variants report `order=[0,...,0]`
  (entrants reported done via `join()` but never enter the synchronized
  block). The other six monitor tests in the same file still pass, so
  the monitorEnter/Exit implementation isn't broken — this is FIFO
  ordering specifically. Needs scheduler tracing rather than another
  ad-hoc edit; see `project_jsport_monitor_fifo_investigation` memory.
- Native-API timeouts (`mediaPlayback`, `browserComponentLoadEvent`,
  `vpnDetectionApi`, `callDetectionApi`, `localNotificationOverride`,
  `base64NativePerformance`, `accessibility`,
  `backgroundThreadUiAccess`, `bytecodeTranslatorRegression`): platform
  APIs that aren't wired on the JS port. Force-timeout in
  `Cn1ssDeviceRunner.shouldForceTimeoutInHtml5` — placeholder/spinner
  output is the expected baseline.

Concrete next-session playbook
------------------------------

The bottleneck is debugging the worker. Don't try to merge more no-op
recovery — the trade-off is bad (each new method id unblocks one test
but lets the suite spin on the next uncovered method). Fix the
upstream Number-leak instead.

Order of operations for the next attempt:

1. **Switch the headless harness from Playwright to puppeteer**, OR
   add a raw-CDP-over-WebSocket variant alongside
   `scripts/run-javascript-headless-browser.mjs`. The worker-attach
   limitation is purely Playwright-side. Time-box to half a day; if
   it's not working, fall back to headed Chrome with manual DevTools.

2. **Set a conditional breakpoint** inside `cn1_ivResolve`
   (`parparvm_runtime.js:3105`-ish) on the
   `typeof target === 'number'` branch. When it fires, walk back
   through the call frames to find the field load that produced the
   667 — `cn1_iv0(target, mid)` where `target` is loaded from a Java
   field via `obj[<field-key>]`. That field is what's holding the
   wrong value.

3. **Identify the assignment site for that field**. Either:
   - Java code is genuinely assigning 667 to a CanvasRenderingContext2D-
     typed field (unlikely — would need type-confusion or a
     `(CanvasRenderingContext2D)<int>` cast that the JS port erases).
   - The translator is emitting wrong code for some field-write
     sequence (look for `_$cN`/`_$cD` or `S.p` /`S.q` stack patterns
     near `cn1_s_getContext` / `cn1_s_getDocument` calls).
   - Some bindNative override is returning a number where an object
     was expected (the `NUMBER_FOR_OBJECT` diag rules this out for
     `invokeJsoBridge`, but there are other dispatch paths).

4. **Fix the source**, remove the no-op recovery as it becomes
   unnecessary, un-park the cascade-victim tests one at a time, and
   regenerate goldens (most already baselined in tree).

5. **Once green for 5+ consecutive runs**, switch
   `scripts/initializr/build.sh javascript` and
   `scripts/website/build.sh build_initializr_for_site` back to the
   ParparVM path (basically revert this commit's revert).

Entry points
------------

- `Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/HTML5Implementation.java`
  — CN1 `CodenameOneImplementation` subclass.
- `Ports/JavaScriptPort/src/main/webapp/port.js` — host-side bindings and
  `bindCiFallback` shims.
- `vm/ByteCodeTranslator/src/javascript/browser_bridge.js` — main-thread
  host bridge handlers.
- `vm/ByteCodeTranslator/src/javascript/worker.js` /
  `vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js` — worker-side VM.

Known limitations (carried over from earlier status)
---------------------------------------------------

- `graphics-transform-perspective.png` and `graphics-transform-camera.png`
  render blank — perspective / 3D transforms aren't implemented in the JS
  impl (`Transform.isPerspectiveSupported()` returns false). Matches
  iOS/Android behaviour on older devices that also skip these paths.
- `OrientationLockScreenshotTest` is the last test in the suite because it
  mutates orientation state that other tests don't reset.

See `scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.java`
for the suite contents and
`scripts/run-javascript-headless-browser.mjs` for the Playwright harness.
