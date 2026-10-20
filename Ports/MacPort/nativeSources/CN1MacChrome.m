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
#import "CN1MacHost.h"
#import "CN1MacMenu.h"
#import "CN1AppKitWindows.h"
#import "METALView.h"
#include "cn1_globals.h"
#include "com_codename1_impl_ios_IOSImplementation.h"

/*
 * Desktop chrome: the title bar, the application menu bar and the appearance.
 *
 * These are the four entry points IOSNative.m routes here on macOS. Mac Catalyst
 * answers the same four through UIKit -- UIMenuBuilder, UIKeyCommand and KVC on
 * a private _nsWindow -- so this file is the AppKit half of a contract the Java
 * side does not know has two implementations.
 */

/// The command rows last pushed from Java, kept so the menu can be rebuilt
/// without asking for them again.
static NSArray *cn1MenuRows = nil;

/// Target for every command menu item. The row index travels in the item's tag
/// and comes back to Java unchanged, so it has to stay aligned with the filtered
/// list IOSImplementation.setNativeCommands built.
@interface CN1MacMenuTarget : NSObject
+ (instancetype)sharedTarget;
- (void)cn1MenuAction:(id)sender;
@end

@implementation CN1MacMenuTarget

+ (instancetype)sharedTarget {
    static CN1MacMenuTarget *shared = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        shared = [[CN1MacMenuTarget alloc] init];
    });
    return shared;
}

- (BOOL)validateMenuItem:(NSMenuItem *)item {
    id enabled = item.representedObject;
    return ![enabled isKindOfClass:[NSNumber class]] || [enabled boolValue];
}

- (void)cn1MenuAction:(id)sender {
    if (![sender isKindOfClass:[NSMenuItem class]]) {
        return;
    }
    struct ThreadLocalData *threadStateData = getThreadLocalData();
    com_codename1_impl_ios_IOSImplementation_fireMacMenuCommand___int(threadStateData,
            (JAVA_INT)((NSMenuItem *)sender).tag);
}

@end

/// Maps a command's desktop menu hint onto one of the standard menus. Returns
/// nil for a hint that names a menu of the application's own.
///
/// Core resolves the standard roles (NativeMenuConventions) and spells the standard
/// menus one way, so About, Settings and Quit arrive here already under "App".
static NSString *cn1StandardMenuForHint(NSString *hint, BOOL *placeAtStart) {
    *placeAtStart = NO;
    NSString *h = [hint lowercaseString];
    if ([h isEqualToString:@"app"]) {
        // Into the application menu, below About, which is where a Mac application
        // puts its own entries (Settings first among them).
        *placeAtStart = YES;
        return @"__app__";
    }
    if ([h isEqualToString:@"file"]) { return @"File"; }
    if ([h isEqualToString:@"edit"]) { return @"Edit"; }
    if ([h isEqualToString:@"view"]) { return @"View"; }
    if ([h isEqualToString:@"window"]) { return @"Window"; }
    if ([h isEqualToString:@"help"]) { return @"Help"; }
    return nil;
}

/// Finds a top-level submenu by title, creating it before the Window menu if it
/// is missing -- which is where a Mac application's own menus go.
static NSMenu *cn1TopLevelMenu(NSMenu *mainMenu, NSString *title, BOOL createIfMissing) {
    for (NSMenuItem *item in mainMenu.itemArray) {
        if (item.submenu != nil && [item.submenu.title isEqualToString:title]) {
            return item.submenu;
        }
    }
    if (!createIfMissing) {
        return nil;
    }
    NSMenuItem *item = [[NSMenuItem alloc] initWithTitle:title action:NULL keyEquivalent:@""];
    NSMenu *menu = [[NSMenu alloc] initWithTitle:title];
    item.submenu = menu;
    NSInteger insertAt = mainMenu.numberOfItems;
    for (NSInteger i = 0; i < mainMenu.numberOfItems; i++) {
        NSMenu *sub = [mainMenu itemAtIndex:i].submenu;
        if (sub != nil && [sub.title isEqualToString:@"Window"]) {
            insertAt = i;
            break;
        }
    }
    [mainMenu insertItem:item atIndex:insertAt];
#ifndef CN1_USE_ARC
    [item release];
    [menu release];
#endif
    return menu;
}

/// Titles of the top-level menus this file invented for a desktop-menu hint.
///
/// Only these may be pruned when they empty out. The standard menus -- File,
/// Edit, View, Window, Help and the application menu -- belong to the
/// application whether or not any command is currently in them, and removing an
/// empty File menu because the visible Form has no File commands would be a far
/// worse bug than the one this fixes.
static NSMutableSet *cn1GeneratedMenuTitles = nil;

/// The stock items a role command took the place of on the previous pass, shown
/// again before every rebuild so a form without that command gets the stock one back.
static NSMutableArray *cn1HiddenStockItems = nil;

/// The stock action of each role ordinal (NativeMenuEncoder.FLAG_ROLE_SHIFT): the
/// item an application's own command of that role replaces in place, rather than
/// sitting beside a second Quit or Copy. NULL for a role with no stock item.
static SEL cn1StockActionForRole(int ordinal) {
    switch (ordinal) {
        case 1: return @selector(orderFrontStandardAboutPanel:);
        case 2: return @selector(hide:);
        case 3: return @selector(terminate:);
        case 4: return @selector(undo:);
        case 5: return @selector(redo:);
        case 6: return @selector(cut:);
        case 7: return @selector(copy:);
        case 8: return @selector(paste:);
        case 9: return @selector(selectAll:);
        case 10: return @selector(performMiniaturize:);
        case 11: return @selector(performZoom:);
        case 12: return @selector(toggleFullScreen:);
        default: return NULL;
    }
}

/// The stock item in `menu` (not in its submenus) carrying `action`, or nil.
static NSMenuItem *cn1StockItem(NSMenu *menu, SEL action) {
    if (action == NULL) {
        return nil;
    }
    for (NSMenuItem *item in menu.itemArray) {
        if (item.action == action && item.target != [CN1MacMenuTarget sharedTarget]) {
            return item;
        }
    }
    return nil;
}

/// Removes every item this file added on a previous pass, so a rebuild replaces
/// the commands rather than appending a second copy of them.
static void cn1RemovePreviousCommands(NSMenu *menu) {
    if (cn1HiddenStockItems != nil) {
        for (NSMenuItem *stock in cn1HiddenStockItems) {
            stock.hidden = NO;
        }
        [cn1HiddenStockItems removeAllObjects];
    }
    NSMutableArray *doomed = [NSMutableArray array];
    for (NSMenuItem *item in menu.itemArray) {
        if (item.target == [CN1MacMenuTarget sharedTarget]) {
            [doomed addObject:item];
        } else if (item.submenu != nil) {
            cn1RemovePreviousCommands(item.submenu);
        }
    }
    for (NSMenuItem *item in doomed) {
        [menu removeItem:item];
    }
}

/// Drops the invented menus that no longer hold anything.
///
/// Command items are removed above, but the container created for a hint stayed
/// behind: moving from a Form with a Tools command to one without left an empty
/// Tools menu in the bar, and every Form introducing a new hint added another
/// that never went away.
static void cn1PruneEmptyGeneratedMenus(NSMenu *mainMenu) {
    if (cn1GeneratedMenuTitles == nil) {
        return;
    }
    NSMutableArray *doomed = [NSMutableArray array];
    for (NSMenuItem *item in mainMenu.itemArray) {
        NSMenu *sub = item.submenu;
        if (sub != nil && sub.numberOfItems == 0
                && [cn1GeneratedMenuTitles containsObject:sub.title]) {
            [doomed addObject:item];
        }
    }
    for (NSMenuItem *item in doomed) {
        // Forgotten as well as removed: the next pass recreates and re-registers
        // it if a command asks for that hint again.
        [cn1GeneratedMenuTitles removeObject:item.submenu.title];
        [mainMenu removeItem:item];
    }
}

static NSMenuItem *cn1MakeCommandItem(NSString *label, int keyChar, int modifiers, NSInteger index) {
    NSString *equivalent = @"";
    NSEventModifierFlags flags = 0;
    if (keyChar != 0) {
        // Java's modifier flags: PRIMARY=1, SHIFT=2, ALT=4. PRIMARY is Command
        // here rather than Control, which is what makes an accelerator declared
        // once read correctly on every desktop.
        if (modifiers & 1) { flags |= NSEventModifierFlagCommand; }
        if (modifiers & 2) { flags |= NSEventModifierFlagShift; }
        if (modifiers & 4) { flags |= NSEventModifierFlagOption; }
        // AppKit reads an uppercase key equivalent as implying Shift, so the
        // character is lowered and Shift left to the modifier mask.
        //
        // %C and unichar, not %c and char. The column carries a Java char, which
        // is a UTF-16 code unit and reaches 0xFFFF, so narrowing it to a byte
        // silently rewrote every accelerator outside Latin-1 into whatever the
        // low eight bits happened to spell -- Cyrillic ZHE (U+0416) became 0x16,
        // a control character AppKit shows as no accelerator at all and no key
        // press produces.
        equivalent = [[NSString stringWithFormat:@"%C", (unichar)keyChar] lowercaseString];
    }
    NSMenuItem *item = [[NSMenuItem alloc] initWithTitle:label
                                                  action:@selector(cn1MenuAction:)
                                           keyEquivalent:equivalent];
    item.keyEquivalentModifierMask = flags;
    item.target = [CN1MacMenuTarget sharedTarget];
    item.tag = index;
#ifndef CN1_USE_ARC
    return [item autorelease];
#else
    return item;
#endif
}

/// Commands published before the first frame, and whether that hold is over.
/// Main thread only, like everything that touches the menu.
static NSArray *cn1PendingStartupRows = nil;
static BOOL cn1StartupMenuReleased = NO;

static void cn1ApplyMenuCommands(NSArray *rows);

void CN1MacHostSetMenuCommands(NSArray *rows) {
    // Held until the first frame is on screen. A Form's commands are published
    // as it is shown, which at launch is while the main thread is still serving
    // the first frame before [NSApp run] -- and applying them builds the whole
    // menu bar first (CN1MacInstallMainMenu), 10-20ms of main thread in front of
    // the frame the user is waiting for on the benchmark runner. Nobody can open
    // a menu before [NSApp run] takes events, so only the latest set is kept and
    // CN1MacReleaseStartupMenu applies it once the frame is out.
    if (!cn1StartupMenuReleased) {
#ifndef CN1_USE_ARC
        [cn1PendingStartupRows release];
        cn1PendingStartupRows = [rows retain];
#else
        cn1PendingStartupRows = rows;
#endif
        return;
    }
    cn1ApplyMenuCommands(rows);
}

void CN1MacReleaseStartupMenu(void) {
    if (cn1StartupMenuReleased) {
        return;
    }
    cn1StartupMenuReleased = YES;
    CN1MacInstallMainMenu();
    if (cn1PendingStartupRows != nil) {
        NSArray *rows = cn1PendingStartupRows;
        cn1PendingStartupRows = nil;
        cn1ApplyMenuCommands(rows);
#ifndef CN1_USE_ARC
        [rows release];
#endif
    }
}

/// Where the application's own items start in the application menu: below the
/// About item and the separator under it, the place macOS applications put
/// Settings. Looked up on every insertion, because a role About inserted before the
/// stock one moves it down.
static NSInteger cn1AppMenuStart(NSMenu *menu) {
    for (NSInteger i = 0; i < menu.numberOfItems; i++) {
        NSMenuItem *item = [menu itemAtIndex:i];
        if (item.action == @selector(orderFrontStandardAboutPanel:)
                && item.target != [CN1MacMenuTarget sharedTarget]) {
            NSInteger at = i + 1;
            if (at < menu.numberOfItems && [menu itemAtIndex:at].isSeparatorItem) {
                at++;
            }
            return at;
        }
    }
    return 0;
}

/// Appends an item, or for the application menu inserts it after the ones this pass
/// already put at its start, below the About item.
static void cn1AddMenuItem(NSMenu *target, NSMenuItem *item, BOOL placeAtStart, NSMutableDictionary *appended) {
    if (placeAtStart) {
        NSNumber *seen = appended[[NSValue valueWithNonretainedObject:target]];
        NSInteger count = seen != nil ? seen.integerValue : 0;
        [target insertItem:item atIndex:cn1AppMenuStart(target) + count];
        appended[[NSValue valueWithNonretainedObject:target]] = @(count + 1);
    } else {
        [target addItem:item];
    }
}

static void cn1ApplyMenuCommands(NSArray *rows) {
#ifndef CN1_USE_ARC
    [cn1MenuRows release];
    cn1MenuRows = [rows retain];
#else
    cn1MenuRows = rows;
#endif
    NSMenu *mainMenu = [NSApp mainMenu];
    if (mainMenu == nil) {
        CN1MacInstallMainMenu();
        mainMenu = [NSApp mainMenu];
        if (mainMenu == nil) {
            return;
        }
    }
    cn1RemovePreviousCommands(mainMenu);
    cn1PruneEmptyGeneratedMenus(mainMenu);

    NSMenu *appMenu = mainMenu.numberOfItems > 0 ? [mainMenu itemAtIndex:0].submenu : nil;
    NSMutableDictionary *appended = [NSMutableDictionary dictionary];

    // Submenus created on this pass, by the full path that named them.
    NSMutableDictionary *submenus = [NSMutableDictionary dictionary];
    NSString *pathSeparator = [NSString stringWithFormat:@"%C", (unichar)0x1f];

    for (NSUInteger i = 0; i < rows.count; i++) {
        // The NativeMenuEncoder row: path, label, key, modifiers, id, flags, icon.
        NSArray *cols = [rows[i] componentsSeparatedByString:@"\t"];
        NSString *pathField = cols.count > 0 ? cols[0] : @"";
        NSArray *path = [pathField componentsSeparatedByString:pathSeparator];
        NSString *hint = path.count > 0 ? path[0] : @"";
        NSString *label = cols.count > 1 ? cols[1] : rows[i];
        int keyChar = cols.count > 2 ? [cols[2] intValue] : 0;
        int modifiers = cols.count > 3 ? [cols[3] intValue] : 0;
        // The tag is the command's id, not its row number. Java publishes the map
        // fireMacMenuCommand() resolves through before this menu is built, so a row
        // number can outlive the list that gave it meaning -- an item still on screen
        // would then name whichever command had since taken that position. An id names
        // one command for as long as anything can still click it.
        NSInteger commandId = cols.count > 4 ? (NSInteger)[cols[4] intValue] : (NSInteger)i;
        int rowFlags = cols.count > 5 ? [cols[5] intValue] : 1;
        NSString *iconPng = cols.count > 6 ? cols[6] : @"";

        BOOL placeAtStart = NO;
        NSString *standard = cn1StandardMenuForHint(hint, &placeAtStart);
        NSMenu *target;
        if (standard == nil) {
            // No hint at all means the default commands menu, and a hint that
            // matches no standard menu names one of the application's own.
            NSString *generated = hint.length == 0 ? @"Commands" : hint;
            target = cn1TopLevelMenu(mainMenu, generated, YES);
            if (target != nil) {
                if (cn1GeneratedMenuTitles == nil) {
                    cn1GeneratedMenuTitles = [[NSMutableSet alloc] init];
                }
                [cn1GeneratedMenuTitles addObject:generated];
            }
        } else if ([standard isEqualToString:@"__app__"]) {
            target = appMenu;
        } else {
            target = cn1TopLevelMenu(mainMenu, standard, YES);
        }
        if (target == nil) {
            continue;
        }
        // Rows arrive depth first, so a submenu is created by its first row and lands at
        // its place among the parent's items. Its container item carries the shared
        // target too, which is how cn1RemovePreviousCommands takes the whole subtree.
        // Keyed by the physical menu, not the hint as written: hints the mapping treats as
        // one menu ("File", "file", the application-menu aliases) share their submenus.
        NSString *key = [NSString stringWithFormat:@"%p%d", (void *)target, placeAtStart ? 1 : 0];
        for (NSUInteger p = 1; p < path.count; p++) {
            key = [key stringByAppendingFormat:@"%@%@", pathSeparator, path[p]];
            NSMenu *sub = submenus[key];
            if (sub == nil) {
                NSMenuItem *holder = [[NSMenuItem alloc] initWithTitle:path[p] action:NULL keyEquivalent:@""];
                sub = [[NSMenu alloc] initWithTitle:path[p]];
                holder.submenu = sub;
                holder.target = [CN1MacMenuTarget sharedTarget];
                cn1AddMenuItem(target, holder, p == 1 && placeAtStart, appended);
                submenus[key] = sub;
#ifndef CN1_USE_ARC
                [holder release];
                [sub release];
#endif
            }
            target = sub;
            placeAtStart = NO;
        }
        NSMenuItem *item;
        if (rowFlags & 16) {
            item = [NSMenuItem separatorItem];
            item.target = [CN1MacMenuTarget sharedTarget];
        } else {
            item = cn1MakeCommandItem(label, keyChar, modifiers, commandId);
            // FLAG_ENABLED=1, FLAG_CHECKABLE=2, FLAG_CHECKED=4. A radio item shows the
            // same checkmark: that is how AppKit draws one, and Java keeps the group
            // exclusive.
            // The standard menus auto-enable (Cut/Copy follow the responder chain), so the
            // enabled flag is answered from validateMenuItem: rather than item.enabled,
            // which AppKit would overwrite.
            item.representedObject = @((rowFlags & 1) != 0);
            if (rowFlags & 2) {
                item.state = (rowFlags & 4) ? NSControlStateValueOn : NSControlStateValueOff;
            }
            if (iconPng.length > 0) {
                NSData *png = [[NSData alloc] initWithBase64EncodedString:iconPng options:0];
                NSImage *image = png == nil ? nil : [[NSImage alloc] initWithData:png];
                if (image != nil) {
                    image.size = NSMakeSize(16, 16);
                    item.image = image;
                }
#ifndef CN1_USE_ARC
                [image release];
                [png release];
#endif
            }
        }
        // A standard role takes the place of the stock item of that role (Quit, Copy,
        // Minimize), which is hidden until the next pass rather than left beside it.
        NSMenuItem *stock = (rowFlags & 16) ? nil
                : cn1StockItem(target, cn1StockActionForRole((rowFlags >> 8) & 0xff));
        if (stock != nil) {
            [target insertItem:item atIndex:[target indexOfItem:stock]];
            stock.hidden = YES;
            if (cn1HiddenStockItems == nil) {
                cn1HiddenStockItems = [[NSMutableArray alloc] init];
            }
            [cn1HiddenStockItems addObject:stock];
            continue;
        }
        cn1AddMenuItem(target, item, placeAtStart, appended);
    }
    // The application's own items in the application menu are a group of their own,
    // closed by a separator above Services.
    if (appMenu != nil) {
        NSNumber *count = appended[[NSValue valueWithNonretainedObject:appMenu]];
        if (count != nil && count.integerValue > 0) {
            NSMenuItem *sep = [NSMenuItem separatorItem];
            sep.target = [CN1MacMenuTarget sharedTarget];
            [appMenu insertItem:sep atIndex:cn1AppMenuStart(appMenu) + count.integerValue];
        }
    }
}

void CN1MacHostSetWindowTitle(NSString *title) {
    // All the key-value coding against a private _nsWindow that Mac Catalyst
    // needs collapses to this.
    dispatch_async(dispatch_get_main_queue(), ^{
        [CN1MacHost sharedHost].window.title = title == nil ? @"" : title;
    });
}

// Copy before dispatch: a Java array must never escape into an asynchronous
// AppKit block, where the moving/collecting VM no longer protects its storage.
void com_codename1_impl_mac_MacNative_macSetTitleBarRegions___int_int_1ARRAY(CODENAME_ONE_THREAD_STATE,
        JAVA_OBJECT __cn1ThisObject, JAVA_INT windowId, JAVA_OBJECT regionsObject) {
    JAVA_ARRAY regions = (JAVA_ARRAY)regionsObject;
    NSData *copy = regionsObject == JAVA_NULL ? [[NSData alloc] init]
            : [[NSData alloc] initWithBytes:CN1_ARRAY_DATA(regions) length:regions->length * sizeof(JAVA_ARRAY_INT)];
    dispatch_async(dispatch_get_main_queue(), ^{
        NSView *view = windowId == -2 ? [CN1MacHost sharedHost].renderingView
                : CN1MacPeerHostViewForWindowId(windowId);
        if ([view isKindOfClass:[METALView class]]) {
            ((METALView *)view).cn1TitleBarRegions = copy;
        }
    });
#if !__has_feature(objc_arc)
    [copy release];
#endif
}

// AppKit updates the snapshot. EDT layout can hold the render lock, so it
// must not dispatch_sync to AppKit merely to measure window controls.
extern void com_codename1_impl_mac_MacImplementation_macTitleBarInsetsChanged__(CODENAME_ONE_THREAD_STATE);
static int cn1TitleInsets[5];
static void cn1MacUpdateTitleInsets(void) {
    NSWindow *window = [CN1MacHost sharedHost].window;
    NSView *view = [CN1MacHost sharedHost].renderingView;
    if (!window || !view) return;
    CGFloat scale = window.backingScaleFactor;
    int metrics[5] = {0};
    NSWindowButton types[] = {NSWindowCloseButton, NSWindowMiniaturizeButton, NSWindowZoomButton};
    for (int i = 0; i < 3; i++) {
        NSButton *button = [window standardWindowButton:types[i]];
        if (!button || button.hidden || !button.superview) continue;
        NSRect rect = [view convertRect:button.frame fromView:button.superview];
        metrics[0] = MAX(metrics[0], (int)ceil((NSMaxX(rect) + 8) * scale));
        CGFloat bottom = view.isFlipped ? NSMaxY(rect) : NSHeight(view.bounds) - NSMinY(rect);
        metrics[4] = MAX(metrics[4], (int)ceil((bottom + 4) * scale));
    }
    BOOL changed;
    @synchronized ([CN1MacHost class]) {
        changed = memcmp(cn1TitleInsets, metrics, sizeof(metrics)) != 0;
        memcpy(cn1TitleInsets, metrics, sizeof(metrics));
    }
    // The query already returned the previous snapshot. Schedule a fresh EDT layout
    // after publishing a change, outside the cache lock. Unchanged metrics stay quiet.
    if (changed) {
        com_codename1_impl_mac_MacImplementation_macTitleBarInsetsChanged__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
}
void com_codename1_impl_mac_MacNative_macTitleBarInsets___int_1ARRAY(CODENAME_ONE_THREAD_STATE,
        JAVA_OBJECT instance, JAVA_OBJECT array) {
    if (array == JAVA_NULL || ((JAVA_ARRAY)array)->length < 5) return;
    @synchronized ([CN1MacHost class]) {
        memcpy(CN1_ARRAY_DATA(array), cn1TitleInsets, sizeof(cn1TitleInsets));
    }
    dispatch_async(dispatch_get_main_queue(), ^{ cn1MacUpdateTitleInsets(); });
}

void CN1MacHostSetWindowUndecorated(BOOL undecorated) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSWindow *w = [CN1MacHost sharedHost].window;
        if (w == nil) {
            return;
        }
        if (undecorated) {
            // Full size content rather than a borderless window: the title bar
            // goes away and the traffic lights stay, which is what a desktop
            // application supplying its own Toolbar chrome wants. A truly
            // frameless main window would leave no way to close it.
            w.styleMask |= NSWindowStyleMaskFullSizeContentView;
            w.titlebarAppearsTransparent = YES;
            w.titleVisibility = NSWindowTitleHidden;
            w.movableByWindowBackground = YES;
            cn1MacUpdateTitleInsets();
        } else {
            // Restored, unlike the Catalyst path, which only ever undecorates:
            // a form that set custom chrome and then navigated away left the
            // window without a title bar for the rest of the session.
            w.styleMask &= ~NSWindowStyleMaskFullSizeContentView;
            w.titlebarAppearsTransparent = NO;
            w.titleVisibility = NSWindowTitleVisible;
            w.movableByWindowBackground = NO;
            NSView *view = [CN1MacHost sharedHost].renderingView;
            if ([view isKindOfClass:[METALView class]]) {
                ((METALView *)view).cn1TitleBarRegions = nil;
            }
        }
    });
}

/// The appearance the framework last forced, or nil while it follows macOS.
///
/// Held so a window created later can adopt it: the framework sets this once
/// when the theme resolves, and a secondary Window opened afterwards would
/// otherwise keep the system appearance and show a light title bar over a dark
/// application.
static NSAppearance *cn1MacForcedAppearance = nil;

/// Applies the forced appearance to one window, if there is one to apply.
void CN1MacApplyForcedAppearance(NSWindow *w) {
    if (w != nil && cn1MacForcedAppearance != nil) {
        w.appearance = cn1MacForcedAppearance;
    }
}

void CN1MacHostSetDarkAppearance(BOOL dark) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAppearance *appearance = [NSAppearance appearanceNamed:
            dark ? NSAppearanceNameDarkAqua : NSAppearanceNameAqua];
#ifndef CN1_USE_ARC
        [appearance retain];
        [cn1MacForcedAppearance release];
#endif
        cn1MacForcedAppearance = appearance;
        // Per window, NOT on NSApplication. Setting NSApp.appearance reaches
        // far more than the title bars this exists to fix: it repainted the
        // window background and CN1's own surfaces too, turning every normally
        // light screenshot dark once any test forced dark once. Twenty three
        // frames changed, from 0.92 mean luma to 0.16.
        //
        // Every window that exists now, and CN1MacApplyForcedAppearance covers
        // the ones created later.
        [CN1MacHost sharedHost].window.appearance = appearance;
        for (NSWindow *w in [NSApp windows]) {
            w.appearance = appearance;
        }
    });
}


/// Whether VoiceOver is running, read from its own preference domain. macOS has
/// no equivalent of UIAccessibilityIsVoiceOverRunning and publishes no
/// notification when it starts, so this is the whole of what the platform will
/// tell an application.
BOOL CN1MacHostIsVoiceOverRunning(void) {
    NSUserDefaults *voiceOver = [[NSUserDefaults alloc]
        initWithSuiteName:@"com.apple.universalaccess"];
    BOOL running = [voiceOver boolForKey:@"voiceOverOnOffKey"];
#ifndef CN1_USE_ARC
    [voiceOver release];
#endif
    return running;
}

BOOL CN1MacHostIsDarkMode(void) {
    NSAppearance *effective = NSApp.effectiveAppearance;
    if (effective == nil) {
        return NO;
    }
    NSAppearanceName best = [effective bestMatchFromAppearancesWithNames:
        @[NSAppearanceNameAqua, NSAppearanceNameDarkAqua]];
    return [best isEqualToString:NSAppearanceNameDarkAqua];
}
