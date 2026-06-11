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

/*
 * Native SIMD kernels for the Windows port (com.codename1.impl.windows.WindowsSimd,
 * the x86/ARM analog of the iOS IOSSimd/NEON layer). Each kernel runs a vectorized
 * main loop over the bulk of the range and a scalar tail for the remainder:
 *
 *   - x64   : SSE2 (128-bit) -- universally present on every x86-64 CPU, so no
 *             runtime feature check is needed. Operations SSE2 lacks natively
 *             (int32 multiply / min / max) fall back to the scalar loop on x64;
 *             they are still vectorized on arm64.
 *   - arm64 : NEON (128-bit), which has the full integer + float set.
 *   - other : a portable scalar loop (keeps the file correct under any compiler).
 *
 * Loads/stores are unaligned (_mm_loadu_* / vld1q_*), so WindowsSimd does NOT need
 * the 16-byte aligned allocator the iOS port uses -- a plain new int[] is fine.
 *
 * The bridge functions are ParparVM-mangled instance natives, so the first two
 * parameters are the thread state and the receiver (__cn1ThisObject), followed by
 * the declared arguments. Java arrays cross as JAVA_OBJECT and the element data is
 * reached through ((JAVA_ARRAY)obj)->data, exactly as in IOSSimd.m.
 */

#ifdef _WIN32

#include "cn1_globals.h"
#include <stdint.h>

#if defined(_M_X64) || defined(__x86_64__)
#define CN1_SIMD_X64 1
#include <emmintrin.h>   /* SSE2 */
#elif defined(_M_ARM64) || defined(__aarch64__)
#define CN1_SIMD_ARM64 1
#include <arm_neon.h>
#endif

static JAVA_ARRAY_BYTE cn1SimdSaturateByte(int value) {
    if (value > 127) {
        return (JAVA_ARRAY_BYTE) 127;
    }
    if (value < -128) {
        return (JAVA_ARRAY_BYTE) -128;
    }
    return (JAVA_ARRAY_BYTE) value;
}

/* Whether the chained byte-shuffle codec (Base64) should use the SIMD path. True
 * on arm64 (NEON vld3/vst3 interleave wins); false on x86-64, where SSE2 has no
 * 3-way interleave and /O2 already autovectorizes the scalar codec. A compile-time
 * constant baked into the per-arch native library. */
JAVA_BOOLEAN com_codename1_impl_windows_WindowsSimd_isByteShuffleAccelerated___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
#if defined(CN1_SIMD_ARM64)
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

/* ------------------------------------------------------------------- int32 */

JAVA_VOID com_codename1_impl_windows_WindowsSimd_add___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        __m128i va = _mm_loadu_si128((const __m128i*) (a + i));
        __m128i vb = _mm_loadu_si128((const __m128i*) (b + i));
        _mm_storeu_si128((__m128i*) (d + i), _mm_add_epi32(va, vb));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_s32((int32_t*) (d + i), vaddq_s32(vld1q_s32((const int32_t*) (a + i)), vld1q_s32((const int32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] + b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_sub___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        __m128i va = _mm_loadu_si128((const __m128i*) (a + i));
        __m128i vb = _mm_loadu_si128((const __m128i*) (b + i));
        _mm_storeu_si128((__m128i*) (d + i), _mm_sub_epi32(va, vb));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_s32((int32_t*) (d + i), vsubq_s32(vld1q_s32((const int32_t*) (a + i)), vld1q_s32((const int32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] - b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_mul___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
    /* SSE2 has no 32-bit integer multiply; arm64 NEON does. */
#if defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_s32((int32_t*) (d + i), vmulq_s32(vld1q_s32((const int32_t*) (a + i)), vld1q_s32((const int32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] * b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_min___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_s32((int32_t*) (d + i), vminq_s32(vld1q_s32((const int32_t*) (a + i)), vld1q_s32((const int32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] < b[i] ? a[i] : b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_max___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_s32((int32_t*) (d + i), vmaxq_s32(vld1q_s32((const int32_t*) (a + i)), vld1q_s32((const int32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] > b[i] ? a[i] : b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_and___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        _mm_storeu_si128((__m128i*) (d + i), _mm_and_si128(_mm_loadu_si128((const __m128i*) (a + i)), _mm_loadu_si128((const __m128i*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_s32((int32_t*) (d + i), vandq_s32(vld1q_s32((const int32_t*) (a + i)), vld1q_s32((const int32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] & b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_or___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        _mm_storeu_si128((__m128i*) (d + i), _mm_or_si128(_mm_loadu_si128((const __m128i*) (a + i)), _mm_loadu_si128((const __m128i*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_s32((int32_t*) (d + i), vorrq_s32(vld1q_s32((const int32_t*) (a + i)), vld1q_s32((const int32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] | b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_xor___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        _mm_storeu_si128((__m128i*) (d + i), _mm_xor_si128(_mm_loadu_si128((const __m128i*) (a + i)), _mm_loadu_si128((const __m128i*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_s32((int32_t*) (d + i), veorq_s32(vld1q_s32((const int32_t*) (a + i)), vld1q_s32((const int32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] ^ b[i];
    }
}

JAVA_INT com_codename1_impl_windows_WindowsSimd_sum___int_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) src)->data;
    int i = offset, end = offset + length;
    int total = 0;
#if defined(CN1_SIMD_X64)
    __m128i acc = _mm_setzero_si128();
    for (; i <= end - 4; i += 4) {
        acc = _mm_add_epi32(acc, _mm_loadu_si128((const __m128i*) (a + i)));
    }
    {
        int32_t tmp[4];
        _mm_storeu_si128((__m128i*) tmp, acc);
        total += tmp[0] + tmp[1] + tmp[2] + tmp[3];
    }
#elif defined(CN1_SIMD_ARM64)
    int32x4_t acc = vdupq_n_s32(0);
    for (; i <= end - 4; i += 4) {
        acc = vaddq_s32(acc, vld1q_s32((const int32_t*) (a + i)));
    }
    total += vaddvq_s32(acc);
#endif
    for (; i < end; i++) {
        total += a[i];
    }
    return total;
}

JAVA_INT com_codename1_impl_windows_WindowsSimd_dot___int_1ARRAY_int_1ARRAY_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) srcB)->data;
    int i = offset, end = offset + length;
    int total = 0;
#if defined(CN1_SIMD_ARM64)
    int32x4_t acc = vdupq_n_s32(0);
    for (; i <= end - 4; i += 4) {
        acc = vmlaq_s32(acc, vld1q_s32((const int32_t*) (a + i)), vld1q_s32((const int32_t*) (b + i)));
    }
    total += vaddvq_s32(acc);
#endif
    for (; i < end; i++) {
        total += a[i] * b[i];
    }
    return total;
}

/* ------------------------------------------------------------------- float */

JAVA_VOID com_codename1_impl_windows_WindowsSimd_add___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        _mm_storeu_ps((float*) (d + i), _mm_add_ps(_mm_loadu_ps((const float*) (a + i)), _mm_loadu_ps((const float*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_f32((float32_t*) (d + i), vaddq_f32(vld1q_f32((const float32_t*) (a + i)), vld1q_f32((const float32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] + b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_sub___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        _mm_storeu_ps((float*) (d + i), _mm_sub_ps(_mm_loadu_ps((const float*) (a + i)), _mm_loadu_ps((const float*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_f32((float32_t*) (d + i), vsubq_f32(vld1q_f32((const float32_t*) (a + i)), vld1q_f32((const float32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] - b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_mul___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        _mm_storeu_ps((float*) (d + i), _mm_mul_ps(_mm_loadu_ps((const float*) (a + i)), _mm_loadu_ps((const float*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_f32((float32_t*) (d + i), vmulq_f32(vld1q_f32((const float32_t*) (a + i)), vld1q_f32((const float32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] * b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_min___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        _mm_storeu_ps((float*) (d + i), _mm_min_ps(_mm_loadu_ps((const float*) (a + i)), _mm_loadu_ps((const float*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_f32((float32_t*) (d + i), vminq_f32(vld1q_f32((const float32_t*) (a + i)), vld1q_f32((const float32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] < b[i] ? a[i] : b[i];
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_max___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 4; i += 4) {
        _mm_storeu_ps((float*) (d + i), _mm_max_ps(_mm_loadu_ps((const float*) (a + i)), _mm_loadu_ps((const float*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 4; i += 4) {
        vst1q_f32((float32_t*) (d + i), vmaxq_f32(vld1q_f32((const float32_t*) (a + i)), vld1q_f32((const float32_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = a[i] > b[i] ? a[i] : b[i];
    }
}

JAVA_FLOAT com_codename1_impl_windows_WindowsSimd_sum___float_1ARRAY_int_int_R_float(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) src)->data;
    int i = offset, end = offset + length;
    float total = 0.0f;
#if defined(CN1_SIMD_X64)
    __m128 acc = _mm_setzero_ps();
    for (; i <= end - 4; i += 4) {
        acc = _mm_add_ps(acc, _mm_loadu_ps((const float*) (a + i)));
    }
    {
        float tmp[4];
        _mm_storeu_ps(tmp, acc);
        total += tmp[0] + tmp[1] + tmp[2] + tmp[3];
    }
#elif defined(CN1_SIMD_ARM64)
    float32x4_t acc = vdupq_n_f32(0.0f);
    for (; i <= end - 4; i += 4) {
        acc = vaddq_f32(acc, vld1q_f32((const float32_t*) (a + i)));
    }
    total += vaddvq_f32(acc);
#endif
    for (; i < end; i++) {
        total += a[i];
    }
    return total;
}

JAVA_FLOAT com_codename1_impl_windows_WindowsSimd_dot___float_1ARRAY_float_1ARRAY_int_int_R_float(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*) ((JAVA_ARRAY) srcB)->data;
    int i = offset, end = offset + length;
    float total = 0.0f;
#if defined(CN1_SIMD_X64)
    __m128 acc = _mm_setzero_ps();
    for (; i <= end - 4; i += 4) {
        acc = _mm_add_ps(acc, _mm_mul_ps(_mm_loadu_ps((const float*) (a + i)), _mm_loadu_ps((const float*) (b + i))));
    }
    {
        float tmp[4];
        _mm_storeu_ps(tmp, acc);
        total += tmp[0] + tmp[1] + tmp[2] + tmp[3];
    }
#elif defined(CN1_SIMD_ARM64)
    float32x4_t acc = vdupq_n_f32(0.0f);
    for (; i <= end - 4; i += 4) {
        acc = vmlaq_f32(acc, vld1q_f32((const float32_t*) (a + i)), vld1q_f32((const float32_t*) (b + i)));
    }
    total += vaddvq_f32(acc);
#endif
    for (; i < end; i++) {
        total += a[i] * b[i];
    }
    return total;
}

/* -------------------------------------------------------------------- byte */

JAVA_VOID com_codename1_impl_windows_WindowsSimd_add___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 16; i += 16) {
        _mm_storeu_si128((__m128i*) (d + i), _mm_adds_epi8(_mm_loadu_si128((const __m128i*) (a + i)), _mm_loadu_si128((const __m128i*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 16; i += 16) {
        vst1q_s8((int8_t*) (d + i), vqaddq_s8(vld1q_s8((const int8_t*) (a + i)), vld1q_s8((const int8_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = cn1SimdSaturateByte((int) a[i] + (int) b[i]);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_sub___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 16; i += 16) {
        _mm_storeu_si128((__m128i*) (d + i), _mm_subs_epi8(_mm_loadu_si128((const __m128i*) (a + i)), _mm_loadu_si128((const __m128i*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 16; i += 16) {
        vst1q_s8((int8_t*) (d + i), vqsubq_s8(vld1q_s8((const int8_t*) (a + i)), vld1q_s8((const int8_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = cn1SimdSaturateByte((int) a[i] - (int) b[i]);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_and___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 16; i += 16) {
        _mm_storeu_si128((__m128i*) (d + i), _mm_and_si128(_mm_loadu_si128((const __m128i*) (a + i)), _mm_loadu_si128((const __m128i*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 16; i += 16) {
        vst1q_s8((int8_t*) (d + i), vandq_s8(vld1q_s8((const int8_t*) (a + i)), vld1q_s8((const int8_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE) (a[i] & b[i]);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_or___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 16; i += 16) {
        _mm_storeu_si128((__m128i*) (d + i), _mm_or_si128(_mm_loadu_si128((const __m128i*) (a + i)), _mm_loadu_si128((const __m128i*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 16; i += 16) {
        vst1q_s8((int8_t*) (d + i), vorrq_s8(vld1q_s8((const int8_t*) (a + i)), vld1q_s8((const int8_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE) (a[i] | b[i]);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_xor___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data;
    int i = offset, end = offset + length;
#if defined(CN1_SIMD_X64)
    for (; i <= end - 16; i += 16) {
        _mm_storeu_si128((__m128i*) (d + i), _mm_xor_si128(_mm_loadu_si128((const __m128i*) (a + i)), _mm_loadu_si128((const __m128i*) (b + i))));
    }
#elif defined(CN1_SIMD_ARM64)
    for (; i <= end - 16; i += 16) {
        vst1q_s8((int8_t*) (d + i), veorq_s8(vld1q_s8((const int8_t*) (a + i)), vld1q_s8((const int8_t*) (b + i))));
    }
#endif
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE) (a[i] ^ b[i]);
    }
}

/* ----------------------------------------------- fused image hot paths */

/* dst[i] = (rgbSrc[i] & 0x00ffffff) | ((alphaSrc[i] & 0xff) << 24) */
JAVA_VOID com_codename1_impl_windows_WindowsSimd_replaceTopByteFromUnsignedBytes___int_1ARRAY_int_byte_1ARRAY_int_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT rgbSrc, JAVA_INT rgbSrcOffset,
        JAVA_OBJECT alphaSrc, JAVA_INT alphaSrcOffset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* rgb = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) rgbSrc)->data;
    JAVA_ARRAY_BYTE* alpha = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) alphaSrc)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = 0;
#if defined(CN1_SIMD_X64)
    __m128i rgbMask = _mm_set1_epi32(0x00ffffff);
    for (; i <= length - 4; i += 4) {
        __m128i v = _mm_and_si128(_mm_loadu_si128((const __m128i*) (rgb + rgbSrcOffset + i)), rgbMask);
        int a0 = alpha[alphaSrcOffset + i] & 0xff;
        int a1 = alpha[alphaSrcOffset + i + 1] & 0xff;
        int a2 = alpha[alphaSrcOffset + i + 2] & 0xff;
        int a3 = alpha[alphaSrcOffset + i + 3] & 0xff;
        __m128i va = _mm_slli_epi32(_mm_set_epi32(a3, a2, a1, a0), 24);
        _mm_storeu_si128((__m128i*) (d + dstOffset + i), _mm_or_si128(v, va));
    }
#elif defined(CN1_SIMD_ARM64)
    uint32x4_t rgbMask = vdupq_n_u32(0x00ffffff);
    for (; i <= length - 4; i += 4) {
        uint32x4_t v = vandq_u32(vld1q_u32((const uint32_t*) (rgb + rgbSrcOffset + i)), rgbMask);
        uint32_t a[4];
        a[0] = (uint32_t) (alpha[alphaSrcOffset + i] & 0xff) << 24;
        a[1] = (uint32_t) (alpha[alphaSrcOffset + i + 1] & 0xff) << 24;
        a[2] = (uint32_t) (alpha[alphaSrcOffset + i + 2] & 0xff) << 24;
        a[3] = (uint32_t) (alpha[alphaSrcOffset + i + 3] & 0xff) << 24;
        vst1q_u32((uint32_t*) (d + dstOffset + i), vorrq_u32(v, vld1q_u32(a)));
    }
#endif
    for (; i < length; i++) {
        int rgbv = rgb[rgbSrcOffset + i] & 0x00ffffff;
        int av = (alpha[alphaSrcOffset + i] & 0xff) << 24;
        d[dstOffset + i] = rgbv | av;
    }
}

/* dst[i] = (src[i] & testMask) != 0 ? (src[i] & trueKeepMask) | trueOrValue : src[i] */
JAVA_VOID com_codename1_impl_windows_WindowsSimd_blendByMaskTestNonzero___int_1ARRAY_int_int_int_int_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT srcOffset,
        JAVA_INT testMask, JAVA_INT trueKeepMask, JAVA_INT trueOrValue, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = 0;
#if defined(CN1_SIMD_X64)
    __m128i vTest = _mm_set1_epi32(testMask);
    __m128i vKeep = _mm_set1_epi32(trueKeepMask);
    __m128i vOr = _mm_set1_epi32(trueOrValue);
    __m128i vZero = _mm_setzero_si128();
    for (; i <= length - 4; i += 4) {
        __m128i v = _mm_loadu_si128((const __m128i*) (s + srcOffset + i));
        __m128i masked = _mm_and_si128(v, vTest);
        /* selMask lane = 0xFFFFFFFF where (v & testMask) != 0, else 0 */
        __m128i selMask = _mm_xor_si128(_mm_cmpeq_epi32(masked, vZero), _mm_set1_epi32(-1));
        __m128i modified = _mm_or_si128(_mm_and_si128(v, vKeep), vOr);
        __m128i result = _mm_or_si128(_mm_and_si128(selMask, modified), _mm_andnot_si128(selMask, v));
        _mm_storeu_si128((__m128i*) (d + dstOffset + i), result);
    }
#elif defined(CN1_SIMD_ARM64)
    int32x4_t vTest = vdupq_n_s32(testMask);
    int32x4_t vKeep = vdupq_n_s32(trueKeepMask);
    int32x4_t vOr = vdupq_n_s32(trueOrValue);
    for (; i <= length - 4; i += 4) {
        int32x4_t v = vld1q_s32((const int32_t*) (s + srcOffset + i));
        uint32x4_t selMask = vtstq_s32(v, vTest); /* lane all-ones where (v & testMask) != 0 */
        int32x4_t modified = vorrq_s32(vandq_s32(v, vKeep), vOr);
        int32x4_t result = vbslq_s32(selMask, modified, v);
        vst1q_s32((int32_t*) (d + dstOffset + i), result);
    }
#endif
    for (; i < length; i++) {
        int v = s[srcOffset + i];
        d[dstOffset + i] = (v & testMask) != 0 ? (v & trueKeepMask) | trueOrValue : v;
    }
}

/* Fused blend used by Image.modifyAlpha(alpha, removeColor): like the above, but
 * a kept value equal to removeMatch substitutes removeValue (transparent) instead
 * of OR-ing trueOrValue. Without this native kernel removeColor fell back to the
 * scalar default and lost; vectorized it joins the other blend ops that win. */
JAVA_VOID com_codename1_impl_windows_WindowsSimd_blendByMaskTestNonzeroSubstituteOnKeepEq___int_1ARRAY_int_int_int_int_int_int_int_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT srcOffset,
        JAVA_INT testMask, JAVA_INT trueKeepMask, JAVA_INT trueOrValue, JAVA_INT removeMatch,
        JAVA_INT removeValue, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*) ((JAVA_ARRAY) dst)->data;
    int i = 0;
#if defined(CN1_SIMD_X64)
    __m128i vTest = _mm_set1_epi32(testMask);
    __m128i vKeep = _mm_set1_epi32(trueKeepMask);
    __m128i vOr = _mm_set1_epi32(trueOrValue);
    __m128i vMatch = _mm_set1_epi32(removeMatch);
    __m128i vRemove = _mm_set1_epi32(removeValue);
    __m128i vZero = _mm_setzero_si128();
    for (; i <= length - 4; i += 4) {
        __m128i v = _mm_loadu_si128((const __m128i*) (s + srcOffset + i));
        __m128i selMask = _mm_xor_si128(_mm_cmpeq_epi32(_mm_and_si128(v, vTest), vZero), _mm_set1_epi32(-1));
        __m128i kept = _mm_and_si128(v, vKeep);
        __m128i isRemove = _mm_cmpeq_epi32(kept, vMatch);
        __m128i normal = _mm_or_si128(kept, vOr);
        __m128i modified = _mm_or_si128(_mm_and_si128(isRemove, vRemove), _mm_andnot_si128(isRemove, normal));
        __m128i result = _mm_or_si128(_mm_and_si128(selMask, modified), _mm_andnot_si128(selMask, v));
        _mm_storeu_si128((__m128i*) (d + dstOffset + i), result);
    }
#elif defined(CN1_SIMD_ARM64)
    int32x4_t vTest = vdupq_n_s32(testMask);
    int32x4_t vKeep = vdupq_n_s32(trueKeepMask);
    int32x4_t vOr = vdupq_n_s32(trueOrValue);
    int32x4_t vMatch = vdupq_n_s32(removeMatch);
    int32x4_t vRemove = vdupq_n_s32(removeValue);
    for (; i <= length - 4; i += 4) {
        int32x4_t v = vld1q_s32((const int32_t*) (s + srcOffset + i));
        uint32x4_t selMask = vtstq_s32(v, vTest);
        int32x4_t kept = vandq_s32(v, vKeep);
        uint32x4_t isRemove = vceqq_s32(kept, vMatch);
        int32x4_t normal = vorrq_s32(kept, vOr);
        int32x4_t modified = vbslq_s32(isRemove, vRemove, normal);
        int32x4_t result = vbslq_s32(selMask, modified, v);
        vst1q_s32((int32_t*) (d + dstOffset + i), result);
    }
#endif
    for (; i < length; i++) {
        int v = s[srcOffset + i];
        if ((v & testMask) == 0) {
            d[dstOffset + i] = v;
        } else {
            int kept = v & trueKeepMask;
            d[dstOffset + i] = (kept == removeMatch) ? removeValue : (kept | trueOrValue);
        }
    }
}

/* --------------------------------------------------------------------------
 * Byte-manipulation kernels the Base64 SIMD codec and Image.createMask use
 * (shl / shrLogical / lookupBytes / pack+unpack interleaved / packIntToByteTruncate).
 * Before these existed they fell through to the generic Simd scalar defaults --
 * lane-scratch loops with per-op method dispatch -- which were *slower* than the
 * straight-line scalar codec, so "SIMD on" lost. A native C kernel (one call,
 * tight loop) already beats that; arm64 additionally vectorizes the hot paths
 * with NEON (mirroring IOSSimd.m), and x64 vectorizes the byte shifts with SSE2
 * (which has no native byte shift -- emulate via 16-bit shift + per-byte mask).
 * The table lookups stay scalar on both, exactly as IOSSimd does.
 * ------------------------------------------------------------------------ */

/* byte[] left shift by (bits & 7), src/dst may differ. */
static void cn1SimdByteShl(JAVA_ARRAY_BYTE* s, int so, JAVA_ARRAY_BYTE* d, int dout, int bits, int length) {
    int shift = bits & 7;
    int i = 0;
#if defined(CN1_SIMD_X64)
    __m128i mask = _mm_set1_epi8((char) ((0xFF << shift) & 0xFF));
    for (; i <= length - 16; i += 16) {
        __m128i v = _mm_loadu_si128((const __m128i*) (s + so + i));
        _mm_storeu_si128((__m128i*) (d + dout + i), _mm_and_si128(_mm_slli_epi16(v, shift), mask));
    }
#elif defined(CN1_SIMD_ARM64)
    int8x16_t vshift = vdupq_n_s8((int8_t) shift);
    for (; i <= length - 16; i += 16) {
        vst1q_u8((uint8_t*) (d + dout + i), vshlq_u8(vld1q_u8((const uint8_t*) (s + so + i)), vshift));
    }
#endif
    for (; i < length; i++) {
        d[dout + i] = (JAVA_ARRAY_BYTE) (((uint8_t) s[so + i]) << shift);
    }
}

/* byte[] logical right shift by (bits & 7). */
static void cn1SimdByteShr(JAVA_ARRAY_BYTE* s, int so, JAVA_ARRAY_BYTE* d, int dout, int bits, int length) {
    int shift = bits & 7;
    int i = 0;
#if defined(CN1_SIMD_X64)
    __m128i mask = _mm_set1_epi8((char) ((0xFF >> shift) & 0xFF));
    for (; i <= length - 16; i += 16) {
        __m128i v = _mm_loadu_si128((const __m128i*) (s + so + i));
        _mm_storeu_si128((__m128i*) (d + dout + i), _mm_and_si128(_mm_srli_epi16(v, shift), mask));
    }
#elif defined(CN1_SIMD_ARM64)
    int8x16_t vneg = vdupq_n_s8((int8_t) (-shift));
    for (; i <= length - 16; i += 16) {
        vst1q_u8((uint8_t*) (d + dout + i), vshlq_u8(vld1q_u8((const uint8_t*) (s + so + i)), vneg));
    }
#endif
    for (; i < length; i++) {
        d[dout + i] = (JAVA_ARRAY_BYTE) (((uint8_t) s[so + i]) >> shift);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsSimd_shl___byte_1ARRAY_int_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT bits,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    cn1SimdByteShl((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data, offset,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, offset, bits, length);
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_shl___byte_1ARRAY_int_int_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT srcOffset,
        JAVA_INT bits, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    cn1SimdByteShl((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data, srcOffset,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, dstOffset, bits, length);
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_shrLogical___byte_1ARRAY_int_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT bits,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    cn1SimdByteShr((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data, offset,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, offset, bits, length);
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_shrLogical___byte_1ARRAY_int_int_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT srcOffset,
        JAVA_INT bits, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    cn1SimdByteShr((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data, srcOffset,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, dstOffset, bits, length);
}

/* dst[i] = (table[indices[i] & 0xff] when in range else 0); scalar, like IOSSimd. */
static void cn1SimdLookup(JAVA_ARRAY_BYTE* t, int tableLen, JAVA_ARRAY_BYTE* idx, int io,
                          JAVA_ARRAY_BYTE* d, int dout, int length) {
    for (int i = 0; i < length; i++) {
        int li = idx[io + i] & 0xff;
        d[dout + i] = li < tableLen ? t[li] : 0;
    }
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_lookupBytes___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT table, JAVA_OBJECT indices,
        JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    cn1SimdLookup((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) table)->data, ((JAVA_ARRAY) table)->length,
                  (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) indices)->data, offset,
                  (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, offset, length);
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_lookupBytes___byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT table, JAVA_OBJECT indices,
        JAVA_INT indicesOffset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    cn1SimdLookup((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) table)->data, ((JAVA_ARRAY) table)->length,
                  (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) indices)->data, indicesOffset,
                  (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, dstOffset, length);
}

/* dst[i] = (byte) src[i]; int32 -> byte truncate. */
static void cn1SimdPackIntToByte(JAVA_ARRAY_INT* s, int so, JAVA_ARRAY_BYTE* d, int dout, int length) {
    for (int i = 0; i < length; i++) {
        d[dout + i] = (JAVA_ARRAY_BYTE) (s[so + i] & 0xff);
    }
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_packIntToByteTruncate___int_1ARRAY_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_OBJECT dst,
        JAVA_INT offset, JAVA_INT length) {
    cn1SimdPackIntToByte((JAVA_ARRAY_INT*) ((JAVA_ARRAY) src)->data, offset,
                         (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, offset, length);
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_packIntToByteTruncate___int_1ARRAY_int_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT srcOffset,
        JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    cn1SimdPackIntToByte((JAVA_ARRAY_INT*) ((JAVA_ARRAY) src)->data, srcOffset,
                         (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, dstOffset, length);
}

/* 3-way de-interleave: src is [a0 b0 c0 a1 b1 c1 ...] -> d0/d1/d2. */
static void cn1SimdUnpack3(JAVA_ARRAY_BYTE* s, int so, JAVA_ARRAY_BYTE* d0, int o0,
                           JAVA_ARRAY_BYTE* d1, int o1, JAVA_ARRAY_BYTE* d2, int o2, int length) {
    int i = 0;
#if defined(CN1_SIMD_ARM64)
    for (; i <= length - 16; i += 16) {
        uint8x16x3_t v = vld3q_u8((const uint8_t*) (s + so + i * 3));
        vst1q_u8((uint8_t*) (d0 + o0 + i), v.val[0]);
        vst1q_u8((uint8_t*) (d1 + o1 + i), v.val[1]);
        vst1q_u8((uint8_t*) (d2 + o2 + i), v.val[2]);
    }
#endif
    for (; i < length; i++) {
        d0[o0 + i] = s[so + i * 3];
        d1[o1 + i] = s[so + i * 3 + 1];
        d2[o2 + i] = s[so + i * 3 + 2];
    }
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_unpackBytesInterleaved3___byte_1ARRAY_int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT srcOffset,
        JAVA_OBJECT dst0, JAVA_OBJECT dst1, JAVA_OBJECT dst2, JAVA_INT length) {
    cn1SimdUnpack3((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data, srcOffset,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst0)->data, 0,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst1)->data, 0,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst2)->data, 0, length);
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_unpackBytesInterleaved3___byte_1ARRAY_int_byte_1ARRAY_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT srcOffset,
        JAVA_OBJECT dst, JAVA_INT dst0Offset, JAVA_INT dst1Offset, JAVA_INT dst2Offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data;
    cn1SimdUnpack3((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data, srcOffset,
                   d, dst0Offset, d, dst1Offset, d, dst2Offset, length);
}

/* 3-way interleave: d0/d1/d2 -> dst [a0 b0 c0 a1 b1 c1 ...]. */
static void cn1SimdPack3(JAVA_ARRAY_BYTE* s0, int o0, JAVA_ARRAY_BYTE* s1, int o1,
                         JAVA_ARRAY_BYTE* s2, int o2, JAVA_ARRAY_BYTE* d, int dout, int length) {
    int i = 0;
#if defined(CN1_SIMD_ARM64)
    for (; i <= length - 16; i += 16) {
        uint8x16x3_t v;
        v.val[0] = vld1q_u8((const uint8_t*) (s0 + o0 + i));
        v.val[1] = vld1q_u8((const uint8_t*) (s1 + o1 + i));
        v.val[2] = vld1q_u8((const uint8_t*) (s2 + o2 + i));
        vst3q_u8((uint8_t*) (d + dout + i * 3), v);
    }
#endif
    for (; i < length; i++) {
        d[dout + i * 3] = s0[o0 + i];
        d[dout + i * 3 + 1] = s1[o1 + i];
        d[dout + i * 3 + 2] = s2[o2 + i];
    }
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_packBytesInterleaved3___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src0, JAVA_OBJECT src1,
        JAVA_OBJECT src2, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    cn1SimdPack3((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src0)->data, 0,
                 (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src1)->data, 0,
                 (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src2)->data, 0,
                 (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, dstOffset, length);
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_packBytesInterleaved3___byte_1ARRAY_int_int_int_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT src0Offset,
        JAVA_INT src1Offset, JAVA_INT src2Offset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data;
    cn1SimdPack3(s, src0Offset, s, src1Offset, s, src2Offset,
                 (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, dstOffset, length);
}

/* 4-way interleave. */
static void cn1SimdPack4(JAVA_ARRAY_BYTE* s0, int o0, JAVA_ARRAY_BYTE* s1, int o1,
                         JAVA_ARRAY_BYTE* s2, int o2, JAVA_ARRAY_BYTE* s3, int o3,
                         JAVA_ARRAY_BYTE* d, int dout, int length) {
    int i = 0;
#if defined(CN1_SIMD_ARM64)
    for (; i <= length - 16; i += 16) {
        uint8x16x4_t v;
        v.val[0] = vld1q_u8((const uint8_t*) (s0 + o0 + i));
        v.val[1] = vld1q_u8((const uint8_t*) (s1 + o1 + i));
        v.val[2] = vld1q_u8((const uint8_t*) (s2 + o2 + i));
        v.val[3] = vld1q_u8((const uint8_t*) (s3 + o3 + i));
        vst4q_u8((uint8_t*) (d + dout + i * 4), v);
    }
#endif
    for (; i < length; i++) {
        d[dout + i * 4] = s0[o0 + i];
        d[dout + i * 4 + 1] = s1[o1 + i];
        d[dout + i * 4 + 2] = s2[o2 + i];
        d[dout + i * 4 + 3] = s3[o3 + i];
    }
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_packBytesInterleaved4___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src0, JAVA_OBJECT src1,
        JAVA_OBJECT src2, JAVA_OBJECT src3, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    cn1SimdPack4((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src0)->data, 0,
                 (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src1)->data, 0,
                 (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src2)->data, 0,
                 (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src3)->data, 0,
                 (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, dstOffset, length);
}
JAVA_VOID com_codename1_impl_windows_WindowsSimd_packBytesInterleaved4___byte_1ARRAY_int_int_int_int_byte_1ARRAY_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT src, JAVA_INT src0Offset,
        JAVA_INT src1Offset, JAVA_INT src2Offset, JAVA_INT src3Offset, JAVA_OBJECT dst,
        JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data;
    cn1SimdPack4(s, src0Offset, s, src1Offset, s, src2Offset, s, src3Offset,
                 (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data, dstOffset, length);
}

/* 4-way de-interleave with a table lookup on each byte. Matches the base Simd
 * contract exactly: an out-of-range index resolves to 0, every quad is written,
 * and the return value is the bitwise OR of all output bytes (the caller uses it
 * to detect invalid input -- e.g. an 0x80 sentinel in the Base64 decode map). */
static int cn1SimdUnpackLookup4(JAVA_ARRAY_BYTE* t, int tableLen, JAVA_ARRAY_BYTE* s, int so,
                                JAVA_ARRAY_BYTE* d0, int o0, JAVA_ARRAY_BYTE* d1, int o1,
                                JAVA_ARRAY_BYTE* d2, int o2, JAVA_ARRAY_BYTE* d3, int o3, int length) {
    int orAll = 0;
    for (int i = 0; i < length; i++) {
        int b0 = s[so + i * 4] & 0xff, b1 = s[so + i * 4 + 1] & 0xff;
        int b2 = s[so + i * 4 + 2] & 0xff, b3 = s[so + i * 4 + 3] & 0xff;
        JAVA_ARRAY_BYTE v0 = b0 < tableLen ? t[b0] : (JAVA_ARRAY_BYTE) 0;
        JAVA_ARRAY_BYTE v1 = b1 < tableLen ? t[b1] : (JAVA_ARRAY_BYTE) 0;
        JAVA_ARRAY_BYTE v2 = b2 < tableLen ? t[b2] : (JAVA_ARRAY_BYTE) 0;
        JAVA_ARRAY_BYTE v3 = b3 < tableLen ? t[b3] : (JAVA_ARRAY_BYTE) 0;
        d0[o0 + i] = v0; d1[o1 + i] = v1; d2[o2 + i] = v2; d3[o3 + i] = v3;
        orAll |= v0 | v1 | v2 | v3;
    }
    return orAll;
}
JAVA_INT com_codename1_impl_windows_WindowsSimd_unpackLookupBytesInterleaved4___byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT table, JAVA_OBJECT src,
        JAVA_INT srcOffset, JAVA_OBJECT dst0, JAVA_OBJECT dst1, JAVA_OBJECT dst2, JAVA_OBJECT dst3, JAVA_INT length) {
    return cn1SimdUnpackLookup4((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) table)->data, ((JAVA_ARRAY) table)->length,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data, srcOffset,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst0)->data, 0,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst1)->data, 0,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst2)->data, 0,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst3)->data, 0, length);
}
JAVA_INT com_codename1_impl_windows_WindowsSimd_unpackLookupBytesInterleaved4___byte_1ARRAY_byte_1ARRAY_int_byte_1ARRAY_int_int_int_int_int_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject, JAVA_OBJECT table, JAVA_OBJECT src,
        JAVA_INT srcOffset, JAVA_OBJECT dst, JAVA_INT dst0Offset, JAVA_INT dst1Offset, JAVA_INT dst2Offset,
        JAVA_INT dst3Offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) dst)->data;
    return cn1SimdUnpackLookup4((JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) table)->data, ((JAVA_ARRAY) table)->length,
                   (JAVA_ARRAY_BYTE*) ((JAVA_ARRAY) src)->data, srcOffset,
                   d, dst0Offset, d, dst1Offset, d, dst2Offset, d, dst3Offset, length);
}

#endif /* _WIN32 */
