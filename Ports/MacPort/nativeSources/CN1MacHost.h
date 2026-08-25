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

/// Pins the window to one size so a screenshot comparison has something stable
/// to compare. Only the compliance suite sets this.
- (void)setFixedContentSize:(NSSize)size;

@end

#endif
#endif
