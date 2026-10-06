# Local camera regressions

These tests use generated barcode data and contain no support attachments.
They run locally; no CI workflow or native-test label is involved.

## Core tests

Use Java 8 and run from the repository root:

```sh
./scripts/fast-core-unit-smoke.sh 'AndroidCameraFrameConverterTest,CameraFrameTest,VisionCameraViewTest,AndroidCameraThreadingTest,VisionPipelineTest,CodeScannerTest'
```

The converter tests compile the production Android helper and check planar,
interleaved, overlapping, read-only, offset and padded YUV buffers, including
last rows without trailing padding and malformed input. Core tests also verify
that live analysis requests raw frames, never invokes the lazy JPEG encoder,
and retains JPEG fallback on ports without raw frames.

## Android emulator/device

Requires JDK 17, Gradle 8.13, Android SDK 36, and a running emulator/device with
an enabled camera. Set `GRADLE` to a Gradle 8.13 executable if needed.

```sh
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_SERIAL=emulator-5554
./scripts/camera-regression/run-android-tests.sh
```

The isolated APK compiles the production converter and CameraFrame classes.
The extraction step copies the production frame-delivery methods unchanged
into a small harness, so it exercises image closing, throttling, rotation,
raw delivery and lazy JPEG generation without needing a CN1 app build.
Tests use real Android ImageReader/ImageWriter buffers and ML Kit, plus QR and
PDF417 fixtures across planar/interleaved and padded/unpadded layouts. A
foreground Camera2 stream feeds emulator camera images through those same
production delivery methods. This does not test CameraX binding or reproduce
a physical device's sensor, autofocus or vendor buffer layout.

Run on both an older and a newer Android image. The suite was checked on API
34 and API 36; the fixture matrix independently covers hardware-dependent
layouts that an OS-version comparison alone cannot guarantee.

## Apple preview layout

On macOS with Xcode selected:

```sh
./scripts/camera-regression/run-apple-preview-tests.sh
./scripts/camera-regression/run-apple-preview-tests.sh BOOTED_IOS_SIMULATOR_UDID
```

The script extracts the production preview subclass and compiles it against
real AppKit/UIKit and AVFoundation. It checks portrait, landscape, embedded,
zero-sized and restored bounds and ensures unrelated layers are untouched.
The macOS test runs under ARC and manual reference counting. The optional
simulator step currently targets Apple Silicon. No camera access is needed:
it verifies preview geometry, not optical capture or barcode recognition.
