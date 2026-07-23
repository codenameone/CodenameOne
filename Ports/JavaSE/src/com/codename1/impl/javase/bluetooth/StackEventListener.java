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
package com.codename1.impl.javase.bluetooth;

/**
 * Observes the simulated Bluetooth stack's event log. Every operation the
 * stack executes -- connects, reads, notifications, scan sightings,
 * scripted failures -- fires one entry, so a debug UI (the future
 * Simulate&nbsp;&rarr;&nbsp;Bluetooth menu) can show a live trace.
 *
 * <p>Listeners are invoked on the stack's scheduler thread.</p>
 */
public interface StackEventListener {

    /**
     * One stack operation.
     *
     * @param op a stable operation key, e.g. {@code "connect"},
     * {@code "read"}, {@code "scan"}
     * @param detail a human-readable description of the specific event
     */
    void event(String op, String detail);
}
