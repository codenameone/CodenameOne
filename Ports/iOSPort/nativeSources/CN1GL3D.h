/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
#ifndef CN1GL3D_h
#define CN1GL3D_h

#import "CN1ES2compat.h"

// The portable 3D API (com.codename1.gpu) is implemented on iOS with Metal.
// The whole backend is gated on CN1_USE_METAL so a non-Metal build still links
// (the IOSNative bridge functions resolve to no-ops returning 0). We build on a
// hand rolled CAMetalLayer + CADisplayLink (the same primitives the 2D METALView
// uses) rather than MTKView so we do not pull in the MetalKit framework.
#ifdef CN1_USE_METAL
#import <UIKit/UIKit.h>
#import <QuartzCore/CAMetalLayer.h>
@import Metal;
@import simd;

// A UIView backed by a CAMetalLayer plus a depth texture, hosting one 3D
// context. Hosted as a Codename One native peer. A CADisplayLink drives
// continuous mode; render-on-demand renders one frame per requestRender.
@interface CN1GL3DView : UIView

@property (nonatomic, strong) id<MTLDevice> device;
@property (nonatomic, strong) id<MTLCommandQueue> commandQueue;
@property (nonatomic, assign) long contextHandle;

- (void)setContinuous:(BOOL)continuous;
- (void)requestRender;
- (void)recordClear:(int)argb color:(BOOL)clearColor depth:(BOOL)clearDepth;
- (void)recordViewport:(int)x y:(int)y width:(int)width height:(int)height;

@end

#endif /* CN1_USE_METAL */
#endif /* CN1GL3D_h */
