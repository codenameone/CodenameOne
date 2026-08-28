/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
#if !TARGET_OS_WATCH

#import "CodenameOne_GLSceneDelegate.h"

#if TARGET_OS_MACCATALYST
#import "CN1MacWindows.h"
/* True when the scene was claimed by a Codename One Window. */
extern BOOL CN1MacWindowAdoptScene(UIWindowScene* scene, NSSet<NSUserActivity*>* activities);
extern int CN1MacWindowIdForScene(UIWindowScene* scene);
extern void CN1MacWindowSceneDisconnected(UIWindowScene* scene);
extern void CN1MacWindowDeliverFocus(int windowId, BOOL gained);
extern void CN1MacWindowDeliverVisibility(int windowId, BOOL shown);
#endif

#ifdef CN1_USE_UI_SCENE
@implementation CodenameOne_GLSceneDelegate

@synthesize window=_window;

- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session options:(UISceneConnectionOptions *)connectionOptions API_AVAILABLE(ios(13.0))
{
    if (![scene isKindOfClass:[UIWindowScene class]]) {
        return;
    }
#if TARGET_OS_MACCATALYST
    // A second scene of the app role belongs to a com.codename1.ui.Window, not to
    // the application's main form. Hand it to the window layer, which owns it from
    // here; only the first scene installs the main root view controller.
    // The connection's activities carry the token the activation request was stamped
    // with, so the window layer can take the scene its own request produced rather
    // than assuming scenes connect in the order they were asked for.
    if (CN1MacWindowAdoptScene((UIWindowScene *)scene, connectionOptions.userActivities)) {
        return;
    }
#endif
#if !TARGET_OS_MACCATALYST
    /* One main surface, so one scene may own it. Codename One has a single global
     * current form and a single rendering surface off Catalyst, and installing a root
     * view controller into a second scene gives two live main surfaces competing for
     * that one state -- which shows up as a rendering fault, not as an error.
     *
     * A plain iOS build never gets here twice: it declares
     * UIApplicationSupportsMultipleScenes false, so the system creates one scene. The
     * case this covers is the iOS destination of a project generated for Mac Catalyst,
     * which shares that project's Info.plist and therefore its true value, plus any
     * scene the system restores on its own.
     *
     * Asked of the connected scenes rather than latched in a static, so a scene that
     * disconnects and reconnects -- which iOS does on its own schedule -- is still
     * allowed to take the main surface back. */
    for (UIScene *eachScene in [UIApplication sharedApplication].connectedScenes) {
        if (eachScene == scene) {
            continue;
        }
        id eachDelegate = eachScene.delegate;
        if ([eachDelegate isKindOfClass:[CodenameOne_GLSceneDelegate class]]
                && ((CodenameOne_GLSceneDelegate *)eachDelegate).window != nil) {
            return;
        }
    }
#endif
    UIWindow *window = [[UIWindow alloc] initWithWindowScene:(UIWindowScene *)scene];
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1InstallRootViewControllerIntoWindow:window];
    self.window = window;
#ifndef CN1_USE_ARC
    [window release];
#endif

#if TARGET_OS_MACCATALYST
    // Opt-in deterministic window sizing for screenshot CI. Read a
    // "<W>x<H>" value from the CN1MacFixedWindowSize Info.plist key --
    // the macNative.fixedWindowSize build hint plumbs the user's
    // setting through there. Absent or unparseable -> normal Mac
    // resize behaviour (production apps are unaffected). Setting it
    // pins both the minimum and maximum scene size so every launch
    // produces a byte-identical window for strict-pixel comparison.
    if (@available(macCatalyst 13.0, *)) {
        NSString *fixedSpec = [[NSBundle mainBundle]
                objectForInfoDictionaryKey:@"CN1MacFixedWindowSize"];
        if ([fixedSpec isKindOfClass:[NSString class]] && fixedSpec.length > 0) {
            NSArray<NSString *> *parts = [fixedSpec componentsSeparatedByString:@"x"];
            if (parts.count == 2) {
                CGFloat w = [parts[0] doubleValue];
                CGFloat h = [parts[1] doubleValue];
                if (w > 0 && h > 0) {
                    UIWindowScene *ws = (UIWindowScene *)scene;
                    if (ws.sizeRestrictions != nil) {
                        CGSize fixed = CGSizeMake(w, h);
                        ws.sizeRestrictions.minimumSize = fixed;
                        ws.sizeRestrictions.maximumSize = fixed;
                    }
                }
            }
        }
    }
#endif

    UIOpenURLContext *urlContext = [connectionOptions.URLContexts anyObject];
    if (urlContext != nil) {
        [appDelegate cn1OpenURL:[UIApplication sharedApplication] url:urlContext.URL sourceApplication:urlContext.options.sourceApplication annotation:urlContext.options.annotation];
    }
    NSUserActivity *userActivity = [connectionOptions.userActivities anyObject];
    if (userActivity != nil) {
        [appDelegate cn1ContinueUserActivity:userActivity];
    }
}

#if TARGET_OS_MACCATALYST
/*
 * The window id of a Codename One window's scene, or -1 for the application's own
 * scene. Every scene lifecycle callback has to ask this before running the global
 * application path: a Codename One window is one window of the application, not the
 * application, so treating its scene as the app's suspends everything -- including
 * the still-visible main window -- and a disconnected scene never comes back to undo
 * it. Shared rather than repeated, because two of these callbacks were missing the
 * check while the other two had it.
 */
static int cn1MacCodenameOneWindowScene(UIScene *scene) {
    if ([scene isKindOfClass:[UIWindowScene class]]) {
        return CN1MacWindowIdForScene((UIWindowScene *)scene);
    }
    return -1;
}

/*
 * Whether the global "application is active" path is currently in effect. Focus
 * moving between the application's own scenes is not the application resigning:
 * running the global path there marks the implementation inactive and fires the
 * application's pause hook, which left the app paused -- main window included --
 * until the main window happened to be focused again. Suppressing the resign means
 * the matching resume has to be suppressed too, or the application would be resumed
 * from a pause it never entered, so both go through this one flag.
 */
static BOOL cn1MacApplicationActive = NO;

/*
 * Whether the application as a whole has been put in the background. Minimizing one
 * window is not the application backgrounding, so the global path is gated on every
 * scene being backgrounded -- and the matching foreground has to be gated the same
 * way, or the application would be resumed from a suspension it never entered.
 * Starts YES so the first scene entering the foreground at launch still runs the
 * global path exactly as it did before any of this existed.
 */
static BOOL cn1MacApplicationBackgrounded = YES;

/* YES while any of the application's scenes is still in the foreground at all. */
static BOOL cn1MacAnySceneForeground(void) {
    for (UIScene *each in [UIApplication sharedApplication].connectedScenes) {
        UISceneActivationState state = each.activationState;
        if (state == UISceneActivationStateForegroundActive
                || state == UISceneActivationStateForegroundInactive) {
            return YES;
        }
    }
    return NO;
}

/* YES while any of the application's scenes is foreground-active. */
static BOOL cn1MacAnySceneActive(void) {
    for (UIScene *each in [UIApplication sharedApplication].connectedScenes) {
        if (each.activationState == UISceneActivationStateForegroundActive) {
            return YES;
        }
    }
    return NO;
}

/*
 * Resigns the application, but only once it is clear the application itself is no
 * longer active. The scene gaining focus has not necessarily reached
 * ForegroundActive by the time the losing scene reports its resign, so reading the
 * activation states here would see none active and pause the app on every
 * window-to-window focus change. Deferring one runloop turn lets the gaining scene
 * settle first: by then either one of our scenes is active, and this does nothing,
 * or none is and the application really did resign.
 */
static void cn1MacResignActiveIfApplicationInactive(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!cn1MacApplicationActive || cn1MacAnySceneActive()) {
            return;
        }
        cn1MacApplicationActive = NO;
        CodenameOne_GLAppDelegate *appDelegate =
                (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
        [appDelegate cn1ApplicationWillResignActive];
    });
}
#endif

- (void)sceneDidBecomeActive:(UIScene *)scene API_AVAILABLE(ios(13.0))
{
#if TARGET_OS_MACCATALYST
    // A Codename One window's scene: report the focus rather than treating it as the
    // application becoming active, which would run the main form's resume path.
    {
        int windowId = cn1MacCodenameOneWindowScene(scene);
        if (windowId >= 0) {
            CN1MacWindowDeliverFocus(windowId, YES);
            // Clicking one of our windows is still how the user brings a resigned
            // application back, so resume it here -- but only from a real resign,
            // which is what the flag distinguishes.
            if (!cn1MacApplicationActive) {
                cn1MacApplicationActive = YES;
                CodenameOne_GLAppDelegate *windowSceneDelegate =
                        (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
                [windowSceneDelegate cn1ApplicationDidBecomeActive];
            }
            return;
        }
    }
    // Focus arriving from one of our own window scenes never resigned the
    // application, so there is nothing to resume.
    if (cn1MacApplicationActive) {
        return;
    }
    cn1MacApplicationActive = YES;
#endif
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ApplicationDidBecomeActive];
}

- (void)sceneWillResignActive:(UIScene *)scene API_AVAILABLE(ios(13.0))
{
#if TARGET_OS_MACCATALYST
    {
        int windowId = cn1MacCodenameOneWindowScene(scene);
        if (windowId >= 0) {
            CN1MacWindowDeliverFocus(windowId, NO);
        }
    }
    // Every Catalyst resign goes through the deferred check, main scene included: the
    // main window losing focus to one of our windows is not the application resigning
    // either, and the check is what tells the two apart.
    cn1MacResignActiveIfApplicationInactive();
#else
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ApplicationWillResignActive];
#endif
}

- (void)sceneDidDisconnect:(UIScene *)scene API_AVAILABLE(ios(13.0))
{
#if TARGET_OS_MACCATALYST
    // The user closed a Codename One window with the native close control. Without
    // this the framework never learns: close listeners and setCloseOperation are
    // skipped and the window stays registered and painted with no scene behind it.
    if ([scene isKindOfClass:[UIWindowScene class]]) {
        CN1MacWindowSceneDisconnected((UIWindowScene *)scene);
    }
#endif
}

- (void)sceneWillEnterForeground:(UIScene *)scene API_AVAILABLE(ios(13.0))
{
#if TARGET_OS_MACCATALYST
    // A Codename One window coming back from minimized is not the application
    // returning to the foreground; running the global resume path here would
    // resume an application that was never suspended. The per-window restore is
    // still reported, as the matching background branch reports the minimize.
    {
        int windowId = cn1MacCodenameOneWindowScene(scene);
        // Window id 0 is the main window: reporting it is what cascades the windows
        // it owns back, and core ignores the id-0 lifecycle notification itself.
        CN1MacWindowDeliverVisibility(windowId >= 0 ? windowId : 0, YES);
    }
    // Restoring any window resumes an application that really was backgrounded, but
    // one that never was has nothing to resume. This cannot ask the scenes, because
    // the scene entering the foreground has not got there yet when this fires.
    if (!cn1MacApplicationBackgrounded) {
        return;
    }
    cn1MacApplicationBackgrounded = NO;
#endif
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ApplicationWillEnterForeground];
}

- (void)sceneDidEnterBackground:(UIScene *)scene API_AVAILABLE(ios(13.0))
{
#if TARGET_OS_MACCATALYST
    // Minimizing or closing one Codename One window is not the application going to
    // the background. The global path sets isAppSuspended, stops the garbage
    // collector and notifies the application of suspension -- and if this scene is
    // then disconnected it never enters the foreground again to undo any of it,
    // leaving the still-visible main window suspended for good.
    //
    // Suppressing the global path is not the same as reporting nothing, though:
    // without the per-window notification the framework kept the window visible,
    // painting it and running its animations while it was minimized.
    {
        int windowId = cn1MacCodenameOneWindowScene(scene);
        // Window id 0 is the main window. The main scene minimizing is no more the
        // application backgrounding than one of ours is, and reporting it is also
        // what takes the windows it owns down with it.
        CN1MacWindowDeliverVisibility(windowId >= 0 ? windowId : 0, NO);
    }
    // Only once nothing of ours is left in the foreground is the application really
    // backgrounding. Reaching the global path early sets isAppSuspended, stops the
    // garbage collector and fires the suspend callback while another window is still
    // perfectly usable -- and a scene that is then disconnected never comes back to
    // undo any of it.
    if (cn1MacAnySceneForeground() || cn1MacApplicationBackgrounded) {
        return;
    }
    cn1MacApplicationBackgrounded = YES;
#endif
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ApplicationDidEnterBackground];
}

- (void)scene:(UIScene *)scene openURLContexts:(NSSet<UIOpenURLContext *> *)URLContexts API_AVAILABLE(ios(13.0))
{
    UIOpenURLContext *urlContext = [URLContexts anyObject];
    if (urlContext == nil) {
        return;
    }
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1OpenURL:[UIApplication sharedApplication] url:urlContext.URL sourceApplication:urlContext.options.sourceApplication annotation:urlContext.options.annotation];
}

- (void)scene:(UIScene *)scene continueUserActivity:(NSUserActivity *)userActivity API_AVAILABLE(ios(13.0))
{
    CodenameOne_GLAppDelegate *appDelegate = (CodenameOne_GLAppDelegate *)[UIApplication sharedApplication].delegate;
    [appDelegate cn1ContinueUserActivity:userActivity];
}

@end
#endif

#else
// Compiled out on watchOS: this file is OpenGL ES / Metal / UIKit-only and the watch
// slice renders through the Core Graphics backend instead. The typedef keeps the
// translation unit non-empty, which ISO C requires.
typedef int cn1_codenameone_glscenedelegate_unused_on_watch;
#endif // !TARGET_OS_WATCH
