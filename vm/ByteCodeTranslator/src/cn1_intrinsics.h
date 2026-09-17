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

// Shared collection primitives: reads and integer metadata compile to direct
// loads/stores. Reference writes keep the runtime's single barrier implementation.
static inline JAVA_OBJECT cn1InlStorageGet(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_INT i) {
    return cn1RefBlockGet(block, i);
}
static inline JAVA_INT cn1InlStorageGetInt(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_INT i) {
    return cn1IntBlockGet(block, i);
}
static inline JAVA_VOID cn1InlStorageSetInt(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_INT i, JAVA_INT value) {
    cn1IntBlockSet(block, i, value);
}
static inline JAVA_VOID cn1InlStorageSet(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_INT i, JAVA_OBJECT value) {
    JAVA_OBJECT* slot = (JAVA_OBJECT*)(uintptr_t)block + i;
    CN1_SATB_DELETE(slot);
    CN1_WRITE_BARRIER(slot, value);
    *slot = value;
}

static inline JAVA_INT cn1InlStorageCapacity(CODENAME_ONE_THREAD_STATE, JAVA_LONG block) {
    return cn1RefBlockCount(block);
}
static inline JAVA_INT cn1InlIdentityNext(JAVA_LONG block, JAVA_INT from) {
    JAVA_OBJECT* data = (JAVA_OBJECT*)(uintptr_t)block;
    JAVA_INT capacity = cn1RefBlockCount(block);
    while(from < capacity && data[from] == JAVA_NULL) from += 2;
    return from;
}

#include "cn1_collections.h"
#ifdef CN1_COLL_ARRAYLIST
static inline JAVA_INT cn1InlListSize(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(owner == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
#endif
    return ((struct obj__java_util_ArrayList*)owner)->java_util_ArrayList_size;
}
static inline JAVA_BOOLEAN cn1InlListEmpty(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner) {
    return cn1InlListSize(threadStateData, owner) == 0;
}
static inline JAVA_OBJECT cn1InlListGet(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner, JAVA_INT index) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(owner == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
#endif
    struct obj__java_util_ArrayList* list = (struct obj__java_util_ArrayList*)owner;
    if(__builtin_expect((unsigned)index < (unsigned)list->java_util_ArrayList_size, 1))
        return cn1RefBlockGet(list->java_util_ArrayList_cn1Storage, index);
    return java_util_ArrayList_get___int_R_java_lang_Object(threadStateData, owner, index);
}
static inline JAVA_OBJECT cn1InlListSet(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner, JAVA_INT index, JAVA_OBJECT value) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(owner == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
#endif
    CN1_KEEP_NATIVE_OWNER(listOwner, owner);
    struct obj__java_util_ArrayList* list = (struct obj__java_util_ArrayList*)owner;
    if(__builtin_expect((unsigned)index < (unsigned)list->java_util_ArrayList_size, 1)) {
        JAVA_LONG block = list->java_util_ArrayList_cn1Storage;
        JAVA_OBJECT previous = cn1RefBlockGet(block, index);
        cn1InlStorageSet(threadStateData, block, index, value);
        return previous;
    }
    return java_util_ArrayList_set___int_java_lang_Object_R_java_lang_Object(threadStateData, owner, index, value);
}
static inline JAVA_BOOLEAN cn1InlListAdd(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner, JAVA_OBJECT value) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(owner == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); }
#endif
    CN1_KEEP_NATIVE_OWNER(listOwner, owner);
    struct obj__java_util_ArrayList* list = (struct obj__java_util_ArrayList*)owner;
    JAVA_INT size = list->java_util_ArrayList_size;
    JAVA_LONG block = list->java_util_ArrayList_cn1Storage;
    if(__builtin_expect(size < list->java_util_ArrayList_capacity && block != 0, 1)) {
        cn1InlStorageSet(threadStateData, block, size, value);
        list->java_util_ArrayList_size = size + 1;
        list->java_util_AbstractList_modCount++;
        return JAVA_TRUE;
    }
    return java_util_ArrayList_add___java_lang_Object_R_boolean(threadStateData, owner, value);
}
#endif

#if defined(__has_include)
#if __has_include("java_lang_StringBuilder.h") && __has_include("java_lang_String.h")
#include "java_lang_StringBuilder.h"
#include "java_lang_String.h"
#define CN1_HAVE_SB_INTRINSICS 1
#endif
#endif

#ifdef CN1_HAVE_SB_INTRINSICS

static inline JAVA_OBJECT cn1InlSbAppendChar(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT sb, JAVA_CHAR c) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, sb);
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)sb;
    JAVA_INT count = t->java_lang_StringBuilder_count;
    void* data = (void*)(uintptr_t)t->java_lang_StringBuilder_cn1Storage;
    if(__builtin_expect(count < t->java_lang_StringBuilder_capacity && (c <= 255 || t->java_lang_StringBuilder_wide), 1)) {
        if(!t->java_lang_StringBuilder_wide) ((JAVA_ARRAY_BYTE*)data)[count] = (JAVA_ARRAY_BYTE)c;
        else ((JAVA_ARRAY_CHAR*)data)[count] = c;
        t->java_lang_StringBuilder_count = count + 1;
        return sb;
    }
    return java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(threadStateData, sb, c);
}

static inline JAVA_OBJECT cn1InlSbAppendInt(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT sb, JAVA_INT v) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, sb);
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)sb;
    JAVA_INT count = t->java_lang_StringBuilder_count;
    void* data = (void*)(uintptr_t)t->java_lang_StringBuilder_cn1Storage;
    // positive with guaranteed headroom stays inline; negatives and
    // tight-capacity builders take the out-of-line path (INT_MIN etc.)
    if(__builtin_expect(v >= 0 && count + 11 <= t->java_lang_StringBuilder_capacity, 1)) {
        JAVA_ARRAY_CHAR* d = (JAVA_ARRAY_CHAR*)data;
        char tmp[11]; int n = 0;
        JAVA_INT q = v;
        do { tmp[n++] = (char)('0' + (q % 10)); q /= 10; } while(q != 0);
        for(int k = n - 1; k >= 0; k--) {
            if(!t->java_lang_StringBuilder_wide) ((JAVA_ARRAY_BYTE*)data)[count++] = (JAVA_ARRAY_BYTE)tmp[k];
            else d[count++] = (JAVA_ARRAY_CHAR)tmp[k];
        }
        t->java_lang_StringBuilder_count = count;
        return sb;
    }
    return java_lang_StringBuilder_append___int_R_java_lang_StringBuilder(threadStateData, sb, v);
}

static inline JAVA_OBJECT cn1InlSbAppendStr(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT sb, JAVA_OBJECT str) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, sb);
    CN1_KEEP_NATIVE_OWNER(stringOwner, str);
    if(__builtin_expect(str != JAVA_NULL, 1)) {
        struct obj__java_lang_String* s = (struct obj__java_lang_String*)str;
        struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)sb;
        JAVA_INT len = s->java_lang_String_count;
        if(len == 0) return sb;
        JAVA_INT count = t->java_lang_StringBuilder_count;
        void* data = (void*)(uintptr_t)t->java_lang_StringBuilder_cn1Storage;
        // short segments (concat literals) copy inline; longer ones ride the
        // out-of-line memcpy path
        if(__builtin_expect(len <= 8 && count + len <= t->java_lang_StringBuilder_capacity, 1)) {
            JAVA_ARRAY sarr = (JAVA_ARRAY)s->java_lang_String_value;
            JAVA_INT so = 0;
            int destinationLatin1 = !t->java_lang_StringBuilder_wide;
            int sourceLatin1 = sarr->__codenameOneParentClsReference == &class_array1__JAVA_BYTE;
            if(destinationLatin1 && !sourceLatin1) {
                return java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, sb, str);
            }
            if(destinationLatin1) {
                memcpy((JAVA_ARRAY_BYTE*)data + count, (JAVA_ARRAY_BYTE*)sarr->data + so, (size_t)len);
            } else {
                JAVA_ARRAY_CHAR* d = (JAVA_ARRAY_CHAR*)data + count;
                for(int i = 0; i < len; i++) d[i] = sourceLatin1
                    ? (JAVA_CHAR)((uint8_t*)sarr->data)[so + i] : ((JAVA_ARRAY_CHAR*)sarr->data)[so + i];
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
    CN1_KEEP_NATIVE_OWNER(bufferOwner, sb);
#ifndef CN1_DISABLE_BIBOP
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)sb;
    void* source = (void*)(uintptr_t)t->java_lang_StringBuilder_cn1Storage;
    if(__builtin_expect(class__java_lang_String.initialized && !t->java_lang_StringBuilder_wide, 1)) {
        JAVA_INT count = t->java_lang_StringBuilder_count;
        int off = (int)((sizeof(struct obj__java_lang_String) + 7) & ~(size_t)7);
        size_t total = off + CN1_FUSED_ARR_BYTES((size_t)count, sizeof(JAVA_ARRAY_BYTE));
        if(total <= CN1_BIBOP_MAX_OBJECT) {
            JAVA_OBJECT result = cn1BibopFastAllocNoZero(threadStateData, (int)total, &class__java_lang_String, CN1_BIBOP_CIDX(total));
            if(result != JAVA_NULL) {
                JAVA_OBJECT array = cn1FusedInstallPrimArray(result, off, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), count);
                if(count > 0) memcpy(((JAVA_ARRAY)array)->data, source, (size_t)count);
                struct obj__java_lang_String* string = (struct obj__java_lang_String*)result;
                string->java_lang_String_value = array;
                string->java_lang_String_count = count;
                string->java_lang_String_hashCode = 0;
                CN1_STRING_CLEAR_PEER(string);
                result->__codenameOneParentClsReference = &class__java_lang_String;
                return result;
            }
        }
    }
#endif
    return java_lang_StringBuilder_toString___R_java_lang_String(threadStateData, sb);
}

// Character replacement over compact storage: one native scan, one owned output
// allocation, and a contiguous byte loop. Preserve identity when nothing changes.
static inline JAVA_OBJECT cn1InlStrReplace(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str,
                                          JAVA_CHAR oldChar, JAVA_CHAR newChar) {
    CN1_KEEP_NATIVE_OWNER(sourceOwner, str);
    if(oldChar == newChar) return str;
    struct obj__java_lang_String* s = (struct obj__java_lang_String*)str;
    JAVA_ARRAY source = (JAVA_ARRAY)s->java_lang_String_value;
    if(source->__codenameOneParentClsReference == &class_array1__JAVA_BYTE) {
        int count = s->java_lang_String_count;
        const uint8_t* input = (const uint8_t*)source->data;
        if(oldChar > 255 || memchr(input, oldChar, (size_t)count) == 0) return str;
        if(newChar <= 255) {
            JAVA_ARRAY_BYTE* output;
            JAVA_OBJECT result = cn1FusedLatin1Begin(threadStateData, count, &output);
            if(result == JAVA_NULL) {
                result = newStringFromAsciiLen(threadStateData, (const char*)input, count);
                output = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)((struct obj__java_lang_String*)result)->java_lang_String_value)->data;
            }
            for(int i = 0; i < count; i++) {
                uint8_t ch = input[i];
                output[i] = (JAVA_ARRAY_BYTE)(ch == oldChar ? newChar : ch);
            }
            cn1FusedLatin1End(result, count);
            return result;
        }
    }
    return java_lang_String_replace___char_char_R_java_lang_String(threadStateData, str, oldChar, newChar);
}

static inline JAVA_INT cn1InlStrLength(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s) {
    return ((struct obj__java_lang_String*)s)->java_lang_String_count;
}

static inline __attribute__((always_inline)) JAVA_BOOLEAN cn1InlStrEquals(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT self, JAVA_OBJECT other) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(self == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); return JAVA_FALSE; }
#endif
    if(self == other) return JAVA_TRUE;
    if(other == JAVA_NULL || CN1_IS_TAGGED(other)
            || other->__codenameOneParentClsReference != &class__java_lang_String) return JAVA_FALSE;
    struct obj__java_lang_String* a = (struct obj__java_lang_String*)self;
    struct obj__java_lang_String* b = (struct obj__java_lang_String*)other;
    int count = a->java_lang_String_count;
    if(count != b->java_lang_String_count) return JAVA_FALSE;
    if(count == 0) return JAVA_TRUE;
    int ah = a->java_lang_String_hashCode, bh = b->java_lang_String_hashCode;
    if(ah != 0 && bh != 0 && ah != bh) return JAVA_FALSE;
    JAVA_ARRAY av = (JAVA_ARRAY)a->java_lang_String_value;
    JAVA_ARRAY bv = (JAVA_ARRAY)b->java_lang_String_value;
    int al = av->__codenameOneParentClsReference == &class_array1__JAVA_BYTE;
    int bl = bv->__codenameOneParentClsReference == &class_array1__JAVA_BYTE;
    if(al && bl) {
        const uint8_t* x = (const uint8_t*)av->data;
        const uint8_t* y = (const uint8_t*)bv->data;
        return cn1CompactBytesEqual(x, y, (size_t)count);
    }
    const uint16_t* ax = (const uint16_t*)av->data;
    const uint16_t* bx = (const uint16_t*)bv->data;
    if(!al && !bl) {
        return cn1CompactBytesEqual(ax,
                bx, (size_t)count * sizeof(uint16_t));
    }
    return al ? cn1CompactMixedEquals((const uint8_t*)av->data,
                    bx, (size_t)count)
              : cn1CompactMixedEquals((const uint8_t*)bv->data,
                    ax, (size_t)count);

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
        JAVA_ARRAY arr = (JAVA_ARRAY)t->java_lang_String_value;
        JAVA_INT end = count;
        JAVA_INT i = 0;
        if(arr->__codenameOneParentClsReference == &class_array1__JAVA_BYTE) {
            // Latin-1: (b & 0xff) IS the char value, so the hash is bit-identical to
            // the same text stored as char[]. 4-way reassociation mirrors the char path.
            JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)arr->data;
            for (; i + 4 <= end; i += 4) {
                hash = hash * 923521
                     + (b[i] & 0xff) * 29791
                     + (b[i + 1] & 0xff) * 961
                     + (b[i + 2] & 0xff) * 31
                     + (b[i + 3] & 0xff);
            }
            for (; i < end; ++i) {
                hash = 31 * hash + (b[i] & 0xff);
            }
        } else {
            JAVA_ARRAY_CHAR* chars = (JAVA_ARRAY_CHAR*)arr->data;
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
        }
        t->java_lang_String_hashCode = hash;
    }
    return hash;
}

static inline JAVA_CHAR cn1InlStrCharAt(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s, JAVA_INT index) {
    struct obj__java_lang_String* t = (struct obj__java_lang_String*)s;
    // bound by the string's LOGICAL length (count), not the backing array's
    // capacity: an aliasing/offset-constructed String can sit in an array
    // longer than count, and charAt(length()) must throw, not read past the
    // logical end
    if(__builtin_expect((unsigned int)index < (unsigned int)t->java_lang_String_count, 1)) {
        JAVA_ARRAY arr = (JAVA_ARRAY)t->java_lang_String_value;
        JAVA_INT o = index;
        // Latin-1 (byte[]) vs UTF-16 (char[]) chosen by the backing array's class.
        if(arr->__codenameOneParentClsReference == &class_array1__JAVA_BYTE) {
            return (JAVA_CHAR)(((JAVA_ARRAY_BYTE*)arr->data)[o] & 0xff);
        }
        return ((JAVA_ARRAY_CHAR*)arr->data)[o];
    }
    return java_lang_String_charAt___int_R_char(threadStateData, s, index); // throws
}

#endif // CN1_HAVE_SB_INTRINSICS

#endif // CN1_INTRINSICS_H
