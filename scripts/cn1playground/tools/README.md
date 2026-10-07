# Tools

Helpers for building and checking the Playground.

`run-playground-smoke-tests.sh` builds the `common` module against the locally
installed framework SNAPSHOT and runs the JavaSE harnesses: the smoke checks
(including the API stub library the build generates), the syntax matrix, the
layout, preview-resolution and samples harnesses. CI runs it under `xvfb-run`.

`run-playground-browser-tests.sh` builds the JavaScript bundle, serves it locally
and runs three Playwright checks in headless browsers:

- `verify-playground-browser.mjs` runs every bundled sample and one editing session:
  a compile error is reported with its line, typed code runs, a re-run redefines
  classes of the same name, a listener's exception is reported and a listener can
  show a second form. Screenshots go to `PLAYGROUND_BROWSER_ARTIFACT_DIR` when set.
- `verify-playground-demos.mjs` checks rendered scenes, animation, and camera
  interaction (see below).
- `verify-lightweight-editor-input.mjs` checks the editor's keyboard, caret and
  clipboard input path.

Playwright must resolve from `scripts/` (`cd scripts && npm install playwright &&
npx playwright install chromium firefox`). `PLAYGROUND_SKIP_BUILD=1` reuses the last
build, and `PLAYGROUND_MVN_ARGS` passes extra Maven arguments.

`compare-javascript-bundles.sh` compares a legacy JavaScript playground output
with a ParparVM JavaScript-port artifact and reports total bundle size plus key
payload file sizes.

`sync-zipsupport-from-initializr.sh` refreshes the bundled ZipSupport cn1lib the
project exporter uses.

### Demo behavior regressions

`verify-playground-demos.mjs` exercises the unmodified Bouncing Balls, 3D / GPU,
and Camera Capture samples. It is part of `run-playground-browser-tests.sh` and
can also target an already-served bundle:

```bash
PLAYGROUND_BROWSER_ARTIFACT_DIR=/tmp/playground-demos \
  node scripts/cn1playground/tools/verify-playground-demos.mjs http://127.0.0.1:8793/index.html
node --test scripts/cn1playground/tools/demo-pixels.test.mjs
```

The checker accepts a standalone app or the website page containing the
"Codename One Playground" iframe. The normal runner builds local sources and
serves them inside a test page with the website's 76px header. This reproduces
its reduced iframe height. A deployed URL can reproduce production failures,
but does not validate local changes.

The checks run in Chromium and Firefox at 1440x900 and 1280x720 with both 1x and
2x (Retina) device pixel ratios. Screenshots are measured in CSS pixels so the
assertions use the same coordinate space as the accessibility bounds. The tests
derive the preview bounds from the accessibility tree and examine screenshot
pixels for visible scene content,
preview coverage, and animation. They reject blank scenes, a fixed-size scene
painted into a corner of a larger preview, and frozen foregrounds. Animation
sampling starts immediately and allows up to five seconds for visible movement
on software WebGL; the foreground change threshold is unchanged. A permanently
frozen scene still fails, with every sampled frame retained in the artifacts. The cube must
also be centered and retain its proportions. Pixel thresholds tolerate antialiasing; these are behavioral
checks rather than machine-specific golden screenshots. The small Node test
suite validates the pixel oracle against good, blank, misplaced and frozen
synthetic frames.

The camera check clicks Start Camera and Take Photo through the canvas at the
controls' current accessible bounds, requiring the controls to fit in the visible
window. Both browsers use synthetic cameras and automate permission acceptance.
The checker verifies that no media request happens before the click and that
clicking Start opens exactly one real getUserMedia request with audio disabled.
It requires visible changing video pixels and captured-photo pixels, then closes
the dialog. A separate navigation case switches through Camera, Hello World and
3D twice in one page, requires every outgoing camera track to end, and verifies
that a new camera stream can be opened and released again. No physical camera is used. This does not test the browser's own
permission-dialog UI or Safari.

Worker errors are observed after initialization and throughout interaction, even
when the app first logs `preview updated`. All cases run after an assertion
failure. Each case writes screenshots and JSON console/error evidence; the
aggregate `demo-results.json` records failed assertions. Without an explicit
artifact directory, the standalone checker writes `playground-demo-artifacts/`.
`PLAYGROUND_DEMO_FILTER=camera` runs only matching cases during iteration; an
unknown filter fails instead of silently testing nothing. The playground browser
CI job runs the full matrix; the website publication gate selects the 3D cases.
`PLAYGROUND_BROWSERS=chromium` or `firefox` narrows a local run; `chrome` uses
an installed Google Chrome. `PLAYGROUND_DEVICE_SCALE_FACTORS=1,1.5,2` can extend
the default `1,2` matrix for fractional browser scaling. Local runs use normal browser GPU settings.
`PLAYGROUND_HEADED=1` runs visible browsers; diagnostic JSON records the WebGL
renderer and whether GPU overrides were requested.
`PLAYGROUND_SOFTWARE_GL=1` explicitly enables software/forced WebGL for hosted
Linux CI runners without hardware GPUs. The CI default runs Chromium and Firefox. A failed behavior assertion fails the command and the existing browser
CI job; the remaining cases still run and retain their evidence.

The website workflow also runs the 3D rendering checks against the actual Hugo
output at both pixel ratios before publishing previews or production. Its
screenshots and diagnostics are uploaded as `website-playground-rendering`.
