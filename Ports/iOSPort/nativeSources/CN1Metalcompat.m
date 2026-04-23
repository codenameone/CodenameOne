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

// --------------- Public draw primitives ---------------

void CN1MetalFillRect(int color, int alpha, int x, int y, int width, int height) {
    float a = alpha / 255.0f;
    simd_float4 colorV = (simd_float4){
        ((color >> 16) & 0xff) / 255.0f * a,
        ((color >> 8)  & 0xff) / 255.0f * a,
        ((color)       & 0xff) / 255.0f * a,
        a
    };
    float vertices[8] = {
        (float)x,         (float)y,
        (float)(x+width), (float)y,
        (float)x,         (float)(y+height),
        (float)(x+width), (float)(y+height)
    };
    drawQuad(CN1MetalPipelineSolidColor, vertices, NULL, colorV, nil);
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
    // GL textures rendered in Codename One use inverted-Y texture coords
    // (matching the GL path's DrawImage textureCoordinates array).
    static const float texcoords[8] = {
        0, 1,
        1, 1,
        0, 0,
        1, 0
    };
    drawQuad(CN1MetalPipelineTexturedRGBA, vertices, texcoords, tint, texture);
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
    // Same pattern as GLUIImage.getTexture; eventually GLUIImage should
    // cache the MTLTexture so this isn't recomputed per draw.
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

#endif /* CN1_USE_METAL */
