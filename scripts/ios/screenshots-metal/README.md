# iOS Metal screenshot baselines

Reference images for the Metal rendering backend (`codename1.arg.ios.metal=true`). The `build-ios-metal` job in `.github/workflows/scripts-ios.yml` compares `scripts/hellocodenameone` simulator output against these PNGs via `run-ios-ui-tests.sh`'s `SCREENSHOT_REF_DIR` override.

## Scope

The Metal backend is a work-in-progress port — see [`Ports/iOSPort/METAL_PORT_STATUS.md`](../../../Ports/iOSPort/METAL_PORT_STATUS.md). The golden images here started as copies of the OpenGL baselines in [`../screenshots/`](../screenshots/) and are expected to drift once:

- `DrawString` lands (Phase 4's CoreText glyph atlas will sub-pixel-position differently from the current whole-string rasterisation; Phase 2 parity-level text will be closer but still not bit-identical),
- `ClipRect` scissor is re-enabled at the correct coord-space,
- Gradient, path, and remaining ops are ported.

The expectation is **not** pixel parity with the GL baselines. These images exist so we can track Metal's own drift over time and accept intentional improvements.

## Updating

When a Metal-side change is expected to modify a screenshot:

1. Run the CI `build-ios-metal` job (or `scripts/run-ios-ui-tests.sh` locally with `SCREENSHOT_REF_DIR=$(pwd)/scripts/ios/screenshots-metal`).
2. Download the `ios-ui-tests-metal` artifact and pull the `*.png` files for the tests that are now "different".
3. Inspect them side-by-side with the previous baseline. Accept only what's intentional.
4. Copy the accepted PNGs into this directory and commit them, naming them after the test IDs (same names as in `../screenshots/`).
