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
package com.codename1.flutter.gestures;

import com.codename1.flutter.Offset;

/**
 * A pointer event, as Flutter's {@code PointerEvent}.
 *
 * <p>Only what a synthesised touch needs: where it happened and which pointer
 * it belongs to. The point of having these at all is that ONE Dart script can
 * drive both stacks -- it hands events to {@link GestureBinding} and the
 * framework underneath routes them, so a tap written once produces a real tap,
 * with the real hit test and the real animations, on whichever runtime is
 * executing it.</p>
 */
public abstract class PointerEvent {

    private Offset position = new Offset(0, 0);
    private long pointer;

    protected PointerEvent() {
    }

    /** Named parameter setter for the Dart {@code position:} parameter. */
    public void position(Offset v) {
        this.position = v == null ? new Offset(0, 0) : v;
    }

    /** Named parameter setter for the Dart {@code pointer:} parameter. */
    public void pointer(long v) {
        this.pointer = v;
    }

    public Offset position() {
        return position;
    }

    public long pointerId() {
        return pointer;
    }
}
