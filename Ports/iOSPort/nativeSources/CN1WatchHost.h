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
 * CN1WatchHost owns the watchOS run loop and bridges the watch UI surface to
 * the Codename One paint + event pipeline. It replaces the iOS
 * UIApplication/UIWindow/CADisplayLink bootstrap, which does not exist on
 * watchOS.
 *
 * Responsibilities:
 *  - Drive a timer-based frame pump (watchOS has no CADisplayLink) that asks
 *    CodenameOne_GLViewController to paint a frame into the
 *    CN1WatchRenderingView.
 *  - Receive the rendered UIImage (as a CN1WatchFramePresenter) and push it to
 *    the host surface - a SpriteKit WKInterfaceSKScene texture or a SwiftUI
 *    Image - supplied by the generated watch app target.
 *  - Translate Digital Crown rotation + screen taps into CN1 pointer / scroll
 *    events.
 *
 * The generated watch app's WKApplication / SwiftUI App instantiates one
 * CN1WatchHost in its entry point and forwards lifecycle + input to it.
 */
#ifndef CN1WatchHost_h
#define CN1WatchHost_h

#include "TargetConditionals.h"

#if TARGET_OS_WATCH
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "CN1WatchRenderingView.h"

// Implemented by the generated watch app to display rendered CN1 frames.
@protocol CN1WatchSurface <NSObject>
- (void)displayFrame:(UIImage *)frame;
@end

@interface CN1WatchHost : NSObject <CN1WatchFramePresenter>

// assign (not weak): the iOS port compiles without ARC. The surface (the
// generated watch app's host view) owns/outlives the host singleton.
@property (nonatomic, assign) id<CN1WatchSurface> surface;
@property (nonatomic, readonly) CN1WatchRenderingView *renderingView;

// Singleton accessor - the CN1 native code expects one host per app.
+ (CN1WatchHost *)sharedHost;

// Start the CN1 EDT + paint pump for a w x h point surface at the given scale.
- (void)startWithWidth:(int)w height:(int)h scale:(CGFloat)scale;

// Lifecycle hooks the watch app forwards from its scene phases.
- (void)applicationDidBecomeActive;
- (void)applicationWillResignActive;

// Input. crownDelta is in CN1 scroll units; tap coordinates are in points.
- (void)crownRotatedBy:(CGFloat)crownDelta;
- (void)tapAtX:(int)x y:(int)y;
- (void)pointerPressedAtX:(int)x y:(int)y;
- (void)pointerDraggedToX:(int)x y:(int)y;
- (void)pointerReleasedAtX:(int)x y:(int)y;

// Request a repaint on the next pump tick (called by CN1 when the UI is dirty).
- (void)setNeedsDisplay;

@end

#endif // TARGET_OS_WATCH
#endif // CN1WatchHost_h
