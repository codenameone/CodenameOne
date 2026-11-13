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
package com.codename1.cli;

/// One key press read from a terminal in raw mode, decoded from the bytes and
/// escape sequences terminals send. [Prompt] is built on these; code that
/// wants keys of its own reads them with [#read].
public final class Key {
    /// What was pressed.
    public enum Type {
        /// A printable character; see [Key#codePoint].
        CHAR,
        /// Enter or Return.
        ENTER,
        /// Tab.
        TAB,
        /// Shift-Tab.
        BACKTAB,
        /// Backspace.
        BACKSPACE,
        /// Delete.
        DELETE,
        /// Insert.
        INSERT,
        /// Escape on its own.
        ESCAPE,
        /// Up arrow.
        UP,
        /// Down arrow.
        DOWN,
        /// Left arrow.
        LEFT,
        /// Right arrow.
        RIGHT,
        /// Home.
        HOME,
        /// End.
        END,
        /// Page Up.
        PAGE_UP,
        /// Page Down.
        PAGE_DOWN,
        /// A function key; see [Key#number].
        FUNCTION,
        /// Control and a letter other than the ones named here; see
        /// [Key#codePoint] for the letter.
        CTRL,
        /// Ctrl-C.
        CTRL_C,
        /// Ctrl-D.
        CTRL_D,
        /// The end of standard input.
        EOF,
        /// An escape sequence this decoder does not know.
        UNKNOWN
    }

    private final Type type;
    private final int codePoint;
    private final boolean alt;

    Key(Type type, int codePoint, boolean alt) {
        this.type = type;
        this.codePoint = codePoint;
        this.alt = alt;
    }

    static Key of(Type type) {
        return new Key(type, 0, false);
    }

    /// What was pressed.
    public Type type() {
        return type;
    }

    /// The character of a [Type#CHAR], the lower-case letter of a
    /// [Type#CTRL], 0 otherwise.
    public int codePoint() {
        return codePoint;
    }

    /// The number of a [Type#FUNCTION] key, 1 for F1.
    public int number() {
        return type == Type.FUNCTION ? codePoint : 0;
    }

    /// Whether Alt (Meta, Option) was held.
    public boolean alt() {
        return alt;
    }

    /// Whether this is the printable character `c`.
    public boolean is(char c) {
        return type == Type.CHAR && codePoint == c;
    }

    /// Reads one key from standard input, which must be in raw mode (see
    /// [Prompt#rawMode]).
    public static Key read() {
        return new KeyReader(Console.backend()).read();
    }

    @Override
    public String toString() {
        String name = type == Type.CHAR ? "CHAR(" + new String(Character.toChars(codePoint)) + ")"
                : type == Type.CTRL ? "CTRL(" + (char) codePoint + ")"
                : type == Type.FUNCTION ? "F" + codePoint : type.name();
        return alt ? "ALT+" + name : name;
    }
}
