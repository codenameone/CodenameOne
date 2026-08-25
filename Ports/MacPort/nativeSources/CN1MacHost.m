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

#import "CN1MacHost.h"
#import "METALView.h"
#import "CN1AppKitCompat.h"

/// Default window size when the application does not ask for one. Chosen to be
/// a reasonable desktop window rather than a phone-shaped one, which is what an
/// iOS-derived default would give.
static const CGFloat CN1_MAC_DEFAULT_WIDTH = 1024;
static const CGFloat CN1_MAC_DEFAULT_HEIGHT = 685;

@implementation CN1MacHost {
    NSWindow *_window;
    METALView *_renderingView;
}

+ (CN1MacHost *)sharedHost {
    static CN1MacHost *shared = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        shared = [[CN1MacHost alloc] init];
    });
    return shared;
}

- (NSWindow *)window {
    if (_window == nil) {
        [self buildWindow];
    }
    return _window;
}

- (NSView *)renderingView {
    if (_renderingView == nil) {
        [self buildWindow];
    }
    return _renderingView;
}

- (void)buildWindow {
    if (_window != nil) {
        return;
    }
    // AppKit is not thread-safe and a window must be built on the main thread.
    // The event dispatch thread is the usual first caller here, so this is a
    // real hop rather than a formality.
    if (![NSThread isMainThread]) {
        dispatch_sync(dispatch_get_main_queue(), ^{
            [self buildWindow];
        });
        return;
    }

    NSRect frame = NSMakeRect(0, 0, CN1_MAC_DEFAULT_WIDTH, CN1_MAC_DEFAULT_HEIGHT);
    NSWindowStyleMask style = NSWindowStyleMaskTitled
            | NSWindowStyleMaskClosable
            | NSWindowStyleMaskMiniaturizable
            | NSWindowStyleMaskResizable;
    _window = [[NSWindow alloc] initWithContentRect:frame
                                          styleMask:style
                                            backing:NSBackingStoreBuffered
                                              defer:NO];
    // Remembers where the user left it between launches. One line, and its
    // absence is the kind of thing that makes an app feel unfinished.
    [_window setFrameAutosaveName:@"CN1MainWindow"];
    _window.releasedWhenClosed = NO;
    NSString *name = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
    if (name == nil) {
        name = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleName"];
    }
    _window.title = name != nil ? name : @"";

    _renderingView = [[METALView alloc] initWithFrame:frame];
    _renderingView.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    _window.contentView = _renderingView;
    [_window center];
    [_window makeKeyAndOrderFront:nil];
    [_window makeFirstResponder:_renderingView];
    // The window is created from the application's own thread rather than in
    // response to a user action, so nothing has asked the window server to put
    // this process in front. Without this the app launches, runs and draws --
    // behind whatever the user was already looking at.
    [NSApp activateIgnoringOtherApps:YES];
}

- (int)displayWidth {
    NSView *v = self.renderingView;
    if (v == nil) {
        return (int)CN1_MAC_DEFAULT_WIDTH;
    }
    // Device pixels, not points: Codename One lays out in pixels and a Retina
    // display has two of them per point.
    return (int)(v.bounds.size.width * CN1AppKitBackingScale(v));
}

- (int)displayHeight {
    NSView *v = self.renderingView;
    if (v == nil) {
        return (int)CN1_MAC_DEFAULT_HEIGHT;
    }
    return (int)(v.bounds.size.height * CN1AppKitBackingScale(v));
}

- (void)setFixedContentSize:(NSSize)size {
    NSWindow *w = self.window;
    if (w == nil) {
        return;
    }
    [w setContentSize:size];
    // Equal minimum and maximum is what actually pins it: a resizable window
    // whose bounds merely start at the requested size still drifts when the
    // user or the window server touches it, and a screenshot comparison that
    // strict cannot survive one stray pixel.
    w.contentMinSize = size;
    w.contentMaxSize = size;
    w.styleMask &= ~NSWindowStyleMaskResizable;
    [w center];
}

@end

#endif
