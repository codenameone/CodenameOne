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

/**
 * A range of characters in a string of text ({@code TextRange} in Flutter). The
 * base type for {@link TextSelection}; {@link TextEditingValue#composing()} is a
 * bare {@code TextRange}. A collapsed range has {@code start == end}; an invalid
 * range uses {@code -1} for both.
 *
 * <p>Transpiler surface: the named {@code start:}/{@code end:} constructor
 * parameters map to the {@link #start(int)}/{@link #end(int)} setters, the Dart
 * getters to the zero-arg {@link #start()}/{@link #end()} accessors.</p>
 */
public class TextRange {

    /** An invalid, empty range (Dart's {@code TextRange.empty}). */
    public static final TextRange empty = new TextRange(-1, -1);

    private long start = -1;
    private long end = -1;

    public TextRange() {
    }

    public TextRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    // Named-parameter setters.
    public void start(long v) {
        this.start = v;
    }

    public void end(long v) {
        this.end = v;
    }

    // Named constructor / static getter.
    public static TextRange collapsed(long offset) {
        return new TextRange(offset, offset);
    }

    // Getters.
    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public boolean isValid() {
        return start >= 0 && end >= 0;
    }

    public boolean isCollapsed() {
        return start == end;
    }
}
