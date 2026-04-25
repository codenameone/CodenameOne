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

#endif /* CN1_USE_METAL */
