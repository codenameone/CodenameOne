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
            "cn1_java_lang_StringBuilder_append_java_lang_Object_R_java_lang_StringBuilder",
            "cn1_java_lang_StringBuilder_append_java_lang_String_R_java_lang_StringBuilder",
            "cn1_java_lang_StringBuilder_charAt_int_R_char",
            "cn1_java_lang_StringBuilder_getChars_int_int_char_1ARRAY_int",
            "cn1_java_lang_StringToReal_parseDblImpl_java_lang_String_int_R_double",
            "cn1_java_lang_String_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY",
            "cn1_java_lang_String_charAt_int_R_char",
            "cn1_java_lang_String_charsToBytes_char_1ARRAY_char_1ARRAY_R_byte_1ARRAY",
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
            "cn1_java_util_HashMap_areEqualKeys_java_lang_Object_java_lang_Object_R_boolean",
            "cn1_java_util_HashMap_findNonNullKeyEntry_java_lang_Object_int_int_R_java_util_HashMap_Entry",
            "cn1_java_util_LinkedHashMap_findNonNullKeyEntry_java_lang_Object_int_int_R_java_util_HashMap_Entry",
            "cn1_java_util_Locale_getOSLanguage_R_java_lang_String",
            "cn1_java_util_TimeZone_getTimezoneId_R_java_lang_String",
            "cn1_java_util_TimeZone_getTimezoneOffset_java_lang_String_int_int_int_int_R_int",
            "cn1_java_util_TimeZone_getTimezoneRawOffset_java_lang_String_R_int",
            "cn1_java_util_TimeZone_isTimezoneDST_java_lang_String_long_R_boolean",
            "cn1_com_codename1_impl_platform_js_VMHost_getLastEventCode_R_int",
            "cn1_com_codename1_impl_platform_js_VMHost_pollEventCode_R_int"
    ));

    private static final Set<String> HOST_HOOK_PREFIXES = new HashSet<String>(Arrays.asList(
            "cn1_com_codename1_impl_platform_js_"
    ));

    private JavascriptNativeRegistry() {
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
