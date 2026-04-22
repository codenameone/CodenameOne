<!-- Licensed under the PolyForm Noncommercial License 1.0.0 -->

JavaScript Port Status (ParparVM)
=================================

The JavaScript port compiles Java bytecode to JavaScript via ParparVM
(`vm/ByteCodeTranslator/`) and runs the app in a Web Worker against a host
bridge on the main thread for DOM / Canvas access.

Build
-----

```
mvn -B -f maven/pom.xml -pl parparvm -am -DskipTests package
SKIP_PARPARVM_BUILD=1 ./scripts/build-javascript-port-hellocodenameone.sh \
    /tmp/hellocodenameone-js.zip
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

Known limitations
-----------------

- `graphics-transform-perspective.png` and `graphics-transform-camera.png`
  render blank — perspective / 3D transforms aren't implemented in the JS
  impl (`Transform.isPerspectiveSupported()` returns false). Matches iOS/Android
  behaviour on older devices that also skip these paths.
- `BrowserComponentScreenshotTest`, `MediaPlaybackScreenshotTest`,
  `BytecodeTranslatorRegressionTest`, `BackgroundThreadUiAccessTest`,
  `VPNDetectionAPITest`, `CallDetectionAPITest`,
  `LocalNotificationOverrideTest`, `Base64NativePerformanceTest`, and
  `AccessibilityTest` are intentionally time-limited in HTML5 mode via
  `Cn1ssDeviceRunner.shouldForceTimeoutInHtml5`; their expected output is the
  placeholder / spinner frame.
- `OrientationLockScreenshotTest` is the last test in the suite because it
  mutates orientation state that other tests don't reset.

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

See `scripts/hellocodenameone/common/src/main/java/com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.java`
for the suite contents and
`scripts/run-javascript-headless-browser.mjs` for the Playwright harness.
