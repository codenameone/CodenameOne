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
    /* The visibility the framework last asked for. A scene is requested
     * asynchronously, so a show()/hide() pair can both land before one exists;
     * without recording it the hide is dropped and adoption shows the window
     * anyway, leaving a native window on screen the framework no longer paints. */
    int pendingVisible;
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
static void CN1MacWindowReportLayout(int windowId, int width, int height) {
    int iter;
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
                    return;
                }
            }
            w->awaitingWidth = 0;
            w->awaitingHeight = 0;
            w->staleLayoutDropped = 0;
        }
        break;
    }
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
        BOOL decorated, BOOL resizable) {
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
    g_macWindows[slot].pendingTitle = [title retain];

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

    w->window.rootViewController = w->controller;
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
            scene.sizeRestrictions.minimumSize =
                    CGSizeMake(MIN(pointWidth, 120), MIN(pointHeight, 120));
            scene.sizeRestrictions.maximumSize = CGSizeMake(CGFLOAT_MAX, CGFLOAT_MAX);
        }
        CN1MacWindowRequestGeometry(scene, CGRectMake(0, 0, pointWidth, pointHeight));
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
    w->pendingVisible = 1;
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
    UIWindow* window = w->window;
    dispatch_async(dispatch_get_main_queue(), ^{
        /* Covers every touch inside the window while a modal blocks it. The scene's
         * own title bar is AppKit chrome the app does not own, so its close button
         * stays live -- see the note on CN1MacWindowSceneDisconnected. */
        window.userInteractionEnabled = enabled;
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
    /* Recorded whether or not a scene exists yet, so adoption can honour it. */
    w->pendingVisible = visible ? 1 : 0;
    UIWindow* window = w->window;
    dispatch_async(dispatch_get_main_queue(), ^{
        if (window != nil) {
            window.hidden = visible ? NO : YES;
            if (visible) {
                [window makeKeyAndVisible];
            }
        }
    });
}

void CN1MacWindowSetTitle(int slot, NSString* title) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    NSString* retained = [title retain];
    NSString* old = w->pendingTitle;
    w->pendingTitle = retained;
    [old release];
    UIWindowScene* scene = w->scene;
    dispatch_async(dispatch_get_main_queue(), ^{
        if (scene != nil) {
            scene.title = retained == nil ? @"" : retained;
        }
    });
}

void CN1MacWindowSetBounds(int slot, int x, int y, int width, int height) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    w->pendingWidth = width;
    w->pendingHeight = height;
    w->awaitingWidth = width;
    w->awaitingHeight = height;
    w->staleLayoutDropped = 0;
    UIWindowScene* scene = w->scene;
    UIWindow* window = w->window;
    dispatch_async(dispatch_get_main_queue(), ^{
        /* Codename One geometry is in pixels, UIKit's is in points. */
        CGFloat scale = (window != nil && window.screen != nil) ? window.screen.scale : 1.0;
        CN1MacWindowRequestGeometry(scene,
                CGRectMake(x / scale, y / scale, width / scale, height / scale));
    });
}

void CN1MacWindowGetBounds(int slot, int* out) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL || out == NULL) {
        return;
    }
    if (w->window != nil) {
        /* Reported in pixels, matching CN1MacWindowGetWidth/Height and the pixel
         * geometry Codename One passes in; UIKit frames are in points. */
        CGFloat scale = w->window.screen != nil ? w->window.screen.scale : 1.0;
        CGRect f = w->window.frame;
        out[0] = (int) (f.origin.x * scale);
        out[1] = (int) (f.origin.y * scale);
        out[2] = (int) (f.size.width * scale);
        out[3] = (int) (f.size.height * scale);
    } else {
        out[0] = 0;
        out[1] = 0;
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
int CN1MacWindowGetWidth(int slot) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return 0;
    }
    if (w->controller == nil) {
        return w->pendingWidth;
    }
    /* The controller's view, not the content subview: that is what the window
     * manager lays out and what viewDidLayoutSubviews reports back. */
    CGFloat scale = w->window != nil ? w->window.screen.scale : 1.0;
    return (int) (w->controller.view.bounds.size.width * scale);
}

int CN1MacWindowGetHeight(int slot) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return 0;
    }
    if (w->controller == nil) {
        return w->pendingHeight;
    }
    CGFloat scale = w->window != nil ? w->window.screen.scale : 1.0;
    return (int) (w->controller.view.bounds.size.height * scale);
}

void CN1MacWindowFocus(int slot) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    UIWindow* window = w->window;
    dispatch_async(dispatch_get_main_queue(), ^{
        [window makeKeyAndVisible];
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
    CN1MacWindowView* view = w->content;
    if (view == nil) {
        return;
    }
    {
        size_t bytes = (size_t) width * (size_t) height * 4;
        /* Copy before wrapping. The caller hands us a Java int[]'s data pointer, and
         * that array is garbage the moment this returns -- the collector is free to
         * reclaim or move it while the image below is still referencing the memory on
         * a later main-queue turn. */
        void* pixels = malloc(bytes);
        if (pixels == NULL) {
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
                });
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
    NSArray<UIScreen*>* screens;
    NSUInteger iter;
    if (w == NULL || w->window == nil) {
        return CN1MacPrimaryMonitor();
    }
    screens = [UIScreen screens];
    for (iter = 0; iter < screens.count; iter++) {
        if (screens[iter] == w->window.screen) {
            return (int) iter;
        }
    }
    return CN1MacPrimaryMonitor();
}

#endif /* TARGET_OS_MACCATALYST */
