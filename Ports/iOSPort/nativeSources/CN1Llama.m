/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
 * llama.cpp for com.codename1.ai.LocalLlmClient. INCLUDE_CN1_LLAMA is enabled
 * by IPhoneBuilder only when it links the cn1-llama-ios xcframework, which it
 * does only for an app that calls LlmClient.local. Every other app compiles
 * the stubs at the bottom and references no llama symbol.
 *
 * The xcframework exports the cn1llama_* API of
 * scripts/ai-natives/llama/src/cn1_llama.h and nothing else; the prototypes
 * below are that header's, so this file never sees a llama.cpp struct.
 *
 * Model loading, prompt evaluation and every generated token block for a long
 * time, and ParparVM's collector can only stop a thread at a safepoint. Each
 * blocking call is therefore made between CN1_YIELD_THREAD and
 * CN1_RESUME_THREAD, with every Java object read before the yield and every
 * result allocated after the resume -- no Java object is touched in between.
 */
#if defined(INCLUDE_CN1_LLAMA)
extern int cn1llama_open(const char *path, int n_ctx, int n_gpu_layers, int n_threads);
extern const char *cn1llama_last_error(int handle);
extern int cn1llama_start(int handle, int n_messages, const char **roles,
                          const char **contents, int max_tokens, float temperature,
                          float top_p, int seed, const char *grammar);
extern int cn1llama_next(int handle, const char **out);
extern int cn1llama_prompt_tokens(int handle);
extern int cn1llama_completion_tokens(int handle);
extern int cn1llama_embed(int handle, const char *text, const float **out);
extern void cn1llama_close(int handle);

static char *cn1LlamaCopy(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s) {
    if (s == JAVA_NULL) {
        return NULL;
    }
    // The worker thread has no autorelease pool of its own.
    @autoreleasepool {
        const char *utf8 = [toNSString(CN1_THREAD_STATE_PASS_ARG s) UTF8String];
        return strdup(utf8 == NULL ? "" : utf8);
    }
}

static JAVA_OBJECT cn1LlamaString(CODENAME_ONE_THREAD_STATE, const char *s) {
    if (s == NULL) {
        return JAVA_NULL;
    }
    @autoreleasepool {
        NSString *str = [[NSString alloc] initWithBytes:s length:strlen(s)
                                               encoding:NSUTF8StringEncoding];
        JAVA_OBJECT out = fromNSString(CN1_THREAD_STATE_PASS_ARG str == nil ? @"" : str);
#if !__has_feature(objc_arc)
        [str release];
#endif
        return out;
    }
}
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1LlamaIsSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if defined(INCLUDE_CN1_LLAMA)
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_cn1LlamaOpen___java_lang_String_int_int_int_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path,
        JAVA_INT contextSize, JAVA_INT gpuLayers, JAVA_INT threads) {
#if defined(INCLUDE_CN1_LLAMA)
    char *p = cn1LlamaCopy(CN1_THREAD_STATE_PASS_ARG path);
    if (p == NULL) {
        return 0;
    }
    CN1_YIELD_THREAD;
    int h = cn1llama_open(p, contextSize, gpuLayers, threads);
    CN1_RESUME_THREAD;
    free(p);
    return h;
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1LlamaLastError___int_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_LLAMA)
    return cn1LlamaString(CN1_THREAD_STATE_PASS_ARG cn1llama_last_error(handle));
#else
    return JAVA_NULL;
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1LlamaStart___int_java_lang_String_1ARRAY_java_lang_String_1ARRAY_int_float_float_int_java_lang_String_R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle,
        JAVA_OBJECT roles, JAVA_OBJECT contents, JAVA_INT maxTokens,
        JAVA_FLOAT temperature, JAVA_FLOAT topP, JAVA_INT seed, JAVA_OBJECT grammar) {
#if defined(INCLUDE_CN1_LLAMA)
    if (roles == JAVA_NULL || contents == JAVA_NULL) {
        return JAVA_FALSE;
    }
    int n = ((JAVA_ARRAY)roles)->length;
    if (((JAVA_ARRAY)contents)->length < n) {
        return JAVA_FALSE;
    }
    JAVA_ARRAY_OBJECT *r = (JAVA_ARRAY_OBJECT *)CN1_ARRAY_DATA((JAVA_ARRAY)roles);
    JAVA_ARRAY_OBJECT *c = (JAVA_ARRAY_OBJECT *)CN1_ARRAY_DATA((JAVA_ARRAY)contents);
    char **rc = (char **)calloc((size_t)(n > 0 ? n : 1), sizeof(char *));
    char **cc = (char **)calloc((size_t)(n > 0 ? n : 1), sizeof(char *));
    for (int i = 0; i < n; i++) {
        rc[i] = cn1LlamaCopy(CN1_THREAD_STATE_PASS_ARG r[i]);
        cc[i] = cn1LlamaCopy(CN1_THREAD_STATE_PASS_ARG c[i]);
        if (rc[i] == NULL) rc[i] = strdup("user");
        if (cc[i] == NULL) cc[i] = strdup("");
    }
    char *g = cn1LlamaCopy(CN1_THREAD_STATE_PASS_ARG grammar);
    CN1_YIELD_THREAD;
    int ok = cn1llama_start(handle, n, (const char **)rc, (const char **)cc, maxTokens,
                            temperature, topP, seed, g);
    CN1_RESUME_THREAD;
    for (int i = 0; i < n; i++) {
        free(rc[i]);
        free(cc[i]);
    }
    free(rc);
    free(cc);
    free(g);
    return ok ? JAVA_TRUE : JAVA_FALSE;
#else
    return JAVA_FALSE;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1LlamaNext___int_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_LLAMA)
    const char *piece = NULL;
    CN1_YIELD_THREAD;
    int more = cn1llama_next(handle, &piece);
    CN1_RESUME_THREAD;
    return more ? cn1LlamaString(CN1_THREAD_STATE_PASS_ARG piece) : JAVA_NULL;
#else
    return JAVA_NULL;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_cn1LlamaPromptTokens___int_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_LLAMA)
    return cn1llama_prompt_tokens(handle);
#else
    return 0;
#endif
}

JAVA_INT com_codename1_impl_ios_IOSNative_cn1LlamaCompletionTokens___int_R_int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_LLAMA)
    return cn1llama_completion_tokens(handle);
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1LlamaEmbed___int_java_lang_String_R_float_1ARRAY(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle,
        JAVA_OBJECT text) {
#if defined(INCLUDE_CN1_LLAMA)
    char *t = cn1LlamaCopy(CN1_THREAD_STATE_PASS_ARG text);
    if (t == NULL) {
        return JAVA_NULL;
    }
    const float *v = NULL;
    CN1_YIELD_THREAD;
    int dim = cn1llama_embed(handle, t, &v);
    CN1_RESUME_THREAD;
    free(t);
    if (dim <= 0) {
        return JAVA_NULL;
    }
    JAVA_OBJECT out = allocArray(CN1_THREAD_STATE_PASS_ARG dim, &class_array1__JAVA_FLOAT,
                                 sizeof(JAVA_ARRAY_FLOAT), 1);
    memcpy(CN1_ARRAY_DATA((JAVA_ARRAY)out), v, sizeof(JAVA_ARRAY_FLOAT) * (size_t)dim);
    return out;
#else
    return JAVA_NULL;
#endif
}

void com_codename1_impl_ios_IOSNative_cn1LlamaClose___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_LLAMA)
    CN1_YIELD_THREAD;
    cn1llama_close(handle);
    CN1_RESUME_THREAD;
#endif
}
