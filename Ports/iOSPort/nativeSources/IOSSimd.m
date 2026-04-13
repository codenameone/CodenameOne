#include "xmlvm.h"
#include <stdint.h>
#include <math.h>
#include <arm_neon.h>

static JAVA_ARRAY_BYTE cn1_saturating_byte(int value) {
    if (value > 127) {
        return 127;
    }
    if (value < -128) {
        return -128;
    }
    return (JAVA_ARRAY_BYTE)value;
}

JAVA_OBJECT com_codename1_impl_ios_IOSSimd_allocByteNative___int_R_byte_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT size) {
    return allocArrayAligned(threadStateData, size, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1, 16);
}

JAVA_OBJECT com_codename1_impl_ios_IOSSimd_allocIntNative___int_R_int_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT size) {
    return allocArrayAligned(threadStateData, size, &class_array1__JAVA_INT, sizeof(JAVA_ARRAY_INT), 1, 16);
}

JAVA_OBJECT com_codename1_impl_ios_IOSSimd_allocFloatNative___int_R_float_1ARRAY(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT size) {
    return allocArrayAligned(threadStateData, size, &class_array1__JAVA_FLOAT, sizeof(JAVA_ARRAY_FLOAT), 1, 16);
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_add___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        int8x16_t vc = vqaddq_s8(va, vb);
        vst1q_s8((int8_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = cn1_saturating_byte((int)a[i] + (int)b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_sub___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        int8x16_t vc = vqsubq_s8(va, vb);
        vst1q_s8((int8_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = cn1_saturating_byte((int)a[i] - (int)b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_mul___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        int16x8_t low = vmull_s8(vget_low_s8(va), vget_low_s8(vb));
        int16x8_t high = vmull_s8(vget_high_s8(va), vget_high_s8(vb));
        int8x8_t low8 = vqmovn_s16(low);
        int8x8_t high8 = vqmovn_s16(high);
        int8x16_t out = vcombine_s8(low8, high8);
        vst1q_s8((int8_t*)(d + i), out);
    }
    for (; i < end; i++) {
        d[i] = cn1_saturating_byte((int)a[i] * (int)b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_min___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        int8x16_t vc = vminq_s8(va, vb);
        vst1q_s8((int8_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = a[i] < b[i] ? a[i] : b[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_max___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        int8x16_t vc = vmaxq_s8(va, vb);
        vst1q_s8((int8_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = a[i] > b[i] ? a[i] : b[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_abs___byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t vs = vld1q_s8((int8_t*)(s + i));
        int8x16_t vd = vqabsq_s8(vs);
        vst1q_s8((int8_t*)(d + i), vd);
    }
    for (; i < end; i++) {
        int v = s[i];
        d[i] = v == -128 ? 127 : (JAVA_ARRAY_BYTE)abs(v);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_clamp___byte_1ARRAY_byte_1ARRAY_byte_byte_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_BYTE minValue, JAVA_BYTE maxValue, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    int8x16_t vminv = vdupq_n_s8((int8_t)minValue);
    int8x16_t vmaxv = vdupq_n_s8((int8_t)maxValue);
    for (; i <= end - 16; i += 16) {
        int8x16_t vs = vld1q_s8((int8_t*)(s + i));
        int8x16_t vc = vmaxq_s8(vminv, vminq_s8(vs, vmaxv));
        vst1q_s8((int8_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        int v = s[i];
        if (v < minValue) {
            d[i] = minValue;
        } else if (v > maxValue) {
            d[i] = maxValue;
        } else {
            d[i] = (JAVA_ARRAY_BYTE)v;
        }
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_add___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        int32x4_t va = vld1q_s32((int32_t*)(a + i));
        int32x4_t vb = vld1q_s32((int32_t*)(b + i));
        int32x4_t vc = vaddq_s32(va, vb);
        vst1q_s32((int32_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)((int32_t)a[i] + (int32_t)b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_sub___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        int32x4_t va = vld1q_s32((int32_t*)(a + i));
        int32x4_t vb = vld1q_s32((int32_t*)(b + i));
        int32x4_t vc = vsubq_s32(va, vb);
        vst1q_s32((int32_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)((int32_t)a[i] - (int32_t)b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_mul___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        int32x4_t va = vld1q_s32((int32_t*)(a + i));
        int32x4_t vb = vld1q_s32((int32_t*)(b + i));
        int32x4_t vc = vmulq_s32(va, vb);
        vst1q_s32((int32_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)((int32_t)a[i] * (int32_t)b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_min___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        int32x4_t va = vld1q_s32((int32_t*)(a + i));
        int32x4_t vb = vld1q_s32((int32_t*)(b + i));
        int32x4_t vc = vminq_s32(va, vb);
        vst1q_s32((int32_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = a[i] < b[i] ? a[i] : b[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_max___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        int32x4_t va = vld1q_s32((int32_t*)(a + i));
        int32x4_t vb = vld1q_s32((int32_t*)(b + i));
        int32x4_t vc = vmaxq_s32(va, vb);
        vst1q_s32((int32_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = a[i] > b[i] ? a[i] : b[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_abs___int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        int32x4_t vs = vld1q_s32((int32_t*)(s + i));
        int32x4_t vd = vqabsq_s32(vs);
        vst1q_s32((int32_t*)(d + i), vd);
    }
    for (; i < end; i++) {
        int32_t v = (int32_t)s[i];
        d[i] = (JAVA_ARRAY_INT)(v == INT32_MIN ? INT32_MAX : (v < 0 ? -v : v));
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_clamp___int_1ARRAY_int_1ARRAY_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_INT minValue, JAVA_INT maxValue, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    int32x4_t vminv = vdupq_n_s32((int32_t)minValue);
    int32x4_t vmaxv = vdupq_n_s32((int32_t)maxValue);
    for (; i <= end - 4; i += 4) {
        int32x4_t vs = vld1q_s32((int32_t*)(s + i));
        int32x4_t vc = vmaxq_s32(vminv, vminq_s32(vs, vmaxv));
        vst1q_s32((int32_t*)(d + i), vc);
    }
    for (; i < end; i++) {
        int v = s[i];
        if (v < minValue) {
            d[i] = minValue;
        } else if (v > maxValue) {
            d[i] = maxValue;
        } else {
            d[i] = (JAVA_ARRAY_INT)v;
        }
    }
}

JAVA_INT com_codename1_impl_ios_IOSSimd_sum___int_1ARRAY_int_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    int i = offset;
    int end = offset + length;
    int64_t total = 0;
    int32x4_t vacc = vdupq_n_s32(0);
    for (; i <= end - 4; i += 4) {
        int32x4_t vs = vld1q_s32((int32_t*)(s + i));
        vacc = vaddq_s32(vacc, vs);
    }
    int32_t partial[4];
    vst1q_s32(partial, vacc);
    total += (int64_t)partial[0] + (int64_t)partial[1] + (int64_t)partial[2] + (int64_t)partial[3];
    for (; i < end; i++) {
        total += (int64_t)((int32_t)s[i]);
    }
    return (JAVA_INT)((int32_t)total);
}

JAVA_INT com_codename1_impl_ios_IOSSimd_dot___int_1ARRAY_int_1ARRAY_int_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    int i = offset;
    int end = offset + length;
    int64_t total = 0;
    int32x4_t vacc = vdupq_n_s32(0);
    for (; i <= end - 4; i += 4) {
        int32x4_t va = vld1q_s32((int32_t*)(a + i));
        int32x4_t vb = vld1q_s32((int32_t*)(b + i));
        vacc = vaddq_s32(vacc, vmulq_s32(va, vb));
    }
    int32_t partial[4];
    vst1q_s32(partial, vacc);
    total += (int64_t)partial[0] + (int64_t)partial[1] + (int64_t)partial[2] + (int64_t)partial[3];
    for (; i < end; i++) {
        total += (int64_t)((int32_t)a[i]) * (int64_t)((int32_t)b[i]);
    }
    return (JAVA_INT)((int32_t)total);
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_add___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        float32x4_t va = vld1q_f32((float*)(a + i));
        float32x4_t vb = vld1q_f32((float*)(b + i));
        float32x4_t vc = vaddq_f32(va, vb);
        vst1q_f32((float*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = a[i] + b[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_sub___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        float32x4_t va = vld1q_f32((float*)(a + i));
        float32x4_t vb = vld1q_f32((float*)(b + i));
        float32x4_t vc = vsubq_f32(va, vb);
        vst1q_f32((float*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = a[i] - b[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_mul___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        float32x4_t va = vld1q_f32((float*)(a + i));
        float32x4_t vb = vld1q_f32((float*)(b + i));
        float32x4_t vc = vmulq_f32(va, vb);
        vst1q_f32((float*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = a[i] * b[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_min___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        float32x4_t va = vld1q_f32((float*)(a + i));
        float32x4_t vb = vld1q_f32((float*)(b + i));
        float32x4_t vc = vminq_f32(va, vb);
        vst1q_f32((float*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = fminf(a[i], b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_max___float_1ARRAY_float_1ARRAY_float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        float32x4_t va = vld1q_f32((float*)(a + i));
        float32x4_t vb = vld1q_f32((float*)(b + i));
        float32x4_t vc = vmaxq_f32(va, vb);
        vst1q_f32((float*)(d + i), vc);
    }
    for (; i < end; i++) {
        d[i] = fmaxf(a[i], b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_abs___float_1ARRAY_float_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* s = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        float32x4_t vs = vld1q_f32((float*)(s + i));
        float32x4_t vd = vabsq_f32(vs);
        vst1q_f32((float*)(d + i), vd);
    }
    for (; i < end; i++) {
        d[i] = fabsf(s[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_clamp___float_1ARRAY_float_1ARRAY_float_float_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_FLOAT minValue, JAVA_FLOAT maxValue, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* s = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_FLOAT* d = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    float32x4_t vminv = vdupq_n_f32((float)minValue);
    float32x4_t vmaxv = vdupq_n_f32((float)maxValue);
    for (; i <= end - 4; i += 4) {
        float32x4_t vs = vld1q_f32((float*)(s + i));
        float32x4_t vc = vmaxq_f32(vminv, vminq_f32(vs, vmaxv));
        vst1q_f32((float*)(d + i), vc);
    }
    for (; i < end; i++) {
        float v = s[i];
        if (v < minValue) {
            d[i] = minValue;
        } else if (v > maxValue) {
            d[i] = maxValue;
        } else {
            d[i] = v;
        }
    }
}

JAVA_FLOAT com_codename1_impl_ios_IOSSimd_sum___float_1ARRAY_int_int_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* s = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)src)->data;
    int i = offset;
    int end = offset + length;
    float total = 0.f;
    float32x4_t vacc = vdupq_n_f32(0.f);
    for (; i <= end - 4; i += 4) {
        float32x4_t vs = vld1q_f32((float*)(s + i));
        vacc = vaddq_f32(vacc, vs);
    }
    float partial[4];
    vst1q_f32(partial, vacc);
    total += partial[0] + partial[1] + partial[2] + partial[3];
    for (; i < end; i++) {
        total += s[i];
    }
    return (JAVA_FLOAT)total;
}

JAVA_FLOAT com_codename1_impl_ios_IOSSimd_dot___float_1ARRAY_float_1ARRAY_int_int_R_float(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_FLOAT* a = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_FLOAT* b = (JAVA_ARRAY_FLOAT*)((JAVA_ARRAY)srcB)->data;
    int i = offset;
    int end = offset + length;
    float total = 0.f;
    float32x4_t vacc = vdupq_n_f32(0.f);
    for (; i <= end - 4; i += 4) {
        float32x4_t va = vld1q_f32((float*)(a + i));
        float32x4_t vb = vld1q_f32((float*)(b + i));
        vacc = vaddq_f32(vacc, vmulq_f32(va, vb));
    }
    float partial[4];
    vst1q_f32(partial, vacc);
    total += partial[0] + partial[1] + partial[2] + partial[3];
    for (; i < end; i++) {
        total += a[i] * b[i];
    }
    return (JAVA_FLOAT)total;
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_and___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        vst1q_s8((int8_t*)(d + i), vandq_s8(va, vb));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)(a[i] & b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_or___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        vst1q_s8((int8_t*)(d + i), vorrq_s8(va, vb));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)(a[i] | b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_xor___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        vst1q_s8((int8_t*)(d + i), veorq_s8(va, vb));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)(a[i] ^ b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_not___byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t vs = vld1q_s8((int8_t*)(s + i));
        vst1q_s8((int8_t*)(d + i), vmvnq_s8(vs));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)(~s[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpEq___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        uint8x16_t cmp = vceqq_s8(va, vb);
        vst1q_u8((uint8_t*)(m + i), cmp);
    }
    for (; i < end; i++) {
        m[i] = a[i] == b[i] ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpLt___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        uint8x16_t cmp = vcltq_s8(va, vb);
        vst1q_u8((uint8_t*)(m + i), cmp);
    }
    for (; i < end; i++) {
        m[i] = a[i] < b[i] ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpGt___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        int8x16_t va = vld1q_s8((int8_t*)(a + i));
        int8x16_t vb = vld1q_s8((int8_t*)(b + i));
        uint8x16_t cmp = vcgtq_s8(va, vb);
        vst1q_u8((uint8_t*)(m + i), cmp);
    }
    for (; i < end; i++) {
        m[i] = a[i] > b[i] ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpRange___byte_1ARRAY_byte_byte_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_BYTE minValue, JAVA_BYTE maxValue, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int i = offset;
    int end = offset + length;
    int8x16_t vminv = vdupq_n_s8((int8_t)minValue);
    int8x16_t vmaxv = vdupq_n_s8((int8_t)maxValue);
    for (; i <= end - 16; i += 16) {
        int8x16_t vs = vld1q_s8((int8_t*)(s + i));
        uint8x16_t ge = vcgeq_s8(vs, vminv);
        uint8x16_t le = vcleq_s8(vs, vmaxv);
        vst1q_u8((uint8_t*)(m + i), vandq_u8(ge, le));
    }
    for (; i < end; i++) {
        int v = s[i];
        m[i] = v >= minValue && v <= maxValue ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_select___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT mask, JAVA_OBJECT trueValues, JAVA_OBJECT falseValues, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)mask)->data;
    JAVA_ARRAY_BYTE* t = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)trueValues)->data;
    JAVA_ARRAY_BYTE* f = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)falseValues)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    int8x16_t zero = vdupq_n_s8(0);
    for (; i <= end - 16; i += 16) {
        int8x16_t vm = vld1q_s8((int8_t*)(m + i));
        int8x16_t vt = vld1q_s8((int8_t*)(t + i));
        int8x16_t vf = vld1q_s8((int8_t*)(f + i));
        uint8x16_t isZero = vceqq_s8(vm, zero);
        uint8x16_t out = vbslq_u8(isZero, vreinterpretq_u8_s8(vf), vreinterpretq_u8_s8(vt));
        vst1q_s8((int8_t*)(d + i), vreinterpretq_s8_u8(out));
    }
    for (; i < end; i++) {
        d[i] = m[i] != 0 ? t[i] : f[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_unpackUnsignedByteToInt___byte_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        uint8x16_t v = vld1q_u8((uint8_t*)(s + i));
        uint16x8_t lo16 = vmovl_u8(vget_low_u8(v));
        uint16x8_t hi16 = vmovl_u8(vget_high_u8(v));
        uint32x4_t x0 = vmovl_u16(vget_low_u16(lo16));
        uint32x4_t x1 = vmovl_u16(vget_high_u16(lo16));
        uint32x4_t x2 = vmovl_u16(vget_low_u16(hi16));
        uint32x4_t x3 = vmovl_u16(vget_high_u16(hi16));
        vst1q_s32((int32_t*)(d + i), vreinterpretq_s32_u32(x0));
        vst1q_s32((int32_t*)(d + i + 4), vreinterpretq_s32_u32(x1));
        vst1q_s32((int32_t*)(d + i + 8), vreinterpretq_s32_u32(x2));
        vst1q_s32((int32_t*)(d + i + 12), vreinterpretq_s32_u32(x3));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)(s[i] & 0xff);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_packIntToByteSaturating___int_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int end = offset + length;
    for (int i = offset; i < end; i++) {
        int v = s[i];
        if (v > 127) {
            d[i] = 127;
        } else if (v < -128) {
            d[i] = -128;
        } else {
            d[i] = (JAVA_ARRAY_BYTE)v;
        }
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_packIntToByteTruncate___int_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int end = offset + length;
    for (int i = offset; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)s[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_packIntToByteTruncate___int_1ARRAY_int_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT srcOffset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = 0;
    for (; i < length; i++) {
        d[dstOffset + i] = (JAVA_ARRAY_BYTE)s[srcOffset + i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_permuteBytes___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT indices, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    int srcLen = ((JAVA_ARRAY)src)->length;
    JAVA_ARRAY_BYTE* idx = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)indices)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int end = offset + length;
    for (int i = offset; i < end; i++) {
        int pos = idx[i];
        d[i] = (pos >= 0 && pos < srcLen) ? s[pos] : 0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_and___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        uint32x4_t va = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(a + i)));
        uint32x4_t vb = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(b + i)));
        vst1q_s32((int32_t*)(d + i), vreinterpretq_s32_u32(vandq_u32(va, vb)));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)(a[i] & b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_and___int_1ARRAY_int_int_1ARRAY_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_INT srcAOffset, JAVA_OBJECT srcB, JAVA_INT srcBOffset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = 0;
    for (; i <= length - 4; i += 4) {
        uint32x4_t va = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(a + srcAOffset + i)));
        uint32x4_t vb = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(b + srcBOffset + i)));
        vst1q_s32((int32_t*)(d + dstOffset + i), vreinterpretq_s32_u32(vandq_u32(va, vb)));
    }
    for (; i < length; i++) {
        d[dstOffset + i] = (JAVA_ARRAY_INT)(a[srcAOffset + i] & b[srcBOffset + i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_or___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        uint32x4_t va = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(a + i)));
        uint32x4_t vb = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(b + i)));
        vst1q_s32((int32_t*)(d + i), vreinterpretq_s32_u32(vorrq_u32(va, vb)));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)(a[i] | b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_or___int_1ARRAY_int_int_1ARRAY_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_INT srcAOffset, JAVA_OBJECT srcB, JAVA_INT srcBOffset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = 0;
    for (; i <= length - 4; i += 4) {
        uint32x4_t va = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(a + srcAOffset + i)));
        uint32x4_t vb = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(b + srcBOffset + i)));
        vst1q_s32((int32_t*)(d + dstOffset + i), vreinterpretq_s32_u32(vorrq_u32(va, vb)));
    }
    for (; i < length; i++) {
        d[dstOffset + i] = (JAVA_ARRAY_INT)(a[srcAOffset + i] | b[srcBOffset + i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_xor___int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        uint32x4_t va = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(a + i)));
        uint32x4_t vb = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(b + i)));
        vst1q_s32((int32_t*)(d + i), vreinterpretq_s32_u32(veorq_u32(va, vb)));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)(a[i] ^ b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_not___int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        uint32x4_t vs = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(s + i)));
        vst1q_s32((int32_t*)(d + i), vreinterpretq_s32_u32(vmvnq_u32(vs)));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)(~s[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_shl___int_1ARRAY_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT bits, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int shift = bits & 31;
    int i = offset;
    int end = offset + length;
    int32x4_t vshift = vdupq_n_s32(shift);
    for (; i <= end - 4; i += 4) {
        int32x4_t vs = vld1q_s32((int32_t*)(s + i));
        vst1q_s32((int32_t*)(d + i), vshlq_s32(vs, vshift));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)(s[i] << shift);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_shl___int_1ARRAY_int_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT srcOffset, JAVA_INT bits, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int shift = bits & 31;
    int i = 0;
    int32x4_t vshift = vdupq_n_s32(shift);
    for (; i <= length - 4; i += 4) {
        int32x4_t vs = vld1q_s32((int32_t*)(s + srcOffset + i));
        vst1q_s32((int32_t*)(d + dstOffset + i), vshlq_s32(vs, vshift));
    }
    for (; i < length; i++) {
        d[dstOffset + i] = (JAVA_ARRAY_INT)(s[srcOffset + i] << shift);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_shrLogical___int_1ARRAY_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT bits, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int shift = bits & 31;
    int i = offset;
    int end = offset + length;
    int32x4_t vshift = vdupq_n_s32(-shift);
    for (; i <= end - 4; i += 4) {
        uint32x4_t vs = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(s + i)));
        vst1q_s32((int32_t*)(d + i), vreinterpretq_s32_u32(vshlq_u32(vs, vshift)));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)(((uint32_t)s[i]) >> shift);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_shrLogical___int_1ARRAY_int_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT srcOffset, JAVA_INT bits, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int shift = bits & 31;
    int i = 0;
    int32x4_t vshift = vdupq_n_s32(-shift);
    for (; i <= length - 4; i += 4) {
        uint32x4_t vs = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(s + srcOffset + i)));
        vst1q_s32((int32_t*)(d + dstOffset + i), vreinterpretq_s32_u32(vshlq_u32(vs, vshift)));
    }
    for (; i < length; i++) {
        d[dstOffset + i] = (JAVA_ARRAY_INT)(((uint32_t)s[srcOffset + i]) >> shift);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_shrArithmetic___int_1ARRAY_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT bits, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int shift = bits & 31;
    int i = offset;
    int end = offset + length;
    int32x4_t vshift = vdupq_n_s32(-shift);
    for (; i <= end - 4; i += 4) {
        int32x4_t vs = vld1q_s32((int32_t*)(s + i));
        vst1q_s32((int32_t*)(d + i), vshlq_s32(vs, vshift));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_INT)(s[i] >> shift);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpEq___int_1ARRAY_int_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int end = offset + length;
    for (int i = offset; i < end; i++) {
        m[i] = a[i] == b[i] ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpLt___int_1ARRAY_int_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int end = offset + length;
    for (int i = offset; i < end; i++) {
        m[i] = a[i] < b[i] ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpGt___int_1ARRAY_int_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int end = offset + length;
    for (int i = offset; i < end; i++) {
        m[i] = a[i] > b[i] ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_select___byte_1ARRAY_int_1ARRAY_int_1ARRAY_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT mask, JAVA_OBJECT trueValues, JAVA_OBJECT falseValues, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)mask)->data;
    JAVA_ARRAY_INT* t = (JAVA_ARRAY_INT*)((JAVA_ARRAY)trueValues)->data;
    JAVA_ARRAY_INT* f = (JAVA_ARRAY_INT*)((JAVA_ARRAY)falseValues)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int end = offset + length;
    for (int i = offset; i < end; i++) {
        d[i] = m[i] != 0 ? t[i] : f[i];
    }
}

/* ------------------------------------------------------------------ */
/*  NEON-accelerated Base64 encode / decode                           */
/* ------------------------------------------------------------------ */

/*
 * Encode lookup: map a 6-bit index (0-63) to the Base64 ASCII byte.
 *
 * Uses branchless range comparisons and additions to avoid a lookup table.
 *
 * Range 0-25  -> 'A'..'Z'  (index + 65)
 * Range 26-51 -> 'a'..'z'  (index + 71)
 * Range 52-61 -> '0'..'9'  (index - 4)
 * 62          -> '+'        (index - 19)
 * 63          -> '/'        (index - 16)
 */
static inline uint8x16_t neon_base64_encode_lut(uint8x16_t indices) {
    /* Determine which range each index falls into */
    uint8x16_t lt26  = vcltq_u8(indices, vdupq_n_u8(26));
    uint8x16_t lt52  = vcltq_u8(indices, vdupq_n_u8(52));
    uint8x16_t lt62  = vcltq_u8(indices, vdupq_n_u8(62));
    uint8x16_t eq62  = vceqq_u8(indices, vdupq_n_u8(62));

    /*
     * Build offset: start with the offset for range 63 (= -16 = 240 unsigned).
     * Then conditionally replace with the offset for each lower range.
     */
    uint8x16_t offset = vdupq_n_u8(240); /* 63 -> '/' : 63+240 = 303 = 47 mod 256 */
    offset = vbslq_u8(eq62, vdupq_n_u8(237), offset); /* 62 -> '+' : 62+237 = 299 = 43 */
    offset = vbslq_u8(lt62, vdupq_n_u8(252), offset); /* 52..61 -> '0'..'9' : 252 = -4 */
    offset = vbslq_u8(lt52, vdupq_n_u8(71),  offset); /* 26..51 -> 'a'..'z' */
    offset = vbslq_u8(lt26, vdupq_n_u8(65),  offset); /* 0..25  -> 'A'..'Z' */

    return vaddq_u8(indices, offset);
}

JAVA_INT com_codename1_impl_ios_IOSSimd_base64Encode___byte_1ARRAY_int_int_byte_1ARRAY_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcObj, JAVA_INT srcOffset, JAVA_INT srcLen, JAVA_OBJECT dstObj, JAVA_INT dstOffset) {
    JAVA_ARRAY_BYTE* src = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcObj)->data;
    JAVA_ARRAY_BYTE* dst = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstObj)->data;
    int outputLength = ((srcLen + 2) / 3) * 4;
    if (srcLen <= 0) return 0;

    int si = srcOffset;
    int di = dstOffset;
    int end3 = srcOffset + srcLen - (srcLen % 3);

    /* NEON fast path: process 48 input bytes -> 64 output bytes per iteration */
    int neonEnd = end3 - 48 + 1;
    while (si <= neonEnd - 1) {
        /* Load 48 bytes and de-interleave into 3 registers of 16 */
        uint8x16x3_t in3 = vld3q_u8((const uint8_t*)(src + si));
        uint8x16_t b0 = in3.val[0];
        uint8x16_t b1 = in3.val[1];
        uint8x16_t b2 = in3.val[2];

        /* Extract four 6-bit index vectors */
        uint8x16_t i0 = vshrq_n_u8(b0, 2);
        uint8x16_t i1 = vorrq_u8(vshlq_n_u8(vandq_u8(b0, vdupq_n_u8(0x03)), 4),
                                   vshrq_n_u8(b1, 4));
        uint8x16_t i2 = vorrq_u8(vshlq_n_u8(vandq_u8(b1, vdupq_n_u8(0x0f)), 2),
                                   vshrq_n_u8(b2, 6));
        uint8x16_t i3 = vandq_u8(b2, vdupq_n_u8(0x3f));

        /* Map indices to Base64 ASCII */
        uint8x16x4_t out4;
        out4.val[0] = neon_base64_encode_lut(i0);
        out4.val[1] = neon_base64_encode_lut(i1);
        out4.val[2] = neon_base64_encode_lut(i2);
        out4.val[3] = neon_base64_encode_lut(i3);

        /* Interleave and store 64 output bytes */
        vst4q_u8((uint8_t*)(dst + di), out4);

        si += 48;
        di += 64;
    }

    /* Scalar tail for remaining complete triplets */
    static const char b64map[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    while (si < end3) {
        unsigned x0 = (unsigned char)src[si];
        unsigned x1 = (unsigned char)src[si + 1];
        unsigned x2 = (unsigned char)src[si + 2];
        dst[di]     = b64map[x0 >> 2];
        dst[di + 1] = b64map[((x0 & 0x03) << 4) | (x1 >> 4)];
        dst[di + 2] = b64map[((x1 & 0x0f) << 2) | (x2 >> 6)];
        dst[di + 3] = b64map[x2 & 0x3f];
        si += 3;
        di += 4;
    }

    /* Handle 1- or 2-byte remainder with padding */
    int remainder = srcOffset + srcLen - end3;
    if (remainder == 1) {
        unsigned x0 = (unsigned char)src[si];
        dst[di]     = b64map[x0 >> 2];
        dst[di + 1] = b64map[(x0 & 0x03) << 4];
        dst[di + 2] = '=';
        dst[di + 3] = '=';
    } else if (remainder == 2) {
        unsigned x0 = (unsigned char)src[si];
        unsigned x1 = (unsigned char)src[si + 1];
        dst[di]     = b64map[x0 >> 2];
        dst[di + 1] = b64map[((x0 & 0x03) << 4) | (x1 >> 4)];
        dst[di + 2] = b64map[(x1 & 0x0f) << 2];
        dst[di + 3] = '=';
    }

    return outputLength;
}

/*
 * Decode lookup: map a Base64 ASCII byte to its 6-bit value.
 *
 * Returns the 6-bit value in the low bits of each byte.
 * Invalid characters get 0xFF (will be detected via high-bit check).
 *
 * 'A'..'Z' (65-90)   -> 0..25
 * 'a'..'z' (97-122)  -> 26..51
 * '0'..'9' (48-57)   -> 52..61
 * '+'      (43)      -> 62
 * '/'      (47)      -> 63
 */
static inline uint8x16_t neon_base64_decode_lut(uint8x16_t chars) {
    uint8x16_t result = vdupq_n_u8(0xFF); /* invalid marker */

    /* 'A'..'Z' -> 0..25 : subtract 65 */
    uint8x16_t geA   = vcgeq_u8(chars, vdupq_n_u8(65));
    uint8x16_t leZ   = vcleq_u8(chars, vdupq_n_u8(90));
    uint8x16_t isUpper = vandq_u8(geA, leZ);
    result = vbslq_u8(isUpper, vsubq_u8(chars, vdupq_n_u8(65)), result);

    /* 'a'..'z' -> 26..51 : subtract 71 */
    uint8x16_t gea   = vcgeq_u8(chars, vdupq_n_u8(97));
    uint8x16_t lez   = vcleq_u8(chars, vdupq_n_u8(122));
    uint8x16_t isLower = vandq_u8(gea, lez);
    result = vbslq_u8(isLower, vsubq_u8(chars, vdupq_n_u8(71)), result);

    /* '0'..'9' -> 52..61 : add 4 (since '0'=48, 48+4=52) */
    uint8x16_t ge0   = vcgeq_u8(chars, vdupq_n_u8(48));
    uint8x16_t le9   = vcleq_u8(chars, vdupq_n_u8(57));
    uint8x16_t isDigit = vandq_u8(ge0, le9);
    result = vbslq_u8(isDigit, vsubq_u8(chars, vdupq_n_u8(252)), result); /* 48 - 252 = 52 mod 256 */

    /* '+' -> 62 */
    uint8x16_t isPlus = vceqq_u8(chars, vdupq_n_u8(43));
    result = vbslq_u8(isPlus, vdupq_n_u8(62), result);

    /* '/' -> 63 */
    uint8x16_t isSlash = vceqq_u8(chars, vdupq_n_u8(47));
    result = vbslq_u8(isSlash, vdupq_n_u8(63), result);

    return result;
}

/* Check if any lane has the 0xFF invalid marker (high bit set) */
static inline int neon_has_invalid(uint8x16_t v) {
    /* If any byte has its high bit set, there was an invalid character */
    uint8x16_t highBits = vshrq_n_u8(v, 7);
    /* Horizontal max: if any lane > 0, we have invalid chars */
    return vmaxvq_u8(highBits) != 0;
}

JAVA_INT com_codename1_impl_ios_IOSSimd_base64Decode___byte_1ARRAY_int_int_byte_1ARRAY_int_R_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcObj, JAVA_INT srcOffset, JAVA_INT srcLen, JAVA_OBJECT dstObj, JAVA_INT dstOffset) {
    JAVA_ARRAY_BYTE* src = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcObj)->data;
    JAVA_ARRAY_BYTE* dst = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstObj)->data;

    if ((srcLen & 0x3) != 0) return -1;
    if (srcLen <= 0) return 0;

    int pad = 0;
    if (src[srcOffset + srcLen - 1] == '=') {
        pad++;
        if (srcLen > 1 && src[srcOffset + srcLen - 2] == '=') {
            pad++;
        }
    }
    if (pad > 2) return -1;

    int outLength = (srcLen / 4) * 3 - pad;
    if (outLength <= 0) return 0;

    int si = srcOffset;
    int di = dstOffset;
    int fullLen = srcLen - (pad > 0 ? 4 : 0);
    int fullEnd = srcOffset + fullLen;

    /* NEON fast path: process 64 input bytes -> 48 output bytes per iteration */
    int neonEnd = fullEnd - 64 + 1;
    while (si <= neonEnd - 1) {
        /* Load 64 bytes and de-interleave into 4 registers of 16 */
        uint8x16x4_t in4 = vld4q_u8((const uint8_t*)(src + si));

        /* Decode each character to its 6-bit value */
        uint8x16_t d0 = neon_base64_decode_lut(in4.val[0]);
        uint8x16_t d1 = neon_base64_decode_lut(in4.val[1]);
        uint8x16_t d2 = neon_base64_decode_lut(in4.val[2]);
        uint8x16_t d3 = neon_base64_decode_lut(in4.val[3]);

        /* Check for invalid characters */
        uint8x16_t anyInvalid = vorrq_u8(vorrq_u8(d0, d1), vorrq_u8(d2, d3));
        if (neon_has_invalid(anyInvalid)) {
            return -1;
        }

        /* Combine 4 x 6-bit values into 3 bytes */
        uint8x16_t o0 = vorrq_u8(vshlq_n_u8(d0, 2), vshrq_n_u8(d1, 4));
        uint8x16_t o1 = vorrq_u8(vshlq_n_u8(d1, 4), vshrq_n_u8(d2, 2));
        uint8x16_t o2 = vorrq_u8(vshlq_n_u8(d2, 6), d3);

        /* Interleave and store 48 output bytes */
        uint8x16x3_t out3;
        out3.val[0] = o0;
        out3.val[1] = o1;
        out3.val[2] = o2;
        vst3q_u8((uint8_t*)(dst + di), out3);

        si += 64;
        di += 48;
    }

    /* Scalar tail for remaining complete quads */
    static const int8_t decLut[256] = {
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,62,-1,-1,-1,63,
        52,53,54,55,56,57,58,59,60,61,-1,-1,-1,-1,-1,-1,
        -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,
        15,16,17,18,19,20,21,22,23,24,25,-1,-1,-1,-1,-1,
        -1,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,
        41,42,43,44,45,46,47,48,49,50,51,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
        -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
    };
    while (si < fullEnd) {
        int v0 = decLut[(unsigned char)src[si]];
        int v1 = decLut[(unsigned char)src[si + 1]];
        int v2 = decLut[(unsigned char)src[si + 2]];
        int v3 = decLut[(unsigned char)src[si + 3]];
        if ((v0 | v1 | v2 | v3) < 0) return -1;
        int quantum = (v0 << 18) | (v1 << 12) | (v2 << 6) | v3;
        dst[di]     = (JAVA_ARRAY_BYTE)((quantum >> 16) & 0xff);
        dst[di + 1] = (JAVA_ARRAY_BYTE)((quantum >> 8) & 0xff);
        dst[di + 2] = (JAVA_ARRAY_BYTE)(quantum & 0xff);
        si += 4;
        di += 3;
    }

    /* Handle last quad with padding */
    if (pad > 0) {
        int i = srcOffset + srcLen - 4;
        int v0 = decLut[(unsigned char)src[i]];
        int v1 = decLut[(unsigned char)src[i + 1]];
        if ((v0 | v1) < 0) return -1;
        dst[di++] = (JAVA_ARRAY_BYTE)((v0 << 2) | (v1 >> 4));
        if (pad == 2) {
            return (src[i + 2] == '=' && src[i + 3] == '=') ? outLength : -1;
        }
        if (src[i + 3] != '=') return -1;
        int v2 = decLut[(unsigned char)src[i + 2]];
        if (v2 < 0) return -1;
        dst[di] = (JAVA_ARRAY_BYTE)((v1 << 4) | (v2 >> 2));
    }

    return outLength;
}
