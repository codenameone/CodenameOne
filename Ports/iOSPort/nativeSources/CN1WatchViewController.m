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
 * watchOS render-driver. Replaces CodenameOne_GLViewController (a
 * UIViewController, which watchOS marks API_UNAVAILABLE) on the watch slice.
 * It is a plain NSObject that owns the same ExecutableOp queue and drains it
 * into the Core Graphics surface (CN1WatchRenderingView via CN1CGGraphics),
 * keeping the same class name + selector surface so the ~10 callers and the
 * translated runtime resolve unchanged. Modeled on the non-view-controller
 * native renderer in Ports/LinuxPort.
 */
#include "TargetConditionals.h"
#if TARGET_OS_WATCH
#import "CodenameOne_GLViewController.h"
#import "CN1WatchHost.h"
#import "CN1WatchRenderingView.h"
#import "CN1CGGraphics.h"
#import "ClipRect.h"
#import "DrawString.h"
#import "GLUIImage.h"

extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayWidthImpl(void);
extern int Java_com_codename1_impl_ios_IOSImplementation_getDisplayHeightImpl(void);

static CodenameOne_GLViewController *singletonInstance = nil;

// Issue #5273: the partial-flush region handed to flushBuffer (the only place
// the watch slice learns which sub-region is being repainted). drawFrame feeds
// it to ClipRect.setDrawRect so a screen clip emitted while draining the op
// queue is confined to the flushed region instead of escaping into a fixed
// band on the persistent CG surface. Stays CGRectZero until the first flush, at
// which point the clamp in ClipRect's watch branch is a guarded no-op.
static CGRect watchFlushRect;

@implementation CodenameOne_GLViewController

@synthesize animationFrameInterval;
@synthesize currentMutableImage;

+ (CodenameOne_GLViewController *)instance {
    if (singletonInstance == nil) {
        singletonInstance = [[CodenameOne_GLViewController alloc] init];
        [singletonInstance initVars];
    }
    return singletonInstance;
}

- (void)initVars {
    if (currentTarget == nil) {
        currentTarget = [[NSMutableArray alloc] init];
        upcomingTarget = [[NSMutableArray alloc] init];
    }
}

// The watch render surface (a CN1WatchRenderingView) lives on CN1WatchHost.
- (id)view {
    return [CN1WatchHost sharedHost].renderingView;
}

- (id)eaglView {
    return [CN1WatchHost sharedHost].renderingView;
}

+ (void)upcoming:(ExecutableOp *)op {
    [[CodenameOne_GLViewController instance] upcomingAdd:op];
}

- (void)upcomingAdd:(ExecutableOp *)op {
    @synchronized (self) {
        [upcomingTarget addObject:op];
    }
}

- (void)upcomingAddClip:(ExecutableOp *)op {
    [self upcomingAdd:op];
}

- (BOOL)isPaintFinished {
    @synchronized (self) {
        return [upcomingTarget count] == 0;
    }
}

+ (BOOL)isDrawTextureSupported {
    return NO;
}

- (void)startAnimation {}
- (void)stopAnimation {}

- (void)drawString:(int)color alpha:(int)alpha font:(UIFont *)font str:(NSString *)str x:(int)x y:(int)y {
    DrawString *op = [[DrawString alloc] initWithArgs:color a:alpha xpos:x ypos:y s:str f:font];
    [self upcomingAdd:op];
#ifndef CN1_USE_ARC
    [op release];
#endif
}

// flushBuffer hands the accumulated op queue to the renderer and requests a
// paint; the actual rasterization happens in drawFrame (driven by CN1WatchHost).
- (void)flushBuffer:(UIImage *)buff x:(int)x y:(int)y width:(int)width height:(int)height {
    @synchronized (self) {
        if ([upcomingTarget count] > 0) {
            // Issue #5273: APPEND the accumulated ops to the not-yet-drawn set
            // rather than swapping and discarding the previous set. When
            // setNeedsDisplay coalesces several flushes into one drawFrame the
            // old swap dropped every batch but the last, leaving stale pixels on
            // the persistent CG surface -- which only looked correct before the
            // clip clamp because the unclamped draws overpainted them. Keeping
            // every batch is what makes the clamp safe (e.g. the lightweight
            // picker's multi-flush repaint no longer corrupts).
            [currentTarget addObjectsFromArray:upcomingTarget];
            [upcomingTarget removeAllObjects];
        }
        // Union the flushed sub-regions so a clip drained in drawFrame is
        // confined to the combined dirty area of all coalesced flushes; each op
        // still draws within its own region, the union only widens the clamp.
        CGRect r = CGRectMake(x, y, width, height);
        watchFlushRect = CGRectIsEmpty(watchFlushRect) ? r : CGRectUnion(watchFlushRect, r);
    }
    [[CN1WatchHost sharedHost] setNeedsDisplay];
}

- (void)drawScreen {
    [self drawFrame:CGRectZero];
}

// Drain the current op queue into the Core Graphics surface.
- (void)drawFrame:(CGRect)rect {
    CN1WatchRenderingView *v = [CN1WatchHost sharedHost].renderingView;
    if (v == nil) {
        return;
    }
    NSArray *ops;
    CGRect flushRect;
    @synchronized (self) {
        ops = [currentTarget copy];
        // Snapshot the flush region with the op queue it belongs to.
        flushRect = watchFlushRect;
        // Consume both: these ops + region are about to be drawn, so the next
        // flush starts fresh. The persistent bitmap keeps the pixels, so a later
        // forced re-present (drawScreen) still shows this frame.
        [currentTarget removeAllObjects];
        watchFlushRect = CGRectZero;
    }
    [v setFramebuffer];
    // Issue #5273: publish the flush region to ClipRect so a screen clip
    // drained below is clamped to the repainted sub-region (the watch branch of
    // ClipRect.execute no-ops the clamp while this is empty).
    [ClipRect setDrawRect:flushRect];
    // Issue #5273: the persistent CG bitmap is never otherwise cleared, so a
    // previous form/test's pixels survive under areas the new frame's CLAMPED
    // draws no longer overpaint (before the clamp the unclamped draws covered
    // them). On a FULL-screen flush clear the bitmap first: a full repaint's ops
    // repaint the whole surface with the opaque form background, so this is
    // gap-safe (nothing is left transparent) and removes the stale underlay.
    // Partial flushes are left alone (their ops may not cover the whole region).
    int lw = [v logicalWidth];
    int lh = [v logicalHeight];
    if (flushRect.origin.x <= 1 && flushRect.origin.y <= 1 &&
        flushRect.size.width >= lw - 1 && flushRect.size.height >= lh - 1) {
        CN1CGResetClip();
        CN1CGClearRect(0, 0, lw, lh);
    }
    for (ExecutableOp *op in ops) {
        @try {
            [op executeWithClipping];
        } @catch (NSException *e) {
            // Keep draining; a single failing op shouldn't blank the frame.
        }
    }
    // Issue #5273: clear the flush region now the screen drain is done so a
    // mutable-image draw executed immediately outside drawFrame is not clamped
    // to the screen flush rect (the clamp in ClipRect's watch branch no-ops on
    // an empty drawingRect).
    [ClipRect setDrawRect:CGRectZero];
    [v presentFramebuffer];
    painted = YES;
#ifndef CN1_USE_ARC
    [ops release];
#endif
}

#ifndef CN1_USE_ARC
- (void)dealloc {
    [currentTarget release];
    [upcomingTarget release];
    [super dealloc];
}
#endif

@end

#endif // TARGET_OS_WATCH
