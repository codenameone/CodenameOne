# Skin Designer screenshots

This directory drives the developer-guide screenshots for the Skin
Designer chapter. The capture runs entirely inside Codename One in
quiet mode — no skin window, no source-watcher, no AWT Robot, no
external xvfb capture step.

## How it works

`scripts/skindesigner/common/src/main/java/.../screenshots/ScreenshotApp.java`
is a plain `main()` class that:

1. Wipes the persisted wizard state in `Preferences` / `Storage`.
2. Sets the `cn1.skindesigner.demo*` system properties for the next
   scenario (step / device / source / sidebar tab / preset).
3. Calls `Display.init(new Container())` once for the whole run, then
   `new SkinDesigner().runApp()` per scenario to build a fresh form.
4. On the EDT, lays out the form at iPhone-class dimensions and calls
   `form.toImage()` to render off-screen.
5. Writes each PNG straight to disk via `cn1.ImageIO`.

`take-screenshots.sh` invokes this via:

```
mvn -pl common exec:java \
    -Dexec.mainClass=com.codename1.tools.skindesigner.screenshots.ScreenshotApp \
    -Dexec.args="$OUT_DIR"
```

— no cn1:simulator, no `verify` lifecycle, no JFrame.

## Running locally

```
scripts/skindesigner/screenshots/take-screenshots.sh
```

Requires Java 17 + Maven on `PATH`. On Linux you also need `xvfb-run`
because CN1's `Display.init` still pokes
`Toolkit.getDefaultToolkit()`. On macOS the script falls back to a
direct `mvn` invocation.

## Demo overrides

`SkinDesigner.applyDemoOverrides()` reads these system properties
from the JVM and, if present, replaces the loaded persisted state
with the specified values:

| Property                              | Effect                                  |
| ------------------------------------- | --------------------------------------- |
| `cn1.skindesigner.demoStep`           | 0 / 1 / 2 / 3                           |
| `cn1.skindesigner.demoDevice`         | Device id from `devices.json`           |
| `cn1.skindesigner.demoSource`         | `shape` / `image` / `blank`             |
| `cn1.skindesigner.demoPreset`         | Shape preset id (e.g. `island`)         |
| `cn1.skindesigner.demoSidebarTab`     | `shape` / `cutouts` / `info`            |

`ScreenshotApp` toggles them between scenarios via `System.setProperty`.

## CI

The Hugo website build (`.github/workflows/website-docs.yml`) runs
this script before invoking `scripts/website/build.sh`, so the
freshly generated PNGs are rsynced into `static/developer-guide/img/`
on every run. Nothing about the screenshots ends up committed —
they are produced on demand by the website pipeline.
