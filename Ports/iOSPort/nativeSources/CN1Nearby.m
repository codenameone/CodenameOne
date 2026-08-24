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
#import "CN1Nearby.h"

#ifdef CN1_INCLUDE_NEARBY

#include "com_codename1_impl_ios_IOSNearbyCallbacks.h"
#import "java_lang_String.h"

#if defined(CN1_NEARBY_RANGING) && __has_include(<NearbyInteraction/NearbyInteraction.h>)
#import <NearbyInteraction/NearbyInteraction.h>
#import <simd/simd.h>
#import <math.h>
#define CN1_NEARBY_HAS_NI 1
#endif

#if defined(CN1_NEARBY_TRANSPORT) \
        && __has_include(<MultipeerConnectivity/MultipeerConnectivity.h>)
#import <MultipeerConnectivity/MultipeerConnectivity.h>
#define CN1_NEARBY_HAS_MPC 1
#endif

#if defined(CN1_NEARBY_COMPANION) \
        && __has_include(<AccessorySetupKit/AccessorySetupKit.h>)
#import <AccessorySetupKit/AccessorySetupKit.h>
// ASDiscoveryDescriptor.bluetoothServiceUUID is a CBUUID, so the companion
// half links CoreBluetooth. That is only the type -- no scanning happens here,
// and the point of AccessorySetupKit is precisely that the app does not need
// the blanket Bluetooth authorization to talk to what the user picked.
#import <CoreBluetooth/CoreBluetooth.h>
#define CN1_NEARBY_HAS_ASK 1
#endif

// ---------------------------------------------------------------------
// Three frameworks, three lifetimes, one bridge
//
// Nearby Interaction, MultipeerConnectivity and AccessorySetupKit share
// nothing but this file. Each is compiled in only when its own define is on,
// and each is also guarded with __has_include so an older Xcode that has never
// heard of AccessorySetupKit still builds the other two rather than failing
// the whole app.
//
// Everything below is manual retain/release, like CN1Bluetooth.m beside it.
// Blocks that outlive their call -- the MultipeerConnectivity invitation
// handler is the only one -- are copied on the way into a dictionary and
// released when they are answered.
//
// Threads: NI, MPC and ASK all call back on queues of their own, and under
// ParparVM none of them is the Codename One EDT. Nothing here hops; the
// callbacks forward straight to IOSNearbyCallbacks, which forwards to the
// public facades, and those own EDT dispatch. That is the same arrangement
// CN1SmartHome.m uses and the reason it is safe.
// ---------------------------------------------------------------------

// Declared per translation unit, as CN1Bluetooth.m and CN1Camera.m do: these
// live in IOSNative.m and no shared header exports them, so a file that uses
// one without saying so compiles with an implicit declaration and then reads
// its result out of the wrong register.
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString *str);
extern NSString *toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT nsDataToByteArr(NSData *data);

static JAVA_OBJECT cn1nbJString(NSString *s) {
    return s == nil ? JAVA_NULL : fromNSString(getThreadLocalData(), s);
}

static JAVA_OBJECT cn1nbJBytes(NSData *d) {
    return d == nil ? JAVA_NULL : nsDataToByteArr(d);
}

static NSData *cn1nbDataFromJavaArray(JAVA_OBJECT arr) {
    if (arr == JAVA_NULL) {
        return nil;
    }
    JAVA_ARRAY a = (JAVA_ARRAY)arr;
    if (a->length <= 0) {
        return [NSData data];
    }
    return [NSData dataWithBytes:a->data length:a->length];
}

/// Replaces the characters a tab-delimited record cannot carry, exactly as
/// NearbyWire.sanitize does on the Java side. A device whose name contains a
/// tab would otherwise shift every field after it.
static NSString *cn1nbSanitize(NSString *s) {
    if (s == nil) {
        return @"";
    }
    NSString *out = [s stringByReplacingOccurrencesOfString:@"\t"
                                                 withString:@" "];
    out = [out stringByReplacingOccurrencesOfString:@"\n" withString:@" "];
    return [out stringByReplacingOccurrencesOfString:@"\r" withString:@" "];
}

static NSString *cn1nbJoin(NSArray *fields) {
    NSMutableArray *safe = [NSMutableArray arrayWithCapacity:[fields count]];
    for (NSString *f in fields) {
        [safe addObject:cn1nbSanitize(f)];
    }
    return [safe componentsJoinedByString:@"\t"];
}

static NSArray *cn1nbSplitLines(NSString *joined) {
    if (joined == nil || [joined length] == 0) {
        return [NSArray array];
    }
    return [joined componentsSeparatedByString:@"\n"];
}

/// How long a ranging start is given to fail before it is called a success.
#define CN1_NEARBY_RANGING_GRACE_NS (500ull * NSEC_PER_MSEC)

static void cn1nbFailRanging(int requestId, int error, NSString *message) {
    com_codename1_impl_ios_IOSNearbyCallbacks_rangingFailed___int_int_java_lang_String(
            getThreadLocalData(), requestId, error, cn1nbJString(message));
}

static void cn1nbFailCompanion(int requestId, int error, NSString *message) {
    com_codename1_impl_ios_IOSNearbyCallbacks_companionFailed___int_int_java_lang_String(
            getThreadLocalData(), requestId, error, cn1nbJString(message));
}

static void cn1nbFailTransport(int requestId, int error, NSString *message) {
    com_codename1_impl_ios_IOSNearbyCallbacks_transportFailed___int_int_java_lang_String(
            getThreadLocalData(), requestId, error, cn1nbJString(message));
}

/// True when an error describes a transfer this app cancelled.
///
/// MultipeerConnectivity reports a cancelled resource through the same
/// completion handler as a broken one, so the error is all there is to go on.
static BOOL cn1nbWasCancelled(NSError *error) {
    if (error == nil) {
        return NO;
    }
    if ([[error domain] isEqualToString:NSCocoaErrorDomain]
            && [error code] == NSUserCancelledError) {
        return YES;
    }
    // NSProgress cancellation surfaces as POSIX ECANCELED on some releases.
    if ([[error domain] isEqualToString:NSPOSIXErrorDomain]
            && [error code] == ECANCELED) {
        return YES;
    }
    return NO;
}

static void cn1nbTransportOk(int requestId) {
    com_codename1_impl_ios_IOSNearbyCallbacks_transportOk___int(
            getThreadLocalData(), requestId);
}

/// How long a start is given to fail before it is called a success.
///
/// MultipeerConnectivity accepts startAdvertisingPeer and
/// startBrowsingForPeers synchronously and rejects them later, through
/// didNotStartAdvertisingPeer / didNotStartBrowsingForPeers -- an unavailable
/// radio, a service type it will not take. Answering the caller straight away
/// meant the AsyncResource had already resolved true by the time the refusal
/// arrived, so the refusal had nowhere to go and the app was left believing
/// it was advertising. The refusals that do come, come immediately; half a
/// second is long enough to catch them and short enough not to be felt.
#define CN1_NEARBY_START_GRACE_NS (500ull * NSEC_PER_MSEC)

// =====================================================================
// Ranging -- Nearby Interaction
// =====================================================================

#ifdef CN1_NEARBY_HAS_NI

API_AVAILABLE(ios(14.0))
@interface CN1NearbyRangingSession : NSObject <NISessionDelegate>
@property (nonatomic, assign) int handle;
@property (nonatomic, assign) int pendingStartRequest;
@property (nonatomic, retain) NISession *session;
- (void)settleStarted;
@end

static NSMutableDictionary *cn1nbSessions = nil;
/// Guards cn1nbSessions.
///
/// NISession delivers on a queue per session, so two peers invalidating at
/// once -- or an app calling stop() while an invalidation is running -- had
/// concurrent readers, writers and removals on a dictionary that is not
/// thread-safe. A separate object rather than the dictionary itself, because
/// the dictionary is created lazily and there would be nothing to lock on
/// before the first session.
static NSObject *cn1nbSessionsLock = nil;

/// Creates the lock, on the one thread that can be first: every entry point
/// below reaches this before touching the registry.
static void cn1nbSessionsLockInit(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1nbSessionsLock = [[NSObject alloc] init];
    });
}

static void cn1nbSessionsInit(void) {
    cn1nbSessionsLockInit();
    if (cn1nbSessions == nil) {
        cn1nbSessions = [[NSMutableDictionary alloc] init];
    }
}

@implementation CN1NearbyRangingSession

- (void)dealloc {
    [_session release];
    [super dealloc];
}

/// Folds Apple's unit direction vector into the azimuth and elevation the
/// portable API reports.
///
/// The frame is x right, y up, z toward the user, so forward is negative z --
/// which is why the azimuth is atan2(x, -z) and not atan2(x, z). Android
/// reports these two angles directly, and this is the conversion that makes
/// the same code read the same on both.
- (void)deliver:(NINearbyObject *)object {
    // Both of these are plain scalars in Objective-C, NOT nullable objects as
    // they are in Swift, and "not available" is signalled in-band: the
    // distance and every component of the direction come back NaN. Testing
    // them for nil would compile and always take the has-a-value branch, and
    // the app would render an arrow pointing at NaN.
    float rawDistance = object.distance;
    simd_float3 d = object.direction;
    JAVA_BOOLEAN hasDistance =
            (!isnan(rawDistance) && rawDistance >= 0
                    && rawDistance != NINearbyObjectDistanceNotAvailable)
            ? JAVA_TRUE : JAVA_FALSE;
    JAVA_DOUBLE distance = hasDistance == JAVA_TRUE ? rawDistance : 0;
    JAVA_BOOLEAN hasDirection = JAVA_FALSE;
    JAVA_DOUBLE azimuth = 0;
    JAVA_DOUBLE elevation = 0;
    JAVA_FLOAT x = 0;
    JAVA_FLOAT y = 0;
    JAVA_FLOAT z = 0;
    if (!isnan(d.x) && !isnan(d.y) && !isnan(d.z)) {
        x = d.x;
        y = d.y;
        z = d.z;
        hasDirection = JAVA_TRUE;
        azimuth = atan2f(d.x, -d.z) * 180.0f / (float)M_PI;
        float clamped = d.y < -1.0f ? -1.0f : (d.y > 1.0f ? 1.0f : d.y);
        elevation = asinf(clamped) * 180.0f / (float)M_PI;
    }
    com_codename1_impl_ios_IOSNearbyCallbacks_rangingUpdate___int_boolean_double_boolean_double_boolean_double_boolean_float_float_float(
            getThreadLocalData(), self.handle, hasDistance, distance,
            hasDirection, azimuth, hasDirection, elevation, hasDirection,
            x, y, z);
}

/// Answers a peer-ranging start that is still waiting.
///
/// runWithConfiguration takes the configuration and reports a refusal
/// asynchronously through didInvalidateWithError -- the user declining Nearby
/// Interaction, the active-session limit -- so answering the caller straight
/// after that call said "ranging started" for a session that never measured
/// anything. Whichever comes first answers it: the first update (it really is
/// measuring), an invalidation (the branch below already fails it), or the
/// grace timer, which is the backstop for a session that starts cleanly and
/// simply has no peer in range yet.
- (void)settleStarted {
    int pending = self.pendingStartRequest;
    if (pending == 0) {
        return;
    }
    self.pendingStartRequest = 0;
    com_codename1_impl_ios_IOSNearbyCallbacks_sessionStarted___int_int(
            getThreadLocalData(), pending, self.handle);
}

- (void)session:(NISession *)session
        didUpdateNearbyObjects:(NSArray<NINearbyObject *> *)nearbyObjects {
    @autoreleasepool {
        [self settleStarted];
        for (NINearbyObject *o in nearbyObjects) {
            [self deliver:o];
        }
    }
}

- (void)session:(NISession *)session
        didRemoveNearbyObjects:(NSArray<NINearbyObject *> *)nearbyObjects
        withReason:(NINearbyObjectRemovalReason)reason {
    @autoreleasepool {
        int mapped = reason == NINearbyObjectRemovalReasonPeerEnded
                ? CN1_NEARBY_REMOVED_PEER_ENDED : CN1_NEARBY_REMOVED_TIMEOUT;
        com_codename1_impl_ios_IOSNearbyCallbacks_peerRemoved___int_int(
                getThreadLocalData(), self.handle, mapped);
    }
}

- (void)sessionWasSuspended:(NISession *)session {
    @autoreleasepool {
        com_codename1_impl_ios_IOSNearbyCallbacks_sessionSuspended___int(
                getThreadLocalData(), self.handle);
    }
}

- (void)sessionSuspensionEnded:(NISession *)session {
    @autoreleasepool {
        // Apple requires the configuration to be run again after a
        // suspension; the session does not resume by itself. Doing it here
        // rather than making the app do it is what lets the portable API
        // promise that a resumed session starts measuring again.
        if (session.configuration != nil) {
            [session runWithConfiguration:session.configuration];
        }
        com_codename1_impl_ios_IOSNearbyCallbacks_sessionResumed___int(
                getThreadLocalData(), self.handle);
    }
}

- (void)session:(NISession *)session didInvalidateWithError:(NSError *)error {
    @autoreleasepool {
        int code = CN1_NEARBY_ERR_SESSION_INVALIDATED;
        if (@available(iOS 14.0, *)) {
            if (error.code == NIErrorCodeUserDidNotAllow) {
                code = CN1_NEARBY_ERR_UNAUTHORIZED;
            } else if (error.code == NIErrorCodeResourceUsageTimeout) {
                code = CN1_NEARBY_ERR_TIMEOUT;
            }
        }
        int handle = self.handle;
        int pending = self.pendingStartRequest;
        self.pendingStartRequest = 0;
        if (pending != 0) {
            // A session that dies before its start request was answered has
            // a caller holding a resource that would otherwise never settle.
            cn1nbFailRanging(pending, code, [error localizedDescription]);
        }
        com_codename1_impl_ios_IOSNearbyCallbacks_sessionInvalidated___int_int_java_lang_String(
                getThreadLocalData(), handle, code,
                cn1nbJString([error localizedDescription]));
        cn1nbSessionsLockInit();
        @synchronized (cn1nbSessionsLock) {
            [cn1nbSessions removeObjectForKey:
                    [NSNumber numberWithInt:handle]];
        }
    }
}

- (void)session:(NISession *)session
        didGenerateShareableConfigurationData:(NSData *)shareableConfigurationData
        forObject:(NINearbyObject *)object API_AVAILABLE(ios(16.0)) {
    @autoreleasepool {
        int pending = self.pendingStartRequest;
        if (pending == 0) {
            return;
        }
        self.pendingStartRequest = 0;
        com_codename1_impl_ios_IOSNearbyCallbacks_accessoryConfiguration___int_int_byte_1ARRAY(
                getThreadLocalData(), pending, self.handle,
                cn1nbJBytes(shareableConfigurationData));
    }
}

@end

/// The session for a handle, retained past any removal the caller performs.
///
/// The registry is normally the ONLY owner: the entry was created
/// autoreleased and the pool it was created in drained long ago. So a caller
/// that looks the entry up, removes it, and then messages it -- which is
/// exactly what stopping a session does -- was messaging freed memory. The
/// lock added for the registry serialises the dictionary; it does nothing for
/// the lifetime of what comes out of it, which is a separate problem with the
/// same shape as the MCSession and invitation-block ones.
static CN1NearbyRangingSession *cn1nbSessionFor(int handle)
        API_AVAILABLE(ios(14.0)) {
    cn1nbSessionsInit();
    @synchronized (cn1nbSessionsLock) {
        return [[[cn1nbSessions objectForKey:[NSNumber numberWithInt:handle]]
                retain] autorelease];
    }
}

/// Answers a peer-ranging start once the session has had its chance to fail.
///
/// A second answer is harmless: the Java side takes the pending request out
/// of its map, so whichever of this, the first update, and an invalidation
/// arrives first wins and the others are dropped.
static void cn1nbSettleRangingStart(CN1NearbyRangingSession *entry)
        API_AVAILABLE(ios(14.0)) {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                    (int64_t)CN1_NEARBY_RANGING_GRACE_NS),
            dispatch_get_main_queue(), ^{
        @autoreleasepool {
            [entry settleStarted];
        }
    });
}

#endif // CN1_NEARBY_HAS_NI

// =====================================================================
// Transport -- MultipeerConnectivity
// =====================================================================

#ifdef CN1_NEARBY_HAS_MPC

/// How long a sent byte payload waits for the receiver's acknowledgement.
///
/// The acknowledgement is what turns a queued send into SUCCESS, and it is
/// itself an ordinary reliable send on the far side -- it can fail to leave
/// the receiver without the session going down, and the frame can be lost
/// with the peer still connected. Either way nothing would arrive here and
/// the send would never reach a terminal status at all, which is worse than
/// reporting it late: an app waiting on that update waits forever.
///
/// Thirty seconds is far longer than the round trip for a payload small
/// enough to travel in one frame, so a timeout means something really is
/// wrong rather than that the link is slow.
#define CN1_NEARBY_ACK_TIMEOUT_NS (30ull * NSEC_PER_SEC)

@interface CN1NearbyTransport : NSObject <MCSessionDelegate,
        MCNearbyServiceAdvertiserDelegate, MCNearbyServiceBrowserDelegate>
@property (nonatomic, retain) MCPeerID *localPeer;
/// One MCSession PER PEER, keyed by endpoint id.
///
/// MCSession has no per-peer disconnect -- `disconnect` tears the whole thing
/// down -- so a single shared session made NearbyTransport.disconnect(endpoint)
/// impossible to honour once two peers were connected: it either dropped
/// everyone or, as it did, silently did nothing. A session per peer is the
/// arrangement MultipeerConnectivity actually supports for that, and it costs
/// only the dictionary: MCSession is cheap and the delegate is shared.
@property (nonatomic, retain) NSMutableDictionary *sessionsById;
@property (nonatomic, retain) MCNearbyServiceAdvertiser *advertiser;
@property (nonatomic, retain) MCNearbyServiceBrowser *browser;
@property (nonatomic, retain) NSMutableDictionary *peersById;
@property (nonatomic, retain) NSMutableDictionary *invitations;
@property (nonatomic, retain) NSMutableDictionary *progressByPayload;
@property (nonatomic, retain) NSMutableSet *everConnected;
/// Peers invited and not yet answered.
///
/// Counted toward the strategy limit alongside the connected ones: two
/// requestConnection calls made before either peer answers both saw a
/// connected count of zero, so both invitations went out and the discoverer
/// ended up holding two sessions under STAR or POINT_TO_POINT with neither
/// call ever told BUSY.
@property (nonatomic, retain) NSMutableSet *inviting;
/// Peers the browser lost while they were still connected.
///
/// forgetPeer refuses to drop a connected peer's mappings, because a send to
/// it must still resolve -- so a peer lost mid-session was never forgotten at
/// all: the disconnect that followed cleared everConnected and nothing
/// retried. Remembered here and dropped when the session ends.
@property (nonatomic, retain) NSMutableSet *lostWhileConnected;
/// Peer id to the payload ids sent to it and not yet acknowledged.
///
/// sendData succeeding means the bytes were QUEUED, not that they arrived --
/// and PayloadStatus.SUCCESS documents that every byte arrived, which is what
/// Android reports because Nearby tells it so. MultipeerConnectivity has no
/// such signal, so the receiving end sends one back and the terminal status
/// waits for it.
@property (nonatomic, retain) NSMutableDictionary *awaitingAck;
@property (nonatomic, assign) int pendingAdvertiseRequest;
@property (nonatomic, assign) int pendingDiscoverRequest;
/// The advertising and discovery services are tracked SEPARATELY.
///
/// One shared pair of fields cannot describe this object honestly: an app may
/// advertise "files" while browsing "chat", and whichever call ran last then
/// decided what both of them reported. Peers found by the browser belong to
/// the discovery service and peers that invite us belong to the advertising
/// one, so each is recorded against the peer that produced it (see
/// serviceIdByPeer) rather than read back out of a single mutable field.
///
/// The unfolded id is what the CALLER passed. Endpoint.getServiceId()
/// documents the id the app used, and reporting the folded Bonjour type
/// instead ("com-example-cha" for "com.example.chat") made a comparison
/// against the argument to startDiscovery fail on iOS alone.
@property (nonatomic, retain) NSString *advertiseServiceType;
@property (nonatomic, retain) NSString *advertiseServiceId;
@property (nonatomic, retain) NSString *discoverServiceType;
@property (nonatomic, retain) NSString *discoverServiceId;
/// Peer id to the unfolded service id the peer was seen on.
@property (nonatomic, retain) NSMutableDictionary *serviceIdByPeer;
/// The topology each half was started with.
///
/// MultipeerConnectivity enforces no topology of its own -- it will happily
/// connect a peer that asked for POINT_TO_POINT to a second one -- so the
/// strategy the caller passed has to be honoured here or not at all. Ignoring
/// it made TransportStrategy a documented promise the iOS port did not keep.
@property (nonatomic, assign) int advertiseStrategy;
@property (nonatomic, assign) int discoverStrategy;
/// Counts received files, so two with the same name get different paths.
/// Read and written under @synchronized(self): MultipeerConnectivity
/// delivers from a queue per session, so two arrivals really can race.
@property (nonatomic, assign) int receiveSequence;
@end

static CN1NearbyTransport *cn1nbTransport = nil;

/// MultipeerConnectivity refuses a service type that is not 1-15 characters
/// of lowercase ASCII letters, digits and hyphens -- it raises, which on a
/// device is a crash rather than an error the app can show. Android has no
/// such rule, so a perfectly good reverse-DNS service id from a cross-platform
/// app arrives here illegal. Folding it into something legal beats crashing,
/// and the public API documents the constraint so an app can pick a name that
/// survives the fold unchanged.
/// The Bonjour service types the Info.plist declared, without their framing.
///
/// iOS 14 and later refuse to browse a service type the app did not declare in
/// NSBonjourServices, and the refusal is a silent "no peers found" rather than
/// an error. So the plist is the authority here: whatever the app passes as a
/// service id is folded and then CHECKED against this list, and a type that is
/// not on it fails the call with a message naming the build hint to add it to.
/// Guessing instead -- registering the folded id and hoping -- is what produces
/// a transport that never works with nothing in any log to explain it.
static NSArray *cn1nbDeclaredServiceTypes(void) {
    NSArray *declared = [[NSBundle mainBundle]
            objectForInfoDictionaryKey:@"NSBonjourServices"];
    if (![declared isKindOfClass:[NSArray class]]) {
        return [NSArray array];
    }
    NSMutableArray *out = [NSMutableArray array];
    for (id entry in declared) {
        if (![entry isKindOfClass:[NSString class]]) {
            continue;
        }
        // "_chat._tcp" -> "chat", and "_chat._tcp." likewise.
        //
        // The trailing dot is not optional to handle: the builder's
        // NSBonjourServices renderer appends one to every entry, so that is
        // the spelling most plists actually carry. Stripped FIRST -- taken
        // last, it is the dot the transport suffix is cut at, which left
        // "chat._tcp" here and made every declared service look undeclared.
        NSString *name = (NSString *)entry;
        while ([name hasSuffix:@"."]) {
            name = [name substringToIndex:[name length] - 1];
        }
        if ([name hasPrefix:@"_"]) {
            name = [name substringFromIndex:1];
        }
        NSRange dot = [name rangeOfString:@"." options:NSBackwardsSearch];
        if (dot.location != NSNotFound) {
            name = [name substringToIndex:dot.location];
        }
        if ([name length] > 0 && ![out containsObject:name]) {
            [out addObject:name];
        }
    }
    return out;
}

/// Cuts a display name down to the 63 UTF-8 bytes MCPeerID accepts.
///
/// By BYTES and on a character boundary, not by taking a fixed number of
/// UTF-16 units: twenty emoji are about eighty bytes, so the old cut left an
/// over-long name that MCPeerID RAISES on -- a crash rather than an error --
/// and cutting mid-unit could split a surrogate pair into something that is
/// not a string at all.
static NSString *cn1nbPeerName(NSString *name) {
    if (name == nil || [name length] == 0) {
        return @"Codename One";
    }
    if ([name lengthOfBytesUsingEncoding:NSUTF8StringEncoding] <= 63) {
        return name;
    }
    NSUInteger end = [name length];
    while (end > 0) {
        // rangeOfComposedCharacterSequenceAtIndex keeps the cut off the
        // middle of a surrogate pair or a combining sequence.
        NSRange last = [name rangeOfComposedCharacterSequenceAtIndex:end - 1];
        end = last.location;
        NSString *candidate = [name substringToIndex:end];
        if ([candidate lengthOfBytesUsingEncoding:NSUTF8StringEncoding]
                <= 63) {
            return [candidate length] == 0 ? @"Codename One" : candidate;
        }
    }
    return @"Codename One";
}

/// Four base-36 characters derived from the whole service id.
///
/// FNV-1a over the id's UTF-8 bytes. Must stay identical to
/// `IPhoneBuilder.bonjourSuffix`: the type this device registers has to be the
/// type the build declared in the Info.plist, or iOS drops the traffic.
static NSString *cn1nbBonjourSuffix(NSString *serviceId) {
    uint32_t hash = 0x811c9dc5u;
    const char *bytes = [serviceId UTF8String];
    if (bytes != NULL) {
        for (const unsigned char *p = (const unsigned char *)bytes;
                *p != '\0'; p++) {
            unsigned int b = (unsigned int)*p;
            // ASCII-lowercased before hashing, so the suffix is as
            // case-insensitive as the fold -- "Chat" and "chat" are one
            // service. The builder does exactly this to exactly these bytes.
            if (b >= 'A' && b <= 'Z') {
                b += 'a' - 'A';
            }
            hash ^= (uint32_t)b;
            hash *= 16777619u;
        }
    }
    uint32_t value = hash % 1679616u;
    char digits[5];
    digits[4] = '\0';
    for (int i = 3; i >= 0; i--) {
        uint32_t digit = value % 36u;
        digits[i] = (char)(digit < 10 ? ('0' + digit) : ('a' + digit - 10));
        value /= 36u;
    }
    return [NSString stringWithUTF8String:digits];
}

static NSString *cn1nbServiceType(NSString *serviceId) {
    NSMutableString *out = [NSMutableString stringWithCapacity:15];
    NSString *lower = [serviceId lowercaseString];
    for (NSUInteger i = 0; i < [lower length] && [out length] < 15; i++) {
        unichar c = [lower characterAtIndex:i];
        if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
            [out appendFormat:@"%C", c];
        } else if ([out length] > 0 && [out length] < 15) {
            // A hyphen may not lead or trail, and two may not be adjacent.
            if (![out hasSuffix:@"-"]) {
                [out appendString:@"-"];
            }
        }
    }
    while ([out hasSuffix:@"-"]) {
        [out deleteCharactersInRange:NSMakeRange([out length] - 1, 1)];
    }
    // A stable suffix derived from the WHOLE id, because the fold above is
    // lossy and the truncation is brutal: "com.example.chat",
    // "com-example-chat" and "com.example.charts" all reduce to
    // "com-example-cha", so three unrelated apps would have discovered and
    // connected to each other while NearbyTransport promises service ids
    // match exactly. Ten characters of the readable fold plus four of hash
    // keeps the type recognisable and inside the fifteen Apple allows.
    if ([out length] > 10) {
        [out deleteCharactersInRange:NSMakeRange(10, [out length] - 10)];
    }
    while ([out hasSuffix:@"-"]) {
        [out deleteCharactersInRange:NSMakeRange([out length] - 1, 1)];
    }
    if ([out length] == 0) {
        [out appendString:@"cn1"];
    }
    [out appendString:@"-"];
    [out appendString:cn1nbBonjourSuffix(serviceId)];
    // At least one ASCII LETTER, not merely one legal character. Apple
    // requires it, and an all-digit id like "123" folded to "123" -- which
    // reads as legal and makes MCNearbyServiceAdvertiser RAISE rather than
    // fail, so the app crashed instead of failing to advertise. Prefixed
    // rather than rejected, and the builder folds identically so the type
    // this registers is the one the Info.plist declares.
    BOOL hasLetter = NO;
    for (NSUInteger i = 0; i < [out length]; i++) {
        unichar c = [out characterAtIndex:i];
        if (c >= 'a' && c <= 'z') {
            hasLetter = YES;
            break;
        }
    }
    if ([out length] > 0 && !hasLetter) {
        [out insertString:@"cn1-" atIndex:0];
        if ([out length] > 15) {
            [out deleteCharactersInRange:NSMakeRange(15, [out length] - 15)];
        }
        while ([out hasSuffix:@"-"]) {
            [out deleteCharactersInRange:NSMakeRange([out length] - 1, 1)];
        }
    }
    return [out length] == 0 ? @"cn1-nearby" : out;
}

static NSString *cn1nbIdForPeer(MCPeerID *peer) {
    // MCPeerID has no stable identifier of its own and two peers may share a
    // display name, so the id an app sees is the pointer-derived hash paired
    // with the name. It is meaningless past the end of this discovery
    // session, which is exactly what Endpoint.getId() documents.
    return [NSString stringWithFormat:@"%lu-%@", (unsigned long)[peer hash],
            cn1nbSanitize(peer.displayName)];
}

@implementation CN1NearbyTransport

- (void)dealloc {
    [_localPeer release];
    [_sessionsById release];
    [_advertiser release];
    [_browser release];
    [_peersById release];
    [_invitations release];
    [_progressByPayload release];
    [_everConnected release];
    [_inviting release];
    [_lostWhileConnected release];
    [_awaitingAck release];
    [_advertiseServiceType release];
    [_advertiseServiceId release];
    [_discoverServiceType release];
    [_discoverServiceId release];
    [_serviceIdByPeer release];
    [super dealloc];
}

/// The session for one endpoint, created on first use.
///
/// The delegate is shared: MCSessionDelegate hands the session back on every
/// callback, and nothing here needs to know which one it was.
- (MCSession *)sessionFor:(NSString *)endpointId {
    if (endpointId == nil) {
        return nil;
    }
    @synchronized (self) {
    MCSession *existing = [self.sessionsById objectForKey:endpointId];
    if (existing != nil) {
        return existing;
    }
    MCSession *created = [[[MCSession alloc] initWithPeer:self.localPeer
                                         securityIdentity:nil
                                     encryptionPreference:MCEncryptionRequired]
            autorelease];
    created.delegate = self;
    [self.sessionsById setObject:created forKey:endpointId];
    return created;
    }
}

/// Drops one endpoint's session and forgets it.
- (void)closeSessionFor:(NSString *)endpointId {
    // Retained across the removal. The dictionary is ordinarily the only
    // owner of this session -- it was created autoreleased and the pool it
    // was created in has long since drained -- so removing the entry first
    // released it, and the two messages below then went to freed memory.
    MCSession *session;
    @synchronized (self) {
        session = [[[self.sessionsById objectForKey:endpointId] retain]
                autorelease];
        if (session == nil) {
            return;
        }
        [self.sessionsById removeObjectForKey:endpointId];
    }
    // Outside the lock: disconnect can run delegate work, and holding the
    // transport's monitor across it would invite a deadlock against the
    // delegate queue that is trying to take the same monitor.
    session.delegate = nil;
    [session disconnect];
    // The reservation goes too. Clearing the delegate above is what stops the
    // state callback that normally releases it, so closing a session whose
    // invitation had not been answered left the slot held -- and every later
    // STAR or POINT_TO_POINT request answered BUSY until the whole transport
    // was stopped.
    [self clearInviting:endpointId];
    // Reported here, because clearing the delegate above is what stops
    // didChangeState:NotConnected from reporting it. A deliberate close is
    // still a disconnection as far as the app is concerned, and suppressing
    // both the callback and its replacement left every listener believing
    // the peer was still connected.
    //
    // Only for a peer that actually reached Connected -- the same test the
    // delegate applies, so a close during an unanswered invitation stays
    // silent rather than inventing a disconnection that never happened.
    if ([self takeEverConnected:endpointId]) {
        MCPeerID *peer = [self peerForId:endpointId];
        if (peer != nil) {
            NSString *encoded = [self encodePeer:peer];
            // Anything queued for this peer and still unacknowledged has to
            // be failed HERE. Clearing the delegate above is what stops
            // didChangeState from reaching takeAllAcksFromPeer, so a
            // deliberate close left an accepted send with no terminal status
            // at all -- and its bookkeeping alive until a full stop swept it.
            for (NSArray<NSNumber *> *stranded in
                    [self takeAllAcksFromPeer:endpointId]) {
                com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                        getThreadLocalData(), cn1nbJString(encoded),
                        (JAVA_INT)[[stranded objectAtIndex:0] intValue], 0,
                        (JAVA_LONG)[[stranded objectAtIndex:1] longLongValue],
                        CN1_NEARBY_PAYLOAD_FAILURE);
            }
            com_codename1_impl_ios_IOSNearbyCallbacks_disconnected___java_lang_String(
                    getThreadLocalData(), cn1nbJString(encoded));
        }
    }
}

/// Records one recipient's transfer so cancel can reach it.
///
/// #### Parameters
///
/// - `progress`: the transfer
/// - `payloadId`: the payload every recipient of this send shares
- (void)rememberProgress:(NSProgress *)progress forPayload:(JAVA_INT)payloadId {
    NSNumber *key = [NSNumber numberWithInt:(int)payloadId];
    @synchronized (self) {
        NSMutableArray *all = [self.progressByPayload objectForKey:key];
        if (all == nil) {
            all = [NSMutableArray array];
            [self.progressByPayload setObject:all forKey:key];
        }
        [all addObject:progress];
    }
}

/// Forgets the transfers in `holder`, and the payload entry once the last
/// recipient is done with it.
- (void)forgetProgress:(NSArray *)holder forPayload:(JAVA_INT)payloadId {
    NSNumber *key = [NSNumber numberWithInt:(int)payloadId];
    @synchronized (self) {
        NSMutableArray *all = [self.progressByPayload objectForKey:key];
        if (all == nil) {
            return;
        }
        [all removeObjectsInArray:holder];
        if ([all count] == 0) {
            [self.progressByPayload removeObjectForKey:key];
        }
    }
}

/// Takes and forgets every transfer registered for a payload, for cancel.
- (NSArray *)takeProgressesForPayload:(JAVA_INT)payloadId {
    NSNumber *key = [NSNumber numberWithInt:(int)payloadId];
    @synchronized (self) {
        NSArray *all = [[[self.progressByPayload objectForKey:key] copy]
                autorelease];
        [self.progressByPayload removeObjectForKey:key];
        return all;
    }
}

/// Drops every unanswered invitation.
- (void)forgetInvitations {
    @synchronized (self) {
        [self.invitations removeAllObjects];
    }
}

/// Records an invitation handler against the peer that sent it.
- (void)rememberInvitation:(id)handler forPeer:(NSString *)pid {
    @synchronized (self) {
        [self.invitations setObject:handler forKey:pid];
    }
}

/// Takes the invitation handler for a peer, retained past the removal so it
/// survives being the dictionary's only owner.
- (id)takeInvitationForPeer:(NSString *)pid {
    if (pid == nil) {
        return nil;
    }
    @synchronized (self) {
        id handler = [[[self.invitations objectForKey:pid] retain] autorelease];
        [self.invitations removeObjectForKey:pid];
        return handler;
    }
}

/// Records that a peer reached Connected, answering whether it is new.
- (void)markEverConnected:(NSString *)pid {
    @synchronized (self) {
        [self.everConnected addObject:pid];
    }
}

/// How many peers are connected or have an invitation outstanding.
- (NSUInteger)heldPeerCount {
    NSUInteger pending;
    @synchronized (self) {
        pending = [self.inviting count];
    }
    return [self connectedPeerCount] + pending;
}

/// Sends the one-frame acknowledgement for a received payload.
///
/// Best effort: a failure here cannot be reported to the sender, which is
/// precisely why the sender does not wait on it forever. If the frame never
/// leaves, or leaves and is lost, the sender's own acknowledgement timeout
/// fails that send rather than leaving it outstanding.
- (void)sendAck:(JAVA_INT)payloadId toPeer:(MCPeerID *)peer
      inSession:(MCSession *)session {
    unsigned char frame[CN1_NEARBY_FRAME_HEADER];
    frame[0] = CN1_NEARBY_FRAME_ACK;
    frame[1] = (unsigned char)((payloadId >> 24) & 0xff);
    frame[2] = (unsigned char)((payloadId >> 16) & 0xff);
    frame[3] = (unsigned char)((payloadId >> 8) & 0xff);
    frame[4] = (unsigned char)(payloadId & 0xff);
    NSError *ignored = nil;
    [session sendData:[NSData dataWithBytes:frame
                                     length:CN1_NEARBY_FRAME_HEADER]
              toPeers:[NSArray arrayWithObject:peer]
             withMode:MCSessionSendDataReliable
                error:&ignored];
}

/// Fails a send whose acknowledgement has not arrived in time.
///
/// Scheduled for every recorded send. It is a no-op in the normal case --
/// by then the acknowledgement, or the peer's disconnection, has already
/// taken the entry, and taking it is what decides who reports the terminal
/// status.
- (void)scheduleAckTimeout:(JAVA_INT)payloadId fromPeer:(NSString *)pid
                   encoded:(NSString *)encoded {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                    (int64_t)CN1_NEARBY_ACK_TIMEOUT_NS),
            dispatch_get_main_queue(), ^{
        @autoreleasepool {
            JAVA_LONG length = -1;
            if (![self takeAck:payloadId fromPeer:pid length:&length]) {
                return;
            }
            com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                    getThreadLocalData(), cn1nbJString(encoded), payloadId,
                    0, length, CN1_NEARBY_PAYLOAD_FAILURE);
        }
    });
}

/// Records a payload sent to a peer and waiting for its acknowledgement.
///
/// The LENGTH is recorded with it, because the acknowledgement frame does
/// not carry one and the terminal update has to. Reporting SUCCESS with
/// nothing transferred contradicted the IN_PROGRESS update just before it,
/// which had already reported the whole payload -- so a listener that
/// persists or displays the terminal update regressed a finished transfer
/// back to zero bytes.
- (void)awaitAck:(JAVA_INT)payloadId fromPeer:(NSString *)pid
          length:(JAVA_LONG)length {
    @synchronized (self) {
        NSMutableDictionary *ids = [self.awaitingAck objectForKey:pid];
        if (ids == nil) {
            ids = [NSMutableDictionary dictionary];
            [self.awaitingAck setObject:ids forKey:pid];
        }
        // One entry per send, not deduplicated. The same immutable Payload
        // sent to one peer twice before either answer arrives is two accepted
        // sends under one portable id -- and a set kept one, so the first
        // acknowledgement emitted the only terminal status and the second
        // send never got one.
        NSNumber *key = [NSNumber numberWithInt:(int)payloadId];
        NSMutableArray *outstanding = [ids objectForKey:key];
        if (outstanding == nil) {
            outstanding = [NSMutableArray array];
            [ids setObject:outstanding forKey:key];
        }
        [outstanding addObject:[NSNumber numberWithLongLong:
                (long long)length]];
    }
}

/// Takes the acknowledgement for one payload, answering whether it was
/// outstanding -- so a duplicate or unknown ack reports nothing -- and
/// handing back the length that send was recorded with.
- (BOOL)takeAck:(JAVA_INT)payloadId fromPeer:(NSString *)pid
          length:(JAVA_LONG *)outLength {
    @synchronized (self) {
        NSMutableDictionary *ids = [self.awaitingAck objectForKey:pid];
        NSNumber *key = [NSNumber numberWithInt:(int)payloadId];
        NSMutableArray *outstanding = [ids objectForKey:key];
        if (outstanding == nil || [outstanding count] == 0) {
            return NO;
        }
        if (outLength != NULL) {
            *outLength = (JAVA_LONG)[[outstanding lastObject] longLongValue];
        }
        [outstanding removeLastObject];
        if ([outstanding count] == 0) {
            [ids removeObjectForKey:key];
        }
        if ([ids count] == 0) {
            [self.awaitingAck removeObjectForKey:pid];
        }
        return YES;
    }
}

/// Takes every peer's outstanding sends of ONE payload, for a cancel.
///
/// #### Returns
///
/// pairs of the peer id and the length each send was recorded with
- (NSArray<NSArray *> *)takeAcksForPayload:(JAVA_INT)payloadId {
    @synchronized (self) {
        NSNumber *key = [NSNumber numberWithInt:(int)payloadId];
        NSMutableArray<NSArray *> *out = [NSMutableArray array];
        for (NSString *pid in [self.awaitingAck allKeys]) {
            NSMutableDictionary *ids = [self.awaitingAck objectForKey:pid];
            NSMutableArray *outstanding = [ids objectForKey:key];
            if (outstanding == nil) {
                continue;
            }
            for (NSNumber *length in outstanding) {
                [out addObject:[NSArray arrayWithObjects:pid, length, nil]];
            }
            [ids removeObjectForKey:key];
            if ([ids count] == 0) {
                [self.awaitingAck removeObjectForKey:pid];
            }
        }
        return out;
    }
}

/// Takes every payload still waiting on a peer, for a disconnect.
- (NSArray<NSArray<NSNumber *> *> *)takeAllAcksFromPeer:(NSString *)pid {
    @synchronized (self) {
        NSMutableDictionary *ids = [self.awaitingAck objectForKey:pid];
        NSMutableArray *out = [NSMutableArray array];
        // One entry per outstanding SEND, so two sends of one payload get two
        // terminal updates -- the same count they would have got as acks.
        // Each is a pair of the payload id and the length it was sent with,
        // so a stranded send reports the same total its progress did.
        //
        // The return type spells the pair out. It used to be a flat array of
        // ids, and when it became pairs one of the two callers went on
        // sending intValue to what was now an NSArray -- an unrecognized
        // selector, so a deliberate close of a session with a send in flight
        // crashed before either the failure or the disconnection was
        // delivered. An untyped NSArray * cannot catch that; this can.
        for (NSNumber *key in [ids allKeys]) {
            for (NSNumber *length in [ids objectForKey:key]) {
                [out addObject:[NSArray arrayWithObjects:key, length, nil]];
            }
        }
        [self.awaitingAck removeObjectForKey:pid];
        return out;
    }
}

/// Records an invitation this device is about to send.
- (void)markInviting:(NSString *)pid {
    @synchronized (self) {
        [self.inviting addObject:pid];
    }
}

/// Forgets an invitation that has been answered, either way.
- (void)clearInviting:(NSString *)pid {
    @synchronized (self) {
        [self.inviting removeObject:pid];
    }
}

/// True when this peer has reached Connected and has not been forgotten.
- (BOOL)isEverConnected:(NSString *)pid {
    @synchronized (self) {
        return [self.everConnected containsObject:pid];
    }
}

/// Forgets a peer nothing is talking to any more.
///
/// The singleton transport retains an MCPeerID for every device it has ever
/// seen, so a long discovery in a busy place accumulated one per device for
/// the life of the process -- and their endpoint ids stayed resolvable, which
/// is worse than the memory: a send addressed to a peer that went away found
/// a mapping and looked like it might work.
///
/// Only for a peer nothing is connected to. lostPeer means the BROWSER can no
/// longer see it, which says nothing about an open session -- dropping the
/// mapping there would have broken sending to a peer that is still connected.
- (void)forgetPeer:(NSString *)pid {
    if (pid == nil) {
        return;
    }
    if ([self isEverConnected:pid]) {
        // Still connected, so the mappings have to stay -- but the loss is
        // remembered, because nothing else would ever come back for it.
        @synchronized (self) {
            [self.lostWhileConnected addObject:pid];
        }
        return;
    }
    @synchronized (self) {
        [self.peersById removeObjectForKey:pid];
        [self.serviceIdByPeer removeObjectForKey:pid];
        [self.lostWhileConnected removeObject:pid];
    }
}

/// Forgets every peer, for a full stop.
- (void)forgetAllPeers {
    @synchronized (self) {
        [self.peersById removeAllObjects];
        [self.serviceIdByPeer removeAllObjects];
        [self.everConnected removeAllObjects];
        [self.inviting removeAllObjects];
        [self.lostWhileConnected removeAllObjects];
        [self.awaitingAck removeAllObjects];
    }
}

/// True when this peer had reached Connected, forgetting it either way.
- (BOOL)takeEverConnected:(NSString *)pid {
    @synchronized (self) {
        if (![self.everConnected containsObject:pid]) {
            return NO;
        }
        [self.everConnected removeObject:pid];
        return YES;
    }
}

/// How many peers are connected across every session.
- (NSUInteger)connectedPeerCount {
    NSUInteger n = 0;
    NSArray *sessions;
    @synchronized (self) {
        sessions = [[[self.sessionsById allValues] copy] autorelease];
    }
    for (MCSession *session in sessions) {
        n += [session.connectedPeers count];
    }
    return n;
}

/// Drops every session.
- (void)closeAllSessions {
    NSArray *keys;
    @synchronized (self) {
        keys = [[[self.sessionsById allKeys] copy] autorelease];
    }
    for (NSString *key in keys) {
        [self closeSessionFor:key];
    }
}

/// Encodes a peer first seen on a known service, remembering which one.
- (NSString *)encodePeer:(MCPeerID *)peer service:(NSString *)serviceId {
    NSString *pid = cn1nbIdForPeer(peer);
    if (serviceId != nil) {
        @synchronized (self) {
            [self.serviceIdByPeer setObject:serviceId forKey:pid];
        }
    }
    return [self encodePeer:peer];
}

- (NSString *)encodePeer:(MCPeerID *)peer {
    NSString *pid = cn1nbIdForPeer(peer);
    NSString *service;
    @synchronized (self) {
        [self.peersById setObject:peer forKey:pid];
    // The service this peer was actually seen on, not whichever of the two
    // was configured most recently. A peer reached through a session -- a
    // state change, an arriving payload -- was found by the browser or came
    // in through the advertiser earlier, and that is when the mapping was
    // recorded.
        service = [self.serviceIdByPeer objectForKey:pid];
        if (service == nil) {
            service = self.discoverServiceId != nil ? self.discoverServiceId
                    : self.advertiseServiceId;
        }
    }
    return cn1nbJoin([NSArray arrayWithObjects:pid,
            peer.displayName == nil ? @"" : peer.displayName,
            service == nil ? @"" : service, nil]);
}

- (MCPeerID *)peerForId:(NSString *)pid {
    if (pid == nil) {
        return nil;
    }
    @synchronized (self) {
        return [self.peersById objectForKey:pid];
    }
}

/// MultipeerConnectivity gives no comparison token, and this does not invent
/// one.
///
/// Nearby Connections derives its authentication digits from the key exchange,
/// which is what makes comparing them on both screens detect a device in the
/// middle. MultipeerConnectivity exposes no equivalent: with
/// `securityIdentity:nil` the peers use ephemeral keys and
/// `session:didReceiveCertificate:` hands over nothing to bind a token to.
///
/// An earlier version hashed the two display names and the service type. That
/// is public information a relay observes and can reproduce on both of its
/// sessions, so it would have shown matching digits at both ends while
/// relaying -- a check that looks like a defence and is not, which is worse
/// than no check at all. So iOS reports an empty token, which
/// `IncomingConnection.getAuthenticationToken()` documents as "the platform
/// does not produce one", and the guide tells iOS apps to verify identity
/// their own way.
- (NSString *)tokenForPeer:(MCPeerID *)peer {
    return @"";
}

// ---- MCSessionDelegate ----------------------------------------------

- (void)session:(MCSession *)session peer:(MCPeerID *)peerID
        didChangeState:(MCSessionState)state {
    @autoreleasepool {
        if (state == MCSessionStateConnecting) {
            return;
        }
        NSString *pid = cn1nbIdForPeer(peerID);
        NSString *encoded = [self encodePeer:peerID];
        // Answered, so the reservation is released whichever way it went.
        [self clearInviting:pid];
        if (state == MCSessionStateConnected) {
            [self markEverConnected:pid];
            com_codename1_impl_ios_IOSNearbyCallbacks_connectionResult___java_lang_String_boolean_int_java_lang_String(
                    getThreadLocalData(), cn1nbJString(encoded), JAVA_TRUE, 0,
                    JAVA_NULL);
            return;
        }
        // NotConnected covers two different events, and reporting both as a
        // disconnection left an app that was INVITING a peer waiting forever
        // for a connected/failed answer that never came -- a rejected or
        // timed-out invitation lands here without ever having been connected.
        // Only a peer that actually reached Connected can disconnect.
        BOOL lostWhileUp;
        @synchronized (self) {
            lostWhileUp = [self.lostWhileConnected containsObject:pid];
        }
        if ([self takeEverConnected:pid]) {
            // Anything still waiting on this peer will never be
            // acknowledged, so it is failed rather than left pending.
            for (NSArray<NSNumber *> *stranded in
                    [self takeAllAcksFromPeer:pid]) {
                com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                        getThreadLocalData(), cn1nbJString(encoded),
                        (JAVA_INT)[[stranded objectAtIndex:0] intValue], 0,
                        (JAVA_LONG)[[stranded objectAtIndex:1] longLongValue],
                        CN1_NEARBY_PAYLOAD_FAILURE);
            }
            if (lostWhileUp) {
                // The browser lost it before the session ended, so this is
                // the moment its mappings can finally go.
                [self forgetPeer:pid];
            }
            com_codename1_impl_ios_IOSNearbyCallbacks_disconnected___java_lang_String(
                    getThreadLocalData(), cn1nbJString(encoded));
            return;
        }
        com_codename1_impl_ios_IOSNearbyCallbacks_connectionResult___java_lang_String_boolean_int_java_lang_String(
                getThreadLocalData(), cn1nbJString(encoded), JAVA_FALSE,
                CN1_NEARBY_ERR_PEER_UNAVAILABLE,
                cn1nbJString(@"the peer declined the invitation or it timed"
                             @" out"));
    }
}

- (void)session:(MCSession *)session didReceiveData:(NSData *)data
        fromPeer:(MCPeerID *)peerID {
    @autoreleasepool {
        NSString *encoded = [self encodePeer:peerID];
        // MultipeerConnectivity carries raw bytes and nothing else, so the
        // sender's payload id is framed into the first four bytes and stripped
        // here. Without it every received payload arrived as id 0 and no app
        // could tell two of them apart, or match one to its progress events --
        // which Payload.getId() promises it can. Both ends of an MPC session
        // are Codename One, so the framing is symmetric by construction.
        // Frame: one kind byte, four id bytes, then the body. The kind
        // distinguishes a payload from the acknowledgement the receiver sends
        // back, which is what lets the SENDER report a terminal SUCCESS that
        // means "arrived" rather than "queued".
        if ([data length] < CN1_NEARBY_FRAME_HEADER) {
            return;
        }
        const unsigned char *b = (const unsigned char *)[data bytes];
        unsigned char kind = b[0];
        JAVA_INT payloadId = (JAVA_INT)((b[1] << 24) | (b[2] << 16)
                | (b[3] << 8) | b[4]);
        NSString *pid = cn1nbIdForPeer(peerID);
        if (kind == CN1_NEARBY_FRAME_ACK) {
            // The far side has the bytes. Reported once: a duplicate or
            // unknown ack is dropped rather than emitting a second terminal
            // status for a payload already finished.
            JAVA_LONG length = -1;
            if ([self takeAck:payloadId fromPeer:pid length:&length]) {
                // The length this send was recorded with, not zero. SUCCESS
                // means every byte arrived, so the terminal update has to
                // carry the same count the IN_PROGRESS before it did.
                com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                        getThreadLocalData(), cn1nbJString(encoded), payloadId,
                        length, length, CN1_NEARBY_PAYLOAD_SUCCESS);
            }
            return;
        }
        NSData *body = [data subdataWithRange:
                NSMakeRange(CN1_NEARBY_FRAME_HEADER,
                        [data length] - CN1_NEARBY_FRAME_HEADER)];
        // Acknowledged before the payload is handed up, so a listener that
        // takes a while cannot delay the sender's terminal status.
        [self sendAck:payloadId toPeer:peerID inSession:session];
        // The terminal SUCCESS update, for the reason the file path emits
        // one: a receiver that releases per-payload state or dismisses its
        // transfer UI on the documented terminal status waited forever on
        // every byte payload, which is the common case and the one Android
        // has always reported.
        com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                getThreadLocalData(), cn1nbJString(encoded), payloadId,
                (JAVA_LONG)[body length], (JAVA_LONG)[body length],
                CN1_NEARBY_PAYLOAD_SUCCESS);
        com_codename1_impl_ios_IOSNearbyCallbacks_payloadReceived___java_lang_String_int_int_byte_1ARRAY_java_lang_String(
                getThreadLocalData(), cn1nbJString(encoded), payloadId,
                CN1_NEARBY_PAYLOAD_BYTES, cn1nbJBytes(body), JAVA_NULL);
    }
}

- (void)session:(MCSession *)session
        didStartReceivingResourceWithName:(NSString *)resourceName
        fromPeer:(MCPeerID *)peerID withProgress:(NSProgress *)progress {
    // Progress is reported on completion; a KVO observer per transfer would
    // buy finer granularity at the cost of an observer lifetime to get wrong.
}

- (void)session:(MCSession *)session
        didFinishReceivingResourceWithName:(NSString *)resourceName
        fromPeer:(MCPeerID *)peerID atURL:(NSURL *)localURL
        withError:(NSError *)error {
    @autoreleasepool {
        NSString *encoded = [self encodePeer:peerID];
        // The sender's id is parsed off the resource name BEFORE anything
        // else, because the failure branch below needs it too. Parsed after
        // it, a transfer that broke mid-flight -- a cancellation at the
        // sender, a dropped link -- reported its failure under id 0 and the
        // receiver could not match it to the transfer it was watching.
        JAVA_INT filePayloadId = 0;
        NSString *bare = resourceName;
        if ([bare hasPrefix:@"cn1id-"]) {
            NSRange dash = [bare rangeOfString:@"-"
                                       options:0
                                         range:NSMakeRange(6, [bare length] - 6)];
            if (dash.location != NSNotFound) {
                filePayloadId = (JAVA_INT)[[bare substringWithRange:
                        NSMakeRange(6, dash.location - 6)] intValue];
                bare = [bare substringFromIndex:dash.location + 1];
            }
        }
        if (error != nil || localURL == nil) {
            com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                    getThreadLocalData(), cn1nbJString(encoded),
                    filePayloadId, 0, -1,
                    cn1nbWasCancelled(error) ? CN1_NEARBY_PAYLOAD_CANCELED
                            : CN1_NEARBY_PAYLOAD_FAILURE);
            return;
        }
        // The URL the framework hands over is in a temporary location it will
        // delete, so the file is moved somewhere the app can still read when
        // the callback returns.
        // resourceName is chosen by the REMOTE peer, so it is untrusted
        // input. Appended raw, a name like "../../Library/Preferences/x"
        // walked out of the app's Documents directory and the removeItem and
        // move below would then delete and overwrite files elsewhere in the
        // container. Reduced to its last path component, and anything that
        // still looks like traversal or a separator is replaced outright.
        // filePayloadId and bare were resolved above, before the failure
        // branch that also needs them.
        NSString *safe = [bare lastPathComponent];
        if (safe == nil || [safe length] == 0
                || [safe isEqualToString:@"."]
                || [safe isEqualToString:@".."]
                || [safe rangeOfString:@"/"].location != NSNotFound) {
            safe = @"payload";
        }
        NSString *docs = [NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        // Unique per transfer, not per name. Built from the basename alone,
        // a second "photo.jpg" from any peer overwrote the first -- and the
        // first Payload had already been handed to the app as an immutable
        // path, so its contents changed under it. The payload id is not
        // enough on its own either: two peers can each send their own id 1.
        int received;
        @synchronized (self) {
            received = ++self->_receiveSequence;
        }
        NSString *target = [docs stringByAppendingPathComponent:
                [NSString stringWithFormat:@"cn1nearby-%d-%d-%@",
                        (int)filePayloadId, received, safe]];
        // Belt and braces: whatever the name folded to, the result has to
        // stay inside the directory it was built from.
        if (![[target stringByStandardizingPath]
                hasPrefix:[docs stringByStandardizingPath]]) {
            com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                    getThreadLocalData(), cn1nbJString(encoded),
                    filePayloadId, 0, -1, CN1_NEARBY_PAYLOAD_FAILURE);
            return;
        }
        [[NSFileManager defaultManager] removeItemAtPath:target error:nil];
        NSError *moveError = nil;
        [[NSFileManager defaultManager] moveItemAtPath:[localURL path]
                                                toPath:target
                                                 error:&moveError];
        if (moveError != nil) {
            // The recovered id, not zero. The sender's id has already been
            // parsed out of the resource name at this point, and reporting
            // the failure under 0 left the receiver unable to match it to the
            // payload or to the progress it had been watching.
            com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                    getThreadLocalData(), cn1nbJString(encoded),
                    filePayloadId, 0, -1, CN1_NEARBY_PAYLOAD_FAILURE);
            return;
        }
        // The terminal SUCCESS update, which only the failure paths above
        // used to emit. A receiver that dismisses its transfer UI or releases
        // per-payload state on the documented terminal status waited forever
        // on every file that actually arrived -- the one case that always
        // works on Android.
        com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                getThreadLocalData(), cn1nbJString(encoded), filePayloadId,
                0, -1, CN1_NEARBY_PAYLOAD_SUCCESS);
        com_codename1_impl_ios_IOSNearbyCallbacks_payloadReceived___java_lang_String_int_int_byte_1ARRAY_java_lang_String(
                getThreadLocalData(), cn1nbJString(encoded), filePayloadId,
                CN1_NEARBY_PAYLOAD_FILE, JAVA_NULL,
                cn1nbJString([@"file://" stringByAppendingString:target]));
    }
}

- (void)session:(MCSession *)session
        didReceiveStream:(NSInputStream *)stream withName:(NSString *)streamName
        fromPeer:(MCPeerID *)peerID {
    // The portable API has no stream payload, so nothing here consumes one.
}

// ---- MCNearbyServiceAdvertiserDelegate -------------------------------

- (void)advertiser:(MCNearbyServiceAdvertiser *)advertiser
        didReceiveInvitationFromPeer:(MCPeerID *)peerID
        withContext:(NSData *)context
        invitationHandler:(void (^)(BOOL, MCSession *))invitationHandler {
    @autoreleasepool {
        if (advertiser != self.advertiser) {
            // From an advertiser that has since been replaced. Labelling it
            // with the CURRENT advertiseServiceId would have handed the app
            // an invitation attributed to a service it was never advertised
            // on, and accepting it would have joined a peer that answered a
            // different advertisement. Declined rather than dropped: the
            // handler is what the remote side is waiting on, and dropping it
            // leaves that peer hanging until MultipeerConnectivity times the
            // invitation out.
            invitationHandler(NO, nil);
            return;
        }
        NSString *pid = cn1nbIdForPeer(peerID);
        NSString *encoded = [self encodePeer:peerID
                                     service:self.advertiseServiceId];
        // Copied because the block outlives this call: it is answered when
        // the app calls accept or reject, which is at least an EDT hop away.
        [self rememberInvitation:[[invitationHandler copy] autorelease]
                         forPeer:pid];
        com_codename1_impl_ios_IOSNearbyCallbacks_connectionRequested___java_lang_String_java_lang_String(
                getThreadLocalData(), cn1nbJString(encoded),
                cn1nbJString([self tokenForPeer:peerID]));
    }
}

- (void)advertiser:(MCNearbyServiceAdvertiser *)advertiser
        didNotStartAdvertisingPeer:(NSError *)error {
    @autoreleasepool {
        if (advertiser != self.advertiser) {
            // From an advertiser that has since been replaced. Clearing the
            // delegate above should stop this, but a callback already in
            // flight is not recalled by it, and answering would fail the
            // replacement's request with a dead advertiser's error.
            return;
        }
        // MultipeerConnectivity rejects advertising asynchronously -- an
        // unavailable radio, a service type it will not take -- and
        // startAdvertising has already resolved true by the time this fires.
        // Dropping the error left the caller believing it was advertising
        // when it was not, with no second signal ever coming. The request id
        // is kept precisely so this can fail it late.
        int requestId = self.pendingAdvertiseRequest;
        self.pendingAdvertiseRequest = 0;
        if (requestId != 0) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_SESSION_FAILED,
                    [error localizedDescription]);
        }
    }
}

// ---- MCNearbyServiceBrowserDelegate ----------------------------------

- (void)browser:(MCNearbyServiceBrowser *)browser
        foundPeer:(MCPeerID *)peerID
        withDiscoveryInfo:(NSDictionary<NSString *, NSString *> *)info {
    @autoreleasepool {
        if (browser != self.browser) {
            // A sighting from a browser that has since been replaced,
            // reported under the service id of its replacement. The app
            // would then hold an endpoint the live browser never found and
            // will never report lost.
            return;
        }
        NSString *encoded = [self encodePeer:peerID
                                     service:self.discoverServiceId];
        com_codename1_impl_ios_IOSNearbyCallbacks_endpointFound___java_lang_String_boolean(
                getThreadLocalData(), cn1nbJString(encoded), JAVA_TRUE);
    }
}

- (void)browser:(MCNearbyServiceBrowser *)browser
        lostPeer:(MCPeerID *)peerID {
    @autoreleasepool {
        if (browser != self.browser) {
            // The other half of the same mislabelling, and the peer is NOT
            // forgotten here either: the mapping is shared with the live
            // browser, which may have found this peer under its own service.
            return;
        }
        NSString *encoded = [self encodePeer:peerID
                                     service:self.discoverServiceId];
        com_codename1_impl_ios_IOSNearbyCallbacks_endpointFound___java_lang_String_boolean(
                getThreadLocalData(), cn1nbJString(encoded), JAVA_FALSE);
        // Forgotten after the event is delivered, because encoding it needs
        // the mappings.
        [self forgetPeer:cn1nbIdForPeer(peerID)];
    }
}

- (void)browser:(MCNearbyServiceBrowser *)browser
        didNotStartBrowsingForPeers:(NSError *)error {
    @autoreleasepool {
        if (browser != self.browser) {
            // From a browser that has since been replaced; answering would
            // fail the replacement's request with a dead browser's error.
            return;
        }
        // Same as advertising: the answer is already out, so the request id is
        // held to fail it when the framework changes its mind.
        int requestId = self.pendingDiscoverRequest;
        self.pendingDiscoverRequest = 0;
        if (requestId != 0) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_SESSION_FAILED,
                    [error localizedDescription]);
        }
    }
}

@end

/// True when the folded form of `serviceId` is one the Info.plist declared.
static BOOL cn1nbServiceTypeIsDeclared(NSString *serviceId) {
    NSArray *declared = cn1nbDeclaredServiceTypes();
    return [declared containsObject:cn1nbServiceType(serviceId)];
}

/// The message an undeclared service type fails with. Names the hint to set,
/// because the developer cannot otherwise tell why discovery found nothing.
static NSString *cn1nbUndeclaredServiceMessage(NSString *serviceId) {
    return [NSString stringWithFormat:
            @"iOS only browses Bonjour service types declared in the app's "
            @"Info.plist, and \"_%@._tcp\" is not one of them (declared: %@). "
            @"Add \"%@\" to the ios.nearby.serviceType build hint, which "
            @"accepts a comma-separated list.",
            cn1nbServiceType(serviceId),
            [cn1nbDeclaredServiceTypes() componentsJoinedByString:@", "],
            serviceId == nil ? @"" : serviceId];
}

/// Gives the local peer the name the caller asked for.
///
/// MCPeerID is immutable and the session, advertiser and browser are all bound
/// to it, so a rename is a rebuild of the lot. Only done when nothing is
/// connected: renaming under a live session would drop it, and an app that
/// passes a different name to a later call did not ask for that.
///
/// This exists because an app that discovers first and names itself only in
/// requestConnection -- the ordinary initiator flow -- showed the peer its
/// device name instead, the identity having been built at discovery time.
static void cn1nbApplyLocalName(CN1NearbyTransport *t, NSString *localName) {
    NSString *wanted = localName == nil || [localName length] == 0
            ? nil : localName;
    // MCPeerID rejects a display name longer than 63 UTF-8 bytes.
    if (wanted != nil
            && [wanted lengthOfBytesUsingEncoding:NSUTF8StringEncoding] > 63) {
        wanted = cn1nbPeerName(wanted);
    }
    // heldPeerCount, not connectedPeerCount: an invitation that has gone out
    // and not been answered counts too. Rebuilding the peer id tears down
    // every session, and doing that while an invitation was pending
    // disconnected it without ever reporting connected or connectionFailed,
    // so the app waited for a lifecycle outcome that was never coming.
    if (t.localPeer != nil && wanted != nil
            && ![t.localPeer.displayName isEqualToString:wanted]
            && [t heldPeerCount] == 0) {
        BOOL wasAdvertising = t.advertiser != nil;
        BOOL wasBrowsing = t.browser != nil;
        if (wasAdvertising) {
            [t.advertiser stopAdvertisingPeer];
            t.advertiser.delegate = nil;
            t.advertiser = nil;
        }
        if (wasBrowsing) {
            [t.browser stopBrowsingForPeers];
            t.browser.delegate = nil;
            t.browser = nil;
        }
        [t closeAllSessions];
        t.localPeer = [[[MCPeerID alloc] initWithDisplayName:wanted]
                autorelease];
        if (wasAdvertising) {
            t.advertiser = [[[MCNearbyServiceAdvertiser alloc]
                    initWithPeer:t.localPeer
                   discoveryInfo:nil
                     serviceType:t.advertiseServiceType] autorelease];
            t.advertiser.delegate = t;
            [t.advertiser startAdvertisingPeer];
        }
        if (wasBrowsing) {
            t.browser = [[[MCNearbyServiceBrowser alloc]
                    initWithPeer:t.localPeer
                     serviceType:t.discoverServiceType] autorelease];
            t.browser.delegate = t;
            [t.browser startBrowsingForPeers];
        }
        return;
    }
    if (t.localPeer == nil) {
        NSString *name = wanted != nil ? wanted
                : [[UIDevice currentDevice] name];
        if ([name lengthOfBytesUsingEncoding:NSUTF8StringEncoding] > 63) {
            name = cn1nbPeerName(name);
        }
        t.localPeer = [[[MCPeerID alloc] initWithDisplayName:name]
                autorelease];
    }
}

static CN1NearbyTransport *cn1nbTransportInit(NSString *serviceId,
        NSString *localName, BOOL advertising) {
    if (cn1nbTransport == nil) {
        cn1nbTransport = [[CN1NearbyTransport alloc] init];
        cn1nbTransport.peersById = [NSMutableDictionary dictionary];
        cn1nbTransport.invitations = [NSMutableDictionary dictionary];
        cn1nbTransport.progressByPayload = [NSMutableDictionary dictionary];
        cn1nbTransport.everConnected = [NSMutableSet set];
        cn1nbTransport.inviting = [NSMutableSet set];
        cn1nbTransport.lostWhileConnected = [NSMutableSet set];
        cn1nbTransport.awaitingAck = [NSMutableDictionary dictionary];
        cn1nbTransport.sessionsById = [NSMutableDictionary dictionary];
        cn1nbTransport.serviceIdByPeer = [NSMutableDictionary dictionary];
    }
    if (serviceId != nil) {
        // Assigned on EVERY call, and only to the half this call is for.
        // Caching it meant stopping discovery for "chat" and starting it for
        // "files" carried on browsing chat; writing one shared field instead
        // meant an app advertising "files" while browsing "chat" relabelled
        // the browser's sightings as "files". The advertiser and browser are
        // rebuilt per call and read their own field.
        if (advertising) {
            cn1nbTransport.advertiseServiceType = cn1nbServiceType(serviceId);
            cn1nbTransport.advertiseServiceId = serviceId;
        } else {
            cn1nbTransport.discoverServiceType = cn1nbServiceType(serviceId);
            cn1nbTransport.discoverServiceId = serviceId;
        }
    }
    cn1nbApplyLocalName(cn1nbTransport, localName);
    return cn1nbTransport;
}

/// Answers a start request once the framework has had its chance to refuse it.
///
/// A second answer is harmless -- the Java side takes a pending request out of
/// its map, so whichever of this and the delegate's failure arrives first
/// wins and the other is dropped -- which is what makes the race between them
/// safe rather than merely unlikely.
///
/// #### Parameters
///
/// - `t`: the transport
/// - `advertising`: YES for advertising, NO for discovery
/// - `requestId`: the request to answer
/// Fails whichever start is still pending, because a stop just cancelled it.
///
/// SESSION_INVALIDATED, and the same wording the simulated bridge uses. This
/// used to answer OK on the grounds that the framework HAD taken the start --
/// true of the radio, and beside the point to the caller, whose question was
/// "is it advertising now". Android and the simulator both fail it, so an app
/// that branched on the answer behaved differently on iOS alone, which is the
/// divergence this whole family of guards exists to remove.
static void cn1nbCancelPendingStart(CN1NearbyTransport *t, BOOL advertising) {
    if (t == nil) {
        return;
    }
    int pending = advertising ? t.pendingAdvertiseRequest
            : t.pendingDiscoverRequest;
    if (advertising) {
        t.pendingAdvertiseRequest = 0;
    } else {
        t.pendingDiscoverRequest = 0;
    }
    if (pending != 0) {
        cn1nbFailTransport(pending, CN1_NEARBY_ERR_SESSION_INVALIDATED,
                advertising
                ? @"advertising was stopped before it started"
                : @"discovery was stopped before it started");
    }
}

static void cn1nbSettleTransportStart(CN1NearbyTransport *t, BOOL advertising,
        int requestId) {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                    (int64_t)CN1_NEARBY_START_GRACE_NS),
            dispatch_get_main_queue(), ^{
        @autoreleasepool {
            int pending = advertising ? t.pendingAdvertiseRequest
                    : t.pendingDiscoverRequest;
            if (pending != requestId) {
                // Already failed by the delegate, already answered by a stop,
                // or superseded by a newer start. Not ours to answer.
                return;
            }
            if (advertising) {
                t.pendingAdvertiseRequest = 0;
            } else {
                t.pendingDiscoverRequest = 0;
            }
            cn1nbTransportOk(requestId);
        }
    });
}

#endif // CN1_NEARBY_HAS_MPC

// =====================================================================
// Companion association -- AccessorySetupKit
// =====================================================================

#ifdef CN1_NEARBY_HAS_ASK

API_AVAILABLE(ios(18.0))
@interface CN1NearbyCompanion : NSObject
@property (nonatomic, retain) ASAccessorySession *session;
@property (nonatomic, assign) BOOL activated;
/// Signalled when the session reports itself active.
///
/// activateWithQueue returns before the session is usable, and the event
/// saying so arrives on the queue it was given. Showing a picker or reading
/// accessories before then made the first association of a fresh process fail
/// and getAssociations answer with an empty list for an app that had
/// associations.
@property (nonatomic, assign) dispatch_semaphore_t activeSignal;
@property (nonatomic, assign) BOOL active;
/// Blocks queued by whenActive: while the session is still coming up.
@property (nonatomic, retain) NSMutableArray *activationWaiters;
- (BOOL)awaitActive;
- (void)whenActive:(void (^)(BOOL active))handler;
- (void)activate;
@end

// Typed as id rather than CN1NearbyCompanion *: a file-scope variable of an
// API_AVAILABLE(ios(18.0)) type is itself flagged as unguarded, and there is
// no availability annotation for a variable declaration to carry.
static id cn1nbCompanion = nil;

@implementation CN1NearbyCompanion

- (void)dealloc {
    [_session release];
    [_activationWaiters release];
    if (_activeSignal != NULL) {
        dispatch_release(_activeSignal);
    }
    [super dealloc];
}

/// Blocks briefly for the session to become active.
///
/// Bounded, and never called on the main thread: the activation event is
/// delivered on the main queue, so waiting there would deadlock. Codename One
/// natives run on the EDT, which on iOS is a thread of its own.
///
/// This is the LAST resort and only getAssociations still uses it. Everything
/// that can be resumed later goes through whenActive: instead, because the
/// EDT is the thread that draws: waiting on it stalls input and rendering for
/// as long as activation takes. getAssociations cannot follow, because the
/// portable API returns the associations from the call -- there is nowhere to
/// resume to, and answering empty on the way past is the very bug the wait
/// was added for, an app with associations told it had none.
- (BOOL)awaitActive {
    if (self.active) {
        return YES;
    }
    if (self.activeSignal == NULL || [NSThread isMainThread]) {
        return self.active;
    }
    dispatch_semaphore_wait(self.activeSignal,
            dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2ull * NSEC_PER_SEC)));
    // Signalled back, because more than one caller may be waiting and the
    // semaphore is a latch rather than a queue.
    if (self.active) {
        dispatch_semaphore_signal(self.activeSignal);
    }
    return self.active;
}

/// Encodes an accessory the way NearbyWire.decodeCompanionDevice expects.
///
/// The address field carries the per-app CoreBluetooth identifier rather than
/// a MAC address, because that is the only handle iOS gives out -- and it is
/// the same one `BluetoothLE.getPeripheral(String)` takes, which is what makes
/// an association useful rather than decorative.
/// The association id for an accessory, stable across process launches.
///
/// NOT the object's hash. An accessory offered through the SSID filter has no
/// bluetoothIdentifier, and the hash that stood in for it was a different
/// number every launch -- so an id the public API documents as persistable
/// could not be persisted, and accessoryForId, which only ever compared
/// Bluetooth UUIDs, could not find it even within one launch.
///
/// One function so the two cannot drift: whatever this returns is what
/// accessoryForId matches on.
static NSString *cn1nbAccessoryId(ASAccessory *accessory)
        API_AVAILABLE(ios(18.0)) {
    if (accessory.bluetoothIdentifier != nil) {
        return [accessory.bluetoothIdentifier UUIDString];
    }
    if (accessory.SSID != nil && [accessory.SSID length] > 0) {
        return [@"ssid:" stringByAppendingString:accessory.SSID];
    }
    return [@"name:" stringByAppendingString:
            accessory.displayName == nil ? @"" : accessory.displayName];
}

- (NSString *)encode:(ASAccessory *)accessory present:(BOOL)present {
    NSString *identifier = accessory.bluetoothIdentifier != nil
            ? [accessory.bluetoothIdentifier UUIDString] : @"";
    return cn1nbJoin([NSArray arrayWithObjects:
            cn1nbAccessoryId(accessory),
            accessory.displayName == nil ? @"" : accessory.displayName,
            identifier,
            @"0",
            present ? @"1" : @"0",
            nil]);
}

- (ASAccessory *)accessoryForId:(NSString *)associationId {
    if (self.session == nil || associationId == nil) {
        return nil;
    }
    // Exactly one, or none.
    //
    // An accessory with no bluetoothIdentifier has no stable identifier in
    // this API at all -- SSID and display name are the only handles, and two
    // Wi-Fi accessories on one network, or two accessories sharing a name,
    // derive the same id. Returning the first match let disassociate remove
    // whichever happened to be encountered first, which is worse than not
    // finding it: the app asked to forget one accessory and forgot another.
    ASAccessory *match = nil;
    for (ASAccessory *a in self.session.accessories) {
        if ([cn1nbAccessoryId(a) isEqualToString:associationId]) {
            if (match != nil) {
                return nil;
            }
            match = a;
        }
    }
    return match;
}

/// Runs `handler` once the session is active, WITHOUT blocking the caller.
///
/// Called with NO when activation does not arrive in time, so a queued
/// operation always settles rather than being forgotten. The handler runs on
/// the caller's thread when the session is already active and on the main
/// queue otherwise, which is where the activation event and the timeout both
/// land.
- (void)whenActive:(void (^)(BOOL active))handler {
    [self activate];
    if (self.active) {
        handler(YES);
        return;
    }
    void (^queued)(BOOL) = [[handler copy] autorelease];
    @synchronized (self) {
        if (self.activationWaiters == nil) {
            self.activationWaiters = [NSMutableArray array];
        }
        [self.activationWaiters addObject:queued];
    }
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                    (int64_t)(2ull * NSEC_PER_SEC)),
            dispatch_get_main_queue(), ^{
        @autoreleasepool {
            // THIS waiter and no other. Draining the whole array here meant
            // an older waiter's timer settled a request queued moments
            // before it fired -- so a second association could fail
            // RADIO_UNAVAILABLE a fraction of a second after it was made,
            // and even after activation went on to succeed well inside its
            // own two seconds. Ordinarily this finds nothing: activation got
            // there first and took every waiter with it.
            BOOL mine = NO;
            @synchronized (self) {
                NSUInteger at = [self.activationWaiters
                        indexOfObjectIdenticalTo:queued];
                if (at != NSNotFound) {
                    [self.activationWaiters removeObjectAtIndex:at];
                    mine = YES;
                }
            }
            if (mine) {
                queued(self.active);
            }
        }
    });
}

/// Hands every queued block the activation outcome, exactly once each.
- (void)drainWaiters:(BOOL)activeNow {
    NSArray *waiting;
    @synchronized (self) {
        waiting = [[self.activationWaiters copy] autorelease];
        [self.activationWaiters removeAllObjects];
    }
    for (void (^handler)(BOOL) in waiting) {
        handler(activeNow);
    }
}

- (void)activate {
    if (self.activated) {
        return;
    }
    self.activated = YES;
    self.activeSignal = dispatch_semaphore_create(0);
    self.session = [[[ASAccessorySession alloc] init] autorelease];
    CN1NearbyCompanion *weakSelf = self;
    // The handler records ACTIVATION and nothing else.
    //
    // AccessorySetupKit reports an accessory entering or leaving the app's
    // SET, which is not the same event as it coming into or going out of
    // RANGE -- and that difference is why startObservingPresence answers
    // false on iOS and the public documentation calls presence Android-only.
    // Forwarding these as presence reported an accessory sitting in a drawer
    // as present the moment it was associated, and reported disassociation as
    // walking out of range. Nothing else needs them either: the association
    // is answered from the picker completion and getAssociations reads the
    // set directly.
    [self.session activateWithQueue:dispatch_get_main_queue()
                       eventHandler:^(ASAccessoryEvent *event) {
        if (event.eventType == ASAccessoryEventTypeActivated) {
            weakSelf.active = YES;
            dispatch_semaphore_signal(weakSelf.activeSignal);
            [weakSelf drainWaiters:YES];
        }
    }];
}

@end

/// Hands the activated session to `handler`, or nil when it never activated.
///
/// The deferring counterpart of cn1nbCompanionInit, for the operations that
/// have somewhere to resume to -- everything with a requestId, which is every
/// companion operation except the synchronous read.
static void cn1nbCompanionWhenActive(void (^handler)(CN1NearbyCompanion *))
        API_AVAILABLE(ios(18.0)) {
    if (cn1nbCompanion == nil) {
        cn1nbCompanion = [[CN1NearbyCompanion alloc] init];
    }
    CN1NearbyCompanion *companion = (CN1NearbyCompanion *)cn1nbCompanion;
    [companion whenActive:^(BOOL active) {
        handler(active ? companion : nil);
    }];
}

static CN1NearbyCompanion *cn1nbCompanionInit(void) API_AVAILABLE(ios(18.0)) {
    if (cn1nbCompanion == nil) {
        cn1nbCompanion = [[CN1NearbyCompanion alloc] init];
    }
    CN1NearbyCompanion *companion = (CN1NearbyCompanion *)cn1nbCompanion;
    [companion activate];
    // Waited for here rather than at each call site, so nothing reaches
    // showPicker or session.accessories before the session is usable -- and
    // nil when it never became usable, so a caller cannot proceed on an
    // inactive session and blame the result on the accessory. Activation can
    // fail outright, not only run late.
    if (![companion awaitActive]) {
        return nil;
    }
    return companion;
}

#endif // CN1_NEARBY_HAS_ASK

// =====================================================================
// Natives
// =====================================================================

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_nearbyRangingSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_NI
    if (@available(iOS 16.0, *)) {
        return NISession.deviceCapabilities.supportsPreciseDistanceMeasurement
                ? JAVA_TRUE : JAVA_FALSE;
    }
    if (@available(iOS 14.0, *)) {
        return NISession.isSupported ? JAVA_TRUE : JAVA_FALSE;
    }
#endif
    return JAVA_FALSE;
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_nearbyCompanionSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_ASK
    if (@available(iOS 18.0, *)) {
        return JAVA_TRUE;
    }
#endif
    return JAVA_FALSE;
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_nearbyTransportSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_MPC
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_INT
com_codename1_impl_ios_IOSNative_nearbyRangingAvailability___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_NI
    if (@available(iOS 14.0, *)) {
        JAVA_BOOLEAN supported =
                com_codename1_impl_ios_IOSNative_nearbyRangingSupported___R_boolean(
                        CN1_THREAD_STATE_PASS_ARG me);
        return supported == JAVA_TRUE ? CN1_NEARBY_AVAIL_AVAILABLE
                : CN1_NEARBY_AVAIL_NOT_SUPPORTED;
    }
#endif
    return CN1_NEARBY_AVAIL_NOT_SUPPORTED;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_nearbyCompanionAvailability___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_ASK
    if (@available(iOS 18.0, *)) {
        return CN1_NEARBY_AVAIL_AVAILABLE;
    }
#endif
    return CN1_NEARBY_AVAIL_NOT_SUPPORTED;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_nearbyTransportAvailability___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_MPC
    return CN1_NEARBY_AVAIL_AVAILABLE;
#else
    return CN1_NEARBY_AVAIL_NOT_SUPPORTED;
#endif
}

JAVA_INT
com_codename1_impl_ios_IOSNative_nearbyRangingCapabilities___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    JAVA_INT bits = 0;
#ifdef CN1_NEARBY_HAS_NI
    if (@available(iOS 16.0, *)) {
        id<NIDeviceCapability> caps = NISession.deviceCapabilities;
        if (caps.supportsPreciseDistanceMeasurement) {
            bits |= CN1_NEARBY_CAP_DISTANCE;
        }
        if (caps.supportsDirectionMeasurement) {
            // Apple reports one direction capability and produces a full
            // vector, so azimuth and elevation stand or fall together here.
            bits |= CN1_NEARBY_CAP_DIRECTION | CN1_NEARBY_CAP_ELEVATION;
        }
        if (caps.supportsCameraAssistance) {
            bits |= CN1_NEARBY_CAP_CAMERA_ASSISTANCE;
        }
        if (bits != 0) {
            bits |= CN1_NEARBY_CAP_ACCESSORY;
        }
    } else if (@available(iOS 14.0, *)) {
        if (NISession.isSupported) {
            bits = CN1_NEARBY_CAP_DISTANCE | CN1_NEARBY_CAP_DIRECTION
                    | CN1_NEARBY_CAP_ELEVATION;
            if (@available(iOS 15.0, *)) {
                bits |= CN1_NEARBY_CAP_ACCESSORY;
            }
        }
    }
    // CAP_BACKGROUND is reported from the app's own configuration, not
    // assumed either way.
    //
    // Background ranging needs the com.apple.developer.nearby-interaction
    // entitlement AND the nearby-interaction background mode, which the
    // builder injects together and only for ios.nearby.background=true --
    // because the entitlement has to be enabled on the App ID first, so it
    // cannot be turned on for everyone. Never setting the bit made
    // isBackgroundRangingSupported() false even in the configuration that
    // enables the feature, so an app that gates on it disabled ranging it
    // actually had.
    //
    // The background MODE is the signal: it is in the Info.plist, which is
    // readable at runtime, whereas the entitlement is not -- and the builder
    // writes neither without the other.
    NSArray *modes = [[NSBundle mainBundle]
            objectForInfoDictionaryKey:@"UIBackgroundModes"];
    if ([modes isKindOfClass:[NSArray class]]
            && [modes containsObject:@"nearby-interaction"]) {
        bits |= CN1_NEARBY_CAP_BACKGROUND;
    }
#endif
    return bits;
}

void com_codename1_impl_ios_IOSNative_nearbyRequestPermissions___int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT permissionBits) {
    // iOS has nothing to ask for up front: Nearby Interaction prompts on the
    // first session and the local network prompt appears on the first browse.
    // The answer still has to arrive rather than not, because a caller is
    // holding a resource.
    com_codename1_impl_ios_IOSNearbyCallbacks_permissionResult___int_boolean(
            CN1_THREAD_STATE_PASS_ARG requestId, JAVA_TRUE);
}

void com_codename1_impl_ios_IOSNative_nearbyPrepareSession___int_int_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT sessionHandle, JAVA_BOOLEAN controller) {
#ifdef CN1_NEARBY_HAS_NI
    if (@available(iOS 14.0, *)) {
        @autoreleasepool {
            if (!NISession.isSupported) {
                cn1nbFailRanging(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
                        @"this device has no ultra-wideband radio");
                return;
            }
            cn1nbSessionsInit();
            CN1NearbyRangingSession *entry =
                    [[[CN1NearbyRangingSession alloc] init] autorelease];
            entry.handle = sessionHandle;
            entry.session = [[[NISession alloc] init] autorelease];
            entry.session.delegate = entry;
            NIDiscoveryToken *token = entry.session.discoveryToken;
            if (token == nil) {
                cn1nbFailRanging(requestId, CN1_NEARBY_ERR_SESSION_FAILED,
                        @"the session produced no discovery token");
                return;
            }
            NSError *err = nil;
            NSData *archived =
                    [NSKeyedArchiver archivedDataWithRootObject:token
                                          requiringSecureCoding:YES
                                                          error:&err];
            if (archived == nil) {
                cn1nbFailRanging(requestId, CN1_NEARBY_ERR_SESSION_FAILED,
                        [err localizedDescription]);
                return;
            }
            @synchronized (cn1nbSessionsLock) {
                [cn1nbSessions setObject:entry
                        forKey:[NSNumber numberWithInt:sessionHandle]];
            }
            com_codename1_impl_ios_IOSNearbyCallbacks_sessionPrepared___int_int_boolean_byte_1ARRAY(
                    CN1_THREAD_STATE_PASS_ARG requestId, sessionHandle,
                    controller, cn1nbJBytes(archived));
            return;
        }
    }
#endif
    cn1nbFailRanging(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"this build does not include precision ranging");
}

void com_codename1_impl_ios_IOSNative_nearbyStartRanging___int_int_byte_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT sessionHandle, JAVA_OBJECT peerToken) {
#ifdef CN1_NEARBY_HAS_NI
    if (@available(iOS 14.0, *)) {
        @autoreleasepool {
            CN1NearbyRangingSession *entry = cn1nbSessionFor(sessionHandle);
            if (entry == nil) {
                cn1nbFailRanging(requestId,
                        CN1_NEARBY_ERR_SESSION_INVALIDATED, @"no such session");
                return;
            }
            NSData *raw = cn1nbDataFromJavaArray(peerToken);
            // The framing NearbyWire puts around a token: magic, version,
            // platform, length. Stripping it here rather than in Java keeps
            // the native interface to plain bytes.
            if (raw == nil || [raw length] < 10) {
                cn1nbFailRanging(requestId, CN1_NEARBY_ERR_INVALID_TOKEN,
                        @"the peer token is not a Codename One token");
                return;
            }
            const unsigned char *b = (const unsigned char *)[raw bytes];
            if (b[0] != 'C' || b[1] != 'N' || b[2] != '1' || b[3] != 'R'
                    || b[5] != 1) {
                cn1nbFailRanging(requestId, CN1_NEARBY_ERR_INVALID_TOKEN,
                        @"this token was minted by another platform");
                return;
            }
            NSData *payload = [raw subdataWithRange:
                    NSMakeRange(10, [raw length] - 10)];
            NSError *err = nil;
            NIDiscoveryToken *token =
                    [NSKeyedUnarchiver unarchivedObjectOfClass:
                            [NIDiscoveryToken class] fromData:payload
                                                         error:&err];
            if (token == nil) {
                cn1nbFailRanging(requestId, CN1_NEARBY_ERR_INVALID_TOKEN,
                        [err localizedDescription]);
                return;
            }
            NINearbyPeerConfiguration *config =
                    [[[NINearbyPeerConfiguration alloc]
                            initWithPeerToken:token] autorelease];
            entry.pendingStartRequest = requestId;
            [entry.session runWithConfiguration:config];
            cn1nbSettleRangingStart(entry);
            return;
        }
    }
#endif
    cn1nbFailRanging(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"this build does not include precision ranging");
}

void com_codename1_impl_ios_IOSNative_nearbyStartAccessoryRanging___int_int_byte_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT sessionHandle, JAVA_OBJECT accessoryData) {
#ifdef CN1_NEARBY_HAS_NI
    if (@available(iOS 15.0, *)) {
        @autoreleasepool {
            CN1NearbyRangingSession *entry = cn1nbSessionFor(sessionHandle);
            if (entry == nil) {
                cn1nbFailRanging(requestId,
                        CN1_NEARBY_ERR_SESSION_INVALIDATED, @"no such session");
                return;
            }
            NSData *data = cn1nbDataFromJavaArray(accessoryData);
            if (data == nil || [data length] == 0) {
                cn1nbFailRanging(requestId, CN1_NEARBY_ERR_INVALID_TOKEN,
                        @"accessory configuration data is required");
                return;
            }
            NSError *err = nil;
            NINearbyAccessoryConfiguration *config =
                    [[[NINearbyAccessoryConfiguration alloc]
                            initWithData:data error:&err] autorelease];
            if (config == nil) {
                cn1nbFailRanging(requestId, CN1_NEARBY_ERR_INVALID_TOKEN,
                        [err localizedDescription]);
                return;
            }
            // Answered from the delegate, not here: the accessory protocol
            // needs the shareable configuration data the session generates,
            // and that arrives asynchronously.
            entry.pendingStartRequest = requestId;
            [entry.session runWithConfiguration:config];
            return;
        }
    }
#endif
    cn1nbFailRanging(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"accessory ranging needs iOS 15 or later");
}

void com_codename1_impl_ios_IOSNative_nearbyStopSession___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT sessionHandle) {
#ifdef CN1_NEARBY_HAS_NI
    if (@available(iOS 14.0, *)) {
        @autoreleasepool {
            CN1NearbyRangingSession *entry = cn1nbSessionFor(sessionHandle);
            if (entry != nil) {
                // A start still waiting for its answer has to be failed FIRST.
                // startAccessory is answered from
                // didGenerateShareableConfigurationData, and clearing the
                // delegate below silences both that and
                // didInvalidateWithError -- so stopping mid-handshake left the
                // caller's AsyncResource pending with nothing left alive to
                // settle it.
                int pending = entry.pendingStartRequest;
                entry.pendingStartRequest = 0;
                if (pending != 0) {
                    cn1nbFailRanging(pending,
                            CN1_NEARBY_ERR_SESSION_INVALIDATED,
                            @"the session was stopped before the accessory"
                            @" handshake completed");
                }
                // Cleared before invalidate so the delegate callback that
                // invalidation triggers finds nothing left to report -- the
                // app asked for this and does not need to be told.
                @synchronized (cn1nbSessionsLock) {
                    [cn1nbSessions removeObjectForKey:
                            [NSNumber numberWithInt:sessionHandle]];
                }
                entry.session.delegate = nil;
                [entry.session invalidate];
            }
        }
    }
#endif
}

// ---- Companion ------------------------------------------------------

void com_codename1_impl_ios_IOSNative_nearbyAssociate___int_int_boolean_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT profile, JAVA_BOOLEAN singleDevice,
        JAVA_OBJECT joinedFilters) {
#ifdef CN1_NEARBY_HAS_ASK
    if (@available(iOS 18.0, *)) {
        @autoreleasepool {
            NSString *joined = toNSString(CN1_THREAD_STATE_PASS_ARG
                                          joinedFilters);
            NSMutableArray *items = [NSMutableArray array];
            BOOL unsupportedFilter = NO;
            NSUInteger filterCount = 0;
            for (NSString *line in cn1nbSplitLines(joined)) {
                NSArray *fields = [line componentsSeparatedByString:@"\t"];
                if ([fields count] < 2) {
                    continue;
                }
                filterCount++;
                int kind = [[fields objectAtIndex:0] intValue];
                NSString *value = [fields objectAtIndex:1];
                ASDiscoveryDescriptor *descriptor =
                        [[[ASDiscoveryDescriptor alloc] init] autorelease];
                descriptor.supportedOptions =
                        ASAccessorySupportBluetoothPairingLE;
                if (kind == CN1_NEARBY_FILTER_BLE_SERVICE) {
                    @try {
                        descriptor.bluetoothServiceUUID =
                                [CBUUID UUIDWithString:value];
                    } @catch (NSException *bad) {
                        // CBUUID raises on a malformed UUID rather than
                        // returning nil, and one bad filter must not take the
                        // whole picker down.
                        continue;
                    }
                } else if (kind == CN1_NEARBY_FILTER_NAME_PATTERN) {
                    // A substring, not a regular expression: this is the
                    // weakest of the three backends and the portable
                    // documentation says so.
                    descriptor.bluetoothNameSubstring = value;
                } else if (kind == CN1_NEARBY_FILTER_WIFI_SSID) {
                    descriptor.SSID = value;
                } else {
                    // KIND_ADDRESS. AccessorySetupKit discovers accessories;
                    // it has no way to be pointed at one identifier. Skipping
                    // the filter used to leave `items` empty, and the
                    // fallback below then filled the picker from every
                    // service the plist declares -- so an exact-device
                    // reconnect offered unrelated accessories and could
                    // associate one. Refused instead: an address filter this
                    // platform cannot honour is not a filter it may ignore.
                    unsupportedFilter = YES;
                    continue;
                }
                ASPickerDisplayItem *item = [[[ASPickerDisplayItem alloc]
                        initWithName:value
                        productImage:[[[UIImage alloc] init] autorelease]
                          descriptor:descriptor] autorelease];
                [items addObject:item];
            }
            if (unsupportedFilter) {
                cn1nbFailCompanion(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
                        @"AccessorySetupKit cannot search for one exact"
                         @" address; filter by service UUID or name instead");
                return;
            }
            if ([items count] == 0 && filterCount > 0) {
                // Filters were supplied and none produced an item, so the
                // request asked for something this platform cannot express.
                // The broad fallback below is for a genuinely EMPTY filter
                // list, and using it here would answer a narrow request with
                // the widest possible picker.
                cn1nbFailCompanion(requestId, CN1_NEARBY_ERR_INVALID_TOKEN,
                        @"none of the supplied device filters could be used");
                return;
            }
            if ([items count] == 0) {
                // No usable filter. The portable API documents an empty
                // filter list as "offer every visible device", and the facade
                // builds exactly that for associate(null) -- so failing here
                // rejected the API's own default request. AccessorySetupKit
                // cannot discover anything it was not told about ahead of
                // time, but the app HAS told it: NSAccessorySetupBluetoothServices
                // in the Info.plist is the complete set it is allowed to see,
                // which is as close to "everything visible" as this platform
                // has.
                NSArray *declared = [[NSBundle mainBundle]
                        objectForInfoDictionaryKey:
                                @"NSAccessorySetupBluetoothServices"];
                if ([declared isKindOfClass:[NSArray class]]) {
                    for (id entry in declared) {
                        if (![entry isKindOfClass:[NSString class]]) {
                            continue;
                        }
                        ASDiscoveryDescriptor *d =
                                [[[ASDiscoveryDescriptor alloc] init]
                                        autorelease];
                        d.supportedOptions =
                                ASAccessorySupportBluetoothPairingLE;
                        @try {
                            d.bluetoothServiceUUID =
                                    [CBUUID UUIDWithString:(NSString *)entry];
                        } @catch (NSException *bad) {
                            continue;
                        }
                        [items addObject:[[[ASPickerDisplayItem alloc]
                                initWithName:(NSString *)entry
                                productImage:[[[UIImage alloc] init]
                                        autorelease]
                                  descriptor:d] autorelease]];
                    }
                }
            }
            if ([items count] == 0) {
                cn1nbFailCompanion(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
                        @"AccessorySetupKit shows only accessories declared up"
                        @" front: pass a DeviceFilter, or set the"
                        @" ios.nearby.accessoryServices build hint");
                return;
            }
            // Resumed from the activation handler rather than waited for.
            // The session is not usable until AccessorySetupKit says it is,
            // and the thread that reached here is the EDT -- the thread that
            // draws. Blocking it for as long as activation takes froze input
            // and rendering on the first companion call of a process, which
            // is exactly when an app is likely to make one.
            cn1nbCompanionWhenActive(^(CN1NearbyCompanion *companion) {
                if (companion == nil) {
                    cn1nbFailCompanion(requestId,
                            CN1_NEARBY_ERR_RADIO_UNAVAILABLE,
                            @"AccessorySetupKit did not become active");
                    return;
                }
                // Taken BEFORE the picker opens, so the accessory it adds can be
                // told apart from the ones this app already had.
                NSMutableSet *before = [NSMutableSet set];
                for (ASAccessory *a in companion.session.accessories) {
                    [before addObject:cn1nbAccessoryId(a)];
                }
                [companion.session showPickerForDisplayItems:items
                                           completionHandler:^(NSError *error) {
                    @autoreleasepool {
                        if (error != nil) {
                            cn1nbFailCompanion(requestId,
                                    CN1_NEARBY_ERR_USER_CANCELED,
                                    [error localizedDescription]);
                            return;
                        }
                        // The one that is NEW, not the last in the array. The
                        // accessories array documents no order, so an app that
                        // already held associations could be handed one the user
                        // did not pick -- and then persist or disassociate the
                        // wrong device.
                        ASAccessory *picked = nil;
                        for (ASAccessory *a in companion.session.accessories) {
                            if (![before containsObject:cn1nbAccessoryId(a)]) {
                                if (picked != nil) {
                                    // Two arrived while the picker was open;
                                    // neither can be claimed as the user's pick.
                                    picked = nil;
                                    break;
                                }
                                picked = a;
                            }
                        }
                        if (picked == nil) {
                            cn1nbFailCompanion(requestId,
                                    CN1_NEARBY_ERR_USER_CANCELED,
                                    @"the picker added no accessory this app"
                                     @" did not already have");
                            return;
                        }
                        // present:NO. Associating an accessory says the user
                        // chose it, not that it is in range -- and this port
                        // reports no presence at all, so claiming YES here was
                        // the one place a CompanionDevice arrived on iOS
                        // asserting something nothing would ever correct.
                        com_codename1_impl_ios_IOSNearbyCallbacks_associated___int_java_lang_String(
                                getThreadLocalData(), requestId,
                                cn1nbJString([companion encode:picked
                                                       present:NO]));
                    }
                }];
            });
            return;
        }
    }
#endif
    cn1nbFailCompanion(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"companion association needs iOS 18 or later");
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_nearbyAssociations___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_ASK
    if (@available(iOS 18.0, *)) {
        @autoreleasepool {
            CN1NearbyCompanion *companion = cn1nbCompanionInit();
            if (companion == nil) {
                // getAssociations is synchronous and has no error channel, so
                // an inactive session can only answer with nothing. The
                // operations that CAN report a failure do.
                return cn1nbJString(@"");
            }
            NSMutableArray *lines = [NSMutableArray array];
            for (ASAccessory *a in companion.session.accessories) {
                [lines addObject:[companion encode:a present:NO]];
            }
            return cn1nbJString([lines componentsJoinedByString:@"\n"]);
        }
    }
#endif
    return cn1nbJString(@"");
}

void com_codename1_impl_ios_IOSNative_nearbyDisassociate___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT associationId) {
#ifdef CN1_NEARBY_HAS_ASK
    if (@available(iOS 18.0, *)) {
        @autoreleasepool {
            // Resolved on this thread, because toNSString needs the thread
            // state the native was entered with and the block below does not
            // run on that thread.
            NSString *aid = toNSString(CN1_THREAD_STATE_PASS_ARG associationId);
            // Deferred for the reason associate is.
            cn1nbCompanionWhenActive(^(CN1NearbyCompanion *companion) {
                if (companion == nil) {
                    // Distinguished from "no such association", which is what an
                    // inactive session used to look like.
                    cn1nbFailCompanion(requestId,
                            CN1_NEARBY_ERR_RADIO_UNAVAILABLE,
                            @"AccessorySetupKit did not become active");
                    return;
                }
                ASAccessory *accessory = [companion accessoryForId:aid];
                if (accessory == nil) {
                    cn1nbFailCompanion(requestId, CN1_NEARBY_ERR_PEER_UNAVAILABLE,
                            @"no such association");
                    return;
                }
                [companion.session removeAccessory:accessory
                                 completionHandler:^(NSError *error) {
                    @autoreleasepool {
                        if (error != nil) {
                            cn1nbFailCompanion(requestId, CN1_NEARBY_ERR_UNKNOWN,
                                    [error localizedDescription]);
                        } else {
                            com_codename1_impl_ios_IOSNearbyCallbacks_disassociated___int(
                                    getThreadLocalData(), requestId);
                        }
                    }
                }];
            });
            return;
        }
    }
#endif
    cn1nbFailCompanion(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"companion association needs iOS 18 or later");
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_nearbyStartObservingPresence___java_lang_String_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT associationId) {
    // False on purpose, and documented as such in the guide's capability
    // matrix. AccessorySetupKit reports an accessory being added to or removed
    // from the app's set, which is a different event from it coming into
    // range: an accessory sitting in a drawer stays "added". Reporting those
    // as presence would tell an app the device is nearby when it is not, and
    // an app that believed it would show a live reading for something it
    // cannot reach. Android has real presence; on iOS the honest answer is
    // that the app should scan with com.codename1.bluetooth instead.
    return JAVA_FALSE;
}

void com_codename1_impl_ios_IOSNative_nearbyStopObservingPresence___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT associationId) {
}

// ---- Transport ------------------------------------------------------

JAVA_INT com_codename1_impl_ios_IOSNative_nearbyMaxPayloadSize___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_MPC
    // MultipeerConnectivity has no published limit for sendData, but it
    // degrades badly past a few tens of kilobytes and the portable API
    // promises one number an app can rely on everywhere. So this is Android's
    // number: its 32K Nearby Connections ceiling less the four bytes its
    // payload-id header takes, which is the tightest of the real backends.
    // Anything else and "fits here fits everywhere" is false by four bytes.
    return 32 * 1024 - 4;
#else
    return 0;
#endif
}

void com_codename1_impl_ios_IOSNative_nearbyStartAdvertising___int_java_lang_String_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT serviceId, JAVA_OBJECT localName, JAVA_INT strategy) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        NSString *sid = toNSString(CN1_THREAD_STATE_PASS_ARG serviceId);
        if (!cn1nbServiceTypeIsDeclared(sid)) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_SESSION_FAILED,
                    cn1nbUndeclaredServiceMessage(sid));
            return;
        }
        NSString *name = toNSString(CN1_THREAD_STATE_PASS_ARG localName);
        CN1NearbyTransport *t = cn1nbTransportInit(sid, name, YES);
        if (t.advertiser != nil) {
            // The delegate goes with it. A replaced advertiser can still
            // deliver didNotStartAdvertisingPeer, and that callback consumes
            // pendingAdvertiseRequest -- which by then belongs to the
            // REPLACEMENT, so the new start was failed with the old
            // advertiser's error.
            t.advertiser.delegate = nil;
            [t.advertiser stopAdvertisingPeer];
            t.advertiser = nil;
        }
        t.advertiser = [[[MCNearbyServiceAdvertiser alloc]
                initWithPeer:t.localPeer
                discoveryInfo:nil
                serviceType:t.advertiseServiceType] autorelease];
        t.advertiser.delegate = t;
        // Recorded BEFORE the answer: didNotStartAdvertisingPeer can fire
        // after this returns, and it needs the id to fail.
        t.advertiseStrategy = (int)strategy;
        // A start within the grace period of an earlier one replaces its
        // pending id, and the earlier settler would then see a mismatch and
        // return -- leaving that caller's AsyncResource pending for good.
        // Answered on the way out: advertising did start, and the newer call
        // is what changed it.
        int superseded = t.pendingAdvertiseRequest;
        t.pendingAdvertiseRequest = requestId;
        if (superseded != 0 && superseded != requestId) {
            cn1nbTransportOk(superseded);
        }
        [t.advertiser startAdvertisingPeer];
        cn1nbSettleTransportStart(t, YES, requestId);
        return;
    }
#endif
    cn1nbFailTransport(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"this build does not include the nearby transport");
}

void com_codename1_impl_ios_IOSNative_nearbyStopAdvertising__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        if (cn1nbTransport != nil && cn1nbTransport.advertiser != nil) {
            [cn1nbTransport.advertiser stopAdvertisingPeer];
            cn1nbTransport.advertiser.delegate = nil;
            cn1nbTransport.advertiser = nil;
        }
        // Settled on the way out, and OUTSIDE the advertiser check. Stopping
        // before the grace period elapsed would otherwise leave the start's
        // AsyncResource unresolved for good, because the deferred answer
        // only fires for a request that is still pending.
        cn1nbCancelPendingStart(cn1nbTransport, YES);
    }
#endif
}

void com_codename1_impl_ios_IOSNative_nearbyStartDiscovery___int_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT serviceId, JAVA_INT strategy) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        NSString *sid = toNSString(CN1_THREAD_STATE_PASS_ARG serviceId);
        if (!cn1nbServiceTypeIsDeclared(sid)) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_SESSION_FAILED,
                    cn1nbUndeclaredServiceMessage(sid));
            return;
        }
        CN1NearbyTransport *t = cn1nbTransportInit(sid, nil, NO);
        if (t.browser != nil) {
            // Detached for the reason the advertiser above is.
            t.browser.delegate = nil;
            [t.browser stopBrowsingForPeers];
            t.browser = nil;
        }
        t.browser = [[[MCNearbyServiceBrowser alloc]
                initWithPeer:t.localPeer
                serviceType:t.discoverServiceType] autorelease];
        t.browser.delegate = t;
        t.discoverStrategy = (int)strategy;
        // Answered for the reason the advertising path is.
        int superseded = t.pendingDiscoverRequest;
        t.pendingDiscoverRequest = requestId;
        if (superseded != 0 && superseded != requestId) {
            cn1nbTransportOk(superseded);
        }
        [t.browser startBrowsingForPeers];
        cn1nbSettleTransportStart(t, NO, requestId);
        return;
    }
#endif
    cn1nbFailTransport(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"this build does not include the nearby transport");
}

void com_codename1_impl_ios_IOSNative_nearbyStopDiscovery__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        if (cn1nbTransport != nil && cn1nbTransport.browser != nil) {
            [cn1nbTransport.browser stopBrowsingForPeers];
            cn1nbTransport.browser.delegate = nil;
            cn1nbTransport.browser = nil;
        }
        // Settled on the way out, for the reason stopAdvertising is.
        cn1nbCancelPendingStart(cn1nbTransport, NO);
    }
#endif
}

void com_codename1_impl_ios_IOSNative_nearbyRequestConnection___int_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT endpointId, JAVA_OBJECT localName) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        if (cn1nbTransport == nil || cn1nbTransport.browser == nil) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_SESSION_FAILED,
                    @"start discovery before requesting a connection");
            return;
        }
        NSString *pid = toNSString(CN1_THREAD_STATE_PASS_ARG endpointId);
        MCPeerID *peer = [cn1nbTransport peerForId:pid];
        if (peer == nil) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_PEER_UNAVAILABLE,
                    @"no such endpoint");
            return;
        }
        // This device is the one CONNECTING, so both STAR and POINT_TO_POINT
        // allow it exactly one peer: under STAR it is the many, not the one.
        if (cn1nbTransport.discoverStrategy != CN1_NEARBY_STRATEGY_CLUSTER
                && [cn1nbTransport heldPeerCount] > 0) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_BUSY,
                    cn1nbTransport.discoverStrategy
                            == CN1_NEARBY_STRATEGY_POINT_TO_POINT
                    ? @"POINT_TO_POINT allows one connection at a time;"
                       @" disconnect the current peer first"
                    : @"a STAR discoverer holds one connection at a time;"
                       @" disconnect the current peer first");
            return;
        }
        // The name the caller wants the invited peer to see. Applied before
        // the invitation goes out, or it would carry the previous identity.
        cn1nbApplyLocalName(cn1nbTransport,
                toNSString(CN1_THREAD_STATE_PASS_ARG localName));
        peer = [cn1nbTransport peerForId:pid];
        if (peer == nil || cn1nbTransport.browser == nil) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_PEER_UNAVAILABLE,
                    @"the endpoint was lost while renaming this device");
            return;
        }
        // Reserved BEFORE the invitation goes out, so a second
        // requestConnection made while this one is still unanswered sees the
        // slot taken.
        [cn1nbTransport markInviting:pid];
        [cn1nbTransport.browser invitePeer:peer
                                 toSession:[cn1nbTransport sessionFor:pid]
                               withContext:nil
                                   timeout:30];
        cn1nbTransportOk(requestId);
        return;
    }
#endif
    cn1nbFailTransport(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"this build does not include the nearby transport");
}

void com_codename1_impl_ios_IOSNative_nearbyAcceptConnection___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT endpointId) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        NSString *pid = toNSString(CN1_THREAD_STATE_PASS_ARG endpointId);
        // Retained across the removal. The dictionary is the only owner of
        // the copied block by the time the app answers -- the pool the
        // delegate autoreleased it into drained long ago -- so removing the
        // entry first freed the block and calling it crashed.
        void (^handler)(BOOL, MCSession *) =
                cn1nbTransport == nil ? nil
                        : [cn1nbTransport takeInvitationForPeer:pid];
        if (handler == nil) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_PEER_UNAVAILABLE,
                    @"there is no invitation from that endpoint");
            return;
        }
        // POINT_TO_POINT means one connection on EACH side, so the
        // advertiser is bounded too. STAR is not: accepting many is what
        // makes this device the star's centre.
        if (cn1nbTransport.advertiseStrategy
                        == CN1_NEARBY_STRATEGY_POINT_TO_POINT
                && [cn1nbTransport heldPeerCount] > 0) {
            handler(NO, nil);
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_BUSY,
                    @"POINT_TO_POINT allows one connection at a time;"
                     @" disconnect the current peer first");
            return;
        }
        // Reserved on ACCEPT, not only on invite. Two incoming invitations
        // accepted before either session reaches Connected both saw a held
        // count of zero -- taking the invitation adds it to nothing -- so
        // POINT_TO_POINT allowed the second one through. Released by the
        // state change, whichever way it goes.
        [cn1nbTransport markInviting:pid];
        handler(YES, [cn1nbTransport sessionFor:pid]);
        cn1nbTransportOk(requestId);
        return;
    }
#endif
    cn1nbFailTransport(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"this build does not include the nearby transport");
}

void com_codename1_impl_ios_IOSNative_nearbyRejectConnection___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT endpointId) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        if (cn1nbTransport == nil) {
            return;
        }
        NSString *pid = toNSString(CN1_THREAD_STATE_PASS_ARG endpointId);
        void (^handler)(BOOL, MCSession *) =
                [cn1nbTransport takeInvitationForPeer:pid];
        if (handler != nil) {
            handler(NO, nil);
        }
    }
#endif
}

void com_codename1_impl_ios_IOSNative_nearbySendPayload___int_java_lang_String_int_int_byte_1ARRAY_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT joinedEndpointIds, JAVA_INT payloadId,
        JAVA_INT payloadType, JAVA_OBJECT bytes, JAVA_OBJECT path) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        if (cn1nbTransport == nil) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_SESSION_FAILED,
                    @"the transport is not running");
            return;
        }
        NSString *joined = toNSString(CN1_THREAD_STATE_PASS_ARG
                                      joinedEndpointIds);
        NSMutableArray *peers = [NSMutableArray array];
        NSMutableArray *peerIds = [NSMutableArray array];
        NSMutableArray *unknown = [NSMutableArray array];
        for (NSString *pid in cn1nbSplitLines(joined)) {
            MCPeerID *peer = [cn1nbTransport peerForId:pid];
            if (peer != nil) {
                [peers addObject:peer];
                [peerIds addObject:pid];
            } else {
                [unknown addObject:pid];
            }
        }
        if ([peers count] == 0) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_PEER_UNAVAILABLE,
                    @"none of those endpoints is connected");
            return;
        }
        if ([unknown count] > 0) {
            // A requested endpoint this transport no longer knows is a FAILED
            // handoff, not a silent omission. Sending to the rest and
            // answering successfully left the omitted recipient with neither
            // the data nor a progress event of any kind -- while the same
            // send reports a per-recipient failure for a peer that is merely
            // unreachable, which is the lesser problem of the two.
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_PEER_UNAVAILABLE,
                    [NSString stringWithFormat:
                            @"these endpoints are no longer connected: %@",
                            [unknown componentsJoinedByString:@", "]]);
            return;
        }
        if (payloadType == CN1_NEARBY_PAYLOAD_FILE) {
            NSString *p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
            if ([p hasPrefix:@"file://"]) {
                p = [p substringFromIndex:7];
            }
            NSURL *url = [NSURL fileURLWithPath:p];
            // The sender's payload id rides in the resource NAME: a resource
            // transfer carries no other metadata, and the receiver otherwise
            // had nothing to report but zero.
            NSString *sentName = [NSString stringWithFormat:@"cn1id-%d-%@",
                    (int)payloadId, [p lastPathComponent]];
            // Read once, here, so the terminal update can report the size
            // the transfer moved. MultipeerConnectivity's completion handler
            // carries an error and nothing else, and reporting zero moved
            // and no total contradicted every progress update before it --
            // so a listener that finalises its display from the terminal
            // event recorded a finished file as having transferred nothing.
            NSNumber *fileSize = [[[NSFileManager defaultManager]
                    attributesOfItemAtPath:p error:NULL]
                    objectForKey:NSFileSize];
            JAVA_LONG fileBytes = fileSize == nil
                    ? -1 : (JAVA_LONG)[fileSize longLongValue];
            NSUInteger started = 0;
            for (NSUInteger i = 0; i < [peers count]; i++) {
                MCPeerID *peer = [peers objectAtIndex:i];
                MCSession *session = [cn1nbTransport
                        sessionFor:[peerIds objectAtIndex:i]];
                // Captured by the completion block so it can forget exactly
                // its own progress. A block cannot capture the __block-free
                // NSProgress before it exists, so the holder stands in.
                NSMutableArray *progressHolder = [NSMutableArray array];
                NSProgress *progress = [session sendResourceAtURL:url
                        withName:sentName
                          toPeer:peer
           withCompletionHandler:^(NSError *error) {
                    @autoreleasepool {
                        NSString *encoded = [cn1nbTransport encodePeer:peer];
                        // Cancellation is its own status, not a failure.
                        // PayloadStatus.CANCELED exists precisely so an app
                        // can tell "I stopped this" from "the link broke",
                        // and mapping every error to FAILURE hid the one it
                        // caused itself.
                        JAVA_INT status = CN1_NEARBY_PAYLOAD_SUCCESS;
                        if (error != nil) {
                            status = cn1nbWasCancelled(error)
                                    ? CN1_NEARBY_PAYLOAD_CANCELED
                                    : CN1_NEARBY_PAYLOAD_FAILURE;
                        }
                        // SUCCESS means the whole file arrived, so it reports
                        // the whole file. A transfer that stopped short
                        // reports what its progress had reached, which the
                        // NSProgress still holds after the fact.
                        NSProgress *finished = [progressHolder count] > 0
                                ? [progressHolder objectAtIndex:0] : nil;
                        JAVA_LONG moved = status == CN1_NEARBY_PAYLOAD_SUCCESS
                                ? fileBytes
                                : (finished == nil ? 0
                                        : (JAVA_LONG)[finished
                                                completedUnitCount]);
                        com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                                getThreadLocalData(), cn1nbJString(encoded),
                                payloadId, moved, fileBytes, status);
                        // Only THIS recipient's transfer is finished. The
                        // others under the same payload id are still going,
                        // and dropping the whole entry here left them
                        // uncancellable.
                        [cn1nbTransport forgetProgress:progressHolder
                                            forPayload:payloadId];
                    }
                }];
                // Retained so cancel() can actually stop a large transfer.
                // Without it cancelling did nothing at all: the file kept
                // going, kept using the radio, and could still report success.
                //
                // One entry per RECIPIENT. Keyed by payload id alone, a send
                // to three peers stored three progresses under one key and
                // kept only the last, so cancel() stopped one transfer and
                // the other two ran to completion reporting success.
                if (progress != nil) {
                    started++;
                    [progressHolder addObject:progress];
                    [cn1nbTransport rememberProgress:progress
                                          forPayload:payloadId];
                } else {
                    // No NSProgress means the framework did not take the
                    // transfer -- the peer went away, or it could not be
                    // scheduled -- so the completion handler will never run
                    // for it. Reported per recipient, like a failed byte send.
                    com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                            getThreadLocalData(),
                            cn1nbJString([cn1nbTransport encodePeer:peer]),
                            payloadId, 0, -1, CN1_NEARBY_PAYLOAD_FAILURE);
                }
            }
            if (started != [peers count]) {
                // EVERY requested transfer has to have started, not just one.
                // A partial handoff answered successfully told the caller the
                // payload was with the platform while one recipient's
                // transfer had never begun -- and the byte path fails the
                // same case, so the two differed on identical input.
                //
                // The ones that DID start are cancelled, because a send the
                // app has been told failed must not go on to deliver the
                // file to some of its recipients. Bytes cannot be recalled
                // once queued; a file can.
                for (NSProgress *partial in
                        [cn1nbTransport takeProgressesForPayload:payloadId]) {
                    [partial cancel];
                }
                cn1nbFailTransport(requestId, CN1_NEARBY_ERR_IO_ERROR,
                        started == 0
                        ? @"the file could not be handed to any of those"
                           @" endpoints"
                        : @"the file could not be handed to every one of"
                           @" those endpoints");
                return;
            }
            cn1nbTransportOk(requestId);
            return;
        }
        NSData *data = cn1nbDataFromJavaArray(bytes);
        // Framed with the payload id -- see didReceiveData for why.
        NSMutableData *framed = [NSMutableData dataWithCapacity:
                (data == nil ? 0 : [data length])
                        + CN1_NEARBY_FRAME_HEADER];
        unsigned char header[CN1_NEARBY_FRAME_HEADER] = {
            CN1_NEARBY_FRAME_DATA,
            (unsigned char)((payloadId >> 24) & 0xff),
            (unsigned char)((payloadId >> 16) & 0xff),
            (unsigned char)((payloadId >> 8) & 0xff),
            (unsigned char)(payloadId & 0xff)
        };
        [framed appendBytes:header length:CN1_NEARBY_FRAME_HEADER];
        if (data != nil) {
            [framed appendData:data];
        }
        // One send per peer, because each has its own session now -- and one
        // progress update per peer, reporting what happened to THAT peer.
        //
        // Reported per recipient rather than suppressed wholesale. Sending to
        // three peers where the third fails still delivered the payload to
        // the first two, and answering the aggregate request with a failure
        // and then skipping the loop entirely meant neither the recipients
        // that got it nor the one that did not produced any terminal update
        // at all.
        NSError *err = nil;
        BOOL sent = [peers count] > 0;
        for (NSUInteger i = 0; i < [peers count]; i++) {
            MCSession *session = [cn1nbTransport
                    sessionFor:[peerIds objectAtIndex:i]];
            NSString *encoded = [cn1nbTransport
                    encodePeer:[peers objectAtIndex:i]];
            // Registered BEFORE the send, and the progress reported before
            // it too. The acknowledgement can come back on the session queue
            // while this thread is still between the two, and one recipient
            // of a multi-peer send can answer before a later recipient has
            // even been sent to -- so registering afterwards let takeAck see
            // an ack for a send it did not know about, drop it as unknown,
            // and then create an entry that could only ever time out and
            // fail a payload the peer already had. Reporting progress first
            // keeps the order right as well: SUCCESS can now only follow the
            // IN_PROGRESS it belongs to, never precede it.
            [cn1nbTransport awaitAck:payloadId
                            fromPeer:[peerIds objectAtIndex:i]
                              length:(JAVA_LONG)[data length]];
            // Queued, not delivered. sendData returning YES says the message
            // was accepted for sending, and PayloadStatus.SUCCESS documents
            // that every byte ARRIVED -- which is what Android reports,
            // because Nearby tells it so. So this reports progress now and
            // the terminal status when the receiver's acknowledgement comes
            // back, or FAILURE if the peer disconnects first.
            com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                    CN1_THREAD_STATE_PASS_ARG cn1nbJString(encoded), payloadId,
                    (JAVA_LONG)[data length], (JAVA_LONG)[data length],
                    CN1_NEARBY_PAYLOAD_IN_PROGRESS);
            NSError *one = nil;
            BOOL ok = [session sendData:framed
                                toPeers:[NSArray arrayWithObject:
                                        [peers objectAtIndex:i]]
                               withMode:MCSessionSendDataReliable
                                  error:&one];
            if (!ok) {
                sent = NO;
                if (err == nil) {
                    err = one;
                }
                // Taken back, so nothing is left for the timeout to fail a
                // second time. If the entry has already gone the send did
                // reach the peer after all and its ack has been reported --
                // in which case this reports nothing.
                JAVA_LONG unused = -1;
                if ([cn1nbTransport takeAck:payloadId
                                   fromPeer:[peerIds objectAtIndex:i]
                                     length:&unused]) {
                    com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                            CN1_THREAD_STATE_PASS_ARG cn1nbJString(encoded),
                            payloadId, 0, (JAVA_LONG)[data length],
                            CN1_NEARBY_PAYLOAD_FAILURE);
                }
                continue;
            }
            [cn1nbTransport scheduleAckTimeout:payloadId
                                      fromPeer:[peerIds objectAtIndex:i]
                                       encoded:encoded];
        }
        if (!sent) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_IO_ERROR,
                    [err localizedDescription]);
            return;
        }
        cn1nbTransportOk(requestId);
        return;
    }
#endif
    cn1nbFailTransport(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"this build does not include the nearby transport");
}

void com_codename1_impl_ios_IOSNative_nearbyCancelPayload___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT payloadId) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        if (cn1nbTransport == nil) {
            return;
        }
        // A file transfer CAN be recalled -- sendResourceAtURL hands back an
        // NSProgress for exactly that -- so it is, and the completion handler
        // reports the cancellation.
        NSArray *all = [cn1nbTransport takeProgressesForPayload:payloadId];
        for (NSProgress *progress in all) {
            [progress cancel];
        }
        // A byte payload cannot be recalled: sendData has left by the time
        // anything could ask, and MultipeerConnectivity offers no handle on
        // it. The SEND is still cancelled, which is what the portable API
        // promises and what Android and the simulator do -- Nearby cannot
        // recall queued bytes either. Its acknowledgement bookkeeping is
        // taken here and answered CANCELED, so the send reaches the terminal
        // status the caller asked for instead of reporting SUCCESS when the
        // acknowledgement it was already going to get comes back. Dropping
        // the entry is also what makes that later ack a no-op.
        for (NSArray *cancelled in
                [cn1nbTransport takeAcksForPayload:payloadId]) {
            NSString *pid = [cancelled objectAtIndex:0];
            MCPeerID *peer = [cn1nbTransport peerForId:pid];
            if (peer == nil) {
                continue;
            }
            com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                    CN1_THREAD_STATE_PASS_ARG
                    cn1nbJString([cn1nbTransport encodePeer:peer]),
                    payloadId, 0,
                    (JAVA_LONG)[[cancelled objectAtIndex:1] longLongValue],
                    CN1_NEARBY_PAYLOAD_CANCELED);
        }
    }
#endif
}

void com_codename1_impl_ios_IOSNative_nearbyDisconnect___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT endpointId) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        if (cn1nbTransport == nil) {
            return;
        }
        // Drops exactly the endpoint asked for. With one shared MCSession this
        // was impossible -- disconnect tears the whole thing down -- so it
        // used to do nothing at all once a second peer connected, quietly
        // breaking a method the public API documents as dropping one endpoint.
        // Each peer has its own session now, so closing one closes one.
        NSString *pid = toNSString(CN1_THREAD_STATE_PASS_ARG endpointId);
        [cn1nbTransport closeSessionFor:pid];
    }
#endif
}

void com_codename1_impl_ios_IOSNative_nearbyStopAllTransport__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        if (cn1nbTransport == nil) {
            return;
        }
        if (cn1nbTransport.advertiser != nil) {
            [cn1nbTransport.advertiser stopAdvertisingPeer];
            cn1nbTransport.advertiser.delegate = nil;
            cn1nbTransport.advertiser = nil;
        }
        if (cn1nbTransport.browser != nil) {
            [cn1nbTransport.browser stopBrowsingForPeers];
            cn1nbTransport.browser.delegate = nil;
            cn1nbTransport.browser = nil;
        }
        // Both of them, for the reason the single stops settle their own: a
        // start inside its grace period had its advertiser destroyed here
        // while its request id stayed pending, so the deferred settler found
        // it unchanged and reported that a stopped transport had started.
        cn1nbCancelPendingStart(cn1nbTransport, YES);
        cn1nbCancelPendingStart(cn1nbTransport, NO);
        [cn1nbTransport closeAllSessions];
        [cn1nbTransport forgetInvitations];
        [cn1nbTransport forgetAllPeers];
    }
#endif
}

#else // CN1_INCLUDE_NEARBY

// ---------------------------------------------------------------------
// Trampolines for a build that never touched com.codename1.nearby
//
// Every native declared in IOSNative.java has to resolve or the app will not
// link, and each one answers "unsupported" so the public API reports
// NOT_SUPPORTED and every operation fails fast. Nothing here imports Nearby
// Interaction, MultipeerConnectivity or AccessorySetupKit, so an app that
// never asks how far away anything is carries none of their symbols and owes
// none of their privacy strings.
// ---------------------------------------------------------------------

#include "com_codename1_impl_ios_IOSNearbyCallbacks.h"

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_nearbyRangingSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_nearbyCompanionSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_nearbyTransportSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_nearbyRangingAvailability___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return CN1_NEARBY_AVAIL_NOT_SUPPORTED;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_nearbyCompanionAvailability___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return CN1_NEARBY_AVAIL_NOT_SUPPORTED;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_nearbyTransportAvailability___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return CN1_NEARBY_AVAIL_NOT_SUPPORTED;
}

JAVA_INT
com_codename1_impl_ios_IOSNative_nearbyRangingCapabilities___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return 0;
}

void com_codename1_impl_ios_IOSNative_nearbyRequestPermissions___int_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT permissionBits) {
    com_codename1_impl_ios_IOSNearbyCallbacks_permissionResult___int_boolean(
            CN1_THREAD_STATE_PASS_ARG requestId, JAVA_FALSE);
}

void com_codename1_impl_ios_IOSNative_nearbyPrepareSession___int_int_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT sessionHandle, JAVA_BOOLEAN controller) {
    com_codename1_impl_ios_IOSNearbyCallbacks_rangingFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nearbyStartRanging___int_int_byte_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT sessionHandle, JAVA_OBJECT peerToken) {
    com_codename1_impl_ios_IOSNearbyCallbacks_rangingFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nearbyStartAccessoryRanging___int_int_byte_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT sessionHandle, JAVA_OBJECT accessoryData) {
    com_codename1_impl_ios_IOSNearbyCallbacks_rangingFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nearbyStopSession___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT sessionHandle) {
}

void com_codename1_impl_ios_IOSNative_nearbyAssociate___int_int_boolean_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_INT profile, JAVA_BOOLEAN singleDevice,
        JAVA_OBJECT joinedFilters) {
    com_codename1_impl_ios_IOSNearbyCallbacks_companionFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

JAVA_OBJECT
com_codename1_impl_ios_IOSNative_nearbyAssociations___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_NULL;
}

void com_codename1_impl_ios_IOSNative_nearbyDisassociate___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT associationId) {
    com_codename1_impl_ios_IOSNearbyCallbacks_companionFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

JAVA_BOOLEAN
com_codename1_impl_ios_IOSNative_nearbyStartObservingPresence___java_lang_String_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT associationId) {
    return JAVA_FALSE;
}

void com_codename1_impl_ios_IOSNative_nearbyStopObservingPresence___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT associationId) {
}

JAVA_INT com_codename1_impl_ios_IOSNative_nearbyMaxPayloadSize___R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return 0;
}

void com_codename1_impl_ios_IOSNative_nearbyStartAdvertising___int_java_lang_String_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT serviceId, JAVA_OBJECT localName, JAVA_INT strategy) {
    com_codename1_impl_ios_IOSNearbyCallbacks_transportFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nearbyStopAdvertising__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

void com_codename1_impl_ios_IOSNative_nearbyStartDiscovery___int_java_lang_String_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT serviceId, JAVA_INT strategy) {
    com_codename1_impl_ios_IOSNearbyCallbacks_transportFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nearbyStopDiscovery__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

void com_codename1_impl_ios_IOSNative_nearbyRequestConnection___int_java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT endpointId, JAVA_OBJECT localName) {
    com_codename1_impl_ios_IOSNearbyCallbacks_transportFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nearbyAcceptConnection___int_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT endpointId) {
    com_codename1_impl_ios_IOSNearbyCallbacks_transportFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nearbyRejectConnection___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT endpointId) {
}

void com_codename1_impl_ios_IOSNative_nearbySendPayload___int_java_lang_String_int_int_byte_1ARRAY_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT requestId,
        JAVA_OBJECT joinedEndpointIds, JAVA_INT payloadId,
        JAVA_INT payloadType, JAVA_OBJECT bytes, JAVA_OBJECT path) {
    com_codename1_impl_ios_IOSNearbyCallbacks_transportFailed___int_int_java_lang_String(
            CN1_THREAD_STATE_PASS_ARG requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            JAVA_NULL);
}

void com_codename1_impl_ios_IOSNative_nearbyCancelPayload___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT payloadId) {
}

void com_codename1_impl_ios_IOSNative_nearbyDisconnect___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT endpointId) {
}

void com_codename1_impl_ios_IOSNative_nearbyStopAllTransport__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
}

#endif // CN1_INCLUDE_NEARBY
