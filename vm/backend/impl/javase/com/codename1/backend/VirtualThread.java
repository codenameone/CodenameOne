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
package com.codename1.backend;

/**
 * The simulator has no virtual threads.
 *
 * They exist because ParparVM owns its whole translation and can switch a stack
 * in a couple of nanoseconds; on a stock JVM the same idea is Loom's job, not
 * ours. {@link #create} returning 0 is the documented "not available" answer and
 * the server falls back to its pooled path, so behaviour here differs in
 * scheduling only -- never in what a client sees.
 */
public final class VirtualThread {
    private VirtualThread() {
    }

    /** Always 0 here: not available, use the pool. */
    public static long create(int fd, int stackBytes) {
        return 0;
    }

    public static final int FINISHED = 0;
    public static final int PARKED_IO = 1;
    public static final int RUNNABLE = 2;

    public static int resume(long handle) {
        return FINISHED;
    }

    public static void free(long handle) {
    }

    /** No virtual threads here. */
    public static int descriptorOf(long handle) {
        return -1;
    }

    public static boolean isVirtual() {
        return false;
    }

    /** No virtual threads here, so the server keeps the pool. */
    public static boolean supported() {
        return false;
    }

    /** No virtual threads here, so there is nothing to step aside for. */
    public static void yieldNow() {
    }

    /** Nothing to report where there are none. */
    public static void report() {
    }
}
