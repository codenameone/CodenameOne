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
 * The operating system's language model for com.codename1.ai.SystemLlmClient:
 * Apple's Foundation Models. The framework is Swift only, so the work is done
 * by CN1SystemLanguageModel, a Swift class IPhoneBuilder (and the native macOS
 * builder) copy into the app target only for an app that uses SystemLlmClient,
 * and that also turn INCLUDE_CN1_SYSTEM_LLM on. Every other app compiles the
 * stubs and references no Swift.
 *
 * The class is found with NSClassFromString rather than through the generated
 * -Swift.h header: that header exists only in a target that has Swift and only
 * under the module name of the app, and this file is shared by every target.
 * The protocol below declares the selectors for the compiler alone; the Swift
 * class names each one with an explicit @objc(...) and need not conform.
 *
 * next and download block, and ParparVM's collector can only stop a thread at
 * a safepoint, so they run between CN1_YIELD_THREAD and CN1_RESUME_THREAD with
 * every Java object read before the yield and every result allocated after it.
 */
#if defined(INCLUDE_CN1_SYSTEM_LLM)
@protocol CN1SystemLanguageModelBridge <NSObject>
- (NSString *)providerName;
- (int)statusFor:(int)feature;
- (NSString *)statusReasonFor:(int)feature;
- (BOOL)downloadFeature:(int)feature;
- (int)startFeature:(int)feature roles:(NSArray *)roles contents:(NSArray *)contents
              image:(NSData *)image option:(int)option maxTokens:(int)maxTokens
        temperature:(float)temperature seed:(int)seed;
- (NSString *)nextFor:(int)handle;
- (NSString *)lastErrorFor:(int)handle;
- (void)closeHandle:(int)handle;
@end

static id<CN1SystemLanguageModelBridge> cn1SystemLlmBridge(void) {
    static id<CN1SystemLanguageModelBridge> bridge = nil;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        Class c = NSClassFromString(@"CN1SystemLanguageModel");
        if (c != nil) {
            bridge = (id<CN1SystemLanguageModelBridge>)[[c alloc] init];
        }
    });
    return bridge;
}

static JAVA_OBJECT cn1SystemLlmString(CODENAME_ONE_THREAD_STATE, NSString *s) {
    if (s == nil) {
        return JAVA_NULL;
    }
    return fromNSString(CN1_THREAD_STATE_PASS_ARG s);
}

static NSArray *cn1SystemLlmStrings(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT arr, NSString *def) {
    NSMutableArray *out = [NSMutableArray array];
    if (arr == JAVA_NULL) {
        return out;
    }
    int n = ((JAVA_ARRAY)arr)->length;
    JAVA_ARRAY_OBJECT *data = (JAVA_ARRAY_OBJECT *)CN1_ARRAY_DATA((JAVA_ARRAY)arr);
    for (int i = 0; i < n; i++) {
        NSString *s = data[i] == JAVA_NULL ? def : toNSString(CN1_THREAD_STATE_PASS_ARG data[i]);
        [out addObject:(s == nil ? def : s)];
    }
    return out;
}
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1SystemLlmIsLinked___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if defined(INCLUDE_CN1_SYSTEM_LLM)
    return cn1SystemLlmBridge() != nil ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1SystemLlmProvider___R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if defined(INCLUDE_CN1_SYSTEM_LLM)
    @autoreleasepool {
        return cn1SystemLlmString(CN1_THREAD_STATE_PASS_ARG [cn1SystemLlmBridge() providerName]);
    }
#else
    return JAVA_NULL;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_cn1SystemLlmStatus___int_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT feature) {
#if defined(INCLUDE_CN1_SYSTEM_LLM)
    id<CN1SystemLanguageModelBridge> b = cn1SystemLlmBridge();
    return b == nil ? 0 : [b statusFor:feature];
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1SystemLlmStatusReason___int_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT feature) {
#if defined(INCLUDE_CN1_SYSTEM_LLM)
    @autoreleasepool {
        return cn1SystemLlmString(CN1_THREAD_STATE_PASS_ARG [cn1SystemLlmBridge() statusReasonFor:feature]);
    }
#else
    return JAVA_NULL;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1SystemLlmDownload___int_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT feature) {
#if defined(INCLUDE_CN1_SYSTEM_LLM)
    id<CN1SystemLanguageModelBridge> b = cn1SystemLlmBridge();
    if (b == nil) {
        return JAVA_FALSE;
    }
    BOOL ok;
    CN1_YIELD_THREAD;
    @autoreleasepool {
        ok = [b downloadFeature:feature];
    }
    CN1_RESUME_THREAD;
    return ok ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_cn1SystemLlmStart___int_java_lang_String_1ARRAY_java_lang_String_1ARRAY_byte_1ARRAY_int_int_float_int_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT feature,
        JAVA_OBJECT roles, JAVA_OBJECT contents, JAVA_OBJECT image, JAVA_INT option,
        JAVA_INT maxTokens, JAVA_FLOAT temperature, JAVA_INT seed) {
#if defined(INCLUDE_CN1_SYSTEM_LLM)
    id<CN1SystemLanguageModelBridge> b = cn1SystemLlmBridge();
    if (b == nil) {
        return 0;
    }
    @autoreleasepool {
        NSArray *r = cn1SystemLlmStrings(CN1_THREAD_STATE_PASS_ARG roles, @"user");
        NSArray *c = cn1SystemLlmStrings(CN1_THREAD_STATE_PASS_ARG contents, @"");
        NSData *img = nil;
        if (image != JAVA_NULL) {
            img = [NSData dataWithBytes:CN1_ARRAY_DATA((JAVA_ARRAY)image)
                                 length:(NSUInteger)((JAVA_ARRAY)image)->length];
        }
        // start only schedules the generation; it does not block.
        return [b startFeature:feature roles:r contents:c image:img option:option
                     maxTokens:maxTokens temperature:temperature seed:seed];
    }
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1SystemLlmNext___int_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_SYSTEM_LLM)
    id<CN1SystemLanguageModelBridge> b = cn1SystemLlmBridge();
    if (b == nil) {
        return JAVA_NULL;
    }
    // The worker thread has no autorelease pool of its own; the piece lives
    // in this one until it has been copied into a Java string.
    @autoreleasepool {
        NSString *piece;
        CN1_YIELD_THREAD;
        piece = [b nextFor:handle];
        CN1_RESUME_THREAD;
        return cn1SystemLlmString(CN1_THREAD_STATE_PASS_ARG piece);
    }
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1SystemLlmLastError___int_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_SYSTEM_LLM)
    @autoreleasepool {
        return cn1SystemLlmString(CN1_THREAD_STATE_PASS_ARG [cn1SystemLlmBridge() lastErrorFor:handle]);
    }
#else
    return JAVA_NULL;
#endif
}

void com_codename1_impl_ios_IOSNative_cn1SystemLlmClose___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_SYSTEM_LLM)
    [cn1SystemLlmBridge() closeHandle:handle];
#endif
}
