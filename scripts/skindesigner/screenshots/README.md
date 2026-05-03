# Skin Designer screenshots

This directory drives the developer-guide screenshots for the Skin
Designer chapter. The pipeline runs entirely inside Codename One —
`Display.captureScreen()` produces the PNGs, no AWT Robot or external
xvfb capture step.

## How it works

`scripts/skindesigner/common/src/main/java/.../screenshots/ScreenshotApp.java`
is a `Lifecycle` subclass that walks each wizard stage:

1. Wipes the persisted wizard state in `Preferences` / `Storage`.
2. Sets the `cn1.skindesigner.demo*` system properties for the next
   scenario (step / device / source / sidebar tab / preset).
3. Calls `new SkinDesigner().runApp()` to build a fresh wizard form.
4. Waits 1.5 s for layout to settle.
5. Calls `Display.getInstance().captureScreen()` and saves the result
   via `Storage.getInstance().createOutputStream(name + ".png")` —
   on the JavaSE port that lands at `~/.cn1/<name>.png`.

After every scenario finishes, the app calls
`Display.exitApplication()`. The shell script then copies the PNGs out
of `~/.cn1/` and into `docs/developer-guide/img/skin-designer/`.

## Running locally

```
scripts/skindesigner/screenshots/take-screenshots.sh
```

Requires Java 17 + Maven on `PATH`. On Linux you also need `xvfb-run`
for headless JavaSE simulator runs (`sudo apt-get install xvfb`); on
macOS the script falls back to a regular simulator launch and you'll
see the wizard window briefly per scenario.

## Demo overrides

`SkinDesigner.applyDemoOverrides()` reads these system properties from
the JVM and, if present, replaces the loaded persisted state with the
specified values:

| Property                              | Effect                                  |
| ------------------------------------- | --------------------------------------- |
| `cn1.skindesigner.demoStep`           | 0 / 1 / 2 / 3                           |
| `cn1.skindesigner.demoDevice`         | Device id from `devices.json`           |
| `cn1.skindesigner.demoSource`         | `shape` / `image` / `blank`             |
| `cn1.skindesigner.demoPreset`         | Shape preset id (e.g. `island`)         |
| `cn1.skindesigner.demoSidebarTab`     | `shape` / `cutouts` / `info`            |

`ScreenshotApp` toggles them between scenarios via `System.setProperty`.

## CI

`.github/workflows/skin-designer-screenshots.yml` runs the script on
`workflow_dispatch` and on PRs that touch the harness or wizard code,
uploads the PNGs as an artifact, and on manual dispatch opens an
automated PR if the committed images drifted.
