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

// Objective-C implementation of the NativeFirebaseAnalytics peer. FIRAnalytics
// is invoked through the Objective-C runtime (NSClassFromString /
// performSelector) rather than a compile-time #import so this source compiles
// even in apps that do not include the Firebase pod; there isSupported returns
// NO and every call is a no-op, so FirebaseAnalyticsProvider degrades cleanly.
// The Firebase/Analytics pod is added by IPhoneBuilder when the
// ios.firebaseAnalytics=true build hint is set.

#import "com_codename1_analytics_NativeFirebaseAnalyticsImpl.h"

// The optional FIRAnalytics selectors are invoked dynamically (the Firebase
// SDK headers are intentionally not imported -- see the file header), so clang
// cannot see their declarations. Silence the resulting -Wundeclared-selector.
#pragma clang diagnostic ignored "-Wundeclared-selector"

@implementation com_codename1_analytics_NativeFirebaseAnalyticsImpl

static Class firAnalyticsClass(void) {
    return NSClassFromString(@"FIRAnalytics");
}

- (BOOL)isSupported {
    return firAnalyticsClass() != nil;
}

- (void)logEvent:(NSString*)param0 param1:(NSString*)param1 {
    Class fir = firAnalyticsClass();
    if (fir == nil) {
        return;
    }
    NSDictionary* params = nil;
    if (param1 != nil && param1.length > 0) {
        NSData* data = [param1 dataUsingEncoding:NSUTF8StringEncoding];
        id parsed = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
        if ([parsed isKindOfClass:[NSDictionary class]]) {
            params = (NSDictionary*) parsed;
        }
    }
    SEL sel = @selector(logEventWithName:parameters:);
    if ([fir respondsToSelector:sel]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
        [fir performSelector:sel withObject:param0 withObject:params];
#pragma clang diagnostic pop
    }
}

- (void)logScreen:(NSString*)param0 {
    Class fir = firAnalyticsClass();
    if (fir == nil) {
        return;
    }
    NSDictionary* params = param0 != nil ? @{ @"screen_name": param0 } : @{};
    SEL sel = @selector(logEventWithName:parameters:);
    if ([fir respondsToSelector:sel]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
        [fir performSelector:sel withObject:@"screen_view" withObject:params];
#pragma clang diagnostic pop
    }
}

- (void)setUserId:(NSString*)param0 {
    Class fir = firAnalyticsClass();
    if (fir == nil) {
        return;
    }
    SEL sel = @selector(setUserID:);
    if ([fir respondsToSelector:sel]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
        [fir performSelector:sel withObject:param0];
#pragma clang diagnostic pop
    }
}

- (void)setUserProperty:(NSString*)param0 param1:(NSString*)param1 {
    Class fir = firAnalyticsClass();
    if (fir == nil) {
        return;
    }
    // FIRAnalytics signature is setUserPropertyString:(value) forName:(name);
    // param0 is the key (name), param1 is the value.
    SEL sel = @selector(setUserPropertyString:forName:);
    if ([fir respondsToSelector:sel]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
        [fir performSelector:sel withObject:param1 withObject:param0];
#pragma clang diagnostic pop
    }
}

@end
