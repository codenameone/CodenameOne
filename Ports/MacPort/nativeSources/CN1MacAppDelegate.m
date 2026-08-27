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
#include "com_codename1_push_PushContent.h"

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
/// Whether a launch-time delivery may reach the application yet.
///
/// Deliberately not cn1MacJavaReady. That flag answers "can the framework be
/// called", and a deep link, a local notification or a push is not a call into
/// the framework -- it is a call into the APPLICATION, which at that moment has
/// only been QUEUED to initialize: IOSImplementation.callback() dispatches the
/// runnable holding init() and start() onto the event dispatch thread and
/// returns. Delivering against that flag ran URLCallback and
/// LocalNotificationCallback code before the application had built anything they
/// read. This one is set from the other side of that runnable, and it gates
/// arrivals during the interval too, so a URL that lands mid-start queues behind
/// the launch ones instead of overtaking them.
static BOOL cn1MacDeliveriesReleased = NO;
/// Whether a push has been handed over and its callback has not run yet.
///
/// Pushes are delivered one at a time, unlike URLs and local notifications,
/// because a push is two things arriving together: the message, and the
/// PushContent the native side sets immediately before it. Only the message is
/// handed straight to the application -- pushReceived() queues the callback onto
/// the event dispatch thread -- so pushing two of them back to back set
/// PushContent twice before either callback ran, and BOTH callbacks then read
/// the second push's title and body. Whoever called PushContent.get() first also
/// consumed it, leaving the other with nothing.
static BOOL cn1MacPushInFlight = NO;
static int cn1MacPendingActive = -1;   // 1 became active, 0 resigned, -1 nothing
static int cn1MacPendingHidden = -1;   // 1 hidden, 0 unhidden, -1 nothing
static NSMutableArray<NSString *> *cn1MacPendingURLs = nil;

/// Pushes and local notifications waiting their turn, in arrival order.
///
/// ONE queue for both kinds, not one each. They write the same singleton --
/// PushContent -- so ordering them against each other is the only way a delivery
/// can be sure the content it wrote is the content its own callback reads. A
/// local notification is handed over synchronously and is safe on its own, which
/// is what made it look like it needed no pacing; it is not safe next to a push
/// whose callback is still queued on the event dispatch thread, because it
/// rewrites the content that push is waiting to be given.
///
/// A notification is held as the whole delivery -- the id plus whatever the user
/// did with it -- because the action and the reply text are as much a part of
/// the callback as the id is, and re-deriving them on replay is not possible
/// once the UNNotificationResponse has gone.
static NSMutableArray<NSDictionary *> *cn1MacPendingDeliveries = nil;
static NSString * const CN1MacDeliveryKind = @"cn1Kind";
static NSString * const CN1MacDeliveryPayload = @"cn1Payload";
static NSString * const CN1MacDeliveryPush = @"push";
static NSString * const CN1MacDeliveryActionId = @"cn1ActionId";
static NSString * const CN1MacDeliveryText = @"cn1TextResponse";

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
/// One field of an APNs alert dictionary, literal or localized.
///
/// A payload may carry `body` and `title` as strings, or name entries in the
/// application's Localizable.strings through `loc-key`/`loc-args` and
/// `title-loc-key`/`title-loc-args` -- which is the standard way to send a push
/// that reads in the recipient's language. Reading only the literal keys left
/// both nil for such a payload, so a localized push with no meta was delivered
/// to the application not at all.
///
/// localizedUserNotificationStringForKey:arguments: is what resolves those
/// against the bundle, and is the same thing UNNotificationContent does before
/// showing the banner.
static NSString *cn1AlertString(NSDictionary *alert, NSString *literalKey,
                                NSString *locKey, NSString *argsKey) {
    id literal = [alert objectForKey:literalKey];
    if ([literal isKindOfClass:[NSString class]]) {
        return (NSString *)literal;
    }
    id key = [alert objectForKey:locKey];
    if (![key isKindOfClass:[NSString class]]) {
        return nil;
    }
    id args = [alert objectForKey:argsKey];
    return [NSString localizedUserNotificationStringForKey:(NSString *)key
                                                arguments:[args isKindOfClass:[NSArray class]]
                                                              ? (NSArray *)args : nil];
}

static void cn1MacDeliverPush(NSDictionary *userInfo, NSString *actionId, NSString *textResponse) {
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

    // PushContent is filled in BEFORE any callback, the way the UIKit router
    // does it. An application that reads PushContent.get() inside
    // PushCallback.push() saw exists() false, or fields left from the previous
    // push, even though the callback itself arrived.
    com_codename1_push_PushContent_reset__(threadStateData);
    // What the user did with the notification, when this push arrived because
    // they did something. Carried from the response rather than read off the
    // payload: only the delegate callback has it, and by the time the pacing
    // queue hands the push over the response is long gone.
    if (actionId != nil) {
        com_codename1_push_PushContent_setActionId___java_lang_String(
            threadStateData, fromNSString(threadStateData, actionId));
    }
    if (textResponse != nil) {
        com_codename1_push_PushContent_setTextResponse___java_lang_String(
            threadStateData, fromNSString(threadStateData, textResponse));
    }
    id mediaUrl = [userInfo objectForKey:@"media-url"];
    if ([mediaUrl isKindOfClass:[NSString class]]) {
        com_codename1_push_PushContent_setImageUrl___java_lang_String(
            threadStateData, fromNSString(threadStateData, (NSString *)mediaUrl));
    }
    if ([meta isKindOfClass:[NSString class]]) {
        com_codename1_push_PushContent_setMetaData___java_lang_String(
            threadStateData, fromNSString(threadStateData, (NSString *)meta));
    }
    id category = aps != nil ? [aps objectForKey:@"category"] : nil;
    if ([category isKindOfClass:[NSString class]]) {
        com_codename1_push_PushContent_setCategory___java_lang_String(
            threadStateData, fromNSString(threadStateData, (NSString *)category));
    }

    if ([alert isKindOfClass:[NSDictionary class]]) {
        NSDictionary *alertDict = (NSDictionary *)alert;
        NSString *title = cn1AlertString(alertDict, @"title", @"title-loc-key",
                                         @"title-loc-args");
        NSString *body = cn1AlertString(alertDict, @"body", @"loc-key", @"loc-args");
        if (title != nil && body != nil) {
            includedBody = YES;
            com_codename1_push_PushContent_setTitle___java_lang_String(
                threadStateData, fromNSString(threadStateData, title));
            com_codename1_push_PushContent_setBody___java_lang_String(
                threadStateData, fromNSString(threadStateData, body));
            // "title;body" is the type 4 wire form the Java side parses.
            NSString *combined = [NSString stringWithFormat:@"%@;%@", title, body];
            com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(
                threadStateData, fromNSString(threadStateData, combined),
                fromNSString(threadStateData, @"4"));
        } else if (body != nil) {
            // A body with no title is the common APNs dictionary form and means
            // exactly what the string form means, so it is reported as that
            // rather than skipped. Skipped, a push with no meta reached the
            // application not at all, and one with meta arrived as a data-only
            // type 2 -- the body silently gone in both.
            //
            // Type 4 stays reserved for title AND body, because that is the
            // "title;body" wire form and half of it is not that.
            includedBody = YES;
            com_codename1_push_PushContent_setBody___java_lang_String(
                threadStateData, fromNSString(threadStateData, body));
            com_codename1_impl_ios_IOSImplementation_pushReceived___java_lang_String_java_lang_String(
                threadStateData, fromNSString(threadStateData, body),
                fromNSString(threadStateData, meta != nil ? @"3" : @"1"));
        }
    } else if ([alert isKindOfClass:[NSString class]]) {
        includedBody = YES;
        com_codename1_push_PushContent_setBody___java_lang_String(
            threadStateData, fromNSString(threadStateData, (NSString *)alert));
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

/// Defined below, beside the other delivery decoders; the queue that paces both
/// kinds has to sit above them.
static void cn1MacDeliverLocalNotification(NSDictionary *delivery);

/// Hands over what is next in the queue, as far as the pacing allows.
///
/// A push costs one round trip through the event dispatch thread: delivering it
/// writes PushContent and QUEUES the callback, so asking Java for a barrier puts
/// a runnable behind that callback and the barrier calls back in here. Nothing
/// else may write PushContent until then -- not the next push, and not a local
/// notification, which would otherwise overwrite the content a queued push
/// callback is still waiting to be given.
///
/// A local notification costs nothing: it is handed over on this thread, so its
/// content has been read by the time it returns and the loop continues.
static void cn1MacPumpDeliveryQueue(void) {
    while (cn1MacDeliveriesReleased && !cn1MacPushInFlight
            && [cn1MacPendingDeliveries count] > 0) {
        NSDictionary *entry = [[cn1MacPendingDeliveries objectAtIndex:0] retain];
        [cn1MacPendingDeliveries removeObjectAtIndex:0];
        NSDictionary *payload = [entry objectForKey:CN1MacDeliveryPayload];
        if ([[entry objectForKey:CN1MacDeliveryKind] isEqualToString:CN1MacDeliveryPush]) {
            // Ends the loop: nothing else may write PushContent until the
            // barrier reports this push's callback has read it.
            cn1MacPushInFlight = YES;
            cn1MacDeliverPush(payload,
                              [entry objectForKey:CN1MacDeliveryActionId],
                              [entry objectForKey:CN1MacDeliveryText]);
            struct ThreadLocalData* threadStateData = getThreadLocalData();
            com_codename1_impl_ios_IOSImplementation_macDeliverAfterEdt__(threadStateData);
        } else {
            // Handed over on this thread, so its content is read before this
            // returns and the next delivery may go straight out.
            cn1MacDeliverLocalNotification(payload);
        }
        [entry release];
    }
}

/// Adds one delivery to the queue and pumps it. `actionId` and `textResponse`
/// are what the user did with a notification, and are nil for one that simply
/// arrived.
static void cn1MacEnqueueDelivery(NSString *kind, NSDictionary *payload,
                                  NSString *actionId, NSString *textResponse) {
    if (payload == nil) {
        return;
    }
    if (cn1MacPendingDeliveries == nil) {
        cn1MacPendingDeliveries = [[NSMutableArray alloc] init];
    }
    NSMutableDictionary *entry = [NSMutableDictionary dictionary];
    [entry setObject:kind forKey:CN1MacDeliveryKind];
    [entry setObject:payload forKey:CN1MacDeliveryPayload];
    if (actionId != nil) {
        [entry setObject:actionId forKey:CN1MacDeliveryActionId];
    }
    if (textResponse != nil) {
        [entry setObject:textResponse forKey:CN1MacDeliveryText];
    }
    [cn1MacPendingDeliveries addObject:entry];
    cn1MacPumpDeliveryQueue();
}

/// Delivers a push payload, or holds it until the Java side exists.
///
/// Queued like the lifecycle transitions and the launch URL, and for the sharper
/// version of the same reason: a notification that LAUNCHED the app arrives
/// before the Java bootstrap has run, IOSImplementation.pushCallback is still
/// null, and pushReceived() drops the payload on the floor. The application then
/// never learns about the push that started it -- the one push it most needs.
///
/// The whole payload is held, not a decoded body: the decode decides the push
/// TYPE, and doing it here would have to be repeated on replay anyway.
///
/// Shared by the remote-notification hook and by both notification-center
/// delegate methods, so none of them can be the one that forgets to wait.
static void cn1MacQueueOrDeliverPush(NSDictionary *userInfo, NSString *actionId,
                                     NSString *textResponse) {
    if (userInfo == nil) {
        return;
    }
    // Queued unconditionally, then pumped. A live push takes the same paced path
    // as a replayed one because the operating system can deliver two in quick
    // succession just as a launch can accumulate two, and the PushContent race
    // does not care which produced them.
    cn1MacEnqueueDelivery(CN1MacDeliveryPush, userInfo, actionId, textResponse);
}

/// Reports a local notification, filling in what the user did with it.
///
/// actionId and textResponse reach the application through PushContent, which
/// is what LocalNotification.addAction and addInputAction document. The UIKit
/// delegate forwards only the id and has the same gap; it is not changed here
/// because this port does not compile that file.
static void cn1MacDeliverLocalNotification(NSDictionary *delivery) {
    struct ThreadLocalData* threadStateData = getThreadLocalData();
    NSString *localId = [delivery objectForKey:@"id"];
    if (localId == nil) {
        return;
    }
    com_codename1_push_PushContent_reset__(threadStateData);
    NSString *title = [delivery objectForKey:@"title"];
    if (title != nil) {
        com_codename1_push_PushContent_setTitle___java_lang_String(
            threadStateData, fromNSString(threadStateData, title));
    }
    NSString *body = [delivery objectForKey:@"body"];
    if (body != nil) {
        com_codename1_push_PushContent_setBody___java_lang_String(
            threadStateData, fromNSString(threadStateData, body));
    }
    NSString *actionId = [delivery objectForKey:@"actionId"];
    if (actionId != nil) {
        com_codename1_push_PushContent_setActionId___java_lang_String(
            threadStateData, fromNSString(threadStateData, actionId));
    }
    NSString *textResponse = [delivery objectForKey:@"textResponse"];
    if (textResponse != nil) {
        com_codename1_push_PushContent_setTextResponse___java_lang_String(
            threadStateData, fromNSString(threadStateData, textResponse));
    }
    com_codename1_impl_ios_IOSImplementation_localNotificationReceived___java_lang_String(
        threadStateData, fromNSString(threadStateData, localId));
}

/// Reports a local notification, or holds it until the Java side exists.
///
/// The same rule pushes follow, and it has to apply here too: opening a
/// notification COLD-LAUNCHES the app, so this delegate runs before the
/// asynchronously dispatched bootstrap has installed LocalNotificationCallback.
/// localNotificationReceived() only schedules a retry when pushCallback is
/// non-null, so an application implementing only LocalNotificationCallback lost
/// the notification that launched it outright.
static void cn1MacQueueOrDeliverLocalNotification(NSDictionary *delivery) {
    if (delivery == nil) {
        return;
    }
    cn1MacEnqueueDelivery(@"local", delivery, nil, nil);
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
            // The macOS callback here too, not the iOS one. This replays a
            // deactivation that happened before Java was ready -- the user
            // switching away during a slow start -- and it deactivates the same
            // still-visible window the live path does, so it must not set the
            // minimized flag either.
            com_codename1_impl_ios_IOSImplementation_macApplicationWillResignActive__(threadStateData);
        }
        if (cn1MacPendingHidden == 1) {
            com_codename1_impl_ios_IOSImplementation_applicationDidEnterBackground__(threadStateData);
        } else if (cn1MacPendingHidden == 0) {
            com_codename1_impl_ios_IOSImplementation_applicationWillEnterForeground__(threadStateData);
        }
        cn1MacPendingActive = -1;
        cn1MacPendingHidden = -1;
        // The lifecycle state above is replayed here because it is the
        // framework's, and the framework exists now. What the APPLICATION is
        // owed -- the deep link or notification it was launched with -- is not
        // released here: init() and start() have only been queued at this point.
        // This asks the Java side to come back once they have run.
        com_codename1_impl_ios_IOSImplementation_macDeliverAfterEdt__(threadStateData);
    });
}

/// Releases the launch-time deliveries, called back from the event dispatch
/// thread once the application's start() has returned.
///
/// Back onto the main queue first, because everything these functions touch --
/// the queues, the flag, the PushContent state each delivery sets immediately
/// before itself -- lives there, and because the deliveries then reach the
/// application on exactly the thread the live path uses. Only the ordering came
/// from the EDT.
void CN1MacRunPendingDeliveries(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        // Reaching here means the event dispatch thread has drained everything
        // that was queued when the barrier went in -- including the callback for
        // the push handed over last time, if there was one.
        cn1MacPushInFlight = NO;
        if (!cn1MacDeliveriesReleased) {
            cn1MacDeliveriesReleased = YES;
            for (NSString *url in cn1MacPendingURLs) {
                cn1MacDeliverURL(url);
            }
            [cn1MacPendingURLs removeAllObjects];
        }
        cn1MacPumpDeliveryQueue();
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
/// Declared here rather than beside the other window externs below: the
/// surface-state refresh above them is its first caller.
extern BOOL CN1MacAnyWindowVisible(void);

/// The two independent reasons the main surface can be out of view, and what
/// was last reported for their combination.
///
/// Separate on purpose. Collapsing them into one flag looks equivalent and is
/// not: minimize the window, then hide the application and unhide it, and the
/// unhide reports the surface visible again while the window is still
/// miniaturized -- so painting and timers resume for something nobody can see.
/// They are ORed, and only a change in the result is reported.
static BOOL cn1MacAppHidden = NO;
static BOOL cn1MacWindowMiniaturized = NO;
static BOOL cn1MacSurfaceReportedHidden = NO;

/// Reports the combined visibility, if it changed.
static void cn1MacRefreshSurfaceHidden(void) {
    // A window still on screen keeps the application in the foreground, whatever
    // the main one is doing. Minimizing the main window while an unowned Window
    // stays visible used to report applicationDidEnterBackground, so timers
    // stopped and resources were released under a window the user was still
    // working in. Hiding the application takes every window off screen, so that
    // path reaches the same answer through this test rather than around it.
    BOOL hidden = (cn1MacAppHidden || cn1MacWindowMiniaturized)
            && !CN1MacAnyWindowVisible();
    if (cn1MacSurfaceReportedHidden == hidden) {
        return;
    }
    cn1MacSurfaceReportedHidden = hidden;
    // Set whether or not Java is up: it is C state the shared paint path reads,
    // and it has to be right from the first frame.
    isAppSuspended = hidden;
    if (!cn1MacJavaReady) {
        cn1MacPendingHidden = hidden ? 1 : 0;
        return;
    }
    if (hidden) {
        com_codename1_impl_ios_IOSImplementation_applicationDidEnterBackground__(
            CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    } else {
        com_codename1_impl_ios_IOSImplementation_applicationWillEnterForeground__(
            CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
    }
}

extern void CN1MacWindowsDeliverAppHidden(BOOL hidden);

/// Recomputes the suspended state after something changed which windows are on
/// screen.
///
/// The state depends on CN1MacAnyWindowVisible(), so every path that shows,
/// hides, minimizes, restores or destroys a window has to say so -- otherwise
/// showing a window while the main one is minimized leaves the application
/// suspended and the new window never paints, and hiding the last one leaves it
/// running with nothing on screen. Exported because those paths live in
/// CN1AppKitWindows.m; the computation itself stays private.
void CN1MacWindowVisibilityChanged(void) {
    cn1MacRefreshSurfaceHidden();
}

/// The application was hidden or unhidden -- Cmd-H, or Hide Others elsewhere.
void CN1MacDeliverAppHidden(BOOL hidden) {
    cn1MacAppHidden = hidden;
    cn1MacRefreshSurfaceHidden();
}

/// The main window was minimized or restored.
///
/// Minimizing does NOT deactivate a Mac application, so applicationDidHide:
/// never fires for it and without this half the framework kept painting and
/// running timers into a window nobody could see.
void CN1MacDeliverWindowMiniaturized(BOOL miniaturized) {
    cn1MacWindowMiniaturized = miniaturized;
    cn1MacRefreshSurfaceHidden();
}

@interface CN1MacAppDelegate : NSObject <NSApplicationDelegate, UNUserNotificationCenterDelegate>
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

- (void)applicationDidFinishLaunching:(NSNotification *)notification {
    // Installed unconditionally, and this early. A scheduled notification is
    // delivered through this delegate whether it fires while the application is
    // frontmost or the user opens it from Notification Center, so without it a
    // notification is displayed and localNotificationReceived() is never called.
    //
    // Not gated on the notifications build flag: that one is a #define inside
    // IOSNative.m and is not visible here, and installing a delegate an
    // application never triggers costs nothing.
    [UNUserNotificationCenter currentNotificationCenter].delegate = self;
}

/// A local notification that fired while the application is frontmost.
///
/// __ios_id__ is what marks one of ours; anything else is a remote payload and
/// goes to the push router. The presentation option mirrors the UIKit delegate:
/// a notification asking to be seen in the foreground is shown, and one that did
/// not is delivered silently to the application.
- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler {
    NSDictionary *info = notification.request.content.userInfo;
    id localId = [info objectForKey:@"__ios_id__"];
    if ([localId isKindOfClass:[NSString class]]) {
        NSMutableDictionary *delivery = [NSMutableDictionary dictionary];
        [delivery setObject:localId forKey:@"id"];
        if (notification.request.content.title != nil) {
            [delivery setObject:notification.request.content.title forKey:@"title"];
        }
        if (notification.request.content.body != nil) {
            [delivery setObject:notification.request.content.body forKey:@"body"];
        }
        cn1MacQueueOrDeliverLocalNotification(delivery);
        if (completionHandler != nil) {
            completionHandler([info objectForKey:@"foreground"] != nil
                ? UNNotificationPresentationOptionAlert
                : UNNotificationPresentationOptionNone);
        }
        return;
    }
    cn1MacQueueOrDeliverPush(info, nil, nil);
    if (completionHandler != nil) {
        completionHandler(UNNotificationPresentationOptionAlert);
    }
}

/// The action the user chose, or nil when they simply opened or dismissed it.
///
/// The default and dismiss identifiers mean "opened" and "swiped away", not an
/// action the application declared, so they are not reported as one --
/// getActionId() would otherwise answer a UN* constant that no
/// LocalNotification.addAction or push category ever named. The UIKit router
/// passes them through; an application asking "did the user choose an action"
/// gets the honest answer here.
static NSString *cn1MacChosenAction(UNNotificationResponse *response) {
    NSString *actionId = response.actionIdentifier;
    if (actionId == nil
            || [actionId isEqualToString:UNNotificationDefaultActionIdentifier]
            || [actionId isEqualToString:UNNotificationDismissActionIdentifier]) {
        return nil;
    }
    return actionId;
}

/// What the user typed into a text-input action, or nil for every other kind.
static NSString *cn1MacResponseText(UNNotificationResponse *response) {
    if (![response isKindOfClass:[UNTextInputNotificationResponse class]]) {
        return nil;
    }
    return ((UNTextInputNotificationResponse *)response).userText;
}

/// The user acted on a notification -- opened it, or chose one of its actions.
- (void)userNotificationCenter:(UNUserNotificationCenter *)center
didReceiveNotificationResponse:(UNNotificationResponse *)response
         withCompletionHandler:(void (^)(void))completionHandler {
    NSDictionary *info = response.notification.request.content.userInfo;
    id localId = [info objectForKey:@"__ios_id__"];
    if ([localId isKindOfClass:[NSString class]]) {
        NSMutableDictionary *delivery = [NSMutableDictionary dictionary];
        [delivery setObject:localId forKey:@"id"];
        if (response.notification.request.content.title != nil) {
            [delivery setObject:response.notification.request.content.title forKey:@"title"];
        }
        if (response.notification.request.content.body != nil) {
            [delivery setObject:response.notification.request.content.body forKey:@"body"];
        }
        NSString *actionId = cn1MacChosenAction(response);
        if (actionId != nil) {
            [delivery setObject:actionId forKey:@"actionId"];
        }
        NSString *userText = cn1MacResponseText(response);
        if (userText != nil) {
            [delivery setObject:userText forKey:@"textResponse"];
        }
        cn1MacQueueOrDeliverLocalNotification(delivery);
    } else {
        // A REMOTE notification the user acted on. The same two values as the
        // local branch above, and for the same reason: PushContent.getActionId()
        // and getTextResponse() are how a push callback learns which action was
        // chosen and what was typed into it, and dropping them here left every
        // actionable push looking like a plain open.
        cn1MacQueueOrDeliverPush(info, cn1MacChosenAction(response),
                                 cn1MacResponseText(response));
    }
    if (completionHandler != nil) {
        completionHandler();
    }
}

- (void)applicationDidBecomeActive:(NSNotification *)notification {
    // isAppSuspended is NOT cleared here. Active and visible are different
    // things on a Mac: Cmd-Tab back to an application whose window is still
    // minimized sends this and no deminiaturize, so clearing the flag resumed
    // painting and the active-state behaviour for a window nobody could see.
    // The surface tracker owns that flag, and the two events that actually
    // change visibility -- unhide and deminiaturize -- are what clear it.
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
    // The macOS callback, not the iOS one. Resigning active on a Mac means
    // another application came to the front; this one's windows are still on
    // screen and still painted. The iOS callback sets the minimized flag, which
    // parks the EDT and suppresses network error dialogs -- doing either to a
    // visible window stops its animations and swallows its errors for as long as
    // the user is in another app. The surface tracker above owns that flag, and
    // hiding and miniaturizing are what set it.
    com_codename1_impl_ios_IOSImplementation_macApplicationWillResignActive__(CN1_THREAD_GET_STATE_PASS_SINGLE_ARG);
}

- (void)applicationWillHide:(NSNotification *)notification {
    // The secondary windows are swept HERE rather than in applicationDidHide:,
    // because the sweep records which windows were actually on screen and by
    // the time the application is hidden every one of them reports NO. The
    // surface flag below does not care about the ordering, so it stays where it
    // was.
    CN1MacWindowsDeliverAppHidden(YES);
}

- (void)applicationDidHide:(NSNotification *)notification {
    CN1MacDeliverAppHidden(YES);
}

- (void)applicationWillUnhide:(NSNotification *)notification {
    CN1MacDeliverAppHidden(NO);
    // Only the windows the hide actually took away, which is what the flag set
    // in applicationWillHide: records.
    CN1MacWindowsDeliverAppHidden(NO);
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
        // Every URL, not just the first. Finder hands over the whole selection
        // in one call -- opening five files, or dropping them on the Dock icon,
        // is one openURLs: with five entries -- and breaking after the first
        // discarded the rest with nothing left to recover them from.
        //
        // A launch URL arrives before the Java side exists, which is the whole
        // point of a launch URL, so those are held rather than dropped. The
        // readiness test stays inside the loop: the queue is drained on the main
        // thread, so it cannot flip part way through this one, and keeping it
        // here means each URL takes whichever path is correct for it.
        if (!cn1MacDeliveriesReleased) {
            if (cn1MacPendingURLs == nil) {
                cn1MacPendingURLs = [[NSMutableArray alloc] init];
            }
            [cn1MacPendingURLs addObject:spec];
            continue;
        }
        cn1MacDeliverURL(spec);
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
    cn1MacQueueOrDeliverPush(userInfo, nil, nil);
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
