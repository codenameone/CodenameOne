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
 * The few ParparVM declarations cn1_crypto_kernels.c reads, so the benchmark
 * compiles the shipped kernel file unchanged outside a translated project.
 */
#ifndef CN1_BENCH_GLOBALS_H
#define CN1_BENCH_GLOBALS_H
#include <stdint.h>
typedef int32_t JAVA_INT;
typedef int8_t JAVA_BOOLEAN;
typedef void JAVA_VOID;
typedef void *JAVA_OBJECT;
typedef struct { void *data; int length; } *JAVA_ARRAY;
#define CN1_ARRAY_DATA(a) (((JAVA_ARRAY)(a))->data)
#define CODENAME_ONE_THREAD_STATE void *threadStateData
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define JAVA_NULL ((JAVA_OBJECT)0)
#endif
