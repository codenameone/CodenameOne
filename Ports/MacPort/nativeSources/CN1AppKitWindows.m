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
#import <AppKit/AppKit.h>
#import "METALView.h"
#import "CN1MacHost.h"
#import "CN1AppKitCompat.h"
#import "CodenameOne_GLViewController.h"
#include "cn1_globals.h"
#include "java_lang_String.h"

// Shared with Mac Catalyst; defined in IOSNative.m.
extern void CN1MacWindowDeliverClose(int windowId);
extern void CN1MacWindowDeliverClosed(int windowId);
extern void CN1MacWindowDeliverMonitorsChanged(void);
extern void CN1MacWindowDeliverMoved(int windowId);
extern void CN1MacWindowDeliverWindowMonitorChanged(int windowId);
extern void CN1MacWindowDeliverFocus(int windowId, BOOL gained);
extern void CN1MacWindowDeliverContentReady(int windowId);
extern void CN1MacWindowDeliverVisibility(int windowId, BOOL shown);
extern void CN1MacWindowDeliverResize(int windowId, int width, int height);

/*
 * Native macOS desktop windows.
 *
 * Where the Mac Catalyst implementation needs a free-scene pool, a pending-slot
 * queue, NSUserActivity activation requests with generation counters and a
 * multi-step asynchronous settle loop -- because Catalyst cannot simply create a
 * window -- AppKit returns one synchronously from an initializer. That is why
 * this file is a fraction of the size of CN1MacWindows.m and why none of that
 * machinery appears here.
 */

@interface CN1MacWindowRecord : NSObject <NSWindowDelegate>
@property (nonatomic, assign) int windowId;
@property (nonatomic, assign) int slot;
@property (nonatomic, retain) NSWindow *window;
@property (nonatomic, retain) METALView *view;
@property (nonatomic, assign) BOOL inputEnabled;
@property (nonatomic, assign) BOOL utility;
@property (nonatomic, assign) BOOL disposed;
@property (nonatomic, assign) NSModalSession modalSession;
/// The owner this window is to be attached to on its first show. Held rather
/// than attached at creation because addChildWindow: orders the child in
/// immediately, and a window created hidden must not appear because it acquired
/// an owner. assign, not retain: AppKit owns its windows and a retain here would
/// be a cycle through the child's own delegate.
@property (nonatomic, assign) NSWindow *pendingOwner;
/// Set when the application was hidden while this window was on screen, so
/// unhiding restores exactly the windows the hide took away and no others. A
/// window already miniaturized when the hide arrived reported itself hidden
/// then and must stay that way.
@property (nonatomic, assign) BOOL hiddenByApp;
@end

/// Reports a window's visibility and that of every window it owns.
/// Defined below the window table; declared here because the record's own
/// delegate methods are the first callers.
static void cn1DeliverVisibilityTree(CN1MacWindowRecord *rec, BOOL shown);

@implementation CN1MacWindowRecord

- (BOOL)windowShouldClose:(NSWindow *)sender {
    // The framework decides whether the window actually closes -- it may have a
    // close handler that vetoes -- so the close is reported and NO returned. If
    // it agrees, it calls back through dispose().
    CN1MacWindowDeliverClose(self.windowId);
    return NO;
}

- (void)windowWillClose:(NSNotification *)notification {
    CN1MacWindowDeliverClosed(self.windowId);
}

- (void)windowDidResize:(NSNotification *)notification {
    METALView *v = self.view;
    CGFloat scale = CN1AppKitBackingScale(v);
    NSSize size = v.bounds.size;
    int w = (int)(size.width * scale);
    int h = (int)(size.height * scale);
    [v updateFrameBufferSize:w h:h];
    CN1MacWindowDeliverResize(self.windowId, w, h);
}

- (void)windowDidBecomeKey:(NSNotification *)notification {
    CN1MacWindowDeliverFocus(self.windowId, YES);
}

- (void)windowDidResignKey:(NSNotification *)notification {
    CN1MacWindowDeliverFocus(self.windowId, NO);
}

- (void)windowDidMove:(NSNotification *)notification {
    // Nothing else reports a user drag, so without this Window.moved() never
    // runs and an application cannot persist where its windows were left.
    CN1MacWindowDeliverMoved(self.windowId);
}

- (void)windowDidMiniaturize:(NSNotification *)notification {
    // A minimized window is a hidden one as far as the framework is concerned:
    // nativeVisible stays true otherwise, so it keeps painting and animating
    // into a window nobody can see, and the Minimized event never fires. The
    // same callback the show/hide path uses, so the owner cascade is identical
    // whichever way the window went away.
    cn1DeliverVisibilityTree(self, NO);
}

- (void)windowDidDeminiaturize:(NSNotification *)notification {
    cn1DeliverVisibilityTree(self, YES);
}

- (void)windowDidChangeScreen:(NSNotification *)notification {
    // This window's own change, not the topology's. Reporting a drag onto a
    // second display as monitorsChanged fired every application monitor
    // listener and relayed out every window, for something that happened to
    // one of them; monitorsChanged is for a display being attached, removed or
    // reconfigured.
    CN1MacWindowDeliverWindowMonitorChanged(self.windowId);
}

- (void)windowDidChangeBackingProperties:(NSNotification *)notification {
    [self windowDidResize:notification];
    CN1MacWindowDeliverWindowMonitorChanged(self.windowId);
}

#ifndef CN1_USE_ARC
- (void)dealloc {
    [_window release];
    [_view release];
    [super dealloc];
}
#endif

@end

/// Slot table. A slot is never reused while a record is alive, and a destroyed
/// window leaves NSNull behind, so a stale slot from Java fails as a lookup miss
/// rather than as a wild pointer or as an operation on somebody else's window.
static NSMutableArray *cn1MacWindows = nil;
static BOOL cn1MacWatchingScreens = NO;

static NSMutableArray *cn1WindowTable(void) {
    if (cn1MacWindows == nil) {
        cn1MacWindows = [[NSMutableArray alloc] init];
    }
    return cn1MacWindows;
}

static CN1MacWindowRecord *cn1WindowAt(int slot) {
    NSMutableArray *table = cn1WindowTable();
    if (slot < 0 || slot >= (int)table.count) {
        return nil;
    }
    id entry = [table objectAtIndex:slot];
    return entry == [NSNull null] ? nil : (CN1MacWindowRecord *)entry;
}

/// The record owning this AppKit window, or nil for a window we did not create.
static CN1MacWindowRecord *cn1RecordForWindow(NSWindow *window) {
    if (window == nil) {
        return nil;
    }
    for (id entry in cn1WindowTable()) {
        if (entry == [NSNull null]) {
            continue;
        }
        CN1MacWindowRecord *rec = (CN1MacWindowRecord *)entry;
        if (rec.window == window) {
            return rec;
        }
    }
    return nil;
}

/// Reports a window's visibility, and its owned windows' along with it.
///
/// AppKit takes child windows off screen with their owner and brings them back
/// with it, but posts no notification for the child, so reporting only the owner
/// left a child window with nativeVisible still true: painting, animating and
/// answering isWindowShowing() while absent from the screen. The inherited
/// cascade in MacWindowManager cannot cover this -- it walks a Catalyst peer
/// list that AppKitWindowManager never populates -- so the cascade belongs here,
/// where AppKit's own ownership graph is the source of truth.
///
/// The child list is snapshotted because delivering runs Java, which can close a
/// window and mutate the collection being enumerated.
static void cn1DeliverVisibilityTree(CN1MacWindowRecord *rec, BOOL shown) {
    if (rec == nil || rec.window == nil) {
        return;
    }
    CN1MacWindowDeliverVisibility(rec.windowId, shown);
    NSArray *children = [NSArray arrayWithArray:rec.window.childWindows];
    for (NSWindow *child in children) {
        CN1MacWindowRecord *childRec = cn1RecordForWindow(child);
        if (childRec != nil && !childRec.disposed) {
            cn1DeliverVisibilityTree(childRec, shown);
        }
    }
}

/// Runs a block on the main thread and waits. Every AppKit call here has to be
/// on the main thread, and the framework calls in from the event dispatch
/// thread; waiting rather than dispatching asynchronously is what lets a
/// getter return an answer at all.
static void cn1OnMain(dispatch_block_t block) {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_sync(dispatch_get_main_queue(), block);
    }
}

static CGFloat cn1DesktopScale(void);

/// Codename One works in device pixels and AppKit in points, so every geometry
/// value crossing the bridge is scaled by the window's backing factor.
/// The window's OWN backing scale, for drawable sizes only.
///
/// Never for a desktop coordinate: see cn1DesktopScale.
static CGFloat cn1WindowScale(CN1MacWindowRecord *rec) {
    if (rec != nil && rec.window != nil) {
        return rec.window.backingScaleFactor;
    }
    // No window means no drawable, so the answer is arbitrary -- but it must not
    // be ARBITRARY PER CALL. mainScreen follows the key window, so the fallback
    // changed as the user moved focus between displays of different densities.
    return cn1DesktopScale();
}

/// The single scale every desktop coordinate is expressed in.
///
/// AppKit lays every screen out in ONE point space whose origin is the primary
/// screen's bottom left, so a position only means something when the whole
/// topology shares one conversion. Scaling each screen's origin by its own
/// backing factor produced a space where the rectangles overlap: a 1440pt
/// Retina primary reports 2880 wide, while the 1x display to its right still
/// reports its origin as 1440, so placing a window there put it back on the
/// primary. The primary's factor is the one that pairs with
/// cn1PrimaryScreenHeight, which the y flip already uses for exactly this
/// reason.
static CGFloat cn1DesktopScale(void) {
    // screens[0], not mainScreen. NSScreen.mainScreen is the screen holding the
    // KEY WINDOW, not the primary one, so on a mixed-DPI setup this changed the
    // unit of every window bound and monitor rectangle as focus moved between a
    // Retina and a 1x display -- while the y flip above stayed on screens[0],
    // leaving the two halves of one conversion disagreeing. A restored window
    // then jumps or resizes for no reason the user did anything to cause.
    NSArray<NSScreen *> *screens = [NSScreen screens];
    if (screens.count == 0) {
        return 1;
    }
    CGFloat s = [screens objectAtIndex:0].backingScaleFactor;
    return s > 0 ? s : 1;
}

/// AppKit's global coordinate space has its origin at the primary screen's
/// bottom left; Codename One's has it at the top left. Converting needs the
/// primary screen's height rather than the window's own screen, which is the
/// classic multi-display bug in a Mac port -- and one that never shows up on a
/// single display machine.
static CGFloat cn1PrimaryScreenHeight(void) {
    NSArray<NSScreen *> *screens = [NSScreen screens];
    if (screens.count == 0) {
        return 0;
    }
    return [screens objectAtIndex:0].frame.size.height;
}

static NSRect cn1ToAppKitFrame(NSRect topLeftFrame) {
    NSRect r = topLeftFrame;
    r.origin.y = cn1PrimaryScreenHeight() - topLeftFrame.origin.y - topLeftFrame.size.height;
    return r;
}

static NSRect cn1FromAppKitFrame(NSRect appKitFrame) {
    NSRect r = appKitFrame;
    r.origin.y = cn1PrimaryScreenHeight() - appKitFrame.origin.y - appKitFrame.size.height;
    return r;
}

/// Blocks cannot capture a C array, so geometry crossing the main-thread hop
/// travels in a struct.
typedef struct {
    int x;
    int y;
    int width;
    int height;
} CN1MacBounds;

static NSScreen *cn1ScreenAt(int monitor) {
    NSArray<NSScreen *> *screens = [NSScreen screens];
    if (screens.count == 0) {
        return nil;
    }
    if (monitor < 0 || monitor >= (int)screens.count) {
        return [screens objectAtIndex:0];
    }
    return [screens objectAtIndex:monitor];
}

static int cn1IndexOfScreen(NSScreen *screen) {
    NSArray<NSScreen *> *screens = [NSScreen screens];
    NSUInteger idx = screen == nil ? NSNotFound : [screens indexOfObject:screen];
    return idx == NSNotFound ? 0 : (int)idx;
}

/// Builds the window, or rebuilds it around an existing view when the utility
/// flag changes -- AppKit decides panel behaviour at initialization, so there is
/// no way to promote a window to a panel in place.
static NSWindow *cn1MakeWindow(NSRect contentRect, BOOL decorated, BOOL resizable, BOOL utility) {
    NSWindowStyleMask mask;
    if (decorated) {
        mask = NSWindowStyleMaskTitled | NSWindowStyleMaskClosable
             | NSWindowStyleMaskMiniaturizable;
        if (resizable) {
            mask |= NSWindowStyleMaskResizable;
        }
        if (utility) {
            mask |= NSWindowStyleMaskUtilityWindow;
        }
    } else {
        // A real frameless window, which is the thing Mac Catalyst cannot do:
        // there it can only hide the title bar's contents and the frame stays.
        mask = NSWindowStyleMaskBorderless;
        if (resizable) {
            mask |= NSWindowStyleMaskResizable;
        }
    }
    Class cls = utility ? [NSPanel class] : [NSWindow class];
    NSWindow *w = [[cls alloc] initWithContentRect:contentRect
                                         styleMask:mask
                                           backing:NSBackingStoreBuffered
                                             defer:NO];
    w.releasedWhenClosed = NO;
    if (utility) {
        ((NSPanel *)w).floatingPanel = NO;
        ((NSPanel *)w).becomesKeyOnlyIfNeeded = NO;
    }
    if (!decorated) {
        w.movableByWindowBackground = YES;
    }
    return w;
}

// ---- natives -------------------------------------------------------------

JAVA_INT com_codename1_impl_mac_MacNative_macWindowCreate___int_java_lang_String_int_int_int_int_boolean_boolean_int_boolean_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT windowId, JAVA_OBJECT titleObj, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_BOOLEAN decorated, JAVA_BOOLEAN resizable, JAVA_INT ownerSlot, JAVA_BOOLEAN positionSet) {
    NSString *title = titleObj != JAVA_NULL
        ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG titleObj)
        : @"";
    __block int slot = -1;
    cn1OnMain(^{
        // The desktop scale, because x/y are desktop coordinates -- the same
        // unit setBounds and the monitor rectangles use. Reading mainScreen here
        // meant a window created while a 1x display held focus was placed and
        // sized in that display's unit and then reported back in the primary's.
        CGFloat scale = cn1DesktopScale();
        // Creation takes the CONTENT rect, unlike setBounds/getBounds which are
        // the outer-frame pair the WindowManager SPI specifies. Deliberate: the
        // size an application asks for at creation is the area it draws into,
        // and shrinking it by the title bar would make every Window come up
        // smaller than it asked for. Once the window exists, geometry travels as
        // the outer rectangle so a get/set round trip agrees with itself.
        NSRect content = NSMakeRect(x / scale, y / scale,
                                    MAX(width / scale, 1), MAX(height / scale, 1));
        NSWindow *w = cn1MakeWindow(content, decorated != 0, resizable != 0, NO);
        if (w == nil) {
            return;
        }
        if (positionSet != 0) {
            NSRect frame = [w frameRectForContentRect:cn1ToAppKitFrame(content)];
            [w setFrame:frame display:NO];
            // Position is desktop-space and converts against the primary
            // screen's scale, but the SIZE is this window's own drawable pixels.
            // Which display that is cannot be known until the window has
            // landed, so the size is corrected here: a 400-pixel window asked
            // for on a 1x secondary came up 200 points wide because it had been
            // divided by the 2x primary's scale.
            CGFloat destScale = w.backingScaleFactor;
            if (destScale > 0 && destScale != scale) {
                NSRect placed = [w contentRectForFrameRect:w.frame];
                CGFloat top = placed.origin.y + placed.size.height;
                placed.size = NSMakeSize(MAX(width / destScale, 1), MAX(height / destScale, 1));
                // AppKit measures from the bottom, so the origin moves to keep
                // the top edge -- the one the caller positioned -- where it was
                // put.
                placed.origin.y = top - placed.size.height;
                [w setFrame:[w frameRectForContentRect:placed] display:NO];
            }
        } else {
            [w center];
        }
        // Re-read rather than reused: the window may have been resized for its
        // display just above, and the view and its framebuffer have to follow
        // what the window actually is.
        content = [w contentRectForFrameRect:w.frame];
        w.title = title;

        METALView *view = [[METALView alloc] initWithFrame:NSMakeRect(0, 0,
                                                                      content.size.width,
                                                                      content.size.height)];
        view.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
        view.cn1WindowId = windowId;
        w.contentView = view;
        [w makeFirstResponder:view];

        CN1MacWindowRecord *rec = [[CN1MacWindowRecord alloc] init];
        rec.windowId = windowId;
        rec.window = w;
        rec.view = view;
        rec.inputEnabled = YES;
        w.delegate = rec;

        NSMutableArray *table = cn1WindowTable();
        slot = (int)table.count;
        rec.slot = slot;
        [table addObject:rec];

        // Ownership, once the window exists and is in the table. AppKit keeps a
        // child above its owner and carries it through the owner's minimize,
        // hide and close -- which is the whole of what an owned dialog or a
        // palette means, and none of it happened while the owner was recorded
        // only on the Java Peer.
        NSWindow *owner = nil;
        if (ownerSlot == -2) {
            owner = [CN1MacHost sharedHost].window;
        } else if (ownerSlot >= 0) {
            CN1MacWindowRecord *ownerRec = cn1WindowAt(ownerSlot);
            owner = ownerRec == nil ? nil : ownerRec.window;
        }
        // addChildWindow: orders the child in immediately, so it is deferred to
        // the first show rather than applied here -- a window created hidden must
        // not appear because it acquired an owner.
        rec.pendingOwner = owner;

        CGFloat s = w.backingScaleFactor;
        [view updateFrameBufferSize:(int)(content.size.width * s)
                                  h:(int)(content.size.height * s)];
#ifndef CN1_USE_ARC
        [rec release];
        [view release];
        [w release];
#endif
    });
    return slot;
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowDestroy___int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        rec.disposed = YES;
        // Before the window goes: activeRenderingView does not retain what it
        // points at, and a paint that claimed this window and never reached its
        // flush -- the window was closed under it -- left the claim standing.
        // The next frame then drew into a freed view, which is a main-thread
        // segfault one test after the one that closed the window.
        [[CN1MacHost sharedHost] forgetRenderingView:rec.view];
        if (rec.modalSession != NULL) {
            [NSApp endModalSession:rec.modalSession];
            rec.modalSession = NULL;
        }
        rec.window.delegate = nil;
        [rec.window orderOut:nil];
        [rec.window close];
        [cn1WindowTable() replaceObjectAtIndex:slot withObject:[NSNull null]];
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowShow___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_BOOLEAN visible) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        if (visible != 0) {
            // Attached on the first show, so the child is ordered in with its
            // owner rather than ahead of it. Cleared afterwards: re-adding an
            // existing child reorders it, which would jump a palette back in
            // front every time its window is shown again.
            if (rec.pendingOwner != nil && rec.pendingOwner != rec.window) {
                [rec.pendingOwner addChildWindow:rec.window ordered:NSWindowAbove];
                rec.pendingOwner = nil;
            }
            [rec.window makeKeyAndOrderFront:nil];
            // The application's own decision outranks the app-hide bookkeeping:
            // whatever unhiding would have done for this window, it has just
            // been said explicitly.
            rec.hiddenByApp = NO;
            cn1DeliverVisibilityTree(rec, YES);
            // AppKit hands back a usable window synchronously, so the content is
            // ready as soon as it is on screen. Catalyst has to wait for a scene
            // to activate before it can say this.
            CN1MacWindowDeliverContentReady(rec.windowId);
        } else {
            [rec.window orderOut:nil];
            // Cleared for the same reason, and this is the direction that
            // matters: hiding a window while the application itself is hidden
            // must not be undone by the later unhide.
            rec.hiddenByApp = NO;
            cn1DeliverVisibilityTree(rec, NO);
        }
    });
}

JAVA_BOOLEAN com_codename1_impl_mac_MacNative_macWindowReopen___int_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    __block BOOL ok = NO;
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil || rec.disposed) {
            return;
        }
        [rec.window makeKeyAndOrderFront:nil];
        rec.hiddenByApp = NO;
        cn1DeliverVisibilityTree(rec, YES);
        ok = YES;
    });
    return ok ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetTitle___int_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_OBJECT titleObj) {
    NSString *title = titleObj != JAVA_NULL
        ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG titleObj)
        : @"";
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        rec.window.title = title;
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetBounds___int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        // The OUTER frame, matching what getBounds returns. Treating the
        // request as a content rect and expanding it by the chrome grew the
        // window by its title bar on every restore, because the value being
        // restored came from getBounds -- the round trip has to agree, and the
        // SPI says both ends of it are the rectangle including chrome.
        CGFloat scale = cn1DesktopScale();
        NSRect frame = NSMakeRect(x / scale, y / scale,
                                  MAX(width / scale, 1), MAX(height / scale, 1));
        [rec.window setFrame:cn1ToAppKitFrame(frame) display:YES];
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowGetBounds___int_int_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_OBJECT outArr) {
    if (outArr == JAVA_NULL) {
        return;
    }
    JAVA_ARRAY arr = (JAVA_ARRAY)outArr;
    if (arr->length < 4) {
        return;
    }
    __block CN1MacBounds b = {0, 0, 0, 0};
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        // The FRAME, not the content rect: WindowManager.getBounds is documented
        // to include native chrome, setBounds accepts the same rectangle, and
        // the Linux and Windows ports both report the outer one. Returning the
        // content rect made the round trip disagree with the request by exactly
        // the title bar, which is what breaks restoring a window where the user
        // left it. getWidth/getHeight remain the drawable-size APIs.
        CGFloat scale = cn1DesktopScale();
        NSRect topLeft = cn1FromAppKitFrame(rec.window.frame);
        b.x = (int)(topLeft.origin.x * scale);
        b.y = (int)(topLeft.origin.y * scale);
        b.width = (int)(topLeft.size.width * scale);
        b.height = (int)(topLeft.size.height * scale);
    });
    JAVA_ARRAY_INT *data = (JAVA_ARRAY_INT *)arr->data;
    data[0] = b.x;
    data[1] = b.y;
    data[2] = b.width;
    data[3] = b.height;
}

JAVA_BOOLEAN com_codename1_impl_mac_MacNative_macMainWindowGetBounds___int_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT outArr) {
    if (outArr == JAVA_NULL) {
        return JAVA_FALSE;
    }
    JAVA_ARRAY arr = (JAVA_ARRAY)outArr;
    if (arr->length < 4) {
        return JAVA_FALSE;
    }
    __block CN1MacBounds b = {0, 0, 0, 0};
    __block BOOL ok = NO;
    cn1OnMain(^{
        NSWindow *w = [CN1MacHost sharedHost].window;
        if (w == nil) {
            return;
        }
        // The shared desktop scale, not this window's own. Monitor rectangles
        // and every secondary window's bounds are expressed in it, so a main
        // window on a display of a different density reported a rectangle in a
        // different space -- and Window.centerOver(Form) then computed a
        // position in one space that was applied in the other, landing the
        // window on the wrong monitor. The frame rather than the content rect,
        // for the same reason getBounds returns the frame.
        CGFloat scale = cn1DesktopScale();
        NSRect topLeft = cn1FromAppKitFrame(w.frame);
        b.x = (int)(topLeft.origin.x * scale);
        b.y = (int)(topLeft.origin.y * scale);
        b.width = (int)(topLeft.size.width * scale);
        b.height = (int)(topLeft.size.height * scale);
        ok = YES;
    });
    if (!ok) {
        return JAVA_FALSE;
    }
    JAVA_ARRAY_INT *data = (JAVA_ARRAY_INT *)arr->data;
    data[0] = b.x;
    data[1] = b.y;
    data[2] = b.width;
    data[3] = b.height;
    return JAVA_TRUE;
}

JAVA_INT com_codename1_impl_mac_MacNative_macWindowGetWidth___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    __block int value = 0;
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec != nil) {
            value = (int)(rec.view.bounds.size.width * cn1WindowScale(rec));
        }
    });
    return value;
}

JAVA_INT com_codename1_impl_mac_MacNative_macWindowGetHeight___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    __block int value = 0;
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec != nil) {
            value = (int)(rec.view.bounds.size.height * cn1WindowScale(rec));
        }
    });
    return value;
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetResizable___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_BOOLEAN resizable) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        if (resizable != 0) {
            rec.window.styleMask |= NSWindowStyleMaskResizable;
        } else {
            rec.window.styleMask &= ~NSWindowStyleMaskResizable;
        }
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetDecorated___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_BOOLEAN decorated) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        NSWindow *w = rec.window;
        BOOL resizable = (w.styleMask & NSWindowStyleMaskResizable) != 0;
        if (decorated != 0) {
            NSWindowStyleMask mask = NSWindowStyleMaskTitled | NSWindowStyleMaskClosable
                                   | NSWindowStyleMaskMiniaturizable;
            if (resizable) {
                mask |= NSWindowStyleMaskResizable;
            }
            if (rec.utility) {
                mask |= NSWindowStyleMaskUtilityWindow;
            }
            w.styleMask = mask;
            w.movableByWindowBackground = NO;
        } else {
            w.styleMask = resizable
                ? (NSWindowStyleMaskBorderless | NSWindowStyleMaskResizable)
                : NSWindowStyleMaskBorderless;
            // Without this an undecorated window cannot be moved at all, since
            // there is no title bar left to drag it by.
            w.movableByWindowBackground = YES;
        }
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetMinimumSize___int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_INT width, JAVA_INT height) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        // Zero or less clears the constraint, per the WindowManager contract.
        // Scaling it instead installed a nonpositive minimum: the old constraint
        // was not lifted, and AppKit is entitled to reject a negative one, so a
        // window that dropped its minimum kept the previous one.
        if (width <= 0 || height <= 0) {
            rec.window.contentMinSize = NSZeroSize;
            return;
        }
        // The WINDOW's own scale, not the desktop one. A minimum size is compared
        // against what getWidth/getHeight report, and those are this window's
        // drawable pixels -- so on a 1x secondary window under a 2x primary, a
        // 400-pixel minimum divided by the desktop scale became 200. Position is
        // desktop-space and size is drawable-space; this is a size.
        CGFloat scale = cn1WindowScale(rec);
        rec.window.contentMinSize = NSMakeSize(width / scale, height / scale);
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetAlwaysOnTop___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_BOOLEAN alwaysOnTop) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec != nil) {
            rec.window.level = alwaysOnTop != 0 ? NSFloatingWindowLevel : NSNormalWindowLevel;
        }
    });
}

/// Defined below, next to the modality natives it belongs with; the utility
/// rebuild needs it too.
static void cn1SetWindowChromeEnabled(NSWindow *w, BOOL enabled);

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetUtility___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_BOOLEAN utility) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil || rec.utility == (utility != 0)) {
            return;
        }
        // A panel and a window are different classes and AppKit decides which at
        // initialization, so the flag is applied by rebuilding the window around
        // the view that is already rendering into it.
        NSWindow *old = rec.window;
        BOOL wasVisible = old.isVisible;
        BOOL decorated = (old.styleMask & NSWindowStyleMaskTitled) != 0;
        BOOL resizable = (old.styleMask & NSWindowStyleMaskResizable) != 0;
        NSRect content = [old contentRectForFrameRect:old.frame];
        NSString *title = old.title;
        METALView *view = rec.view;
        // Everything else the window had been told, carried across. The
        // replacement is a different NSWindow, so anything not copied here is
        // silently lost -- and the framework does not re-apply it, because from
        // its side nothing changed. Window.show() sets always-on-top before
        // converting to a utility window, so dropping the level left a window
        // whose isAlwaysOnTop() is true and which does not float.
        NSWindowLevel level = old.level;
        NSSize minSize = old.contentMinSize;
        NSWindow *owner = old.parentWindow;
        // The windows this one OWNS, snapshotted before the close below detaches
        // them. Only the parent link was carried across, so a rebuild left every
        // child attached to a closed window: they stopped floating above their
        // owner, and stopped hiding, minimizing and closing with it. The Java
        // ownership graph still said they were children, so nothing on that side
        // ever re-established the link. Copied rather than held, because
        // childWindows is a live array that the detaching below mutates.
        NSArray *ownedChildren = [NSArray arrayWithArray:old.childWindows];
        // A modal session belongs to the window it was begun for, so it cannot
        // outlive this one: left alone it would keep the CLOSED window modal and
        // leave the visible replacement ordinary. Ended here, restarted below.
        BOOL wasModal = rec.modalSession != NULL;
        if (wasModal) {
            [NSApp endModalSession:rec.modalSession];
            rec.modalSession = NULL;
        }

        [view removeFromSuperview];
        old.delegate = nil;
        if (owner != nil) {
            [owner removeChildWindow:old];
        }
        for (NSWindow *child in ownedChildren) {
            [old removeChildWindow:child];
        }
        [old orderOut:nil];
        [old close];

        NSWindow *fresh = cn1MakeWindow(content, decorated, resizable, utility != 0);
        fresh.title = title;
        [fresh setFrame:[fresh frameRectForContentRect:content] display:NO];
        fresh.contentView = view;
        fresh.delegate = rec;
        fresh.level = level;
        fresh.contentMinSize = minSize;
        rec.window = fresh;
        rec.utility = utility != 0;
        if (owner != nil) {
            // Re-attached only if it was already attached; a window still
            // waiting for its first show keeps its pendingOwner instead, so the
            // conversion does not order it in early.
            [owner addChildWindow:fresh ordered:NSWindowAbove];
        }
        // Re-adopted by the replacement, so ownership survives the rebuild in
        // both directions rather than only upwards.
        for (NSWindow *child in ownedChildren) {
            [fresh addChildWindow:child ordered:NSWindowAbove];
        }
        // The view carries cn1InputEnabled across with it, but the chrome is the
        // new window's own: a window blocked by a modal dialog would otherwise
        // come back with live close and minimize buttons.
        cn1SetWindowChromeEnabled(fresh, rec.inputEnabled);
        if (wasVisible) {
            [fresh makeKeyAndOrderFront:nil];
        }
        [fresh makeFirstResponder:view];
        if (wasModal) {
            rec.modalSession = [NSApp beginModalSessionForWindow:fresh];
        }
#ifndef CN1_USE_ARC
        [fresh release];
#endif
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowMinimize___int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    cn1OnMain(^{
        [cn1WindowAt(slot).window miniaturize:nil];
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowRestore___int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        if (rec.window.isMiniaturized) {
            [rec.window deminiaturize:nil];
        }
        // Deliberately NOT un-maximized. restore() undoes minimize and nothing
        // else -- toggleMaximize() owns that independently -- and deminiaturize:
        // already brings a window back to whatever size it had. Zooming after it
        // took a window that was maximized when the user minimized it and
        // un-maximized it on the way back.
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowToggleMaximize___int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    cn1OnMain(^{
        [cn1WindowAt(slot).window zoom:nil];
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowRequestFocus___int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec != nil) {
            [rec.window makeKeyAndOrderFront:nil];
            [rec.window makeFirstResponder:rec.view];
        }
    });
}

/// The window's own chrome, which the framework's input filter cannot reach.
///
/// Disabling the rendering view stops content events and nothing else: the title
/// bar belongs to AppKit, so the close button of a window blocked by a modal
/// dialog still reached the application and disposed it. That is the exact case
/// WindowManager.setInputEnabled documents as its reason for existing.
static void cn1SetWindowChromeEnabled(NSWindow *w, BOOL enabled) {
    if (w == nil) {
        return;
    }
    NSWindowButton buttons[] = {
        NSWindowCloseButton, NSWindowMiniaturizeButton, NSWindowZoomButton
    };
    for (unsigned i = 0; i < sizeof(buttons) / sizeof(buttons[0]); i++) {
        NSButton *b = [w standardWindowButton:buttons[i]];
        if (b != nil) {
            b.enabled = enabled;
        }
    }
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetInputEnabled___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_BOOLEAN enabled) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        rec.inputEnabled = enabled != 0;
        rec.view.cn1InputEnabled = enabled != 0;
        cn1SetWindowChromeEnabled(rec.window, enabled != 0);
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macMainWindowSetInputEnabled___boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_BOOLEAN enabled) {
    cn1OnMain(^{
        METALView *v = (METALView *)[CN1MacHost sharedHost].renderingView;
        v.cn1InputEnabled = enabled != 0;
        // The main window has a title bar too, and blocking it is the whole
        // point when a modal dialog is up.
        cn1SetWindowChromeEnabled([CN1MacHost sharedHost].window, enabled != 0);
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetModal___int_boolean_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_BOOLEAN modal, JAVA_BOOLEAN applicationWide) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        // An AppKit modal session stops EVERY other window taking focus, which
        // is what MODALITY_APPLICATION means and is wrong for MODALITY_WINDOW:
        // that one blocks the dialog's owner only, and an unrelated top-level
        // window has to stay usable. Window modality is left to the framework,
        // which disables the blocked window's input -- and now its chrome too --
        // through setInputEnabled.
        if (modal != 0 && applicationWide == 0) {
            if (rec.modalSession != NULL) {
                [NSApp endModalSession:rec.modalSession];
                rec.modalSession = NULL;
            }
            return;
        }
        if (modal != 0) {
            if (rec.modalSession == NULL) {
                // A session rather than runModalForWindow:, which would spin its
                // own event loop and block the one the application is already
                // running. The framework enforces the modality itself; this is
                // what makes the window behave like a Mac modal window while it
                // does.
                rec.modalSession = [NSApp beginModalSessionForWindow:rec.window];
            }
        } else if (rec.modalSession != NULL) {
            [NSApp endModalSession:rec.modalSession];
            rec.modalSession = NULL;
        }
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetIcon___int_int_1ARRAY_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_OBJECT argbArr, JAVA_INT width, JAVA_INT height) {
    if (argbArr == JAVA_NULL || width <= 0 || height <= 0) {
        return;
    }
    JAVA_ARRAY arr = (JAVA_ARRAY)argbArr;
    if (arr->length < width * height) {
        return;
    }
    NSImage *image = CN1AppKitNSImageFromARGB((unsigned int *)arr->data, width, height);
    if (image == nil) {
        return;
    }
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        // A window's icon on macOS is its represented file's icon, and a window
        // with no file has none. Setting the application's icon is the closest
        // honest equivalent and is what the user actually sees.
        [NSApp setApplicationIconImage:image];
    });
}

// ---- rendering -----------------------------------------------------------

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowBeginPaint___int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    CN1MacWindowRecord *rec = cn1WindowAt(slot);
    if (rec != nil) {
        [CN1MacHost sharedHost].activeRenderingView = rec.view;
    }
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowFlush___int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1MacWindowRecord *rec = cn1WindowAt(slot);
    if (rec == nil) {
        // The window went away between beginPaint and here. The claim it made
        // has to be dropped even though there is nothing left to flush.
        [CN1MacHost sharedHost].activeRenderingView = nil;
        return;
    }
    [CN1MacHost sharedHost].activeRenderingView = rec.view;
    [[CodenameOne_GLViewController instance] flushBuffer:nil x:x y:y width:width height:height];
    // Cleared so anything that paints without claiming a window -- the main
    // window's own cycle, most of all -- cannot land in this one's drawable.
    [CN1MacHost sharedHost].activeRenderingView = nil;
}

JAVA_BOOLEAN com_codename1_impl_mac_MacNative_macWindowCapture___int_int_1ARRAY_int_int_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_OBJECT argbArr, JAVA_INT width, JAVA_INT height) {
    if (argbArr == JAVA_NULL || width <= 0 || height <= 0) {
        return JAVA_FALSE;
    }
    JAVA_ARRAY arr = (JAVA_ARRAY)argbArr;
    if (arr->length < width * height) {
        return JAVA_FALSE;
    }
    CN1MacWindowRecord *rec = cn1WindowAt(slot);
    if (rec == nil) {
        return JAVA_FALSE;
    }
    // Read back from the window's own retained render target rather than from a
    // shared raster: each window has its own, which is the whole reason this
    // port needs no offscreen image per window.
    //
    // This is the texture, so it holds what Codename One drew and NOT the native
    // peer views AppKit composites above it -- a BrowserComponent or a video
    // player in the window is absent from the result. Compositing them in is not
    // one call: cacheDisplayInRect: does not render a layer-hosted CAMetalLayer,
    // and the peers that matter each need their own snapshot API (WKWebView's is
    // asynchronous, AVPlayerView has none), so a correct composite is a change of
    // its own rather than a line here. Documented as a limitation in
    // Desktop-Windows.asciidoc rather than left for someone to discover.
    return [rec.view readbackInto:(unsigned int *)arr->data width:width height:height]
        ? JAVA_TRUE : JAVA_FALSE;
}

/// Report every on-screen window hidden when the application is hidden, and
/// restore them when it is unhidden.
///
/// Cmd-H (and Hide Others) takes every window off screen, but AppKit posts no
/// per-window notification for it -- windowDidMiniaturize: fires only for an
/// actual miniaturize -- so without this a secondary window kept reporting
/// nativeVisible. Display.shouldEDTSleep() takes its minimized shortcut only
/// when no window is visible, deliberately, so that a miniaturized main window
/// cannot park the EDT while a tool window is still on screen animating. Under
/// a whole-application hide nothing is on screen, so that same guard held the
/// EDT awake for as long as the user left the application hidden.
void CN1MacWindowsDeliverAppHidden(BOOL hidden) {
    NSMutableArray *table = cn1WindowTable();
    for (id entry in table) {
        if (entry == [NSNull null]) {
            continue;
        }
        CN1MacWindowRecord *rec = (CN1MacWindowRecord *)entry;
        if (rec.disposed || rec.window == nil) {
            continue;
        }
        if (hidden) {
            // isVisible, not isMiniaturized. A window the application already
            // took off screen with Window.hide() is not miniaturized either, and
            // marking it would hand it a visible callback on unhide that AppKit
            // never backs -- the framework would then paint a window that is
            // still ordered out. isVisible is NO for a miniaturized window too,
            // so this covers that case as well. It is only meaningful because
            // the sweep runs from applicationWillHide:, while the windows are
            // still on screen; by applicationDidHide: they all read NO.
            if (!rec.window.isVisible || rec.hiddenByApp) {
                continue;
            }
            rec.hiddenByApp = YES;
            CN1MacWindowDeliverVisibility(rec.windowId, NO);
        } else if (rec.hiddenByApp) {
            rec.hiddenByApp = NO;
            CN1MacWindowDeliverVisibility(rec.windowId, YES);
        }
    }
}

// ---- monitors ------------------------------------------------------------

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowWatchScreens__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    cn1OnMain(^{
        if (cn1MacWatchingScreens) {
            return;
        }
        cn1MacWatchingScreens = YES;
        [[NSNotificationCenter defaultCenter]
            addObserverForName:NSApplicationDidChangeScreenParametersNotification
                        object:nil
                         queue:[NSOperationQueue mainQueue]
                    usingBlock:^(NSNotification *note) {
            CN1MacWindowDeliverMonitorsChanged();
        }];
    });
}

JAVA_INT com_codename1_impl_mac_MacNative_macMonitorCount___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return (int)[NSScreen screens].count;
}

JAVA_INT com_codename1_impl_mac_MacNative_macPrimaryMonitor___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    // Index zero by definition: NSScreen.screens is documented to lead with the
    // screen holding the menu bar.
    return 0;
}

JAVA_VOID com_codename1_impl_mac_MacNative_macMonitorBounds___int_boolean_int_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT monitor, JAVA_BOOLEAN workArea, JAVA_OBJECT outArr) {
    if (outArr == JAVA_NULL) {
        return;
    }
    JAVA_ARRAY arr = (JAVA_ARRAY)outArr;
    if (arr->length < 4) {
        return;
    }
    NSScreen *screen = cn1ScreenAt(monitor);
    if (screen == nil) {
        return;
    }
    NSRect r = workArea != 0 ? screen.visibleFrame : screen.frame;
    NSRect topLeft = cn1FromAppKitFrame(r);
    // One scale for the whole desktop, not this screen's own -- see
    // cn1DesktopScale. Per screen, the reported rectangles overlap on a
    // mixed-DPI setup and a window placed at a monitor's origin lands on the
    // wrong display.
    CGFloat scale = cn1DesktopScale();
    JAVA_ARRAY_INT *data = (JAVA_ARRAY_INT *)arr->data;
    data[0] = (int)(topLeft.origin.x * scale);
    data[1] = (int)(topLeft.origin.y * scale);
    data[2] = (int)(topLeft.size.width * scale);
    data[3] = (int)(topLeft.size.height * scale);
}

JAVA_INT com_codename1_impl_mac_MacNative_macMonitorDpi___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT monitor) {
    NSScreen *screen = cn1ScreenAt(monitor);
    if (screen == nil) {
        return 96;
    }
    NSValue *res = [screen.deviceDescription objectForKey:NSDeviceResolution];
    if (res == nil) {
        return 96;
    }
    // NSDeviceResolution is in dots per inch of the backing store already, so it
    // does not want the backing scale applied on top of it.
    NSSize dpi = [res sizeValue];
    return dpi.width > 0 ? (int)dpi.width : 96;
}

JAVA_INT com_codename1_impl_mac_MacNative_macMonitorScaleTimes100___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT monitor) {
    NSScreen *screen = cn1ScreenAt(monitor);
    CGFloat scale = screen != nil ? screen.backingScaleFactor : 1;
    return (int)(scale * 100);
}

JAVA_INT com_codename1_impl_mac_MacNative_macMonitorForWindow___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot) {
    __block int index = 0;
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec != nil) {
            index = cn1IndexOfScreen(rec.window.screen);
        }
    });
    return index;
}

JAVA_INT com_codename1_impl_mac_MacNative_macMonitorForMainWindow___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    __block int index = 0;
    cn1OnMain(^{
        index = cn1IndexOfScreen([CN1MacHost sharedHost].window.screen);
    });
    return index;
}

JAVA_OBJECT com_codename1_impl_mac_MacNative_macMonitorName___int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT monitor) {
    NSScreen *screen = cn1ScreenAt(monitor);
    NSString *name = screen != nil ? screen.localizedName : nil;
    if (name == nil) {
        return JAVA_NULL;
    }
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG name);
}
