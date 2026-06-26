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
 * Portable extras of the cn1jni runtime used by the Windows port sources
 * (and harmless on macOS): C-string -> Java String, translated-code array
 * allocation, exception instance creation and the fault-handler hooks.
 */
#include "cn1jni_runtime.h"

#include <stdio.h>
#include <string.h>

/*
 * Java String from a UTF-8 C string - the Windows sources' equivalent of
 * fromNSString.
 */
JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char *str) {
    if (str == NULL) {
        return JAVA_NULL;
    }
    JNIEnv *env = cn1jni_env(threadStateData);
    jstring s = (*env)->NewStringUTF(env, str);
    return cn1jni_wrap_string(threadStateData, s);
}

/*
 * Array class identity tokens: translated code passes &class_array1__JAVA_X
 * to allocArray; the compat runtime only needs the IDENTITY to pick the
 * element type, so zeroed globals suffice.
 */
struct clazz class_array1__JAVA_BYTE;
struct clazz class_array1__JAVA_CHAR;
struct clazz class_array1__JAVA_SHORT;
struct clazz class_array1__JAVA_INT;
struct clazz class_array1__JAVA_LONG;
struct clazz class_array1__JAVA_FLOAT;
struct clazz class_array1__JAVA_DOUBLE;
struct clazz class_array1__JAVA_BOOLEAN;

JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int size, struct clazz *type, int primitiveSize, int dim) {
    if (type == &class_array1__JAVA_BYTE) {
        return __NEW_ARRAY_JAVA_BYTE(threadStateData, size);
    }
    if (type == &class_array1__JAVA_CHAR) {
        return __NEW_ARRAY_JAVA_CHAR(threadStateData, size);
    }
    if (type == &class_array1__JAVA_SHORT) {
        return __NEW_ARRAY_JAVA_SHORT(threadStateData, size);
    }
    if (type == &class_array1__JAVA_INT) {
        return __NEW_ARRAY_JAVA_INT(threadStateData, size);
    }
    if (type == &class_array1__JAVA_LONG) {
        return __NEW_ARRAY_JAVA_LONG(threadStateData, size);
    }
    if (type == &class_array1__JAVA_FLOAT) {
        return __NEW_ARRAY_JAVA_FLOAT(threadStateData, size);
    }
    if (type == &class_array1__JAVA_DOUBLE) {
        return __NEW_ARRAY_JAVA_DOUBLE(threadStateData, size);
    }
    if (type == &class_array1__JAVA_BOOLEAN) {
        return __NEW_ARRAY_JAVA_BOOLEAN(threadStateData, size);
    }
#ifdef _WIN32
    {
        extern struct clazz class_array1__java_lang_String;
        if (type == &class_array1__java_lang_String) {
            JNIEnv *env = cn1jni_env(threadStateData);
            jclass stringCls = (*env)->FindClass(env, "java/lang/String");
            jobjectArray arr = (*env)->NewObjectArray(env, size, stringCls, NULL);
            (*env)->DeleteLocalRef(env, stringCls);
            return cn1jni_wrap_array_object(threadStateData, arr);
        }
    }
#endif
    fprintf(stderr, "cn1sim: allocArray with unsupported array class\n");
    return JAVA_NULL;
}

static JAVA_OBJECT cn1jni_new_throwable(struct ThreadLocalData *ts, const char *clsName) {
    JNIEnv *env = cn1jni_env(ts);
    jclass cls = (*env)->FindClass(env, clsName);
    if (cls == NULL) {
        (*env)->ExceptionClear(env);
        return JAVA_NULL;
    }
    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "()V");
    jobject o = (*env)->NewObject(env, cls, ctor);
    (*env)->DeleteLocalRef(env, cls);
    return cn1jni_wrap_object(ts, o);
}

JAVA_OBJECT __NEW_INSTANCE_java_lang_NullPointerException(CODENAME_ONE_THREAD_STATE) {
    return cn1jni_new_throwable(threadStateData, "java/lang/NullPointerException");
}

JAVA_OBJECT __NEW_INSTANCE_java_lang_StackOverflowError(CODENAME_ONE_THREAD_STATE) {
    return cn1jni_new_throwable(threadStateData, "java/lang/StackOverflowError");
}

/* the JVM fills stack traces at construction; nothing to do */
void java_lang_Throwable_fillInStack__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT throwable) {
    (void) threadStateData;
    (void) throwable;
}

/* fault-handler-only hooks - no-ops under the compat runtime */
__attribute__((weak)) void monitorExitBlock(struct ThreadLocalData *threadStateData, JAVA_OBJECT monitor) {
    (void) threadStateData;
    (void) monitor;
}

__attribute__((weak)) JAVA_BOOLEAN instanceofFunction(int sourceClass, int destId) {
    (void) sourceClass;
    (void) destId;
    return JAVA_FALSE;
}

#ifndef __APPLE__
/* no NSString cache outside macOS */
void cn1jni_release_nscache(cn1jni_object *w) {
    (void) w;
}
#endif
