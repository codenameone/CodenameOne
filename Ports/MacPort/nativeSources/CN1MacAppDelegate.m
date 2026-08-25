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

- (void)applicationDidBecomeActive:(NSNotification *)notification {
    isAppSuspended = NO;
    com_codename1_impl_ios_IOSImplementation_applicationDidBecomeActive__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationWillResignActive:(NSNotification *)notification {
    com_codename1_impl_ios_IOSImplementation_applicationWillResignActive__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationDidHide:(NSNotification *)notification {
    isAppSuspended = YES;
    com_codename1_impl_ios_IOSImplementation_applicationDidEnterBackground__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationWillUnhide:(NSNotification *)notification {
    isAppSuspended = NO;
    com_codename1_impl_ios_IOSImplementation_applicationWillEnterForeground__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationWillTerminate:(NSNotification *)notification {
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
    // Recorded as the AppArg display property, which is where every Apple port
    // puts a launch URL and where Display.getProperty("AppArg") reads it.
    for (NSURL *url in urls) {
        if (url == nil) {
            continue;
        }
        struct ThreadLocalData* threadStateData = getThreadLocalData();
        JAVA_OBJECT display = com_codename1_ui_Display_getInstance___R_com_codename1_ui_Display(threadStateData);
        JAVA_OBJECT key = fromNSString(threadStateData, @"AppArg");
        JAVA_OBJECT value = fromNSString(threadStateData,
            url.isFileURL ? url.path : [url absoluteString]);
        com_codename1_ui_Display_setProperty___java_lang_String_java_lang_String(threadStateData, display, key, value);
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
    NSDictionary *aps = [userInfo objectForKey:@"aps"];
    id alert = aps != nil ? [aps objectForKey:@"alert"] : nil;
    NSString *body = nil;
    if ([alert isKindOfClass:[NSString class]]) {
        body = (NSString *)alert;
    } else if ([alert isKindOfClass:[NSDictionary class]]) {
        body = [(NSDictionary *)alert objectForKey:@"body"];
    }
    if (body == nil) {
        return;
    }
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(threadStateData,
        fromNSString(threadStateData, body), JAVA_NULL);
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
