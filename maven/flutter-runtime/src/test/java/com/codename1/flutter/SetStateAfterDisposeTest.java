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
package com.codename1.flutter;

import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A timer or future callback that outlives its page calls setState on a disposed
 * State. Flutter rejects it; running the closure anyway mutated shared data for a
 * dead state and hid the bug.
 */
class SetStateAfterDisposeTest {

    static final class Counter extends StatefulWidget {
        CounterState state;

        @Override
        public State<? extends StatefulWidget> createState() {
            state = new CounterState();
            return state;
        }
    }

    static final class CounterState extends State<Counter> {
        @Override
        public Widget build(BuildContext context) {
            return new ProbeBox(1, 1);
        }
    }

    @Test
    void setStateAfterDisposeDoesNotRunTheClosure() {
        Counter w = new Counter();
        Element root = FlutterUI.mount(w, new RenderHost(), new BuildOwner());
        final int[] runs = {0};
        w.state.setState(() -> runs[0]++);
        assertEquals(1, runs[0], "a mounted state runs its closure");

        FlutterUI.unmountTree(root);
        w.state.setState(() -> runs[0]++);
        assertEquals(1, runs[0], "a disposed state must not");
    }
}
