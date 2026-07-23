/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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

// Native implementation of IOSNative.webauthnSupported(),
// .webauthnCreate(String) and .webauthnGet(String). Implements the
// com.codename1.io.webauthn.WebAuthnClient primitive via
// ASAuthorizationPlatformPublicKeyCredentialProvider (iOS 16+).
//
// Inputs are W3C PublicKeyCredentialCreationOptionsJSON /
// PublicKeyCredentialRequestOptionsJSON documents; the JSON is parsed with
// NSJSONSerialization and the relevant fields fed to the OS authenticator.
// Outputs are RegistrationResponseJSON / AuthenticationResponseJSON; binary
// fields (challenge, credential id, attestation object, signature, etc.) are
// emitted base64url-encoded so they round-trip cleanly against every WebAuthn
// server library on the market.

#include "xmlvm.h"
#ifndef NEW_CODENAME_ONE_VM
#include "xmlvm-util.h"
#endif
#import "CodenameOne_GLViewController.h"

#ifdef CN1_INCLUDE_WEBAUTHN

#import <UIKit/UIKit.h>
#import <AuthenticationServices/AuthenticationServices.h>

#ifdef NEW_CODENAME_ONE_VM
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);
extern NSString*   toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
#else
extern JAVA_OBJECT fromNSString(NSString* str);
extern NSString*   toNSString(JAVA_OBJECT str);
#endif

// --------------------------------------------------------------------------
// base64url helpers. The W3C JSON wire format uses base64url *without*
// padding; iOS's NSData base64 methods produce standard base64 *with* padding,
// so we translate in both directions.

static NSData *cn1WebAuthnB64UrlDecode(NSString *s) {
    if (s == nil) return nil;
    NSMutableString *m = [NSMutableString stringWithString:s];
    [m replaceOccurrencesOfString:@"-" withString:@"+"
                          options:0 range:NSMakeRange(0, m.length)];
    [m replaceOccurrencesOfString:@"_" withString:@"/"
                          options:0 range:NSMakeRange(0, m.length)];
    while (m.length % 4 != 0) {
        [m appendString:@"="];
    }
    return [[NSData alloc] initWithBase64EncodedString:m options:0];
}

static NSString *cn1WebAuthnB64UrlEncode(NSData *data) {
    if (data == nil) return @"";
    NSString *std = [data base64EncodedStringWithOptions:0];
    NSMutableString *m = [NSMutableString stringWithString:std];
    [m replaceOccurrencesOfString:@"+" withString:@"-"
                          options:0 range:NSMakeRange(0, m.length)];
    [m replaceOccurrencesOfString:@"/" withString:@"_"
                          options:0 range:NSMakeRange(0, m.length)];
    [m replaceOccurrencesOfString:@"=" withString:@""
                          options:0 range:NSMakeRange(0, m.length)];
    return m;
}

// --------------------------------------------------------------------------
// Delegate that captures the outcome of an ASAuthorizationController run and
// signals a semaphore so the worker thread can return to Java.

API_AVAILABLE(ios(16.0))
@interface CN1WebAuthnDelegate : NSObject <ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding>
@property (nonatomic, strong) NSString *resultJson;
@property (nonatomic, strong) NSString *errorCode;
@property (nonatomic, strong) NSString *errorMessage;
@property (nonatomic, copy)   void(^completion)(void);
@end

@implementation CN1WebAuthnDelegate

- (ASPresentationAnchor)presentationAnchorForAuthorizationController:(ASAuthorizationController *)controller {
    UIWindow *anchor = nil;
    if (@available(iOS 13.0, *)) {
        for (UIScene *scene in [UIApplication sharedApplication].connectedScenes) {
            if (scene.activationState == UISceneActivationStateForegroundActive &&
                [scene isKindOfClass:[UIWindowScene class]]) {
                UIWindowScene *ws = (UIWindowScene *)scene;
                for (UIWindow *w in ws.windows) {
                    if (w.isKeyWindow) { anchor = w; break; }
                }
                if (anchor) break;
                if (ws.windows.count > 0) {
                    anchor = ws.windows.firstObject;
                    break;
                }
            }
        }
    }
    if (!anchor) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
        anchor = [UIApplication sharedApplication].keyWindow;
#pragma clang diagnostic pop
    }
    return anchor;
}

- (void)setErrorFromASError:(NSError *)error {
    NSString *code = @"NotAllowedError";
    if ([error.domain isEqualToString:ASAuthorizationErrorDomain]) {
        switch (error.code) {
            case ASAuthorizationErrorCanceled:           code = @"NotAllowedError";   break;
            case ASAuthorizationErrorFailed:             code = @"SecurityError";     break;
            case ASAuthorizationErrorInvalidResponse:    code = @"invalid_response";  break;
            case ASAuthorizationErrorNotHandled:         code = @"NotSupportedError"; break;
            case ASAuthorizationErrorUnknown:            code = @"transport_error";   break;
            default:
                code = @"transport_error";
                break;
        }
    }
    self.errorCode = code;
    self.errorMessage = error.localizedDescription
            ?: [NSString stringWithFormat:@"AS error %ld", (long)error.code];
}

- (void)authorizationController:(ASAuthorizationController *)controller
   didCompleteWithAuthorization:(ASAuthorization *)authorization {
    if (@available(iOS 16.0, *)) {
        if ([authorization.credential isKindOfClass:
             [ASAuthorizationPlatformPublicKeyCredentialRegistration class]]) {
            ASAuthorizationPlatformPublicKeyCredentialRegistration *reg =
                (ASAuthorizationPlatformPublicKeyCredentialRegistration *)authorization.credential;
            NSString *rawId = cn1WebAuthnB64UrlEncode(reg.credentialID);
            NSString *clientData = cn1WebAuthnB64UrlEncode(reg.rawClientDataJSON);
            NSString *attestation = cn1WebAuthnB64UrlEncode(reg.rawAttestationObject);
            NSDictionary *result = @{
                @"id":   rawId,
                @"rawId": rawId,
                @"type": @"public-key",
                @"authenticatorAttachment": @"platform",
                @"response": @{
                    @"clientDataJSON":    clientData,
                    @"attestationObject": attestation,
                    @"transports": @[@"internal"]
                },
                @"clientExtensionResults": @{}
            };
            NSError *jsonErr = nil;
            NSData *data = [NSJSONSerialization dataWithJSONObject:result options:0 error:&jsonErr];
            if (data) {
                self.resultJson = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            } else {
                self.errorCode = @"invalid_response";
                self.errorMessage = jsonErr.localizedDescription ?: @"Failed to encode registration response";
            }
            if (self.completion) self.completion();
            return;
        }
        if ([authorization.credential isKindOfClass:
             [ASAuthorizationPlatformPublicKeyCredentialAssertion class]]) {
            ASAuthorizationPlatformPublicKeyCredentialAssertion *as =
                (ASAuthorizationPlatformPublicKeyCredentialAssertion *)authorization.credential;
            NSString *rawId = cn1WebAuthnB64UrlEncode(as.credentialID);
            NSDictionary *result = @{
                @"id":   rawId,
                @"rawId": rawId,
                @"type": @"public-key",
                @"authenticatorAttachment": @"platform",
                @"response": @{
                    @"clientDataJSON":    cn1WebAuthnB64UrlEncode(as.rawClientDataJSON),
                    @"authenticatorData": cn1WebAuthnB64UrlEncode(as.rawAuthenticatorData),
                    @"signature":         cn1WebAuthnB64UrlEncode(as.signature),
                    @"userHandle":        cn1WebAuthnB64UrlEncode(as.userID)
                },
                @"clientExtensionResults": @{}
            };
            NSError *jsonErr = nil;
            NSData *data = [NSJSONSerialization dataWithJSONObject:result options:0 error:&jsonErr];
            if (data) {
                self.resultJson = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            } else {
                self.errorCode = @"invalid_response";
                self.errorMessage = jsonErr.localizedDescription ?: @"Failed to encode assertion response";
            }
            if (self.completion) self.completion();
            return;
        }
    }
    self.errorCode = @"NotSupportedError";
    self.errorMessage = @"Unexpected ASAuthorization credential type";
    if (self.completion) self.completion();
}

- (void)authorizationController:(ASAuthorizationController *)controller
           didCompleteWithError:(NSError *)error {
    [self setErrorFromASError:error];
    if (self.completion) self.completion();
}

@end

// --------------------------------------------------------------------------
// Strong refs to the delegate / controller for the duration of a flow. ARC
// would otherwise drop them as soon as performRequests returns.

static id g_cn1WebAuthnCurrentDelegate = nil;
static id g_cn1WebAuthnCurrentController = nil;

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_webauthnSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (@available(iOS 16.0, *)) {
        return NSClassFromString(@"ASAuthorizationPlatformPublicKeyCredentialProvider") != nil
                ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

// Builds the ERR:code:message string the Java side parses with WebAuthnNativeImpl#unwrap.
static NSString *cn1WebAuthnErrorString(NSString *code, NSString *msg) {
    if (code == nil) code = @"transport_error";
    if (msg == nil)  msg = @"Native authenticator failed";
    return [NSString stringWithFormat:@"ERR:%@:%@", code, msg];
}

static NSDictionary *cn1WebAuthnParse(NSString *jsonStr) {
    if (jsonStr == nil) return nil;
    NSData *data = [jsonStr dataUsingEncoding:NSUTF8StringEncoding];
    if (data == nil) return nil;
    id obj = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    return [obj isKindOfClass:[NSDictionary class]] ? (NSDictionary *)obj : nil;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_webauthnCreate___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT optionsJsonObj) {
    if (@available(iOS 16.0, *)) {
        // fall through
    } else {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(@"not_supported", @"iOS 16+ required for passkeys"));
    }
    NSString *optionsJson = toNSString(CN1_THREAD_STATE_PASS_ARG optionsJsonObj);
    NSDictionary *opts = cn1WebAuthnParse(optionsJson);
    if (opts == nil) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(@"invalid_options", @"Could not parse creation options JSON"));
    }
    NSDictionary *rp   = [opts objectForKey:@"rp"];
    NSDictionary *user = [opts objectForKey:@"user"];
    NSString *rpId           = [rp   isKindOfClass:[NSDictionary class]] ? [rp   objectForKey:@"id"]   : nil;
    NSString *userId         = [user isKindOfClass:[NSDictionary class]] ? [user objectForKey:@"id"]   : nil;
    NSString *userName       = [user isKindOfClass:[NSDictionary class]] ? [user objectForKey:@"name"] : nil;
    NSString *challengeB64Url = [opts objectForKey:@"challenge"];
    if (rpId == nil || userId == nil || challengeB64Url == nil) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(@"invalid_options",
                        @"creation options missing rp.id / user.id / challenge"));
    }
    NSData *challengeData = cn1WebAuthnB64UrlDecode(challengeB64Url);
    NSData *userIdData    = cn1WebAuthnB64UrlDecode(userId);
    if (challengeData == nil || userIdData == nil) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(@"invalid_options",
                        @"challenge / user.id must be base64url-encoded"));
    }

    __block NSString *finalResult = nil;
    __block NSString *finalErrCode = nil;
    __block NSString *finalErrMsg = nil;
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);

    dispatch_async(dispatch_get_main_queue(), ^{
        ASAuthorizationPlatformPublicKeyCredentialProvider *provider =
            [[ASAuthorizationPlatformPublicKeyCredentialProvider alloc]
                initWithRelyingPartyIdentifier:rpId];
        ASAuthorizationPlatformPublicKeyCredentialRegistrationRequest *request =
            [provider createCredentialRegistrationRequestWithChallenge:challengeData
                                                                  name:(userName ?: @"")
                                                                userID:userIdData];

        // Optional: honour userVerification when the server requested it.
        NSDictionary *authSel = [opts objectForKey:@"authenticatorSelection"];
        if ([authSel isKindOfClass:[NSDictionary class]]) {
            NSString *uv = [authSel objectForKey:@"userVerification"];
            if ([uv isEqualToString:@"required"]) {
                request.userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired;
            } else if ([uv isEqualToString:@"discouraged"]) {
                request.userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferenceDiscouraged;
            } else if (uv != nil) {
                request.userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferencePreferred;
            }
        }

        CN1WebAuthnDelegate *del = [[CN1WebAuthnDelegate alloc] init];
        del.completion = ^{
            finalResult = del.resultJson;
            finalErrCode = del.errorCode;
            finalErrMsg = del.errorMessage;
            dispatch_semaphore_signal(sem);
        };

        ASAuthorizationController *controller =
            [[ASAuthorizationController alloc] initWithAuthorizationRequests:@[request]];
        controller.delegate = del;
        controller.presentationContextProvider = del;

        g_cn1WebAuthnCurrentDelegate = del;
        g_cn1WebAuthnCurrentController = controller;
        [controller performRequests];
    });

    // Hour-cap is purely defensive; users finish in seconds.
    dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3600 * NSEC_PER_SEC)));
    g_cn1WebAuthnCurrentDelegate = nil;
    g_cn1WebAuthnCurrentController = nil;

    if (finalErrCode != nil) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(finalErrCode, finalErrMsg));
    }
    if (finalResult == nil) {
        return JAVA_NULL;
    }
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG finalResult);
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_webauthnGet___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT optionsJsonObj) {
    if (@available(iOS 16.0, *)) {
        // fall through
    } else {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(@"not_supported", @"iOS 16+ required for passkeys"));
    }
    NSString *optionsJson = toNSString(CN1_THREAD_STATE_PASS_ARG optionsJsonObj);
    NSDictionary *opts = cn1WebAuthnParse(optionsJson);
    if (opts == nil) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(@"invalid_options", @"Could not parse request options JSON"));
    }
    NSString *rpId = [opts objectForKey:@"rpId"];
    NSString *challengeB64Url = [opts objectForKey:@"challenge"];
    if (rpId == nil || challengeB64Url == nil) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(@"invalid_options",
                        @"request options missing rpId / challenge"));
    }
    NSData *challengeData = cn1WebAuthnB64UrlDecode(challengeB64Url);
    if (challengeData == nil) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(@"invalid_options",
                        @"challenge must be base64url-encoded"));
    }

    NSMutableArray<ASAuthorizationPlatformPublicKeyCredentialDescriptor *> *allowed = [NSMutableArray array];
    NSArray *allowList = [opts objectForKey:@"allowCredentials"];
    if ([allowList isKindOfClass:[NSArray class]]) {
        for (id raw in allowList) {
            if (![raw isKindOfClass:[NSDictionary class]]) continue;
            NSString *credIdB64Url = [(NSDictionary *)raw objectForKey:@"id"];
            NSData *credIdData = cn1WebAuthnB64UrlDecode(credIdB64Url);
            if (credIdData != nil) {
                [allowed addObject:
                    [[ASAuthorizationPlatformPublicKeyCredentialDescriptor alloc]
                        initWithCredentialID:credIdData]];
            }
        }
    }

    __block NSString *finalResult = nil;
    __block NSString *finalErrCode = nil;
    __block NSString *finalErrMsg = nil;
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);

    dispatch_async(dispatch_get_main_queue(), ^{
        ASAuthorizationPlatformPublicKeyCredentialProvider *provider =
            [[ASAuthorizationPlatformPublicKeyCredentialProvider alloc]
                initWithRelyingPartyIdentifier:rpId];
        ASAuthorizationPlatformPublicKeyCredentialAssertionRequest *request =
            [provider createCredentialAssertionRequestWithChallenge:challengeData];
        if (allowed.count > 0) {
            request.allowedCredentials = allowed;
        }
        NSString *uv = [opts objectForKey:@"userVerification"];
        if ([uv isEqualToString:@"required"]) {
            request.userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired;
        } else if ([uv isEqualToString:@"discouraged"]) {
            request.userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferenceDiscouraged;
        } else if (uv != nil) {
            request.userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferencePreferred;
        }

        CN1WebAuthnDelegate *del = [[CN1WebAuthnDelegate alloc] init];
        del.completion = ^{
            finalResult = del.resultJson;
            finalErrCode = del.errorCode;
            finalErrMsg = del.errorMessage;
            dispatch_semaphore_signal(sem);
        };

        ASAuthorizationController *controller =
            [[ASAuthorizationController alloc] initWithAuthorizationRequests:@[request]];
        controller.delegate = del;
        controller.presentationContextProvider = del;

        g_cn1WebAuthnCurrentDelegate = del;
        g_cn1WebAuthnCurrentController = controller;
        [controller performRequests];
    });

    dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3600 * NSEC_PER_SEC)));
    g_cn1WebAuthnCurrentDelegate = nil;
    g_cn1WebAuthnCurrentController = nil;

    if (finalErrCode != nil) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                cn1WebAuthnErrorString(finalErrCode, finalErrMsg));
    }
    if (finalResult == nil) {
        return JAVA_NULL;
    }
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG finalResult);
}

#else

// Stubs when CN1_INCLUDE_WEBAUTHN is not defined: app didn't reference any
// com.codename1.io.webauthn.* class, so the Java side won't load
// WebAuthnNativeImpl and these natives are unreachable. ParparVM still needs
// the symbols to satisfy the native-method declarations on IOSNative.java.

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_webauthnSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    return JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_webauthnCreate___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT optionsJsonObj) {
    return JAVA_NULL;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_webauthnGet___java_lang_String_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT optionsJsonObj) {
    return JAVA_NULL;
}

#endif // CN1_INCLUDE_WEBAUTHN
