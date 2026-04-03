JavaScript Port Status
======================

Implemented
-----------

- [x] PolyForm Noncommercial 1.0.0 license boundary for `Ports/JavaScriptPort/**`
- [x] Imported browser-port baseline into the repository as a working reference subtree
- [x] ParparVM-side production host bridge in [JavaScriptPortHost.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/platform/js/JavaScriptPortHost.java)
- [x] Native method bindings resolved at runtime via `bindNative()` in` parparvm_runtime.js` and `port.js` (no hardcoded registry needed)
- [x] Browser bundle bootstrap shell and host bridge in `vm/ByteCodeTranslator`
- [x] PolyForm smoke fixtures for the JavaScript port under `Ports/JavaScriptPort/tests/**`
- [x] ParparVM smoke and browser-bundle integration coverage in `vm/tests`
- [x] Extraction of reusable runtime helpers from `HTML5Implementation`
- [x] Extraction of input/bootstrap/event wiring helpers from `HTML5Implementation`
- [x] Initial rendering subsystem extraction from `HTML5Graphics` and `BufferedGraphics`
- [x] Shared native-image model and cache invalidation policy via [JavaScriptNativeImageAdapter.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/JavaScriptNativeImageAdapter.java)
- [x] Async image load state coordination via [JavaScriptAsyncImageLoadCoordinator.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/JavaScriptAsyncImageLoadCoordinator.java)
- [x] Rendering backend contract in [JavaScriptRenderingBackend.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/JavaScriptRenderingBackend.java)
- [x] Neutral default backend implementation name: `BrowserDomRenderingBackend`
- [x] Backend-routed image creation, scaling, pixel readback, and canvas serialization for the main image runtime paths
- [x] JavaScript-port screenshot log decode/report entrypoint in [/Users/shai/dev/cn1/scripts/run-javascript-screenshot-tests.sh](/Users/shai/dev/cn1/scripts/run-javascript-screenshot-tests.sh)
- [x] Browser-harness entrypoint for JavaScript CN1SS execution in [/Users/shai/dev/cn1/scripts/run-javascript-browser-tests.sh](/Users/shai/dev/cn1/scripts/run-javascript-browser-tests.sh)
- [x] Playwright-based `BROWSER_CMD` entrypoint for CI browser execution in [/Users/shai/dev/cn1/scripts/run-javascript-headless-browser.mjs](/Users/shai/dev/cn1/scripts/run-javascript-headless-browser.mjs)
- [x] ParparVM-backed HelloCodenameOne JavaScript-port bundle builder in [/Users/shai/dev/cn1/scripts/build-javascript-port-hellocodenameone.sh](/Users/shai/dev/cn1/scripts/build-javascript-port-hellocodenameone.sh)
- [x] Dedicated JavaScript screenshot CI workflow in [/Users/shai/dev/cn1/.github/workflows/scripts-javascript.yml](/Users/shai/dev/cn1/.github/workflows/scripts-javascript.yml)
- [x] CI-owned headless browser execution path for JavaScript CN1SS runs via Playwright Chromium
- [x] `scripts/hellocodenameone` CN1SS suite wired to execute through the ParparVM JavaScript-port browser bundle in CI
- [x] Screenshot artifacts and PR comment generation wired through the shared CN1SS reporting flow for the JavaScript-port pipeline
- [x] Playground JavaScript bundle size comparison helper in [/Users/shai/dev/cn1/scripts/cn1playground/tools/compare-javascript-bundles.sh](/Users/shai/dev/cn1/scripts/cn1playground/tools/compare-javascript-bundles.sh)
- [x] Playground build-script entrypoint for legacy-vs-ParparVM JavaScript bundle comparison in [/Users/shai/dev/cn1/scripts/cn1playground/build.sh](/Users/shai/dev/cn1/scripts/cn1playground/build.sh)
- [x] Local end-to-end HelloCodenameOne ParparVM JavaScript-port bundle build now completes with [/Users/shai/dev/cn1/scripts/build-javascript-port-hellocodenameone.sh](/Users/shai/dev/cn1/scripts/build-javascript-port-hellocodenameone.sh)
- [x] ParparVM translator regression fix for straight-line lowering fallback on unsupported stack patterns in [/Users/shai/dev/cn1/vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptMethodGenerator.java](/Users/shai/dev/cn1/vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptMethodGenerator.java)
- [x] ParparVM translator regression fix for var-load/store opcodes arriving as `BasicInstruction` during JavaScript emission in [/Users/shai/dev/cn1/vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptMethodGenerator.java](/Users/shai/dev/cn1/vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptMethodGenerator.java)
- [x] Local Playwright-backed browser startup now advances through missing helper natives, browser-window wrapper casting, and `requestAnimationFrame` functor conversion in [/Users/shai/dev/cn1/vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js](/Users/shai/dev/cn1/vm/ByteCodeTranslator/src/javascript/parparvm_runtime.js)

In Progress
-----------

- [ ] Replace remaining direct browser-runtime assumptions in `HTML5Implementation` with backend-owned or adapter-owned seams
- [ ] Reduce the remaining `org.teavm.*` and `com.codename1.teavm.*` coupling inside the production runtime path
- [ ] Define a concrete ParparVM-native browser/runtime backend implementation behind `JavaScriptRenderingBackend`
- [ ] Adapt `scripts/cn1playground` to build and serve the ParparVM-backed JavaScript port instead of the legacy Maven JavaScript target
- [ ] Validate the new HelloCodenameOne ParparVM browser bundle path end-to-end in CI and add the first checked-in screenshot baselines under `/Users/shai/dev/cn1/scripts/javascript/screenshots`
- [ ] Continue the local Playwright startup burn-down until HelloCodenameOne reaches `CN1SS:SUITE:FINISHED`; the current blocker is post-startup browser/runtime behavior after the initial `requestAnimationFrame` bridge path

TODO
----

- [ ] Implement a non-TeaVM production backend behind `JavaScriptRenderingBackend`
- [ ] Port the remaining image/media/photo-capture canvas paths onto backend-owned abstractions where appropriate
- [ ] Port browser peer integration and native peer lifecycle beyond the current extraction layer
- [ ] Port text editing and native text-field overlay behavior away from legacy assumptions
- [ ] Port networking and storage from extracted helper contracts into a clearly ParparVM-oriented runtime path
- [ ] Port browser/media/database/file-chooser integrations away from direct legacy dependencies
- [ ] Replace or adapt legacy `teavm` package names in production code where they are part of the long-term runtime contract
- [ ] Expand runtime tests from smoke/contract coverage into broader end-to-end JavaScript-port behavior coverage
- [ ] Add JavaScript-port screenshot baselines under `/Users/shai/dev/cn1/scripts/javascript/screenshots`
- [ ] Add `scripts/cn1playground` size regression checks against the legacy JavaScript output so ParparVM bundle growth is tracked continuously
- [ ] Add `scripts/cn1playground` startup/runtime performance comparisons against the legacy JavaScript output so the new port stays in the same performance scale
- [ ] Adapt the remaining `scripts/hellocodenameone` native-oriented helpers so JavaScript screenshot execution can share the same fixture definitions and result naming conventions as the iOS/Android runners
- [ ] Add a dedicated implementation-factory selection path for the JavaScript port if the final runtime wiring needs one beyond current bootstrap behavior

Verification
------------

- Focused verification currently uses:
  ```bash
  export JAVA_HOME=/Users/shai/Library/Java/JavaVirtualMachines/azul-1.8.0_372/Contents/Home
  export PATH="$JAVA_HOME/bin:$PATH"
  mvn -B test -f /Users/shai/dev/cn1/vm/pom.xml -pl tests -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=JavaScriptRuntimeFacadeTest,JavaScriptPortSmokeIntegrationTest,JavascriptTargetIntegrationTest#generatesBrowserBundleForJavascriptTarget
  ```
- Additional local verification now includes:
  ```bash
  export JAVA_HOME=/Users/shai/Library/Java/JavaVirtualMachines/azul-1.8.0_372/Contents/Home
  export PATH="$JAVA_HOME/bin:$PATH"
  mvn -B -f /Users/shai/dev/cn1/maven/pom.xml -pl parparvm -am -DskipTests -Dmaven.javadoc.skip=true package

  export JAVA_HOME=/Users/shai/Library/Java/JavaVirtualMachines/jbr-17.0.7/Contents/Home
  export PATH="$JAVA_HOME/bin:$PATH"
  SKIP_PARPARVM_BUILD=1 /Users/shai/dev/cn1/scripts/build-javascript-port-hellocodenameone.sh /tmp/hellocodenameone-javascript-port.zip
  ```
- Current local browser startup verification loop:
  ```bash
  export ARTIFACTS_DIR=/tmp/cn1-js-browser-artifacts
  export CN1_JS_TIMEOUT_SECONDS=45
  export CN1_JS_BROWSER_LIFETIME_SECONDS=30
  export BROWSER_CMD='playwright-cli open "$URL"; sleep 30; playwright-cli close'
  /Users/shai/dev/cn1/scripts/run-javascript-browser-tests.sh /tmp/hellocodenameone-javascript-port.zip /Users/shai/dev/cn1/scripts/javascript/screenshots
  ```
- Additional JavaScript-port testing/build utilities now available:
  ```bash
  /Users/shai/dev/cn1/scripts/run-javascript-screenshot-tests.sh <device-runner.log> [/Users/shai/dev/cn1/scripts/javascript/screenshots]
  /Users/shai/dev/cn1/scripts/cn1playground/tools/compare-javascript-bundles.sh --legacy <legacy-js-zip-or-dir> --parparvm <parparvm-dist-dir>
  ```
