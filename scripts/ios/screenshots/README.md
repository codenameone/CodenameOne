# iOS Simulator Automation Screenshots

This directory stores reference PNG files for the Codename One iOS simulator
automation tests.

Each PNG file should be named after the CN1SS test stream that emits the
screenshot (for example `MainActivity.png` or `BrowserComponent.png`). The
`run-ios-simulator-tests.sh` script compares simulator output against these
references and reports any differences in the pull request summary.

When a reference image is missing or the pixels differ, the GitHub Actions
workflow posts a pull request comment that includes the updated screenshot so it
can be reviewed and promoted to the baseline as needed.
