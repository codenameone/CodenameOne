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

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include "cn1_globals.h"
#include "cn1_virtual_thread.h"
#include <stdint.h>
#include <stdio.h>
#ifndef _WIN32
#include <sys/mman.h> /* cn1AllocThreadStack maps the shadow stack */
#endif
#include <ctype.h>
#include <assert.h>
#include <errno.h>
#include <time.h>
#include <stdlib.h>
#include <string.h>
#if defined(__APPLE__)
#include <malloc/malloc.h>
#endif

#ifndef MAX
#define MAX(a,b) ((a) > (b) ? (a) : (b))
#endif

#include "java_lang_Object.h"
#include "java_lang_Boolean.h"
#include "java_lang_String.h"
#include "java_lang_Integer.h"
#include "java_lang_Byte.h"
#include "java_lang_Short.h"
#include "java_lang_Character.h"
#include "java_lang_Thread.h"
#include "java_lang_Long.h"
#include "java_lang_Double.h"
#include "java_lang_Float.h"
#include "java_lang_Runnable.h"
#include "java_lang_Throwable.h"
#include "java_lang_StringBuilder.h"
#include "java_util_HashMap.h"
#include "java_lang_NullPointerException.h"
#include "java_lang_Class.h"
#include "java_lang_System.h"
#include "java_lang_StackOverflowError.h"

#if defined(__APPLE__) && defined(__OBJC__)
#import <Foundation/Foundation.h>
#endif

#ifdef _WIN32
#include "cn1_win_compat.h"
#else
#include <pthread.h>
#include <unistd.h>
#if defined(__linux__)
#include <signal.h>   /* sigaltstack / stack_t for the per-thread alt signal stack */
#endif
#include <sys/time.h>
#endif
#include "java_util_Date.h"
#include "java_text_DateFormat.h"
#if defined(__APPLE__) && defined(__OBJC__)
#include "CodenameOne_GLViewController.h"
#endif
#include "java_lang_StringToReal.h"

#if defined(__APPLE__) && defined(__OBJC__)
#import <mach/mach.h>
#import <mach/task_info.h>
#elif defined(__APPLE__)
// Plain-C Apple target (the clean target emits .c): the same mach and sysctl
// interfaces are available without Objective-C, and back Runtime.freeMemory().
#include <mach/mach.h>
#include <mach/task_info.h>
#include <sys/sysctl.h>
#endif

extern _Atomic JAVA_BOOLEAN lowMemoryMode;

/* Start of a String's characters, whatever backs them.
 *
 * Every read of the characters goes through here so the backing store is named in
 * ONE place. That matters because the store is about to shrink: a fused String
 * carries a 24-byte JavaArrayPrototype for a payload only that String can reach,
 * and length duplicates count, dimensions is always 1, primitiveSize is the coder
 * the class pointer already gives, dataOffset is a constant for the shape, and
 * two of the fields are GC sentinels. Across the self-hosting corpus that header
 * is ~27MB, and it is what keeps a typical String in size class 96 instead of 64.
 *
 * The coder still comes from cn1StrIsLatin1 below, which reads the class pointer
 * -- the one piece of that header carrying information. */
void* cn1StrChars(JAVA_OBJECT s) {
    JAVA_OBJECT v = ((struct obj__java_lang_String*)s)->java_lang_String_value;
    if(v == JAVA_NULL) {
        // Inline: the characters begin at the first 8-aligned byte after the fields.
        return (void*)((char*)s + ((sizeof(struct obj__java_lang_String) + 7) & ~(size_t)7));
    }
    return CN1_ARRAY_DATA((JAVA_ARRAY)v);
}
int cn1StrIsLatin1(JAVA_OBJECT s) {
    JAVA_OBJECT v = ((struct obj__java_lang_String*)s)->java_lang_String_value;
    if(v == JAVA_NULL) {
        // The twin IS the coder -- that is the whole point of carrying it in the
        // class word instead of a header around the payload.
        return s->__codenameOneParentClsReference == &class__java_lang_String_i8;
    }
    return ((JAVA_ARRAY)v)->__codenameOneParentClsReference == &class_array1__JAVA_BYTE;
}

// Compact-string: logical char at index i of String s, decoding Latin-1 or UTF-16.
// Reads the store only through the two helpers above.
JAVA_CHAR cn1StrCharAtRaw(JAVA_OBJECT s, JAVA_INT i) {
    if (cn1StrIsLatin1(s)) {
        return (JAVA_CHAR)(((JAVA_ARRAY_BYTE*)cn1StrChars(s))[i] & 0xff);
    }
    return ((JAVA_ARRAY_CHAR*)cn1StrChars(s))[i];
}

// Copyright (c) 2008-2009 Bjoern Hoehrmann <bjoern@hoehrmann.de>
// See http://bjoern.hoehrmann.de/utf-8/decoder/dfa/ for details.

#define UTF8_ACCEPT 0
#define UTF8_REJECT 1
#define USE_DFA_UTF8_DECODER

static const uint8_t utf8d[] = {
  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, // 00..1f
  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, // 20..3f
  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, // 40..5f
  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0, // 60..7f
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9, // 80..9f
  7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7, // a0..bf
  8,8,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2, // c0..df
  0xa,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x3,0x4,0x3,0x3, // e0..ef
  0xb,0x6,0x6,0x6,0x5,0x8,0x8,0x8,0x8,0x8,0x8,0x8,0x8,0x8,0x8,0x8, // f0..ff
  0x0,0x1,0x2,0x3,0x5,0x8,0x7,0x1,0x1,0x1,0x4,0x6,0x1,0x1,0x1,0x1, // s0..s0
  1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,0,1,0,1,1,1,1,1,1, // s1..s2
  1,2,1,1,1,1,1,2,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1, // s3..s4
  1,2,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,3,1,3,1,1,1,1,1,1, // s5..s6
  1,3,1,1,1,1,1,3,1,3,1,1,1,1,1,1,1,3,1,1,1,1,1,1,1,1,1,1,1,1,1,1, // s7..s8
};

static uint32_t inline
decode(uint32_t* state, uint32_t* codep, uint32_t byte) {
  uint32_t type = utf8d[byte];

  *codep = (*state != UTF8_ACCEPT) ?
    (byte & 0x3fu) | (*codep << 6) :
    (0xff >> type) & (byte);

  *state = utf8d[256 + *state*16 + type];
  return *state;
}

// Surrogate-pair / supplementary-codepoint boundaries used when emitting
// UTF-16 char arrays for the Java String layout.
#define CN1_REPLACEMENT_CHAR 0xFFFD
#define CN1_MIN_HIGH_SURROGATE 0xD800
#define CN1_MIN_LOW_SURROGATE  0xDC00
#define CN1_MIN_SUPPLEMENTARY_CODEPOINT 0x10000

// The NEON ASCII fast-path only kicks in once the source is long enough that
// the 16-byte vector cost amortises; shorter inputs stay on the scalar DFA.
#define CN1_UTF8_NEON_MIN_LEN 64

typedef enum {
    CN1_ENC_UTF8 = 0,
    CN1_ENC_US_ASCII,
    CN1_ENC_UTF16,
    CN1_ENC_ISO_8859_1,
    CN1_ENC_ISO_8859_2,
    CN1_ENC_UNKNOWN
} cn1_encoding_t;

extern JAVA_BOOLEAN compareStringToCharArray(const char* str, JAVA_ARRAY_CHAR* chrs, int length);

static cn1_encoding_t cn1_resolve_encoding_from_chars(JAVA_ARRAY_CHAR* chars, int len) {
    if (chars == NULL || len == 0) {
        return CN1_ENC_UTF8;
    }
    if (compareStringToCharArray("UTF-8", chars, len) ||
        compareStringToCharArray("UTF8",  chars, len)) {
        return CN1_ENC_UTF8;
    }
    if (compareStringToCharArray("US-ASCII", chars, len) ||
        compareStringToCharArray("ASCII",    chars, len)) {
        return CN1_ENC_US_ASCII;
    }
    if (compareStringToCharArray("UTF-16", chars, len) ||
        compareStringToCharArray("UTF16",  chars, len)) {
        return CN1_ENC_UTF16;
    }
    if (compareStringToCharArray("ISO-8859-1", chars, len) ||
        compareStringToCharArray("ISO8859-1",  chars, len) ||
        compareStringToCharArray("LATIN1",     chars, len)) {
        return CN1_ENC_ISO_8859_1;
    }
    if (compareStringToCharArray("ISO-8859-2", chars, len) ||
        compareStringToCharArray("ISO8859-2",  chars, len) ||
        compareStringToCharArray("LATIN2",     chars, len)) {
        return CN1_ENC_ISO_8859_2;
    }
    return CN1_ENC_UNKNOWN;
}

#if defined(__APPLE__) && defined(__OBJC__)
static NSStringEncoding cn1_nsencoding_for(cn1_encoding_t enc) {
    switch (enc) {
        case CN1_ENC_UTF8:       return NSUTF8StringEncoding;
        case CN1_ENC_US_ASCII:   return NSASCIIStringEncoding;
        case CN1_ENC_UTF16:      return NSUTF16StringEncoding;
        case CN1_ENC_ISO_8859_1: return NSISOLatin1StringEncoding;
        case CN1_ENC_ISO_8859_2: return NSISOLatin2StringEncoding;
        default:                 return NSUTF8StringEncoding;
    }
}
#endif

#if defined(__ARM_NEON)
#include <arm_neon.h>

// Returns the count of leading bytes in src that have the high bit clear.
// Scans 16 bytes per iteration with NEON, falls back to scalar for the tail.
static size_t cn1_utf8_ascii_prefix_neon(const uint8_t* src, size_t len) {
    size_t i = 0;
    while (i + 16 <= len) {
        uint8x16_t v = vld1q_u8(src + i);
        if (vmaxvq_u8(v) >= 0x80) {
            break;
        }
        i += 16;
    }
    while (i < len && (src[i] & 0x80) == 0) {
        i++;
    }
    return i;
}

// Widens `len` ASCII bytes into JAVA_ARRAY_CHAR (uint16_t) slots using NEON
// u8 -> u16 promotion. Caller guarantees every byte is < 0x80.
static void cn1_utf8_widen_ascii_neon(const uint8_t* src, JAVA_ARRAY_CHAR* dst, size_t len) {
    size_t i = 0;
    while (i + 16 <= len) {
        uint8x16_t v = vld1q_u8(src + i);
        uint16x8_t lo = vmovl_u8(vget_low_u8(v));
        uint16x8_t hi = vmovl_u8(vget_high_u8(v));
        vst1q_u16((uint16_t*)(dst + i), lo);
        vst1q_u16((uint16_t*)(dst + i + 8), hi);
        i += 16;
    }
    while (i < len) {
        dst[i] = (JAVA_ARRAY_CHAR)src[i];
        i++;
    }
}
#endif

// JDK-compatible UTF-16 -> UTF-8 encode. Reads JAVA_ARRAY_CHAR units, joins
// well-formed surrogate pairs, and emits the canonical 1/2/3/4-byte UTF-8
// sequence. Unpaired surrogates are encoded as U+FFFD (matching the JDK
// encoder's REPLACE behaviour). When `out` is NULL only the output length is
// computed -- callers use this as the size pass before allocating.
static size_t cn1_utf8_encode_chars(const JAVA_ARRAY_CHAR* src, size_t len, JAVA_ARRAY_BYTE* out) {
    size_t outLen = 0;
    size_t i = 0;
    while (i < len) {
        uint32_t cp = (uint32_t)src[i++];
        if (cp >= 0xD800 && cp <= 0xDBFF) {
            // High surrogate -- combine with the following low surrogate.
            if (i < len) {
                uint32_t low = (uint32_t)src[i];
                if (low >= 0xDC00 && low <= 0xDFFF) {
                    cp = 0x10000 + ((cp - 0xD800) << 10) + (low - 0xDC00);
                    i++;
                } else {
                    cp = CN1_REPLACEMENT_CHAR;
                }
            } else {
                cp = CN1_REPLACEMENT_CHAR;
            }
        } else if (cp >= 0xDC00 && cp <= 0xDFFF) {
            // Lone low surrogate.
            cp = CN1_REPLACEMENT_CHAR;
        }
        if (cp < 0x80) {
            if (out) out[outLen] = (JAVA_ARRAY_BYTE)cp;
            outLen += 1;
        } else if (cp < 0x800) {
            if (out) {
                out[outLen]     = (JAVA_ARRAY_BYTE)(0xC0 | (cp >> 6));
                out[outLen + 1] = (JAVA_ARRAY_BYTE)(0x80 | (cp & 0x3F));
            }
            outLen += 2;
        } else if (cp < 0x10000) {
            if (out) {
                out[outLen]     = (JAVA_ARRAY_BYTE)(0xE0 | (cp >> 12));
                out[outLen + 1] = (JAVA_ARRAY_BYTE)(0x80 | ((cp >> 6) & 0x3F));
                out[outLen + 2] = (JAVA_ARRAY_BYTE)(0x80 | (cp & 0x3F));
            }
            outLen += 3;
        } else {
            if (out) {
                out[outLen]     = (JAVA_ARRAY_BYTE)(0xF0 | (cp >> 18));
                out[outLen + 1] = (JAVA_ARRAY_BYTE)(0x80 | ((cp >> 12) & 0x3F));
                out[outLen + 2] = (JAVA_ARRAY_BYTE)(0x80 | ((cp >> 6) & 0x3F));
                out[outLen + 3] = (JAVA_ARRAY_BYTE)(0x80 | (cp & 0x3F));
            }
            outLen += 4;
        }
    }
    return outLen;
}

// JDK-compatible UTF-8 -> UTF-16 decode using the Hoehrmann DFA.
// On malformed input emits a single U+FFFD per maximal-subpart violation and
// resumes decoding (matches CodingErrorAction.REPLACE in StandardCharsets).
// When `out` is NULL only the output length is computed -- the caller uses
// this for the size pass before allocating the destination char array.
static size_t cn1_utf8_decode_replace(const uint8_t* src, size_t len, JAVA_ARRAY_CHAR* out) {
    size_t outLen = 0;
    uint32_t state = UTF8_ACCEPT;
    uint32_t codepoint = 0;
    size_t i = 0;
    while (i < len) {
        uint32_t prev = state;
        decode(&state, &codepoint, src[i]);
        if (state == UTF8_ACCEPT) {
            if (codepoint >= CN1_MIN_SUPPLEMENTARY_CODEPOINT) {
                if (out) {
                    out[outLen]     = (JAVA_ARRAY_CHAR)(CN1_MIN_HIGH_SURROGATE +
                            (((codepoint - CN1_MIN_SUPPLEMENTARY_CODEPOINT) >> 10) & 0x3FF));
                    out[outLen + 1] = (JAVA_ARRAY_CHAR)(CN1_MIN_LOW_SURROGATE +
                            ((codepoint - CN1_MIN_SUPPLEMENTARY_CODEPOINT) & 0x3FF));
                }
                outLen += 2;
            } else {
                if (out) out[outLen] = (JAVA_ARRAY_CHAR)codepoint;
                outLen++;
            }
            i++;
        } else if (state == UTF8_REJECT) {
            if (out) out[outLen] = (JAVA_ARRAY_CHAR)CN1_REPLACEMENT_CHAR;
            outLen++;
            state = UTF8_ACCEPT;
            codepoint = 0;
            // If the rejecting byte was itself an invalid leading byte
            // (prev == ACCEPT) consume it; otherwise re-feed so the byte
            // that broke an incomplete sequence still starts a new char.
            if (prev == UTF8_ACCEPT) {
                i++;
            }
        } else {
            // Continuation byte that did not yet complete a codepoint.
            i++;
        }
    }
    if (state != UTF8_ACCEPT) {
        // Truncated trailing sequence at end of input.
        if (out) out[outLen] = (JAVA_ARRAY_CHAR)CN1_REPLACEMENT_CHAR;
        outLen++;
    }
    return outLen;
}


/*
 * The class representing classes
 */
struct clazz ClazzClazz = {
    DEBUG_GC_INIT 0, 0, 0, 0, 0, 0, 0, cn1_array_start_offset, "java.lang.Class", JAVA_FALSE, 0, 0, JAVA_FALSE, &class__java_lang_Object, EMPTY_INTERFACES, 0, 0, 0
};


JAVA_BOOLEAN compareStringToCharArray(const char* str, JAVA_ARRAY_CHAR* chrs, int length) {
    if(strlen(str) != length) {
        return JAVA_FALSE;
    }
    for(int iter = 0 ; iter < length ; iter++) {
        if(toupper(chrs[iter]) != str[iter]) {
            return JAVA_FALSE;
        }
    }
    return JAVA_TRUE;
}

JAVA_VOID java_lang_String_releaseNSString___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG ns) {
#if defined(__APPLE__) && defined(__OBJC__)
    if(ns != 0) {
        // this prevents a race condition where the string might get GC'd and the NSString is still pending
        // on a call in the native thread
        dispatch_async(dispatch_get_main_queue(), ^{
            NSString* n = (NSString*)ns;
            [n release];
        });
    }
#endif
}

static inline __attribute__((always_inline)) JAVA_BOOLEAN cn1StringEquals(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {
    if(__cn1ThisObject == __cn1Arg1) {
        return JAVA_TRUE;
    }
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(__cn1ThisObject == JAVA_NULL) {
        THROW_NULL_POINTER_EXCEPTION();
    }
#endif
    if(__cn1Arg1 == JAVA_NULL || CN1_CLASS_OF(__cn1Arg1)->classId != __cn1ThisObject->__codenameOneParentClsReference->classId) {
        return JAVA_FALSE;
    }
    struct obj__java_lang_String* t = (struct obj__java_lang_String*)__cn1ThisObject;
    struct obj__java_lang_String* o = (struct obj__java_lang_String*)__cn1Arg1;
    if(t->java_lang_String_count != o->java_lang_String_count) {
        return JAVA_FALSE;
    }
    // cached-hash precheck: two computed-and-cached unequal hashes prove
    // inequality without touching the character data (big win for string keys
    // in maps, where equals runs right after both hashes were computed).
    JAVA_INT th = t->java_lang_String_hashCode;
    JAVA_INT oh = o->java_lang_String_hashCode;
    if(th != 0 && oh != 0 && th != oh) {
        return JAVA_FALSE;
    }

    if(cn1StrIsLatin1(__cn1ThisObject) && cn1StrIsLatin1(__cn1Arg1)) {
        const uint8_t* a = (const uint8_t*)cn1StrChars((JAVA_OBJECT)t);
        const uint8_t* b = (const uint8_t*)cn1StrChars((JAVA_OBJECT)o);
        return cn1CompactBytesEqual(a, b, (size_t)t->java_lang_String_count);
    }
    // Fast path: both backing arrays are char[] -- byte-equality of UTF-16 code
    // units == string equality; libc memcmp is the SIMD-optimized comparison on
    // every target.
    if(!cn1StrIsLatin1(__cn1ThisObject) && !cn1StrIsLatin1(__cn1Arg1)) {
        JAVA_ARRAY_CHAR* oa = ((JAVA_ARRAY_CHAR*)cn1StrChars((JAVA_OBJECT)o));
        JAVA_ARRAY_CHAR* ta = ((JAVA_ARRAY_CHAR*)cn1StrChars((JAVA_OBJECT)t));
        return cn1CompactBytesEqual(ta, oa, (size_t)t->java_lang_String_count * sizeof(JAVA_ARRAY_CHAR));
    }
    return cn1StrIsLatin1(__cn1ThisObject)
        ? cn1CompactMixedEquals((const uint8_t*)cn1StrChars((JAVA_OBJECT)t),
                (const uint16_t*)cn1StrChars((JAVA_OBJECT)o), (size_t)t->java_lang_String_count)
        : cn1CompactMixedEquals((const uint8_t*)cn1StrChars((JAVA_OBJECT)o),
                (const uint16_t*)cn1StrChars((JAVA_OBJECT)t), (size_t)t->java_lang_String_count);

}

JAVA_BOOLEAN java_lang_String_equals___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {
    return cn1StringEquals(threadStateData, __cn1ThisObject, __cn1Arg1);
}

JAVA_INT java_lang_String_compareTo___java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {
    if(__cn1ThisObject == __cn1Arg1) {
        return 0;
    }
    struct obj__java_lang_String* t = (struct obj__java_lang_String*)__cn1ThisObject;
    struct obj__java_lang_String* o = (struct obj__java_lang_String*)__cn1Arg1;
    JAVA_INT tc = t->java_lang_String_count;
    JAVA_INT oc = o->java_lang_String_count;
    JAVA_INT minL = tc < oc ? tc : oc;
    if(cn1StrIsLatin1(__cn1ThisObject) && cn1StrIsLatin1(__cn1Arg1)) {
        const uint8_t* a = (const uint8_t*)cn1StrChars((JAVA_OBJECT)t);
        const uint8_t* b = (const uint8_t*)cn1StrChars((JAVA_OBJECT)o);
        JAVA_INT i = 0;
        // memcmp only promises the sign of its answer. Skip equal blocks, then
        // return the exact unsigned character difference required by Java.
        for(; i <= minL - 8; i += 8) {
            uint64_t x, y;
            memcpy(&x, a + i, sizeof(x)); memcpy(&y, b + i, sizeof(y));
            if(x != y) break;
        }
        for(; i < minL; i++) if(a[i] != b[i]) return (JAVA_INT)a[i] - (JAVA_INT)b[i];
        return tc - oc;
    }
    // Fast path: both backing arrays are char[] -- find the first differing
    // 4-char block with 64-bit compares, then resolve the exact code unit inside
    // it (UTF-16 code-unit order, like Java).
    if(!cn1StrIsLatin1(__cn1ThisObject) && !cn1StrIsLatin1(__cn1Arg1)) {
        const JAVA_ARRAY_CHAR* ta = ((JAVA_ARRAY_CHAR*)cn1StrChars((JAVA_OBJECT)t));
        const JAVA_ARRAY_CHAR* oa = ((JAVA_ARRAY_CHAR*)cn1StrChars((JAVA_OBJECT)o));
        JAVA_INT i = 0;
        for(; i + 4 <= minL; i += 4) {
            uint64_t a, b;
            memcpy(&a, ta + i, 8);
            memcpy(&b, oa + i, 8);
            if(a != b) {
                break;
            }
        }
        for(; i < minL; i++) {
            if(ta[i] != oa[i]) {
                return (JAVA_INT)ta[i] - (JAVA_INT)oa[i];
            }
        }
        return tc - oc;
    }
    // Coder-aware path: at least one string is Latin-1 (byte[]); compare logical
    // chars. Same UTF-16 code-unit ordering, bit-identical to the char[] path.
    for(JAVA_INT k = 0; k < minL; k++) {
        int d = (int)cn1StrCharAtRaw(__cn1ThisObject, k) - (int)cn1StrCharAtRaw(__cn1Arg1, k);
        if(d) {
            return d;
        }
    }
    return tc - oc;
}

JAVA_INT java_lang_Character_toLowerCase___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT __cn1Arg1) {
    if ('A' <= __cn1Arg1 && __cn1Arg1 <= 'Z') {
        return (JAVA_CHAR) (__cn1Arg1 + ('a' - 'A'));
    }
    return __cn1Arg1;
}

JAVA_CHAR java_lang_Character_toLowerCase___char_R_char(CODENAME_ONE_THREAD_STATE, JAVA_INT __cn1Arg1) {
    if ('A' <= __cn1Arg1 && __cn1Arg1 <= 'Z') {
        return (JAVA_CHAR) (__cn1Arg1 + ('a' - 'A'));
    }
    return __cn1Arg1;
}

JAVA_BOOLEAN java_lang_String_equalsIgnoreCase___java_lang_String_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {
    if(__cn1ThisObject == __cn1Arg1) {
        return JAVA_TRUE;
    }
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(__cn1ThisObject == JAVA_NULL) {
        THROW_NULL_POINTER_EXCEPTION();
    }
#endif
    if(__cn1Arg1 == JAVA_NULL || CN1_CLASS_OF(__cn1Arg1)->classId != __cn1ThisObject->__codenameOneParentClsReference->classId) {
        return JAVA_FALSE;
    }
    struct obj__java_lang_String* t = (struct obj__java_lang_String*)__cn1ThisObject;
    struct obj__java_lang_String* o = (struct obj__java_lang_String*)__cn1Arg1;
    if(t->java_lang_String_count != o->java_lang_String_count) {
        return JAVA_FALSE;
    }
    
    // Fast path: both backing arrays are char[]; index directly.
    if(!cn1StrIsLatin1(__cn1ThisObject) && !cn1StrIsLatin1(__cn1Arg1)) {
        JAVA_ARRAY_CHAR* oa = (JAVA_ARRAY_CHAR*)cn1StrChars((JAVA_OBJECT)o);
        JAVA_ARRAY_CHAR* ta = (JAVA_ARRAY_CHAR*)cn1StrChars((JAVA_OBJECT)t);
        JAVA_INT oo = 0;
        JAVA_INT to = 0;

        for(int iter = 0 ; iter < t->java_lang_String_count ; iter++) {
            JAVA_ARRAY_CHAR jo = oa[iter+oo];
            JAVA_ARRAY_CHAR jt = ta[iter+to];
            if ('A' <= jo && jo <= 'Z') {
                jo = (JAVA_ARRAY_CHAR) (jo + ('a' - 'A'));
            }
            if ('A' <= jt && jt <= 'Z') {
                jt = (JAVA_ARRAY_CHAR) (jt + ('a' - 'A'));
            }
            if(jo != jt) {
                return JAVA_FALSE;
            }
        }
        return JAVA_TRUE;
    }
    // Coder-aware path: at least one string is Latin-1 (byte[]); read logical
    // chars and apply the same ASCII case fold. Bit-identical to the char[] path.
    for(int iter = 0 ; iter < t->java_lang_String_count ; iter++) {
        JAVA_ARRAY_CHAR jo = cn1StrCharAtRaw(__cn1Arg1, iter);
        JAVA_ARRAY_CHAR jt = cn1StrCharAtRaw(__cn1ThisObject, iter);
        if ('A' <= jo && jo <= 'Z') {
            jo = (JAVA_ARRAY_CHAR) (jo + ('a' - 'A'));
        }
        if ('A' <= jt && jt <= 'Z') {
            jt = (JAVA_ARRAY_CHAR) (jt + ('a' - 'A'));
        }
        if(jo != jt) {
            return JAVA_FALSE;
        }
    }
    return JAVA_TRUE;
}

JAVA_INT java_lang_String_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
    struct obj__java_lang_String* t = (struct obj__java_lang_String*)__cn1ThisObject;
    JAVA_INT hash = t->java_lang_String_hashCode;
    if (hash == 0) {
        if (t->java_lang_String_count == 0) {
            return 0;
        }
        JAVA_INT end = t->java_lang_String_count;
        JAVA_INT i = 0;
        // 4-way polynomial reassociation: h = h*31^4 + c0*31^3 + c1*31^2 + c2*31 + c3.
        // The naive loop is a serially-dependent multiply chain (one 31*h per char);
        // this breaks the dependency so the four products issue in parallel.
        // -fwrapv makes the int overflow wrap exactly like Java's.
        if(cn1StrIsLatin1(__cn1ThisObject)) {
            // Latin-1: (b & 0xff) IS the char value, so the hash is bit-identical
            // to the same text stored as char[]. Mirrors cn1_intrinsics.h.
            JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)cn1StrChars(__cn1ThisObject);
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
            JAVA_ARRAY_CHAR* chars = (JAVA_ARRAY_CHAR*)cn1StrChars(__cn1ThisObject);
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

JAVA_OBJECT java_lang_reflect_Array_newInstanceImpl___java_lang_Class_int_R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls, JAVA_INT len) {
    enteringNativeAllocations();
    struct clazz* clz = (struct clazz*)cls;
    if (clz->arrayClass == 0) {
        JAVA_OBJECT ex = __NEW_java_lang_RuntimeException(CN1_THREAD_STATE_PASS_SINGLE_ARG);
        java_lang_RuntimeException___INIT_____java_lang_String(CN1_THREAD_STATE_PASS_ARG ex, newStringFromCString(CN1_THREAD_STATE_PASS_ARG "Attempt to create array with reflection, but the component class has no registered array class"));
        finishedNativeAllocations();
        throwException(threadStateData, ex);
        return NULL;
    }
    JAVA_OBJECT out = allocArray(CN1_THREAD_STATE_PASS_ARG len, clz->arrayClass, sizeof(JAVA_OBJECT), 1);
    finishedNativeAllocations();
    return out;
}

JAVA_OBJECT java_lang_String_bytesToChars___byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len, JAVA_OBJECT encoding) {
    enteringNativeAllocations();
    JAVA_ARRAY_BYTE* sourceData = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)b) + off;

    // `encoding` is a java.lang.String whose backing array may be a compact
    // Latin-1 byte[]; decode the (short, ASCII) encoding name logically into a
    // temporary char buffer so cn1_resolve_encoding_from_chars sees code units.
    JAVA_ARRAY_CHAR encBuf[64];
    JAVA_ARRAY_CHAR* encChars = NULL;
    int encLen = 0;
    if (encoding != JAVA_NULL) {
        struct obj__java_lang_String* encString = (struct obj__java_lang_String*)encoding;
        encLen = encString->java_lang_String_count;
        if (encLen > (int)(sizeof(encBuf)/sizeof(encBuf[0]))) {
            encLen = (int)(sizeof(encBuf)/sizeof(encBuf[0]));
        }
        for (int i = 0; i < encLen; i++) {
            encBuf[i] = cn1StrCharAtRaw(encoding, i);
        }
        encChars = encBuf;
    }
    cn1_encoding_t enc = cn1_resolve_encoding_from_chars(encChars, encLen);

    // US-ASCII: bytes < 0x80 map to char, anything else becomes U+FFFD --
    // matches JDK CharsetDecoder.REPLACE on StandardCharsets.US_ASCII.
    if (enc == CN1_ENC_US_ASCII) {
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_CHAR(threadStateData, len);
        JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
        for (int iter = 0; iter < len; iter++) {
            uint8_t v = (uint8_t)sourceData[iter];
            dest[iter] = (v < 0x80) ? (JAVA_ARRAY_CHAR)v : (JAVA_ARRAY_CHAR)CN1_REPLACEMENT_CHAR;
        }
        finishedNativeAllocations();
        return destArr;
    }

    // ISO-8859-1: every byte maps 1:1 to char.
    if (enc == CN1_ENC_ISO_8859_1) {
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_CHAR(threadStateData, len);
        JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
        for (int iter = 0; iter < len; iter++) {
            dest[iter] = (JAVA_ARRAY_CHAR)(uint8_t)sourceData[iter];
        }
        finishedNativeAllocations();
        return destArr;
    }

    if (enc == CN1_ENC_UTF8) {
        const uint8_t* src = (const uint8_t*)sourceData;
        size_t srcLen = (size_t)len;
        size_t asciiPrefix = 0;

#if defined(__ARM_NEON)
        if (srcLen >= CN1_UTF8_NEON_MIN_LEN) {
            asciiPrefix = cn1_utf8_ascii_prefix_neon(src, srcLen);
        }
#endif

        if (asciiPrefix == srcLen) {
            // Whole input is ASCII -- single allocation + vector widen.
            JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_CHAR(threadStateData, (JAVA_INT)srcLen);
            JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
#if defined(__ARM_NEON)
            cn1_utf8_widen_ascii_neon(src, dest, srcLen);
#else
            for (size_t k = 0; k < srcLen; k++) {
                dest[k] = (JAVA_ARRAY_CHAR)src[k];
            }
#endif
            finishedNativeAllocations();
            return destArr;
        }

        // Mixed: count the tail with the DFA, allocate exactly, then decode.
        size_t tailLen = cn1_utf8_decode_replace(src + asciiPrefix, srcLen - asciiPrefix, NULL);
        size_t total = asciiPrefix + tailLen;
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_CHAR(threadStateData, (JAVA_INT)total);
        JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
        if (asciiPrefix > 0) {
#if defined(__ARM_NEON)
            cn1_utf8_widen_ascii_neon(src, dest, asciiPrefix);
#else
            for (size_t k = 0; k < asciiPrefix; k++) {
                dest[k] = (JAVA_ARRAY_CHAR)src[k];
            }
#endif
        }
        cn1_utf8_decode_replace(src + asciiPrefix, srcLen - asciiPrefix, dest + asciiPrefix);
        finishedNativeAllocations();
        return destArr;
    }

#if defined(__APPLE__) && defined(__OBJC__)
    // UTF-16, ISO-8859-2 and unknown encodings go through NSString. When the
    // native decoder rejects the input we no longer silently re-decode as
    // Latin-1 (that masked encoding errors); instead we map bytes < 0x80
    // straight through and replace high-bit bytes with U+FFFD.
    NSStringEncoding nsEnc = cn1_nsencoding_for(enc);
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* nsStr = [[NSString alloc] initWithBytes:sourceData length:len encoding:nsEnc];
    if (nsStr == nil) {
        [pool release];
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_CHAR(threadStateData, len);
        JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
        for (int iter = 0; iter < len; iter++) {
            uint8_t v = (uint8_t)sourceData[iter];
            dest[iter] = (v < 0x80) ? (JAVA_ARRAY_CHAR)v : (JAVA_ARRAY_CHAR)CN1_REPLACEMENT_CHAR;
        }
        finishedNativeAllocations();
        return destArr;
    }

    JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_CHAR(threadStateData, [nsStr length]);
    __block JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
    __block int length = 0;
    [nsStr enumerateSubstringsInRange:NSMakeRange(0, [nsStr length])
                               options:NSStringEnumerationByComposedCharacterSequences
                            usingBlock:^(NSString *substring, NSRange substringRange, NSRange enclosingRange, BOOL *stop) {
                                unichar ch = [nsStr characterAtIndex:length];
                                dest[length] = (JAVA_ARRAY_CHAR)ch;
                                length++;
                                if([substring length] > 1) {
                                    // we have surrogate pairs here...
                                    ch = [substring characterAtIndex:1];
                                    dest[length] = (JAVA_ARRAY_CHAR)ch;
                                    length++;
                                }
                            }];

    [nsStr release];
    [pool release];
    finishedNativeAllocations();
    return destArr;
#else
    // POSIX/test build: everything that is not UTF-8 / ASCII / Latin-1 falls
    // through here. Widen bytes 1:1 (Latin-1-ish) so test coverage stays
    // exercised without pulling in Apple's full encoding catalogue.
    JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_CHAR(threadStateData, len);
    JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
    for (int iter = 0; iter < len; iter++) {
        dest[iter] = (JAVA_ARRAY_CHAR)(uint8_t)sourceData[iter];
    }
    finishedNativeAllocations();
    return destArr;
#endif
}

JAVA_OBJECT java_io_InputStreamReader_bytesToChars___byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len, JAVA_OBJECT encoding) {
    return java_lang_String_bytesToChars___byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(threadStateData, b, off, len, encoding);
}

JAVA_BOOLEAN isAsciiArray(JAVA_ARRAY sourceArr) {
    JAVA_ARRAY_CHAR* arr = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)sourceArr);
    for(int iter = 0 ; iter < sourceArr->length ; iter++) {
        if(arr[iter] > 127) {
            return JAVA_FALSE;
        }
    }
    return JAVA_TRUE;
}

// A compact String never needs a temporary UTF-16 array for these encodings.
// NULL selects the existing Java fallback for other encodings or huge inputs.
JAVA_OBJECT java_lang_String_compactBytes___java_lang_String_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner, JAVA_OBJECT encoding) {
    CN1_KEEP_NATIVE_OWNER(sourceOwner, owner);
    struct obj__java_lang_String* string = (struct obj__java_lang_String*)owner;
    if(!cn1StrIsLatin1(owner)) return JAVA_NULL;
    JAVA_ARRAY_CHAR encBuffer[64];
    JAVA_ARRAY_CHAR* encChars = NULL;
    int encLength = 0;
    if(encoding != JAVA_NULL) {
        encLength = ((struct obj__java_lang_String*)encoding)->java_lang_String_count;
        if(encLength > 64) return JAVA_NULL;
        for(int i = 0; i < encLength; i++) encBuffer[i] = cn1StrCharAtRaw(encoding, i);
        encChars = encBuffer;
    }
    cn1_encoding_t enc = cn1_resolve_encoding_from_chars(encChars, encLength);
    if(enc != CN1_ENC_UTF8 && enc != CN1_ENC_US_ASCII && enc != CN1_ENC_ISO_8859_1) return JAVA_NULL;
    int count = string->java_lang_String_count;
    if(count > INT_MAX / 2) return JAVA_NULL;
    const uint8_t* input = (const uint8_t*)cn1StrChars(owner);
    int length = count;
    if(enc == CN1_ENC_UTF8) {
        for(int i = 0; i < count; i++) length += input[i] >> 7;
    }
    JAVA_OBJECT result = __NEW_ARRAY_JAVA_BYTE(threadStateData, length);
    uint8_t* output = (uint8_t*)CN1_ARRAY_DATA((JAVA_ARRAY)result);
    if(enc == CN1_ENC_ISO_8859_1 || (enc == CN1_ENC_UTF8 && length == count)) {
        memcpy(output, input, (size_t)count);
    } else if(enc == CN1_ENC_US_ASCII) {
        for(int i = 0; i < count; i++) output[i] = input[i] < 128 ? input[i] : '?';
    } else {
        for(int i = 0, j = 0; i < count; i++) {
            uint8_t c = input[i];
            if(c < 128) output[j++] = c;
            else { output[j++] = 0xc0 | (c >> 6); output[j++] = 0x80 | (c & 63); }
        }
    }
    return result;
}

JAVA_OBJECT java_lang_String_charsToBytes___char_1ARRAY_char_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT arr, JAVA_OBJECT encoding) {
    JAVA_ARRAY sourceArr = (JAVA_ARRAY)arr;
    JAVA_ARRAY_CHAR* src = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)sourceArr);
    int srcLen = sourceArr->length;

    JAVA_ARRAY_CHAR* encChars = NULL;
    int encLen = 0;
    if (encoding != JAVA_NULL) {
        encChars = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)encoding);
        encLen = ((JAVA_ARRAY)encoding)->length;
    }
    cn1_encoding_t enc = cn1_resolve_encoding_from_chars(encChars, encLen);

    // ASCII fast path: every char < 0x80 maps to itself as a single byte. Both
    // UTF-8 and ISO-8859-1 agree on this byte sequence, so we can take this
    // shortcut without checking the requested encoding.
    if (isAsciiArray(sourceArr)) {
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, srcLen);
        JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
        for (int iter = 0; iter < srcLen; iter++) {
            dest[iter] = (JAVA_ARRAY_BYTE)src[iter];
        }
        return destArr;
    }

    if (enc == CN1_ENC_UTF8 || enc == CN1_ENC_UNKNOWN) {
        size_t outLen = cn1_utf8_encode_chars(src, (size_t)srcLen, NULL);
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, (JAVA_INT)outLen);
        JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
        cn1_utf8_encode_chars(src, (size_t)srcLen, dest);
        return destArr;
    }

    if (enc == CN1_ENC_US_ASCII || enc == CN1_ENC_ISO_8859_1) {
        // One replacement per unmappable code point, including a surrogate pair.
        int outLen = srcLen;
        for(int i = 0; i + 1 < srcLen; i++) {
            if(src[i] >= 0xd800 && src[i] <= 0xdbff && src[i + 1] >= 0xdc00 && src[i + 1] <= 0xdfff) {
                outLen--; i++;
            }
        }
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, outLen);
        JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
        unsigned int max = (enc == CN1_ENC_US_ASCII) ? 0x80u : 0x100u;
        for (int iter = 0, out = 0; iter < srcLen; iter++) {
            unsigned int c = (unsigned int)src[iter];
            dest[out++] = (c < max) ? (JAVA_ARRAY_BYTE)c : (JAVA_ARRAY_BYTE)'?';
            if(c >= 0xd800 && c <= 0xdbff && iter + 1 < srcLen
                    && src[iter + 1] >= 0xdc00 && src[iter + 1] <= 0xdfff) iter++;
        }
        return destArr;
    }

#if defined(__APPLE__) && defined(__OBJC__)
    // UTF-16, ISO-8859-2 etc. -- defer to NSString for the unusual encodings.
    NSStringEncoding nsEnc = cn1_nsencoding_for(enc);
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* nsStr = [[NSString alloc] initWithCharacters:CN1_ARRAY_DATA(sourceArr) length:srcLen];
    NSData* data = [nsStr dataUsingEncoding:nsEnc allowLossyConversion:YES];
    if (data == nil) {
        data = [nsStr dataUsingEncoding:NSUTF8StringEncoding allowLossyConversion:YES];
    }
    JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, [data length]);
    JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
    [data getBytes:dest length:[data length]];
    [nsStr release];
    [pool release];
    return destArr;
#else
    // POSIX/test build: encode the remaining rare cases as UTF-8 so the
    // fallback at least round-trips a Unicode payload cleanly.
    size_t outLen = cn1_utf8_encode_chars(src, (size_t)srcLen, NULL);
    JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, (JAVA_INT)outLen);
    JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)destArr);
    cn1_utf8_encode_chars(src, (size_t)srcLen, dest);
    return destArr;
#endif
}

JAVA_VOID java_lang_Throwable_fillInStack__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
    if (get_field_java_lang_Throwable_stack(__cn1ThisObject) == JAVA_NULL) {
        set_field_java_lang_Throwable_stack(java_lang_Throwable_getStack___R_java_lang_String(threadStateData, __cn1ThisObject), __cn1ThisObject);
    }
    
}

JAVA_OBJECT newline = JAVA_NULL;
JAVA_OBJECT dot = JAVA_NULL;
JAVA_OBJECT colon = JAVA_NULL;
JAVA_OBJECT indent = JAVA_NULL;


JAVA_OBJECT java_lang_Throwable_getStack___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {
    JAVA_OBJECT bld = __NEW_INSTANCE_java_lang_StringBuilder(threadStateData);
    JAVA_OBJECT classObj = java_lang_Object_getClass___R_java_lang_Class(threadStateData, me);
    JAVA_OBJECT className = java_lang_Class_getName___R_java_lang_String(threadStateData, classObj);
    java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, bld, className);
    if(newline == JAVA_NULL) {
        newline = newStringFromCString(threadStateData, "\n");
        dot = newStringFromCString(threadStateData, ".");
        colon = newStringFromCString(threadStateData, ":");
        indent = newStringFromCString(threadStateData, "    at ");
        // These strings are referenced ONLY by the C globals above -- invisible
        // to every GC root source. The old removeObjectFromHeapCollection trick
        // only immortalized objects living in the legacy table; BiBOP-resident
        // objects (all small objects, arrays included) are swept by the page
        // walk regardless, so they must be registered as real roots instead.
        cn1AddImmortalRoot(newline);
        cn1AddImmortalRoot(dot);
        cn1AddImmortalRoot(colon);
        cn1AddImmortalRoot(indent);
    }
    java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, bld, newline);

    int cn1StackOff = threadStateData->callStackOffset;
    if(cn1StackOff < 0 || cn1StackOff > CN1_MAX_STACK_CALL_DEPTH) {
        fprintf(stderr, "CN1_STACKGUARD bad callStackOffset=%d max=%d\n", cn1StackOff, CN1_MAX_STACK_CALL_DEPTH);
        fflush(stderr);
        cn1StackOff = (cn1StackOff < 0) ? 0 : CN1_MAX_STACK_CALL_DEPTH;
    }
    for(int iter = cn1StackOff - 1 ; iter >= 0 ; iter--) {
        int classId = threadStateData->callStackClass[iter];
        int methodId = threadStateData->callStackMethod[iter];
        int line = threadStateData->callStackLine[iter];

        /* Defensive: a corrupt/out-of-range frame id must never crash stack-trace
         * printing (which itself runs while reporting another failure). Skip it. */
        if(classId < 0 || classId >= CN1_CONSTANT_POOL_SIZE ||
           methodId < 0 || methodId >= CN1_CONSTANT_POOL_SIZE) {
            fprintf(stderr, "CN1_STACKGUARD bad frame iter=%d off=%d classId=%d methodId=%d line=%d poolSize=%d\n",
                iter, cn1StackOff, classId, methodId, line, (int)CN1_CONSTANT_POOL_SIZE);
            fflush(stderr);
            continue;
        }

        /* A resolved constant-pool entry can be transiently NULL (observed on the
         * native Windows clean target). Appending a NULL String dereferences it
         * (count at offset 0x3C) and crashes the very stack-trace printer that is
         * reporting another failure -- so guard every append against NULL. */
        JAVA_OBJECT clsStr = STRING_FROM_CONSTANT_POOL_OFFSET(classId);
        JAVA_OBJECT mtdStr = STRING_FROM_CONSTANT_POOL_OFFSET(methodId);
        if(clsStr == JAVA_NULL || mtdStr == JAVA_NULL) {
            fprintf(stderr, "CN1_STACKGUARD null pool string iter=%d off=%d classId=%d(%s) methodId=%d(%s) line=%d\n",
                iter, cn1StackOff, classId, clsStr == JAVA_NULL ? "null" : "ok",
                methodId, mtdStr == JAVA_NULL ? "null" : "ok", line);
            fflush(stderr);
            continue;
        }

        java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, bld, indent);

        java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, bld, clsStr);

        java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, bld, dot);

        java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, bld, mtdStr);

        java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, bld, colon);

        java_lang_StringBuilder_append___int_R_java_lang_StringBuilder(threadStateData, bld, line);
        
        java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, bld, newline);
    }
    JAVA_OBJECT o = java_lang_StringBuilder_toString___R_java_lang_String(threadStateData, bld);
    return o;
}

JAVA_VOID java_io_NSLogOutputStream_write___byte_1ARRAY_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_OBJECT b, JAVA_INT off, JAVA_INT len) {
    if(b == JAVA_NULL) { THROW_NULL_POINTER_EXCEPTION(); return; }
    JAVA_ARRAY a = (JAVA_ARRAY)b;
    if(off < 0 || len < 0 || off > a->length - len) { THROW_ARRAY_INDEX_EXCEPTION(off); return; }
    if(len == 0) return;
    CN1_KEEP_NATIVE_OWNER(outputOwner, b);
    JAVA_ARRAY_BYTE* arr = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(a) + off;
#if defined(__APPLE__) && defined(__OBJC__)
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSData * data = [NSData dataWithBytes:arr length:len];
    NSString* str = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    
    // otherwise this produces a security warning in the compiler
    NSLog(@"%@", str);
    
    // if we disable arc we will need to re-enable these
    [str release];
    [pool release];
#else
    // Clean-target stdout is unbuffered. A putchar loop therefore makes a
    // system call per byte; preserve immediate output with one bulk write.
    fwrite(arr, 1, (size_t)len, stdout);
#endif
}

JAVA_VOID java_lang_System_arraycopy___java_lang_Object_int_java_lang_Object_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT src, JAVA_INT srcOffset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    __STATIC_INITIALIZER_java_lang_System(threadStateData);
    JAVA_ARRAY srcArr = (JAVA_ARRAY)src;
    JAVA_ARRAY dstArr = (JAVA_ARRAY)dst;
    if (src == JAVA_NULL || dst == JAVA_NULL) {
        THROW_NULL_POINTER_EXCEPTION();
        return;
    }
    if (srcOffset < 0 || dstOffset < 0 || srcOffset + length > srcArr->length || dstOffset + length > dstArr->length || length < 0) {
        THROW_ARRAY_INDEX_EXCEPTION(-1);
        return;
    }
    struct clazz* cls = (*srcArr).__codenameOneParentClsReference;
    int byteSize = byteSizeForArray(cls);
    // SATB barrier, BOTH halves: an object arraycopy replaces dst[dstOffset..+length)
    // with a bulk memmove that bypasses the per-element setter, so neither half fires on
    // its own. No-op (one flag load) off-GC.
    //
    // The DELETION half preserves the references being overwritten, for the usual
    // snapshot reason.
    //
    // The INSERTION half preserves the references being written IN, and it was missing.
    // That half exists (see CN1_WRITE_BARRIER in cn1_globals.h) specifically to keep an
    // object alive when "the container it is stored into is a fresh grace object not yet
    // reachable" -- so copying into a freshly allocated Object[] during a mark, and then
    // dropping the source, left the copied-in objects unmarked. The BiBOP grace pass
    // covers most of that window by walking fresh slots, but not a destination allocated
    // after the walk has passed its page, and not the belt/fixpoint phases that run after
    // it. The result is a live object swept with a surviving reference to it -- the
    // container->content class of crash the insertion half was added for.
    //
    // Both reads happen BEFORE the memmove, which is also what makes this correct for the
    // overlapping src/dst that arraycopy is contractually required to support.
    // ONE registration around BOTH halves. Bracketing each range separately would let the
    // in-flight count fall to zero between them, and the collector can clear gcSatbActive,
    // see zero and finish its final drain in that gap -- after which the insertion half
    // logs nothing while the memmove below publishes those references regardless. See
    // cn1SatbBulkBegin.
    // NO FLAG PRECHECK. Sampling gcSatbActive out here is unsafe at BOTH ends of a mark:
    // termination clears and re-raises it during the trial-clear protocol, and startup arms
    // it without waiting for a copy that already looked. Either way the copy skips the
    // barrier and then publishes into a live mark. Registering first and reading the flags
    // while registered is the whole point of the protocol, and it is what lets
    // codenameOneGCMark and mark termination both wait for an in-flight copy.
    //
    // The cost lands only on OBJECT arrays: cls->primitiveType is a load and a branch, and
    // it rejects the byte[]/char[] copies that dominate arraycopy traffic before any atomic
    // is executed.
    // The bracket spans the memmove below, not just the logging: the registration is what
    // mark startup and mark termination wait on, so releasing it before the copy would let
    // a scan interleave with the publication. See cn1SatbBulkBegin.
    JAVA_BOOLEAN cn1__satbReg = JAVA_FALSE;
    if(!cls->primitiveType) {
        cn1__satbReg = JAVA_TRUE;
        if(cn1SatbBulkBegin()) {
            // One acquisition of the SATB mutex per 256 references rather than per
            // reference; this used to be two locked enqueues per element. See
            // cn1SatbEnqueueRangeLocked.
            if(srcArr == dstArr) {
                // SAME ARRAY -- the overlap-safe shift the ASM tree classes and any
                // array-backed list perform. Every overwritten slot is logged, not just
                // the ones whose value leaves the array: a marker can be scanning this
                // array while the memmove runs and miss the element the move carries
                // past it. The insertion half is still owed nothing, because every value
                // written was already in this array. See the comment above
                // cn1SatbBulkEnd in cn1_globals.h.
                cn1SatbEnqueueRangeLocked(((JAVA_ARRAY_OBJECT*)CN1_ARRAY_DATA(dstArr)) + dstOffset, length);
            } else {
                // TWO ARRAYS: nothing is preserved by the copy, so every overwritten
                // slot is owed, and the copy really does publish references into an
                // object that may already be black -- both halves stay.
                cn1SatbEnqueueRangeLocked(((JAVA_ARRAY_OBJECT*)CN1_ARRAY_DATA(dstArr)) + dstOffset, length);
#ifndef CN1_NO_BULK_INSERTION_BARRIER
                cn1SatbEnqueueRangeLocked(((JAVA_ARRAY_OBJECT*)CN1_ARRAY_DATA(srcArr)) + srcOffset, length);
#endif
            }
        }
    }
    /* java.lang.System.arraycopy is contractually overlap-safe (the spec defines
     * it as if copying via a temporary), and callers such as ArrayList.remove
     * shift elements within a single array (overlapping src/dst). memcpy is
     * undefined behaviour on overlap: x86-64's implementation happens to tolerate
     * the downward shift, but AArch64's optimized memcpy corrupts it (observed as
     * heap corruption on the arm64 clean target). memmove is the correct,
     * overlap-safe primitive. */
    memmove( CN1_ARRAY_DATA(dstArr) + (dstOffset * byteSize), CN1_ARRAY_DATA(srcArr)  + (srcOffset * byteSize), length * byteSize);
    if(cn1__satbReg) {
        cn1SatbBulkEnd();
    }
}

/*
 * The per-thread shadow stack, mapped rather than malloc'd + memset.
 *
 * This is CN1_MAX_OBJECT_STACK_DEPTH * sizeof(elementStruct) -- 258KB at the
 * default depth. It used to be malloc'd and then memset in full at thread
 * creation, which is 258KB of stores on the spawn path for a stack the thread
 * will walk a few frames of. A fresh anonymous mapping is zero-filled by the
 * kernel and commits per page on first touch, so neither the stores nor the pages
 * are paid for up front.
 *
 * The eager clear was redundant: every frame prologue memsets the slots it claims
 * (see the frame-entry helpers in cn1_globals.h), and the collector scans only up
 * to threadObjectStackOffset, so no slot is read before its owning frame zeroed it.
 *
 * On RESIDENT memory this is worth less than it looks. Measured on musl/arm64 with
 * 512 parked threads, per-thread RSS went 258KB -> 240KB: the shadow stack was
 * already mostly uncommitted, and the per-thread cost actually lives in the
 * callStack arrays (~50KB), pendingHeapAllocations (~27KB) and the try-block array
 * (~15KB). Shrinking CN1_MAX_OBJECT_STACK_DEPTH on Linux changes nothing at all.
 * The win here is the spawn path, not the footprint.
 *
 * Growing it is deliberately NOT how depth is solved. Generated frames hold
 * interior pointers into this array (`locals` and `stack` are C locals pointing
 * into it), so anything that MOVED the allocation would dangle every frame below
 * the one that grew it. Reserving the range up front and letting the kernel decide
 * what is resident keeps every pointer stable.
 */
/* Reports through *mapped which allocator answered, because the caller cannot tell
   from the pointer and the two do not free the same way. */
static struct elementStruct* cn1AllocThreadStack(int* mapped) {
    *mapped = 0;
#if defined(_WIN32)
    /* VirtualAlloc would be the equivalent; calloc keeps the Windows target on one
       well-trodden path, and it is not the target where thread counts are large. */
    return (struct elementStruct*)calloc(CN1_MAX_OBJECT_STACK_DEPTH, sizeof(struct elementStruct));
#else
    /* Declared here rather than above the #if: it is used only on this arm, and on
       Windows it was an unused local the compiler is entitled to warn about. */
    size_t bytes = CN1_MAX_OBJECT_STACK_DEPTH * sizeof(struct elementStruct);
    void* p = mmap(NULL, bytes, PROT_READ | PROT_WRITE,
                   MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if(p == MAP_FAILED) {
        /* Out of mappings rather than out of memory; calloc may still succeed. The
           caller must remember this happened -- munmap on the result would fail with
           EINVAL and leak the stack. */
        return (struct elementStruct*)calloc(CN1_MAX_OBJECT_STACK_DEPTH, sizeof(struct elementStruct));
    }
    *mapped = 1;
    return (struct elementStruct*)p;
#endif
}

/* mapped MUST be the value cn1AllocThreadStack reported for this pointer. */
static void cn1FreeThreadStack(struct elementStruct* stack, int mapped) {
    if(stack == NULL) {
        return;
    }
    if(!mapped) {
        free(stack);
        return;
    }
#if !defined(_WIN32)
    munmap(stack, CN1_MAX_OBJECT_STACK_DEPTH * sizeof(struct elementStruct));
#endif
}

// getenv returns a pointer into the process environment, which is owned by the
// C runtime and must not be freed. stringToUTF8 hands back the calling thread's
// scratch buffer, so the lookup must finish with it before anything else on this
// thread converts another string -- newStringFromNative copies, so building the
// result here is safe.
//
// DECODED, not widened. An environment value is outside text: newStringFromCString
// turns each byte into its own char, so a UTF-8 value comes back as one garbage
// char per byte. (On Windows the value is in the ACTIVE CODE PAGE rather than
// UTF-8, so it needs _wgetenv before any decoding is meaningful -- the same unfixed
// issue recorded against the file layer below, and the same remedy.)
/* Renamed from getenv: the null and embedded-NUL checks moved to the Java side,
   where they are one line each, so this is now the raw lookup. */
JAVA_OBJECT java_lang_System_getenvImpl___java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name) {
    if(name == JAVA_NULL) {
        return JAVA_NULL;
    }
    const char* key = stringToUTF8(threadStateData, name);
    if(key == NULL) {
        return JAVA_NULL;
    }
    const char* value = getenv(key);
    if(value == NULL) {
        return JAVA_NULL;
    }
    return newStringFromNative(threadStateData, value);
}

// ---------------------------------------------------------------------------
// java.io file streams and standard input.
//
// Backed by C stdio (not POSIX fds) so the same code serves the Windows clean
// target, which has no unistd.h. The Java side stores the FILE* as a long; 0 is
// the "not open" value, which is why every open returns 0 rather than -1 on
// failure. Negative returns below -1 mean "error" as opposed to -1's "end of
// file", and the Java side turns those into IOException.
//
// The byte[] is only touched between entry and return, so it needs no GC
// bracket: under conservative roots the argument is a scanned native local, and
// nothing here allocates.
// ---------------------------------------------------------------------------

JAVA_LONG java_io_FileInputStream_openImpl___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name) {
    if(name == JAVA_NULL) {
        return 0;
    }
    const char* path = stringToUTF8(threadStateData, name);
    if(path == NULL) {
        return 0;
    }
    {
        /* Opening BLOCKS on a FIFO: fopen for read waits until a writer opens the
           other end, and there may never be one. `path` points into the thread's
           utf8Buffer, which is C memory and unaffected by a collection, so it stays
           valid across the safepoint. */
        FILE* f;
        CN1_YIELD_THREAD;
        f = fopen(path, "rb");
        if(f != NULL) {
            /* Same reasoning, and the same 16KB ceiling for the same reason: it is
             * dirty memory held per open stream. */
            setvbuf(f, NULL, _IOFBF, 16384);
        }
        CN1_RESUME_THREAD;
        return (JAVA_LONG)(intptr_t)f;
    }
}

/*
 * TWO KNOWN LIMITATIONS of this file layer, recorded here rather than fixed,
 * because both are pre-existing on every platform and neither is what enabling the
 * clean target is about. Raised in review on PR #5658; written down so the next
 * reader finds the analysis instead of rediscovering it.
 *
 * 1. FILE POSITIONS ARE 32-BIT WHERE C `long` IS. skipImpl/availableImpl below use
 *    ftell/fseek, so a file over 2GiB cannot have its position represented on
 *    Windows (LLP64: long is 32 bits) even though the Java API is `long`
 *    throughout. The fix is _ftelli64/_fseeki64 against ftello/fseeko, plus
 *    widening the local arithmetic -- worth doing, and not a build-enablement
 *    change.
 *
 * 2. PATHS ARE PASSED TO THE NARROW CRT. stringToUTF8 produces UTF-8, and the
 *    Windows CRT's fopen reads it in the active ANSI code page, so a path holding
 *    a non-ASCII user or file name fails to open. The same mismatch runs through
 *    java_io_File.m's stat/access/FindFirstFile calls. cn1_db_sqlite_impl.h around
 *    line 196 already documents this exact problem and converts UTF-8 to UTF-16
 *    before calling the wide API; the file layer needs the same treatment applied
 *    across every entry point, which is its own change rather than a line here.
 */

/* Keeps a Java object provably live past a safepoint. Only an INTERIOR pointer into
   an array is used across the blocking calls below, so the optimizer is free to drop
   the array reference itself -- and the concurrent collector, scanning this parked
   thread, then sees no root and sweeps the buffer while the read is still filling it.
   The Linux port solves this with an asm barrier; this file also compiles under
   clang-cl, which has no __asm__ __volatile__, so it uses a volatile store, which no
   compiler may elide. The sink is written from several threads and never read: that
   is the entire point of it, and the races are benign because no value is consumed. */
static volatile JAVA_OBJECT cn1BlockingIoKeepAlive;
#define CN1_KEEP_ALIVE_ACROSS_SAFEPOINT(obj) do { cn1BlockingIoKeepAlive = (obj); } while(0)

JAVA_INT java_io_FileInputStream_readImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    FILE* f = (FILE*)(intptr_t)handle;
    if(f == NULL || buffer == JAVA_NULL) {
        return -2;
    }
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)buffer);
    size_t n;
    int atEof;
    /* A "file" is not always a file: a FIFO, a device or a network-backed path can
       block here for as long as the other end stays quiet, and with the thread left
       ACTIVE the collector spins for a safepoint it cannot reach. Same treatment as
       the socket reads and StandardInputStream. */
    CN1_YIELD_THREAD;
    n = fread(&data[offset], 1, (size_t)length, f);
    atEof = feof(f);            /* before the resume: the resume is a safepoint */
    CN1_RESUME_THREAD;
    CN1_KEEP_ALIVE_ACROSS_SAFEPOINT(buffer);
    if(n == 0) {
        return atEof ? -1 : -2;
    }
    return (JAVA_INT)n;
}

JAVA_LONG java_io_FileInputStream_skipImpl___long_long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_LONG count) {
    FILE* f = (FILE*)(intptr_t)handle;
    if(f == NULL) {
        return -1;
    }
    // Clamped to the real end so the return value is bytes actually skipped, which
    // is what InputStream.skip promises -- seeking past EOF succeeds in C and would
    // otherwise report a skip that did not happen.
    long start;
    long end;
    long remaining;
    long skipped;
    long target;
    /* Parked for the same reason availableImpl is: on a remote or FUSE filesystem
       these go over the wire, and the collector cannot stop a thread sitting in the
       CRT. One yield spans the sequence; the arithmetic below is local and stays
       outside it. */
    CN1_YIELD_THREAD;
    start = ftell(f);
    if(start < 0 || fseek(f, 0, SEEK_END) != 0) {
        CN1_RESUME_THREAD;
        return -1;
    }
    end = ftell(f);
    CN1_RESUME_THREAD;
    if(end < 0) {
        return -1;
    }
    /* Clamp against the DISTANCE, never by adding first. skip(Long.MAX_VALUE) after
       any byte has been read overflows `start + count` before the comparison can
       clamp it -- signed overflow is undefined behaviour, and in practice wraps
       negative and seeks backwards, so the caller is told it skipped a negative
       distance or gets an error instead of landing on EOF. Subtracting cannot
       overflow: end >= start >= 0, and start + skipped is at most end. */
    remaining = end - start;
    if(count <= 0) {
        skipped = 0;
    } else if(count >= (JAVA_LONG)remaining) {
        skipped = remaining;
    } else {
        skipped = (long)count;
    }
    target = start + skipped;
    {
        int failed;
        CN1_YIELD_THREAD;
        failed = fseek(f, target, SEEK_SET) != 0;
        CN1_RESUME_THREAD;
        if(failed) {
            return -1;
        }
    }
    return (JAVA_LONG)skipped;
}

JAVA_INT java_io_FileInputStream_availableImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    FILE* f = (FILE*)(intptr_t)handle;
    if(f == NULL) {
        return -1;
    }
    {
        /* Seeking is not free on every filesystem: a remote mount or a FUSE
           filesystem services ftell/fseek over the wire, and the thread is inside
           the CRT for the duration. One yield spans the whole sequence rather than
           bracketing each call -- the collector only needs the thread parked, and
           three yield/resume pairs would cost more than the seeks. */
        long start, end, remaining;
        int failed = 0;
        CN1_YIELD_THREAD;
        start = ftell(f);
        if(start < 0 || fseek(f, 0, SEEK_END) != 0) {
            failed = 1;
        }
        if(!failed) {
            end = ftell(f);
            if(fseek(f, start, SEEK_SET) != 0) {
                failed = 1;
            }
        }
        CN1_RESUME_THREAD;
        if(failed) {
            return -1;
        }
        remaining = end - start;
        if(remaining < 0) {
            return -1;
        }
        return remaining > 0x7fffffffL ? 0x7fffffff : (JAVA_INT)remaining;
    }
}

JAVA_INT java_io_FileInputStream_closeImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    FILE* f = (FILE*)(intptr_t)handle;
    if(f == NULL) {
        return 0;
    }
    {
        int r;
        CN1_YIELD_THREAD;
        r = fclose(f);
        CN1_RESUME_THREAD;
        return r == 0 ? 0 : -1;
    }
}

JAVA_LONG java_io_FileOutputStream_openImpl___java_lang_String_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name, JAVA_BOOLEAN append) {
    if(name == JAVA_NULL) {
        return 0;
    }
    const char* path = stringToUTF8(threadStateData, name);
    if(path == NULL) {
        return 0;
    }
    {
        /* The mirror of the read side: opening a FIFO for write waits for a reader. */
        FILE* f;
        CN1_YIELD_THREAD;
        f = fopen(path, append ? "ab" : "wb");
        if(f != NULL) {
            /* stdio's default buffer is a few kilobytes, so writing a file of any
             * size becomes that many write(2) calls. A bigger one cuts them by the
             * same factor, and setvbuf must be called before the first write.
             *
             * 16KB rather than more: this buffer is DIRTY memory for as long as the
             * stream is open, and it is per stream, so an application holding N open
             * files pays it N times -- on iOS dirty memory is the budget that
             * matters. 16KB is still four times the default, which is where most of
             * the syscall reduction is. */
            setvbuf(f, NULL, _IOFBF, 16384);
        }
        CN1_RESUME_THREAD;
        return (JAVA_LONG)(intptr_t)f;
    }
}

JAVA_INT java_io_FileOutputStream_writeImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    FILE* f = (FILE*)(intptr_t)handle;
    if(f == NULL || buffer == JAVA_NULL) {
        return -1;
    }
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)buffer);
    size_t written;
    /* Blocks for the same reasons the read does -- a full pipe, a slow device -- and
       strands the collector the same way. */
    CN1_YIELD_THREAD;
    written = fwrite(&data[offset], 1, (size_t)length, f);
    CN1_RESUME_THREAD;
    CN1_KEEP_ALIVE_ACROSS_SAFEPOINT(buffer);
    return (JAVA_INT)written;
}

JAVA_INT java_io_FileOutputStream_flushImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    FILE* f = (FILE*)(intptr_t)handle;
    if(f == NULL) {
        return -1;
    }
    {
        /* fflush pushes the buffer at the peer and blocks for the same reasons the
           write does -- a FIFO nobody is draining, a slow network filesystem. */
        int r;
        CN1_YIELD_THREAD;
        r = fflush(f);
        CN1_RESUME_THREAD;
        return r == 0 ? 0 : -1;
    }
}

JAVA_INT java_io_FileOutputStream_closeImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    FILE* f = (FILE*)(intptr_t)handle;
    if(f == NULL) {
        return 0;
    }
    {
        /* fclose FLUSHES before it closes, so it blocks exactly where the flush
           above does. */
        int r;
        CN1_YIELD_THREAD;
        r = fclose(f);
        CN1_RESUME_THREAD;
        return r == 0 ? 0 : -1;
    }
}

// Standard input. Separate from FileInputStream because stdin is not seekable, so
// skip/available cannot be implemented by the ftell dance above.
JAVA_INT java_io_StandardInputStream_readImpl___byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    if(buffer == JAVA_NULL) {
        return -2;
    }
    JAVA_ARRAY_BYTE* data = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)buffer);
    size_t n;
    int atEof;
    /* System.in.read() on a terminal or pipe waits for as long as nobody types. With
       the thread left ACTIVE the concurrent collector spins for a safepoint this
       thread cannot reach until input arrives -- on a target where forced-stop
       escalation does not succeed, that is the whole VM stalled on a human. */
    CN1_YIELD_THREAD;
    n = fread(&data[offset], 1, (size_t)length, stdin);
    /* Read BEFORE the resume. CN1_RESUME_THREAD is a safepoint and can park this
       thread on a timed wait, and anything the stream state is asked for afterwards
       describes the wait rather than the read. */
    atEof = feof(stdin);
    CN1_RESUME_THREAD;
    CN1_KEEP_ALIVE_ACROSS_SAFEPOINT(buffer);
    if(n == 0) {
        return atEof ? -1 : -2;
    }
    return (JAVA_INT)n;
}

JAVA_LONG java_lang_System_currentTimeMillis___R_long(CODENAME_ONE_THREAD_STATE) {
    __STATIC_INITIALIZER_java_lang_System(threadStateData);
    struct timeval time;
    gettimeofday(&time, NULL);
    JAVA_LONG l = (((JAVA_LONG)time.tv_sec) * 1000) + (time.tv_usec / 1000);
    return l;
}

JAVA_LONG java_lang_System_nanoTime___R_long(CODENAME_ONE_THREAD_STATE) {
    __STATIC_INITIALIZER_java_lang_System(threadStateData);
#ifdef _WIN32
    /* clock_gettime / CLOCK_MONOTONIC are absent from the MSVC / clang-cl
       target, so fall back to the microsecond wall clock that already backs
       currentTimeMillis on Windows (gettimeofday is supplied by cn1_win_compat). */
    struct timeval time;
    gettimeofday(&time, NULL);
    return (((JAVA_LONG)time.tv_sec) * 1000000000LL) + (((JAVA_LONG)time.tv_usec) * 1000LL);
#else
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (((JAVA_LONG)ts.tv_sec) * 1000000000LL) + (JAVA_LONG)ts.tv_nsec;
#endif
}

JAVA_DOUBLE java_lang_Double_longBitsToDouble___long_R_double(CODENAME_ONE_THREAD_STATE, JAVA_LONG n1)
{
    union {
        JAVA_DOUBLE d;
        JAVA_LONG   l;
    } u;
    
    u.l = n1;
    return u.d;
}

JAVA_LONG java_lang_Double_doubleToLongBits___double_R_long(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE n1) {
    union {
        JAVA_DOUBLE d;
        JAVA_LONG   l;
    } u;
    
    u.d = n1;
    return u.l;
}

JAVA_FLOAT java_lang_Float_intBitsToFloat___int_R_float(CODENAME_ONE_THREAD_STATE, JAVA_INT n1)
{
    union {
        JAVA_FLOAT  f;
        JAVA_INT    i;
    } u;
    
    u.i = n1;
    return u.f;
}

JAVA_INT java_lang_Float_floatToIntBits___float_R_int(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT n1)
{
    union {
        JAVA_FLOAT  f;
        JAVA_INT    i;
    } u;
    
    u.f = n1;
    return u.i;
}


// java.lang.Double.toString / java.lang.Float.toString must print "the smallest number
// of digits that uniquely distinguishes the argument value from adjacent values of the
// same type". The previous implementation instead asked snprintf for a fixed "%f" (six
// decimals) in the plain range and "%1.20E" (twenty-one significant digits) in the
// scientific range, so Double.toString(1.0/3.0) came back as "0.333333" instead of
// "0.3333333333333333", and Double.toString(1e30) came back as
// "1.00000000000000001988E30" instead of "1.0E30". Both are wrong in every direction
// that matters: too few digits loses information, too many invent it, and neither round
// trips. String concatenation of a double went through this, so did String.format.
//
// snprintf("%.*e") is correctly rounded, and a rendering that round trips stays round
// tripping as digits are added, so a binary search over the digit count finds the
// shortest faithful rendering in a handful of formatting attempts. The search starts at
// two significand digits because the specification requires at least one digit after the
// decimal point: Double.toString(Double.MIN_VALUE) is "4.9E-324", not a zero-padded
// "5.0E-324", even though a single digit round trips.
static void cn1ShortestDouble(char* buffer, int bufferSize, JAVA_DOUBLE d) {
    int low = 2;
    int high = 17;
    while (low < high) {
        int mid = (low + high) / 2;
        snprintf(buffer, bufferSize, "%.*e", mid - 1, d);
        if (strtod(buffer, NULL) == d) {
            high = mid;
        } else {
            low = mid + 1;
        }
    }
    snprintf(buffer, bufferSize, "%.*e", low - 1, d);
}

static void cn1ShortestFloat(char* buffer, int bufferSize, JAVA_FLOAT f) {
    int low = 2;
    int high = 9;
    while (low < high) {
        int mid = (low + high) / 2;
        snprintf(buffer, bufferSize, "%.*e", mid - 1, (double)f);
        if (strtof(buffer, NULL) == f) {
            high = mid;
        } else {
            low = mid + 1;
        }
    }
    snprintf(buffer, bufferSize, "%.*e", low - 1, (double)f);
}

// Rewrites the "[-]d.dddde[+-]dd" rendering above into the shape java.lang.Double.toString
// specifies: no '+' and no leading zeros on the exponent, no trailing zeros in the
// significand, and always at least one digit after the decimal point.
// Appends one character if there is room for it and for the terminating NUL. Every
// write in cn1JavaFloatingText goes through here so that boundedness is a property of
// the helper rather than of reasoning about the largest exponent that can reach it.
static void cn1AppendChar(char* out, int outSize, int* at, char c) {
    if (*at < outSize - 1) {
        out[(*at)++] = c;
    }
}

static void cn1JavaFloatingText(char* out, int outSize, const char* raw, JAVA_BOOLEAN scientific) {
    const char* p = raw;
    int at = 0;
    char digits[32];
    int digitCount = 0;
    int exponent = 0;
    int i;
    if (outSize < 1) {
        return;
    }
    if (*p == '-') {
        cn1AppendChar(out, outSize, &at, '-');
        p++;
    }
    while (*p != 0 && *p != 'e' && *p != 'E') {
        if (*p >= '0' && *p <= '9' && digitCount < (int)sizeof(digits) - 1) {
            digits[digitCount++] = *p;
        }
        p++;
    }
    if (*p == 'e' || *p == 'E') {
        exponent = (int)strtol(p + 1, NULL, 10);
    }
    while (digitCount > 1 && digits[digitCount - 1] == '0') {
        digitCount--;
    }
    if (digitCount == 0) {
        digits[digitCount++] = '0';
    }
    if (scientific) {
        cn1AppendChar(out, outSize, &at, digits[0]);
        cn1AppendChar(out, outSize, &at, '.');
        if (digitCount == 1) {
            cn1AppendChar(out, outSize, &at, '0');
        } else {
            for (i = 1; i < digitCount; i++) {
                cn1AppendChar(out, outSize, &at, digits[i]);
            }
        }
        cn1AppendChar(out, outSize, &at, 'E');
        snprintf(out + at, outSize - at, "%d", exponent);
        return;
    }
    // Double.toString only takes the plain branch for 1e-3 <= |d| < 1e7, so the exponent
    // reaching here is small. The native is reachable on its own though, and a large one
    // would otherwise run past the buffer, so the appends above bound themselves.
    if (exponent < 0) {
        cn1AppendChar(out, outSize, &at, '0');
        cn1AppendChar(out, outSize, &at, '.');
        for (i = 0; i < -exponent - 1; i++) {
            cn1AppendChar(out, outSize, &at, '0');
        }
        for (i = 0; i < digitCount; i++) {
            cn1AppendChar(out, outSize, &at, digits[i]);
        }
    } else {
        for (i = 0; i <= exponent; i++) {
            cn1AppendChar(out, outSize, &at, i < digitCount ? digits[i] : '0');
        }
        cn1AppendChar(out, outSize, &at, '.');
        if (exponent + 1 >= digitCount) {
            cn1AppendChar(out, outSize, &at, '0');
        } else {
            for (i = exponent + 1; i < digitCount; i++) {
                cn1AppendChar(out, outSize, &at, digits[i]);
            }
        }
    }
    out[at] = 0;
}

JAVA_OBJECT java_lang_Double_toStringImpl___double_boolean_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE d, JAVA_BOOLEAN b) {
    // Double.toString handles these before delegating, but the method is reachable on
    // its own and the parser below has no meaning for them.
    if (d != d) {
        return newStringFromCString(threadStateData, "NaN");
    }
    if (d > 1.7976931348623157E308) {
        return newStringFromCString(threadStateData, "Infinity");
    }
    if (d < -1.7976931348623157E308) {
        return newStringFromCString(threadStateData, "-Infinity");
    }
    char raw[48];
    char out[512];
    cn1ShortestDouble(raw, (int)sizeof(raw), d);
    cn1JavaFloatingText(out, (int)sizeof(out), raw, b);
    return newStringFromCString(threadStateData, out);
}

JAVA_OBJECT java_lang_Float_toStringImpl___float_boolean_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT d, JAVA_BOOLEAN b) {
    if (d != d) {
        return newStringFromCString(threadStateData, "NaN");
    }
    if (d > 3.4028234663852886E38f) {
        return newStringFromCString(threadStateData, "Infinity");
    }
    if (d < -3.4028234663852886E38f) {
        return newStringFromCString(threadStateData, "-Infinity");
    }
    char raw[48];
    char out[512];
    cn1ShortestFloat(raw, (int)sizeof(raw), d);
    cn1JavaFloatingText(out, (int)sizeof(out), raw, b);
    return newStringFromCString(threadStateData, out);
}


JAVA_OBJECT java_lang_Integer_toString___int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT d) {
    char s[12];
    sprintf(s, "%i", d);
    return newStringFromCString(threadStateData, s);
}

char *ltostr (char *str, long long val, unsigned base) {
    ldiv_t r;           /* result of val / base */
    
    /* no conversion if wrong base */
    if (base > 36) {
        str = '\0';
        return str;
    }
    if (val < 0)    *str++ = '-';
    r = ldiv (labs(val), base);
    
    /* output digits of val/base first */
    
    if (r.quot > 0)  str = ltostr (str, r.quot, base);
    
    /* output last digit */
    
    *str++ = "0123456789abcdefghijklmnopqrstuvwxyz"[(int)r.rem];
    *str   = '\0';
    return str;
}

JAVA_OBJECT java_lang_Integer_toString___int_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT d, JAVA_INT radix) {
    char s[12];
    if(radix == 10) {
        // Direct decimal-digit extraction + single-pass compact Latin-1 String (see
        // java_lang_Long_toString above). Unsigned magnitude handles INT_MIN.
        char tmp[12];
        int pos = 12;
        int neg = d < 0;
        unsigned int u = neg ? (~((unsigned int)d) + 1U) : (unsigned int)d;
        do { tmp[--pos] = (char)('0' + (int)(u % 10U)); u /= 10U; } while(u != 0);
        if(neg) { tmp[--pos] = '-'; }
        return newStringFromAsciiLen(threadStateData, tmp + pos, 12 - pos);
    }
    ltostr(s, d, radix);
    return newStringFromCString(threadStateData, s);
}

JAVA_OBJECT java_lang_Long_toString___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG d, JAVA_INT radix) {
    char str[256];
    if(radix == 10) {
        // Direct decimal-digit extraction into a stack buffer, then a single-pass
        // compact Latin-1 String (digits are always ASCII). Avoids sprintf's format
        // parsing AND newStringFromCString's strlen + char[] decode + Latin-1 scan --
        // the hot path for int.toString()/string interpolation. ~20 digits max for a
        // 64-bit value; the unsigned magnitude handles LONG_MIN without overflow.
        char tmp[24];
        int pos = 24;
        int neg = d < 0;
        unsigned long long u = neg ? (~((unsigned long long)d) + 1ULL) : (unsigned long long)d;
        do { tmp[--pos] = (char)('0' + (int)(u % 10ULL)); u /= 10ULL; } while(u != 0);
        if(neg) { tmp[--pos] = '-'; }
        return newStringFromAsciiLen(threadStateData, tmp + pos, 24 - pos);
    }
    switch(radix) {
        case 16:
            sprintf(str, "%llx", d);
            return newStringFromCString(threadStateData, str);
    }
    ltostr(str, d, radix);
    return newStringFromCString(threadStateData, str);
}

// Fused compact string concatenation. String.cn1ConcatN calls these ONLY once it has verified every
// part is a Latin-1 (byte[]-backed) String, so we read raw bytes and build a SINGLE fused block
// (byte[] inline in the String) -- one allocation and no byte<->char conversion, vs StringBuilder's
// four allocations + two conversions. Args are guaranteed non-null (String.cn1c handled null).
#define CN1_SB_PTR(s) ((JAVA_ARRAY_BYTE*)cn1StrChars((JAVA_OBJECT)(s)))
#define CN1_SB_LEN(s) (((struct obj__java_lang_String*)(s))->java_lang_String_count)

static JAVA_OBJECT cn1ConcatFallback(CODENAME_ONE_THREAD_STATE, JAVA_ARRAY_BYTE* const* parts, const int* lens, int n, int total) {
    enteringNativeAllocations();
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, total, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*) CN1_ARRAY_DATA(dat);
    int o = 0;
    for(int p = 0 ; p < n ; p++) { for(int i = 0 ; i < lens[p] ; i++) d[o + i] = parts[p][i]; o += lens[p]; }
    JAVA_OBJECT so = __NEW_java_lang_String(threadStateData);
    java_lang_String___INIT____(threadStateData, so);
    struct obj__java_lang_String* ss = (struct obj__java_lang_String*)so;
    ss->java_lang_String_value = (JAVA_OBJECT)dat;
    ss->java_lang_String_count = total;
    finishedNativeAllocations();
    return so;
}

JAVA_OBJECT java_lang_String_cn1FusedConcat2___java_lang_String_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT a, JAVA_OBJECT b) {
    JAVA_ARRAY_BYTE* p[2] = { CN1_SB_PTR(a), CN1_SB_PTR(b) };
    int l[2] = { CN1_SB_LEN(a), CN1_SB_LEN(b) };
    int total = l[0] + l[1];
    JAVA_ARRAY_BYTE* dst;
    JAVA_OBJECT so = cn1FusedLatin1Begin(threadStateData, total, &dst);
    if(so != JAVA_NULL) {
        int o = 0;
        for(int q = 0 ; q < 2 ; q++) { for(int i = 0 ; i < l[q] ; i++) dst[o + i] = p[q][i]; o += l[q]; }
        cn1FusedLatin1End(so, total);
        return so;
    }
    return cn1ConcatFallback(threadStateData, p, l, 2, total);
}

JAVA_OBJECT java_lang_String_cn1FusedConcat3___java_lang_String_java_lang_String_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT a, JAVA_OBJECT b, JAVA_OBJECT c) {
    JAVA_ARRAY_BYTE* p[3] = { CN1_SB_PTR(a), CN1_SB_PTR(b), CN1_SB_PTR(c) };
    int l[3] = { CN1_SB_LEN(a), CN1_SB_LEN(b), CN1_SB_LEN(c) };
    int total = l[0] + l[1] + l[2];
    JAVA_ARRAY_BYTE* dst;
    JAVA_OBJECT so = cn1FusedLatin1Begin(threadStateData, total, &dst);
    if(so != JAVA_NULL) {
        int o = 0;
        for(int q = 0 ; q < 3 ; q++) { for(int i = 0 ; i < l[q] ; i++) dst[o + i] = p[q][i]; o += l[q]; }
        cn1FusedLatin1End(so, total);
        return so;
    }
    return cn1ConcatFallback(threadStateData, p, l, 3, total);
}

JAVA_OBJECT java_lang_String_cn1FusedConcat4___java_lang_String_java_lang_String_java_lang_String_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT a, JAVA_OBJECT b, JAVA_OBJECT c, JAVA_OBJECT d) {
    JAVA_ARRAY_BYTE* p[4] = { CN1_SB_PTR(a), CN1_SB_PTR(b), CN1_SB_PTR(c), CN1_SB_PTR(d) };
    int l[4] = { CN1_SB_LEN(a), CN1_SB_LEN(b), CN1_SB_LEN(c), CN1_SB_LEN(d) };
    int total = l[0] + l[1] + l[2] + l[3];
    JAVA_ARRAY_BYTE* dst;
    JAVA_OBJECT so = cn1FusedLatin1Begin(threadStateData, total, &dst);
    if(so != JAVA_NULL) {
        int o = 0;
        for(int q = 0 ; q < 4 ; q++) { for(int i = 0 ; i < l[q] ; i++) dst[o + i] = p[q][i]; o += l[q]; }
        cn1FusedLatin1End(so, total);
        return so;
    }
    return cn1ConcatFallback(threadStateData, p, l, 4, total);
}

JAVA_OBJECT java_lang_String_cn1FusedConcat5___java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT a, JAVA_OBJECT b, JAVA_OBJECT c, JAVA_OBJECT d, JAVA_OBJECT e) {
    JAVA_ARRAY_BYTE* p[5] = { CN1_SB_PTR(a), CN1_SB_PTR(b), CN1_SB_PTR(c), CN1_SB_PTR(d), CN1_SB_PTR(e) };
    int l[5] = { CN1_SB_LEN(a), CN1_SB_LEN(b), CN1_SB_LEN(c), CN1_SB_LEN(d), CN1_SB_LEN(e) };
    int total = l[0] + l[1] + l[2] + l[3] + l[4];
    JAVA_ARRAY_BYTE* dst;
    JAVA_OBJECT so = cn1FusedLatin1Begin(threadStateData, total, &dst);
    if(so != JAVA_NULL) {
        int o = 0;
        for(int q = 0 ; q < 5 ; q++) { for(int i = 0 ; i < l[q] ; i++) dst[o + i] = p[q][i]; o += l[q]; }
        cn1FusedLatin1End(so, total);
        return so;
    }
    return cn1ConcatFallback(threadStateData, p, l, 5, total);
}

JAVA_DOUBLE java_lang_Math_cos___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return cos(a);
}

JAVA_DOUBLE java_lang_Math_sin___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return sin(a);
}

// "a < 0" is false for negative zero, so the old form returned -0.0 where the JDK
// specifies positive zero. fabs clears the sign bit unconditionally.
JAVA_DOUBLE java_lang_Math_abs___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return fabs(a);
}

JAVA_FLOAT java_lang_Math_abs___float_R_float(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT a) {
    return fabsf(a);
}

JAVA_INT java_lang_Math_abs___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT a) {
    if(a < 0) {
        return a * -1;
    }
    return a;
}

JAVA_LONG java_lang_Math_abs___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG a) {
    if(a < 0) {
        return a * -1;
    }
    return a;
}

JAVA_DOUBLE java_lang_Math_ceil___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    JAVA_LONG la = (JAVA_LONG)a;
    if ( a == la || a < 0) return la;
    return la+1;
}

JAVA_DOUBLE java_lang_Math_floor___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    JAVA_LONG la = (JAVA_LONG)a;
    if ( a >= 0 || a == la ) return la;
    return la-1;
}

JAVA_DOUBLE java_lang_Math_max___double_double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a, JAVA_DOUBLE b){
    if(a > b) return a;
    return b;
}

JAVA_DOUBLE java_lang_Math_pow___double_double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a, JAVA_DOUBLE b){
    return pow(a, b);
}

JAVA_FLOAT java_lang_Math_max___float_float_R_float(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT a, JAVA_FLOAT b){
    if(a > b) return a;
    return b;
}

JAVA_INT java_lang_Math_max___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT a, JAVA_INT b){
    if(a > b) return a;
    return b;
}

JAVA_LONG java_lang_Math_max___long_long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG a, JAVA_LONG b){
    if(a > b) return a;
    return b;
}

JAVA_DOUBLE java_lang_Math_min___double_double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a, JAVA_DOUBLE b){
    if(a < b) return a;
    return b;
}

JAVA_FLOAT java_lang_Math_min___float_float_R_float(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT a, JAVA_FLOAT b){
    if(a < b) return a;
    return b;
}

JAVA_INT java_lang_Math_min___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT a, JAVA_INT b){
    if(a < b) return a;
    return b;
}

JAVA_LONG java_lang_Math_min___long_long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG a, JAVA_LONG b){
    if(a < b) return a;
    return b;
}

JAVA_DOUBLE java_lang_Math_sqrt___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return sqrt(a);
}

JAVA_DOUBLE java_lang_Math_tan___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return tan(a);
}

JAVA_DOUBLE java_lang_Math_atan___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return atan(a);
}

JAVA_DOUBLE java_lang_Math_acos___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return acos(a);
}

JAVA_DOUBLE java_lang_Math_asin___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return asin(a);
}

JAVA_DOUBLE java_lang_Math_atan2___double_double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE y, JAVA_DOUBLE x) {
    return atan2(y, x);
}

JAVA_DOUBLE java_lang_Math_exp___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return exp(a);
}

JAVA_DOUBLE java_lang_Math_log___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return log(a);
}

JAVA_DOUBLE java_lang_Math_log10___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    return log10(a);
}

JAVA_BOOLEAN isClassNameEqual(const char * clsName, JAVA_ARRAY_CHAR* chrs, int length) {
    for(int i = 0 ; i < length ; i++) {
        if(clsName[i] != chrs[i]) return JAVA_FALSE;
    }
    return JAVA_TRUE;
}

JAVA_OBJECT java_lang_Class_forNameImpl___java_lang_String_R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT className) {
    int length = java_lang_String_length___R_int(threadStateData, className);
    JAVA_ARRAY arrayData = (JAVA_ARRAY)java_lang_String_toCharNoCopy___R_char_1ARRAY(threadStateData, className);
    JAVA_ARRAY_CHAR* chrs = CN1_ARRAY_DATA(arrayData);
    
    for(int iter = 0 ; iter < classListSize ; iter++) {
        if(strlen(classesList[iter]->clsName) == length) {
            if(!isClassNameEqual(classesList[iter]->clsName, chrs, length)) {
                continue;
            }
            return (JAVA_OBJECT)classesList[iter];
        }
    }
    return JAVA_NULL;
}

JAVA_OBJECT java_lang_Class_getComponentType___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    if (((struct clazz*)cls)->isArray) {
        return (JAVA_OBJECT)((struct clazz*)cls)->arrayType;
    }
    return JAVA_NULL;
}

JAVA_OBJECT java_lang_Class_getName___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    return newStringFromCString(threadStateData, clz->clsName);
}


JAVA_BOOLEAN java_lang_Class_isArray___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    return clz->isArray;
}

// NOTE on instanceofFunction's argument order: despite its parameter names, it
// is called as instanceofFunction(TARGET_TYPE, OBJECT_CLASS) — see BC_INSTANCEOF,
// which passes the bytecode's type operand first and GET_CLASS_ID(obj) second.
// It then indexes classInstanceOf[] by the OBJECT's class (whose table lists that
// class's supertypes) and searches it for the target. Both helpers below must
// therefore pass the receiver Class first.

JAVA_BOOLEAN java_lang_Class_isAssignableFrom___java_lang_Class_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls, JAVA_OBJECT cls2) {
    struct clazz* clz1 = (struct clazz*)cls;
    struct clazz* clz2 = (struct clazz*)cls2;
    // A primitive class carries CN1_PRIMITIVE_CLASS_ID, which indexes no row of
    // the instanceof tables, so it must never reach instanceofFunction. The JDK
    // rule is also simply identity: int is assignable only from int.
    if(clz1->primitiveType || clz2->primitiveType) {
        return clz1 == clz2 ? JAVA_TRUE : JAVA_FALSE;
    }
    // A.isAssignableFrom(B): target is A, the class under test is B.
    return instanceofFunction(clz1->classId, clz2->classId);
}

JAVA_BOOLEAN java_lang_Class_isInstance___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls, JAVA_OBJECT obj) {
    if(obj == JAVA_NULL) { return JAVA_FALSE; }
    struct clazz* clz1 = (struct clazz*)cls;
    // No object is ever an instance of a primitive class, and its sentinel
    // classId indexes no instanceof table row -- see isAssignableFrom above.
    if(((struct clazz*)cls)->primitiveType) { return JAVA_FALSE; }
    struct clazz* clz2 = (struct clazz*)CN1_CLASS_OF(obj); // tag-aware: a tagged Integer has no header
    // A.isInstance(o): target is A, the class under test is o's class. These were
    // reversed, so isInstance searched the TARGET's supertype table for the
    // object's class and answered false for every subclass — every
    // Class.isInstance in a native build was wrong unless the types were equal.
    return instanceofFunction(clz1->classId, clz2->classId);
}

JAVA_OBJECT java_lang_Class_getSuperclass___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    // Object, primitives and void already carry a null baseClass, so they need
    // no special case. Interfaces DO: a class file records java/lang/Object as
    // an interface's super_class and Parser.visit copies that straight into
    // baseClass, so the isInterface flag is the only thing separating them --
    // and Class.getSuperclass() is required to report null for an interface.
    if(clz->isInterface) {
        return JAVA_NULL;
    }
    return (JAVA_OBJECT)clz->baseClass;
}

JAVA_BOOLEAN java_lang_Class_isInterface___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    return clz->isInterface;
}

JAVA_BOOLEAN java_lang_Class_isSynthetic___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    return clz->isSynthetic;
}

JAVA_BOOLEAN java_lang_Class_isPrimitive___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    return clz->primitiveType;
}

JAVA_BOOLEAN java_lang_Class_isAnonymousClass___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    return clz->isAnonymous;
}

JAVA_BOOLEAN java_lang_Class_isAnnotation___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    return clz->isAnnotation;
}

JAVA_BOOLEAN java_lang_Class_isEnum___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    return (clz->enumValueOfFp != 0);
}

JAVA_OBJECT java_lang_Class_newInstanceImpl___R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls) {
    struct clazz* clz = (struct clazz*)cls;
    newInstanceFunctionPointer f = clz->newInstanceFp;
    return f(threadStateData);
}

JAVA_OBJECT java_lang_Enum_valueOf___java_lang_Class_java_lang_String_R_java_lang_Enum(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls, JAVA_OBJECT value) {
    struct clazz* clz = (struct clazz*)cls;
    enumValueOfFunctionPointer f = clz->enumValueOfFp;
    if (f == 0) {
        return JAVA_NULL;
    }
    return f(threadStateData, value);
}

JAVA_OBJECT java_lang_Object_toString___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    if (obj == JAVA_NULL) {
        return newStringFromCString(threadStateData, "null");
    } else {
        struct clazz* cls = obj->__codenameOneParentClsReference;
        const char* className = cls->clsName;
        char s[strlen(className) + 32];
        sprintf(s, "%s@%llX", className, ((JAVA_LONG)obj));
        return newStringFromCString(threadStateData, s);
    }
}

void initClazzClazz() {
    if(!ClazzClazz.initialized) {
        ClazzClazz.initialized = JAVA_TRUE;
        
        ClazzClazz.vtable = malloc(sizeof(void*) *10);
        ClazzClazz.vtable[0] = &java_lang_Object_equals___java_lang_Object_R_boolean;
        ClazzClazz.vtable[1] = &java_lang_Object_getClass___R_java_lang_Class;
        ClazzClazz.vtable[2] = &java_lang_Object_hashCode___R_int;
        ClazzClazz.vtable[3] = &java_lang_Object_notify__;
        ClazzClazz.vtable[4] = &java_lang_Object_notifyAll__;
        ClazzClazz.vtable[5] = &java_lang_Object_toString___R_java_lang_String;
        ClazzClazz.vtable[6] = &java_lang_Object_wait__;
        ClazzClazz.vtable[7] = &java_lang_Object_wait___long;
        ClazzClazz.vtable[8] = &java_lang_Object_wait___long_int;
    }
}

JAVA_OBJECT java_lang_Object_getClassImpl___R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    initClazzClazz();
#if CN1_TAGGED_ACTIVE
    // A tagged immediate has no object header to read; its class comes from the tag code.
    // Derived through CN1_CLASS_OF rather than named here, so a new tagged type needs no
    // edit -- getClass() was one of the four sites that crashed the original tagged-int
    // build precisely because it read the header directly.
    if(CN1_IS_TAGGED(obj)) {
        struct clazz* cn1__tagCls = CN1_CLASS_OF(obj);
        cn1__tagCls->__codenameOneParentClsReference = &ClazzClazz;
        return (JAVA_OBJECT)cn1__tagCls;
    }
#endif
    struct clazz* cn1__cls = obj->__codenameOneParentClsReference;
    if(!cn1__cls) {
        return (JAVA_OBJECT)(&ClazzClazz);
    }
    // A String's twin is the SAME class -- same classId, name, vtable, type-test row
    // -- and differs only in address, which is how it carries the coder for free. This
    // function is the one place that address becomes visible to Java, because the Class
    // object IS the clazz struct: without this, `s.getClass() == String.class` answers
    // false for a fused String and true for an array-backed one. Found by TwinProbe,
    // which compares both shapes against the JDK; nothing else in the suite asked.
    if(cn1IsInlineStringClass(cn1__cls)) {
        cn1__cls = &class__java_lang_String;
    }
    cn1__cls->__codenameOneParentClsReference = &ClazzClazz;
    return (JAVA_OBJECT)cn1__cls;
}

JAVA_INT java_lang_Class_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    return (JAVA_INT)obj;
}

JAVA_INT java_lang_Object_hashCode___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    return (JAVA_INT)obj;
}

struct ThreadLocalData** allThreads = 0;
int nThreadsToKill = 0;         // the number of threads we expect to be finalized, eventually

pthread_key_t   threadIdKey = 0;
JAVA_LONG threadKeyCounter = 1;
/**
 * Build a fresh VM thread state.
 *
 * Split out of getThreadLocalData so a VIRTUAL thread can have one too. A
 * virtual thread needs its own Java locals and operand stack -- that is the
 * whole point of it, since a request's state lives there -- and it must be
 * registered in allThreads like any other, or the precise scan never walks its
 * object stack and its live objects are collected under it.
 *
 * The one thing this deliberately does NOT do is bind the state to the calling
 * OS thread: a virtual thread's state belongs to the virtual thread and travels
 * with it between hosts.
 */
/* Defined with cn1ReleaseThreadLocalData further down; the capacity-failure path
   below unwinds a partially built state through it. */
static void cn1FreeThreadLocalDataFields(struct ThreadLocalData* head);

struct ThreadLocalData* cn1CreateThreadLocalData(JAVA_BOOLEAN bindToCallingOsThread) {
    struct ThreadLocalData* i;
        JAVA_LONG nativeThreadId = threadKeyCounter;
    threadKeyCounter++;
    i = malloc(sizeof(struct ThreadLocalData));
    i->threadId = nativeThreadId;
    i->tryBlockOffset = 0;
    i->nativeBuffers = NULL;
    
    i->lightweightThread = JAVA_FALSE;
    i->threadBlockedByGC = JAVA_FALSE;
    i->threadActive = JAVA_FALSE;
    i->threadKilled = JAVA_FALSE;
#ifdef CN1_GC_CONFORM
    // Malloc'd, so this starts as garbage. See gcThreadStartMs in cn1_globals.h.
    { extern void cn1StallRegisterThread(struct ThreadLocalData* t);
      cn1StallRegisterThread(i); }
#endif
    i->interrupted = JAVA_FALSE;

    i->currentThreadObject = 0;
    
    i->utf8Buffer = 0;
    i->utf8BufferSize = 0;
    /*
     * calloc, not malloc+memset. These four buffers are ~300KB per thread and the
     * eager memset TOUCHED EVERY PAGE, so a thread that never runs a deep call
     * chain still paid the whole footprint in resident memory -- measured at
     * ~118KB per parked thread, which is what decides whether a server-side
     * binary can afford a thread per connection.
     *
     * The eager clear was redundant: every frame prologue memsets exactly the
     * slots it is about to claim (see the frame-entry helpers in cn1_globals.h),
     * and the collector only scans threadObjectStack up to
     * threadObjectStackOffset, so no slot is ever read before the frame that owns
     * it has zeroed it. calloc for a request this size comes from mmap and is
     * lazily zeroed by the OS, so a shallow thread commits a few pages instead of
     * all of them.
     */
    i->threadObjectStack = cn1AllocThreadStack(&i->threadObjectStackMapped);
    i->threadObjectStackOffset = 0;

    i->callStackClass = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));
    i->callStackLine = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));
    i->callStackMethod = calloc(CN1_MAX_STACK_CALL_DEPTH, sizeof(int));

#ifdef CN1_ON_DEVICE_DEBUG
    i->callStackLocalsAddresses = malloc(CN1_MAX_STACK_CALL_DEPTH * sizeof(void**));
    memset(i->callStackLocalsAddresses, 0, CN1_MAX_STACK_CALL_DEPTH * sizeof(void**));
    i->callStackFrameInfo = malloc(CN1_MAX_STACK_CALL_DEPTH * sizeof(struct cn1_frame_info*));
    memset(i->callStackFrameInfo, 0, CN1_MAX_STACK_CALL_DEPTH * sizeof(struct cn1_frame_info*));
#endif

    i->callStackOffset = 0;

    /* THE STACK LIMIT IS SETTLED HERE, NOT ON FIRST FRAMELESS ENTRY.
     *
     * CN1_FRAMELESS_SOE_GUARD used to open with
     * `if (nativeStackLimit == 0) cn1ComputeNativeStackLimit(...)`, which put a
     * branch AND a cold call site into every guarded method -- 4,840 call sites
     * to cn1ComputeNativeStackLimit and 3,141 to cn1ThrowStackOverflow across the
     * hello corpus, more than any other symbol in the binary. They never execute,
     * but they are code, and clang costs a callee by its whole body: that bulk is
     * part of why small methods were not being inlined into their callers even
     * with the inline threshold raised five-fold.
     *
     * bindToCallingOsThread is exactly the right predicate. TRUE means we are ON
     * the thread whose stack this is, so pthread_self() answers about the right
     * stack. FALSE means the caller is spawning a VIRTUAL thread that will run on
     * its own allocated stack, where a pthread-derived limit would describe the
     * SPAWNER's stack -- so it gets the same "introspection gave nothing usable"
     * sentinel the compute path already uses, which makes the two-sided test
     * inert. That is what the guard already did for virtual threads in practice;
     * this says so on purpose.
     *
     * THIS MUST STAY BELOW THE OTHER FIELD INITIALIZATION. Written higher up it
     * is silently overwritten by the zero this replaces, and with the lazy check
     * gone the limit stays 0, the two-sided test can never trip, and the guard is
     * inert -- SoeTest caught exactly that, dying with no output instead of
     * throwing StackOverflowError. */
    if(bindToCallingOsThread) {
        cn1ComputeNativeStackLimit(i);
    } else {
        i->nativeStackLimit = 1;
    }

    i->pendingHeapAllocations = calloc(PER_THREAD_ALLOCATION_COUNT, sizeof(void *));
    i->heapAllocationSize = 0;
    i->threadHeapTotalSize = PER_THREAD_ALLOCATION_COUNT;
    // ThreadLocalData is malloc'd, NOT zeroed. bibopBytesLocal feeds the GC
    // trigger/pacing accounting (CN1_BIBOP_FLUSH_BYTES adds it into the global
    // counters); garbage here means a spurious immediate GC + hard-cap park, or
    // a dead allocation trigger, on every new thread. nativeAllocationMode is
    // read by the inlined alloc fast path (cn1BibopFastAlloc) before any setter
    // runs -- garbage-nonzero silently disables the fast path for the thread.
    i->bibopBytesLocal = 0;
    i->bibopEpochBytes = 0;
#ifndef CN1_DISABLE_BIBOP
    i->bibopObservedGcEpoch = atomic_load_explicit(&bibopGcEpoch,
                                        memory_order_relaxed);
#else
    i->bibopObservedGcEpoch = 0;
#endif
    i->bibopHighThroughputUntilEpoch = 0;
    i->nativeAllocationMode = JAVA_FALSE;
    // dead-thread pending-migration queue state (single-writer allObjectsInHeap)
    i->gcDeadNext = 0;
    i->gcQueuedForDrain = JAVA_FALSE;
    i->gcReleaseRequested = JAVA_FALSE;
    
    i->blocks = malloc(CN1_MAX_TRY_BLOCKS * sizeof(struct TryBlock));
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // PHASE 3b: record this thread's pthread handle + TLS self pointer so the GC can
    // signal-stop it and the async-signal-safe stop handler can find its state.
    i->gcParkCaptured = JAVA_FALSE;
    // Carried over when this initialisation was extracted into a function: the
    // forced-stop work (issue #5537) added this field to the inline block that used
    // to live in the thread runner, and ThreadLocalData is malloc'd, NOT zeroed --
    // an uninitialised flag here reads as garbage and the collector would believe it
    // had already force-stopped a thread it never touched.
    i->gcMarkForcedStop = JAVA_FALSE;
    i->gcStackPointerAtPark = 0;
    i->gcSigStopRequest = 0;
    i->gcSigStopped = 0;
    i->gcSigRelease = 0;
    i->gcSigStopGen = 0;
    i->gcSigStackPointer = 0;
    // Zeroed for the same reason as the rest of this block: ThreadLocalData is
    // malloc'd, not zeroed. The forced-stop scan guards on these being non-zero
    // before it marks [sp, base), so garbage here would pass that guard and hand
    // the conservative scan a bogus range.
    i->gcStopFailures = 0;
    i->gcSigStackBase = 0;
    i->gcSigStackSize = 0;
    i->gcSigRegsLen = 0;
    if(bindToCallingOsThread) {
        i->gcPthread = pthread_self();
        i->gcPthreadValid = JAVA_TRUE;
        cn1TlsSelf = i;
    } else {
        // A VIRTUAL thread has no pthread of its own and may run on a different
        // host next time, so binding either of these to whoever happens to be
        // creating it would be a lie the collector acts on. gcPthreadValid false
        // makes cn1GcScanThreadNativeStack skip it, which is right: its C stack is
        // reached through the virtual-thread registry instead, and its Java object
        // stack through allThreads like everyone else. cn1TlsSelf must keep naming
        // the HOST thread, because the async-signal stop handler runs on the host
        // and needs the host's state.
        // memset rather than `= 0`: pthread_t is a POINTER on Apple and glibc but a
        // struct {handle, id} in the Windows compat shim, where assigning 0 is not
        // even a type error the reader would expect -- it is "assigning to
        // 'pthread_t' from incompatible type 'int'", and it failed only the Windows
        // and cross-compile legs. Zeroing the bytes is correct for both shapes, and
        // gcPthreadValid below is what actually gates every read of this field.
        memset(&i->gcPthread, 0, sizeof(i->gcPthread));
        i->gcPthreadValid = JAVA_FALSE;
    }
#endif
    if(bindToCallingOsThread) {
        pthread_setspecific(threadIdKey, i);
    }
    
    if(!allThreads) {
        allThreads = malloc(NUMBER_OF_SUPPORTED_THREADS * sizeof(struct ThreadLocalData*));
        memset(allThreads, 0, NUMBER_OF_SUPPORTED_THREADS * sizeof(struct ThreadLocalData*));
    }
    int threadOffset = -1;
    lockCriticalSection();
    for(int iter = 0 ; iter < NUMBER_OF_SUPPORTED_THREADS ; iter++) {
        if(allThreads[iter] == 0) {
        threadOffset = iter;
        break;
        }
    }
    /* EXHAUSTION IS A RETURN VALUE, not an assertion. CODENAME_ONE_ASSERT is plain
       assert(), which NDEBUG compiles out of every release build -- so once all
       NUMBER_OF_SUPPORTED_THREADS slots were taken this fell through and executed
       allThreads[-1] = i, corrupting whatever precedes the table instead of failing.
       A debug build aborted; a shipped one carried on with silent corruption, which
       is worse. Reporting it lets a caller that can cope do so. */
    if(threadOffset < 0) {
        unlockCriticalSection();
        /* UNBIND BEFORE FREEING. pthread_setspecific ran above, so the key already
           points at this state; freeing it without clearing leaves every later
           getThreadLocalData() on this thread returning memory that has been given
           back -- a use-after-free introduced by the exhaustion check itself, and
           worse than the out-of-bounds write it replaced, because the thread would
           keep using the stale pointer instead of retrying. Cleared here rather than
           by moving the bind below the search: cn1TlsSelf is expected to name the
           host thread for the whole of the rest of this function. */
        if(bindToCallingOsThread) {
            pthread_setspecific(threadIdKey, NULL);
        }
        cn1FreeThreadLocalDataFields(i);
        return 0;
    }
    allThreads[threadOffset] = i;
    unlockCriticalSection();
    //printf("Thread slot %d assigned to thread %d\n",threadOffset,(int)i->threadId);

    return i;
}

/**
 * Create a virtual thread that can run Java: a stack of its own plus a VM thread
 * state of its own.
 *
 * The two halves are both necessary and neither is sufficient. The stack carries
 * the C activation records of the Java methods it is inside; the thread state
 * carries their locals and operand stack, which is where a request's objects
 * actually live. Giving it a stack but sharing the host's state would have two
 * threads of control writing one Java stack.
 *
 * EXPERIMENTAL. This pair -- cn1SpawnVirtualThread and cn1RetireVirtualThread --
 * is the VM-state half of virtual threads and is NOT FINISHED. Nothing in this
 * repository calls it; it ships so the server work can build against it, and its
 * lifecycle is designed against a real workload rather than guessed at. Review
 * findings against it are noted rather than patched, because each patch so far has
 * traded one hole for another (a scanning race became a collector hang; a bounds
 * fix became a use-after-free). The COROUTINE runtime it sits on --
 * cn1_virtual_thread.{h,c,S} and the collector's stack scanning -- is finished,
 * tested and used, and is not covered by this notice.
 *
 * Known and deliberately open, all of them waiting on one design decision (carrier
 * association, i.e. making the collector's stop handshake stop being per-TLD):
 *   - a collection can walk this state's object stack while its virtual thread
 *     mutates it;
 *   - retiring a virtual thread retires the CARRIER's BiBOP pages, because
 *     collectThreadResources works on thread-local state rather than the state it
 *     is handed;
 *   - the collector cannot stop a compute-only virtual thread at all, which is why
 *     marking such a state active is a hang rather than a fix;
 *   - a virtual thread registered after the collector's once-per-cycle registry
 *     snapshot is invisible to the stack scan until the next cycle;
 *   - past CN1_VT_SNAPSHOT_MAX (4096) registered virtual threads the snapshot is
 *     truncated, and the collector warns but continues, so the overflow is unscanned.
 *     Reaching that count needs this API, which is why it is listed here rather than
 *     fixed in the collector.
 *
 * KNOWN GAP, stated here because the obvious fix is worse than the problem. The
 * state this creates is never marked threadActive while its virtual thread runs,
 * so a collection concurrent with a running virtual thread can walk that state's
 * object stack and pending-allocation table while the virtual thread mutates them.
 * Raised in review on PR #5658, and NOT fixed by setting threadActive: the state
 * has no pthread of its own, the collector's while(threadActive) wait is unbounded,
 * and the forced-stop escalation that breaks such a wait is gated on
 * gcPthreadValid, which is permanently false here. Setting the flag converts a
 * possible race into a certain hang for any virtual thread that computes without
 * reaching a safepoint. That was measured against, not guessed: the flag was set,
 * and this is the reverted state.
 *
 * The real fix is carrier association -- while a virtual thread runs, its state is
 * executing ON a carrier that DOES have a stoppable pthread, so the collector
 * should satisfy the wait by stopping the carrier rather than the state. That needs
 * the stop handshake to stop being per-TLD (the signal handler records into the TLD
 * of the thread it runs on, which is the carrier's), which is a change to the
 * collector's stop protocol rather than to this function.
 *
 * The C stack half of the same report IS fixed, independently:
 * cn1GcScanParkedVirtualThreads scans every registered virtual thread whether or
 * not it is running, so no virtual stack goes unscanned.
 *
 * Sizing: threadObjectStack is mmap'd and lazily faulted, so the 264KB it
 * reserves costs only the pages a virtual thread touches -- a handler that nests
 * a dozen frames commits a page or two. That is the difference against the
 * ~118KB RESIDENT a parked OS thread costs, and it is what decides whether a
 * context per connection is affordable.
 */
#ifdef CN1_VIRTUAL_THREADS
struct cn1VirtualThread* cn1SpawnVirtualThread(cn1VirtualThreadBody body, void* arg,
                                               size_t stackBytes) {
    struct ThreadLocalData* state;
    struct cn1VirtualThread* vt = cn1VirtualThreadCreate(body, arg, stackBytes);
    if(vt == 0) {
        return 0;
    }
    // JAVA_FALSE: this state belongs to the virtual thread, not to whoever is
    // creating it. See cn1CreateThreadLocalData for what that turns off.
    state = cn1CreateThreadLocalData(JAVA_FALSE);
    if(state == 0) {
        cn1VirtualThreadFree(vt);
        return 0;
    }
    state->lightweightThread = JAVA_TRUE;
    cn1VirtualThreadSetState(vt, state);
    return vt;
}

/* All defined further down this file; the virtual-thread retire path and the
   capacity-failure path in cn1CreateThreadLocalData need them above their bodies. */
extern void markDeadThread(struct ThreadLocalData* d);
extern void cn1ReleaseThreadLocalData(struct ThreadLocalData* head);

/**
 * Retire a virtual thread produced by cn1SpawnVirtualThread, releasing BOTH halves.
 *
 * cn1VirtualThreadFree alone is not enough and the difference is not a small leak.
 * That function knows only about the coroutine: it unregisters it and releases the
 * stack. The VM thread state spawned alongside it holds a 264KB shadow stack, the
 * call-stack arrays and the pending-allocation table, and -- the part that ends the
 * process rather than merely growing it -- one of the NUMBER_OF_SUPPORTED_THREADS
 * slots in allThreads. A server that spawns a virtual thread per request and never
 * came through here would consume a slot per completed request and eventually trip
 * CODENAME_ONE_ASSERT(threadOffset > -1) in cn1CreateThreadLocalData.
 *
 * Must NOT be called from inside the virtual thread's own body: this releases the
 * stack that body is running on. Retire it from whoever resumed it, after
 * cn1VirtualThreadFinished reports true.
 */
void cn1RetireVirtualThread(struct cn1VirtualThread* vt) {
    struct ThreadLocalData* state;
    if(vt == 0) {
        return;
    }
    state = (struct ThreadLocalData*)cn1VirtualThreadState(vt);
    cn1VirtualThreadSetState(vt, 0);
    if(state != 0) {
        // Frees the allThreads slot and runs collectThreadResources, exactly as an
        // OS thread's death does.
        markDeadThread(state);
        // ALWAYS deferred, never freed here, and there is deliberately no
        // synchronous branch to fall into. collectThreadResources -- which
        // markDeadThread just called, and which has no early return -- sets
        // gcQueuedForDrain unconditionally, so the release is the drain's job.
        //
        // That is required rather than incidental, and the reason is worth stating
        // because a synchronous free reads as harmless once the allThreads slot is
        // cleared: codenameOneGCMark copies each ThreadLocalData* out of allThreads
        // under the critical section and then dereferences it OUTSIDE the lock, so
        // a mark already past that copy is still reading this state. The drain runs
        // at the START of a mark, after the previous one has finished, which is the
        // one point where no collector iteration can still hold the pointer.
        lockCriticalSection();
        state->gcReleaseRequested = JAVA_TRUE;
        unlockCriticalSection();
    }
    cn1VirtualThreadFree(vt);
}
#endif /* CN1_VIRTUAL_THREADS -- see the capability gate in cn1_virtual_thread.h */

struct ThreadLocalData* getThreadLocalData() {
    // A running virtual thread supplies its own state; every generated method
    // reaches its locals through this, so missing it would silently give the
    // virtual thread the HOST thread's Java stack and corrupt both.
    {
        struct cn1VirtualThread* __vt = cn1VirtualThreadCurrent();
        if(__vt != 0) {
            struct ThreadLocalData* __s = (struct ThreadLocalData*)cn1VirtualThreadState(__vt);
            if(__s != 0) {
                return __s;
            }
        }
    }
    if(threadIdKey == 0) {
        pthread_key_create(&threadIdKey, NULL);
    }
    struct ThreadLocalData* i = pthread_getspecific(threadIdKey);
    if(i == NULL) {
        i = cn1CreateThreadLocalData(JAVA_TRUE);
    }
    return i;
}

JAVA_LONG currentThreadId() {
    struct ThreadLocalData* i = getThreadLocalData();
    return i->threadId;
}

pthread_mutex_t* criticalSection = NULL;
pthread_mutex_t* getCriticalSection() {
    if(criticalSection == NULL) {
        criticalSection = malloc(sizeof(pthread_mutex_t));
        pthread_mutex_init(criticalSection, NULL);
    }
    return criticalSection;
}

void lockCriticalSection() {
    pthread_mutex_lock(getCriticalSection());
}

void unlockCriticalSection() {
    pthread_mutex_unlock(criticalSection);
}
pthread_mutex_t* threadHeapMutex = NULL;
pthread_mutex_t* getThreadHeapMutex() {
    if(threadHeapMutex == NULL) {
        threadHeapMutex = malloc(sizeof(pthread_mutex_t));
        pthread_mutex_init(threadHeapMutex, NULL);
    }
    return threadHeapMutex;
}

void lockThreadHeapMutex() {
    pthread_mutex_lock(getThreadHeapMutex());
}

void unlockThreadHeapMutex() {
    pthread_mutex_unlock(getThreadHeapMutex());
}

extern void flushReleaseQueue();
long gcThreadId = -1;
JAVA_VOID java_lang_System_gcLight__(CODENAME_ONE_THREAD_STATE) {
    gcThreadId = (long)threadStateData->threadId;
    flushReleaseQueue();
}

JAVA_BOOLEAN firstTimeGcThread = JAVA_TRUE;
JAVA_BOOLEAN gcCurrentlyRunning = JAVA_FALSE;
#ifdef CN1_GC_CONFORM
extern void cn1GcProbeCycle(double markMs, double sweepMs, int threw);
double cn1GcProbeMarkMs = 0, cn1GcProbeSweepMs = 0;
int cn1GcProbeThrew = 0;
#endif
JAVA_VOID java_lang_System_gcMarkSweep__(CODENAME_ONE_THREAD_STATE) {
    // FREEZE: refuse to start a cycle at all once the exit census has claimed the
    // heap. Clearing System.gcShouldLoop is not sufficient on its own -- the GC
    // thread may already have evaluated `while(gcShouldLoop)` and be on its way
    // here, and System's start-up path re-raises that flag after its initial wait.
    // Either way the census would see gcCurrentlyRunning false, start walking, and
    // have the pending cycle resume and sweep underneath it. Checked here because
    // this is the one door every cycle comes through.
    // CLAIM the cycle, do not merely check a flag. Loading a freeze flag and then
    // setting gcCurrentlyRunning is two steps, and the collector can be preempted
    // between them: the census would raise the freeze, see gcCurrentlyRunning still
    // false, and start walking a heap this thread is about to sweep. The claim below
    // is a single compare-exchange, so a cycle is either started or refused with
    // nothing observable in between.
    {
        int cn1Expected = CN1_GC_CYCLE_IDLE;
        if(!atomic_compare_exchange_strong_explicit(&cn1GcCycleState, &cn1Expected,
                CN1_GC_CYCLE_RUNNING, memory_order_acq_rel, memory_order_acquire)) {
            // In practice only the FROZEN case can be taken: System's GC thread is
            // the sole caller (System.java's `while(gcShouldLoop)` loop), so no second
            // entrant can observe RUNNING. Refusing on RUNNING too is defence rather
            // than policy -- two concurrent cycles would be worse than a skipped one --
            // and it means this is not a behaviour change for any existing caller.
            return;
        }
    }
    cn1RefBlockBeginCycle();
    gcCurrentlyRunning = JAVA_TRUE;
    if(firstTimeGcThread) {
        firstTimeGcThread = JAVA_FALSE;
        
        // reduce thread priority
        int policy;
        struct sched_param param;
        pthread_getschedparam(pthread_self(), &policy, &param);
        param.sched_priority--;
        pthread_setschedparam(pthread_self(), policy, &param);
    }
    flushReleaseQueue();
    // Defense in depth: a collection cycle must NEVER let an exception escape to the GC
    // thread's run loop (System$1.run only catches InterruptedException, so anything else
    // kills the collector -- and because the gcCurrentlyRunning=FALSE reset below would be
    // skipped and gcThreadInstance stays non-null, every allocating thread then deadlocks in
    // cn1BibopMaybeGc's backpressure spin). Individual hazards are contained at their source
    // (throwing finalizers are swallowed in freeAndFinalize), but wrap the whole cycle as a
    // backstop: on any throw, drop it, and still clear gcCurrentlyRunning so the collector
    // stays healthy and the next cycle retries. If MARK threw, SWEEP is skipped -- correct,
    // sweeping a partial mark would free reachable objects.
#ifdef CN1_GC_CONFORM
    // Cleared BEFORE the protected region. A cycle that throws jumps past the timing
    // assignments below, so without this the row would carry the previous cycle's markMs
    // and sweepMs beside the partial current cycle's phase counters -- two cycles in one
    // row, concealing the exceptional cycle, which is the one worth seeing. These are
    // file-scope, so the setjmp/longjmp indeterminate-local rule does not apply.
    cn1GcProbeMarkMs = 0;
    cn1GcProbeSweepMs = 0;
    cn1GcProbeThrew = 0;
#endif
    int __gcSavedTryBlock = threadStateData->tryBlockOffset;
    jmp_buf __gcTryJmp;
    if(CN1_TRY_SETJMP(__gcTryJmp) == 0) {
        threadStateData->blocks[threadStateData->tryBlockOffset].monitor = 0;
        threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass = 0; // catch-all
        threadStateData->blocks[threadStateData->tryBlockOffset].nativeBuffers = threadStateData->nativeBuffers;
        memcpy(threadStateData->blocks[threadStateData->tryBlockOffset].destination, __gcTryJmp, sizeof(jmp_buf));
        threadStateData->tryBlockOffset++;
#ifdef CN1_GC_INSTRUMENT
    extern long long cn1_instr_allocCount; extern int currentSizeOfAllObjectsInHeap;
    static long long markNs=0, sweepNs=0; static int gcCount=0;
    struct timespec _t0,_t1,_t2;
    clock_gettime(CLOCK_MONOTONIC,&_t0);
    codenameOneGCMark();
    clock_gettime(CLOCK_MONOTONIC,&_t1);
    codenameOneGCSweep();
    clock_gettime(CLOCK_MONOTONIC,&_t2);
    markNs  += (_t1.tv_sec-_t0.tv_sec)*1000000000LL+(_t1.tv_nsec-_t0.tv_nsec);
    sweepNs += (_t2.tv_sec-_t1.tv_sec)*1000000000LL+(_t2.tv_nsec-_t1.tv_nsec);
#ifdef CN1_GC_CONFORM
    // PER-CYCLE, not cumulative: "the pauses get longer" is a statement about the trend of
    // one cycle's cost, and a running total cannot express it.
    cn1GcProbeMarkMs  = ((_t1.tv_sec-_t0.tv_sec)*1000000000LL+(_t1.tv_nsec-_t0.tv_nsec)) / 1e6;
    cn1GcProbeSweepMs = ((_t2.tv_sec-_t1.tv_sec)*1000000000LL+(_t2.tv_nsec-_t1.tv_nsec)) / 1e6;
#endif
    gcCount++;
    // outOfLineAllocs, NOT allocations: cn1_instr_allocCount is bumped in
    // codenameOneGcMalloc, and CN1_FAST_NEW's inlined bump path never reaches it. On a
    // small-object workload -- the shape issue 5537 reported -- that is the overwhelming
    // majority of allocation, so reading this as a total understates it by orders of
    // magnitude. cn1AllocCensus (CN1_ALLOC_CENSUS) counts at every entry point.
    if(gcCount==1 || (gcCount % 20)==0) fprintf(stderr,"[GC-INSTR] cycles=%d outOfLineAllocs=%lld heapTableSize=%d markMs=%.0f sweepMs=%.0f\n",
        gcCount, cn1_instr_allocCount, currentSizeOfAllObjectsInHeap, markNs/1e6, sweepNs/1e6);
#else
    codenameOneGCMark();
    codenameOneGCSweep();
#endif
        threadStateData->tryBlockOffset = __gcSavedTryBlock;
    } else {
        threadStateData->tryBlockOffset = __gcSavedTryBlock;
        threadStateData->exception = JAVA_NULL;
#ifdef CN1_GC_CONFORM
        cn1GcProbeThrew = 1;
#endif
    }
    flushReleaseQueue();
#ifdef CN1_GC_CONFORM
    cn1GcProbeCycle(cn1GcProbeMarkMs, cn1GcProbeSweepMs, cn1GcProbeThrew);
#endif
#ifdef CN1_ALLOC_CENSUS
    {
        // Several points, not one: allocation during startup and allocation once
        // the first screen is up are different questions, and a single sample
        // cannot tell them apart.
        extern void cn1AllocCensus(const char*);
        extern void cn1HeapAccounting(const char*);
        static int cn1CensusCycle = 0;
        int c = ++cn1CensusCycle;
        if(c == 3 || c == 10 || c == 25 || c == 50) {
            char lbl[32];
            snprintf(lbl, sizeof(lbl), "cycle%d", c);
            cn1AllocCensus(lbl);
            cn1HeapAccounting(lbl);
        }
    }
#endif
#ifdef CN1_HEAP_HISTOGRAM
    {
        // Once, on the third cycle: the first two run while the first screen is
        // still being built, and a census of a half-built heap names the wrong
        // things.
        extern void cn1HeapHistogramPublic(void);
        static int cn1HistCycle = 0;
        if(++cn1HistCycle == 3) {
            cn1HeapHistogramPublic();
        }
    }
#endif
    // No call reclaims the emptied pages. A sweep frees objects with free(),
    // and libmalloc keeps most of the emptied pages in its zone rather than
    // returning them to the kernel -- they stay counted against the process.
    // malloc_zone_pressure_relief looks like the answer and is not: measured on
    // macOS 26, a program that mallocs 200,000 x 500 bytes, touches them and
    // frees every one sits at 52.9MB of physical footprint, and stays at
    // exactly 52.9MB after malloc_zone_pressure_relief(NULL, 0) AND after
    // calling it on every zone from malloc_get_all_zones. Three revisions of
    // this file called it here on a countdown (every sixteenth cycle, every
    // eighth, then every cycle) and the idle remainder never moved: 45.8MB of
    // "MALLOC_SMALL (empty)" against 0.8MB for the same application built with
    // a competing toolchain. The retained pages are a function of how many
    // small blocks were ever live at once, so the only lever is allocating
    // fewer of them -- see the object allocator, which keeps Java objects out
    // of malloc entirely for exactly this reason.
    lowMemoryMode = JAVA_FALSE;
    cn1RefBlockEndCycle();
    gcCurrentlyRunning = JAVA_FALSE;
    // Release the claim. Only ever RUNNING -> IDLE: a census that froze while this
    // cycle ran holds the state at FROZEN and this must not clobber it, which is why
    // the transition is a compare-exchange rather than a store.
    {
        int cn1Running = CN1_GC_CYCLE_RUNNING;
        atomic_compare_exchange_strong_explicit(&cn1GcCycleState, &cn1Running,
                CN1_GC_CYCLE_IDLE, memory_order_acq_rel, memory_order_relaxed);
    }
}

JAVA_VOID java_lang_System_exit___int(CODENAME_ONE_THREAD_STATE, JAVA_INT i) {
    exit(i);
}

JAVA_VOID monitorEnter(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    // TAGGED Integers lock like any other object: the monitor lives in the
    // pointer-keyed side table below and a tagged value is a perfectly stable
    // key -- it never moves and never gets collected. Making tagged locks a
    // no-op (the previous behavior) broke Java synchronization semantics:
    // synchronized (Integer.valueOf(1)) let every thread through at once, and
    // wait()/notify() on the value dereferenced nonexistent monitor data.
    // Equal tagged values share one monitor (identical bit pattern), which is
    // the JDK's own -128..127 Integer-cache behavior extended to the whole
    // tagged range -- legal and deterministic. The only header-touching step,
    // cn1BibopNoteMonitorAttached, skips tagged values internally.
    //
    // MONITORS ON TAGGED VALUES ARE NEVER RECLAIMED. There is no object death to
    // trigger removal, so `synchronized (Float.valueOf(i))` over a loop leaves one
    // side-table entry per distinct value, forever. Now that Long, Double, Float,
    // Character and Short are tagged too, that reaches five types whose monitors a
    // heap box did used to get reclaimed -- so this is a real widening, not just a
    // restatement of the Integer case. Reviewers reasonably ask for lifecycle
    // tracking; deliberately not done, for three reasons:
    //
    //  - The shape is ALREADY unbounded for Integer and always has been. Removal
    //    would be a new mechanism for every monitor in the VM, not a tagged-only
    //    patch, because nothing here reclaims a monitor at monitorExit today.
    //  - This subsystem is where the documented three-way monitorEnter deadlock
    //    lives (see the first-creation branch below). Adding exit-time removal
    //    means a waiter can be parked outside the table mutex holding monitor data
    //    a remover is about to free. That is a correctness risk taken on for a
    //    leak, which is the wrong trade.
    //  - The DIRECT form is refused at BUILD time. BytecodeComplianceMojo rejects
    //    MONITORENTER whose operand is statically typed as one of the eight primitive
    //    wrappers, and BytecodeComplianceMojoTest proves it fires for each type rather
    //    than for Integer alone.
    //
    // That build check is a PARTIAL mitigation and should not be described as more.
    // The analysis is intra-procedural and types-only, so passing Float.valueOf(i) to a
    // helper that takes Object and synchronizing on the parameter walks straight past
    // it -- the operand is typed Object and the build passes. Storing the box in a
    // field, an array or a collection defeats it the same way. Tracking erased wrapper
    // origins would catch the easiest of those and lose to the rest, which is why it is
    // a gate against the obvious mistake rather than a proof of unreachability.
    //
    // So the leak IS reachable from application code, and it is accepted rather than
    // solved. It is bounded by the number of distinct boxed values a program ever uses
    // as a lock, which is zero for every program that follows the rule the gate states.
    // Framework and JavaAPI code is not scanned by that mojo at all; there is no
    // `synchronized` on a boxed value anywhere in CodenameOne/src, vm/JavaAPI/src or
    // Ports today, and adding one would reintroduce this. Use a dedicated lock object.
    int err = 0;
    // Double-checked locking for the lazily allocated per-object monitor. The fast-path
    // read MUST be an acquire load and the publishing store (inside the critical section)
    // MUST be a release store: otherwise a second thread can observe the
    // __codenameOneThreadData pointer the instant malloc() returns -- before
    // pthread_mutex_init() has run on it -- and then pthread_mutex_lock() an
    // uninitialized mutex. On ARM's weak memory model that hangs forever (observed as the
    // EDT wedged in monitorEnter while logging an exception, which freezes the whole app).
    // Build the struct fully, init its mutex/condition, and only then publish into the
    // monitor side table. The table's own mutex (taken by Get/Set) supplies the
    // acquire/release ordering the header atomics used to provide, so a thread that reads
    // the entry from the table always sees the fully-initialized mutex.
    struct CN1ThreadData* data = (struct CN1ThreadData*)cn1MonitorDataGet(obj);
    if(!data) {
        lockCriticalSection();
        data = (struct CN1ThreadData*)cn1MonitorDataGet(obj);
        if(!data) {
            data = malloc(sizeof(struct CN1ThreadData));
            memset(data, 0, sizeof(struct CN1ThreadData));
            pthread_mutex_init(&data->__codenameOneMutex, NULL);
            pthread_cond_init(&data->__codenameOneCondition, NULL);
            cn1MonitorDataSet(obj, data);
#if !defined(CN1_DISABLE_BIBOP) && !defined(CN1_BIBOP_NO_FASTSWEEP)
            // If obj is a BiBOP slot, flag that a monitor now needs freeing at reclaim so
            // the O(1) all-dead page shortcut is suppressed until it is freed.
            cn1BibopNoteMonitorAttached(obj);
#endif
        }
        unlockCriticalSection();
        err = pthread_mutex_lock(&data->__codenameOneMutex);
        data->ownerThread = CN1_MONITOR_SELF();
        data->counter++;
    } else {
        // The reentrancy identity MUST be the pthread, not threadStateData->threadId:
        // one pthread can legitimately run under two ThreadLocalData structs (the
        // main-thread-hosted EDT executes with the CodenameOneThread's state passed
        // explicitly, while any native helper that calls getThreadLocalData() gets the
        // pthread's own TLS struct). With id-based ownership a static-field accessor
        // running inside that thread's own class initializer compared different ids,
        // missed the reentrant case, and pthread_mutex_lock'd the mutex the same
        // pthread already held -- observed as the EDT freezing in
        // __STATIC_INITIALIZER_BufferedOutputStream while logging its first throwable.
        JAVA_LONG own = CN1_MONITOR_SELF();
        JAVA_LONG currentlyHeldBy = data->ownerThread;

        // we already own the lock...
        if(currentlyHeldBy == own) {
            data->counter++;
            return;
        }
        // NO try-lock fast path here. It was tried and reverted; do not restore
        // it without solving all three of these.
        //
        // The idea was that a lock which does not block need not announce a park,
        // saving the handshake wait on every uncontended synchronized call --
        // measured at 8.7ms of blocked event dispatch thread per launch. What it
        // actually cost:
        //
        //  - pthread_mutex_trylock is not in the Windows compatibility layer, so
        //    every Windows target failed to compile until it was added.
        //  - Returning early skipped the GC HANDSHAKE, and monitorEnter is one of
        //    the few safepoints a long-running loop is guaranteed to reach, so a
        //    loop synchronizing in its body could spin forever while a
        //    stop-the-world collector waited for it.
        //  - Returning early also skipped the threadActive = TRUE below. A thread
        //    that entered here already marked inactive then ran Java while the
        //    collector believed it was parked, which is heap corruption: the
        //    collector scans a stale stack and reclaims objects that are live.
        //    Seen as a SIGSEGV on a wild pointer inside ArrayList.remove, called
        //    from the event dispatch loop -- nowhere near a monitor.
        //
        // The saving is real but small, and it is not worth reintroducing without
        // a way to prove the thread-state invariants hold.
        threadStateData->threadActive = JAVA_FALSE;
        err = pthread_mutex_lock(&data->__codenameOneMutex);
        data->counter++;
        data->ownerThread = own;
        CN1_GC_WAIT_UNBLOCKED(threadStateData);
        threadStateData->threadActive = JAVA_TRUE;


    }
    //printf("Locking mutex %i started from %@", (int)obj->__codenameOneMutex, [NSThread callStackSymbols]);
    //printf("Locking mutex %i completed", (int)obj->__codenameOneMutex);
    if(err != 0) {
        printf("Error with lock %i EINVAL %i, ETIMEDOUT %i, EPERM %i\n", err, EINVAL, ETIMEDOUT, EPERM);
    }
}

// monitorEnterBlock is used for synchronized methods because the JVM bytecode
// doesn't actually generate the "try/catch" blocks for us like they do with 
// synchronized blocks.  monitorEnterBlock will add a "block" to the block
// stack so that throwException() can exit the block in the case that an exception
// is thrown.
JAVA_VOID monitorEnterBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    monitorEnter(threadStateData, obj);
    threadStateData->blocks[threadStateData->tryBlockOffset].monitor = obj;
    threadStateData->tryBlockOffset++;
}

JAVA_VOID monitorExit(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    // Tagged Integers unlock through the same side-table monitor monitorEnter
    // created for the value (see the comment there); no header access happens
    // on this path.
    //printf("Unlocked mutex %i ", (int)obj->__codenameOneMutex);
    // remove the ownership of the thread
    struct CN1ThreadData* data = (struct CN1ThreadData*)cn1MonitorDataGet(obj);
    data->counter--;
    if(data->counter > 0) {
        return;
    }
    data->ownerThread = 0;
    int err = pthread_mutex_unlock(&data->__codenameOneMutex);
    if(err != 0) {
        printf("Error with unlock %i EINVAL %i, ETIMEDOUT %i, EPERM %i\n", err, EINVAL, ETIMEDOUT, EPERM);
    }
}

// A tagged value never allocates, so nothing else runs the boxed class's <clinit> -- and
// that initializer is what fills the vtable a later hashCode/equals/compareTo on the
// immediate dispatches through. Each valueOf below therefore forces it once.
//
// The flag is a FAST PATH, and both of its edges have to be ordered.
//
// Publication: `volatile` in C is neither atomic nor ordered, so a plain flag lets a second
// thread observe 1 while the initializer's vtable and static-field writes are still
// invisible to it on a weak-memory target, and the next virtual call dispatches through
// stale class state. Release on publish, acquire on read -- the same pairing
// CN1_CONSTANT_POOL_LOAD documents in cn1_globals.h, and for the same reason.
//
// Acquisition: release/acquire only carries what the PUBLISHING thread itself observed, and
// __STATIC_INITIALIZER_X is not a reliable synchronisation point. Its generated fast path
// (ByteCodeClass.emitClassInitializer) is `if(__X_LOADED__) return;` -- a plain load -- and
// the matching `__X_LOADED__=1` is a plain store placed AFTER monitorExitBlock. A thread
// that returns through that path has taken no lock and may hold none of the initialising
// thread's writes, so publishing our flag from it would hand a stale view to everyone who
// acquires it. Taking the class monitor here makes the publisher synchronise-with whoever
// actually ran the body; monitors are reentrant (see monitorEnter's ownerThread check), so
// the initializer's own enter nests harmlessly. It costs one uncontended lock per class per
// process -- the flag short-circuits every call after the first -- and the hot path is
// unchanged at a single acquire load.
//
// This closes the hole for the six boxed classes only. `__X_LOADED__` is a plain-load,
// plain-store double-check on EVERY generated class initializer in the VM, which predates
// tagging and is not fixed here.
#if CN1_TAGGED_ACTIVE
/* THE FLAG IS A FILE-SCOPE GLOBAL, NOT A FUNCTION-LOCAL STATIC, AND THAT IS THE
 * POINT. valueOf is the whole construction of a tagged box -- it should compile to
 * a shift and an OR -- but a function holding a MUTABLE STATIC LOCAL cannot be
 * freely duplicated, so ThinLTO declines to inline it across modules. It therefore
 * showed up as its own frame at 17% of mutator time in a HashMap workload that
 * calls it 6M times per rep, for a value type that is supposed to be free.
 * Hoisting the flag to file scope removes the obstacle; the semantics are
 * identical (one flag per class, acquire on read, release on publish). */
#define CN1_DECLARE_BOX_CLINIT_FLAG(cls) static int cn1__clinit_##cls = 0;
#define CN1_FORCE_BOX_CLINIT(cls) \
    do { \
        if(__builtin_expect(!__atomic_load_n(&cn1__clinit_##cls, __ATOMIC_ACQUIRE), 0)) { \
            monitorEnterBlock(threadStateData, (JAVA_OBJECT)&class__##cls); \
            __STATIC_INITIALIZER_##cls(threadStateData); \
            monitorExitBlock(threadStateData, (JAVA_OBJECT)&class__##cls); \
            __atomic_store_n(&cn1__clinit_##cls, 1, __ATOMIC_RELEASE); \
        } \
    } while(0)
#endif

// ---- Tagged small-integer support (poor man's Valhalla, 64-bit pointers only) ----
// Integer.valueOf returns an immediate tagged pointer instead of allocating; cn1Value
// recovers the int from either a tagged immediate or a heap Integer's field. When the
// optimization is off (or on a 32-bit-pointer target) these behave exactly as before:
// valueOf delegates to the cached heap path and cn1Value just reads the field.
#if CN1_TAGGED_ACTIVE
extern void __STATIC_INITIALIZER_java_lang_Integer(CODENAME_ONE_THREAD_STATE);
#endif
#if CN1_TAGGED_ACTIVE
CN1_DECLARE_BOX_CLINIT_FLAG(java_lang_Integer)
CN1_DECLARE_BOX_CLINIT_FLAG(java_lang_Long)
CN1_DECLARE_BOX_CLINIT_FLAG(java_lang_Double)
CN1_DECLARE_BOX_CLINIT_FLAG(java_lang_Float)
CN1_DECLARE_BOX_CLINIT_FLAG(java_lang_Character)
CN1_DECLARE_BOX_CLINIT_FLAG(java_lang_Short)
#endif

JAVA_OBJECT java_lang_Integer_valueOf___int_R_java_lang_Integer(CODENAME_ONE_THREAD_STATE, JAVA_INT i) {
#if CN1_TAGGED_ACTIVE
    CN1_FORCE_BOX_CLINIT(java_lang_Integer);
    return CN1_TAG_INT(i);
#else
    return java_lang_Integer_valueOfHeap___int_R_java_lang_Integer(threadStateData, i);
#endif
}

JAVA_INT java_lang_Integer_cn1Value___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if CN1_TAGGED_ACTIVE
    if(CN1_IS_TAGGED(__cn1ThisObject)) {
        return CN1_UNTAG_INT(__cn1ThisObject);
    }
#endif
    return ((struct obj__java_lang_Integer*)__cn1ThisObject)->java_lang_Integer_value;
}

// ---- The other five tagged boxes ----
// Same shape as Integer above, and the same two obligations at every valueOf:
//
//  * Force the class's static initializer once. A tagged value never allocates, so nothing
//    else runs the clinit -- and the clinit is what fills the vtable that dispatching
//    hashCode/equals/compareTo on the immediate reads through cn1ClassOf.
//  * Fall back to valueOfHeap when the value cannot be represented. Only Long and Double
//    have a fallback; Float, Character and Short always fit.
//
// cn1Value is the single door every read of the boxed value goes through. It has to be a
// native and not a Java getter: these classes are final, so Invoke.asInlinableFieldAccess
// would fold `return value;` into a raw GETFIELD off a pointer that has no fields.
#if CN1_TAGGED_ACTIVE
extern void __STATIC_INITIALIZER_java_lang_Long(CODENAME_ONE_THREAD_STATE);
extern void __STATIC_INITIALIZER_java_lang_Double(CODENAME_ONE_THREAD_STATE);
extern void __STATIC_INITIALIZER_java_lang_Float(CODENAME_ONE_THREAD_STATE);
extern void __STATIC_INITIALIZER_java_lang_Character(CODENAME_ONE_THREAD_STATE);
extern void __STATIC_INITIALIZER_java_lang_Short(CODENAME_ONE_THREAD_STATE);
#endif

JAVA_OBJECT java_lang_Long_valueOf___long_R_java_lang_Long(CODENAME_ONE_THREAD_STATE, JAVA_LONG i) {
#if CN1_TAGGED_EXTRA_ACTIVE
    if(CN1_LONG_TAGGABLE(i)) {
        CN1_FORCE_BOX_CLINIT(java_lang_Long);
        return CN1_TAG_LONG_VAL(i);
    }
#endif
    return java_lang_Long_valueOfHeap___long_R_java_lang_Long(threadStateData, i);
}

JAVA_LONG java_lang_Long_cn1Value___R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if CN1_TAGGED_ACTIVE
    if(CN1_TAG_CODE(__cn1ThisObject) == CN1_TAG_LONG) {
        return CN1_UNTAG_LONG(__cn1ThisObject);
    }
#endif
    return ((struct obj__java_lang_Long*)__cn1ThisObject)->java_lang_Long_value;
}

JAVA_OBJECT java_lang_Double_valueOf___double_R_java_lang_Double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE d) {
#if CN1_TAGGED_EXTRA_ACTIVE
    union { JAVA_DOUBLE d; uint64_t b; } u;
    u.d = d;
    if(CN1_DOUBLE_TAGGABLE_BITS(u.b)) {
        CN1_FORCE_BOX_CLINIT(java_lang_Double);
        return CN1_TAG_DOUBLE_BITS(u.b);
    }
#endif
    return java_lang_Double_valueOfHeap___double_R_java_lang_Double(threadStateData, d);
}

JAVA_DOUBLE java_lang_Double_cn1Value___R_double(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if CN1_TAGGED_ACTIVE
    if(CN1_TAG_CODE(__cn1ThisObject) == CN1_TAG_DOUBLE) {
        union { JAVA_DOUBLE d; uint64_t b; } u;
        u.b = CN1_UNTAG_DOUBLE_BITS(__cn1ThisObject);
        return u.d;
    }
#endif
    return ((struct obj__java_lang_Double*)__cn1ThisObject)->java_lang_Double_value;
}

JAVA_OBJECT java_lang_Float_valueOf___float_R_java_lang_Float(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT f) {
#if CN1_TAGGED_EXTRA_ACTIVE
    union { JAVA_FLOAT f; uint32_t b; } u;
    u.f = f;
    {
        CN1_FORCE_BOX_CLINIT(java_lang_Float);
    }
    return CN1_TAG_FLOAT_BITS(u.b);
#else
    return java_lang_Float_valueOfHeap___float_R_java_lang_Float(threadStateData, f);
#endif
}

JAVA_FLOAT java_lang_Float_cn1Value___R_float(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if CN1_TAGGED_ACTIVE
    if(CN1_TAG_CODE(__cn1ThisObject) == CN1_TAG_FLOAT) {
        union { JAVA_FLOAT f; uint32_t b; } u;
        u.b = CN1_UNTAG_FLOAT_BITS(__cn1ThisObject);
        return u.f;
    }
#endif
    return ((struct obj__java_lang_Float*)__cn1ThisObject)->java_lang_Float_value;
}

JAVA_OBJECT java_lang_Character_valueOf___char_R_java_lang_Character(CODENAME_ONE_THREAD_STATE, JAVA_CHAR c) {
#if CN1_TAGGED_EXTRA_ACTIVE
    {
        CN1_FORCE_BOX_CLINIT(java_lang_Character);
    }
    return CN1_TAG_CHAR_VAL(c);
#else
    return java_lang_Character_valueOfHeap___char_R_java_lang_Character(threadStateData, c);
#endif
}

JAVA_CHAR java_lang_Character_cn1Value___R_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if CN1_TAGGED_ACTIVE
    if(CN1_TAG_CODE(__cn1ThisObject) == CN1_TAG_CHARACTER) {
        return CN1_UNTAG_CHAR(__cn1ThisObject);
    }
#endif
    return ((struct obj__java_lang_Character*)__cn1ThisObject)->java_lang_Character_value;
}

JAVA_OBJECT java_lang_Short_valueOf___short_R_java_lang_Short(CODENAME_ONE_THREAD_STATE, JAVA_SHORT v) {
#if CN1_TAGGED_EXTRA_ACTIVE
    {
        CN1_FORCE_BOX_CLINIT(java_lang_Short);
    }
    return CN1_TAG_SHORT_VAL(v);
#else
    return java_lang_Short_valueOfHeap___short_R_java_lang_Short(threadStateData, v);
#endif
}

JAVA_SHORT java_lang_Short_cn1Value___R_short(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if CN1_TAGGED_ACTIVE
    if(CN1_TAG_CODE(__cn1ThisObject) == CN1_TAG_SHORT) {
        return CN1_UNTAG_SHORT(__cn1ThisObject);
    }
#endif
    return ((struct obj__java_lang_Short*)__cn1ThisObject)->java_lang_Short_value;
}

// monitorEnterBlock is used for synchronized methods because the JVM bytecode
// doesn't actually generate the "try/catch" blocks for us like they do with 
// synchronized blocks.  monitorEnterBlock will add a "block" to the block
// stack so that throwException() can exit the block in the case that an exception
// is thrown.
JAVA_VOID monitorExitBlock(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    threadStateData->tryBlockOffset--;
    monitorExit(threadStateData, obj);
}

JAVA_VOID java_lang_Object_wait___long_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj, JAVA_LONG timeout, JAVA_INT nanos) {
    //printf("Waiting on mutex %i with timeout %i started", (int)obj->__codenameOneMutex, (int)timeout);
    threadStateData->threadActive = JAVA_FALSE;

    struct CN1ThreadData* data = (struct CN1ThreadData*)cn1MonitorDataGet(obj);
    int counter;
    counter = data->counter;

    // remove the ownership of the thread
    data->ownerThread = 0;
    data->counter = 0;

    int errCode = 0;
    if(timeout == 0 && nanos == 0) {
        errCode = pthread_cond_wait(&data->__codenameOneCondition, &data->__codenameOneMutex);
        if(errCode != 0) {
            printf("Error with wait %i EINVAL %i, ETIMEDOUT %i, EPERM %i\n", errCode, EINVAL, ETIMEDOUT, EPERM);
        }
    } else {
        struct timeval   tv;
        gettimeofday(&tv, NULL);
        struct timespec   ts;
        /* Built in 64-bit throughout. `timeout` is a JAVA_LONG and time_t is
           64-bit everywhere we run, but `long` is only 32 bits on Windows
           (LLP64) -- so casting the seconds through it truncated
           Long.MAX_VALUE / 1000 (9223372036854775) to -1511828489, putting the
           deadline about 48 years in the PAST. wait(Long.MAX_VALUE), the
           ordinary park-until-notified idiom, therefore returned immediately
           instead of blocking until notified. LP64 platforms were unaffected,
           which is why this only ever showed up on Windows.

           The addend is capped so the deadline cannot overflow time_t on any
           platform; ~34,000 years is indistinguishable from never for a wait,
           and the condition-variable wrapper chunks it down to timeouts the
           host API can express. */
        JAVA_LONG addSeconds = timeout / 1000;
        if (addSeconds > ((JAVA_LONG)1 << 40)) {
            addSeconds = ((JAVA_LONG)1 << 40);
        }
        /* Normalized in 64-bit as well: tv_usec * 1000 plus the sub-second part
           of the timeout plus nanos can exceed 2e9, which no longer fits a
           32-bit tv_nsec, and a single subtract-one-second could not normalize
           it anyway. */
        JAVA_LONG nsec = (JAVA_LONG)tv.tv_usec * 1000
                       + (timeout % 1000) * 1000000
                       + (JAVA_LONG)nanos;
        ts.tv_sec = (time_t)((JAVA_LONG)tv.tv_sec + addSeconds + nsec / 1000000000);
        ts.tv_nsec = (long)(nsec % 1000000000);
        pthread_cond_timedwait(&data->__codenameOneCondition, &data->__codenameOneMutex, &ts);
    }

    while(threadStateData->threadBlockedByGC) {
        struct timeval   tv;
        gettimeofday(&tv, NULL);
        struct timespec   ts;
        ts.tv_sec = tv.tv_sec;
        ts.tv_nsec = (tv.tv_usec * 1000) + 2000000;
        if ( ts.tv_nsec > 1000000000 ){
            ts.tv_nsec -= 1000000000;
            ts.tv_sec++;
        }
        pthread_cond_timedwait(&data->__codenameOneCondition, &data->__codenameOneMutex, &ts);
    }

    // restore the ownership of the thread
    data->ownerThread = CN1_MONITOR_SELF();
    data->counter = counter;
    
    threadStateData->threadActive = JAVA_TRUE;
    //printf("Waiting on mutex %i with timeout %i finished", (int)obj->__codenameOneMutex, (int)timeout);
}

JAVA_VOID java_lang_Object_notify__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    //printf("Notifying mutex %i", (int)obj->__codenameOneMutex);
    pthread_cond_signal(&((struct CN1ThreadData*)cn1MonitorDataGet(obj))->__codenameOneCondition);
}

JAVA_VOID java_lang_Object_notifyAll__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT obj) {
    //printf("Notifying all mutex threads %i", (int)obj->__codenameOneMutex);
    pthread_cond_broadcast(&((struct CN1ThreadData*)cn1MonitorDataGet(obj))->__codenameOneCondition);
}

JAVA_VOID java_lang_Thread_setPriorityImpl___int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT t, JAVA_INT p) {
}

/* Every buffer a thread state owns, and the state itself. Shared with the failure
   path in cn1CreateThreadLocalData, which must NOT touch nThreadsToKill -- a state
   that never reached allThreads was never counted as living. */
static void cn1FreeThreadLocalDataFields(struct ThreadLocalData *head) {
    free(head->blocks);
    /* Free it the way it was ALLOCATED -- see cn1AllocThreadStack, which falls back
       to calloc when mmap is out of mappings. Neither mismatch is survivable: free()
       on a mapping is undefined behaviour, and munmap on an allocator block leaks the
       stack at best. */
    cn1FreeThreadStack(head->threadObjectStack, head->threadObjectStackMapped);
    free(head->callStackClass);
    free(head->callStackLine);
    free(head->callStackMethod);
#ifdef CN1_ON_DEVICE_DEBUG
    free(head->callStackLocalsAddresses);
    free(head->callStackFrameInfo);
#endif
    free(head->pendingHeapAllocations);
    free(head);
}

void cn1ReleaseThreadLocalData(struct ThreadLocalData *head) {
    cn1FreeThreadLocalDataFields(head);
    nThreadsToKill--;
}

JAVA_VOID java_lang_Thread_releaseThreadNativeResources___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG nativeThreadStruct) {
    // if a thread object was created and never started, it will still become garbage
    // and will still be finalized.  In that case, it never had resources allocated at all.
    if(nativeThreadStruct!=0)
    {
    struct ThreadLocalData *head = (struct ThreadLocalData *)nativeThreadStruct;
    lockCriticalSection();
    if(head->gcQueuedForDrain) {
        // The TLD is still on the GC's dead-thread queue: its pendingHeapAllocations
        // have not been migrated into allObjectsInHeap yet (this finalizer can run
        // in the same cycle the thread died, when death lands mid-mark after the
        // drain point). Freeing now would hand the drain a dangling TLD -- defer
        // the free to cn1DrainDeadThreadPending, which runs it after migration.
        head->gcReleaseRequested = JAVA_TRUE;
        unlockCriticalSection();
        return;
    }
    unlockCriticalSection();
    cn1ReleaseThreadLocalData(head);
    }
}

// Monotonic microsecond clock for the sleep deadline. Monotonic on BOTH
// targets: the MSVC / clang-cl side goes through the QueryPerformanceCounter
// shim in cn1_win_compat (gettimeofday here would tie the deadline to the
// wall clock, so a system time step would stretch or cut a sleep-in-progress).
static JAVA_LONG cn1SleepNowMicros(void) {
#ifdef _WIN32
    return (JAVA_LONG)cn1_monotonic_micros();
#else
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (((JAVA_LONG)ts.tv_sec) * 1000000LL) + (JAVA_LONG)(ts.tv_nsec / 1000);
#endif
}

// Bound to Thread.sleepImpl: the public Thread.sleep(long) is Java code that
// throws IllegalArgumentException for negative millis (per the JDK contract)
// before delegating here, so millis is always >= 0 at this point.
JAVA_VOID java_lang_Thread_sleepImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG millis) {
    // Park per the CN1_YIELD_THREAD contract (cn1_globals.h): capture SP +
    // callee-saved registers FIRST, then publish threadActive=FALSE. Under
    // CN1_CONSERVATIVE_GC_ROOTS this lets the collector scan the sleeper's
    // native stack cooperatively; on Windows there is NO signal-stop fallback,
    // so without the capture a sleeping thread's native-stack roots would go
    // unscanned for the cycle. No-op when conservative roots are off.
    CN1_GC_PARK_CAPTURE(threadStateData);
    threadStateData->threadActive = JAVA_FALSE;
    // usleep()/nanosleep() return EINTR when any signal lands, and POSIX never
    // restarts them (SA_RESTART explicitly excludes them). The conservative-roots
    // collector SIGNAL-STOPS sleeping threads every cycle to scan their native
    // stacks (a sleeper has no cooperative park capture), so the old single
    // usleep() call effectively slept only until the next collection or any other
    // process signal: Thread.sleep(600000) was measured returning after ~20ms on
    // the Linux port under allocation churn. java.util.Timer schedules through
    // Thread.sleep, so every TimerTask fired almost immediately -- observed as
    // ToastBar's "practically unexpiring" status dismissing itself mid-test
    // (issue-5425 PR, ToastBarTopPosition CI failure). Sleep on a monotonic
    // deadline and resume across early wakeups. Chunked below 1s because POSIX
    // allows usleep() to reject arguments >= 1000000 outright (EINVAL on musl),
    // which silently turned long sleeps into no-ops on those libcs.
    // INTERRUPT SEMANTICS UNCHANGED: this VM has never delivered
    // InterruptedException from sleep (interrupt() only sets the flag), so the
    // resume loop deliberately does NOT exit early on it -- waking without the
    // exception would be a third behavior, neither historical nor Java's.
    // Proper interrupt delivery is a separate change.
    // Saturate instead of overflowing: Thread.sleep(Long.MAX_VALUE) is a real
    // "park this thread" idiom, and millis * 1000 would wrap negative and make
    // it return immediately.
    JAVA_LONG now = cn1SleepNowMicros();
    JAVA_LONG maxMicros = 0x7fffffffffffffffLL;
    JAVA_LONG deltaMicros = (millis > maxMicros / 1000) ? maxMicros : millis * 1000;
    JAVA_LONG deadline = (deltaMicros > maxMicros - now) ? maxMicros : now + deltaMicros;
    for(;;) {
        JAVA_LONG remainMicros = deadline - cn1SleepNowMicros();
        if(remainMicros <= 0) {
            break;
        }
        if(remainMicros > 500000) {
            remainMicros = 500000;
        }
        // JAVA_INT cast, not useconds_t: the MSVC/clang-cl target's cn1_win_compat
        // usleep shim has no useconds_t typedef (same reason the old code cast here)
        usleep((JAVA_INT)remainMicros);
    }
    CN1_GC_WAIT_UNBLOCKED(threadStateData);
    threadStateData->threadActive = JAVA_TRUE;
#ifdef CN1_CONSERVATIVE_GC_ROOTS
    // Mirror CN1_RESUME_THREAD: drop the capture so a stale SP can never
    // satisfy the scanner's cooperative path after this frame unwinds.
    threadStateData->gcParkCaptured = JAVA_FALSE;
#endif
}

JAVA_OBJECT java_lang_Thread_currentThread___R_java_lang_Thread(CODENAME_ONE_THREAD_STATE) {
    if(threadStateData->currentThreadObject == JAVA_NULL) {
        threadStateData->currentThreadObject = __NEW_INSTANCE_java_lang_Thread(threadStateData);
    }
    return threadStateData->currentThreadObject;
}
extern void collectThreadResources(struct ThreadLocalData *current);
void markDeadThread(struct ThreadLocalData *d)
{
    lockCriticalSection();
    int found = -1;
    for(int iter = 0 ; iter < NUMBER_OF_SUPPORTED_THREADS ; iter++) {
        if(allThreads[iter] == d) {
            allThreads[iter] = 0;
            d->threadKilled = JAVA_TRUE;
            d->threadActive = JAVA_FALSE;
            found = iter;
            nThreadsToKill++;
#ifdef CN1_GC_CONFORM
            // Bank this thread's lifetime before its TLD leaves allThreads, so the duty
            // denominator keeps it. Mirrors cn1StallMutatorNs on the numerator side: both
            // have to survive the thread that earned them, and both count only lightweight
            // non-collector threads so the two describe one population.
            { extern void cn1StallRetireThread(struct ThreadLocalData* t);
              cn1StallRetireThread(d); }
#endif
            collectThreadResources(d);
            break;
        }
    }
    unlockCriticalSection();
   
    if(found>=0)
    {
        //  printf("Deleting thread slot %i id %d", found,(int)d->threadId);
    }
    else
    {
        printf("Thread %d not found !!\n",(int)d->threadId);
    }

}
// Register a per-thread alternate signal stack (Linux). The crash-protection SIGSEGV
// handler is installed with SA_ONSTACK, but SA_ONSTACK only takes effect if THIS thread
// has an alt stack registered (sigaltstack is per-thread). Without it, a STACK-OVERFLOW
// SIGSEGV re-faults immediately on the already-exhausted stack and the process dies
// SILENTLY -- no handler, no backtrace, no StackOverflowError. That is exactly why the
// deep-recursion overflow produced only an opaque core. With an alt stack the handler
// runs and dumps the faulting thread's backtrace (the recursion). Runtime-only, so it
// does not perturb codegen.
void cn1InstallThreadAltStack(void) {
#if defined(__linux__)
    static const size_t CN1_ALTSTACK_SZ = 512 * 1024;
    stack_t ss;
    ss.ss_sp = malloc(CN1_ALTSTACK_SZ);
    if (ss.ss_sp == NULL) return;
    ss.ss_size = CN1_ALTSTACK_SZ;
    ss.ss_flags = 0;
    sigaltstack(&ss, NULL); // intentionally leaked: lives for the thread's lifetime
#endif
}

void* threadRunner(void *x)
{
    cn1InstallThreadAltStack();
    JAVA_OBJECT t = (JAVA_OBJECT)x;
    struct ThreadLocalData* d = getThreadLocalData();
    d->lightweightThread = JAVA_TRUE;
    d->threadActive = JAVA_TRUE;
    d->currentThreadObject = t;
    
   // printf("launching thread %d",(int)d->threadId);
    // Pass the struct pointer as the thread id. Cast through intptr_t, not long:
    // on Windows (LLP64) `long` is 32-bit while pointers are 64-bit, so `(long)d`
    // truncates + sign-extends the pointer, and freeing it later in
    // releaseThreadNativeResources corrupts memory / crashes.
    java_lang_Thread_runImpl___long(d, t, (JAVA_LONG)(intptr_t)d); // pass the actual structure as threadid
   // printf("terminate thread %d",(int)d->threadId);
    
    // we remove the thread here since this is the only place we can do this
    // we add the thread in the getThreadLocalData() method to handle native threads
    // too. Hopefully we won't spawn too many of those...
    
    markDeadThread(d);
   
    /*free(d->blocks);
    free(d->threadObjectStack);
    free(d->callStackClass);
    free(d->callStackLine);
    free(d->callStackMethod);
    free(d);*/
    
    return NULL;
}

JAVA_VOID java_lang_Thread_start__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT th) {
    // Mark the thread alive HERE, on the calling thread, before the worker is
    // spawned. Previously `alive` was set inside runImpl, which runs on the new
    // worker thread asynchronously after start() returns. That left a window in
    // which a caller that did start() then join() could observe isAlive()==false
    // (worker not yet scheduled) and have join() return immediately -- before any
    // of the worker's writes were published -- producing nondeterministic results
    // (the MtStress "DONE 0"). Setting it synchronously here, in program order
    // before start() returns, guarantees a subsequent join()/isAlive() on the
    // same thread sees the thread as alive until the worker clears the flag under
    // the monitor (runImpl: synchronized{ alive=false; notifyAll(); }), which also
    // establishes the happens-before that publishes the worker's results.
    set_field_java_lang_Thread_alive(JAVA_TRUE, th);
    pthread_t pt;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
	// create the thread detached, as we never join.  This
	// fixes the "error 35" problem that occurred after a 
	// finite number of threads. [ddyer 5/2017]
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
#if defined(__linux__) && !defined(__ANDROID__)
    // musl's default thread stack is only 128KB (glibc defaults to 8MB). ParparVM's
    // recursive C interpreter painting a deep component tree (e.g. a Replace/Fade
    // transition) easily overflows 128KB, corrupting the thread stack and crashing
    // at a varying site. Pin a JVM-sized 16MB stack so CN1 threads behave the same
    // as on every other port regardless of the linked libc.
    pthread_attr_setstacksize(&attr, CN1_THREAD_STACK_BYTES);
#endif
    int rc = pthread_create(&pt, &attr, threadRunner, (void *)th);
    if (rc != 0) {
        printf("ERROR creating thread. Return code: %i", rc);
        exit(-1);
    }
    pthread_attr_destroy(&attr);
}

JAVA_LONG java_lang_Thread_getNativeThreadId___R_long(CODENAME_ONE_THREAD_STATE) {
    return currentThreadId();
}

JAVA_VOID java_lang_Thread_interrupt0__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me) {
    lockCriticalSection();
    for(int i=0; i<NUMBER_OF_SUPPORTED_THREADS; i++) {
        struct ThreadLocalData* d = allThreads[i];
        if(d && d->currentThreadObject == me) {
            d->interrupted = JAVA_TRUE;
            break;
        }
    }
    unlockCriticalSection();
}

JAVA_BOOLEAN java_lang_Thread_isInterrupted___boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, JAVA_BOOLEAN clear) {
    JAVA_BOOLEAN ret = JAVA_FALSE;
    // optimization: checking current thread
    if(threadStateData->currentThreadObject == me) {
        ret = threadStateData->interrupted;
        if(clear) threadStateData->interrupted = JAVA_FALSE;
        return ret;
    }

    lockCriticalSection();
    for(int i=0; i<NUMBER_OF_SUPPORTED_THREADS; i++) {
        struct ThreadLocalData* d = allThreads[i];
        if(d && d->currentThreadObject == me) {
            ret = d->interrupted;
            if(clear) d->interrupted = JAVA_FALSE;
            break;
        }
    }
    unlockCriticalSection();
    return ret;
}

JAVA_DOUBLE java_lang_StringToReal_parseDblImpl___java_lang_String_int_R_double(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s, JAVA_INT e) {
    int length = java_lang_String_length___R_int(threadStateData, s);
    JAVA_ARRAY arrayData = (JAVA_ARRAY)java_lang_String_toCharNoCopy___R_char_1ARRAY(threadStateData, s);
    JAVA_ARRAY_CHAR* chrs = CN1_ARRAY_DATA(arrayData);
    char data[length + 1];
    for(int iter = 0 ; iter < length ; iter++) {
        data[iter] = (char)chrs[iter];
    }
    data[length] = 0;
    char *err;
    JAVA_DOUBLE db = strtod(data, &err);
    if (data == err) {
        JAVA_OBJECT numberFormatException = java_lang_StringToReal_invalidReal___java_lang_String_boolean_R_java_lang_NumberFormatException(threadStateData, s, JAVA_TRUE);
        throwException(threadStateData,numberFormatException);
    }
    JAVA_LONG exp = 1;
    if(e != 0) {
        if(e < 0) {
            while (e < -18) {
                // Long accumulator will overflow past 18 digits so we do
                // floating point math until we get there.
                // fixes https://github.com/codenameone/CodenameOne/issues/3250
                e++;
                db /= 10;
            }
            while(e < 0) {
                e++;
                exp *= 10;
            }
            db /= exp;
        } else {
            while (e > 18) {
                // Long accumulator will overflow past 18 digits so we do
                // floating point math until we get there.
                // fixes https://github.com/codenameone/CodenameOne/issues/3250
                e--;
                db /= 10;
            }
            while(e > 0) {
                e--;
                exp *= 10;
            }
            db *= exp;
        }
    }
    return db;
}

void initMethodStack(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, int stackSize, int localsStackSize, int classNameId, int methodNameId) {
#ifdef CN1_INCLUDE_NPE_CHECKS
    if(__cn1ThisObject == JAVA_NULL) {
        THROW_NULL_POINTER_EXCEPTION();
    }
#endif
    if (threadStateData->callStackOffset >= CN1_STACK_OVERFLOW_CALL_DEPTH_LIMIT - 1) {
        cn1ThrowStackOverflow(threadStateData); // preallocated: no alloc/trace at exhaustion
        return;
    }
    memset(&threadStateData->threadObjectStack[threadStateData->threadObjectStackOffset], 0, sizeof(struct elementStruct) * (localsStackSize + stackSize));
    threadStateData->threadObjectStackOffset += localsStackSize + stackSize;
    threadStateData->callStackClass[threadStateData->callStackOffset] = classNameId;
    threadStateData->callStackMethod[threadStateData->callStackOffset] = methodNameId;
    CN1_DEBUG_FRAME_ENTER(threadStateData)
    threadStateData->callStackOffset++;
}


void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset) {
    threadStateData->tryBlockOffset = methodBlockOffset;
    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread;
    threadStateData->callStackOffset--;
}

// Bytes this process is metered at. Both Apple branches report phys_footprint
// rather than resident_size, because that is the figure the kernel actually
// charges an app against (it is what jetsam kills on, and what Xcode's memory
// gauge shows). The distinction became load-bearing with the issue-5537 page
// release: memory handed back with MADV_FREE_REUSABLE leaves phys_footprint
// immediately but stays in resident_size until the system is under pressure, so
// a resident_size reading reports a process that has genuinely released memory
// as though it had not -- and an app calling Runtime.freeMemory() to decide
// whether it can afford a cache would never see the memory it just got back.
#if defined(__APPLE__)
static uint64_t cn1PhysFootprint(void) {
    task_vm_info_data_t info;
    mach_msg_type_number_t count = TASK_VM_INFO_COUNT;
    if(task_info(mach_task_self(), TASK_VM_INFO, (task_info_t)&info, &count) == KERN_SUCCESS) {
        return (uint64_t)info.phys_footprint;
    }
    return 0;
}
#endif

// Resident set of this process in bytes, read from /proc/self/statm (field 2 is
// resident pages). Linux has no phys_footprint; RSS is the right metric there
// because MADV_DONTNEED drops the pages immediately rather than deferring to
// memory pressure, so a release shows up in RSS the moment it happens.
#if defined(__linux__)
static uint64_t cn1LinuxResidentBytes(void) {
    // /proc/self/statm field 2 is resident pages. Parsed with fgets + strtoul rather
    // than the scanf family: glibc 2.38 redirects fscanf to __isoc23_fscanf in
    // <stdio.h>, and the cross-linked Linux target resolves against a sysroot that has
    // no such symbol, so any retained scanf call fails the link outright
    // (ld.lld: undefined symbol: __isoc23_fscanf).
    FILE* f = fopen("/proc/self/statm", "r");
    if(f == 0) {
        return 0;
    }
    char buf[128];
    char* line = fgets(buf, sizeof(buf), f);
    fclose(f);
    if(line == 0) {
        return 0;
    }
    char* end = 0;
    strtoul(line, &end, 10);            // field 1: total program size, unused
    if(end == line) {
        return 0;
    }
    char* residentStart = end;
    unsigned long resident = strtoul(residentStart, &end, 10);
    if(end == residentStart) {
        return 0;
    }
    long ps = sysconf(_SC_PAGESIZE);
    if(ps <= 0) {
        return 0;
    }
    return (uint64_t)resident * (uint64_t)ps;
}
#endif

JAVA_LONG java_lang_Runtime_totalMemoryImpl___R_long(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) && defined(__OBJC__)
    return [NSProcessInfo processInfo].physicalMemory;
#elif defined(__linux__)
    {
        long pages = sysconf(_SC_PHYS_PAGES);
        long ps = sysconf(_SC_PAGESIZE);
        if(pages > 0 && ps > 0) {
            return (JAVA_LONG)((uint64_t)pages * (uint64_t)ps);
        }
    }
    return 1024*1024*1024;
#elif defined(__APPLE__)
    // Plain-C Apple target (the translator's clean target emits .c, so the
    // __OBJC__ branch above is unavailable there). sysctl reports the same
    // figure NSProcessInfo.physicalMemory does.
    {
        uint64_t total = 0;
        size_t len = sizeof(total);
        if(sysctlbyname("hw.memsize", &total, &len, NULL, 0) == 0 && total > 0) {
            return (JAVA_LONG)total;
        }
    }
    return 1024*1024*1024;
#else
    // TODO: implement for other platforms
    return 1024*1024*1024;
#endif
}

JAVA_LONG java_lang_Runtime_freeMemoryImpl___R_long(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__)
    {
        JAVA_LONG total = java_lang_Runtime_totalMemoryImpl___R_long(threadStateData);
        uint64_t used = cn1PhysFootprint();
        return used == 0 ? total : total - (JAVA_LONG)used;
    }
#elif defined(__linux__)
    {
        JAVA_LONG total = java_lang_Runtime_totalMemoryImpl___R_long(threadStateData);
        uint64_t used = cn1LinuxResidentBytes();
        return used == 0 ? total : total - (JAVA_LONG)used;
    }
#else
    // TODO: implement for other platforms
    return 1024*1024*1024;
#endif
}


// Closed-world native HashMap.get: one C call instead of get -> getEntry ->
// computeHashCode(virtual hashCode) -> findNonNullKeyEntry. The chain walk and the
// tagged-int hashCode (an inline untag in virtual_..._hashCode) are already cheap; this
// removes the translated-Java wrapper frames. Bit-identical to the Java getEntry path.
// ============================================================================
// COMPACT HashMap natives (open addressing over parallel arrays -- see the
// layout notes in java/util/HashMap.java). These are the C twins of the
// pure-Java *Impl methods; any semantic change must be applied to BOTH (the
// JavaScript port runs the Impls). ParparVM natives read the Java arrays as
// raw C memory, so a probe is a linear scan of an int array -- no JNI cost,
// no entry objects, no pointer chasing.
// ============================================================================

JAVA_BOOLEAN java_util_HashMap_areEqualKeys___java_lang_Object_java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    if(__cn1Arg1 == __cn1Arg2) {
        return JAVA_TRUE;
    }
    return virtual_java_lang_Object_equals___java_lang_Object_R_boolean(threadStateData, __cn1Arg1, __cn1Arg2);
}

// the occupied-slot marker: mixed hash with the sign bit forced on (must match
// HashMap.cn1Marker exactly). The spread is deliberately weak so Integer keys
// stay at slot == value and a dense range keeps its sequential-store locality;
// what makes that safe is the probe sequence below, not the spread.
#if CN1_TAGGED_ACTIVE
/* The five non-Integer tagged types. Out of line deliberately: keeping them here
 * is what lets cn1HmMarker stay small enough to inline into get/put. Each arm is
 * that type's hashCode as vm/JavaAPI defines it, checked against those sources --
 * Short/Character return the value, Float is floatToIntBits, Long and Double fold
 * the high half into the low. Boolean and Byte are NOT tagged (no tag code), and
 * an unrecognised tag falls back to the virtual dispatch, which stays the source
 * of truth. Double untags by MASKING the tag bits, lossless because the scheme
 * only tags doubles whose low three bits are already zero. */
static JAVA_INT cn1TaggedHashSlow(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT key, uintptr_t tag) {
    switch(tag) {
        case CN1_TAG_SHORT:     return (JAVA_INT)CN1_UNTAG_SHORT(key);
        case CN1_TAG_CHARACTER: return (JAVA_INT)CN1_UNTAG_CHAR(key);
        case CN1_TAG_FLOAT:     return (JAVA_INT)CN1_UNTAG_FLOAT_BITS(key);
        case CN1_TAG_LONG: {
            JAVA_LONG v = CN1_UNTAG_LONG(key);
            return (JAVA_INT)(v ^ (((uint64_t)v) >> 32));
        }
        case CN1_TAG_DOUBLE: {
            uint64_t b = CN1_UNTAG_DOUBLE_BITS(key);
            return (JAVA_INT)(b ^ (b >> 32));
        }
        default: return virtual_java_lang_Object_hashCode___R_int(threadStateData, key);
    }
}
#endif

#ifdef CN1_HM_PROBE_CENSUS
long cn1HmProbeCalls = 0, cn1HmProbeSteps = 0;
static void cn1HmProbeReport(void) {
    if(cn1HmProbeCalls == 0) return;
    fprintf(stderr, "[HMPROBE] lookups=%ld extraSteps=%ld stepsPerLookup=%.3f\n",
            cn1HmProbeCalls, cn1HmProbeSteps,
            (double)cn1HmProbeSteps / (double)cn1HmProbeCalls);
    fflush(stderr);
}
__attribute__((constructor)) static void cn1HmProbeInit(void){ atexit(cn1HmProbeReport); }
#endif

static inline JAVA_INT cn1HmMarker(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT key) {
    if(key == JAVA_NULL) {
        return (JAVA_INT)0x80000000;
    }
    JAVA_INT h;
#if CN1_TAGGED_ACTIVE
    /* A TAGGED KEY CARRIES ITS OWN HASH IN ITS POINTER BITS.
     *
     * Integer keys are the common case for a Map and every Integer in this VM is
     * an immediate, yet the path below reached its hash through TWO tag resolves
     * (CN1_CLASS_OF for the String test, then another inside the thunk) and a
     * virtual dispatch -- to compute a value that is literally (intptr_t)key >> 3.
     * That made a value type cost MORE than HotSpot's heap Integer, whose hash is
     * one field load, and it ran on every get and every put.
     *
     * Each arm below is that type's java.lang hashCode contract, not a shortcut:
     * Integer/Short/Character/Byte return the value, Boolean is 1231/1237, Float
     * is floatToIntBits, Long and Double fold the high half into the low.
     */
    /* ONLY THE INTEGER CASE IS INLINE. The first cut put all six tagged types in
     * a switch here, and cn1HmMarker -- which was `static inline` and folded into
     * put -- became too big to inline: the profile grew a 28.3% cn1HmMarker frame
     * that had not existed, and the benchmark did not move. Integer is what a Map
     * is keyed on; the other five go out of line where their size costs nothing. */
    uintptr_t cn1__tag = CN1_TAG_CODE(key);
    if(__builtin_expect(cn1__tag == CN1_TAG_INTEGER, 0)) {
        h = CN1_UNTAG_INT(key);
    } else if(__builtin_expect(cn1__tag != 0, 0)) {
        h = cn1TaggedHashSlow(threadStateData, key, cn1__tag);
    } else
#endif
    if(cn1IsStringClass(CN1_CLASS_OF(key))) {
        h = ((struct obj__java_lang_String*)key)->java_lang_String_hashCode;
        if(h == 0) h = java_lang_String_hashCode___R_int(threadStateData, key);
    } else h = virtual_java_lang_Object_hashCode___R_int(threadStateData, key);
    h ^= (JAVA_INT)(((uint32_t)h) >> 16);
    return h | (JAVA_INT)0x80000000;
}

// The next slot on a probe path -- CPython's dict recurrence, and the C twin of
// HashMap.cn1NextSlot. NOT i + 1: linear probing walks a run of occupied slots
// end to end, and the weak spread above puts every dense Integer key range in
// exactly one such run, so a MISS inside it was O(n) -- 2547 probes at 20k
// keys, 222721 at 1M. The first probe is still marker & mask, so sequential
// placement and its locality are untouched; every probe after it jumps
// pseudo-randomly and leaves the run at once. perturb decays to 0 within seven
// steps, after which 5 * i + 1 is a full-period permutation of a power-of-two
// table, so the walk always reaches the terminating empty slot.
static inline int cn1HmNextSlot(int i, uint32_t perturb, int mask) {
    return (int)((((uint32_t)i << 2) + (uint32_t)i + 1u + perturb) & (uint32_t)mask);
}

// probe; >=0 found slot, else -(insertionPoint+1) (first tombstone on the path
// if any, else the terminating empty slot). Must match cn1FindSlotImpl.
/* THE BLOCK POINTERS ARE READ RELAXED ON THE MUTATOR SIDE. They are Java `volatile`,
 * which the translator emits as _Atomic, so a plain read was a sequentially consistent
 * load -- an ldar on arm64, two of them on every lookup. They are volatile for the
 * COLLECTOR, whose marker reads them concurrently while a grow swaps them. The mutator
 * reading pointers it wrote itself needs no ordering at all: program order already
 * covers it, and a second mutator thread touching the same HashMap is a data race
 * whatever the load is. The generated writers and the marker's reads are unchanged. */
#define CN1_HM_BLK(t, which) atomic_load_explicit(&(t)->java_util_HashMap_cn1##which##Block, memory_order_relaxed)

static JAVA_INT cn1HmFindSlotSlow(CODENAME_ONE_THREAD_STATE, struct obj__java_util_HashMap* t, JAVA_OBJECT key, JAVA_INT marker)
    __attribute__((cold, noinline));

/* THE FIRST PROBE IS INLINE; EVERYTHING ELSE IS A CALL.
 *
 * This was one function of ~400 instructions, called out of line from get, put,
 * containsKey and remove, and its prologue saved TWELVE callee-saved registers and
 * materialised a handful of addresses on every call -- register pressure from the cold
 * paths it carries (String equality, the user equals(), the CME throw), paid by the hot
 * path, which is a few instructions: probe discipline is already perfect here, the
 * CN1_HM_PROBE_CENSUS count is 0.000 extra steps per lookup on hashMapChurn. Same shape
 * as the static initializer that was inlined into CommonWorkloads.fib.
 *
 * The inline part decides exactly the two first-probe outcomes that need no equality
 * call, and decides them as the full probe would: an EMPTY slot answers -(i+1) (no
 * tombstone can have been seen before the first slot), and an IDENTITY hit answers i
 * (no user code ran, so the CME check cannot fire). Anything else restarts the full
 * probe from the beginning, which only re-reads what this read. */
static inline JAVA_INT cn1HmFindSlot(CODENAME_ONE_THREAD_STATE, struct obj__java_util_HashMap* t, JAVA_OBJECT key, JAVA_INT marker) {
    JAVA_INT* meta = (JAVA_INT*)(uintptr_t)CN1_HM_BLK(t, Meta);
    int i = marker & (t->java_util_HashMap_cn1Cap - 1);
    if(__builtin_expect(meta == NULL, 0)) {
        return -(i + 1);
    }
#ifdef CN1_HM_PROBE_CENSUS
    cn1HmProbeCalls++;
#endif
    JAVA_INT m = meta[i];
    if(m == 0) {
        return -(i + 1);
    }
    if(m == marker && ((JAVA_OBJECT*)(uintptr_t)CN1_HM_BLK(t, Keys))[i] == key) {
        return i;
    }
#ifdef CN1_HM_PROBE_CENSUS
    cn1HmProbeCalls--;   // the full probe counts this lookup itself
#endif
    return cn1HmFindSlotSlow(threadStateData, t, key, marker);
}

static JAVA_INT cn1HmFindSlotSlow(CODENAME_ONE_THREAD_STATE, struct obj__java_util_HashMap* t, JAVA_OBJECT key, JAVA_INT marker) {
    JAVA_INT* meta = (JAVA_INT*)(uintptr_t)CN1_HM_BLK(t, Meta);
    JAVA_OBJECT* keys = (JAVA_OBJECT*)(uintptr_t)CN1_HM_BLK(t, Keys);
    int mask = t->java_util_HashMap_cn1Cap - 1;
    int i = marker & mask;
    if(meta == NULL) return -(i + 1);
    uint32_t perturb = (uint32_t)marker;
#ifdef CN1_HM_PROBE_CENSUS
    /* How many slots does one lookup actually touch? A COUNT, so a loaded machine
     * cannot corrupt it -- unlike every timing taken on this host tonight. Near
     * 1.0 means the spreading is healthy and the remaining cost is cache
     * behaviour across the three separate blocks; materially above it means the
     * marker clusters and the probe recurrence itself is the cost. Those want
     * opposite fixes, which is why this is measured rather than guessed. */
    cn1HmProbeCalls++;
#endif
    int firstTomb = -1;
    JAVA_INT expected = t->java_util_HashMap_modCount;
    /* SHORT-CIRCUITING THE TAG TEST HERE WAS MEASURED SLOWER AND REVERTED.
     * Skipping cn1IsStringClass for a tagged key removes a tag resolve from every
     * lookup and is obviously correct -- and ab-bench.sh put hashMapChurn at
     * 1.055 (5.5% SLOWER) with every control at 1.000. cn1HmFindSlot is static,
     * and changing its body changed how clang inlines it into get/put; the
     * removed work cost less than the inlining it disturbed. Re-measure with
     * ab-bench.sh before trying it again. */
    /* THE IDENTITY HIT RETURNS AT ONCE. When keys[i] == key -- which is every hit for a
     * tagged key, since equal tagged values are the same word, and most hits for any
     * interned or reused key -- no user code has run, so the CME re-check below cannot
     * fire, and the String test is not needed at all. Both used to run on EVERY hit:
     * stringKey was computed up front (for a tagged key a tag resolve plus three
     * class-pointer compares) and modCount and the meta-block pointer were reloaded.
     * Now stringKey is computed only when identity fails, once per call.
     *
     * Semantics are unchanged: with key == JAVA_NULL, identity IS the old
     * `k == JAVA_NULL` test, and a non-identity null key still never matches. */
    int stringKey = -1;
    while(1) {
        JAVA_INT m = meta[i];
        if(m == 0) { // META_EMPTY
            return -((firstTomb >= 0 ? firstTomb : i) + 1);
        }
        if(m == marker) {
            JAVA_OBJECT k = keys[i];
            if(k == key) {
                return i;
            }
            if(key != JAVA_NULL) {
                if(stringKey < 0) {
                    stringKey = cn1IsStringClass(CN1_CLASS_OF(key)) ? 1 : 0;
                }
                JAVA_BOOLEAN matches = stringKey ? cn1StringEquals(threadStateData, key, k)
                        : java_util_HashMap_areEqualKeys___java_lang_Object_java_lang_Object_R_boolean(threadStateData, key, k);
                // No cached pointer may be read after a structurally mutating callback.
                if(!stringKey && (expected != t->java_util_HashMap_modCount ||
                        meta != (JAVA_INT*)(uintptr_t)CN1_HM_BLK(t, Meta))) CN1_THROW_CME();
                if(matches) {
                    return i;
                }
            }
        } else if(m == 1 && firstTomb < 0) { // META_TOMB
            firstTomb = i;
        }
        perturb >>= 5;
        i = cn1HmNextSlot(i, perturb, mask);
#ifdef CN1_HM_PROBE_CENSUS
        cn1HmProbeSteps++;
#endif
    }
}

#include "cn1_collections.h"
#ifdef CN1_COLL_SET
// ============================================================================
// HashSet, ENTIRELY IN C.
//
// A set used to be a HashMap with the set itself stored as every value: each one
// allocated a map object AND that map's table allocated a full VALUES reference
// array holding one repeated pointer. Measured on the HelloCodenameOne corpus,
// 155,787 live HashSets against 366,554 live HashMaps -- 42% of every map in the
// heap existed only to back a set -- costing ~96 bytes of map plus a ~144-byte
// values stride each, and handing the collector 2.5M reference slots to mark that
// could only ever point at the set that already owned them.
//
// WHY THE WHOLE THING IS HERE AND NOT IN JAVA. A first attempt kept the probe in
// Java over NativeStorage.get/set. Every one of those is a separate native call,
// so a collection can land BETWEEN any two of them -- and during a rebuild that
// meant the marker traced a half-filled table while elements still living only in
// the old one were reachable from nothing. They were freed while still in the set
// and the damage surfaced far away, as a null dereference inside
// ByteCodeTranslator.copy. In C the whole operation is one call with no safepoint
// in it: the rehash loop below does no allocation and makes no Java call, so there
// is no instant at which a live element is untraced. That is the same reason
// HashMap rebuilds through the single NativeStorage.rehash native.
//
// The probe is cn1HmFindSlot's, deliberately: same marker, same recurrence, same
// String fast path. A second copy of a probe sequence is a second thing to get
// subtly wrong, so the only difference here is what is ABSENT -- there is no
// values array to allocate, store into, rehash or mark.
// ============================================================================

#define CN1_HS(o) ((struct obj__java_util_HashSet*)(o))

static JAVA_INT cn1HsFindSlot(CODENAME_ONE_THREAD_STATE, struct obj__java_util_HashSet* s,
        JAVA_OBJECT key, JAVA_INT marker) {
    JAVA_INT* meta = (JAVA_INT*)(uintptr_t)s->java_util_HashSet_cn1MetaBlock;
    JAVA_OBJECT* keys = (JAVA_OBJECT*)(uintptr_t)s->java_util_HashSet_cn1KeysBlock;
    int mask = s->java_util_HashSet_cn1Cap - 1;
    int i = marker & mask;
    if(meta == NULL) return -(i + 1);
    uint32_t perturb = (uint32_t)marker;
    int firstTomb = -1;
    JAVA_INT expected = s->java_util_HashSet_cn1ModCount;
    int stringKey = key != JAVA_NULL && cn1IsStringClass(CN1_CLASS_OF(key));
    while(1) {
        JAVA_INT m = meta[i];
        if(m == 0) { // META_EMPTY
            return -((firstTomb >= 0 ? firstTomb : i) + 1);
        }
        if(m == marker) {
            JAVA_OBJECT k = keys[i];
            JAVA_BOOLEAN matches = key == JAVA_NULL ? k == JAVA_NULL
                    : (k == key || (stringKey ? cn1StringEquals(threadStateData, key, k)
                        : java_util_HashMap_areEqualKeys___java_lang_Object_java_lang_Object_R_boolean(threadStateData, key, k)));
            // A user equals() can re-enter and rearrange this table; no pointer cached
            // before the callback may be used after it. A String equals cannot re-enter.
            if(!stringKey && (expected != s->java_util_HashSet_cn1ModCount ||
                    meta != (JAVA_INT*)(uintptr_t)s->java_util_HashSet_cn1MetaBlock)) CN1_THROW_CME();
            if(matches) return i;
            meta = (JAVA_INT*)(uintptr_t)s->java_util_HashSet_cn1MetaBlock;
            keys = (JAVA_OBJECT*)(uintptr_t)s->java_util_HashSet_cn1KeysBlock;
        } else if(m == 1 && firstTomb < 0) { // META_TOMB
            firstTomb = i;
        }
        perturb >>= 5;
        i = cn1HmNextSlot(i, perturb, mask);
    }
}

/* Derived, not stored. It was always (int)(cap * 0.75f), and cap is a power of two
 * of at least 16, for which cap - (cap >> 2) is that value exactly. The field cost 4
 * bytes on every HashSet, enough to push the object out of the 48-byte BiBOP slot
 * class into the 64-byte one.
 *
 * The old clamp ("always keep one empty slot: a table with none makes the probe
 * non-terminating") is not lost -- it is unreachable. cap >= 16 gives a threshold of
 * at most cap - 4, so four slots always stay free. */
static inline JAVA_INT cn1HsThreshold(const struct obj__java_util_HashSet* s) {
    JAVA_INT cap = s->java_util_HashSet_cn1Cap;
    return cap - (cap >> 2);
}

/** Rebuild. Doubles when the LIVE count reached the threshold, rebuilds at the same
 *  size when TOMBSTONES did, which purges them instead of growing a table of holes.
 *  One call, no safepoint: see the header note. */
static void cn1HsGrow(CODENAME_ONE_THREAD_STATE, struct obj__java_util_HashSet* s) {
    JAVA_INT oldCap = s->java_util_HashSet_cn1Cap;
    JAVA_INT cap = oldCap;
    if(s->java_util_HashSet_cn1Size >= cn1HsThreshold(s)) {
        cap <<= 1;
        if(cap <= 0) { CN1_THROW_OOM(); return; }
    }
    JAVA_LONG oldKeys = s->java_util_HashSet_cn1KeysBlock;
    JAVA_LONG oldMeta = s->java_util_HashSet_cn1MetaBlock;
    // These can collect. The OLD table is still published throughout, so every live
    // element remains traced; the fresh blocks are empty and hold nothing to lose.
    JAVA_LONG keys = cn1RefBlockAlloc(cap);
    JAVA_LONG meta = cn1IntBlockAlloc(cap);
    if(keys == 0 || meta == 0) {
        cn1RefBlockFree(keys); cn1RefBlockFree(meta);
        CN1_THROW_OOM();
        return;
    }
    JAVA_INT* src = (JAVA_INT*)(uintptr_t)oldMeta;
    JAVA_INT* dst = (JAVA_INT*)(uintptr_t)meta;
    JAVA_OBJECT* srcKeys = (JAVA_OBJECT*)(uintptr_t)oldKeys;
    int mask = cap - 1;
    JAVA_INT occupied = 0;
    // No allocation and no Java call in this loop, so no collection can observe the
    // half-built table.
    for(JAVA_INT i = 0 ; i < oldCap ; i++) {
        JAVA_INT marker = src[i];
        if(marker >= 0) continue;            // META_EMPTY (0) or META_TOMB (1)
        int slot = marker & mask;
        uint32_t perturb = (uint32_t)marker;
        while(dst[slot] != 0) {
            perturb >>= 5;
            slot = cn1HmNextSlot(slot, perturb, mask);
        }
        dst[slot] = marker;
        cn1RefBlockSet(threadStateData, keys, slot, srcKeys[i]);
        occupied++;
    }
    s->java_util_HashSet_cn1Cap = cap;
    s->java_util_HashSet_cn1KeysBlock = keys;
    s->java_util_HashSet_cn1MetaBlock = meta;
    s->java_util_HashSet_cn1Occupied = occupied;
    cn1RefBlockRetire(oldKeys);
    cn1RefBlockRetire(oldMeta);
}

JAVA_BOOLEAN java_util_HashSet_cn1AddNative___java_lang_Object_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {
    CN1_KEEP_NATIVE_OWNER(setOwner, __cn1ThisObject);
    CN1_KEEP_NATIVE_OWNER(elementOwner, __cn1Arg1);
    struct obj__java_util_HashSet* s = CN1_HS(__cn1ThisObject);
    JAVA_INT marker = cn1HmMarker(threadStateData, __cn1Arg1);
    if(s->java_util_HashSet_cn1MetaBlock == 0) {
        JAVA_LONG keys = cn1RefBlockAlloc(s->java_util_HashSet_cn1Cap);
        JAVA_LONG meta = cn1IntBlockAlloc(s->java_util_HashSet_cn1Cap);
        if(keys == 0 || meta == 0) {
            cn1RefBlockFree(keys); cn1RefBlockFree(meta);
            CN1_THROW_OOM();
            return JAVA_FALSE;
        }
        s->java_util_HashSet_cn1KeysBlock = keys;
        s->java_util_HashSet_cn1MetaBlock = meta;
        s->java_util_HashSet_cn1Size = 0;
        s->java_util_HashSet_cn1Occupied = 0;
        }
    JAVA_INT idx = cn1HsFindSlot(threadStateData, s, __cn1Arg1, marker);
    if(idx >= 0) return JAVA_FALSE;
    JAVA_INT ins = -idx - 1;
    JAVA_INT* meta = (JAVA_INT*)(uintptr_t)s->java_util_HashSet_cn1MetaBlock;
    int wasEmpty = meta[ins] == 0;
    meta[ins] = marker;
    cn1RefBlockSet(threadStateData, s->java_util_HashSet_cn1KeysBlock, ins, __cn1Arg1);
    s->java_util_HashSet_cn1Size++;
    if(wasEmpty) s->java_util_HashSet_cn1Occupied++;
    s->java_util_HashSet_cn1ModCount++;
    if(s->java_util_HashSet_cn1Occupied >= cn1HsThreshold(s)) {
        cn1HsGrow(threadStateData, s);
    }
    return JAVA_TRUE;
}

JAVA_BOOLEAN java_util_HashSet_cn1ContainsNative___java_lang_Object_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {
    CN1_KEEP_NATIVE_OWNER(setOwner, __cn1ThisObject);
    CN1_KEEP_NATIVE_OWNER(elementOwner, __cn1Arg1);
    struct obj__java_util_HashSet* s = CN1_HS(__cn1ThisObject);
    if(s->java_util_HashSet_cn1MetaBlock == 0) return JAVA_FALSE;
    return cn1HsFindSlot(threadStateData, s, __cn1Arg1,
            cn1HmMarker(threadStateData, __cn1Arg1)) >= 0 ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_BOOLEAN java_util_HashSet_cn1RemoveNative___java_lang_Object_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {
    CN1_KEEP_NATIVE_OWNER(setOwner, __cn1ThisObject);
    CN1_KEEP_NATIVE_OWNER(elementOwner, __cn1Arg1);
    struct obj__java_util_HashSet* s = CN1_HS(__cn1ThisObject);
    if(s->java_util_HashSet_cn1MetaBlock == 0) return JAVA_FALSE;
    JAVA_INT idx = cn1HsFindSlot(threadStateData, s, __cn1Arg1,
            cn1HmMarker(threadStateData, __cn1Arg1));
    if(idx < 0) return JAVA_FALSE;
    // A TOMBSTONE, not an empty slot: emptying it would terminate the probe path early
    // and hide every element that collided past this point.
    ((JAVA_INT*)(uintptr_t)s->java_util_HashSet_cn1MetaBlock)[idx] = 1;
    cn1RefBlockSet(threadStateData, s->java_util_HashSet_cn1KeysBlock, idx, JAVA_NULL);
    s->java_util_HashSet_cn1Size--;
    s->java_util_HashSet_cn1ModCount++;
    return JAVA_TRUE;
}

JAVA_VOID java_util_HashSet_cn1ClearNative__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    CN1_KEEP_NATIVE_OWNER(setOwner, __cn1ThisObject);
    struct obj__java_util_HashSet* s = CN1_HS(__cn1ThisObject);
    if(s->java_util_HashSet_cn1MetaBlock != 0) {
        JAVA_INT cap = s->java_util_HashSet_cn1Cap;
        memset((void*)(uintptr_t)s->java_util_HashSet_cn1MetaBlock, 0, (size_t)cap * sizeof(JAVA_INT));
        for(JAVA_INT i = 0 ; i < cap ; i++) {
            cn1RefBlockSet(threadStateData, s->java_util_HashSet_cn1KeysBlock, i, JAVA_NULL);
        }
    }
    s->java_util_HashSet_cn1Size = 0;
    s->java_util_HashSet_cn1Occupied = 0;
    s->java_util_HashSet_cn1ModCount++;
}

/** Next occupied slot at or after `from`, or -1. The iterator's whole scan. */
JAVA_INT java_util_HashSet_cn1NextOccupied___int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT __cn1Arg1) {
    struct obj__java_util_HashSet* s = CN1_HS(__cn1ThisObject);
    JAVA_LONG metaBlock = s->java_util_HashSet_cn1MetaBlock;
    if(metaBlock == 0) return -1;
    JAVA_INT* meta = (JAVA_INT*)(uintptr_t)metaBlock;
    JAVA_INT cap = s->java_util_HashSet_cn1Cap;
    for(JAVA_INT i = __cn1Arg1 ; i < cap ; i++) {
        if(meta[i] < 0) return i;          // occupied: the marker has its sign bit set
    }
    return -1;
}

JAVA_OBJECT java_util_HashSet_cn1ElementAt___int_R_java_lang_Object(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT __cn1Arg1) {
    struct obj__java_util_HashSet* s = CN1_HS(__cn1ThisObject);
    return ((JAVA_OBJECT*)(uintptr_t)s->java_util_HashSet_cn1KeysBlock)[__cn1Arg1];
}

/** Iterator.remove: tombstone the slot the iterator last returned. */
JAVA_VOID java_util_HashSet_cn1RemoveSlot___int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT __cn1Arg1) {
    CN1_KEEP_NATIVE_OWNER(setOwner, __cn1ThisObject);
    struct obj__java_util_HashSet* s = CN1_HS(__cn1ThisObject);
    ((JAVA_INT*)(uintptr_t)s->java_util_HashSet_cn1MetaBlock)[__cn1Arg1] = 1;
    cn1RefBlockSet(threadStateData, s->java_util_HashSet_cn1KeysBlock, __cn1Arg1, JAVA_NULL);
    s->java_util_HashSet_cn1Size--;
    s->java_util_HashSet_cn1ModCount++;
}
#undef CN1_HS
#endif /* CN1_COLL_SET */

#ifdef CN1_COLL_ARRAYLIST
// The complete bulk operation owns one native destination block. Layout selection
// is outside the loops; their bodies contain only loads, stores and increments.
JAVA_INT java_util_ArrayList_addAllNative___int_java_util_Collection_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner, JAVA_INT index, JAVA_OBJECT collection) {
    CN1_KEEP_NATIVE_OWNER(destinationOwner, owner);
    CN1_KEEP_NATIVE_OWNER(sourceOwner, collection);
    CN1CollectionView source;
    if(!cn1CollectionOpen(threadStateData, collection, &source)) return -1;
    struct obj__java_util_ArrayList* list = (struct obj__java_util_ArrayList*)owner;
    int n = source.count, size = list->java_util_ArrayList_size;
    if(n == 0) return 0;
    if(n < 0 || n > INT_MAX - size) return -2;
    JAVA_LONG old = list->java_util_ArrayList_cn1Storage;
    // From the block header rather than a field -- ArrayList has no capacity field;
    // see the comment on its declaration order. cn1RefBlockCount(0) is 0, which is
    // the right answer for the not-yet-allocated state the `old == 0` test below
    // already handles.
    int required = size + n, capacity = cn1RefBlockCount(old);
    JAVA_LONG block = old;
    if(required > capacity || old == 0) {
        if(required > capacity) {
            int64_t grown = (int64_t)capacity + (capacity >> 1) + 1;
            capacity = grown < required || grown > INT_MAX ? required : (int)grown;
        }
        block = cn1RefBlockAlloc(capacity);
        if(block == 0) return -2;
    }
    JAVA_OBJECT* data = (JAVA_OBJECT*)(uintptr_t)block;
    JAVA_OBJECT* previous = (JAVA_OBJECT*)(uintptr_t)old;
    JAVA_BOOLEAN marking = cn1SatbBulkBegin();
    // During a mark the destination block is queued for a collector re-scan instead of
    // logging every inserted and shifted reference here -- see DEFERRED BLOCK RE-SCAN in
    // cn1_globals.m. Nothing is lost that a re-scan could miss: the slots an insertion
    // overwrites lie past the old size, and hold no live reference. Registered for the
    // whole of the write, which is what the collector waits on before it scans.
    int deferred = marking ? cn1BlockMoveBegin(block) : 0;
    if(block != old) {
        if(index > 0) memcpy(data, previous, (size_t)index * sizeof(JAVA_OBJECT));
        cn1CollectionCopy(&source, data + index);
        if(size > index) memcpy(data + index + n, previous + index, (size_t)(size - index) * sizeof(JAVA_OBJECT));
    } else {
        if(marking && !deferred) cn1SatbEnqueueRangeLocked((JAVA_ARRAY_OBJECT*)(data + index), size - index);
        memmove(data + index + n, data + index, (size_t)(size - index) * sizeof(JAVA_OBJECT));
        if(source.kind == CN1_COLL_DENSE && source.data == data) {
            // Self insertion: the prefix is still in place; the suffix has moved.
            memmove(data + index, data, (size_t)index * sizeof(JAVA_OBJECT));
            memmove(data + 2 * index, data + index + n, (size_t)(size - index) * sizeof(JAVA_OBJECT));
        } else cn1CollectionCopy(&source, data + index);
    }
    if(marking && !deferred) cn1SatbEnqueueRangeLocked((JAVA_ARRAY_OBJECT*)(data + index), n);
    if(block != old) list->java_util_ArrayList_cn1Storage = block;
    // No capacity store: the block it was describing already records it.
    list->java_util_ArrayList_size = required;
    list->java_util_AbstractList_modCount++;
    if(marking) cn1BlockMoveEnd(block);
    cn1SatbBulkEnd();
    if(block != old) cn1RefBlockRetire(old);
    return 1;
}
JAVA_INT java_util_ArrayList_initFromNative___java_util_Collection_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT owner, JAVA_OBJECT collection) {
    // A subclass constructor can observe its overridden addAll being invoked.
    if(owner->__codenameOneParentClsReference != &class__java_util_ArrayList) return -1;
    return java_util_ArrayList_addAllNative___int_java_util_Collection_R_int(threadStateData, owner, 0, collection);
}

#endif

// ===================== SHARED NATIVE COLLECTION STORAGE =====================
// The three parallel tables are C blocks, not Java arrays -- see the field comment in
// HashMap.java. These are the Java-visible handles; the collector reaches the reference
// blocks through the generated __GC_MARK_ (ByteCodeClass.NATIVE_BLOCKS) and frees all
// three from the generated __FINALIZER_.
JAVA_LONG java_util_NativeStorage_allocateTable___int_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT n, JAVA_BOOLEAN ordered) {
    return cn1TableAlloc(n, ordered);
}
JAVA_LONG java_util_NativeStorage_part___long_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG table, JAVA_INT part) {
    return cn1TablePart(table, part);
}

JAVA_LONG java_util_NativeStorage_allocateReferences___int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT n) {
    return cn1RefBlockAlloc(n);
}

JAVA_LONG java_util_NativeStorage_allocateIntegers___int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT n) {
    return cn1IntBlockAlloc(n);
}

JAVA_INT java_util_NativeStorage_capacity___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG block) {
    return cn1RefBlockCount(block);
}

JAVA_VOID java_util_NativeStorage_free___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG b) {
    cn1RefBlockFree(b);
}

// RETIRE, not free -- for a block that is being replaced while the map stays alive.
// A concurrent marker may have loaded the old pointer before the swap and still be
// walking it; the retire list is drained by the sweep, after the mark that could have
// done so has ended. Freeing here directly is a use-after-free, and it is the bug
// MapTorture2 caught on its first run.
JAVA_VOID java_util_NativeStorage_retire___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG b) {
    cn1RefBlockRetire(b);
}

JAVA_OBJECT java_util_NativeStorage_get___long_int_R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_LONG b, JAVA_INT i) {
    return cn1RefBlockGet(b, i);
}

JAVA_VOID java_util_NativeStorage_set___long_int_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_LONG b, JAVA_INT i, JAVA_OBJECT v) {
    cn1RefBlockSet(threadStateData, b, i, v);
}

JAVA_INT java_util_NativeStorage_getInt___long_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG b, JAVA_INT i) {
    return cn1IntBlockGet(b, i);
}

JAVA_VOID java_util_NativeStorage_setInt___long_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG b, JAVA_INT i, JAVA_INT v) {
    cn1IntBlockSet(b, i, v);
}

// The iterator's advance. In C so the scan is one crossing per call rather than one per
// slot -- the Java loop this replaces read a slot per iteration, which would have become
// a native call per iteration on a target that does not link with LTO.
JAVA_INT java_util_NativeStorage_rehash___long_long_long_long_int_long_long_long_long_long_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG keys, JAVA_LONG values, JAVA_LONG metadata,
        JAVA_LONG links, JAVA_INT head, JAVA_LONG newKeys, JAVA_LONG newValues,
        JAVA_LONG newMetadata, JAVA_LONG newPrev, JAVA_LONG newNext) {
    JAVA_INT* source = (JAVA_INT*)(uintptr_t)metadata;
    JAVA_INT* target = (JAVA_INT*)(uintptr_t)newMetadata;
    JAVA_INT capacity = cn1RefBlockCount(metadata), mask = cn1RefBlockCount(newMetadata) - 1;
    JAVA_INT tail = -1;
    for(JAVA_INT i = links ? head : 0; i >= 0 && i < capacity;
            i = links ? cn1IntBlockGet(links, i) : i + 1) {
        JAVA_INT marker = source[i];
        if(marker >= 0) continue;
        JAVA_INT slot = marker & mask;
        uint32_t perturb = (uint32_t)marker;
        while(target[slot] != 0) {
            perturb >>= 5;
            slot = cn1HmNextSlot(slot, perturb, mask);
        }
        target[slot] = marker;
        cn1RefBlockSet(threadStateData, newKeys, slot, cn1RefBlockGet(keys, i));
        cn1RefBlockSet(threadStateData, newValues, slot, cn1RefBlockGet(values, i));
        if(links) {
            cn1IntBlockSet(newPrev, slot, tail);
            cn1IntBlockSet(newNext, slot, -1);
            if(tail >= 0) cn1IntBlockSet(newNext, tail, slot);
            tail = slot;
        }
    }
    return tail;
}

JAVA_INT java_util_NativeStorage_nextOccupied___long_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG metaBlock, JAVA_INT from, JAVA_INT cap) {
    return cn1InlTableNext(metaBlock, from, cap);
}

// clear(): the reference blocks are blanked under the bulk SATB barrier, the metadata
// with a plain memset (ints are not traced).
JAVA_VOID java_util_NativeStorage_clearMap___long_long_long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG keys, JAVA_LONG vals, JAVA_LONG meta, JAVA_INT cap) {
    cn1RefBlockClear(threadStateData, keys, 0, cap);
    cn1RefBlockClear(threadStateData, vals, 0, cap);
    cn1IntBlockClear(meta, cap);
}

JAVA_VOID java_util_NativeStorage_move___long_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_INT from, JAVA_INT to, JAVA_INT count) {
    cn1RefBlockMove(threadStateData, block, from, to, count);
}
JAVA_VOID java_util_NativeStorage_clear___long_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG block, JAVA_INT from, JAVA_INT count) {
    cn1RefBlockClear(threadStateData, block, from, count);
}
JAVA_VOID java_util_NativeStorage_copy___long_int_long_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG source, JAVA_INT from, JAVA_LONG destination, JAVA_INT to, JAVA_INT count) {
    if(count > 0) memcpy(((JAVA_OBJECT*)(uintptr_t)destination) + to,
        ((JAVA_OBJECT*)(uintptr_t)source) + from, (size_t)count * sizeof(JAVA_OBJECT));
}

JAVA_OBJECT java_util_HashMap_get___java_lang_Object_R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT key) {
    CN1_KEEP_NATIVE_OWNER(__cn1StorageOwner, __cn1ThisObject);
    CN1_KEEP_NATIVE_OWNER(__cn1StorageKey, key);
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    JAVA_INT idx = cn1HmFindSlot(threadStateData, t, key, cn1HmMarker(threadStateData, key));
    if(idx < 0) {
        return JAVA_NULL;
    }
    return ((JAVA_OBJECT*)(uintptr_t)CN1_HM_BLK(t, Vals))[idx];
}

JAVA_BOOLEAN java_util_HashMap_containsKey___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT key) {
    CN1_KEEP_NATIVE_OWNER(__cn1StorageOwner, __cn1ThisObject);
    CN1_KEEP_NATIVE_OWNER(__cn1StorageKey, key);
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    return cn1HmFindSlot(threadStateData, t, key, cn1HmMarker(threadStateData, key)) >= 0 ? JAVA_TRUE : JAVA_FALSE;
}

extern JAVA_VOID java_util_HashMap_cn1Alloc___int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_INT);
extern JAVA_VOID java_util_HashMap_cn1Grow__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);

JAVA_OBJECT java_util_HashMap_put___java_lang_Object_java_lang_Object_R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT key, JAVA_OBJECT value) {
    CN1_KEEP_NATIVE_OWNER(__cn1StorageOwner, __cn1ThisObject);
    CN1_KEEP_NATIVE_OWNER(__cn1StorageKey, key);
    CN1_KEEP_NATIVE_OWNER(__cn1StorageValue, value);
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    JAVA_INT marker = cn1HmMarker(threadStateData, key);
    if(CN1_HM_BLK(t, Meta) == 0) {
        java_util_HashMap_cn1Alloc___int(threadStateData, __cn1ThisObject, t->java_util_HashMap_cn1Cap);
    }
    JAVA_INT idx = cn1HmFindSlot(threadStateData, t, key, marker);
    JAVA_LONG valsObj = CN1_HM_BLK(t, Vals);
    JAVA_OBJECT* vals = (JAVA_OBJECT*)(uintptr_t)valsObj;
    if(idx >= 0) {
        JAVA_OBJECT old = vals[idx];
        CN1_WRITE_BARRIER(valsObj, value);
        CN1_SATB_DELETE(&vals[idx]); // preserve the overwritten value for this mark cycle
        vals[idx] = value;
        return old;
    }
    JAVA_INT ins = -idx - 1;
    JAVA_INT* meta = (JAVA_INT*)(uintptr_t)CN1_HM_BLK(t, Meta);
    JAVA_LONG keysObj = CN1_HM_BLK(t, Keys);
    JAVA_OBJECT* keys = (JAVA_OBJECT*)(uintptr_t)keysObj;
    JAVA_BOOLEAN wasEmpty = meta[ins] == 0 ? JAVA_TRUE : JAVA_FALSE;
    meta[ins] = marker;
    CN1_WRITE_BARRIER(keysObj, key);
    keys[ins] = key;
    CN1_WRITE_BARRIER(valsObj, value);
    vals[ins] = value;
    t->java_util_HashMap_elementCount++;
    if(wasEmpty) {
        t->java_util_HashMap_cn1Occupied++;
    }
    t->java_util_HashMap_modCount++;
    // NOTE: base-class receivers only -- LinkedHashMap overrides put() in Java
    // (virtual dispatch never routes an LHM here), and cn1Grow is package-
    // private so application subclasses cannot override it: the direct call to
    // the base implementation below is exact.
    if(t->java_util_HashMap_cn1Occupied >= t->java_util_HashMap_threshold) {
        java_util_HashMap_cn1Grow__(threadStateData, __cn1ThisObject);
    }
    return JAVA_NULL;
}

JAVA_OBJECT java_util_HashMap_remove___java_lang_Object_R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT key) {
    CN1_KEEP_NATIVE_OWNER(__cn1StorageOwner, __cn1ThisObject);
    CN1_KEEP_NATIVE_OWNER(__cn1StorageKey, key);
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    JAVA_INT idx = cn1HmFindSlot(threadStateData, t, key, cn1HmMarker(threadStateData, key));
    if(idx < 0) {
        return JAVA_NULL;
    }
    JAVA_INT* meta = (JAVA_INT*)(uintptr_t)CN1_HM_BLK(t, Meta);
    JAVA_OBJECT* keys = (JAVA_OBJECT*)(uintptr_t)CN1_HM_BLK(t, Keys);
    JAVA_OBJECT* vals = (JAVA_OBJECT*)(uintptr_t)CN1_HM_BLK(t, Vals);
    JAVA_OBJECT old = vals[idx];
    CN1_SATB_DELETE(&keys[idx]); // preserve the removed key/value for this mark cycle
    CN1_SATB_DELETE(&vals[idx]);
    meta[idx] = 1; // META_TOMB
    keys[idx] = JAVA_NULL;
    vals[idx] = JAVA_NULL;
    t->java_util_HashMap_elementCount--;
    t->java_util_HashMap_modCount++;
    return old;
}

JAVA_VOID java_util_HashMap_clear__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    CN1_KEEP_NATIVE_OWNER(__cn1StorageOwner, __cn1ThisObject);
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    if(t->java_util_HashMap_elementCount > 0 || t->java_util_HashMap_cn1Occupied > 0) {
        int len = t->java_util_HashMap_cn1Cap;
        java_util_NativeStorage_clearMap___long_long_long_int(threadStateData,
            CN1_HM_BLK(t, Keys), CN1_HM_BLK(t, Vals),
            CN1_HM_BLK(t, Meta), len);
        t->java_util_HashMap_elementCount = 0;
        t->java_util_HashMap_cn1Occupied = 0;
        t->java_util_HashMap_modCount++;
    }
}

JAVA_OBJECT java_util_Locale_getOSLanguage___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) && defined(__OBJC__)
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSUserDefaults* defs = [NSUserDefaults standardUserDefaults];
    NSArray* languages = [defs objectForKey:@"AppleLanguages"];
    NSString* language_ = [languages objectAtIndex:0];
    JAVA_OBJECT language = fromNSString(threadStateData, language_);
    [pool release];
    return language;
#else
    return newStringFromCString(threadStateData, "en");
#endif
}

#if !defined(__APPLE__) || !defined(__OBJC__)
static char* cn1_strdup(const char* value) {
    if (value == NULL) {
        return NULL;
    }
    size_t len = strlen(value);
    char* out = (char*)malloc(len + 1);
    if (out != NULL) {
        memcpy(out, value, len + 1);
    }
    return out;
}

static pthread_mutex_t cn1_timezone_mutex = PTHREAD_MUTEX_INITIALIZER;

static void cn1_with_timezone(const char* zoneId, void (*func)(void*), void* ctx) {
    pthread_mutex_lock(&cn1_timezone_mutex);
    char* original = cn1_strdup(getenv("TZ"));
    if (zoneId != NULL && strlen(zoneId) > 0) {
        setenv("TZ", zoneId, 1);
    } else {
        unsetenv("TZ");
    }
    tzset();
    func(ctx);
    if (original != NULL) {
        setenv("TZ", original, 1);
        free(original);
    } else {
        unsetenv("TZ");
    }
    tzset();
    pthread_mutex_unlock(&cn1_timezone_mutex);
}

/*
 * Windows named-zone support.
 *
 * The POSIX path below sets TZ and reads tm_gmtoff back. Neither half works
 * here: the Microsoft C runtime only understands the "EST5EDT" form of TZ, not
 * an IANA identifier, and its struct tm carries no GMT offset at all -- so
 * every named zone resolved to an offset of zero and, for instance,
 * America/New_York reported UTC. Windows ships ICU (icu.dll, Windows 10 1703
 * and later), whose calendar speaks IANA identifiers directly and knows the
 * daylight rules for the instant being asked about.
 *
 * cn1WinZoneOffsetMillis answers the total offset (zone + daylight) at an
 * instant, or reports failure so the caller can fall back.
 */
#ifdef _WIN32
/* Total offset (zone plus daylight) for a zone at an instant, 0 when the
 * platform cannot answer -- the caller then keeps the C runtime's reply.
 * The lookup itself lives in cn1_win_compat.c, the one translation unit that
 * may include <windows.h>; keeping it out of here is what lets the clean
 * target compile this file against a minimal SDK layout. */
static int cn1WinZoneOffsetMillis(const char* zoneId, long long millis, int* offsetOut,
                                  int* dstOut, int* rawOut) {
    return cn1_win_zone_offset_millis(zoneId, millis, offsetOut, dstOut, rawOut);
}

/* Milliseconds since the epoch for a set of UTC calendar fields. */
static long long cn1WinUtcMillis(int year, int month, int day, int millisOfDay) {
    struct tm utc;
    memset(&utc, 0, sizeof(utc));
    utc.tm_year = year - 1900;
    utc.tm_mon = month - 1;
    utc.tm_mday = day;
    utc.tm_hour = millisOfDay / 3600000;
    utc.tm_min = (millisOfDay / 60000) % 60;
    utc.tm_sec = (millisOfDay / 1000) % 60;
    utc.tm_isdst = 0;
    return (long long) timegm(&utc) * 1000LL;
}
#endif

typedef struct {
    int year;
    int month;
    int day;
    int millis;
    int result;
} cn1_timezone_offset_ctx;

static void cn1_compute_timezone_offset(void* data) {
    cn1_timezone_offset_ctx* ctx = (cn1_timezone_offset_ctx*)data;
    struct tm utc;
    memset(&utc, 0, sizeof(utc));
    utc.tm_year = ctx->year - 1900;
    utc.tm_mon = ctx->month - 1;
    utc.tm_mday = ctx->day;
    utc.tm_hour = ctx->millis / 3600000;
    utc.tm_min = (ctx->millis / 60000) % 60;
    utc.tm_sec = (ctx->millis / 1000) % 60;
    utc.tm_isdst = 0;
    time_t epoch = timegm(&utc);
    struct tm resolved;
    localtime_r(&epoch, &resolved);
#if defined(__APPLE__) || defined(__USE_MISC)
    ctx->result = (int)resolved.tm_gmtoff * 1000;
#else
    ctx->result = 0;
#endif
}

typedef struct {
    long long millis;
    int result;
} cn1_timezone_dst_ctx;

static void cn1_compute_timezone_dst(void* data) {
    cn1_timezone_dst_ctx* ctx = (cn1_timezone_dst_ctx*)data;
    time_t epoch = (time_t)(ctx->millis / 1000LL);
    struct tm resolved;
    localtime_r(&epoch, &resolved);
    ctx->result = resolved.tm_isdst > 0 ? JAVA_TRUE : JAVA_FALSE;
}

typedef struct {
    int januaryOffset;
    int januaryIsDst;
    int julyOffset;
    int julyIsDst;
} cn1_timezone_raw_ctx;

static void cn1_compute_timezone_raw(void* data) {
    cn1_timezone_raw_ctx* ctx = (cn1_timezone_raw_ctx*)data;
    time_t now = time(NULL);
    struct tm sample;
    localtime_r(&now, &sample);
    sample.tm_year = 124;
    sample.tm_mon = 0;
    sample.tm_mday = 1;
    sample.tm_hour = 12;
    sample.tm_min = 0;
    sample.tm_sec = 0;
    sample.tm_isdst = -1;
    time_t january = mktime(&sample);
    localtime_r(&january, &sample);
#if defined(__APPLE__) || defined(__USE_MISC)
    ctx->januaryOffset = (int)sample.tm_gmtoff * 1000;
    ctx->januaryIsDst = sample.tm_isdst > 0;
#else
    ctx->januaryOffset = 0;
    ctx->januaryIsDst = 0;
#endif
    sample.tm_year = 124;
    sample.tm_mon = 6;
    sample.tm_mday = 1;
    sample.tm_hour = 12;
    sample.tm_min = 0;
    sample.tm_sec = 0;
    sample.tm_isdst = -1;
    time_t july = mktime(&sample);
    localtime_r(&july, &sample);
#if defined(__APPLE__) || defined(__USE_MISC)
    ctx->julyOffset = (int)sample.tm_gmtoff * 1000;
    ctx->julyIsDst = sample.tm_isdst > 0;
#else
    ctx->julyOffset = 0;
    ctx->julyIsDst = 0;
#endif
}
#endif

#if !defined(__APPLE__) || !defined(__OBJC__)
JAVA_OBJECT java_util_TimeZone_getTimezoneId___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    time_t now = time(NULL);
    struct tm localTm;
    localtime_r(&now, &localTm);
#if defined(__APPLE__) || defined(__USE_MISC)
    if (localTm.tm_zone != NULL) {
        return newStringFromCString(threadStateData, localTm.tm_zone);
    }
#endif
    const char* tz = getenv("TZ");
    return newStringFromCString(threadStateData, tz == NULL ? "GMT" : tz);
}

JAVA_INT java_util_TimeZone_getTimezoneOffset___java_lang_String_int_int_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name, JAVA_INT year, JAVA_INT month, JAVA_INT day, JAVA_INT timeOfDayMillis) {
    const char* buffer = stringToUTF8(threadStateData, name);
    cn1_timezone_offset_ctx ctx;
#ifdef _WIN32
    {
        /* The fields are UTC, matching the POSIX path below (timegm) and every
         * other port. Reading them as local standard time instead -- which is
         * what java.util.TimeZone.getOffset's signature suggests -- moves the
         * instant by the raw offset and jumps DST transitions: TimeApiTest
         * resolves 2020-03-08T01:30 EST as 02:30 EDT. The contract this native
         * actually has is the one its callers and that test rely on. */
        int offset = 0;
        if (cn1WinZoneOffsetMillis(buffer, cn1WinUtcMillis(year, month, day, timeOfDayMillis),
                                   &offset, 0, 0)) {
            return offset;
        }
    }
#endif
    ctx.year = year;
    ctx.month = month;
    ctx.day = day;
    ctx.millis = timeOfDayMillis;
    ctx.result = 0;
    cn1_with_timezone(buffer, cn1_compute_timezone_offset, &ctx);
    return ctx.result;
}

JAVA_INT java_util_TimeZone_getTimezoneRawOffset___java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name) {
    const char* buffer = stringToUTF8(threadStateData, name);
    cn1_timezone_raw_ctx ctx;
#ifdef _WIN32
    {
        /* The raw offset is the standard-time one in force right now. ICU keeps
         * the base offset and the daylight adjustment in separate calendar
         * fields, so asking at the current instant answers it directly.
         *
         * This used to sample both solstices of the current year and prefer
         * July's when neither was in daylight saving, which is wrong whenever
         * the base offset changes mid-year: with the clock in February 2024,
         * Asia/Almaty was still UTC+6 but July's reading is the UTC+5 rule that
         * had not taken effect yet, and a change landing after July stayed
         * invisible for the rest of the year. */
        int rawOffset = 0;
        long long nowMillis = (long long) time(NULL) * 1000LL;
        if (cn1WinZoneOffsetMillis(buffer, nowMillis, 0, 0, &rawOffset)) {
            return rawOffset;
        }
    }
#endif
    ctx.januaryOffset = 0;
    ctx.januaryIsDst = 0;
    ctx.julyOffset = 0;
    ctx.julyIsDst = 0;
    cn1_with_timezone(buffer, cn1_compute_timezone_raw, &ctx);
    if (!ctx.januaryIsDst) {
        return ctx.januaryOffset;
    }
    if (!ctx.julyIsDst) {
        return ctx.julyOffset;
    }
    return ctx.januaryOffset < ctx.julyOffset ? ctx.januaryOffset : ctx.julyOffset;
}

JAVA_BOOLEAN java_util_TimeZone_isTimezoneDST___java_lang_String_long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT name, JAVA_LONG millis) {
    const char* buffer = stringToUTF8(threadStateData, name);
    cn1_timezone_dst_ctx ctx;
#ifdef _WIN32
    {
        int dst = 0;
        if (cn1WinZoneOffsetMillis(buffer, (long long) millis, 0, &dst, 0)) {
            return dst ? JAVA_TRUE : JAVA_FALSE;
        }
    }
#endif
    ctx.millis = millis;
    ctx.result = JAVA_FALSE;
    cn1_with_timezone(buffer, cn1_compute_timezone_dst, &ctx);
    return ctx.result;
}
#endif

/*JAVA_OBJECT java_util_Locale_getOSCountry___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
}*/

JAVA_OBJECT java_text_DateFormat_format___java_util_Date_java_lang_StringBuffer_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
#if defined(__APPLE__) && defined(__OBJC__)
    struct obj__java_text_DateFormat* df = (struct obj__java_text_DateFormat*)__cn1ThisObject;
    POOL_BEGIN();
#ifndef CN1_USE_ARC
    NSDateFormatter *formatter = [[[NSDateFormatter alloc] init] autorelease];
#else
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
#endif
    struct obj__java_util_Date* dateObj = (struct obj__java_util_Date*)__cn1Arg1;
    NSDate* date = [NSDate dateWithTimeIntervalSince1970:(dateObj->java_util_Date_date / 1000)];

    switch (df->java_text_DateFormat_dateStyle) {
        // no date - only time
        case -1:
            [formatter setDateStyle:NSDateFormatterNoStyle];
            switch (df->java_text_DateFormat_dateStyle) {
                // long time format
                case 1:
                    [formatter setTimeStyle:NSDateFormatterLongStyle];
                    break;
                    
                // medium time format
                case 2:
                    [formatter setTimeStyle:NSDateFormatterMediumStyle];
                    break;
                    
                // short time format
                case 3:
                    [formatter setTimeStyle:NSDateFormatterShortStyle];
                    break;
                   
                // full time format
                default:
                    [formatter setTimeStyle:NSDateFormatterFullStyle];
                    break;
            }
            break;

        // long date format
        case 1:
            [formatter setDateStyle:NSDateFormatterLongStyle];
            break;

        // medium date length
        case 2:
            [formatter setDateStyle:NSDateFormatterMediumStyle];
            break;

        // short date length
        case 3:
            [formatter setDateStyle:NSDateFormatterShortStyle];
            break;
            
        // full date
        default:
            [formatter setDateStyle:NSDateFormatterFullStyle];
            break;
    }
    JAVA_OBJECT str = fromNSString(CN1_THREAD_STATE_PASS_ARG [formatter stringFromDate:date]);
    POOL_END();

    return str;
#else
    /* Clean target (Windows / Linux native): format via the C library so that
     * Date.toString() (which routes here through DateFormat) returns a real
     * string rather than NULL. A NULL here propagates into callers such as
     * DateSpinner3D, which do formatDateLongStyle(new Date()).substring(0,1)
     * and crash on the null receiver. Styles: FULL=0, LONG=1, MEDIUM=2,
     * SHORT=3; a negative style means that component is omitted (NONE). */
    struct obj__java_text_DateFormat* df = (struct obj__java_text_DateFormat*)__cn1ThisObject;
    struct obj__java_util_Date* dateObj = (struct obj__java_util_Date*)__cn1Arg1;
    if (dateObj == JAVA_NULL) {
        return JAVA_NULL;
    }
    time_t secs = (time_t)(dateObj->java_util_Date_date / 1000);
    struct tm tmv;
#ifdef _WIN32
    if (localtime_s(&tmv, &secs) != 0) { memset(&tmv, 0, sizeof(tmv)); }
#else
    localtime_r(&secs, &tmv);
#endif
    int dateStyle = df->java_text_DateFormat_dateStyle;
    int timeStyle = df->java_text_DateFormat_timeStyle;
    char datePart[128]; datePart[0] = '\0';
    char timePart[128]; timePart[0] = '\0';
    char out[300];
    if (dateStyle >= 0) {
        const char* dfmt;
        switch (dateStyle) {
            case 1:  dfmt = "%B %d, %Y"; break;     /* LONG */
            case 2:  dfmt = "%b %d, %Y"; break;     /* MEDIUM */
            case 3:  dfmt = "%m/%d/%y"; break;      /* SHORT */
            default: dfmt = "%A, %B %d, %Y"; break; /* FULL */
        }
        strftime(datePart, sizeof(datePart), dfmt, &tmv);
    }
    if (timeStyle >= 0) {
        const char* tfmt = (timeStyle == 3) ? "%I:%M %p" : "%I:%M:%S %p";
        strftime(timePart, sizeof(timePart), tfmt, &tmv);
    }
    if (datePart[0] != '\0' && timePart[0] != '\0') {
        snprintf(out, sizeof(out), "%s %s", datePart, timePart);
    } else if (datePart[0] != '\0') {
        snprintf(out, sizeof(out), "%s", datePart);
    } else if (timePart[0] != '\0') {
        snprintf(out, sizeof(out), "%s", timePart);
    } else {
        snprintf(out, sizeof(out), "%lld", (long long)(dateObj->java_util_Date_date));
    }
    return newStringFromCString(threadStateData, out);
#endif
}


JAVA_CHAR java_lang_String_charAt___int_R_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_INT __cn1Arg1) {
    struct obj__java_lang_String* encString = (struct obj__java_lang_String*)__cn1ThisObject;
    // JDK contract: bound by the string's LOGICAL length, not the backing
    // array's capacity (offset/aliasing-constructed strings differ)
    if(__cn1Arg1 < 0 || __cn1Arg1 >= encString->java_lang_String_count) { THROW_ARRAY_INDEX_EXCEPTION(__cn1Arg1); }
    // coder-aware: decodes Latin-1(byte[]) or UTF-16(char[])
    return cn1StrCharAtRaw(__cn1ThisObject, __cn1Arg1);
}

JAVA_INT java_lang_String_indexOf___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_INT ch, JAVA_INT fromIndex) {
    fromIndex = MAX(0, fromIndex);
    struct obj__java_lang_String* encString = (struct obj__java_lang_String*)__cn1ThisObject;
    int off = 0;
    int count = encString->java_lang_String_count;
    if(cn1StrIsLatin1(__cn1ThisObject)) {
        // Latin-1: (b & 0xff) IS the char value; matches char[] scan bit-identically.
        JAVA_ARRAY_BYTE* encArr = (JAVA_ARRAY_BYTE*)cn1StrChars(__cn1ThisObject);
        int endOff = off+count;
        for (int i=off+fromIndex; i<endOff; i++) {
            if ((encArr[i] & 0xff) == ch) {
                return i-off;
            }
        }
        return -1;
    }
    JAVA_ARRAY_CHAR* encArr = (JAVA_ARRAY_CHAR*)cn1StrChars(__cn1ThisObject);
    int endOff = off+count;
    for (int i=off+fromIndex; i<endOff; i++) {
        if (encArr[i] == ch) {
            return i-off;
        }
    }
    return -1;
}

JAVA_OBJECT java_lang_String_toString___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
    return __cn1ThisObject;
}

static inline void* cn1BuilderData(JAVA_OBJECT builder) {
    return (void*)(uintptr_t)((struct obj__java_lang_StringBuilder*)builder)->java_lang_StringBuilder_cn1Storage;
}
static inline int cn1BuilderIsLatin1(JAVA_OBJECT builder) {
    return !((struct obj__java_lang_StringBuilder*)builder)->java_lang_StringBuilder_wide;
}
static inline JAVA_CHAR cn1BuilderUnit(JAVA_OBJECT builder, JAVA_INT index) {
    const void* data = cn1BuilderData(builder);
    return cn1BuilderIsLatin1(builder) ? (JAVA_CHAR)((const uint8_t*)data)[index]
        : ((const JAVA_ARRAY_CHAR*)data)[index];
}
static inline void cn1BuilderStore(JAVA_OBJECT builder, JAVA_INT index, JAVA_CHAR value) {
    void* data = cn1BuilderData(builder);
    if(cn1BuilderIsLatin1(builder)) ((uint8_t*)data)[index] = (uint8_t)value;
    else ((JAVA_ARRAY_CHAR*)data)[index] = value;
}

/* GROWTH THAT FITS THE STORAGE THE BUILDER ALREADY OWNS IS NOT A CALL.
 *
 * Every native append that runs out of capacity called Java enlargeBuffer, which
 * calls resizeBuffer, which calls the resizeBufferImpl native -- three calls -- and
 * when the new size still fits the block the builder is already using (its inline
 * bytes, the CN1StackBuffer an escape-proven builder lives in, or a heap block with
 * slack) all three together do exactly one thing: store the new capacity. In
 * stringBuilding that chain was ~550 of ~3200 mutator samples, because a stack
 * builder's growth from 16 to 34 always fits its stack buffer.
 *
 * The policy is enlargeBuffer's, byte for byte -- capacity*2+2, or the minimum when
 * that is larger or overflows -- so capacity() reports exactly what the Java path
 * would have set. Anything that does not fit, or is not a plain in-place case, goes
 * to enlargeBuffer unchanged, which stays the source of truth for allocation,
 * copying and the OutOfMemoryError on a negative minimum. */
static JAVA_VOID cn1SbEnsure(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder, JAVA_INT needed);

JAVA_BOOLEAN java_lang_StringBuilder_resizeBufferImpl___int_boolean_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder, JAVA_INT capacity, JAVA_BOOLEAN wide) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, builder);
    struct obj__java_lang_StringBuilder* target = (struct obj__java_lang_StringBuilder*)builder;
    if(capacity < 0 || capacity > INT_MAX / (wide ? 2 : 1)) return JAVA_FALSE;
    int wasWide = target->java_lang_StringBuilder_wide;
    int count = target->java_lang_StringBuilder_count;
    JAVA_LONG previous = target->java_lang_StringBuilder_cn1Storage;
    JAVA_LONG inlineStorage = (JAVA_LONG)(uintptr_t)target->__cn1InlineStorage;
    int inlineBytes = sizeof(target->__cn1InlineStorage);
    struct CN1StackBuffer* scope = NULL;
    if(target->__heapPosition == CN1_GC_STACK_BUILDER) {
        memcpy(&scope, target->__cn1InlineStorage, sizeof(scope));
        inlineStorage = (JAVA_LONG)(uintptr_t)scope->initialData;
        inlineBytes = scope->initialBytes;
    }
    int bytes = capacity * (wide ? 2 : 1);
    JAVA_LONG block;
    if(bytes <= inlineBytes) {
        block = inlineStorage;
        if(previous != inlineStorage) {
            if(count > 0) memcpy((void*)(uintptr_t)inlineStorage, (void*)(uintptr_t)previous,
                                 (size_t)count * (wasWide ? 2 : 1));
            cn1RefBlockFree(previous);
        }
    } else {
        block = cn1PrimitiveBlockResize(previous == inlineStorage ? 0 : previous, bytes);
        if(block == 0) return JAVA_FALSE;
        if(previous == inlineStorage && count > 0)
            memcpy((void*)(uintptr_t)block, (void*)(uintptr_t)inlineStorage, (size_t)count * (wasWide ? 2 : 1));
    }
    if(scope != NULL) scope->owned = block == inlineStorage ? 0 : block;
    target->java_lang_StringBuilder_cn1Storage = block;
    if(wide && !wasWide) {
        // The bytes and widened units overlap: walk backwards in the same block.
        uint8_t* bytes = (uint8_t*)(uintptr_t)block;
        JAVA_ARRAY_CHAR* chars = (JAVA_ARRAY_CHAR*)(uintptr_t)block;
        for(int i = count; i-- > 0;) chars[i] = bytes[i];
    }
    target->java_lang_StringBuilder_capacity = capacity;
    target->java_lang_StringBuilder_wide = wide;
    return JAVA_TRUE;
}

static JAVA_VOID cn1SbEnsure(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder, JAVA_INT needed) {
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)builder;
    JAVA_INT cap = t->java_lang_StringBuilder_capacity;
    if(__builtin_expect(needed >= 0, 1)) {
        JAVA_INT grown = cap * 2 + 2;
        if(grown < needed || grown < 0) {
            grown = needed;
        }
        long long bytes = (long long)grown * (t->java_lang_StringBuilder_wide ? 2 : 1);
        JAVA_LONG storage = t->java_lang_StringBuilder_cn1Storage;
        long long avail = -1;
        if(t->__heapPosition == CN1_GC_STACK_BUILDER) {
            struct CN1StackBuffer* scope;
            memcpy(&scope, t->__cn1InlineStorage, sizeof(scope));
            if(storage == (JAVA_LONG)(uintptr_t)scope->initialData) {
                avail = scope->initialBytes;
            }
        } else if(storage == (JAVA_LONG)(uintptr_t)t->__cn1InlineStorage) {
            avail = (long long)sizeof(t->__cn1InlineStorage);
        }
        if(bytes <= avail) {
            t->java_lang_StringBuilder_capacity = grown;
            return;
        }
    }
    java_lang_StringBuilder_enlargeBuffer___int(threadStateData, builder, needed);
}

JAVA_CHAR java_lang_StringBuilder_unit___int_R_char(CODENAME_ONE_THREAD_STATE,
        JAVA_OBJECT builder, JAVA_INT index) {
    return cn1BuilderUnit(builder, index);
}
JAVA_VOID java_lang_StringBuilder_put___int_char(CODENAME_ONE_THREAD_STATE,
        JAVA_OBJECT builder, JAVA_INT index, JAVA_CHAR value) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, builder);
    if(value > 255 && cn1BuilderIsLatin1(builder))
        java_lang_StringBuilder_widen__(threadStateData, builder);
    cn1BuilderStore(builder, index, value);
}
JAVA_VOID java_lang_StringBuilder_move___int_int_int(CODENAME_ONE_THREAD_STATE,
        JAVA_OBJECT builder, JAVA_INT from, JAVA_INT to, JAVA_INT length) {
    if(length <= 0) return;
    size_t width = cn1BuilderIsLatin1(builder) ? 1 : sizeof(JAVA_ARRAY_CHAR);
    char* data = (char*)cn1BuilderData(builder);
    memmove(data + (size_t)to * width, data + (size_t)from * width, (size_t)length * width);
}
JAVA_VOID java_lang_StringBuilder_zero___int_int(CODENAME_ONE_THREAD_STATE,
        JAVA_OBJECT builder, JAVA_INT from, JAVA_INT length) {
    if(length <= 0) return;
    size_t width = cn1BuilderIsLatin1(builder) ? 1 : sizeof(JAVA_ARRAY_CHAR);
    memset((char*)cn1BuilderData(builder) + (size_t)from * width, 0, (size_t)length * width);
}

JAVA_CHAR java_lang_StringBuilder_charAt___int_R_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder, JAVA_INT index) {
    java_lang_StringBuilder_checkIndex___int(threadStateData, builder, index);
    return cn1BuilderUnit(builder, index);
}

// Small-copy helper: a libc memcpy call costs more than the copy itself for the
// short segments string building deals in (a handful of UTF-16 chars). Inline
// 8-byte word moves up to 32 chars; beyond that libc's SIMD wins. src/dst never
// overlap here (fresh destination or append region beyond the source).
static inline void cn1CharCopy(JAVA_ARRAY_CHAR* dst, const JAVA_ARRAY_CHAR* src, int count) {
    if(count <= 32) {
        int b = count * (int)sizeof(JAVA_ARRAY_CHAR);
        int i = 0;
        for(; i + 8 <= b; i += 8) {
            uint64_t w; memcpy(&w, (const char*)src + i, 8); memcpy((char*)dst + i, &w, 8);
        }
        for(; i + 2 <= b; i += 2) {
            uint16_t w; memcpy(&w, (const char*)src + i, 2); memcpy((char*)dst + i, &w, 2);
        }
    } else {
        memcpy(dst, src, (size_t)count * sizeof(JAVA_ARRAY_CHAR));
    }
}

JAVA_OBJECT java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder, JAVA_OBJECT str) {
    enteringNativeAllocations();
    if(str == JAVA_NULL) {
        java_lang_StringBuilder_appendNull__(threadStateData, builder);
        finishedNativeAllocations();
        return builder;
    }
    CN1_KEEP_NATIVE_OWNER(bufferOwner, builder);
    CN1_KEEP_NATIVE_OWNER(stringOwner, str);
    struct obj__java_lang_String* source = (struct obj__java_lang_String*)str;
    struct obj__java_lang_StringBuilder* target = (struct obj__java_lang_StringBuilder*)builder;
    JAVA_INT length = source->java_lang_String_count, count = target->java_lang_StringBuilder_count;
    if(length == 0) { finishedNativeAllocations(); return builder; }
    JAVA_INT needed = count + length;
    if(needed < 0 || needed > target->java_lang_StringBuilder_capacity)
        cn1SbEnsure(threadStateData, builder, needed);
    int sourceLatin1 = cn1StrIsLatin1((JAVA_OBJECT)source);
    if(!sourceLatin1 && cn1BuilderIsLatin1(builder)) {
        JAVA_ARRAY_CHAR* units = (JAVA_ARRAY_CHAR*)cn1StrChars((JAVA_OBJECT)source);
        for(int i = 0; i < length; i++) if(units[i] > 255) {
            java_lang_StringBuilder_widen__(threadStateData, builder);
            break;
        }
    }
    void* output = cn1BuilderData(builder);
    if(sourceLatin1 && cn1BuilderIsLatin1(builder)) {
        cn1SmallCopy((JAVA_ARRAY_BYTE*)output + count,
               (JAVA_ARRAY_BYTE*)cn1StrChars((JAVA_OBJECT)source), (size_t)length);
    } else if(!sourceLatin1 && !cn1BuilderIsLatin1(builder)) {
        cn1CharCopy((JAVA_ARRAY_CHAR*)output + count,
                   (JAVA_ARRAY_CHAR*)cn1StrChars((JAVA_OBJECT)source), length);
    } else {
        for(int i = 0; i < length; i++) {
            JAVA_CHAR ch = sourceLatin1 ? (JAVA_CHAR)((uint8_t*)cn1StrChars((JAVA_OBJECT)source))[i]
                : ((JAVA_ARRAY_CHAR*)cn1StrChars((JAVA_OBJECT)source))[i];
            cn1BuilderStore(builder, count + i, ch);
        }
    }
    target->java_lang_StringBuilder_count = needed;
    finishedNativeAllocations();
    return builder;
}

// Range bounds are checked by the Java entry point. Known compact sequences
// copy whole ranges; arbitrary CharSequence implementations retain their Java
// charAt semantics in the caller. Re-read storage after growth for self-append.
JAVA_BOOLEAN java_lang_StringBuilder_tryAppendRange___java_lang_CharSequence_int_int_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder, JAVA_OBJECT text, JAVA_INT start, JAVA_INT end) {
    int sourceBuilder = text->__codenameOneParentClsReference == &class__java_lang_StringBuilder;
    if(!sourceBuilder && !cn1IsStringClass(text->__codenameOneParentClsReference)) return JAVA_FALSE;
    JAVA_INT length = end - start;
    if(length == 0) return JAVA_TRUE;
    CN1_KEEP_NATIVE_OWNER(bufferOwner, builder);
    CN1_KEEP_NATIVE_OWNER(sourceOwner, text);
    struct obj__java_lang_StringBuilder* target = (struct obj__java_lang_StringBuilder*)builder;
    JAVA_INT count = target->java_lang_StringBuilder_count;
    JAVA_INT needed = count + length;
    if(needed < 0 || needed > target->java_lang_StringBuilder_capacity)
        cn1SbEnsure(threadStateData, builder, needed);
    int sourceLatin1;
    const void* input;
    if(sourceBuilder) {
        sourceLatin1 = cn1BuilderIsLatin1(text);
        input = cn1BuilderData(text);
    } else {
        struct obj__java_lang_String* string = (struct obj__java_lang_String*)text;
        sourceLatin1 = cn1StrIsLatin1(text);
        input = cn1StrChars(text);
        start += 0;
    }
    if(!sourceLatin1 && cn1BuilderIsLatin1(builder)) {
        const JAVA_ARRAY_CHAR* units = (const JAVA_ARRAY_CHAR*)input + start;
        for(int i = 0; i < length; i++) if(units[i] > 255) {
            java_lang_StringBuilder_widen__(threadStateData, builder);
            break;
        }
    }
    // Widening can also replace a builder's storage. In the aliasing case source
    // and destination have the same coder; reload both only after resizing ends.
    if(sourceBuilder) {
        sourceLatin1 = cn1BuilderIsLatin1(text);
        input = cn1BuilderData(text);
    }
    int targetLatin1 = cn1BuilderIsLatin1(builder);
    void* output = cn1BuilderData(builder);
    if(sourceLatin1 == targetLatin1) {
        size_t width = sourceLatin1 ? 1 : sizeof(JAVA_ARRAY_CHAR);
        memmove((char*)output + (size_t)count * width,
                (const char*)input + (size_t)start * width, (size_t)length * width);
    } else if(sourceLatin1) {
        const uint8_t* src = (const uint8_t*)input + start;
        JAVA_ARRAY_CHAR* dst = (JAVA_ARRAY_CHAR*)output + count;
        for(int i = 0; i < length; i++) dst[i] = src[i];
    } else {
        const JAVA_ARRAY_CHAR* src = (const JAVA_ARRAY_CHAR*)input + start;
        uint8_t* dst = (uint8_t*)output + count;
        for(int i = 0; i < length; i++) dst[i] = (uint8_t)src[i];
    }
    target->java_lang_StringBuilder_count = needed;
    return JAVA_TRUE;
}

JAVA_OBJECT java_lang_StringBuilder_append___char_1ARRAY_int_int_R_java_lang_StringBuilder(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder, JAVA_OBJECT source, JAVA_INT offset, JAVA_INT length) {
    if(source == JAVA_NULL) THROW_NULL_POINTER_EXCEPTION();
    JAVA_ARRAY array = (JAVA_ARRAY)source;
    if(offset < 0 || length < 0 || offset > array->length - length) THROW_ARRAY_INDEX_EXCEPTION(offset);
    if(length == 0) return builder;
    CN1_KEEP_NATIVE_OWNER(bufferOwner, builder);
    CN1_KEEP_NATIVE_OWNER(arrayOwner, source);
    struct obj__java_lang_StringBuilder* target = (struct obj__java_lang_StringBuilder*)builder;
    int count = target->java_lang_StringBuilder_count;
    int needed = count + length;
    if(needed < 0 || needed > target->java_lang_StringBuilder_capacity)
        cn1SbEnsure(threadStateData, builder, needed);
    const JAVA_ARRAY_CHAR* input = (const JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA(array) + offset;
    if(cn1BuilderIsLatin1(builder)) {
        for(int i = 0; i < length; i++) if(input[i] > 255) {
            java_lang_StringBuilder_widen__(threadStateData, builder);
            break;
        }
    }
    if(cn1BuilderIsLatin1(builder)) {
        uint8_t* data = (uint8_t*)cn1BuilderData(builder) + count;
        for(int i = 0; i < length; i++) data[i] = (uint8_t)input[i];
    } else {
        cn1CharCopy((JAVA_ARRAY_CHAR*)cn1BuilderData(builder) + count, input, length);
    }
    target->java_lang_StringBuilder_count = needed;
    return builder;
}

// The oversized result remains two managed objects. The mutable builder buffer
// stays native and is copied directly, without an intermediate Java array.
static JAVA_OBJECT cn1BuilderStringCopy(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder, int latin1) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, builder);
    int count = ((struct obj__java_lang_StringBuilder*)builder)->java_lang_StringBuilder_count;
    JAVA_OBJECT result = __NEW_INSTANCE_java_lang_String(threadStateData);
    CN1_KEEP_NATIVE_OWNER(resultOwner, result);
    JAVA_OBJECT value = latin1 ? __NEW_ARRAY_JAVA_BYTE(threadStateData, count)
                              : __NEW_ARRAY_JAVA_CHAR(threadStateData, count);
    struct obj__java_lang_String* text = (struct obj__java_lang_String*)result;
    text->java_lang_String_value = value;
    text->java_lang_String_count = count;
    if(count > 0) {
        void* destination = CN1_ARRAY_DATA((JAVA_ARRAY)value);
        if(!latin1 || cn1BuilderIsLatin1(builder)) {
            memcpy(destination, cn1BuilderData(builder), (size_t)count * (latin1 ? 1 : sizeof(JAVA_ARRAY_CHAR)));
        } else {
            for(int i = 0; i < count; i++) ((uint8_t*)destination)[i] = (uint8_t)cn1BuilderUnit(builder, i);
        }
    }
    return result;
}

// Native toString: the result String is ONE fused block (object + char[] child)
// filled with a single memcpy -- no Java ctor chain, no System.arraycopy, no
// separate array allocation. Falls back to the pure-Java twin when the fused
// allocator declines (oversize for BiBOP).
/*
 * SUBSTRING AS ONE ALLOCATION.
 *
 * The Java slice constructor allocates twice -- a backing array, then the String that
 * points at it -- and substring is the busiest String producer in this VM: 290,581 calls
 * on the self-hosting corpus averaging TWELVE characters, with 866 call sites in the
 * framework core alone. At that length the separate array is mostly header: 32 bytes of
 * array header carrying 12 bytes of payload. Fusing the characters into the String's own
 * block removes the second allocation; the embedded array header is still present.
 *
 * The parent's coder is preserved rather than re-derived. A slice of a Latin-1 string is
 * Latin-1 by construction -- every unit is one the parent already held -- so there is
 * nothing to scan for, unlike StringBuilder.toString which is converting from char[].
 *
 * Returns JAVA_NULL when a fused block is unavailable (over CN1_BIBOP_MAX_OBJECT, or
 * BiBOP disabled); String.substring then takes the ordinary two-object path, which is
 * correct for any length. That keeps this native free of bounds checking and exception
 * construction -- substring has already done both before calling.
 */
/* The inline store, seen from Java.
 *
 * Only meaningful when value == null, which the callers all check. The coder is
 * the class word -- cn1StrIsLatin1 reads it -- and the characters follow the
 * fields, which cn1StrChars knows. Java asks through these two and nothing else.
 */
JAVA_BOOLEAN java_lang_String_cn1InlineLatin1___R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    return cn1StrIsLatin1(__cn1ThisObject) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_CHAR java_lang_String_cn1InlineCharAt___int_R_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT index) {
    return cn1StrCharAtRaw(__cn1ThisObject, index);
}

JAVA_OBJECT java_lang_String_cn1SubstringFused___int_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT off, JAVA_INT n) {
    struct obj__java_lang_String* me = (struct obj__java_lang_String*)__cn1ThisObject;
    if(n < 0) {
        return JAVA_NULL;
    }
    // NOT "value == NULL means no source": a NULL store is now the INLINE marker,
    // and an inline parent is the common case this native exists to serve.
    cn1InitStringTwin();
    JAVA_BOOLEAN latin1 = cn1StrIsLatin1(__cn1ThisObject);
    int esz = latin1 ? (int)sizeof(JAVA_ARRAY_BYTE) : (int)sizeof(JAVA_ARRAY_CHAR);

    enteringNativeAllocations();
    int fieldsEnd = (int)((sizeof(struct obj__java_lang_String) + 7) & ~(size_t)7);
    int total = fieldsEnd + n * esz;
    JAVA_OBJECT so = cn1AllocFused(threadStateData, total,
            latin1 ? &class__java_lang_String_i8 : &class__java_lang_String_i16);
    if(so == JAVA_NULL) {
        finishedNativeAllocations();
        return JAVA_NULL;   // caller falls back to the two-object path
    }
    // DELIBERATELY NOT RE-ATTRIBUTED TO THE ARRAY CLASS. cn1AllocFused charges the
    // whole block to java.lang.String, which is the truth: a fused substring is ONE
    // allocation, and the characters are inside it. An earlier version of this split
    // the bytes and added a second COUNT for the array class so the census would read
    // like the two-object path -- which silently cancelled the very saving this native
    // exists for, and made the object total look unchanged. The census reports what
    // happened; it is not the place to preserve an old shape.
    struct obj__java_lang_String* rs = (struct obj__java_lang_String*)so;
    rs->java_lang_String_value = JAVA_NULL;   // inline
    rs->java_lang_String_count = n;
    rs->java_lang_String_hashCode = 0;
    CN1_STRING_CLEAR_PEER(rs);
    if(n > 0) {
        // Re-read the parent's characters AFTER the allocation: cn1AllocFused can run a
        // GC handshake. The heap does not move, but re-loading is free and keeps this
        // correct against a future where it does.
        void* dst = (void*)((char*)so + fieldsEnd);
        if(latin1) {
            memcpy((JAVA_ARRAY_BYTE*)dst,
                   ((JAVA_ARRAY_BYTE*)cn1StrChars(__cn1ThisObject)) + off, (size_t)n);
        } else {
            memcpy((JAVA_ARRAY_CHAR*)dst,
                   ((JAVA_ARRAY_CHAR*)cn1StrChars(__cn1ThisObject)) + off, (size_t)n * sizeof(JAVA_ARRAY_CHAR));
        }
    }
    finishedNativeAllocations();
    return so;
}

JAVA_OBJECT java_lang_StringBuilder_toString___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, __cn1ThisObject);
    if(__builtin_expect(!class__java_lang_String.initialized, 0)) __STATIC_INITIALIZER_java_lang_String(threadStateData);
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)__cn1ThisObject;
    int count = t->java_lang_StringBuilder_count;
    enteringNativeAllocations();

    int latin1 = cn1BuilderIsLatin1(__cn1ThisObject);
    if(!latin1) {
        latin1 = 1;
        for(int i = 0; i < count; i++) if(cn1BuilderUnit(__cn1ThisObject, i) > 255) { latin1 = 0; break; }
    }
    if(latin1) {
        JAVA_ARRAY_BYTE* destination;
        JAVA_OBJECT result = cn1FusedLatin1Begin(threadStateData, count, &destination);
        if(result != JAVA_NULL) {
            if(cn1BuilderIsLatin1(__cn1ThisObject)) {
                if(count > 0) memcpy(destination, cn1BuilderData(__cn1ThisObject), (size_t)count);
            } else {
                for(int i = 0; i < count; i++) destination[i] = (JAVA_ARRAY_BYTE)cn1BuilderUnit(__cn1ThisObject, i);
            }
            cn1FusedLatin1End(result, count);
            finishedNativeAllocations();
            return result;
        }
        JAVA_OBJECT fallback = cn1BuilderStringCopy(threadStateData, __cn1ThisObject, latin1);
        finishedNativeAllocations();
        return fallback;
    }

    int off = (int)((sizeof(struct obj__java_lang_String) + 7) & ~(size_t)7);
    int total = off + CN1_FUSED_ARR_BYTES(count, sizeof(JAVA_ARRAY_CHAR));
    // Inline no-zero bump path (init-before-publish, same discipline as the
    // translator's CN1_FAST_NEW_NOZERO sites): every field of the String AND
    // of the fused child header is stored, the data region is fully memcpy'd,
    // and ONLY THEN is the class pointer published -- until that store the
    // parentCls==0 guard keeps a signal-stopped scan from tracing the body.
#ifndef CN1_DISABLE_BIBOP
    JAVA_OBJECT so = cn1BibopFastAllocNoZero(threadStateData, total, &class__java_lang_String, CN1_BIBOP_CIDX(total));
#else
    JAVA_OBJECT so = JAVA_NULL;
#endif
    JAVA_BOOLEAN published = JAVA_FALSE;
    if(so == JAVA_NULL) {
        so = cn1AllocFused(threadStateData, total, &class__java_lang_String); // zeroed, parentCls set
        if(so == JAVA_NULL) {
            JAVA_OBJECT r = cn1BuilderStringCopy(threadStateData, __cn1ThisObject, latin1);
            finishedNativeAllocations();
            return r;
        }
        published = JAVA_TRUE;
    }
    JAVA_OBJECT arr = cn1FusedInstallPrimArray(so, off, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), count);
    struct obj__java_lang_String* rs = (struct obj__java_lang_String*)so;
    rs->java_lang_String_value = arr;
    rs->java_lang_String_count = count;
    rs->java_lang_String_hashCode = 0;
    CN1_STRING_CLEAR_PEER(rs);
    if(count > 0) {
        // re-read the buffer AFTER the allocation (it can run a GC handshake;
        // non-moving heap, but the value field itself is re-loadable for free)
        JAVA_ARRAY_CHAR* src = (JAVA_ARRAY_CHAR*)cn1BuilderData(__cn1ThisObject);
        cn1CharCopy((JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)arr), src, count);
    }
    if(!published) {
        so->__codenameOneParentClsReference = &class__java_lang_String; // PUBLISH
    }
    finishedNativeAllocations();
    return so;
}

JAVA_VOID java_lang_StringBuilder_getChars___int_int_char_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT builder, JAVA_INT start, JAVA_INT end, JAVA_OBJECT destination, JAVA_INT destinationStart) {
    java_lang_StringBuilder_checkRange___int_int(threadStateData, builder, start, end);
    if(destination == JAVA_NULL) THROW_NULL_POINTER_EXCEPTION();
    JAVA_ARRAY dst = (JAVA_ARRAY)destination;
    if(destinationStart < 0 || destinationStart > dst->length - (end - start)) THROW_ARRAY_INDEX_EXCEPTION(destinationStart);
    if(end == start) return;
    void* src = cn1BuilderData(builder);
    JAVA_ARRAY_CHAR* chars = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA(dst) + destinationStart;
    if(cn1BuilderIsLatin1(builder)) {
        for(int i = start; i < end; i++) chars[i - start] = (JAVA_CHAR)((uint8_t*)src)[i];
    } else cn1CharCopy(chars, (JAVA_ARRAY_CHAR*)src + start, end - start);
}

JAVA_OBJECT java_lang_StringBuilder_append___java_lang_Object_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT obj) {
    if(obj == JAVA_NULL) {
        java_lang_StringBuilder_appendNull__(threadStateData, __cn1ThisObject);
        return __cn1ThisObject;
    }
    return java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, __cn1ThisObject, virtual_java_lang_Object_toString___R_java_lang_String(threadStateData, obj));
}

// Native append(int)/append(long): write decimal digits straight into the StringBuilder
// buffer, no temporary String. Digits are generated in negative space so INT/LONG_MIN
// work without overflow.
JAVA_OBJECT java_lang_StringBuilder_append___int_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_INT i) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, __cn1ThisObject);
    enteringNativeAllocations();
    char tmp[12]; int tlen = 0;
    JAVA_BOOLEAN neg = (i < 0);
    JAVA_INT q = i; if(q > 0) q = -q;
    do { tmp[tlen++] = (char)('0' - (q % 10)); q /= 10; } while(q != 0);
    JAVA_INT needed = tlen + (neg ? 1 : 0);
    JAVA_INT count = get_field_java_lang_StringBuilder_count(__cn1ThisObject);
    if(count + needed < 0 || count + needed > get_field_java_lang_StringBuilder_capacity(__cn1ThisObject)) {
        cn1SbEnsure(threadStateData, __cn1ThisObject, count + needed);
    }
    JAVA_INT pos = count;
    if(neg) { cn1BuilderStore(__cn1ThisObject, pos++, '-'); }
    for(int k = tlen - 1; k >= 0; k--) { cn1BuilderStore(__cn1ThisObject, pos++, (JAVA_CHAR)tmp[k]); }
    set_field_java_lang_StringBuilder_count(count + needed, __cn1ThisObject);
    finishedNativeAllocations();
    return __cn1ThisObject;
}

JAVA_OBJECT java_lang_StringBuilder_append___long_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG l) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, __cn1ThisObject);
    enteringNativeAllocations();
    char tmp[21]; int tlen = 0;
    JAVA_BOOLEAN neg = (l < 0);
    JAVA_LONG q = l; if(q > 0) q = -q;
    do { tmp[tlen++] = (char)('0' - (q % 10)); q /= 10; } while(q != 0);
    JAVA_INT needed = tlen + (neg ? 1 : 0);
    JAVA_INT count = get_field_java_lang_StringBuilder_count(__cn1ThisObject);
    if(count + needed < 0 || count + needed > get_field_java_lang_StringBuilder_capacity(__cn1ThisObject)) {
        cn1SbEnsure(threadStateData, __cn1ThisObject, count + needed);
    }
    JAVA_INT pos = count;
    if(neg) { cn1BuilderStore(__cn1ThisObject, pos++, '-'); }
    for(int k = tlen - 1; k >= 0; k--) { cn1BuilderStore(__cn1ThisObject, pos++, (JAVA_CHAR)tmp[k]); }
    set_field_java_lang_StringBuilder_count(count + needed, __cn1ThisObject);
    finishedNativeAllocations();
    return __cn1ThisObject;
}

JAVA_OBJECT java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_CHAR __cn1Arg1) {
    CN1_KEEP_NATIVE_OWNER(bufferOwner, __cn1ThisObject);
    enteringNativeAllocations();
    JAVA_INT len = get_field_java_lang_StringBuilder_count(__cn1ThisObject);
    JAVA_INT valueLen = get_field_java_lang_StringBuilder_capacity(__cn1ThisObject);
    if (len==valueLen) {
        cn1SbEnsure(threadStateData, __cn1ThisObject, len+1);
    }
    if(__cn1Arg1 > 255 && cn1BuilderIsLatin1(__cn1ThisObject)) java_lang_StringBuilder_widen__(threadStateData, __cn1ThisObject);
    cn1BuilderStore(__cn1ThisObject, len, __cn1Arg1);
    set_field_java_lang_StringBuilder_count(len+1, __cn1ThisObject);
    finishedNativeAllocations();
    return __cn1ThisObject;
}

JAVA_VOID java_lang_String_getChars___int_int_char_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_INT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_OBJECT __cn1Arg3, JAVA_INT __cn1Arg4) {
    
    JAVA_INT offset = 0;
    JAVA_ARRAY_CHAR* dst = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)__cn1Arg3);
    if(cn1StrIsLatin1(__cn1ThisObject)) {
        // Latin-1: widen each byte (& 0xff) into the destination char[].
        JAVA_ARRAY_BYTE* src = (JAVA_ARRAY_BYTE*)cn1StrChars(__cn1ThisObject);
        for(JAVA_INT k = 0 ; k < __cn1Arg2 - __cn1Arg1 ; k++) {
            dst[__cn1Arg4 + k] = (JAVA_ARRAY_CHAR)(src[offset + __cn1Arg1 + k] & 0xff);
        }
        return;
    }
    JAVA_ARRAY_CHAR* src = (JAVA_ARRAY_CHAR*)cn1StrChars(__cn1ThisObject);
    // memmove: String and destination can only overlap through VM-internal
    // aliasing tricks, but the safe spelling costs nothing here
    memmove(dst + __cn1Arg4, src + offset + __cn1Arg1, (size_t)(__cn1Arg2 - __cn1Arg1) * sizeof(JAVA_ARRAY_CHAR));
}

// Shared non-ObjC case-conversion body: per-char simple mapping via towupper/
// towlower (identical to Character.toUpperCase/toLowerCase for ASCII and the
// BMP simple mappings). Returns `this` unchanged when no character maps (same
// contract as the JDK). The old #else branch was a stub that ALWAYS returned
// `this` -- silently wrong on every non-ObjC target (Linux, clean builds).
#if !(defined(__APPLE__) && defined(__OBJC__))
#include <wctype.h>
static JAVA_OBJECT cn1StringConvertCase(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me, int toUpper) {
    struct obj__java_lang_String* s = (struct obj__java_lang_String*)me;
    int count = s->java_lang_String_count;
    if(count == 0) return me;
    enteringNativeAllocations();
    // PRESERVE THE SOURCE'S REPRESENTATION WHERE THE RESULT ALLOWS IT. This used to
    // allocate a char[] unconditionally, so toUpperCase/toLowerCase silently turned a
    // compact Latin-1 string into a UTF-16 one and every later reader paid for it.
    //
    // A cased Latin-1 string is USUALLY Latin-1 again but not always -- towupper(0xFF)
    // is 0x178 and towupper(0xB5) is 0x39C -- so a compact source is converted
    // OPTIMISTICALLY into a byte[] and abandons it for the char[] path the moment a
    // converted unit does not fit. That keeps the common case at ONE allocation
    // instead of converting into a char[] and compacting afterwards.
    JAVA_BOOLEAN changed = JAVA_FALSE;
    if(cn1StrIsLatin1(me)) {
        JAVA_OBJECT barr = allocArray(threadStateData, count, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
        JAVA_ARRAY_BYTE* bd = (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA((JAVA_ARRAY)barr);
        JAVA_BOOLEAN fits = JAVA_TRUE;
        for(int i = 0 ; i < count ; i++) {
            JAVA_ARRAY_CHAR c = cn1StrCharAtRaw(me, i);
            JAVA_ARRAY_CHAR m = (JAVA_ARRAY_CHAR)(toUpper ? towupper(c) : towlower(c));
            if(m > 0xFF) { fits = JAVA_FALSE; break; }
            if(m != c) changed = JAVA_TRUE;
            bd[i] = (JAVA_ARRAY_BYTE)m;
        }
        if(fits) {
            if(!changed) {
                finishedNativeAllocations();
                return me;   // barr becomes garbage; the GC reclaims it
            }
            JAVA_OBJECT bs = __NEW_INSTANCE_java_lang_String(threadStateData);
            struct obj__java_lang_String* bo = (struct obj__java_lang_String*)bs;
            bo->java_lang_String_value = barr;
            bo->java_lang_String_count = count;
            finishedNativeAllocations();
            return bs;
        }
        changed = JAVA_FALSE;   // the char[] pass below decides this again from scratch
    }
    JAVA_OBJECT arr = allocArray(threadStateData, count, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1);
    // read the source AFTER the allocation (non-moving GC, `me` rooted by the
    // caller). Coder-aware: cn1StrCharAtRaw decodes Latin-1(byte[]) or UTF-16(char[]).
    JAVA_ARRAY_CHAR* dst = (JAVA_ARRAY_CHAR*)CN1_ARRAY_DATA((JAVA_ARRAY)arr);
    for(int i = 0 ; i < count ; i++) {
        JAVA_ARRAY_CHAR c = cn1StrCharAtRaw(me, i);
        JAVA_ARRAY_CHAR m = (JAVA_ARRAY_CHAR)(toUpper ? towupper(c) : towlower(c));
        if(m != c) changed = JAVA_TRUE;
        dst[i] = m;
    }
    if(!changed) {
        finishedNativeAllocations();
        return me; // arr becomes garbage; the GC reclaims it
    }
    JAVA_OBJECT str = __NEW_INSTANCE_java_lang_String(threadStateData);
    struct obj__java_lang_String* o = (struct obj__java_lang_String*)str;
    o->java_lang_String_value = arr;   // fresh private array: alias, no copy
    o->java_lang_String_count = count;
    finishedNativeAllocations();
    return str;
}
#endif

JAVA_OBJECT java_lang_String_toUpperCase___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
#if defined(__APPLE__) && defined(__OBJC__)
    enteringNativeAllocations();
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString *nsString = [toNSString(CN1_THREAD_STATE_PASS_ARG __cn1ThisObject) uppercaseString];
    JAVA_OBJECT jString = fromNSString(CN1_THREAD_STATE_PASS_ARG nsString);
    [pool release];
    finishedNativeAllocations();
    return jString;
#else
    return cn1StringConvertCase(threadStateData, __cn1ThisObject, 1);
#endif
}

JAVA_OBJECT java_lang_String_toLowerCase___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject) {
#if defined(__APPLE__) && defined(__OBJC__)
    enteringNativeAllocations();
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString *nsString = [toNSString(CN1_THREAD_STATE_PASS_ARG __cn1ThisObject) lowercaseString];
    JAVA_OBJECT jString = fromNSString(CN1_THREAD_STATE_PASS_ARG nsString);
    [pool release];
    finishedNativeAllocations();
    return jString;
#else
    return cn1StringConvertCase(threadStateData, __cn1ThisObject, 0);
#endif
}
