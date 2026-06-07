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

### 1b. Mouse wheel scrolling — not wired

`WM_MOUSEWHEEL` / `WM_MOUSEHWHEEL` are not handled in the native window proc, so
the scroll wheel does nothing. Touch/drag scrolling works (that path is what the
overscroll-smear fix exercised). To wire it, push a wheel event from the WndProc
into the input ring (delta in the spare `eventScratch` slot, like keys) and, in
`WindowsImplementation.drainInput()`, translate it into a scroll. The JavaSE port
shows the canonical approach (`JavaSEPort.mouseWheelMoved`): a synthetic
pointerPressed → pointerDragged → pointerReleased sequence run through Codename
One's scroll logic, with the component under the cursor temporarily made
non-focusable so the synthetic press cannot register as a click. Deferred.

### 2. SIMD acceleration — software fallback only

`com.codename1.util.Simd` (`Simd.java`, `@Concrete` → `IOSSimd`) is the portable
vector API (packed byte/int/float add/sub/mul/min/max/dot/unpack/…). iOS overrides
it with NEON-backed `native` methods (`IOSSimd.isSupported()` → `true`). The
Windows port has **no `Simd` override**, so `isSupported()` is `false` and every
op runs the Java software fallback — no SSE/AVX (x64) or NEON (arm64). Correct,
but slow for the image/pixel hot paths that use it. A `WindowsSimd` with intrinsic
or compiler-autovectorized native ops is the iOS-parity fix.

### 3. Camera — no host webcam access

`createCameraImpl()` is **not** overridden, so `Camera.isSupported()` is `false`
(the base returns `null`). This is deliberate: the earlier synthetic-frame backend
was removed because a port must not hand a shipping app fake frames. The legacy
`Capture` API (`capturePhoto/captureVideo/captureAudio`) is likewise unimplemented.
**To-do:** real Media Foundation (`IMFSourceReader` / Media Capture) webcam
enumeration + preview peer + still capture, surfaced honestly through
`Camera.getCameras()` / `isSupported()`.

### 4. Platform services — not implemented (report unsupported)

All inherit the base defaults (null / no-op / `false`); none fabricate data:

- Location / GPS (`getLocationManager` → null)
- Sensors: accelerometer, compass/magnetometer (`getSensor*`)
- Contacts (`getContacts` / `getContactById`)
- Push + local notifications (`sendLocalNotification`)
- Vibrate, dial, SMS
- Clipboard (copy/paste) — common desktop expectation; worth doing early
- Native file open/save dialogs + gallery picker (`IFileOpenDialog` /
  `IFileSaveDialog`) — common desktop expectation; worth doing early
- System share, print
- Biometric (Windows Hello)
- Audio recording

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

**Short answer: a runnable build needs Windows.** The native compile links against
the Windows SDK (Direct2D / DirectWrite / WIC / Media Foundation / WinHTTP / Win32)
and uses `clang-cl` (LLVM on the MSVC ABI) + CMake + Ninja inside the Visual Studio
developer environment (`vcvarsall.bat`, located via `vswhere`). See
`WindowsNativeBuilder.java` and `ByteCodeTranslator.writeCmakeProject`.

- **CI** builds on real Windows runners: `windows-latest` (x64, the merge gate) and
  `windows-11-arm` (arm64, experimental / non-blocking). Cross-*architecture*
  builds work *on Windows* (e.g. arm64 from an x64 host via the VS cross
  environment).
- **The build orchestration is OS-independent** (arch resolution, translator
  invocation, CMake argument assembly are unit-tested anywhere); only the final
  `clang-cl` compile/link step is Windows-bound.
- **Linux cross-compile:** *theoretically* possible but **not set up and not
  recommended**. `clang-cl` can target Windows from Linux with `/winsysroot`
  pointing at a Windows SDK + MSVC toolchain fetched by a tool like
  [`xwin`](https://github.com/Jake-Shadle/xwin). That could give a *compile-only*
  pre-check, but: (a) it cannot **run or validate** the result — Direct2D /
  DirectWrite need a real Windows GPU/display stack, so rendering can only be
  verified on Windows (a Parallels/HyperV VM or a real machine); (b) it is
  unproven against this port's link set; (c) the MSVC headers/libs are Microsoft's
  and you must accept their license via xwin. **Recommendation:** develop and
  validate in a Windows 11 VM (Direct2D/DirectWrite are GPU-accelerated there);
  let CI's two Windows legs be the authoritative gate.

---

## Testing notes

- The screenshot suite renders each test Form **headlessly** (offscreen Direct2D /
  WIC) and diffs against goldens in `scripts/windows/screenshots/`. This covers
  rendering well but, by construction, exercises **no interactive paths** — native
  text editing (gap #1), real input focus, IME, and camera preview are all
  invisible to it. Those need interactive verification on a real Windows desktop
  plus dedicated device-runner coverage before they can be trusted.
