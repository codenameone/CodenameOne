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

/*
 * Delivery into the framework. Defined in IOSNative.m alongside the existing
 * pointerPressed / screenSizeChanged bridges, so all the ParparVM thread-state
 * handling stays in one place.
 */
extern void CN1MacWindowDeliverClose(int windowId);
extern void CN1MacWindowDeliverFocus(int windowId, BOOL gained);
extern void CN1MacWindowDeliverResize(int windowId, int width, int height);
extern void CN1MacWindowDeliverPointer(int windowId, int type, int x, int y);

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
    CGPoint p = [t locationInView:self];
    CN1MacWindowDeliverPointer(self.windowId, type, (int) p.x, (int) p.y);
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

/* The view controller a Codename One window scene is rooted at. */
@interface CN1MacWindowController : UIViewController
@property (nonatomic, assign) int windowId;
@property (nonatomic, assign) CN1MacWindowView* content;
@end

@implementation CN1MacWindowController

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    CGSize size = self.view.bounds.size;
    CGFloat scale = self.view.window != nil ? self.view.window.screen.scale : 1.0;
    CN1MacWindowDeliverResize(self.windowId,
            (int) (size.width * scale), (int) (size.height * scale));
}

@end

typedef struct {
    UIWindowScene* scene;
    UIWindow* window;
    CN1MacWindowController* controller;
    CN1MacWindowView* content;
    int windowId;
    int inUse;
    int pendingWidth;
    int pendingHeight;
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

static int slotForScene(UIWindowScene* scene) {
    int iter;
    for (iter = 0; iter < CN1_MAC_MAX_WINDOWS; iter++) {
        if (g_macWindows[iter].inUse && g_macWindows[iter].scene == scene) {
            return iter;
        }
    }
    return -1;
}

/* A slot that has been requested but has no scene yet; the system hands scenes
 * back asynchronously, so the first unattached slot claims the next arrival. */
static int slotAwaitingScene(void) {
    int iter;
    for (iter = 0; iter < CN1_MAC_MAX_WINDOWS; iter++) {
        if (g_macWindows[iter].inUse && g_macWindows[iter].scene == nil) {
            return iter;
        }
    }
    return -1;
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
    memset(&g_macWindows[slot], 0, sizeof(CN1MacWindow));
    g_macWindows[slot].inUse = 1;
    g_macWindows[slot].windowId = windowId;
    g_macWindows[slot].pendingWidth = width;
    g_macWindows[slot].pendingHeight = height;
    g_macWindows[slot].pendingTitle = [title retain];

    dispatch_async(dispatch_get_main_queue(), ^{
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
    if (slotAwaitingScene() < 0) {
        return NO;
    }
    CN1MacWindowSceneConnected(scene);
    return YES;
}

void CN1MacWindowSceneConnected(UIWindowScene* scene) {
    int slot = slotAwaitingScene();
    CN1MacWindow* w;
    if (slot < 0) {
        return;
    }
    w = &g_macWindows[slot];
    w->scene = [scene retain];

    w->window = [[UIWindow alloc] initWithWindowScene:scene];
    w->controller = [[CN1MacWindowController alloc] init];
    w->controller.windowId = w->windowId;

    w->content = [[CN1MacWindowView alloc] initWithFrame:w->window.bounds];
    w->content.windowId = w->windowId;
    w->content.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    w->controller.view.backgroundColor = [UIColor blackColor];
    [w->controller.view addSubview:w->content];
    w->controller.content = w->content;

    w->window.rootViewController = w->controller;
    [w->window makeKeyAndVisible];

    if (w->pendingTitle != nil) {
        scene.title = w->pendingTitle;
    }
    if (scene.sizeRestrictions != nil && w->pendingWidth > 0 && w->pendingHeight > 0) {
        scene.sizeRestrictions.minimumSize = CGSizeMake(200, 150);
    }
}

void CN1MacWindowDestroy(int slot) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
    UIWindowScene* scene = w->scene;
    UIWindow* window = w->window;
    CN1MacWindowController* controller = w->controller;
    CN1MacWindowView* content = w->content;
    NSString* title = w->pendingTitle;
    memset(w, 0, sizeof(CN1MacWindow));

    dispatch_async(dispatch_get_main_queue(), ^{
        if (window != nil) {
            window.hidden = YES;
            window.rootViewController = nil;
        }
        if (scene != nil) {
            UISceneDestructionRequestOptions* opts =
                    [[UISceneDestructionRequestOptions alloc] init];
            [[UIApplication sharedApplication] requestSceneSessionDestruction:scene.session
                                                                      options:opts
                                                                 errorHandler:nil];
            [opts release];
        }
        /* No ARC in this port: everything retained above is released here, after
         * UIKit has finished with it on the main thread. */
        [content release];
        [controller release];
        [window release];
        [scene release];
        [title release];
    });
}

void CN1MacWindowShow(int slot, BOOL visible) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return;
    }
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
    UIWindowScene* scene = w->scene;
    dispatch_async(dispatch_get_main_queue(), ^{
        if (@available(macCatalyst 16.0, *)) {
            if (scene != nil) {
                /* Catalyst has no direct window-move API; the geometry request is
                 * the supported way to ask for a size. Position is left to the
                 * window manager, which is also what a Mac user expects. */
                UIWindowSceneGeometryPreferencesMac* prefs =
                        [[UIWindowSceneGeometryPreferencesMac alloc]
                                initWithSystemFrame:CGRectMake(x, y, width, height)];
                [scene requestGeometryUpdateWithPreferences:prefs errorHandler:nil];
                [prefs release];
            }
        }
    });
}

void CN1MacWindowGetBounds(int slot, int* out) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL || out == NULL) {
        return;
    }
    if (w->window != nil) {
        CGRect f = w->window.frame;
        out[0] = (int) f.origin.x;
        out[1] = (int) f.origin.y;
        out[2] = (int) f.size.width;
        out[3] = (int) f.size.height;
    } else {
        out[0] = 0;
        out[1] = 0;
        out[2] = w->pendingWidth;
        out[3] = w->pendingHeight;
    }
}

int CN1MacWindowGetWidth(int slot) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return 0;
    }
    if (w->content != nil) {
        CGFloat scale = w->window != nil ? w->window.screen.scale : 1.0;
        return (int) (w->content.bounds.size.width * scale);
    }
    return w->pendingWidth;
}

int CN1MacWindowGetHeight(int slot) {
    CN1MacWindow* w = slotAt(slot);
    if (w == NULL) {
        return 0;
    }
    if (w->content != nil) {
        CGFloat scale = w->window != nil ? w->window.screen.scale : 1.0;
        return (int) (w->content.bounds.size.height * scale);
    }
    return w->pendingHeight;
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
        CGColorSpaceRef cs = CGColorSpaceCreateDeviceRGB();
        CGContextRef ctx = CGBitmapContextCreate(argb, width, height, 8, width * 4, cs,
                kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Little);
        CGImageRef image = ctx != NULL ? CGBitmapContextCreateImage(ctx) : NULL;
        CGColorSpaceRelease(cs);
        if (ctx != NULL) {
            CGContextRelease(ctx);
        }
        if (image != NULL) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [view presentImage:image];
                CGImageRelease(image);
            });
        }
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
    out[0] = (int) r.origin.x;
    out[1] = (int) r.origin.y;
    out[2] = (int) r.size.width;
    out[3] = (int) r.size.height;
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
