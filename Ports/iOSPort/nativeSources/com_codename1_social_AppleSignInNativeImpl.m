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

#import "com_codename1_social_AppleSignInNativeImpl.h"
#import <UIKit/UIKit.h>
#import <AuthenticationServices/AuthenticationServices.h>

/// User-defaults key under which we remember the Apple `user identifier`
/// returned on a successful sign-in. ASAuthorizationAppleIDProvider exposes
/// `getCredentialStateForUserID:` which lets us tell whether that credential
/// is still valid for the bundle; that is the most accurate "isLoggedIn?"
/// signal on iOS.
static NSString * const kCN1AppleUserDefaultsKey = @"cn1.applesignin.userid";

/// Delegate + presentation provider that drives a single sign-in attempt.
/// Outlives the call to ASAuthorizationController.performRequests by being
/// strongly held on the impl class until the delegate fires.
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

    NSString *identityToken = nil;
    if (cred.identityToken) {
        identityToken = [[NSString alloc] initWithData:cred.identityToken encoding:NSUTF8StringEncoding];
    }
    NSString *authorizationCode = nil;
    if (cred.authorizationCode) {
        authorizationCode = [[NSString alloc] initWithData:cred.authorizationCode encoding:NSUTF8StringEncoding];
    }
    NSString *userId = cred.user ?: @"";
    NSString *given = cred.fullName.givenName ?: @"";
    NSString *family = cred.fullName.familyName ?: @"";
    NSString *email = cred.email ?: @"";

    // Persist the user id so subsequent isLoggedIn checks can ask the OS
    // about credential state.
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


@implementation com_codename1_social_AppleSignInNativeImpl {
    id _currentDelegate;
    id _currentController;
}

- (BOOL)isSupported {
    if (@available(iOS 13.0, *)) {
        // Compile-time class check covers older SDKs; runtime check guards
        // misconfigured projects that disable the framework link.
        return NSClassFromString(@"ASAuthorizationAppleIDProvider") != nil;
    }
    return NO;
}

- (BOOL)isLoggedIn {
    if (![self isSupported]) {
        return NO;
    }
    NSString *uid = [[NSUserDefaults standardUserDefaults] stringForKey:kCN1AppleUserDefaultsKey];
    if (uid == nil || uid.length == 0) {
        return NO;
    }
    if (@available(iOS 13.0, *)) {
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
        // Cap the wait. The OS-side answer is almost instantaneous.
        dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(5 * NSEC_PER_SEC)));
        return state == ASAuthorizationAppleIDProviderCredentialAuthorized;
    }
    return NO;
}

- (void)signOut {
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:kCN1AppleUserDefaultsKey];
}

- (NSString *)signIn:(NSString *)param param1:(NSString *)param1 {
    NSString *scopes = param;
    NSString *nonce = param1;
    if (![self isSupported]) {
        return nil;
    }
    if (@available(iOS 13.0, *)) {
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
            request.requestedScopes = requested.count > 0 ? requested : @[ASAuthorizationScopeFullName, ASAuthorizationScopeEmail];
            if (nonce && nonce.length > 0) {
                request.nonce = nonce;
            }

            CN1AppleSignInDelegate *del = [[CN1AppleSignInDelegate alloc] init];
            __weak CN1AppleSignInDelegate *weakDel = del;
            del.completion = ^{
                __strong CN1AppleSignInDelegate *strongDel = weakDel;
                if (strongDel) {
                    // hop signal to whichever thread waited
                }
                dispatch_semaphore_signal(sem);
            };

            ASAuthorizationController *controller =
                [[ASAuthorizationController alloc] initWithAuthorizationRequests:@[request]];
            controller.delegate = del;
            controller.presentationContextProvider = del;

            self->_currentDelegate = del;
            self->_currentController = controller;
            [controller performRequests];
        });

        // 1 hour upper bound; the user finishes the sheet in seconds normally.
        dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3600 * NSEC_PER_SEC)));

        CN1AppleSignInDelegate *del = (CN1AppleSignInDelegate *)_currentDelegate;
        _currentDelegate = nil;
        _currentController = nil;
        if (del == nil) {
            return nil;
        }
        if (del.errorResult != nil) {
            // 1001 = canceled; return nil so AppleSignIn reports onCancel.
            return nil;
        }
        return del.resultString;
    }
    return nil;
}

@end
