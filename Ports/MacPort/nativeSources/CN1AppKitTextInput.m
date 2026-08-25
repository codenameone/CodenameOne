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
 * Native text input: not implemented yet on the native macOS port.
 *
 * The iOS implementation lives in CN1TextInputView.m and is a UITextInput
 * client, which has no macOS analogue; AppKit's equivalent is NSTextInputClient
 * on the rendering view. That file is therefore excluded from this port (see
 * Ports/MacPort/shared-natives.exclude) and these four symbols stand in for it.
 *
 * They must exist even while unimplemented, because ParparVM keeps a native
 * method alive BY its symbol appearing in the native sources; without them the
 * dead-code pass drops the Java side and the build stays green with text input
 * silently absent.
 *
 * Until NSTextInputClient is wired up, startTextInput does nothing, so the
 * framework's own editing path is what runs. Editing through the pure
 * com.codename1.ui.editor engine is unaffected -- it needs no native editor.
 */

JAVA_VOID com_codename1_impl_ios_IOSNative_setTextInputBounds___int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_startTextInput___int_boolean_boolean_boolean_java_lang_String_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT __cn1Arg1, JAVA_BOOLEAN __cn1Arg2, JAVA_BOOLEAN __cn1Arg3, JAVA_BOOLEAN __cn1Arg4, JAVA_OBJECT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7, JAVA_INT __cn1Arg8) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_stopTextInput__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
}

JAVA_VOID com_codename1_impl_ios_IOSNative_updateTextInputState___java_lang_String_int_int_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7, JAVA_INT __cn1Arg8) {
}

