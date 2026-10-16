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

/**
 * A logical (layout-dependent) keyboard key, mirroring Flutter's
 * {@code LogicalKeyboardKey}. The static constants are singletons, so the
 * {@code event.logicalKey == LogicalKeyboardKey.enter} comparisons that
 * new_gallery performs resolve by reference identity.
 *
 * <p>Key ids follow Flutter's logical key-id numbering where practical; the
 * exact numeric value is unimportant for Codename One since only identity
 * comparisons are used.</p>
 */
public class LogicalKeyboardKey {

    private final long keyId;
    private final String keyLabel;
    private final String debugName;

    LogicalKeyboardKey(long keyId, String keyLabel, String debugName) {
        this.keyId = keyId;
        this.keyLabel = keyLabel;
        this.debugName = debugName;
    }

    /** The unique logical key id. */
    public long keyId() {
        return keyId;
    }

    /** The printable label for the key, or {@code null} for non-printables. */
    public String keyLabel() {
        return keyLabel;
    }

    /** A human-readable name for debugging. */
    public String debugName() {
        return debugName;
    }

    public static final LogicalKeyboardKey arrowUp = new LogicalKeyboardKey(0x100000301L, null, "Arrow Up");
    public static final LogicalKeyboardKey arrowDown = new LogicalKeyboardKey(0x100000303L, null, "Arrow Down");
    public static final LogicalKeyboardKey arrowLeft = new LogicalKeyboardKey(0x100000302L, null, "Arrow Left");
    public static final LogicalKeyboardKey arrowRight = new LogicalKeyboardKey(0x100000304L, null, "Arrow Right");
    public static final LogicalKeyboardKey enter = new LogicalKeyboardKey(0x100000013L, null, "Enter");
    public static final LogicalKeyboardKey numpadEnter = new LogicalKeyboardKey(0x20000020eL, null, "Numpad Enter");
    public static final LogicalKeyboardKey escape = new LogicalKeyboardKey(0x100000009L, null, "Escape");
    public static final LogicalKeyboardKey tab = new LogicalKeyboardKey(0x100000009L + 1, null, "Tab");
    public static final LogicalKeyboardKey space = new LogicalKeyboardKey(0x00000000020L, " ", "Space");
    public static final LogicalKeyboardKey backspace = new LogicalKeyboardKey(0x100000008L, null, "Backspace");
    public static final LogicalKeyboardKey delete = new LogicalKeyboardKey(0x10000007fL, null, "Delete");
    public static final LogicalKeyboardKey home = new LogicalKeyboardKey(0x100000306L, null, "Home");
    public static final LogicalKeyboardKey end = new LogicalKeyboardKey(0x100000305L, null, "End");
    public static final LogicalKeyboardKey pageUp = new LogicalKeyboardKey(0x100000308L, null, "Page Up");
    public static final LogicalKeyboardKey pageDown = new LogicalKeyboardKey(0x100000307L, null, "Page Down");
    public static final LogicalKeyboardKey shift = new LogicalKeyboardKey(0x1000700e1L, null, "Shift");
    public static final LogicalKeyboardKey control = new LogicalKeyboardKey(0x1000700e0L, null, "Control");
    public static final LogicalKeyboardKey meta = new LogicalKeyboardKey(0x1000700e3L, null, "Meta");
    public static final LogicalKeyboardKey alt = new LogicalKeyboardKey(0x1000700e2L, null, "Alt");

    // Accessor forms for the `external static ... get name` stubs, in case the
    // emitter routes property reads through zero-arg methods.
    public static LogicalKeyboardKey arrowUp() { return arrowUp; }
    public static LogicalKeyboardKey arrowDown() { return arrowDown; }
    public static LogicalKeyboardKey arrowLeft() { return arrowLeft; }
    public static LogicalKeyboardKey arrowRight() { return arrowRight; }
    public static LogicalKeyboardKey enter() { return enter; }
    public static LogicalKeyboardKey numpadEnter() { return numpadEnter; }
    public static LogicalKeyboardKey escape() { return escape; }
    public static LogicalKeyboardKey tab() { return tab; }
    public static LogicalKeyboardKey space() { return space; }
    public static LogicalKeyboardKey backspace() { return backspace; }
    public static LogicalKeyboardKey delete() { return delete; }
    public static LogicalKeyboardKey home() { return home; }
    public static LogicalKeyboardKey end() { return end; }
    public static LogicalKeyboardKey pageUp() { return pageUp; }
    public static LogicalKeyboardKey pageDown() { return pageDown; }
    public static LogicalKeyboardKey shift() { return shift; }
    public static LogicalKeyboardKey control() { return control; }
    public static LogicalKeyboardKey meta() { return meta; }
    public static LogicalKeyboardKey alt() { return alt; }
}
