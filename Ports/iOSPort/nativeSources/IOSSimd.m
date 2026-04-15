#include "xmlvm.h"
#include <stdint.h>
#include <string.h>
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

static void cn1_store_u8x16_to_ints(uint8x16_t v, JAVA_ARRAY_INT* d, int offset) {
    uint16x8_t lo16 = vmovl_u8(vget_low_u8(v));
    uint16x8_t hi16 = vmovl_u8(vget_high_u8(v));
    uint32x4_t x0 = vmovl_u16(vget_low_u16(lo16));
    uint32x4_t x1 = vmovl_u16(vget_high_u16(lo16));
    uint32x4_t x2 = vmovl_u16(vget_low_u16(hi16));
    uint32x4_t x3 = vmovl_u16(vget_high_u16(hi16));
    vst1q_s32((int32_t*)(d + offset), vreinterpretq_s32_u32(x0));
    vst1q_s32((int32_t*)(d + offset + 4), vreinterpretq_s32_u32(x1));
    vst1q_s32((int32_t*)(d + offset + 8), vreinterpretq_s32_u32(x2));
    vst1q_s32((int32_t*)(d + offset + 12), vreinterpretq_s32_u32(x3));
}

static uint8x16_t cn1_load_ints_to_u8x16(JAVA_ARRAY_INT* s, int offset) {
    int16x8_t lo16 = vcombine_s16(
            vmovn_s32(vld1q_s32((int32_t*)(s + offset))),
            vmovn_s32(vld1q_s32((int32_t*)(s + offset + 4))));
    int16x8_t hi16 = vcombine_s16(
            vmovn_s32(vld1q_s32((int32_t*)(s + offset + 8))),
            vmovn_s32(vld1q_s32((int32_t*)(s + offset + 12))));
    int8x16_t out = vcombine_s8(vmovn_s16(lo16), vmovn_s16(hi16));
    return vreinterpretq_u8_s8(out);
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

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpEq___byte_1ARRAY_byte_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_BYTE value, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int i = offset;
    int end = offset + length;
    int8x16_t vv = vdupq_n_s8((int8_t)value);
    for (; i <= end - 16; i += 16) {
        int8x16_t vs = vld1q_s8((int8_t*)(s + i));
        vst1q_s8((int8_t*)(m + i), vreinterpretq_s8_u8(vceqq_s8(vs, vv)));
    }
    for (; i < end; i++) {
        m[i] = s[i] == value ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
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

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpLt___byte_1ARRAY_byte_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_BYTE value, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int i = offset;
    int end = offset + length;
    int8x16_t vv = vdupq_n_s8((int8_t)value);
    for (; i <= end - 16; i += 16) {
        int8x16_t vs = vld1q_s8((int8_t*)(s + i));
        vst1q_s8((int8_t*)(m + i), vreinterpretq_s8_u8(vcltq_s8(vs, vv)));
    }
    for (; i < end; i++) {
        m[i] = s[i] < value ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
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

JAVA_VOID com_codename1_impl_ios_IOSSimd_packIntToByteTruncateInterleaved4___int_1ARRAY_int_int_int_int_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT src0Offset, JAVA_INT src1Offset, JAVA_INT src2Offset, JAVA_INT src3Offset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* s = (JAVA_ARRAY_INT*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = 0;
    for (; i <= length - 16; i += 16) {
        uint8x16x4_t v;
        v.val[0] = cn1_load_ints_to_u8x16(s, src0Offset + i);
        v.val[1] = cn1_load_ints_to_u8x16(s, src1Offset + i);
        v.val[2] = cn1_load_ints_to_u8x16(s, src2Offset + i);
        v.val[3] = cn1_load_ints_to_u8x16(s, src3Offset + i);
        vst4q_u8((uint8_t*)(d + dstOffset + i * 4), v);
    }
    for (; i < length; i++) {
        int dstIndex = dstOffset + i * 4;
        d[dstIndex] = (JAVA_ARRAY_BYTE)s[src0Offset + i];
        d[dstIndex + 1] = (JAVA_ARRAY_BYTE)s[src1Offset + i];
        d[dstIndex + 2] = (JAVA_ARRAY_BYTE)s[src2Offset + i];
        d[dstIndex + 3] = (JAVA_ARRAY_BYTE)s[src3Offset + i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_permuteBytes___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_OBJECT indices, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    int srcLen = ((JAVA_ARRAY)src)->length;
    JAVA_ARRAY_BYTE* idx = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)indices)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
#if defined(__aarch64__)
    if (srcLen <= 16) {
        uint8x16_t table = vld1q_u8((uint8_t*)s);
        for (; i <= end - 16; i += 16) {
            int8x16_t rawIdx = vld1q_s8((int8_t*)(idx + i));
            uint8x16_t valid = vcgeq_s8(rawIdx, vdupq_n_s8(0));
            uint8x16_t selected = vqtbl1q_u8(table, vreinterpretq_u8_s8(rawIdx));
            vst1q_u8((uint8_t*)(d + i), vandq_u8(selected, valid));
        }
    }
#endif
    for (; i < end; i++) {
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
    int i = offset;
    int end = offset + length;
    for (; i <= end - 8; i += 8) {
        uint16x4_t lo16 = vmovn_u32(vceqq_s32(vld1q_s32((int32_t*)(a + i)), vld1q_s32((int32_t*)(b + i))));
        uint16x4_t hi16 = vmovn_u32(vceqq_s32(vld1q_s32((int32_t*)(a + i + 4)), vld1q_s32((int32_t*)(b + i + 4))));
        vst1_u8((uint8_t*)(m + i), vmovn_u16(vcombine_u16(lo16, hi16)));
    }
    for (; i < end; i++) {
        m[i] = a[i] == b[i] ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpLt___int_1ARRAY_int_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dstMask, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 8; i += 8) {
        uint16x4_t lo16 = vmovn_u32(vreinterpretq_u32_s32(vcltq_s32(vld1q_s32((int32_t*)(a + i)), vld1q_s32((int32_t*)(b + i)))));
        uint16x4_t hi16 = vmovn_u32(vreinterpretq_u32_s32(vcltq_s32(vld1q_s32((int32_t*)(a + i + 4)), vld1q_s32((int32_t*)(b + i + 4)))));
        vst1_u8((uint8_t*)(m + i), vmovn_u16(vcombine_u16(lo16, hi16)));
    }
    for (; i < end; i++) {
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
    int i = offset;
    int end = offset + length;
    for (; i <= end - 4; i += 4) {
        uint32_t packedMask;
        memcpy(&packedMask, m + i, sizeof(packedMask));
        uint8x8_t maskBytes = vreinterpret_u8_u32(vdup_n_u32(packedMask));
        uint32x4_t vm = vcgtq_u32(vmovl_u16(vget_low_u16(vmovl_u8(maskBytes))), vdupq_n_u32(0));
        uint32x4_t vt = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(t + i)));
        uint32x4_t vf = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(f + i)));
        vst1q_s32((int32_t*)(d + i), vreinterpretq_s32_u32(vbslq_u32(vm, vt, vf)));
    }
    for (; i < end; i++) {
        d[i] = m[i] != 0 ? t[i] : f[i];
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_shl___byte_1ARRAY_int_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT bits, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int shift = bits & 7;
    int i = offset;
    int end = offset + length;
    int8x16_t vshift = vdupq_n_s8((int8_t)shift);
    for (; i <= end - 16; i += 16) {
        uint8x16_t vs = vld1q_u8((uint8_t*)(s + i));
        vst1q_u8((uint8_t*)(d + i), vshlq_u8(vs, vshift));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)(((uint8_t)s[i]) << shift);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_shrLogical___byte_1ARRAY_int_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT bits, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int shift = bits & 7;
    int i = offset;
    int end = offset + length;
    int8x16_t vneg = vdupq_n_s8((int8_t)(-shift));
    for (; i <= end - 16; i += 16) {
        uint8x16_t vs = vld1q_u8((uint8_t*)(s + i));
        vst1q_u8((uint8_t*)(d + i), vshlq_u8(vs, vneg));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)(((uint8_t)s[i]) >> shift);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_addWrapping___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        uint8x16_t va = vld1q_u8((uint8_t*)(a + i));
        uint8x16_t vb = vld1q_u8((uint8_t*)(b + i));
        vst1q_u8((uint8_t*)(d + i), vaddq_u8(va, vb));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)((uint8_t)a[i] + (uint8_t)b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_addWrapping___byte_1ARRAY_byte_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_BYTE value, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    uint8x16_t vv = vdupq_n_u8((uint8_t)value);
    for (; i <= end - 16; i += 16) {
        uint8x16_t vs = vld1q_u8((uint8_t*)(s + i));
        vst1q_u8((uint8_t*)(d + i), vaddq_u8(vs, vv));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)((uint8_t)s[i] + (uint8_t)value);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_subWrapping___byte_1ARRAY_byte_1ARRAY_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_OBJECT srcB, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* a = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_BYTE* b = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    for (; i <= end - 16; i += 16) {
        uint8x16_t va = vld1q_u8((uint8_t*)(a + i));
        uint8x16_t vb = vld1q_u8((uint8_t*)(b + i));
        vst1q_u8((uint8_t*)(d + i), vsubq_u8(va, vb));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)((uint8_t)a[i] - (uint8_t)b[i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_subWrapping___byte_1ARRAY_byte_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_BYTE value, JAVA_OBJECT dst, JAVA_INT offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_BYTE* d = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dst)->data;
    int i = offset;
    int end = offset + length;
    uint8x16_t vv = vdupq_n_u8((uint8_t)value);
    for (; i <= end - 16; i += 16) {
        uint8x16_t vs = vld1q_u8((uint8_t*)(s + i));
        vst1q_u8((uint8_t*)(d + i), vsubq_u8(vs, vv));
    }
    for (; i < end; i++) {
        d[i] = (JAVA_ARRAY_BYTE)((uint8_t)s[i] - (uint8_t)value);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_unpackUnsignedByteToInt___byte_1ARRAY_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT srcOffset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = 0;
    for (; i <= length - 16; i += 16) {
        uint8x16_t v = vld1q_u8((uint8_t*)(s + srcOffset + i));
        uint16x8_t lo16 = vmovl_u8(vget_low_u8(v));
        uint16x8_t hi16 = vmovl_u8(vget_high_u8(v));
        uint32x4_t x0 = vmovl_u16(vget_low_u16(lo16));
        uint32x4_t x1 = vmovl_u16(vget_high_u16(lo16));
        uint32x4_t x2 = vmovl_u16(vget_low_u16(hi16));
        uint32x4_t x3 = vmovl_u16(vget_high_u16(hi16));
        vst1q_s32((int32_t*)(d + dstOffset + i), vreinterpretq_s32_u32(x0));
        vst1q_s32((int32_t*)(d + dstOffset + i + 4), vreinterpretq_s32_u32(x1));
        vst1q_s32((int32_t*)(d + dstOffset + i + 8), vreinterpretq_s32_u32(x2));
        vst1q_s32((int32_t*)(d + dstOffset + i + 12), vreinterpretq_s32_u32(x3));
    }
    for (; i < length; i++) {
        d[dstOffset + i] = (JAVA_ARRAY_INT)(s[srcOffset + i] & 0xff);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_unpackUnsignedByteToIntInterleaved3___byte_1ARRAY_int_int_1ARRAY_int_int_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT src, JAVA_INT srcOffset, JAVA_OBJECT dst, JAVA_INT dst0Offset, JAVA_INT dst1Offset, JAVA_INT dst2Offset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* s = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)src)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = 0;
    for (; i <= length - 16; i += 16) {
        uint8x16x3_t v = vld3q_u8((uint8_t*)(s + srcOffset + i * 3));
        cn1_store_u8x16_to_ints(v.val[0], d, dst0Offset + i);
        cn1_store_u8x16_to_ints(v.val[1], d, dst1Offset + i);
        cn1_store_u8x16_to_ints(v.val[2], d, dst2Offset + i);
    }
    for (; i < length; i++) {
        int srcIndex = srcOffset + i * 3;
        d[dst0Offset + i] = (JAVA_ARRAY_INT)(s[srcIndex] & 0xff);
        d[dst1Offset + i] = (JAVA_ARRAY_INT)(s[srcIndex + 1] & 0xff);
        d[dst2Offset + i] = (JAVA_ARRAY_INT)(s[srcIndex + 2] & 0xff);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_add___int_1ARRAY_int_int_1ARRAY_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_INT srcAOffset, JAVA_OBJECT srcB, JAVA_INT srcBOffset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = 0;
    for (; i <= length - 4; i += 4) {
        int32x4_t va = vld1q_s32((int32_t*)(a + srcAOffset + i));
        int32x4_t vb = vld1q_s32((int32_t*)(b + srcBOffset + i));
        vst1q_s32((int32_t*)(d + dstOffset + i), vaddq_s32(va, vb));
    }
    for (; i < length; i++) {
        d[dstOffset + i] = (JAVA_ARRAY_INT)((int32_t)a[srcAOffset + i] + (int32_t)b[srcBOffset + i]);
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpEq___int_1ARRAY_int_int_1ARRAY_int_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_INT srcAOffset, JAVA_OBJECT srcB, JAVA_INT srcBOffset, JAVA_OBJECT dstMask, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int i = 0;
    int end = length;
    for (; i <= end - 8; i += 8) {
        uint16x4_t lo16 = vmovn_u32(vceqq_s32(vld1q_s32((int32_t*)(a + srcAOffset + i)), vld1q_s32((int32_t*)(b + srcBOffset + i))));
        uint16x4_t hi16 = vmovn_u32(vceqq_s32(vld1q_s32((int32_t*)(a + srcAOffset + i + 4)), vld1q_s32((int32_t*)(b + srcBOffset + i + 4))));
        vst1_u8((uint8_t*)(m + dstOffset + i), vmovn_u16(vcombine_u16(lo16, hi16)));
    }
    for (; i < end; i++) {
        m[dstOffset + i] = a[srcAOffset + i] == b[srcBOffset + i] ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_cmpLt___int_1ARRAY_int_int_1ARRAY_int_byte_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT srcA, JAVA_INT srcAOffset, JAVA_OBJECT srcB, JAVA_INT srcBOffset, JAVA_OBJECT dstMask, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_INT* a = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcA)->data;
    JAVA_ARRAY_INT* b = (JAVA_ARRAY_INT*)((JAVA_ARRAY)srcB)->data;
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)dstMask)->data;
    int i = 0;
    int end = length;
    for (; i <= end - 8; i += 8) {
        uint16x4_t lo16 = vmovn_u32(vreinterpretq_u32_s32(vcltq_s32(vld1q_s32((int32_t*)(a + srcAOffset + i)), vld1q_s32((int32_t*)(b + srcBOffset + i)))));
        uint16x4_t hi16 = vmovn_u32(vreinterpretq_u32_s32(vcltq_s32(vld1q_s32((int32_t*)(a + srcAOffset + i + 4)), vld1q_s32((int32_t*)(b + srcBOffset + i + 4)))));
        vst1_u8((uint8_t*)(m + dstOffset + i), vmovn_u16(vcombine_u16(lo16, hi16)));
    }
    for (; i < end; i++) {
        m[dstOffset + i] = a[srcAOffset + i] < b[srcBOffset + i] ? (JAVA_ARRAY_BYTE)-1 : (JAVA_ARRAY_BYTE)0;
    }
}

JAVA_VOID com_codename1_impl_ios_IOSSimd_select___byte_1ARRAY_int_int_1ARRAY_int_int_1ARRAY_int_int_1ARRAY_int_int(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT mask, JAVA_INT maskOffset, JAVA_OBJECT trueValues, JAVA_INT trueOffset, JAVA_OBJECT falseValues, JAVA_INT falseOffset, JAVA_OBJECT dst, JAVA_INT dstOffset, JAVA_INT length) {
    JAVA_ARRAY_BYTE* m = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)mask)->data;
    JAVA_ARRAY_INT* t = (JAVA_ARRAY_INT*)((JAVA_ARRAY)trueValues)->data;
    JAVA_ARRAY_INT* f = (JAVA_ARRAY_INT*)((JAVA_ARRAY)falseValues)->data;
    JAVA_ARRAY_INT* d = (JAVA_ARRAY_INT*)((JAVA_ARRAY)dst)->data;
    int i = 0;
    int end = length;
    for (; i <= end - 4; i += 4) {
        uint32_t packedMask;
        memcpy(&packedMask, m + maskOffset + i, sizeof(packedMask));
        uint8x8_t maskBytes = vreinterpret_u8_u32(vdup_n_u32(packedMask));
        uint32x4_t vm = vcgtq_u32(vmovl_u16(vget_low_u16(vmovl_u8(maskBytes))), vdupq_n_u32(0));
        uint32x4_t vt = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(t + trueOffset + i)));
        uint32x4_t vf = vreinterpretq_u32_s32(vld1q_s32((int32_t*)(f + falseOffset + i)));
        vst1q_s32((int32_t*)(d + dstOffset + i), vreinterpretq_s32_u32(vbslq_u32(vm, vt, vf)));
    }
    for (; i < end; i++) {
        d[dstOffset + i] = m[maskOffset + i] != 0 ? t[trueOffset + i] : f[falseOffset + i];
    }
}
