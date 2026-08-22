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

#import "CN1MacWindows.h"

#if TARGET_OS_MACCATALYST

#import <CoreGraphics/CoreGraphics.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

/*
 * Delivery into the framework. Defined in IOSNative.m alongside the existing
 * pointerPressed / screenSizeChanged bridges, so all the ParparVM thread-state
 * handling stays in one place.
 */
extern void CN1MacWindowDeliverClose(int windowId);
extern void CN1MacWindowDeliverClosed(int windowId);
extern void CN1MacWindowDeliverMonitorsChanged(void);
extern void CN1MacWindowDeliverFocus(int windowId, BOOL gained);
extern void CN1MacWindowDeliverResize(int windowId, int width, int height);
extern void CN1MacWindowDeliverPointer(int windowId, int type, int x, int y);
extern void CN1MacWindowDeliverKey(int windowId, int keyCode, BOOL pressed);
extern void CN1MacWindowDeliverHover(int windowId, int type, int x, int y);
extern void CN1MacWindowDeliverWheel(int windowId, int x, int y, int scrollX, int scrollY);
extern void CN1MacWindowDeliverPinch(int windowId, float scale, int x, int y);
extern void CN1MacWindowDeliverRotation(int windowId, float radians, int x, int y);
extern void cn1CapturePointerMetadata(UITouch* touch);

/* The main view controller's UIKey mapping, shared so the two cannot drift. */
extern int cn1MapUIKeyToKeyCode(UIKey* key) API_AVAILABLE(ios(13.4));

#define CN1_MAC_MAX_WINDOWS 32

/*
 * The view a Codename One window's content is presented in. The framework
 * renders into its own raster and hands it here; setting layer.contents is the
 * cheapest way to get that on screen without standing up a second Metal surface.
 */
@interface CN1MacWindowView : UIView
@property (nonatomic, assign) int windowId;
@end

@implementation CN1MacWindowView

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self != nil) {
        self.opaque = YES;
        self.layer.magnificationFilter = kCAFilterNearest;
        self.multipleTouchEnabled = NO;
        self.userInteractionEnabled = YES;
    }
    return self;
}

- (void)presentImage:(CGImageRef)image {
    /* Assigning to layer.contents must happen on the main thread; the caller
     * dispatches, so this is only reached there. */
    self.layer.contents = (__bridge id) image;
}

- (void)deliver:(NSSet<UITouch*>*)touches type:(int)type {
    UITouch* t = [touches anyObject];
    if (t == nil) {
        return;
    }
    /* Capture the pointer type, pressure and tilt before the event is queued, the
     * same way the main surface's touch handlers do. Without it the queued event
     * carries the defaults, so a pen reads as an ordinary touch -- and the stylus
     * listeners the framework dispatches in a Window stayed silent no matter what
     * the Java side did. */
    cn1CapturePointerMetadata(t);
    /* UIKit reports the location in points while the window is laid out in device
     * pixels -- the resize path multiplies by the screen scale -- so an unscaled
     * coordinate arrives at half its rendered position on a Retina display and only
     * the top left corner of the window is clickable where it looks like it is. */
    CGPoint p = [t locationInView:self];
    CGFloat scale = self.window != nil ? self.window.screen.scale : 1.0;
    CN1MacWindowDeliverPointer(self.windowId, type,
            (int) (p.x * scale), (int) (p.y * scale));
}

- (void)touchesBegan:(NSSet<UITouch*>*)touches withEvent:(UIEvent*)event {
    [self deliver:touches type:1];
}

- (void)touchesMoved:(NSSet<UITouch*>*)touches withEvent:(UIEvent*)event {
    [self deliver:touches type:3];
}

- (void)touchesEnded:(NSSet<UITouch*>*)touches withEvent:(UIEvent*)event {
    [self deliver:touches type:2];
}

- (void)touchesCancelled:(NSSet<UITouch*>*)touches withEvent:(UIEvent*)event {
    [self deliver:touches type:2];
}

@end

static void CN1MacWindowReportLayout(int windowId, int width, int height);
static void CN1MacWindowApplyDecoration(UIWindowScene* scene, int decorated);

/* The view controller a Codename One window scene is rooted at. */
@interface CN1MacWindowController : UIViewController
@property (nonatomic, assign) int windowId;
@property (nonatomic, assign) CN1MacWindowView* content;
@end

@implementation CN1MacWindowController

/*
 * Hover, indirect scroll, magnify and rotate. A secondary scene is rooted at this
 * controller rather than at CodenameOne_GLViewController, and every one of these is
 * delivered by a gesture recognizer installed on that controller's view -- none of
 * them arrive as touches. Without the same recognizers here, a mouse hover, a wheel
 * or trackpad scroll and a trackpad pinch or rotation over a secondary window
 * produced no Codename One event at all. Each one is the main controller's handler
 * with the window id carried through, so the two cannot disagree about what a
 * gesture means.
 *
 * Codename One geometry is in device pixels and UIKit reports points, hence the
 * screen scale, exactly as the touch path does.
 */
- (CGFloat)cn1Scale {
    return self.view.window != nil ? self.view.window.screen.scale : 1.0;
}

- (void)cn1InstallWindowRecognizers {
    if (@available(macCatalyst 13.0, *)) {
        UIHoverGestureRecognizer* hover = [[UIHoverGestureRecognizer alloc]
                initWithTarget:self action:@selector(cn1WindowHover:)];
        /* Hover is independent of touch and must not preempt a tap, the same
         * reasoning as the main controller's. */
        hover.cancelsTouchesInView = NO;
        hover.delaysTouchesBegan = NO;
        hover.delaysTouchesEnded = NO;
        [self.view addGestureRecognizer:hover];
        [hover release];
    }
    if (@available(macCatalyst 13.4, *)) {
        /* maximumNumberOfTouches 0 restricts this to indirect-pointer scrolling, so
         * it never competes with the touch recognizers. */
        UIPanGestureRecognizer* scroll = [[UIPanGestureRecognizer alloc]
                initWithTarget:self action:@selector(cn1WindowScroll:)];
        scroll.allowedScrollTypesMask = UIScrollTypeMaskAll;
        scroll.maximumNumberOfTouches = 0;
        scroll.cancelsTouchesInView = NO;
        scroll.delaysTouchesBegan = NO;
        scroll.delaysTouchesEnded = NO;
        [self.view addGestureRecognizer:scroll];
        [scroll release];
    }
    UIPinchGestureRecognizer* pinch = [[UIPinchGestureRecognizer alloc]
            initWithTarget:self action:@selector(cn1WindowPinch:)];
    pinch.cancelsTouchesInView = NO;
    pinch.delaysTouchesBegan = NO;
    pinch.delaysTouchesEnded = NO;
    [self.view addGestureRecognizer:pinch];
    [pinch release];

    UIRotationGestureRecognizer* rotate = [[UIRotationGestureRecognizer alloc]
            initWithTarget:self action:@selector(cn1WindowRotate:)];
    rotate.cancelsTouchesInView = NO;
    rotate.delaysTouchesBegan = NO;
    rotate.delaysTouchesEnded = NO;
    [self.view addGestureRecognizer:rotate];
    [rotate release];
}

- (void)cn1WindowHover:(UIGestureRecognizer*)recognizer {
    CGPoint p = [recognizer locationInView:self.view];
    CGFloat scale = [self cn1Scale];
    int x = (int) (p.x * scale);
    int y = (int) (p.y * scale);
    switch (recognizer.state) {
        case UIGestureRecognizerStateBegan:
            CN1MacWindowDeliverHover(self.windowId, 1, x, y);
            break;
        case UIGestureRecognizerStateChanged:
            CN1MacWindowDeliverHover(self.windowId, 3, x, y);
            break;
        case UIGestureRecognizerStateEnded:
        case UIGestureRecognizerStateCancelled:
        case UIGestureRecognizerStateFailed:
            CN1MacWindowDeliverHover(self.windowId, 2, x, y);
            break;
        default:
            break;
    }
}

- (void)cn1WindowScroll:(UIPanGestureRecognizer*)recognizer {
    if (recognizer.state != UIGestureRecognizerStateBegan
            && recognizer.state != UIGestureRecognizerStateChanged) {
        return;
    }
    CGPoint loc = [recognizer locationInView:self.view];
    CGPoint t = [recognizer translationInView:self.view];
    CGFloat scale = [self cn1Scale];
    int dx = (int) (t.x * scale);
    int dy = (int) (t.y * scale);
    if (dx != 0 || dy != 0) {
        CN1MacWindowDeliverWheel(self.windowId, (int) (loc.x * scale),
                (int) (loc.y * scale), dx, dy);
        /* Reset so each callback carries an incremental delta. */
        [recognizer setTranslation:CGPointZero inView:self.view];
    }
}

- (void)cn1WindowPinch:(UIPinchGestureRecognizer*)recognizer {
    if (recognizer.state == UIGestureRecognizerStateChanged && recognizer.scale > 0) {
        CGPoint loc = [recognizer locationInView:self.view];
        CGFloat scale = [self cn1Scale];
        CN1MacWindowDeliverPinch(self.windowId, (float) recognizer.scale,
                (int) (loc.x * scale), (int) (loc.y * scale));
        /* Incremental relative to 1.0, as the main controller does it. */
        recognizer.scale = 1.0;
    }
}

- (void)cn1WindowRotate:(UIRotationGestureRecognizer*)recognizer {
    if (recognizer.state == UIGestureRecognizerStateChanged && recognizer.rotation != 0) {
        CGPoint loc = [recognizer locationInView:self.view];
        CGFloat scale = [self cn1Scale];
        CN1MacWindowDeliverRotation(self.windowId, (float) recognizer.rotation,
                (int) (loc.x * scale), (int) (loc.y * scale));
        recognizer.rotation = 0;
    }
}

/*
 * Hardware keyboard. A secondary scene is rooted at this controller rather than at
 * CodenameOne_GLViewController, so without these the window's focused component
 * never receives a key: UIKit delivers presses up the responder chain of the window
 * that has focus, and only the main controller implements them.
 */
- (void)deliverPresses:(NSSet<UIPress*>*)presses pressed:(BOOL)pressed
                 event:(UIPressesEvent*)event {
    if (@available(iOS 13.4, *)) {
        BOOL handled = NO;
        for (UIPress* press in presses) {
            UIKey* key = press.key;
            int code = key != nil ? cn1MapUIKeyToKeyCode(key) : 0;
            if (code != 0) {
                CN1MacWindowDeliverKey(self.windowId, code, pressed);
                handled = YES;
            }
        }
        if (handled) {
            return;
        }
    }
    if (pressed) {
        [super pressesBegan:presses withEvent:event];
    } else {
        [super pressesEnded:presses withEvent:event];
    }
}

- (void)pressesBegan:(NSSet<UIPress*>*)presses withEvent:(UIPressesEvent*)event {
    [self deliverPresses:presses pressed:YES event:event];
}

- (void)pressesEnded:(NSSet<UIPress*>*)presses withEvent:(UIPressesEvent*)event {
    [self deliverPresses:presses pressed:NO event:event];
}

- (void)pressesCancelled:(NSSet<UIPress*>*)presses withEvent:(UIPressesEvent*)event {
    /* Treated as a release: leaving a key latched down in the framework is worse
     * than an extra release the focused component ignores. */
    [self deliverPresses:presses pressed:NO event:event];
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    /* Pin the content view to the controller's view rather than relying on the
     * autoresizing mask. The mask distributes a resize *delta*, so a content view
     * created while the window still had zero bounds stays at zero forever -- and
     * the size query reads the content view, so the window then reports nothing. */
    if (self.content != nil) {
        self.content.frame = self.view.bounds;
    }
    CGSize size = self.view.bounds.size;
    CGFloat scale = self.view.window != nil ? self.view.window.screen.scale : 1.0;
    CN1MacWindowReportLayout(self.windowId,
            (int) (size.width * scale), (int) (size.height * scale));
}

@end

typedef struct {
    UIWindowScene* scene;
    UIWindow* window;
    CN1MacWindowController* controller;
    CN1MacWindowView* content;
    int windowId;
    /* Bumped every time the slot is taken. A block queued on the main thread
     * captures the value it saw, so a request left over from a window that was
     * disposed cannot enqueue or adopt a scene for whoever took the slot next. */
    int generation;
    int inUse;
    int pendingWidth;
    int pendingHeight;
    /* The origin asked for, in pixels, and whether one was asked for at all. The
     * creation bridge carries x and y but adoption used to hardcode (0,0), so a
     * window positioned before its first show() -- a restored layout, or an explicit
     * setWindowLocation -- opened somewhere else. Zero positionSet means "no opinion",
     * which leaves the placement to the platform. */
    int pendingX;
    int pendingY;
    int positionSet;
    /* The visibility the framework last asked for. A scene is requested
     * asynchronously, so a show()/hide() pair can both land before one exists;
     * without recording it the hide is dropped and adoption shows the window
     * anyway, leaving a native window on screen the framework no longer paints. */
    int pendingVisible;
    /* Whether the application asked for a resizable window. Recorded rather than
     * consulted only at creation, because the scene arrives later and the size
     * restrictions can only be applied once it exists. */
    int resizable;
    /* The requested minimum, in pixels, or 0 for none. Recorded for the same reason
     * as resizable: the scene does not exist when the request is made. */
    int minWidth;
    int minHeight;
    /* Whether the application asked for a decorated window. Catalyst cannot remove
     * the frame, but it can hide the title bar's title and toolbar, which is the
     * part an application replacing the chrome cares about. */
    int decorated;
    /* Whether input is allowed inside the window. Recorded for the same reason as
     * pendingVisible: a modal can block a window whose scene has not connected yet,
     * and the request would otherwise be delivered to a nil window and dropped,
     * leaving the newly connected window's native peers interactive underneath a
     * modal that is supposed to be blocking them. 1 means enabled. */
    int inputEnabled;
    /* The geometry asked for but not yet granted, in pixels, or 0 when nothing is
     * outstanding. A recycled scene reports the *previous* window's size the moment
     * it is adopted, before the new geometry request lands, and delivering that would
     * lay the window out at the wrong size -- which a capture then records. Layout
     * sizes are suppressed until one matches what was asked for. */
    int awaitingWidth;
    int awaitingHeight;
    int staleLayoutDropped;
    NSString* pendingTitle;
} CN1MacWindow;

static CN1MacWindow g_macWindows[CN1_MAC_MAX_WINDOWS];

/* Defined further down, beside the main-window helpers that first needed it. */
static void CN1MacRunOnMainSync(void (^block)(void));

static CN1MacWindow* slotAt(int slot) {
    if (slot < 0 || slot >= CN1_MAC_MAX_WINDOWS) {
        return NULL;
    }
    if (!g_macWindows[slot].inUse) {
        return NULL;
    }
    return &g_macWindows[slot];
}

/*
 * A layout size from UIKit, filtered against any geometry still being asked for.
 *
 * A recycled scene reports the previous window's size the instant it is adopted --
 * before the new geometry request has landed -- and delivering that lays the window
 * out at the wrong size, which a capture then records. While a request is
 * outstanding only the size that was asked for is delivered; anything else is the
 * old geometry on its way out. Once it matches, the window is free again and every
 * later layout (a user resize) passes straight through.
 */
/* Guards the pending queue and the slot table's lifecycle fields. Both are
 * touched from two threads: scene requests and deliveries run on UIKit's main
 * queue, while create and destroy are called from the Codename One event
 * dispatch thread. Without it a dispose racing a scene delivery could pop a
 * half-compacted queue, or adopt a scene into a slot as it was being cleared,
 * misassigning or orphaning a native scene.
 *
 * The event dispatch thread never blocks on the main queue while holding this,
 * so it cannot deadlock against UIKit. The …Locked helpers assume it is held. */
static pthread_mutex_t g_slotLock = PTHREAD_MUTEX_INITIALIZER;

static void CN1MacWindowReportLayout(int windowId, int width, int height) {
    int iter;
    int drop = 0;
    /* Same critical section as the writer in CN1MacWindowSetBounds: the decision to
     * drop this layout and the clearing of the request have to see one consistent
     * set of the handshake fields. */
    pthread_mutex_lock(&g_slotLock);
    for (iter = 0; iter < CN1_MAC_MAX_WINDOWS; iter++) {
        CN1MacWindow* w = &g_macWindows[iter];
        if (!w->inUse || w->windowId != windowId) {
            continue;
        }
        if (w->awaitingWidth > 0 && w->awaitingHeight > 0) {
            if (width != w->awaitingWidth || height != w->awaitingHeight) {
                /* Only the first differing layout is dropped -- that is the
                 * recycled scene reporting the previous window's size before the
                 * request lands. A second one is the system's settled answer, which
                 * may legitimately differ from what was asked for (an oversized
                 * window, or one the window manager constrained), and discarding it
                 * forever would leave the framework laying out at a size the window
                 * does not have. */
                if (!w->staleLayoutDropped) {
                    w->staleLayoutDropped = 1;
                    drop = 1;
                    break;
                }
            }
            w->awaitingWidth = 0;
            w->awaitingHeight = 0;
            w->staleLayoutDropped = 0;
        }
        /* Whatever the window actually ended up as, including a size the user dragged
         * it to. Without this pendingWidth/Height only ever held the last size the
         * application asked for, so setResizable(false) pinned the restrictions to
         * that and snapped a user-resized window back to it. */
        w->pendingWidth = width;
        w->pendingHeight = height;
        break;
    }
    pthread_mutex_unlock(&g_slotLock);
    if (drop) {
        return;
    }
    /* Outside the lock: this re-enters Codename One, which must never happen while
     * holding a lock the event dispatch thread can be waiting on. */
    CN1MacWindowDeliverResize(windowId, width, height);
}

/* Assumes g_slotLock is held: the table it scans is mutated by both threads. */
static int slotForSceneLocked(UIWindowScene* scene) {
    int iter;
    for (iter = 0; iter < CN1_MAC_MAX_WINDOWS; iter++) {
        if (g_macWindows[iter].inUse && g_macWindows[iter].scene == scene) {
            return iter;
        }
    }
    return -1;
}

/*
 * Slots that have asked for a scene and are still waiting for one, oldest first.
 *
 * The system hands scenes back asynchronously, so a slot cannot simply take "the
 * next arrival": open two windows in quick succession and picking the first
 * unattached slot each time would swap their identities. Scenes are delivered in
 * the order they were requested, so a FIFO matches them correctly. Only touched
 * on the main thread, where both the request and the delivery happen.
 */
static int g_pendingSlots[CN1_MAC_MAX_WINDOWS];
static int g_pendingCount;



static void pushPendingSlotLocked(int slot) {
    if (g_pendingCount < CN1_MAC_MAX_WINDOWS) {
        g_pendingSlots[g_pendingCount++] = slot;
    }
}

static int popPendingSlotLocked(void) {
    int slot;
    int iter;
    if (g_pendingCount <= 0) {
        return -1;
    }
    slot = g_pendingSlots[0];
    for (iter = 1; iter < g_pendingCount; iter++) {
        g_pendingSlots[iter - 1] = g_pendingSlots[iter];
    }
    g_pendingCount--;
    return slot;
}

/*
 * Asks the system to give a scene the supplied frame, in points. Catalyst has no
 * direct window-move or window-resize API; a geometry preference is the supported
 * way to ask, and the window manager remains free to adjust the result -- which is
 * why every caller re-reads the delivered size rather than assuming it was granted.
 * Must be called on the main thread.
 */
static void CN1MacWindowRequestGeometry(UIWindowScene* scene, CGRect frame) {
    if (scene == nil) {
        return;
    }
    if (@available(macCatalyst 16.0, *)) {
        UIWindowSceneGeometryPreferencesMac* prefs =
                [[UIWindowSceneGeometryPreferencesMac alloc] initWithSystemFrame:frame];
        [scene requestGeometryUpdateWithPreferences:prefs errorHandler:^(NSError* error) {
            NSLog(@"CN1: window geometry request failed: %@", error);
        }];
        [prefs release];
    }
}

/*
 * Gets a window to the content size Codename One asked for, and keeps it there.
 *
 * Two separate problems, and an earlier attempt at each made things worse.
 *
 * A geometry preference is a request, not an instruction. When the window manager
 * ignores one the scene keeps the size it already had -- Catalyst's 1024x768 default
 * -- and nothing asked again, so the window stayed wrong for good and the windowed
 * screenshot suite came up short of captures.
 *
 * And the preference is a *system* frame, which encloses the title bar, while what
 * has to come out right is the content area. Whether the chrome is charged against a
 * request depends on when it arrives, so the same window came back 900x700 on one run
 * and 900x684 on the next -- fine for a live application, fatal for a golden.
 *
 * So: sample twice and only act on a settled reading, then apply **one** correction,
 * capped. The cap is what makes this safe. Correcting from an unsettled reading is
 * how a 400x300 window was once driven to its 120x120 minimum and a 1000x400 window
 * overshot to 1700x400; a correction that can only ever move the frame by the width
 * of some chrome cannot do either, whatever it measures.
 */
#define CN1_GEOMETRY_CHROME_SLACK 64.0
#define CN1_GEOMETRY_ATTEMPTS 10

static CGSize CN1MacWindowContentSize(UIWindow* window) {
    /* The root view controller's view, because that is the same thing
     * viewDidLayoutSubviews reports to the framework as the window's size. */
    UIView* rootView = window.rootViewController.view;
    return rootView != nil ? rootView.bounds.size : window.bounds.size;
}

static void CN1MacWindowSettleGeometry(int slot, int generation, UIWindowScene* scene,
        UIWindow* window, CGSize wantedContent, CGRect request, CGSize lastSample,
        int attemptsLeft);

/*
 * True while this settler still owns the window it was started for.
 *
 * A settler runs for up to a couple of seconds after the request, and a disposed
 * window's scene goes back to the recycling pool inside that window. Without this a
 * settler left over from the closed window would go on sampling -- and re-requesting
 * geometry -- against the scene now hosting a *different* window, resizing the
 * replacement out from under it.
 */
static BOOL CN1MacWindowSettlerStillOwns(int slot, int generation, UIWindowScene* scene) {
    BOOL owns = NO;
    if (slot < 0 || slot >= CN1_MAC_MAX_WINDOWS) {
        return NO;
    }
    pthread_mutex_lock(&g_slotLock);
    owns = g_macWindows[slot].inUse
            && g_macWindows[slot].generation == generation
            && g_macWindows[slot].scene == scene;
    pthread_mutex_unlock(&g_slotLock);
    return owns;
}

static void CN1MacWindowSettleGeometryStep(int slot, int generation,
        UIWindowScene* scene, UIWindow* window, CGSize wantedContent, CGRect request,
        CGSize lastSample, int attemptsLeft) {
    if (!CN1MacWindowSettlerStillOwns(slot, generation, scene)) {
        return;
    }
    CGSize got = CN1MacWindowContentSize(window);
    if (got.width <= 0 || got.height <= 0) {
        CN1MacWindowSettleGeometry(slot, generation, scene, window, wantedContent,
                request, got, attemptsLeft - 1);
        return;
    }
    CGFloat dw = wantedContent.width - got.width;
    CGFloat dh = wantedContent.height - got.height;
    if (fabs(dw) <= 1.0 && fabs(dh) <= 1.0) {
        return;                      /* content is what was asked for */
    }
    BOOL settled = fabs(got.width - lastSample.width) <= 1.0
            && fabs(got.height - lastSample.height) <= 1.0;
    if (!settled) {
        /* Still laying out. Look again without touching the request -- acting on a
         * reading that is still moving is what caused the two earlier regressions. */
        CN1MacWindowSettleGeometry(slot, generation, scene, window, wantedContent,
                request, got, attemptsLeft - 1);
        return;
    }
    if (fabs(dw) > CN1_GEOMETRY_CHROME_SLACK || fabs(dh) > CN1_GEOMETRY_CHROME_SLACK) {
        /* Nowhere near: the request was ignored rather than adjusted for chrome. Ask
         * again for exactly the same frame -- never a computed one, which could not
         * be trusted at this distance. */
        CN1MacWindowRequestGeometry(scene, request);
        CN1MacWindowSettleGeometry(slot, generation, scene, window, wantedContent,
                request, got, attemptsLeft - 1);
        return;
    }
    /* Chrome-sized shortfall on a settled window: correct once, by that much. */
    CGRect next = CGRectMake(request.origin.x, request.origin.y,
            request.size.width + dw, request.size.height + dh);
    CN1MacWindowRequestGeometry(scene, next);
    CN1MacWindowSettleGeometry(slot, generation, scene, window, wantedContent, next,
            got, attemptsLeft - 1);
}

static void CN1MacWindowSettleGeometry(int slot, int generation, UIWindowScene* scene,
        UIWindow* window, CGSize wantedContent, CGRect request, CGSize lastSample,
        int attemptsLeft) {
    if (scene == nil || window == nil || attemptsLeft <= 0) {
        return;
    }
    /* Retained across the delay: this port builds without ARC, and a disconnect can
     * release either of these while the check is queued. */
    UIWindowScene* heldScene = [scene retain];
    UIWindow* heldWindow = [window retain];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t) (0.25 * NSEC_PER_SEC)),
            dispatch_get_main_queue(), ^{
        CN1MacWindowSettleGeometryStep(slot, generation, heldScene, heldWindow,
                wantedContent, request, lastSample, attemptsLeft);
        [heldWindow release];
        [heldScene release];
    });
}

static void dropPendingSlotLocked(int slot) {
    int read;
    int write = 0;
    for (read = 0; read < g_pendingCount; read++) {
        if (g_pendingSlots[read] != slot) {
            g_pendingSlots[write++] = g_pendingSlots[read];
        }
    }
    g_pendingCount = write;
}

/*
 * Scenes that a closed window gave back, kept alive for the next one.
 *
 * A scene session is not a cheap object and the system does not hand them out on
 * demand: asking for one while a previous destruction is still in flight fails with
 * "scene invalidated before create completion", and the window that asked is then
 * left with no scene at all. Closing one window and opening another is completely
 * ordinary, so recycling is the only way that sequence can be reliable. Only touched
 * on the main thread.
 */
static UIWindowScene* g_freeScenes[CN1_MAC_MAX_WINDOWS];
static int g_freeSceneCount;

static UIWindowScene* takeFreeScene(void) {
    if (g_freeSceneCount <= 0) {
        return NULL;
    }
    return g_freeScenes[--g_freeSceneCount];
}

int CN1MacWindowCreate(int windowId, NSString* title, int x, int y, int width, int height,
        BOOL decorated, BOOL resizable, BOOL positionSet) {
    int slot = -1;
    int iter;
    for (iter = 0; iter < CN1_MAC_MAX_WINDOWS; iter++) {
        if (!g_macWindows[iter].inUse) {
            slot = iter;
            break;
        }
    }
    if (slot < 0) {
        return -1;
    }
    {
        int generation = g_macWindows[slot].generation + 1;
        memset(&g_macWindows[slot], 0, sizeof(CN1MacWindow));
        g_macWindows[slot].generation = generation;
    }
    g_macWindows[slot].inUse = 1;
    g_macWindows[slot].windowId = windowId;
    g_macWindows[slot].pendingWidth = width;
    g_macWindows[slot].pendingHeight = height;
    g_macWindows[slot].pendingX = x;
    g_macWindows[slot].pendingY = y;
    /* Carried through rather than inferred from the coordinates: a window explicitly
     * placed at 0,0 is placed, and guessing from the numbers made it look unplaced so
     * the window server put it wherever it liked. */
    g_macWindows[slot].positionSet = positionSet ? 1 : 0;
    g_macWindows[slot].pendingTitle = [title retain];
    g_macWindows[slot].resizable = resizable ? 1 : 0;
    g_macWindows[slot].decorated = decorated ? 1 : 0;
    /* Set explicitly because the slot was just memset to zero, and zero here would
     * mean "input disabled" -- every new window would come up inert. */
    g_macWindows[slot].inputEnabled = 1;

    const int generation = g_macWindows[slot].generation;
    dispatch_async(dispatch_get_main_queue(), ^{
        pthread_mutex_lock(&g_slotLock);
        if (!g_macWindows[slot].inUse || g_macWindows[slot].generation != generation) {
            /* The window was disposed before this ran; the slot may already belong to
             * another one, and requesting a scene for it would leave an orphan. */
            pthread_mutex_unlock(&g_slotLock);
            return;
        }
        UIWindowScene* recycled = takeFreeScene();
        if (recycled != nil) {
            /* Adopt it straight away rather than going through the pending queue:
             * there is no asynchronous delivery to wait for. */
            pushPendingSlotLocked(slot);
            pthread_mutex_unlock(&g_slotLock);
            CN1MacWindowSceneConnected(recycled);
            [recycled release];
            return;
        }
        // Enqueue and request on the same main-thread turn, so the queue order is
        // exactly the request order the system will deliver scenes in.
        pushPendingSlotLocked(slot);
        pthread_mutex_unlock(&g_slotLock);
        if (@available(macCatalyst 13.0, *)) {
            UISceneActivationRequestOptions* options =
                    [[UISceneActivationRequestOptions alloc] init];
            options.requestingScene = [UIApplication sharedApplication].connectedScenes.anyObject;
            [[UIApplication sharedApplication] requestSceneSessionActivation:nil
                                                                userActivity:nil
                                                                     options:options
                                                                errorHandler:^(NSError* error) {
                NSLog(@"CN1: window scene activation failed: %@", error);
            }];
            [options release];
        }
    });
    return slot;
}

/*
 * Adopts a newly connected scene into the slot that asked for it. Called from the
 * scene delegate, which is the only place a scene object becomes available.
 */
BOOL CN1MacWindowAdoptScene(UIWindowScene* scene) {
    BOOL claimed;
    pthread_mutex_lock(&g_slotLock);
    // Nothing is waiting, or this scene was already adopted: it belongs to the
    // application's main form.
    claimed = g_pendingCount > 0 && slotForSceneLocked(scene) < 0;
    pthread_mutex_unlock(&g_slotLock);
    if (!claimed) {
        return NO;
    }
    CN1MacWindowSceneConnected(scene);
    return YES;
}

/*
 * The window a scene belongs to, or -1 for the application's main scene. Lets the
 * scene delegate route activation and disconnection without knowing about slots.
 */
int CN1MacWindowIdForScene(UIWindowScene* scene) {
    int windowId;
    pthread_mutex_lock(&g_slotLock);
    {
        int slot = slotForSceneLocked(scene);
        windowId = slot < 0 ? -1 : g_macWindows[slot].windowId;
    }
    pthread_mutex_unlock(&g_slotLock);
    return windowId;
}

/*
 * The user closed the window with the native close control.
 *
 * Reported as a close that has already happened, not as a request. UIKit hands the
 * disconnect over after the scene is gone, so there is nothing left to veto: asking
 * would let DO_NOTHING_ON_CLOSE leave a registered window painting into a surface
 * that no longer exists, and HIDE_ON_CLOSE keep a window with no scene to show
 * again. An application that needs to intervene closes the window itself, which is
 * a request and is vetoable.
 */
void CN1MacWindowSceneDisconnected(UIWindowScene* scene) {
    /* Locked against CN1MacWindowDestroy, which snapshots this same slot and
     * clears it. Unsynchronized, the two interleaved badly in both directions:
     * teardown could snapshot w->scene after this released it but before it was
     * nilled, and then message or release a deallocated scene, or this could read
     * w->windowId after teardown had zeroed the slot and report the close under a
     * window id that no longer meant anything. */
    UIWindowScene* dead = nil;
    int windowId = -1;
    pthread_mutex_lock(&g_slotLock);
    {
        int slot = slotForSceneLocked(scene);
        if (slot >= 0) {
            CN1MacWindow* w = &g_macWindows[slot];
            /* The scene is gone, so it must not be recycled or presented into. */
            dead = w->scene;
            w->scene = nil;
            windowId = w->windowId;
        }
    }
    pthread_mutex_unlock(&g_slotLock);
    /* Both outside the lock: -release can run arbitrary teardown, and delivering
     * the close re-enters Codename One, which must never happen while holding a
     * lock the event dispatch thread can be waiting on. */
    [dead release];
    if (windowId >= 0) {
        CN1MacWindowDeliverClosed(windowId);
    }
}

void CN1MacWindowSceneConnected(UIWindowScene* scene) {
    int slot;
    CN1MacWindow* w;
    pthread_mutex_lock(&g_slotLock);
    slot = popPendingSlotLocked();
    if (slot < 0 || !g_macWindows[slot].inUse) {
        /* The window this scene was requested for is already gone. Park the scene
         * rather than dropping it on the floor: it is a live native scene, and
         * leaking one per raced dispose eventually exhausts what the system will
         * hand out. */
        if (scene != nil && g_freeSceneCount < CN1_MAC_MAX_WINDOWS) {
            scene.title = @"";
            g_freeScenes[g_freeSceneCount++] = [scene retain];
        }
        pthread_mutex_unlock(&g_slotLock);
        return;
    }
    w = &g_macWindows[slot];
    w->scene = [scene retain];
    /* Held across the rest of the adoption so a dispose cannot clear the slot
     * from under it. Only UIKit calls follow -- nothing re-enters Codename One --
     * so this cannot deadlock against the event dispatch thread. */

    /* A slot can be adopted more than once. A close that a modal blocks is put
     * back through CN1MacWindowReopen, and disconnection releases only the scene:
     * the window, controller and view built for the previous scene are still
     * here. Overwriting the three pointers below without releasing them leaks an
     * entire native window and view hierarchy on every blocked close. Nothing in
     * this file implements dealloc, so none of these releases re-enters Codename
     * One and they are safe under the lock. */
    if (w->window != nil || w->controller != nil || w->content != nil) {
        UIWindow* staleWindow = w->window;
        CN1MacWindowController* staleController = w->controller;
        CN1MacWindowView* staleContent = w->content;
        w->window = nil;
        w->controller = nil;
        w->content = nil;
        [staleContent removeFromSuperview];
        staleWindow.rootViewController = nil;
        staleWindow.hidden = YES;
        [staleContent release];
        [staleController release];
        [staleWindow release];
    }

    w->window = [[UIWindow alloc] initWithWindowScene:scene];
    w->controller = [[CN1MacWindowController alloc] init];
    w->controller.windowId = w->windowId;

    w->content = [[CN1MacWindowView alloc] initWithFrame:w->window.bounds];
    w->content.windowId = w->windowId;
    w->content.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    w->controller.view.backgroundColor = [UIColor blackColor];
    [w->controller.view addSubview:w->content];
    w->controller.content = w->content;
    /* After the view exists, since every recognizer attaches to it. */
    [w->controller cn1InstallWindowRecognizers];
    CN1MacWindowApplyDecoration(scene, w->decorated);

    w->window.rootViewController = w->controller;
    /* As with visibility: honour the input state last asked for. A window opened
     * while an application modal is already up has its blocking requested before the
     * scene exists, and without this the peers inside it stayed interactive. */
    w->window.userInteractionEnabled = w->inputEnabled ? YES : NO;
    /* Honour the visibility last asked for rather than always showing: a window
     * hidden before its scene arrived must not appear now. */
    if (w->pendingVisible) {
        [w->window makeKeyAndVisible];
    } else {
        w->window.hidden = YES;
    }

    if (w->pendingTitle != nil) {
        scene.title = w->pendingTitle;
    }
    if (w->pendingWidth > 0 && w->pendingHeight > 0) {
        /* Ask for the size the window was created with. Without this the system
         * hands the scene whatever size it feels like -- in practice the main
         * scene's size -- and the Codename One window then lays out into a raster
         * that does not match what was requested. Codename One geometry is in
         * pixels and UIKit's is in points, hence the divide by the screen scale.
         * The restrictions have to be relaxed first, because the system clamps the
         * requested frame against them and the default minimum is larger than a
         * small window. They are lowered rather than pinned to the requested size,
         * so the window stays resizable by hand afterwards. */
        CGFloat scale = w->window.screen != nil ? w->window.screen.scale : 1.0;
        CGFloat pointWidth = w->pendingWidth / scale;
        CGFloat pointHeight = w->pendingHeight / scale;
        /* Until this lands, a layout report is the old geometry -- see
         * CN1MacWindowReportLayout. */
        w->awaitingWidth = w->pendingWidth;
        w->awaitingHeight = w->pendingHeight;
        w->staleLayoutDropped = 0;
        if (scene.sizeRestrictions != nil) {
            if (w->resizable) {
                CGFloat minW = w->minWidth > 0 ? w->minWidth / scale : MIN(pointWidth, 120);
                CGFloat minH = w->minHeight > 0 ? w->minHeight / scale : MIN(pointHeight, 120);
                scene.sizeRestrictions.minimumSize = CGSizeMake(minW, minH);
                scene.sizeRestrictions.maximumSize = CGSizeMake(CGFLOAT_MAX, CGFLOAT_MAX);
            } else {
                /* Pinned both ways, which is how Catalyst expresses a fixed size.
                 * The flag reached creation and was then dropped, so a window the
                 * framework reported as non-resizable could still be dragged out of
                 * shape by the user. */
                scene.sizeRestrictions.minimumSize = CGSizeMake(pointWidth, pointHeight);
                scene.sizeRestrictions.maximumSize = CGSizeMake(pointWidth, pointHeight);
            }
        }
        /* The requested origin, when one was asked for. Hardcoding (0,0) here threw
         * away an explicitly positioned or restored window's placement. */
        CGFloat pointX = w->positionSet ? w->pendingX / scale : 0;
        CGFloat pointY = w->positionSet ? w->pendingY / scale : 0;
        CGRect wantedFrame = CGRectMake(pointX, pointY, pointWidth, pointHeight);
        CN1MacWindowRequestGeometry(scene, wantedFrame);
        CN1MacWindowSettleGeometry(slot, w->generation, scene, w->window,
                CGSizeMake(pointWidth, pointHeight), wantedFrame,
                CGSizeMake(-1, -1), CN1_GEOMETRY_ATTEMPTS);
    }
    pthread_mutex_unlock(&g_slotLock);
}

void CN1MacWindowDestroy(int slot) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    /* The whole teardown is one critical section: removing the slot from the
     * pending queue, taking ownership of the native objects and clearing the slot
     * have to be indivisible against a scene delivery arriving on the main queue,
     * which pops that same queue and adopts into that same slot. Splitting them
     * let a delivery adopt a scene into a slot that was half cleared. */
    pthread_mutex_lock(&g_slotLock);
    dropPendingSlotLocked(slot);
    UIWindowScene* scene = w->scene;
    UIWindow* window = w->window;
    CN1MacWindowController* controller = w->controller;
    CN1MacWindowView* content = w->content;
    NSString* title = w->pendingTitle;
    /* Bumped across the clear, so a request still queued for this window sees a
     * different generation and does nothing. */
    int generation = w->generation + 1;
    memset(w, 0, sizeof(CN1MacWindow));
    w->generation = generation;
    pthread_mutex_unlock(&g_slotLock);

    dispatch_async(dispatch_get_main_queue(), ^{
        if (window != nil) {
            window.hidden = YES;
            window.rootViewController = nil;
        }
        if (scene != nil && g_freeSceneCount < CN1_MAC_MAX_WINDOWS) {
            /* Park the scene instead of destroying it -- see g_freeScenes. Its
             * ownership moves from the slot to the pool, so it is deliberately not
             * released here. */
            scene.title = @"";
            g_freeScenes[g_freeSceneCount++] = scene;
        } else if (scene != nil) {
            UISceneDestructionRequestOptions* opts =
                    [[UISceneDestructionRequestOptions alloc] init];
            [[UIApplication sharedApplication] requestSceneSessionDestruction:scene.session
                                                                      options:opts
                                                                 errorHandler:nil];
            [opts release];
            [scene release];
        }
        /* No ARC in this port: everything retained above is released here, after
         * UIKit has finished with it on the main thread. */
        [content release];
        [controller release];
        [window release];
        [title release];
    });
}

/*
 * Asks the system for a scene again after one was disconnected without the app
 * getting a say. Reuses the slot, so the framework's peer and raster stay valid.
 */
BOOL CN1MacWindowReopen(int slot) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL || w->scene != nil) {
        return NO;
    }
    /* Under the lock for the same reason as the other pending state: adoption
     * reads this field while holding it. */
    pthread_mutex_lock(&g_slotLock);
    w->pendingVisible = 1;
    pthread_mutex_unlock(&g_slotLock);
    const int generation = w->generation;
    dispatch_async(dispatch_get_main_queue(), ^{
        pthread_mutex_lock(&g_slotLock);
        if (!g_macWindows[slot].inUse || g_macWindows[slot].generation != generation) {
            pthread_mutex_unlock(&g_slotLock);
            return;
        }
        UIWindowScene* recycled = takeFreeScene();
        pushPendingSlotLocked(slot);
        pthread_mutex_unlock(&g_slotLock);
        if (recycled != nil) {
            CN1MacWindowSceneConnected(recycled);
            [recycled release];
            return;
        }
        if (@available(macCatalyst 13.0, *)) {
            UISceneActivationRequestOptions* options =
                    [[UISceneActivationRequestOptions alloc] init];
            options.requestingScene = [UIApplication sharedApplication].connectedScenes.anyObject;
            [[UIApplication sharedApplication] requestSceneSessionActivation:nil
                                                                userActivity:nil
                                                                     options:options
                                                                errorHandler:^(NSError* error) {
                NSLog(@"CN1: window scene reopen failed: %@", error);
            }];
            [options release];
        }
    });
    return YES;
}

void CN1MacWindowSetInputEnabled(int slot, BOOL enabled) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    UIWindow* window;
    /* Recorded before it is applied, so a request that arrives before the scene
     * exists is honoured at adoption instead of being delivered to a nil window --
     * and recorded under the slot lock, because CN1MacWindowSceneConnected reads
     * this field and installs the window while holding it. Unsynchronized, adoption
     * could enable the hierarchy from the value this call is in the middle of
     * replacing while this call sees a nil window and returns, leaving the peers
     * interactive underneath the modal. */
    pthread_mutex_lock(&g_slotLock);
    w->inputEnabled = enabled ? 1 : 0;
    window = [w->window retain];
    pthread_mutex_unlock(&g_slotLock);
    if (window == nil) {
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        /* Covers every touch inside the window while a modal blocks it. The scene's
         * own title bar is AppKit chrome the app does not own, so its close button
         * stays live -- see the note on CN1MacWindowSceneDisconnected. */
        window.userInteractionEnabled = enabled;
        [window release];
    });
}

/*
 * Displays being attached, removed or reconfigured. UIScreen posts all three, and
 * the app has no other way to learn about them, so a Codename One monitor listener
 * depends entirely on these.
 */
@interface CN1MacScreenWatch : NSObject
@end

@implementation CN1MacScreenWatch
- (void)screensChanged:(NSNotification*)note {
    CN1MacWindowDeliverMonitorsChanged();
}
@end

static CN1MacScreenWatch* g_screenWatch;

void CN1MacWindowWatchScreens(void) {
    if (g_screenWatch != nil) {
        return;
    }
    g_screenWatch = [[CN1MacScreenWatch alloc] init];
    NSNotificationCenter* nc = [NSNotificationCenter defaultCenter];
    [nc addObserver:g_screenWatch selector:@selector(screensChanged:)
               name:UIScreenDidConnectNotification object:nil];
    [nc addObserver:g_screenWatch selector:@selector(screensChanged:)
               name:UIScreenDidDisconnectNotification object:nil];
    [nc addObserver:g_screenWatch selector:@selector(screensChanged:)
               name:UIScreenModeDidChangeNotification object:nil];
}

void CN1MacWindowShow(int slot, BOOL visible) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    UIWindow* window;
    /* Recorded whether or not a scene exists yet, so adoption can honour it, and
     * under the slot lock for the same reason as the input state: adoption reads
     * pendingVisible while holding it. */
    pthread_mutex_lock(&g_slotLock);
    w->pendingVisible = visible ? 1 : 0;
    window = [w->window retain];
    pthread_mutex_unlock(&g_slotLock);
    dispatch_async(dispatch_get_main_queue(), ^{
        if (window != nil) {
            window.hidden = visible ? NO : YES;
            if (visible) {
                [window makeKeyAndVisible];
            }
        }
        [window release];
    });
}

void CN1MacWindowSetTitle(int slot, NSString* title) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    NSString* retained = [title retain];
    NSString* old;
    NSString* forBlock;
    UIWindowScene* scene;
    /* Swapped under the slot lock. This runs on the event dispatch thread while
     * CN1MacWindowSceneConnected can be adopting the same slot on UIKit's main
     * queue, and adoption reads pendingTitle under this lock. Unsynchronized, a
     * title change could release the very string adoption was about to assign --
     * this file builds without ARC, so that is a use after free rather than a
     * missed update. */
    pthread_mutex_lock(&g_slotLock);
    old = w->pendingTitle;
    w->pendingTitle = retained;
    scene = w->scene;
    /* An extra reference for the block: the slot's reference belongs to the slot,
     * and a later setTitle can replace and release it before the block runs. The
     * scene needs the same treatment -- CN1MacWindowSceneDisconnected can clear and
     * release it between this snapshot and the block, and the block would then
     * message a deallocated UIWindowScene. */
    forBlock = [retained retain];
    scene = [scene retain];
    pthread_mutex_unlock(&g_slotLock);
    /* Released outside the lock, because -release can run arbitrary teardown. */
    [old release];
    dispatch_async(dispatch_get_main_queue(), ^{
        if (scene != nil) {
            scene.title = forBlock == nil ? @"" : forBlock;
        }
        [forBlock release];
        [scene release];
    });
}

void CN1MacWindowSetBounds(int slot, int x, int y, int width, int height) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    /* The five geometry handshake fields move together and are read by
     * CN1MacWindowReportLayout on UIKit's main thread. Updated unlocked, a layout
     * callback landing mid-update could accept a recycled scene's old size, or clear
     * a request that had only half arrived -- either way the framework lays out at
     * dimensions the native window does not have. */
    UIWindowScene* scene;
    UIWindow* window;
    pthread_mutex_lock(&g_slotLock);
    w->pendingWidth = width;
    w->pendingHeight = height;
    /* The origin too, and the fact that one was given. Scene activation is
     * asynchronous, so a move made straight after show() usually finds no scene at
     * all: only the size was recorded, the request went to a nil scene and was
     * dropped, and adoption then placed the window at the origin it was created with.
     * Recording it here means adoption applies the move when the scene arrives. */
    w->pendingX = x;
    w->pendingY = y;
    w->positionSet = 1;
    w->awaitingWidth = width;
    w->awaitingHeight = height;
    w->staleLayoutDropped = 0;
    /* Retained for the block: a native disconnection can clear and release the
     * scene between this snapshot and the block running on the main queue, and the
     * block would then message a deallocated object. */
    scene = [w->scene retain];
    window = [w->window retain];
    pthread_mutex_unlock(&g_slotLock);
    dispatch_async(dispatch_get_main_queue(), ^{
        /* Codename One geometry is in pixels, UIKit's is in points. */
        CGFloat scale = (window != nil && window.screen != nil) ? window.screen.scale : 1.0;
        // Deliberately a single request. setBounds is defined in native coordinates
        // including chrome, so the system frame *is* what was asked for -- converging
        // on a content size here would silently redefine the API.
        CN1MacWindowRequestGeometry(scene,
                CGRectMake(x / scale, y / scale, width / scale, height / scale));
        [scene release];
        [window release];
    });
}

void CN1MacWindowGetBounds(int slot, int* out) {
    CN1MacWindow* w = slotAt(slot);
    UIWindow* window;
    if (w == NULL || out == NULL) {
        return;
    }
    /* Snapshotted and retained under the lock, then read on the main queue. This
     * runs on the event dispatch thread while CN1MacWindowSceneConnected can be
     * replacing and releasing this very window under the lock, and UIKit geometry
     * must not be read off the main thread anyway. */
    pthread_mutex_lock(&g_slotLock);
    window = [w->window retain];
    pthread_mutex_unlock(&g_slotLock);
    if (window != nil) {
        __block CGFloat scale = 1.0;
        __block CGRect f = CGRectZero;
        CN1MacRunOnMainSync(^{
            /* Reported in pixels, matching CN1MacWindowGetWidth/Height and the pixel
             * geometry Codename One passes in; UIKit frames are in points. */
            scale = window.screen != nil ? window.screen.scale : 1.0;
            f = window.frame;
        });
        [window release];
        out[0] = (int) (f.origin.x * scale);
        out[1] = (int) (f.origin.y * scale);
        out[2] = (int) (f.size.width * scale);
        out[3] = (int) (f.size.height * scale);
    } else {
        /* The requested origin, not (0,0). A window that has returned from show()
         * but whose scene has not connected yet is still readable, and a
         * setWindowSize() reads these bounds and writes the whole rectangle back --
         * so reporting a zero origin here overwrote an explicitly placed window's
         * position before it ever appeared. The size below already worked this way. */
        out[0] = w->pendingX;
        out[1] = w->pendingY;
        out[2] = w->pendingWidth;
        out[3] = w->pendingHeight;
    }
}

/*
 * The scene's size once it exists, and the requested size until then.
 *
 * The system grants a scene asynchronously and can refuse outright, so a window has
 * to be usable before one arrives: falling back to the request is what lets it lay
 * out and render meanwhile. Once a scene attaches, viewDidLayoutSubviews delivers
 * the real size and the framework re-lays out against it.
 */
/*
 * One axis of the laid-out size, synchronized the same way the bounds read is: the
 * controller, its view and the window are snapshotted and retained under the slot
 * lock, and their UIKit geometry is read on the main queue. Adoption replaces all
 * three, so reading them here unsynchronized could message a deallocated object or
 * mix a new view's bounds with an old window's scale.
 */
static int CN1MacWindowLayoutExtent(int slot, int wantWidth) {
    CN1MacWindow* w = slotAt(slot);
    CN1MacWindowController* controller;
    UIWindow* window;
    int pending;
    __block CGFloat extent = 0;
    __block CGFloat scale = 1.0;
    if (w == NULL) {
        return 0;
    }
    pthread_mutex_lock(&g_slotLock);
    controller = [w->controller retain];
    window = [w->window retain];
    pending = wantWidth ? w->pendingWidth : w->pendingHeight;
    pthread_mutex_unlock(&g_slotLock);
    if (controller == nil) {
        [window release];
        return pending;
    }
    CN1MacRunOnMainSync(^{
        /* The controller's view, not the content subview: that is what the window
         * manager lays out and what viewDidLayoutSubviews reports back. */
        CGSize size = controller.view.bounds.size;
        extent = wantWidth ? size.width : size.height;
        scale = window != nil ? window.screen.scale : 1.0;
    });
    [controller release];
    [window release];
    return (int) (extent * scale);
}

int CN1MacWindowGetWidth(int slot) {
    return CN1MacWindowLayoutExtent(slot, 1);
}

int CN1MacWindowGetHeight(int slot) {
    return CN1MacWindowLayoutExtent(slot, 0);
}

void CN1MacWindowFocus(int slot) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    UIWindow* window;
    /* Under the lock and retained, like every other snapshot handed to a block in
     * this file: scene re-adoption replaces and releases the window, and the block
     * would otherwise message a deallocated object. */
    pthread_mutex_lock(&g_slotLock);
    window = [w->window retain];
    pthread_mutex_unlock(&g_slotLock);
    dispatch_async(dispatch_get_main_queue(), ^{
        [window makeKeyAndVisible];
        [window release];
    });
}

void CN1MacWindowSetState(int slot, int state) {
    /* Catalyst exposes no programmatic minimize or zoom to a UIKit app; the
     * window manager owns those. Focus is the one that is available. */
    if (state == 3) {
        CN1MacWindowFocus(slot);
    }
}

UIView* CN1MacWindowContentView(int slot) {
    CN1MacWindow* w = slotAt(slot);
    return w == NULL ? nil : w->content;
}

/* Frees a frame's pixels once Core Graphics has finished with the image. */
static void cn1MacReleasePixels(void* info, const void* data, size_t size) {
    (void) info;
    (void) size;
    free((void*) data);
}

void CN1MacWindowPresent(int slot, void* argb, int width, int height) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL || argb == NULL || width <= 0 || height <= 0) {
        return;
    }
    CN1MacWindowView* view;
    /* Retained for the block below for the same reason as the window and the scene:
     * adoption releases the previous content view when it builds a new one. */
    pthread_mutex_lock(&g_slotLock);
    view = [w->content retain];
    pthread_mutex_unlock(&g_slotLock);
    if (view == nil) {
        return;
    }
    {
        size_t bytes = (size_t) width * (size_t) height * 4;
        /* Copy before wrapping, and do not be tempted to wrap the caller's memory to
         * save the copy. The caller hands us a Java int[]'s data pointer, and that
         * array is garbage the moment this returns -- the collector is free to reclaim
         * or move it while the image below is still referencing the memory on a later
         * main-queue turn. MacWindowManager now reuses one frame buffer per window
         * rather than allocating per frame, which makes wrapping strictly worse: the
         * next frame overwrites the very array a still-live CGImage would point at.
         * This is C heap with a deterministic lifetime (cn1MacReleasePixels below),
         * not garbage-collected memory, so it is not the per-frame GC pressure the
         * Java-side reuse was there to remove. */
        void* pixels = malloc(bytes);
        if (pixels == NULL) {
            [view release];
            return;
        }
        memcpy(pixels, argb, bytes);
        {
            CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
            /* A data provider with a release callback, rather than a bitmap context:
             * CGBitmapContextCreateImage is copy-on-write, so it is not defined when
             * the backing buffer becomes free to release. Here the buffer's lifetime is
             * explicit -- Core Graphics calls cn1MacReleasePixels once the image is
             * finished with it. */
            CGDataProviderRef provider =
                    CGDataProviderCreateWithData(NULL, pixels, bytes, cn1MacReleasePixels);
            CGImageRef image = NULL;
            if (provider != NULL) {
                /* Codename One hands back straight (non-premultiplied) ARGB and a
                 * window's content is opaque, so skip the alpha channel rather than
                 * declaring it premultiplied -- claiming premultiplied would darken
                 * every pixel that is not fully opaque. ByteOrder32Little pairs with
                 * ARGB ints on a little-endian host. */
                image = CGImageCreate(width, height, 8, 32, (size_t) width * 4, cs,
                        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Little,
                        provider, NULL, false, kCGRenderingIntentDefault);
                CGDataProviderRelease(provider);
            } else {
                free(pixels);
            }
            CGColorSpaceRelease(cs);
            if (image != NULL) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [view presentImage:image];
                    CGImageRelease(image);
                    [view release];
                });
            } else {
                [view release];
            }
        }
    }
}

BOOL CN1MacMultiWindowSupported(void) {
    id value = [[NSBundle mainBundle]
            objectForInfoDictionaryKey:@"UIApplicationSceneManifest"];
    if (![value isKindOfClass:[NSDictionary class]]) {
        return NO;
    }
    {
        id multi = [(NSDictionary*) value
                objectForKey:@"UIApplicationSupportsMultipleScenes"];
        return [multi respondsToSelector:@selector(boolValue)]
                && [multi boolValue] ? YES : NO;
    }
}

/* ------------------------------------------------------------- monitors */

int CN1MacMonitorCount(void) {
    /* UIKit on Catalyst reports the screens the app can see. */
    return (int) [UIScreen screens].count;
}

int CN1MacPrimaryMonitor(void) {
    NSArray<UIScreen*>* screens = [UIScreen screens];
    NSUInteger iter;
    for (iter = 0; iter < screens.count; iter++) {
        if (screens[iter] == [UIScreen mainScreen]) {
            return (int) iter;
        }
    }
    return 0;
}

static UIScreen* screenAt(int monitor) {
    NSArray<UIScreen*>* screens = [UIScreen screens];
    if (monitor >= 0 && monitor < (int) screens.count) {
        return screens[monitor];
    }
    return [UIScreen mainScreen];
}

void CN1MacMonitorBounds(int monitor, BOOL workArea, int* out) {
    UIScreen* screen = screenAt(monitor);
    CGRect r = screen.bounds;
    if (out == NULL) {
        return;
    }
    /* UIScreen has no work-area concept; the menu bar and dock are excluded from
     * a Catalyst app's usable area by the window server rather than reported, so
     * the bounds are the best available answer for both. */
    (void) workArea;
    /* In pixels, like CN1MacWindowGetBounds. Monitor and window rectangles are
     * combined by centerOnDesktop() and compared when a position is persisted, so
     * two coordinate systems would silently place windows wrong on a Retina
     * display. */
    CGFloat scale = screen.scale;
    out[0] = (int) (r.origin.x * scale);
    out[1] = (int) (r.origin.y * scale);
    out[2] = (int) (r.size.width * scale);
    out[3] = (int) (r.size.height * scale);
}

double CN1MacMonitorScale(int monitor) {
    return (double) screenAt(monitor).scale;
}

int CN1MacMonitorDpi(int monitor) {
    /* Catalyst reports a backing scale rather than a physical resolution; 72
     * points per inch times that scale is the conventional macOS mapping. */
    return (int) (72.0 * screenAt(monitor).scale + 0.5);
}

int CN1MacMonitorForWindow(int slot) {
    CN1MacWindow* w = slotAt(slot);
    UIWindow* window;
    __block int found = -1;
    if (w == NULL) {
        return CN1MacPrimaryMonitor();
    }
    /* Snapshotted and retained under the lock and inspected on the main queue, like
     * the geometry reads: adoption replaces and releases this window, and UIScreen
     * association is UIKit state that does not belong to the event dispatch thread. */
    pthread_mutex_lock(&g_slotLock);
    window = [w->window retain];
    pthread_mutex_unlock(&g_slotLock);
    if (window == nil) {
        return CN1MacPrimaryMonitor();
    }
    CN1MacRunOnMainSync(^{
        NSArray<UIScreen*>* screens = [UIScreen screens];
        NSUInteger iter;
        for (iter = 0; iter < screens.count; iter++) {
            if (screens[iter] == window.screen) {
                found = (int) iter;
                break;
            }
        }
    });
    [window release];
    return found >= 0 ? found : CN1MacPrimaryMonitor();
}

/* The screen the application's own scene is on. The main window has no slot, so
 * CN1MacMonitorForWindow cannot answer for it, and without this everything
 * positioned against the main form reported the primary screen even after the user
 * had dragged the application to an external display. */
/* Applies a resizability change to a window that already has a scene. The flag is
 * also recorded so a scene adopted later (a reopen, or a request still in flight)
 * picks up the current value rather than the one from creation. */
void CN1MacWindowSetResizable(int slot, BOOL resizable) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    pthread_mutex_lock(&g_slotLock);
    w->resizable = resizable ? 1 : 0;
    /* Retained for the block, as elsewhere in this file. */
    UIWindowScene* scene = [w->scene retain];
    int pixelWidth = w->pendingWidth;
    int pixelHeight = w->pendingHeight;
    /* Snapshotted under the lock with the rest: re-enabling resize has to put the
     * application's own minimum back, not the fallback floor below. Replacing it left
     * the window resizable below a minimum its Java getter still reported. */
    int minPixelWidth = w->minWidth;
    int minPixelHeight = w->minHeight;
    pthread_mutex_unlock(&g_slotLock);
    if (scene == nil) {
        /* No scene yet: adoption reads the flag. */
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        if (scene.sizeRestrictions == nil) {
            [scene release];
            return;
        }
        CGFloat scale = scene.screen != nil ? scene.screen.scale : 1.0;
        CGFloat pointWidth = pixelWidth / scale;
        CGFloat pointHeight = pixelHeight / scale;
        if (resizable) {
            if (minPixelWidth > 0 && minPixelHeight > 0) {
                /* A configured minimum wins: it is what the framework reports, so it
                 * has to be what the platform enforces. */
                scene.sizeRestrictions.minimumSize =
                        CGSizeMake(minPixelWidth / scale, minPixelHeight / scale);
            } else {
                /* No minimum configured, so fall back to a floor small enough not to
                 * be a constraint in practice while keeping the window grabbable. */
                scene.sizeRestrictions.minimumSize =
                        CGSizeMake(MIN(pointWidth, 120), MIN(pointHeight, 120));
            }
            scene.sizeRestrictions.maximumSize = CGSizeMake(CGFLOAT_MAX, CGFLOAT_MAX);
        } else {
            scene.sizeRestrictions.minimumSize = CGSizeMake(pointWidth, pointHeight);
            scene.sizeRestrictions.maximumSize = CGSizeMake(pointWidth, pointHeight);
        }
        [scene release];
    });
}

/* Hides or shows the scene's title bar chrome. Catalyst cannot remove the window
 * frame the way an undecorated desktop window does, but it can hide the title and
 * the toolbar, which is what an application supplying its own chrome needs -- and
 * without it setDecorated(false) changed the framework's state while the window
 * kept a standard title bar, and could show two sets of chrome at once. */
static void CN1MacWindowApplyDecoration(UIWindowScene* scene, int decorated) {
    if (scene == nil) {
        return;
    }
    if (@available(macCatalyst 13.0, *)) {
        UITitlebar* bar = scene.titlebar;
        if (bar != nil) {
            bar.titleVisibility = decorated ? UITitlebarTitleVisibilityVisible
                    : UITitlebarTitleVisibilityHidden;
            if (!decorated) {
                bar.toolbar = nil;
            }
        }
    }
}

/* Applies a decoration change to a window that may already have a scene, and
 * records it for a scene adopted later. */
void CN1MacWindowSetDecorated(int slot, BOOL decorated) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    pthread_mutex_lock(&g_slotLock);
    w->decorated = decorated ? 1 : 0;
    /* Retained for the block, as elsewhere in this file. */
    UIWindowScene* scene = [w->scene retain];
    pthread_mutex_unlock(&g_slotLock);
    if (scene == nil) {
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        CN1MacWindowApplyDecoration(scene, decorated ? 1 : 0);
        [scene release];
    });
}

/* Records a minimum size and applies it to an existing scene. Codename One
 * geometry is in pixels and UIKit's in points, hence the screen scale. */
void CN1MacWindowSetMinimumSize(int slot, int width, int height) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    pthread_mutex_lock(&g_slotLock);
    w->minWidth = width > 0 ? width : 0;
    w->minHeight = height > 0 ? height : 0;
    /* Retained for the block, as elsewhere in this file. */
    UIWindowScene* scene = [w->scene retain];
    int resizable = w->resizable;
    pthread_mutex_unlock(&g_slotLock);
    if (scene == nil || !resizable) {
        [scene release];
        /* A fixed window's restrictions are pinned to its size; a minimum would
         * fight that. */
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        if (scene.sizeRestrictions == nil) {
            [scene release];
            return;
        }
        CGFloat scale = scene.screen != nil ? scene.screen.scale : 1.0;
        if (width > 0 && height > 0) {
            scene.sizeRestrictions.minimumSize = CGSizeMake(width / scale, height / scale);
        } else {
            /* The SPI expresses "no minimum" as non-positive dimensions. Skipping the
             * update left the previous native minimum in force while the getter
             * reported no constraint, so a cleared minimum silently kept applying.
             * CGSizeZero is what a scene starts with. */
            scene.sizeRestrictions.minimumSize = CGSizeZero;
        }
        [scene release];
    });
}

/* The window whose field is being edited, or -1 for the application's main
 * surface. There is one native editor at a time -- IOSImplementation keeps a single
 * currentEditing -- so a single slot is enough to route it, and it avoids threading
 * a window through the twenty-odd argument editStringAt bridge. */
static int g_editingSlot = -1;

void CN1MacWindowSetEditingSlot(int slot) {
    g_editingSlot = slot;
}

/* The view the native editor should be added to. Returns nil for the main surface,
 * which leaves the caller on its existing path. */
UIView* CN1MacWindowEditingHostView(void) {
    if (g_editingSlot < 0) {
        return nil;
    }
    CN1MacWindow* w = slotAt(g_editingSlot);
    return w == NULL ? nil : w->content;
}

/*
 * Blocks input to the application's own window while a Codename One window holds an
 * application modal.
 *
 * The framework's event filter drops packed input events before they reach a
 * component, but a UIKit peer -- a native editor, a web view, a media control --
 * receives its touches directly from the window server and never passes through
 * that filter. Without this the main window's peers stayed fully interactive
 * underneath a modal that was supposed to be blocking them.
 *
 * The main scene is the connected window scene that no Codename One window claims,
 * the same way CN1MacMonitorForMainWindow finds it.
 */
/* Runs a block on the main queue and waits. Reads that have to return a value
 * cannot dispatch_async, and UIKit geometry must be read on the main thread. */
static void CN1MacRunOnMainSync(void (^block)(void)) {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_sync(dispatch_get_main_queue(), block);
    }
}

/*
 * The application's own window in pixels, or NO when it cannot be found.
 *
 * centerOn(Form) needs this: a Form lives in the main window, so centring over one
 * means centring over that window. Without it the framework falls back to the
 * monitor work area, which is a different place whenever the main window has been
 * moved, resized or simply does not fill the screen.
 *
 * The main scene is the connected window scene no Codename One window claims, the
 * same way CN1MacMonitorForMainWindow finds it.
 */
BOOL CN1MacMainWindowGetBounds(int* out) {
    __block BOOL found = NO;
    if (out == NULL) {
        return NO;
    }
    CN1MacRunOnMainSync(^{
        for (UIScene* scene in [UIApplication sharedApplication].connectedScenes) {
            if (![scene isKindOfClass:[UIWindowScene class]]) {
                continue;
            }
            UIWindowScene* windowScene = (UIWindowScene*) scene;
            if (CN1MacWindowIdForScene(windowScene) >= 0) {
                continue;
            }
            for (UIWindow* window in windowScene.windows) {
                CGFloat scale = window.screen != nil ? window.screen.scale : 1.0;
                CGRect f = window.frame;
                out[0] = (int) (f.origin.x * scale);
                out[1] = (int) (f.origin.y * scale);
                out[2] = (int) (f.size.width * scale);
                out[3] = (int) (f.size.height * scale);
                found = YES;
                break;
            }
            if (found) {
                break;
            }
        }
    });
    return found;
}

void CN1MacMainWindowSetInputEnabled(BOOL enabled) {
    dispatch_async(dispatch_get_main_queue(), ^{
        for (UIScene* scene in [UIApplication sharedApplication].connectedScenes) {
            if (![scene isKindOfClass:[UIWindowScene class]]) {
                continue;
            }
            UIWindowScene* windowScene = (UIWindowScene*) scene;
            if (CN1MacWindowIdForScene(windowScene) >= 0) {
                continue;
            }
            for (UIWindow* window in windowScene.windows) {
                window.userInteractionEnabled = enabled;
            }
        }
    });
}

int CN1MacMonitorForMainWindow(void) {
    NSArray<UIScreen*>* screens = [UIScreen screens];
    UIScreen* main = nil;
    NSUInteger iter;
    for (UIScene* scene in [UIApplication sharedApplication].connectedScenes) {
        if (![scene isKindOfClass:[UIWindowScene class]]) {
            continue;
        }
        /* Skip the scenes belonging to Codename One windows; what is left is the
         * application's own. */
        if (CN1MacWindowIdForScene((UIWindowScene*) scene) >= 0) {
            continue;
        }
        main = ((UIWindowScene*) scene).screen;
        break;
    }
    if (main == nil) {
        return CN1MacPrimaryMonitor();
    }
    screens = [UIScreen screens];
    for (iter = 0; iter < screens.count; iter++) {
        if (screens[iter] == main) {
            return (int) iter;
        }
    }
    return CN1MacPrimaryMonitor();
}

#endif /* TARGET_OS_MACCATALYST */
