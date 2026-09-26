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
    // SATB half only; the BLOCK is what the generational half remembers (see
    // cn1RefBlockSet) -- a slot address is not an object header.
    CN1_WRITE_BARRIER(JAVA_NULL, value);
    CN1_GEN_REMEMBER_BLOCK(block, value);
    *slot = value;
}

// A store into a block that `owner` traces. Barrier-wise it is a field store into the
// owner: the SATB halves as for any slot, and the generational half remembers the owner
// (only if it is old), whose mark function then traces whichever block it holds now.
static inline JAVA_VOID cn1InlStorageSetOwned(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner, JAVA_LONG block, JAVA_INT i, JAVA_OBJECT value) {
    JAVA_OBJECT* slot = (JAVA_OBJECT*)(uintptr_t)block + i;
    CN1_SATB_DELETE(slot);
    CN1_WRITE_BARRIER(owner, value);
    *slot = value;
}
static inline JAVA_LONG cn1InlStoragePart(CODENAME_ONE_THREAD_STATE, JAVA_LONG table, JAVA_INT part) {
    return cn1TablePart(table, part);
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
    // The block carries its own element count in its header, so there is no
    // capacity field to read: cn1RefBlockCount is one load, from the same header
    // line this fast path is about to write through. It answers 0 for a handle of 0,
    // which subsumes the block != 0 test -- kept anyway because it is what makes the
    // unchecked store below safe to read at a glance.
    if(__builtin_expect(block != 0 && size < cn1RefBlockCount(block), 1)) {
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

/* ONE CASE, INLINE. EVERYTHING ELSE IS A CALL.
 *
 * This used to try to handle every shape here -- latin1 or wide destination,
 * fused or heap-backed source, with a decode loop for the wide case -- and the
 * result was ~60 instructions including two adrp/add pairs just to materialise
 * the class pointers the coder test compares against. clang declined to inline
 * that, correctly: the disassembly carried 3,408 call sites and SIXTY-SIX
 * out-of-line copies of it. A "fast path" the compiler will not take is not a
 * fast path, it is a call with extra steps.
 *
 * So the inline body is now only the shape that actually dominates string
 * concatenation -- appending a short, fused, Latin-1 String onto a Latin-1
 * builder that has room. That is one class-pointer comparison, three integer
 * tests and a memcpy. Every other case (null, wide destination, heap-backed
 * byte[] source, long, does not fit) goes to the out-of-line native, which was
 * already the fallback here and is the source of truth for all of them -- so
 * narrowing can only move work off this path, never change its answer. */
/* THE CONSTRUCTOR'S OWN RESIZE NEVER ALLOCATES, SO IT SHOULD NOT BE A CALL.
 *
 * `new StringBuilder()` is `resizeBuffer(INITIAL_CAPACITY=16, false)`, and a
 * StringBuilder carries __cn1InlineStorage[16] inside the object -- so 16 narrow
 * bytes fit exactly, with nothing to allocate and nothing to copy. It was still
 * an out-of-line native carrying a keepalive fence and a dozen branches, and the
 * growth that follows is a SECOND trip through the same native: two calls per
 * builder, 32M of them in the stringBuilding benchmark. A dense profile put
 * resizeBuffer + resizeBufferImpl at 17% of mutator time with memmove another
 * 9.8%; HotSpot's constructor is an inlined `new byte[16]`.
 *
 * Only the case that allocates nothing is inlined here. A fresh builder has
 * cn1Storage == 0 (the object is zeroed), so that test alone separates the
 * constructor from every later growth, and the stack-builder scope -- whose
 * inline bytes live in a CN1StackBuffer rather than the object -- is excluded
 * explicitly. Everything else, growth included, goes to the native, which stays
 * the source of truth.
 */
static inline JAVA_BOOLEAN cn1InlSbResize(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder,
                                          JAVA_INT capacity, JAVA_BOOLEAN wide) {
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)builder;
    /* CAPACITY IS PART OF THE RESULT. This path used to install the storage and
     * return without recording the capacity, so `new StringBuilder().capacity()`
     * answered 0 on a heap builder where the JDK answers 16 -- measured, by a probe
     * whose stack-builder arm (which went to the native) answered 16. It stayed
     * invisible because growth recovers from capacity 0, at the cost of an extra
     * growth trip on every heap builder's first append. */
    if(__builtin_expect(!wide
            && t->java_lang_StringBuilder_cn1Storage == 0
            && capacity >= 0
            && capacity <= (JAVA_INT)sizeof(t->__cn1InlineStorage)
            && CN1_OBJ_HEAPPOS(t) != CN1_GC_STACK_BUILDER, 1)) {
        t->java_lang_StringBuilder_cn1Storage = (JAVA_LONG)(uintptr_t)t->__cn1InlineStorage;
        t->java_lang_StringBuilder_capacity = capacity;
        return JAVA_TRUE;
    }
    /* An ESCAPE-PROVEN builder arrives with its storage already installed -- NEW
     * points cn1Storage at the CN1StackBuffer's data -- so its constructor's resize
     * allocates nothing and copies nothing either: the native's own bytes <=
     * inlineBytes branch finds previous == inlineStorage and only stores the
     * capacity. Doing that here takes the call off every stack builder's
     * construction, which the profile put at ~375 of ~3200 stringBuilding samples. */
    if(__builtin_expect(!wide
            && capacity >= 0
            && CN1_OBJ_HEAPPOS(t) == CN1_GC_STACK_BUILDER, 1)) {
        struct CN1StackBuffer* scope;
        memcpy(&scope, t->__cn1InlineStorage, sizeof(scope));
        if(t->java_lang_StringBuilder_cn1Storage == (JAVA_LONG)(uintptr_t)scope->initialData
                && capacity <= scope->initialBytes
                && !t->java_lang_StringBuilder_wide) {
            t->java_lang_StringBuilder_capacity = capacity;
            return JAVA_TRUE;
        }
    }
    return java_lang_StringBuilder_resizeBufferImpl___int_boolean_R_boolean(
            threadStateData, builder, capacity, wide);
}

static inline JAVA_OBJECT cn1InlSbAppendStr(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT sb, JAVA_OBJECT str) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, sb);
    CN1_KEEP_NATIVE_OWNER(stringOwner, str);
    if(__builtin_expect(str != JAVA_NULL, 1)) {
        struct obj__java_lang_String* s = (struct obj__java_lang_String*)str;
        struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)sb;
        /* value == JAVA_NULL means the payload is fused into the String itself,
         * and the twin class word IS the coder -- see cn1StrIsLatin1. Testing
         * both at once keeps this to a single class-pointer comparison. */
        if(__builtin_expect(s->java_lang_String_value == JAVA_NULL
                && CN1_OBJ_CLASS(str) == &class__java_lang_String_i8
                && !t->java_lang_StringBuilder_wide, 1)) {
            JAVA_INT len = s->java_lang_String_count;
            JAVA_INT count = t->java_lang_StringBuilder_count;
            if(__builtin_expect((unsigned)len <= 8u
                    && count + len <= t->java_lang_StringBuilder_capacity, 1)) {
                cn1SmallCopy((JAVA_ARRAY_BYTE*)(uintptr_t)t->java_lang_StringBuilder_cn1Storage + count,
                       (char*)str + ((sizeof(struct obj__java_lang_String) + 7) & ~(size_t)7),
                       (size_t)len);
                t->java_lang_StringBuilder_count = count + len;
                return sb;
            }
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
        cn1InitStringTwin();
        // No array header: the characters follow the fields, the coder is the twin.
        size_t total = off + (size_t)count;
        if(total <= CN1_BIBOP_MAX_OBJECT) {
            JAVA_OBJECT result = cn1BibopFastAllocNoZero(threadStateData, (int)total, &class__java_lang_String_i8, CN1_BIBOP_CIDX(total));
            if(result != JAVA_NULL) {
                cn1SmallCopy((char*)result + off, source, (size_t)count);
                struct obj__java_lang_String* string = (struct obj__java_lang_String*)result;
                // NoZero: value must be nulled by hand, and NULL is the inline marker.
                string->java_lang_String_value = JAVA_NULL;
                string->java_lang_String_count = count;
                string->java_lang_String_hashCode = 0;
                CN1_STRING_CLEAR_PEER(string);
                CN1_OBJ_SET_CLASS(result, &class__java_lang_String_i8);
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
    if(cn1StrIsLatin1(str)) {
        int count = s->java_lang_String_count;
        const uint8_t* input = (const uint8_t*)cn1StrChars(str);
        if(oldChar > 255 || memchr(input, oldChar, (size_t)count) == 0) return str;
        if(newChar <= 255) {
            JAVA_ARRAY_BYTE* output;
            JAVA_OBJECT result = cn1FusedLatin1Begin(threadStateData, count, &output);
            if(result == JAVA_NULL) {
                result = newStringFromAsciiLen(threadStateData, (const char*)input, count);
                output = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)((struct obj__java_lang_String*)result)->java_lang_String_value);
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
            || !cn1IsStringClass(CN1_OBJ_CLASS(other))) return JAVA_FALSE;
    struct obj__java_lang_String* a = (struct obj__java_lang_String*)self;
    struct obj__java_lang_String* b = (struct obj__java_lang_String*)other;
    int count = a->java_lang_String_count;
    if(count != b->java_lang_String_count) return JAVA_FALSE;
    if(count == 0) return JAVA_TRUE;
    int ah = a->java_lang_String_hashCode, bh = b->java_lang_String_hashCode;
    if(ah != 0 && bh != 0 && ah != bh) return JAVA_FALSE;
    int al = cn1StrIsLatin1(self);
    int bl = cn1StrIsLatin1(other);
    if(al && bl) {
        const uint8_t* x = (const uint8_t*)cn1StrChars(self);
        const uint8_t* y = (const uint8_t*)cn1StrChars(other);
        return cn1CompactBytesEqual(x, y, (size_t)count);
    }
    const uint16_t* ax = (const uint16_t*)cn1StrChars(self);
    const uint16_t* bx = (const uint16_t*)cn1StrChars(other);
    if(!al && !bl) {
        return cn1CompactBytesEqual(ax,
                bx, (size_t)count * sizeof(uint16_t));
    }
    return al ? cn1CompactMixedEquals((const uint8_t*)cn1StrChars(self),
                    bx, (size_t)count)
              : cn1CompactMixedEquals((const uint8_t*)cn1StrChars(other),
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
        JAVA_INT end = count;
        JAVA_INT i = 0;
        if(cn1StrIsLatin1(s)) {
            // Latin-1: (b & 0xff) IS the char value, so the hash is bit-identical to
            // the same text stored as char[]. 4-way reassociation mirrors the char path.
            JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)cn1StrChars(s);
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
            JAVA_ARRAY_CHAR* chars = (JAVA_ARRAY_CHAR*)cn1StrChars(s);
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
        JAVA_INT o = index;
        // Latin-1 vs UTF-16 chosen by the store, named in one place.
        if(cn1StrIsLatin1(s)) {
            return (JAVA_CHAR)(((JAVA_ARRAY_BYTE*)cn1StrChars(s))[o] & 0xff);
        }
        return ((JAVA_ARRAY_CHAR*)cn1StrChars(s))[o];
    }
    return java_lang_String_charAt___int_R_char(threadStateData, s, index); // throws
}

#endif // CN1_HAVE_SB_INTRINSICS

#if defined(__has_include)
#if __has_include("dart_core_DartLongList.h")
#include "dart_core_DartLongList.h"
#define CN1_HAVE_DLL_INTRINSICS 1
#endif
#endif

#ifdef CN1_HAVE_DLL_INTRINSICS

// Dart List<int> element access. The bounds test is against the LOGICAL length
// (which can be smaller than the backing array), exactly as DartLongList does;
// out-of-range falls through to the out-of-line method so RangeError.indexError
// stays single-sourced there.

static inline JAVA_LONG cn1InlDllGet(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT self, JAVA_LONG index) {
    struct obj__dart_core_DartLongList* t = (struct obj__dart_core_DartLongList*)self;
    if(__builtin_expect(self != JAVA_NULL &&
            index >= 0 && index < (JAVA_LONG)t->dart_core_DartLongList_len, 1)) {
        JAVA_ARRAY arr = (JAVA_ARRAY)t->dart_core_DartLongList_a;
        return ((JAVA_ARRAY_LONG*)CN1_ARRAY_DATA(arr))[(JAVA_INT)index];
    }
    return dart_core_DartLongList_getLong___long_R_long(threadStateData, self, index);
}

static inline JAVA_LONG cn1InlDllSet(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT self, JAVA_LONG index, JAVA_LONG value) {
    struct obj__dart_core_DartLongList* t = (struct obj__dart_core_DartLongList*)self;
    if(__builtin_expect(self != JAVA_NULL &&
            index >= 0 && index < (JAVA_LONG)t->dart_core_DartLongList_len, 1)) {
        JAVA_ARRAY arr = (JAVA_ARRAY)t->dart_core_DartLongList_a;
        ((JAVA_ARRAY_LONG*)CN1_ARRAY_DATA(arr))[(JAVA_INT)index] = value;
        return value;
    }
    return dart_core_DartLongList_setLong___long_long_R_long(threadStateData, self, index, value);
}

#endif // CN1_HAVE_DLL_INTRINSICS

#endif // CN1_INTRINSICS_H
