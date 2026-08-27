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
static NSString *cn1StandardMenuForHint(NSString *hint, BOOL *placeAtStart) {
    *placeAtStart = NO;
    NSString *h = [hint lowercaseString];
    if ([h isEqualToString:@"app"] || [h isEqualToString:@"about"]
            || [h isEqualToString:@"preferences"] || [h isEqualToString:@"quit"]) {
        // Into the application menu, above the About item, which is where a Mac
        // application puts its own top-level entries.
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

/// Removes every item this file added on a previous pass, so a rebuild replaces
/// the commands rather than appending a second copy of them.
static void cn1RemovePreviousCommands(NSMenu *menu) {
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

void CN1MacHostSetMenuCommands(NSArray *rows) {
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

    for (NSUInteger i = 0; i < rows.count; i++) {
        NSArray *cols = [rows[i] componentsSeparatedByString:@"\t"];
        NSString *hint = cols.count > 0 ? cols[0] : @"";
        NSString *label = cols.count > 1 ? cols[1] : rows[i];
        int keyChar = cols.count > 2 ? [cols[2] intValue] : 0;
        int modifiers = cols.count > 3 ? [cols[3] intValue] : 0;
        // The tag is the command's id, not its row number. Java publishes the map
        // fireMacMenuCommand() resolves through before this menu is built, so a row
        // number can outlive the list that gave it meaning -- an item still on screen
        // would then name whichever command had since taken that position. An id names
        // one command for as long as anything can still click it.
        NSInteger commandId = cols.count > 4 ? (NSInteger)[cols[4] intValue] : (NSInteger)i;
        NSMenuItem *item = cn1MakeCommandItem(label, keyChar, modifiers, commandId);

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
        if (placeAtStart) {
            NSNumber *seen = appended[[NSValue valueWithNonretainedObject:target]];
            NSInteger at = seen != nil ? seen.integerValue : 0;
            [target insertItem:item atIndex:at];
            appended[[NSValue valueWithNonretainedObject:target]] = @(at + 1);
        } else {
            [target addItem:item];
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
        } else {
            // Restored, unlike the Catalyst path, which only ever undecorates:
            // a form that set custom chrome and then navigated away left the
            // window without a title bar for the rest of the session.
            w.styleMask &= ~NSWindowStyleMaskFullSizeContentView;
            w.titlebarAppearsTransparent = NO;
            w.titleVisibility = NSWindowTitleVisible;
            w.movableByWindowBackground = NO;
        }
    });
}

void CN1MacHostSetDarkAppearance(BOOL dark) {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSAppearance *appearance = [NSAppearance appearanceNamed:
            dark ? NSAppearanceNameDarkAqua : NSAppearanceNameAqua];
        [CN1MacHost sharedHost].window.appearance = appearance;
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
