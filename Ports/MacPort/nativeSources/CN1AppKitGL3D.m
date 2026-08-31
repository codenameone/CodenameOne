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
#import <Foundation/Foundation.h>
#include "cn1_globals.h"

/*
 * GPU 3D surface: not implemented on the native macOS port.
 *
 * The iOS implementation lives in CN1GL3D.m and is written against OpenGL ES,
 * which does not exist on macOS at all, so that file is excluded from this port
 * (see Ports/MacPort/shared-natives.exclude). These natives are declared on the
 * shared IOSNative, so the symbols still have to exist here: ParparVM keeps a
 * native method alive BY its symbol appearing in the native sources, and an
 * absent one is read by the dead-code pass as "unused" and dropped -- a green
 * build with the feature quietly gone.
 *
 * They return failure rather than doing nothing, so IOSGLSurface's capability
 * check reports the surface as unavailable and callers take the unsupported
 * path instead of drawing into a context that was never created. A real
 * implementation over MTKView is a separate piece of work.
 */

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dClear___long_int_boolean_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_BOOLEAN __cn1Arg3, JAVA_BOOLEAN __cn1Arg4) {
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateContext___R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return 0;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateFloatBuffer___float_1ARRAY_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2) {
    return 0;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateShortBuffer___short_1ARRAY_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2) {
    return 0;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dCreateTexture___int_1ARRAY_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    return 0;
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dDestroyContext___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dDisposeBuffer___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dDisposePipeline___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dDisposeTexture___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dDrawArrays___long_long_long_int_int_int_float_1ARRAY_int_long_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_LONG __cn1Arg2, JAVA_LONG __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_OBJECT __cn1Arg7, JAVA_INT __cn1Arg8, JAVA_LONG __cn1Arg9, JAVA_INT __cn1Arg10, JAVA_INT __cn1Arg11) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dDrawIndexed___long_long_long_int_long_int_int_float_1ARRAY_int_long_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_LONG __cn1Arg2, JAVA_LONG __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_LONG __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7, JAVA_OBJECT __cn1Arg8, JAVA_INT __cn1Arg9, JAVA_LONG __cn1Arg10, JAVA_INT __cn1Arg11, JAVA_INT __cn1Arg12) {
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetOrCreatePipeline___long_java_lang_String_java_lang_String_int_int_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_OBJECT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7) {
    return 0;
}

JAVA_LONG com_codename1_impl_ios_IOSNative_gl3dGetViewPeer___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1) {
    return 0;
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dRequestRender___long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dSetContinuous___long_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_BOOLEAN __cn1Arg2) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dSetViewport___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dUpdateFloatBuffer___long_float_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_gl3dUpdateShortBuffer___long_short_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3) {
}

