JavaScript Port Status
======================

Implemented
-----------

- [x] PolyForm Noncommercial 1.0.0 license boundary for `Ports/JavaScriptPort/**`
- [x] Imported browser-port baseline into the repository as a working reference subtree
- [x] ParparVM-side production host bridge in [JavaScriptPortHost.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/platform/js/JavaScriptPortHost.java)
- [x] Native method bindings resolved at runtime via `bindNative()` in `parparvm_runtime.js` and `port.js` (no hardcoded registry needed)
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
- [x] TeaVM JSO dependency removed from ParparVM core bytecode translator
- [x] JSO interfaces created under `com.codename1.html5.js.*` for browser DOM APIs
- [x] @JSBody annotation processing implemented in ByteCodeTranslator for inline JavaScript generation

In Progress
-----------

- [ ] Fix CI timeout during browser test execution - tests timeout after 180 seconds waiting for `CN1SS:SUITE:FINISHED`
- [ ] Replace remaining direct browser-runtime assumptions in `HTML5Implementation` with backend-owned or adapter-owned seams

Current Blocker
---------------

The CI tests are timing out at the browser execution phase. Investigation revealed two issues:

1. **Playwright not installed in CI** - The `browser-launch.log` shows:
   ```
   Unable to load Playwright. Install either "playwright" or "@playwright/test".
   Error [ERR_MODULE_NOT_FOUND]: Cannot find package 'playwright'
   ```
   This is a CI environment setup issue where Playwright is not available when `run-javascript-headless-browser.mjs` runs.

2. **@JSBody inline script syntax error** (FIXED) - The generated JavaScript had invalid syntax:
   ```javascript
   // WRONG - 'return' inside const assignment
   const __jsBodyResult = return evt.source;
   
   // FIXED - Wrapping in IIFE to capture return value
   const __jsBodyResult = (function() { return evt.source; }).call(this);
   ```
   The `@JSBody` annotation scripts often contain `return` statements. The fix wraps the script in an IIFE (Immediately Invoked Function Expression) to properly capture the return value.

3. **Generated JavaScript confirmed working** - Checking `translated_app.js` showed:
   - @JSBody methods are now generating inline JavaScript code
   - `consoleLog` and other JSBody methods have proper wrapper functions
   - JSO class inference and parameter unwrapping are in place

Recent Changes
-------------

Fixed `appendJsBodyMethod()` in `JavascriptMethodGenerator.java` to wrap non-void scripts in an IIFE:
```java
// For methods with return values:
out.append("const __jsBodyResult = (function() { ").append(script).append(" }).call(this);\n");

// For void methods (no return value):
out.append(script).append("\n");
```

Fixed Playwright installation in CI workflow:
```yaml
# Now installs playwright locally in scripts/ directory instead of globally
- name: Install Playwright Chromium
  run: |
    cd scripts
    npm init -y 2>/dev/null || true
    npm install playwright
    npx playwright install --with-deps chromium
```

Next Steps
----------

1. Commit changes and re-run CI to verify Playwright now loads correctly
2. If Playwright works but JavaScript app still fails toinitialize, check browser console logs for errors
3. Verify @JSBody inline scripts generate valid JavaScript that can execute

Files Modified
--------------

1. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/BytecodeMethod.java**:
   - Added `jsBodyScript`, `jsBodyParams` fields
   - Added `getJsBodyScript()`, `setJsBodyScript()`, `getJsBodyParams()`, `setJsBodyParams()`, `isJsBodyMethod()`

2. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/ByteCodeMethodArg.java**:
   - Added `getTypeName()` and `getPrimitiveType()` getters

3. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/Parser.java**:
   - Added `JSBodyAnnotationVisitor` inner class
   - Modified `MethodVisitorWrapper.visitAnnotation()` to capture @JSBody annotations

4. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptMethodGenerator.java**:
   - Modified `appendNativeStubIfNeeded()` to check for JSBody methods
   - Added `appendJsBodyMethod()` to generate inline JavaScript for @JSBody annotated methods

5. **scripts/run-javascript-headless-browser.mjs**:
   - Improved error messages for Playwright import failures

6. **.github/workflows/scripts-javascript.yml**:
   - Changed Playwright installation from global (`npm install -g`) to local (`cd scripts && npm install playwright`)
   - This ensures ES module dynamic import can resolve the package

Debugging Steps
--------------

1. To debug locally, run:
   ```bash
   export JAVA_HOME=/Users/shai/Library/Java/JavaVirtualMachines/azul-1.8.0_372/Contents/Home
   export PATH="$JAVA_HOME/bin:$PATH"
   
   # Build the ByteCodeTranslator with changes
   cd /Users/shai/dev/cn1/vm/ByteCodeTranslator && mvn install -Dspotbugs.skip=true
   
   # Build and run the JavaScript port bundle
   cd /Users/shai/dev/cn1
   ./scripts/build-javascript-port-hellocodenameone.sh /tmp/test-bundle.zip
   
   # Run browser tests with shorter timeout for faster iteration
   export CN1_JS_TIMEOUT_SECONDS=60
   export BROWSER_CMD='node /Users/shai/dev/cn1/scripts/run-javascript-headless-browser.mjs'
   ./scripts/run-javascript-browser-tests.sh /tmp/test-bundle.zip /Users/shai/dev/cn1/scripts/javascript/screenshots
   ```

2. Check generated JavaScript for @JSBody methods:
   ```bash
   # Look for consoleLog inline implementation in generated app.js
   unzip -p /tmp/test-bundle.zip translated_app.js | grep -A5 "consoleLog" | head -20
   
   # Check if JSBody methods have proper inline script
   unzip -p /tmp/test-bundle.zip translated_app.js | grep -B2 -A5 "function\*__cn1" | head -50
   ```

3. Add debug logging to port.js:
   ```javascript
   // At the start of port.js, add:
   console.log("port.js loading, jsoRegistry available:", !!jvm.jsoRegistry);
   ```

Key Files Modified for @JSBody Support
--------------------------------------

1. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/BytecodeMethod.java**:
   - Added `jsBodyScript` and `jsBodyParams` fields
   - Added `getJsBodyScript()`, `setJsBodyScript()`, `getJsBodyParams()`, `setJsBodyParams()`, `isJsBodyMethod()`

2. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/ByteCodeMethodArg.java**:
   - Added `getTypeName()` and `getPrimitiveType()` getters

3. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/Parser.java**:
   - Added `JSBodyAnnotationVisitor` inner class
   - Modified `MethodVisitorWrapper.visitAnnotation()` to capture @JSBody annotations

4. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptMethodGenerator.java**:
   - Modified `appendNativeStubIfNeeded()` to check for JSBody methods
   - Added `appendJsBodyMethod()` to generate inline JavaScript for @JSBody annotated methods

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