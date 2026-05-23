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

#import "com_codename1_io_oidc_OidcBrowserNativeImpl.h"
#import <UIKit/UIKit.h>
#import <AuthenticationServices/AuthenticationServices.h>

/// ASWebAuthenticationPresentationContextProviding wrapper that vends the
/// current keyWindow back to the system. iOS 13+ requires a non-nil context
/// provider before -[ASWebAuthenticationSession start] will succeed. We hold
/// a strong reference on the session itself for the duration of the flow.
API_AVAILABLE(ios(12.0))
@interface CN1OidcAuthContext : NSObject <ASWebAuthenticationPresentationContextProviding>
@end

@implementation CN1OidcAuthContext

- (ASPresentationAnchor)presentationAnchorForWebAuthenticationSession:(ASWebAuthenticationSession *)session API_AVAILABLE(ios(13.0)) {
    UIWindow *anchor = nil;
    if (@available(iOS 13.0, *)) {
        for (UIScene *scene in [UIApplication sharedApplication].connectedScenes) {
            if (scene.activationState == UISceneActivationStateForegroundActive &&
                [scene isKindOfClass:[UIWindowScene class]]) {
                UIWindowScene *ws = (UIWindowScene *)scene;
                for (UIWindow *w in ws.windows) {
                    if (w.isKeyWindow) {
                        anchor = w;
                        break;
                    }
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
        // Fallback path; on iOS 13+ UIApplication.keyWindow is deprecated but
        // still works, on iOS 12 it is the only option. The deprecation is
        // expected -- silence the warning on this single call.
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
        anchor = [UIApplication sharedApplication].keyWindow;
        if (!anchor) {
            // Last-ditch: any window that exists.
            for (UIWindow *w in [UIApplication sharedApplication].windows) {
                if (w) { anchor = w; break; }
            }
        }
#pragma clang diagnostic pop
    }
    return anchor;
}

@end

@implementation com_codename1_io_oidc_OidcBrowserNativeImpl {
    // Strongly-held during a flow so ARC does not deallocate the session
    // while the user is signing in.
    id _currentSession;
    CN1OidcAuthContext *_currentContext;
}

- (BOOL)isSupported {
    if (@available(iOS 12.0, *)) {
        return YES;
    }
    return NO;
}

- (NSString *)startAuthorization:(NSString *)param param1:(NSString *)param1 {
    NSString *authUrl = param;
    NSString *redirectScheme = param1;
    if (authUrl == nil || redirectScheme == nil) {
        return nil;
    }
    if (@available(iOS 12.0, *)) {
        // Block the calling (background) thread until the OS sheet completes.
        // ASWebAuthenticationSession must be created and started on the main
        // thread; we dispatch_async over and wait on a semaphore.
        dispatch_semaphore_t sem = dispatch_semaphore_create(0);
        __block NSString *result = nil;
        __block NSError *failure = nil;

        NSURL *url = [NSURL URLWithString:authUrl];
        if (url == nil) {
            return nil;
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            ASWebAuthenticationSession *session =
                [[ASWebAuthenticationSession alloc] initWithURL:url
                                              callbackURLScheme:redirectScheme
                                              completionHandler:^(NSURL * _Nullable callbackURL, NSError * _Nullable error) {
                    if (callbackURL) {
                        result = [callbackURL absoluteString];
                    } else if (error) {
                        failure = error;
                    }
                    self->_currentSession = nil;
                    self->_currentContext = nil;
                    dispatch_semaphore_signal(sem);
                }];

            // iOS 13+ requires a presentation context provider; on iOS 12 the
            // property exists but is optional.
            if (@available(iOS 13.0, *)) {
                CN1OidcAuthContext *ctx = [[CN1OidcAuthContext alloc] init];
                self->_currentContext = ctx;
                session.presentationContextProvider = ctx;
                // Force a fresh sign-in UI -- avoids surprising cookie reuse from
                // a prior unrelated provider in the same scheme.
                session.prefersEphemeralWebBrowserSession = NO;
            }
            self->_currentSession = session;
            BOOL started = [session start];
            if (!started) {
                failure = [NSError errorWithDomain:@"com.codename1.io.oidc"
                                              code:-1
                                          userInfo:@{NSLocalizedDescriptionKey: @"ASWebAuthenticationSession refused to start"}];
                self->_currentSession = nil;
                self->_currentContext = nil;
                dispatch_semaphore_signal(sem);
            }
        });

        // Cap the wait at one hour. A real-world user finishes in seconds; the
        // cap exists so a stuck flow eventually unwinds instead of holding the
        // thread forever.
        dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3600 * NSEC_PER_SEC)));

        if (failure) {
            // User cancellation comes back as ASWebAuthenticationSessionErrorCodeCanceledLogin
            // (code 1) on iOS 12+. Return nil so the Java side reports USER_CANCELLED.
            return nil;
        }
        return result;
    }
    return nil;
}

@end
