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
#ifndef CN1MacHost_h
#define CN1MacHost_h
#import <TargetConditionals.h>
#if TARGET_OS_OSX

#import <AppKit/AppKit.h>

/// The application's main window and the view Codename One paints into.
///
/// The counterpart of CN1WatchHost on the watch slice: the render driver asks
/// this for its surface rather than owning one, so the window's lifetime and the
/// drawing queue's stay separate.
///
/// The window is a real `NSWindow` created at launch -- not a scene requested
/// asynchronously and settled afterwards, which is what a Mac Catalyst window
/// requires and why that path needs a free-scene pool, a pending-slot queue and
/// a geometry settling loop. `initWithContentRect:` returns a window.
@interface CN1MacHost : NSObject

+ (CN1MacHost *)sharedHost;

/// The main window. Created on first access, on the main thread.
@property (nonatomic, readonly) NSWindow *window;

/// The METALView inside it. This is what the render driver draws through.
@property (nonatomic, readonly) NSView *renderingView;

/// Size in pixels rather than points: Codename One works in device pixels, and
/// on a Retina display the two differ by the backing scale.
@property (nonatomic, readonly) int displayWidth;
@property (nonatomic, readonly) int displayHeight;

/// The surface the render driver is currently drawing into.
///
/// There is one because the event dispatch thread paints one window at a time,
/// so the drawing pipeline's global encoder is genuinely a single value rather
/// than a limitation. A secondary window sets this for the duration of its own
/// paint and clears it again, and everything else draws into the main window.
@property (nonatomic, assign) NSView *activeRenderingView;

/// Clears activeRenderingView if it is this view, and does nothing otherwise.
///
/// The property is unretained -- a rendering view is owned by its window -- so a
/// window torn down while it is still the claimed one leaves a dangling pointer
/// that the next frame messages. Anything releasing a rendering view calls this
/// first.
- (void)forgetRenderingView:(NSView *)view;

/// Pins the window to one size so a screenshot comparison has something stable
/// to compare. Only the compliance suite sets this.
- (void)setFixedContentSize:(NSSize)size;

@end

/// The rendering view that should host a native popover, sheet or picker.
///
/// The key window's view when that window is one of ours, and the main window's
/// otherwise. Resolved through the key window rather than by threading an id
/// down from Java: this kind of UI opens in response to a click, and the window
/// that received the click is the key one. Conformance to NSTextInputClient is
/// what marks a Codename One rendering view, which needs no header of the
/// view's own.
///
/// Anchoring to the main window instead puts the UI in the wrong window at
/// unrelated coordinates, and cannot present it at all when that window is
/// hidden.
NSView *CN1MacKeyRenderingHostView(void);

/// The backing scale to divide a Codename One pixel coordinate by before
/// handing it to AppKit as a point coordinate in `host`.
///
/// That window's own scale, not the process-wide one: scaleValue tracks the
/// MAIN window and is the wrong divisor for a window on a display of a
/// different density.
CGFloat CN1MacHostViewScale(NSView *host);


#endif
#endif
