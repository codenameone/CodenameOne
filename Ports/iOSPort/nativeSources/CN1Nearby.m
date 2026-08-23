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

static void cn1nbTransportOk(int requestId) {
    com_codename1_impl_ios_IOSNearbyCallbacks_transportOk___int(
            getThreadLocalData(), requestId);
}

// =====================================================================
// Ranging -- Nearby Interaction
// =====================================================================

#ifdef CN1_NEARBY_HAS_NI

API_AVAILABLE(ios(14.0))
@interface CN1NearbyRangingSession : NSObject <NISessionDelegate>
@property (nonatomic, assign) int handle;
@property (nonatomic, assign) int pendingStartRequest;
@property (nonatomic, retain) NISession *session;
@end

static NSMutableDictionary *cn1nbSessions = nil;

static void cn1nbSessionsInit(void) {
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

- (void)session:(NISession *)session
        didUpdateNearbyObjects:(NSArray<NINearbyObject *> *)nearbyObjects {
    @autoreleasepool {
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
        [cn1nbSessions removeObjectForKey:[NSNumber numberWithInt:handle]];
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

static CN1NearbyRangingSession *cn1nbSessionFor(int handle)
        API_AVAILABLE(ios(14.0)) {
    cn1nbSessionsInit();
    return [cn1nbSessions objectForKey:[NSNumber numberWithInt:handle]];
}

#endif // CN1_NEARBY_HAS_NI

// =====================================================================
// Transport -- MultipeerConnectivity
// =====================================================================

#ifdef CN1_NEARBY_HAS_MPC

@interface CN1NearbyTransport : NSObject <MCSessionDelegate,
        MCNearbyServiceAdvertiserDelegate, MCNearbyServiceBrowserDelegate>
@property (nonatomic, retain) MCPeerID *localPeer;
@property (nonatomic, retain) MCSession *session;
@property (nonatomic, retain) MCNearbyServiceAdvertiser *advertiser;
@property (nonatomic, retain) MCNearbyServiceBrowser *browser;
@property (nonatomic, retain) NSMutableDictionary *peersById;
@property (nonatomic, retain) NSMutableDictionary *invitations;
@property (nonatomic, retain) NSMutableDictionary *progressByPayload;
@property (nonatomic, retain) NSMutableSet *everConnected;
@property (nonatomic, assign) int pendingAdvertiseRequest;
@property (nonatomic, assign) int pendingDiscoverRequest;
@property (nonatomic, retain) NSString *serviceType;
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
        // "_chat._tcp" -> "chat"
        NSString *name = (NSString *)entry;
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
    [_session release];
    [_advertiser release];
    [_browser release];
    [_peersById release];
    [_invitations release];
    [_progressByPayload release];
    [_everConnected release];
    [_serviceType release];
    [super dealloc];
}

- (NSString *)encodePeer:(MCPeerID *)peer {
    NSString *pid = cn1nbIdForPeer(peer);
    [self.peersById setObject:peer forKey:pid];
    return cn1nbJoin([NSArray arrayWithObjects:pid,
            peer.displayName == nil ? @"" : peer.displayName,
            self.serviceType == nil ? @"" : self.serviceType, nil]);
}

- (MCPeerID *)peerForId:(NSString *)pid {
    return pid == nil ? nil : [self.peersById objectForKey:pid];
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
        if (state == MCSessionStateConnected) {
            [self.everConnected addObject:pid];
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
        if ([self.everConnected containsObject:pid]) {
            [self.everConnected removeObject:pid];
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
        JAVA_INT payloadId = 0;
        NSData *body = data;
        if ([data length] >= 4) {
            const unsigned char *b = (const unsigned char *)[data bytes];
            payloadId = (JAVA_INT)((b[0] << 24) | (b[1] << 16) | (b[2] << 8)
                    | b[3]);
            body = [data subdataWithRange:NSMakeRange(4, [data length] - 4)];
        }
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
        if (error != nil || localURL == nil) {
            com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                    getThreadLocalData(), cn1nbJString(encoded), 0, 0, -1,
                    CN1_NEARBY_PAYLOAD_FAILURE);
            return;
        }
        // The URL the framework hands over is in a temporary location it will
        // delete, so the file is moved somewhere the app can still read when
        // the callback returns.
        NSString *docs = [NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
        NSString *target = [docs stringByAppendingPathComponent:
                [NSString stringWithFormat:@"cn1nearby-%@", resourceName]];
        [[NSFileManager defaultManager] removeItemAtPath:target error:nil];
        NSError *moveError = nil;
        [[NSFileManager defaultManager] moveItemAtPath:[localURL path]
                                                toPath:target
                                                 error:&moveError];
        if (moveError != nil) {
            com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                    getThreadLocalData(), cn1nbJString(encoded), 0, 0, -1,
                    CN1_NEARBY_PAYLOAD_FAILURE);
            return;
        }
        com_codename1_impl_ios_IOSNearbyCallbacks_payloadReceived___java_lang_String_int_int_byte_1ARRAY_java_lang_String(
                getThreadLocalData(), cn1nbJString(encoded), 0,
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
        NSString *pid = cn1nbIdForPeer(peerID);
        NSString *encoded = [self encodePeer:peerID];
        // Copied because the block outlives this call: it is answered when
        // the app calls accept or reject, which is at least an EDT hop away.
        [self.invitations setObject:[[invitationHandler copy] autorelease]
                             forKey:pid];
        com_codename1_impl_ios_IOSNearbyCallbacks_connectionRequested___java_lang_String_java_lang_String(
                getThreadLocalData(), cn1nbJString(encoded),
                cn1nbJString([self tokenForPeer:peerID]));
    }
}

- (void)advertiser:(MCNearbyServiceAdvertiser *)advertiser
        didNotStartAdvertisingPeer:(NSError *)error {
    @autoreleasepool {
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
        NSString *encoded = [self encodePeer:peerID];
        com_codename1_impl_ios_IOSNearbyCallbacks_endpointFound___java_lang_String_boolean(
                getThreadLocalData(), cn1nbJString(encoded), JAVA_TRUE);
    }
}

- (void)browser:(MCNearbyServiceBrowser *)browser
        lostPeer:(MCPeerID *)peerID {
    @autoreleasepool {
        NSString *encoded = [self encodePeer:peerID];
        com_codename1_impl_ios_IOSNearbyCallbacks_endpointFound___java_lang_String_boolean(
                getThreadLocalData(), cn1nbJString(encoded), JAVA_FALSE);
    }
}

- (void)browser:(MCNearbyServiceBrowser *)browser
        didNotStartBrowsingForPeers:(NSError *)error {
    @autoreleasepool {
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
        wanted = [wanted substringToIndex:20];
    }
    if (t.localPeer != nil && wanted != nil
            && ![t.localPeer.displayName isEqualToString:wanted]
            && [t.session.connectedPeers count] == 0) {
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
        t.session.delegate = nil;
        [t.session disconnect];
        t.session = nil;
        t.localPeer = [[[MCPeerID alloc] initWithDisplayName:wanted]
                autorelease];
        t.session = [[[MCSession alloc] initWithPeer:t.localPeer
                                    securityIdentity:nil
                                encryptionPreference:MCEncryptionRequired]
                autorelease];
        t.session.delegate = t;
        if (wasAdvertising) {
            t.advertiser = [[[MCNearbyServiceAdvertiser alloc]
                    initWithPeer:t.localPeer
                   discoveryInfo:nil
                     serviceType:t.serviceType] autorelease];
            t.advertiser.delegate = t;
            [t.advertiser startAdvertisingPeer];
        }
        if (wasBrowsing) {
            t.browser = [[[MCNearbyServiceBrowser alloc]
                    initWithPeer:t.localPeer
                     serviceType:t.serviceType] autorelease];
            t.browser.delegate = t;
            [t.browser startBrowsingForPeers];
        }
        return;
    }
    if (t.localPeer == nil) {
        NSString *name = wanted != nil ? wanted
                : [[UIDevice currentDevice] name];
        if ([name lengthOfBytesUsingEncoding:NSUTF8StringEncoding] > 63) {
            name = [name substringToIndex:20];
        }
        t.localPeer = [[[MCPeerID alloc] initWithDisplayName:name]
                autorelease];
    }
}

static CN1NearbyTransport *cn1nbTransportInit(NSString *serviceId,
        NSString *localName) {
    if (cn1nbTransport == nil) {
        cn1nbTransport = [[CN1NearbyTransport alloc] init];
        cn1nbTransport.peersById = [NSMutableDictionary dictionary];
        cn1nbTransport.invitations = [NSMutableDictionary dictionary];
        cn1nbTransport.progressByPayload = [NSMutableDictionary dictionary];
        cn1nbTransport.everConnected = [NSMutableSet set];
    }
    if (serviceId != nil) {
        // Reassigned on EVERY call, not just the first. Caching it meant
        // stopping discovery for "chat" and starting it for "files" carried on
        // browsing chat, and an app advertising one service while browsing
        // another silently used whichever call came first. The advertiser and
        // browser below are rebuilt per call and read this, so the id the
        // caller passed is the one that takes effect.
        cn1nbTransport.serviceType = cn1nbServiceType(serviceId);
    }
    cn1nbApplyLocalName(cn1nbTransport, localName);
    if (cn1nbTransport.session == nil) {
        cn1nbTransport.session = [[[MCSession alloc]
                initWithPeer:cn1nbTransport.localPeer
                securityIdentity:nil
                encryptionPreference:MCEncryptionRequired] autorelease];
        cn1nbTransport.session.delegate = cn1nbTransport;
    }
    return cn1nbTransport;
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
@end

// Typed as id rather than CN1NearbyCompanion *: a file-scope variable of an
// API_AVAILABLE(ios(18.0)) type is itself flagged as unguarded, and there is
// no availability annotation for a variable declaration to carry.
static id cn1nbCompanion = nil;

@implementation CN1NearbyCompanion

- (void)dealloc {
    [_session release];
    [super dealloc];
}

/// Encodes an accessory the way NearbyWire.decodeCompanionDevice expects.
///
/// The address field carries the per-app CoreBluetooth identifier rather than
/// a MAC address, because that is the only handle iOS gives out -- and it is
/// the same one `BluetoothLE.getPeripheral(String)` takes, which is what makes
/// an association useful rather than decorative.
- (NSString *)encode:(ASAccessory *)accessory present:(BOOL)present {
    NSString *identifier = accessory.bluetoothIdentifier != nil
            ? [accessory.bluetoothIdentifier UUIDString] : @"";
    return cn1nbJoin([NSArray arrayWithObjects:
            identifier.length > 0 ? identifier
                    : [NSString stringWithFormat:@"%lu",
                            (unsigned long)[accessory hash]],
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
    for (ASAccessory *a in self.session.accessories) {
        if (a.bluetoothIdentifier != nil
                && [[a.bluetoothIdentifier UUIDString]
                        isEqualToString:associationId]) {
            return a;
        }
    }
    return nil;
}

- (void)handleEvent:(ASAccessoryEvent *)event {
    @autoreleasepool {
        if (event.accessory == nil) {
            return;
        }
        BOOL added = event.eventType == ASAccessoryEventTypeAccessoryAdded;
        BOOL removed = event.eventType == ASAccessoryEventTypeAccessoryRemoved;
        if (!added && !removed) {
            return;
        }
        com_codename1_impl_ios_IOSNearbyCallbacks_presenceChanged___java_lang_String_boolean(
                getThreadLocalData(),
                cn1nbJString([self encode:event.accessory present:added]),
                added ? JAVA_TRUE : JAVA_FALSE);
    }
}

- (void)activate {
    if (self.activated) {
        return;
    }
    self.activated = YES;
    self.session = [[[ASAccessorySession alloc] init] autorelease];
    CN1NearbyCompanion *weakSelf = self;
    [self.session activateWithQueue:dispatch_get_main_queue()
                       eventHandler:^(ASAccessoryEvent *event) {
        [weakSelf handleEvent:event];
    }];
}

@end

static CN1NearbyCompanion *cn1nbCompanionInit(void) API_AVAILABLE(ios(18.0)) {
    if (cn1nbCompanion == nil) {
        cn1nbCompanion = [[CN1NearbyCompanion alloc] init];
    }
    CN1NearbyCompanion *companion = (CN1NearbyCompanion *)cn1nbCompanion;
    [companion activate];
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
    // CAP_BACKGROUND is deliberately never set. Background ranging needs the
    // com.apple.developer.nearby-interaction entitlement, which the builder
    // never injects on its own because it has to be enabled on the App ID
    // first -- so claiming it here would be a promise the binary usually
    // cannot keep.
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
            [cn1nbSessions setObject:entry
                              forKey:[NSNumber numberWithInt:sessionHandle]];
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
            entry.pendingStartRequest = 0;
            [entry.session runWithConfiguration:config];
            com_codename1_impl_ios_IOSNearbyCallbacks_sessionStarted___int_int(
                    CN1_THREAD_STATE_PASS_ARG requestId, sessionHandle);
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
                [cn1nbSessions removeObjectForKey:
                        [NSNumber numberWithInt:sessionHandle]];
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
            for (NSString *line in cn1nbSplitLines(joined)) {
                NSArray *fields = [line componentsSeparatedByString:@"\t"];
                if ([fields count] < 2) {
                    continue;
                }
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
                    // it has no way to be pointed at one identifier, and
                    // widening the picker to everything would be worse than
                    // skipping the filter.
                    continue;
                }
                ASPickerDisplayItem *item = [[[ASPickerDisplayItem alloc]
                        initWithName:value
                        productImage:[[[UIImage alloc] init] autorelease]
                          descriptor:descriptor] autorelease];
                [items addObject:item];
            }
            if ([items count] == 0) {
                cn1nbFailCompanion(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
                        @"AccessorySetupKit needs at least one Bluetooth"
                        @" service, name or SSID filter to show a picker");
                return;
            }
            CN1NearbyCompanion *companion = cn1nbCompanionInit();
            [companion.session showPickerForDisplayItems:items
                                       completionHandler:^(NSError *error) {
                @autoreleasepool {
                    if (error != nil) {
                        cn1nbFailCompanion(requestId,
                                CN1_NEARBY_ERR_USER_CANCELED,
                                [error localizedDescription]);
                        return;
                    }
                    ASAccessory *picked =
                            [companion.session.accessories lastObject];
                    if (picked == nil) {
                        cn1nbFailCompanion(requestId,
                                CN1_NEARBY_ERR_USER_CANCELED,
                                @"the picker returned no accessory");
                        return;
                    }
                    com_codename1_impl_ios_IOSNearbyCallbacks_associated___int_java_lang_String(
                            getThreadLocalData(), requestId,
                            cn1nbJString([companion encode:picked
                                                   present:YES]));
                }
            }];
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
            CN1NearbyCompanion *companion = cn1nbCompanionInit();
            NSString *aid = toNSString(CN1_THREAD_STATE_PASS_ARG associationId);
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
    // promises one number an app can rely on everywhere. Matching the tighter
    // of the two real backends means a payload that fits here fits on Android.
    return 32 * 1024;
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
        CN1NearbyTransport *t = cn1nbTransportInit(sid, name);
        if (t.advertiser != nil) {
            [t.advertiser stopAdvertisingPeer];
            t.advertiser = nil;
        }
        t.advertiser = [[[MCNearbyServiceAdvertiser alloc]
                initWithPeer:t.localPeer
                discoveryInfo:nil
                serviceType:t.serviceType] autorelease];
        t.advertiser.delegate = t;
        // Recorded BEFORE the answer: didNotStartAdvertisingPeer can fire
        // after this returns, and it needs the id to fail.
        t.pendingAdvertiseRequest = requestId;
        [t.advertiser startAdvertisingPeer];
        cn1nbTransportOk(requestId);
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
            cn1nbTransport.pendingAdvertiseRequest = 0;
        }
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
        CN1NearbyTransport *t = cn1nbTransportInit(sid, nil);
        if (t.browser != nil) {
            [t.browser stopBrowsingForPeers];
            t.browser = nil;
        }
        t.browser = [[[MCNearbyServiceBrowser alloc]
                initWithPeer:t.localPeer
                serviceType:t.serviceType] autorelease];
        t.browser.delegate = t;
        t.pendingDiscoverRequest = requestId;
        [t.browser startBrowsingForPeers];
        cn1nbTransportOk(requestId);
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
            cn1nbTransport.pendingDiscoverRequest = 0;
        }
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
        [cn1nbTransport.browser invitePeer:peer
                                 toSession:cn1nbTransport.session
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
        void (^handler)(BOOL, MCSession *) =
                cn1nbTransport == nil ? nil
                        : [cn1nbTransport.invitations objectForKey:pid];
        if (handler == nil) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_PEER_UNAVAILABLE,
                    @"there is no invitation from that endpoint");
            return;
        }
        [cn1nbTransport.invitations removeObjectForKey:pid];
        handler(YES, cn1nbTransport.session);
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
                [cn1nbTransport.invitations objectForKey:pid];
        if (handler != nil) {
            [cn1nbTransport.invitations removeObjectForKey:pid];
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
        if (cn1nbTransport == nil || cn1nbTransport.session == nil) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_SESSION_FAILED,
                    @"the transport is not running");
            return;
        }
        NSString *joined = toNSString(CN1_THREAD_STATE_PASS_ARG
                                      joinedEndpointIds);
        NSMutableArray *peers = [NSMutableArray array];
        for (NSString *pid in cn1nbSplitLines(joined)) {
            MCPeerID *peer = [cn1nbTransport peerForId:pid];
            if (peer != nil) {
                [peers addObject:peer];
            }
        }
        if ([peers count] == 0) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_PEER_UNAVAILABLE,
                    @"none of those endpoints is connected");
            return;
        }
        if (payloadType == CN1_NEARBY_PAYLOAD_FILE) {
            NSString *p = toNSString(CN1_THREAD_STATE_PASS_ARG path);
            if ([p hasPrefix:@"file://"]) {
                p = [p substringFromIndex:7];
            }
            NSURL *url = [NSURL fileURLWithPath:p];
            for (MCPeerID *peer in peers) {
                [cn1nbTransport.session sendResourceAtURL:url
                        withName:[p lastPathComponent]
                          toPeer:peer
           withCompletionHandler:^(NSError *error) {
                    @autoreleasepool {
                        NSString *encoded = [cn1nbTransport encodePeer:peer];
                        com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                                getThreadLocalData(), cn1nbJString(encoded),
                                payloadId, 0, -1,
                                error == nil ? CN1_NEARBY_PAYLOAD_SUCCESS
                                        : CN1_NEARBY_PAYLOAD_FAILURE);
                    }
                }];
            }
            cn1nbTransportOk(requestId);
            return;
        }
        NSData *data = cn1nbDataFromJavaArray(bytes);
        // Framed with the payload id -- see didReceiveData for why.
        NSMutableData *framed = [NSMutableData dataWithCapacity:
                (data == nil ? 0 : [data length]) + 4];
        unsigned char header[4] = {
            (unsigned char)((payloadId >> 24) & 0xff),
            (unsigned char)((payloadId >> 16) & 0xff),
            (unsigned char)((payloadId >> 8) & 0xff),
            (unsigned char)(payloadId & 0xff)
        };
        [framed appendBytes:header length:4];
        if (data != nil) {
            [framed appendData:data];
        }
        NSError *err = nil;
        BOOL sent = [cn1nbTransport.session sendData:framed
                                             toPeers:peers
                                            withMode:MCSessionSendDataReliable
                                               error:&err];
        if (!sent) {
            cn1nbFailTransport(requestId, CN1_NEARBY_ERR_IO_ERROR,
                    [err localizedDescription]);
            return;
        }
        cn1nbTransportOk(requestId);
        for (MCPeerID *peer in peers) {
            NSString *encoded = [cn1nbTransport encodePeer:peer];
            com_codename1_impl_ios_IOSNearbyCallbacks_payloadProgress___java_lang_String_int_long_long_int(
                    CN1_THREAD_STATE_PASS_ARG cn1nbJString(encoded), payloadId,
                    (JAVA_LONG)[data length], (JAVA_LONG)[data length],
                    CN1_NEARBY_PAYLOAD_SUCCESS);
        }
        return;
    }
#endif
    cn1nbFailTransport(requestId, CN1_NEARBY_ERR_NOT_SUPPORTED,
            @"this build does not include the nearby transport");
}

void com_codename1_impl_ios_IOSNative_nearbyCancelPayload___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_INT payloadId) {
    // MultipeerConnectivity offers no cancellation for sendData, and the
    // NSProgress a resource transfer returns is not retained here. Silently
    // doing nothing is the same outcome an app gets from cancelling a
    // byte payload that has already left, which is the common case.
}

void com_codename1_impl_ios_IOSNative_nearbyDisconnect___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT endpointId) {
#ifdef CN1_NEARBY_HAS_MPC
    @autoreleasepool {
        if (cn1nbTransport == nil || cn1nbTransport.session == nil) {
            return;
        }
        // MCSession disconnects as a whole rather than per peer, so a
        // one-peer session is the only case this can honour precisely. The
        // delegate reports the drop either way, so the app is told the truth.
        NSString *pid = toNSString(CN1_THREAD_STATE_PASS_ARG endpointId);
        MCPeerID *peer = [cn1nbTransport peerForId:pid];
        if (peer != nil
                && [cn1nbTransport.session.connectedPeers count] <= 1) {
            [cn1nbTransport.session disconnect];
        }
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
        [cn1nbTransport.session disconnect];
        [cn1nbTransport.invitations removeAllObjects];
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
