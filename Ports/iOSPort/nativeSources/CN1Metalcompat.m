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
#import "CN1ES2compat.h"
#ifdef CN1_USE_METAL
#import "CN1Metalcompat.h"
#import "CN1MetalPipelineCache.h"
#import "CN1MetalGlyphAtlas.h"
#import "METALView.h"
#import "CodenameOne_GLViewController.h"
#import "GLUIImage.h"
#import <CoreText/CoreText.h>

// --------------- Static state ---------------

static __unsafe_unretained id<MTLRenderCommandEncoder> activeEncoder = nil;
static simd_float4x4 currentProjection;
static simd_float4x4 currentModelView;
static simd_float4x4 currentTransform;
static int currentFramebufferWidth = 0;
static int currentFramebufferHeight = 0;
static CN1MetalPipelineCache *pipelineCache = nil;

#define CN1_MATRIX_STACK_DEPTH 32
static simd_float4x4 modelViewStack[CN1_MATRIX_STACK_DEPTH];
static int modelViewStackTop = 0;

static simd_float4x4 identityMatrix(void) {
    return (simd_float4x4){{
        { 1, 0, 0, 0 },
        { 0, 1, 0, 0 },
        { 0, 0, 1, 0 },
        { 0, 0, 0, 1 }
    }};
}

static simd_float4x4 glkToSimd(GLKMatrix4 m) {
    simd_float4x4 r;
    memcpy(&r, m.m, sizeof(float) * 16);
    return r;
}

static GLKMatrix4 simdToGlk(simd_float4x4 m) {
    GLKMatrix4 r;
    memcpy(r.m, &m, sizeof(float) * 16);
    return r;
}

static void ensurePipelineCache(void) {
    if (pipelineCache == nil) {
        pipelineCache = [[CN1MetalPipelineCache alloc] initWithDevice:CN1MetalDevice()];
    }
}

// --------------- Encoder lifecycle ---------------

void CN1MetalBeginFrame(id<MTLRenderCommandEncoder> encoder,
                        simd_float4x4 projection,
                        int framebufferWidth,
                        int framebufferHeight) {
    activeEncoder = encoder;
    currentProjection = projection;
    currentFramebufferWidth = framebufferWidth;
    currentFramebufferHeight = framebufferHeight;
    // modelView is always identity for 2D UI rendering. The GL path uses it
    // only as a y-flip in drawFrame; our ortho projection bakes the flip in.
    currentModelView = identityMatrix();
    if (modelViewStackTop == 0) {
        currentTransform = identityMatrix();
    }
    ensurePipelineCache();
}

void CN1MetalEndFrame(void) {
    activeEncoder = nil;
}

id<MTLRenderCommandEncoder> CN1MetalActiveEncoder(void) {
    return activeEncoder;
}

int CN1MetalFramebufferWidth(void) { return currentFramebufferWidth; }
int CN1MetalFramebufferHeight(void) { return currentFramebufferHeight; }

id<MTLDevice> CN1MetalDevice(void) {
    METALView *mv = (METALView *)[[CodenameOne_GLViewController instance] eaglView];
    return ((CAMetalLayer *)mv.layer).device;
}

id<MTLCommandQueue> CN1MetalCommandQueue(void) {
    METALView *mv = (METALView *)[[CodenameOne_GLViewController instance] eaglView];
    return mv.commandQueue;
}

// --------------- Matrix state ---------------

void CN1MetalSetTransform(GLKMatrix4 transform) {
    currentTransform = glkToSimd(transform);
}

GLKMatrix4 CN1MetalGetTransform(void) {
    return simdToGlk(currentTransform);
}

void CN1MetalLoadIdentity(void) {
    currentModelView = identityMatrix();
}

void CN1MetalPushMatrix(void) {
    if (modelViewStackTop < CN1_MATRIX_STACK_DEPTH) {
        modelViewStack[modelViewStackTop++] = currentModelView;
    }
}

void CN1MetalPopMatrix(void) {
    if (modelViewStackTop > 0) {
        currentModelView = modelViewStack[--modelViewStackTop];
    }
}

void CN1MetalScale(float x, float y, float z) {
    simd_float4x4 s = (simd_float4x4){{
        { x, 0, 0, 0 },
        { 0, y, 0, 0 },
        { 0, 0, z, 0 },
        { 0, 0, 0, 1 }
    }};
    currentModelView = simd_mul(currentModelView, s);
}

void CN1MetalTranslate(float x, float y, float z) {
    simd_float4x4 t = identityMatrix();
    t.columns[3] = (simd_float4){ x, y, z, 1 };
    currentModelView = simd_mul(currentModelView, t);
}

void CN1MetalRotate(float angle, float x, float y, float z) {
    float rad = angle * (float)M_PI / 180.0f;
    float c = cosf(rad);
    float s = sinf(rad);
    float len = sqrtf(x*x + y*y + z*z);
    if (len > 0) { x /= len; y /= len; z /= len; }
    float ic = 1.0f - c;
    simd_float4x4 r = (simd_float4x4){{
        { x*x*ic + c,   y*x*ic + z*s, z*x*ic - y*s, 0 },
        { x*y*ic - z*s, y*y*ic + c,   z*y*ic + x*s, 0 },
        { x*z*ic + y*s, y*z*ic - x*s, z*z*ic + c,   0 },
        { 0, 0, 0, 1 }
    }};
    currentModelView = simd_mul(currentModelView, r);
}

// --------------- Clip state ---------------

void CN1MetalSetScissor(int x, int y, int width, int height) {
    if (activeEncoder == nil) return;
    if (width <= 0 || height <= 0) {
        // Disable clipping: set scissor to full framebuffer.
        [activeEncoder setScissorRect:(MTLScissorRect){
            0, 0,
            (NSUInteger)currentFramebufferWidth,
            (NSUInteger)currentFramebufferHeight
        }];
        return;
    }
    // Clamp to framebuffer; Metal requires scissor to be within the
    // attachment bounds or it fails the render pass.
    int fx = MAX(0, x);
    int fy = MAX(0, y);
    int fw = MIN(width, currentFramebufferWidth - fx);
    int fh = MIN(height, currentFramebufferHeight - fy);
    if (fw <= 0 || fh <= 0) {
        // Clip is entirely outside — set a zero-size scissor to cull everything.
        [activeEncoder setScissorRect:(MTLScissorRect){0, 0, 1, 1}];
        return;
    }
    [activeEncoder setScissorRect:(MTLScissorRect){
        (NSUInteger)fx, (NSUInteger)fy,
        (NSUInteger)fw, (NSUInteger)fh
    }];
}

// --------------- Drawing helpers ---------------

static CN1MetalMatrices currentMatrices(void) {
    CN1MetalMatrices m;
    m.projection = currentProjection;
    m.modelView = currentModelView;
    m.transform = currentTransform;
    return m;
}

static void drawQuad(CN1MetalPipeline pipeline,
                     const float vertices[8],
                     const float *texcoords, // may be NULL
                     simd_float4 color,
                     id<MTLTexture> texture) {
    if (activeEncoder == nil || pipelineCache == nil) return;
    id<MTLRenderPipelineState> state = [pipelineCache pipelineFor:pipeline];
    if (state == nil) return;
    [activeEncoder setRenderPipelineState:state];

    // buffer(0): positions (8 floats = 4 x (x,y))
    [activeEncoder setVertexBytes:vertices length:sizeof(float) * 8 atIndex:0];
    // buffer(1): matrices
    CN1MetalMatrices matrices = currentMatrices();
    [activeEncoder setVertexBytes:&matrices length:sizeof(matrices) atIndex:1];
    // buffer(2): optional texcoords (only textured/alpha-mask pipelines read this)
    if (texcoords != NULL) {
        [activeEncoder setVertexBytes:texcoords length:sizeof(float) * 8 atIndex:2];
    }
    // Fragment buffer(0): color uniform
    [activeEncoder setFragmentBytes:&color length:sizeof(color) atIndex:0];
    // Fragment texture(0): optional
    if (texture != nil) {
        [activeEncoder setFragmentTexture:texture atIndex:0];
    }
    [activeEncoder drawPrimitives:MTLPrimitiveTypeTriangleStrip vertexStart:0 vertexCount:4];
}

// Draws an arbitrary solid-color primitive (line / line strip / triangle list)
// with the pre-encoded vertex array. Used for DrawLine, DrawRect, FillPolygon.
static void drawSolidPrimitive(MTLPrimitiveType primitive,
                               const float *vertices,
                               int vertexCount,
                               simd_float4 color) {
    if (activeEncoder == nil || pipelineCache == nil || vertexCount <= 0) return;
    id<MTLRenderPipelineState> state = [pipelineCache pipelineFor:CN1MetalPipelineSolidColor];
    if (state == nil) return;
    // setVertexBytes has a 4KB limit; at 8 bytes per vertex (float2) that's
    // 512 vertices. Convex polygons from CN1 are well within that.
    size_t byteCount = sizeof(float) * 2 * (size_t)vertexCount;
    if (byteCount > 4096) return;

    [activeEncoder setRenderPipelineState:state];
    [activeEncoder setVertexBytes:vertices length:byteCount atIndex:0];
    CN1MetalMatrices matrices = currentMatrices();
    [activeEncoder setVertexBytes:&matrices length:sizeof(matrices) atIndex:1];
    [activeEncoder setFragmentBytes:&color length:sizeof(color) atIndex:0];
    [activeEncoder drawPrimitives:primitive vertexStart:0 vertexCount:(NSUInteger)vertexCount];
}

static simd_float4 premultipliedColor(int color, int alpha) {
    float a = alpha / 255.0f;
    return (simd_float4){
        ((color >> 16) & 0xff) / 255.0f * a,
        ((color >> 8)  & 0xff) / 255.0f * a,
        ((color)       & 0xff) / 255.0f * a,
        a
    };
}

// --------------- Public draw primitives ---------------

void CN1MetalFillRect(int color, int alpha, int x, int y, int width, int height) {
    simd_float4 colorV = premultipliedColor(color, alpha);
    float vertices[8] = {
        (float)x,         (float)y,
        (float)(x+width), (float)y,
        (float)x,         (float)(y+height),
        (float)(x+width), (float)(y+height)
    };
    drawQuad(CN1MetalPipelineSolidColor, vertices, NULL, colorV, nil);
}

// GPU line rasterisation snaps each line to the pixel grid: a horizontal
// line at integer y straddles the boundary between row y-1 and row y, so
// hardware antialiasing splits the coverage between two rows at half
// intensity each -- the line ends up looking 2 px wide and washed out.
// The standard fix is to offset the line's endpoints by half a pixel so
// the line passes through the pixel-centre of a single row. The GL ES2
// DrawLine / DrawRect ops already do this (DrawLine.m:122, DrawRect.m:122).
//
// One catch: `MTLPrimitiveTypeLine` clips at the viewport boundary, and
// pushing endpoints to `(coord + 0.5)` shoves a line that ends at the
// viewport's right/bottom edge (pixel coord == viewport size) just
// outside the [-1, 1] NDC range. For DrawLine that's used inside
// graphics tests like TileImage's source bitmap (a 20x20 mutable image
// drawn with `drawLine(0, 0, 20, 20)` and `drawLine(20, 0, 0, 20)`),
// pushing the (20, 20) endpoint to (20.5, 20.5) makes the GPU clip the
// line entirely, leaving the resulting tile a solid colour with no X
// at all. Snap each endpoint independently: only nudge to a pixel
// centre when doing so would not push it past the viewport, otherwise
// pull back from the boundary by 0.5 to keep the line inside.
//
// DrawRect doesn't need that guard -- its end vertex is `x + width`,
// which is always one pixel past the rect's last drawn pixel, so
// `+ 0.5` lands inside the visible viewport for any valid rect (and
// rect's right/bottom-edge lines are *supposed* to be flush against
// the viewport edge in their natural use case).
static inline float lineCoord(int v, int extent) {
    if (extent > 0 && v >= extent) return (float)v - 0.5f;
    return (float)v + 0.5f;
}
void CN1MetalDrawLine(int color, int alpha, int x1, int y1, int x2, int y2) {
    simd_float4 colorV = premultipliedColor(color, alpha);
    int fbW = currentFramebufferWidth;
    int fbH = currentFramebufferHeight;
    float vertices[4] = {
        lineCoord(x1, fbW), lineCoord(y1, fbH),
        lineCoord(x2, fbW), lineCoord(y2, fbH)
    };
    drawSolidPrimitive(MTLPrimitiveTypeLine, vertices, 2, colorV);
}

void CN1MetalDrawRect(int color, int alpha, int x, int y, int width, int height) {
    simd_float4 colorV = premultipliedColor(color, alpha);
    // Closed rectangle outline as a 5-vertex line strip. +0.5 on every
    // vertex for the same pixel-centre reason as CN1MetalDrawLine.
    float vertices[10] = {
        (float)x         + 0.5f, (float)y          + 0.5f,
        (float)(x+width) + 0.5f, (float)y          + 0.5f,
        (float)(x+width) + 0.5f, (float)(y+height) + 0.5f,
        (float)x         + 0.5f, (float)(y+height) + 0.5f,
        (float)x         + 0.5f, (float)y          + 0.5f
    };
    drawSolidPrimitive(MTLPrimitiveTypeLineStrip, vertices, 5, colorV);
}

void CN1MetalFillPolygon(const float *xCoords, const float *yCoords, int num,
                         int color, int alpha) {
    if (num < 3) return;
    simd_float4 colorV = premultipliedColor(color, alpha);
    // Triangulate as a fan from vertex 0: (0,1,2), (0,2,3), (0,3,4), ...
    // Works for convex polygons only, matching the GL path's assumption.
    //
    // setVertexBytes has a 4KB hard limit (= 512 float2 vertices = ~170
    // triangles per draw call). Polygons with more triangles must be
    // submitted in chunks. The previous implementation silently truncated
    // at 170 triangles, leaving half a 360-point circle unfilled in
    // graphics-fill-polygon. Fix: emit batches of up to BATCH_TRIS
    // triangles, each starting from vertex 0 (so the fan still meets
    // contiguously). Adjacent chunks share the seam vertex (i, i+1) so
    // the visual surface stays gap-free.
    enum { BATCH_TRIS = 168, BATCH_FLOATS = BATCH_TRIS * 6 };
    float stackBuf[BATCH_FLOATS];
    int triRemaining = num - 2;
    int firstTri = 0;
    while (triRemaining > 0) {
        int batch = (triRemaining > BATCH_TRIS) ? BATCH_TRIS : triRemaining;
        int out = 0;
        for (int t = 0; t < batch; t++) {
            int i = 1 + firstTri + t;            // 1, 2, 3, ...
            stackBuf[out++] = xCoords[0];        stackBuf[out++] = yCoords[0];
            stackBuf[out++] = xCoords[i];        stackBuf[out++] = yCoords[i];
            stackBuf[out++] = xCoords[i + 1];    stackBuf[out++] = yCoords[i + 1];
        }
        drawSolidPrimitive(MTLPrimitiveTypeTriangle, stackBuf, out / 2, colorV);
        firstTri += batch;
        triRemaining -= batch;
    }
}

void CN1MetalClearRect(int x, int y, int width, int height) {
    simd_float4 zero = (simd_float4){0, 0, 0, 0};
    float vertices[8] = {
        (float)x,         (float)y,
        (float)(x+width), (float)y,
        (float)x,         (float)(y+height),
        (float)(x+width), (float)(y+height)
    };
    drawQuad(CN1MetalPipelineClearPunch, vertices, NULL, zero, nil);
}

void CN1MetalDrawImage(id<MTLTexture> texture, int alpha, int x, int y, int width, int height) {
    if (texture == nil) return;
    float a = alpha / 255.0f;
    // Texture tint uses straight alpha modulator (no premultiplication here;
    // the fragment shader handles it).
    simd_float4 tint = (simd_float4){ a, a, a, a };
    float vertices[8] = {
        (float)x,         (float)y,
        (float)(x+width), (float)y,
        (float)x,         (float)(y+height),
        (float)(x+width), (float)(y+height)
    };
    // V=0-at-top sampling: memory_row_0 lands at the top vertex. For
    // UIImage-backed sources, CN1MetalTextureFromUIImage stores them in the
    // GL-compatible layout (memory_row_0 = source's visual BOTTOM), so this
    // mapping renders the source upside-down vs. its natural orientation —
    // matching what GL does for assets designed against its V=1-at-top
    // convention. For mutable-image targets, Phase 3 renders into the texture
    // with user-y=0 at memory_row_0, so V=0-at-top correctly puts the
    // mutable's own top at dest top.
    static const float texcoords[8] = {
        0, 0,
        1, 0,
        0, 1,
        1, 1
    };
    drawQuad(CN1MetalPipelineTexturedRGBA, vertices, texcoords, tint, texture);
}

void CN1MetalTileImage(id<MTLTexture> texture, int alpha,
                       int x, int y, int width, int height,
                       int imageWidth, int imageHeight) {
    if (texture == nil || width <= 0 || height <= 0 || imageWidth <= 0 || imageHeight <= 0) return;
    float a = alpha / 255.0f;
    simd_float4 tint = (simd_float4){ a, a, a, a };

    for (int yPos = 0; yPos < height; yPos += imageHeight) {
        int dh = imageHeight;
        if (yPos + dh > height) dh = height - yPos;
        float vMax = (float)dh / (float)imageHeight;
        for (int xPos = 0; xPos < width; xPos += imageWidth) {
            int dw = imageWidth;
            if (xPos + dw > width) dw = width - xPos;
            float uMax = (float)dw / (float)imageWidth;
            int dx = x + xPos;
            int dy = y + yPos;
            float vertices[8] = {
                (float)dx,        (float)dy,
                (float)(dx + dw), (float)dy,
                (float)dx,        (float)(dy + dh),
                (float)(dx + dw), (float)(dy + dh)
            };
            float texcoords[8] = {
                0.0f, 0.0f,
                uMax, 0.0f,
                0.0f, vMax,
                uMax, vMax
            };
            drawQuad(CN1MetalPipelineTexturedRGBA, vertices, texcoords, tint, texture);
        }
    }
}

// --------------- Text rendering (CoreText glyph atlas) ---------------
//
// Per the Metal-port plan's Phase 4: shape the string via CoreText and
// emit one alpha-mask quad per glyph against the per-(font, pointSize)
// R8 atlas owned by CN1MetalGlyphAtlas. There is no whole-string CG-
// rasterise fallback -- the plan was explicit ("Delete DrawStringTextureCache
// usage on the Metal path -- no more whole-string LRU"). If the atlas
// or CTLine cannot be built we log and skip the string; that exposes
// the failure rather than papering over it with a different pipeline
// that would silently mask the bug.

void CN1MetalDrawString(NSString *str, UIFont *font, int color, int alpha, int x, int y) {
    if (str == nil || font == nil || str.length == 0) return;

    CN1MetalGlyphAtlas *atlas = [CN1MetalGlyphAtlas atlasForFont:font];
    if (atlas == nil) {
        NSLog(@"CN1MetalDrawString: no atlas available for font %@ pt=%g; string skipped",
              font.fontName, (double)font.pointSize);
        return;
    }

    // Pass the UIFont directly as the kCTFontAttributeName value. CoreText
    // accepts UIFont here and uses it to drive glyph mapping and positions
    // -- keeping the CTLine completely consistent with how UIKit's
    // drawAtPoint:withAttributes: shapes the same string. Bridging through
    // atlas.ctFont (built via CTFontCreateWithFontDescriptor) was producing
    // slightly different metrics for the first DrawString call after a
    // fresh form, which surfaced as the TL panel of graphics-draw-string-
    // decorated rendering larger/wider glyphs than TR/BL/BR despite
    // identical Java state.
    NSDictionary *attrs = @{ (__bridge NSString *)kCTFontAttributeName: font };
    CFAttributedStringRef attrStr = CFAttributedStringCreate(NULL,
                                                             (__bridge CFStringRef)str,
                                                             (__bridge CFDictionaryRef)attrs);
    if (attrStr == NULL) {
        NSLog(@"CN1MetalDrawString: CFAttributedStringCreate failed for \"%@\"; string skipped", str);
        return;
    }
    CTLineRef line = CTLineCreateWithAttributedString(attrStr);
    CFRelease(attrStr);
    if (line == NULL) {
        NSLog(@"CN1MetalDrawString: CTLineCreateWithAttributedString failed for \"%@\"; string skipped", str);
        return;
    }

    // cn1's drawString convention: (x, y) is the TOP-LEFT of the line bbox
    // in Y-down screen coords (matches the GL path's whole-string-bitmap
    // approach where drawAtPoint:withAttributes: puts line TOP at the given
    // point). UIKit's drawAtPoint then places the baseline at point.y +
    // font.ascender; we mirror that exactly so per-glyph positioning lines
    // up with the Phase-2 fallback and with GL output. Using UIFont.ascender
    // (not CTFontGetAscent) is intentional — UIKit's metric is what
    // drawAtPoint references and the values can disagree slightly across
    // fonts.
    float baselineY = (float)y + (float)font.ascender;

    simd_float4 colorV = premultipliedColor(color, alpha);
    int textureW = atlas.textureWidth;
    int textureH = atlas.textureHeight;
    id<MTLTexture> atlasTex = atlas.texture;

    CFArrayRef runs = CTLineGetGlyphRuns(line);
    CFIndex runCount = CFArrayGetCount(runs);
    for (CFIndex r = 0; r < runCount; r++) {
        CTRunRef run = CFArrayGetValueAtIndex(runs, r);
        CFIndex glyphCount = CTRunGetGlyphCount(run);
        if (glyphCount == 0) continue;

        const CGGlyph *glyphPtr = CTRunGetGlyphsPtr(run);
        CGGlyph *glyphBuf = NULL;
        if (glyphPtr == NULL) {
            glyphBuf = (CGGlyph *)malloc(sizeof(CGGlyph) * (size_t)glyphCount);
            CTRunGetGlyphs(run, CFRangeMake(0, glyphCount), glyphBuf);
            glyphPtr = glyphBuf;
        }
        const CGPoint *posPtr = CTRunGetPositionsPtr(run);
        CGPoint *posBuf = NULL;
        if (posPtr == NULL) {
            posBuf = (CGPoint *)malloc(sizeof(CGPoint) * (size_t)glyphCount);
            CTRunGetPositions(run, CFRangeMake(0, glyphCount), posBuf);
            posPtr = posBuf;
        }

        for (CFIndex i = 0; i < glyphCount; i++) {
            CGGlyph g = glyphPtr[i];
            CN1MetalGlyphSlot *slot = [atlas slotForGlyph:g];
            if (slot == nil) continue;          // atlas full
            if (slot.width == 0) continue;      // empty glyph (space, control)

            // Slot bitmap covers (bbox + 2px padding). Place the slot's
            // top-left so the glyph art lines up with where CT expects:
            //   bbox-left-on-screen  = x + posX + bearingX
            //   bbox-top-on-screen   = baselineY - posY - (bearingY + bbox.height)
            // Slot extends 1px above and to the left of the bbox.
            float gx = (float)x + (float)posPtr[i].x + slot.bearingX - 1.0f;
            float gy = baselineY - (float)posPtr[i].y
                       - (slot.bearingY + slot.bboxHeight) - 1.0f;
            float gw = (float)slot.width;
            float gh = (float)slot.height;

            float vertices[8] = {
                gx,        gy,
                gx + gw,   gy,
                gx,        gy + gh,
                gx + gw,   gy + gh
            };

            // CN1MetalGlyphAtlas rasterises with default Y-up CG; the
            // glyph ends up right-side-up in raster memory order with
            // memory_row_0 at the slot's TOP edge. V=0-at-top sampling
            // maps the slot rect to the dest quad without a flip.
            float u0 = (float)slot.atlasX / (float)textureW;
            float u1 = (float)(slot.atlasX + slot.width) / (float)textureW;
            float v0 = (float)slot.atlasY / (float)textureH;
            float v1 = (float)(slot.atlasY + slot.height) / (float)textureH;
            float texcoords[8] = {
                u0, v0,
                u1, v0,
                u0, v1,
                u1, v1,
            };

            drawQuad(CN1MetalPipelineAlphaMask, vertices, texcoords, colorV, atlasTex);
        }

        if (glyphBuf) free(glyphBuf);
        if (posBuf) free(posBuf);
    }

    CFRelease(line);
}

// --------------- Gradient rendering ---------------
// Per the Metal-port plan's Phase 1 pipeline list (linear-gradient,
// radial-gradient): pure-GPU MSL shaders, no CGBitmap rasterise, no
// LRU cache. The shaders interpolate startColor->endColor across the
// quad in 0..1 texcoord space; see cn1_fs_linear_gradient and
// cn1_fs_radial_gradient in CN1MetalShaders.metal.

// Convert a 0xAARRGGBB int (alpha=0 implies opaque, matching Java's
// historical createImage/setColor convention) to a premultiplied
// simd_float4. Used by the gradient shaders, which expect premultiplied
// inputs because the pipeline blend factors are (One, OneMinusSrcAlpha).
static simd_float4 premultipliedFromARGB(int argb) {
    float a = ((argb >> 24) & 0xff) / 255.0f;
    if (((argb >> 24) & 0xff) == 0) {
        a = 1.0f; // alpha=0 in legacy paint state means "opaque" (see GL gradient impl)
    }
    float r = ((argb >> 16) & 0xff) / 255.0f;
    float g = ((argb >> 8)  & 0xff) / 255.0f;
    float b = ( argb        & 0xff) / 255.0f;
    return (simd_float4){ r * a, g * a, b * a, a };
}

// Quad-with-three-uniforms helper. The gradient and alpha-mask-radial
// pipelines all need (startColor, endColor, params) as fragment buffers
// 0/1/2 plus a 0..1 texcoord at vertex buffer 2. drawQuad above only
// supports a single fragment uniform so we have a separate helper
// instead of overloading it.
static void drawGradientQuad(CN1MetalPipeline pipeline,
                             const float vertices[8],
                             const float texcoords[8],
                             simd_float4 startColor,
                             simd_float4 endColor,
                             simd_float4 params) {
    if (activeEncoder == nil || pipelineCache == nil) return;
    id<MTLRenderPipelineState> state = [pipelineCache pipelineFor:pipeline];
    if (state == nil) return;
    [activeEncoder setRenderPipelineState:state];
    [activeEncoder setVertexBytes:vertices  length:sizeof(float) * 8 atIndex:0];
    CN1MetalMatrices matrices = currentMatrices();
    [activeEncoder setVertexBytes:&matrices  length:sizeof(matrices)  atIndex:1];
    [activeEncoder setVertexBytes:texcoords length:sizeof(float) * 8 atIndex:2];
    [activeEncoder setFragmentBytes:&startColor length:sizeof(startColor) atIndex:0];
    [activeEncoder setFragmentBytes:&endColor   length:sizeof(endColor)   atIndex:1];
    [activeEncoder setFragmentBytes:&params     length:sizeof(params)     atIndex:2];
    [activeEncoder drawPrimitives:MTLPrimitiveTypeTriangleStrip vertexStart:0 vertexCount:4];
}

void CN1MetalDrawGradient(int type, int startColor, int endColor,
                          int x, int y, int width, int height,
                          float relativeX, float relativeY, float relativeSize) {
    if (width <= 0 || height <= 0) return;

    // Per the plan's Phase 1: gradients render through pure-GPU MSL
    // fragment shaders -- no CGContextDrawLinearGradient / CGContextDrawRadialGradient,
    // no offscreen bitmap upload, no LRU cache. The shader interpolates
    // start->end across the quad in 0..1 texcoord space.
    simd_float4 sc = premultipliedFromARGB(startColor);
    simd_float4 ec = premultipliedFromARGB(endColor);

    float vertices[8] = {
        (float)x,           (float)y,
        (float)(x + width), (float)y,
        (float)x,           (float)(y + height),
        (float)(x + width), (float)(y + height)
    };
    static const float texcoords[8] = {
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    };

    // Constants must match DrawGradient.h.
    enum { GRAD_TYPE_RADIAL = 1, GRAD_TYPE_HORIZONTAL = 2, GRAD_TYPE_VERTICAL = 3 };

    switch (type) {
        case GRAD_TYPE_HORIZONTAL: {
            // axis.x == 1 picks texcoord.x as the gradient parameter t.
            simd_float4 axis = (simd_float4){ 1.0f, 0.0f, 0.0f, 0.0f };
            drawGradientQuad(CN1MetalPipelineLinearGradient, vertices, texcoords, sc, ec, axis);
            break;
        }
        case GRAD_TYPE_VERTICAL: {
            // axis.x == 0 picks texcoord.y.
            simd_float4 axis = (simd_float4){ 0.0f, 0.0f, 0.0f, 0.0f };
            drawGradientQuad(CN1MetalPipelineLinearGradient, vertices, texcoords, sc, ec, axis);
            break;
        }
        case GRAD_TYPE_RADIAL: {
            // Mirror the GL/CG semantics: centre at (relativeX, relativeY)
            // in 0..1 fractions of (width, height); radius_px = relativeSize
            // * MIN(width, height); convert that radius into 0..1 texcoord
            // space along each axis (different along each axis whenever
            // width != height -- the resulting elliptical iso-curves match
            // CGContextDrawRadialGradient's circular iso-curves at the
            // smaller-dim boundary).
            float minDim = (float)((width < height) ? width : height);
            float radiusPx = relativeSize * minDim;
            float rxTex = radiusPx / (float)width;
            float ryTex = radiusPx / (float)height;
            simd_float4 params = (simd_float4){ relativeX, relativeY, rxTex, ryTex };
            drawGradientQuad(CN1MetalPipelineRadialGradient, vertices, texcoords, sc, ec, params);
            break;
        }
    }
}

// --------------- Alpha mask rendering (path-based shapes) ---------------

id<MTLTexture> CN1MetalCreateAlphaMaskTexture(const uint8_t *bytes, int width, int height) {
    if (bytes == NULL || width <= 0 || height <= 0) return nil;
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return nil;
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatR8Unorm
        width:width height:height mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    id<MTLTexture> tex = [device newTextureWithDescriptor:desc];
    if (tex == nil) return nil;
    [tex replaceRegion:MTLRegionMake2D(0, 0, width, height)
           mipmapLevel:0
             withBytes:bytes
           bytesPerRow:width];
    return tex;
}

void CN1MetalDrawAlphaMask(id<MTLTexture> texture, int color, int alpha,
                           int x, int y, int width, int height) {
    if (texture == nil) return;
    // The AlphaMask fragment shader (cn1_fs_alpha_mask) does:
    //   float a = sample(tex).r;
    //   return float4(color.rgb * a, color.a * a);
    // For premultiplied-alpha blending we need (R*a, G*a, B*a, a) where a is
    // (alpha/255) and (R,G,B) are color components. The shader multiplies by
    // tex.r once; we pass color premultiplied by alpha so the final out is
    // (R*alpha*a, G*alpha*a, B*alpha*a, alpha*a) which matches GL's
    // DrawTextureAlphaMask basic shader.
    simd_float4 colorV = premultipliedColor(color, alpha);
    float vertices[8] = {
        (float)x,           (float)y,
        (float)(x + width), (float)y,
        (float)x,           (float)(y + height),
        (float)(x + width), (float)(y + height)
    };
    static const float texcoords[8] = {
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    };
    drawQuad(CN1MetalPipelineAlphaMask, vertices, texcoords, colorV, texture);
}

// Draw an alpha-mask quad with a radial gradient as colour source. Mirrors
// the GL radial-gradient program in DrawTextureAlphaMask.m:340-395 — the
// gradient is parameterised in texcoord-space (0..1) so the shader can
// compute it without knowing screen coordinates. Caller passes the
// gradient's screen-space bbox (gx, gy, gw, gh) and we convert to
// texcoord-space relative to the alpha-mask quad (x, y, width, height).
void CN1MetalDrawAlphaMaskRadial(id<MTLTexture> texture,
                                 int x, int y, int width, int height,
                                 int startColor, int endColor,
                                 float gx, float gy, float gw, float gh) {
    if (texture == nil || width <= 0 || height <= 0) return;
    if (activeEncoder == nil || pipelineCache == nil) return;
    id<MTLRenderPipelineState> state = [pipelineCache pipelineFor:CN1MetalPipelineAlphaMaskRadial];
    if (state == nil) return;
    [activeEncoder setRenderPipelineState:state];

    float vertices[8] = {
        (float)x,           (float)y,
        (float)(x + width), (float)y,
        (float)x,           (float)(y + height),
        (float)(x + width), (float)(y + height)
    };
    static const float texcoords[8] = {
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    };
    [activeEncoder setVertexBytes:vertices length:sizeof(float) * 8 atIndex:0];
    CN1MetalMatrices matrices = currentMatrices();
    [activeEncoder setVertexBytes:&matrices length:sizeof(matrices) atIndex:1];
    [activeEncoder setVertexBytes:texcoords length:sizeof(float) * 8 atIndex:2];

    // Premultiplied colours so blending produces the right output.
    simd_float4 startV = premultipliedColor(startColor, 0xff);
    simd_float4 endV   = premultipliedColor(endColor,   0xff);
    // Centre and radii in texcoord space.
    float cx = (gx + gw / 2.0f - (float)x) / (float)width;
    float cy = (gy + gh / 2.0f - (float)y) / (float)height;
    float rx = (gw / 2.0f) / (float)width;
    float ry = (gh / 2.0f) / (float)height;
    simd_float4 params = (simd_float4){ cx, cy, rx, ry };
    [activeEncoder setFragmentBytes:&startV length:sizeof(startV) atIndex:0];
    [activeEncoder setFragmentBytes:&endV   length:sizeof(endV)   atIndex:1];
    [activeEncoder setFragmentBytes:&params length:sizeof(params) atIndex:2];
    [activeEncoder setFragmentTexture:texture atIndex:0];
    [activeEncoder drawPrimitives:MTLPrimitiveTypeTriangleStrip vertexStart:0 vertexCount:4];
}

// --------------- Texture helpers ---------------

id<MTLTexture> CN1MetalTextureFromUIImage(UIImage *image) {
    if (image == nil) return nil;
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return nil;
    int w = (int)image.size.width * image.scale;
    int h = (int)image.size.height * image.scale;
    if (w <= 0 || h <= 0) return nil;

    // Rasterize UIImage into a CGBitmapContext, then upload as MTLTexture.
    // No CTM flip: with default CG (Y-up) coords, CGContextDrawImage lays the
    // source's row 0 at the BOTTOM of memory and the source's last row at
    // memory_row_0 — i.e. the texture is stored upside-down in memory order.
    // That mirrors GLUIImage.getTexture's POW2 layout (modulo padding) and is
    // the orientation cn1's iOS theme assets are designed for: GL's V=1-at-top
    // sampling renders them right-side-up; Metal's V=0-at-top sampling on this
    // same memory layout reproduces GL's pixels exactly. Flipping the CTM
    // here (the original implementation) made source row 0 land at
    // memory_row_0 and produced a 1-pixel decoration leak at the title-bar
    // top edge (rows 246-247) because cn1 9-patch slices put their drop-shadow
    // row at source row 0 — which GL has always rendered at dest BOTTOM.
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    void *rawData = calloc(h * w * 4, sizeof(uint8_t));
    CGContextRef ctx = CGBitmapContextCreate(rawData, w, h, 8, w * 4, cs,
        kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGColorSpaceRelease(cs);
    CGContextDrawImage(ctx, CGRectMake(0, 0, w, h), image.CGImage);
    CGContextRelease(ctx);

    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatRGBA8Unorm
        width:w height:h mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    id<MTLTexture> texture = [device newTextureWithDescriptor:desc];
    [texture replaceRegion:MTLRegionMake2D(0, 0, w, h)
               mipmapLevel:0
                 withBytes:rawData
               bytesPerRow:w * 4];
    free(rawData);
    return texture;
}

// --------------- Phase 3 v2: mutable-image rendering ---------------

// Saved screen state during a mutable-image drain. drawFrame opens the
// screen encoder via setFramebuffer (publishing it into activeEncoder).
// When draining a mutable target we side-trip: save these globals, swap
// to the mutable encoder, encode the mutable's ops, then restore.
// Single-threaded: drawFrame is the only drainer; nested mutable side-trips
// are not supported (and not needed -- ops are flat in a single queue).
static __unsafe_unretained id<MTLRenderCommandEncoder> savedScreenEncoder = nil;
static simd_float4x4 savedScreenProjection;
static int savedScreenFw = 0;
static int savedScreenFh = 0;
static BOOL savedScreenStateValid = NO;

// Build a Y-down ortho projection for an offscreen (w x h) framebuffer.
// Mirrors METALView's CN1MetalOrtho -- if that one ever changes, update
// this in lockstep.
static simd_float4x4 mutableProjection(int w, int h) {
    float invW = 1.0f / (float)w;
    float invH = 1.0f / (float)h;
    return (simd_float4x4){{
        { 2.0f * invW,           0.0f,                  0.0f,    0.0f },
        { 0.0f,                  -2.0f * invH,          0.0f,    0.0f },
        { 0.0f,                  0.0f,                  0.5f,    0.0f },
        { -1.0f,                 1.0f,                  0.5f,    1.0f }
    }};
}

void CN1MetalEnsureMutableTexture(GLUIImage *image, int width, int height) {
    if (image == nil || width <= 0 || height <= 0) return;
    id<MTLTexture> existing = [image mtlMutableTexture];
    if (existing != nil &&
        [image mtlMutableWidth] == width &&
        [image mtlMutableHeight] == height) {
        return;
    }
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return;
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
        width:(NSUInteger)width height:(NSUInteger)height mipmapped:NO];
    desc.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModePrivate;
    id<MTLTexture> tex = [device newTextureWithDescriptor:desc];
    if (tex == nil) return;
    // Clear new texture to the fill colour stashed by createNativeMutableImage.
    // Default Image.createImage(w, h) → 0xffffffff opaque white; createImage(w, h, argb)
    // honours the user's fill. Sentinel 0 (uninitialised ivar) keeps the prior
    // transparent-black behaviour for non-mutable getMTLTexture paths.
    int argb = [image mtlMutableInitialARGB];
    double a = ((argb >> 24) & 0xff) / 255.0;
    double r = ((argb >> 16) & 0xff) / 255.0;
    double g = ((argb >> 8)  & 0xff) / 255.0;
    double b = ( argb        & 0xff) / 255.0;
    // Store premultiplied so subsequent ops (which run through the
    // pipeline cache's premultiplied blend: src=One, dst=OneMinusSrcAlpha)
    // composite correctly when this texture is later sampled. Without this,
    // a half-transparent green fill (Image.createImage(w,h, 0x2000ff00))
    // sampled at full green=1.0 + alpha=0.125 gets blended as green*1.0 +
    // dst*(1-0.125) instead of green*0.125 + dst*(1-0.125), producing a
    // saturated cyan when composed over a blue background instead of the
    // intended faintly-green tint. (Compare graphics-draw-image-rect's
    // blue arcs visible through the green mutableWithAlpha box: GL renders
    // them blue with a faint green wash; pre-fix Metal rendered them
    // turquoise.)
    // Single command buffer combining clear + (optional) UIImage seed.
    // The seed render pass loadAction=Load reads the cleared bg from the
    // earlier subpass within the same cb -- using two separate cb's
    // would race because cb commit is async on the queue and the seed
    // pass's Load could capture pre-clear state. Render-pass-based seed
    // (vs blit) is required because CN1MetalTextureFromUIImage allocates
    // RGBA8Unorm textures while the mutable target is BGRA8Unorm; a
    // blit would copy raw bytes and swap R/B, the textured pipeline's
    // sampler does the format conversion automatically.
    id<MTLCommandQueue> queue = CN1MetalCommandQueue();
    UIImage *existingUI = [image getImage];
    if (queue != nil) {
        id<MTLCommandBuffer> setupCb = [queue commandBuffer];

        // Pass 1: clear to bg colour.
        MTLRenderPassDescriptor *clearPass = [MTLRenderPassDescriptor renderPassDescriptor];
        clearPass.colorAttachments[0].texture = tex;
        clearPass.colorAttachments[0].loadAction = MTLLoadActionClear;
        clearPass.colorAttachments[0].storeAction = MTLStoreActionStore;
        clearPass.colorAttachments[0].clearColor = MTLClearColorMake(r * a, g * a, b * a, a);
        [[setupCb renderCommandEncoderWithDescriptor:clearPass] endEncoding];

        // Pass 2: if the GLUIImage has an existing UIImage (e.g. it was
        // returned by gausianBlurImage / FontImage / etc.), seed the
        // freshly-cleared mutable texture with those pixels so subsequent
        // draws layer on top. Without this seed gausianBlurImage's blurred
        // shadow halo (Switch's createRoundThumbImage path) is lost the
        // moment the next draw triggers EnsureMutableTexture, and the
        // composited Switch ends up with no outline halo around the thumb.
        // GL's startDrawingOnImageImpl gets this implicitly by drawing the
        // existing UIImage into the CG context.
        ensurePipelineCache();
        id<MTLRenderPipelineState> seedState = (existingUI != nil && pipelineCache != nil)
            ? [pipelineCache pipelineFor:CN1MetalPipelineTexturedRGBA]
            : nil;
        if (seedState != nil) {
            id<MTLTexture> srcTex = CN1MetalTextureFromUIImage(existingUI);
            if (srcTex != nil) {
                MTLRenderPassDescriptor *seedPass = [MTLRenderPassDescriptor renderPassDescriptor];
                seedPass.colorAttachments[0].texture = tex;
                seedPass.colorAttachments[0].loadAction = MTLLoadActionLoad;
                seedPass.colorAttachments[0].storeAction = MTLStoreActionStore;
                id<MTLRenderCommandEncoder> seedEnc = [setupCb renderCommandEncoderWithDescriptor:seedPass];
                [seedEnc setViewport:(MTLViewport){0.0, 0.0, (double)width, (double)height, 0.0, 1.0}];
                [seedEnc setRenderPipelineState:seedState];

                CN1MetalMatrices seedMatrices;
                seedMatrices.projection = mutableProjection(width, height);
                seedMatrices.modelView = identityMatrix();
                seedMatrices.transform = identityMatrix();

                float seedVerts[8] = {
                    0.0f,         0.0f,
                    (float)width, 0.0f,
                    0.0f,         (float)height,
                    (float)width, (float)height
                };
                // V flipped: source memory_row_0 = visual BOTTOM (no CTM
                // flip in CN1MetalTextureFromUIImage), so we want V=1 at
                // dest top (sample source's last memory row = visual top
                // there) and V=0 at dest bottom.
                float seedTexcoords[8] = {
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 0.0f
                };
                simd_float4 seedTint = (simd_float4){ 1.0f, 1.0f, 1.0f, 1.0f };
                [seedEnc setVertexBytes:seedVerts length:sizeof(seedVerts) atIndex:0];
                [seedEnc setVertexBytes:&seedMatrices length:sizeof(seedMatrices) atIndex:1];
                [seedEnc setVertexBytes:seedTexcoords length:sizeof(seedTexcoords) atIndex:2];
                [seedEnc setFragmentBytes:&seedTint length:sizeof(seedTint) atIndex:0];
                [seedEnc setFragmentTexture:srcTex atIndex:0];
                [seedEnc drawPrimitives:MTLPrimitiveTypeTriangleStrip vertexStart:0 vertexCount:4];
                [seedEnc endEncoding];
#ifndef CN1_USE_ARC
                // CN1MetalTextureFromUIImage returns a +1 retain. Metal
                // keeps the texture alive internally for the duration of
                // the encoded work, so dropping the retain now is safe.
                [srcTex release];
#endif
            }
        }

        [setupCb commit];
    }
    [image setMtlMutableTexture:tex width:width height:height];
    // setMtlMutableTexture retains; balance the +1 from
    // newTextureWithDescriptor: so the GLUIImage owns the only retain.
    // Without this the texture leaks even with the dealloc release.
#ifndef CN1_USE_ARC
    [tex release];
#endif
}

BOOL CN1MetalBeginMutableImageDraw(GLUIImage *image) {
    if (image == nil) return NO;
    id<MTLTexture> tex = [image mtlMutableTexture];
    if (tex == nil) return NO;
    int w = [image mtlMutableWidth];
    int h = [image mtlMutableHeight];
    id<MTLCommandQueue> queue = CN1MetalCommandQueue();
    if (queue == nil) return NO;
    id<MTLCommandBuffer> cb = [queue commandBuffer];
    if (cb == nil) return NO;
    MTLRenderPassDescriptor *desc = [MTLRenderPassDescriptor renderPassDescriptor];
    desc.colorAttachments[0].texture = tex;
    // Load existing pixels so successive frames accumulate -- the GL/CG path
    // semantically holds an "image buffer" that persists between draws into
    // the same Image.getGraphics().
    desc.colorAttachments[0].loadAction = MTLLoadActionLoad;
    desc.colorAttachments[0].storeAction = MTLStoreActionStore;
    id<MTLRenderCommandEncoder> enc = [cb renderCommandEncoderWithDescriptor:desc];
    if (enc == nil) return NO;
    [enc setViewport:(MTLViewport){0.0, 0.0, (double)w, (double)h, 0.0, 1.0}];

    // Save current screen state (drawFrame opened the screen encoder via
    // setFramebuffer before starting drain) and swap in the mutable's.
    savedScreenEncoder = activeEncoder;
    savedScreenProjection = currentProjection;
    savedScreenFw = currentFramebufferWidth;
    savedScreenFh = currentFramebufferHeight;
    savedScreenStateValid = YES;

    activeEncoder = enc;
    currentProjection = mutableProjection(w, h);
    currentFramebufferWidth = w;
    currentFramebufferHeight = h;

    // Stash the cb on the image so End can commit + readback can wait.
    [image setMtlMutableCommandBuffer:cb];
    return YES;
}

void CN1MetalEndMutableImageDraw(GLUIImage *image) {
    if (image == nil) return;
    if (activeEncoder != nil) {
        [activeEncoder endEncoding];
    }
    id<MTLCommandBuffer> cb = [image mtlMutableCommandBuffer];
    if (cb != nil) {
        [cb commit];
        // Keep the cb on the image so readback paths can waitUntilCompleted.
        // It will be released when a subsequent Begin overwrites it (the
        // Metal driver releases the buffer once GPU work is done).
    }

    // Restore screen state so subsequent screen-target ops on the drain
    // queue continue to use the screen encoder.
    if (savedScreenStateValid) {
        activeEncoder = savedScreenEncoder;
        currentProjection = savedScreenProjection;
        currentFramebufferWidth = savedScreenFw;
        currentFramebufferHeight = savedScreenFh;
        savedScreenEncoder = nil;
        savedScreenStateValid = NO;
    }
}

void CN1MetalFlushMutableImageSync(GLUIImage *image) {
    if (image == nil) return;
    id<MTLCommandBuffer> cb = [image mtlMutableCommandBuffer];
    if (cb == nil) return;
    [cb waitUntilCompleted];
    // Don't nil the cb -- multiple readbacks of the same already-completed
    // buffer should be no-op-fast (waitUntilCompleted is idempotent).
}

BOOL CN1MetalReadMutableImagePixels(GLUIImage *image, int *outARGB,
                                     int x, int y, int w, int h,
                                     int imgWidth, int imgHeight) {
    if (image == nil || outARGB == NULL || w <= 0 || h <= 0) return NO;
    id<MTLTexture> tex = [image mtlMutableTexture];
    if (tex == nil) return NO;

    // Ensure GPU work for this image is finished before sampling.
    CN1MetalFlushMutableImageSync(image);

    int texW = (int)tex.width;
    int texH = (int)tex.height;

    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return NO;
    id<MTLCommandQueue> queue = CN1MetalCommandQueue();
    if (queue == nil) return NO;

    // Private storage textures can't be getBytes'd directly on iOS. Blit
    // into a shared-storage scratch texture, wait, then read.
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
        width:(NSUInteger)texW height:(NSUInteger)texH mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModeShared;
    id<MTLTexture> shared = [device newTextureWithDescriptor:desc];
    if (shared == nil) return NO;

    id<MTLCommandBuffer> blitCb = [queue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [blitCb blitCommandEncoder];
    [blit copyFromTexture:tex sourceSlice:0 sourceLevel:0
              sourceOrigin:MTLOriginMake(0, 0, 0)
                sourceSize:MTLSizeMake((NSUInteger)texW, (NSUInteger)texH, 1)
                 toTexture:shared destinationSlice:0 destinationLevel:0
         destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [blitCb commit];
    [blitCb waitUntilCompleted];

    // Read shared texture into a temp BGRA buffer, then convert + scale
    // into outARGB. Texture dims equal imgWidth/imgHeight when the
    // mutable was created via Image.createImage(w, h) but the API allows
    // for arbitrary scaling; honour it.
    NSUInteger rowBytes = (NSUInteger)(texW * 4);
    uint8_t *bytes = (uint8_t *)malloc(rowBytes * (NSUInteger)texH);
    if (bytes == NULL) return NO;
    [shared getBytes:bytes bytesPerRow:rowBytes
          fromRegion:MTLRegionMake2D(0, 0, (NSUInteger)texW, (NSUInteger)texH)
         mipmapLevel:0];

    float scaleX = (imgWidth  > 0) ? ((float)texW / (float)imgWidth)  : 1.0f;
    float scaleY = (imgHeight > 0) ? ((float)texH / (float)imgHeight) : 1.0f;
    for (int row = 0; row < h; row++) {
        for (int col = 0; col < w; col++) {
            int srcX = (int)((x + col) * scaleX);
            int srcY = (int)((y + row) * scaleY);
            int dstIdx = row * w + col;
            if (srcX < 0 || srcX >= texW || srcY < 0 || srcY >= texH) {
                outARGB[dstIdx] = 0;
                continue;
            }
            int srcIdx = srcY * (int)rowBytes + srcX * 4;
            uint8_t b = bytes[srcIdx + 0];
            uint8_t g = bytes[srcIdx + 1];
            uint8_t r = bytes[srcIdx + 2];
            uint8_t a = bytes[srcIdx + 3];
            outARGB[dstIdx] = ((int)a << 24) | ((int)r << 16) | ((int)g << 8) | (int)b;
        }
    }
    free(bytes);
#ifndef CN1_USE_ARC
    // shared is +1 from newTextureWithDescriptor: release it now that the
    // CPU-visible bytes are copied out. Without this every Image.getRGB
    // round-trip leaks a full-resolution staging texture.
    [shared release];
#endif
    return YES;
}

// CGDataProviderCreateWithData expects a C function pointer for the
// release callback, not a block, so this lives at file scope.
static void cn1MetalReadbackFreeData(void * __unused info, const void *data, size_t __unused size) {
    free((void *)data);
}

UIImage *CN1MetalReadMutableImageAsUIImage(GLUIImage *image) {
    if (image == nil) return nil;
    id<MTLTexture> tex = [image mtlMutableTexture];
    if (tex == nil) return nil;

    CN1MetalFlushMutableImageSync(image);

    int texW = (int)tex.width;
    int texH = (int)tex.height;
    if (texW <= 0 || texH <= 0) return nil;

    id<MTLDevice> device = CN1MetalDevice();
    id<MTLCommandQueue> queue = CN1MetalCommandQueue();
    if (device == nil || queue == nil) return nil;

    // Same blit-to-shared dance as CN1MetalReadMutableImagePixels: private
    // textures aren't getBytes'able directly. Build the UIImage from the
    // shared scratch's bytes.
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
        width:(NSUInteger)texW height:(NSUInteger)texH mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModeShared;
    id<MTLTexture> shared = [device newTextureWithDescriptor:desc];
    if (shared == nil) return nil;

    id<MTLCommandBuffer> blitCb = [queue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [blitCb blitCommandEncoder];
    [blit copyFromTexture:tex sourceSlice:0 sourceLevel:0
              sourceOrigin:MTLOriginMake(0, 0, 0)
                sourceSize:MTLSizeMake((NSUInteger)texW, (NSUInteger)texH, 1)
                 toTexture:shared destinationSlice:0 destinationLevel:0
         destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [blitCb commit];
    [blitCb waitUntilCompleted];

    NSUInteger rowBytes = (NSUInteger)(texW * 4);
    NSUInteger byteCount = rowBytes * (NSUInteger)texH;
    uint8_t *bytes = (uint8_t *)malloc(byteCount);
    if (bytes == NULL) {
#ifndef CN1_USE_ARC
        [shared release];
#endif
        return nil;
    }
    [shared getBytes:bytes bytesPerRow:rowBytes
          fromRegion:MTLRegionMake2D(0, 0, (NSUInteger)texW, (NSUInteger)texH)
         mipmapLevel:0];
#ifndef CN1_USE_ARC
    [shared release];
#endif

    // Wrap the BGRA buffer as a CGImage / UIImage. The provider takes
    // ownership of the malloc'd bytes via the freeData callback below.
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    CGDataProviderRef provider = CGDataProviderCreateWithData(NULL, bytes, byteCount,
        cn1MetalReadbackFreeData);
    CGImageRef cgImg = CGImageCreate((size_t)texW, (size_t)texH, 8, 32, rowBytes, cs,
        (CGBitmapInfo)(kCGBitmapByteOrder32Little | kCGImageAlphaPremultipliedFirst),
        provider, NULL, NO, kCGRenderingIntentDefault);
    CGDataProviderRelease(provider);
    CGColorSpaceRelease(cs);
    if (cgImg == NULL) return nil;
    UIImage *out = [UIImage imageWithCGImage:cgImg];
    CGImageRelease(cgImg);
    return out;
}

// --------------- Memory-pressure cache release ---------------
//
// METALView observes UIApplicationDidReceiveMemoryWarning and calls
// this. We drop the lazy texture caches (whole-string text, gradient,
// per-(font,size) glyph atlases) but keep the pipeline state cache —
// rebuilding pipelines is expensive and they're tiny. The screen
// texture stays too; updateFrameBufferSize: handles its replacement
// on resize. Cleared caches re-fill on demand on the next frame.

extern void CN1MetalGlyphAtlasReleaseAll(void);

void CN1MetalReleaseCaches(void) {
    // Whole-string text cache no longer exists -- text rendering goes
    // exclusively through the CN1MetalGlyphAtlas (Phase 4 mandate).
    // Gradient cache no longer exists either -- gradients render through
    // pure-GPU MSL fragment shaders (Phase 1 pipeline list:
    // linear-gradient / radial-gradient), no offscreen bitmap to cache.
    // Only the glyph atlases need releasing under memory pressure.
    CN1MetalGlyphAtlasReleaseAll();
}

#endif /* CN1_USE_METAL */
