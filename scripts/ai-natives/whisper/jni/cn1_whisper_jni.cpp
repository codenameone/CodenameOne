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
 * JNI bridge for com.codename1.ai.whisper.NativeWhisperRecognizerImpl (the
 * cn1-ai-whisper cn1lib's Android half) over cn1_whisper.h.
 *
 * Results are built with new String(byte[], "UTF-8"), never NewStringUTF:
 * JNI's string functions speak modified UTF-8, and a transcript holding a
 * four-byte sequence (an emoji) is invalid input to them -- CheckJNI aborts.
 */
#include <jni.h>
#include <cstring>
#include <string>

#include "cn1_whisper.h"

namespace {

std::string utf(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return std::string();
    }
    // Paths are what arrive here; GetStringUTFChars is exact for every
    // character outside the supplementary planes, which no path uses.
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string out = chars == nullptr ? std::string() : std::string(chars);
    if (chars != nullptr) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return out;
}

jstring toJava(JNIEnv* env, const char* text) {
    const char* value = text == nullptr ? "" : text;
    jsize n = static_cast<jsize>(std::strlen(value));
    jbyteArray bytes = env->NewByteArray(n);
    if (bytes == nullptr) {
        return nullptr;
    }
    env->SetByteArrayRegion(bytes, 0, n, reinterpret_cast<const jbyte*>(value));
    jclass stringClass = env->FindClass("java/lang/String");
    jmethodID ctor = env->GetMethodID(stringClass, "<init>", "([BLjava/lang/String;)V");
    jstring charset = env->NewStringUTF("UTF-8");
    jstring out = static_cast<jstring>(env->NewObject(stringClass, ctor, bytes, charset));
    env->DeleteLocalRef(charset);
    env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(stringClass);
    return out;
}

jstring run(JNIEnv* env, jstring modelPath, jstring audioPath, int segments) {
    std::string model = utf(env, modelPath);
    std::string audio = utf(env, audioPath);
    char* text = cn1whisper_transcribe(model.c_str(), audio.c_str(), segments);
    jstring out = toJava(env, text);
    cn1whisper_free(text);
    return out;
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_codename1_ai_whisper_NativeWhisperRecognizerImpl_nativeTranscribe(
        JNIEnv* env, jobject, jstring modelPath, jstring audioPath) {
    return run(env, modelPath, audioPath, 0);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_codename1_ai_whisper_NativeWhisperRecognizerImpl_nativeTranscribeSegments(
        JNIEnv* env, jobject, jstring modelPath, jstring audioPath) {
    return run(env, modelPath, audioPath, 1);
}
