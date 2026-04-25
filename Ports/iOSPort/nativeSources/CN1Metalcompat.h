/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
#ifndef CN1Metalcompat_h
#define CN1Metalcompat_h

#import "CN1ES2compat.h"
#ifdef CN1_USE_METAL

@import Metal;
@import simd;
#import <UIKit/UIKit.h>
#import <GLKit/GLKit.h>

// Metal rendering backend for Codename One iOS.
//
// The existing OpenGL ES 2 ExecutableOp implementations call raw GL functions
// (glUseProgram, glUniformMatrix4fv, glDrawArrays). They cannot transparently
// run on Metal, so each op gets an #ifdef CN1_USE_METAL branch in its execute
// method that calls the higher-level C API declared here.
//
// State lives in CN1Metalcompat.m:
//   - active render command encoder (set by METALView.setFramebuffer,
//     cleared by presentFramebuffer)
//   - projection + modelView + transform matrices
//   - current clip rect / scissor
//   - pipeline state cache (lazy built, keyed by pipeline variant + blend)
//
// Matrices use the same GLKMatrix4 type as the GL path so SetTransform.m can
// pass its matrix through unchanged; CN1Metalcompat converts to simd_float4x4
// at upload time.

// -------- Pipeline variants --------
typedef NS_ENUM(NSInteger, CN1MetalPipeline) {
    CN1MetalPipelineSolidColor = 0,    // FillRect, DrawLine, FillPolygon
    CN1MetalPipelineTexturedRGBA,      // DrawImage, TileImage
    CN1MetalPipelineAlphaMask,         // DrawString glyph, DrawTextureAlphaMask
    CN1MetalPipelineClearPunch,        // ClearRect: write zeros, no blend
    CN1MetalPipelineLinearGradient,    // Phase 2
    CN1MetalPipelineRadialGradient,    // Phase 2
    CN1MetalPipelineCount
};

// -------- Uniform struct matching CN1MetalShaders.metal --------
// Vertex stage receives this struct at buffer index 1.
typedef struct {
    simd_float4x4 projection;
    simd_float4x4 modelView;
    simd_float4x4 transform;
} CN1MetalMatrices;

// -------- Encoder lifecycle (called by CodenameOne_GLViewController / METALView) --------

// Called by METALView.setFramebuffer after acquiring a command encoder for
// the screen drawable. Captures the encoder so ops can issue draw calls
// against it. Also captures the projection matrix for the current drawable
// size. A nil encoder is valid and means "frame dropped" — ops must no-op.
void CN1MetalBeginFrame(id<MTLRenderCommandEncoder> encoder,
                        simd_float4x4 projection,
                        int framebufferWidth,
                        int framebufferHeight);

// Called by METALView.presentFramebuffer just before commit. Clears the
// active encoder reference so subsequent ops no-op until the next frame.
void CN1MetalEndFrame(void);

// Returns the active encoder or nil if no frame is in flight. Ops use this
// to skip drawing when setFramebuffer couldn't acquire a drawable.
id<MTLRenderCommandEncoder> CN1MetalActiveEncoder(void);

// Access to the framebuffer dimensions for the current encoder.
int CN1MetalFramebufferWidth(void);
int CN1MetalFramebufferHeight(void);

// -------- Matrix state (mirrors CN1modelViewMatrix / CN1projectionMatrix / CN1transformMatrix in the GL path) --------

// Called by SetTransform.execute. Replaces the current transform matrix.
void CN1MetalSetTransform(GLKMatrix4 transform);

// Returns the current transform matrix (for SetTransform.currentTransform).
GLKMatrix4 CN1MetalGetTransform(void);

// Matrix stack operations used by the drawFrame flip workaround, Rotate,
// Scale, ResetAffine. (Phase 2.)
void CN1MetalPushMatrix(void);
void CN1MetalPopMatrix(void);
void CN1MetalScale(float x, float y, float z);
void CN1MetalTranslate(float x, float y, float z);
void CN1MetalRotate(float angle, float x, float y, float z);
void CN1MetalLoadIdentity(void);

// -------- Clip state (mirrors ClipRect.m) --------

// Set a scissor rect in framebuffer pixel coordinates (Y-down, matching
// our projection). Passing width<=0 or height<=0 disables clipping.
void CN1MetalSetScissor(int x, int y, int width, int height);

// -------- Draw primitives (invoked from ExecutableOp subclasses' execute methods) --------

// Fill a rectangle with a solid color + alpha (0-255 each). x/y/w/h in
// iOS Y-down framebuffer coordinates (matches the GL path's semantic after
// its drawFrame flip).
void CN1MetalFillRect(int color, int alpha, int x, int y, int width, int height);

// Same geometry but punches zero into color+alpha with no blending —
// equivalent to the ClearRect fragment shader writing vec4(0,0,0,0).
void CN1MetalClearRect(int x, int y, int width, int height);

// Draw a 1-pixel line from (x1,y1) to (x2,y2) with the given color+alpha.
// Note: Metal does not support line width > 1; on retina displays lines
// appear thin. Matches the GL path's glLineWidth=1 default.
void CN1MetalDrawLine(int color, int alpha, int x1, int y1, int x2, int y2);

// Draw a rectangular outline (not filled) at (x,y,w,h). Rendered as a
// closed 4-segment line strip.
void CN1MetalDrawRect(int color, int alpha, int x, int y, int width, int height);

// Fill a convex polygon given N (x,y) vertex pairs. The polygon is
// triangulated on the CPU as a fan from the first vertex, so it must
// be convex for correct results (matches the GL path's assumption).
void CN1MetalFillPolygon(const float *xCoords, const float *yCoords, int num,
                         int color, int alpha);

// Draw an RGBA image to (x,y,w,h) with a uniform alpha modulator (0-255).
// Texture is owned by the caller (typically a GLUIImage); it is retained
// only for the current command buffer.
void CN1MetalDrawImage(id<MTLTexture> texture, int alpha, int x, int y, int width, int height);

// Tile an RGBA image across (x,y,w,h). imageWidth/imageHeight are the
// natural size of the source UIImage. Issues one textured quad per
// tile (full or clipped at the right/bottom edges); a future batched
// version could pack into a single draw call.
void CN1MetalTileImage(id<MTLTexture> texture, int alpha,
                       int x, int y, int width, int height,
                       int imageWidth, int imageHeight);

// Draw a string at (x,y). The string is rasterised via CoreGraphics into
// an RGBA MTLTexture with the colour baked in (matching the GL path's
// approach) and then rendered as a textured quad with alpha modulation.
// A small LRU cache keyed on (str, font, color) avoids re-rasterising per
// frame. Phase 4 will replace this with a CoreText glyph atlas.
void CN1MetalDrawString(NSString *str, UIFont *font, int color, int alpha, int x, int y);

// Build an MTLTexture from a single-channel (R8/alpha-only) bitmap. The
// alpha bytes are produced by Renderer.c (Renderer_produceAlphas) when
// rasterising a path; the resulting texture is sampled by the AlphaMask
// pipeline. Caller bridge-retains the returned id<MTLTexture> through
// JAVA_LONG so the same handle can flow through the existing TextureAlphaMask
// path on the Java side. Returns nil on failure.
id<MTLTexture> CN1MetalCreateAlphaMaskTexture(const uint8_t *bytes, int width, int height);

// Render an alpha-mask texture (R8 or A8) at (x,y,w,h) tinted by the given
// premultiplied color+alpha. Wrapper for the AlphaMask pipeline; equivalent
// to the GL DrawTextureAlphaMask basic shader path. Used by fillArc /
// fillShape / drawShape after Renderer.c rasterises the path.
void CN1MetalDrawAlphaMask(id<MTLTexture> texture, int color, int alpha,
                           int x, int y, int width, int height);

// Draw a linear or radial gradient filling (x,y,w,h). type is one of
// GRADIENT_TYPE_HORIZONTAL / GRADIENT_TYPE_VERTICAL / GRADIENT_TYPE_RADIAL
// (defined in DrawGradient.h). startColor/endColor are 0xAARRGGBB. The
// gradient is rasterised via CGContextDrawLinearGradient or
// CGContextDrawRadialGradient (matching the GL path's DrawGradient.m and
// RadialGradientPaint.m exactly), uploaded as an MTLTexture, cached on
// (type,start,end,w,h,relX,relY,relSize), and rendered as a textured
// quad through the existing TexturedRGBA pipeline. relativeX/Y/Size are
// only used for radial gradients.
void CN1MetalDrawGradient(int type, int startColor, int endColor,
                          int x, int y, int width, int height,
                          float relativeX, float relativeY, float relativeSize);

// -------- Texture helpers for GLUIImage --------

// Lazily build an MTLTexture from a UIImage. Cached on the GLUIImage.
// Returns nil on failure. Caller does not own the texture.
id<MTLTexture> CN1MetalTextureFromUIImage(UIImage *image);

// Global Metal device (from METALView's command queue); shared by anyone
// who needs to allocate Metal resources.
id<MTLDevice> CN1MetalDevice(void);

// Global Metal command queue (from METALView). Mutable-image command buffers
// allocate from this queue so they share scheduling with screen drawing.
id<MTLCommandQueue> CN1MetalCommandQueue(void);

// -------- Phase 3: unified mutable-image rendering --------
//
// On the Metal build, mutable images (`Image.createImage(w,h).getGraphics()`)
// no longer round-trip through CoreGraphics. Each mutable image owns a
// dedicated MTLTexture that the CN1 op queue draws into directly, sharing
// the same encoder/draw primitives as the screen path. The CG-backed
// shim in CodenameOne_GLViewController.m's nativeXxxMutableImpl functions
// remains untouched for the GL build.
//
// Lifecycle:
//   1. Java calls Image.getGraphics() -> JNI startDrawingOnImageImpl ->
//      CN1MetalBeginMutableImageDraw(width, height, peer). This allocates
//      (or reuses) an MTLTexture for the image, opens a command buffer +
//      render encoder against it, and redirects subsequent draw primitives
//      to the mutable encoder.
//   2. Each draw op (FillRect, DrawImage, ...) goes through the same
//      CN1MetalActiveEncoder() lookup as the screen path. While a mutable
//      draw is active, the active encoder is the mutable one; the screen
//      encoder is preserved and restored in finish.
//   3. Java calls a finishing operation (Graphics is reset / image used)
//      -> JNI finishDrawingOnImageImpl -> CN1MetalFinishMutableImageDraw()
//      ends the mutable encoder. Pixels live on the GPU; the command
//      buffer is **deferred** -- not committed until a readback or
//      cross-image consume forces commit-and-wait.
//   4. Pixel-reading paths (Image.getRGB, PNG/JPEG encode, toImage):
//      CN1MetalReadMutableImagePixels commits the deferred command buffer
//      and waitUntilCompleted before reading.

// Begin a Metal render pass into the mutable image identified by `peer`
// (a GLUIImage*). Allocates the MTLTexture lazily and saves the screen
// encoder if one is active. Subsequent CN1MetalFillRect / DrawImage /
// SetTransform / etc. calls render into this texture instead of the
// screen. Returns YES on success; NO if the device is unavailable or
// allocation fails (caller should fall back to the CG path).
BOOL CN1MetalBeginMutableImageDraw(int width, int height, void *peer);

// End the active mutable-image render pass. Restores the previous screen
// encoder (if there was one) and leaves the mutable image's command
// buffer uncommitted. The buffer is committed lazily on read or when
// reused as a source for a screen draw.
void CN1MetalFinishMutableImageDraw(void *peer);

// Force-commit and wait on a mutable image's pending command buffer (if
// any). Called before any pixel-reading path so the GPU work has
// finished before we sample the texture. Safe to call when no work is
// pending (no-op).
void CN1MetalFlushMutableImage(void *peer);

// Read a region of a mutable image's MTLTexture into an int[] buffer in
// 0xAARRGGBB format. Forces a flush first. Returns YES on success.
BOOL CN1MetalReadMutableImagePixels(void *peer, int *outARGB,
                                    int x, int y, int w, int h,
                                    int imgWidth, int imgHeight);

// Returns YES while a Metal mutable-image draw pass is open between
// CN1MetalBeginMutableImageDraw and CN1MetalFinishMutableImageDraw. Used
// by JNI mutable-image entry points to decide whether to route through
// Metal or fall back to the GL/CG path (the latter is dead on Metal
// builds during this transition but the symbol is still referenced).
BOOL CN1MetalIsMutableActive(void);

// Rasterise an arbitrary CoreGraphics drawing block into a temporary
// CGBitmapContext sized (w x h), upload it as an MTLTexture, and draw
// it through the active encoder at (dx, dy, w, h). Used by mutable-image
// ops that don't yet have native Metal equivalents (fillRoundRect,
// fillArc, drawArc, fillRadialGradient, ...) so they can keep their
// existing CG drawing semantics while bridging into the Metal pipeline.
// The block draws into the temporary context with the origin at (0, 0)
// of an h-tall bitmap; the shim handles upload + dispatch. CTM flip is
// applied so display-row-0 lands at memory-row-0 (matches the
// orientation convention used by every other texture producer in this
// file). Caller is responsible for setting fill / stroke colours on
// the supplied context.
void CN1MetalDrawCGRasterizedRect(int dx, int dy, int w, int h,
                                  void (^cgBlock)(CGContextRef ctx));

#endif /* CN1_USE_METAL */
#endif /* CN1Metalcompat_h */
