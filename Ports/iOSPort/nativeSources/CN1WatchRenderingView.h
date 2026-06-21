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

/*
 * CN1WatchRenderingView is the watchOS rendering surface. It owns a
 * CGBitmapContext sized to the watch screen (in device pixels). setFramebuffer
 * binds that context as the CN1CGGraphics target; the ExecutableOp queue
 * rasterizes into it; presentFramebuffer snapshots the bitmap into a UIImage
 * and notifies the host (a SpriteKit WKInterfaceSKScene texture or a SwiftUI
 * Image) to display it.
 *
 * This is NOT a UIView (watchOS has no view hierarchy) - it is a plain
 * NSObject that conforms to the CN1RenderingView protocol so the shared
 * CodenameOne_GLViewController drive code works unchanged.
 */
#ifndef CN1WatchRenderingView_h
#define CN1WatchRenderingView_h

#include "TargetConditionals.h"

#if TARGET_OS_WATCH
#import <Foundation/Foundation.h>
#import <CoreGraphics/CoreGraphics.h>
#import <UIKit/UIKit.h>
#import "CN1RenderingView.h"

// Notified on the main thread whenever a freshly rendered frame is available.
@protocol CN1WatchFramePresenter <NSObject>
- (void)presentWatchFrame:(UIImage *)frame;
@end

@interface CN1WatchRenderingView : NSObject <CN1RenderingView> {
    CGContextRef bitmapContext;
    int bufferWidth;
    int bufferHeight;
    CGFloat scale; // device pixels per logical point
}

// assign (not weak): the iOS port compiles without ARC, where weak properties
// can't be synthesized. The presenter (the host) outlives the rendering view.
@property (nonatomic, assign) id<CN1WatchFramePresenter> presenter;

- (id)initWithWidth:(int)w height:(int)h scale:(CGFloat)s;

// Surface size in logical points (device pixels / scale). The watch paint code
// uses these to lay out the frame.
- (int)logicalWidth;
- (int)logicalHeight;

// The most recent rendered frame, or nil before the first present.
- (UIImage *)currentFrame;

@end

#endif // TARGET_OS_WATCH
#endif // CN1WatchRenderingView_h
