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
package com.codename1.flutter.services;

import dart.core.Duration;

/**
 * Base class for a keyboard event in Flutter's modern {@code HardwareKeyboard}
 * API ({@code KeyEvent}). The {@code Focus.onKeyEvent} / {@code KeyboardListener}
 * callbacks receive one of the concrete subclasses ({@link KeyDownEvent},
 * {@link KeyUpEvent}, {@link KeyRepeatEvent}); code switches on the runtime type
 * with {@code event is KeyDownEvent} and reads {@link #logicalKey()}.
 */
public abstract class KeyEvent {

    final LogicalKeyboardKey logicalKey;
    final PhysicalKeyboardKey physicalKey;
    final String character;
    final Duration timeStamp;

    KeyEvent(LogicalKeyboardKey logicalKey, PhysicalKeyboardKey physicalKey, String character) {
        this.logicalKey = logicalKey;
        this.physicalKey = physicalKey;
        this.character = character;
        this.timeStamp = null;
    }

    /** The logical (layout-dependent) key for this event. */
    public LogicalKeyboardKey logicalKey() {
        return logicalKey;
    }

    /** The physical (scan-code) key for this event. */
    public PhysicalKeyboardKey physicalKey() {
        return physicalKey;
    }

    /** The character produced, or {@code null} for non-printable keys. */
    public String character() {
        return character;
    }

    /** The event time, relative to an arbitrary epoch. */
    public Duration timeStamp() {
        return timeStamp;
    }
}
