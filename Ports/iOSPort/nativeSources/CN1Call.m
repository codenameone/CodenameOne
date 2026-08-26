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
/// The actions this app submitted itself, as "<uuid>|<CXAction class>".
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
static NSMutableSet *cn1clJavaStarts = nil;

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
        cn1clJavaStarts = [[NSMutableSet alloc] init];
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
        cn1clAudioRetiring = cn1clAudioCall;
        cn1clAudioCall = nil;
    }
}

/// Holds an action until Java is listening, answering whether it did.
///
/// The block is what will run at replay time; nothing is fulfilled or failed
/// here, because CallKit's own timeout is the only honest deadline and the
/// app is expected to be listening within a fraction of it.
static BOOL cn1clHoldUntilReady(void (^deliver)(void)) {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        if (cn1clJavaReady) {
            return NO;
        }
        [cn1clQueuedActions addObject:[deliver copy]];
        return YES;
    }
}

/// Replays everything held while the app was not listening.
static void cn1clReplayQueuedActions(void) {
    NSArray *held = nil;
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        held = [NSArray arrayWithArray:cn1clQueuedActions];
        [cn1clQueuedActions removeAllObjects];
    }
    for (void (^deliver)(void) in held) {
        deliver();
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
        if (![cn1clJavaStarts containsObject:key]) {
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

/// Records the call the audio session is about to belong to.
static void cn1clOwnAudio(NSString *uuidString) {
    cn1clEnsureState();
    @synchronized (cn1clLock) {
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
            return cn1clAudioCall;
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
            NSString *retiring = cn1clAudioRetiring;
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
    @synchronized (cn1clLock) {
        [cn1clCalls removeAllObjects];
        [cn1clActions removeAllObjects];
        [cn1clJavaStarts removeAllObjects];
        [cn1clSystemStarts removeAllObjects];
        cn1clAudioCall = nil;
        cn1clAudioRetiring = nil;
    }
    com_codename1_impl_ios_IOSCallCallbacks_providerReset__(getThreadLocalData());
}

- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action {
    cn1clOwnAudio([action.callUUID UUIDString]);
    // Held when the app has no listener yet: a pushed call rings before any
    // of its code runs, and the user can answer it there and then.
    if (cn1clHoldUntilReady(^{
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
    if (cn1clHoldUntilReady(^{
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
    if (cn1clHoldUntilReady(^{
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
    if (cn1clHoldUntilReady(^{
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
    if (cn1clHoldUntilReady(^{
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
static CXProviderConfiguration *cn1clConfiguration(void) {
    NSString *name = cn1clPlistString(@"CN1CallProviderName",
            cn1clPlistString(@"CFBundleDisplayName",
                    cn1clPlistString(@"CFBundleName", @"Codename One")));
    CXProviderConfiguration *cfg;
#if defined(__IPHONE_14_0)
    if (@available(iOS 14.0, *)) {
        cfg = [[CXProviderConfiguration alloc] init];
    } else {
        cfg = [[CXProviderConfiguration alloc] initWithLocalizedName:name];
    }
#else
    cfg = [[CXProviderConfiguration alloc] initWithLocalizedName:name];
#endif
    cfg.supportsVideo = cn1clPlistBool(@"CN1CallSupportsVideo", NO);
    cfg.includesCallsInRecents = cn1clPlistBool(@"CN1CallIncludesCallsInRecents", YES);
    cfg.maximumCallGroups = 1;
    cfg.maximumCallsPerCallGroup = 1;
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
    return [[CXHandle alloc] initWithType:t value:value];
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    if (uuid == nil) {
        if (requestId >= 0) {
            cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID,
                    @"Not a canonical call id");
        }
        return;
    }
    CXCallUpdate *update = [[CXCallUpdate alloc] init];
    update.remoteHandle = cn1clHandleFromWire(handleWire);
    if ([displayName length] > 0) {
        update.localizedCallerName = displayName;
    }
    update.hasVideo = hasVideo;
    update.supportsHolding = YES;
    update.supportsDTMF = YES;
    update.supportsGrouping = NO;
    update.supportsUngrouping = NO;

    BOOL known = NO;
    BOOL stillPending = NO;
    @synchronized (cn1clLock) {
        known = [cn1clCalls objectForKey:uuidString] != nil;
        stillPending = [cn1clReporting containsObject:uuidString];
        if (known && requestId >= 0 && stillPending) {
            // The first report has not heard back from CallKit yet, so
            // acknowledging this one as accepted would be a guess -- and if
            // the original is then filtered or refused, its completion drops
            // the uuid while Java holds a session with no system call behind
            // it. Wait and share the original's answer instead.
            NSMutableArray *waiters = [cn1clReportWaiters objectForKey:uuidString];
            if (waiters == nil) {
                waiters = [NSMutableArray array];
                [cn1clReportWaiters setObject:waiters forKey:uuidString];
            }
            [waiters addObject:[NSNumber numberWithInt:requestId]];
        }
    }
    if (known) {
        // Already ringing -- the other origin got here first.
        [cn1clEnsureProvider() reportCallWithUUID:uuid updated:update];
        if (requestId >= 0 && !stillPending) {
            cn1clAck(requestId, YES, 0, nil);
        }
        return;
    }
    @synchronized (cn1clLock) {
        [cn1clCalls setObject:uuidString forKey:uuidString];
        [cn1clReporting addObject:uuidString];
    }
    [cn1clEnsureProvider() reportNewIncomingCallWithUUID:uuid update:update
            completion:^(NSError *error) {
        NSArray *waiters = nil;
        @synchronized (cn1clLock) {
            [cn1clReporting removeObject:uuidString];
            waiters = [cn1clReportWaiters objectForKey:uuidString];
            [cn1clReportWaiters removeObjectForKey:uuidString];
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
        [cn1clPending addObject:rec];
        if (uuid != nil && !stale) {
            [cn1clUnclaimed addObject:uuid];
        }
    }
}

static void cn1clDrain(int requestId) {
    cn1clEnsureState();
    NSArray *batch;
    @synchronized (cn1clLock) {
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
        NSUUID *parsed = [[NSUUID alloc] initWithUUIDString:(NSString *)raw];
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
    cn1clVoipToken = [hex copy];
    // Delivered EVERY time, not only when a register() is waiting. APNs
    // rotates a VoIP token while the app stays installed, and the rotation
    // used to update this variable and stop -- so the app's server kept the
    // old token and incoming calls quietly stopped arriving. A requestId of
    // -1 settles nothing and still reaches the tokenChanged listener, which
    // is what the rotation case needs.
    NSArray *waiting = nil;
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        waiting = [NSArray arrayWithArray:cn1clTokenRequests];
        [cn1clTokenRequests removeAllObjects];
    }
    if ([waiting count] == 0) {
        // A rotation with nobody waiting. A requestId of -1 settles nothing
        // and still reaches the tokenChanged listener, which is what this
        // case needs.
        com_codename1_impl_ios_IOSCallCallbacks_voipToken___int_java_lang_String(
                getThreadLocalData(), -1, cn1clJString(cn1clVoipToken));
        return;
    }
    // One settlement per waiting registration. deliverToken tells the
    // listener only when the value actually changed, so several of these
    // announce one rotation.
    for (NSNumber *req in waiting) {
        com_codename1_impl_ios_IOSCallCallbacks_voipToken___int_java_lang_String(
                getThreadLocalData(), [req intValue],
                cn1clJString(cn1clVoipToken));
    }
}

- (void)pushRegistry:(PKPushRegistry *)registry
        didInvalidatePushTokenForType:(PKPushType)type {
    cn1clVoipToken = nil;
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
        CXCallUpdate *update = [[CXCallUpdate alloc] init];
        update.remoteHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric
                                                      value:@" "];
        [cn1clEnsureProvider() reportNewIncomingCallWithUUID:
                [[NSUUID alloc] initWithUUIDString:uuid] update:update
                completion:^(NSError *error) {
            [cn1clEnsureProvider() reportCallWithUUID:
                    [[NSUUID alloc] initWithUUIDString:uuid]
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
        NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
        if (uuid != nil) {
            id reason = [call objectForKey:@"reason"];
            CXCallEndedReason r = CXCallEndedReasonRemoteEnded;
            if ([reason respondsToSelector:@selector(intValue)]
                    && [reason intValue] == CN1_CALL_END_UNANSWERED) {
                r = CXCallEndedReasonUnanswered;
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
            NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
            [cn1clEnsureProvider() reportCallWithUUID:uuid endedAtDate:nil
                    reason:CXCallEndedReasonUnanswered];
            @synchronized (cn1clLock) {
                [cn1clCalls removeObjectForKey:uuidString];
                cn1clDropAudioLocked(uuidString);
                [cn1clUnclaimed removeObject:uuidString];
            }
        }
    });

    if (cn1clJavaReady) {
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
            | CN1_CALL_CAP_HOLD | CN1_CALL_CAP_MUTE | CN1_CALL_CAP_DTMF
            | CN1_CALL_CAP_VIDEO;
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
    CXCallObserver *observer = [[CXCallObserver alloc] init];
    for (CXCall *c in observer.calls) {
        BOOL mine = NO;
        @synchronized (cn1clLock) {
            mine = cn1clCalls != nil
                    && [cn1clCalls objectForKey:[c.UUID UUIDString]] != nil;
        }
        if (!mine && !c.hasEnded) {
            return CN1_CALL_AVAIL_OTHER_APP;
        }
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
        if (!wantsCamera) {
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
            cfg = [[CXProviderConfiguration alloc] initWithLocalizedName:name];
            cfg.ringtoneSound = cn1clPlistString(@"CN1CallRingtoneSound", nil);
        }
    }
    cfg.supportsVideo = [cn1clField(cfgFields, 1) isEqualToString:@"1"];
    cfg.includesCallsInRecents =
            [cn1clField(cfgFields, 2) isEqualToString:@"1"];
    NSInteger groups = [cn1clField(cfgFields, 3) integerValue];
    NSInteger perGroup = [cn1clField(cfgFields, 4) integerValue];
    cfg.maximumCallGroups = groups > 0 ? groups : 1;
    cfg.maximumCallsPerCallGroup = perGroup > 0 ? perGroup : 1;
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
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
    CXStartCallAction *action = [[CXStartCallAction alloc]
            initWithCallUUID:uuid
            handle:cn1clHandleFromWire(toNSString(threadStateData, handleWire))];
    action.video = hasVideo != JAVA_FALSE;
    if (displayName != JAVA_NULL) {
        action.contactIdentifier = toNSString(threadStateData, displayName);
    }
    cn1clEnsureProvider();
    // Marked BEFORE the transaction is requested: CallKit may dispatch the
    // action to performStartCallAction before this call returns, and the
    // delegate uses this to tell an app-originated start from a system one.
    cn1clClaimOwn(uuidString, [CXStartCallAction class]);
    [cn1clController requestTransaction:[[CXTransaction alloc] initWithAction:action]
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)];
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)];
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)];
    if (uuid == nil) {
        return;
    }
    CXCallUpdate *update = [[CXCallUpdate alloc] init];
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    BOOL known = NO;
    @synchronized (cn1clLock) {
        known = cn1clCalls != nil && [cn1clCalls objectForKey:uuidString] != nil;
    }
    if (uuid == nil || !known) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID,
                @"No such call");
        return;
    }
    CXEndCallAction *action = [[CXEndCallAction alloc] initWithCallUUID:uuid];
    cn1clClaimOwn(uuidString, [CXEndCallAction class]);
    [cn1clController requestTransaction:[[CXTransaction alloc] initWithAction:action]
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)];
    if (uuid == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID, @"No such call");
        return;
    }
    CXSetHeldCallAction *action = [[CXSetHeldCallAction alloc]
            initWithCallUUID:uuid onHold:held != JAVA_FALSE];
    NSString *heldUuid = [uuid UUIDString];
    cn1clClaimOwn(heldUuid, [CXSetHeldCallAction class]);
    [cn1clController requestTransaction:[[CXTransaction alloc] initWithAction:action]
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)];
    if (uuid == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID, @"No such call");
        return;
    }
    CXSetMutedCallAction *action = [[CXSetMutedCallAction alloc]
            initWithCallUUID:uuid muted:muted != JAVA_FALSE];
    NSString *mutedUuid = [uuid UUIDString];
    cn1clClaimOwn(mutedUuid, [CXSetMutedCallAction class]);
    [cn1clController requestTransaction:[[CXTransaction alloc] initWithAction:action]
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
    NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:
            toNSString(threadStateData, callId)];
    if (uuid == nil) {
        cn1clAck(requestId, NO, CN1_CALL_ERR_INVALID_ID, @"No such call");
        return;
    }
    CXPlayDTMFCallAction *action = [[CXPlayDTMFCallAction alloc]
            initWithCallUUID:uuid
            digits:toNSString(threadStateData, digits)
            type:CXPlayDTMFCallActionTypeSingleTone];
    [cn1clController requestTransaction:[[CXTransaction alloc] initWithAction:action]
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

void com_codename1_impl_ios_IOSNative_callCompleteAction___long_boolean(
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
    if (action == nil) {
        return;
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
    if (cn1clVoipToken != nil) {
        com_codename1_impl_ios_IOSCallCallbacks_voipToken___int_java_lang_String(
                threadStateData, requestId, cn1clJString(cn1clVoipToken));
        return;
    }
    // The token arrives asynchronously; park the request for the delegate.
    cn1clEnsureState();
    @synchronized (cn1clLock) {
        [cn1clTokenRequests addObject:[NSNumber numberWithInt:requestId]];
    }
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
    cn1clVoipToken = nil;
    // Anything still waiting for credentials is failed rather than left
    // parked: with delivery switched off no callback can settle it, and the
    // invalidation path uses -1 and settles nothing. An AsyncResource that
    // never answers is the failure this SPI exists to prevent.
    NSArray *waiting = nil;
    cn1clEnsureState();
    @synchronized (cn1clLock) {
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
#endif
}

void com_codename1_impl_ios_IOSNative_callSetJavaReady___boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_BOOLEAN ready) {
#ifdef CN1_CALL_HAS_CALLKIT
    cn1clJavaReady = ready != JAVA_FALSE;
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

void com_codename1_impl_ios_IOSNative_callCompleteAction___long_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_LONG actionToken, JAVA_BOOLEAN fulfilled) {
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
