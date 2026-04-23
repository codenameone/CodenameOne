# iOS Metal Rendering Port — Status

Branch: `metal-ios-backend`

Full architectural plan: `/Users/shai/.claude/plans/currently-the-ios-port-zany-aho.md`

## Goal

Add a Metal-based rendering backend to the iOS port, gated by `#ifdef CN1_USE_METAL` (build flag `-Dios.metal=true`). OpenGL ES 2 remains the default. Both screen rendering and mutable-image rendering will converge onto Metal; text rendering moves to a CoreText glyph atlas.

## Phase progress

| Phase | Scope | Status |
|-------|-------|--------|
| 0 | Unblock the Metal stub; scaffolding + compile | **in progress** |
| 1 | `CN1Metalcompat` + MVP ops (`FillRect`, `DrawImage`, `ClipRect`, `SetTransform`, `ClearRect`) | not started |
| 2 | Remaining `ExecutableOp`s + parity text | not started |
| 3 | Unify mutable image rendering onto Metal | not started |
| 4 | CoreText glyph atlas | not started |
| 5 | Harden (colour space, drawable throttling, memory, lifecycle) | not started |

## Phase 0 — Scaffolding (in progress)

### Complete

- `CN1RenderingView.h` — new protocol declaring the shared method surface implemented by both `EAGLView` (GL) and `METALView` (Metal): `setFramebuffer`, `presentFramebuffer`, `deleteFramebuffer`, `updateFrameBufferSize:h:`, `addPeerComponent:`, keyboard/text callbacks. Lets `CodenameOne_GLViewController` drive either backend through duck typing.
- `MainWindowMETAL.xib` and `CodenameOne_METALViewController.xib` — the two files `IPhoneBuilder.java:698–699` copies into place on `-Dios.metal=true`. The Metal xib instantiates `METALView` as the view with `CodenameOne_GLViewController` as its custom class (so we reuse the existing controller — no parallel god-object).
- `METALView.h` — adopted `CN1RenderingView`; imported `@import Metal` / `@import simd` / `<QuartzCore/CAMetalLayer.h>`; removed stray GL ivars; added `simd_float4x4 projectionMatrix` + readonly accessors for framebuffer size and projection matrix; corrected Metal property types to use `id<MTLFoo>` protocols (MTLDrawable → `id<CAMetalDrawable>`).
- `METALView.m` — fixed the broken `[self.renderCommandEncoder ]` → `[self.renderCommandEncoder endEncoding]`; removed GL holdover calls in `setFramebuffer` (`_glMatrixMode`, `_glLoadIdentity`, `_glOrthof`); corrected Swift-style Metal method names to Objective-C (`makeCommandQueue` → `newCommandQueue`, `makeCommandBuffer` → `commandBuffer`, `makeRenderCommandEncoderWithDescriptor:` → `renderCommandEncoderWithDescriptor:`); implemented `updateFrameBufferSize:h:` with a Y-down orthographic projection (avoiding the GL path's `_glScalef(1,-1,1)+_glTranslatef(0,-h,0)` workaround in `drawFrame`); rewrote `setFramebuffer`/`presentFramebuffer` to handle `nextDrawable` returning nil (drop the frame — never block).
- `EAGLView.h` — adopted `CN1RenderingView`; removed stale `removePeerComponent:` declaration (never implemented, never called).
- `CodenameOne_GLViewController.m` — added conditional `#import "METALView.h"`; the `eaglView` accessor now finds `METALView` under `CN1_USE_METAL` (still typed as `EAGLView*`, duck-typed through the protocol); the one EAGLView-only call (`setContext:`) guarded by `#ifndef CN1_USE_METAL`.

### Pending Phase 0 validation

- **Build the iOS port and a hellocodenameone project with `-Dios.metal=true`.** The native compile should succeed with the scaffolding above.
- **Run on iOS simulator via `scripts/run-ios-ui-tests.sh`.** Phase 0 milestone: app launches, shows a cleared (black) `CAMetalLayer`, no crash, peer `UIView`s visible. Most screenshots will fail (no ops are rendered through Metal yet) — that is expected.

## Key architectural decisions

1. **Single controller class.** The Metal xib reuses `CodenameOne_GLViewController` with a `METALView` inside it. The 2300-line god-object is not forked.
2. **Protocol-based duck typing.** `CN1RenderingView` declares the shared method surface. `[self eaglView]` still returns `EAGLView*` as the declared type for ABI stability; the cast is duck-typed. EAGLView-only call sites (like `setContext:`) are guarded.
3. **One ifdef seam.** `#ifdef CN1_USE_METAL` in `CN1ES2compat.h` (Phase 1) will flip the macro surface from GL to Metal for all 20+ `ExecutableOp` subclasses, so those files need near-zero changes.
4. **Mutable images on Metal.** Phase 3 unifies screen + mutable image rendering with deferred command buffer commit + eager flush on pixel read. CoreGraphics is removed on the Metal build.
5. **Text.** Phase 4 replaces whole-string texture caching with a CoreText-driven R8 glyph atlas (per font+size key, shelf-packed).
6. **Peer components unchanged.** `CAMetalLayer` is a `CALayer` subclass; the existing `peerComponentsLayer` re-parenting dance works byte-for-byte.

## Out of scope (per approved plan)

- Removing the OpenGL path (GL remains default).
- Porting dead `CN1ES1compat.m`.
- Metal 3-only features, MSAA/depth, runtime MSL compilation.
- Replacing `ExecutableOp` with a new command protocol.
- Redesigning peer-component compositing.
