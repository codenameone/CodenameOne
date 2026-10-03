# Tools

Helpers for building and checking the Playground.

`run-playground-smoke-tests.sh` builds the `common` module against the locally
installed framework SNAPSHOT and runs the JavaSE harnesses: the smoke checks
(including the API stub library the build generates), the syntax matrix, the
layout, preview-resolution and samples harnesses. CI runs it under `xvfb-run`.

`run-playground-browser-tests.sh` builds the JavaScript bundle, serves it locally
and runs two Playwright checks in headless Chromium:

- `verify-playground-browser.mjs` runs every bundled sample and one editing session:
  a compile error is reported with its line, typed code runs, a re-run redefines
  classes of the same name, a listener's exception is reported and a listener can
  show a second form. Screenshots go to `PLAYGROUND_BROWSER_ARTIFACT_DIR` when set.
- `verify-lightweight-editor-input.mjs` checks the editor's keyboard, caret and
  clipboard input path.

Playwright must resolve from `scripts/` (`cd scripts && npm install playwright &&
npx playwright install chromium`). `PLAYGROUND_SKIP_BUILD=1` reuses the last
build, and `PLAYGROUND_MVN_ARGS` passes extra Maven arguments.

`compare-javascript-bundles.sh` compares a legacy JavaScript playground output
with a ParparVM JavaScript-port artifact and reports total bundle size plus key
payload file sizes.

`sync-zipsupport-from-initializr.sh` refreshes the bundled ZipSupport cn1lib the
project exporter uses.
