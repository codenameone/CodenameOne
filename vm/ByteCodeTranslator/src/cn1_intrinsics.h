// Call-site-inlined fast paths for the hottest String/StringBuilder natives.
// The translator renames DEVIRTUALIZED calls of the mapped methods (see
// InlineIntrinsics.java) to these; every function falls back to the
// out-of-line native (nativeMethods.m) off its fast path, so semantics --
// growth, negative numbers, null receivers' appendNull, exact exceptions,
// hash computation -- are single-sourced there. Included by every generated
// .c after its class-header includes; compiles to nothing when the classes
// were eliminated from the build.
#ifndef CN1_INTRINSICS_H
#define CN1_INTRINSICS_H

#if defined(__has_include)
#if __has_include("java_lang_StringBuilder.h") && __has_include("java_lang_String.h")
#include "java_lang_StringBuilder.h"
#include "java_lang_String.h"
#define CN1_HAVE_SB_INTRINSICS 1
#endif
#endif

#ifdef CN1_HAVE_SB_INTRINSICS

static inline JAVA_OBJECT cn1InlSbAppendChar(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT sb, JAVA_CHAR c) {
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)sb;
    JAVA_INT count = t->java_lang_StringBuilder_count;
    JAVA_ARRAY arr = (JAVA_ARRAY)t->java_lang_StringBuilder_value;
    if(__builtin_expect(count < arr->length, 1)) {
        ((JAVA_ARRAY_CHAR*)arr->data)[count] = c;
        t->java_lang_StringBuilder_count = count + 1;
        return sb;
    }
    return java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(threadStateData, sb, c);
}

static inline JAVA_OBJECT cn1InlSbAppendInt(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT sb, JAVA_INT v) {
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)sb;
    JAVA_INT count = t->java_lang_StringBuilder_count;
    JAVA_ARRAY arr = (JAVA_ARRAY)t->java_lang_StringBuilder_value;
    // positive with guaranteed headroom stays inline; negatives and
    // tight-capacity builders take the out-of-line path (INT_MIN etc.)
    if(__builtin_expect(v >= 0 && count + 11 <= arr->length, 1)) {
        JAVA_ARRAY_CHAR* d = (JAVA_ARRAY_CHAR*)arr->data;
        char tmp[11]; int n = 0;
        JAVA_INT q = v;
        do { tmp[n++] = (char)('0' + (q % 10)); q /= 10; } while(q != 0);
        for(int k = n - 1; k >= 0; k--) {
            d[count++] = (JAVA_ARRAY_CHAR)tmp[k];
        }
        t->java_lang_StringBuilder_count = count;
        return sb;
    }
    return java_lang_StringBuilder_append___int_R_java_lang_StringBuilder(threadStateData, sb, v);
}

static inline JAVA_OBJECT cn1InlSbAppendStr(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT sb, JAVA_OBJECT str) {
    if(__builtin_expect(str != JAVA_NULL, 1)) {
        struct obj__java_lang_String* s = (struct obj__java_lang_String*)str;
        struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)sb;
        JAVA_INT len = s->java_lang_String_count;
        JAVA_INT count = t->java_lang_StringBuilder_count;
        JAVA_ARRAY arr = (JAVA_ARRAY)t->java_lang_StringBuilder_value;
        // short segments (concat literals) copy inline; longer ones ride the
        // out-of-line memcpy path
        if(__builtin_expect(len <= 8 && count + len <= arr->length, 1)) {
            JAVA_ARRAY_CHAR* src = ((JAVA_ARRAY_CHAR*)((JAVA_ARRAY)s->java_lang_String_value)->data) + s->java_lang_String_offset;
            JAVA_ARRAY_CHAR* d = ((JAVA_ARRAY_CHAR*)arr->data) + count;
            for(int i = 0; i < len; i++) {
                d[i] = src[i];
            }
            t->java_lang_StringBuilder_count = count + len;
            return sb;
        }
    }
    return java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, sb, str);
}

// Inline toString fast path: fused single-block String on the no-zero bump
// allocator with init-before-publish (all fields + child header + data are
// stored, THEN the class pointer publishes -- until that store the
// parentCls==0 guard keeps a signal-stopped scan from tracing the body).
// Mirrors the out-of-line native, which stays the fallback + source of truth.
static inline JAVA_OBJECT cn1InlSbToString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT sb) {
#if !defined(CN1_DISABLE_BIBOP) && !defined(DEBUG_GC_OBJECTS_IN_HEAP)
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)sb;
    if(__builtin_expect(class__java_lang_String.initialized, 1)) {
        int count = t->java_lang_StringBuilder_count;
        int off = (int)((sizeof(struct obj__java_lang_String) + 7) & ~(size_t)7);
        int total = off + CN1_FUSED_ARR_BYTES(count, sizeof(JAVA_ARRAY_CHAR));
        JAVA_OBJECT so = cn1BibopFastAllocNoZero(threadStateData, total, &class__java_lang_String, CN1_BIBOP_CIDX(total));
        if(__builtin_expect(so != JAVA_NULL, 1)) {
            JAVA_OBJECT arr = cn1FusedInstallPrimArray(so, off, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), count);
            struct obj__java_lang_String* rs = (struct obj__java_lang_String*)so;
            rs->java_lang_String_value = arr;
            rs->java_lang_String_offset = 0;
            rs->java_lang_String_count = count;
            rs->java_lang_String_hashCode = 0;
            rs->java_lang_String_nsString = 0;
            JAVA_ARRAY_CHAR* src = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)t->java_lang_StringBuilder_value)->data;
            JAVA_ARRAY_CHAR* dst = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)arr)->data;
            for(int i = 0; i < count; i++) {
                dst[i] = src[i];
            }
            so->__codenameOneParentClsReference = &class__java_lang_String; // PUBLISH
            return so;
        }
    }
#endif
    return java_lang_StringBuilder_toString___R_java_lang_String(threadStateData, sb);
}

static inline JAVA_INT cn1InlStrLength(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s) {
    return ((struct obj__java_lang_String*)s)->java_lang_String_count;
}

static inline JAVA_INT cn1InlStrHash(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s) {
    struct obj__java_lang_String* t = (struct obj__java_lang_String*)s;
    JAVA_INT hash = t->java_lang_String_hashCode;
    if(hash == 0) {
        // EXACT mirror of the native (nativeMethods.m): 4-way polynomial
        // reassociation, wrapping like Java under -fwrapv. Inlined because a
        // freshly built string (toString -> hashCode) always takes this path.
        JAVA_INT count = t->java_lang_String_count;
        if(count == 0) {
            return 0;
        }
        JAVA_INT end = count + t->java_lang_String_offset;
        JAVA_ARRAY_CHAR* chars = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)t->java_lang_String_value)->data;
        JAVA_INT i = t->java_lang_String_offset;
        for (; i + 4 <= end; i += 4) {
            hash = hash * 923521
                 + chars[i] * 29791
                 + chars[i + 1] * 961
                 + chars[i + 2] * 31
                 + chars[i + 3];
        }
        for (; i < end; ++i) {
            hash = 31 * hash + chars[i];
        }
        t->java_lang_String_hashCode = hash;
    }
    return hash;
}

static inline JAVA_CHAR cn1InlStrCharAt(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s, JAVA_INT index) {
    struct obj__java_lang_String* t = (struct obj__java_lang_String*)s;
    JAVA_ARRAY arr = (JAVA_ARRAY)t->java_lang_String_value;
    if(__builtin_expect((unsigned int)index < (unsigned int)arr->length, 1)) {
        return ((JAVA_ARRAY_CHAR*)arr->data)[t->java_lang_String_offset + index];
    }
    return java_lang_String_charAt___int_R_char(threadStateData, s, index); // throws
}

#endif // CN1_HAVE_SB_INTRINSICS
#endif // CN1_INTRINSICS_H
