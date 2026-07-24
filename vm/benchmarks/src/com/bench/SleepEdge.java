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
package com.bench;

/**
 * Thread.sleep edge cases: tiny sleeps stay accurate and a
 * Thread.sleep(Long.MAX_VALUE) "park this thread" idiom must not return
 * immediately (the saturating-deadline guard -- an overflowing deadline
 * wrapped negative and made huge sleeps no-ops). Interrupt semantics are
 * deliberately NOT asserted: this VM has never delivered
 * InterruptedException from sleep, and the process exit reaps the parked
 * daemon-equivalent thread.
 */
public class SleepEdge {
    public static void main(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();
        Thread.sleep(0);
        Thread.sleep(1);
        Thread.sleep(50);
        long elapsed = System.currentTimeMillis() - t0;
        final Thread parked = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                }
            }
        });
        parked.start();
        Thread.sleep(300);
        boolean stillParked = parked.isAlive();
        // Negative millis must throw IllegalArgumentException (JDK contract),
        // not return immediately or park.
        boolean negativeThrew = false;
        try {
            Thread.sleep(-1);
        } catch (IllegalArgumentException e) {
            negativeThrew = true;
        }
        System.out.println("small_elapsed=" + elapsed + " parkedAlive=" + stillParked
                + " negativeThrew=" + negativeThrew);
        System.out.println(elapsed >= 50 && elapsed < 2000 && stillParked && negativeThrew
                ? "SLEEP_EDGE_OK" : "SLEEP_EDGE_FAIL");
        System.exit(0);
    }
}
