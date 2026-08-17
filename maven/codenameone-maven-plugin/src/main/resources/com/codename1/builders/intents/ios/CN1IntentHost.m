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
#import "CN1IntentHost.h"
#include "cn1_globals.h"
#include "com_codename1_impl_ios_IOSIntentCallbacks.h"

// Pending continuations keyed by token. An intent can be invoked while another is still
// running, so this is a map rather than the single global the background-fetch path uses --
// two concurrent invocations sharing one slot would answer each other's caller.
static NSMutableDictionary *cn1PendingIntents = nil;
static NSObject *cn1PendingLock = nil;
static long long cn1IntentTokenCounter = 0;

static void cn1IntentsEnsureState(void) {
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        cn1PendingIntents = [[NSMutableDictionary alloc] init];
        cn1PendingLock = [[NSObject alloc] init];
    });
}

@implementation CN1IntentHost

+ (void)performIntent:(NSString *)intentId
           paramsJson:(NSString *)paramsJson
             headless:(BOOL)headless
           completion:(void (^)(NSString *, NSString *))completion {
    cn1IntentsEnsureState();
    NSString *token;
    @synchronized (cn1PendingLock) {
        token = [NSString stringWithFormat:@"cn1i-%lld", ++cn1IntentTokenCounter];
        if (completion != nil) {
            [cn1PendingIntents setObject:[completion copy] forKey:token];
        }
    }

    JAVA_OBJECT jtoken = fromNSString(CN1_THREAD_GET_STATE_PASS_ARG token);
    JAVA_OBJECT jid = intentId == nil ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG intentId);
    JAVA_OBJECT jparams = paramsJson == nil ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG paramsJson);

    // Returns immediately. The framework runs the handler off the event dispatch thread and
    // answers through completeToken:resultJson: when it finishes or its deadline passes.
    com_codename1_impl_ios_IOSIntentCallbacks_nativePerformIntent___java_lang_String_java_lang_String_java_lang_String_boolean(
            CN1_THREAD_GET_STATE_PASS_ARG jtoken, jid, jparams, headless ? JAVA_TRUE : JAVA_FALSE);
}

+ (void)completeToken:(NSString *)token
           resultJson:(NSString *)resultJson
            imagesDir:(NSString *)imagesDir {
    if (token == nil) {
        return;
    }
    cn1IntentsEnsureState();
    void (^completion)(NSString *, NSString *) = nil;
    @synchronized (cn1PendingLock) {
        completion = [cn1PendingIntents objectForKey:token];
        // Removed before firing, so a late handler result racing the deadline cannot resume
        // the same continuation twice.
        [cn1PendingIntents removeObjectForKey:token];
    }
    if (completion != nil) {
        completion(resultJson == nil ? @"{}" : resultJson, imagesDir);
    }
}

+ (NSString *)queryEntities:(NSString *)entityType
                       kind:(NSString *)kind
                   argument:(NSString *)argument {
    JAVA_OBJECT jtype = entityType == nil ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG entityType);
    JAVA_OBJECT jkind = kind == nil ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG kind);
    JAVA_OBJECT jarg = argument == nil ? JAVA_NULL
            : fromNSString(CN1_THREAD_GET_STATE_PASS_ARG argument);
#ifdef NEW_CODENAME_ONE_VM
    JAVA_OBJECT result = com_codename1_impl_ios_IOSIntentCallbacks_nativeQueryEntities___java_lang_String_java_lang_String_java_lang_String_R_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG jtype, jkind, jarg);
#else
    JAVA_OBJECT result = com_codename1_impl_ios_IOSIntentCallbacks_nativeQueryEntities___java_lang_String_java_lang_String_java_lang_String(
            CN1_THREAD_GET_STATE_PASS_ARG jtype, jkind, jarg);
#endif
    if (result == JAVA_NULL) {
        return nil;
    }
    return toNSString(CN1_THREAD_GET_STATE_PASS_ARG result);
}

@end
