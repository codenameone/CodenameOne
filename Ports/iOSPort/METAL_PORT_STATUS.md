# iOS Metal Rendering Port — Status

Branch: `metal-ios-backend`

Full architectural plan: `/Users/shai/.claude/plans/currently-the-ios-port-zany-aho.md`

## Goal

Add a Metal-based rendering backend to the iOS port, gated by `#ifdef CN1_USE_METAL` (build flag `-Dcodename1.arg.ios.metal=true`). OpenGL ES 2 remains the default. Both screen rendering and mutable-image rendering will converge onto Metal; text rendering moves to a CoreText glyph atlas.

## Phase progress

| Phase | Scope | Status |
|-------|-------|--------|
| 0 | Unblock the Metal stub; scaffolding + compile | **complete** |
| 1 | `CN1Metalcompat` + MVP ops (`FillRect`, `DrawImage`, `ClipRect`, `SetTransform`, `ClearRect`) | **complete (MVP)** |
| 2 | Remaining `ExecutableOp`s + parity text + coordinate-system calibration | **in progress — Form bg renders** |
| 3 | Unify mutable image rendering onto Metal | not started |
| 4 | CoreText glyph atlas | not started |
| 5 | Harden (colour space, drawable throttling, memory, lifecycle) | not started |

## Phase 2 — Coord fixes, persistent render target, more ops (in progress)

### Landed

- Ortho projection Y flip: now maps input y=0 to NDC y=+1, matching UIKit's Y-down coordinate system.
- Framebuffer dimensions always computed from `self.bounds * contentScaleFactor`; ignores the logical-point values `CodenameOne_GLViewController` passes to `updateFrameBufferSize:h:` (the GL path re-reads pixel dims from the renderbuffer).
- `layoutSubviews` now drives `updateFrameBufferSize` so the drawable gets resized when the view reaches its runtime size (xib has a 320x460 placeholder).
- Persistent offscreen `screenTexture` with `MTLLoadActionLoad` — CN1 only queues diff ops per frame and the OpenGL path relies on the renderbuffer persisting; Metal drawables are ephemeral, so we render into this reusable texture and blit it to the drawable at present time.
- `setFramebuffer` is now idempotent (reuse the existing encoder instead of discarding it). Fixes the issue where multiple `drawFrame` invocations per visible frame caused later calls to throw away earlier ones' ops.
- Drawable only acquired at present time — minimises dwell and avoids `nextDrawable` stalls.
- Ported `DrawLine`, `DrawRect`, `FillPolygon`, and `Scale` to Metal. `Rotate` and `ResetAffine` were already working via `SetTransform`.

### Screenshot baselines

Metal-specific golden images live in `scripts/ios/screenshots-metal/` (started as copies of `scripts/ios/screenshots/`). The `build-ios-metal` CI job compares the Metal-backed `scripts/hellocodenameone` output against that directory via `run-ios-ui-tests.sh`'s `SCREENSHOT_REF_DIR` env-var override. Rationale: pixel parity with the GL pipeline is **not** a goal — CoreText glyph positioning, gradient sampling, and other Metal-vs-GL differences are expected to drift. Tracking Metal's own baseline lets us accept intentional changes without regressing the GL validation. See `scripts/ios/screenshots-metal/README.md` for the update workflow.

**Current baseline status** (refreshed `cee97d7fe` → partially refreshed `368b9c088`): 37 PNGs from the Metal pipeline post-DrawString work. Four GL-era orphans deleted because the Metal test run no longer captures them.

**Important: what the 35/37-matched stat does *not* tell us.** The `scripts/ios/screenshots-metal/` baselines were copied from the Metal pipeline's own CI output, so "35/37 matches the Metal baseline" only says the pipeline produces the same output twice — it is a determinism metric, *not* a correctness metric. Likewise, the GL reference set `scripts/ios/screenshots/` is the current GL-pipeline behaviour, not a known-correct oracle, so Metal-vs-GL diffs say "these implementations render differently" rather than "Metal is wrong." There is no pixel-accurate ground truth for this work; visual inspection is the only reliable correctness signal right now.

**Mutable-image Y-flip — fixed.** Earlier in this branch, content drawn through `Image.createImage(w,h).getGraphics()` displayed upside-down when composited back to the screen because `CN1MetalTextureFromUIImage` rasterised without a CTM flip while `CN1MetalDrawImage` used inverted-Y texcoords — a pair that worked for disk-loaded UIImages but produced upside-down output for `UIGraphicsGetImageFromCurrentImageContext`-derived ones. `AbstractGraphicsScreenshotTest`-based tests (the entire `graphics-*` suite) were over-reporting drift by ~16 percentage points because of this. Both halves are now patched in `CN1Metalcompat.m`. (Phase 3 will remove the whole CG-backed mutable-image round-trip anyway, making this fix obsolete.)

**Determinism findings across two consecutive CI runs.** Two tests drift run-to-run with *different* drift shapes each time (which rules out a systematic Metal rendering issue — 35 other tests are run-to-run stable). Both are test-level flakes, not Metal rendering problems:

- `landscape` (`OrientationLockScreenshotTest`) — captures after `waitForOrientation` returns and an extra 50 ms wait. The rotation transition timing on the iOS simulator is non-deterministic, and 50 ms is not consistently enough for layout + paint to settle. Drift seen: run A = 11.6% (col bands 7+8), run B = 3.3% (col band 8 only). Fix at the test level: poll a layout-stable condition instead of a fixed delay.
- `graphics-fill-round-rect` (`FillRoundRect`) — draws `bounds.getWidth()/2` round rects with `nextColor(g)` darkening between each call. A one-pixel difference in `bounds.getWidth()` shifts every subsequent colour, producing a different rendered result across the whole draw. Compounded by 2/4 quadrants going through the mutable-image path (see Y-flip bug above), which adds its own run-to-run noise. Drift seen: run A = 2.3% (right edge only), run B = 63% (full width). Fix at the test level: fixed iteration count, or seed `nextColor` deterministically.

**Baseline integrity note:** PNGs copied out of the workflow artifact can come through with truncated-CRC chunks that `file`/`sips` accept but Pillow's strict reader (used by `run-ios-ui-tests.sh`) rejects as "PNG chunk truncated before CRC". Happened once on `graphics-transform-rotation`. When refreshing baselines, prefer files downloaded via `gh run download`.

### Landed (continued)

- **ClipRect.** Enabled against a physical-pixel scissor rect. The tricky part turned out to be not ClipRect itself but `updateFrameBufferSize:h:` — when `layoutSubviews` updated the real view bounds (402×874 at 3x) mid-frame, any in-flight encoder kept referencing the xib-time 960×1380 screenTexture and the cached `currentFramebufferWidth/Height` in `CN1Metalcompat` stayed stale for the whole frame. Now `updateFrameBufferSize:h:` tears down any live encoder + command buffer before rebuilding the texture so the next `setFramebuffer` captures fresh dimensions.
- **DrawString (parity level).** Whole-string texture approach — rasterise via `CGBitmapContext` + UIKit `drawAtPoint:withAttributes:`, upload as RGBA `MTLTexture`, render through the existing `TexturedRGBA` pipeline with alpha modulation. Simple round-robin cache capped at 128 entries keyed on `"str|font|color"`. CTM flipped before UIKit draws so text lands at the top-left of the bitmap buffer (Metal's V=0-at-top convention, vs OpenGL which tolerated unflipped CG because its tex coords use V=1-at-top). CoreText glyph atlas (Phase 4) will replace this.
- **Mutable-image Y-flip fix.** `CN1MetalTextureFromUIImage` now applies the same CTM flip as `GLUIImage.getTexture` and the text-cache path before `CGContextDrawImage`, putting display-row-0 at memory-row-0. `CN1MetalDrawImage` switched from inverted-Y to non-inverted texcoords. All texture producers in `CN1Metalcompat.m` now share a single orientation convention; mutable images (`Image.createImage().getGraphics()` → `UIGraphicsGetImageFromCurrentImageContext`) and disk-loaded `UIImage`s both render right-side-up.
- **DrawGradient (linear + radial via texture rasterisation).** Direct port of the GL approach: rasterise via `CGContextDrawLinearGradient` / `CGContextDrawRadialGradient` into a `CGBitmapContext`, upload as `MTLTexture`, render through the existing `TexturedRGBA` pipeline. No gradient shaders needed — matches GL pixel-for-pixel modulo upload format. 32-entry round-robin cache keyed on `(type, startColor, endColor, w, h, relX, relY, relSize)`. Handles `RADIAL` / `HORIZONTAL` / `VERTICAL` types. The `LinearGradient` / `RadialGradient` placeholders in `CN1MetalShaders.metal` remain reserved (unused) — keeping them for a future "rasterise on GPU" optimisation, but it's not in the critical path.
- **TileImage.** Loops over the destination rect, emits one textured quad per tile via `drawQuad`, clips texcoords on right/bottom partial tiles. The GL path batches all tiles into one `glDrawArrays`; Metal version trades a tiny amount of per-frame overhead for cleaner code. Real-world tile counts (Form-sized backgrounds) are tens, not thousands.
- **GLUIImage `MTLTexture` cache.** `getMTLTexture` lazily builds and caches an `MTLTexture` on the `GLUIImage` instance. `setImage:` invalidates. `DrawImage` and `TileImage` now hit the cache instead of re-rasterising the UIImage on every execute.

### Known issues / follow-ups

- **`RadialGradientPaint`.** This op is a paint-state setter (it stashes gradient params for `PaintOp.current`), not a renderer. The actual gradient rendering for paint-bound ops happens later via shape-aware draws (`DrawShape`/`FillShape`) which look at `PaintOp.current`. Porting it cleanly needs `PaintOp` + the shape ops on Metal first.
- **Path rendering (`DrawPath`, `DrawTextureAlphaMask`).** Paths are rasterised through `Renderer.c` to a pixel buffer; needs either a direct Metal port or an alpha-mask texture upload through the existing `AlphaMask` pipeline.
- **Stencil clipping for non-rectangular clips.** Currently falls back to a bounding-box scissor — Form layout handles that OK but paths/textures-as-masks will clip incorrectly.
- **Two flaky screenshot tests** (`landscape`, `graphics-fill-round-rect`). See "Current baseline status" above — test-level non-determinism, not Metal rendering problems.

## Phase 0 — Scaffolding (complete)

- `CN1RenderingView.h` — shared protocol both `EAGLView` and `METALView` conform to.
- `MainWindowMETAL.xib` + `CodenameOne_METALViewController.xib` — the two files `IPhoneBuilder.java:698–699` copies into place on `-Dcodename1.arg.ios.metal=true`. The Metal xib instantiates `METALView` with `CodenameOne_GLViewController` as its custom class — the existing god-object controller is reused, not forked.
- `METALView.{h,m}` — fixed the syntax error, removed GL holdovers, implemented Y-down orthographic projection, corrected Swift-style method names to Objective-C, handle `nextDrawable` returning nil, make `setFramebuffer` idempotent (awakeFromNib calls it once with no matching `presentFramebuffer`).
- `CodenameOne_GLViewController.m` — `eaglView` accessor finds `METALView` under `CN1_USE_METAL`; `setContext:` call guarded.
- `CN1ES2compat.h` — added the `//#define CN1_USE_METAL` comment that `IPhoneBuilder.java:697` uncomments at build time.
- Fixed `METALView.h`/`METALView.m` to import `CN1ES2compat.h` **before** their `#ifdef CN1_USE_METAL` check so the macro is visible (otherwise the whole file evaluated to empty and the linker complained about `_OBJC_CLASS_$_METALView`).

**Validation:** `scripts/hellocodenameone` built for iphonesimulator under `-Dcodename1.arg.ios.metal=true`, installed and launched on iPhone 17 Pro simulator. App launches cleanly, no crash, CN1SS screenshot tests run to completion. Screenshots are blank because no op has been ported yet — that's Phase 1.

## Phase 1 — Metal MVP (in progress)

### Complete

- `CN1Metalcompat.{h,m}` — higher-level C API for Metal draws. Holds the active `MTLRenderCommandEncoder`, projection/modelView/transform matrices, and scissor state. Exposes:
  - Encoder lifecycle: `CN1MetalBeginFrame` / `CN1MetalEndFrame` / `CN1MetalActiveEncoder`.
  - Matrix state: `CN1MetalSetTransform` / `CN1MetalGetTransform` / `CN1MetalLoadIdentity` / `CN1MetalPushMatrix` / `CN1MetalPopMatrix` / `CN1MetalScale` / `CN1MetalTranslate` / `CN1MetalRotate`.
  - Clip state: `CN1MetalSetScissor`.
  - Draw primitives: `CN1MetalFillRect` / `CN1MetalClearRect` / `CN1MetalDrawImage`.
  - Texture helpers: `CN1MetalTextureFromUIImage` (Phase 1; Phase 1.5 will cache the MTLTexture on `GLUIImage`).
- `CN1MetalShaders.metal` — MSL vertex + fragment shaders for: SolidColor, TexturedRGBA, AlphaMask, ClearPunch pipelines. LinearGradient / RadialGradient reserved for Phase 2.
- `CN1MetalPipelineCache.{h,m}` — lazy-builds one `MTLRenderPipelineState` per pipeline variant on first use, keyed by the `CN1MetalPipeline` enum. Premultiplied-alpha blending (matching GL path); `ClearPunch` has blending disabled.
- `METALView.m` — `setFramebuffer` publishes the encoder + projection to `CN1MetalBeginFrame`; `presentFramebuffer` calls `CN1MetalEndFrame` before `endEncoding`. Back-to-back-setFramebuffer safety path also calls `CN1MetalEndFrame` on the abandoned encoder.
- `ByteCodeTranslator.java` — registers `.metal` as `sourcecode.metal` in the generated `project.pbxproj` and includes it in the Sources build phase so Xcode builds `default.metallib` from our shader source.
- Op ports: `FillRect.m`, `ClearRect.m`, `ClipRect.m` (rectangular-only, stencil clip TBD Phase 2), `SetTransform.m`, `DrawImage.m` — each with a one-line `#ifdef CN1_USE_METAL` branch at the top of `execute` that calls the new API, followed by `return`.

### Validated (MVP)

- **xcodebuild end-to-end success** under `-Dcodename1.arg.ios.metal=true` on iOS 26 SDK (Xcode 26.3). Shader compiles into `default.metallib` and is embedded in the app bundle.
- **Runtime smoke test** — a forced `CN1MetalFillRect(0xFF0000, 0xFF, 100, 100, 400, 400)` at the tail of every frame produces a visible red rectangle on the iPhone 17 Pro simulator (screenshot archived). `drawQuad` trace confirmed 841+ draw calls per 6-second window with non-nil `MTLRenderCommandEncoder` and resolved `MTLRenderPipelineState`. No crashes, no Metal validation assertions, no `nextDrawable` stalls.
- **Form background renders.** The hellocodenameone test runner's Form displays its grey background through the Metal pipeline (not through UIKit fallback) — confirmed by clearColor=black contrast: we see grey, not black.

### Known follow-ups for Phase 2

- **Coordinate-system calibration.** The red rect from the smoke test landed lower and larger than expected, suggesting a logical-vs-physical pixel mismatch in the projection. The Metal `framebufferWidth`/`framebufferHeight` are in physical pixels but CodenameOne's `drawFrame` may be passing op coordinates in logical points. Needs a pass to align with the GL path's convention (probably `_glScalef(scaleValue, scaleValue, 1)` equivalent or adjust projection to logical-point extent).
- **Metal Toolchain dependency.** Xcode 26.3 on iOS 26 SDK requires `xcodebuild -downloadComponent MetalToolchain` as a one-time setup, else xcodebuild fails at `CompileMetalFile` with *"cannot execute tool 'metal' due to missing Metal Toolchain"*. Documented in the verification flow below.
- **GLUIImage Metal texture caching.** Currently `CN1MetalDrawImage` re-rasterises the UIImage on every draw. Add an `MTLTexture` cache on `GLUIImage` so repeated draws reuse the texture.
- **Header include pattern for new Metal sources.** Any new file that checks `#ifdef CN1_USE_METAL` MUST `#import "CN1ES2compat.h"` before the ifdef, otherwise the preprocessor treats the whole file as empty and the linker reports undefined symbols (a bug we hit twice during Phase 0/1).

## Phase 1 verification flow

```bash
# From the repo root; this refers to the JDK paths actually available on the dev box.
export JAVA_HOME=/Users/shai/Library/Java/JavaVirtualMachines/azul-1.8.0_372/Contents/Home
export JAVA17_HOME=/Users/shai/Library/Java/JavaVirtualMachines/jbr-17.0.7/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# 1. Build the iOS port + plugin into local maven
cd maven
mvn install -Plocal-dev-javase -DskipTests -pl parparvm,ios,codenameone-maven-plugin -am -q

# 2. One-time: install the Metal toolchain (Xcode 26.3+)
xcodebuild -downloadComponent MetalToolchain

# 3. Temporarily set codename1.arg.ios.metal=true in hellocodenameone's
#    common/codenameone_settings.properties, then regenerate.
#    (Maven -Dcodename1.arg.ios.metal=true on the CLI is NOT picked up by
#    CN1BuildMojo; only properties from codenameone_settings.properties flow
#    through.)
cd ../scripts/hellocodenameone
export JAVA_HOME=$JAVA17_HOME
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw package -DskipTests -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source \
    -Dcn1.version=8.0-SNAPSHOT -Dcn1.plugin.version=8.0-SNAPSHOT -U

# 4. xcodebuild for simulator
cd ios/target/hellocodenameone-ios-1.0-SNAPSHOT-ios-source
xcodebuild -project HelloCodenameOne.xcodeproj -scheme HelloCodenameOne \
    -configuration Debug -sdk iphonesimulator \
    -destination "platform=iOS Simulator,OS=latest,name=iPhone 17 Pro" \
    -derivedDataPath /tmp/hcn_metal_derived build

# 5. Install and launch
xcrun simctl boot "iPhone 17 Pro" 2>/dev/null || true
APP=$(find /tmp/hcn_metal_derived -name "HelloCodenameOne.app" -type d | head -1)
xcrun simctl install booted "$APP"
xcrun simctl launch --console-pty booted com.codenameone.examples.hellocodenameone
```

## Key architectural decisions

1. **Single controller class.** The Metal xib reuses `CodenameOne_GLViewController` with a `METALView` inside it. The 2300-line god-object is not forked.
2. **Protocol-based duck typing.** `CN1RenderingView` declares the shared method surface. `[self eaglView]` still returns `EAGLView*` as the declared type for ABI stability; the cast is duck-typed.
3. **Higher-level C API seam.** The existing GL ops call raw GL (not the `_gl*` macros), so a per-op `#ifdef CN1_USE_METAL` branch at the top of `execute` calls `CN1MetalFillRect` / `CN1MetalDrawImage` / etc. directly. Concentrates all Metal logic in `CN1Metalcompat.m` while keeping per-op changes minimal.
4. **Offline-compiled shaders.** `CN1MetalShaders.metal` compiled by Xcode into `default.metallib`; loaded at runtime via `[device newDefaultLibrary]` by the pipeline cache.
5. **Mutable images on Metal.** Phase 3 unifies screen + mutable image rendering with deferred command buffer commit + eager flush on pixel read. CoreGraphics is removed on the Metal build.
6. **Text.** Phase 4 replaces whole-string texture caching with a CoreText-driven R8 glyph atlas.
7. **Peer components unchanged.** `CAMetalLayer` is a `CALayer` subclass; the existing `peerComponentsLayer` re-parenting dance works byte-for-byte.

## Out of scope (per approved plan)

- Removing the OpenGL path (GL remains default).
- Porting dead `CN1ES1compat.m`.
- Metal 3-only features, MSAA/depth, runtime MSL compilation.
- Replacing `ExecutableOp` with a new command protocol.
- Redesigning peer-component compositing.
