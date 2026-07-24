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
#include <stdint.h>
#include <ctype.h>
#include <assert.h>
#include <errno.h>
#include <time.h>
#include <stdlib.h>
#include <string.h>

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
#endif

extern JAVA_BOOLEAN lowMemoryMode;

// Compact-string: logical char at index i of String s, decoding Latin-1(byte[]) or UTF-16(char[]).
static inline JAVA_CHAR cn1StrCharAtRaw(JAVA_OBJECT s, JAVA_INT i) {
    struct obj__java_lang_String* t = (struct obj__java_lang_String*)s;
    JAVA_ARRAY a = (JAVA_ARRAY)t->java_lang_String_value;
    JAVA_INT o = t->java_lang_String_offset + i;
    if (a->__codenameOneParentClsReference == &class_array1__JAVA_BYTE) {
        return (JAVA_CHAR)(((JAVA_ARRAY_BYTE*)a->data)[o] & 0xff);
    }
    return ((JAVA_ARRAY_CHAR*)a->data)[o];
}
static inline int cn1StrIsLatin1(JAVA_OBJECT s) {
    return ((JAVA_ARRAY)((struct obj__java_lang_String*)s)->java_lang_String_value)->__codenameOneParentClsReference == &class_array1__JAVA_BYTE;
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

JAVA_BOOLEAN java_lang_String_equals___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT __cn1Arg1) {
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

    // Fast path: both backing arrays are char[] -- byte-equality of UTF-16 code
    // units == string equality; libc memcmp is the SIMD-optimized comparison on
    // every target.
    if(!cn1StrIsLatin1(__cn1ThisObject) && !cn1StrIsLatin1(__cn1Arg1)) {
        JAVA_ARRAY_CHAR* oa = ((JAVA_ARRAY_CHAR*)((JAVA_ARRAY)o->java_lang_String_value)->data) + o->java_lang_String_offset;
        JAVA_ARRAY_CHAR* ta = ((JAVA_ARRAY_CHAR*)((JAVA_ARRAY)t->java_lang_String_value)->data) + t->java_lang_String_offset;
        return memcmp(ta, oa, (size_t)t->java_lang_String_count * sizeof(JAVA_ARRAY_CHAR)) == 0 ? JAVA_TRUE : JAVA_FALSE;
    }
    // Coder-aware path: at least one string is Latin-1 (byte[]); compare logical
    // chars. A Latin-1 char equals a char[] code unit exactly when it holds the
    // same text, so this is bit-identical to the all-char[] comparison.
    JAVA_INT count = t->java_lang_String_count;
    for(JAVA_INT i = 0 ; i < count ; i++) {
        if(cn1StrCharAtRaw(__cn1ThisObject, i) != cn1StrCharAtRaw(__cn1Arg1, i)) {
            return JAVA_FALSE;
        }
    }
    return JAVA_TRUE;
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
    // Fast path: both backing arrays are char[] -- find the first differing
    // 4-char block with 64-bit compares, then resolve the exact code unit inside
    // it (UTF-16 code-unit order, like Java).
    if(!cn1StrIsLatin1(__cn1ThisObject) && !cn1StrIsLatin1(__cn1Arg1)) {
        const JAVA_ARRAY_CHAR* ta = ((JAVA_ARRAY_CHAR*)((JAVA_ARRAY)t->java_lang_String_value)->data) + t->java_lang_String_offset;
        const JAVA_ARRAY_CHAR* oa = ((JAVA_ARRAY_CHAR*)((JAVA_ARRAY)o->java_lang_String_value)->data) + o->java_lang_String_offset;
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
        JAVA_ARRAY_CHAR* oa = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)o->java_lang_String_value)->data;
        JAVA_ARRAY_CHAR* ta = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)t->java_lang_String_value)->data;
        JAVA_INT oo = o->java_lang_String_offset;
        JAVA_INT to = t->java_lang_String_offset;

        for(int iter = 0 ; iter < t->java_lang_String_count ; iter++) {
            JAVA_ARRAY_CHAR jo = oa[iter+oo];
            JAVA_ARRAY_CHAR jt = ta[iter+oo];
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
        JAVA_INT end = t->java_lang_String_count + t->java_lang_String_offset;
        JAVA_ARRAY arr = (JAVA_ARRAY)t->java_lang_String_value;
        JAVA_INT i = t->java_lang_String_offset;
        // 4-way polynomial reassociation: h = h*31^4 + c0*31^3 + c1*31^2 + c2*31 + c3.
        // The naive loop is a serially-dependent multiply chain (one 31*h per char);
        // this breaks the dependency so the four products issue in parallel.
        // -fwrapv makes the int overflow wrap exactly like Java's.
        if(arr->__codenameOneParentClsReference == &class_array1__JAVA_BYTE) {
            // Latin-1: (b & 0xff) IS the char value, so the hash is bit-identical
            // to the same text stored as char[]. Mirrors cn1_intrinsics.h.
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
    JAVA_ARRAY_BYTE* sourceData = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)b)->data + off;

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
        JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)destArr)->data;
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
        JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)destArr)->data;
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
            JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)destArr)->data;
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
        JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)destArr)->data;
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
        JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)destArr)->data;
        for (int iter = 0; iter < len; iter++) {
            uint8_t v = (uint8_t)sourceData[iter];
            dest[iter] = (v < 0x80) ? (JAVA_ARRAY_CHAR)v : (JAVA_ARRAY_CHAR)CN1_REPLACEMENT_CHAR;
        }
        finishedNativeAllocations();
        return destArr;
    }

    JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_CHAR(threadStateData, [nsStr length]);
    __block JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)destArr)->data;
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
    JAVA_ARRAY_CHAR* dest = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)destArr)->data;
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
    JAVA_ARRAY_CHAR* arr = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)sourceArr)->data;
    for(int iter = 0 ; iter < sourceArr->length ; iter++) {
        if(arr[iter] > 127) {
            return JAVA_FALSE;
        }
    }
    return JAVA_TRUE;
}

JAVA_OBJECT java_lang_String_charsToBytes___char_1ARRAY_char_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT arr, JAVA_OBJECT encoding) {
    JAVA_ARRAY sourceArr = (JAVA_ARRAY)arr;
    JAVA_ARRAY_CHAR* src = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)sourceArr)->data;
    int srcLen = sourceArr->length;

    JAVA_ARRAY_CHAR* encChars = NULL;
    int encLen = 0;
    if (encoding != JAVA_NULL) {
        encChars = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)encoding)->data;
        encLen = ((JAVA_ARRAY)encoding)->length;
    }
    cn1_encoding_t enc = cn1_resolve_encoding_from_chars(encChars, encLen);

    // ASCII fast path: every char < 0x80 maps to itself as a single byte. Both
    // UTF-8 and ISO-8859-1 agree on this byte sequence, so we can take this
    // shortcut without checking the requested encoding.
    if (isAsciiArray(sourceArr)) {
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, srcLen);
        JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)destArr)->data;
        for (int iter = 0; iter < srcLen; iter++) {
            dest[iter] = (JAVA_ARRAY_BYTE)src[iter];
        }
        return destArr;
    }

    if (enc == CN1_ENC_UTF8 || enc == CN1_ENC_UNKNOWN) {
        size_t outLen = cn1_utf8_encode_chars(src, (size_t)srcLen, NULL);
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, (JAVA_INT)outLen);
        JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)destArr)->data;
        cn1_utf8_encode_chars(src, (size_t)srcLen, dest);
        return destArr;
    }

    if (enc == CN1_ENC_US_ASCII || enc == CN1_ENC_ISO_8859_1) {
        // 1:1 truncation; chars outside the encoding's range become '?',
        // matching the JDK encoder's REPLACE default.
        JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, srcLen);
        JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)destArr)->data;
        unsigned int max = (enc == CN1_ENC_US_ASCII) ? 0x80u : 0x100u;
        for (int iter = 0; iter < srcLen; iter++) {
            unsigned int c = (unsigned int)src[iter];
            dest[iter] = (c < max) ? (JAVA_ARRAY_BYTE)c : (JAVA_ARRAY_BYTE)'?';
        }
        return destArr;
    }

#if defined(__APPLE__) && defined(__OBJC__)
    // UTF-16, ISO-8859-2 etc. -- defer to NSString for the unusual encodings.
    NSStringEncoding nsEnc = cn1_nsencoding_for(enc);
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSString* nsStr = [[NSString alloc] initWithCharacters:sourceArr->data length:srcLen];
    NSData* data = [nsStr dataUsingEncoding:nsEnc allowLossyConversion:YES];
    if (data == nil) {
        data = [nsStr dataUsingEncoding:NSUTF8StringEncoding allowLossyConversion:YES];
    }
    JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, [data length]);
    JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)destArr)->data;
    [data getBytes:dest length:[data length]];
    [nsStr release];
    [pool release];
    return destArr;
#else
    // POSIX/test build: encode the remaining rare cases as UTF-8 so the
    // fallback at least round-trips a Unicode payload cleanly.
    size_t outLen = cn1_utf8_encode_chars(src, (size_t)srcLen, NULL);
    JAVA_OBJECT destArr = __NEW_ARRAY_JAVA_BYTE(threadStateData, (JAVA_INT)outLen);
    JAVA_ARRAY_BYTE* dest = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)destArr)->data;
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
#if defined(__APPLE__) && defined(__OBJC__)
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    JAVA_ARRAY a = (JAVA_ARRAY)b;
    JAVA_ARRAY_BYTE* arr = (JAVA_ARRAY_BYTE*)(*a).data;
    NSData * data = [NSData dataWithBytes:arr length:len];
    NSString* str = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    
    // otherwise this produces a security warning in the compiler
    NSLog(@"%@", str);
    
    // if we disable arc we will need to re-enable these
    [str release];
    [pool release];
#else
    JAVA_ARRAY a = (JAVA_ARRAY)b;
    JAVA_ARRAY_BYTE* arr = (JAVA_ARRAY_BYTE*)(*a).data;
    // Just print to stdout
    for(int i=0; i<len; i++) putchar(arr[off+i]);
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
    // SATB deletion barrier: an object arraycopy overwrites dst[dstOffset..+length)
    // with a bulk memmove that bypasses the per-element setter, so preserve those
    // overwritten references for the current mark cycle. No-op (one flag load) off-GC.
    if(__builtin_expect(gcSatbActive, 0) && !cls->primitiveType) {
        JAVA_ARRAY_OBJECT* dstData = (JAVA_ARRAY_OBJECT*)(*dstArr).data;
        for(int i = 0 ; i < length ; i++) {
            JAVA_OBJECT o = dstData[dstOffset + i];
            if(o != JAVA_NULL && !CN1_IS_TAGGED(o)) cn1SatbEnqueue(o);
        }
    }
    /* java.lang.System.arraycopy is contractually overlap-safe (the spec defines
     * it as if copying via a temporary), and callers such as ArrayList.remove
     * shift elements within a single array (overlapping src/dst). memcpy is
     * undefined behaviour on overlap: x86-64's implementation happens to tolerate
     * the downward shift, but AArch64's optimized memcpy corrupts it (observed as
     * heap corruption on the arm64 clean target). memmove is the correct,
     * overlap-safe primitive. */
    memmove( (*dstArr).data + (dstOffset * byteSize), (*srcArr).data  + (srcOffset * byteSize), length * byteSize);
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

JAVA_LONG java_lang_Double_doubleToRawLongBits___double_R_long(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE n1) {
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


JAVA_OBJECT java_lang_Double_toStringImpl___double_boolean_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE d, JAVA_BOOLEAN b) {
    char s[32];
    if ( !b ){
        snprintf(s, 32, "%lf", d);
    } else {
        snprintf(s, 32, "%1.20E", d);
    }
    
    // We need to match the format of Java spec.  That includes:
    // No "+" for positive exponent.
    // No leading zeroes in positive exponents.
    // No trailing zeroes in decimal portion.
    int j=0;
    // Process only the actual formatted length, not the uninitialized buffer tail
    // (see the matching note in the Float variant below): walking the garbage past
    // the snprintf'd string can push `j` past the end of s2[32] and smash the stack.
    int i = (int) strlen(s);
    char s2[32];
    BOOL inside=NO;
    while (i-->0 && j < 30){
        if (inside){
            if (s[i]=='.'){
                s2[j++]='0';
            }
            if (s[i]!='0'){
                inside=NO;
                s2[j++]=s[i];
            }

        } else {
            if (s[i]=='E'){
                inside=YES;
            }
            if (s[i]=='+'){
                // If a positive exponent, we don't need leading zeroes in
                // the exponent
                while (s2[--j]=='0'){

                }
                j++;
                continue;
            }
            s2[j++]=s[i];
        }
    }
    i=0;
    while (j-->0 && i < 31){
        s[i++]=s2[j];
    }
    s[i]='\0';
    if (strcmp(s, "NAN") == 0) {
        s[1] = 'a';
    }
    return newStringFromCString(threadStateData, s);
}

JAVA_OBJECT java_lang_Float_toStringImpl___float_boolean_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT d, JAVA_BOOLEAN b) {
    char s[32];
    if ( !b ){
        snprintf(s, 32, "%f", d);
    } else {
        snprintf(s, 32, "%1.20E", d);
    }
    // We need to match the format of Java spec.  That includes:
    // No "+" for positive exponent.
    // No leading zeroes in positive exponents.
    // No trailing zeroes in decimal portion.
    int j=0;
    // Start the reversal at the actual formatted length, NOT the full 32-byte
    // buffer: the bytes past the snprintf'd string are uninitialized stack, and
    // walking them feeds garbage into the loop below -- each iteration can do up to
    // two `s2[j++]` writes, so a non-'0' tail pushes `j` past the end of s2[32] and
    // smashes the stack (a top-of-loop `j < 32` guard cannot stop a 2-wide write).
    // glibc/musl don't zero this region, so on the Linux clean target this
    // overflowed reliably (formatting a derived font size). Processing only strlen(s)
    // is both safe and what the algorithm always intended.
    int i = (int) strlen(s);
    char s2[32];
    BOOL inside=NO;
    while (i-->0 && j < 30){
        if (inside){
            if (s[i]=='.'){
                s2[j++]='0';
            }
            if (s[i]!='0'){
                inside=NO;
                s2[j++]=s[i];
            }
            
        } else {
            if (s[i]=='E'){
                inside=YES;
            }
            if (s[i]=='+'){
                // If a positive exponent, we don't need leading zeroes in
                // the exponent
                while (s2[--j]=='0'){
                    
                }
                j++;
                continue;
            }
            s2[j++]=s[i];
        }
    }
    i=0;
    while (j-->0 && i < 31){
        s[i++]=s2[j];
    }
    s[i]='\0';
    if (strcmp(s, "NAN") == 0) {
        s[1] = 'a';
    }
    return newStringFromCString(threadStateData, s);
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
#define CN1_SB_PTR(s) ((JAVA_ARRAY_BYTE*)((JAVA_ARRAY)((struct obj__java_lang_String*)(s))->java_lang_String_value)->data + ((struct obj__java_lang_String*)(s))->java_lang_String_offset)
#define CN1_SB_LEN(s) (((struct obj__java_lang_String*)(s))->java_lang_String_count)

static JAVA_OBJECT cn1ConcatFallback(CODENAME_ONE_THREAD_STATE, JAVA_ARRAY_BYTE* const* parts, const int* lens, int n, int total) {
    enteringNativeAllocations();
    JAVA_ARRAY dat = (JAVA_ARRAY)allocArray(threadStateData, total, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*) (*dat).data;
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

JAVA_DOUBLE java_lang_Math_abs___double_R_double(CODENAME_ONE_THREAD_STATE, JAVA_DOUBLE a) {
    if(a < 0) {
        return a * -1;
    }
    return a;
}

JAVA_FLOAT java_lang_Math_abs___float_R_float(CODENAME_ONE_THREAD_STATE, JAVA_FLOAT a) {
    if(a < 0) {
        return a * -1;
    }
    return a;
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

JAVA_BOOLEAN isClassNameEqual(const char * clsName, JAVA_ARRAY_CHAR* chrs, int length) {
    for(int i = 0 ; i < length ; i++) {
        if(clsName[i] != chrs[i]) return JAVA_FALSE;
    }
    return JAVA_TRUE;
}

JAVA_OBJECT java_lang_Class_forNameImpl___java_lang_String_R_java_lang_Class(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT className) {
    int length = java_lang_String_length___R_int(threadStateData, className);
    JAVA_ARRAY arrayData = (JAVA_ARRAY)java_lang_String_toCharNoCopy___R_char_1ARRAY(threadStateData, className);
    JAVA_ARRAY_CHAR* chrs = arrayData->data;
    
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

JAVA_BOOLEAN java_lang_Class_isAssignableFrom___java_lang_Class_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls, JAVA_OBJECT cls2) {
    struct clazz* clz1 = (struct clazz*)cls;
    struct clazz* clz2 = (struct clazz*)cls2;
    return instanceofFunction(clz1->classId, clz2->classId);
}

JAVA_BOOLEAN java_lang_Class_isInstance___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT cls, JAVA_OBJECT obj) {
    if(obj == JAVA_NULL) { return JAVA_FALSE; }
    struct clazz* clz1 = (struct clazz*)cls;
    struct clazz* clz2 = (struct clazz*)CN1_CLASS_OF(obj); // tag-aware: a tagged Integer has no header
    return instanceofFunction(clz2->classId, clz1->classId);
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
    // A tagged Integer has no object header to read; its class is always Integer.
    if(CN1_IS_TAGGED(obj)) {
        class__java_lang_Integer.__codenameOneParentClsReference = &ClazzClazz;
        return (JAVA_OBJECT)(&class__java_lang_Integer);
    }
#endif
    if(!obj->__codenameOneParentClsReference) {
        return (JAVA_OBJECT)(&ClazzClazz);
    }
    obj->__codenameOneParentClsReference->__codenameOneParentClsReference = &ClazzClazz;
    return (JAVA_OBJECT)obj->__codenameOneParentClsReference;
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
struct ThreadLocalData* getThreadLocalData() {
    if(threadIdKey == 0) {
        pthread_key_create(&threadIdKey, NULL);
    }
    struct ThreadLocalData* i = pthread_getspecific(threadIdKey);
    if(i == NULL) {
        JAVA_LONG nativeThreadId = threadKeyCounter;
        threadKeyCounter++;
        i = malloc(sizeof(struct ThreadLocalData));
        i->threadId = nativeThreadId;
        i->tryBlockOffset = 0;
        
        i->lightweightThread = JAVA_FALSE;
        i->threadBlockedByGC = JAVA_FALSE;
        i->threadActive = JAVA_FALSE;
        i->threadKilled = JAVA_FALSE;
        i->interrupted = JAVA_FALSE;
        
        i->currentThreadObject = 0;
        
        i->utf8Buffer = 0;
        i->utf8BufferSize = 0;
        i->threadObjectStack = malloc(CN1_MAX_OBJECT_STACK_DEPTH * sizeof(struct elementStruct));
        memset(i->threadObjectStack, 0, CN1_MAX_OBJECT_STACK_DEPTH * sizeof(struct elementStruct));
        i->threadObjectStackOffset = 0;
        
        i->callStackClass = malloc(CN1_MAX_STACK_CALL_DEPTH * sizeof(int));
        memset(i->callStackClass, 0, CN1_MAX_STACK_CALL_DEPTH * sizeof(int));
        
        i->callStackLine = malloc(CN1_MAX_STACK_CALL_DEPTH * sizeof(int));
        memset(i->callStackLine, 0, CN1_MAX_STACK_CALL_DEPTH * sizeof(int));
        
        i->callStackMethod = malloc(CN1_MAX_STACK_CALL_DEPTH * sizeof(int));
        memset(i->callStackMethod, 0, CN1_MAX_STACK_CALL_DEPTH * sizeof(int));

#ifdef CN1_ON_DEVICE_DEBUG
        i->callStackLocalsAddresses = malloc(CN1_MAX_STACK_CALL_DEPTH * sizeof(void**));
        memset(i->callStackLocalsAddresses, 0, CN1_MAX_STACK_CALL_DEPTH * sizeof(void**));
        i->callStackFrameInfo = malloc(CN1_MAX_STACK_CALL_DEPTH * sizeof(struct cn1_frame_info*));
        memset(i->callStackFrameInfo, 0, CN1_MAX_STACK_CALL_DEPTH * sizeof(struct cn1_frame_info*));
#endif

        i->callStackOffset = 0;

        // ThreadLocalData is malloc'd (not zeroed); 0 means "frameless native-stack
        // limit not yet computed" -- it is filled in lazily on first frameless entry.
        i->nativeStackLimit = 0;

        i->pendingHeapAllocations = malloc(PER_THREAD_ALLOCATION_COUNT * sizeof(void *));
        memset(i->pendingHeapAllocations, 0, PER_THREAD_ALLOCATION_COUNT * sizeof(void *));
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
        for(int __bi = 0 ; __bi < CN1_BIBOP_NUM_CLASSES ; __bi++) {
#ifndef CN1_DISABLE_BIBOP
            i->bibopBypassSeen[__bi] = atomic_load_explicit(&bibopBypassGeneration[__bi],
                                                            memory_order_relaxed);
#else
            i->bibopBypassSeen[__bi] = 0;
#endif
            i->bibopBypassRemaining[__bi] = 0;
        }
        i->nativeAllocationMode = JAVA_FALSE;
        // dead-thread pending-migration queue state (single-writer allObjectsInHeap)
        i->gcDeadNext = 0;
        i->gcQueuedForDrain = JAVA_FALSE;
        i->gcReleaseRequested = JAVA_FALSE;
        
        i->blocks = malloc(500 * sizeof(struct TryBlock));
#ifdef CN1_CONSERVATIVE_GC_ROOTS
        // PHASE 3b: record this thread's pthread handle + TLS self pointer so the GC can
        // signal-stop it and the async-signal-safe stop handler can find its state.
        i->gcPthread = pthread_self();
        i->gcPthreadValid = JAVA_TRUE;
        i->gcParkCaptured = JAVA_FALSE;
        i->gcStackPointerAtPark = 0;
        i->gcSigStopRequest = 0;
        i->gcSigStopped = 0;
        i->gcSigRelease = 0;
        i->gcSigStopGen = 0;
        i->gcSigStackPointer = 0;
        i->gcSigRegsLen = 0;
        cn1TlsSelf = i;
#endif
        pthread_setspecific(threadIdKey, i);
        
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
        CODENAME_ONE_ASSERT(threadOffset > -1);
        allThreads[threadOffset] = i;
        unlockCriticalSection();
        //printf("Thread slot %d assigned to thread %d\n",threadOffset,(int)i->threadId);
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
JAVA_VOID java_lang_System_gcMarkSweep__(CODENAME_ONE_THREAD_STATE) {
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
    int __gcSavedTryBlock = threadStateData->tryBlockOffset;
    jmp_buf __gcTryJmp;
    if(CN1_TRY_SETJMP(__gcTryJmp) == 0) {
        threadStateData->blocks[threadStateData->tryBlockOffset].monitor = 0;
        threadStateData->blocks[threadStateData->tryBlockOffset].exceptionClass = 0; // catch-all
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
    gcCount++;
    if(gcCount==1 || (gcCount % 20)==0) fprintf(stderr,"[GC-INSTR] cycles=%d allocs=%lld heapTableSize=%d markMs=%.0f sweepMs=%.0f\n",
        gcCount, cn1_instr_allocCount, currentSizeOfAllObjectsInHeap, markNs/1e6, sweepNs/1e6);
#else
    codenameOneGCMark();
    codenameOneGCSweep();
#endif
        threadStateData->tryBlockOffset = __gcSavedTryBlock;
    } else {
        threadStateData->tryBlockOffset = __gcSavedTryBlock;
        threadStateData->exception = JAVA_NULL;
    }
    flushReleaseQueue();
    lowMemoryMode = JAVA_FALSE;
    gcCurrentlyRunning = JAVA_FALSE;
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
    // tagged range -- legal and deterministic. Monitors created for tagged
    // values are never reclaimed (there is no object death to trigger removal);
    // they are bounded by the number of DISTINCT integer values a program ever
    // synchronizes on, which is negligible in practice. The only header-touching
    // step, cn1BibopNoteMonitorAttached, skips tagged values internally.
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
        threadStateData->threadActive = JAVA_FALSE;
        err = pthread_mutex_lock(&data->__codenameOneMutex);
        data->counter++;
        data->ownerThread = own;
        while (threadStateData->threadBlockedByGC) {
            usleep(100);
        }
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

// ---- Tagged small-integer support (poor man's Valhalla, 64-bit pointers only) ----
// Integer.valueOf returns an immediate tagged pointer instead of allocating; cn1Value
// recovers the int from either a tagged immediate or a heap Integer's field. When the
// optimization is off (or on a 32-bit-pointer target) these behave exactly as before:
// valueOf delegates to the cached heap path and cn1Value just reads the field.
#if CN1_TAGGED_ACTIVE
extern void __STATIC_INITIALIZER_java_lang_Integer(CODENAME_ONE_THREAD_STATE);
#endif
JAVA_OBJECT java_lang_Integer_valueOf___int_R_java_lang_Integer(CODENAME_ONE_THREAD_STATE, JAVA_INT i) {
#if CN1_TAGGED_ACTIVE
    // Tagged ints never allocate, so nothing else triggers Integer's class init -- but
    // dispatching hashCode/equals on a tagged int reads class__java_lang_Integer.vtable,
    // which the static initializer populates. Force it once. The guard keeps the hot path
    // a single predictable branch (the initializer itself is also idempotent).
    static volatile int cn1IntInit = 0;
    if(!cn1IntInit) { __STATIC_INITIALIZER_java_lang_Integer(threadStateData); cn1IntInit = 1; }
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
        ts.tv_sec = tv.tv_sec + (long)(timeout / 1000);
        ts.tv_nsec = tv.tv_usec * 1000 + (timeout % 1000) * 1000000 + nanos;
        if ( ts.tv_nsec > 1000000000 ){
            ts.tv_nsec -= 1000000000;
            ts.tv_sec++;
        }
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

void cn1ReleaseThreadLocalData(struct ThreadLocalData *head) {
    free(head->blocks);
    free(head->threadObjectStack);
    free(head->callStackClass);
    free(head->callStackLine);
    free(head->callStackMethod);
#ifdef CN1_ON_DEVICE_DEBUG
    free(head->callStackLocalsAddresses);
    free(head->callStackFrameInfo);
#endif
    free(head->pendingHeapAllocations);
    free(head);
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

// Monotonic microsecond clock for the sleep deadline (wall clock on the MSVC /
// clang-cl target, which lacks clock_gettime -- same fallback as nanoTime above).
static JAVA_LONG cn1SleepNowMicros(void) {
#ifdef _WIN32
    struct timeval time;
    gettimeofday(&time, NULL);
    return (((JAVA_LONG)time.tv_sec) * 1000000LL) + (JAVA_LONG)time.tv_usec;
#else
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (((JAVA_LONG)ts.tv_sec) * 1000000LL) + (JAVA_LONG)(ts.tv_nsec / 1000);
#endif
}

JAVA_VOID java_lang_Thread_sleep___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG millis) {
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
    // which silently turned long sleeps into no-ops on those libcs. Honors the
    // interrupt flag so interrupt() now actually shortens a sleep instead of
    // relying on an accidental EINTR.
    JAVA_LONG deadline = cn1SleepNowMicros() + millis * 1000;
    for(;;) {
        if(threadStateData->interrupted) {
            break;
        }
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
    while(threadStateData->threadBlockedByGC) {
        usleep(1000);
    }
    threadStateData->threadActive = JAVA_TRUE;
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
    pthread_attr_setstacksize(&attr, 16 * 1024 * 1024);
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
    JAVA_ARRAY_CHAR* chrs = arrayData->data;
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
    threadStateData->callStackOffset++;
}


void releaseForReturnInException(CODENAME_ONE_THREAD_STATE, int cn1LocalsBeginInThread, int methodBlockOffset) {
    threadStateData->tryBlockOffset = methodBlockOffset;
    threadStateData->threadObjectStackOffset = cn1LocalsBeginInThread;
    threadStateData->callStackOffset--;
}

JAVA_LONG java_lang_Runtime_totalMemoryImpl___R_long(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) && defined(__OBJC__)
    return [NSProcessInfo processInfo].physicalMemory;
#else
    // TODO: implement for other platforms
    return 1024*1024*1024;
#endif
}

JAVA_LONG java_lang_Runtime_freeMemoryImpl___R_long(CODENAME_ONE_THREAD_STATE) {
#if defined(__APPLE__) && defined(__OBJC__)
    struct task_basic_info info;
    mach_msg_type_number_t size = sizeof(info);
    kern_return_t kerr = task_info(mach_task_self(),
                                   TASK_BASIC_INFO,
                                   (task_info_t)&info,
                                   &size);
    return [NSProcessInfo processInfo].physicalMemory - info.resident_size;
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
// HashMap.cn1Marker exactly)
static inline JAVA_INT cn1HmMarker(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT key) {
    if(key == JAVA_NULL) {
        return (JAVA_INT)0x80000000;
    }
    JAVA_INT h = virtual_java_lang_Object_hashCode___R_int(threadStateData, key);
    h ^= (JAVA_INT)(((uint32_t)h) >> 16);
    return h | (JAVA_INT)0x80000000;
}

// probe; >=0 found slot, else -(insertionPoint+1) (first tombstone on the path
// if any, else the terminating empty slot). Must match cn1FindSlotImpl.
static JAVA_INT cn1HmFindSlot(CODENAME_ONE_THREAD_STATE, struct obj__java_util_HashMap* t, JAVA_OBJECT key, JAVA_INT marker) {
    JAVA_ARRAY metaArr = (JAVA_ARRAY)t->java_util_HashMap_cn1Meta;
    JAVA_ARRAY_INT* meta = (JAVA_ARRAY_INT*)metaArr->data;
    JAVA_ARRAY_OBJECT* keys = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)t->java_util_HashMap_cn1Keys)->data;
    int mask = metaArr->length - 1;
    int i = marker & mask;
    int firstTomb = -1;
    while(1) {
        JAVA_INT m = meta[i];
        if(m == 0) { // META_EMPTY
            return -((firstTomb >= 0 ? firstTomb : i) + 1);
        }
        if(m == marker) {
            JAVA_OBJECT k = keys[i];
            if(key == JAVA_NULL ? k == JAVA_NULL
                    : (k == key || java_util_HashMap_areEqualKeys___java_lang_Object_java_lang_Object_R_boolean(threadStateData, key, k))) {
                return i;
            }
        } else if(m == 1 && firstTomb < 0) { // META_TOMB
            firstTomb = i;
        }
        i = (i + 1) & mask;
    }
}

JAVA_OBJECT java_util_HashMap_get___java_lang_Object_R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT key) {
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    JAVA_INT idx = cn1HmFindSlot(threadStateData, t, key, cn1HmMarker(threadStateData, key));
    if(idx < 0) {
        return JAVA_NULL;
    }
    return ((JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)t->java_util_HashMap_cn1Vals)->data)[idx];
}

JAVA_BOOLEAN java_util_HashMap_containsKey___java_lang_Object_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT key) {
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    return cn1HmFindSlot(threadStateData, t, key, cn1HmMarker(threadStateData, key)) >= 0 ? JAVA_TRUE : JAVA_FALSE;
}

extern JAVA_VOID java_util_HashMap_cn1Grow__(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT);

JAVA_OBJECT java_util_HashMap_put___java_lang_Object_java_lang_Object_R_java_lang_Object(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT key, JAVA_OBJECT value) {
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    JAVA_INT marker = cn1HmMarker(threadStateData, key);
    JAVA_INT idx = cn1HmFindSlot(threadStateData, t, key, marker);
    JAVA_OBJECT valsObj = t->java_util_HashMap_cn1Vals;
    JAVA_ARRAY_OBJECT* vals = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)valsObj)->data;
    if(idx >= 0) {
        JAVA_OBJECT old = vals[idx];
        CN1_WRITE_BARRIER(valsObj, value);
        CN1_SATB_DELETE(&vals[idx]); // preserve the overwritten value for this mark cycle
        vals[idx] = value;
        return old;
    }
    JAVA_INT ins = -idx - 1;
    JAVA_ARRAY_INT* meta = (JAVA_ARRAY_INT*)((JAVA_ARRAY)t->java_util_HashMap_cn1Meta)->data;
    JAVA_OBJECT keysObj = t->java_util_HashMap_cn1Keys;
    JAVA_ARRAY_OBJECT* keys = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)keysObj)->data;
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
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    JAVA_INT idx = cn1HmFindSlot(threadStateData, t, key, cn1HmMarker(threadStateData, key));
    if(idx < 0) {
        return JAVA_NULL;
    }
    JAVA_ARRAY_INT* meta = (JAVA_ARRAY_INT*)((JAVA_ARRAY)t->java_util_HashMap_cn1Meta)->data;
    JAVA_ARRAY_OBJECT* keys = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)t->java_util_HashMap_cn1Keys)->data;
    JAVA_ARRAY_OBJECT* vals = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)t->java_util_HashMap_cn1Vals)->data;
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
    struct obj__java_util_HashMap* t = (struct obj__java_util_HashMap*)__cn1ThisObject;
    if(t->java_util_HashMap_elementCount > 0 || t->java_util_HashMap_cn1Occupied > 0) {
        JAVA_ARRAY metaArr = (JAVA_ARRAY)t->java_util_HashMap_cn1Meta;
        int len = metaArr->length;
        // SATB deletion barrier: the memsets below bulk-null every live key/value ref,
        // so preserve them for the current mark cycle first. No-op (one flag load) off-GC.
        if(__builtin_expect(gcSatbActive, 0)) {
            JAVA_ARRAY_OBJECT* ck = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)t->java_util_HashMap_cn1Keys)->data;
            JAVA_ARRAY_OBJECT* cv = (JAVA_ARRAY_OBJECT*)((JAVA_ARRAY)t->java_util_HashMap_cn1Vals)->data;
            for(int i = 0 ; i < len ; i++) {
                JAVA_OBJECT k = ck[i]; if(k != JAVA_NULL && !CN1_IS_TAGGED(k)) cn1SatbEnqueue(k);
                JAVA_OBJECT v = cv[i]; if(v != JAVA_NULL && !CN1_IS_TAGGED(v)) cn1SatbEnqueue(v);
            }
        }
        memset(metaArr->data, 0, (size_t)len * sizeof(JAVA_ARRAY_INT));
        memset(((JAVA_ARRAY)t->java_util_HashMap_cn1Keys)->data, 0, (size_t)len * sizeof(JAVA_ARRAY_OBJECT));
        memset(((JAVA_ARRAY)t->java_util_HashMap_cn1Vals)->data, 0, (size_t)len * sizeof(JAVA_ARRAY_OBJECT));
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
    JAVA_ARRAY arr = (JAVA_ARRAY)(encString->java_lang_String_value);
    int off = get_field_java_lang_String_offset(__cn1ThisObject);
    int count = encString->java_lang_String_count;
    if(arr->__codenameOneParentClsReference == &class_array1__JAVA_BYTE) {
        // Latin-1: (b & 0xff) IS the char value; matches char[] scan bit-identically.
        JAVA_ARRAY_BYTE* encArr = (JAVA_ARRAY_BYTE*)arr->data;
        int endOff = off+count;
        for (int i=off+fromIndex; i<endOff; i++) {
            if ((encArr[i] & 0xff) == ch) {
                return i-off;
            }
        }
        return -1;
    }
    JAVA_ARRAY_CHAR* encArr = (JAVA_ARRAY_CHAR*)arr->data;
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

JAVA_CHAR java_lang_StringBuilder_charAt___int_R_char(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_INT index) {
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)__cn1ThisObject;
    // bound by count (the JDK contract), NOT capacity: the lax capacity bound
    // let buggy code read chars beyond length(), and it forced the stack-
    // allocated builder path to zero its whole buffer just so such reads see
    // the '\0' a fresh heap array would have held
    if(index < 0 || index >= t->java_lang_StringBuilder_count) { THROW_ARRAY_INDEX_EXCEPTION(index); }
    JAVA_ARRAY_CHAR* dat = ((JAVA_ARRAY)t->java_lang_StringBuilder_value)->data;
    return dat[index];
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

JAVA_OBJECT java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_OBJECT str) {
    enteringNativeAllocations();
    if (str == JAVA_NULL) {
        java_lang_StringBuilder_appendNull__(threadStateData, __cn1ThisObject);
        finishedNativeAllocations();
        return __cn1ThisObject;
    }
    struct obj__java_lang_String* s = (struct obj__java_lang_String*)str;
    int length = s->java_lang_String_count;
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)__cn1ThisObject;
    int newCount = t->java_lang_StringBuilder_count + length;
    if (newCount > ((JAVA_ARRAY)t->java_lang_StringBuilder_value)->length) {
        java_lang_StringBuilder_enlargeBuffer___int(threadStateData, __cn1ThisObject, newCount);
    }
    JAVA_ARRAY srcArr = (JAVA_ARRAY)s->java_lang_String_value;
    JAVA_ARRAY_CHAR* dst = ((JAVA_ARRAY_CHAR*)((JAVA_ARRAY)t->java_lang_StringBuilder_value)->data) + t->java_lang_StringBuilder_count;
    if(srcArr->__codenameOneParentClsReference == &class_array1__JAVA_BYTE) {
        // Latin-1 source string: widen each byte (& 0xff) into the char[] buffer.
        JAVA_ARRAY_BYTE* src = ((JAVA_ARRAY_BYTE*)srcArr->data) + s->java_lang_String_offset;
        for(int k = 0 ; k < length ; k++) {
            dst[k] = (JAVA_ARRAY_CHAR)(src[k] & 0xff);
        }
    } else {
        JAVA_ARRAY_CHAR* src = ((JAVA_ARRAY_CHAR*)srcArr->data) + s->java_lang_String_offset;
        cn1CharCopy(dst, src, length);
    }
    t->java_lang_StringBuilder_count = newCount;
    finishedNativeAllocations();
    return __cn1ThisObject;

}

// Native toString: the result String is ONE fused block (object + char[] child)
// filled with a single memcpy -- no Java ctor chain, no System.arraycopy, no
// separate array allocation. Falls back to the pure-Java twin when the fused
// allocator declines (oversize for BiBOP).
JAVA_OBJECT java_lang_StringBuilder_toString___R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    if(__builtin_expect(!class__java_lang_String.initialized, 0)) __STATIC_INITIALIZER_java_lang_String(threadStateData);
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)__cn1ThisObject;
    int count = t->java_lang_StringBuilder_count;
    enteringNativeAllocations();
    int off = (int)((sizeof(struct obj__java_lang_String) + 7) & ~(size_t)7);
    int total = off + CN1_FUSED_ARR_BYTES(count, sizeof(JAVA_ARRAY_CHAR));
    // Inline no-zero bump path (init-before-publish, same discipline as the
    // translator's CN1_FAST_NEW_NOZERO sites): every field of the String AND
    // of the fused child header is stored, the data region is fully memcpy'd,
    // and ONLY THEN is the class pointer published -- until that store the
    // parentCls==0 guard keeps a signal-stopped scan from tracing the body.
#if !defined(CN1_DISABLE_BIBOP) && !defined(DEBUG_GC_OBJECTS_IN_HEAP)
    JAVA_OBJECT so = cn1BibopFastAllocNoZero(threadStateData, total, &class__java_lang_String, CN1_BIBOP_CIDX(total));
#else
    JAVA_OBJECT so = JAVA_NULL;
#endif
    JAVA_BOOLEAN published = JAVA_FALSE;
    if(so == JAVA_NULL) {
        so = cn1AllocFused(threadStateData, total, &class__java_lang_String); // zeroed, parentCls set
        if(so == JAVA_NULL) {
            JAVA_OBJECT r = java_lang_StringBuilder_toStringImpl___R_java_lang_String(threadStateData, __cn1ThisObject);
            finishedNativeAllocations();
            return r;
        }
        published = JAVA_TRUE;
    }
    JAVA_OBJECT arr = cn1FusedInstallPrimArray(so, off, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), count);
    struct obj__java_lang_String* rs = (struct obj__java_lang_String*)so;
    rs->java_lang_String_value = arr;
    rs->java_lang_String_offset = 0;
    rs->java_lang_String_count = count;
    rs->java_lang_String_hashCode = 0;
    rs->java_lang_String_nsString = 0;
    if(count > 0) {
        // re-read the buffer AFTER the allocation (it can run a GC handshake;
        // non-moving heap, but the value field itself is re-loadable for free)
        JAVA_ARRAY_CHAR* src = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)t->java_lang_StringBuilder_value)->data;
        cn1CharCopy((JAVA_ARRAY_CHAR*)((JAVA_ARRAY)arr)->data, src, count);
    }
    if(!published) {
        so->__codenameOneParentClsReference = &class__java_lang_String; // PUBLISH
    }
    finishedNativeAllocations();
    return so;
}

JAVA_VOID java_lang_StringBuilder_getChars___int_int_char_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_INT start, JAVA_INT end, JAVA_OBJECT dst, JAVA_INT dstStart) {
    struct obj__java_lang_StringBuilder* t = (struct obj__java_lang_StringBuilder*)__cn1ThisObject;
    // JDK contract: srcEnd bounded by length(), not capacity. Also keeps a
    // lax caller from reading a stack-allocated builder's unzeroed tail.
    if(start < 0 || start > end || end > t->java_lang_StringBuilder_count) {
        THROW_ARRAY_INDEX_EXCEPTION(end);
    }
    java_lang_System_arraycopy___java_lang_Object_int_java_lang_Object_int_int(threadStateData, t->java_lang_StringBuilder_value, start, dst, dstStart, end - start);
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
    enteringNativeAllocations();
    char tmp[12]; int tlen = 0;
    JAVA_BOOLEAN neg = (i < 0);
    JAVA_INT q = i; if(q > 0) q = -q;
    do { tmp[tlen++] = (char)('0' - (q % 10)); q /= 10; } while(q != 0);
    JAVA_INT needed = tlen + (neg ? 1 : 0);
    JAVA_INT count = get_field_java_lang_StringBuilder_count(__cn1ThisObject);
    JAVA_OBJECT value = get_field_java_lang_StringBuilder_value(__cn1ThisObject);
    if(count + needed > ((JAVA_ARRAY)value)->length) {
        java_lang_StringBuilder_enlargeBuffer___int(threadStateData, __cn1ThisObject, count + needed);
        value = get_field_java_lang_StringBuilder_value(__cn1ThisObject);
    }
    JAVA_ARRAY_CHAR* d = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)value)->data;
    JAVA_INT pos = count;
    if(neg) { d[pos++] = '-'; }
    for(int k = tlen - 1; k >= 0; k--) { d[pos++] = tmp[k]; }
    set_field_java_lang_StringBuilder_count(count + needed, __cn1ThisObject);
    finishedNativeAllocations();
    return __cn1ThisObject;
}

JAVA_OBJECT java_lang_StringBuilder_append___long_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_LONG l) {
    enteringNativeAllocations();
    char tmp[21]; int tlen = 0;
    JAVA_BOOLEAN neg = (l < 0);
    JAVA_LONG q = l; if(q > 0) q = -q;
    do { tmp[tlen++] = (char)('0' - (q % 10)); q /= 10; } while(q != 0);
    JAVA_INT needed = tlen + (neg ? 1 : 0);
    JAVA_INT count = get_field_java_lang_StringBuilder_count(__cn1ThisObject);
    JAVA_OBJECT value = get_field_java_lang_StringBuilder_value(__cn1ThisObject);
    if(count + needed > ((JAVA_ARRAY)value)->length) {
        java_lang_StringBuilder_enlargeBuffer___int(threadStateData, __cn1ThisObject, count + needed);
        value = get_field_java_lang_StringBuilder_value(__cn1ThisObject);
    }
    JAVA_ARRAY_CHAR* d = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)value)->data;
    JAVA_INT pos = count;
    if(neg) { d[pos++] = '-'; }
    for(int k = tlen - 1; k >= 0; k--) { d[pos++] = tmp[k]; }
    set_field_java_lang_StringBuilder_count(count + needed, __cn1ThisObject);
    finishedNativeAllocations();
    return __cn1ThisObject;
}

JAVA_OBJECT java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_CHAR __cn1Arg1) {
    enteringNativeAllocations();
    JAVA_INT len = get_field_java_lang_StringBuilder_count(__cn1ThisObject);
    JAVA_OBJECT value = get_field_java_lang_StringBuilder_value(__cn1ThisObject);
    JAVA_INT valueLen = ((JAVA_ARRAY)value)->length;
    if (len==valueLen) {
        java_lang_StringBuilder_enlargeBuffer___int(threadStateData, __cn1ThisObject, len+1);
        value = get_field_java_lang_StringBuilder_value(__cn1ThisObject);
    }
    JAVA_ARRAY_CHAR* d = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)value)->data;
    d[len] = __cn1Arg1;
    set_field_java_lang_StringBuilder_count(len+1, __cn1ThisObject);
    finishedNativeAllocations();
    return __cn1ThisObject;
}

JAVA_VOID java_lang_String_getChars___int_int_char_1ARRAY_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT  __cn1ThisObject, JAVA_INT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_OBJECT __cn1Arg3, JAVA_INT __cn1Arg4) {
    
    JAVA_INT offset = get_field_java_lang_String_offset(__cn1ThisObject);
    JAVA_ARRAY srcArr = (JAVA_ARRAY)get_field_java_lang_String_value(__cn1ThisObject);
    JAVA_ARRAY_CHAR* dst = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)__cn1Arg3)->data;
    if(srcArr->__codenameOneParentClsReference == &class_array1__JAVA_BYTE) {
        // Latin-1: widen each byte (& 0xff) into the destination char[].
        JAVA_ARRAY_BYTE* src = (JAVA_ARRAY_BYTE*)srcArr->data;
        for(JAVA_INT k = 0 ; k < __cn1Arg2 - __cn1Arg1 ; k++) {
            dst[__cn1Arg4 + k] = (JAVA_ARRAY_CHAR)(src[offset + __cn1Arg1 + k] & 0xff);
        }
        return;
    }
    JAVA_ARRAY_CHAR* src = (JAVA_ARRAY_CHAR*)srcArr->data;
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
    JAVA_OBJECT arr = allocArray(threadStateData, count, &class_array1__JAVA_CHAR, sizeof(JAVA_ARRAY_CHAR), 1);
    // read the source AFTER the allocation (non-moving GC, `me` rooted by the
    // caller). Coder-aware: cn1StrCharAtRaw decodes Latin-1(byte[]) or UTF-16(char[]).
    JAVA_ARRAY_CHAR* dst = (JAVA_ARRAY_CHAR*)((JAVA_ARRAY)arr)->data;
    JAVA_BOOLEAN changed = JAVA_FALSE;
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
    o->java_lang_String_offset = 0;
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

JAVA_OBJECT java_lang_String_format___java_lang_String_java_lang_Object_1ARRAY_R_java_lang_String(CODENAME_ONE_THREAD_STATE,  JAVA_OBJECT format, JAVA_OBJECT args) {
#if defined(__APPLE__) && defined(__OBJC__)
    enteringNativeAllocations();
    JAVA_ARRAY argsArray = (JAVA_ARRAY)args;
    JAVA_ARRAY_OBJECT* objs = (JAVA_ARRAY_OBJECT*)argsArray->data;
    int len = argsArray->length;
    NSMutableArray* argsList1 = [NSMutableArray arrayWithCapacity:len];
    for (int i=0; i<len; i++) {
        [argsList1 insertObject: toNSString(CN1_THREAD_STATE_PASS_ARG java_lang_String_valueOf___java_lang_Object_R_java_lang_String(CN1_THREAD_STATE_PASS_ARG objs[i])) atIndex:i];
    }
    char *argList = (char *)malloc(sizeof(NSString *) * len);
    [argsList1 getObjects:(id *)argList];
    NSString* result = [[[NSString alloc] initWithFormat:toNSString(CN1_THREAD_STATE_PASS_ARG format) arguments:argList] autorelease];
    free(argList);
    JAVA_OBJECT out = fromNSString(CN1_THREAD_STATE_PASS_ARG [NSString init]);
    finishedNativeAllocations();
    return out;
#else
    JAVA_OBJECT builder = __NEW_INSTANCE_java_lang_StringBuilder(threadStateData);
    int formatLength = java_lang_String_length___R_int(threadStateData, format);
    JAVA_ARRAY argsArray = (JAVA_ARRAY)args;
    JAVA_ARRAY_OBJECT* values = argsArray == JAVA_NULL ? JAVA_NULL : (JAVA_ARRAY_OBJECT*)argsArray->data;
    int valuesLength = argsArray == JAVA_NULL ? 0 : argsArray->length;
    int argIndex = 0;

    for (int i = 0; i < formatLength; i++) {
        JAVA_CHAR ch = java_lang_String_charAt___int_R_char(threadStateData, format, i);
        if (ch != '%' || i == formatLength - 1) {
            java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(threadStateData, builder, ch);
            continue;
        }

        JAVA_CHAR token = java_lang_String_charAt___int_R_char(threadStateData, format, i + 1);
        i++;
        if (token == '%') {
            java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(threadStateData, builder, '%');
            continue;
        }

        if (argIndex >= valuesLength || values == JAVA_NULL) {
            java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(threadStateData, builder, '%');
            java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(threadStateData, builder, token);
            continue;
        }

        JAVA_OBJECT value = values[argIndex++];
        JAVA_OBJECT valueText = java_lang_String_valueOf___java_lang_Object_R_java_lang_String(threadStateData, value);
        if (token == 'c') {
            int valueTextLength = java_lang_String_length___R_int(threadStateData, valueText);
            if (valueTextLength > 0) {
                JAVA_CHAR out = java_lang_String_charAt___int_R_char(threadStateData, valueText, 0);
                java_lang_StringBuilder_append___char_R_java_lang_StringBuilder(threadStateData, builder, out);
            }
        } else {
            java_lang_StringBuilder_append___java_lang_String_R_java_lang_StringBuilder(threadStateData, builder, valueText);
        }
    }
    return java_lang_StringBuilder_toString___R_java_lang_String(threadStateData, builder);
#endif
}
