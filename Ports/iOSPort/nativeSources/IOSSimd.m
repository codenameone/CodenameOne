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
