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
#import <Foundation/Foundation.h>
#import <objc/runtime.h>
#include <assert.h>

#undef TARGET_OS_OSX
#define TARGET_OS_OSX 0
#undef TARGET_OS_TV
#define TARGET_OS_TV 0

@protocol UIKeyInput <NSObject>
@end

@interface UIView : NSObject
@property BOOL firstResponder;
@property (retain) NSArray *subviews;
@property (assign) UIView *window;
- (BOOL)isFirstResponder;
@end
@implementation UIView
- (BOOL)isFirstResponder { return self.firstResponder; }
- (void)dealloc { [_subviews release]; [super dealloc]; }
@end

// Like WKContentView, the text input is nested below the public peer view.
@interface PeerEditor : UIView <UIKeyInput>
@end
@implementation PeerEditor
@end

@interface UIKey : NSObject
@property int code;
@end
@implementation UIKey
@end
@interface UIPress : NSObject
@property (retain) UIKey *key;
@property long long identity;
@end
@implementation UIPress
- (void)dealloc { [_key release]; [super dealloc]; }
@end
@interface UIPressesEvent : NSObject
@end

static UIView *editingComponent;
static NSMutableArray *delivered;
static int cn1MapUIKeyToKeyCode(UIKey *key) { return key.code; }
static long long cn1PressIdentity(UIPress *press, int code) { return press.identity; }
static void keyPressedNative(int code) { [delivered addObject:@(code)]; }
static void keyReleasedNative(int code) { [delivered addObject:@(-code)]; }
static void CN1MacWindowDeliverKey(int windowId, int code, BOOL pressed) {
    if (pressed) keyPressedNative(code); else keyReleasedNative(code);
}

@interface BaseController : NSObject
@property (retain) UIView *view;
@property (retain) NSMutableSet *began;
@property (retain) NSMutableSet *ended;
@property (retain) NSMutableSet *cancelled;
- (void)pressesBegan:(NSSet *)presses withEvent:(UIPressesEvent *)event;
- (void)pressesEnded:(NSSet *)presses withEvent:(UIPressesEvent *)event;
- (void)pressesCancelled:(NSSet *)presses withEvent:(UIPressesEvent *)event;
@end
@implementation BaseController
- (instancetype)init {
    if ((self = [super init])) {
        self.began = [NSMutableSet set];
        self.ended = [NSMutableSet set];
        self.cancelled = [NSMutableSet set];
    }
    return self;
}
- (void)pressesBegan:(NSSet *)presses withEvent:(UIPressesEvent *)event { [self.began unionSet:presses]; }
- (void)pressesEnded:(NSSet *)presses withEvent:(UIPressesEvent *)event { [self.ended unionSet:presses]; }
- (void)pressesCancelled:(NSSet *)presses withEvent:(UIPressesEvent *)event { [self.cancelled unionSet:presses]; }
- (void)dealloc {
    [_view release]; [_began release]; [_ended release]; [_cancelled release];
    [super dealloc];
}
@end

#include "globals.h"
@interface MainController : BaseController
@end
@implementation MainController
#include "main.h"
@end
@interface MacController : BaseController
@property int windowId;
@property (retain) NSMutableSet *cn1FrameworkOwnedPresses;
@end
@implementation MacController
#include "mac.h"
@end

static UIPress *press(int code, long long identity) {
    UIPress *p = [[[UIPress alloc] init] autorelease];
    p.key = [[[UIKey alloc] init] autorelease];
    p.key.code = code;
    p.identity = identity;
    return p;
}

static void check(Class controllerClass) {
    BaseController *controller = [[[controllerClass alloc] init] autorelease];
    UIView *window = [[[UIView alloc] init] autorelease];
    UIView *root = [[[UIView alloc] init] autorelease];
    UIView *peer = [[[UIView alloc] init] autorelease];
    PeerEditor *editor = [[[PeerEditor alloc] init] autorelease];
    root.window = window;
    root.subviews = @[peer]; peer.subviews = @[editor]; window.subviews = @[root];
    controller.view = root;
    delivered = [NSMutableArray array];
    assert(!cn1HasNativeTextInput(nil));

    // An unfocused peer must not disable framework keys, even when a separate
    // scene contains a focused editor. A canvas first responder isn't text input.
    UIView *sibling = [[[UIView alloc] init] autorelease];
    PeerEditor *otherEditor = [[[PeerEditor alloc] init] autorelease];
    otherEditor.firstResponder = YES; sibling.subviews = @[otherEditor];
    root.firstResponder = YES;
    assert(cn1HasNativeTextInput(sibling));
    assert(!cn1HasNativeTextInput(window));
    UIPress *held = press(97, 1);
    [controller pressesBegan:[NSSet setWithObject:held] withEvent:nil];
    assert([delivered isEqual:@[@97]]);
    assert(controller.began.count == 0);

    // Focus an HTML editor while a CN1 key is held. The next key belongs to
    // UIKit, and a mixed key-up batch must finish each press with its owner.
    root.firstResponder = NO; editor.firstResponder = YES;
    UIPress *typed = press(98, 2);
    [controller pressesBegan:[NSSet setWithObject:typed] withEvent:nil];
    assert([controller.began containsObject:typed]);
    assert(delivered.count == 1);
    [controller pressesEnded:[NSSet setWithObjects:held, typed, nil] withEvent:nil];
    assert(([delivered isEqual:@[@97, @-97]]));
    assert([controller.ended containsObject:typed]);
    assert(![controller.ended containsObject:held]);

    // Losing native focus between press and release must not invent a CN1 up.
    [delivered removeAllObjects];
    [controller pressesBegan:[NSSet setWithObject:typed] withEvent:nil];
    editor.firstResponder = NO;
    [controller pressesEnded:[NSSet setWithObject:typed] withEvent:nil];
    assert(delivered.count == 0);

    // Cancellation preserves UIKit's phase and releases a held framework key.
    [controller pressesBegan:[NSSet setWithObject:held] withEvent:nil];
    editor.firstResponder = YES;
    [controller pressesBegan:[NSSet setWithObject:typed] withEvent:nil];
    [controller pressesCancelled:[NSSet setWithObjects:held, typed, nil] withEvent:nil];
    assert(([delivered isEqual:@[@97, @-97]]));
    assert([controller.cancelled containsObject:typed]);

    // Keep the earlier fix: editor creation protects keys before native focus.
    editor.firstResponder = NO; editingComponent = editor;
    [delivered removeAllObjects];
    [controller pressesBegan:[NSSet setWithObject:typed] withEvent:nil];
    editingComponent = nil;
    [controller pressesEnded:[NSSet setWithObject:typed] withEvent:nil];
    assert(delivered.count == 0);
    printf("PASS: %s peer focus, window isolation, mixed releases, cancellation, editor startup\n",
           class_getName(controllerClass));
}

int main(void) {
    @autoreleasepool { check([MainController class]); check([MacController class]); }
    return 0;
}
