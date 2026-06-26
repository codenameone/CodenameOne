/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

/*
 * The simulator's replacement for the device CodenameOne_GLViewController:
 * an NSObject (no UIKit) with the same class name and the selectors the port
 * sources use, so any compiled native source file referencing
 * [CodenameOne_GLViewController instance] works unmodified.
 *
 * Rendering model (mirrors METALView + the device drawFrame Metal path):
 *  - drawing natives append ExecutableOps to the upcoming queue
 *  - flushBuffer swaps the queues and drains ops into a persistent
 *    BGRA8 screen texture (load-action preserve, partial repaints work)
 *  - the frame ends with a blit from the screen texture to the
 *    CAMetalLayer's next drawable and a present
 *
 * Unlike the device (which dispatch_syncs to the UIKit main thread), the
 * simulator renders directly on the calling thread - the CN1 EDT -
 * because CAMetalLayer.nextDrawable and command encoding are thread-safe
 * and the AWT process owns the AppKit main thread.
 *
 * This file also provides:
 *  - the strong definitions of the M1 allowlist IOSNative_* symbols
 *    (drawing, clipping, fonts, lifecycle); everything else links against
 *    the generated weak stubs
 *  - the JNI natives of com.codename1.impl.ios.sim.CN1SimHost: JAWT
 *    surface attachment and resize handling
 */
#import <UIKit/UIKit.h>   /* the sim shim: AppKit + UIFont/UIImage compat */
#import <Metal/Metal.h>
#import <QuartzCore/CAMetalLayer.h>
#import <IOSurface/IOSurface.h>
#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>

#include "cn1jni_runtime.h"
#include "CN1Metalcompat.h"
#import "CodenameOne_GLViewController.h"   /* the sim shim header */
#import "ExecutableOp.h"
#import "FillRect.h"
#import "DrawLine.h"
#import "DrawString.h"
#import "ClipRect.h"
#import "DrawImage.h"
#import "GLUIImage.h"

/* referenced as extern by the ExecutableOp sources */
float scaleValue = 1;

/* per-universe draw confinement (defined in CN1Metalcompat.m, CN1_SIM_DESKTOP) */
extern void CN1SimSetUniverseClip(int x, int y, int w, int h);

/* texture sizing helper defined by the device view controller */
int nextPowerOf2(int val) {
    int v = 1;
    while (v < val) {
        v <<= 1;
    }
    return v;
}

/* display metrics published to the Java side */
static int cn1simDisplayWidth = 320;
static int cn1simDisplayHeight = 480;

static simd_float4x4 CN1SimOrtho(float left, float right, float bottom, float top, float nearZ, float farZ) {
    float ral = right + left;
    float rsl = right - left;
    float tab = top + bottom;
    float tsb = top - bottom;
    float fan = farZ + nearZ;
    float fsn = farZ - nearZ;
    simd_float4x4 m = (simd_float4x4) {{
        {2.0f / rsl, 0.0f, 0.0f, 0.0f},
        {0.0f, 2.0f / tsb, 0.0f, 0.0f},
        {0.0f, 0.0f, -2.0f / fsn, 0.0f},
        {-ral / rsl, -tab / tsb, -fan / fsn, 1.0f}
    }};
    return m;
}

/* class extension holding the rendering state (interface in the shim header) */
@interface CodenameOne_GLViewController () {
    NSMutableArray *currentTarget;
    NSMutableArray *upcomingTarget;
    /*
     * Presentation: a plain CALayer whose contents are double-buffered
     * IOSurfaces the GPU blits the screen texture into. CAMetalLayer
     * presentDrawable is NOT used - CoreAnimation silently drops drawable
     * presents (presentedTime stays 0) for sublayers of the AWT window's
     * layer tree, while IOSurface contents composite reliably.
     */
    CALayer *hostLayer;
    IOSurfaceRef presentSurfaces[2];
    id<MTLTexture> presentTextures[2];
    int presentIndex;
    id<MTLDevice> device;
    id<MTLCommandQueue> commandQueue;
    id<MTLTexture> screenTexture;
    id<MTLTexture> stencilTexture;
    id<MTLCommandBuffer> commandBuffer;
    id<MTLRenderCommandEncoder> renderCommandEncoder;
    MTLRenderPassDescriptor *renderPassDescriptor;
    simd_float4x4 projectionMatrix;
    int framebufferWidth;
    int framebufferHeight;
    /*
     * Screen overlay: the skin's screen-rectangle crop (transparent except
     * the rounded corner bezels) composited on top of the app's pixels at
     * the end of every frame, so the app screen respects the device's
     * rounded corners no matter which universe painted last.
     */
    GLUIImage *overlayImage;
    int overlayX, overlayY, overlayW, overlayH;
    int overlayClipX, overlayClipY, overlayClipW, overlayClipH;
}
@end

static CodenameOne_GLViewController *cn1simSharedSingleton = nil;

@implementation CodenameOne_GLViewController

+ (CodenameOne_GLViewController *)instance {
    if (cn1simSharedSingleton == nil) {
        cn1simSharedSingleton = [[CodenameOne_GLViewController alloc] init];
    }
    return cn1simSharedSingleton;
}

- (id)init {
    self = [super init];
    if (self != nil) {
        currentTarget = [[NSMutableArray alloc] init];
        upcomingTarget = [[NSMutableArray alloc] init];
        device = MTLCreateSystemDefaultDevice();
        commandQueue = [device newCommandQueue];
        CN1MetalSetDeviceAndCommandQueue(device, commandQueue);
    }
    return self;
}

static int cn1simDebug(void) {
    static int v = -1;
    if (v < 0) {
        v = getenv("CN1_SIM_DEBUG") != NULL ? 1 : 0;
    }
    return v;
}

- (void)attachLayer:(CALayer *)layer width:(int)w height:(int)h {
    @synchronized (self) {
        hostLayer = [layer retain];
        /* JAWT clients size their own layer; without a frame the layer is
         * zero-sized and nothing shows */
        hostLayer.frame = CGRectMake(0, 0, w, h);
        hostLayer.contentsScale = 1.0;
        hostLayer.contentsGravity = kCAGravityTopLeft;
        [self resizeSurface:w height:h];
        if (cn1simDebug()) {
            fprintf(stderr, "cn1sim: attachLayer %dx%d device=%s\n", w, h,
                    device != nil ? [[device name] UTF8String] : "nil");
        }
    }
}

- (void)createPresentSurfaces:(int)w height:(int)h {
    for (int i = 0; i < 2; i++) {
        if (presentSurfaces[i] != NULL) {
            CFRelease(presentSurfaces[i]);
            presentSurfaces[i] = NULL;
        }
        [presentTextures[i] release];
        presentTextures[i] = nil;
    }
    NSDictionary *props = @{
        (id) kIOSurfaceWidth : @(w),
        (id) kIOSurfaceHeight : @(h),
        (id) kIOSurfaceBytesPerElement : @4,
        (id) kIOSurfacePixelFormat : @((uint32_t) 'BGRA'),
    };
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
            texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
                                         width:w height:h mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead | MTLTextureUsageRenderTarget;
    desc.storageMode = MTLStorageModeShared;
    for (int i = 0; i < 2; i++) {
        presentSurfaces[i] = IOSurfaceCreate((CFDictionaryRef) props);
        presentTextures[i] = [device newTextureWithDescriptor:desc
                                                    iosurface:presentSurfaces[i]
                                                        plane:0];
    }
}

- (void)resizeSurface:(int)w height:(int)h {
    if (w <= 0 || h <= 0) {
        return;
    }
    @synchronized (self) {
        cn1simDisplayWidth = w;
        cn1simDisplayHeight = h;
        framebufferWidth = w;
        framebufferHeight = h;
        [self createPresentSurfaces:w height:h];
        projectionMatrix = CN1SimOrtho(0.0f, (float) w, (float) h, 0.0f, -1.0f, 1.0f);

        MTLTextureDescriptor *desc = [MTLTextureDescriptor
                texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
                                             width:w height:h mipmapped:NO];
        desc.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead;
        desc.storageMode = MTLStorageModePrivate;
        id<MTLTexture> newScreen = [device newTextureWithDescriptor:desc];

        MTLTextureDescriptor *stencilDesc = [MTLTextureDescriptor
                texture2DDescriptorWithPixelFormat:MTLPixelFormatStencil8
                                             width:w height:h mipmapped:NO];
        stencilDesc.usage = MTLTextureUsageRenderTarget;
        stencilDesc.storageMode = MTLStorageModePrivate;
        id<MTLTexture> newStencil = [device newTextureWithDescriptor:stencilDesc];

        /* clear the new screen texture to the shell chrome color so resizes
         * (rotation, skin switch) do not flash white before CN1 repaints */
        id<MTLCommandBuffer> clearCb = [commandQueue commandBuffer];
        MTLRenderPassDescriptor *clearPass = [MTLRenderPassDescriptor renderPassDescriptor];
        clearPass.colorAttachments[0].texture = newScreen;
        clearPass.colorAttachments[0].loadAction = MTLLoadActionClear;
        clearPass.colorAttachments[0].storeAction = MTLStoreActionStore;
        clearPass.colorAttachments[0].clearColor =
                MTLClearColorMake(0x2b / 255.0, 0x2b / 255.0, 0x2b / 255.0, 1);
        id<MTLRenderCommandEncoder> clearEnc = [clearCb renderCommandEncoderWithDescriptor:clearPass];
        [clearEnc endEncoding];
        [clearCb commit];

        [screenTexture release];
        screenTexture = newScreen;
        [stencilTexture release];
        stencilTexture = newStencil;
    }
}

/**
 * True once the layer is attached and the screen texture exists - flushes
 * before this point would drop their ops.
 */
- (BOOL)surfaceReady {
    @synchronized (self) {
        return hostLayer != nil && screenTexture != nil;
    }
}

/**
 * Writes the most recently presented frame to a PNG file. Used by headless
 * verification - no AWT Robot exists in pure mode. A non-positive crop width
 * or height saves the full frame ("Screenshot With Skin"); a real rectangle
 * crops to it ("Screenshot" = the app screen only).
 */
- (BOOL)saveScreenshotPNG:(NSString *)path cropX:(int)cx cropY:(int)cy cropW:(int)cw cropH:(int)ch {
    @synchronized (self) {
        CGImageRef img = NULL;
        IOSurfaceRef lockedSurf = NULL;
        id<MTLBuffer> readback = nil;
        if (cw > 0 && ch > 0) {
            /* cropped (app-screen) shot: read the SCREEN TEXTURE, which is
             * pre-overlay - classic square screenshot semantics, no rounded
             * corner bezel baked in */
            if (screenTexture == nil) {
                return NO;
            }
            int w = framebufferWidth, h = framebufferHeight;
            readback = [device newBufferWithLength:(NSUInteger) w * h * 4
                                           options:MTLResourceStorageModeShared];
            id<MTLCommandBuffer> cb = [commandQueue commandBuffer];
            id<MTLBlitCommandEncoder> b = [cb blitCommandEncoder];
            [b copyFromTexture:screenTexture
                   sourceSlice:0 sourceLevel:0
                  sourceOrigin:MTLOriginMake(0, 0, 0)
                    sourceSize:MTLSizeMake(w, h, 1)
                      toBuffer:readback
             destinationOffset:0
        destinationBytesPerRow:(NSUInteger) w * 4
      destinationBytesPerImage:(NSUInteger) w * h * 4];
            [b endEncoding];
            [cb commit];
            [cb waitUntilCompleted];
            CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
            CGContextRef ctx = CGBitmapContextCreateWithData([readback contents], w, h, 8,
                    (size_t) w * 4, cs,
                    kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little, NULL, NULL);
            CGColorSpaceRelease(cs);
            if (ctx != NULL) {
                CGImageRef full = CGBitmapContextCreateImage(ctx);
                CGContextRelease(ctx);
                img = CGImageCreateWithImageInRect(full, CGRectMake(cx, cy, cw, ch));
                CGImageRelease(full);
            }
        } else {
            /* full-window shot: the presented frame, bezel overlay included */
            int last = (presentIndex + 1) % 2;
            IOSurfaceRef surf = presentSurfaces[last];
            if (surf == NULL) {
                return NO;
            }
            IOSurfaceLock(surf, kIOSurfaceLockReadOnly, NULL);
            lockedSurf = surf;
            size_t w = IOSurfaceGetWidth(surf);
            size_t h = IOSurfaceGetHeight(surf);
            size_t stride = IOSurfaceGetBytesPerRow(surf);
            void *base = IOSurfaceGetBaseAddress(surf);
            CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
            CGContextRef ctx = CGBitmapContextCreateWithData(base, w, h, 8, stride, cs,
                    kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little, NULL, NULL);
            CGColorSpaceRelease(cs);
            if (ctx != NULL) {
                img = CGBitmapContextCreateImage(ctx);
                CGContextRelease(ctx);
            }
        }
        BOOL ok = NO;
        if (img != NULL) {
            NSURL *url = [NSURL fileURLWithPath:path];
            CGImageDestinationRef dest = CGImageDestinationCreateWithURL(
                    (CFURLRef) url, CFSTR("public.png"), 1, NULL);
            if (dest != NULL) {
                CGImageDestinationAddImage(dest, img, NULL);
                ok = CGImageDestinationFinalize(dest);
                CFRelease(dest);
            }
            CGImageRelease(img);
        }
        if (lockedSurf != NULL) {
            IOSurfaceUnlock(lockedSurf, kIOSurfaceLockReadOnly, NULL);
        }
        [readback release];
        return ok;
    }
}

- (void)upcomingAdd:(ExecutableOp *)op {
    @synchronized (self) {
        [upcomingTarget addObject:op];
    }
}

- (void)upcomingAddClip:(ExecutableOp *)op {
    @synchronized (self) {
        NSUInteger count = [upcomingTarget count];
        if (count > 0 && [[upcomingTarget objectAtIndex:count - 1] isKindOfClass:[ClipRect class]]) {
            [upcomingTarget replaceObjectAtIndex:count - 1 withObject:op];
        } else {
            [upcomingTarget addObject:op];
        }
    }
}

- (void)createRenderPassDescriptor {
    if (screenTexture == nil) {
        renderPassDescriptor = nil;
        return;
    }
    renderPassDescriptor = [MTLRenderPassDescriptor renderPassDescriptor];
    MTLRenderPassColorAttachmentDescriptor *colorAttachment = renderPassDescriptor.colorAttachments[0];
    colorAttachment.texture = screenTexture;
    colorAttachment.loadAction = MTLLoadActionLoad;
    colorAttachment.storeAction = MTLStoreActionStore;
    if (stencilTexture != nil) {
        MTLRenderPassStencilAttachmentDescriptor *stencilAttachment = renderPassDescriptor.stencilAttachment;
        stencilAttachment.texture = stencilTexture;
        stencilAttachment.loadAction = MTLLoadActionClear;
        stencilAttachment.storeAction = MTLStoreActionDontCare;
        stencilAttachment.clearStencil = 0;
    }
}

- (void)setFramebuffer {
    if (renderCommandEncoder != nil) {
        return;
    }
    commandBuffer = [commandQueue commandBuffer];
    [self createRenderPassDescriptor];
    if (renderPassDescriptor == nil) {
        renderCommandEncoder = nil;
        return;
    }
    renderCommandEncoder = [commandBuffer renderCommandEncoderWithDescriptor:renderPassDescriptor];
    [renderCommandEncoder setViewport:(MTLViewport) {0.0, 0.0,
            (double) framebufferWidth, (double) framebufferHeight, 0.0, 1.0}];
    CN1MetalBeginFrame(renderCommandEncoder, projectionMatrix, framebufferWidth, framebufferHeight);
}

- (void)presentFramebuffer {
    if (renderCommandEncoder == nil) {
        commandBuffer = nil;
        return;
    }
    CN1MetalEndFrame();
    [renderCommandEncoder endEncoding];
    renderCommandEncoder = nil;
    renderPassDescriptor = nil;

    /* CN1_SIM_DUMP: copy the screen texture to CPU memory and report pixel
     * stats - splits "ops did not render" from "compositing failed" */
    if (getenv("CN1_SIM_DUMP") != NULL) {
        int w = framebufferWidth, h = framebufferHeight;
        id<MTLBuffer> readback = [device newBufferWithLength:(NSUInteger) w * h * 4
                                                     options:MTLResourceStorageModeShared];
        id<MTLBlitCommandEncoder> rb = [commandBuffer blitCommandEncoder];
        [rb copyFromTexture:screenTexture
                sourceSlice:0 sourceLevel:0
               sourceOrigin:MTLOriginMake(0, 0, 0)
                 sourceSize:MTLSizeMake(w, h, 1)
                   toBuffer:readback
          destinationOffset:0
     destinationBytesPerRow:(NSUInteger) w * 4
   destinationBytesPerImage:(NSUInteger) w * h * 4];
        [rb endEncoding];
        [commandBuffer addCompletedHandler:^(id<MTLCommandBuffer> cb) {
            const unsigned int *px = (const unsigned int *) [readback contents];
            unsigned int first = px[0], mid = px[(h / 2) * w + w / 2];
            int distinct = 0;
            unsigned int seen[64];
            for (int i = 0; i < w * h; i += 997) {
                unsigned int v = px[i];
                int found = 0;
                for (int s = 0; s < distinct && s < 64; s++) {
                    if (seen[s] == v) {
                        found = 1;
                        break;
                    }
                }
                if (!found && distinct < 64) {
                    seen[distinct++] = v;
                }
            }
            fprintf(stderr, "cn1sim: frame dump %dx%d first=%08x mid=%08x distinct>=%d\n",
                    w, h, first, mid, distinct);
            [readback release];
        }];
    }
    /*
     * Blit the screen texture into the next IOSurface and assign it as the
     * host layer's contents from the main thread. The GPU work completes
     * before the assignment (waitUntilCompleted) so the surface holds the
     * full frame when CoreAnimation picks it up.
     */
    if (hostLayer != nil && presentTextures[presentIndex] != nil) {
        int idx = presentIndex;
        presentIndex = (presentIndex + 1) % 2;
        id<MTLBlitCommandEncoder> blit = [commandBuffer blitCommandEncoder];
        [blit copyFromTexture:screenTexture
                  sourceSlice:0 sourceLevel:0
                 sourceOrigin:MTLOriginMake(0, 0, 0)
                   sourceSize:MTLSizeMake(framebufferWidth, framebufferHeight, 1)
                    toTexture:presentTextures[idx]
             destinationSlice:0 destinationLevel:0
            destinationOrigin:MTLOriginMake(0, 0, 0)];
        [blit endEncoding];
        /*
         * The skin's rounded-corner bezel is composited into the PRESENTED
         * frame only - the screen texture itself stays square so the
         * Screenshot command keeps the classic square semantics.
         */
        if (overlayImage != nil) {
            MTLRenderPassDescriptor *opass = [MTLRenderPassDescriptor renderPassDescriptor];
            opass.colorAttachments[0].texture = presentTextures[idx];
            opass.colorAttachments[0].loadAction = MTLLoadActionLoad;
            opass.colorAttachments[0].storeAction = MTLStoreActionStore;
            opass.stencilAttachment.texture = stencilTexture;
            opass.stencilAttachment.loadAction = MTLLoadActionClear;
            opass.stencilAttachment.storeAction = MTLStoreActionDontCare;
            opass.stencilAttachment.clearStencil = 0;
            id<MTLRenderCommandEncoder> oenc =
                    [commandBuffer renderCommandEncoderWithDescriptor:opass];
            [oenc setViewport:(MTLViewport) {0.0, 0.0,
                    (double) framebufferWidth, (double) framebufferHeight, 0.0, 1.0}];
            CN1MetalBeginFrame(oenc, projectionMatrix, framebufferWidth, framebufferHeight);
            /* the overlay pass is not part of any universe's batch */
            CN1SimSetUniverseClip(0, 0, 0x7fffffff, 0x7fffffff);
            ClipRect *clip = [[ClipRect alloc] initWithArgs:overlayClipX ypos:overlayClipY
                                                          w:overlayClipW h:overlayClipH f:YES];
            [clip executeWithClipping];
            [clip release];
            DrawImage *di = [[DrawImage alloc] initWithArgs:255 xpos:overlayX ypos:overlayY
                                                          i:overlayImage w:overlayW h:overlayH];
            [di executeWithClipping];
            [di release];
            CN1MetalEndFrame();
            [oenc endEncoding];
        }
        [commandBuffer commit];
        [commandBuffer waitUntilCompleted];
        commandBuffer = nil;

        /* the block runs after this frame returns - it must own its surface
         * reference or a concurrent resizeSurface frees it first (rotate) */
        CALayer *layer = hostLayer;
        IOSurfaceRef surf = (IOSurfaceRef) CFRetain(presentSurfaces[idx]);
        dispatch_async(dispatch_get_main_queue(), ^{
            layer.contents = (id) surf;
            CFRelease(surf);
        });
    } else {
        [commandBuffer commit];
        commandBuffer = nil;
    }
}

- (void)drawFrame {
    /* the whole frame holds the lock so resizeSurface can't swap the screen /
     * present textures mid-encode (rotate during a flush) */
    @synchronized (self) {
        NSArray *cp = [currentTarget copy];
        [currentTarget removeAllObjects];
        [self setFramebuffer];
        if (renderCommandEncoder == nil) {
            if (cn1simDebug()) {
                fprintf(stderr, "cn1sim: drawFrame skipped (no encoder), ops=%lu\n",
                        (unsigned long) [cp count]);
            }
            [cp release];
            return;
        }
        if (cn1simDebug()) {
            fprintf(stderr, "cn1sim: drawFrame ops=%lu\n", (unsigned long) [cp count]);
        }
        for (ExecutableOp *ex in cp) {
            /* M1 renders to the screen only; mutable-image targets come with M2 */
            [ex executeWithClipping];
        }
        [cp release];
        [self presentFramebuffer];
    }
}

/**
 * Installs (or clears, peer==0) the screen overlay: the skin's screen-rect
 * crop drawn on top of the app's pixels every frame.
 */
- (void)setScreenOverlay:(GLUIImage *)img x:(int)x y:(int)y w:(int)w h:(int)h
                   clipX:(int)cx clipY:(int)cy clipW:(int)cw clipH:(int)ch {
    @synchronized (self) {
        [img retain];
        [overlayImage release];
        overlayImage = img;
        overlayX = x;
        overlayY = y;
        overlayW = w;
        overlayH = h;
        overlayClipX = cx;
        overlayClipY = cy;
        overlayClipW = cw;
        overlayClipH = ch;
    }
}

- (void)flushBuffer:(UIImage *)buff x:(int)x y:(int)y width:(int)width height:(int)height {
    @autoreleasepool {
        @synchronized (self) {
            if ([currentTarget count] > 0) {
                [currentTarget addObjectsFromArray:upcomingTarget];
                [upcomingTarget removeAllObjects];
            } else {
                NSMutableArray *tmp = currentTarget;
                currentTarget = upcomingTarget;
                upcomingTarget = tmp;
            }
        }
        [self drawFrame];
    }
}

@end

/* ---- VM lifecycle hooks referenced by other compiled sources --------------- */

void initVMImpl(void) {
    /* the device version allocates an autorelease pool for the calling
     * thread; cn1jni shims run inside their own autoreleasepool scopes */
}

void deinitVMImpl(void) {
}

/* ---- strong IOSNative_* implementations (M1 allowlist) -------------------- */

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isMetalRendering___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject) {
    return JAVA_TRUE;
}

void com_codename1_impl_ios_IOSNative_initVM__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject) {
    initVMImpl();
    /* make sure the controller (and the Metal device/queue) exist */
    [CodenameOne_GLViewController instance];
}

void com_codename1_impl_ios_IOSNative_deinitializeVM__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject) {
    deinitVMImpl();
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayWidth___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject) {
    return cn1simDisplayWidth;
}

JAVA_INT com_codename1_impl_ios_IOSNative_getDisplayHeight___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject) {
    return cn1simDisplayHeight;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isTablet___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isRunningOnMac___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject) {
    /* the simulator emulates an iPhone, not a Catalyst app */
    return JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_getUserAgentString___java_lang_String_R_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_OBJECT callbackId) {
    /* the device implementation resolves this from a WKWebView and may
     * deliver it via an async callback; returning a value directly keeps the
     * Java side synchronous */
    @autoreleasepool {
        return fromNSString(threadStateData,
                @"Mozilla/5.0 (iPhone; CPU iPhone OS like Mac OS X) AppleWebKit (KHTML, like Gecko) Mobile CN1Simulator");
    }
}

void com_codename1_impl_ios_IOSNative_nativeFillRectGlobal___int_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_INT color, JAVA_INT alpha, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    @autoreleasepool {
        FillRect *f = [[FillRect alloc] initWithArgs:color a:alpha xpos:x ypos:y w:w h:h];
        [[CodenameOne_GLViewController instance] upcomingAdd:f];
        [f release];
    }
}

void com_codename1_impl_ios_IOSNative_nativeDrawLineGlobal___int_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_INT color, JAVA_INT alpha, JAVA_INT x1, JAVA_INT y1, JAVA_INT x2, JAVA_INT y2) {
    @autoreleasepool {
        DrawLine *d = [[DrawLine alloc] initWithArgs:color a:alpha xpos1:x1 ypos1:y1 xpos2:x2 ypos2:y2];
        [[CodenameOne_GLViewController instance] upcomingAdd:d];
        [d release];
    }
}

void com_codename1_impl_ios_IOSNative_nativeDrawStringGlobal___int_int_long_java_lang_String_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_INT color, JAVA_INT alpha, JAVA_LONG fontPeer, JAVA_OBJECT str, JAVA_INT x, JAVA_INT y) {
    @autoreleasepool {
        NSString *s = toNSString(threadStateData, str);
        if (s == nil) {
            return;
        }
        DrawString *d = [[DrawString alloc] initWithArgs:color a:alpha xpos:x ypos:y
                                                       s:s f:(UIFont *) (intptr_t) fontPeer];
        [[CodenameOne_GLViewController instance] upcomingAdd:d];
        [d release];
    }
}

void com_codename1_impl_ios_IOSNative_setNativeClippingGlobal___int_int_int_int_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_BOOLEAN firstClip) {
    @autoreleasepool {
        ClipRect *c = [[ClipRect alloc] initWithArgs:x ypos:y w:w h:h f:firstClip];
        [[CodenameOne_GLViewController instance] upcomingAddClip:c];
        [c release];
    }
}

/*
 * Ordered op installing a universe's window-region clip floor: drains in
 * queue order so the bound applies exactly to the ops that follow it, even
 * when batches from both universes end up in one frame.
 */
@interface CN1SimUniverseClipOp : ExecutableOp {
    int ux, uy, uw, uh;
}
- (id)initWithArgs:(int)x y:(int)y w:(int)w h:(int)h;
@end

@implementation CN1SimUniverseClipOp

- (id)initWithArgs:(int)x y:(int)y w:(int)w h:(int)h {
    self = [super init];
    ux = x;
    uy = y;
    uw = w;
    uh = h;
    return self;
}

- (void)execute {
    CN1SimSetUniverseClip(ux, uy, uw, uh);
}

@end

void com_codename1_impl_ios_IOSNative_setUniverseClipGlobal___int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    @autoreleasepool {
        CN1SimUniverseClipOp *op = [[CN1SimUniverseClipOp alloc] initWithArgs:x y:y w:w h:h];
        [[CodenameOne_GLViewController instance] upcomingAdd:op];
        [op release];
    }
}

/*
 * One-shot shape fill/stroke: rasterizes a GeneralPath (PathIterator
 * commands + coords) through CoreGraphics into a transparent bitmap and
 * queues it as an image draw. Powers fillShape/drawShape - and with them
 * RoundRectBorder, the themed dialogs, buttons and text field borders.
 */
void com_codename1_impl_ios_IOSNative_nativeShapeGlobalSim___byte_1ARRAY_int_float_1ARRAY_int_int_int_boolean_float_int_int_float_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_OBJECT commands, JAVA_INT commandsLen, JAVA_OBJECT points, JAVA_INT pointsLen,
        JAVA_INT color, JAVA_INT alpha, JAVA_BOOLEAN stroke, JAVA_FLOAT lineWidth,
        JAVA_INT capStyle, JAVA_INT joinStyle, JAVA_FLOAT miterLimit,
        JAVA_INT translateX, JAVA_INT translateY) {
    @autoreleasepool {
        const JAVA_ARRAY_BYTE *cmd = (const JAVA_ARRAY_BYTE *) ((JAVA_ARRAY) commands)->data;
        const JAVA_ARRAY_FLOAT *pts = (const JAVA_ARRAY_FLOAT *) ((JAVA_ARRAY) points)->data;
        CGMutablePathRef path = CGPathCreateMutable();
        int p = 0;
        for (int i = 0; i < commandsLen; i++) {
            switch (cmd[i]) {
                case 0: /* SEG_MOVETO */
                    CGPathMoveToPoint(path, NULL, pts[p], pts[p + 1]);
                    p += 2;
                    break;
                case 1: /* SEG_LINETO */
                    CGPathAddLineToPoint(path, NULL, pts[p], pts[p + 1]);
                    p += 2;
                    break;
                case 2: /* SEG_QUADTO */
                    CGPathAddQuadCurveToPoint(path, NULL, pts[p], pts[p + 1],
                            pts[p + 2], pts[p + 3]);
                    p += 4;
                    break;
                case 3: /* SEG_CUBICTO */
                    CGPathAddCurveToPoint(path, NULL, pts[p], pts[p + 1],
                            pts[p + 2], pts[p + 3], pts[p + 4], pts[p + 5]);
                    p += 6;
                    break;
                case 4: /* SEG_CLOSE */
                    CGPathCloseSubpath(path);
                    break;
                default:
                    break;
            }
        }
        CGRect bounds = CGPathGetPathBoundingBox(path);
        CGFloat pad = stroke ? MAX(1.0f, lineWidth) : 1.0f;
        bounds = CGRectIntegral(CGRectInset(bounds, -pad, -pad));
        int w = (int) bounds.size.width;
        int h = (int) bounds.size.height;
        if (w <= 0 || h <= 0 || w > 8192 || h > 8192) {
            CGPathRelease(path);
            return;
        }
        CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
        CGContextRef ctx = CGBitmapContextCreate(NULL, w, h, 8, (size_t) w * 4, cs,
                kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little);
        CGColorSpaceRelease(cs);
        if (ctx == NULL) {
            CGPathRelease(path);
            return;
        }
        /* flip to a top-left origin so CN1 path coords land upright */
        CGContextTranslateCTM(ctx, 0, h);
        CGContextScaleCTM(ctx, 1, -1);
        CGContextTranslateCTM(ctx, -bounds.origin.x, -bounds.origin.y);
        CGFloat r = ((color >> 16) & 0xff) / 255.0;
        CGFloat g2 = ((color >> 8) & 0xff) / 255.0;
        CGFloat b = (color & 0xff) / 255.0;
        CGFloat a = alpha / 255.0;
        CGContextAddPath(ctx, path);
        if (stroke) {
            CGContextSetRGBStrokeColor(ctx, r, g2, b, a);
            CGContextSetLineWidth(ctx, MAX(0.5f, lineWidth));
            CGContextSetLineCap(ctx, capStyle == 1 ? kCGLineCapRound
                    : capStyle == 2 ? kCGLineCapSquare : kCGLineCapButt);
            CGContextSetLineJoin(ctx, joinStyle == 1 ? kCGLineJoinRound
                    : joinStyle == 2 ? kCGLineJoinBevel : kCGLineJoinMiter);
            CGContextSetMiterLimit(ctx, miterLimit);
            CGContextStrokePath(ctx);
        } else {
            CGContextSetRGBFillColor(ctx, r, g2, b, a);
            CGContextFillPath(ctx);
        }
        CGPathRelease(path);
        CGImageRef cg = CGBitmapContextCreateImage(ctx);
        CGContextRelease(ctx);
        if (cg == NULL) {
            return;
        }
        UIImage *img = [UIImage imageWithCGImage:cg];
        CGImageRelease(cg);
        GLUIImage *glimg = [[GLUIImage alloc] initWithImage:img];
        DrawImage *di = [[DrawImage alloc]
                initWithArgs:255
                        xpos:(int) bounds.origin.x + translateX
                        ypos:(int) bounds.origin.y + translateY
                           i:glimg w:w h:h];
        [[CodenameOne_GLViewController instance] upcomingAdd:di];
        [di release];
        [glimg release];
    }
}

void com_codename1_impl_ios_IOSNative_flushBuffer___long_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_LONG peer, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h) {
    [[CodenameOne_GLViewController instance] flushBuffer:nil x:x y:y width:w height:h];
}

/* ---- fonts over NSFont (peer = retained NSFont*) ---------------------------- */

JAVA_LONG com_codename1_impl_ios_IOSNative_createSystemFont___int_int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_INT face, JAVA_INT style, JAVA_INT size) {
    @autoreleasepool {
        int pSize = 14;
        if (size == 8) {       /* Font.SIZE_SMALL */
            pSize = 11;
        } else if (size == 16) { /* Font.SIZE_LARGE */
            pSize = 20;
        }
        NSFont *fnt;
        if ((face & 32) == 32) {        /* FACE_MONOSPACE */
            fnt = [NSFont userFixedPitchFontOfSize:pSize];
        } else if ((style & 1) == 1) {  /* STYLE_BOLD */
            fnt = [NSFont boldSystemFontOfSize:pSize];
        } else if ((style & 2) == 2) {  /* STYLE_ITALIC */
            fnt = [[NSFontManager sharedFontManager]
                    convertFont:[NSFont systemFontOfSize:pSize]
                    toHaveTrait:NSItalicFontMask];
        } else {
            fnt = [NSFont systemFontOfSize:pSize];
        }
        [fnt retain];
        return (JAVA_LONG) (intptr_t) fnt;
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_createTruetypeFont___java_lang_String_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_OBJECT name) {
    @autoreleasepool {
        NSString *n = toNSString(threadStateData, name);
        NSFont *fnt = nil;
        if (n != nil && ![n hasPrefix:@"native:"]) {
            fnt = [NSFont fontWithName:n size:14];
        }
        if (fnt == nil) {
            /* native:MainRegular and friends map to the system font family;
             * weights are applied in deriveTruetypeFont */
            fnt = [NSFont systemFontOfSize:14];
        }
        [fnt retain];
        return (JAVA_LONG) (intptr_t) fnt;
    }
}

JAVA_LONG com_codename1_impl_ios_IOSNative_deriveTruetypeFont___long_boolean_boolean_float_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject,
        JAVA_LONG peer, JAVA_BOOLEAN bold, JAVA_BOOLEAN italic, JAVA_FLOAT size) {
    @autoreleasepool {
        NSFont *base = (NSFont *) (intptr_t) peer;
        NSFont *fnt = base != nil ? [base fontWithSize:size] : [NSFont systemFontOfSize:size];
        NSFontManager *fm = [NSFontManager sharedFontManager];
        if (bold) {
            fnt = [fm convertFont:fnt toHaveTrait:NSBoldFontMask];
        }
        if (italic) {
            fnt = [fm convertFont:fnt toHaveTrait:NSItalicFontMask];
        }
        [fnt retain];
        return (JAVA_LONG) (intptr_t) fnt;
    }
}

JAVA_INT com_codename1_impl_ios_IOSNative_stringWidthNative___long_java_lang_String_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_OBJECT str) {
    @autoreleasepool {
        NSFont *f = (NSFont *) (intptr_t) peer;
        NSString *s = toNSString(threadStateData, str);
        if (f == nil || s == nil || [s length] == 0) {
            return 0;
        }
        return (JAVA_INT) [s sizeWithFont:f].width;
    }
}

JAVA_INT com_codename1_impl_ios_IOSNative_charWidthNative___long_char_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer, JAVA_CHAR ch) {
    @autoreleasepool {
        NSFont *f = (NSFont *) (intptr_t) peer;
        if (f == nil) {
            return 0;
        }
        unichar c = (unichar) ch;
        NSString *s = [NSString stringWithCharacters:&c length:1];
        return (JAVA_INT) [s sizeWithFont:f].width;
    }
}

JAVA_INT com_codename1_impl_ios_IOSNative_getFontHeightNative___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    @autoreleasepool {
        NSFont *f = (NSFont *) (intptr_t) peer;
        if (f == nil) {
            return 0;
        }
        return (JAVA_INT) ceil([f ascender] - [f descender] + [f leading]);
    }
}

JAVA_INT com_codename1_impl_ios_IOSNative_fontAscentNative___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    @autoreleasepool {
        NSFont *f = (NSFont *) (intptr_t) peer;
        if (f == nil) {
            return 0;
        }
        return (JAVA_INT) roundf([f ascender]);
    }
}

JAVA_INT com_codename1_impl_ios_IOSNative_fontDescentNative___long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT instanceObject, JAVA_LONG peer) {
    @autoreleasepool {
        NSFont *f = (NSFont *) (intptr_t) peer;
        if (f == nil) {
            return 0;
        }
        return (JAVA_INT) roundf([f descender]);
    }
}

/* ---- CN1SimHost JNI natives (JAWT surface attachment) ----------------------- */

static jint cn1sim_attach_surface(JNIEnv *env, jobject canvas, jint width, jint height) {
    JAWT awt;
    /* JDK 9+: CALayer surfaces are the only mode; the legacy
     * JAWT_MACOSX_USE_CALAYER flag belongs to the 1.7 version word */
    awt.version = JAWT_VERSION_9;
    if (JAWT_GetAWT(env, &awt) == JNI_FALSE) {
        awt.version = JAWT_VERSION_1_7 | JAWT_MACOSX_USE_CALAYER;
        if (JAWT_GetAWT(env, &awt) == JNI_FALSE) {
            fprintf(stderr, "cn1sim: JAWT_GetAWT failed\n");
            return -1;
        }
    }
    JAWT_DrawingSurface *ds = awt.GetDrawingSurface(env, canvas);
    if (ds == NULL) {
        fprintf(stderr, "cn1sim: GetDrawingSurface failed\n");
        return -2;
    }
    jint result = 0;
    jint lock = ds->Lock(ds);
    if ((lock & JAWT_LOCK_ERROR) != 0) {
        awt.FreeDrawingSurface(ds);
        fprintf(stderr, "cn1sim: surface Lock failed\n");
        return -3;
    }
    JAWT_DrawingSurfaceInfo *dsi = ds->GetDrawingSurfaceInfo(ds);
    if (dsi == NULL) {
        ds->Unlock(ds);
        awt.FreeDrawingSurface(ds);
        fprintf(stderr, "cn1sim: GetDrawingSurfaceInfo failed\n");
        return -4;
    }
    id<JAWT_SurfaceLayers> surfaceLayers = (id<JAWT_SurfaceLayers>) dsi->platformInfo;
    CALayer *layer = [CALayer layer];
    [[CodenameOne_GLViewController instance] attachLayer:layer width:width height:height];
    dispatch_async(dispatch_get_main_queue(), ^{
        surfaceLayers.layer = layer;
        layer.opaque = YES;
        if (getenv("CN1_SIM_DEBUG_RED") != NULL) {
            CGColorRef red = CGColorCreateGenericRGB(1, 0, 0, 1);
            layer.backgroundColor = red;
            CGColorRelease(red);
        }
        if (getenv("CN1_SIM_DEBUG") != NULL) {
            CALayer *parent = layer.superlayer;
            fprintf(stderr, "cn1sim: layer attached, parent=%s parentBounds=%fx%f layerFrame=%f,%f %fx%f\n",
                    parent != nil ? [[parent description] UTF8String] : "nil",
                    parent != nil ? parent.bounds.size.width : -1,
                    parent != nil ? parent.bounds.size.height : -1,
                    layer.frame.origin.x, layer.frame.origin.y,
                    layer.frame.size.width, layer.frame.size.height);
        }
    });
    ds->FreeDrawingSurfaceInfo(dsi);
    ds->Unlock(ds);
    awt.FreeDrawingSurface(ds);
    return result;
}

static void jni_CN1SimHost_attachSurface(JNIEnv *env, jclass cls, jobject canvas, jint width, jint height) {
    cn1sim_attach_surface(env, canvas, width, height);
}

static void jni_CN1SimHost_resizeSurface(JNIEnv *env, jclass cls, jint width, jint height) {
    [[CodenameOne_GLViewController instance] resizeSurface:width height:height];
}

static jboolean jni_CN1SimHost_saveScreenshot(JNIEnv *env, jclass cls, jstring path,
        jint x, jint y, jint w, jint h) {
    const char *p = (*env)->GetStringUTFChars(env, path, NULL);
    if (p == NULL) {
        return JNI_FALSE;
    }
    BOOL ok;
    @autoreleasepool {
        ok = [[CodenameOne_GLViewController instance]
                saveScreenshotPNG:[NSString stringWithUTF8String:p]
                            cropX:x cropY:y cropW:w cropH:h];
    }
    (*env)->ReleaseStringUTFChars(env, path, p);
    return ok ? JNI_TRUE : JNI_FALSE;
}

static jboolean jni_CN1SimHost_isSurfaceReady(JNIEnv *env, jclass cls) {
    return [[CodenameOne_GLViewController instance] surfaceReady] ? JNI_TRUE : JNI_FALSE;
}

static void jni_CN1SimHost_setScreenOverlay(JNIEnv *env, jclass cls, jlong peer,
        jint x, jint y, jint w, jint h, jint cx, jint cy, jint cw, jint ch) {
    [[CodenameOne_GLViewController instance]
            setScreenOverlay:(GLUIImage *) (intptr_t) peer x:x y:y w:w h:h
                       clipX:cx clipY:cy clipW:cw clipH:ch];
}

static const JNINativeMethod cn1sim_host_methods[] = {
    {"attachSurface", "(Ljava/awt/Canvas;II)V", (void *) jni_CN1SimHost_attachSurface},
    {"resizeSurface", "(II)V", (void *) jni_CN1SimHost_resizeSurface},
    {"saveScreenshot", "(Ljava/lang/String;IIII)Z", (void *) jni_CN1SimHost_saveScreenshot},
    {"isSurfaceReady", "()Z", (void *) jni_CN1SimHost_isSurfaceReady},
    {"setScreenOverlay", "(JIIIIIIII)V", (void *) jni_CN1SimHost_setScreenOverlay},
};

jint cn1sim_register_host(JNIEnv *env) {
    jclass cls = (*env)->FindClass(env, "com/codename1/impl/ios/sim/CN1SimHost");
    if (cls == NULL) {
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, cls, cn1sim_host_methods,
            sizeof(cn1sim_host_methods) / sizeof(JNINativeMethod));
}
