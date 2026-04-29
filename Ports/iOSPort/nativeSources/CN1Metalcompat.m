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

// Phase-2 fallback: same whole-string LRU rendering kept for the case where
// the Phase-4 glyph atlas can't be created (no MTLDevice etc.) so text
// always shows up even if the atlas path is unavailable.
static void drawStringWholeStringFallback(NSString *str, UIFont *font, int color, int alpha, int x, int y) {
    CN1MetalTextCacheEntry *entry = findOrBuildTextTexture(str, font, color);
    if (entry == NULL || entry->texture == nil) return;
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

void CN1MetalDrawString(NSString *str, UIFont *font, int color, int alpha, int x, int y) {
    if (str == nil || font == nil || str.length == 0) return;

    static int s_dbg = 0;
    if (s_dbg < 6) {
        NSLog(@"CN1SS:METAL_DIAG DrawString.enter #%d str=\"%@\"", s_dbg, str);
    }
    // Phase 4: shape the string with CTLine and emit one alpha-mask quad
    // per glyph against a per-(font, point-size) R8 atlas. Falls back to
    // the whole-string LRU path if the atlas can't be created (no
    // MTLDevice, CTFont creation failed, etc.) so text always renders.
    CN1MetalGlyphAtlas *atlas = [CN1MetalGlyphAtlas atlasForFont:font];
    if (s_dbg < 6) {
        NSLog(@"CN1SS:METAL_DIAG DrawString.atlasOk #%d ok=%d", s_dbg, atlas != nil);
        s_dbg++;
    }
    if (atlas == nil) {
        drawStringWholeStringFallback(str, font, color, alpha, x, y);
        return;
    }

    NSDictionary *attrs = @{ (__bridge NSString *)kCTFontAttributeName: (__bridge id)atlas.ctFont };
    CFAttributedStringRef attrStr = CFAttributedStringCreate(NULL,
                                                             (__bridge CFStringRef)str,
                                                             (__bridge CFDictionaryRef)attrs);
    if (attrStr == NULL) {
        drawStringWholeStringFallback(str, font, color, alpha, x, y);
        return;
    }
    CTLineRef line = CTLineCreateWithAttributedString(attrStr);
    CFRelease(attrStr);
    if (line == NULL) {
        drawStringWholeStringFallback(str, font, color, alpha, x, y);
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
    id<MTLCommandQueue> queue = CN1MetalCommandQueue();
    if (queue != nil) {
        id<MTLCommandBuffer> clearCb = [queue commandBuffer];
        MTLRenderPassDescriptor *clearPass = [MTLRenderPassDescriptor renderPassDescriptor];
        clearPass.colorAttachments[0].texture = tex;
        clearPass.colorAttachments[0].loadAction = MTLLoadActionClear;
        clearPass.colorAttachments[0].storeAction = MTLStoreActionStore;
        clearPass.colorAttachments[0].clearColor = MTLClearColorMake(r, g, b, a);
        [[clearCb renderCommandEncoderWithDescriptor:clearPass] endEncoding];
        [clearCb commit];
    }
    [image setMtlMutableTexture:tex width:width height:height];
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
    return YES;
}

#endif /* CN1_USE_METAL */
