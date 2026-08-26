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
#import <UserNotifications/UserNotifications.h>
#import "CN1MacHost.h"
#import "CN1MacMenu.h"
#include "cn1_globals.h"
#include "com_codename1_impl_ios_IOSImplementation.h"
#include "com_codename1_ui_Display.h"

/*
 * The application delegate.
 *
 * The UIKit ports get one from UIApplicationMain naming
 * CodenameOne_GLAppDelegate; that file is UIKit through and through -- scenes,
 * launch options, background modes -- and is excluded from this port. What it
 * carries that macOS also needs is a much shorter list: the remote-notification
 * handshake, the lifecycle forwards, and URL opening.
 *
 * The counter below is one of them. It lives here rather than beside the
 * registerPush native for the same reason it lives in the UIKit delegate: it is
 * decremented by a delegate callback, and a counter owned by the code that
 * clears it is the one that cannot be left dangling.
 */
int pendingRemoteNotificationRegistrations = 0;

/*
 * Whether the Java side exists yet.
 *
 * The generated main installs this delegate and enters [NSApp run] BEFORE the
 * thread that constructs IOSImplementation has got there -- it dispatches the
 * app's main onto a background queue and does not wait. AppKit then activates
 * the application immediately, so applicationDidBecomeActive: can arrive first,
 * and IOSImplementation.applicationDidBecomeActive() synchronizes on
 * instance.onActiveListeners with no null guard. Whether launch survives is
 * down to thread scheduling.
 *
 * So the transitions are held until initVM publishes the lifecycle callback,
 * and the state that accumulated meanwhile is replayed then. State, not events:
 * these are two independent level pairs -- active/inactive and hidden/visible --
 * and what the application needs to be told on startup is where each one ENDED
 * UP, not every edge it passed through while nobody was listening.
 *
 * Everything here runs on the main queue, including the flush, so the flag needs
 * no lock: cn1_mac_runtime_markJavaReady is called from the bootstrap thread and
 * hops over rather than touching this state directly.
 */
static BOOL cn1MacJavaReady = NO;
static int cn1MacPendingActive = -1;   // 1 became active, 0 resigned, -1 nothing
static int cn1MacPendingHidden = -1;   // 1 hidden, 0 unhidden, -1 nothing
static NSMutableArray<NSString *> *cn1MacPendingURLs = nil;
static NSMutableArray<NSDictionary *> *cn1MacPendingPushes = nil;

/// Decodes one APNs payload the way the shared iOS router does and hands each
/// part to the application.
///
/// The four push types the framework defines are distinguished by what the
/// payload carries, not by a field naming them: an aps.alert dictionary with a
/// title and a body is type 4, an alert string is type 3 when a `meta` key rides
/// along and type 1 when it does not, and a payload with `meta` and NO alert at
/// all is type 2 -- the data-only push.
///
/// Reading only aps.alert.body, as this used to, dropped every type 2 on the
/// floor: a silent notification has no alert by definition, so the whole
/// category of background pushes never reached the application on this port.
static void cn1MacDeliverPush(NSDictionary *userInfo) {
    if (userInfo == nil) {
        return;
    }
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    // Managed push carries the canonical typed envelope as a JSON object under
    // "cn1". It goes out intact, with a null type, BEFORE the historical
    // aps/meta decoder can split or rewrite it -- which is what the UIKit
    // delegate does too, and for the same reason: an envelope with no legacy
    // fields is dropped by that decoder entirely, and one that happens to carry
    // an alert reaches PushClient as the alert text rather than as JSON and is
    // rejected as invalid_envelope.
    id cn1Envelope = [userInfo objectForKey:@"cn1"];
    if ([cn1Envelope isKindOfClass:[NSDictionary class]]) {
        NSError *jsonError = nil;
        NSData *jsonData = [NSJSONSerialization dataWithJSONObject:cn1Envelope options:0 error:&jsonError];
        if (jsonData != nil && jsonError == nil) {
            NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
#ifndef CN1_USE_ARC
            [jsonString autorelease];
#endif
            com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(
                threadStateData, fromNSString(threadStateData, jsonString), JAVA_NULL);
            return;
        }
    }
    NSDictionary *aps = [userInfo objectForKey:@"aps"];
    id alert = aps != nil ? [aps objectForKey:@"alert"] : nil;
    id meta = [userInfo objectForKey:@"meta"];
    BOOL includedBody = NO;

    if ([alert isKindOfClass:[NSDictionary class]]) {
        NSDictionary *alertDict = (NSDictionary *)alert;
        NSString *title = [alertDict objectForKey:@"title"];
        NSString *body = [alertDict objectForKey:@"body"];
        if (title != nil && body != nil) {
            includedBody = YES;
            // "title;body" is the type 4 wire form the Java side parses.
            NSString *combined = [NSString stringWithFormat:@"%@;%@", title, body];
            com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(
                threadStateData, fromNSString(threadStateData, combined),
                fromNSString(threadStateData, @"4"));
        }
    } else if ([alert isKindOfClass:[NSString class]]) {
        includedBody = YES;
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(
            threadStateData, fromNSString(threadStateData, (NSString *)alert),
            fromNSString(threadStateData, meta != nil ? @"3" : @"1"));
    }

    if (meta != nil) {
        NSString *metaText = [meta isKindOfClass:[NSString class]]
            ? (NSString *)meta : [meta description];
        // A null type when the body already went out: the type was reported
        // with it, and repeating it would count the same push twice.
        com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(
            threadStateData, fromNSString(threadStateData, metaText),
            includedBody ? JAVA_NULL : fromNSString(threadStateData, @"2"));
    }
}

static void cn1MacDeliverURL(NSString *url) {
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    JAVA_OBJECT str = fromNSString(threadStateData, url);
    // Through shouldApplicationHandleURL, not straight into the AppArg property.
    // That method notifies a URLCallback application AND sets AppArg, and it is
    // what the UIKit delegate calls; setting the property alone leaves every app
    // implementing URLCallback.shouldApplicationHandleURL with no notification
    // that a deep link arrived at all.
    com_codename1_impl_ios_IOSImplementation_shouldApplicationHandleURL___java_lang_String_java_lang_String_R_boolean(
        threadStateData, str, JAVA_NULL);
}

/// Called from initVM once IOSImplementation exists and its lifecycle callback
/// is installed. Mirrors the watch port's cn1_watch_runtime_markJavaReady, and
/// for the same reason: that is the first honest moment at which the Java side
/// can be told anything.
void cn1_mac_runtime_markJavaReady(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        cn1MacJavaReady = YES;
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        if (cn1MacPendingActive == 1) {
            com_codename1_impl_ios_IOSImplementation_applicationDidBecomeActive__(threadStateData);
        } else if (cn1MacPendingActive == 0) {
            com_codename1_impl_ios_IOSImplementation_applicationWillResignActive__(threadStateData);
        }
        if (cn1MacPendingHidden == 1) {
            com_codename1_impl_ios_IOSImplementation_applicationDidEnterBackground__(threadStateData);
        } else if (cn1MacPendingHidden == 0) {
            com_codename1_impl_ios_IOSImplementation_applicationWillEnterForeground__(threadStateData);
        }
        cn1MacPendingActive = -1;
        cn1MacPendingHidden = -1;
        for (NSString *url in cn1MacPendingURLs) {
            cn1MacDeliverURL(url);
        }
        [cn1MacPendingURLs removeAllObjects];
        for (NSDictionary *payload in cn1MacPendingPushes) {
            cn1MacDeliverPush(payload);
        }
        [cn1MacPendingPushes removeAllObjects];
    });
}

/// Whether the framework may be called. Also the answer to "is there a Display
/// yet", which is why the resize hook asks it too.
BOOL cn1MacRuntimeIsJavaReady(void) {
    return cn1MacJavaReady;
}

/// Set when the application is hidden or its last window closes, mirroring the
/// UIKit flag the shared code reads to decide whether it may paint.
extern BOOL isAppSuspended;

@interface CN1MacAppDelegate : NSObject <NSApplicationDelegate>
@end

@implementation CN1MacAppDelegate

// ---- lifecycle -----------------------------------------------------------
//
// A Mac has no background state, so the four UIKit transitions collapse onto
// two pairs: active/inactive when the application is focused or not, and
// hide/unhide, which is the closest thing to entering and leaving the
// background. Both are reported so an application that pauses work off screen
// keeps working here.

// isAppSuspended is set whether or not Java is up: it is C state the shared
// paint path reads, and it has to be right from the first frame.

- (void)applicationDidBecomeActive:(NSNotification *)notification {
    isAppSuspended = NO;
    if (!cn1MacJavaReady) {
        cn1MacPendingActive = 1;
        return;
    }
    com_codename1_impl_ios_IOSImplementation_applicationDidBecomeActive__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationWillResignActive:(NSNotification *)notification {
    if (!cn1MacJavaReady) {
        cn1MacPendingActive = 0;
        return;
    }
    com_codename1_impl_ios_IOSImplementation_applicationWillResignActive__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationDidHide:(NSNotification *)notification {
    isAppSuspended = YES;
    if (!cn1MacJavaReady) {
        cn1MacPendingHidden = 1;
        return;
    }
    com_codename1_impl_ios_IOSImplementation_applicationDidEnterBackground__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationWillUnhide:(NSNotification *)notification {
    isAppSuspended = NO;
    if (!cn1MacJavaReady) {
        cn1MacPendingHidden = 0;
        return;
    }
    com_codename1_impl_ios_IOSImplementation_applicationWillEnterForeground__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationWillTerminate:(NSNotification *)notification {
    // Not queued: there is nothing after this to replay it on, and calling into
    // a Java side that does not exist is the crash this guard exists to avoid.
    if (!cn1MacJavaReady) {
        return;
    }
    com_codename1_impl_ios_IOSImplementation_applicationWillTerminate__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

/// Quitting with the last window is the behaviour every other Codename One port
/// has, and what Display.exitApplication and a window close both mean here. It
/// is not the Mac convention for a document application -- those stay running
/// with no windows -- but a Codename One application has one main window that
/// is the application, so leaving the process alive after it closes would
/// strand it in the Dock with no way back.
- (BOOL)applicationShouldTerminateAfterLastWindowClosed:(NSApplication *)sender {
    return YES;
}

- (void)application:(NSApplication *)application openURLs:(NSArray<NSURL *> *)urls {
    for (NSURL *url in urls) {
        if (url == nil) {
            continue;
        }
        NSString *spec = url.isFileURL ? url.path : [url absoluteString];
        // A launch URL arrives before the Java side exists, which is the whole
        // point of a launch URL, so it is held rather than dropped.
        if (!cn1MacJavaReady) {
            if (cn1MacPendingURLs == nil) {
                cn1MacPendingURLs = [[NSMutableArray alloc] init];
            }
            [cn1MacPendingURLs addObject:spec];
            break;
        }
        cn1MacDeliverURL(spec);
        break;
    }
}

// ---- remote notifications ------------------------------------------------

- (void)application:(NSApplication *)application
        didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    if (pendingRemoteNotificationRegistrations > 0) {
        pendingRemoteNotificationRegistrations--;
    }
    // Formatted from the bytes rather than from the NSData description, which
    // changed shape between releases and is not a documented encoding.
    const unsigned char *bytes = [deviceToken bytes];
    NSMutableString *token = [NSMutableString stringWithCapacity:deviceToken.length * 2];
    for (NSUInteger i = 0; i < deviceToken.length; i++) {
        [token appendFormat:@"%02x", bytes[i]];
    }
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    com_codename1_impl_ios_IOSImplementation_pushRegistered___java_lang_String(threadStateData,
        fromNSString(threadStateData, token));
}

- (void)application:(NSApplication *)application
        didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    if (pendingRemoteNotificationRegistrations > 0) {
        pendingRemoteNotificationRegistrations--;
    }
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    com_codename1_impl_ios_IOSImplementation_pushRegistrationError___java_lang_String(threadStateData,
        fromNSString(threadStateData, [error localizedDescription]));
}

- (void)application:(NSApplication *)application
        didReceiveRemoteNotification:(NSDictionary *)userInfo {
    if (userInfo == nil) {
        return;
    }
    // Queued like the lifecycle transitions and the launch URL, and for the
    // sharper version of the same reason: a notification that LAUNCHED the app
    // arrives before the Java bootstrap has run, IOSImplementation.pushCallback
    // is still null, and pushReceived() drops the payload on the floor. The
    // application then never learns about the push that started it -- the one
    // push it most needs.
    //
    // The whole payload is held, not a decoded body: the decode decides the push
    // TYPE, and doing it here would have to be repeated on replay anyway.
    if (!cn1MacJavaReady) {
        if (cn1MacPendingPushes == nil) {
            cn1MacPendingPushes = [[NSMutableArray alloc] init];
        }
        [cn1MacPendingPushes addObject:userInfo];
        return;
    }
    cn1MacDeliverPush(userInfo);
}

@end

/// Installs the delegate and the menu bar. Called from the generated main,
/// before [NSApp run].
void CN1MacInstallAppDelegate(void) {
    static CN1MacAppDelegate *delegate = nil;
    if (delegate == nil) {
        delegate = [[CN1MacAppDelegate alloc] init];
        [NSApp setDelegate:delegate];
    }
}
