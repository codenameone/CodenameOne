# Apple TV (tvOS) port

This document describes how the Codename One iOS port is built for **Apple TV
(tvOS)** and tracks the native-source guarding work. It is the tvOS analog of
`WATCHOS_PORT.md`.

## Strategy: tvOS is the Mac Catalyst model, not the watch model

Unlike watchOS (which has no UIKit / Metal / UIView and therefore needs the
dedicated Core Graphics backend in `CN1CGGraphics`), **tvOS is very close to
iOS**: it has UIKit, UIView, `UIApplicationMain` and Metal. The one rendering
difference is that **OpenGL ES is not used at runtime on tvOS** (the framework
headers ship in the SDK but the GL pipeline is unavailable), so the tvOS slice
renders through the existing **Metal backend** (`CN1_USE_METAL`) exactly like the
Mac Catalyst slice does.

Consequently the tvOS app:

- is a **separate `appletvos` Xcode target** (`<Main>TV`) added by
  `TvNativeBuilder` (it cannot be a variant of the `iphoneos` app target the way
  Mac Catalyst is, because the SDK differs), compiled from the **same**
  ParparVM-generated sources;
- builds with `CN1_USE_METAL` (the iOS default) and **excludes the OpenGL-only
  implementation files** (`CN1ES2compat.m`, `CN1ES1compat.m`, `EAGLView.m`) plus
  the iOS XIBs — identical to the Mac Catalyst `EXCLUDED_SOURCE_FILE_NAMES`;
- reuses the shared `UIApplicationMain` entry, the app delegate and the Metal
  view controller (no SwiftUI shell and no duplicate-`main` rename, unlike the
  watch target);
- weak-links the genuinely-absent frameworks (`WebKit`, and `OpenGLES`/`GLKit`
  which are present-but-unused) via ParparVM `-Doptional.frameworks`.

The tvOS SDK **does** ship the `OpenGLES`, `GLKit` and (a link-stub-only)
`MessageUI` frameworks, so — unlike the Mac Catalyst slice — **no GLKit/OpenGLES
stub headers are needed**. `MessageUI` ships only a `.tbd` with no composer
headers, so the mail/SMS composer is guarded out (as on watchOS).

Enable it with `tvNative.enabled=true`, or implicitly by declaring
`codename1.tvMain` in `codenameone_settings.properties`. `CN.isTV()` returns
`true` at runtime (backed by the `TARGET_OS_TV` native flag).

## Native guarding pattern

Where the iOS code uses an API that is **absent on tvOS**, guard it with
`#if !TARGET_OS_TV` (or broaden an existing `#if !TARGET_OS_WATCH` to
`#if !TARGET_OS_WATCH && !TARGET_OS_TV` when the API is absent on both, and an
`#if TARGET_OS_WATCH` no-op branch to `#if TARGET_OS_WATCH || TARGET_OS_TV`).
This mirrors the established `TARGET_OS_WATCH` / `TARGET_OS_MACCATALYST` guards.

### tvOS-absent APIs to guard (the working list)

Verified absent on the `appletvos`/`appletvsimulator` 26.x SDK:

- **MessageUI** composer (`MFMailComposeViewController`,
  `MFMessageComposeViewController`) — *guarded* (CodenameOne_GLViewController.h/.m,
  IOSNative.m: email/SMS methods route to the watch no-op path).
- **AddressBookUI / AddressBook** — *guarded* (IOSNative.m import).
- **UIImagePickerController**, **UIDocumentInteractionController**,
  **UIPopoverController**, **UIActionSheet**, **UIPickerView**, **UIDatePicker**
  delegates/peers — *guarding in progress* (CodenameOne_GLViewController.h
  conformance list + the date/picker peer methods in
  CodenameOne_GLViewController.m).
- **WebKit / UIWebView** — to guard (no web view on tvOS).
- **Status bar / device orientation / `UIApplication openURL`** — to guard
  (no status bar or orientation on tvOS; the remote replaces touch).

### Native compile status (verified locally against tvOS 26 simulator SDK)

Building the `HelloCodenameOneTV` target surfaces the tvOS-absent APIs in waves.
Done so far (compile clean): the builder/target generation, `IOSNative.m`,
`CodenameOne_GLAppDelegate.m`, `NetworkConnectionImpl.m`, `UIWebViewEventDelegate.*`,
`DrawStringTextureCache.m`, and the sample's `LocalNotificationNativeImpl.m`.

* **`IOSNative.m`** (was 144 errors): the tvOS-absent native features there
  (CLLocation region monitoring, `MPMoviePlayer*`, `UIPasteboard`, orientation,
  `statusBar*`, telephony) overlap almost exactly with what watchOS lacks, so the
  `!TARGET_OS_WATCH` / `TARGET_OS_WATCH` guards were broadened to also fire on
  `TARGET_OS_TV`. NOTE: this also disables a few features tvOS *does* support
  (e.g. `UITextField`/`UITextView`, AudioToolbox) — re-enabling those surgically
  is a follow-up; the broad guard gets the slice compiling + rendering first.
**Correction to the earlier IOSNative.m note:** treating tvOS like watchOS for
IOSNative.m was wrong — tvOS *supports* most of what watchOS lacks (CIFilter,
vImage/Accelerate, UIView capture, UITextField, audio, UNUserNotificationCenter).
The correct model is **tvOS ≈ iOS**: follow the iOS code path and guard only the
genuinely tvOS-absent APIs with `#if !TARGET_OS_TV`. The blanket guard-broadening
was reverted. The genuine IOSNative.m remainder is ~6 localized features:
`UIWebView` (legacy browser peer — guard like UIWebViewEventDelegate),
`MPMoviePlayerController`/`MPMoviePlayerViewController` (deprecated media player),
`UIPasteboard` (no clipboard), device orientation
(`UIInterfaceOrientation*`/`statusBarOrientation`/`attemptRotationToDeviceOrientation`),
`UIDocumentInteractionController`, and `scrollsToTop`. Plus `LAContext`
(LocalAuthentication) and the `UNNotificationAction`/`UNNotificationCategory`
push-action registration are tvOS-absent.

* **`CodenameOne_GLViewController.m`** — COMPILES on tvOS (the ~63 surgical guards
  landed: orientation, status bar, toolbar input-accessory, keyboard, hover,
  pickers/datePicker/imagePicker/actionSheet/documentInteraction delegates,
  legacy text -> `sizeWithAttributes:`, `UIPopoverController` -> `id`).
  Unlike IOSNative, this file must follow the **iOS** path (tvOS has UIView/Metal),
  so the guards are surgical `#if !TARGET_OS_TV`, NOT the watch broadening. The
  sites cluster as: device orientation (`UIInterfaceOrientation*`,
  `statusBarOrientation`), status bar (`statusBarFrame`,
  `setNeedsStatusBarAppearanceUpdate`), `UIToolbar`/`UIBarStyleBlackTranslucent`
  input-accessory, on-screen keyboard notifications (`UIKeyboard*`),
  `scrollsToTop`, `setMultipleTouchEnabled`, `UIHoverGestureRecognizer`, legacy
  `sizeWithFont:` / `drawAtPoint:withFont:` (use `sizeWithAttributes:` /
  `drawAtPoint:withAttributes:`), `systemBackgroundColor` (fall back to
  `whiteColor`), and the `UIImagePickerController` / `UIDatePicker` /
  `UIPickerView` / `UIActionSheet` delegate methods. IMPORTANT: `UIPopoverController`
  (`popoverController` / `popoverControllerInstance`) is unavailable on tvOS and
  is referenced file-wide — change its type to `id` rather than block-guarding,
  or the picker delegate guards cascade into "undeclared identifier" errors.

### Build recipe (local, no `~/.m2` collision)

The `ios-source` build bundles the *installed* `codenameone-ios` artifact's
`nativeSources`, so edits under `Ports/iOSPort/nativeSources/` only reach the
generated project after the iOS port is rebuilt+installed (use an isolated repo:
`-Dmaven.repo.local=/path/to/iso -nsu`). For a fast edit loop, sync edited
sources straight into the generated `<Main>-src/` (but do NOT overwrite the
generated `CN1ES2compat.h` — the builder uncomments `#define CN1_USE_METAL` there;
clobbering it drops the slice to the legacy GL text path). Build with arm64
(Apple-silicon tvOS sim) and a high error limit to see the full wave:

```
./scripts/build-ios-app.sh          # generates the <Main>TV target
xcodebuild -project <...>/HelloCodenameOne.xcodeproj -target HelloCodenameOneTV \
  -sdk appletvsimulator -arch arm64 CODE_SIGNING_ALLOWED=NO ONLY_ACTIVE_ARCH=YES \
  OTHER_CFLAGS="-ferror-limit=0" build
```

Once it compiles + links, run it via simctl on an "Apple TV 4K" simulator and
seed `scripts/ios/screenshots-tv/` from the captured frames (the `build-ios-tv`
CI job is non-blocking until then).

## Status: compiles, renders, and is screenshot-gated

The tvOS slice now builds end-to-end, boots on the Apple TV simulator, runs the
`cn1ss` suite, and captures real frames. `build-ios-tv` in `scripts-ios.yml` is a
**blocking** golden gate (goldens in `scripts/ios/screenshots-tv/`, captured from a
CI run at 3840×2160). The two non-obvious fixes that made rendering work:

1. **Programmatic METALView (`-loadView`)** and **the `screenTexture` readback**
   (`cn1_copyMetalScreenTextureImage` + its call site in `cn1_renderViewIntoContext`)
   were both `#if defined(CN1_USE_METAL) && TARGET_OS_MACCATALYST`. tvOS uses the
   same nil-NIB path as Mac Catalyst (its XIBs are excluded from the bundle), so
   without `loadView` no METALView is created (`EAGLView not found`) and forms
   render to nothing → blank captures; and on a headless simulator
   `-drawViewHierarchyInRect:` snapshots a blank CALayer, so the screenTexture
   readback is required. Both broadened to `(TARGET_OS_MACCATALYST || TARGET_OS_TV)`.
   tvOS stages the readback into a **Shared** texture (`MTLStorageModeManaged` /
   `-synchronizeResource:` are macOS-only).
2. The VC is instantiated with **`cn1NibName = nil`** on tvOS (the iOS XIB is
   excluded), same as Mac Catalyst — otherwise launch crashes with "Could not load
   NIB in bundle … CodenameOne_GLViewController".

### Local repro recipe (faithful to CI)

CI compiles the generated sources with the builder's permission/feature `#define`s
on, which a naive local build leaves off, hiding whole waves of tvOS-absent APIs.
To reproduce CI exactly, compile with the full define set and arm64:

```
xcodebuild -project <…>/HelloCodenameOne.xcodeproj -target HelloCodenameOneTV \
  -sdk appletvsimulator -arch arm64 ONLY_ACTIVE_ARCH=NO CODE_SIGNING_ALLOWED=NO \
  GCC_PREPROCESSOR_DEFINITIONS='$(inherited) ENABLE_WKWEBVIEW=1 INCLUDE_CAMERA_USAGE=1 \
    INCLUDE_CN1_CAMERA=1 CN1_INCLUDE_NOTIFICATIONS=1 CN1_INCLUDE_NOTIFICATIONS2=1 \
    INCLUDE_CN1_PUSH=1 INCLUDE_CN1_PUSH2=1' OTHER_CFLAGS=-ferror-limit=0 build
```

Sync **all** of `Ports/iOSPort/nativeSources/*` into the generated `<Main>-src/`
(not just the file under edit) and keep `CN1_USE_METAL` defined in the generated
`CN1ES2compat.h`. For an end-to-end render check, download the tvOS simulator
runtime (`xcodebuild -downloadPlatform tvOS`), create an "Apple TV 4K" device, and
run `scripts/run-tv-ui-tests.sh <project>` with `CN1SS_TV_UDID` + `JAVA17_BIN` set.

**Seed goldens from CI, never local:** a local Apple TV sim rendered at 1080×2206
(portrait) while CI renders 3840×2160 — 0/127 matched. Download the `tv-ui-tests`
artifact and seed those. Watch for title-capture races when seeding (a frame can
catch the *previous* test's title); the 127/128 that match across two independent
CI runs are reliable, a lone differ usually means a glitched golden.

## Input

tvOS is remote/focus-driven (no touchscreen). Codename One's existing D-pad focus
traversal (`Form.updateFocus`, `GAME_UP/DOWN/LEFT/RIGHT`) drives navigation; the
Siri-remote → `GAME_*` mapping (UIPress / the focus engine) is a follow-up on top
of the rendering slice (the screenshot pipeline does not require input).
