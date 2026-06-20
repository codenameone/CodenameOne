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

## Input

tvOS is remote/focus-driven (no touchscreen). Codename One's existing D-pad focus
traversal (`Form.updateFocus`, `GAME_UP/DOWN/LEFT/RIGHT`) drives navigation; the
Siri-remote → `GAME_*` mapping (UIPress / the focus engine) is a follow-up on top
of the rendering slice (the screenshot pipeline does not require input).
