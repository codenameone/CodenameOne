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
#import "CN1WatchHost.h"

#if TARGET_OS_WATCH

// CN1 entry points implemented elsewhere in the port. Declared here to avoid a
// header dependency cycle with CodenameOne_GLViewController (which is UIKit-
// heavy and partly excluded on watch).
extern void cn1_watch_bootstrap(void);          // start the EDT / main class
extern void cn1_watch_paintFrame(void);         // drain the op queue -> render view
extern void cn1_watch_pointerPressed(int x, int y);
extern void cn1_watch_pointerDragged(int x, int y);
extern void cn1_watch_pointerReleased(int x, int y);
extern void pointerWheelMovedCallback(int x, int y, int scrollX, int scrollY);

static CN1WatchHost *sharedHostInstance = nil;

@implementation CN1WatchHost {
    NSTimer *pumpTimer;
    BOOL active;
    BOOL needsDisplay;
    BOOL bootstrapped;
}

+ (CN1WatchHost *)sharedHost {
    if (sharedHostInstance == nil) {
        sharedHostInstance = [[CN1WatchHost alloc] init];
    }
    return sharedHostInstance;
}

- (void)startWithWidth:(int)w height:(int)h scale:(CGFloat)scale {
    _renderingView = [[CN1WatchRenderingView alloc] initWithWidth:w height:h scale:scale];
    _renderingView.presenter = self;
    active = YES;
    needsDisplay = YES;
    if (!bootstrapped) {
        bootstrapped = YES;
        cn1_watch_bootstrap();
    }
    [self startPump];
}

- (void)startPump {
    if (pumpTimer != nil) {
        return;
    }
    // ~30fps. watchOS has no CADisplayLink; an NSTimer on the main run loop is
    // the supported substitute. Battery-friendly: only paints when dirty.
    pumpTimer = [NSTimer scheduledTimerWithTimeInterval:1.0 / 30.0
                                                 target:self
                                               selector:@selector(pump)
                                               userInfo:nil
                                                repeats:YES];
}

- (void)stopPump {
    [pumpTimer invalidate];
    pumpTimer = nil;
}

- (void)pump {
    if (!active || !needsDisplay) {
        return;
    }
    needsDisplay = NO;
    // Paint: CN1 drains its op queue against the rendering view, which begins a
    // CG frame, rasterizes, and presents (-> presentWatchFrame: below).
    cn1_watch_paintFrame();
}

- (void)setNeedsDisplay {
    needsDisplay = YES;
}

#pragma mark - CN1WatchFramePresenter

- (void)presentWatchFrame:(UIImage *)frame {
    id<CN1WatchSurface> s = self.surface;
    if (s != nil && frame != nil) {
        [s displayFrame:frame];
    }
}

#pragma mark - Lifecycle

- (void)applicationDidBecomeActive {
    active = YES;
    needsDisplay = YES;
    [self startPump];
}

- (void)applicationWillResignActive {
    active = NO;
    [self stopPump];
}

#pragma mark - Input

- (void)crownRotatedBy:(CGFloat)crownDelta {
    // Route the Digital Crown through the cross-platform wheel pipeline so it is the same universal
    // scroll-gesture input as a mouse wheel or trackpad: it scrolls the component under the center
    // of the watch face and is also delivered to any mouse wheel listeners as a WheelEvent. A
    // positive crown delta reveals content above (scrolls down), matching the wheel convention.
    needsDisplay = YES;
    int cx = _renderingView != nil ? [_renderingView logicalWidth] / 2 : 0;
    int cy = _renderingView != nil ? [_renderingView logicalHeight] / 2 : 0;
    pointerWheelMovedCallback(cx, cy, 0, (int)(-crownDelta));
}

- (void)tapAtX:(int)x y:(int)y {
    needsDisplay = YES;
    cn1_watch_pointerPressed(x, y);
    cn1_watch_pointerReleased(x, y);
}

- (void)pointerPressedAtX:(int)x y:(int)y {
    needsDisplay = YES;
    cn1_watch_pointerPressed(x, y);
}

- (void)pointerDraggedToX:(int)x y:(int)y {
    needsDisplay = YES;
    cn1_watch_pointerDragged(x, y);
}

- (void)pointerReleasedAtX:(int)x y:(int)y {
    needsDisplay = YES;
    cn1_watch_pointerReleased(x, y);
}

- (void)dealloc {
    [self stopPump];
#ifndef CN1_USE_ARC
    [super dealloc];
#endif
}

@end

#endif // TARGET_OS_WATCH
