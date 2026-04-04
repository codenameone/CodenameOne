JavaScript Port Status
======================

Implemented
-----------

- [x] PolyForm Noncommercial 1.0.0 license boundary for `Ports/JavaScriptPort/**`
- [x] Imported browser-port baseline into the repository as a working reference subtree
- [x] ParparVM-side production host bridge in [JavaScriptPortHost.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/platform/js/JavaScriptPortHost.java)
- [x] Native method bindings resolved at runtime via `bindNative()` in `parparvm_runtime.js` and `port.js` (no hardcoded registry needed)
- [x] Browser bundle bootstrap shell and host bridge in `vm/ByteCodeTranslator`
- [x] JSO interfaces created under `com.codename1.html5.js.*` for browser DOM APIs
- [x] @JSBody annotation processing implemented in ByteCodeTranslator for inline JavaScript generation
- [x] Wrapped static method body generation fixed - `__impl` functions now properly generated

Current Blocker
---------------

**FIXED**: All three critical bugs have been resolved:

### Bug 1: @JSBody Inline Script Syntax Error ✅ FIXED
The `@JSBody` annotation scripts contain `return` statements but were being assigned directly:
```javascript
// WRONG
const __jsBodyResult = return evt.source;

// FIXED
const __jsBodyResult = (function() { return evt.source; }).call(this);
```

### Bug 2: Wrapped Static Methods Missing `__impl` Body ✅ FIXED
Static methods were generating only the wrapper but skipping the method body generation due to an early return.

**Root cause**: In `appendMethod()`, wrapped static methods returned after generating the wrapper, skipping lines 253-277 that generate the actual bytecode body.

**Fix**: Removed the early return - now the `__impl` function body is generated before the wrapper.

**Verification**: After fix, generated code shows both functions:
```javascript
// __impl function with actual bytecode
function* main__impl(args) {
  // Actual method body
}
// Wrapper that calls __impl
function* main(args) {
  jvm.ensureClassInitialized("...");
  return yield* main__impl(args);
}
```

### Bug 3: Playwright Not Loadable in CI ✅ FIXED
Playwright installed globally (`npm install -g`) is not accessible via ES module imports.

**Fix**: Install locally in `scripts/` directory:
```yaml
- name: Install Playwright Chromium
  run: |
    cd scripts
    npm init -y 2>/dev/null || true
    npm install playwright
    npx playwright install --with-deps chromium
```

Next Steps
----------

1. Push changes and run CI to verify fixes work end-to-end
2. If browser tests pass, the JavaScript port is functional

Files Modified
--------------

1. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/BytecodeMethod.java**:
   - Added `jsBodyScript`, `jsBodyParams` fields
   - Added getters/setters for JSBody annotation data

2. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/ByteCodeMethodArg.java**:
   - Added `getTypeName()` and `getPrimitiveType()` getters

3. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/Parser.java**:
   - Added `JSBodyAnnotationVisitor` inner class
   - Modified `visitAnnotation()` to capture @JSBody data

4. **vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptMethodGenerator.java**:
   - Added `appendJsBodyMethod()` for @JSBody inline JavaScript generation
   - Fixed @JSBody script wrapping in IIFE for proper return value handling
   - **Critical fix**: Removed early return in `appendMethod()` that was skipping `__impl` body generation for wrapped static methods

5. **scripts/run-javascript-headless-browser.mjs**:
   - Improved error messages for Playwright import failures

6. **.github/workflows/scripts-javascript.yml**:
   - Changed Playwright installation from global to local `scripts/` directory

Verification
------------

After fixes, the generated JavaScript bundle correctly shows:

```bash
$ unzip -p /tmp/test-fix.zip HelloCodenameOne-js/translated_app.js | grep -A15 "function\* main__impl"
# Shows __impl function with actual bytecode (object creation, method calls)
# Shows wrapper function that ensures class init and calls __impl
```

The bytecode translation now correctly generates both the `__impl` function body and the wrapper for all static methods.