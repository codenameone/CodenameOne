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
package com.codename1.gaming.physics.box2d.pooling.normal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the Box2D {@link CircleStack} ring-buffer pool: pre-allocation
 * via {@code newInstance()}, the wrapping single {@code pop()}, and the batch
 * {@code pop(int)} both within a single span and across the wrap boundary.
 *
 * <p>{@code newInstance()} runs inside the superclass constructor (before any
 * subclass instance field exists), so the created instances are captured in a
 * static list that the test resets before each construction.
 */
class CircleStackTest {

    private static final List<Object> CREATED = new ArrayList<Object>();

    /** Concrete stack whose pooled elements are distinct, identity-comparable. */
    private static final class TestStack extends CircleStack<Object> {
        TestStack(int stackSize, int containerSize) {
            super(stackSize, containerSize);
        }

        @Override
        protected Object newInstance() {
            Object o = new Object();
            CREATED.add(o);
            return o;
        }
    }

    @BeforeEach
    void resetCreated() {
        CREATED.clear();
    }

    @Test
    void constructorPreallocatesOneInstancePerSlot() {
        new TestStack(3, 4);
        assertEquals(3, CREATED.size());
    }

    @Test
    void popAdvancesThenWrapsAroundThePool() {
        TestStack s = new TestStack(3, 4);
        // pop() pre-increments the index, so the first element returned is pool[1].
        assertSame(CREATED.get(1), s.pop());
        assertSame(CREATED.get(2), s.pop());
        assertSame(CREATED.get(0), s.pop());
        // ...and it keeps cycling.
        assertSame(CREATED.get(1), s.pop());
    }

    @Test
    void popBatchWithinSpanReturnsContiguousSlice() {
        TestStack s = new TestStack(3, 4);
        Object[] batch = s.pop(2);
        assertSame(CREATED.get(0), batch[0]);
        assertSame(CREATED.get(1), batch[1]);
    }

    @Test
    void popBatchAcrossWrapBoundaryStitchesHeadAndTail() {
        TestStack s = new TestStack(3, 4);
        s.pop(2); // index -> 2
        Object[] batch = s.pop(2); // 2 + 2 > 3: wraps
        assertSame(CREATED.get(2), batch[0]);
        assertSame(CREATED.get(0), batch[1]);
    }

    @Test
    void pushIsANoOpAndDoesNotDisturbThePool() {
        TestStack s = new TestStack(3, 4);
        s.push(5);
        // Index is untouched by push, so pop() still starts from pool[1].
        assertSame(CREATED.get(1), s.pop());
    }
}
