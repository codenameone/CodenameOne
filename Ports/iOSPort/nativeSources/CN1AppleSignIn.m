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

// Native implementation of IOSNative.appleSignInSupported(),
// .appleSignIn(String, String), .appleSignInIsLoggedIn() and
// .appleSignInSignOut(). Implements com.codename1.social.AppleSignIn via
// ASAuthorizationAppleIDProvider (iOS 13+).

#include "xmlvm.h"
#ifndef NEW_CODENAME_ONE_VM
#include "xmlvm-util.h"
#endif
#import <UIKit/UIKit.h>
#import <AuthenticationServices/AuthenticationServices.h>

#ifdef NEW_CODENAME_ONE_VM
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);
extern NSString*   toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
#else
extern JAVA_OBJECT fromNSString(NSString* str);
extern NSString*   toNSString(JAVA_OBJECT str);
#endif

// Persists the Apple `user` identifier so subsequent isLoggedIn checks can
// ask `ASAuthorizationAppleIDProvider getCredentialStateForUserID:` -- which
// is the most accurate "signed in?" signal on iOS.
static NSString * const kCN1AppleUserDefaultsKey = @"cn1.applesignin.userid";

API_AVAILABLE(ios(13.0))
@interface CN1AppleSignInDelegate : NSObject <ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding>
@property (nonatomic, strong) NSString *resultString;
@property (nonatomic, strong) NSError  *errorResult;
@property (nonatomic, copy)   void(^completion)(void);
@end

@implementation CN1AppleSignInDelegate

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

- (void)authorizationController:(ASAuthorizationController *)controller
   didCompleteWithAuthorization:(ASAuthorization *)authorization {
    if (![authorization.credential isKindOfClass:[ASAuthorizationAppleIDCredential class]]) {
        self.errorResult = [NSError errorWithDomain:@"com.codename1.social.AppleSignIn"
                                               code:-1
                                           userInfo:@{NSLocalizedDescriptionKey: @"Unexpected credential type"}];
        if (self.completion) self.completion();
        return;
    }
    ASAuthorizationAppleIDCredential *cred = (ASAuthorizationAppleIDCredential *)authorization.credential;
    NSString *identityToken = cred.identityToken
            ? [[NSString alloc] initWithData:cred.identityToken encoding:NSUTF8StringEncoding]
            : nil;
    NSString *authorizationCode = cred.authorizationCode
            ? [[NSString alloc] initWithData:cred.authorizationCode encoding:NSUTF8StringEncoding]
            : nil;
    NSString *userId = cred.user ?: @"";
    NSString *given = cred.fullName.givenName ?: @"";
    NSString *family = cred.fullName.familyName ?: @"";
    NSString *email = cred.email ?: @"";

    if (cred.user) {
        [[NSUserDefaults standardUserDefaults] setObject:cred.user forKey:kCN1AppleUserDefaultsKey];
    }

    self.resultString = [NSString stringWithFormat:@"%@|%@|%@|%@|%@|%@",
                         identityToken ?: @"",
                         authorizationCode ?: @"",
                         userId,
                         given,
                         family,
                         email];
    if (self.completion) self.completion();
}

- (void)authorizationController:(ASAuthorizationController *)controller
           didCompleteWithError:(NSError *)error {
    self.errorResult = error;
    if (self.completion) self.completion();
}

@end

static id g_cn1AppleCurrentDelegate = nil;
static id g_cn1AppleCurrentController = nil;

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_appleSignInSupported__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (@available(iOS 13.0, *)) {
        return NSClassFromString(@"ASAuthorizationAppleIDProvider") != nil ? JAVA_TRUE : JAVA_FALSE;
    }
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_appleSignInIsLoggedIn__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (@available(iOS 13.0, *)) {
        // fall through
    } else {
        return JAVA_FALSE;
    }
    NSString *uid = [[NSUserDefaults standardUserDefaults] stringForKey:kCN1AppleUserDefaultsKey];
    if (uid == nil || uid.length == 0) {
        return JAVA_FALSE;
    }
    ASAuthorizationAppleIDProvider *provider = [[ASAuthorizationAppleIDProvider alloc] init];
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    __block ASAuthorizationAppleIDProviderCredentialState state =
        ASAuthorizationAppleIDProviderCredentialNotFound;
    [provider getCredentialStateForUserID:uid
                               completion:^(ASAuthorizationAppleIDProviderCredentialState s,
                                            NSError * _Nullable error) {
        state = s;
        dispatch_semaphore_signal(sem);
    }];
    dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(5 * NSEC_PER_SEC)));
    return state == ASAuthorizationAppleIDProviderCredentialAuthorized ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_VOID com_codename1_impl_ios_IOSNative_appleSignInSignOut__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:kCN1AppleUserDefaultsKey];
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_appleSignIn___java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT scopesObj, JAVA_OBJECT nonceObj) {
    if (@available(iOS 13.0, *)) {
        // fall through
    } else {
        return JAVA_NULL;
    }
    NSString *scopes = toNSString(CN1_THREAD_STATE_PASS_ARG scopesObj);
    NSString *nonce  = toNSString(CN1_THREAD_STATE_PASS_ARG nonceObj);
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);

    dispatch_async(dispatch_get_main_queue(), ^{
        ASAuthorizationAppleIDProvider *provider = [[ASAuthorizationAppleIDProvider alloc] init];
        ASAuthorizationAppleIDRequest *request = [provider createRequest];

        NSMutableArray *requested = [NSMutableArray array];
        if (scopes && [scopes rangeOfString:@"name"].location != NSNotFound) {
            [requested addObject:ASAuthorizationScopeFullName];
        }
        if (scopes && [scopes rangeOfString:@"email"].location != NSNotFound) {
            [requested addObject:ASAuthorizationScopeEmail];
        }
        request.requestedScopes = requested.count > 0
            ? requested
            : @[ASAuthorizationScopeFullName, ASAuthorizationScopeEmail];
        if (nonce && nonce.length > 0) {
            request.nonce = nonce;
        }

        CN1AppleSignInDelegate *del = [[CN1AppleSignInDelegate alloc] init];
        del.completion = ^{
            dispatch_semaphore_signal(sem);
        };

        ASAuthorizationController *controller =
            [[ASAuthorizationController alloc] initWithAuthorizationRequests:@[request]];
        controller.delegate = del;
        controller.presentationContextProvider = del;

        g_cn1AppleCurrentDelegate = del;
        g_cn1AppleCurrentController = controller;
        [controller performRequests];
    });

    dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3600 * NSEC_PER_SEC)));

    CN1AppleSignInDelegate *del = (CN1AppleSignInDelegate *)g_cn1AppleCurrentDelegate;
    g_cn1AppleCurrentDelegate = nil;
    g_cn1AppleCurrentController = nil;
    if (del == nil || del.errorResult != nil || del.resultString == nil) {
        return JAVA_NULL;
    }
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG del.resultString);
}
