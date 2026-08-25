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
#ifndef CN1AppKitMetalView_h
#define CN1AppKitMetalView_h
#import <TargetConditionals.h>
#if TARGET_OS_OSX

#import <AppKit/AppKit.h>
#import <QuartzCore/CAMetalLayer.h>
@import Metal;
@import simd;

/// The rendering surface of one Codename One window on the native macOS port.
///
/// Layer-hosted rather than layer-backed: `makeBackingLayer` returns the
/// `CAMetalLayer` itself, so AppKit never draws into it and the only thing that
/// touches those pixels is Metal.
///
/// #### Why this is a separate class from METALView
///
/// `METALView` is a `UIView`, and the roughly fifty UIKit references in it are
/// not all mechanical: `NSImage` is a resolution-independent representation
/// container rather than a bitmap, `NSView` is not flipped by default, and the
/// keyboard and text-delegate half of that file has no macOS meaning at all.
/// Guarding it would put the iOS renderer -- the code every existing iOS app
/// depends on -- one careless `#if` away from a regression that only a device
/// screenshot run would catch. This class instead drives the very same
/// `CN1Metal*` C API that the drawing operations already call, so the renderer
/// is shared where it matters and no iOS file is touched.
///
/// #### Why a global encoder is still correct
///
/// Every `ExecutableOp` reaches the active encoder through `CN1Metalcompat`'s
/// process-global rather than through a view pointer. That reads like an
/// obstacle to per-window rendering and is not one: the event dispatch thread
/// paints one window at a time, so "the encoder currently being written to" is
/// genuinely a single value. Each window brackets its own paint with
/// `setFramebuffer` and `presentFramebuffer`, and the global names whichever
/// window is between the two.
///
/// #### `isFlipped`
///
/// Returns YES, which puts AppKit's origin at the top left and makes it agree
/// with Codename One's coordinate space. Every boundary in the port -- mouse
/// locations, tracking areas, peer frames, the text input client's rects --
/// then needs no vertical flip. The alternative is a conversion at each of
/// those sites and a permanent tax on every new one.
@interface CN1AppKitMetalView : NSView {
@private
    int framebufferWidth;
    int framebufferHeight;
    simd_float4x4 projectionMatrix;
    BOOL clearRetainedFramebufferOnNextFrame;
}

@property (nonatomic, retain) id<MTLCommandQueue> commandQueue;
@property (nonatomic, retain) id<MTLCommandBuffer> commandBuffer;
@property (nonatomic, retain) MTLRenderPassDescriptor *renderPassDescriptor;
@property (nonatomic, retain) id<MTLRenderCommandEncoder> renderCommandEncoder;
/// Persistent render target. Codename One only queues the operations that
/// changed since the previous frame, so the target has to survive between them;
/// a Metal drawable does not, being cleared on acquire. Blitted to the drawable
/// at present time.
@property (nonatomic, retain) id<MTLTexture> screenTexture;
/// Stencil attachment for polygon-shape clipping. Every pipeline in the cache
/// declares a Stencil8 stencil format, and Metal aborts if a pass binds one of
/// them without a matching attachment, so this exists whether or not the frame
/// clips.
@property (nonatomic, retain) id<MTLTexture> stencilTexture;
/// Host for native peer components, above the Metal layer.
@property (nonatomic, retain) NSView *peerComponentsLayer;
@property (nonatomic, readonly) int framebufferWidth;
@property (nonatomic, readonly) int framebufferHeight;
@property (nonatomic, readonly) simd_float4x4 projectionMatrix;
/// The framework's window id, echoed back on every event so input routes
/// without a lookup on the platform's own thread.
@property (nonatomic, assign) int cn1WindowId;

- (void)setFramebuffer;
- (BOOL)presentFramebuffer;
- (void)updateFrameBufferSize:(int)w h:(int)h;
- (void)invalidateRetainedFramebuffer;
- (void)addPeerComponent:(NSView *)view;
/// Reads the finished frame back as an ARGB raster. Used by the screenshot
/// pipeline and by Window.capture().
- (BOOL)readbackInto:(unsigned int *)argb width:(int)w height:(int)h;

@end

#endif
#endif
