# iOS screenshot baselines

Reference images for the iOS simulator. The `build-ios-metal` job in `.github/workflows/scripts-ios.yml` compares `scripts/hellocodenameone` simulator output against these PNGs via `run-ios-ui-tests.sh`, which also defaults to this directory when `SCREENSHOT_REF_DIR` is unset.

## Scope

Metal is the only iOS rendering backend; the OpenGL ES 2 pipeline and its separate baseline set are gone. These images are the iOS baseline, full stop.

The directory keeps its `-metal` suffix because the port id (`ios-metal`), the published port-status report and its history are all keyed on that name.

## Updating

When a change is expected to modify a screenshot:

1. Run the CI `build-ios-metal` job (or `scripts/run-ios-ui-tests.sh` locally).
2. Download the `ios-ui-tests-metal` artifact and pull the `*.png` files for the tests that are now "different".
3. Inspect them side-by-side with the previous baseline. Accept only what's intentional.
4. Copy the accepted PNGs into this directory and commit them, naming them after the test IDs.
