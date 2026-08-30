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

//
//  CN1Vpn.m
//  The NEVPNManager bridge behind com.codename1.vpn.
//
//  Gated on CN1_INCLUDE_VPN, which IPhoneBuilder uncomments only for apps
//  that reference com.codename1.vpn.
//
//  Note this is a DIFFERENT thing from the VPN detection in IOSNative.m,
//  which is always compiled in, needs no entitlement, and answers whether
//  some VPN is carrying this device's traffic rather than managing one.
//

#include "com_codename1_impl_ios_IOSCallCallbacks.h"
#import "java_lang_String.h"

// com.codename1.vpn.VpnError ordinals.
#define CN1_VPN_ERR_NOT_SUPPORTED   0
#define CN1_VPN_ERR_USER_DECLINED   1
#define CN1_VPN_ERR_INVALID_CONFIG  2
#define CN1_VPN_ERR_AUTH_FAILED     3
#define CN1_VPN_ERR_CONNECT_FAILED  4
#define CN1_VPN_ERR_UNAUTHORIZED    5
#define CN1_VPN_ERR_NOT_CONFIGURED  6
#define CN1_VPN_ERR_TIMEOUT         7
#define CN1_VPN_ERR_UNKNOWN         8

// com.codename1.vpn.VpnStatus ordinals.
#define CN1_VPN_STATUS_NOT_CONFIGURED 0
#define CN1_VPN_STATUS_DISCONNECTED   1
#define CN1_VPN_STATUS_CONNECTING     2
#define CN1_VPN_STATUS_CONNECTED      3
#define CN1_VPN_STATUS_DISCONNECTING  4
#define CN1_VPN_STATUS_WAITING        5
#define CN1_VPN_STATUS_UNKNOWN        6

// com.codename1.vpn.VpnProtocol ordinals.
#define CN1_VPN_PROTO_IKEV2  0
#define CN1_VPN_PROTO_IPSEC  1
#define CN1_VPN_PROTO_CUSTOM 2

// VpnBridge.CAPABILITY_* bits.
#define CN1_VPN_CAP_IKEV2         1
#define CN1_VPN_CAP_IPSEC         2
#define CN1_VPN_CAP_CUSTOM_TUNNEL 4
#define CN1_VPN_CAP_ON_DEMAND     8
#define CN1_VPN_CAP_ALWAYS_ON     16
#define CN1_VPN_CAP_PER_APP       32

extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString *str);
extern NSString *toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

static JAVA_OBJECT cn1vpJString(NSString *s) {
    return s == nil ? JAVA_NULL : fromNSString(getThreadLocalData(), s);
}

static void cn1vpAck(int requestId, BOOL ok, int error, NSString *message) {
    com_codename1_impl_ios_IOSCallCallbacks_vpnAck___int_boolean_int_java_lang_String(
            getThreadLocalData(), requestId, ok ? JAVA_TRUE : JAVA_FALSE,
            error, cn1vpJString(message));
}

#if defined(CN1_INCLUDE_VPN) && __has_include(<NetworkExtension/NetworkExtension.h>)
#import <NetworkExtension/NetworkExtension.h>
#define CN1_VPN_HAS_NE 1
#endif

#ifdef CN1_VPN_HAS_NE

/// Whether Java has a status listener installed.
///
/// Guarded by cn1vpInstallLock, like cn1vpLoaded. Java writes it from the
/// EDT while the preferences-load completion and the status notification both
/// read it on their own platform queues, so an unsynchronized read could see
/// the old NO and swallow the only baseline delivery a listener added during
/// startup ever gets -- leaving it unaware that an installed VPN is already
/// connected until something else happens to change.
static BOOL cn1vpListening = NO;
static id cn1vpObserver = nil;

/// Splits a tab-delimited record, preserving trailing empty fields, exactly as
/// VpnWire does on the Java side.
static NSArray *cn1vpSplit(NSString *record) {
    if (record == nil) {
        return [NSArray array];
    }
    return [record componentsSeparatedByString:@"\t"];
}

/// Reverses VpnWire.escape, which is what the Java side writes.
///
/// The call wire's sanitizer turns a tab, carriage return or newline into a
/// space. That is right for display text and wrong for a secret: a password
/// or pre-shared key containing one of them was installed as a DIFFERENT
/// string, the install was acknowledged, and authentication then failed with
/// nothing anywhere to say the credential had been altered. Every field is
/// escaped, so every field is unescaped here.
///
/// A backslash before anything else -- including at the end -- is kept as
/// itself, matching the Java side: refusing to guess beats dropping a
/// character out of a password.
static NSString *cn1vpUnescape(NSString *value) {
    if (value == nil) {
        return @"";
    }
    NSRange first = [value rangeOfString:@"\\"];
    if (first.location == NSNotFound) {
        return value;
    }
    NSMutableString *out = [NSMutableString stringWithCapacity:[value length]];
    NSUInteger i = 0;
    NSUInteger len = [value length];
    while (i < len) {
        unichar c = [value characterAtIndex:i];
        if (c != '\\' || i + 1 >= len) {
            [out appendFormat:@"%C", c];
            i++;
            continue;
        }
        unichar n = [value characterAtIndex:i + 1];
        if (n == '\\') {
            [out appendString:@"\\"];
        } else if (n == 't') {
            [out appendString:@"\t"];
        } else if (n == 'r') {
            [out appendString:@"\r"];
        } else if (n == 'n') {
            [out appendString:@"\n"];
        } else {
            [out appendFormat:@"%C", c];
            i++;
            continue;
        }
        i += 2;
    }
    return out;
}

/// The write side of cn1vpUnescape, for the record load() hands back.
///
/// Java unescapes every field it reads, so iOS must escape every field it
/// writes -- otherwise a legitimate backslash in a value, and a domain
/// username like CORP\\alice is the ordinary case, is eaten on the way back.
/// cn1vpSanitize is still right for text iOS shows the USER, which is why
/// localizedDescription keeps it.
static NSString *cn1vpEscape(NSString *value) {
    if (value == nil) {
        return @"";
    }
    NSString *out = [value stringByReplacingOccurrencesOfString:@"\\"
            withString:@"\\\\"];
    out = [out stringByReplacingOccurrencesOfString:@"\t" withString:@"\\t"];
    out = [out stringByReplacingOccurrencesOfString:@"\r" withString:@"\\r"];
    return [out stringByReplacingOccurrencesOfString:@"\n" withString:@"\\n"];
}

static NSString *cn1vpField(NSArray *fields, NSUInteger index) {
    if (fields == nil || index >= [fields count]) {
        return @"";
    }
    return cn1vpUnescape([fields objectAtIndex:index]);
}

/// Answers a tunnel start or stop.
///
/// A different channel from cn1vpAck because it settles a different request
/// map: com.codename1.vpn.tunnel.Tunnels keeps its own, so an ack routed
/// through the profile facade would find no waiter and drop.
static void cn1vpTunnelAck(int requestId, BOOL ok, int error,
        NSString *message) {
    com_codename1_impl_ios_IOSCallCallbacks_vpnTunnelAck___int_boolean_int_java_lang_String(
            getThreadLocalData(), requestId, ok ? JAVA_TRUE : JAVA_FALSE,
            error, cn1vpJString(message));
}

/// Answers a tunnel request with the message an NSError carried.
static void cn1vpFail(int requestId, int error, NSError *e) {
    cn1vpTunnelAck(requestId, NO, error,
            e == nil ? nil : [e localizedDescription]);
}

/// A string from the app's own Info.plist, or the empty string.
static NSString *cn1vpPlistString(NSString *key) {
    id value = [[NSBundle mainBundle] objectForInfoDictionaryKey:key];
    return [value isKindOfClass:[NSString class]] ? (NSString *)value : @"";
}

/// One field of a tab-delimited setup record, unescaped.
static NSString *cn1vpWireField(NSString *record, NSUInteger index) {
    if (record == nil) {
        return @"";
    }
    // componentsSeparatedByString keeps trailing empties, which is what the
    // positional record needs -- a reader that drops them shifts every field
    // after the first empty one.
    return cn1vpField([record componentsSeparatedByString:@"\t"], index);
}


static NSString *cn1vpSanitize(NSString *s) {
    if (s == nil) {
        return @"";
    }
    NSString *out = [s stringByReplacingOccurrencesOfString:@"\t" withString:@" "];
    out = [out stringByReplacingOccurrencesOfString:@"\n" withString:@" "];
    return [out stringByReplacingOccurrencesOfString:@"\r" withString:@" "];
}

static int cn1vpStatusOrdinal(NEVPNStatus status) {
    switch (status) {
        case NEVPNStatusInvalid:       return CN1_VPN_STATUS_NOT_CONFIGURED;
        case NEVPNStatusDisconnected:  return CN1_VPN_STATUS_DISCONNECTED;
        case NEVPNStatusConnecting:    return CN1_VPN_STATUS_CONNECTING;
        case NEVPNStatusConnected:     return CN1_VPN_STATUS_CONNECTED;
        case NEVPNStatusReasserting:   return CN1_VPN_STATUS_CONNECTING;
        case NEVPNStatusDisconnecting: return CN1_VPN_STATUS_DISCONNECTING;
        default:                       return CN1_VPN_STATUS_UNKNOWN;
    }
}

/// The keychain reference for a password stored under this key.
///
/// NEVPNProtocol takes a keychain persistent reference rather than a string,
/// which is the whole reason a loaded profile can never hand the secret back.
/// Which generation of keychain items the installed profile references.
///
/// Persisted, because the profile it belongs to outlives the process. Zero
/// means nothing this port installed is on the device yet.
static int cn1vpSecretGeneration = -1;

/// Whether an installation owns the shared manager and the next generation.
static BOOL cn1vpInstalling = NO;

/// Guards cn1vpInstalling; every read and write takes it.
static NSObject *cn1vpInstallLock = nil;

/// Creates the install lock once. @synchronized on nil is a no-op, which
/// would leave the reservation unguarded rather than fail visibly.
static void cn1vpEnsureInstallLock(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1vpInstallLock = [[NSObject alloc] init];
    });
}

/// Releases the profile-operation reservation.
///
/// In one place because every path out of a control now has to release it:
/// the reservation is claimed BEFORE the asynchronous load, so an early
/// return from the completion handler that forgot would refuse every later
/// install as though an operation were still running.
static void cn1vpReleaseInstalling(void) {
    cn1vpEnsureInstallLock();
    @synchronized (cn1vpInstallLock) {
        cn1vpInstalling = NO;
    }
}


/// Whether the shared manager's saved configuration has been read.
static BOOL cn1vpLoaded = NO;

/// Reads cn1vpListening under the lock that publishes it.
static BOOL cn1vpIsListening(void) {
    cn1vpEnsureInstallLock();
    @synchronized (cn1vpInstallLock) {
        return cn1vpListening;
    }
}

/// Reads cn1vpLoaded under the lock that publishes it.
static BOOL cn1vpIsLoaded(void) {
    cn1vpEnsureInstallLock();
    @synchronized (cn1vpInstallLock) {
        return cn1vpLoaded;
    }
}

/// Whether a read is in flight, so a failure can be retried without two
/// loads racing.
static BOOL cn1vpLoading = NO;

/// Reads the generation once per process.
static int cn1vpLoadSecretGeneration(void) {
    if (cn1vpSecretGeneration < 0) {
        cn1vpSecretGeneration = (int)[[NSUserDefaults standardUserDefaults]
                integerForKey:@"cn1vpn.secretGeneration"];
    }
    return cn1vpSecretGeneration;
}

static NSDictionary *cn1vpSecretQuery(NSString *account) {
    return [NSDictionary dictionaryWithObjectsAndKeys:
            (__bridge id)kSecClassGenericPassword, (__bridge id)kSecClass,
            account, (__bridge id)kSecAttrAccount,
            @"com.codename1.vpn", (__bridge id)kSecAttrService, nil];
}

/// Stores a secret under a FRESH account name and returns its reference.
///
/// The account carries a generation suffix and the previous generation is
/// left alone until the save succeeds. Replacing in place deleted the item
/// the INSTALLED profile still referenced before the replacement had reached
/// saveToPreferences -- so an invalid configuration, a failed save or a user
/// who declined left the old profile installed with its password reference
/// pointing at a deleted item, and a tunnel that had been working could no
/// longer authenticate.
static NSData *cn1vpStoreSecret(NSString *account, NSString *secret) {
    if (secret == nil || [secret length] == 0) {
        return nil;
    }
    NSData *value = [secret dataUsingEncoding:NSUTF8StringEncoding];
    NSMutableDictionary *add = [NSMutableDictionary dictionaryWithDictionary:
            cn1vpSecretQuery(account)];
    [add setObject:value forKey:(__bridge id)kSecValueData];
    // AFTER FIRST UNLOCK, not the when-unlocked default.
    //
    // NetworkExtension reads the password and shared-secret references
    // itself, and with an on-demand profile it does that whenever a rule
    // fires -- including while the device is locked. Under the default
    // accessibility the read fails there, so a profile that installed
    // perfectly could not authenticate on exactly the connections
    // on-demand exists to make, and the failure is a system-level
    // authentication error with nothing in the app to explain it.
    //
    // AfterFirstUnlock rather than Always: the secret still cannot be read
    // before the user has unlocked once since boot, which is the weakest
    // protection that lets a background reassert work. It is also what
    // Apple's own guidance names for this case.
    [add setObject:(__bridge id)kSecAttrAccessibleAfterFirstUnlock
            forKey:(__bridge id)kSecAttrAccessible];
    [add setObject:(__bridge id)kCFBooleanTrue
            forKey:(__bridge id)kSecReturnPersistentRef];
    CFTypeRef result = NULL;
    OSStatus status = SecItemAdd((__bridge CFDictionaryRef)add, &result);
    if (status == errSecDuplicateItem) {
        // Same generation twice in one process: overwriting this one is safe
        // because no saved profile can reference it yet.
        SecItemDelete((__bridge CFDictionaryRef)cn1vpSecretQuery(account));
        status = SecItemAdd((__bridge CFDictionaryRef)add, &result);
    }
    if (status != errSecSuccess) {
        return nil;
    }
    // AUTORELEASED, not __bridge_transfer. This port compiles without ARC
    // (CLANG_ENABLE_OBJC_ARC = NO), where the whole __bridge family is a
    // no-op -- clang says so with -Warc-bridge-casts-disallowed-in-nonarc --
    // so the +1 SecItemAdd hands back on kSecReturnPersistentRef was never
    // consumed and every profile install leaked a persistent-ref CFData.
    // The cast is only a type change here; the ownership transfer has to be
    // written out.
    return [(NSData *)result autorelease];
}

/// The generation the NEXT install writes its secrets under.
static NSString *cn1vpSecretAccount(NSString *base) {
    return [NSString stringWithFormat:@"%@.%d", base,
            cn1vpLoadSecretGeneration() + 1];
}

/// Deletes the live generation's items and forgets it.
///
/// For a profile that has been removed rather than replaced: there is
/// nothing left to reference them.
static void cn1vpDiscardSecrets(void) {
    int live = cn1vpLoadSecretGeneration();
    if (live > 0) {
        SecItemDelete((__bridge CFDictionaryRef)cn1vpSecretQuery(
                [NSString stringWithFormat:@"cn1vpn.psk.%d", live]));
        SecItemDelete((__bridge CFDictionaryRef)cn1vpSecretQuery(
                [NSString stringWithFormat:@"cn1vpn.password.%d", live]));
    }
    cn1vpSecretGeneration = 0;
    [[NSUserDefaults standardUserDefaults]
            setInteger:0 forKey:@"cn1vpn.secretGeneration"];
}

/// Deletes the items an installation staged but never saved.
///
/// The secrets go into the keychain while the configuration is built, which
/// is before saveToPreferences is even called -- so a declined prompt or an
/// invalid configuration left a password and a pre-shared key that NOTHING
/// references, and that a later remove() would not find either, because
/// remove only knows about the live generation.
static void cn1vpDiscardStagedSecrets(void) {
    int staged = cn1vpLoadSecretGeneration() + 1;
    SecItemDelete((__bridge CFDictionaryRef)cn1vpSecretQuery(
            [NSString stringWithFormat:@"cn1vpn.psk.%d", staged]));
    SecItemDelete((__bridge CFDictionaryRef)cn1vpSecretQuery(
            [NSString stringWithFormat:@"cn1vpn.password.%d", staged]));
}

/// Retires the generations older than the one that just saved.
///
/// Only after a successful save: until then the installed profile still
/// references them.
static void cn1vpRetireOldSecrets(void) {
    int retired = cn1vpLoadSecretGeneration();
    cn1vpSecretGeneration = retired + 1;
    [[NSUserDefaults standardUserDefaults]
            setInteger:cn1vpSecretGeneration forKey:@"cn1vpn.secretGeneration"];
    if (retired <= 0) {
        return;
    }
    SecItemDelete((__bridge CFDictionaryRef)cn1vpSecretQuery(
            [NSString stringWithFormat:@"cn1vpn.psk.%d", retired]));
    SecItemDelete((__bridge CFDictionaryRef)cn1vpSecretQuery(
            [NSString stringWithFormat:@"cn1vpn.password.%d", retired]));
}

@interface CN1VpnStatusWatcher : NSObject
- (void)statusChanged:(NSNotification *)note;
@end

@implementation CN1VpnStatusWatcher
- (void)statusChanged:(NSNotification *)note {
    if (!cn1vpIsListening()) {
        return;
    }
    com_codename1_impl_ios_IOSCallCallbacks_vpnStatusChanged___int(
            getThreadLocalData(),
            cn1vpStatusOrdinal([[NEVPNManager sharedManager] connection].status));
}
@end

static CN1VpnStatusWatcher *cn1vpWatcher = nil;

#endif /* CN1_VPN_HAS_NE */

// ---------------------------------------------------------------------
// The exported natives. Both halves always defined, for the reason
// CN1Call.m gives at length.
// ---------------------------------------------------------------------

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_vpnSupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_VPN_HAS_NE
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_vpnTunnelSupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if defined(CN1_VPN_TUNNEL) && defined(CN1_VPN_HAS_NE)
    // The build generated the packet-tunnel target. Whether Apple GRANTED
    // the entitlement is not knowable here -- an ungranted App ID fails at
    // codesigning, long before this runs -- so a build that got this far has
    // the extension.
    return JAVA_TRUE;
#else
    // No tunnel target in this build, which is the ordinary case: the
    // entitlement is granted case by case, so the builder generates the
    // extension only for a project that asked for it and said it has the
    // grant.
    return JAVA_FALSE;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_vpnCapabilities___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_VPN_HAS_NE
    // No CN1_VPN_CAP_ALWAYS_ON: always-on VPN on iOS needs a supervised
    // device and an MDM payload, not something an app may request.
#ifdef CN1_VPN_TUNNEL
    return CN1_VPN_CAP_IKEV2 | CN1_VPN_CAP_IPSEC | CN1_VPN_CAP_ON_DEMAND
            | CN1_VPN_CAP_CUSTOM_TUNNEL;
#else
    // No CN1_VPN_CAP_CUSTOM_TUNNEL: this build generated no tunnel target.
    return CN1_VPN_CAP_IKEV2 | CN1_VPN_CAP_IPSEC | CN1_VPN_CAP_ON_DEMAND;
#endif
#else
    return 0;
#endif
}

#ifdef CN1_VPN_HAS_NE
/// Loads the saved configuration into the shared manager, once.
///
/// NEVPNManager starts with nothing loaded, and until
/// loadFromPreferencesWithCompletionHandler: has run its connection.status is
/// NEVPNStatusInvalid -- which reads as NOT_CONFIGURED. So a fresh process
/// with an installed, even connected, profile reported that no VPN existed,
/// and went on doing so until some other operation happened to load it.
///
/// The load is asynchronous and vpnStatus is not, so the first answer after
/// a launch can still be the unloaded one. What this guarantees is that it
/// stops being wrong: when the load lands, a listening app is told through
/// the same statusChanged path a real transition uses.
static void cn1vpEnsureLoaded(void) {
    // NOT dispatch_once: a load that FAILS must be retried. Marking the
    // manager loaded regardless published its unloaded NEVPNStatusInvalid as
    // NOT_CONFIGURED for good, so a transient read failure left an installed
    // -- even connected -- VPN reported as absent for the life of the
    // process.
    cn1vpEnsureInstallLock();
    @synchronized (cn1vpInstallLock) {
        if (cn1vpLoaded || cn1vpLoading) {
            return;
        }
        cn1vpLoading = YES;
    }
    [[NEVPNManager sharedManager] loadFromPreferencesWithCompletionHandler:
            ^(NSError *error) {
        @synchronized (cn1vpInstallLock) {
            cn1vpLoading = NO;
            if (error != nil) {
                // Left unloaded on purpose, so the next status query retries.
                return;
            }
            cn1vpLoaded = YES;
        }
        if (cn1vpIsListening()) {
            com_codename1_impl_ios_IOSCallCallbacks_vpnStatusChanged___int(
                    getThreadLocalData(), cn1vpStatusOrdinal(
                            [[NEVPNManager sharedManager] connection].status));
        }
    }];
}

#endif

JAVA_INT com_codename1_impl_ios_IOSNative_vpnStatus___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_VPN_HAS_NE
    cn1vpEnsureLoaded();
    if (!cn1vpIsLoaded()) {
        // The load is asynchronous and this is not. Until it lands the shared
        // manager reports NEVPNStatusInvalid, which reads as NOT_CONFIGURED
        // -- an ACTIVE claim that no VPN exists, for a device that may have a
        // connected one. UNKNOWN is the truth: nothing is known yet, and an
        // app is told to wait rather than to draw an install button.
        return CN1_VPN_STATUS_UNKNOWN;
    }
    return cn1vpStatusOrdinal([[NEVPNManager sharedManager] connection].status);
#else
    return CN1_VPN_STATUS_NOT_CONFIGURED;
#endif
}

void com_codename1_impl_ios_IOSNative_vpnInstallProfile___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT profileWire) {
#ifdef CN1_VPN_HAS_NE
    NSArray *f = cn1vpSplit(toNSString(threadStateData, profileWire));
    NSString *server = cn1vpField(f, 0);
    if ([server length] == 0) {
        cn1vpAck(requestId, NO, CN1_VPN_ERR_INVALID_CONFIG,
                @"The profile has no server address");
        return;
    }
    int proto = [cn1vpField(f, 1) intValue];
    NSString *remoteId = cn1vpField(f, 2);
    NSString *localId = cn1vpField(f, 3);
    NSString *user = cn1vpField(f, 4);
    NSString *pass = cn1vpField(f, 5);
    NSString *psk = cn1vpField(f, 6);
    // Fields 7 and 8 are reserved empty slots; see VpnWire.
    BOOL onDemand = [cn1vpField(f, 9) isEqualToString:@"1"];
    NSString *name = cn1vpField(f, 10);

    // One installation at a time. Two overlapping calls share the single
    // NEVPNManager AND the same next-generation keychain accounts, so the
    // second's SecItemAdd replaced the first's not-yet-saved item and either
    // save could retire the generation both were using -- acknowledging the
    // wrong profile, or leaving the installed one pointing at credentials
    // that had been deleted. Rejecting is better than queueing: the second
    // caller learns immediately rather than waiting on a prompt for
    // somebody else's profile.
    // Test AND set together. Two installs could otherwise both read this as
    // free and both go on to mutate the one shared NEVPNManager and the same
    // next-generation keychain accounts -- which is precisely the credential
    // replacement and wrong-profile acknowledgement this guard exists to
    // prevent.
    cn1vpEnsureInstallLock();
    BOOL busy = NO;
    @synchronized (cn1vpInstallLock) {
        busy = cn1vpInstalling;
        if (!busy) {
            cn1vpInstalling = YES;
        }
    }
    if (busy) {
        cn1vpAck(requestId, NO, CN1_VPN_ERR_UNKNOWN,
                @"A VPN profile installation is already in progress");
        return;
    }
    NEVPNManager *manager = [NEVPNManager sharedManager];
    [manager loadFromPreferencesWithCompletionHandler:^(NSError *loadError) {
        if (loadError != nil) {
            @synchronized (cn1vpInstallLock) {
                cn1vpInstalling = NO;
            }
            cn1vpAck(requestId, NO, CN1_VPN_ERR_UNAUTHORIZED,
                    [loadError localizedDescription]);
            return;
        }
        NEVPNProtocol *cfg;
        // Set when a keychain write hands back nil; checked before the save.
        BOOL ipsecSecretMissing = NO;
        BOOL passwordMissing = NO;
        if (proto == CN1_VPN_PROTO_IPSEC) {
            NEVPNProtocolIPSec *ipsec = [[[NEVPNProtocolIPSec alloc] init] autorelease];
            ipsec.authenticationMethod = [psk length] > 0
                    ? NEVPNIKEAuthenticationMethodSharedSecret
                    : NEVPNIKEAuthenticationMethodNone;
            if ([psk length] > 0) {
                ipsec.sharedSecretReference =
                        cn1vpStoreSecret(cn1vpSecretAccount(@"cn1vpn.psk"), psk);
                ipsecSecretMissing = ipsec.sharedSecretReference == nil;
            }
            ipsec.localIdentifier = [localId length] > 0 ? localId : nil;
            ipsec.remoteIdentifier = [remoteId length] > 0 ? remoteId : server;
            // The PSK-plus-user-credentials arrangement is the common one,
            // and without this NetworkExtension never performs the user half:
            // the profile saves cleanly and then cannot connect.
            ipsec.useExtendedAuthentication = [user length] > 0;
            cfg = ipsec;
        } else {
            NEVPNProtocolIKEv2 *ike =
                    [[[NEVPNProtocolIKEv2 alloc] init] autorelease];
            ike.authenticationMethod = [psk length] > 0
                    ? NEVPNIKEAuthenticationMethodSharedSecret
                    : NEVPNIKEAuthenticationMethodNone;
            if ([psk length] > 0) {
                ike.sharedSecretReference = cn1vpStoreSecret(cn1vpSecretAccount(@"cn1vpn.psk"), psk);
                ipsecSecretMissing = ike.sharedSecretReference == nil;
            }
            ike.localIdentifier = [localId length] > 0 ? localId : nil;
            ike.remoteIdentifier = [remoteId length] > 0 ? remoteId : server;
            ike.useExtendedAuthentication = [user length] > 0;
            cfg = ike;
        }
        cfg.serverAddress = server;
        if ([user length] > 0) {
            cfg.username = user;
            // The password goes to the keychain and the profile keeps only a
            // reference, which is why a loaded profile can never hand the
            // secret back to the app.
            cfg.passwordReference = cn1vpStoreSecret(cn1vpSecretAccount(@"cn1vpn.password"), pass);
            passwordMissing = cfg.passwordReference == nil;
        }
        cfg.disconnectOnSleep = NO;

        manager.protocolConfiguration = cfg;
        manager.localizedDescription = [name length] > 0
                ? name : cn1vpSanitize(server);
        manager.enabled = YES;
        manager.onDemandEnabled = onDemand;
        if (onDemand) {
            NEOnDemandRuleConnect *rule = [[[NEOnDemandRuleConnect alloc] init] autorelease];
            manager.onDemandRules = [NSArray arrayWithObject:rule];
        }
        // A keychain write that failed hands back nil, and assigning that as
        // the reference saved a profile with no credential in it: the install
        // reported success and the next connection could not authenticate --
        // having already replaced a profile that worked.
        // Keyed off the USER, not the password length, because that is what
        // the branch above keys off: a non-empty user turns extended
        // authentication on and tries to store a password, so "the password
        // could not be stored" has to be an error for exactly those
        // profiles. Testing the password length instead made an empty
        // password the one case that configured extended authentication with
        // no credential and still reported success. The facade rejects that
        // profile now; this is the same rule stated where the branch is.
        if (([psk length] > 0 && ipsecSecretMissing)
                || ([user length] > 0 && passwordMissing)) {
            cn1vpDiscardStagedSecrets();
            @synchronized (cn1vpInstallLock) {
                cn1vpInstalling = NO;
            }
            cn1vpAck(requestId, NO, CN1_VPN_ERR_UNKNOWN,
                    @"The VPN credentials could not be stored in the keychain");
            return;
        }
        [manager saveToPreferencesWithCompletionHandler:^(NSError *saveError) {
            if (saveError == nil) {
                // The new profile is installed and owns the new generation,
                // so the previous one's items can go. A failed save falls
                // through and leaves them exactly where the still-installed
                // profile expects them.
                cn1vpRetireOldSecrets();
                // Released only now. Cleared before the cleanup, a second
                // install could reserve the SAME next generation and stage
                // its secrets into it while this one was still retiring --
                // and in the failure case below, delete what the second had
                // just staged.
                @synchronized (cn1vpInstallLock) {
                    cn1vpInstalling = NO;
                }
                cn1vpAck(requestId, YES, 0, nil);
                return;
            }
            cn1vpDiscardStagedSecrets();
            @synchronized (cn1vpInstallLock) {
                cn1vpInstalling = NO;
            }
            // A user who declines the system prompt lands here, and it is an
            // ordinary outcome rather than a failure to report twice.
            int code = saveError.code == NEVPNErrorConfigurationReadWriteFailed
                    ? CN1_VPN_ERR_USER_DECLINED : CN1_VPN_ERR_INVALID_CONFIG;
            if (saveError.code == NEVPNErrorConfigurationInvalid) {
                code = CN1_VPN_ERR_INVALID_CONFIG;
            }
            cn1vpAck(requestId, NO, code, [saveError localizedDescription]);
        }];
    }];
#else
    cn1vpAck(requestId, NO, CN1_VPN_ERR_NOT_SUPPORTED,
            @"This build did not link NetworkExtension");
#endif
}

void com_codename1_impl_ios_IOSNative_vpnRemoveProfile___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
#ifdef CN1_VPN_HAS_NE
    // Both mutate the one shared NEVPNManager, so running a removal across an
    // installation lets the two callbacks interleave: the removal can
    // acknowledge success before the pending save puts the profile back, or
    // delete the profile the installation has just acknowledged. Whichever
    // way round, one of the two answered success for a state that is not the
    // one on the device.
    // The removal RESERVES the manager for its whole asynchronous run, not
    // just for the instant it looks. Checking and letting go, an install
    // could start while the load and the remove were still in flight, and the
    // two callbacks interleaved on the one shared manager: both answered
    // success while the later one either put the profile back or deleted the
    // one just installed, and the secret cleanup no longer matched what was
    // on the device.
    cn1vpEnsureInstallLock();
    BOOL busy = NO;
    @synchronized (cn1vpInstallLock) {
        busy = cn1vpInstalling;
        if (!busy) {
            cn1vpInstalling = YES;
        }
    }
    if (busy) {
        cn1vpAck(requestId, NO, CN1_VPN_ERR_UNKNOWN,
                @"A VPN profile installation is in progress");
        return;
    }
    NEVPNManager *manager = [NEVPNManager sharedManager];
    [manager loadFromPreferencesWithCompletionHandler:^(NSError *loadError) {
        if (loadError != nil) {
            // Removing against an unloaded or stale manager and then
            // reporting only the SECOND callback's result turned a transient
            // read failure into a successful removal -- discarding the
            // keychain credentials for a configuration never established as
            // removed.
            @synchronized (cn1vpInstallLock) {
                cn1vpInstalling = NO;
            }
            cn1vpAck(requestId, NO, CN1_VPN_ERR_UNAUTHORIZED,
                    [loadError localizedDescription]);
            return;
        }
        [manager removeFromPreferencesWithCompletionHandler:^(NSError *error) {
            if (error == nil) {
                // The profile that referenced them is gone, so the items are
                // unreachable -- and left behind they were the user's VPN
                // password and pre-shared key sitting in the app's keychain
                // for ever, or until an install happened to retire that
                // generation. Only after a SUCCESSFUL removal: a failed one
                // leaves the profile installed and still needing them.
                cn1vpDiscardSecrets();
            }
            @synchronized (cn1vpInstallLock) {
                cn1vpInstalling = NO;
            }
            cn1vpAck(requestId, error == nil, CN1_VPN_ERR_UNKNOWN,
                    error == nil ? nil : [error localizedDescription]);
        }];
    }];
#else
    cn1vpAck(requestId, NO, CN1_VPN_ERR_NOT_SUPPORTED, nil);
#endif
}

void com_codename1_impl_ios_IOSNative_vpnLoadProfile___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
#ifdef CN1_VPN_HAS_NE
    NEVPNManager *manager = [NEVPNManager sharedManager];
    [manager loadFromPreferencesWithCompletionHandler:^(NSError *error) {
        if (error != nil) {
            com_codename1_impl_ios_IOSCallCallbacks_vpnProfileFailed___int_int_java_lang_String(
                    getThreadLocalData(), requestId, CN1_VPN_ERR_UNKNOWN,
                    cn1vpJString([error localizedDescription]));
            return;
        }
        NEVPNProtocol *cfg = manager.protocolConfiguration;
        if (cfg == nil || [cfg.serverAddress length] == 0) {
            com_codename1_impl_ios_IOSCallCallbacks_vpnProfile___int_java_lang_String(
                    getThreadLocalData(), requestId, cn1vpJString(@""));
            return;
        }
        // NEVPNProtocolIKEv2 IS an NEVPNProtocolIPSec -- it derives from it
        // -- so the SPECIFIC test has to come first here or every IKEv2
        // profile would be read back as IPSEC.
        int proto = [cfg isKindOfClass:[NEVPNProtocolIKEv2 class]]
                ? CN1_VPN_PROTO_IKEV2 : CN1_VPN_PROTO_IPSEC;
        NSString *remoteId = @"";
        NSString *localId = @"";
        // And the same inheritance is why ONE test covers both protocols
        // below: localIdentifier and remoteIdentifier are declared on
        // NEVPNProtocolIPSec, and IKEv2 inherits them rather than
        // redeclaring them -- which is exactly why the install path can set
        // them on an NEVPNProtocolIKEv2. A review read this as skipping the
        // identifiers for IKEv2 and losing them from the loaded record; the
        // isKindOfClass answer for an IKEv2 configuration is YES, so both
        // are read. Spelled out because the two tests look inconsistent
        // until you know the hierarchy, and the compiler cannot say so.
        if ([cfg isKindOfClass:[NEVPNProtocolIPSec class]]) {
            NEVPNProtocolIPSec *ip = (NEVPNProtocolIPSec *)cfg;
            remoteId = ip.remoteIdentifier == nil ? @"" : ip.remoteIdentifier;
            localId = ip.localIdentifier == nil ? @"" : ip.localIdentifier;
        }
        // Fields 5 and 6 -- the password and the shared secret -- are left
        // EMPTY. The platform holds them in the keychain as references and
        // never hands the values back, and the Java side documents that.
        // ESCAPED, not sanitized: Java unescapes every field it reads, and
        // sanitizing here would both mangle a value that legitimately holds
        // a tab and leave a real backslash -- CORP\\alice -- to be eaten by
        // the unescape at the other end.
        NSArray *fields = [NSArray arrayWithObjects:
                cn1vpEscape(cfg.serverAddress),
                [NSString stringWithFormat:@"%d", proto],
                cn1vpEscape(remoteId),
                cn1vpEscape(localId),
                cfg.username == nil ? @"" : cn1vpEscape(cfg.username),
                // 5 and 6 are the password and the shared secret, which iOS
                // keeps in the keychain and never hands back; 7 and 8 are the
                // reserved slots VpnWire.encodeProfile documents. Four
                // empties, then on-demand at 9 and the description at 10 --
                // which is what decodeProfile reads.
                //
                // This used to emit three empties and the on-demand flag
                // TWICE, at 8 and 9. The indices came out right, since 8 is
                // ignored, but the only way to see that was to notice the
                // duplication -- and a review counting the empties read it as
                // an off-by-one and filed it as a bug. Same bytes on the
                // wire, one obvious reading.
                @"", @"", @"", @"",
                manager.onDemandEnabled ? @"1" : @"0",
                manager.localizedDescription == nil ? @""
                        : cn1vpEscape(manager.localizedDescription), nil];
        com_codename1_impl_ios_IOSCallCallbacks_vpnProfile___int_java_lang_String(
                getThreadLocalData(), requestId,
                cn1vpJString([fields componentsJoinedByString:@"\t"]));
    }];
#else
    com_codename1_impl_ios_IOSCallCallbacks_vpnProfileFailed___int_int_java_lang_String(
            threadStateData, requestId, CN1_VPN_ERR_NOT_SUPPORTED,
            cn1vpJString(@"This build did not link NetworkExtension"));
#endif
}

void com_codename1_impl_ios_IOSNative_vpnStart___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
#ifdef CN1_VPN_HAS_NE
    // RESERVED BEFORE the load, not inside its completion.
    //
    // The load is asynchronous against the one shared NEVPNManager, and
    // claiming the reservation only in the completion left the ORDER of two
    // overlapping controls decided by which load finished first: a stop
    // could complete, stop the connection and report success, and the start
    // that arrived before it could then claim the freed flag and bring the
    // tunnel up -- the caller told its stop succeeded while the VPN ends
    // connected. Claimed here, the second control is refused as busy
    // instead, which is an answer it can act on.
    cn1vpEnsureInstallLock();
    @synchronized (cn1vpInstallLock) {
        if (cn1vpInstalling) {
            cn1vpAck(requestId, NO, CN1_VPN_ERR_UNKNOWN,
                    @"A VPN profile operation is in progress; wait for it"
                    " to finish");
            return;
        }
        cn1vpInstalling = YES;
    }
    NEVPNManager *manager = [NEVPNManager sharedManager];
    [manager loadFromPreferencesWithCompletionHandler:^(NSError *loadError) {
        if (loadError != nil) {
            // A read that FAILED is not a profile that is absent. Reported
            // as NOT_CONFIGURED, a transient read or authorization failure
            // told the app there was nothing installed -- inviting it to
            // reinstall over a profile that was there all along, and to
            // hand the user a prompt for a configuration they already have.
            cn1vpReleaseInstalling();
            cn1vpAck(requestId, NO, CN1_VPN_ERR_UNKNOWN,
                    [@"The VPN configuration could not be read: "
                            stringByAppendingString:
                                    [loadError localizedDescription]]);
            return;
        }
        if (manager.protocolConfiguration == nil) {
            cn1vpReleaseInstalling();
            cn1vpAck(requestId, NO, CN1_VPN_ERR_NOT_CONFIGURED,
                    @"No VPN configuration is installed");
            return;
        }
        NSError *error = nil;
        [[manager connection] startVPNTunnelAndReturnError:&error];
        // Released the moment the platform has the request, on every path.
        // Left set, every later install would be refused as though an
        // operation were still running.
        @synchronized (cn1vpInstallLock) {
            cn1vpInstalling = NO;
        }
        if (error != nil) {
            cn1vpAck(requestId, NO, CN1_VPN_ERR_CONNECT_FAILED,
                    [error localizedDescription]);
            return;
        }
        // The tunnel is CONNECTING at best; the status listener carries the
        // rest. Answering now says the request was accepted, which is all
        // that is actually known.
        cn1vpAck(requestId, YES, 0, nil);
    }];
#else
    cn1vpAck(requestId, NO, CN1_VPN_ERR_NOT_SUPPORTED, nil);
#endif
}

void com_codename1_impl_ios_IOSNative_vpnStop___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
#ifdef CN1_VPN_HAS_NE
    // RESERVED BEFORE the load, not inside its completion.
    //
    // The load is asynchronous against the one shared NEVPNManager, and
    // claiming the reservation only in the completion left the ORDER of two
    // overlapping controls decided by which load finished first: a stop
    // could complete, stop the connection and report success, and the start
    // that arrived before it could then claim the freed flag and bring the
    // tunnel up -- the caller told its stop succeeded while the VPN ends
    // connected. Claimed here, the second control is refused as busy
    // instead, which is an answer it can act on.
    cn1vpEnsureInstallLock();
    @synchronized (cn1vpInstallLock) {
        if (cn1vpInstalling) {
            cn1vpAck(requestId, NO, CN1_VPN_ERR_UNKNOWN,
                    @"A VPN profile operation is in progress; wait for it"
                    " to finish");
            return;
        }
        cn1vpInstalling = YES;
    }
    NEVPNManager *manager = [NEVPNManager sharedManager];
    [manager loadFromPreferencesWithCompletionHandler:^(NSError *loadError) {
        if (loadError != nil) {
            // A read that FAILED is not a profile that is absent. Reported
            // as NOT_CONFIGURED, a transient read or authorization failure
            // told the app there was nothing installed -- inviting it to
            // reinstall over a profile that was there all along, and to
            // hand the user a prompt for a configuration they already have.
            cn1vpReleaseInstalling();
            cn1vpAck(requestId, NO, CN1_VPN_ERR_UNKNOWN,
                    [@"The VPN configuration could not be read: "
                            stringByAppendingString:
                                    [loadError localizedDescription]]);
            return;
        }
        if (manager.protocolConfiguration == nil) {
            cn1vpReleaseInstalling();
            cn1vpAck(requestId, NO, CN1_VPN_ERR_NOT_CONFIGURED,
                    @"No VPN configuration is installed");
            return;
        }
        [[manager connection] stopVPNTunnel];
        cn1vpReleaseInstalling();
        cn1vpAck(requestId, YES, 0, nil);
    }];
#else
    cn1vpAck(requestId, NO, CN1_VPN_ERR_NOT_SUPPORTED, nil);
#endif
}

void com_codename1_impl_ios_IOSNative_vpnSetStatusListening___boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_BOOLEAN listening) {
#ifdef CN1_VPN_HAS_NE
    BOOL wanted = listening != JAVA_FALSE;
    cn1vpEnsureInstallLock();
    @synchronized (cn1vpInstallLock) {
        // Published under the lock the two callback queues read it through.
        cn1vpListening = wanted;
    }
    if (wanted) {
        // Before the observer, so the load's own notification finds a
        // listening app: the baseline a new listener sees has to be the
        // profile iOS actually holds, not the unloaded manager's Invalid.
        cn1vpEnsureLoaded();
    }
    if (wanted && cn1vpObserver == nil) {
        cn1vpWatcher = [[CN1VpnStatusWatcher alloc] init];
        [[NSNotificationCenter defaultCenter] addObserver:cn1vpWatcher
                selector:@selector(statusChanged:)
                name:NEVPNStatusDidChangeNotification object:nil];
        cn1vpObserver = cn1vpWatcher;
    } else if (!wanted && cn1vpObserver != nil) {
        [[NSNotificationCenter defaultCenter] removeObserver:cn1vpObserver];
        // RELEASED, not merely forgotten. Both statics point at the one +1
        // allocation above, and NSNotificationCenter does not retain its
        // observer -- so dropping the pointers leaked a watcher on every
        // add/remove cycle a listener made.
        [cn1vpWatcher release];
        cn1vpObserver = nil;
        cn1vpWatcher = nil;
    }
#endif
}

#if defined(CN1_VPN_TUNNEL) && defined(CN1_VPN_HAS_NE)

/// The saved provider manager, so a stop does not have to reload it.
static NETunnelProviderManager *cn1vpTunnelManager = nil;

/// Watches a starting tunnel so the app is told when it is actually up.
///
/// startVPNTunnelAndReturnError answers whether the REQUEST was accepted.
/// The extension launches afterwards and applies its network settings
/// asynchronously, so a tunnel that fails to launch, or whose
/// setTunnelNetworkSettings errors, does so long after that call returned
/// YES. Acknowledging there reported a tunnel that was never up.
@interface CN1VpnTunnelWatcher : NSObject
@property (nonatomic, assign) int requestId;
@property (nonatomic, assign) BOOL answered;
/// Whether the connection has left the state it was started in; see
/// statusChanged.
@property (nonatomic, assign) BOOL transitioned;
- (void)statusChanged:(NSNotification *)note;
@end

static CN1VpnTunnelWatcher *cn1vpTunnelWatcher = nil;

/// Which start is current.
///
/// The watcher covers a start once it is armed. Everything before that --
/// loading the manager, saving it, reloading it -- is asynchronous too, and a
/// stop arriving in that window found no watcher and no manager, answered
/// successfully, and then the continuation went on to start the session. The
/// tunnel came up after the caller was told it was down.
///
/// MAIN QUEUE ONLY, and that is what makes the check mean anything. A
/// generation compared on one thread and acted on while another may change
/// it is a narrower window, not a closed one -- and vpnStopTunnel is called
/// from a Java thread. Every read and every write of this, of
/// cn1vpTunnelWatcher and of cn1vpTunnelManager happens on the main queue,
/// so a check and the act that follows it cannot be interleaved with a stop.
static int cn1vpTunnelGeneration = 0;

/// Stops watching and releases the watcher, ANSWERING it if it never was.
///
/// A watcher carries the only completion path its start has. Releasing one
/// that has not answered -- because a stop arrived while it was connecting,
/// or a second start replaced it -- left the application's AsyncResource
/// unresolved for ever and its tunnel retained with it.
///
/// No lock: every path that touches this -- the start, the stop and the
/// notification -- runs on the main queue.
static void cn1vpStopWatchingTunnel(void) {
    if (cn1vpTunnelWatcher == nil) {
        return;
    }
    CN1VpnTunnelWatcher *watcher = cn1vpTunnelWatcher;
    cn1vpTunnelWatcher = nil;
    [[NSNotificationCenter defaultCenter] removeObserver:watcher];
    if (!watcher.answered) {
        watcher.answered = YES;
        cn1vpTunnelAck(watcher.requestId, NO, CN1_VPN_ERR_UNKNOWN,
                @"The tunnel start was superseded before it connected");
    }
    [watcher release];
}

@implementation CN1VpnTunnelWatcher

- (void)statusChanged:(NSNotification *)note {
    if (self.answered) {
        return;
    }
    NEVPNStatus status = ((NEVPNConnection *)note.object).status;
    if (status == NEVPNStatusConnected) {
        self.answered = YES;
        cn1vpTunnelAck(self.requestId, YES, 0, nil);
        cn1vpStopWatchingTunnel();
        return;
    }
    if (self.transitioned
            && (status == NEVPNStatusDisconnected
                    || status == NEVPNStatusInvalid)) {
        // The extension refused to come up, or came up and stopped before it
        // connected. Either way the app asked for a tunnel and has not got
        // one, and this SPI calls an operation that never answers worse than
        // one that fails.
        //
        // Only once the connection has MOVED, though. startVPNTunnel is
        // asynchronous, so the status is still Disconnected for a moment
        // after it is accepted -- and the immediate probe below reads it
        // there. Failing on that reading reported a tunnel as failed while
        // it was on its way up, and took the observer down with it so the
        // Connected that followed had nobody to tell.
        self.answered = YES;
        cn1vpTunnelAck(self.requestId, NO, CN1_VPN_ERR_UNKNOWN,
                @"The packet tunnel extension did not start; check its"
                @" provisioning profile and entitlement");
        cn1vpStopWatchingTunnel();
        return;
    }
    if (status != NEVPNStatusDisconnected) {
        // Anything other than the state it started in counts as movement;
        // from here a return to Disconnected is a real failure.
        self.transitioned = YES;
    }
    // Connecting and Reasserting are passed over: they are the states this
    // is waiting through.
}

@end

/// How long a stop waits for the connection to actually leave.
///
/// A bound rather than a guess at the right answer: the point is that the
/// request is answered, and this SPI calls an operation that never answers
/// worse than one that fails.
#define CN1_VPN_STOP_TIMEOUT_SECONDS 10

/// Watches a STOP until the connection has really gone.
///
/// stopVPNTunnel only begins the disconnect. Acknowledging the request on the
/// next line said "down" while the tunnel was still routing, so an app could
/// free its resources, or start a replacement, against a link that was still
/// carrying packets and an extension that had not yet been told to stop.
///
/// The file already made this argument in the other direction two branches
/// down, where a manager that could not be loaded refuses to answer YES
/// precisely because the tunnel may still be up. Both halves now agree.
@interface CN1VpnTunnelStopWatcher : NSObject
@property (nonatomic, assign) int requestId;
@property (nonatomic, assign) BOOL answered;
- (void)statusChanged:(NSNotification *)note;
@end

static CN1VpnTunnelStopWatcher *cn1vpTunnelStopWatcher = nil;

/// Which stop is current, so a timeout cannot answer a later one.
///
/// MAIN QUEUE ONLY, for the reason cn1vpTunnelGeneration gives.
static int cn1vpStopGeneration = 0;

/// Answers the stop being watched, once, and stops watching.
static void cn1vpFinishStop(BOOL ok, int error, NSString *message) {
    if (cn1vpTunnelStopWatcher == nil) {
        return;
    }
    CN1VpnTunnelStopWatcher *watcher = cn1vpTunnelStopWatcher;
    cn1vpTunnelStopWatcher = nil;
    [[NSNotificationCenter defaultCenter] removeObserver:watcher];
    if (!watcher.answered) {
        watcher.answered = YES;
        cn1vpTunnelAck(watcher.requestId, ok, error, message);
    }
    [watcher release];
}

@implementation CN1VpnTunnelStopWatcher

- (void)statusChanged:(NSNotification *)note {
    if (self.answered) {
        return;
    }
    NEVPNStatus status = ((NEVPNConnection *)note.object).status;
    // Disconnected is the answer; Invalid is one too, and means the
    // configuration went away underneath -- there is no tunnel either way,
    // which is what the caller asked for. Disconnecting is passed over: it is
    // the state this is waiting through, and it is exactly the state the old
    // code answered from.
    if (status == NEVPNStatusDisconnected || status == NEVPNStatusInvalid) {
        cn1vpFinishStop(YES, 0, nil);
    }
}

@end

/// Asks a session to stop and answers `rid` once it has.
///
/// Called on the main queue only, like everything else that touches the
/// watcher and the generations.
static void cn1vpStopAndWatch(int rid, NETunnelProviderSession *session) {
    // A previous stop still waiting is superseded rather than abandoned: it
    // carries the only completion path its request has.
    cn1vpFinishStop(NO, CN1_VPN_ERR_UNKNOWN,
            @"The tunnel stop was superseded by another stop");
    int generation = ++cn1vpStopGeneration;
    CN1VpnTunnelStopWatcher *watcher =
            [[CN1VpnTunnelStopWatcher alloc] init];
    watcher.requestId = rid;
    cn1vpTunnelStopWatcher = watcher;
    // OBSERVED BEFORE the stop is asked for. Armed afterwards, a disconnect
    // that completed immediately would post its notification to nobody and
    // the request would wait out the timeout for something that had already
    // happened.
    [[NSNotificationCenter defaultCenter]
            addObserver:watcher
            selector:@selector(statusChanged:)
            name:NEVPNStatusDidChangeNotification
            object:session];
    [session stopVPNTunnel];
    // Checked once immediately, for the connection that is already down --
    // a manager with no saved configuration reads Invalid, and asking a
    // tunnel that is not running to stop has got the caller what they asked
    // for. Without this the ordinary "stop something already stopped" case
    // would post no further notification and answer only on the timeout.
    [watcher statusChanged:
            [NSNotification notificationWithName:NEVPNStatusDidChangeNotification
                    object:session]];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW,
                    (int64_t)CN1_VPN_STOP_TIMEOUT_SECONDS * NSEC_PER_SEC),
            dispatch_get_main_queue(), ^{
        // Only if THIS stop is still the current one. A later stop has its
        // own watcher and its own deadline, and answering here would settle
        // a request that is still legitimately waiting.
        if (generation == cn1vpStopGeneration) {
            cn1vpFinishStop(NO, CN1_VPN_ERR_TIMEOUT,
                    @"The tunnel did not disconnect");
        }
    });
}

/// Loads or creates the manager for this app's packet tunnel.
///
/// NETunnelProviderManager is per app, and loading is asynchronous, so this
/// answers through the block rather than returning. A first run has no saved
/// configuration and gets a fresh manager, which is the ordinary path -- an
/// app installs its tunnel configuration the first time it starts one.
static void cn1vpLoadTunnelManager(void (^done)(NETunnelProviderManager *m,
        NSError *e)) {
    [NETunnelProviderManager loadAllFromPreferencesWithCompletionHandler:
            ^(NSArray<NETunnelProviderManager *> *managers, NSError *error) {
        if (error != nil) {
            done(nil, error);
            return;
        }
        NETunnelProviderManager *m = [managers count] > 0
                ? [managers objectAtIndex:0]
                : [[[NETunnelProviderManager alloc] init] autorelease];
        done(m, nil);
    }];
}

void com_codename1_impl_ios_IOSNative_vpnStartTunnel___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT setupWire) {
    // COPIED out of the Java string here, on the calling thread. The blocks
    // below run later and on another queue, where the JAVA_OBJECT is no
    // longer safe to touch.
    NSString *wire = setupWire == JAVA_NULL ? @""
            : [NSString stringWithUTF8String:
                    stringToUTF8(threadStateData, setupWire)];
    [wire retain];
    int rid = (int)requestId;
    // ONTO the main queue before anything is read or written; see
    // cn1vpTunnelGeneration. This native is called from a Java thread.
    dispatch_async(dispatch_get_main_queue(), ^{
    int generation = ++cn1vpTunnelGeneration;
    cn1vpLoadTunnelManager(^(NETunnelProviderManager *m, NSError *loadError) {
        if (generation != cn1vpTunnelGeneration) {
            cn1vpTunnelAck(rid, NO, CN1_VPN_ERR_UNKNOWN,
                    @"The tunnel start was superseded before it opened");
            [wire release];
            return;
        }
        if (m == nil) {
            cn1vpFail(rid, CN1_VPN_ERR_UNKNOWN, loadError);
            [wire release];
            return;
        }
        NETunnelProviderProtocol *proto =
                [[[NETunnelProviderProtocol alloc] init] autorelease];
        // The extension's bundle identifier, which is the app's plus the
        // suffix the generated target signs under. Baked in by the builder
        // through this plist key rather than guessed, because a project can
        // override the extension's PRODUCT_BUNDLE_IDENTIFIER.
        proto.providerBundleIdentifier =
                cn1vpPlistString(@"CN1VpnTunnelExtensionIdentifier");
        // serverAddress is what the system shows in Settings for this VPN.
        // It is display text to iOS, not something it connects to -- the
        // tunnel decides that -- so the session name goes here when the app
        // gave one.
        NSString *display = cn1vpWireField(wire, 6);
        if ([display length] == 0) {
            display = cn1vpWireField(wire, 1);
        }
        proto.serverAddress = [display length] > 0 ? display : @"VPN";
        // The WHOLE setup, handed to the extension. This dictionary is the
        // only channel between the two processes at start-up, and the
        // extension reads the same record with the same field indices.
        proto.providerConfiguration = [NSDictionary dictionaryWithObject:wire
                forKey:@"cn1TunnelSetup"];
        m.protocolConfiguration = proto;
        m.localizedDescription = proto.serverAddress;
        m.enabled = YES;
        [m saveToPreferencesWithCompletionHandler:^(NSError *saveError) {
            if (saveError != nil) {
                cn1vpFail(rid, CN1_VPN_ERR_UNKNOWN, saveError);
                [wire release];
                return;
            }
            // RELOADED before starting. A manager that has just been saved
            // is stale in this process until it is read back, and
            // startVPNTunnel on a stale manager fails with a configuration
            // error that names nothing the developer wrote.
            [m loadFromPreferencesWithCompletionHandler:^(NSError *reloadError) {
                if (reloadError != nil) {
                    cn1vpFail(rid, CN1_VPN_ERR_UNKNOWN, reloadError);
                    [wire release];
                    return;
                }
                NSError *startError = nil;
                NETunnelProviderSession *session =
                        (NETunnelProviderSession *)m.connection;
                if (generation != cn1vpTunnelGeneration) {
                    // A stop landed while this was saving and reloading.
                    // Starting now would bring the tunnel up behind the
                    // answer that caller already has.
                    cn1vpTunnelAck(rid, NO, CN1_VPN_ERR_UNKNOWN,
                            @"The tunnel start was superseded while it was"
                            @" opening");
                    [wire release];
                    return;
                }
                BOOL ok = [session startVPNTunnelAndReturnError:&startError];
                if (!ok) {
                    cn1vpFail(rid, CN1_VPN_ERR_UNKNOWN, startError);
                    [wire release];
                    return;
                }
                [cn1vpTunnelManager release];
                cn1vpTunnelManager = [m retain];
                // NOT acknowledged here; see CN1VpnTunnelWatcher. The call
                // above accepted the REQUEST, and the extension that has to
                // launch and apply its settings has not run yet.
                cn1vpStopWatchingTunnel();
                cn1vpTunnelWatcher = [[CN1VpnTunnelWatcher alloc] init];
                cn1vpTunnelWatcher.requestId = rid;
                [[NSNotificationCenter defaultCenter]
                        addObserver:cn1vpTunnelWatcher
                        selector:@selector(statusChanged:)
                        name:NEVPNStatusDidChangeNotification
                        object:session];
                // Checked once immediately for the CONNECTED case only: a
                // connection already up posts no further notification and
                // would leave the request pending for ever. A Disconnected
                // reading here means "not started yet", not "failed", which
                // is what the transitioned flag above is about.
                [cn1vpTunnelWatcher statusChanged:
                        [NSNotification notificationWithName:
                                NEVPNStatusDidChangeNotification
                                object:session]];
                [wire release];
            }];
        }];
    });
    });
}

void com_codename1_impl_ios_IOSNative_vpnStopTunnel___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
    int rid = (int)requestId;
    // ONTO the main queue, like the start: this runs on a Java thread, and
    // the generation it is about to bump is read there. See
    // cn1vpTunnelGeneration.
    dispatch_async(dispatch_get_main_queue(), ^{
    // Invalidates any start in flight, armed or not. The watcher covers the
    // armed ones and this covers the window before that, where there is
    // nothing to disarm.
    cn1vpTunnelGeneration++;
    cn1vpStopWatchingTunnel();
    if (cn1vpTunnelManager != nil) {
        // WATCHED, not fired and forgotten. stopVPNTunnel begins an
        // asynchronous disconnect; answering on the next line told
        // Tunnels.stop() the tunnel was down while it was still routing, and
        // an app that then released its resources or started a replacement
        // was racing a link that had not gone anywhere yet.
        cn1vpStopAndWatch(rid,
                (NETunnelProviderSession *)cn1vpTunnelManager.connection);
        return;
    }
    // No manager in this process. Loaded rather than refused: the app may
    // have been restarted while its tunnel kept running, which is exactly
    // when a stop matters most.
    cn1vpLoadTunnelManager(^(NETunnelProviderManager *m, NSError *e) {
        if (m == nil) {
            // The LOAD failed, which is the only way the helper yields nil:
            // with no error it hands back a manager either way, a saved one
            // or a fresh one whose connection is already disconnected. So
            // this branch is not "nothing was running" -- it is "iOS would
            // not tell us what is running", and the tunnel may well still be
            // up and routing. Answering YES here reported a stopped tunnel
            // to Tunnels.stop() while traffic kept flowing through it.
            cn1vpFail(rid, CN1_VPN_ERR_UNKNOWN, e);
            return;
        }
        // The same wait as the branch above, and for the same reason. The
        // disconnected manager still answers immediately: its connection
        // reads Disconnected or Invalid, the watcher's opening probe sees
        // that, and asking a tunnel that is not running to stop has got the
        // caller what they asked for.
        cn1vpStopAndWatch(rid, (NETunnelProviderSession *)m.connection);
    });
    });
}

#elif defined(CN1_INCLUDE_VPN)

void com_codename1_impl_ios_IOSNative_vpnStartTunnel___int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId, JAVA_OBJECT setupWire) {
    // The unsupported half, present so a build without the tunnel target
    // links identically. Answered rather than ignored: the SPI calls an
    // operation that never answers worse than one that fails.
    com_codename1_impl_ios_IOSCallCallbacks_vpnTunnelAck___int_boolean_int_java_lang_String(
            threadStateData, requestId, JAVA_FALSE, CN1_VPN_ERR_NOT_SUPPORTED,
            cn1vpJString(@"This build has no packet tunnel extension"));
}

void com_codename1_impl_ios_IOSNative_vpnStopTunnel___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject,
        JAVA_INT requestId) {
    com_codename1_impl_ios_IOSCallCallbacks_vpnTunnelAck___int_boolean_int_java_lang_String(
            threadStateData, requestId, JAVA_FALSE, CN1_VPN_ERR_NOT_SUPPORTED,
            cn1vpJString(@"This build has no packet tunnel extension"));
}

#endif
