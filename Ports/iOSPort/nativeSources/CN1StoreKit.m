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
#import "xmlvm.h"
#import "java_lang_String.h"

/*
 * StoreKit 2 for com.codename1.payment.Purchase, over CN1StoreKit2, the Swift
 * class IPhoneBuilder (and the native macOS builder) copies into the app target
 * of an app that uses the purchase API. StoreKit 2 has no Objective-C API.
 *
 * Exactly one StoreKit runs in a process. When the Swift class is linked and
 * the OS has StoreKit 2 (iOS/tvOS 15, macOS 12), it is the only one: the
 * StoreKit 1 payment queue never gets an observer, because Apple does not
 * support observing one API while buying through the other. Below those
 * versions the StoreKit 1 code in CodenameOne_GLViewController.m and
 * IOSNative.m runs exactly as before.
 *
 * The bridge never calls Java. ZoozPurchase drains its events on a thread of
 * its own through storeKit2NextEvent, and acknowledges each transaction with
 * storeKit2Finish once the receipt is stored, which is when StoreKit is told
 * to finish it. The blocking calls run between CN1_YIELD_THREAD and
 * CN1_RESUME_THREAD with every Java object read before the yield and every
 * result allocated after it, as CN1SystemLlm.m does.
 */
#if defined(CN1_USE_STOREKIT) && !TARGET_OS_WATCH
id<CN1StoreKit2Bridge> CN1StoreKit2BridgeInstance(void) {
    static id<CN1StoreKit2Bridge> bridge = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        Class c = NSClassFromString(@"CN1StoreKit2");
        if (c != nil) {
            id<CN1StoreKit2Bridge> b = (id<CN1StoreKit2Bridge>)[[c alloc] init];
            if ([b isSupported]) {
                bridge = b;
            } else {
                [b release];
            }
        }
    });
    return bridge;
}
#endif

/// Starts observing transactions; called once at launch, which is when StoreKit
/// wants an observer: a transaction left unfinished by the last run, approved
/// by a parent (Ask to Buy), renewed or refunded while the app was closed is
/// delivered to whatever is listening then.
void CN1StoreKitInstallObserver(void) {
#if defined(CN1_USE_STOREKIT) && !TARGET_OS_WATCH
    id<CN1StoreKit2Bridge> b = CN1StoreKit2BridgeInstance();
    if (b != nil) {
        [b start];
        return;
    }
    [[SKPaymentQueue defaultQueue] addTransactionObserver:[CodenameOne_GLViewController instance]];
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_storeKit2Active___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if defined(CN1_USE_STOREKIT) && !TARGET_OS_WATCH
    return CN1StoreKit2BridgeInstance() != nil ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_storeKit2NextEvent___int_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT timeoutMs) {
#if defined(CN1_USE_STOREKIT) && !TARGET_OS_WATCH
    id<CN1StoreKit2Bridge> b = CN1StoreKit2BridgeInstance();
    if (b == nil) {
        return JAVA_NULL;
    }
    NSString *event = nil;
    CN1_YIELD_THREAD;
    @autoreleasepool {
        event = [[b nextEvent:timeoutMs] retain];
    }
    CN1_RESUME_THREAD;
    if (event == nil) {
        return JAVA_NULL;
    }
    JAVA_OBJECT out = fromNSString(CN1_THREAD_STATE_PASS_ARG event);
    [event release];
    return out;
#else
    return JAVA_NULL;
#endif
}

void com_codename1_impl_ios_IOSNative_storeKit2Finish___java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT transactionId) {
#if defined(CN1_USE_STOREKIT) && !TARGET_OS_WATCH
    id<CN1StoreKit2Bridge> b = CN1StoreKit2BridgeInstance();
    if (b == nil || transactionId == JAVA_NULL) {
        return;
    }
    @autoreleasepool {
        [b finish:toNSString(CN1_THREAD_STATE_PASS_ARG transactionId)];
    }
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_storeKit2FetchProducts___java_lang_String_1ARRAY_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT skus) {
#if defined(CN1_USE_STOREKIT) && !TARGET_OS_WATCH
    id<CN1StoreKit2Bridge> b = CN1StoreKit2BridgeInstance();
    if (b == nil || skus == JAVA_NULL) {
        return JAVA_NULL;
    }
    NSMutableArray *ids = [[NSMutableArray alloc] init];
    int count = ((JAVA_ARRAY)skus)->length;
    JAVA_ARRAY_OBJECT *data = (JAVA_ARRAY_OBJECT *)CN1_ARRAY_DATA((JAVA_ARRAY)skus);
    for (int i = 0; i < count; i++) {
        if (data[i] != JAVA_NULL) {
            [ids addObject:toNSString(CN1_THREAD_STATE_PASS_ARG data[i])];
        }
    }
    NSString *answer = nil;
    CN1_YIELD_THREAD;
    @autoreleasepool {
        answer = [[b fetchProducts:ids timeoutMs:30000] retain];
    }
    CN1_RESUME_THREAD;
    [ids release];
    if (answer == nil) {
        return JAVA_NULL;
    }
    JAVA_OBJECT out = fromNSString(CN1_THREAD_STATE_PASS_ARG answer);
    [answer release];
    return out;
#else
    return JAVA_NULL;
#endif
}
