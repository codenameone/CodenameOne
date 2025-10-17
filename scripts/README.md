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
  executes instrumentation tests, and prepares screenshot reports for pull
  requests.

## Subdirectories

- `android/` – Python helpers, baseline screenshots, and utilities that power
  the Android instrumentation test workflow.
  - `android/lib/` – library-style Python modules shared across Android
    automation scripts.
  - `android/tests/` – command-line tools used by CI for processing screenshots
    and posting feedback to pull requests.
  - `android/screenshots/` – reference images used when comparing emulator
    output.
- `templates/` – code templates consumed by the sample app builders.

These scripts are designed so that shell logic focuses on orchestration, while
Python modules encapsulate the heavier data processing steps. This separation
keeps the entry points easy to follow and simplifies maintenance.
