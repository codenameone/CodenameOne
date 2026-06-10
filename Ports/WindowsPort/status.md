# Native Windows Port — Status & Gaps

The native Windows port compiles a Codename One app to a standalone Win32
executable (no JVM) via ParparVM's "windows" clean C target + clang-cl/Ninja,
rendering with Direct2D / DirectWrite / WIC. This document tracks what is **not**
done yet, so nobody mistakes the port for feature-complete.

The guiding rule for every gap below: a real device port returns **real data or
reports unsupported** — it must never fabricate data (synthetic camera frames,
fake sensor readings, …) that could reach a shipping app. Only the JavaSE
*simulator* is allowed to synthesize, because the developer knows it is a
simulator. If a capability is not implemented here, the matching
`isXxxSupported()` returns `false` / the factory returns `null`.

## What works today

- Windowing, input (pointer/keyboard), EDT/message-pump threading.
- Graphics: Direct2D primitives, shapes, gradients, clipping (incl. shape clips
  under transform), affine + perspective/camera transforms (software texture
  map), images via WIC, screenshots.
- Text: DirectWrite layout/measure/draw; bundled TrueType fonts loaded in-memory.
- Resources embedded in the `.exe` (PE resource section) — single self-contained
  binary; `getResourceAsStream` reads them via `FindResource`.
- CSS/SVG/Lottie via the standard Maven build (transcode-svg, CEF-backed CSS).
- Networking (WinHTTP), raw sockets, WebSocket, filesystem/storage.
- Media playback (Media Foundation), `BrowserComponent` (WebView2, when the SDK
  is present at build time).
- Theme: material native theme + app theme, light/dark.

---

## Gaps (in rough priority order)

### 1. Native in-place text editing — implemented; lacks automated coverage

In Codename One a `TextField` / `TextArea`, while being edited, is replaced by a
**native editing widget** (a real OS text control with the native keyboard / IME
/ caret / selection), removed when editing ends with the value written back to
the lightweight component. It is a subtle, stateful, async path and historically
one of the most bug-prone areas of any port.

- **Current state:** implemented. `isNativeInputSupported()` and
  `isAsyncEditMode()` return `true`; `editString(...)` overlays a borderless
  Win32 `EDIT` control on the component (styled to match the field — font, fg/bg,
  padding-inset bounds), streams typed text back to the `TextArea` live (so
  switching field / scrolling away never loses changes), and tears the control
  down on commit. Scrolling away or editing another field commits and removes the
  overlay (`hideTextEditor` / `stopTextEditing` overrides). Clipboard copy/paste
  is wired. Native layer: `cn1_windows_edit.c`; Java: `WindowsImplementation`
  `editString` / `syncEditText` / `commitEdit`. Verified interactively on a real
  Windows desktop.
- **Remaining work:** there is still **no automated coverage** — the headless
  screenshot path never opens the edit overlay, so a regression would not be
  caught by CI; a device-runner case (focus → type → commit → blur → value) is
  needed. IME composition (CJK), bidi/RTL, and a keyboard toolbar are not yet
  verified. A `RichEdit`-backed control may be needed for advanced cases.

### 1b. Mouse wheel scrolling — implemented (shared core API)

Done. `WM_MOUSEWHEEL` / `WM_MOUSEHWHEEL` are handled in the native window proc
(`cn1_windows_window.cpp`): the cursor is mapped to client coordinates and the
signed delta is pushed into the input ring as a new `CN1_EVENT_MOUSE_WHEEL` /
`CN1_EVENT_MOUSE_HWHEEL` event. `WindowsImplementation.drainInput()` converts the
delta to a DPI-scaled pixel distance (`wheelUnits`) and feeds it to the shared
scroll entry point.

Rather than re-implement the synthetic-scroll hack per port, the mapping now
lives once in the core: `CodenameOneImplementation.pointerWheelMoved(x, y,
scrollX, scrollY)` replays the wheel as a press → drag → release gesture spread
across four EDT cycles (so Codename One's own tensile/deceleration animates it),
temporarily makes the component under the cursor non-focusable so the synthetic
press is not a click, and reports `isScrollWheeling()` for the duration. The
JavaSE port was refactored onto this same method (it had carried the original
inline implementation), so every desktop port maps the wheel identically.

### 2. SIMD acceleration — implemented (SSE2 x64 / NEON arm64)

Done. `WindowsSimd` (the x86/ARM analog of `IOSSimd`) overrides the hot-path
vector ops with `native` SSE2 (x64) / NEON (arm64) kernels in
`nativeSources/cn1_windows_simd.c`, and `Simd`'s `@Concrete` now carries
`win = "com.codename1.impl.windows.WindowsSimd"` plus a
`WindowsImplementation.createSimd()` override, so `Simd.get().isSupported()`
returns `true` on Windows. Each kernel runs a 128-bit vector main loop with a
scalar tail; loads/stores are unaligned, so no special aligned allocator is
needed. Ops SSE2 lacks natively (int32 mul/min/max/dot) stay scalar on x64 but
are vectorized on arm64; every op not overridden falls back to the portable
`Simd` scalar loop, so the API is always complete and correct.

Covered natively: int add/sub/mul/min/max/and/or/xor/sum/dot, float
add/sub/mul/min/max/sum/dot, byte add/sub (saturating)/and/or/xor, and the fused
image hot paths `replaceTopByteFromUnsignedBytes` / `blendByMaskTestNonzero`.

Correctness is gated by the existing `SimdApiTest` (already in the Windows
screenshot suite). A new `SimdBenchmarkTest` tallies the speedup: it times the
native kernel against an inline Java scalar loop over a 64K-element workload,
verifies the native result is identical, and logs `CN1SS:SIMD:BENCH … speedup=Nx`
so CI shows the concrete benefit (the iOS-parity "benchmark/tally").

### 3. Camera — no host webcam access

`createCameraImpl()` is **not** overridden, so `Camera.isSupported()` is `false`
(the base returns `null`). This is deliberate: the earlier synthetic-frame backend
was removed because a port must not hand a shipping app fake frames. The legacy
`Capture` API (`capturePhoto/captureVideo/captureAudio`) is likewise unimplemented.
**To-do:** real Media Foundation (`IMFSourceReader` / Media Capture) webcam
enumeration + preview peer + still capture, surfaced honestly through
`Camera.getCameras()` / `isSupported()`.

### 4. Platform services — desktop services done; hardware services unsupported

**Implemented (honest desktop backends):**

- Clipboard (copy/paste) — real Win32 clipboard (`cn1_windows_io.c`,
  `clipboardSetText` / `clipboardGetText`), so text round-trips with other apps.
- Launch services via `ShellExecuteW` (`shellOpen`): `execute(url)` opens the
  default browser/handler, `dial()` opens the dialer (`tel:`), `sendSMS()` the
  Messaging app (`sms:?body=`, so `getSMSSupport()` reports `SMS_INTERACTIVE`),
  `sendMessage()` the mail client (`mailto:?subject=&body=`). Nothing is
  fabricated — an absent handler reports failure rather than pretending to send.
- Native file open dialog + gallery picker — `GetOpenFileNameW` (comdlg32) run
  modally on the window-owning pump thread (marshaled via a blocking
  `WM_CN1_FILEDIALOG` `SendMessage`), filtered by media type. `openGallery` /
  `openImageGallery` now use the real OS picker and return a `file://` path the
  port's `FileSystemStorage` opens, instead of the in-app `FileTree` fallback.
  `fileDialog(save, …)` also exposes the save dialog for future hooks.
- Secure storage (`getSecureStorage` → `WindowsSecureStorage`) — the
  non-prompting key/value store the networking layer reads on every call (LLM API
  keys, refresh tokens). Values are encrypted with DPAPI (`CryptProtectData`,
  bound to the Windows user account) and the ciphertext persisted through CN1
  `Storage`; the desktop analog of the iOS keychain / Android
  EncryptedSharedPreferences. The biometric-prompting overloads map to the same
  store (DPAPI is itself the user-account auth boundary); a Windows Hello gate can
  layer on once biometric support lands. Round-tripped by `SecureStorageTest`.
- Local notifications (`scheduleLocalNotification` / `cancelLocalNotification`) —
  a `Shell_NotifyIcon` tray balloon, the same desktop semantic the JavaSE port
  uses: while the app runs, a `Timer` fires the notification at its scheduled time
  (with `REPEAT_*` support) and clicking the balloon dispatches the id to the
  app's `LocalNotificationCallback` (`cn1_windows_notify.c`). Desktop background
  scheduling only fires while the process is running -- there is no OS scheduler
  that survives app exit (a tracked limitation, not a desktop capability).

**System share — implemented (WinRT).** `isNativeShareSupported()` /
`share(...)` use the WinRT `DataTransferManager`: the EDT-facing `shareText`
marshals to the window thread (`WM_CN1_SHARE`), where `IDataTransferManagerInterop`
`GetForWindow` + `ShowShareUIForWindow` open the system share flyout for the
unpackaged Win32 window and a `DataRequested` handler supplies the text/title
(`cn1_windows_winrt.cpp`, same `CN1_HAVE_WINRT` gate). Shares text today (the
common case); image-file sharing via `SetStorageItems` is a follow-up. Compiles
(real + stub) on the Windows ARM64 VM; the flyout itself is interactive.

**Print — not applicable.** Codename One core exposes no printing API (no
`Printer` class or impl hook), so there is nothing for the port to override; this
would require a new cross-platform printing API in core first.

**Still unsupported (return null / no-op / `false`, never fabricated):** these
are genuine hardware/OS-account capabilities that a desktop either lacks or that
need further Media-Foundation work, so per the port's "real or unsupported" rule
they stay honest until backed by a real implementation:

- Location / GPS (`getLocationManager` → null)
- Sensors: accelerometer, compass/magnetometer (`getSensor*`)
- Contacts (`getContacts` / `getContactById`)
- Push + local notifications (`sendLocalNotification`)
- Vibrate (no desktop vibration motor)
- System share, print

**Contacts — implemented (WinRT).** `getAllContacts` / `getContactById` read the
user's contacts via the WinRT `ContactStore` (`cn1_windows_winrt.cpp`, same
`CN1_HAVE_WINRT` gate). One native call returns every contact as a delimited blob
(id / name / phone / email) which the impl parses and briefly caches, so the
base's id-then-fetch loop shares a single store read. Returns nothing when the
store is inaccessible (no WinRT / access denied) -- honest, never fabricated.
Compiles on the Windows ARM64 VM via the same proven WRL await pattern as
biometric/location.

**Location / GPS — implemented (WinRT).** `getLocationManager()` →
`WindowsLocationManager`, backed by the WinRT `Geolocator`
(`cn1_windows_winrt.cpp`, same `CN1_HAVE_WINRT` gate). `getCurrentLocation` /
`getLastKnownLocation` resolve one fix (lat/lon/accuracy/altitude/heading/speed);
a continuous `LocationListener` is served by a polling thread. When Windows
location is disabled or no provider answers, it reports `OUT_OF_SERVICE` / throws
rather than fabricating a position; `getLocationManager` returns `null` on a
WinRT-less build. Verified on the Windows ARM64 VM: the `Geolocator` activates and
`GetGeopositionAsync` returns `E_ACCESSDENIED` (location off on the VM), which the
port surfaces honestly as unavailable.

**Biometric (Windows Hello) — implemented.** `getBiometrics()` →
`WindowsBiometrics`, backed by the WinRT `UserConsentVerifier` (face / fingerprint
/ PIN). `isSupported()` / `canAuthenticate()` map to `CheckAvailabilityAsync`;
`authenticate(...)` runs the system Hello prompt off the EDT and completes the
`AsyncResource`. WinRT is consumed via the WRL ABI projection
(`cn1_windows_winrt.cpp`), gated on `CN1_HAVE_WINRT` (the generated CMake probes
the toolchain and defines it only when the WinRT ABI headers + `runtimeobject`
link), so a cross-compile sysroot without WinRT compiles the natives as honest
"unsupported" stubs and stays green — the same gating model as WebView2. Verified
on a real Windows ARM64 VM: the WRL pattern activates the factory and awaits the
async op; the VM correctly reports `DeviceNotPresent` (no Hello hardware), so the
port reports unsupported there and a real Hello-equipped laptop reports available.

**Audio recording — implemented.** `createMediaRecorder` / `captureAudio` record
from the default microphone via the classic `waveIn` (winmm) API to a 16-bit PCM
WAV file (`cn1_windows_audiorec.c` + `WindowsAudioRecorder`); a worker thread
drains capture buffers to disk and the RIFF/data sizes are patched on stop.
`getAvailableRecordingMimeTypes()` reports `audio/wav` (also decodable by the
port's MF playback). waveIn rather than an MF encode pipeline: dependency-free
and robust (no codec/media-type negotiation). Verified on a real Windows ARM64
VM — compiles clean and `waveIn` captures 88200 bytes/s from the mic.

### 5. Native peers beyond WebView2

Only `BrowserComponent` (WebView2) is wired as a native peer, and only when the
WebView2 SDK is present at build time (otherwise the browser natives compile as
stubs and `isNativeBrowserComponentSupported()` is `false`). No other native peer
components (e.g. native maps, video peer, native list).

### 5a. Custom native interfaces (`@NativeInterface`) — builder binding wired

`WindowsNativeBuilder` now binds app-defined native interfaces the same way the
iOS builder does, so they resolve to the app's own C/C++ on the clean target:

- It scans the app classes for `NativeInterface` implementors and generates, per
  interface, an `XxxStub` (the `NativeLookup.register(Xxx.class, XxxStub.class)`
  target) plus an `XxxImplCodenameOne` carrying the actual `native` methods. The
  translator emits one C function per native method (mangled name); the app
  defines them in its own `nativeSources` C/C++. A `PeerComponent` return is
  bridged as a `long[]{handle}` and unwrapped via `((long[])p.getNativePeer())[0]`
  — identical to iOS — so a returned native widget becomes a real `PeerComponent`.
- It also generates `<MainClass>Stub` (the executable entry point) whose `main()`
  runs the generated `NativeLookup.register(...)` calls and boots the Lifecycle app
  windowed. The clean target auto-selects it because it is the only class with a
  `main(String[])`. (Before this, `WindowsNativeBuilder` passed the Lifecycle main
  class straight through and could not build a real app — the translator requires a
  `main()`.)

Verified to the plugin-compile level only. **Still to verify end-to-end** with a
real app that declares a native interface (run through the full Maven plugin), and
the *rendering* side of returned peers — positioning/painting a child `HWND` over
the CN1 `PeerComponent` (only `BrowserComponent`'s WebView2 peer does this today;
the generic peer-placement path in `WindowsImplementation` is the remaining work).

---

## Build & toolchain — where can this be built?

**Short answer: the binary builds on Windows *or* Linux; only *running* it needs
Windows.** The native compile links against the Windows SDK (Direct2D / DirectWrite
/ WIC / Media Foundation / WinHTTP / Win32). `WindowsNativeBuilder` picks the
toolchain from the host:

- **Windows host:** `clang-cl` (LLVM on the MSVC ABI) + CMake + Ninja inside the
  Visual Studio developer environment (`vcvarsall.bat`, located via `vswhere`).
- **Non-Windows host (e.g. the Linux build cloud):** cross-compiles with `clang-cl`
  + `lld-link` + `llvm-rc` against a Windows SDK laid out by
  [`xwin`](https://github.com/Jake-Shadle/xwin), pointed at by the `windows.sdkRoot`
  build hint / `CN1_XWIN_SYSROOT`. clang is a cross-compiler, so the PE is identical
  to a Windows-host build. Both `x64` and `arm64` are produced this way. The MSVC
  headers/libs are Microsoft's — `xwin` requires accepting their license.

  Prerequisites for such a host: a modern LLVM (`clang-cl`/`lld-link`/`llvm-rc`,
  18+ known-good), CMake ≥ 3.10, Ninja, a JDK, and an `xwin splat` of the SDK. The
  `crossCompilesWindowsExeWithXwin` test (and the `windows-cross-compile.yml` CI
  job it backs) is the readiness check: if it links a PE, the host is good.

- **CI:** Windows runners (`windows-latest` x64 + `windows-11-arm` arm64) build
  *and run* the screenshot suite (the authoritative render gate); a separate
  `ubuntu-latest` job cross-compiles the port to a Windows PE to prove the
  Windows-free build path stays green.
- **What still needs Windows:** *running* the binary. Direct2D/DirectWrite need a
  real Windows GPU/display stack (Wine's D2D/DWrite/DXGI support is too incomplete),
  so rendering can only be verified on Windows — a Parallels/Hyper-V VM or a real
  machine. Build on Linux; test on Windows.

---

## Testing notes

- The screenshot suite renders each test Form **headlessly** (offscreen Direct2D /
  WIC) and diffs against goldens in `scripts/windows/screenshots/`. This covers
  rendering well but, by construction, exercises **no interactive paths** — native
  text editing (gap #1), real input focus, IME, and camera preview are all
  invisible to it. Those need interactive verification on a real Windows desktop
  plus dedicated device-runner coverage before they can be trusted.
