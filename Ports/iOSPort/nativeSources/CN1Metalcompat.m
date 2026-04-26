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
#import "METALView.h"
#import "CodenameOne_GLViewController.h"
#import "GLUIImage.h"
#include <pthread.h>

// --------------- Static state ---------------
//
// All rendering state is THREAD-LOCAL via pthread_key_t. Phase 3 mutable-image
// rendering runs on CN1's EDT while screen rendering runs on the main thread
// (CADisplayLink → drawFrame). A previous attempt used __thread but the
// observed effect (mutable textures stayed cleared even though Begin/Finish
// ran cleanly) suggests __thread isolation didn't take effect under this
// build's compiler/deployment-target combination. pthread_key is more explicit
// and battle-tested. The accessor goes through a lazy pthread_once init so
// calling threadState() from any thread is safe.
//
// Pipeline cache stays shared since it's read-only after first build.

#define CN1_MATRIX_STACK_DEPTH 32

typedef struct {
    __unsafe_unretained id<MTLRenderCommandEncoder> activeEncoder;
    simd_float4x4 currentProjection;
    simd_float4x4 currentModelView;
    simd_float4x4 currentTransform;
    int currentFramebufferWidth;
    int currentFramebufferHeight;
    simd_float4x4 modelViewStack[CN1_MATRIX_STACK_DEPTH];
    int modelViewStackTop;
    // Mutable-image draw state (per-thread because mutable rendering happens
    // on EDT while screen rendering happens on main).
    BOOL mutableActive;
    __unsafe_unretained GLUIImage *mutableActivePeer;
    // Saved screen state during a mutable-image render pass on this thread.
    __unsafe_unretained id<MTLRenderCommandEncoder> savedEncoderBeforeMutable;
    simd_float4x4 savedProjectionBeforeMutable;
    int savedFramebufferWidthBeforeMutable;
    int savedFramebufferHeightBeforeMutable;
    simd_float4x4 savedModelViewBeforeMutable;
    simd_float4x4 savedTransformBeforeMutable;
} CN1MetalThreadState;

static pthread_key_t threadStateKey;
static pthread_once_t threadStateKeyOnce = PTHREAD_ONCE_INIT;

static void freeThreadState(void *p) { free(p); }
static void initThreadStateKey(void) { pthread_key_create(&threadStateKey, freeThreadState); }

static CN1MetalThreadState *threadState(void) {
    pthread_once(&threadStateKeyOnce, initThreadStateKey);
    CN1MetalThreadState *s = (CN1MetalThreadState *)pthread_getspecific(threadStateKey);
    if (s == NULL) {
        s = (CN1MetalThreadState *)calloc(1, sizeof(CN1MetalThreadState));
        // Identity-init transforms so the first frame doesn't draw with a zero matrix.
        // Note: variable name avoids 'I' which is the C99 imaginary-unit macro
        // from <complex.h> (pulled in transitively by simd headers).
        simd_float4x4 ident = (simd_float4x4){{ {1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1} }};
        s->currentTransform = ident;
        s->currentModelView = ident;
        pthread_setspecific(threadStateKey, s);
    }
    return s;
}

// Macros so the rest of this file reads as if these were plain statics.
#define activeEncoder (threadState()->activeEncoder)
#define currentProjection (threadState()->currentProjection)
#define currentModelView (threadState()->currentModelView)
#define currentTransform (threadState()->currentTransform)
#define currentFramebufferWidth (threadState()->currentFramebufferWidth)
#define currentFramebufferHeight (threadState()->currentFramebufferHeight)
#define modelViewStack (threadState()->modelViewStack)
#define modelViewStackTop (threadState()->modelViewStackTop)

static CN1MetalPipelineCache *pipelineCache = nil;

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

// Cache the device + queue so EDT can fetch them without walking through
// METALView's UIKit chain. Walking the chain on a non-main thread (CALayer
// .device, UIView .commandQueue ivar via a UIView accessor) was suspected
// of returning garbage on EDT during Phase 3 mutable rendering, causing
// the mutable encoder to come out nil and all draws to silently no-op.
// Both objects are immutable after METALView's init, so a one-time main-
// thread populate + thread-safe reads is correct.
static id<MTLDevice> _cachedMetalDevice = nil;
static id<MTLCommandQueue> _cachedMetalCommandQueue = nil;

static void populateMetalSingletonsIfNeeded(void) {
    if (_cachedMetalDevice != nil) return;
    @synchronized([METALView class]) {
        if (_cachedMetalDevice != nil) return;
        METALView *mv = (METALView *)[[CodenameOne_GLViewController instance] eaglView];
        _cachedMetalDevice = ((CAMetalLayer *)mv.layer).device;
        _cachedMetalCommandQueue = mv.commandQueue;
    }
}

id<MTLDevice> CN1MetalDevice(void) {
    populateMetalSingletonsIfNeeded();
    return _cachedMetalDevice;
}

id<MTLCommandQueue> CN1MetalCommandQueue(void) {
    populateMetalSingletonsIfNeeded();
    return _cachedMetalCommandQueue;
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

void CN1MetalDrawLine(int color, int alpha, int x1, int y1, int x2, int y2) {
    simd_float4 colorV = premultipliedColor(color, alpha);
    float vertices[4] = { (float)x1, (float)y1, (float)x2, (float)y2 };
    drawSolidPrimitive(MTLPrimitiveTypeLine, vertices, 2, colorV);
}

void CN1MetalDrawRect(int color, int alpha, int x, int y, int width, int height) {
    simd_float4 colorV = premultipliedColor(color, alpha);
    // Closed rectangle outline as a 5-vertex line strip.
    float vertices[10] = {
        (float)x,         (float)y,
        (float)(x+width), (float)y,
        (float)(x+width), (float)(y+height),
        (float)x,         (float)(y+height),
        (float)x,         (float)y
    };
    drawSolidPrimitive(MTLPrimitiveTypeLineStrip, vertices, 5, colorV);
}

void CN1MetalFillPolygon(const float *xCoords, const float *yCoords, int num,
                         int color, int alpha) {
    if (num < 3) return;
    simd_float4 colorV = premultipliedColor(color, alpha);
    // Triangulate as a fan from vertex 0: (0,1,2), (0,2,3), (0,3,4), ...
    // Works for convex polygons only, matching the GL path's assumption.
    int triCount = num - 2;
    int vertCount = triCount * 3;
    // Cap at setVertexBytes's 4KB limit (512 float2 vertices). Convex
    // polygons from CN1 are typically <100 vertices; this should not clip.
    if (vertCount > 512) vertCount = 512;
    float stackBuf[1024]; // 512 vertices * 2 floats = 1024 floats
    int out = 0;
    for (int i = 1; i + 1 < num && out + 6 <= (int)(sizeof(stackBuf) / sizeof(float)); i++) {
        stackBuf[out++] = xCoords[0];   stackBuf[out++] = yCoords[0];
        stackBuf[out++] = xCoords[i];   stackBuf[out++] = yCoords[i];
        stackBuf[out++] = xCoords[i+1]; stackBuf[out++] = yCoords[i+1];
    }
    drawSolidPrimitive(MTLPrimitiveTypeTriangle, stackBuf, out / 2, colorV);
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
    // CN1MetalTextureFromUIImage rasterises with a Y-flipped CTM so the
    // texture stores display-row-0 at memory-row-0; with Metal's V=0-at-top
    // convention, sampling top-of-quad at V=0 gives the top of the image.
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

// --------------- Text rendering (parity level) ---------------

// LRU cache mapping "str|font|color" -> {MTLTexture, stringWidth, stringHeight,
// p2w, p2h}. Capped so long-running apps don't leak memory. Phase 4 will
// replace this with a CoreText glyph atlas; this whole-string cache is a
// direct port of what DrawStringTextureCache does on the GL path.
#define CN1_METAL_TEXT_CACHE_MAX 128
typedef struct {
    NSString *key;
    id<MTLTexture> texture;
    int strWidth;
    int strHeight;
    int p2w;
    int p2h;
} CN1MetalTextCacheEntry;
static CN1MetalTextCacheEntry textCache[CN1_METAL_TEXT_CACHE_MAX];
static int textCacheCount = 0;
static int textCacheNextEvict = 0;

static int nextPowerOf2ForText(int v) {
    int p = 1;
    while (p < v) p <<= 1;
    return p;
}

static CN1MetalTextCacheEntry *findOrBuildTextTexture(NSString *str, UIFont *font, int color) {
    NSString *key = [NSString stringWithFormat:@"%@|%p|%08x", str, font, color];
    for (int i = 0; i < textCacheCount; i++) {
        if ([textCache[i].key isEqualToString:key]) {
            return &textCache[i];
        }
    }
    // Not cached — rasterise. Measure via sizeWithAttributes (sizeWithFont:
    // was deprecated long ago; the GL path still uses it, but UIKit prints
    // a warning on newer SDKs. Keep the same measurement for consistency).
    NSDictionary *attrs = @{ NSFontAttributeName: font };
    CGSize sz = [str sizeWithAttributes:attrs];
    int w = (int)ceilf((float)sz.width);
    int h = (int)ceilf((float)font.lineHeight + 1.0f);
    if (w <= 0 || h <= 0) return NULL;
    int p2w = nextPowerOf2ForText(w);
    int p2h = nextPowerOf2ForText(h);

    // Rasterise into an RGBA bitmap with the colour baked in (same scheme
    // as DrawString.m's GL branch -- fragment shader just modulates alpha).
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    size_t bytesPerRow = 4 * (size_t)p2w;
    void *bitmap = calloc((size_t)p2h * bytesPerRow, 1);
    CGContextRef ctx = CGBitmapContextCreate(bitmap, p2w, p2h, 8, bytesPerRow, cs,
        kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGColorSpaceRelease(cs);

    // UIGraphicsPushContext only sets the current context; it does NOT flip
    // the CTM. Without a flip, UIKit's drawAtPoint: targets CG (0,0) which
    // is the bottom of the bitmap, and characters render upside-down in
    // buffer memory. The GL path tolerates that because its texcoords use
    // V=1 at the top vertex and sample bottom-to-top; Metal's V=0-at-top
    // convention doesn't compensate. Apply a y-axis flip so text ends up
    // at the TOP-LEFT of the buffer, right-side-up, matching the texcoords
    // I use in CN1MetalDrawString below.
    CGContextTranslateCTM(ctx, 0, p2h);
    CGContextScaleCTM(ctx, 1, -1);
    UIGraphicsPushContext(ctx);
    UIColor *uiColor = [UIColor colorWithRed:((color >> 16) & 0xff)/255.0f
                                       green:((color >> 8)  & 0xff)/255.0f
                                        blue:((color)       & 0xff)/255.0f
                                       alpha:1.0f];
    [uiColor set];
    [str drawAtPoint:CGPointZero withAttributes:@{ NSFontAttributeName: font,
                                                   NSForegroundColorAttributeName: uiColor }];
    UIGraphicsPopContext();

    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) {
        CGContextRelease(ctx);
        free(bitmap);
        return NULL;
    }
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatRGBA8Unorm
        width:p2w height:p2h mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    id<MTLTexture> tex = [device newTextureWithDescriptor:desc];
    [tex replaceRegion:MTLRegionMake2D(0, 0, p2w, p2h)
           mipmapLevel:0
             withBytes:bitmap
           bytesPerRow:bytesPerRow];

    CGContextRelease(ctx);
    free(bitmap);

    // Cache with simple round-robin eviction.
    int slot;
    if (textCacheCount < CN1_METAL_TEXT_CACHE_MAX) {
        slot = textCacheCount++;
    } else {
        slot = textCacheNextEvict;
        textCacheNextEvict = (textCacheNextEvict + 1) % CN1_METAL_TEXT_CACHE_MAX;
        textCache[slot].key = nil;
        textCache[slot].texture = nil;
    }
    textCache[slot].key = [key copy];
    textCache[slot].texture = tex;
    textCache[slot].strWidth = w;
    textCache[slot].strHeight = h;
    textCache[slot].p2w = p2w;
    textCache[slot].p2h = p2h;
    return &textCache[slot];
}

void CN1MetalDrawString(NSString *str, UIFont *font, int color, int alpha, int x, int y) {
    if (str == nil || font == nil || str.length == 0) return;
    CN1MetalTextCacheEntry *entry = findOrBuildTextTexture(str, font, color);
    if (entry == NULL || entry->texture == nil) return;

    // Texture-coord region: the text occupies the top-left (w,h) of a
    // power-of-two bitmap. In Metal, tex (0,0) is the top-left, so we
    // sample the rectangle [0, 0] .. [w/p2w, h/p2h].
    float a = alpha / 255.0f;
    simd_float4 tint = (simd_float4){ a, a, a, a };
    float vertices[8] = {
        (float)x,                      (float)y,
        (float)(x + entry->strWidth),  (float)y,
        (float)x,                      (float)(y + entry->strHeight),
        (float)(x + entry->strWidth),  (float)(y + entry->strHeight)
    };
    float uMax = (float)entry->strWidth / (float)entry->p2w;
    float vMax = (float)entry->strHeight / (float)entry->p2h;
    float texcoords[8] = {
        0.0f, 0.0f,
        uMax, 0.0f,
        0.0f, vMax,
        uMax, vMax
    };
    drawQuad(CN1MetalPipelineTexturedRGBA, vertices, texcoords, tint, entry->texture);
}

// --------------- Gradient rendering ---------------

// Direct port of DrawGradient.m's GL path: rasterise the gradient via
// CGContextDrawLinearGradient / CGContextDrawRadialGradient into a
// CGBitmapContext, upload as MTLTexture, render as a textured quad.
// A small round-robin cache avoids re-rasterising the same gradient
// every frame (the same pattern as the text cache above).
//
// Cache size limit chosen to match the GL DrawGradientTextureCache's
// effective per-frame footprint without growing unbounded. A typical
// CN1 form has 2-5 distinct gradient backgrounds; 32 covers transitions.
#define CN1_METAL_GRAD_CACHE_MAX 32
typedef struct {
    int type;
    int startColor;
    int endColor;
    int width;
    int height;
    float relativeX;
    float relativeY;
    float relativeSize;
    int p2w;
    int p2h;
    id<MTLTexture> texture;
} CN1MetalGradCacheEntry;
static CN1MetalGradCacheEntry gradCache[CN1_METAL_GRAD_CACHE_MAX];
static int gradCacheCount = 0;
static int gradCacheNextEvict = 0;

static int nextPowerOf2ForGrad(int v) {
    int p = 1;
    while (p < v) p <<= 1;
    return p;
}

static CN1MetalGradCacheEntry *findOrBuildGradTexture(int type, int startColor, int endColor,
                                                     int width, int height,
                                                     float relativeX, float relativeY, float relativeSize) {
    for (int i = 0; i < gradCacheCount; i++) {
        CN1MetalGradCacheEntry *e = &gradCache[i];
        if (e->type == type && e->startColor == startColor && e->endColor == endColor
            && e->width == width && e->height == height
            && e->relativeX == relativeX && e->relativeY == relativeY && e->relativeSize == relativeSize) {
            return e;
        }
    }

    if (width <= 0 || height <= 0) return NULL;
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return NULL;

    int p2w = nextPowerOf2ForGrad(width);
    int p2h = nextPowerOf2ForGrad(height);

    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    size_t bytesPerRow = 4 * (size_t)p2w;
    void *bitmap = calloc((size_t)p2h * bytesPerRow, 1);
    CGContextRef ctx = CGBitmapContextCreate(bitmap, p2w, p2h, 8, bytesPerRow, colorSpace,
        kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    // Y-flip the CTM so display-row-0 lands at memory-row-0 (matches the
    // texture-from-UIImage path; with non-inverted texcoords on the draw
    // call, the gradient renders right-side-up).
    CGContextTranslateCTM(ctx, 0, p2h);
    CGContextScaleCTM(ctx, 1, -1);

    UIGraphicsPushContext(ctx);

    float alpha1 = 1.0f;
    if (((startColor >> 24) & 0xff) != 0) {
        alpha1 = ((float)((startColor >> 24) & 0xff)) / 255.0f;
    }
    float alpha2 = 1.0f;
    if (((endColor >> 24) & 0xff) != 0) {
        alpha2 = ((float)((endColor >> 24) & 0xff)) / 255.0f;
    }
    CGFloat components[8] = {
        ((float)((startColor >> 16) & 0xff)) / 255.0f,
        ((float)((startColor >> 8)  & 0xff)) / 255.0f,
        ((float)(startColor & 0xff)) / 255.0f,
        alpha1,
        ((float)((endColor >> 16) & 0xff)) / 255.0f,
        ((float)((endColor >> 8)  & 0xff)) / 255.0f,
        ((float)(endColor & 0xff)) / 255.0f,
        alpha2
    };
    CGFloat locations[2] = { 0.0, 1.0 };
    CGGradientRef grad = CGGradientCreateWithColorComponents(colorSpace, components, locations, 2);

    // Constants from DrawGradient.h — duplicated here so this file doesn't
    // need to import every op header. Keep in sync with DrawGradient.h.
    enum { GRAD_TYPE_RADIAL = 1, GRAD_TYPE_HORIZONTAL = 2, GRAD_TYPE_VERTICAL = 3 };

    switch (type) {
        case GRAD_TYPE_RADIAL: {
            UIColor *endC = [UIColor colorWithRed:((endColor >> 16) & 0xff) / 255.0f
                                            green:((endColor >> 8)  & 0xff) / 255.0f
                                             blue:((endColor)       & 0xff) / 255.0f
                                            alpha:alpha2];
            [endC set];
            CGContextFillRect(ctx, CGRectMake(0, 0, width, height));
            CGPoint centre = CGPointMake(relativeX * width, relativeY * height);
            float radius = MIN(width, height) * relativeSize;
            CGContextDrawRadialGradient(ctx, grad, centre, 0, centre, radius,
                                         kCGGradientDrawsAfterEndLocation);
            break;
        }
        case GRAD_TYPE_HORIZONTAL:
            CGContextDrawLinearGradient(ctx, grad,
                CGPointMake(0, 0), CGPointMake(width, 0),
                kCGGradientDrawsBeforeStartLocation | kCGGradientDrawsAfterEndLocation);
            break;
        case GRAD_TYPE_VERTICAL:
            CGContextDrawLinearGradient(ctx, grad,
                CGPointMake(0, 0), CGPointMake(0, height),
                kCGGradientDrawsBeforeStartLocation | kCGGradientDrawsAfterEndLocation);
            break;
    }

    UIGraphicsPopContext();
    CGGradientRelease(grad);
    CGColorSpaceRelease(colorSpace);

    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatRGBA8Unorm
        width:p2w height:p2h mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    id<MTLTexture> tex = [device newTextureWithDescriptor:desc];
    [tex replaceRegion:MTLRegionMake2D(0, 0, p2w, p2h)
           mipmapLevel:0
             withBytes:bitmap
           bytesPerRow:bytesPerRow];

    CGContextRelease(ctx);
    free(bitmap);

    int slot;
    if (gradCacheCount < CN1_METAL_GRAD_CACHE_MAX) {
        slot = gradCacheCount++;
    } else {
        slot = gradCacheNextEvict;
        gradCacheNextEvict = (gradCacheNextEvict + 1) % CN1_METAL_GRAD_CACHE_MAX;
        gradCache[slot].texture = nil;
    }
    gradCache[slot].type = type;
    gradCache[slot].startColor = startColor;
    gradCache[slot].endColor = endColor;
    gradCache[slot].width = width;
    gradCache[slot].height = height;
    gradCache[slot].relativeX = relativeX;
    gradCache[slot].relativeY = relativeY;
    gradCache[slot].relativeSize = relativeSize;
    gradCache[slot].p2w = p2w;
    gradCache[slot].p2h = p2h;
    gradCache[slot].texture = tex;
    return &gradCache[slot];
}

void CN1MetalDrawGradient(int type, int startColor, int endColor,
                          int x, int y, int width, int height,
                          float relativeX, float relativeY, float relativeSize) {
    CN1MetalGradCacheEntry *e = findOrBuildGradTexture(type, startColor, endColor,
                                                       width, height,
                                                       relativeX, relativeY, relativeSize);
    if (e == NULL || e->texture == nil) return;

    simd_float4 tint = (simd_float4){ 1.0f, 1.0f, 1.0f, 1.0f };
    float vertices[8] = {
        (float)x,           (float)y,
        (float)(x + width), (float)y,
        (float)x,           (float)(y + height),
        (float)(x + width), (float)(y + height)
    };
    float uMax = (float)width / (float)e->p2w;
    float vMax = (float)height / (float)e->p2h;
    float texcoords[8] = {
        0.0f, 0.0f,
        uMax, 0.0f,
        0.0f, vMax,
        uMax, vMax
    };
    drawQuad(CN1MetalPipelineTexturedRGBA, vertices, texcoords, tint, e->texture);
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

// --------------- Texture helpers ---------------

id<MTLTexture> CN1MetalTextureFromUIImage(UIImage *image) {
    if (image == nil) return nil;
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return nil;
    int w = (int)image.size.width * image.scale;
    int h = (int)image.size.height * image.scale;
    if (w <= 0 || h <= 0) return nil;

    // Rasterize UIImage into a CGBitmapContext then upload as MTLTexture.
    // CTM flip so display-row-0 lands at memory-row-0 — matches GLUIImage.
    // getTexture's pattern, and matches the text-cache path. Mutable images
    // (UIGraphicsGetImageFromCurrentImageContext output) and disk-loaded
    // UIImages both come through here right-side-up after this flip.
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    void *rawData = calloc(h * w * 4, sizeof(uint8_t));
    CGContextRef ctx = CGBitmapContextCreate(rawData, w, h, 8, w * 4, cs,
        kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGColorSpaceRelease(cs);
    CGContextTranslateCTM(ctx, 0, h);
    CGContextScaleCTM(ctx, 1, -1);
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

// --------------- Phase 3: mutable-image rendering ---------------

// CN1's iOS port can run startDrawingOnImage on a different thread than the
// actual draw JNI calls (CI diagnostic showed Begin on pthread A, drawQuad
// on pthread B). Per-thread state on the Begin thread is invisible to the
// drawing thread. The fix: store the encoder + dimensions + transform ON the
// active mutable image (a regular Obj-C object visible to all threads) and
// have each mutable JNI call hijack the calling thread's activeEncoder via
// CN1MetalEnterMutableScope / LeaveMutableScope.
//
// `_mutableActiveGl` is set globally by Begin and cleared by Finish. It's
// the same object as [CodenameOne_GLViewController instance].currentMutableImage
// but kept here for clarity and to avoid a UIView-touching round-trip from
// arbitrary threads.
static __unsafe_unretained GLUIImage *_mutableActiveGl = nil;
static int _mutableActiveWidth = 0;
static int _mutableActiveHeight = 0;

// Build a Y-down ortho projection for a (w x h) framebuffer. Mirrors
// METALView's CN1MetalOrtho call -- if that one ever changes (e.g. adds
// a half-pixel offset for pixel-perfect rendering), update this in lockstep.
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

BOOL CN1MetalBeginMutableImageDraw(int width, int height, void *peer) {
    if (peer == NULL || width <= 0 || height <= 0) return NO;
    NSLog(@"CN1SS:METAL_DIAG Begin enter thread=%p peer=%p w=%d h=%d _mutableActiveGl=%p",
          (void*)pthread_self(), peer, width, height, (__bridge void*)_mutableActiveGl);
    if (_mutableActiveGl != nil) {
        NSLog(@"CN1Metal: nested CN1MetalBeginMutableImageDraw -- forcing finish on previous peer");
        CN1MetalFinishMutableImageDraw((__bridge void *)_mutableActiveGl);
    }
    GLUIImage *gl = (__bridge GLUIImage *)peer;
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return NO;
    ensurePipelineCache();

    // Reset the mutable transform to identity at the start of every draw
    // pass (matches CG's behaviour: a fresh CGContext starts with identity
    // CTM). nativeSetTransformMutableImpl will overwrite as the user calls
    // setTransform.
    float *t = [gl mtlMutableTransformPtr];
    for (int r = 0; r < 4; r++)
        for (int c = 0; c < 4; c++)
            t[r*4 + c] = (r == c) ? 1.0f : 0.0f;

    // Allocate (or reuse) the mutable texture.
    id<MTLTexture> tex = [gl mtlMutableTexture];
    if (tex == nil || [gl mtlMutableWidth] != width || [gl mtlMutableHeight] != height) {
        MTLTextureDescriptor *desc = [MTLTextureDescriptor
            texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
            width:(NSUInteger)width height:(NSUInteger)height mipmapped:NO];
        desc.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead;
        desc.storageMode = MTLStorageModePrivate;
        tex = [device newTextureWithDescriptor:desc];
        if (tex == nil) return NO;
        // Clear new texture to transparent black -- matches
        // UIGraphicsBeginImageContextWithOptions(opaque=NO) behaviour.
        id<MTLCommandBuffer> clearCb = [CN1MetalCommandQueue() commandBuffer];
        MTLRenderPassDescriptor *clearPass = [MTLRenderPassDescriptor renderPassDescriptor];
        clearPass.colorAttachments[0].texture = tex;
        clearPass.colorAttachments[0].loadAction = MTLLoadActionClear;
        clearPass.colorAttachments[0].storeAction = MTLStoreActionStore;
        clearPass.colorAttachments[0].clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 0.0);
        [[clearCb renderCommandEncoderWithDescriptor:clearPass] endEncoding];
        [clearCb commit];
        [gl setMtlMutableTexture:tex width:width height:height];
        [gl setMtlMutableCommandBuffer:nil];
    }

    // Allocate a fresh command buffer for this draw pass. Each begin/finish
    // gets its own buffer (sync-on-finish: commit+wait in Finish). Crucially,
    // EnterMutableScope/LeaveMutableScope pulls the encoder OFF the GLUIImage
    // (not from per-thread state), so the actual draw thread doesn't need to
    // be the same as the Begin thread.
    id<MTLCommandQueue> queue = CN1MetalCommandQueue();
    if (queue == nil) return NO;
    id<MTLCommandBuffer> cb = [queue commandBuffer];
    if (cb == nil) return NO;
    [gl setMtlMutableCommandBuffer:cb];
    MTLRenderPassDescriptor *desc = [MTLRenderPassDescriptor renderPassDescriptor];
    desc.colorAttachments[0].texture = tex;
    desc.colorAttachments[0].loadAction = MTLLoadActionLoad;
    desc.colorAttachments[0].storeAction = MTLStoreActionStore;
    id<MTLRenderCommandEncoder> enc = [cb renderCommandEncoderWithDescriptor:desc];
    if (enc == nil) return NO;
    [enc setViewport:(MTLViewport){0.0, 0.0, (double)width, (double)height, 0.0, 1.0}];
    [gl setMtlMutableEncoder:enc];

    _mutableActiveGl = gl;
    _mutableActiveWidth = width;
    _mutableActiveHeight = height;
    NSLog(@"CN1SS:METAL_DIAG Begin OK thread=%p peer=%p enc=%p tex=%p",
          (void*)pthread_self(), (__bridge void*)gl, (__bridge void*)enc, (__bridge void*)tex);
    return YES;
}

void CN1MetalFinishMutableImageDraw(void *peer) {
    if (peer == NULL) return;
    GLUIImage *gl = (__bridge GLUIImage *)peer;
    NSLog(@"CN1SS:METAL_DIAG Finish thread=%p peer=%p _mutableActiveGl=%p match=%d",
          (void*)pthread_self(), peer, (__bridge void*)_mutableActiveGl, gl == _mutableActiveGl);
    if (gl != _mutableActiveGl) return;
    id<MTLRenderCommandEncoder> enc = [gl mtlMutableEncoder];
    if (enc != nil) {
        [enc endEncoding];
        [gl setMtlMutableEncoder:nil];
    }
    // Sync model: commit + wait so the texture has finalised pixels by the
    // time control returns to the caller. Matches GL CG path's semantics.
    id<MTLCommandBuffer> cb = [gl mtlMutableCommandBuffer];
    if (cb != nil) {
        [cb commit];
        [cb waitUntilCompleted];
        [gl setMtlMutableCommandBuffer:nil];
    }
    _mutableActiveGl = nil;
    _mutableActiveWidth = 0;
    _mutableActiveHeight = 0;
}

static int diagEnterCount = 0;
CN1MetalMutableScope CN1MetalEnterMutableScope(void) {
    CN1MetalMutableScope s = {0};
    GLUIImage *gl = _mutableActiveGl;
    int n = diagEnterCount++;
    if ((n % 200) == 0) {
        NSLog(@"CN1SS:METAL_DIAG Enter #%d thread=%p _mutableActiveGl=%p enc=%p",
              n, (void*)pthread_self(), (__bridge void*)gl,
              (__bridge void*)(gl ? [gl mtlMutableEncoder] : nil));
    }
    if (gl == nil) return s;
    id<MTLRenderCommandEncoder> mEnc = [gl mtlMutableEncoder];
    if (mEnc == nil) return s;

    // Save current thread's state into the scope.
    s._savedEnc = (__bridge void *)activeEncoder;
    memcpy(s._savedProj, &currentProjection, sizeof(simd_float4x4));
    s._savedFw = currentFramebufferWidth;
    s._savedFh = currentFramebufferHeight;
    memcpy(s._savedTransform, &currentTransform, sizeof(simd_float4x4));
    s._valid = YES;

    // Switch this thread's view to the mutable image's encoder + projection
    // + dims + transform.
    activeEncoder = mEnc;
    simd_float4x4 mp = mutableProjection(_mutableActiveWidth, _mutableActiveHeight);
    currentProjection = mp;
    currentFramebufferWidth = _mutableActiveWidth;
    currentFramebufferHeight = _mutableActiveHeight;
    float *t = [gl mtlMutableTransformPtr];
    memcpy(&currentTransform, t, sizeof(simd_float4x4));
    return s;
}

void CN1MetalLeaveMutableScope(CN1MetalMutableScope scope) {
    if (!scope._valid) return;
    activeEncoder = (__bridge id<MTLRenderCommandEncoder>)scope._savedEnc;
    memcpy(&currentProjection, scope._savedProj, sizeof(simd_float4x4));
    currentFramebufferWidth = scope._savedFw;
    currentFramebufferHeight = scope._savedFh;
    memcpy(&currentTransform, scope._savedTransform, sizeof(simd_float4x4));
}

void CN1MetalSetMutableImageTransform(const float matrix[16]) {
    GLUIImage *gl = _mutableActiveGl;
    if (gl == nil) return;
    float *t = [gl mtlMutableTransformPtr];
    memcpy(t, matrix, sizeof(float) * 16);
}

void CN1MetalFlushMutableImage(void *peer) {
    if (peer == NULL) return;
    GLUIImage *gl = (__bridge GLUIImage *)peer;
    // With sync-on-finish semantics, the cmd buffer is already committed and
    // waited by the time anyone calls flush. Defensive cleanup if a caller
    // reaches here with a stuck encoder anyway.
    id<MTLRenderCommandEncoder> enc = [gl mtlMutableEncoder];
    if (enc != nil) {
        [enc endEncoding];
        [gl setMtlMutableEncoder:nil];
    }
    id<MTLCommandBuffer> cb = [gl mtlMutableCommandBuffer];
    if (cb != nil) {
        [cb commit];
        [cb waitUntilCompleted];
        [gl setMtlMutableCommandBuffer:nil];
    }
}

BOOL CN1MetalIsMutableActive(void) {
    return _mutableActiveGl != nil;
}

void CN1MetalDrawCGRasterizedRect(int dx, int dy, int w, int h,
                                  void (^cgBlock)(CGContextRef ctx)) {
    if (w <= 0 || h <= 0 || cgBlock == NULL) return;
    id<MTLDevice> device = CN1MetalDevice();
    if (device == nil) return;

    // Build a CGBitmapContext sized to the geometry; orient with a CTM flip
    // so display-row-0 ends up at memory-row-0 (matches CN1MetalTextureFromUIImage
    // and the text-cache rasterisation convention).
    CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
    void *bytes = calloc((size_t)h * (size_t)w * 4, 1);
    CGContextRef ctx = CGBitmapContextCreate(bytes, w, h, 8, w * 4, cs,
        kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGColorSpaceRelease(cs);
    CGContextTranslateCTM(ctx, 0, h);
    CGContextScaleCTM(ctx, 1, -1);

    // UIKit-aware drawing inside the block (so [color set] on the supplied
    // ctx works as expected) -- push as the current UIGraphics context.
    UIGraphicsPushContext(ctx);
    cgBlock(ctx);
    UIGraphicsPopContext();

    // Upload as MTLTexture and draw through CN1MetalDrawImage. Goes through
    // the active encoder, which during a mutable draw is the mutable encoder.
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatRGBA8Unorm
        width:(NSUInteger)w height:(NSUInteger)h mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    id<MTLTexture> tex = [device newTextureWithDescriptor:desc];
    [tex replaceRegion:MTLRegionMake2D(0, 0, w, h)
           mipmapLevel:0
             withBytes:bytes
           bytesPerRow:w * 4];
    CGContextRelease(ctx);
    free(bytes);

    CN1MetalDrawImage(tex, 255, dx, dy, w, h);
}

BOOL CN1MetalReadMutableImagePixels(void *peer, int *outARGB,
                                    int x, int y, int w, int h,
                                    int imgWidth, int imgHeight) {
    (void)imgWidth; (void)imgHeight; // reserved for future scaling; matches GL signature
    if (peer == NULL || outARGB == NULL || w <= 0 || h <= 0) return NO;
    GLUIImage *gl = (__bridge GLUIImage *)peer;
    CN1MetalFlushMutableImage(peer);
    id<MTLTexture> tex = [gl mtlMutableTexture];
    if (tex == nil) return NO;
    int tw = (int)tex.width;
    int th = (int)tex.height;
    if (x < 0 || y < 0 || x + w > tw || y + h > th) return NO;

    id<MTLDevice> device = CN1MetalDevice();
    NSUInteger bytesPerRow = (NSUInteger)w * 4;
    id<MTLBuffer> staging = [device newBufferWithLength:bytesPerRow * (NSUInteger)h
                                                options:MTLResourceStorageModeShared];
    id<MTLCommandBuffer> cb = [CN1MetalCommandQueue() commandBuffer];
    id<MTLBlitCommandEncoder> blit = [cb blitCommandEncoder];
    [blit copyFromTexture:tex
              sourceSlice:0
              sourceLevel:0
             sourceOrigin:MTLOriginMake((NSUInteger)x, (NSUInteger)y, 0)
               sourceSize:MTLSizeMake((NSUInteger)w, (NSUInteger)h, 1)
                 toBuffer:staging
        destinationOffset:0
   destinationBytesPerRow:bytesPerRow
 destinationBytesPerImage:bytesPerRow * (NSUInteger)h];
    [blit endEncoding];
    [cb commit];
    [cb waitUntilCompleted];
    // Texture stores BGRA8Unorm: byte[0]=B byte[1]=G byte[2]=R byte[3]=A.
    // Java ARGB int packing: 0xAARRGGBB.
    uint8_t *src = (uint8_t *)[staging contents];
    for (int row = 0; row < h; row++) {
        for (int col = 0; col < w; col++) {
            int off = row * (int)bytesPerRow + col * 4;
            uint8_t b = src[off + 0];
            uint8_t gV = src[off + 1];
            uint8_t r = src[off + 2];
            uint8_t a = src[off + 3];
            outARGB[row * w + col] = ((int)a << 24) | ((int)r << 16) | ((int)gV << 8) | (int)b;
        }
    }
    return YES;
}

#endif /* CN1_USE_METAL */
