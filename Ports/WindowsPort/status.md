# Native Windows Port — Remaining TODO

The native Windows port compiles a Codename One app to a standalone Win32
executable (no JVM) via ParparVM's "windows" clean C target + clang-cl/Ninja,
rendering with Direct2D / DirectWrite / WIC. Everything previously tracked here as
a gap is now implemented — windowing/input, graphics, text, images, media playback,
`BrowserComponent` (WebView2), networking/sockets/WebSocket, storage, CSS/SVG/Lottie,
SIMD (SSE2/NEON), secure storage (DPAPI), biometric (Windows Hello), location,
contacts and share (WinRT), audio recording (waveIn), local notifications, mouse
wheel, native text editing, camera (still capture + the device camera API with a
live preview peer), and generic native-peer placement.

This file now lists **only what is not done**, so it can be deleted once these are
closed or moved to issues. The guiding rule stays: a real device port returns **real
data or reports unsupported** — it must never fabricate data; only the JavaSE
*simulator* may synthesize.

## TODO

### Native text editing — automated coverage
Implemented (a borderless Win32 `EDIT` overlay styled to the field, live write-back,
clipboard) and verified interactively, but the headless screenshot suite never opens
the overlay, so a regression would not be caught. Needs a device-runner case
(focus → type → commit → blur → value), plus IME composition (CJK), bidi/RTL, and a
keyboard toolbar; a `RichEdit`-backed control may be needed for advanced cases.

### Camera — video recording
Still capture, the device camera API (live preview peer + stills + frame listener,
backed by Media Foundation), and device enumeration are implemented. Video recording
is not: a generic desktop webcam exposes no hardware encoder through the source
reader, so `startVideoRecording` reports unsupported — a real MF sink-writer
(H.264/AAC) pipeline is the remaining work. Flash / optical zoom / focus-point are
absent on desktop webcams and are reported unsupported.

### Native peers — broaden + verify end-to-end
Generic native-peer placement (reparent a child HWND onto the host window,
position/size/show it over the `PeerComponent`, and fall back to a `PrintWindow`
peer image in the offscreen pipeline) is implemented for `@NativeInterface`-returned
widgets, alongside the WebView2 browser and Direct3D GPU peers. Still to do: verify
an app-declared `@NativeInterface` that returns a peer end-to-end through the full
Maven plugin, and add higher-level peers (native maps, a native video peer).

### Sensors / vibrate
Accelerometer / compass are unimplemented (`getSensor*`); some laptops and tablets
expose them through the `Windows.Devices.Sensors` API and could be wired where the
hardware is present. Vibrate is not applicable (no desktop vibration motor).

## Testing note
The screenshot suite renders each Form **headlessly** (offscreen Direct2D / WIC), so
by construction it exercises **no interactive paths**: native text editing, real
input focus / IME, camera preview and native-peer placement are invisible to it and
need device-runner / VM coverage before they can be fully trusted. The camera live
path and native peers in particular need a real Windows machine (a webcam for the
camera) to verify; build on Linux or Windows, but test the live behaviour on Windows.
