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
package com.codename1.tools.translator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class JavascriptNativeRegistry {
    enum NativeCategory {
        RUNTIME_IMPLEMENTED,
        HOST_HOOK,
        UNSUPPORTED,
        UNCATEGORIZED
    }

    private static final Set<String> RUNTIME_IMPLEMENTED = new HashSet<String>(Arrays.asList(
            "cn1_java_io_InputStreamReader_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY",
            "cn1_java_io_NSLogOutputStream_write_byte_1ARRAY_int_int",
            "cn1_java_lang_Class_forNameImpl_java_lang_String_R_java_lang_Class",
            "cn1_java_lang_Class_getComponentType_R_java_lang_Class",
            "cn1_java_lang_Class_getNameImpl_R_java_lang_String",
            "cn1_java_lang_Class_getName_R_java_lang_String",
            "cn1_java_lang_Class_hashCode_R_int",
            "cn1_java_lang_Class_isAnnotation_R_boolean",
            "cn1_java_lang_Class_isAnonymousClass_R_boolean",
            "cn1_java_lang_Class_isArray_R_boolean",
            "cn1_java_lang_Class_isAssignableFrom_java_lang_Class_R_boolean",
            "cn1_java_lang_Class_isEnum_R_boolean",
            "cn1_java_lang_Class_isInstance_java_lang_Object_R_boolean",
            "cn1_java_lang_Class_isInterface_R_boolean",
            "cn1_java_lang_Class_isPrimitive_R_boolean",
            "cn1_java_lang_Class_isSynthetic_R_boolean",
            "cn1_java_lang_Class_newInstanceImpl_R_java_lang_Object",
            "cn1_java_lang_Character_toLowerCase_char_R_char",
            "cn1_java_lang_Character_toLowerCase_int_R_int",
            "cn1_java_lang_Double_doubleToLongBits_double_R_long",
            "cn1_java_lang_Double_longBitsToDouble_long_R_double",
            "cn1_java_lang_Double_toStringImpl_double_boolean_R_java_lang_String",
            "cn1_java_lang_Enum_valueOf_java_lang_Class_java_lang_String_R_java_lang_Enum",
            "cn1_java_lang_Float_floatToIntBits_float_R_int",
            "cn1_java_lang_Float_intBitsToFloat_int_R_float",
            "cn1_java_lang_Float_toStringImpl_float_boolean_R_java_lang_String",
            "cn1_java_lang_Integer_toString_int_R_java_lang_String",
            "cn1_java_lang_Integer_toString_int_int_R_java_lang_String",
            "cn1_java_lang_Long_toString_long_int_R_java_lang_String",
            "cn1_java_lang_Math_abs_double_R_double",
            "cn1_java_lang_Math_abs_float_R_float",
            "cn1_java_lang_Math_abs_int_R_int",
            "cn1_java_lang_Math_abs_long_R_long",
            "cn1_java_lang_Math_atan_double_R_double",
            "cn1_java_lang_Math_ceil_double_R_double",
            "cn1_java_lang_Math_cos_double_R_double",
            "cn1_java_lang_Math_floor_double_R_double",
            "cn1_java_lang_Math_max_double_double_R_double",
            "cn1_java_lang_Math_max_float_float_R_float",
            "cn1_java_lang_Math_max_int_int_R_int",
            "cn1_java_lang_Math_max_long_long_R_long",
            "cn1_java_lang_Math_min_double_double_R_double",
            "cn1_java_lang_Math_min_float_float_R_float",
            "cn1_java_lang_Math_min_int_int_R_int",
            "cn1_java_lang_Math_min_long_long_R_long",
            "cn1_java_lang_Math_pow_double_double_R_double",
            "cn1_java_lang_Math_sin_double_R_double",
            "cn1_java_lang_Math_sqrt_double_R_double",
            "cn1_java_lang_Math_tan_double_R_double",
            "cn1_java_lang_Object_getClassImpl_R_java_lang_Class",
            "cn1_java_lang_Object_hashCode_R_int",
            "cn1_java_lang_Object_notify",
            "cn1_java_lang_Object_notifyAll",
            "cn1_java_lang_Object_toString_R_java_lang_String",
            "cn1_java_lang_Object_wait_long_int",
            "cn1_java_lang_Runtime_freeMemoryImpl_R_long",
            "cn1_java_lang_Runtime_totalMemoryImpl_R_long",
            "cn1_java_lang_StringBuilder_append_char_R_java_lang_StringBuilder",
            "cn1_java_lang_StringBuilder_append_int_R_java_lang_StringBuilder",
            "cn1_java_lang_StringBuilder_append_long_R_java_lang_StringBuilder",
            "cn1_java_lang_StringBuilder_append_java_lang_Object_R_java_lang_StringBuilder",
            "cn1_java_lang_StringBuilder_append_java_lang_String_R_java_lang_StringBuilder",
            "cn1_java_lang_StringBuilder_charAt_int_R_char",
            "cn1_java_lang_StringBuilder_getChars_int_int_char_1ARRAY_int",
            "cn1_java_lang_StringBuilder_toString_R_java_lang_String",
            "cn1_java_lang_StringToReal_parseDblImpl_java_lang_String_int_R_double",
            "cn1_java_lang_String_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY",
            "cn1_java_lang_String_charAt_int_R_char",
            "cn1_java_lang_String_charsToBytes_char_1ARRAY_char_1ARRAY_R_byte_1ARRAY",
            "cn1_java_lang_String_cn1FusedConcat2_java_lang_String_java_lang_String_R_java_lang_String",
            "cn1_java_lang_String_cn1FusedConcat3_java_lang_String_java_lang_String_java_lang_String_R_java_lang_String",
            "cn1_java_lang_String_cn1FusedConcat4_java_lang_String_java_lang_String_java_lang_String_java_lang_String_R_java_lang_String",
            "cn1_java_lang_String_cn1FusedConcat5_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_R_java_lang_String",
            "cn1_java_lang_String_equalsIgnoreCase_java_lang_String_R_boolean",
            "cn1_java_lang_String_equals_java_lang_Object_R_boolean",
            "cn1_java_lang_String_format_java_lang_String_java_lang_Object_1ARRAY_R_java_lang_String",
            "cn1_java_lang_String_getChars_int_int_char_1ARRAY_int",
            "cn1_java_lang_String_hashCode_R_int",
            "cn1_java_lang_String_indexOf_int_int_R_int",
            "cn1_java_lang_String_releaseNSString_long",
            "cn1_java_lang_String_toLowerCase_R_java_lang_String",
            "cn1_java_lang_String_toString_R_java_lang_String",
            "cn1_java_lang_String_toUpperCase_R_java_lang_String",
            "cn1_java_lang_System_arraycopy_java_lang_Object_int_java_lang_Object_int_int",
            "cn1_java_lang_System_currentTimeMillis_R_long",
            "cn1_java_lang_System_exit_int",
            "cn1_java_lang_System_gcLight",
            "cn1_java_lang_System_gcMarkSweep",
            "cn1_java_lang_System_identityHashCode_java_lang_Object_R_int",
            "cn1_java_lang_Integer_cn1Value_R_int",
            "cn1_java_lang_Integer_valueOf_int_R_java_lang_Integer",
            "cn1_java_lang_System_isHighFrequencyGC_R_boolean",
            "cn1_java_lang_Thread_currentThread_R_java_lang_Thread",
            "cn1_java_lang_Thread_getNativeThreadId_R_long",
            "cn1_java_lang_Thread_interrupt0",
            "cn1_java_lang_Thread_isInterrupted_boolean_R_boolean",
            "cn1_java_lang_Thread_releaseThreadNativeResources_long",
            "cn1_java_lang_Thread_setPriorityImpl_int",
            "cn1_java_lang_Thread_sleep_long",
            "cn1_java_lang_Thread_start",
            "cn1_java_lang_Throwable_fillInStack",
            "cn1_java_lang_Throwable_getStack_R_java_lang_String",
            "cn1_java_lang_reflect_Array_newInstanceImpl_java_lang_Class_int_R_java_lang_Object",
            "cn1_java_text_DateFormat_format_java_util_Date_java_lang_StringBuffer_R_java_lang_String",
            "cn1_java_lang_String_compareTo_java_lang_String_R_int",
            "cn1_java_util_HashMap_areEqualKeys_java_lang_Object_java_lang_Object_R_boolean",
            "cn1_java_util_HashMap_get_java_lang_Object_R_java_lang_Object",
            "cn1_java_util_HashMap_put_java_lang_Object_java_lang_Object_R_java_lang_Object",
            "cn1_java_util_HashMap_remove_java_lang_Object_R_java_lang_Object",
            "cn1_java_util_HashMap_containsKey_java_lang_Object_R_boolean",
            "cn1_java_util_HashMap_clear",
            "cn1_java_util_Locale_getOSLanguage_R_java_lang_String",
            "cn1_java_util_TimeZone_getTimezoneId_R_java_lang_String",
            "cn1_java_util_TimeZone_getTimezoneOffset_java_lang_String_int_int_int_int_R_int",
            "cn1_java_util_TimeZone_getTimezoneRawOffset_java_lang_String_R_int",
            "cn1_java_util_TimeZone_isTimezoneDST_java_lang_String_long_R_boolean",
            "cn1_com_codename1_impl_platform_js_VMHost_getLastEventCode_R_int",
            "cn1_com_codename1_impl_platform_js_VMHost_pollEventCode_R_int",
            // NativeInterface bridge: runtime-implemented in parparvm_runtime.js
            // (bindNative). Each forwards to the main thread via the
            // __cn1_native_interface_call__ host hook and coerces the result to
            // the declared Java type (createJavaString / _LfromNumber / newArray).
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callBoolean_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_boolean",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callByte_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_byte",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callShort_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_short",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callInt_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_int",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callChar_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_char",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callLong_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_long",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callFloat_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_float",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callDouble_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_double",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callString_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_java_lang_String",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callObject_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_java_lang_Object",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callArray_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_java_lang_String_R_java_lang_Object",
            "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_callVoid_java_lang_String_java_lang_String_java_lang_Object_1ARRAY"
    ));

    private static final Set<String> HOST_HOOK_PREFIXES = new HashSet<String>(Arrays.asList(
            "cn1_com_codename1_impl_platform_js_"
    ));

    private JavascriptNativeRegistry() {
    }

    // Pure-Java twins the JS RUNTIME bindings delegate to (bindNative ->
    // cn1_..._getImpl_... etc). Nothing in BYTECODE calls them, so the
    // unused-method cull would eliminate them and the delegation would throw
    // ReferenceError at runtime -- they must be retention roots. Keyed as
    // "<mangled-class>.<method-name>" (all overloads of the name are kept).
    private static final Set<String> RUNTIME_DELEGATE_TARGETS = new HashSet<String>(Arrays.asList(
            "java_util_HashMap.getImpl",
            "java_util_HashMap.putImpl",
            "java_util_HashMap.removeImpl",
            "java_util_HashMap.containsKeyImpl",
            "java_util_HashMap.clearImpl",
            "java_lang_StringBuilder.toStringImpl",
            "java_lang_Integer.valueOfHeap"
    ));

    static boolean isRuntimeDelegateTarget(String mangledClassName, String methodName) {
        return RUNTIME_DELEGATE_TARGETS.contains(mangledClassName + "." + methodName);
    }

    static boolean hasRuntimeImplementation(String symbol) {
        return RUNTIME_IMPLEMENTED.contains(symbol) || RUNTIME_IMPLEMENTED.contains(normalizeTripleUnderscores(symbol));
    }

    static boolean isHostHook(String symbol) {
        for (String prefix : HOST_HOOK_PREFIXES) {
            if (symbol.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    static NativeCategory categoryFor(String symbol) {
        if (hasRuntimeImplementation(symbol)) {
            return NativeCategory.RUNTIME_IMPLEMENTED;
        }
        if (isHostHook(symbol)) {
            return NativeCategory.HOST_HOOK;
        }
        if (unsupportedReason(symbol) != null) {
            return NativeCategory.UNSUPPORTED;
        }
        return NativeCategory.UNCATEGORIZED;
    }

    static String unsupportedReason(String symbol) {
        if (symbol.startsWith("cn1_java_io_File_")) {
            return "java.io.File native filesystem access is not supported in javascript backend";
        }
        return null;
    }

    private static String normalizeTripleUnderscores(String symbol) {
        return symbol.replace("___", "_");
    }
}
