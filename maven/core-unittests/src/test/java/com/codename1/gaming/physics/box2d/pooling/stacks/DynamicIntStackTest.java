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
package com.codename1.gaming.physics.box2d.pooling.stacks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the Box2D {@link DynamicIntStack}: LIFO push/pop ordering, the
 * count accessor, reset, and the automatic capacity doubling that preserves
 * already-stored values.
 */
class DynamicIntStackTest {

    @Test
    void newStackIsEmpty() {
        assertEquals(0, new DynamicIntStack(4).getCount());
    }

    @Test
    void pushIncrementsCountAndPopIsLifo() {
        DynamicIntStack s = new DynamicIntStack(4);
        s.push(10);
        s.push(20);
        s.push(30);
        assertEquals(3, s.getCount());
        assertEquals(30, s.pop());
        assertEquals(20, s.pop());
        assertEquals(10, s.pop());
        assertEquals(0, s.getCount());
    }

    @Test
    void resetEmptiesWithoutLosingCapacity() {
        DynamicIntStack s = new DynamicIntStack(2);
        s.push(1);
        s.push(2);
        s.reset();
        assertEquals(0, s.getCount());
        // Still usable after reset.
        s.push(99);
        assertEquals(99, s.pop());
    }

    @Test
    void growsBeyondInitialCapacityPreservingValues() {
        DynamicIntStack s = new DynamicIntStack(2);
        // Push past the initial size of 2 to trigger the doubling branch.
        for (int i = 0; i < 10; i++) {
            s.push(i);
        }
        assertEquals(10, s.getCount());
        // Values survive the array copy and pop back in LIFO order.
        for (int i = 9; i >= 0; i--) {
            assertEquals(i, s.pop());
        }
        assertEquals(0, s.getCount());
    }
}
