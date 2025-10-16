# Android Instrumentation Test Screenshots

This directory stores reference screenshots for Android native instrumentation tests.

Each PNG file should be named after the test stream that emits the screenshot
(e.g. `MainActivity.png` or `BrowserComponent.png`). The automation in
`scripts/run-android-instrumentation-tests.sh` compares the screenshots emitted
by the emulator with the files stored here. If the pixels differ (ignoring PNG
metadata) or if a reference image is missing, the workflow posts a pull request
comment that includes the updated screenshot.

When the comparison passes, no screenshot artifacts are published and no
comment is created.
