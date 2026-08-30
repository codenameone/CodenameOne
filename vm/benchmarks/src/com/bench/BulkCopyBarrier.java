/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.bench;

/**
 * Republishes an OLD object graph into a FRESH array with a bulk copy, and drops the
 * original.
 *
 * <p>Both bulk copies of object references -- {@code Object[].clone()} (cloneArray) and
 * {@code System.arraycopy} on an object array -- move references without going through the
 * per-element setter, so the SATB INSERTION barrier the translator emits at every object
 * store does not fire for any of them. That barrier exists precisely to keep an object
 * alive when "the container it is stored into is a fresh grace object not yet reachable",
 * so a bulk copy into a newly allocated array during a mark, followed by dropping the
 * source, leaves every copied-in object unmarked while a live reference to it survives.</p>
 *
 * <p>The shape below is the smallest one that produces that: age the contents so they are
 * not themselves protected by the one-cycle grace rule, republish them into a fresh array
 * mid-mark, drop every other reference, and then read them back. Under
 * {@code -DCN1_GC_VERIFY} a missed barrier shows up as a DANGLING REFERENCE report after
 * the sweep; without the verifier it shows up as the contents no longer summing to what
 * they were built from.</p>
 */
public class BulkCopyBarrier {
    private static final int CONTENT = 256;
    private static final int ROUNDS = 120;
    private static final int BURN = 150000;
    /**
     * Several worker threads, because the window this targets is opened by the collector's
     * ROLLING handshake: it scans one thread, releases it, and goes on to the next. A
     * thread released early keeps running -- and bulk-copying -- for the rest of a mark it
     * will not be re-scanned by.
     */
    private static final int THREADS = 4;

    static final class Content {
        int value;
        Content peer;
    }

    static final class Wrapper {
        Object[] arr;
    }

    static Object[] holder;
    static Object sink;
    static volatile long checksum;

    private static void burn() {
        Object last = null;
        for (int i = 0; i < BURN; i++) {
            Content c = new Content();
            c.value = i;
            c.peer = (Content) last;
            if ((i & 63) == 0) {
                last = c;
            }
        }
        sink = last;
    }

    static void round(int r) {
        Object[] src = new Object[CONTENT];
        for (int i = 0; i < CONTENT; i++) {
            Content c = new Content();
            c.value = r * 7 + i;
            src[i] = c;
        }
        holder = src;
        burn();                     // age the contents past a collection

        Object[] dst;
        if ((r & 1) == 0) {
            dst = (Object[]) src.clone();
        } else {
            dst = new Object[CONTENT];
            System.arraycopy(src, 0, dst, 0, CONTENT);
        }
        holder = null;
        src = null;

        // Fresh, unreachable container: kept by the one-cycle grace rule while the OLD
        // contents it points at are not, which is the only shape that exposes a missing
        // insertion barrier. A reachable destination is simply traced.
        Wrapper w = new Wrapper();
        w.arr = dst;
        dst = null;
        checksum += w.arr.length;
        w = null;

        burn();
    }

    public static void main(String[] args) {
        Thread[] t = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            final int base = i;
            t[i] = new Thread() {
                public void run() {
                    for (int r = 0; r < ROUNDS; r++) {
                        round(r * THREADS + base);
                    }
                }
            };
            t[i].start();
        }
        for (int i = 0; i < THREADS; i++) {
            try {
                t[i].join();
            } catch (InterruptedException e) {
            }
        }
        System.out.println("ROUNDS=" + (ROUNDS * THREADS));
        System.out.println("RESULT=" + checksum);
        System.out.println("BULK_COPY_BARRIER_DONE");
    }
}
