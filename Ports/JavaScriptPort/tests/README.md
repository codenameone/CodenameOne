These fixtures define the smoke-test host contract for the in-repo ParparVM JavaScript port work.

They are not built through TeaVM. They are compiled and translated by the local ParparVM JavaScript backend in `vm/ByteCodeTranslator`, and exercised by integration tests in `vm/tests`.

`JavaScriptSelectionApp` exercises browser startup with the initial default font,
editable/readonly selection, Unicode rendering, and application-owned interactions
(issues #5943, #5944 and #5946). With Java 8 and Playwright installed:

```sh
bash scripts/build-javascript-selection-fixture.sh /tmp/cn1-selection
node scripts/test-javascript-text-interactions.mjs
node scripts/test-javascript-text-selection.mjs /tmp/cn1-selection/output/dist/JavaScriptSelectionApp-js
```

The browser test runs Chromium and Firefox. Set `CN1_JS_CHROME_CHANNEL=chrome` to
use installed Chrome, or `CN1_JS_FIREFOX_EXECUTABLE` for an existing Playwright
Firefox binary. Append `--android` to run against the local Android emulator;
`CN1_ANDROID_SERIAL` selects its adb serial (default `emulator-5584`). The emulator
accesses the test server through `10.0.2.2`. Screenshots and logs go under
`artifacts/javascript-text-selection`, or the optional second argument.

The translated compact-string regression is
`JavascriptRuntimeSemanticsTest#preservesCompactLatin1AndWideStrings` in `vm/tests`.

The selection harness also exercises runtime constraint changes, foreground alpha,
bottom alignment, form shortcuts, fixed-height textarea scrolling in both directions,
interactive/custom labels, and horizontal container scrolling. Set
`CN1_JS_REVIEW_ONLY=true` to run only those review regression cases while iterating.
