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
package com.codename1.tools.translator;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps direct string and native-storage calls to the
 * call-site-inlined fast paths in cn1_intrinsics.h. Applied by Invoke and
 * CustomInvoke ONLY to non-virtual (direct or closed-world-devirtualized)
 * calls; the inline functions share the exact signature of the natives they
 * wrap and fall back to them off the fast path, so the swap is a pure rename.
 */
public final class InlineIntrinsics {
    private static final Map<String, String> RENAMES = new HashMap<String, String>();
    static {
        RENAMES.put("java_util_ArrayList_get___int_R_java_lang_Object", "cn1InlListGet");
        RENAMES.put("java_util_ArrayList_set___int_java_lang_Object_R_java_lang_Object", "cn1InlListSet");
        RENAMES.put("java_util_ArrayList_add___java_lang_Object_R_boolean", "cn1InlListAdd");
        RENAMES.put("java_util_ArrayList_size___R_int", "cn1InlListSize");
        RENAMES.put("java_util_ArrayList_isEmpty___R_boolean", "cn1InlListEmpty");
        RENAMES.put("java_util_NativeStorage_capacity___long_R_int", "cn1InlStorageCapacity");
        RENAMES.put("java_util_NativeStorage_part___long_int_R_long", "cn1InlStoragePart");
        RENAMES.put("java_util_NativeStorage_get___long_int_R_java_lang_Object", "cn1InlStorageGet");
        RENAMES.put("java_util_NativeStorage_getInt___long_int_R_int", "cn1InlStorageGetInt");
        RENAMES.put("java_util_NativeStorage_setInt___long_int_int", "cn1InlStorageSetInt");
        RENAMES.put("java_util_NativeStorage_set___long_int_java_lang_Object", "cn1InlStorageSet");
        RENAMES.put("java_util_NativeStorage_setOwned___java_lang_Object_long_int_java_lang_Object", "cn1InlStorageSetOwned");
        RENAMES.put("java_util_NativeStorage_move___long_int_int_int", "cn1RefBlockMove");
        RENAMES.put("java_util_NativeStorage_clear___long_int_int", "cn1RefBlockClear");
        RENAMES.put("java_lang_StringBuilder_append___char_R_java_lang_StringBuilder", "cn1InlSbAppendChar");
        RENAMES.put("java_lang_StringBuilder_append___int_R_java_lang_StringBuilder", "cn1InlSbAppendInt");
        RENAMES.put("java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder", "cn1InlSbAppendStr");
        RENAMES.put("java_lang_StringBuilder_toString___R_java_lang_String", "cn1InlSbToString");
        RENAMES.put("java_lang_StringBuilder_resizeBufferImpl___int_boolean_R_boolean", "cn1InlSbResize");
        RENAMES.put("java_lang_String_replace___char_char_R_java_lang_String", "cn1InlStrReplace");
        RENAMES.put("java_lang_String_length___R_int", "cn1InlStrLength");
        RENAMES.put("java_lang_String_hashCode___R_int", "cn1InlStrHash");
        RENAMES.put("java_lang_String_equals___java_lang_Object_R_boolean", "cn1InlStrEquals");
        RENAMES.put("java_lang_String_charAt___int_R_char", "cn1InlStrCharAt");
        // Dart List<int> index accesses (DartLongList) -- inlined to raw long[] access
        // in a hot loop; see cn1InlDllGet/Set in cn1_intrinsics.h (__has_include-guarded).
        RENAMES.put("dart_core_DartLongList_getLong___long_R_long", "cn1InlDllGet");
        RENAMES.put("dart_core_DartLongList_setLong___long_long_R_long", "cn1InlDllSet");
    }

    private InlineIntrinsics() {
    }

    /** The inlined name for a direct-call C function name, or the name itself. */
    public static String rename(String cFunctionName) {
        String r = RENAMES.get(cFunctionName);
        return r != null ? r : cFunctionName;
    }
}
