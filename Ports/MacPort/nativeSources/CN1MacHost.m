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

/**
 * Republishes the main window's backing scale to the shared global.
 *
 * <p>Re-read rather than captured once: moving the window between a Retina and
 * a non-Retina display changes the answer while the app runs. The framebuffer
 * path already re-asks, so a stale global left the two disagreeing -- shared
 * rendering and CN1MacPickers divide anchor coordinates by this, so a popover
 * opened after the move landed at the wrong place and at the wrong size.</p>
 *
 * <p>Always the MAIN window, whoever calls: this is one process-wide value, and
 * a secondary window on another display must not claim it.</p>
 */
/**
 * The rendering view a native peer being placed right now belongs to.
 *
 * <p>The window manager sets activeRenderingView for the duration of a window's
 * paint, so during that window's layout -- which is when its peers are created,
 * shown and moved -- this answers that window's view. Outside a paint it falls
 * back to the main surface, which is where an unowned peer belongs anyway.</p>
 *
 * <p>Read it BEFORE dispatching to the main queue: the bracket is cleared when
 * the paint ends, so a block that asks later always gets the main window and the
 * peer lands over the wrong one.</p>
 */
NSView *CN1MacPeerHostView(void) {
    return [CN1MacHost sharedHost].activeRenderingView;
}

/// The window a presentation was told to anchor in, or nil for "work it out".
///
/// A picker or share sheet opened programmatically for a component in a visible
/// but non-key Window supplies coordinates relative to THAT window, so anchoring
/// in the key window put the popover over the wrong one at unrelated
/// coordinates -- and could not present at all when the key window was not the
/// source. The Java side knows the source component's window and names it here.
static NSView *cn1PendingHostView = nil;

void CN1MacSetPendingHostView(NSView *view) {
    cn1PendingHostView = view;
}

NSView *CN1MacKeyRenderingHostView(void) {
    if (cn1PendingHostView != nil) {
        // Consumed, not left standing: it was named for ONE presentation, and a
        // stale one would anchor the next popover in a window nobody named.
        NSView *named = cn1PendingHostView;
        cn1PendingHostView = nil;
        return named;
    }
    NSWindow *key = [NSApp keyWindow];
    if (key != nil && [key.contentView conformsToProtocol:@protocol(NSTextInputClient)]) {
        return key.contentView;
    }
    return [CN1MacHost sharedHost].renderingView;
}

CGFloat CN1MacHostViewScale(NSView *host) {
    extern float scaleValue;
    CGFloat scale = (host != nil && host.window != nil) ? host.window.backingScaleFactor : 0;
    if (scale <= 0) {
        scale = scaleValue > 0 ? scaleValue : 1;
    }
    return scale;
}

void CN1MacRefreshScaleValue(void) {
    extern float scaleValue;
    NSWindow *w = [CN1MacHost sharedHost].window;
    if (w == nil) {
        return;
    }
    CGFloat s = w.backingScaleFactor;
    if (s > 0) {
        scaleValue = (float)s;
    }
}

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

- (NSView *)activeRenderingView {
    return _activeRenderingView != nil ? _activeRenderingView : self.renderingView;
}

- (void)forgetRenderingView:(NSView *)view {
    if (view != nil && _activeRenderingView == view) {
        _activeRenderingView = nil;
    }
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
    // Minimizing the main window is the application's surface going out of
    // view, and it has to reach the framework. It does NOT deactivate a Mac
    // application, so applicationDidHide: never fires for it -- and this window
    // has no delegate, unlike a secondary one -- so without these two the
    // framework carried on painting and running timers into a window nobody
    // could see. Observed rather than delegated because the delegate slot on
    // this window belongs to nobody and should stay that way.
    extern void CN1MacDeliverWindowMiniaturized(BOOL miniaturized);
    [[NSNotificationCenter defaultCenter]
        addObserverForName:NSWindowDidMiniaturizeNotification
                    object:_window
                     queue:[NSOperationQueue mainQueue]
                usingBlock:^(NSNotification *note) {
        CN1MacDeliverWindowMiniaturized(YES);
    }];
    [[NSNotificationCenter defaultCenter]
        addObserverForName:NSWindowDidDeminiaturizeNotification
                    object:_window
                     queue:[NSOperationQueue mainQueue]
                usingBlock:^(NSNotification *note) {
        CN1MacDeliverWindowMiniaturized(NO);
    }];
    // Closing the main window is the application quitting, which is what
    // applicationShouldTerminateAfterLastWindowClosed: says -- but AppKit only
    // consults that when the LAST window goes, so with a secondary Window still
    // open the primary Form was closed with no lifecycle transition and no way
    // back, while the process and that other window carried on. Terminating here
    // routes it through the delegate, so applicationWillTerminate reaches the
    // framework exactly as it does for Quit.
    [[NSNotificationCenter defaultCenter]
        addObserverForName:NSWindowWillCloseNotification
                    object:_window
                     queue:[NSOperationQueue mainQueue]
                usingBlock:^(NSNotification *note) {
        [NSApp terminate:nil];
    }];
    _window.releasedWhenClosed = NO;
    NSString *name = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
    if (name == nil) {
        name = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleName"];
    }
    _window.title = name != nil ? name : @"";

    _renderingView = [[METALView alloc] initWithFrame:frame];
    _renderingView.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
    _window.contentView = _renderingView;
    // The shared Apple code converts between Codename One's device pixels and
    // AppKit's points through this global. It is initialised to one and set on
    // iOS from the screen scale; without setting it here every peer component
    // would be laid out at pixel coordinates read as points, so on a Retina
    // display each one lands at twice its size in the wrong place.
    CN1MacRefreshScaleValue();
    // macos.fixedWindowSize, read back out of the bundle. The builder wrote
    // CN1FixedWindowWidth/Height and nothing read them, so the hint did nothing
    // and the screenshot workflow that sets 1024x685 was comparing frames from a
    // window free to be any size -- which is the one thing a strict pixel
    // comparison cannot survive.
    //
    // In points, because that is what setContentSize: takes; the value in the
    // plist is what the developer asked for and the backing scale turns it into
    // pixels on a Retina display.
    NSNumber *fixedW = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CN1FixedWindowWidth"];
    NSNumber *fixedH = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CN1FixedWindowHeight"];
    if (fixedW != nil && fixedH != nil
            && fixedW.doubleValue > 0 && fixedH.doubleValue > 0) {
        [self setFixedContentSize:NSMakeSize(fixedW.doubleValue, fixedH.doubleValue)];
    }
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
