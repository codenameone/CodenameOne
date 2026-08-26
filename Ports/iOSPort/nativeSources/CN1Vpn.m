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

static NSString *cn1vpField(NSArray *fields, NSUInteger index) {
    if (fields == nil || index >= [fields count]) {
        return @"";
    }
    return [fields objectAtIndex:index];
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

/// Whether the shared manager's saved configuration has been read.
static BOOL cn1vpLoaded = NO;

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
    return (__bridge_transfer NSData *)result;
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
    if (!cn1vpListening) {
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
    // Always false. A packet tunnel runs in a Network Extension, which is a
    // separate process with no ParparVM in it, so there is no way to carry a
    // tunnel written in this framework into one.
    return JAVA_FALSE;
}

JAVA_INT com_codename1_impl_ios_IOSNative_vpnCapabilities___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_VPN_HAS_NE
    // No CN1_VPN_CAP_ALWAYS_ON: always-on VPN on iOS needs a supervised
    // device and an MDM payload, not something an app may request.
    // No CN1_VPN_CAP_CUSTOM_TUNNEL: see vpnTunnelSupported above.
    return CN1_VPN_CAP_IKEV2 | CN1_VPN_CAP_IPSEC | CN1_VPN_CAP_ON_DEMAND;
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
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        [[NEVPNManager sharedManager] loadFromPreferencesWithCompletionHandler:
                ^(NSError *error) {
            cn1vpLoaded = YES;
            if (cn1vpListening) {
                com_codename1_impl_ios_IOSCallCallbacks_vpnStatusChanged___int(
                        getThreadLocalData(), cn1vpStatusOrdinal(
                                [[NEVPNManager sharedManager] connection].status));
            }
        }];
    });
}

#endif

JAVA_INT com_codename1_impl_ios_IOSNative_vpnStatus___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_VPN_HAS_NE
    cn1vpEnsureLoaded();
    if (!cn1vpLoaded) {
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
            NEVPNProtocolIPSec *ipsec = [[NEVPNProtocolIPSec alloc] init];
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
            NEVPNProtocolIKEv2 *ike = [[NEVPNProtocolIKEv2 alloc] init];
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
            NEOnDemandRuleConnect *rule = [[NEOnDemandRuleConnect alloc] init];
            manager.onDemandRules = [NSArray arrayWithObject:rule];
        }
        // A keychain write that failed hands back nil, and assigning that as
        // the reference saved a profile with no credential in it: the install
        // reported success and the next connection could not authenticate --
        // having already replaced a profile that worked.
        if (([psk length] > 0 && ipsecSecretMissing)
                || ([pass length] > 0 && passwordMissing)) {
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
        int proto = [cfg isKindOfClass:[NEVPNProtocolIKEv2 class]]
                ? CN1_VPN_PROTO_IKEV2 : CN1_VPN_PROTO_IPSEC;
        NSString *remoteId = @"";
        NSString *localId = @"";
        if ([cfg isKindOfClass:[NEVPNProtocolIPSec class]]) {
            NEVPNProtocolIPSec *ip = (NEVPNProtocolIPSec *)cfg;
            remoteId = ip.remoteIdentifier == nil ? @"" : ip.remoteIdentifier;
            localId = ip.localIdentifier == nil ? @"" : ip.localIdentifier;
        }
        // Fields 5 and 6 -- the password and the shared secret -- are left
        // EMPTY. The platform holds them in the keychain as references and
        // never hands the values back, and the Java side documents that.
        NSArray *fields = [NSArray arrayWithObjects:
                cn1vpSanitize(cfg.serverAddress),
                [NSString stringWithFormat:@"%d", proto],
                cn1vpSanitize(remoteId),
                cn1vpSanitize(localId),
                cfg.username == nil ? @"" : cn1vpSanitize(cfg.username),
                @"", @"", @"",
                manager.onDemandEnabled ? @"1" : @"0",
                manager.onDemandEnabled ? @"1" : @"0",
                manager.localizedDescription == nil ? @""
                        : cn1vpSanitize(manager.localizedDescription), nil];
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
    NEVPNManager *manager = [NEVPNManager sharedManager];
    [manager loadFromPreferencesWithCompletionHandler:^(NSError *loadError) {
        if (loadError != nil || manager.protocolConfiguration == nil) {
            cn1vpAck(requestId, NO, CN1_VPN_ERR_NOT_CONFIGURED,
                    @"No VPN configuration is installed");
            return;
        }
        NSError *error = nil;
        [[manager connection] startVPNTunnelAndReturnError:&error];
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
    NEVPNManager *manager = [NEVPNManager sharedManager];
    [manager loadFromPreferencesWithCompletionHandler:^(NSError *loadError) {
        if (loadError != nil || manager.protocolConfiguration == nil) {
            cn1vpAck(requestId, NO, CN1_VPN_ERR_NOT_CONFIGURED,
                    @"No VPN configuration is installed");
            return;
        }
        [[manager connection] stopVPNTunnel];
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
    cn1vpListening = listening != JAVA_FALSE;
    if (cn1vpListening) {
        // Before the observer, so the load's own notification finds a
        // listening app: the baseline a new listener sees has to be the
        // profile iOS actually holds, not the unloaded manager's Invalid.
        cn1vpEnsureLoaded();
    }
    if (cn1vpListening && cn1vpObserver == nil) {
        cn1vpWatcher = [[CN1VpnStatusWatcher alloc] init];
        [[NSNotificationCenter defaultCenter] addObserver:cn1vpWatcher
                selector:@selector(statusChanged:)
                name:NEVPNStatusDidChangeNotification object:nil];
        cn1vpObserver = cn1vpWatcher;
    } else if (!cn1vpListening && cn1vpObserver != nil) {
        [[NSNotificationCenter defaultCenter] removeObserver:cn1vpObserver];
        cn1vpObserver = nil;
        cn1vpWatcher = nil;
    }
#endif
}
