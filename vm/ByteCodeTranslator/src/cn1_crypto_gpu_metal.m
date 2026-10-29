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
 * The Metal backend of the GPU crypto batches (iOS, iPadOS, tvOS, macOS). The translator emits it
 * beside cn1_crypto_kernels.c for an application that uses the batch APIs. A device with no usable
 * GPU (MTLCreateSystemDefaultDevice answering nil) keeps every batch on the CPU.
 *
 * The kernels are compiled from source at run time, once, on a background queue: a precompiled
 * library is tied to the SDK that built it, and compiling takes long enough that the first batch
 * must not wait for it -- until the pipelines exist, ready() answers 0 and batches run on the CPU.
 * Metal keeps its own cache of compiled source, so later launches are fast.
 */
#include <TargetConditionals.h>
#if !TARGET_OS_WATCH
#import <Foundation/Foundation.h>
#import <Metal/Metal.h>
#include <string.h>
#include "cn1_crypto_gpu.h"
#include "cn1_crypto_gpu_shaders.h"

#if __has_feature(objc_arc)
#  define CN1G_RELEASE(x) ((void) 0)
#else
#  define CN1G_RELEASE(x) [(x) release]
#endif

#define CN1G_KERNEL_COUNT 5

/* one dispatch per command buffer is capped so no command buffer runs long enough to trip the
 * GPU watchdog, which on iOS ends one after a few seconds */
#define CN1G_MAX_PER_DISPATCH 16384u

static const char *const cn1g_metal_names[CN1G_KERNEL_COUNT] = {
    "cn1g_sha256", "cn1g_keccak256", "cn1g_ecdsa", "cn1g_schnorr", "cn1g_ed25519"
};


static id<MTLDevice> cn1g_device = nil;
static id<MTLCommandQueue> cn1g_queue = nil;
static id<MTLComputePipelineState> cn1g_pipelines[CN1G_KERNEL_COUNT];
static id<MTLBuffer> cn1g_tables[2];
static const uint32_t *cn1g_table_keys[2];
/* 0 not started, 1 compiling, 2 ready, 3 failed for good */
static int cn1g_state = 0;
static NSLock *cn1g_lock = nil;

static void cn1g_metal_compile(void) {
    @autoreleasepool {
        NSError *err = nil;
        id<MTLDevice> device = MTLCreateSystemDefaultDevice();
        MTLCompileOptions *opts;
        id<MTLLibrary> lib;
        int i, ok = 1;
        if (device == nil) {
            __atomic_store_n(&cn1g_state, 3, __ATOMIC_RELEASE);
            return;
        }
        opts = [[MTLCompileOptions alloc] init];
        lib = [device newLibraryWithSource:[NSString stringWithUTF8String:cn1g_msl_source] options:opts error:&err];
        CN1G_RELEASE(opts);
        if (lib == nil) {
            NSLog(@"cn1 crypto: the Metal kernels did not compile, batches stay on the CPU: %@", err);
            CN1G_RELEASE(device);
            __atomic_store_n(&cn1g_state, 3, __ATOMIC_RELEASE);
            return;
        }
        for (i = 0; i < CN1G_KERNEL_COUNT && ok; i++) {
            id<MTLFunction> fn = [lib newFunctionWithName:[NSString stringWithUTF8String:cn1g_metal_names[i]]];
            cn1g_pipelines[i] = fn == nil ? nil : [device newComputePipelineStateWithFunction:fn error:&err];
            CN1G_RELEASE(fn);
            ok = cn1g_pipelines[i] != nil;
        }
        CN1G_RELEASE(lib);
        cn1g_queue = ok ? [device newCommandQueue] : nil;
        if (!ok || cn1g_queue == nil) {
            NSLog(@"cn1 crypto: no Metal pipeline, batches stay on the CPU: %@", err);
            __atomic_store_n(&cn1g_state, 3, __ATOMIC_RELEASE);
            return;
        }
        cn1g_device = device;
        cn1g_lock = [[NSLock alloc] init];
        __atomic_store_n(&cn1g_state, 2, __ATOMIC_RELEASE);
    }
}

static int cn1g_metal_ready(int kernel) {
    (void) kernel;
    int s = __atomic_load_n(&cn1g_state, __ATOMIC_ACQUIRE);
    if (s == 0) {
        int expected = 0;
        if (__atomic_compare_exchange_n(&cn1g_state, &expected, 1, 0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
            dispatch_async(dispatch_get_global_queue(QOS_CLASS_UTILITY, 0), ^{
                cn1g_metal_compile();
            });
        }
        return 0;
    }
    return s == 2;
}

/* the table as a GPU buffer, made once per table: slot 0 the secp256k1 one, slot 1 ed25519's;
 * a hash has none and gets a one-word placeholder */
static id<MTLBuffer> cn1g_metal_table(int slot, const uint32_t *table, size_t bytes) {
    static const uint32_t zero = 0;
    id<MTLBuffer> b;
    [cn1g_lock lock];
    if (cn1g_tables[slot] == nil || cn1g_table_keys[slot] != table) {
        if (cn1g_tables[slot] != nil) {
            CN1G_RELEASE(cn1g_tables[slot]);
        }
        cn1g_tables[slot] = table == NULL
                ? [cn1g_device newBufferWithBytes:&zero length:4 options:MTLResourceStorageModeShared]
                : [cn1g_device newBufferWithBytes:table length:bytes options:MTLResourceStorageModeShared];
        cn1g_table_keys[slot] = table;
    }
    b = cn1g_tables[slot];
    [cn1g_lock unlock];
    return b;
}

static int cn1g_metal_run(int kernel, const void *in, size_t inBytes, void *out, size_t outBytes,
                          const uint32_t *table, size_t tableBytes, uint32_t len, uint32_t count) {
    int ok = 1;
    if (kernel < 0 || kernel >= CN1G_KERNEL_COUNT || !cn1g_metal_ready(kernel)) {
        return 0;
    }
    @autoreleasepool {
        id<MTLComputePipelineState> pipe = cn1g_pipelines[kernel];
        id<MTLBuffer> tbl = cn1g_metal_table(kernel == 4 ? 1 : 0, kernel >= 2 ? table : NULL, tableBytes);
        /* rounded up to a word: the kernels read the last input byte as part of a word */
        id<MTLBuffer> inb = [cn1g_device newBufferWithLength:(inBytes + 3) & ~(size_t) 3
                                                      options:MTLResourceStorageModeShared];
        id<MTLBuffer> outb = [cn1g_device newBufferWithLength:outBytes options:MTLResourceStorageModeShared];
        NSUInteger width = pipe.threadExecutionWidth, perGroup = pipe.maxTotalThreadsPerThreadgroup;
        NSMutableArray *sent = [NSMutableArray array];
        uint32_t at = 0;
        if (inb == nil || outb == nil || tbl == nil) {
            CN1G_RELEASE(inb);
            CN1G_RELEASE(outb);
            return 0;
        }
        memcpy(inb.contents, in, inBytes);
        if (perGroup > 4 * width) {
            perGroup = 4 * width;
        }
        while (at < count) {
            /* items at..at+n-1: the kernel adds the base, so no buffer is offset */
            uint32_t n = count - at < CN1G_MAX_PER_DISPATCH ? count - at : CN1G_MAX_PER_DISPATCH;
            uint32_t pc[4];
            id<MTLCommandBuffer> cb = [cn1g_queue commandBuffer];
            id<MTLComputeCommandEncoder> enc = [cb computeCommandEncoder];
            pc[0] = len;
            pc[1] = at + n;
            pc[2] = at;
            pc[3] = 0;
            [enc setComputePipelineState:pipe];
            [enc setBuffer:inb offset:0 atIndex:0];
            [enc setBuffer:outb offset:0 atIndex:1];
            [enc setBuffer:tbl offset:0 atIndex:2];
            [enc setBytes:pc length:sizeof(pc) atIndex:3];
            [enc dispatchThreadgroups:MTLSizeMake((n + perGroup - 1) / perGroup, 1, 1)
                threadsPerThreadgroup:MTLSizeMake(perGroup, 1, 1)];
            [enc endEncoding];
            [cb commit];
            [sent addObject:cb];
            at += n;
        }
        /* a queue completes its buffers in order; each one's own status says whether it ran */
        [[sent lastObject] waitUntilCompleted];
        for (id<MTLCommandBuffer> cb in sent) {
            [cb waitUntilCompleted];
            if (cb.status != MTLCommandBufferStatusCompleted) {
                ok = 0;
            }
        }
        if (ok) {
            memcpy(out, outb.contents, outBytes);
        }
        CN1G_RELEASE(inb);
        CN1G_RELEASE(outb);
    }
    return ok;
}

static const cn1g_backend cn1g_metal = { "Metal", cn1g_metal_ready, cn1g_metal_run };

/* registered at load; nothing touches Metal until a batch asks for it */
__attribute__((constructor)) static void cn1g_metal_register(void) {
    cn1g_metal_backend = &cn1g_metal;
}
#endif
