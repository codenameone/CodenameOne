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
#import "CodenameOne_GLViewController.h"
#import "xmlvm.h"

#if !__has_feature(objc_arc)
#error CN1Inference.m requires ARC (-fobjc-arc)
#endif

#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV
#import <Foundation/Foundation.h>
#import "java_lang_String.h"

#if __has_include(<TFLTensorFlowLite/TFLTensorFlowLite.h>)
#import <TFLTensorFlowLite/TFLTensorFlowLite.h>
#define CN1_HAS_LITERT 1
#endif

#if defined(CN1_HAS_LITERT)
@interface CN1InferenceHandle : NSObject
@property(nonatomic, strong) TFLInterpreter *interpreter;
@property(nonatomic, strong) TFLDelegate *delegate;
@property(nonatomic, copy) NSString *modelPath;
@property(nonatomic) BOOL deleteModelOnClose;
@end

@implementation CN1InferenceHandle
@end

static NSMutableDictionary<NSNumber *, CN1InferenceHandle *> *cn1InferenceHandles;
static int cn1InferenceNextHandle = 1;

static void cn1InferenceEnsureHandles(void) {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        cn1InferenceHandles = [NSMutableDictionary dictionary];
    });
}

static NSString *cn1InferenceJSON(NSDictionary *value) {
    NSError *error = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:value options:0
                                                     error:&error];
    if (data == nil) {
        return [NSString stringWithFormat:@"{\"error\":\"%@\"}",
                error.localizedDescription ?: @"Could not encode inference result"];
    }
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

static NSString *cn1InferenceError(NSError *error, NSString *fallback) {
    return cn1InferenceJSON(@{
        @"error": error.localizedDescription ?: fallback
    });
}

static NSString *cn1InferenceType(TFLTensorDataType type) {
    switch (type) {
        case TFLTensorDataTypeFloat32: return @"FLOAT32";
        case TFLTensorDataTypeInt32: return @"INT32";
        case TFLTensorDataTypeUInt8: return @"UINT8";
        case TFLTensorDataTypeInt64: return @"INT64";
        case TFLTensorDataTypeBool: return @"BOOL";
        case TFLTensorDataTypeInt8: return @"INT8";
        default: return nil;
    }
}

static CN1InferenceHandle *cn1InferenceHandle(int handle) {
    cn1InferenceEnsureHandles();
    @synchronized (cn1InferenceHandles) {
        return cn1InferenceHandles[@(handle)];
    }
}

static NSString *cn1InferenceOpenPath(NSString *path, BOOL deleteModelOnClose,
        int threads, int accelerator, BOOL allowFallback) {
    // TFLCoreMLDelegate does not report whether it delegated the whole graph.
    // Unsupported operations may remain on LiteRT's CPU path, and delegated
    // operations may use the Neural Engine, GPU, or CPU. Strict NPU and
    // Core ML requests therefore cannot honor the no-fallback contract.
    if ((accelerator == 3 || accelerator == 4) && !allowFallback) {
        if (deleteModelOnClose) {
            [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
        }
        NSString *target = accelerator == 3 ? @"NPU" : @"Core ML";
        return cn1InferenceJSON(@{
            @"error": [NSString stringWithFormat:
                    @"Strict %@ execution cannot be verified on iOS; "
                    @"the Core ML delegate may leave unsupported operations "
                    @"on CPU", target]
        });
    }
    TFLInterpreterOptions *options = [[TFLInterpreterOptions alloc] init];
    if (threads > 0) options.numberOfThreads = (NSUInteger)threads;
    NSMutableArray<TFLDelegate *> *delegates = [NSMutableArray array];
    TFLDelegate *delegate = nil;

    // InferenceOptions.Accelerator ordinal:
    // AUTO=0, CPU=1, GPU=2, NPU=3, CORE_ML=4.
    if (accelerator == 0 || accelerator == 3 || accelerator == 4) {
#if __has_include(<TFLTensorFlowLite/TFLCoreMLDelegate.h>)
        delegate = [[TFLCoreMLDelegate alloc] init];
        if (delegate != nil) [delegates addObject:delegate];
#endif
        if (delegate == nil && !allowFallback && accelerator != 0) {
            if (deleteModelOnClose) {
                [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
            }
            return cn1InferenceJSON(@{
                @"error": @"Core ML delegate is unavailable on this device"
            });
        }
    } else if (accelerator == 2 && !allowFallback) {
        if (deleteModelOnClose) {
            [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
        }
        return cn1InferenceJSON(@{
            @"error": @"The iOS backend does not provide a GPU delegate"
        });
    }

    NSError *error = nil;
    TFLInterpreter *interpreter = [[TFLInterpreter alloc]
            initWithModelPath:path options:options delegates:delegates error:&error];
    BOOL interpreterCreated = interpreter != nil && error == nil;
    BOOL tensorsAllocated = interpreterCreated
            && [interpreter allocateTensorsWithError:&error];
    if ((!interpreterCreated || !tensorsAllocated)
            && delegate != nil && allowFallback) {
        delegate = nil;
        error = nil;
        interpreter = [[TFLInterpreter alloc]
                initWithModelPath:path options:options delegates:@[] error:&error];
        interpreterCreated = interpreter != nil && error == nil;
        tensorsAllocated = interpreterCreated
                && [interpreter allocateTensorsWithError:&error];
    }
    if (!interpreterCreated) {
        if (deleteModelOnClose) {
            [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
        }
        return cn1InferenceError(error, @"Could not create LiteRT interpreter");
    }
    if (!tensorsAllocated) {
        if (deleteModelOnClose) {
            [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
        }
        return cn1InferenceError(error, @"Could not allocate LiteRT tensors");
    }

    CN1InferenceHandle *value = [[CN1InferenceHandle alloc] init];
    value.interpreter = interpreter;
    value.delegate = delegate;
    value.modelPath = path;
    value.deleteModelOnClose = deleteModelOnClose;
    cn1InferenceEnsureHandles();
    int handle;
    @synchronized (cn1InferenceHandles) {
        handle = cn1InferenceNextHandle++;
        cn1InferenceHandles[@(handle)] = value;
    }
    return cn1InferenceJSON(@{@"handle": @(handle)});
}

static NSString *cn1InferenceOpen(NSData *model, int threads, int accelerator,
                                  BOOL allowFallback) {
    NSString *path = [NSTemporaryDirectory() stringByAppendingPathComponent:
            [NSString stringWithFormat:@"cn1-litert-%@.tflite",
                    NSUUID.UUID.UUIDString]];
    if (![model writeToFile:path atomically:YES]) {
        return cn1InferenceJSON(@{@"error": @"Could not stage LiteRT model"});
    }
    return cn1InferenceOpenPath(path, YES, threads, accelerator, allowFallback);
}

static NSString *cn1InferenceMetadata(int handle, BOOL outputs) {
    CN1InferenceHandle *value = cn1InferenceHandle(handle);
    if (value == nil) {
        return cn1InferenceJSON(@{@"error": @"Unknown LiteRT handle"});
    }
    TFLInterpreter *interpreter = value.interpreter;
    NSUInteger count = outputs ? interpreter.outputTensorCount
                               : interpreter.inputTensorCount;
    NSMutableArray *items = [NSMutableArray array];
    for (NSUInteger i = 0; i < count; i++) {
        NSError *error = nil;
        TFLTensor *tensor = outputs
                ? [interpreter outputTensorAtIndex:i error:&error]
                : [interpreter inputTensorAtIndex:i error:&error];
        if (tensor == nil || error != nil) {
            return cn1InferenceError(error, @"Could not read LiteRT tensor");
        }
        NSString *type = cn1InferenceType(tensor.dataType);
        if (type == nil) {
            return cn1InferenceJSON(@{
                @"error": [NSString stringWithFormat:
                        @"Unsupported LiteRT tensor type %lu",
                        (unsigned long)tensor.dataType]
            });
        }
        NSArray<NSNumber *> *shape = [tensor shapeWithError:&error];
        if (shape == nil || error != nil) {
            return cn1InferenceError(error, @"Could not read LiteRT tensor shape");
        }
        [items addObject:@{
            @"index": @(i),
            @"name": tensor.name ?: @"",
            @"type": type,
            @"shape": shape
        }];
    }
    return cn1InferenceJSON(@{@"items": items});
}

static NSString *cn1InferenceCopyInput(int handle, int index, NSData *data) {
    CN1InferenceHandle *value = cn1InferenceHandle(handle);
    if (value == nil) {
        return cn1InferenceJSON(@{@"error": @"Unknown LiteRT handle"});
    }
    NSError *error = nil;
    TFLInterpreter *interpreter = value.interpreter;
    if (index < 0 || (NSUInteger)index >= interpreter.inputTensorCount) {
        return cn1InferenceJSON(@{@"error": @"Invalid LiteRT input index"});
    }
    TFLTensor *tensor = [interpreter inputTensorAtIndex:(NSUInteger)index
                                                  error:&error];
    if (tensor == nil || error != nil || ![tensor copyData:data error:&error]) {
        return cn1InferenceError(error, @"Could not copy LiteRT input");
    }
    return cn1InferenceJSON(@{@"ok": @(YES)});
}

static NSString *cn1InferenceInvoke(int handle) {
    CN1InferenceHandle *value = cn1InferenceHandle(handle);
    if (value == nil) {
        return cn1InferenceJSON(@{@"error": @"Unknown LiteRT handle"});
    }
    NSError *error = nil;
    TFLInterpreter *interpreter = value.interpreter;
    if (![interpreter invokeWithError:&error]) {
        return cn1InferenceError(error, @"LiteRT invocation failed");
    }
    return cn1InferenceJSON(@{@"ok": @(YES)});
}

static NSData *cn1InferenceOutputData(int handle, int index) {
    CN1InferenceHandle *value = cn1InferenceHandle(handle);
    if (value == nil || index < 0
            || (NSUInteger)index >= value.interpreter.outputTensorCount) {
        return nil;
    }
    NSError *error = nil;
    TFLTensor *tensor = [value.interpreter outputTensorAtIndex:(NSUInteger)index
                                                        error:&error];
    return tensor == nil || error != nil ? nil : [tensor dataWithError:&error];
}

static NSString *cn1InferenceResize(int handle, int index,
                                    NSArray<NSNumber *> *shape) {
    CN1InferenceHandle *value = cn1InferenceHandle(handle);
    if (value == nil) {
        return cn1InferenceJSON(@{@"error": @"Unknown LiteRT handle"});
    }
    NSError *error = nil;
    if (![value.interpreter resizeInputTensorAtIndex:(NSUInteger)index
            toShape:shape error:&error]) {
        return cn1InferenceError(error, @"Could not resize LiteRT input");
    }
    if (![value.interpreter allocateTensorsWithError:&error]) {
        return cn1InferenceError(error, @"Could not reallocate LiteRT tensors");
    }
    return cn1InferenceJSON(@{@"ok": @(YES)});
}
#endif
#endif

JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_cn1InferenceIsSupported___R_boolean(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {
#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV && defined(CN1_HAS_LITERT)
    return 1;
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1InferenceOpen___byte_1ARRAY_int_int_boolean_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT model,
        JAVA_INT threads, JAVA_INT accelerator, JAVA_BOOLEAN allowFallback) {
#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV && defined(CN1_HAS_LITERT)
    if (model == JAVA_NULL) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                @"{\"error\":\"Model data is null\"}");
    }
    JAVA_ARRAY bytes = (JAVA_ARRAY)model;
    NSData *data = [NSData dataWithBytes:bytes->data
                                  length:(NSUInteger)bytes->length];
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
            cn1InferenceOpen(data, threads, accelerator, allowFallback));
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1InferenceMetadata___int_boolean_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle,
        JAVA_BOOLEAN outputs) {
#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV && defined(CN1_HAS_LITERT)
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
            cn1InferenceMetadata(handle, outputs));
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1InferenceOpenFile___java_lang_String_int_int_boolean_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT path,
        JAVA_INT threads, JAVA_INT accelerator, JAVA_BOOLEAN allowFallback) {
#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV && defined(CN1_HAS_LITERT)
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
            cn1InferenceOpenPath(
                    toNSString(CN1_THREAD_GET_STATE_PASS_ARG path), NO,
                    threads, accelerator, allowFallback));
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1InferenceCopyInput___int_int_byte_1ARRAY_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle,
        JAVA_INT index, JAVA_OBJECT input) {
#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV && defined(CN1_HAS_LITERT)
    if (input == JAVA_NULL) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                @"{\"error\":\"Input data is null\"}");
    }
    JAVA_ARRAY bytes = (JAVA_ARRAY)input;
    NSData *data = [NSData dataWithBytes:bytes->data
                                  length:(NSUInteger)bytes->length];
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
            cn1InferenceCopyInput(handle, index, data));
#else
    return JAVA_NULL;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1InferenceInvoke___int_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV && defined(CN1_HAS_LITERT)
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG cn1InferenceInvoke(handle));
#else
    return JAVA_NULL;
#endif
}

JAVA_LONG com_codename1_impl_ios_IOSNative_cn1InferenceOutputData___int_int_R_long(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle,
        JAVA_INT index) {
#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV && defined(CN1_HAS_LITERT)
    NSData *data = cn1InferenceOutputData(handle, index);
    return data == nil ? 0 : (JAVA_LONG)CFBridgingRetain(data);
#else
    return 0;
#endif
}

JAVA_OBJECT com_codename1_impl_ios_IOSNative_cn1InferenceResize___int_int_int_1ARRAY_R_java_lang_String(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle,
        JAVA_INT index, JAVA_OBJECT shape) {
#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV && defined(CN1_HAS_LITERT)
    if (shape == JAVA_NULL) {
        return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
                @"{\"error\":\"Shape is null\"}");
    }
    JAVA_ARRAY array = (JAVA_ARRAY)shape;
    JAVA_ARRAY_INT *values = (JAVA_ARRAY_INT *)array->data;
    NSMutableArray<NSNumber *> *nativeShape =
            [NSMutableArray arrayWithCapacity:(NSUInteger)array->length];
    for (int i = 0; i < array->length; i++) {
        [nativeShape addObject:@(values[i])];
    }
    return fromNSString(CN1_THREAD_GET_STATE_PASS_ARG
            cn1InferenceResize(handle, index, nativeShape));
#else
    return JAVA_NULL;
#endif
}

void com_codename1_impl_ios_IOSNative_cn1InferenceClose___int(
        CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_INT handle) {
#if defined(INCLUDE_CN1_INFERENCE) && !TARGET_OS_WATCH && !TARGET_OS_TV && defined(CN1_HAS_LITERT)
    cn1InferenceEnsureHandles();
    CN1InferenceHandle *value;
    @synchronized (cn1InferenceHandles) {
        value = cn1InferenceHandles[@(handle)];
        [cn1InferenceHandles removeObjectForKey:@(handle)];
    }
    if (value.deleteModelOnClose && value.modelPath != nil) {
        [[NSFileManager defaultManager] removeItemAtPath:value.modelPath error:nil];
    }
#endif
}
