package com.codename1.tools.translator;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps the C names of the hottest String/StringBuilder natives to the
 * call-site-inlined fast paths in cn1_intrinsics.h. Applied by Invoke and
 * CustomInvoke ONLY to non-virtual (direct or closed-world-devirtualized)
 * calls; the inline functions share the exact signature of the natives they
 * wrap and fall back to them off the fast path, so the swap is a pure rename.
 */
public final class InlineIntrinsics {
    private static final Map<String, String> RENAMES = new HashMap<String, String>();
    static {
        RENAMES.put("java_lang_StringBuilder_append___char_R_java_lang_StringBuilder", "cn1InlSbAppendChar");
        RENAMES.put("java_lang_StringBuilder_append___int_R_java_lang_StringBuilder", "cn1InlSbAppendInt");
        RENAMES.put("java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder", "cn1InlSbAppendStr");
        RENAMES.put("java_lang_StringBuilder_toString___R_java_lang_String", "cn1InlSbToString");
        RENAMES.put("java_lang_String_length___R_int", "cn1InlStrLength");
        RENAMES.put("java_lang_String_hashCode___R_int", "cn1InlStrHash");
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
