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
#include "TargetConditionals.h"
#if TARGET_OS_OSX

/*
 * The native macOS render driver.
 *
 * Replaces CodenameOne_GLViewController, which is a UIViewController and has no
 * AppKit counterpart. Like the watchOS driver it is a plain NSObject that owns
 * the same ExecutableOp queue and drains it into the platform's rendering view
 * -- here a METALView on a real NSWindow -- and it keeps the same class name and
 * selector surface so the callers and the translated runtime resolve unchanged.
 *
 * The pattern is the codebase's own: watchOS already replaces this class
 * wholesale rather than guarding a UIViewController into something it is not.
 */

#import "CodenameOne_GLViewController.h"
#import "CN1MacHost.h"
#import "METALView.h"
#import "ExecutableOp.h"
#import "DrawString.h"
#import "ClipRect.h"
#import "GLUIImage.h"
#import "CN1Metalcompat.h"
#import "DrawGradientTextureCache.h"
#import "DrawStringTextureCache.h"

/// Set while the process is in the background. The garbage collector reads it
/// to avoid allocating during a suspension the OS may end by terminating us.
/// Defined here because on the UIKit ports it lives in the app delegate, which
/// this port replaces.
BOOL isAppSuspended = NO;
int mallocWhileSuspended = 0;

static CodenameOne_GLViewController *singletonInstance = nil;

@implementation CodenameOne_GLViewController {
    CGRect macFlushRect;
}

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

/// The rendering surface of the application's main window.
- (id)view {
    return [CN1MacHost sharedHost].renderingView;
}

/// Historic name from the OpenGL ES backend. There is no EAGL anything here;
/// the selector is kept because the shared code calls it.
- (id)eaglView {
    return [CN1MacHost sharedHost].renderingView;
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
    return YES;
}

// Frames are driven by the event dispatch thread's own paint cycle rather than
// by a display link, exactly as on the UIKit ports with the display link off.
- (void)startAnimation {
}

- (void)stopAnimation {
}

- (void)drawString:(int)color alpha:(int)alpha font:(CN1Font *)font str:(NSString *)str x:(int)x y:(int)y {
    DrawString *op = [[DrawString alloc] initWithArgs:color a:alpha xpos:x ypos:y s:str f:font];
    [self upcomingAdd:op];
#ifndef CN1_USE_ARC
    [op release];
#endif
}

- (void)flushBuffer:(CN1Image *)buff x:(int)x y:(int)y width:(int)width height:(int)height {
    @synchronized (self) {
        if ([upcomingTarget count] > 0) {
            // Append rather than swap. When several flushes coalesce into one
            // drawFrame a swap drops every batch but the last, leaving stale
            // pixels on screen -- the same reason the watch driver appends.
            [currentTarget addObjectsFromArray:upcomingTarget];
            [upcomingTarget removeAllObjects];
            CGRect r = CGRectMake(x, y, width, height);
            macFlushRect = CGRectIsEmpty(macFlushRect) ? r : CGRectUnion(macFlushRect, r);
        }
    }
    [self drawFrame:CGRectMake(x, y, width, height)];
}

- (void)drawScreen {
    [self drawFrame:CGRectZero];
}

- (void)flushBufferForReadback:(int)x y:(int)y width:(int)width height:(int)height {
    @synchronized (self) {
        if ([upcomingTarget count] > 0) {
            [currentTarget addObjectsFromArray:upcomingTarget];
            [upcomingTarget removeAllObjects];
        }
    }
    painted = NO;
    // On the main thread and waiting, because the caller reads the framebuffer
    // back as soon as this returns; drawing asynchronously would capture the
    // previous frame. allowInactive is YES for the same reason: a screenshot
    // taken while the window is not key still has to reflect what was painted.
    void (^flushBlock)(void) = ^{
        [self drawFrame:CGRectMake(x, y, width, height) allowInactive:YES];
    };
    if ([NSThread isMainThread]) {
        flushBlock();
    } else {
        dispatch_sync(dispatch_get_main_queue(), flushBlock);
    }
}

- (void)drawFrame:(CGRect)rect {
    [self drawFrame:rect allowInactive:NO];
}

- (void)drawFrame:(CGRect)rect allowInactive:(BOOL)allowInactive {
    METALView *v = (METALView *)[CN1MacHost sharedHost].activeRenderingView;
    if (v == nil) {
        return;
    }
    NSArray *ops;
    CGRect flushRect;
    @synchronized (self) {
        ops = [currentTarget copy];
        flushRect = macFlushRect;
        [currentTarget removeAllObjects];
        macFlushRect = CGRectZero;
    }
    [v setFramebuffer];
    // Clamp a screen clip to the region actually being repainted.
    [ClipRect setDrawRect:flushRect];

    // The queue is not all screen drawing. An operation that paints into a
    // mutable image carries that image as its target, and it needs an encoder
    // opened against THAT texture -- running it against the screen encoder the
    // line above opened does not merely draw in the wrong place, it hands Metal
    // a pipeline bound to the wrong attachment and eventually crashes inside
    // objc_msgSend. So the drain switches encoders whenever the target changes,
    // exactly as the UIKit backend does.
    GLUIImage *currentDrainTarget = nil;
    BOOL mutableEncoderOpen = NO;
    for (ExecutableOp *op in ops) {
        GLUIImage *opTarget = [op target];
        if (opTarget != currentDrainTarget) {
            if (mutableEncoderOpen) {
                CN1MetalEndMutableImageDraw(currentDrainTarget);
                mutableEncoderOpen = NO;
            }
            currentDrainTarget = opTarget;
            if (opTarget != nil) {
                mutableEncoderOpen = CN1MetalBeginMutableImageDraw(opTarget);
            }
        }
        // A mutable-image operation whose encoder would not open is skipped
        // rather than run against the screen, which is where it would otherwise
        // land.
        if (opTarget != nil && !mutableEncoderOpen) {
            continue;
        }
        @try {
            [op executeWithClipping];
        } @catch (NSException *e) {
            // Keep draining: one failing operation must not blank the frame.
        }
    }
    if (mutableEncoderOpen) {
        CN1MetalEndMutableImageDraw(currentDrainTarget);
    }

    [ClipRect setDrawRect:CGRectZero];
    // Textures retired during the drain are freed here, after the last
    // operation that could still reference one has run. Without this the
    // caches grow for the life of the process.
    [DrawGradientTextureCache flushDeleted];
    [DrawStringTextureCache flushDeleted];
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

#endif
