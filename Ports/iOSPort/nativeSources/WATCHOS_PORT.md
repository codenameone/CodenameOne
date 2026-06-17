# watchOS rendering port — status & rollout

This documents the watchOS slice of the iOS port (full CN1 UI on Apple Watch via
a Core Graphics backend). Everything is additive under `#if TARGET_OS_WATCH`, so
the iOS slice is byte-for-byte unchanged.

## watchMain entry point & seamless double-app build

A CN1 project declares the watch entry point next to the phone main in
`codenameone_settings.properties`:
```
codename1.mainName=com.example.MyApp        # phone lifecycle ("main" class)
codename1.watchMain=com.example.MyWatchApp  # watch lifecycle (Apple Watch + Wear)
```
`codename1.watchMain` flows through `CN1BuildMojo` as the `watchMain` build arg.
`WatchNativeBuilder.parseHints` auto-enables the watch slice whenever `watchMain`
is present (no separate `watchNative.enabled` needed), so the regular iPhone
build emits the packaged double app. The watch lifecycle class may equal the
phone main, but a distinct class lets the watch slice tree-shake from its own
root (a follow-up can run a second ParparVM pass rooted at `watchMain` for a
smaller watch binary; today both slices share one translation).

`WatchNativeBuilder.writeWatchEntry` generates the watch target's entry point:
- `CN1WatchApp.swift` - the SwiftUI `@main` shell that starts `CN1WatchHost`
  and forwards Digital Crown + tap input;
- `CN1WatchBootstrap.m` - defines the `cn1_watch_*` hooks `CN1WatchHost` calls,
  delegating to `cn1_watch_runtime_*` (the CN1 runtime started at the watchMain
  lifecycle, emitted by the generated watch Stub - the remaining rollout item);
- a Swift bridging header.

Because the watch app is SwiftUI-`@main`-rooted, the shared ParparVM `int main()`
(the phone entry) must be excluded from the watch target via
`watchNative.phoneMainSource=<translated phone-main .m filename>` (added to the
watch target's `EXCLUDED_SOURCE_FILE_NAMES`).

## Complete interactive app on the simulator — VERIFIED (2026-06-17)

A complete multi-screen watch app runs on the watchOS 26.2 simulator through the
**production pipeline** (`CN1WatchHost` timer pump -> `cn1_watch_paintFrame` ->
`CN1CGGraphics` -> `presentFramebuffer` -> SwiftUI surface), driven by a
watchMain-style lifecycle (`/tmp/watchspike/SpikeWatch/Sources/CN1SpikeWatchMain.m`):
a scrollable home list (Weather/Steps/Heart Rate/Messages/Settings) with colored
accent bars, Digital-Crown + drag scrolling, and tap navigation into per-item
detail screens with a Back header. Home + detail both render correctly
(`/tmp/watchspike/app_home.png`, `app_detail.png`). This exercises the full
lifecycle -> host -> render -> input loop on real watchOS. Still pending for the
*ParparVM-translated framework* app (vs this hand-written lifecycle): the per-op
CG rollout below + ParparVM `arm64_32`/GC + the watch Stub emitting
`cn1_watch_runtime_*`.

## Phase 0 spike — PASSED (2026-06-17)

The Core Graphics rendering foundation was validated on the real watchOS 26.2
simulator (Xcode 26.3). A minimal watchOS SwiftUI app (`/tmp/watchspike/`) links
`CN1CGGraphics` + `CN1WatchRenderingView` (compiled `arm64`, `-fno-objc-arc`) and
renders a CN1-style form (title bar, button, separator line, Core Text labels,
vertical gradient) full-screen at native 2x. Rebuild/run:
```
ruby /tmp/watchspike/gen_project.rb
cd /tmp/watchspike/SpikeWatch && xcodebuild -target SpikeWatch -sdk watchsimulator \
  -configuration Debug SYMROOT=/tmp/watchspike/sym build
xcrun simctl install <udid> /tmp/watchspike/sym/Debug-watchsimulator/SpikeWatch.app
xcrun simctl launch  <udid> com.codename1.spikewatch
```
Two real bugs were found + fixed during the spike (both in the canonical
sources):
1. `CN1WatchRenderingView`/`CN1WatchHost` delegate properties were `weak` —
   illegal under the port's manual reference counting; changed to `assign`.
2. `CN1CGBeginFrame` reset the CTM via `CGAffineTransformInvert`, discarding the
   device scale `allocBitmap` had applied, so content drew at 1x in the
   bottom-left corner. Now it saves the scaled base and only applies the
   top-left flip on top (S0/S1 save-state model; `CN1CGEndFrame` pops both).

What this proves: the CG backend compiles + runs on watchOS and the
coordinate/scale model is correct. The per-op rollout below is now unblocked.
What it does NOT yet prove: ParparVM C + GC on `arm64_32` (still the Phase 0
runtime risk for the *real* translated app, vs. this hand-written spike).

## Implemented (foundation)

- **`CN1CGGraphics.{h,m}`** — Core Graphics rasterizer backend. Top-left
  coordinate flip, color/alpha helpers, and primitives: fill/draw/clear rect,
  line, polygon, image, tiled image, Core Text string, linear/radial gradient,
  clip (rect + polygon, with CN1 replace-semantics emulated via gstate rebase),
  and the affine transform stack.
- **`CN1WatchRenderingView.{h,m}`** — `CN1RenderingView`-conforming surface
  backed by a `CGBitmapContext`. `setFramebuffer` binds the context to
  `CN1CGGraphics`; `presentFramebuffer` snapshots a `UIImage` and hands it to a
  `CN1WatchFramePresenter` (the host).
- **`CN1RenderingView.h`** — `addPeerComponent:` degrades to `id` on watchOS
  (no `UIView`).
- **Ops wired** (proven pattern, `#if TARGET_OS_WATCH` branch → `CN1CG*`):
  `FillRect`, `DrawRect`, `ClearRect`, `DrawLine`.

## Mechanical rollout (do after the Phase 0 on-device spike)

Each remaining op gets the same treatment as `FillRect.m`:
```objc
#if TARGET_OS_WATCH
-(void)execute { CN1CG<Primitive>(...); }
#elif defined(USE_ES2)
   ... existing ES2/Metal ...
#else
   ... existing ES1 ...
#endif
```

Remaining ops and their `CN1CG*` target:

| Op | Backend call | Notes |
|----|--------------|-------|
| `FillPolygon` | `CN1CGFillPolygon` | guard the direct `<OpenGLES/*>` imports in the .m |
| `DrawString` | `CN1CGDrawString` | header imports OpenGLES + UIKit — guard them |
| `DrawImage` | `CN1CGDrawImage` | use `[img getImage].CGImage`; guard GL headers in .h |
| `TileImage` | `CN1CGTileImage` | as DrawImage |
| `DrawGradient` | `CN1CGGradientRect` | guard GL headers in .h |
| `Scale` / `Rotate` | `CN1CGScale` / `CN1CGRotate` | Scale.m imports ClipRect.h (see below) |
| `SetTransform` | `CN1CGSetAffine` | header interface is inside `#ifdef USE_ES2`; add a watch interface |
| `ResetAffine` | `CN1CGResetAffine` | |
| `ClipRect` | `CN1CGSetClipRect` / `CN1CGSetClipPolygon` | has `GLuint` ivars — guard |
| `DrawPath` | (tessellate → `CN1CGFillPolygon`/path) | uses `Renderer*`; needs CG path build |
| `DrawTextureAlphaMask` | `CN1CGDrawImage` of the mask | Metal/GL only today |
| `DrawMultiStopGradient` | extend `CN1CGGradientRect` | entirely inside `#ifdef CN1_USE_METAL` |
| `RadialGradientPaint` | paint-state; map to gradient | |

Shared headers needing `#if TARGET_OS_WATCH` / `#else` guards so the watch slice
compiles (they pull `<OpenGLES/*>` / `GLuint` / GLKit):
`GLUIImage.h` (keep the `UIImage` ivar + `getImage`; drop GL texture members),
`ClipRect.h`, `SetTransform.h`, `Rotate.h`, `RadialGradientPaint.h`,
`DrawImage.h`, `TileImage.h`, `DrawString.h`, `DrawGradient.h`,
`DrawTextureAlphaMask.h`, `DrawPath.h`/`Renderer.h`.

Files **excluded** from the watch slice via
`EXCLUDED_SOURCE_FILE_NAMES[sdk=watchos*]` (GL/Metal-only, no watch substitute) —
see `WatchNativeBuilder.applyXcodeSettings`:
`EAGLView.m`, `METALView.m`, `CN1ES1compat.m`, `CN1ES2compat.m`, `CN1GL3D.m`,
`CN1Metalcompat.m`, `CN1MetalGlyphAtlas.m`, `CN1MetalPipelineCache.m`,
`DrawGradientTextureCache.m`, `DrawStringTextureCache.m`,
`CodenameOne_GLViewController.xib`, `CodenameOne_GLSceneDelegate.m`.

## Bootstrap (Phase 3)

`CodenameOne_GLAppDelegate.m` / `CodenameOne_GLViewController.m` instantiate
`CN1WatchRenderingView` instead of `EAGLView`/`METALView` and replace the
`CADisplayLink` pump with a timer (see `CN1WatchHost`). The watch host
(`CN1WatchHost.{h,m}`, SwiftUI/SpriteKit surface) owns the run loop and feeds
Digital-Crown + tap input into the CN1 pointer/scroll event path.

> Why staged: the per-op edits and header guards cannot be compile-verified
> without the watchOS SDK + Xcode. The Phase 0 spike (ParparVM on `arm64_32`,
> one CG frame on-device) must validate the toolchain before the full rollout is
> worth committing. The foundation above is what the spike exercises.
