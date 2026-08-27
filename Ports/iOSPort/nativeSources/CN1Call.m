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

#import "CodenameOne_GLViewController.h"
#import "CN1Call.h"

#ifdef CN1_INCLUDE_CALL

#include "com_codename1_impl_ios_IOSCallCallbacks.h"
#import "java_lang_String.h"

#if __has_include(<CallKit/CallKit.h>)
#import <CallKit/CallKit.h>
#import <AVFoundation/AVFoundation.h>
#define CN1_CALL_HAS_CALLKIT 1
#endif

#if defined(CN1_CALL_VOIP) && __has_include(<PushKit/PushKit.h>)
#import <PushKit/PushKit.h>
#define CN1_CALL_HAS_PUSHKIT 1
#endif

// ---------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------

// Declared per translation unit, as CN1Nearby.m and CN1Bluetooth.m do: these
// live in IOSNative.m and no shared header exports them, so a file that uses
// one without saying so compiles with an implicit declaration and then reads
// its result out of the wrong register.
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString *str);
extern NSString *toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

static JAVA_OBJECT cn1clJString(NSString *s) {
    return s == nil ? JAVA_NULL : fromNSString(getThreadLocalData(), s);
}

/// Replaces the characters a tab-delimited record cannot carry, exactly as
/// CallWire.sanitize does on the Java side. A caller whose display name
/// contains a tab would otherwise shift every field after it -- and a display
/// name is attacker-influenced on any app that shows a remote party's chosen
/// name.
static NSString *cn1clSanitize(NSString *s) {
    if (s == nil) {
        return @"";
    }
    NSString *out = [s stringByReplacingOccurrencesOfString:@"\t" withString:@" "];
    out = [out stringByReplacingOccurrencesOfString:@"\n" withString:@" "];
    return [out stringByReplacingOccurrencesOfString:@"\r" withString:@" "];
}

static NSString *cn1clJoin(NSArray *fields) {
    NSMutableArray *safe = [NSMutableArray arrayWithCapacity:[fields count]];
    for (NSString *f in fields) {
        [safe addObject:cn1clSanitize(f)];
    }
    return [safe componentsJoinedByString:@"\t"];
}

static NSArray *cn1clSplit(NSString *record) {
    if (record == nil) {
        return [NSArray array];
    }
    return [record componentsSeparatedByString:@"\t"];
}

static NSString *cn1clField(NSArray *fields, NSUInteger index) {
    if (fields == nil || index >= [fields count]) {
        return @"";
    }
    return [fields objectAtIndex:index];
}

static void cn1clAck(int requestId, BOOL ok, int error, NSString *message) {
    com_codename1_impl_ios_IOSCallCallbacks_ack___int_boolean_int_java_lang_String(
            getThreadLocalData(), requestId, ok ? JAVA_TRUE : JAVA_FALSE,
            error, cn1clJString(message));
}

/// The value of an Info.plist key, or nil.
static id cn1clPlist(NSString *key) {
    return [[NSBundle mainBundle] objectForInfoDictionaryKey:key];
}

static NSString *cn1clPlistString(NSString *key, NSString *fallback) {
    id v = cn1clPlist(key);
    if ([v isKindOfClass:[NSString class]] && [(NSString *)v length] > 0) {
        return (NSString *)v;
    }
    return fallback;
}

static BOOL cn1clPlistBool(NSString *key, BOOL fallback) {
    id v = cn1clPlist(key);
    if ([v isKindOfClass:[NSString class]]) {
        return [(NSString *)v boolValue];
    }
    if ([v isKindOfClass:[NSNumber class]]) {
        return [(NSNumber *)v boolValue];
    }
    return fallback;
}

#ifdef CN1_CALL_HAS_CALLKIT

// ---------------------------------------------------------------------
// state
// ---------------------------------------------------------------------

static CXProvider *cn1clProvider = nil;
static CXCallController *cn1clController = nil;

/// Calls this app currently has, keyed by canonical id string.
static NSMutableDictionary *cn1clCalls = nil;
/// The actions this app submitted itself, as "<uuid>|<CXAction class>",
/// counted so that two overlapping requests of the same kind for one call
/// are both recognised as this app's.
///
/// Every transaction an app requests comes back through the provider
/// delegate, which is the same door the system uses -- so end(), setHeld()
/// and setMuted() were delivered to the app as endRequested, holdRequested
/// and muteRequested, callbacks documented for the SYSTEM asking. An app
/// that honours those contracts signalled the remote end a second time for a
/// hang-up it had just performed itself.
///
/// A submitted action is claimed here and fulfilled natively instead. The
/// caller already has its own AsyncResource for the outcome, and the session
/// state moves on that acknowledgement.
static NSCountedSet *cn1clJavaStarts = nil;

/// The calls whose CXStartCallAction the SYSTEM submitted -- Recents, Siri --
/// and that Java has not adopted yet.
///
/// startCallRequested tells the app to report the call with that id, and
/// reportOutgoing() answered by submitting a SECOND start action for a uuid
/// whose first one was still pending. CallKit can refuse that transaction,
/// which failed reportOutgoing() and took the Java session with it while the
/// original action went on to be fulfilled.
static NSMutableSet *cn1clSystemStarts = nil;

/// Every registration waiting for the first credentials callback.
///
/// A scalar slot lost all but the last: two register() calls before PushKit
/// answered left the first AsyncResource pending for ever and retained in
/// CallRequests. Guarded by cn1clLock like the rest of the state.
static NSMutableArray *cn1clTokenRequests = nil;

/// The call whose audio session is being torn down.
///
/// didDeactivateAudioSession arrives AFTER the call is gone, so an owner
/// dropped when the end action was fulfilled left that callback with nobody
/// to report to -- and an app waiting for audioSessionDeactivated to stop its
/// media and release the microphone never heard it. With another call still
/// up it was worse: the deactivation was attributed to the survivor.
static NSString *cn1clAudioRetiring = nil;

/// The call CallKit last put in charge of the audio session.
///
/// didActivateAudioSession names no call, and with more than one call up --
/// a ringing one, a held one, and the active one -- picking allKeys.first
/// started media for whichever call the dictionary happened to hash first.
/// Audio follows the answer, the outgoing start, and the resume, so those
/// three set it and an ending call clears it.
static NSString *cn1clAudioCall = nil;

/// CXActions delivered to Java and not yet answered, keyed by token.
static NSMutableDictionary *cn1clActions = nil;
static int64_t cn1clNextActionToken = 1;

/// Calls reported to CallKit that Java has not yet seen.
static NSMutableArray *cn1clPending = nil;

/// Pushed calls the application has not yet taken responsibility for.
///
/// The TTL watchdog exists to end a call the app never learned about, so
/// "still ringing" is the wrong test -- an ANSWERED call is still in
/// cn1clCalls, and testing only that ended live calls as Unanswered once the
/// TTL elapsed. A call leaves this set when Java is told about it or when the
/// user acts on it through the system UI; either means somebody owns it now.
static NSMutableSet *cn1clUnclaimed = nil;

/// Calls reported to CallKit whose completion has not come back yet.
/// Bumped by every provider reset, so a report that was in flight across one
/// can tell that the tables it is about to touch are no longer its own.
/// Guarded by cn1clLock.
static int cn1clProviderGeneration = 0;

static NSMutableSet *cn1clReporting = nil;

/// Java requests waiting on somebody else's in-flight report, by uuid.
static NSMutableDictionary *cn1clReportWaiters = nil;

/// Whether application code has installed a VoIP listener.
static BOOL cn1clJavaReady = NO;

/// CXActions that arrived before the app had a listener.
///
/// A pushed call rings before any of this app's code has run, and the user
/// can answer it there and then. Delivered into a facade with no listeners,
/// the action was auto-fulfilled by the facade's own safety net and
/// cn1clTrackAction had already claimed the call -- so the watchdog left it
/// alone, the drain handed Java a session still marked RINGING, and
/// answerRequested was never delivered at all: the call was connected on
/// screen and the app never started its media.
///
/// Held here instead and replayed once Java says it is listening, which is
/// the same handshake the pending-call drain uses.
static NSMutableArray *cn1clQueuedActions = nil;

static int cn1clRoute = CN1_CALL_ROUTE_EARPIECE;
static BOOL cn1clConfigured = NO;
static NSString *cn1clDirectoryPath = nil;

static NSObject *cn1clLock = nil;

static void cn1clEnsureState(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1clCalls = [[NSMutableDictionary alloc] init];
        cn1clActions = [[NSMutableDictionary alloc] init];
        cn1clPending = [[NSMutableArray alloc] init];
        cn1clUnclaimed = [[NSMutableSet alloc] init];
        cn1clReporting = [[NSMutableSet alloc] init];
        cn1clReportWaiters = [[NSMutableDictionary alloc] init];
        cn1clJavaStarts = [[NSCountedSet alloc] init];
        cn1clSystemStarts = [[NSMutableSet alloc] init];
        cn1clTokenRequests = [[NSMutableArray alloc] init];
        cn1clQueuedActions = [[NSMutableArray alloc] init];
        cn1clLock = [[NSObject alloc] init];
    });
}

// ---------------------------------------------------------------------
// the provider delegate
// ---------------------------------------------------------------------

/// Marks a pushed call as owned, so the TTL watchdog leaves it alone.
static void cn1clClaim(NSString *uuidString) {
    if (uuidString == nil) {
        return;
    }
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        [cn1clUnclaimed removeObject:uuidString];
    }
}

/// Forgets the audio owner when its call is gone. Caller holds cn1clLock.
static void cn1clDropAudioLocked(NSString *uuidString) {
    if (cn1clAudioCall != nil && uuidString != nil
            && [cn1clAudioCall isEqualToString:uuidString]) {
        // Retired rather than forgotten: CallKit deactivates the session
        // after the call is gone, and that callback still has to name it.
        // The previous retiring owner goes first: this slot holds the +1
        // that cn1clOwnAudio made, and overwriting it dropped one string per
        // call that ended while another was already retiring.
        [cn1clAudioRetiring release];
        cn1clAudioRetiring = cn1clAudioCall;
        cn1clAudioCall = nil;
    }
}

/// Holds an action until Java is listening, answering whether it did.
///
/// The block is what will run at replay time; nothing is fulfilled or failed
/// here, because CallKit's own timeout is the only honest deadline and the
/// app is expected to be listening within a fraction of it.
static BOOL cn1clHoldUntilReady(CXAction *action, void (^deliver)(void)) {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        if (cn1clJavaReady) {
            return NO;
        }
        // Keyed by the action so the timeout below can withdraw it. CallKit
        // gives an action a few seconds and then rejects it; a queue that
        // only ever grows replayed the answer, hang-up or hold minutes later,
        // when a listener finally arrived -- acting on user intent the system
        // had already discarded, and moving Java's session to match.
        [cn1clQueuedActions addObject:@{
            @"action": action == nil ? [NSNull null] : action,
            @"deliver": [[deliver copy] autorelease]
        }];
        return YES;
    }
}

/// Drops the tracked copy of an action the system has given up on, so a
/// later completion finds nothing to answer.
static void cn1clForgetTrackedAction(CXAction *action) {
    if (action == nil) {
        return;
    }
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        NSArray *keys = [cn1clActions allKeys];
        for (NSNumber *key in keys) {
            if ([cn1clActions objectForKey:key] == action) {
                [cn1clActions removeObjectForKey:key];
                return;
            }
        }
    }
}

/// Drops a held action the system has given up on.
static void cn1clWithdrawHeldAction(CXAction *action) {
    if (action == nil) {
        return;
    }
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        NSMutableArray *keep = [NSMutableArray array];
        for (NSDictionary *entry in cn1clQueuedActions) {
            id held = [entry objectForKey:@"action"];
            if (held != action) {
                [keep addObject:entry];
            }
        }
        [cn1clQueuedActions setArray:keep];
    }
}

/// Replays everything held while the app was not listening.
static void cn1clReplayQueuedActions(void) {
    NSArray *held = nil;
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        // Readiness RE-READ here, under the same lock that guards the queue.
        // setJavaReady replays through a dispatch_async, so the app can drop
        // its last listener between the flag going true and this block
        // running. Draining then delivered every held action into a facade
        // with nobody listening, which auto-fulfills them -- so CallKit was
        // told an answer, a hang-up or a hold had been carried out while no
        // app code ever signalled it. Left queued instead, for whenever a
        // listener comes back.
        if (!cn1clJavaReady) {
            return;
        }
        held = [NSArray arrayWithArray:cn1clQueuedActions];
        [cn1clQueuedActions removeAllObjects];
    }
    for (NSDictionary *entry in held) {
        void (^deliver)(void) = [entry objectForKey:@"deliver"];
        if (deliver != nil) {
            deliver();
        }
    }
}

/// The claim key for an action this app submitted.
static NSString *cn1clOwnKey(NSString *uuidString, Class kind) {
    return [NSString stringWithFormat:@"%@|%@", uuidString,
            NSStringFromClass(kind)];
}

/// Remembers that this app submitted an action, before the transaction is
/// requested: CallKit can dispatch it before the request call returns.
static void cn1clClaimOwn(NSString *uuidString, Class kind) {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        [cn1clJavaStarts addObject:cn1clOwnKey(uuidString, kind)];
    }
}

/// Whether the delegate is looking at an action this app submitted, clearing
/// the claim as it answers.
static BOOL cn1clTakeOwn(CXCallAction *action) {
    cn1clEnsureState();
    NSString *key = cn1clOwnKey([action.callUUID UUIDString],
            [action class]);
    @synchronized (cn1clLock) {
        // COUNTED, not a set. Two setHeld() or two setMuted() calls for one
        // call produce the same key, and a plain set kept one entry -- so the
        // first callback consumed it and the second was misread as the system
        // asking, which handed the app a hold or mute request it had made
        // itself and had it signal the change twice.
        if ([cn1clJavaStarts countForObject:key] == 0) {
            return NO;
        }
        [cn1clJavaStarts removeObject:key];
        return YES;
    }
}

/// Drops a claim whose transaction never reached the delegate.
static void cn1clReleaseOwn(NSString *uuidString, Class kind) {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        [cn1clJavaStarts removeObject:cn1clOwnKey(uuidString, kind)];
    }
}

/// The route iOS is actually using; defined with the other audio natives.
static int cn1clCurrentRoute(void);

/// Records the call the audio session is about to belong to.
static void cn1clOwnAudio(NSString *uuidString) {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        // Balanced: the copy is +1 and this slot owns it, so replacing it
        // without releasing leaked one string per call for the life of the
        // process.
        [cn1clAudioCall release];
        cn1clAudioCall = [uuidString copy];
    }
}

/// The call the audio session belongs to.
///
/// Falls back to the only live call, which is both the common case and the
/// one where the fallback cannot be wrong; with none it answers nil and Java
/// ignores the callback rather than starting media for a guess.
static NSString *cn1clAudioOwner(void) {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        if (cn1clAudioCall != nil && [cn1clCalls objectForKey:cn1clAudioCall] != nil) {
            // Retained and autoreleased because it ESCAPES the lock: the
            // caller reads it after this returns, and the slot can be
            // released by an end arriving on another thread in between.
            // Handing out the raw pointer is what made releasing these
            // slots unsafe before.
            return [[cn1clAudioCall retain] autorelease];
        }
        if ([cn1clCalls count] == 1) {
            return [[cn1clCalls allKeys] firstObject];
        }
        return nil;
    }
}

/// The call a deactivation belongs to: the retiring one if there is one.
///
/// Deactivation follows the END of a call, so the ordinary owner lookup --
/// which requires a live call -- is the wrong question here.
static NSString *cn1clAudioDeactivating(void) {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        if (cn1clAudioRetiring != nil) {
            // The slot's +1 goes into the pool rather than to the caller,
            // which neither expects nor discharges ownership.
            NSString *retiring = [cn1clAudioRetiring autorelease];
            cn1clAudioRetiring = nil;
            return retiring;
        }
    }
    return cn1clAudioOwner();
}

/// Allocates a token for a CXAction and remembers it.
///
/// Java answers with completeAction; until it does, the action is neither
/// fulfilled nor failed. Both are a few seconds from timing out, which is why
/// the Java facade answers automatically for a listener that ignores one.
static int64_t cn1clTrackAction(CXAction *action) {
    cn1clEnsureState();
    if ([action isKindOfClass:[CXCallAction class]]) {
        // The user acted on it, so it is owned whatever the app does next.
        cn1clClaim([[(CXCallAction *)action callUUID] UUIDString]);
    }
    @synchronized (cn1clLock) {
        int64_t token = cn1clNextActionToken++;
        [cn1clActions setObject:action forKey:[NSNumber numberWithLongLong:token]];
        return token;
    }
}

@interface CN1CallProviderDelegate : NSObject <CXProviderDelegate>
@end

@implementation CN1CallProviderDelegate

- (void)providerDidReset:(CXProvider *)provider {
    // Every call is gone. Java is told so it can drop media WITHOUT ending
    // calls that no longer exist.
    cn1clEnsureState();
    NSMutableArray *abandoned = [NSMutableArray array];
    @synchronized (cn1clLock) {
        [cn1clCalls removeAllObjects];
        [cn1clActions removeAllObjects];
        [cn1clJavaStarts removeAllObjects];
        [cn1clSystemStarts removeAllObjects];
        // Held actions go too. They name calls this reset has just destroyed,
        // so replaying them after the drain would deliver answerRequested for
        // a session that no longer exists -- and a stale system start would
        // have the app place a call the user asked for before the reset.
        [cn1clQueuedActions removeAllObjects];
        [cn1clAudioCall release];
        cn1clAudioCall = nil;
        [cn1clAudioRetiring release];
        cn1clAudioRetiring = nil;
        // Every report in flight belongs to the provider that has just gone,
        // so their completions must not touch what comes after them.
        cn1clProviderGeneration++;
        [cn1clReporting removeAllObjects];
        for (NSArray *perCall in [cn1clReportWaiters allValues]) {
            [abandoned addObjectsFromArray:perCall];
        }
        [cn1clReportWaiters removeAllObjects];
        // cn1clPending is deliberately NOT cleared here. A queued pushed call
        // the reset destroyed still has to reach the app, as a MISSED call --
        // dropping the record would leave the user with no trace of a call
        // that rang. The drain already downgrades it: a record queued
        // non-stale is delivered stale unless its uuid is still in
        // cn1clCalls, and that table has just been emptied above. Clearing
        // the queue here would lose the notification, not protect anything.
    }
    // Answered rather than dropped: their report is being abandoned and no
    // completion will reach them now that the generation has moved on.
    for (NSNumber *waiter in abandoned) {
        cn1clAck([waiter intValue], NO, CN1_CALL_ERR_CALL_REFUSED,
                @"The call provider reset while the call was being reported");
    }
    com_codename1_impl_ios_IOSCallCallbacks_providerReset__(getThreadLocalData());
}

- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action {
    cn1clOwnAudio([action.callUUID UUIDString]);
    // CLAIMED here, not only when the delivery runs. cn1clTrackAction is what
    // takes a pushed call off the unclaimed list, and a held action does not
    // reach it -- so a user who answered before the app was listening had the
    // TTL watchdog end their call as unanswered while the answer sat in the
    // queue. The user acting on it is the claim; when Java hears about it is
    // not.
    //
    // ANSWER ONLY, deliberately. End must not claim: if Java never arrives,
    // the watchdog ending the call is exactly what the user asked for, and
    // claiming would leave it on screen for ever instead. Hold, mute and DTMF
    // need no claim of their own -- they are only reachable on a call that
    // was already answered, and the answer claimed it.
    cn1clClaim([action.callUUID UUIDString]);
    // Held when the app has no listener yet: a pushed call rings before any
    // of its code runs, and the user can answer it there and then.
    if (cn1clHoldUntilReady(action, ^{
        com_codename1_impl_ios_IOSCallCallbacks_answerRequested___java_lang_String_long(
                getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
                cn1clTrackAction(action));
    })) {
        return;
    }
    com_codename1_impl_ios_IOSCallCallbacks_answerRequested___java_lang_String_long(
            getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
            cn1clTrackAction(action));
}

- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action {
    if (cn1clTakeOwn(action)) {
        // end() submitted this. The call's bookkeeping is dropped by that
        // request's own completion block, and the app is already ending the
        // call -- delivering endRequested would have it signal the remote
        // end a second time for its own hang-up.
        [action fulfill];
        return;
    }
    // The call is NOT forgotten here. A listener that fails this action --
    // with or without defer() -- is saying it could not end the call, and
    // CallKit then restores it; forgetting the uuid on delivery meant a later
    // CallSession.end() answered INVALID_ID for a call the system was still
    // showing, and availability checks misread it as somebody else's.
    // cn1clCompleteAction drops it once the action is fulfilled.
    if (cn1clHoldUntilReady(action, ^{
        com_codename1_impl_ios_IOSCallCallbacks_endRequested___java_lang_String_long(
                getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
                cn1clTrackAction(action));
    })) {
        return;
    }
    com_codename1_impl_ios_IOSCallCallbacks_endRequested___java_lang_String_long(
            getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
            cn1clTrackAction(action));
}

- (void)provider:(CXProvider *)provider performSetHeldCallAction:(CXSetHeldCallAction *)action {
    if (cn1clTakeOwn(action)) {
        // setHeld() submitted this; the session moves on its own
        // acknowledgement, and holdRequested is for the system asking.
        if (!action.onHold) {
            cn1clOwnAudio([action.callUUID UUIDString]);
        }
        [action fulfill];
        return;
    }
    if (!action.onHold) {
        cn1clOwnAudio([action.callUUID UUIDString]);
    }
    if (cn1clHoldUntilReady(action, ^{
        com_codename1_impl_ios_IOSCallCallbacks_holdRequested___java_lang_String_boolean_long(
                getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
                action.onHold ? JAVA_TRUE : JAVA_FALSE, cn1clTrackAction(action));
    })) {
        return;
    }
    com_codename1_impl_ios_IOSCallCallbacks_holdRequested___java_lang_String_boolean_long(
            getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
            action.onHold ? JAVA_TRUE : JAVA_FALSE, cn1clTrackAction(action));
}

- (void)provider:(CXProvider *)provider performSetMutedCallAction:(CXSetMutedCallAction *)action {
    if (cn1clTakeOwn(action)) {
        // setMuted() submitted this; see performSetHeldCallAction.
        [action fulfill];
        return;
    }
    if (cn1clHoldUntilReady(action, ^{
        com_codename1_impl_ios_IOSCallCallbacks_muteRequested___java_lang_String_boolean_long(
                getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
                action.muted ? JAVA_TRUE : JAVA_FALSE, cn1clTrackAction(action));
    })) {
        return;
    }
    com_codename1_impl_ios_IOSCallCallbacks_muteRequested___java_lang_String_boolean_long(
            getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
            action.muted ? JAVA_TRUE : JAVA_FALSE, cn1clTrackAction(action));
}

- (void)provider:(CXProvider *)provider performPlayDTMFCallAction:(CXPlayDTMFCallAction *)action {
    if (cn1clHoldUntilReady(action, ^{
        com_codename1_impl_ios_IOSCallCallbacks_dtmfRequested___java_lang_String_java_lang_String_long(
                getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
                cn1clJString(action.digits), cn1clTrackAction(action));
    })) {
        return;
    }
    com_codename1_impl_ios_IOSCallCallbacks_dtmfRequested___java_lang_String_java_lang_String_long(
            getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
            cn1clJString(action.digits), cn1clTrackAction(action));
}

- (void)provider:(CXProvider *)provider timedOutPerformingAction:(CXAction *)action {
    // CallKit has given up on this action, so whatever the user asked for is
    // no longer something the system will carry out. A held copy replayed
    // afterwards would act on intent that has expired -- answering a call the
    // system has already treated as unanswered, or hanging up one the user
    // has since been reconnected to -- and would move Java's session to match
    // a state CallKit is not in.
    //
    // Nothing is fulfilled or failed here: the action is already over as far
    // as the system is concerned, and cn1clCompleteAction ignores a token it
    // no longer holds.
    cn1clWithdrawHeldAction(action);
    // And the TRACKED copy, which is the other place an expired action can
    // hide. Java being ready is not the same as Java being prompt: with a
    // blocked EDT the action is tracked and its event queued rather than
    // held, so withdrawing only from the held queue left the token in
    // cn1clActions -- and when the EDT resumed, cn1clCompleteAction fetched
    // a CXAction CallKit had already timed out and answered it.
    cn1clForgetTrackedAction(action);
    if ([action isKindOfClass:[CXStartCallAction class]]) {
        // And the ADOPTION marker, which outlives both queues. Only
        // callCompleteAction cleared it, so a start the app was still
        // deferring stayed adoptable: when the EDT resumed, reportOutgoing()
        // matched the uuid, adopted an action CallKit had already abandoned
        // and answered the application with success for a call the system
        // call UI was never going to show.
        NSString *startUuid = [[(CXCallAction *)action callUUID] UUIDString];
        @synchronized (cn1clLock) {
            [cn1clSystemStarts removeObject:startUuid];
        }
    }
}

- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action {
    // The system asking this app to PLACE a call: Recents, or a voice
    // assistant. No call exists yet; Java reports one with this id.
    NSString *startedUuid = [action.callUUID UUIDString];
    cn1clOwnAudio(startedUuid);
    cn1clEnsureState();
    if (cn1clTakeOwn(action)) {
        // reportOutgoing() submitted this one. The action still has to be
        // fulfilled or CallKit times it out, but Java already knows about
        // the call: telling it to place one would have it call
        // reportOutgoing() again for a call it is already placing.
        [action fulfill];
        return;
    }
    // The system asked. Remembered so the reportOutgoing() this callback is
    // about to provoke ADOPTS this action instead of submitting a second one
    // for the same uuid.
    @synchronized (cn1clLock) {
        [cn1clSystemStarts addObject:startedUuid];
    }
    // Held like the other five while the app has no listener. A start that
    // reached an empty facade was auto-fulfilled, so the call Recents or Siri
    // asked for was never placed and no session was ever created.
    if (cn1clHoldUntilReady(action, ^{
        [self provider:provider performStartCallAction:action];
    })) {
        return;
    }
    NSString *wire = cn1clJoin([NSArray arrayWithObjects:
            [NSString stringWithFormat:@"%d",
                    action.handle.type == CXHandleTypePhoneNumber
                            ? CN1_CALL_HANDLE_PHONE
                            : (action.handle.type == CXHandleTypeEmailAddress
                                    ? CN1_CALL_HANDLE_EMAIL
                                    : CN1_CALL_HANDLE_GENERIC)],
            action.handle.value == nil ? @"" : action.handle.value, nil]);
    com_codename1_impl_ios_IOSCallCallbacks_startCallRequested___java_lang_String_java_lang_String_boolean_long(
            getThreadLocalData(), cn1clJString([action.callUUID UUIDString]),
            cn1clJString(wire), action.video ? JAVA_TRUE : JAVA_FALSE,
            cn1clTrackAction(action));
}

- (void)provider:(CXProvider *)provider didActivateAudioSession:(AVAudioSession *)audioSession {
    // THE moment media may start. Reported for whichever call is current;
    // the Java side carries the id so an app with one call needs no
    // bookkeeping.
    NSString *uuid = cn1clAudioOwner();
    if (uuid == nil) {
        return;
    }
    // The route iOS ACTUALLY chose, not the cached one. cn1clRoute starts at
    // earpiece and only an app-initiated setter moves it, so a call activated
    // on a connected headset or Bluetooth device was announced to the app as
    // earpiece.
    cn1clRoute = cn1clCurrentRoute();
    com_codename1_impl_ios_IOSCallCallbacks_audioActivated___java_lang_String_int(
            getThreadLocalData(), cn1clJString(uuid), cn1clRoute);
}

- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession {
    NSString *uuid = cn1clAudioDeactivating();
    if (uuid == nil) {
        return;
    }
    com_codename1_impl_ios_IOSCallCallbacks_audioDeactivated___java_lang_String(
            getThreadLocalData(), cn1clJString(uuid));
}

@end

static CN1CallProviderDelegate *cn1clDelegate = nil;

/// Builds the provider configuration from Info.plist ALONE.
///
/// Deliberately not from anything Java set: on a cold start a VoIP push is
/// reported before application code has run, so the name the user sees has to
/// come from the bundle. IPhoneBuilder writes these keys from the ios.call.*
/// build hints.
/// The capacity the provider is CONFIGURED with, mirrored here because
/// CXProviderConfiguration does not hand it back and availability has to
/// compare against it. Guarded by cn1clLock.
static int cn1clMaxGroups = 1;
static int cn1clMaxPerGroup = 1;

/// How many live calls this app may hold at once, under the current
/// configuration.
static int cn1clCapacity(void) {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        // The GROUP limit, not groups times calls-per-group. Every call this
        // port reports carries supportsGrouping = NO, and CallSession's
        // groupWith answers NOT_SUPPORTED on every port, so each call is its
        // own single-call group and maximumCallsPerCallGroup is never the
        // binding constraint. Multiplying them said there was room for a
        // second call under maximumCallGroups(1).maximumCallsPerGroup(2),
        // and CallKit then refused the report for want of a second GROUP.
        // If grouping is ever implemented this becomes a product again --
        // and cn1clMaxPerGroup is mirrored ready for that.
        return cn1clMaxGroups > 0 ? cn1clMaxGroups : 1;
    }
}

static CXProviderConfiguration *cn1clConfiguration(void) {
    NSString *name = cn1clPlistString(@"CN1CallProviderName",
            cn1clPlistString(@"CFBundleDisplayName",
                    cn1clPlistString(@"CFBundleName", @"Codename One")));
    CXProviderConfiguration *cfg;
#if defined(__IPHONE_14_0)
    if (@available(iOS 14.0, *)) {
        cfg = [[[CXProviderConfiguration alloc] init] autorelease];
    } else {
        cfg = [[[CXProviderConfiguration alloc] initWithLocalizedName:name] autorelease];
    }
#else
    cfg = [[[CXProviderConfiguration alloc] initWithLocalizedName:name] autorelease];
#endif
    cfg.supportsVideo = cn1clPlistBool(@"CN1CallSupportsVideo", NO);
    cfg.includesCallsInRecents = cn1clPlistBool(@"CN1CallIncludesCallsInRecents", YES);
    cfg.maximumCallGroups = 1;
    cfg.maximumCallsPerCallGroup = 1;
    @synchronized (cn1clLock) {
        cn1clMaxGroups = 1;
        cn1clMaxPerGroup = 1;
    }
    cfg.supportedHandleTypes = [NSSet setWithObjects:
            [NSNumber numberWithInteger:CXHandleTypeGeneric],
            [NSNumber numberWithInteger:CXHandleTypePhoneNumber],
            [NSNumber numberWithInteger:CXHandleTypeEmailAddress], nil];
    NSString *ringtone = cn1clPlistString(@"CN1CallRingtoneSound", nil);
    if (ringtone != nil) {
        cfg.ringtoneSound = ringtone;
    }
    NSString *icon = cn1clPlistString(@"CN1CallIconTemplateImageName", nil);
    if (icon != nil) {
        UIImage *img = [UIImage imageNamed:icon];
        if (img != nil) {
            cfg.iconTemplateImageData = UIImagePNGRepresentation(img);
        }
    }
    return cfg;
}

/// The provider, created on first use and never later than the push registry.
static CXProvider *cn1clEnsureProvider(void) {
    cn1clEnsureState();
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1clProvider = [[CXProvider alloc] initWithConfiguration:cn1clConfiguration()];
        cn1clDelegate = [[CN1CallProviderDelegate alloc] init];
        [cn1clProvider setDelegate:cn1clDelegate queue:nil];
        cn1clController = [[CXCallController alloc] init];
    });
    return cn1clProvider;
}

// ---------------------------------------------------------------------
// reporting
// ---------------------------------------------------------------------

static CXHandle *cn1clHandleFromWire(NSString *wire) {
    NSArray *f = cn1clSplit(wire);
    NSString *value = cn1clField(f, 1);
    if ([value length] == 0) {
        value = @" ";
    }
    int type = [cn1clField(f, 0) intValue];
    CXHandleType t = CXHandleTypeGeneric;
    if (type == CN1_CALL_HANDLE_PHONE) {
        t = CXHandleTypePhoneNumber;
    } else if (type == CN1_CALL_HANDLE_EMAIL) {
        t = CXHandleTypeEmailAddress;
    }
    return [[[CXHandle alloc] initWithType:t value:value] autorelease];
}

/// The single funnel through which a call is reported to CallKit.
///
/// Both the push path and the Java path come through here, which is what
/// keeps a socket and a push racing for the same call from reporting the uuid
/// twice: CallKit answers a duplicate with
/// CXErrorCodeIncomingCallErrorCallUUIDAlreadyExists and THROWS, so a
/// duplicate is downgraded to an update instead.
///
/// requestId < 0 means the push path, which has nobody waiting on an answer.
static void cn1clReportIncoming(int requestId, NSString *uuidString,
        NSString *handleWire, NSString *displayName, BOOL hasVideo) {
    cn1clEnsureState();
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:uuidString] autorelease];
    if (uuid == nil) {
        if (requestId >= 0) {
            cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID,
                    @"Not a canonical call id");
        }
        return;
    }
    CXCallUpdate *update = [[[CXCallUpdate alloc] init] autorelease];
    update.remoteHandle = cn1clHandleFromWire(handleWire);
    if ([displayName length] > 0) {
        update.localizedCallerName = displayName;
    }
    update.hasVideo = hasVideo;
    update.supportsHolding = YES;
    update.supportsDTMF = YES;
    update.supportsGrouping = NO;
    update.supportsUngrouping = NO;

    // ONE critical section for the test, the waiter registration AND the
    // reservation. Split, a socket and a push reporting the same uuid could
    // both read known == NO and both call reportNewIncomingCallWithUUID:
    // whichever completion got CallKit's duplicate error then dropped the
    // shared record, so the other report succeeded while every later end or
    // update on it answered INVALID_ID.
    BOOL known = NO;
    BOOL stillPending = NO;
    int reportGeneration = 0;
    @synchronized (cn1clLock) {
        known = [cn1clCalls objectForKey:uuidString] != nil;
        stillPending = [cn1clReporting containsObject:uuidString];
        if (known) {
            if (requestId >= 0 && stillPending) {
                // The first report has not heard back from CallKit yet, so
                // acknowledging this one as accepted would be a guess -- and
                // if the original is then filtered or refused, its completion
                // drops the uuid while Java holds a session with no system
                // call behind it. Wait and share the original's answer.
                NSMutableArray *waiters =
                        [cn1clReportWaiters objectForKey:uuidString];
                if (waiters == nil) {
                    waiters = [NSMutableArray array];
                    [cn1clReportWaiters setObject:waiters forKey:uuidString];
                }
                [waiters addObject:[NSNumber numberWithInt:requestId]];
            }
        } else {
            // Reserved here, under the same lock that just found it absent.
            [cn1clCalls setObject:uuidString forKey:uuidString];
            [cn1clReporting addObject:uuidString];
        }
        reportGeneration = cn1clProviderGeneration;
    }
    if (known) {
        // Already ringing -- the other origin got here first.
        [cn1clEnsureProvider() reportCallWithUUID:uuid updated:update];
        if (requestId >= 0 && !stillPending) {
            cn1clAck(requestId, YES, 0, nil);
        }
        return;
    }
    [cn1clEnsureProvider() reportNewIncomingCallWithUUID:uuid update:update
            completion:^(NSError *error) {
        NSArray *waiters = nil;
        BOOL stale = NO;
        @synchronized (cn1clLock) {
            // A provider RESET while this report was in flight takes every
            // call with it, and the app may already have reported the same
            // uuid again against the new provider. This completion belongs to
            // the old one: removing the uuid from cn1clReporting would strip
            // the NEW report's pending marker, taking the waiters would settle
            // the new report's callers with this outcome, and on error it
            // would drop the new call from cn1clCalls and tell Java it had
            // ended. None of that state is ours any more.
            stale = reportGeneration != cn1clProviderGeneration;
            if (!stale) {
                [cn1clReporting removeObject:uuidString];
                waiters = [cn1clReportWaiters objectForKey:uuidString];
                [cn1clReportWaiters removeObjectForKey:uuidString];
            }
        }
        if (stale) {
            // Its OWN request is still answered -- the reset failed the
            // waiters, but nothing else knows about this one, and a request
            // that never answers is what this SPI calls the worst outcome.
            if (requestId >= 0) {
                cn1clAck(requestId, NO, CN1_CALL_ERR_CALL_REFUSED,
                        @"The call provider reset while the call was being"
                        @" reported");
            }
            return;
        }
        if (error != nil) {
            @synchronized (cn1clLock) {
                [cn1clCalls removeObjectForKey:uuidString];
                cn1clDropAudioLocked(uuidString);
                [cn1clUnclaimed removeObject:uuidString];
            }
            for (NSNumber *waiter in waiters) {
                cn1clAck([waiter intValue], NO, CN1_CALL_ERR_CALL_REFUSED,
                        [error localizedDescription]);
            }
            if (requestId >= 0) {
                int code = CN1_CALL_ERR_CALL_REFUSED;
                if (error.code == CXErrorCodeIncomingCallErrorFilteredByDoNotDisturb
                        || error.code == CXErrorCodeIncomingCallErrorFilteredByBlockList) {
                    code = CN1_CALL_ERR_CALL_FILTERED;
                } else if (error.code == CXErrorCodeIncomingCallErrorCallUUIDAlreadyExists) {
                    code = CN1_CALL_ERR_DUPLICATE_CALL;
                }
                cn1clAck(requestId, NO, code, [error localizedDescription]);
            }
            // AND retire the session, whatever the request was. A pushed call
            // has no requestId (-1), and the push path starts the drain as
            // soon as this report is under way -- so Java can already hold a
            // live RINGING CallSession for a uuid CallKit then refused, for
            // Do Not Disturb or the block list. Clearing only the native
            // tables left that session ringing for the life of the process,
            // addressable but attached to no system call. Reported as FAILED
            // rather than filtered: the app is being told the call is over,
            // and the reason it did not happen already reached whoever asked.
            //
            // A uuid Java has never heard of is ignored by the facade, so
            // sending this unconditionally costs nothing.
            com_codename1_impl_ios_IOSCallCallbacks_callEnded___java_lang_String_int(
                    getThreadLocalData(), cn1clJString(uuidString),
                    CN1_CALL_END_FAILED);
            return;
        }
        for (NSNumber *waiter in waiters) {
            cn1clAck([waiter intValue], YES, 0, nil);
        }
        if (requestId >= 0) {
            cn1clAck(requestId, YES, 0, nil);
        }
    }];
}

/// Queues a pushed call for Java, and drains immediately when it is listening.
static void cn1clQueuePushed(NSString *uuid, NSString *handleWire,
        NSString *displayName, BOOL video, BOOL stale, BOOL synthesized,
        NSString *data) {
    cn1clEnsureState();
    NSDictionary *rec = [NSDictionary dictionaryWithObjectsAndKeys:
            uuid == nil ? @"" : uuid, @"uuid",
            handleWire == nil ? @"" : handleWire, @"handle",
            displayName == nil ? @"" : displayName, @"name",
            [NSNumber numberWithBool:video], @"video",
            [NSNumber numberWithBool:stale], @"stale",
            [NSNumber numberWithBool:synthesized], @"synth",
            data == nil ? @"" : data, @"data",
            [NSNumber numberWithLongLong:
                    (int64_t)([[NSDate date] timeIntervalSince1970] * 1000.0)], @"at",
            nil];
    @synchronized (cn1clLock) {
        // ONE record per uuid. APNs retries a VoIP payload and a server that
        // resends is doing what it should, and the CallKit report itself is
        // already deduplicated -- but the QUEUE was not, so a drain handed
        // callReceived to the app once per copy for a single live
        // CallSession. Signalling would attach, answer or start media twice
        // for one call.
        //
        // A stale record is exempt: it is a missed-call notice rather than a
        // live call, and two of those are two rows in a log.
        BOOL duplicate = NO;
        if (uuid != nil && !stale) {
            for (NSDictionary *queued in cn1clPending) {
                if ([[queued objectForKey:@"uuid"] isEqualToString:uuid]
                        && ![[queued objectForKey:@"stale"] boolValue]) {
                    duplicate = YES;
                    break;
                }
            }
            if (!duplicate && [cn1clCalls objectForKey:uuid] != nil
                    && ![cn1clUnclaimed containsObject:uuid]) {
                // Already drained and live: Java has this call, so a resend
                // is a repeat of something it has already been told.
                duplicate = YES;
            }
        }
        if (!duplicate) {
            [cn1clPending addObject:rec];
            if (uuid != nil && !stale) {
                [cn1clUnclaimed addObject:uuid];
            }
        }
    }
}

/// Whether Java has ever asked for the pending calls itself.
///
/// Only VoipPush.setListener does that, so it answers the question the union
/// readiness flag cannot: is there a listener that can actually receive a
/// pushed call. Never cleared -- a listener that registers and unregisters
/// leaves the Java side able to hold a delivery until the next one arrives,
/// which is not true of a process that has never had one at all.
static BOOL cn1clPushDrainSeen = NO;

static void cn1clDrain(int requestId) {
    cn1clEnsureState();
    NSArray *batch;
    @synchronized (cn1clLock) {
        if (requestId >= 0) {
            // An EXPLICIT drain: the one VoipPush asks for. The internal
            // replay (-1) must not count, or the first pushed call would mark
            // the process ready on its own behalf.
            //
            // Written under the lock the push handler reads it through.
            // Java sets this from the EDT and the handler runs on the main
            // queue, so an unsynchronized write left the handler free to see
            // the old NO, skip the only automatic drain, and leave the call
            // queued until its TTL watchdog ended it -- with a listener
            // installed and waiting the whole time.
            cn1clPushDrainSeen = YES;
        }
        batch = [NSArray arrayWithArray:cn1clPending];
        [cn1clPending removeAllObjects];
    }
    for (NSDictionary *rec in batch) {
        NSString *uuid = [rec objectForKey:@"uuid"];
        // Java is being told about it now, so it is no longer unclaimed.
        cn1clClaim(uuid);
        BOOL stale = [[rec objectForKey:@"stale"] boolValue];
        if (!stale) {
            // A call whose uuid is no longer live ended while the app was
            // getting here, so it is delivered as stale rather than as
            // something the app could answer.
            BOOL live = NO;
            @synchronized (cn1clLock) {
                live = [cn1clCalls objectForKey:uuid] != nil;
            }
            stale = !live;
        }
        com_codename1_impl_ios_IOSCallCallbacks_pushedCall___java_lang_String_java_lang_String_java_lang_String_boolean_boolean_boolean_java_lang_String_long(
                getThreadLocalData(), cn1clJString(uuid),
                cn1clJString([rec objectForKey:@"handle"]),
                cn1clJString([rec objectForKey:@"name"]),
                [[rec objectForKey:@"video"] boolValue] ? JAVA_TRUE : JAVA_FALSE,
                stale ? JAVA_TRUE : JAVA_FALSE,
                [[rec objectForKey:@"synth"] boolValue] ? JAVA_TRUE : JAVA_FALSE,
                cn1clJString([rec objectForKey:@"data"]),
                (JAVA_LONG)[[rec objectForKey:@"at"] longLongValue]);
    }
    if (requestId >= 0) {
        com_codename1_impl_ios_IOSCallCallbacks_pendingCallsDrained___int_int(
                getThreadLocalData(), requestId, (int)[batch count]);
    }
}

#endif /* CN1_CALL_HAS_CALLKIT */

// ---------------------------------------------------------------------
// PushKit: the deadline path
// ---------------------------------------------------------------------
//
// iOS terminates the app if didReceiveIncomingPushWithPayload returns without
// reporting the call to CallKit, and repeated offences revoke VoIP push for
// the installed app. That is a one-strike API, so NOTHING in this path
// touches Java: the payload is read, the call is reported, the record is
// queued, and only later -- when application code asks -- is any of it handed
// up. There is exactly one code path whether the app was running or not,
// because a fast path that only ran sometimes would be the half nobody
// tested.

#if defined(CN1_CALL_HAS_PUSHKIT) && defined(CN1_CALL_HAS_CALLKIT)

@interface CN1CallPushDelegate : NSObject <PKPushRegistryDelegate>
@end

static PKPushRegistry *cn1clRegistry = nil;
static NSString *cn1clVoipToken = nil;

/// The uuid a payload names, or a fresh one when it names none.
///
/// Refusing would return without reporting, which kills the process. So a
/// malformed payload still rings -- flagged, so the server bug is findable
/// rather than presenting as calls that never connect.
static NSString *cn1clUuidFrom(NSDictionary *call, BOOL *synthesized) {
    id raw = [call objectForKey:@"uuid"];
    if ([raw isKindOfClass:[NSString class]]) {
        NSUUID *parsed = [[[NSUUID alloc] initWithUUIDString:(NSString *)raw] autorelease];
        if (parsed != nil) {
            *synthesized = NO;
            return [parsed UUIDString];
        }
    }
    *synthesized = YES;
    return [[NSUUID UUID] UUIDString];
}

@implementation CN1CallPushDelegate

- (void)pushRegistry:(PKPushRegistry *)registry
        didUpdatePushCredentials:(PKPushCredentials *)credentials
        forType:(PKPushType)type {
    const unsigned char *bytes = (const unsigned char *)[credentials.token bytes];
    NSMutableString *hex =
            [NSMutableString stringWithCapacity:[credentials.token length] * 2];
    for (NSUInteger i = 0; i < [credentials.token length]; i++) {
        [hex appendFormat:@"%02x", bytes[i]];
    }
    // Delivered EVERY time, not only when a register() is waiting. APNs
    // rotates a VoIP token while the app stays installed, and the rotation
    // used to update this variable and stop -- so the app's server kept the
    // old token and incoming calls quietly stopped arriving. A requestId of
    // -1 settles nothing and still reaches the tokenChanged listener, which
    // is what the rotation case needs.
    NSArray *waiting = nil;
    NSString *delivered = nil;
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        // The STORE and the drain under one lock, which is the lock a
        // registration reads the token and parks itself under. Storing
        // outside it left a registration able to read nil from a token this
        // callback had already produced.
        cn1clVoipToken = [hex copy];
        // CAPTURED with the batch. The deliveries below re-read the global,
        // and callUnregisterVoipPush clears it under this same lock -- so an
        // unregister landing between the drain and the loop had every pending
        // register answered SUCCESSFULLY with nil, which is neither what this
        // callback produced nor the failure the unregister path intends. The
        // waiters and the value they are being told about have to come out
        // of the lock together.
        delivered = cn1clVoipToken;
        waiting = [NSArray arrayWithArray:cn1clTokenRequests];
        [cn1clTokenRequests removeAllObjects];
    }
    if ([waiting count] == 0) {
        // A rotation with nobody waiting. A requestId of -1 settles nothing
        // and still reaches the tokenChanged listener, which is what this
        // case needs.
        com_codename1_impl_ios_IOSCallCallbacks_voipToken___int_java_lang_String(
                getThreadLocalData(), -1, cn1clJString(delivered));
        return;
    }
    // One settlement per waiting registration. deliverToken tells the
    // listener only when the value actually changed, so several of these
    // announce one rotation.
    for (NSNumber *req in waiting) {
        com_codename1_impl_ios_IOSCallCallbacks_voipToken___int_java_lang_String(
                getThreadLocalData(), [req intValue],
                cn1clJString(delivered));
    }
}

- (void)pushRegistry:(PKPushRegistry *)registry
        didInvalidatePushTokenForType:(PKPushType)type {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        // Under the lock a registration reads it under. Cleared outside it,
        // this raced the read at callRegisterVoipPush with no ordering at
        // all -- two writers (this and callUnregisterVoipPush, which runs on
        // a Java thread) against one locked reader.
        cn1clVoipToken = nil;
    }
    // Told, not just forgotten. Clearing the native cache alone left
    // VoipPush.getToken() answering with the dead token and tokenChanged
    // never firing, so the app went on believing its server could still
    // reach it -- and every call sent to that token was simply lost.
    // deliverToken already handles a null value; the -1 settles no request,
    // which is right because nobody asked for this.
    com_codename1_impl_ios_IOSCallCallbacks_voipToken___int_java_lang_String(
            getThreadLocalData(), -1, JAVA_NULL);
}

- (void)pushRegistry:(PKPushRegistry *)registry
        didReceiveIncomingPushWithPayload:(PKPushPayload *)payload
        forType:(PKPushType)type
        withCompletionHandler:(void (^)(void))completion {
    // Everything below runs before this method returns, and none of it is
    // Java. PushKit hands the payload already parsed, so there is no JSON
    // parser in the deadline window either.
    cn1clEnsureState();
    id rawCall = [payload.dictionaryPayload objectForKey:@"cn1call"];
    if (![rawCall isKindOfClass:[NSDictionary class]]) {
        // A payload with no cn1call rings nothing, which is the case that
        // gets the app killed. Reporting a placeholder and ending it at once
        // is strictly better than returning without reporting: the process
        // survives, and the user sees nothing.
        NSString *uuid = [[NSUUID UUID] UUIDString];
        CXCallUpdate *update = [[[CXCallUpdate alloc] init] autorelease];
        update.remoteHandle = [[[CXHandle alloc] initWithType:CXHandleTypeGeneric
                                                      value:@" "] autorelease];
        [cn1clEnsureProvider() reportNewIncomingCallWithUUID:
                [[[NSUUID alloc] initWithUUIDString:uuid] autorelease] update:update
                completion:^(NSError *error) {
            [cn1clEnsureProvider() reportCallWithUUID:
                    [[[NSUUID alloc] initWithUUIDString:uuid] autorelease]
                    endedAtDate:nil
                    reason:CXCallEndedReasonFailed];
            completion();
        }];
        return;
    }
    NSDictionary *call = (NSDictionary *)rawCall;
    BOOL synthesized = NO;
    NSString *uuidString = cn1clUuidFrom(call, &synthesized);

    id cancel = [call objectForKey:@"cancel"];
    if ([cancel respondsToSelector:@selector(boolValue)] && [cancel boolValue]) {
        // A retraction: the call was cancelled before it was answered.
        NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:uuidString] autorelease];
        if (uuid != nil) {
            id reason = [call objectForKey:@"reason"];
            CXCallEndedReason r = CXCallEndedReasonRemoteEnded;
            if ([reason respondsToSelector:@selector(intValue)]
                    && [reason intValue] == CN1_CALL_END_UNANSWERED) {
                r = CXCallEndedReasonUnanswered;
            }
            BOOL live;
            cn1clEnsureState();
            @synchronized (cn1clLock) {
                live = [cn1clCalls objectForKey:uuidString] != nil;
            }
            if (!live) {
                // A COLD cancellation: the process was restarted since the
                // call was reported, so there is no live CallKit call to end
                // and reportCallWithUUID:endedAtDate: does nothing. PushKit
                // does not care that this push is a retraction -- it requires
                // reportNewIncomingCallWithUUID for EVERY VoIP push, and iOS
                // terminates an app that returns without one, then revokes
                // VoIP delivery if it keeps happening. A server that retracts
                // reliably would be punished for it.
                //
                // Reported and ended at once, exactly as the payload with no
                // cn1call above is: the process survives and the user sees
                // nothing.
                CXCallUpdate *cancelled = [[[CXCallUpdate alloc] init] autorelease];
                cancelled.remoteHandle =
                        [[[CXHandle alloc] initWithType:CXHandleTypeGeneric
                                                 value:@" "] autorelease];
                [cn1clEnsureProvider() reportNewIncomingCallWithUUID:uuid
                        update:cancelled completion:^(NSError *error) {
                    [cn1clEnsureProvider() reportCallWithUUID:uuid
                            endedAtDate:nil reason:r];
                    completion();
                    // Told to Java anyway: a fresh process has no session to
                    // retire, and the facade ignores a uuid it has never
                    // heard of -- but the same push can arrive in a process
                    // that HAS drained one.
                    com_codename1_impl_ios_IOSCallCallbacks_callEnded___java_lang_String_int(
                            getThreadLocalData(), cn1clJString(uuidString),
                            r == CXCallEndedReasonUnanswered
                                    ? CN1_CALL_END_UNANSWERED
                                    : CN1_CALL_END_REMOTE);
                }];
                return;
            }
            [cn1clEnsureProvider() reportCallWithUUID:uuid endedAtDate:nil reason:r];
            @synchronized (cn1clLock) {
                [cn1clCalls removeObjectForKey:uuidString];
                cn1clDropAudioLocked(uuidString);
            }
            completion();
            // AFTER the completion, so PushKit's deadline is satisfied first
            // whatever Java does with this.
            //
            // Sent unconditionally. A retraction that beats the drain names a
            // call Java has never heard of and the facade ignores it; one
            // that arrives after the drain names a session Java has adopted,
            // which without this stayed RINGING for the life of the process
            // with every later operation on it answering INVALID_ID.
            com_codename1_impl_ios_IOSCallCallbacks_callEnded___java_lang_String_int(
                    getThreadLocalData(), cn1clJString(uuidString),
                    r == CXCallEndedReasonUnanswered
                            ? CN1_CALL_END_UNANSWERED
                            : CN1_CALL_END_REMOTE);
            return;
        }
        completion();
        return;
    }

    id handleValue = [call objectForKey:@"handle"];
    NSString *handle = [handleValue isKindOfClass:[NSString class]]
            ? (NSString *)handleValue : @"";
    id typeValue = [call objectForKey:@"handleType"];
    int handleType = CN1_CALL_HANDLE_GENERIC;
    if ([typeValue isKindOfClass:[NSString class]]) {
        if ([(NSString *)typeValue isEqualToString:@"phoneNumber"]) {
            handleType = CN1_CALL_HANDLE_PHONE;
        } else if ([(NSString *)typeValue isEqualToString:@"emailAddress"]) {
            handleType = CN1_CALL_HANDLE_EMAIL;
        }
    }
    NSString *handleWire = cn1clJoin([NSArray arrayWithObjects:
            [NSString stringWithFormat:@"%d", handleType], handle, nil]);

    id nameValue = [call objectForKey:@"displayName"];
    NSString *name = [nameValue isKindOfClass:[NSString class]]
            ? (NSString *)nameValue
            : cn1clPlistString(@"CN1CallDefaultCallerName", @"");
    id videoValue = [call objectForKey:@"video"];
    BOOL video = [videoValue respondsToSelector:@selector(boolValue)]
            && [videoValue boolValue];
    id dataValue = [call objectForKey:@"data"];
    NSString *data = [dataValue isKindOfClass:[NSString class]]
            ? (NSString *)dataValue : nil;

    int ttl = [cn1clPlistString(@"CN1CallPendingTTLSeconds", @"30") intValue];
    id ttlValue = [call objectForKey:@"ttl"];
    if ([ttlValue respondsToSelector:@selector(intValue)] && [ttlValue intValue] > 0) {
        ttl = [ttlValue intValue];
    }

    cn1clQueuePushed(uuidString, handleWire, name, video, NO, synthesized, data);

    // The report itself. requestId -1: nobody in Java is waiting on it.
    cn1clReportIncoming(-1, uuidString, handleWire, name, video);

    // A watchdog, so a call the application never claims is ended rather than
    // left ringing forever on a lock screen the user cannot clear.
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)ttl * NSEC_PER_SEC),
            dispatch_get_main_queue(), ^{
        BOOL live = NO;
        @synchronized (cn1clLock) {
            // BOTH conditions. Presence in cn1clCalls alone is true of an
            // answered, active call, and ending one of those as Unanswered is
            // exactly the bug this test used to have.
            live = [cn1clCalls objectForKey:uuidString] != nil
                    && [cn1clUnclaimed containsObject:uuidString];
        }
        if (live) {
            NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:uuidString] autorelease];
            [cn1clEnsureProvider() reportCallWithUUID:uuid endedAtDate:nil
                    reason:CXCallEndedReasonUnanswered];
            @synchronized (cn1clLock) {
                [cn1clCalls removeObjectForKey:uuidString];
                cn1clDropAudioLocked(uuidString);
                [cn1clUnclaimed removeObject:uuidString];
            }
        }
    });

    // Java readiness is the UNION of the two listener kinds, so an app with a
    // Calls action listener and no VoipPush one made it true. Draining on
    // that took the call off cn1clUnclaimed -- the TTL watchdog's list -- and
    // handed it to a facade with no push listener, which had nothing to give
    // it to. The explicit drain is VoipPush's own operation, so having seen
    // one is what says a push listener exists; before that the call stays
    // queued and stays the watchdog's.
    BOOL readyToDrain;
    @synchronized (cn1clLock) {
        // BOTH flags in one snapshot, under the lock each is written through,
        // so this cannot read a half-updated pair.
        readyToDrain = cn1clJavaReady && cn1clPushDrainSeen;
    }
    if (readyToDrain) {
        // Deliberately asynchronous and OUTSIDE this handler: the report above
        // has already satisfied the deadline, and hopping to the main queue
        // keeps the one code path from ever calling into the VM while the
        // push handler is on the stack.
        dispatch_async(dispatch_get_main_queue(), ^{
            cn1clDrain(-1);
        });
    }
    completion();
}

@end

static CN1CallPushDelegate *cn1clPushDelegate = nil;

void cn1CallInstallPushRegistry(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1clEnsureState();
        // The provider exists before the registry, so a push delivered during
        // launch has something to report to.
        cn1clEnsureProvider();
        cn1clPushDelegate = [[CN1CallPushDelegate alloc] init];
        cn1clRegistry = [[PKPushRegistry alloc]
                initWithQueue:dispatch_get_main_queue()];
        cn1clRegistry.delegate = cn1clPushDelegate;
        cn1clRegistry.desiredPushTypes = [NSSet setWithObject:PKPushTypeVoIP];
    });
}

#else

void cn1CallInstallPushRegistry(void) {
    // No PushKit in this build.
}

#endif /* CN1_CALL_HAS_PUSHKIT */

// ---------------------------------------------------------------------
// The exported natives.
//
// Both halves are always defined. A build without CN1_INCLUDE_CALL still
// links every symbol -- answering "unsupported" -- because a native method is
// kept alive BY its symbol appearing in the native sources: absent it, the
// dead-code pass drops the Java method and the feature ships inert with a
// green build and nothing in the log.
// ---------------------------------------------------------------------

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_callSupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_CALL_HAS_CALLKIT
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_callVoipSupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if defined(CN1_CALL_HAS_PUSHKIT) && defined(CN1_CALL_HAS_CALLKIT)
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_callDirectorySupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if defined(CN1_CALL_DIRECTORY) && defined(CN1_CALL_HAS_CALLKIT)
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_callCapabilities___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_CALL_HAS_CALLKIT
    // Deliberately NO CN1_CALL_CAP_ROUTE_PICKER. CallKit has no system audio
    // route picker to present -- AVRoutePickerView is a view an app places
    // itself -- so advertising the bit only leads apps to call a method that
    // always answers NOT_SUPPORTED.
    int caps = CN1_CALL_CAP_SYSTEM_UI | CN1_CALL_CAP_OUTGOING
            | CN1_CALL_CAP_HOLD | CN1_CALL_CAP_MUTE | CN1_CALL_CAP_DTMF;
    // VIDEO only when the BUILD is video-enabled. IPhoneBuilder writes
    // NSCameraUsageDescription only for a video build, and iOS terminates an
    // app that touches the camera with no purpose string -- so advertising
    // the bit on an audio-only build invited a caller that followed the
    // capability API into requestPermissions(PERMISSION_CAMERA) and a kill.
    // The same plist key the provider is configured from answers it, so the
    // capability and the configuration cannot disagree.
    //
    // This is the iOS half of the Android camera gate; that one asks the
    // manifest, this one asks the bundle, and both refuse to promise video a
    // build cannot deliver.
    if (cn1clPlistBool(@"CN1CallSupportsVideo", NO)) {
        caps |= CN1_CALL_CAP_VIDEO;
    }
#ifdef CN1_CALL_HAS_PUSHKIT
    caps |= CN1_CALL_CAP_VOIP_PUSH;
#endif
#ifdef CN1_CALL_DIRECTORY
    caps |= CN1_CALL_CAP_DIRECTORY;
#endif
    return caps;
#else
    return 0;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_callAvailability___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_CALL_HAS_CALLKIT
    // CXCallObserver reports every call on the device, including other apps'
    // and the cellular one. A call this app does not own means CallKit will
    // refuse the next report, which is worth knowing BEFORE telling a caller
    // their call is ringing.
    // RELEASED on every exit: this target compiles with
    // CLANG_ENABLE_OBJC_ARC=NO, so the alloc below is +1 and nothing else
    // owns it. getAvailability() is documented as the call to make before
    // every incoming call, so leaking one observer -- and the call list it
    // retains -- per invocation is a leak that grows with use.
    CXCallObserver *observer = [[CXCallObserver alloc] init];
    int mineLive = 0;
    BOOL foreign = NO;
    for (CXCall *c in observer.calls) {
        if (c.hasEnded) {
            continue;
        }
        BOOL mine = NO;
        @synchronized (cn1clLock) {
            mine = cn1clCalls != nil
                    && [cn1clCalls objectForKey:[c.UUID UUIDString]] != nil;
        }
        if (!mine) {
            foreign = YES;
            break;
        }
        mineLive++;
    }
    [observer release];
    if (foreign) {
        return CN1_CALL_AVAIL_OTHER_APP;
    }
    if (mineLive >= cn1clCapacity()) {
        // OUR OWN calls fill the provider. cn1clConfiguration defaults to
        // one group of one call, so by default a single live call is enough;
        // an app that raised the limits through CallConfiguration gets room
        // for as many as it asked for, which is why this compares a COUNT
        // against the configured capacity rather than testing for any call at
        // all. CallKit refuses the report once the provider is full -- and
        // answering AVAILABLE then would be the same broken
        // promise as answering it before the provider was configured: the
        // caller is told to stop retrying only when it CANNOT ring, and it is
        // told it can ring when the report is about to be refused.
        //
        // Skipped on Android on purpose, not by omission: Telecom accepts a
        // second self-managed call from the same account, which is why that
        // bridge ignores its own calls when it answers this question.
        return CN1_CALL_AVAIL_THIS_APP;
    }
    return CN1_CALL_AVAIL_AVAILABLE;
#else
    return CN1_CALL_AVAIL_UNSUPPORTED;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_callGrantedPermissions___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_CALL_HAS_CALLKIT
    // Owning a call needs no permission on iOS, so that bit is always set;
    // the microphone is the one the user can refuse.
    int mask = CN1_CALL_PERM_MANAGE_CALLS | CN1_CALL_PERM_NOTIFICATIONS;
    if ([[AVAudioSession sharedInstance] recordPermission]
            == AVAudioSessionRecordPermissionGranted) {
        mask |= CN1_CALL_PERM_MICROPHONE;
    }
    // The camera too, not just the microphone: an app branching on
    // CAPABILITY_VIDEO could otherwise never see the bit turn on, whatever
    // the user had granted.
    if ([AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo]
            == AVAuthorizationStatusAuthorized) {
        mask |= CN1_CALL_PERM_CAMERA;
    }
    return mask;
#else
    return 0;
#endif
}

void com_codename1_impl_ios_IOSNative_callRequestPermissions___int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_INT permissionBits) {
#ifdef CN1_CALL_HAS_CALLKIT
    // Both prompts, chained, so a video app gets one answer covering the
    // pair rather than having to ask twice through different APIs. Each is
    // skipped when it was not asked for.
    BOOL wantsMic = (permissionBits & CN1_CALL_PERM_MICROPHONE) != 0;
    BOOL wantsCamera = (permissionBits & CN1_CALL_PERM_CAMERA) != 0;
    void (^answer)(void) = ^{
        com_codename1_impl_ios_IOSCallCallbacks_permissionResult___int_int(
                getThreadLocalData(), requestId,
                com_codename1_impl_ios_IOSNative_callGrantedPermissions___R_int(
                        getThreadLocalData(), JAVA_NULL));
    };
    void (^askCamera)(void) = ^{
        // The BUILD decides, before the API is touched at all. IPhoneBuilder
        // writes NSCameraUsageDescription only for a video build, and iOS
        // terminates an app that reaches a protected resource without its
        // purpose string -- so asking here on an audio-only build killed the
        // app rather than answering it. getCapabilities already refuses to
        // advertise VIDEO on such a build; this is the same question asked
        // where the request is actually made, because an app may pass the
        // bit without consulting the capability first.
        if (!wantsCamera || !cn1clPlistBool(@"CN1CallSupportsVideo", NO)) {
            answer();
            return;
        }
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo
                completionHandler:^(BOOL granted) {
            answer();
        }];
    };
    if (wantsMic) {
        [[AVAudioSession sharedInstance] requestRecordPermission:^(BOOL granted) {
            askCamera();
        }];
        return;
    }
    askCamera();
#else
    com_codename1_impl_ios_IOSCallCallbacks_permissionResult___int_int(
            threadStateData, requestId, 0);
#endif
}

void com_codename1_impl_ios_IOSNative_callConfigureProvider___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT configWire) {
#ifdef CN1_CALL_HAS_CALLKIT
    // Info.plist supplies the defaults, because a pushed call is reported
    // before any of this could have been set -- but the record is applied on
    // top of them, which is what CallConfiguration documents. Ignoring it
    // meant includesCallsInRecents, the handle types, video support and the
    // display name had no effect even once the app was running.
    CXProvider *provider = cn1clEnsureProvider();
    NSArray *cfgFields = cn1clSplit(toNSString(threadStateData, configWire));
    CXProviderConfiguration *cfg = cn1clConfiguration();
    NSString *name = cn1clField(cfgFields, 0);
    if ([name length] > 0) {
        // localizedName is read-only from iOS 14, where the configuration is
        // built without a name and the bundle supplies it; setting it there
        // would throw.
        if (![cfg respondsToSelector:@selector(setLocalizedName:)]) {
            CXProviderConfiguration *replacement =
                    [[[CXProviderConfiguration alloc] initWithLocalizedName:name] autorelease];
            // EVERYTHING the bundle configured, carried across -- not just the
            // ringtone. The icon in particular comes only from
            // CN1CallIconTemplateImageName, so an app that set ios.call.icon
            // and then made the documented Calls.configure() call lost its
            // CallKit icon the moment it did so. Copied from the old
            // configuration rather than re-read, so this cannot drift from
            // cn1clConfiguration again.
            replacement.ringtoneSound = cfg.ringtoneSound;
            replacement.iconTemplateImageData = cfg.iconTemplateImageData;
            replacement.supportedHandleTypes = cfg.supportedHandleTypes;
            cfg = replacement;
        }
    }
    cfg.supportsVideo = [cn1clField(cfgFields, 1) isEqualToString:@"1"];
    cfg.includesCallsInRecents =
            [cn1clField(cfgFields, 2) isEqualToString:@"1"];
    NSInteger groups = [cn1clField(cfgFields, 3) integerValue];
    NSInteger perGroup = [cn1clField(cfgFields, 4) integerValue];
    cfg.maximumCallGroups = groups > 0 ? groups : 1;
    cfg.maximumCallsPerCallGroup = perGroup > 0 ? perGroup : 1;
    // Mirrored, because availability has to answer against the limits the
    // provider is ACTUALLY running with. Reporting "this app is in a call"
    // as soon as one existed was right for the one-call default and wrong
    // the moment CallConfiguration raised it: callers were told to reject a
    // call the configured provider had room to ring.
    @synchronized (cn1clLock) {
        cn1clMaxGroups = (int) cfg.maximumCallGroups;
        cn1clMaxPerGroup = (int) cfg.maximumCallsPerCallGroup;
    }
    NSString *types = cn1clField(cfgFields, 5);
    if ([types length] > 0) {
        NSMutableSet *set = [NSMutableSet set];
        for (NSString *t in [types componentsSeparatedByString:@","]) {
            int ordinal = [t intValue];
            if (ordinal == CN1_CALL_HANDLE_PHONE) {
                [set addObject:[NSNumber numberWithInteger:CXHandleTypePhoneNumber]];
            } else if (ordinal == CN1_CALL_HANDLE_EMAIL) {
                [set addObject:[NSNumber numberWithInteger:CXHandleTypeEmailAddress]];
            } else {
                [set addObject:[NSNumber numberWithInteger:CXHandleTypeGeneric]];
            }
        }
        if ([set count] > 0) {
            cfg.supportedHandleTypes = set;
        }
    }
    provider.configuration = cfg;
    cn1clConfigured = YES;
    cn1clAck(requestId, YES, 0, nil);
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED,
            @"This build did not link CallKit");
#endif
}

void com_codename1_impl_ios_IOSNative_callReportIncoming___int_java_lang_String_java_lang_String_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_OBJECT handleWire,
        JAVA_OBJECT displayName, JAVA_BOOLEAN hasVideo) {
#ifdef CN1_CALL_HAS_CALLKIT
    cn1clReportIncoming(requestId, toNSString(threadStateData, callId),
            toNSString(threadStateData, handleWire),
            displayName == JAVA_NULL ? nil : toNSString(threadStateData, displayName),
            hasVideo != JAVA_FALSE);
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED,
            @"This build did not link CallKit");
#endif
}

void com_codename1_impl_ios_IOSNative_callReportOutgoing___int_java_lang_String_java_lang_String_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_OBJECT handleWire,
        JAVA_OBJECT displayName, JAVA_BOOLEAN hasVideo) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSString *uuidString = toNSString(threadStateData, callId);
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:uuidString] autorelease];
    if (uuid == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID,
                @"Not a canonical call id");
        return;
    }
    // A call the system asked this app to place is already a CallKit
    // transaction in flight. Submitting a second start action for the same
    // uuid is what CallKit refuses, and that refusal failed reportOutgoing()
    // and took the Java session with it while the original action went on to
    // be fulfilled. Adopt it instead: register the call and answer yes.
    cn1clEnsureState();
    BOOL adopting = NO;
    @synchronized (cn1clLock) {
        adopting = [cn1clSystemStarts containsObject:uuidString];
        if (adopting) {
            [cn1clSystemStarts removeObject:uuidString];
            [cn1clCalls setObject:uuidString forKey:uuidString];
        }
    }
    if (adopting) {
        cn1clAck(requestId, YES, 0, nil);
        return;
    }
    CXStartCallAction *action = [[[CXStartCallAction alloc]
            initWithCallUUID:uuid
            handle:cn1clHandleFromWire(toNSString(threadStateData, handleWire))] autorelease];
    action.video = hasVideo != JAVA_FALSE;
    if (displayName != JAVA_NULL) {
        action.contactIdentifier = toNSString(threadStateData, displayName);
    }
    cn1clEnsureProvider();
    // Marked BEFORE the transaction is requested: CallKit may dispatch the
    // action to performStartCallAction before this call returns, and the
    // delegate uses this to tell an app-originated start from a system one.
    cn1clClaimOwn(uuidString, [CXStartCallAction class]);
    [cn1clController requestTransaction:[[[CXTransaction alloc] initWithAction:action] autorelease]
            completion:^(NSError *error) {
        if (error != nil) {
            cn1clReleaseOwn(uuidString, [CXStartCallAction class]);
            cn1clAck(requestId, NO, CN1_CALL_ERR_CALL_REFUSED,
                    [error localizedDescription]);
            return;
        }
        @synchronized (cn1clLock) {
            [cn1clCalls setObject:uuidString forKey:uuidString];
        }
        cn1clAck(requestId, YES, 0, nil);
    }];
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED,
            @"This build did not link CallKit");
#endif
}

void com_codename1_impl_ios_IOSNative_callStartedConnecting___java_lang_String_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_LONG timestampMs) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)] autorelease];
    if (uuid != nil) {
        [cn1clEnsureProvider() reportOutgoingCallWithUUID:uuid
                startedConnectingAtDate:[NSDate dateWithTimeIntervalSince1970:
                        (double)timestampMs / 1000.0]];
    }
#endif
}

void com_codename1_impl_ios_IOSNative_callOutgoingConnected___java_lang_String_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_LONG timestampMs) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)] autorelease];
    if (uuid != nil) {
        [cn1clEnsureProvider() reportOutgoingCallWithUUID:uuid
                connectedAtDate:[NSDate dateWithTimeIntervalSince1970:
                        (double)timestampMs / 1000.0]];
    }
#endif
}

void com_codename1_impl_ios_IOSNative_callIncomingConnected___java_lang_String_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_LONG timestampMs) {
    // CallKit has no incoming-connected report: an answered call is connected
    // by the answer action itself. Present so the SPI is uniform.
}

void com_codename1_impl_ios_IOSNative_callUpdate___java_lang_String_java_lang_String_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_OBJECT handleWire, JAVA_OBJECT displayName,
        JAVA_BOOLEAN hasVideo) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)] autorelease];
    if (uuid == nil) {
        return;
    }
    CXCallUpdate *update = [[[CXCallUpdate alloc] init] autorelease];
    if (handleWire != JAVA_NULL) {
        NSString *wire = toNSString(threadStateData, handleWire);
        if ([wire length] > 0) {
            update.remoteHandle = cn1clHandleFromWire(wire);
        }
    }
    if (displayName != JAVA_NULL) {
        update.localizedCallerName = toNSString(threadStateData, displayName);
    }
    [cn1clEnsureProvider() reportCallWithUUID:uuid updated:update];
#endif
}

void com_codename1_impl_ios_IOSNative_callReportEnded___java_lang_String_int_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_INT endReasonOrdinal, JAVA_LONG timestampMs) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSString *uuidString = toNSString(threadStateData, callId);
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:uuidString] autorelease];
    if (uuid == nil) {
        return;
    }
    // The reason is what the system writes in the call log, so it is
    // user-visible rather than cosmetic.
    CXCallEndedReason reason = CXCallEndedReasonRemoteEnded;
    switch (endReasonOrdinal) {
        case CN1_CALL_END_UNANSWERED: reason = CXCallEndedReasonUnanswered; break;
        case CN1_CALL_END_BUSY:       reason = CXCallEndedReasonUnanswered; break;
        case CN1_CALL_END_FAILED:     reason = CXCallEndedReasonFailed; break;
        case CN1_CALL_END_FILTERED:   reason = CXCallEndedReasonDeclinedElsewhere; break;
        default:                      reason = CXCallEndedReasonRemoteEnded; break;
    }
    [cn1clEnsureProvider() reportCallWithUUID:uuid
            endedAtDate:[NSDate dateWithTimeIntervalSince1970:
                    (double)timestampMs / 1000.0]
            reason:reason];
    @synchronized (cn1clLock) {
        [cn1clCalls removeObjectForKey:uuidString];
        cn1clDropAudioLocked(uuidString);
    }
#endif
}

void com_codename1_impl_ios_IOSNative_callEnd___int_java_lang_String_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_INT endReasonOrdinal) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSString *uuidString = toNSString(threadStateData, callId);
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:uuidString] autorelease];
    BOOL known = NO;
    @synchronized (cn1clLock) {
        known = cn1clCalls != nil && [cn1clCalls objectForKey:uuidString] != nil;
    }
    if (uuid == nil || !known) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID,
                @"No such call");
        return;
    }
    CXEndCallAction *action = [[[CXEndCallAction alloc] initWithCallUUID:uuid] autorelease];
    cn1clClaimOwn(uuidString, [CXEndCallAction class]);
    [cn1clController requestTransaction:[[[CXTransaction alloc] initWithAction:action] autorelease]
            completion:^(NSError *error) {
        if (error != nil) {
            cn1clReleaseOwn(uuidString, [CXEndCallAction class]);
            // The call is STILL LIVE: CallKit refused the transaction. It
            // used to be forgotten here regardless, so a retry answered
            // INVALID_ID and no later update or remote-end report could reach
            // a call the system was still showing.
            cn1clAck(requestId, NO, CN1_CALL_ERR_UNKNOWN,
                    [error localizedDescription]);
            return;
        }
        @synchronized (cn1clLock) {
            [cn1clCalls removeObjectForKey:uuidString];
            cn1clDropAudioLocked(uuidString);
            [cn1clUnclaimed removeObject:uuidString];
        }
        cn1clAck(requestId, YES, 0, nil);
    }];
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED, nil);
#endif
}

void com_codename1_impl_ios_IOSNative_callSetHeld___int_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_BOOLEAN held) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)] autorelease];
    if (uuid == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID, @"No such call");
        return;
    }
    CXSetHeldCallAction *action = [[[CXSetHeldCallAction alloc]
            initWithCallUUID:uuid onHold:held != JAVA_FALSE] autorelease];
    NSString *heldUuid = [uuid UUIDString];
    cn1clClaimOwn(heldUuid, [CXSetHeldCallAction class]);
    [cn1clController requestTransaction:[[[CXTransaction alloc] initWithAction:action] autorelease]
            completion:^(NSError *error) {
        if (error != nil) {
            cn1clReleaseOwn(heldUuid, [CXSetHeldCallAction class]);
        }
        cn1clAck(requestId, error == nil, CN1_CALL_ERR_UNKNOWN,
                error == nil ? nil : [error localizedDescription]);
    }];
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED, nil);
#endif
}

void com_codename1_impl_ios_IOSNative_callSetMuted___int_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_BOOLEAN muted) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)] autorelease];
    if (uuid == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID, @"No such call");
        return;
    }
    CXSetMutedCallAction *action = [[[CXSetMutedCallAction alloc]
            initWithCallUUID:uuid muted:muted != JAVA_FALSE] autorelease];
    NSString *mutedUuid = [uuid UUIDString];
    cn1clClaimOwn(mutedUuid, [CXSetMutedCallAction class]);
    [cn1clController requestTransaction:[[[CXTransaction alloc] initWithAction:action] autorelease]
            completion:^(NSError *error) {
        if (error != nil) {
            cn1clReleaseOwn(mutedUuid, [CXSetMutedCallAction class]);
        }
        cn1clAck(requestId, error == nil, CN1_CALL_ERR_UNKNOWN,
                error == nil ? nil : [error localizedDescription]);
    }];
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED, nil);
#endif
}

void com_codename1_impl_ios_IOSNative_callSendDtmf___int_java_lang_String_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_OBJECT digits) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSUUID *uuid = [[[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)] autorelease];
    if (uuid == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID, @"No such call");
        return;
    }
    CXPlayDTMFCallAction *action = [[[CXPlayDTMFCallAction alloc]
            initWithCallUUID:uuid
            digits:toNSString(threadStateData, digits)
            type:CXPlayDTMFCallActionTypeSingleTone] autorelease];
    [cn1clController requestTransaction:[[[CXTransaction alloc] initWithAction:action] autorelease]
            completion:^(NSError *error) {
        cn1clAck(requestId, error == nil, CN1_CALL_ERR_UNKNOWN,
                error == nil ? nil : [error localizedDescription]);
    }];
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED, nil);
#endif
}

void com_codename1_impl_ios_IOSNative_callSetGroup___int_java_lang_String_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_OBJECT otherCallId) {
    // NOT_SUPPORTED whether or not CallKit is compiled in, because CallKit
    // offers no app-initiated group action: CXSetGroupCallAction travels
    // system to app, and CXCallUpdate.supportsGrouping only says the system
    // MAY offer grouping in its own UI. Setting that flag and acknowledging
    // success meant groupWith() reported that it had conferenced two calls
    // and had done nothing of the kind -- and callCapabilities has never
    // claimed CN1_CALL_CAP_GROUPING, so this was the one entry point
    // disagreeing with the mask beside it.
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED, nil);
}

/// The route iOS is ACTUALLY using, read from the session.
static int cn1clCurrentRoute(void) {
    AVAudioSessionRouteDescription *route =
            [[AVAudioSession sharedInstance] currentRoute];
    for (AVAudioSessionPortDescription *port in route.outputs) {
        if ([port.portType isEqualToString:AVAudioSessionPortBuiltInSpeaker]) {
            return CN1_CALL_ROUTE_SPEAKER;
        }
        if ([port.portType isEqualToString:AVAudioSessionPortHeadphones]
                || [port.portType isEqualToString:AVAudioSessionPortHeadsetMic]) {
            return CN1_CALL_ROUTE_WIRED;
        }
        if ([port.portType isEqualToString:AVAudioSessionPortBluetoothHFP]
                || [port.portType isEqualToString:AVAudioSessionPortBluetoothA2DP]
                || [port.portType isEqualToString:AVAudioSessionPortBluetoothLE]) {
            return CN1_CALL_ROUTE_BLUETOOTH;
        }
        if ([port.portType isEqualToString:AVAudioSessionPortBuiltInReceiver]) {
            return CN1_CALL_ROUTE_EARPIECE;
        }
    }
    return CN1_CALL_ROUTE_UNKNOWN;
}

JAVA_INT com_codename1_impl_ios_IOSNative_callAudioRoute___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_CALL_HAS_CALLKIT
    return cn1clCurrentRoute();
#else
    return CN1_CALL_ROUTE_UNKNOWN;
#endif
}

void com_codename1_impl_ios_IOSNative_callSetAudioRoute___int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_INT routeOrdinal) {
#ifdef CN1_CALL_HAS_CALLKIT
    NSError *error = nil;
    AVAudioSession *session = [AVAudioSession sharedInstance];
    if (routeOrdinal == CN1_CALL_ROUTE_SPEAKER) {
        [session overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker
                                   error:&error];
    } else {
        [session overrideOutputAudioPort:AVAudioSessionPortOverrideNone
                                   error:&error];
    }
    if (error != nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_AUDIO_FAILED,
                [error localizedDescription]);
        return;
    }
    // The override is the ONLY route control iOS offers an app: it moves
    // audio to the speaker or takes that override off, and iOS then picks
    // among the rest itself. Clearing the override and reporting the
    // requested route as achieved meant asking for BLUETOOTH with no device
    // paired answered yes, and audioSessionActivated went on to tell the app
    // its audio was on Bluetooth while it played out of the earpiece.
    //
    // So the session is asked what actually happened, and the answer is what
    // gets recorded and reported.
    int actual = cn1clCurrentRoute();
    cn1clRoute = actual;
    if (actual == routeOrdinal || routeOrdinal == CN1_CALL_ROUTE_UNKNOWN) {
        cn1clAck(requestId, YES, 0, nil);
        return;
    }
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED,
            @"iOS chooses the non-speaker route itself; the requested one is"
            @" not the one in use");
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED, nil);
#endif
}

void com_codename1_impl_ios_IOSNative_callShowRoutePicker___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId) {
#ifdef CN1_CALL_HAS_CALLKIT
    // There is no CallKit route picker to present; AVRoutePickerView is a UI
    // component an app places itself. Answering false rather than pretending
    // keeps an app from waiting for a sheet that will never appear.
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED,
            @"iOS has no system call audio route picker to present");
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED, nil);
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_callCompleteAction___long_boolean_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_LONG actionToken, JAVA_BOOLEAN fulfilled) {
#ifdef CN1_CALL_HAS_CALLKIT
    cn1clEnsureState();
    CXAction *action = nil;
    NSNumber *key = [NSNumber numberWithLongLong:(int64_t)actionToken];
    @synchronized (cn1clLock) {
        action = [cn1clActions objectForKey:key];
        [cn1clActions removeObjectForKey:key];
    }
    // A second answer for the same token finds nothing and does nothing. The
    // facade's safety net and a slow application may both answer, and that
    // race is not worth making anyone think about.
    // Answering an action CallKit no longer holds is not a no-op for the
    // CALLER: the Java event was queued before the deadline passed, so its
    // local effect -- ending the session, moving it to HELD or ACTIVE -- would
    // still run and leave Java in a state the system call UI is not in.
    // Reporting the miss is what lets the facade skip it, and it costs no
    // extra native symbol, which is the point: a separate expiry callback is
    // one more mangled name that can be wrong in silence.
    if (action == nil) {
        return JAVA_FALSE;
    }
    if ([action isKindOfClass:[CXStartCallAction class]]) {
        // The adopt window is over either way: the app has answered the
        // action, so it either reported the call (which removed this) or
        // never will. Left behind, an unreported start would sit in the set
        // for the life of the process.
        NSString *startUuid = [[(CXCallAction *)action callUUID] UUIDString];
        @synchronized (cn1clLock) {
            [cn1clSystemStarts removeObject:startUuid];
        }
    }
    if (fulfilled != JAVA_FALSE) {
        // A fulfilled end is the only point at which the call is really over,
        // so it is where the native bookkeeping drops it.
        if ([action isKindOfClass:[CXEndCallAction class]]) {
            NSString *uuid = [[(CXCallAction *)action callUUID] UUIDString];
            @synchronized (cn1clLock) {
                [cn1clCalls removeObjectForKey:uuid];
                cn1clDropAudioLocked(uuid);
                [cn1clUnclaimed removeObject:uuid];
            }
        }
        [action fulfill];
    } else {
        [action fail];
    }
    return JAVA_TRUE;
#else
    // No CallKit in this build, so nothing can have expired. Saying TRUE
    // keeps the facade's local effects running rather than silently
    // suppressing them.
    return JAVA_TRUE;
#endif
}

void com_codename1_impl_ios_IOSNative_callRegisterVoipPush___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
#if defined(CN1_CALL_HAS_PUSHKIT) && defined(CN1_CALL_HAS_CALLKIT)
    cn1CallInstallPushRegistry();
    // Asked for on EVERY registration, not only the first. The installer is
    // dispatch_once-guarded, so after an unregister() emptied desiredPushTypes
    // a later register() went through here, found the registry already built,
    // and parked a request that no credentials could ever answer -- VoIP
    // delivery stayed off until the process restarted. Re-asserting the type
    // is what makes PushKit hand the token over again.
    if (cn1clRegistry != nil) {
        cn1clRegistry.desiredPushTypes = [NSSet setWithObject:PKPushTypeVoIP];
    }
    // The token READ and the parking under one lock, and the delegate stores
    // the token and drains the waiters under the same one. Split, the
    // credentials callback could land between this check and the insert:
    // it stores the token, sees no waiters, emits only its unsolicited -1
    // delivery, and this registration then parks behind the sole callback
    // that would ever have answered it. An AsyncResource that never settles
    // is the failure this SPI exists to prevent.
    cn1clEnsureState();
    // Answered on the REGISTRY'S OWN QUEUE, which is where didUpdate and
    // didInvalidate run, so every token Java is told about is decided in one
    // order. Reading here and delivering from this thread let an
    // invalidation land in between: this copy then answered with the token
    // it had read, AFTER the invalidation had already told Java the token
    // was gone. Java's last word was the dead value, and an app that
    // registers its token with its server in the callback re-registered the
    // one PushKit had just retired -- so every call sent to it was lost,
    // which is the failure the invalidation delivery exists to prevent.
    //
    // The value is re-read inside the block rather than carried into it, so
    // a token that stopped being current between the request and the reply
    // is never the one delivered. Still outside the lock at the VM call: it
    // reaches application code, which may call back in here.
    dispatch_async(dispatch_get_main_queue(), ^{
        NSString *known = nil;
        BOOL wanted = cn1clRegistry == nil
                || [cn1clRegistry.desiredPushTypes count] > 0;
        @synchronized (cn1clLock) {
            known = cn1clVoipToken;
            if (known == nil && wanted) {
                [cn1clTokenRequests addObject:
                        [NSNumber numberWithInt:(int)requestId]];
            }
        }
        if (known != nil) {
            com_codename1_impl_ios_IOSCallCallbacks_voipToken___int_java_lang_String(
                    getThreadLocalData(), requestId, cn1clJString(known));
        } else if (!wanted) {
            // Parking here would never be answered: unregister drains the
            // waiters as it runs, so one added afterwards has no callback
            // left that could settle it.
            com_codename1_impl_ios_IOSCallCallbacks_voipRegistrationFailed___int_int_java_lang_String(
                    getThreadLocalData(), requestId,
                    CN1_CALL_ERR_PUSH_UNAVAILABLE,
                    cn1clJString(@"VoIP push delivery is switched off"));
        }
    });
    return;
#else
    com_codename1_impl_ios_IOSCallCallbacks_voipRegistrationFailed___int_int_java_lang_String(
            threadStateData, requestId, CN1_CALL_ERR_NOT_SUPPORTED,
            cn1clJString(@"This build did not link PushKit"));
#endif
}

void com_codename1_impl_ios_IOSNative_callUnregisterVoipPush___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
#if defined(CN1_CALL_HAS_PUSHKIT) && defined(CN1_CALL_HAS_CALLKIT)
    if (cn1clRegistry != nil) {
        cn1clRegistry.desiredPushTypes = [NSSet set];
    }
    // Anything still waiting for credentials is failed rather than left
    // parked: with delivery switched off no callback can settle it, and the
    // invalidation path uses -1 and settles nothing. An AsyncResource that
    // never answers is the failure this SPI exists to prevent.
    NSArray *waiting = nil;
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        // The clear belongs in here with the drain, not above it: this runs
        // on a Java thread, and the token is read under this lock.
        cn1clVoipToken = nil;
        waiting = [NSArray arrayWithArray:cn1clTokenRequests];
        [cn1clTokenRequests removeAllObjects];
    }
    for (NSNumber *req in waiting) {
        com_codename1_impl_ios_IOSCallCallbacks_voipRegistrationFailed___int_int_java_lang_String(
                getThreadLocalData(), [req intValue],
                CN1_CALL_ERR_PUSH_UNAVAILABLE,
                cn1clJString(@"VoIP push was unregistered before the token"
                        @" arrived"));
    }
    // And Java is TOLD the token is gone, exactly as the invalidation
    // callback does. Clearing only the native copy left VoipPush.getToken()
    // answering with the revoked token, a newly installed listener replaying
    // it, and -- if a later registration produced the SAME value -- no
    // tokenChanged at all, because nothing had changed as far as Java could
    // see. An app following the documented listener flow would then never
    // re-register with its server. The -1 settles no request, which is right:
    // this is not an answer to anything, it is a state change.
    com_codename1_impl_ios_IOSCallCallbacks_voipToken___int_java_lang_String(
            getThreadLocalData(), -1, JAVA_NULL);
#endif
}

void com_codename1_impl_ios_IOSNative_callSetJavaReady___boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_BOOLEAN ready) {
#ifdef CN1_CALL_HAS_CALLKIT
    BOOL wasReady;
    BOOL nowReady;
    cn1clEnsureState();
    // Under the same lock the replay re-reads it through, so the two cannot
    // disagree about whether a listener is installed. Nothing is called out
    // to from inside, so this cannot deadlock against a delivery.
    @synchronized (cn1clLock) {
        wasReady = cn1clJavaReady;
        cn1clJavaReady = ready != JAVA_FALSE;
        nowReady = cn1clJavaReady;
    }
    if (nowReady && !wasReady) {
        // Replayed here as well as after the drain, because readiness no
        // longer implies a drain: an app that registers a Calls action
        // listener without ever touching VoipPush has nothing to drain, and
        // its held actions would otherwise wait until CallKit timed them out.
        //
        // Asynchronously, so the VoipPush path -- which calls this and then
        // drainPendingCalls on the same thread -- still replays AFTER the
        // drain has handed Java its sessions. The replay empties the queue
        // under the lock, so whichever runs first, nothing is delivered
        // twice.
        dispatch_async(dispatch_get_main_queue(), ^{
            cn1clReplayQueuedActions();
        });
    }
#endif
}

void com_codename1_impl_ios_IOSNative_callDrainPendingCalls___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
#ifdef CN1_CALL_HAS_CALLKIT
    cn1clDrain(requestId);
    // AFTER the drain, not on setJavaReady: an action held from the cold
    // start names a call the app has only just been handed, and delivering
    // answerRequested for a session Java has not adopted yet would arrive
    // for a call it does not know about.
    cn1clReplayQueuedActions();
#else
    com_codename1_impl_ios_IOSCallCallbacks_pendingCallsDrained___int_int(
            threadStateData, requestId, 0);
#endif
}

void com_codename1_impl_ios_IOSNative_callSetDirectorySource___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT filePath) {
#if defined(CN1_CALL_DIRECTORY) && defined(CN1_CALL_HAS_CALLKIT)
    // The extension runs in its own process and cannot see this one's memory,
    // so the data has to reach it through the shared App Group container. The
    // path handed in is inside the app's own home, so it is copied.
    NSString *src = toNSString(threadStateData, filePath);
    NSString *group = cn1clPlistString(@"CN1CallAppGroup", nil);
    if (group == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_DIRECTORY_FAILED,
                @"No App Group is configured for the call directory");
        return;
    }
    NSURL *container = [[NSFileManager defaultManager]
            containerURLForSecurityApplicationGroupIdentifier:group];
    if (container == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_DIRECTORY_FAILED,
                @"The configured App Group is not available to this app");
        return;
    }
    NSURL *dest = [container URLByAppendingPathComponent:@"cn1calldirectory.tsv"];
    // Staged, then swapped. The READER is the Call Directory extension in
    // its own process, and iOS starts it on its own schedule -- so deleting
    // the live file first gave it a window in which the file was missing,
    // which it reports as a successful load of ZERO entries and iOS then
    // drops every caller-ID and blocking record. A failed copy did the same
    // thing permanently, while setEntries answered that it had failed.
    NSURL *staging = [container URLByAppendingPathComponent:
            @"cn1calldirectory.tsv.new"];
    NSFileManager *files = [NSFileManager defaultManager];
    NSError *error = nil;
    [files removeItemAtURL:staging error:nil];
    [files copyItemAtURL:[NSURL fileURLWithPath:src] toURL:staging
                   error:&error];
    if (error != nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_DIRECTORY_FAILED,
                [error localizedDescription]);
        return;
    }
    // replaceItemAtURL is the atomic swap; with no file to replace it fails,
    // and a plain move is right because nothing can be reading what is not
    // there yet.
    if ([files fileExistsAtPath:[dest path]]) {
        error = nil;
        [files replaceItemAtURL:dest withItemAtURL:staging
                 backupItemName:nil options:0 resultingItemURL:nil
                          error:&error];
    } else {
        error = nil;
        [files moveItemAtURL:staging toURL:dest error:&error];
    }
    if (error != nil) {
        [files removeItemAtURL:staging error:nil];
        cn1clAck(requestId, NO, CN1_CALL_ERR_DIRECTORY_FAILED,
                [error localizedDescription]);
        return;
    }
    cn1clDirectoryPath = [[dest path] copy];
    cn1clAck(requestId, YES, 0, nil);
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED,
            @"This build has no call directory extension");
#endif
}

void com_codename1_impl_ios_IOSNative_callReloadDirectory___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
#if defined(CN1_CALL_DIRECTORY) && defined(CN1_CALL_HAS_CALLKIT)
    NSString *ext = cn1clPlistString(@"CN1CallDirectoryExtensionIdentifier", nil);
    if (ext == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_DIRECTORY_FAILED,
                @"No call directory extension identifier is configured");
        return;
    }
    [CXCallDirectoryManager.sharedInstance
            reloadExtensionWithIdentifier:ext
            completionHandler:^(NSError *error) {
        cn1clAck(requestId, error == nil, CN1_CALL_ERR_DIRECTORY_FAILED,
                error == nil ? nil : [error localizedDescription]);
    }];
#else
    cn1clAck(requestId, NO, CN1_CALL_ERR_NOT_SUPPORTED,
            @"This build has no call directory extension");
#endif
}

void com_codename1_impl_ios_IOSNative_callDirectoryStatus___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
#if defined(CN1_CALL_DIRECTORY) && defined(CN1_CALL_HAS_CALLKIT)
    NSString *ext = cn1clPlistString(@"CN1CallDirectoryExtensionIdentifier", nil);
    if (ext == nil) {
        com_codename1_impl_ios_IOSCallCallbacks_directoryStatus___int_java_lang_String(
                threadStateData, requestId,
                cn1clJString(cn1clJoin([NSArray arrayWithObjects:
                        @"0", @"-1", @"no extension configured", nil])));
        return;
    }
    [CXCallDirectoryManager.sharedInstance
            getEnabledStatusForExtensionWithIdentifier:ext
            completionHandler:^(CXCallDirectoryEnabledStatus status, NSError *error) {
        // Enabled is the user's decision in Settings, and it is OFF by
        // default -- an app whose numbers never appear has usually not been
        // enabled rather than failed.
        NSString *wire = cn1clJoin([NSArray arrayWithObjects:
                status == CXCallDirectoryEnabledStatusEnabled ? @"1" : @"0",
                @"-1",
                error == nil ? @"ok" : [error localizedDescription], nil]);
        com_codename1_impl_ios_IOSCallCallbacks_directoryStatus___int_java_lang_String(
                getThreadLocalData(), requestId, cn1clJString(wire));
    }];
#else
    com_codename1_impl_ios_IOSCallCallbacks_directoryStatus___int_java_lang_String(
            threadStateData, requestId,
            cn1clJString(@"0\t-1\tunsupported"));
#endif
}

#else /* CN1_INCLUDE_CALL */

// ---------------------------------------------------------------------
// The unsupported half.
//
// Every symbol above is defined here too, answering "not supported". A build
// that never referenced com.codename1.call links byte-identically to one that
// did except for these bodies -- and, crucially, the Java methods survive the
// dead-code pass, which keeps them BY their symbol appearing in a native
// source.
// ---------------------------------------------------------------------

#include "com_codename1_impl_ios_IOSCallCallbacks.h"
#import "java_lang_String.h"

extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString *str);

static JAVA_OBJECT cn1clOffString(NSString *s) {
    return s == nil ? JAVA_NULL : fromNSString(getThreadLocalData(), s);
}

static void cn1clOffAck(int requestId) {
    com_codename1_impl_ios_IOSCallCallbacks_ack___int_boolean_int_java_lang_String(
            getThreadLocalData(), requestId, JAVA_FALSE,
            CN1_CALL_ERR_NOT_SUPPORTED,
            cn1clOffString(@"This build did not include com.codename1.call"));
}

void cn1CallInstallPushRegistry(void) {
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_callSupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_callVoipSupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_callDirectorySupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return JAVA_FALSE;
}

JAVA_INT com_codename1_impl_ios_IOSNative_callCapabilities___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return 0;
}

JAVA_INT com_codename1_impl_ios_IOSNative_callAvailability___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return CN1_CALL_AVAIL_UNSUPPORTED;
}

JAVA_INT com_codename1_impl_ios_IOSNative_callGrantedPermissions___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return 0;
}

void com_codename1_impl_ios_IOSNative_callRequestPermissions___int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_INT permissionBits) {
    com_codename1_impl_ios_IOSCallCallbacks_permissionResult___int_int(
            threadStateData, requestId, 0);
}

void com_codename1_impl_ios_IOSNative_callConfigureProvider___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT configWire) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callReportIncoming___int_java_lang_String_java_lang_String_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_OBJECT handleWire,
        JAVA_OBJECT displayName, JAVA_BOOLEAN hasVideo) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callReportOutgoing___int_java_lang_String_java_lang_String_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_OBJECT handleWire,
        JAVA_OBJECT displayName, JAVA_BOOLEAN hasVideo) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callStartedConnecting___java_lang_String_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_LONG timestampMs) {
}

void com_codename1_impl_ios_IOSNative_callOutgoingConnected___java_lang_String_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_LONG timestampMs) {
}

void com_codename1_impl_ios_IOSNative_callIncomingConnected___java_lang_String_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_LONG timestampMs) {
}

void com_codename1_impl_ios_IOSNative_callUpdate___java_lang_String_java_lang_String_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_OBJECT handleWire, JAVA_OBJECT displayName,
        JAVA_BOOLEAN hasVideo) {
}

void com_codename1_impl_ios_IOSNative_callReportEnded___java_lang_String_int_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_OBJECT callId, JAVA_INT endReasonOrdinal, JAVA_LONG timestampMs) {
}

void com_codename1_impl_ios_IOSNative_callEnd___int_java_lang_String_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_INT endReasonOrdinal) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callSetHeld___int_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_BOOLEAN held) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callSetMuted___int_java_lang_String_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_BOOLEAN muted) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callSendDtmf___int_java_lang_String_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_OBJECT digits) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callSetGroup___int_java_lang_String_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId, JAVA_OBJECT otherCallId) {
    cn1clOffAck(requestId);
}

JAVA_INT com_codename1_impl_ios_IOSNative_callAudioRoute___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return CN1_CALL_ROUTE_UNKNOWN;
}

void com_codename1_impl_ios_IOSNative_callSetAudioRoute___int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_INT routeOrdinal) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callShowRoutePicker___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT callId) {
    cn1clOffAck(requestId);
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_callCompleteAction___long_boolean_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_LONG actionToken, JAVA_BOOLEAN fulfilled) {
    return JAVA_TRUE;
}

void com_codename1_impl_ios_IOSNative_callRegisterVoipPush___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
    com_codename1_impl_ios_IOSCallCallbacks_voipRegistrationFailed___int_int_java_lang_String(
            threadStateData, requestId, CN1_CALL_ERR_NOT_SUPPORTED,
            cn1clOffString(@"This build did not include com.codename1.call"));
}

void com_codename1_impl_ios_IOSNative_callUnregisterVoipPush___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
}

void com_codename1_impl_ios_IOSNative_callSetJavaReady___boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_BOOLEAN ready) {
}

void com_codename1_impl_ios_IOSNative_callDrainPendingCalls___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
    com_codename1_impl_ios_IOSCallCallbacks_pendingCallsDrained___int_int(
            threadStateData, requestId, 0);
}

void com_codename1_impl_ios_IOSNative_callSetDirectorySource___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT filePath) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callReloadDirectory___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
    cn1clOffAck(requestId);
}

void com_codename1_impl_ios_IOSNative_callDirectoryStatus___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
    com_codename1_impl_ios_IOSCallCallbacks_directoryStatus___int_java_lang_String(
            threadStateData, requestId, cn1clOffString(@"0\t-1\tunsupported"));
}

#endif /* CN1_INCLUDE_CALL */
