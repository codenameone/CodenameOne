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

The remaining guards are mechanical and are driven to convergence by the
`build-ios-tv` CI job (`scripts/run-tv-ui-tests.sh`) building the `<Main>TV`
scheme against the tvOS simulator, the same way the watch port converged. Run it
locally with:

```
./scripts/build-ios-app.sh          # generates the <Main>TV target
xcodebuild -project <...>/HelloCodenameOne.xcodeproj -target HelloCodenameOneTV \
  -sdk appletvsimulator CODE_SIGNING_ALLOWED=NO build
```

## Input

tvOS is remote/focus-driven (no touchscreen). Codename One's existing D-pad focus
traversal (`Form.updateFocus`, `GAME_UP/DOWN/LEFT/RIGHT`) drives navigation; the
Siri-remote → `GAME_*` mapping (UIPress / the focus engine) is a follow-up on top
of the rendering slice (the screenshot pipeline does not require input).
