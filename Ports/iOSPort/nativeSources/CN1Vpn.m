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
static NSData *cn1vpStoreSecret(NSString *account, NSString *secret) {
    if (secret == nil || [secret length] == 0) {
        return nil;
    }
    NSData *value = [secret dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *query = [NSDictionary dictionaryWithObjectsAndKeys:
            (__bridge id)kSecClassGenericPassword, (__bridge id)kSecClass,
            account, (__bridge id)kSecAttrAccount,
            @"com.codename1.vpn", (__bridge id)kSecAttrService, nil];
    SecItemDelete((__bridge CFDictionaryRef)query);
    NSMutableDictionary *add = [NSMutableDictionary dictionaryWithDictionary:query];
    [add setObject:value forKey:(__bridge id)kSecValueData];
    [add setObject:(__bridge id)kCFBooleanTrue
            forKey:(__bridge id)kSecReturnPersistentRef];
    CFTypeRef result = NULL;
    if (SecItemAdd((__bridge CFDictionaryRef)add, &result) != errSecSuccess) {
        return nil;
    }
    return (__bridge_transfer NSData *)result;
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
#if defined(CN1_VPN_HAS_NE) && defined(CN1_VPN_TUNNEL)
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_vpnCapabilities___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_VPN_HAS_NE
    int caps = CN1_VPN_CAP_IKEV2 | CN1_VPN_CAP_IPSEC | CN1_VPN_CAP_ON_DEMAND
            | CN1_VPN_CAP_ALWAYS_ON;
#ifdef CN1_VPN_TUNNEL
    caps |= CN1_VPN_CAP_CUSTOM_TUNNEL;
#endif
    return caps;
#else
    return 0;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_vpnStatus___R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#ifdef CN1_VPN_HAS_NE
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
    BOOL alwaysOn = [cn1vpField(f, 8) isEqualToString:@"1"];
    BOOL onDemand = [cn1vpField(f, 9) isEqualToString:@"1"];
    NSString *name = cn1vpField(f, 10);

    NEVPNManager *manager = [NEVPNManager sharedManager];
    [manager loadFromPreferencesWithCompletionHandler:^(NSError *loadError) {
        if (loadError != nil) {
            cn1vpAck(requestId, NO, CN1_VPN_ERR_UNAUTHORIZED,
                    [loadError localizedDescription]);
            return;
        }
        NEVPNProtocol *cfg;
        if (proto == CN1_VPN_PROTO_IPSEC) {
            NEVPNProtocolIPSec *ipsec = [[NEVPNProtocolIPSec alloc] init];
            ipsec.authenticationMethod = [psk length] > 0
                    ? NEVPNIKEAuthenticationMethodSharedSecret
                    : NEVPNIKEAuthenticationMethodNone;
            if ([psk length] > 0) {
                ipsec.sharedSecretReference =
                        cn1vpStoreSecret(@"cn1vpn.psk", psk);
            }
            ipsec.localIdentifier = [localId length] > 0 ? localId : nil;
            ipsec.remoteIdentifier = [remoteId length] > 0 ? remoteId : server;
            cfg = ipsec;
        } else {
            NEVPNProtocolIKEv2 *ike = [[NEVPNProtocolIKEv2 alloc] init];
            ike.authenticationMethod = [psk length] > 0
                    ? NEVPNIKEAuthenticationMethodSharedSecret
                    : NEVPNIKEAuthenticationMethodNone;
            if ([psk length] > 0) {
                ike.sharedSecretReference = cn1vpStoreSecret(@"cn1vpn.psk", psk);
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
            cfg.passwordReference = cn1vpStoreSecret(@"cn1vpn.password", pass);
        }
        cfg.disconnectOnSleep = NO;

        manager.protocolConfiguration = cfg;
        manager.localizedDescription = [name length] > 0
                ? name : cn1vpSanitize(server);
        manager.enabled = YES;
        manager.onDemandEnabled = onDemand || alwaysOn;
        if (onDemand || alwaysOn) {
            NEOnDemandRuleConnect *rule = [[NEOnDemandRuleConnect alloc] init];
            manager.onDemandRules = [NSArray arrayWithObject:rule];
        }
        [manager saveToPreferencesWithCompletionHandler:^(NSError *saveError) {
            if (saveError == nil) {
                cn1vpAck(requestId, YES, 0, nil);
                return;
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
    NEVPNManager *manager = [NEVPNManager sharedManager];
    [manager loadFromPreferencesWithCompletionHandler:^(NSError *loadError) {
        [manager removeFromPreferencesWithCompletionHandler:^(NSError *error) {
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
