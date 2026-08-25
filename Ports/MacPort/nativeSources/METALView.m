/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
#import <TargetConditionals.h>
#if TARGET_OS_OSX

#import "METALView.h"
#import "CN1AppKitCompat.h"
#import "CN1Metalcompat.h"
#include "cn1_globals.h"

/// Orthographic projection with the origin at the top left.
///
/// Passing bottom=h and top=0 makes y=0 land at the top of the drawable, which
/// is where Codename One expects it. The alternative is a flip in the model-view
/// matrix and a compensating translate, which the OpenGL path had to do and
/// which is a permanent source of off-by-one at the edges.
static simd_float4x4 CN1MacOrtho(float left, float right, float bottom, float top,
                                 float nearZ, float farZ) {
    float ral = right + left, rsl = right - left;
    float tab = top + bottom, tsb = top - bottom;
    float fan = farZ + nearZ, fsn = farZ - nearZ;
    simd_float4x4 m;
    m.columns[0] = (simd_float4){ 2.0f / rsl, 0.0f, 0.0f, 0.0f };
    m.columns[1] = (simd_float4){ 0.0f, 2.0f / tsb, 0.0f, 0.0f };
    m.columns[2] = (simd_float4){ 0.0f, 0.0f, -2.0f / fsn, 0.0f };
    m.columns[3] = (simd_float4){ -ral / rsl, -tab / tsb, -fan / fsn, 1.0f };
    return m;
}

@implementation METALView

@synthesize commandQueue;
@synthesize commandBuffer;
@synthesize renderPassDescriptor;
@synthesize renderCommandEncoder;
@synthesize screenTexture;
@synthesize stencilTexture;
@synthesize peerComponentsLayer;
@synthesize framebufferWidth;
@synthesize framebufferHeight;
@synthesize projectionMatrix;
@synthesize cn1WindowId;

// ---- view configuration -------------------------------------------------

/// Layer-hosted rather than layer-backed. Returning the CAMetalLayer from here
/// means AppKit adopts it as the view's own layer and never draws into it, so
/// Metal is the only thing that touches those pixels.
- (CALayer *)makeBackingLayer {
    return [CAMetalLayer layer];
}

- (BOOL)wantsUpdateLayer {
    return YES;
}

/// Top-left origin, matching Codename One's coordinate space. This is the single
/// highest-leverage line in the port: without it every boundary -- mouse
/// locations, tracking areas, peer frames, the text input client's rects --
/// needs its own vertical flip, and each new one is a fresh chance to get it
/// wrong.
- (BOOL)isFlipped {
    return YES;
}

- (BOOL)isOpaque {
    return YES;
}

- (BOOL)acceptsFirstResponder {
    return YES;
}

/// A click that activates the window should also reach the app, which is what a
/// Codename One user expects from a tap and is not AppKit's default.
- (BOOL)acceptsFirstMouse:(NSEvent *)event {
    return YES;
}

- (instancetype)initWithFrame:(NSRect)frameRect {
    self = [super initWithFrame:frameRect];
    if (self) {
        [self cn1SetupMetal];
    }
    return self;
}

- (void)cn1SetupMetal {
    self.wantsLayer = YES;
    self.layerContentsRedrawPolicy = NSViewLayerContentsRedrawDuringViewResize;
    // Without TopLeft the layer scales its last frame while the user drags a
    // resize, which reads as a smear rather than a redraw.
    self.layerContentsPlacement = NSViewLayerContentsPlacementTopLeft;

    CAMetalLayer *metalLayer = (CAMetalLayer *)self.layer;
    metalLayer.device = MTLCreateSystemDefaultDevice();
    metalLayer.opaque = YES;
    metalLayer.pixelFormat = MTLPixelFormatBGRA8Unorm;
    // Must be NO: presentFramebuffer blits screenTexture into the drawable, and
    // Metal's blit validation rejects a framebufferOnly destination.
    metalLayer.framebufferOnly = NO;
    metalLayer.maximumDrawableCount = 3;

    // sRGB so CoreGraphics-rasterised images and gradients, which carry
    // DeviceRGB bytes, are not treated as linear and displayed too bright.
    CGColorSpaceRef cs = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
    if (cs != NULL) {
        metalLayer.colorspace = cs;
        CGColorSpaceRelease(cs);
    }

    id<MTLCommandQueue> newQueue = [metalLayer.device newCommandQueue];
    self.commandQueue = newQueue;
#ifndef CN1_USE_ARC
    [newQueue release];
#endif
    // Publish device and queue once, on the main thread, so CN1Metalcompat's
    // accessors are cheap static reads from the EDT and any background queue
    // rather than a dereference of this view's layer.
    CN1MetalSetDeviceAndCommandQueue(metalLayer.device, self.commandQueue);

    [self updateBackingSize];
}

- (void)updateBackingSize {
    CGFloat s = CN1AppKitBackingScale(self);
    NSSize sz = self.bounds.size;
    [self updateFrameBufferSize:(int)(sz.width * s) h:(int)(sz.height * s)];
}

/// Re-asked rather than cached: dragging a window between a Retina and a
/// non-Retina display changes the answer while the app is running.
- (void)viewDidChangeBackingProperties {
    [super viewDidChangeBackingProperties];
    [self updateBackingSize];
}

- (void)setFrameSize:(NSSize)newSize {
    [super setFrameSize:newSize];
    [self updateBackingSize];
}

// ---- framebuffer --------------------------------------------------------

- (void)updateFrameBufferSize:(int)w h:(int)h {
    int pw = w, ph = h;
    if (pw <= 0 || ph <= 0) {
        CGFloat s = CN1AppKitBackingScale(self);
        pw = (int)(self.bounds.size.width * s);
        ph = (int)(self.bounds.size.height * s);
    }
    if (pw <= 0 || ph <= 0 || (pw == framebufferWidth && ph == framebufferHeight)) {
        return;
    }
    // An encoder may be mid-frame. Tear it down rather than letting draws land
    // on a texture about to be replaced, which would also leave the stale
    // dimensions cached inside CN1Metalcompat and break scissor clamping.
    if (self.renderCommandEncoder != nil) {
        CN1MetalEndFrame();
        [self.renderCommandEncoder endEncoding];
        self.renderCommandEncoder = nil;
    }
    if (self.commandBuffer != nil) {
        [self.commandBuffer commit];
        self.commandBuffer = nil;
        self.renderPassDescriptor = nil;
    }

    framebufferWidth = pw;
    framebufferHeight = ph;
    projectionMatrix = CN1MacOrtho(0.0f, (float)pw, (float)ph, 0.0f, -1.0f, 1.0f);

    CAMetalLayer *layer = (CAMetalLayer *)self.layer;
    layer.contentsScale = CN1AppKitBackingScale(self);
    layer.drawableSize = CGSizeMake(pw, ph);

    // The persistent target. Codename One only queues the operations that
    // changed since the previous frame, so it has to survive between them; a
    // Metal drawable does not, being cleared on acquire.
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
        width:pw height:ph mipmapped:NO];
    desc.usage = MTLTextureUsageRenderTarget | MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModePrivate;
    id<MTLTexture> newScreen = [layer.device newTextureWithDescriptor:desc];
    self.screenTexture = newScreen;
#ifndef CN1_USE_ARC
    [newScreen release];
#endif

    // Private storage comes back uninitialised, and the first frame loads
    // rather than clears, so anything not yet drawn would sample garbage.
    id<MTLCommandBuffer> clearCb = [self.commandQueue commandBuffer];
    MTLRenderPassDescriptor *clearPass = [MTLRenderPassDescriptor renderPassDescriptor];
    clearPass.colorAttachments[0].texture = self.screenTexture;
    clearPass.colorAttachments[0].loadAction = MTLLoadActionClear;
    clearPass.colorAttachments[0].storeAction = MTLStoreActionStore;
    clearPass.colorAttachments[0].clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0);
    id<MTLRenderCommandEncoder> clearEnc =
        [clearCb renderCommandEncoderWithDescriptor:clearPass];
    [clearEnc endEncoding];
    [clearCb commit];

    // Every pipeline in the cache declares a Stencil8 stencil format for
    // polygon clipping, and Metal aborts if a pass binds one without a matching
    // attachment -- so this exists whether or not the frame actually clips.
    MTLTextureDescriptor *stencilDesc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatStencil8
        width:pw height:ph mipmapped:NO];
    stencilDesc.usage = MTLTextureUsageRenderTarget;
    stencilDesc.storageMode = MTLStorageModePrivate;
    id<MTLTexture> newStencil = [layer.device newTextureWithDescriptor:stencilDesc];
    self.stencilTexture = newStencil;
#ifndef CN1_USE_ARC
    [newStencil release];
#endif
}

- (void)createRenderPassDescriptor {
    if (self.screenTexture == nil) {
        self.renderPassDescriptor = nil;
        return;
    }
    self.renderPassDescriptor = [MTLRenderPassDescriptor renderPassDescriptor];
    MTLRenderPassColorAttachmentDescriptor *color =
        self.renderPassDescriptor.colorAttachments[0];
    color.texture = self.screenTexture;
    // Load, not clear: Codename One queues only the operations that changed, so
    // the previous frame's pixels have to still be there.
    if (clearRetainedFramebufferOnNextFrame) {
        color.loadAction = MTLLoadActionClear;
        color.clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0);
        clearRetainedFramebufferOnNextFrame = NO;
    } else {
        color.loadAction = MTLLoadActionLoad;
    }
    color.storeAction = MTLStoreActionStore;
    if (self.stencilTexture != nil) {
        MTLRenderPassStencilAttachmentDescriptor *stencil =
            self.renderPassDescriptor.stencilAttachment;
        stencil.texture = self.stencilTexture;
        stencil.loadAction = MTLLoadActionClear;
        stencil.storeAction = MTLStoreActionDontCare;
        stencil.clearStencil = 0;
    }
}

- (void)setFramebuffer {
    // Tolerates being called more than once per frame, as the UIKit backend
    // does: creating a second encoder would discard everything queued against
    // the first. Only presentFramebuffer ends and commits.
    if (self.renderCommandEncoder != nil) {
        return;
    }
    self.commandBuffer = [self.commandQueue commandBuffer];
    [self createRenderPassDescriptor];
    if (self.renderPassDescriptor == nil) {
        self.renderCommandEncoder = nil;
        return;
    }
    self.renderCommandEncoder =
        [self.commandBuffer renderCommandEncoderWithDescriptor:self.renderPassDescriptor];
    [self.renderCommandEncoder setViewport:(MTLViewport){
        0.0, 0.0, (double)framebufferWidth, (double)framebufferHeight, 0.0, 1.0 }];
    // Publish encoder and projection so each ExecutableOp's Metal branch finds
    // them. The global is correct here because the event dispatch thread paints
    // one window at a time.
    CN1MetalBeginFrame(self.renderCommandEncoder, projectionMatrix,
                       framebufferWidth, framebufferHeight);
}

- (BOOL)presentFramebuffer {
    if (self.renderCommandEncoder == nil) {
        self.commandBuffer = nil;
        return NO;
    }
    CN1MetalEndFrame();
    [self.renderCommandEncoder endEncoding];
    self.renderCommandEncoder = nil;
    self.renderPassDescriptor = nil;

    // Acquired here rather than in setFramebuffer to keep its dwell time short:
    // holding a drawable across the whole encoding phase stalls nextDrawable for
    // the frames behind it.
    CAMetalLayer *layer = (CAMetalLayer *)self.layer;
    id<CAMetalDrawable> dr = [layer nextDrawable];
    if (dr == nil) {
        [self.commandBuffer commit];
        self.commandBuffer = nil;
        return NO;
    }
    id<MTLBlitCommandEncoder> blit = [self.commandBuffer blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture
              sourceSlice:0 sourceLevel:0
             sourceOrigin:MTLOriginMake(0, 0, 0)
               sourceSize:MTLSizeMake(framebufferWidth, framebufferHeight, 1)
                toTexture:dr.texture
         destinationSlice:0 destinationLevel:0
        destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [self.commandBuffer presentDrawable:dr];
    [self.commandBuffer commit];
    self.commandBuffer = nil;
    return YES;
}

- (void)deleteFramebuffer {
    self.screenTexture = nil;
    self.stencilTexture = nil;
    framebufferWidth = 0;
    framebufferHeight = 0;
}

- (void)invalidateRetainedFramebuffer {
    clearRetainedFramebufferOnNextFrame = YES;
}

- (void)prepareRetainedFramebufferForDrawRect:(CGRect)rect displayWidth:(int)displayWidth
                                displayHeight:(int)displayHeight {
    // A full-screen repaint is the only case where the retained target's
    // contents are known to be about to be replaced entirely.
    if (rect.origin.x <= 0 && rect.origin.y <= 0
            && rect.size.width >= displayWidth && rect.size.height >= displayHeight) {
        clearRetainedFramebufferOnNextFrame = YES;
    }
}

// ---- readback -----------------------------------------------------------

- (BOOL)readbackInto:(unsigned int *)argb width:(int)w height:(int)h {
    if (argb == NULL || self.screenTexture == nil
            || w != framebufferWidth || h != framebufferHeight) {
        return NO;
    }
    // Blit into shared storage first: the screen texture is private, so the CPU
    // cannot read it directly.
    MTLTextureDescriptor *desc = [MTLTextureDescriptor
        texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
        width:w height:h mipmapped:NO];
    desc.usage = MTLTextureUsageShaderRead;
    desc.storageMode = MTLStorageModeShared;
    id<MTLTexture> staging = [((CAMetalLayer *)self.layer).device newTextureWithDescriptor:desc];
    if (staging == nil) {
        return NO;
    }
    id<MTLCommandBuffer> cb = [self.commandQueue commandBuffer];
    id<MTLBlitCommandEncoder> blit = [cb blitCommandEncoder];
    [blit copyFromTexture:self.screenTexture
              sourceSlice:0 sourceLevel:0
             sourceOrigin:MTLOriginMake(0, 0, 0)
               sourceSize:MTLSizeMake(w, h, 1)
                toTexture:staging
         destinationSlice:0 destinationLevel:0
        destinationOrigin:MTLOriginMake(0, 0, 0)];
    [blit endEncoding];
    [cb commit];
    [cb waitUntilCompleted];
    [staging getBytes:argb bytesPerRow:(NSUInteger)w * 4
           fromRegion:MTLRegionMake2D(0, 0, w, h) mipmapLevel:0];
#ifndef CN1_USE_ARC
    [staging release];
#endif
    return YES;
}

// ---- peer components ----------------------------------------------------

- (void)addPeerComponent:(CN1View *)view {
    if (view == nil) {
        return;
    }
    if (self.peerComponentsLayer == nil) {
        NSView *host = [[NSView alloc] initWithFrame:self.bounds];
        host.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
        self.peerComponentsLayer = host;
        [self addSubview:host];
#ifndef CN1_USE_ARC
        [host release];
#endif
    }
    [self.peerComponentsLayer addSubview:view];
}

// ---- protocol members with no AppKit meaning ----------------------------

// AppKit has no software keyboard and no UITextField delegate. These exist to
// satisfy CN1RenderingView, which the UIKit ports need them for.
- (void)keyboardDoneClicked {
}

- (void)keyboardNextClicked {
}

- (void)textFieldDidChange {
}

// ---- materials ----------------------------------------------------------

// The blur, glass and lens materials are not implemented yet. They read the
// screen texture back, run a CoreImage pipeline over it and upload the result,
// and each needs its own verification against a golden. Doing nothing leaves the
// region showing whatever was already drawn there, which is the same thing the
// UIKit backend does when its own capture fails -- it never leaves a hole.
- (void)blurScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h radius:(float)radius {
}

- (void)glassScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h radius:(float)radius
              cornerRadius:(float)cornerRadius sat:(float)sat scale:(float)scale
                    offset:(float)offset refract:(float)refract specular:(float)specular {
}

- (void)lensScreenRegionX:(int)x y:(int)y w:(int)w h:(int)h cornerRadius:(float)cornerRadius
                  magnify:(float)magnify aberration:(float)aberration
                tintColor:(int)tintColor tintStrength:(float)tintStrength {
}

@end

#endif
