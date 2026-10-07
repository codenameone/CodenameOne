# JavaScript Screenshot Baselines

Curated reference PNGs for the JavaScript-port CN1SS screenshot suite, produced
by the `scripts/hellocodenameone` test harness running against the JavaScript
port under Playwright (see `scripts/run-javascript-browser-tests.sh` and
`scripts/run-javascript-headless-browser.mjs`).

These are **platform-specific** — they are not the iOS/Android PNGs. The JS
port rasterises through `iOS7Theme.res` at headless Chromium's
`deviceScaleFactor: 2` on a 375×667 viewport (750×1334 output), so channel
values and anti-aliasing differ from the native renderers. Each run's output
is compared against the PNG with the matching name here via
`scripts/common/java/ProcessScreenshots.java`.

Regenerate by running a full suite and copying `$ARTIFACTS_DIR/*.png` into
this directory — only update after a clean `CN1SS:SUITE:FINISHED` run with no
`__parparError` in the browser log.

The six GPU/immersive-media references were refreshed from run `37442276559`
after fixing WebGL compositing at 2× pixel density. Their scenes must fill the
content area below the title; the previous references incorrectly accepted a
half-width, half-height scene in the upper-left corner. Comparison tolerances
were not changed.
