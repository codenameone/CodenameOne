# Skin Designer screenshots

This directory contains the harness that drives the Skin Designer through
each wizard stage in the JavaSE simulator and captures Robot screenshots
for the developer guide.

## Files

- `lib/SkinDesignerScreenshotter.java` — Java harness. Spawns the
  simulator JVM with the demo-mode system properties wired up, waits for
  the UI to settle, then captures the desktop via `java.awt.Robot`.
- `take-screenshots.sh` — top-level shell script. Builds the Skin
  Designer Maven project, resolves the simulator classpath, compiles the
  harness, and runs each scenario inside `xvfb-run` (Linux) or directly
  (other platforms).

The PNG output lands in
`docs/developer-guide/img/skin-designer/`, where the
`Skin-Designer.asciidoc` chapter references it.

## Demo overrides

The harness drives the wizard via three system properties read by
`SkinDesigner.applyDemoOverrides`:

| Property                              | Effect                                  |
| ------------------------------------- | --------------------------------------- |
| `cn1.skindesigner.demoStep`           | 0 / 1 / 2 / 3                           |
| `cn1.skindesigner.demoDevice`         | Device id from `devices.json`           |
| `cn1.skindesigner.demoSource`         | `shape` / `image` / `blank`             |
| `cn1.skindesigner.demoPreset`         | Shape preset id (e.g. `island`)         |
| `cn1.skindesigner.demoSidebarTab`     | `shape` / `cutouts` / `info`            |

These reset the persisted wizard state so each scenario starts at a
deterministic place.

## Running locally

```
scripts/skindesigner/screenshots/take-screenshots.sh
```

Requires Java 17 + Maven on `PATH`. On Linux you also need
`xvfb-run` for headless desktop capture — install via
`sudo apt-get install xvfb`. On macOS the script falls back to direct
launches; you'll see the simulator pop up briefly between scenarios.

## CI

`.github/workflows/skin-designer-screenshots.yml` runs the script on
`workflow_dispatch` and on PRs that touch the harness or the wizard
source. It uploads the PNGs as an artifact and (on manual dispatch)
opens an automated PR if the committed images drifted.
