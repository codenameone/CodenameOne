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
#ifndef CN1_CRYPTO_GPU_H
#define CN1_CRYPTO_GPU_H

#include <stddef.h>
#include <stdint.h>

/*
 * A GPU that runs the kernels of cn1_crypto_gpu_kernels.h. cn1_crypto_kernels.c asks for one only
 * for a batch large enough to be worth it, and any answer other than 1 sends the batch to the CPU,
 * so a backend may refuse for any reason: no device, still compiling, running in the background,
 * out of memory.
 */
typedef struct cn1g_backend {
    const char *name;
    /*
     * 1 once the backend can run `kernel` (CN1G_KERNEL_*). It may start preparing on the first call
     * and answer 0 meanwhile, and a backend that builds its kernels one at a time may answer 1 for
     * some before others.
     */
    int (*ready)(int kernel);
    /*
     * Runs `kernel` (CN1G_KERNEL_*) over `count` items and answers 1 when `out` is complete.
     * `in` holds the records or the packed inputs (`len` bytes each for a hash), `table` the
     * fixed-base table or NULL; the table's address is stable for the life of the process, so a
     * backend may keep its own copy keyed on it.
     */
    int (*run)(int kernel, const void *in, size_t inBytes, void *out, size_t outBytes,
               const uint32_t *table, size_t tableBytes, uint32_t len, uint32_t count);
} cn1g_backend;

/* set by cn1_crypto_gpu_metal.m when it is linked in; defined in cn1_crypto_kernels.c */
extern const cn1g_backend *cn1g_metal_backend;

#endif
