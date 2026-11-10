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
/*
 * JNI bridge for com.codename1.impl.android.ai.AndroidLocalLlmImpl over
 * cn1_llama.h. Text arrives as NUL-terminated UTF-8 byte arrays and leaves as
 * UTF-8 byte arrays: JNI's own string functions use modified UTF-8, which
 * cannot carry the four-byte sequences a model emits for emoji.
 */
#include <jni.h>
#include <string>
#include <vector>

#include "cn1_llama.h"

namespace {

std::string bytes(JNIEnv *env, jbyteArray a) {
    if (!a) {
        return std::string();
    }
    jsize n = env->GetArrayLength(a);
    std::string out((size_t) n, '\0');
    env->GetByteArrayRegion(a, 0, n, reinterpret_cast<jbyte *>(&out[0]));
    // Drop the terminator the Java side appends.
    size_t z = out.find('\0');
    if (z != std::string::npos) {
        out.resize(z);
    }
    return out;
}

jbyteArray array(JNIEnv *env, const char *s) {
    if (!s) {
        return nullptr;
    }
    jsize n = (jsize) std::char_traits<char>::length(s);
    jbyteArray a = env->NewByteArray(n);
    if (a) {
        env->SetByteArrayRegion(a, 0, n, reinterpret_cast<const jbyte *>(s));
    }
    return a;
}

}  // namespace

#define CN1_JNI(ret, name) extern "C" JNIEXPORT ret JNICALL \
    Java_com_codename1_impl_android_ai_AndroidLocalLlmImpl_##name

CN1_JNI(jint, nOpen)(JNIEnv *env, jclass, jbyteArray path, jint ctx, jint gpu, jint threads) {
    return cn1llama_open(bytes(env, path).c_str(), ctx, gpu, threads);
}

CN1_JNI(jbyteArray, nLastError)(JNIEnv *env, jclass, jint h) {
    return array(env, cn1llama_last_error(h));
}

CN1_JNI(jboolean, nStart)(JNIEnv *env, jclass, jint h, jobjectArray roles,
                         jobjectArray contents, jint maxTokens, jfloat temperature,
                         jfloat topP, jint seed, jbyteArray grammar) {
    jsize n = env->GetArrayLength(roles);
    std::vector<std::string> r((size_t) n), c((size_t) n);
    std::vector<const char *> rp((size_t) n), cp((size_t) n);
    for (jsize i = 0; i < n; i++) {
        jbyteArray ra = (jbyteArray) env->GetObjectArrayElement(roles, i);
        jbyteArray ca = (jbyteArray) env->GetObjectArrayElement(contents, i);
        r[(size_t) i] = bytes(env, ra);
        c[(size_t) i] = bytes(env, ca);
        env->DeleteLocalRef(ra);
        env->DeleteLocalRef(ca);
        rp[(size_t) i] = r[(size_t) i].c_str();
        cp[(size_t) i] = c[(size_t) i].c_str();
    }
    std::string g = bytes(env, grammar);
    return cn1llama_start(h, n, rp.data(), cp.data(), maxTokens, temperature, topP, seed,
                          grammar ? g.c_str() : nullptr) ? JNI_TRUE : JNI_FALSE;
}

CN1_JNI(jbyteArray, nNext)(JNIEnv *env, jclass, jint h) {
    const char *piece = nullptr;
    return cn1llama_next(h, &piece) ? array(env, piece) : nullptr;
}

CN1_JNI(jint, nPromptTokens)(JNIEnv *, jclass, jint h) {
    return cn1llama_prompt_tokens(h);
}

CN1_JNI(jint, nCompletionTokens)(JNIEnv *, jclass, jint h) {
    return cn1llama_completion_tokens(h);
}

CN1_JNI(jfloatArray, nEmbed)(JNIEnv *env, jclass, jint h, jbyteArray text) {
    const float *v = nullptr;
    int dim = cn1llama_embed(h, bytes(env, text).c_str(), &v);
    if (dim <= 0) {
        return nullptr;
    }
    jfloatArray a = env->NewFloatArray(dim);
    if (a) {
        env->SetFloatArrayRegion(a, 0, dim, v);
    }
    return a;
}

CN1_JNI(void, nClose)(JNIEnv *, jclass, jint h) {
    cn1llama_close(h);
}
