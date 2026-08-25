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

#include "cn1_globals.h"
#include <math.h>

// Filled by viewSafeAreaInsetsDidChange on iOS, which has no watchOS counterpart -- see
// cn1PublishWatchSafeArea below. Defined in CodenameOne_GLViewController.m alongside the
// getSafeLeft()/getSafeTop()/... readers the Java side calls.
extern JAVA_INT safeLeft;
extern JAVA_INT safeRight;
extern JAVA_INT safeTop;
extern JAVA_INT safeBottom;

// CN1 entry points implemented elsewhere in the port. Declared here to avoid a
// header dependency cycle with CodenameOne_GLViewController (which is UIKit-
// heavy and partly excluded on watch).
extern void cn1_watch_bootstrap(void);          // start the EDT / main class
extern void cn1_watch_paintFrame(void);         // drain the op queue -> render view
extern void cn1_watch_pointerPressed(int x, int y);
extern void cn1_watch_pointerDragged(int x, int y);
extern void cn1_watch_pointerReleased(int x, int y);
extern void pointerWheelMovedCallback(int x, int y, int scrollX, int scrollY);
extern void cn1_watch_didEnterBackground(void);  // forward the CN1 app lifecycle
extern void cn1_watch_willEnterForeground(void);

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

/// Fraction of the shorter edge taken as the display's corner radius.
///
/// watchOS reports no safe-area inset and no corner radius -- WKInterfaceDevice offers neither --
/// and the SwiftUI host deliberately ignoresSafeArea so CN1 owns the whole surface. Something has
/// to name the curve, and this is the same ratio the generated watch skins are drawn with, so a
/// layout previewed in the simulator matches the device.
static const CGFloat CN1_WATCH_CORNER_RADIUS_RATIO = 0.0707;

/// Publishes the inset that keeps a layout's corners clear of the rounded bezel.
///
/// Without this getDisplaySafeArea() returned the full rectangle on every Apple Watch, so even a
/// layout that honours the safe area put its corner controls under the curve. The insets live in
/// the globals viewSafeAreaInsetsDidChange fills on iOS, which the watch host never runs.
///
/// A rounded RECTANGLE, not a circle: the largest rectangle inside a corner arc of radius r clears
/// it once each edge is inset by r*(1 - 1/sqrt(2)). Inscribing the whole display in a circle, as a
/// genuinely round Wear face requires, would throw away a third of an Apple Watch screen.
static void cn1PublishWatchSafeArea(int w, int h) {
    if (w <= 0 || h <= 0) {
        safeLeft = 0;
        safeTop = 0;
        safeRight = 0;
        safeBottom = 0;
        return;
    }
    CGFloat radius = CN1_WATCH_CORNER_RADIUS_RATIO * (CGFloat)MIN(w, h);
    JAVA_INT inset = (JAVA_INT)ceil(radius * (1.0 - 1.0 / sqrt(2.0)));
    if (inset < 0) {
        inset = 0;
    }
    // Points, not pixels: scaleValue is 1 on this slice, so CN1's coordinate space IS the logical
    // one getDisplayWidthImpl reports from the rendering view.
    safeLeft = inset;
    safeTop = inset;
    safeRight = inset;
    safeBottom = inset;
}

- (void)startWithWidth:(int)w height:(int)h scale:(CGFloat)scale {
    _renderingView = [[CN1WatchRenderingView alloc] initWithWidth:w height:h scale:scale];
    _renderingView.presenter = self;
    // Every call, not just the first: this is also the resize path -- the SwiftUI host calls it
    // again with new bounds and a fresh rendering view is built from them.
    cn1PublishWatchSafeArea(w, h);
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

- (void)presentWatchFrame:(CN1Image *)frame {
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

- (void)applicationDidEnterBackground {
    // The pump is already stopped by applicationWillResignActive, which watchOS sends first. What
    // this adds is the CN1 lifecycle itself: without it the application's stop() never ran on the
    // watch, so timers and resources it releases there stayed live through a normal suspension.
    cn1_watch_didEnterBackground();
}

- (void)applicationWillEnterForeground {
    // Before applicationDidBecomeActive restarts the pump, so the application's start() -- and any
    // refresh-on-foreground work it does, which the same lifecycle class gets on the phone -- is
    // queued before the first frame of the resumed session is drawn.
    cn1_watch_willEnterForeground();
}

#pragma mark - Input

/// Logical points a container scrolls per unit of crown rotation.
///
/// The Swift view asks for crown values `by: 1`, so an ordinary detent arrives as roughly 1.0.
/// Forwarding that straight through moved a form by ONE POINT per detent, which is a scroll bar
/// that never visibly moves -- the Android rotary path does not do this, it multiplies by the
/// platform's own scroll factor. watchOS exposes no equivalent, so this is the line-height-sized
/// step that factor amounts to elsewhere.
static const CGFloat CN1_WATCH_CROWN_POINTS_PER_UNIT = 24.0;

/// What was left over after the last whole-point delivery.
///
/// The wheel callback takes an int, and truncating each event independently threw away every
/// rotation smaller than a point -- a slow, deliberate turn produced a stream of zeroes and
/// scrolled nothing at all. Carrying the remainder makes the small movements add up to the same
/// distance as one fast turn.
static CGFloat cn1WatchCrownRemainder = 0;

- (void)crownRotatedBy:(CGFloat)crownDelta {
    // Route the Digital Crown through the cross-platform wheel pipeline so it is the same universal
    // scroll-gesture input as a mouse wheel or trackpad: it scrolls the component under the center
    // of the watch face and is also delivered to any mouse wheel listeners as a WheelEvent. A
    // positive crown delta reveals content above (scrolls down), matching the wheel convention.
    CGFloat scaled = (-crownDelta * CN1_WATCH_CROWN_POINTS_PER_UNIT) + cn1WatchCrownRemainder;
    int whole = (int)scaled;
    cn1WatchCrownRemainder = scaled - (CGFloat)whole;
    if (whole == 0) {
        // Nothing to deliver yet, and nothing to repaint: the remainder is holding the movement
        // until it amounts to a point.
        return;
    }
    needsDisplay = YES;
    int cx = _renderingView != nil ? [_renderingView logicalWidth] / 2 : 0;
    int cy = _renderingView != nil ? [_renderingView logicalHeight] / 2 : 0;
    pointerWheelMovedCallback(cx, cy, 0, whole);
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
