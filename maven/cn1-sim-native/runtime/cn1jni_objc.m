/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

/*
 * Objective-C half of the cn1jni runtime: the NSString bridge the port
 * native sources use for all string traffic. Compile with -fno-objc-arc to
 * match the port sources (manual retain/release).
 */
#import <Foundation/Foundation.h>
#include "cn1jni_runtime.h"
#include <stdlib.h>

/*
 * Both functions deliberately IGNORE their threadStateData parameter: some
 * port sources declare them with a legacy extern that omits it, so the
 * argument registers are shifted at those call sites. The real thread state
 * is always resolved via getThreadLocalData().
 */

JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString *str) {
    (void) threadStateData;
    if (str == nil) {
        return JAVA_NULL;
    }
    struct ThreadLocalData *ts = getThreadLocalData();
    JNIEnv *env = cn1jni_env(ts);
    NSUInteger len = [str length];
    jchar *chars = (jchar *) malloc(sizeof(jchar) * (len > 0 ? len : 1));
    [str getCharacters:(unichar *) chars range:NSMakeRange(0, len)];
    jstring js = (*env)->NewString(env, chars, (jsize) len);
    free(chars);
    JAVA_OBJECT wrapped = cn1jni_wrap_string(ts, js);
    if (wrapped != JAVA_NULL) {
        /* cache the NSString so a toNSString round trip is free */
        ((cn1jni_object *) wrapped)->nsCache = (void *) [str retain];
    }
    return wrapped;
}

NSString *toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str) {
    (void) threadStateData;
    if (str == JAVA_NULL) {
        return nil;
    }
    cn1jni_object *w = (cn1jni_object *) str;
    if (w->nsCache != NULL) {
        return (NSString *) w->nsCache;
    }
    struct ThreadLocalData *ts = getThreadLocalData();
    JNIEnv *env = cn1jni_env(ts);
    jstring js = (jstring) w->jref;
    jsize len = (*env)->GetStringLength(env, js);
    const jchar *chars = (*env)->GetStringChars(env, js, NULL);
    if (chars == NULL) {
        return nil;
    }
    NSString *result = [NSString stringWithCharacters:(const unichar *) chars length:(NSUInteger) len];
    (*env)->ReleaseStringChars(env, js, chars);
    /* cache (retained); released when the wrapper is freed */
    w->nsCache = (void *) [result retain];
    return result;
}

/* called by the arena when a wrapper is freed */
void cn1jni_release_nscache(cn1jni_object *w) {
    if (w->nsCache != NULL) {
        [(NSString *) w->nsCache release];
        w->nsCache = NULL;
    }
}
