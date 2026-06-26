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
 * cn1jni: the ParparVM-compatibility runtime for the native simulator
 * backend.
 *
 * The simulator loads the port's handwritten native sources (which follow
 * ParparVM conventions: JAVA_OBJECT wrappers, CODENAME_ONE_THREAD_STATE
 * threading, fromNSString/toNSString string helpers, direct
 * ((JAVA_ARRAY)x)->data array access) into the JVM process via JNI. This
 * runtime implements the subset of the ParparVM runtime API those sources
 * call, backed by JNIEnv instead of the translated-code heap:
 *
 *  - JAVA_OBJECT values are pointers to cn1jni_object wrappers whose head is
 *    binary-compatible with struct JavaArrayPrototype from cn1_globals.h, so
 *    direct ->data / ->length array access in the port sources works.
 *  - Wrappers created during a JNI downcall live in a per-thread arena that
 *    is released when the outermost shim returns: array elements are
 *    copied back / released, JNI local refs are dropped with the native
 *    frame. Native code that retains a JAVA_OBJECT beyond the call must pin
 *    it (cn1jni_pin) which promotes the JNI reference to a global ref and
 *    removes the wrapper from the arena.
 *  - The thread state (struct ThreadLocalData) exists per attached thread;
 *    only the fields the port sources actually touch are meaningful.
 *
 * Compile the port native sources against the real cn1_globals.h (this
 * header includes it) but do NOT compile cn1_globals.m - cn1jni_runtime.c
 * provides the extern functions instead.
 */
#ifndef CN1JNI_RUNTIME_H
#define CN1JNI_RUNTIME_H

#include <jni.h>
#include "cn1_globals.h"

#ifdef __cplusplus
extern "C" {
#endif

/* ---- wrapper object ---------------------------------------------------- */

enum cn1jni_kind {
    CN1JNI_KIND_OBJECT = 1,   /* opaque jobject */
    CN1JNI_KIND_STRING,       /* jstring; nsCache holds a retained NSString */
    CN1JNI_KIND_ARRAY_PRIM,   /* primitive array; proto.data pinned elements */
    CN1JNI_KIND_ARRAY_OBJECT  /* object array; proto.data lazily materialized */
};

typedef struct cn1jni_object {
    /* binary-compatible head: ((JAVA_ARRAY)x)->data and ->length resolve here */
    struct JavaArrayPrototype proto;
    /* cn1jni bookkeeping */
    jobject jref;                  /* local ref (arena) or global ref (pinned) */
    int kind;                      /* enum cn1jni_kind */
    char elemType;                 /* JNI descriptor char for arrays (I, B, ...) */
    int pinned;                    /* promoted to global ref, excluded from arena */
    void *nsCache;                 /* CN1JNI_KIND_STRING: retained NSString* */
    struct cn1jni_object *arenaNext;
} cn1jni_object;

/* ---- shim entry/exit ---------------------------------------------------- */

/*
 * Returns the calling thread's ParparVM-compatible thread state, attaching
 * bookkeeping on first use, and opens an arena scope (re-entrant: nested
 * shim calls share the outermost scope).
 */
struct ThreadLocalData *cn1jni_enter(JNIEnv *env);

/*
 * Closes the arena scope opened by cn1jni_enter. When the outermost scope
 * closes: pinned wrappers survive, all other wrappers release their array
 * elements (JNI_ABORT for read-only copies, copy-back mode 0 for written
 * arrays - the runtime always copies back since it cannot know), drop their
 * NSString caches and are freed. Pending Java exceptions raised through
 * throwException remain pending on the JNIEnv so the JVM sees them when the
 * native frame returns.
 */
void cn1jni_exit(struct ThreadLocalData *ts);

/*
 * The JNIEnv associated with a thread state (the env passed to cn1jni_enter,
 * or the attached env for upcall threads).
 */
JNIEnv *cn1jni_env(struct ThreadLocalData *ts);

/*
 * Thread state for upcalls arriving on non-Java threads (display link,
 * NSURLSession delegate queues, audio callbacks): attaches the thread to the
 * JVM as a daemon if needed and pushes a JNI local frame; pair with
 * cn1jni_upcall_exit.
 */
struct ThreadLocalData *cn1jni_upcall_enter(void);
void cn1jni_upcall_exit(struct ThreadLocalData *ts);

/* ---- argument wrapping (JNI ref -> JAVA_OBJECT) ------------------------- */

JAVA_OBJECT cn1jni_wrap_object(struct ThreadLocalData *ts, jobject o);
JAVA_OBJECT cn1jni_wrap_string(struct ThreadLocalData *ts, jstring s);
JAVA_OBJECT cn1jni_wrap_array_boolean(struct ThreadLocalData *ts, jbooleanArray a);
JAVA_OBJECT cn1jni_wrap_array_byte(struct ThreadLocalData *ts, jbyteArray a);
JAVA_OBJECT cn1jni_wrap_array_char(struct ThreadLocalData *ts, jcharArray a);
JAVA_OBJECT cn1jni_wrap_array_short(struct ThreadLocalData *ts, jshortArray a);
JAVA_OBJECT cn1jni_wrap_array_int(struct ThreadLocalData *ts, jintArray a);
JAVA_OBJECT cn1jni_wrap_array_long(struct ThreadLocalData *ts, jlongArray a);
JAVA_OBJECT cn1jni_wrap_array_float(struct ThreadLocalData *ts, jfloatArray a);
JAVA_OBJECT cn1jni_wrap_array_double(struct ThreadLocalData *ts, jdoubleArray a);
JAVA_OBJECT cn1jni_wrap_array_object(struct ThreadLocalData *ts, jobjectArray a);

/* ---- return unwrapping (JAVA_OBJECT -> JNI ref) ------------------------- */

jobject cn1jni_unwrap_object(struct ThreadLocalData *ts, JAVA_OBJECT o);
jstring cn1jni_unwrap_string(struct ThreadLocalData *ts, JAVA_OBJECT o);
jbooleanArray cn1jni_unwrap_array_boolean(struct ThreadLocalData *ts, JAVA_OBJECT o);
jbyteArray cn1jni_unwrap_array_byte(struct ThreadLocalData *ts, JAVA_OBJECT o);
jcharArray cn1jni_unwrap_array_char(struct ThreadLocalData *ts, JAVA_OBJECT o);
jshortArray cn1jni_unwrap_array_short(struct ThreadLocalData *ts, JAVA_OBJECT o);
jintArray cn1jni_unwrap_array_int(struct ThreadLocalData *ts, JAVA_OBJECT o);
jlongArray cn1jni_unwrap_array_long(struct ThreadLocalData *ts, JAVA_OBJECT o);
jfloatArray cn1jni_unwrap_array_float(struct ThreadLocalData *ts, JAVA_OBJECT o);
jdoubleArray cn1jni_unwrap_array_double(struct ThreadLocalData *ts, JAVA_OBJECT o);
jobjectArray cn1jni_unwrap_array_object(struct ThreadLocalData *ts, JAVA_OBJECT o);

/* ---- lifetime ----------------------------------------------------------- */

/*
 * Pins a wrapper so it survives the arena: the JNI reference becomes a
 * global ref, array elements stay pinned. For the handful of audited sites
 * in the port sources that store JAVA_OBJECTs in statics or retain them in
 * Objective-C objects.
 */
JAVA_OBJECT cn1jni_pin(struct ThreadLocalData *ts, JAVA_OBJECT o);

/*
 * Releases a pinned wrapper: copy-back/release array elements, delete the
 * global ref, free the wrapper.
 */
void cn1jni_unpin(struct ThreadLocalData *ts, JAVA_OBJECT o);

/*
 * Synchronizes a primitive array wrapper's pinned buffer back to the Java
 * array without releasing it. Call before an upcall that passes a
 * native-filled array to Java.
 */
void cn1jni_sync_array(struct ThreadLocalData *ts, JAVA_OBJECT o);

/*
 * Allocates a buffer owned by the current arena scope (freed when the
 * outermost shim returns). Used for UTF-8 conversions and other transient
 * native-visible buffers.
 */
void *cn1jni_arena_alloc(struct ThreadLocalData *ts, size_t size);

/* ---- ParparVM runtime API used by the port native sources --------------- */
/* These are the extern symbols the .m files already call; cn1jni provides
 * them instead of the translated-code runtime. */

struct ThreadLocalData *getThreadLocalData(void);

JAVA_OBJECT __NEW_ARRAY_JAVA_BOOLEAN(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
JAVA_OBJECT __NEW_ARRAY_JAVA_CHAR(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
JAVA_OBJECT __NEW_ARRAY_JAVA_BYTE(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
JAVA_OBJECT __NEW_ARRAY_JAVA_SHORT(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
JAVA_OBJECT __NEW_ARRAY_JAVA_INT(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
JAVA_OBJECT __NEW_ARRAY_JAVA_LONG(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
JAVA_OBJECT __NEW_ARRAY_JAVA_FLOAT(CODENAME_ONE_THREAD_STATE, JAVA_INT size);
JAVA_OBJECT __NEW_ARRAY_JAVA_DOUBLE(CODENAME_ONE_THREAD_STATE, JAVA_INT size);

/* string helpers (UTF-8 buffer is owned by the arena) */
JAVA_OBJECT xmlvm_create_java_string(CODENAME_ONE_THREAD_STATE, const char *str);
const char *stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);

/* exception support: makes the throwable pending on the JNIEnv */
void throwException(struct ThreadLocalData *threadStateData, JAVA_OBJECT exceptionArg);

/*
 * Note: enteringNativeAllocations()/finishedNativeAllocations() are macros in
 * cn1_globals.h expanding to a ThreadLocalData field assignment - they work
 * as-is against the cn1jni thread state and need no functions here.
 */

#ifdef __OBJC__
@class NSString;
/*
 * NSString bridge - implemented in cn1jni_objc.m.
 *
 * IMPORTANT: some port sources declare these with a legacy extern that omits
 * the thread-state parameter (e.g. "extern JAVA_OBJECT fromNSString(NSString*)"),
 * so the arguments arrive shifted at those call sites. The implementations
 * must therefore NEVER read their threadStateData parameter - they resolve
 * the thread state internally via getThreadLocalData().
 */
JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString *str);
NSString *toNSString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
#endif

/* ---- registration ------------------------------------------------------- */

/* generated by ShimGenerator; called from JNI_OnLoad */
jint cn1sim_register_ios(JNIEnv *env);
jint cn1sim_register_windows(JNIEnv *env);

/* defined in CN1SimViewController.m; registers the CN1SimHost JAWT natives */
jint cn1sim_register_host(JNIEnv *env);

/* defined in CN1SimWindow.m; registers the pure-native window natives */
jint cn1sim_register_window(JNIEnv *env);

/* the JavaVM captured at JNI_OnLoad (for upcalls from AppKit threads) */
JavaVM *cn1jni_javavm(void);

#ifdef __cplusplus
}
#endif

#endif /* CN1JNI_RUNTIME_H */
