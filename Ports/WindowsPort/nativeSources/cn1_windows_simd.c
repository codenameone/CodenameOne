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

#endif /* _WIN32 */
