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
 * The native half of the iOS device runtime.
 *
 * ParparVM has no reflection, so an interpreter cannot ask a class for a method
 * by name. What an interp-host build provides instead is a per-method invoke
 * thunk registered under a numeric id (see ByteCodeClass's
 * appendOnDeviceDebugInvokeThunks) and a symbol table mapping JVM names and
 * descriptors to those ids. This file is the bridge: it hands the symbol table
 * to Java, and dispatches a thunk once Java has resolved an id.
 *
 * Everything here is inert without CN1_INTERP_HOST -- the thunks simply are not
 * in the binary -- so each entry point degrades to "unsupported" rather than
 * failing at the first call.
 */

#include "cn1_globals.h"
#include "cn1_reflect.h"
#include <stdatomic.h>
#include "java_lang_String.h"
/* The exceptions this file raises. Each __NEW_INSTANCE_ constructor is declared
   in its own generated header, and clang treats a missing declaration as an
   error rather than an implicit int-returning function. */
#include "java_lang_IllegalArgumentException.h"
#include "java_lang_NegativeArraySizeException.h"
#include "java_lang_NoSuchFieldError.h"
#include "java_lang_NullPointerException.h"
#include "java_lang_UnsupportedOperationException.h"
#import <Foundation/Foundation.h>

#ifdef CN1_ON_DEVICE_DEBUG
#include "cn1_debugger.h"
extern const unsigned char* cn1_debug_symbols_data(void);
extern int cn1_debug_symbols_length(void);
#endif

/* Declared in cn1_reflect.h; the strong definitions live with the registries
   in cn1_debugger.m / cn1_debugger_objects.c. */
extern JAVA_OBJECT fromNSString(CODENAME_ONE_THREAD_STATE, NSString* str);

#include <string.h>
#include <stdlib.h>

/* Kind codes, matching com.codename1.impl.interp.InterpOpcodes. */
#define K_VOID    0
#define K_INT     1
#define K_LONG    2
#define K_FLOAT   3
#define K_DOUBLE  4
#define K_OBJECT  5
#define K_BOOLEAN 6
#define K_BYTE    7
#define K_CHAR    8
#define K_SHORT   9

JAVA_BOOLEAN com_codename1_impl_ios_InterpIOSNative_isInterpHostBuild___R_boolean(
        CODENAME_ONE_THREAD_STATE) {
#if defined(CN1_INTERP_HOST) && defined(CN1_ON_DEVICE_DEBUG)
    return JAVA_TRUE;
#else
    return JAVA_FALSE;
#endif
}

/*
 * The symbol table is linked in gzip-compressed, because it is large and highly
 * repetitive. ParparVM's java.util has no zip package, so it is inflated here
 * and handed over as text: the alternative would be shipping an inflater in
 * Java purely for this.
 */
JAVA_OBJECT com_codename1_impl_ios_InterpIOSNative_symbolTable___R_java_lang_String(
        CODENAME_ONE_THREAD_STATE) {
#if defined(CN1_INTERP_HOST) && defined(CN1_ON_DEVICE_DEBUG)
    const unsigned char* gz = cn1_debug_symbols_data();
    int gzLen = cn1_debug_symbols_length();
    if (gz == NULL || gzLen <= 0) {
        return newStringFromCString(threadStateData, "");
    }
    /* The sidecar is written by java.util.zip.GZIPOutputStream, so it is a gzip
       stream. NSDataCompressionAlgorithmZlib is, despite the name, raw DEFLATE
       with no header of any kind -- handing it the gzip bytes fails, and fails
       silently by returning nil, which presents as "this build has no
       interpreter bindings" on an app that plainly has them.

       So strip the framing: a fixed 10-byte header, then whichever optional
       fields FLG announces, and an 8-byte CRC/length trailer at the end. Java
       sets no optional flags today; the parsing is here so that a future
       toolchain that does set one does not reintroduce a silent nil. */
    int deflateStart = 10;
    int deflateEnd = gzLen - 8;
    if (gzLen < 18 || gz[0] != 0x1f || gz[1] != 0x8b || gz[2] != 8) {
        return newStringFromCString(threadStateData, "");
    }
    unsigned char flg = gz[3];
    if (flg & 0x04) {                       /* FEXTRA */
        if (deflateStart + 2 > deflateEnd) {
            return newStringFromCString(threadStateData, "");
        }
        int xlen = gz[deflateStart] | (gz[deflateStart + 1] << 8);
        deflateStart += 2 + xlen;
    }
    if (flg & 0x08) {                       /* FNAME, NUL terminated */
        while (deflateStart < deflateEnd && gz[deflateStart] != 0) deflateStart++;
        deflateStart++;
    }
    if (flg & 0x10) {                       /* FCOMMENT, NUL terminated */
        while (deflateStart < deflateEnd && gz[deflateStart] != 0) deflateStart++;
        deflateStart++;
    }
    if (flg & 0x02) {                       /* FHCRC */
        deflateStart += 2;
    }
    if (deflateStart >= deflateEnd) {
        return newStringFromCString(threadStateData, "");
    }
    NSData* compressed = [NSData dataWithBytes:gz + deflateStart
                                        length:deflateEnd - deflateStart];
    NSData* plain = nil;
    if (@available(iOS 13.0, *)) {
        NSError* err = nil;
        plain = [compressed decompressedDataUsingAlgorithm:NSDataCompressionAlgorithmZlib
                                                     error:&err];
        if (err != nil) {
            plain = nil;
        }
    }
    if (plain == nil) {
        return newStringFromCString(threadStateData, "");
    }
    NSString* text = [[NSString alloc] initWithData:plain encoding:NSUTF8StringEncoding];
    if (text == nil) {
        return newStringFromCString(threadStateData, "");
    }
    return fromNSString(threadStateData, text);
#else
    return newStringFromCString(threadStateData, "");
#endif
}

JAVA_OBJECT com_codename1_impl_ios_InterpIOSNative_invokeById___int_java_lang_Object_long_1ARRAY_java_lang_Object_1ARRAY_int_1ARRAY_int_int_long_1ARRAY_R_java_lang_Object(
        CODENAME_ONE_THREAD_STATE, JAVA_INT methodId, JAVA_OBJECT target,
        JAVA_OBJECT prims, JAVA_OBJECT objs, JAVA_OBJECT kinds,
        JAVA_INT argCount, JAVA_INT returnKind, JAVA_OBJECT resultOut) {
#if defined(CN1_INTERP_HOST) && defined(CN1_ON_DEVICE_DEBUG)
    cn1_invoke_thunk_t thunk = cn1_reflect_thunk_for_method(methodId);
    if (thunk == NULL) {
        throwException(threadStateData,
                __NEW_INSTANCE_java_lang_UnsupportedOperationException(threadStateData));
        return JAVA_NULL;
    }
    if (argCount < 0 || argCount > 32) {
        throwException(threadStateData,
                __NEW_INSTANCE_java_lang_IllegalArgumentException(threadStateData));
        return JAVA_NULL;
    }

    cn1_invoke_arg argv[32];
    memset(argv, 0, sizeof(argv));
    JAVA_ARRAY primArray = (JAVA_ARRAY)prims;
    JAVA_ARRAY objArray = (JAVA_ARRAY)objs;
    JAVA_ARRAY kindArray = (JAVA_ARRAY)kinds;
    JAVA_ARRAY_LONG* primData = primArray == JAVA_NULL
            ? NULL : (JAVA_ARRAY_LONG*)primArray->data;
    JAVA_ARRAY_OBJECT* objData = objArray == JAVA_NULL
            ? NULL : (JAVA_ARRAY_OBJECT*)objArray->data;
    JAVA_ARRAY_INT* kindData = kindArray == JAVA_NULL
            ? NULL : (JAVA_ARRAY_INT*)kindArray->data;

    for (int i = 0; i < argCount; i++) {
        int k = kindData == NULL ? K_OBJECT : (int)kindData[i];
        JAVA_LONG raw = primData == NULL ? 0 : (JAVA_LONG)primData[i];
        switch (k) {
            case K_OBJECT:
                argv[i].o = objData == NULL ? JAVA_NULL : (JAVA_OBJECT)objData[i];
                break;
            case K_LONG:
                argv[i].j = raw;
                break;
            case K_FLOAT: {
                /* Floats travel as their raw int bits, so the value survives
                   the trip through a long slot unchanged. */
                uint32_t bits = (uint32_t)raw;
                memcpy(&argv[i].f, &bits, 4);
                break;
            }
            case K_DOUBLE: {
                uint64_t bits = (uint64_t)raw;
                memcpy(&argv[i].d, &bits, 8);
                break;
            }
            default:
                argv[i].i = (JAVA_INT)raw;
                break;
        }
    }

    cn1_invoke_result result;
    memset(&result, 0, sizeof(result));
    thunk(threadStateData, target, argv, &result);

    if (result.type == 'X') {
        /* The callee threw. Re-raise it on this thread so the interpreter's
           exception table -- and any Java catch above it -- sees a real
           throwable rather than a silently swallowed failure. */
        throwException(threadStateData, result.value.o);
        return JAVA_NULL;
    }

    if (returnKind == K_OBJECT) {
        return result.value.o;
    }
    if (resultOut != JAVA_NULL) {
        JAVA_ARRAY out = (JAVA_ARRAY)resultOut;
        JAVA_ARRAY_LONG* outData = (JAVA_ARRAY_LONG*)out->data;
        switch (returnKind) {
            case K_LONG:
                outData[0] = (JAVA_ARRAY_LONG)result.value.j;
                break;
            case K_FLOAT: {
                uint32_t bits;
                memcpy(&bits, &result.value.f, 4);
                outData[0] = (JAVA_ARRAY_LONG)bits;
                break;
            }
            case K_DOUBLE: {
                uint64_t bits;
                memcpy(&bits, &result.value.d, 8);
                outData[0] = (JAVA_ARRAY_LONG)bits;
                break;
            }
            case K_VOID:
                outData[0] = 0;
                break;
            default:
                outData[0] = (JAVA_ARRAY_LONG)result.value.i;
                break;
        }
    }
    return JAVA_NULL;
#else
    throwException(threadStateData,
            __NEW_INSTANCE_java_lang_UnsupportedOperationException(threadStateData));
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_InterpIOSNative_getFieldById___int_java_lang_Object_int_long_1ARRAY_R_java_lang_Object(
        CODENAME_ONE_THREAD_STATE, JAVA_INT fieldId, JAVA_OBJECT target,
        JAVA_INT kind, JAVA_OBJECT resultOut) {
#if defined(CN1_INTERP_HOST) && defined(CN1_ON_DEVICE_DEBUG)
    if (target == JAVA_NULL) {
        throwException(threadStateData,
                __NEW_INSTANCE_java_lang_NullPointerException(threadStateData));
        return JAVA_NULL;
    }
    const cn1_field_entry* entry = cn1_reflect_field_for(
            target->__codenameOneParentClsReference->classId, fieldId);
    if (entry == NULL) {
        throwException(threadStateData,
                __NEW_INSTANCE_java_lang_NoSuchFieldError(threadStateData));
        return JAVA_NULL;
    }
    char* base = (char*)target;
    void* slot = base + entry->offset;
    // A `volatile` field's backing storage is declared `_Atomic` by the
    // translator, and host reads through the generated accessor use
    // atomic_load_explicit with memory_order_acquire. A plain dereference
    // here would race with a host write on another thread, and clang is
    // free to tear or reorder the read. Match the accessor's ordering so
    // interpreted reads see the same happens-before the host does.
    JAVA_BOOLEAN vol = entry->isVolatile ? JAVA_TRUE : JAVA_FALSE;
    if (kind == K_OBJECT) {
        if (vol) {
            return atomic_load_explicit((_Atomic(JAVA_OBJECT)*)slot,
                    memory_order_acquire);
        }
        return *(JAVA_OBJECT*)slot;
    }
    if (resultOut != JAVA_NULL) {
        JAVA_ARRAY out = (JAVA_ARRAY)resultOut;
        JAVA_ARRAY_LONG* outData = (JAVA_ARRAY_LONG*)out->data;
        switch (entry->type) {
            case 'J':
                outData[0] = vol
                        ? (JAVA_ARRAY_LONG)atomic_load_explicit(
                                (_Atomic(JAVA_LONG)*)slot, memory_order_acquire)
                        : (JAVA_ARRAY_LONG)(*(JAVA_LONG*)slot);
                break;
            case 'D': {
                uint64_t bits;
                if (vol) {
                    // The storage is _Atomic(JAVA_DOUBLE); casting through
                    // _Atomic(uint64_t)* is an incompatible atomic object
                    // type under C aliasing and clang is free to miscompile
                    // it. Load the declared floating type, then memcpy to
                    // the raw bits the caller wants.
                    JAVA_DOUBLE value = atomic_load_explicit(
                            (_Atomic(JAVA_DOUBLE)*)slot, memory_order_acquire);
                    memcpy(&bits, &value, 8);
                } else {
                    memcpy(&bits, slot, 8);
                }
                outData[0] = (JAVA_ARRAY_LONG)bits;
                break;
            }
            case 'F': {
                uint32_t bits;
                if (vol) {
                    JAVA_FLOAT value = atomic_load_explicit(
                            (_Atomic(JAVA_FLOAT)*)slot, memory_order_acquire);
                    memcpy(&bits, &value, 4);
                } else {
                    memcpy(&bits, slot, 4);
                }
                outData[0] = (JAVA_ARRAY_LONG)bits;
                break;
            }
            case 'Z': case 'B':
                outData[0] = vol
                        ? (JAVA_ARRAY_LONG)atomic_load_explicit(
                                (_Atomic(JAVA_BYTE)*)slot, memory_order_acquire)
                        : (JAVA_ARRAY_LONG)(*(JAVA_BYTE*)slot);
                break;
            case 'C':
                outData[0] = vol
                        ? (JAVA_ARRAY_LONG)atomic_load_explicit(
                                (_Atomic(JAVA_CHAR)*)slot, memory_order_acquire)
                        : (JAVA_ARRAY_LONG)(*(JAVA_CHAR*)slot);
                break;
            case 'S':
                outData[0] = vol
                        ? (JAVA_ARRAY_LONG)atomic_load_explicit(
                                (_Atomic(JAVA_SHORT)*)slot, memory_order_acquire)
                        : (JAVA_ARRAY_LONG)(*(JAVA_SHORT*)slot);
                break;
            default:
                outData[0] = vol
                        ? (JAVA_ARRAY_LONG)atomic_load_explicit(
                                (_Atomic(JAVA_INT)*)slot, memory_order_acquire)
                        : (JAVA_ARRAY_LONG)(*(JAVA_INT*)slot);
                break;
        }
    }
    return JAVA_NULL;
#else
    throwException(threadStateData,
            __NEW_INSTANCE_java_lang_UnsupportedOperationException(threadStateData));
    return JAVA_NULL;
#endif
}

/*
 * Static fields.
 *
 * An instance field is an offset from a receiver, which is what the field table
 * above records. A static has no receiver: the translator gives it a named C
 * global plus typed accessor functions, so there is nothing to index by offset.
 * Under interp-host it also emits one uniform wrapper per static, registered by
 * fieldId, and these two entry points dispatch through it.
 *
 * Reading through the generated getter rather than the global is what makes the
 * class's static initializer run first, so an interpreted GETSTATIC initialises
 * the class exactly as compiled code would.
 */
JAVA_OBJECT com_codename1_impl_ios_InterpIOSNative_getStaticById___int_int_long_1ARRAY_R_java_lang_Object(
        CODENAME_ONE_THREAD_STATE, JAVA_INT fieldId, JAVA_INT kind, JAVA_OBJECT resultOut) {
#if defined(CN1_INTERP_HOST) && defined(CN1_ON_DEVICE_DEBUG)
    cn1_static_accessor_t acc = cn1_reflect_static_accessor_for(fieldId);
    if (acc == NULL) {
        throwException(threadStateData,
                __NEW_INSTANCE_java_lang_NoSuchFieldError(threadStateData));
        return JAVA_NULL;
    }
    cn1_invoke_arg value;
    memset(&value, 0, sizeof(value));
    char type = 'V';
    acc(threadStateData, 0, &value, &type);
    if (kind == K_OBJECT) {
        return value.o;
    }
    if (resultOut != JAVA_NULL) {
        JAVA_ARRAY out = (JAVA_ARRAY)resultOut;
        JAVA_ARRAY_LONG* outData = (JAVA_ARRAY_LONG*)out->data;
        switch (type) {
            case 'J': outData[0] = (JAVA_ARRAY_LONG)value.j; break;
            case 'D': {
                uint64_t bits;
                memcpy(&bits, &value.d, 8);
                outData[0] = (JAVA_ARRAY_LONG)bits;
                break;
            }
            case 'F': {
                uint32_t bits;
                memcpy(&bits, &value.f, 4);
                outData[0] = (JAVA_ARRAY_LONG)bits;
                break;
            }
            default: outData[0] = (JAVA_ARRAY_LONG)value.i; break;
        }
    }
    return JAVA_NULL;
#else
    throwException(threadStateData,
            __NEW_INSTANCE_java_lang_UnsupportedOperationException(threadStateData));
    return JAVA_NULL;
#endif
}

JAVA_VOID com_codename1_impl_ios_InterpIOSNative_setStaticById___int_int_long_java_lang_Object(
        CODENAME_ONE_THREAD_STATE, JAVA_INT fieldId, JAVA_INT kind,
        JAVA_LONG rawValue, JAVA_OBJECT refValue) {
#if defined(CN1_INTERP_HOST) && defined(CN1_ON_DEVICE_DEBUG)
    cn1_static_accessor_t acc = cn1_reflect_static_accessor_for(fieldId);
    if (acc == NULL) {
        throwException(threadStateData,
                __NEW_INSTANCE_java_lang_NoSuchFieldError(threadStateData));
        return;
    }
    cn1_invoke_arg value;
    memset(&value, 0, sizeof(value));
    char type = 'V';
    if (kind == K_OBJECT) {
        value.o = refValue;
    } else {
        /* The accessor reports the field's own type, so ask it first and then
           unpack the raw bits into the matching slot. A float arrives as its
           IEEE bit pattern in the low 32 bits, not as a widened double. */
        cn1_invoke_arg probe;
        memset(&probe, 0, sizeof(probe));
        acc(threadStateData, 0, &probe, &type);
        switch (type) {
            case 'J': value.j = (JAVA_LONG)rawValue; break;
            case 'D': {
                uint64_t bits = (uint64_t)rawValue;
                memcpy(&value.d, &bits, 8);
                break;
            }
            case 'F': {
                uint32_t bits = (uint32_t)rawValue;
                memcpy(&value.f, &bits, 4);
                break;
            }
            default: value.i = (JAVA_INT)rawValue; break;
        }
    }
    acc(threadStateData, 1, &value, &type);
#else
    throwException(threadStateData,
            __NEW_INSTANCE_java_lang_UnsupportedOperationException(threadStateData));
#endif
}

JAVA_VOID com_codename1_impl_ios_InterpIOSNative_setFieldById___int_java_lang_Object_int_long_java_lang_Object(
        CODENAME_ONE_THREAD_STATE, JAVA_INT fieldId, JAVA_OBJECT target,
        JAVA_INT kind, JAVA_LONG rawValue, JAVA_OBJECT refValue) {
#if defined(CN1_INTERP_HOST) && defined(CN1_ON_DEVICE_DEBUG)
    if (target == JAVA_NULL) {
        throwException(threadStateData,
                __NEW_INSTANCE_java_lang_NullPointerException(threadStateData));
        return;
    }
    const cn1_field_entry* entry = cn1_reflect_field_for(
            target->__codenameOneParentClsReference->classId, fieldId);
    if (entry == NULL) {
        throwException(threadStateData,
                __NEW_INSTANCE_java_lang_NoSuchFieldError(threadStateData));
        return;
    }
    char* base = (char*)target;
    void* slot = base + entry->offset;
    // Volatile fields need atomic_store_explicit with memory_order_release
    // -- the pair to the acquire load in the getter above -- so a host
    // thread reading the same volatile through its generated accessor
    // sees the happens-before this write establishes. The reference
    // write barriers still fire either way: they are about GC bookkeeping,
    // not memory ordering.
    JAVA_BOOLEAN vol = entry->isVolatile ? JAVA_TRUE : JAVA_FALSE;
    if (kind == K_OBJECT) {
        /* An interpreted PUTFIELD writes into a compiled object, so it owes the
           collector exactly what the translator's generated reference setter
           pays -- see the set_field_ emission in ByteCodeClass.generateCCode,
           which writes these two in this order ahead of the store.

           CN1_WRITE_BARRIER promotes a nursery value that is escaping into the
           heap; without it the nursery frees an object the host object still
           points at. CN1_SATB_DELETE hands the collector the reference being
           overwritten, so a reference that was in the start-of-cycle snapshot
           survives a mark that has already scanned this thread. Skipping either
           is silent: the store succeeds and the damage appears a cycle later, in
           a mark walking a field that no longer points at a live object.

           Both are single predicted-not-taken flag loads outside a GC, and the
           interpreter is on-device-debug only, so there is nothing to weigh
           here against correctness. */
        CN1_WRITE_BARRIER(target, refValue);
        CN1_SATB_DELETE(slot);
        if (vol) {
            atomic_store_explicit((_Atomic(JAVA_OBJECT)*)slot, refValue,
                    memory_order_release);
        } else {
            *(JAVA_OBJECT*)slot = refValue;
        }
        return;
    }
    switch (entry->type) {
        case 'J':
            if (vol) {
                atomic_store_explicit((_Atomic(JAVA_LONG)*)slot,
                        (JAVA_LONG)rawValue, memory_order_release);
            } else {
                *(JAVA_LONG*)slot = (JAVA_LONG)rawValue;
            }
            break;
        case 'D': {
            uint64_t bits = (uint64_t)rawValue;
            if (vol) {
                // The storage is _Atomic(JAVA_DOUBLE); storing through
                // _Atomic(uint64_t)* is an incompatible atomic object type
                // under C aliasing. Round-trip through the declared
                // floating type instead.
                JAVA_DOUBLE value;
                memcpy(&value, &bits, 8);
                atomic_store_explicit((_Atomic(JAVA_DOUBLE)*)slot, value,
                        memory_order_release);
            } else {
                memcpy(slot, &bits, 8);
            }
            break;
        }
        case 'F': {
            uint32_t bits = (uint32_t)rawValue;
            if (vol) {
                JAVA_FLOAT value;
                memcpy(&value, &bits, 4);
                atomic_store_explicit((_Atomic(JAVA_FLOAT)*)slot, value,
                        memory_order_release);
            } else {
                memcpy(slot, &bits, 4);
            }
            break;
        }
        case 'Z': case 'B':
            if (vol) {
                atomic_store_explicit((_Atomic(JAVA_BYTE)*)slot,
                        (JAVA_BYTE)rawValue, memory_order_release);
            } else {
                *(JAVA_BYTE*)slot = (JAVA_BYTE)rawValue;
            }
            break;
        case 'C':
            if (vol) {
                atomic_store_explicit((_Atomic(JAVA_CHAR)*)slot,
                        (JAVA_CHAR)rawValue, memory_order_release);
            } else {
                *(JAVA_CHAR*)slot = (JAVA_CHAR)rawValue;
            }
            break;
        case 'S':
            if (vol) {
                atomic_store_explicit((_Atomic(JAVA_SHORT)*)slot,
                        (JAVA_SHORT)rawValue, memory_order_release);
            } else {
                *(JAVA_SHORT*)slot = (JAVA_SHORT)rawValue;
            }
            break;
        default:
            if (vol) {
                atomic_store_explicit((_Atomic(JAVA_INT)*)slot,
                        (JAVA_INT)rawValue, memory_order_release);
            } else {
                *(JAVA_INT*)slot = (JAVA_INT)rawValue;
            }
            break;
    }
#else
    throwException(threadStateData,
            __NEW_INSTANCE_java_lang_UnsupportedOperationException(threadStateData));
#endif
}

JAVA_BOOLEAN com_codename1_impl_ios_InterpIOSNative_isInstanceOfId___int_java_lang_Object_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_INT classId, JAVA_OBJECT value) {
    if (value == JAVA_NULL || classId < 0) {
        return JAVA_FALSE;
    }
    return instanceofFunction(classId, value->__codenameOneParentClsReference->classId)
            ? JAVA_TRUE : JAVA_FALSE;
}

/// The class id of an object's actual class.
///
/// Virtual dispatch needs it. A call site names the type it was compiled
/// against -- java.util.List for `list.add(x)` -- and invoking the method that
/// name resolves to would run AbstractList's, which throws. The receiver's own
/// class is the only thing that says which override to run.
JAVA_INT com_codename1_impl_ios_InterpIOSNative_classIdOf___java_lang_Object_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT value) {
    if (value == JAVA_NULL) {
        return -1;
    }
    return value->__codenameOneParentClsReference->classId;
}

JAVA_OBJECT com_codename1_impl_ios_InterpIOSNative_classObjectById___int_R_java_lang_Object(
        CODENAME_ONE_THREAD_STATE, JAVA_INT classId) {
#if defined(CN1_INTERP_HOST) && defined(CN1_ON_DEVICE_DEBUG)
    struct clazz* c = cn1_reflect_clazz_for(classId);
    if (c == NULL) {
        return JAVA_NULL;
    }
    return (JAVA_OBJECT)c;
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_InterpIOSNative_newObjectArray___int_int_R_java_lang_Object(
        CODENAME_ONE_THREAD_STATE, JAVA_INT arrayClassId, JAVA_INT length) {
    if (length < 0) {
        throwException(threadStateData,
                __NEW_INSTANCE_java_lang_NegativeArraySizeException(threadStateData));
        return JAVA_NULL;
    }
    /* The array's own clazz when the build registered one, so a host array
       carries its real type: `(String[]) value` is a checkcast against the
       array class, and an Object[] fails it however its elements look. Rank 1
       to 3 of every class is registered by the interp-host build; anything
       else -- deeper ranks, or an array of a class only the bundle has -- gets
       the Object[] the interpreter uses for its own arrays anyway. */
    struct clazz* arrayClass = cn1_reflect_clazz_for(arrayClassId);
    if (arrayClass == NULL) {
        arrayClass = (struct clazz*)&class_array1__java_lang_Object;
    }
    return allocArray(threadStateData, length, arrayClass, sizeof(JAVA_OBJECT), 1);
}

JAVA_OBJECT com_codename1_impl_ios_InterpIOSNative_newArrayLike___java_lang_Object_int_R_java_lang_Object(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT source, JAVA_INT length) {
    if (source == JAVA_NULL || length < 0) {
        return JAVA_NULL;
    }
    /* An empty array shaped exactly like the source: its own clazz, its own
       dimension count and its own element size. Cloning a host reference array
       through the generic path produced a plain Object[], and a String[] that
       has become an Object[] fails the next checkcast and cannot be handed to
       a method declaring String[] -- so a clone silently broke the value it
       was copying. Taking the clazz from the object needs no registry lookup
       and is right for ranks and component types no table anticipated. */
    struct clazz* arrayClass = source->__codenameOneParentClsReference;
    if (arrayClass == NULL) {
        return JAVA_NULL;
    }
    JAVA_ARRAY src = (JAVA_ARRAY)source;
    return allocArray(threadStateData, length, arrayClass, src->primitiveSize,
                      src->dimensions);
}

/*
 * The class-initializer registry.
 *
 * ParparVM initializes a class on first entry into one of its methods and from
 * the generated static-field accessors. Neither is something the device runtime
 * can reach for a class that declares no static field and whose methods it has
 * no reason to call -- and it has to initialize a host superclass before an
 * interpreted subclass's own initializer runs, or the parent's static state is
 * built after the child's.
 *
 * So the interp-host build registers every class's __STATIC_INITIALIZER_ here,
 * from the same __attribute__((constructor)) that publishes its fields, and the
 * runtime asks for one by class id. The generated function is idempotent, so a
 * request for a class that is already initialized costs a comparison.
 */
static cn1_class_init_t* g_classInits = NULL;
static int g_classInitCap = 0;
static pthread_mutex_t g_classInitMutex = PTHREAD_MUTEX_INITIALIZER;

void cn1_register_class_initializer(int classId, cn1_class_init_t fn) {
    if (classId < 0 || fn == NULL) {
        return;
    }
    pthread_mutex_lock(&g_classInitMutex);
    if (classId >= g_classInitCap) {
        int newCap = g_classInitCap == 0 ? 1024 : g_classInitCap * 2;
        while (classId >= newCap) {
            newCap *= 2;
        }
        cn1_class_init_t* n = (cn1_class_init_t*)realloc(
                g_classInits, newCap * sizeof(cn1_class_init_t));
        if (!n) {
            pthread_mutex_unlock(&g_classInitMutex);
            return;
        }
        memset(n + g_classInitCap, 0,
               (newCap - g_classInitCap) * sizeof(cn1_class_init_t));
        g_classInits = n;
        g_classInitCap = newCap;
    }
    g_classInits[classId] = fn;
    pthread_mutex_unlock(&g_classInitMutex);
}

JAVA_VOID com_codename1_impl_ios_InterpIOSNative_initializeClassById___int(
        CODENAME_ONE_THREAD_STATE, JAVA_INT classId) {
    cn1_class_init_t fn = NULL;
    pthread_mutex_lock(&g_classInitMutex);
    if (classId >= 0 && classId < g_classInitCap) {
        fn = g_classInits[classId];
    }
    pthread_mutex_unlock(&g_classInitMutex);
    if (fn != NULL) {
        /* Outside the lock: the initializer runs arbitrary Java, which may
           initialize another class and re-enter this. */
        fn(threadStateData);
    }
}
