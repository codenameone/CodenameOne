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

// Native implementation of IOSNative.oidcSystemBrowserSupported() and
// IOSNative.oidcStartAuthorization(String, String). Implements the
// com.codename1.io.oidc.SystemBrowser primitive (drives sign-in through the
// hardened OS sign-in sheet via ASWebAuthenticationSession, iOS 12+).

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

// Presentation-context provider that hands the OS sheet a window. iOS 13+
// requires a non-nil provider before -[ASWebAuthenticationSession start]
// succeeds.
API_AVAILABLE(ios(12.0))
@interface CN1OidcAuthContext : NSObject <ASWebAuthenticationPresentationContextProviding>
@end

@implementation CN1OidcAuthContext

- (ASPresentationAnchor)presentationAnchorForWebAuthenticationSession:(ASWebAuthenticationSession *)session
        API_AVAILABLE(ios(13.0)) {
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
        if (!anchor) {
            for (UIWindow *w in [UIApplication sharedApplication].windows) {
                if (w) { anchor = w; break; }
            }
        }
#pragma clang diagnostic pop
    }
    return anchor;
}

@end

// Single static slot to keep the session strongly referenced for the
// duration of a flow (ARC would otherwise deallocate it the moment the
// dispatch_async block returned).
static id g_cn1OidcCurrentSession = nil;
static id g_cn1OidcCurrentContext = nil;

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_oidcSystemBrowserSupported__(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me) {
    if (@available(iOS 12.0, *)) {
        return JAVA_TRUE;
    }
    return JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_oidcStartAuthorization___java_lang_String_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT me, JAVA_OBJECT authUrlObj, JAVA_OBJECT redirectSchemeObj) {
    if (@available(iOS 12.0, *)) {
        // fall through
    } else {
        return JAVA_NULL;
    }
    NSString *authUrl = toNSString(CN1_THREAD_STATE_PASS_ARG authUrlObj);
    NSString *redirectScheme = toNSString(CN1_THREAD_STATE_PASS_ARG redirectSchemeObj);
    if (authUrl == nil || redirectScheme == nil) {
        return JAVA_NULL;
    }
    NSURL *url = [NSURL URLWithString:authUrl];
    if (url == nil) {
        return JAVA_NULL;
    }

    __block NSString *result = nil;
    __block NSError  *failure = nil;
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);

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
                g_cn1OidcCurrentSession = nil;
                g_cn1OidcCurrentContext = nil;
                dispatch_semaphore_signal(sem);
            }];

        if (@available(iOS 13.0, *)) {
            CN1OidcAuthContext *ctx = [[CN1OidcAuthContext alloc] init];
            g_cn1OidcCurrentContext = ctx;
            session.presentationContextProvider = ctx;
            session.prefersEphemeralWebBrowserSession = NO;
        }
        g_cn1OidcCurrentSession = session;
        if (![session start]) {
            failure = [NSError errorWithDomain:@"com.codename1.io.oidc"
                                          code:-1
                                      userInfo:@{NSLocalizedDescriptionKey: @"ASWebAuthenticationSession refused to start"}];
            g_cn1OidcCurrentSession = nil;
            g_cn1OidcCurrentContext = nil;
            dispatch_semaphore_signal(sem);
        }
    });

    // 1-hour upper bound; users finish in seconds, the cap unwinds if the
    // sheet is ever held open indefinitely.
    dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3600 * NSEC_PER_SEC)));

    if (failure != nil || result == nil) {
        return JAVA_NULL;
    }
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG result);
}
