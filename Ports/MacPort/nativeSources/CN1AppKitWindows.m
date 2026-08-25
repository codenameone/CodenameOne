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
@end

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

- (void)windowDidChangeScreen:(NSNotification *)notification {
    CN1MacWindowDeliverMonitorsChanged();
}

- (void)windowDidChangeBackingProperties:(NSNotification *)notification {
    [self windowDidResize:notification];
    CN1MacWindowDeliverMonitorsChanged();
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

/// Codename One works in device pixels and AppKit in points, so every geometry
/// value crossing the bridge is scaled by the window's backing factor.
static CGFloat cn1WindowScale(CN1MacWindowRecord *rec) {
    if (rec != nil && rec.window != nil) {
        return rec.window.backingScaleFactor;
    }
    return [NSScreen mainScreen].backingScaleFactor;
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

JAVA_INT com_codename1_impl_mac_MacNative_macWindowCreate___int_java_lang_String_int_int_int_int_boolean_boolean_boolean_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT windowId, JAVA_OBJECT titleObj, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_BOOLEAN decorated, JAVA_BOOLEAN resizable, JAVA_BOOLEAN positionSet) {
    NSString *title = titleObj != JAVA_NULL
        ? toNSString(CN1_THREAD_GET_STATE_PASS_ARG titleObj)
        : @"";
    __block int slot = -1;
    cn1OnMain(^{
        CGFloat scale = [NSScreen mainScreen].backingScaleFactor;
        if (scale <= 0) {
            scale = 1;
        }
        NSRect content = NSMakeRect(x / scale, y / scale,
                                    MAX(width / scale, 1), MAX(height / scale, 1));
        NSWindow *w = cn1MakeWindow(content, decorated != 0, resizable != 0, NO);
        if (w == nil) {
            return;
        }
        if (positionSet != 0) {
            NSRect frame = [w frameRectForContentRect:cn1ToAppKitFrame(content)];
            [w setFrame:frame display:NO];
        } else {
            [w center];
        }
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
            [rec.window makeKeyAndOrderFront:nil];
            CN1MacWindowDeliverVisibility(rec.windowId, YES);
            // AppKit hands back a usable window synchronously, so the content is
            // ready as soon as it is on screen. Catalyst has to wait for a scene
            // to activate before it can say this.
            CN1MacWindowDeliverContentReady(rec.windowId);
        } else {
            [rec.window orderOut:nil];
            CN1MacWindowDeliverVisibility(rec.windowId, NO);
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
        CN1MacWindowDeliverVisibility(rec.windowId, YES);
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
        CGFloat scale = cn1WindowScale(rec);
        NSRect content = NSMakeRect(x / scale, y / scale,
                                    MAX(width / scale, 1), MAX(height / scale, 1));
        NSRect frame = [rec.window frameRectForContentRect:cn1ToAppKitFrame(content)];
        [rec.window setFrame:frame display:YES];
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
        CGFloat scale = cn1WindowScale(rec);
        NSRect content = [rec.window contentRectForFrameRect:rec.window.frame];
        NSRect topLeft = cn1FromAppKitFrame(content);
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
        CGFloat scale = w.backingScaleFactor;
        NSRect topLeft = cn1FromAppKitFrame([w contentRectForFrameRect:w.frame]);
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

        [view removeFromSuperview];
        old.delegate = nil;
        [old orderOut:nil];
        [old close];

        NSWindow *fresh = cn1MakeWindow(content, decorated, resizable, utility != 0);
        fresh.title = title;
        [fresh setFrame:[fresh frameRectForContentRect:content] display:NO];
        fresh.contentView = view;
        fresh.delegate = rec;
        rec.window = fresh;
        rec.utility = utility != 0;
        if (wasVisible) {
            [fresh makeKeyAndOrderFront:nil];
        }
        [fresh makeFirstResponder:view];
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
        if (rec.window.isZoomed) {
            [rec.window zoom:nil];
        }
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

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetInputEnabled___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_BOOLEAN enabled) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
            return;
        }
        rec.inputEnabled = enabled != 0;
        rec.view.cn1InputEnabled = enabled != 0;
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macMainWindowSetInputEnabled___boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_BOOLEAN enabled) {
    cn1OnMain(^{
        METALView *v = (METALView *)[CN1MacHost sharedHost].renderingView;
        v.cn1InputEnabled = enabled != 0;
    });
}

JAVA_VOID com_codename1_impl_mac_MacNative_macWindowSetModal___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT slot, JAVA_BOOLEAN modal) {
    cn1OnMain(^{
        CN1MacWindowRecord *rec = cn1WindowAt(slot);
        if (rec == nil) {
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
    return [rec.view readbackInto:(unsigned int *)arr->data width:width height:height]
        ? JAVA_TRUE : JAVA_FALSE;
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
    CGFloat scale = screen.backingScaleFactor;
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
