# Codename One build scripts

This directory houses helper scripts used by local contributors and the CI
workflows to build and validate Codename One ports.

## Top-level shell scripts

- `setup-workspace.sh` – provisions the JDKs, Maven installation, and other
  tooling required to build Codename One locally or inside CI.
- `build-android-port.sh` / `build-ios-port.sh` – compile the Android and iOS
  native ports from source.
- `build-android-app.sh` / `build-ios-app.sh` – generate a "Hello Codename One"
  sample application and build it against the freshly compiled port.
- `run-android-instrumentation-tests.sh` – launches the Android emulator,
  drives the Codename One DeviceRunner suite, and prepares screenshot
  reports for pull requests.
- `run-ios-ui-tests.sh` – runs the Codename One DeviceRunner suite on the iOS
  simulator and emits matching screenshot reports.

## Subdirectories

- `android/` – Java helpers, baseline screenshots, and utilities that power the
  Android DeviceRunner test workflow.
  - `android/lib/` – standalone Java sources invoked directly by the shell
    scripts for Gradle patching and similar tasks.
  - `android/tests/` – command-line tools used by CI for processing screenshots
    and posting feedback to pull requests.
  - `android/screenshots/` – reference images used when comparing emulator
    output.
- `ios/` – Helpers and screenshot baselines used by the iOS DeviceRunner
  workflow.
- `device-runner-app/` – Java sources for the shared sample application and its
  DeviceRunner UI tests.

These scripts are designed so that shell logic focuses on orchestration, while
small Java utilities encapsulate the heavier data processing steps. This
separation keeps the entry points easy to follow and simplifies maintenance.
