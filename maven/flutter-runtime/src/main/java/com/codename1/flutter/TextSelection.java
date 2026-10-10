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
 * A selected range within editable text ({@code TextSelection} in Flutter),
 * extending {@link TextRange} with a base/extent (anchor/caret) pair. The
 * new_gallery phone-number formatter reads {@code selection.end} and produces a
 * collapsed selection via {@link #collapsed(int)}.
 */
public class TextSelection extends TextRange {

    private long baseOffset;
    private long extentOffset;

    public TextSelection() {
    }

    // Named-parameter setters.
    public void baseOffset(long v) {
        this.baseOffset = v;
        syncRange();
    }

    public void extentOffset(long v) {
        this.extentOffset = v;
        syncRange();
    }

    /** {@code TextSelection.collapsed(offset: ...)} — a zero-length selection. */
    public static TextSelection collapsed(long offset) {
        TextSelection s = new TextSelection();
        s.baseOffset = offset;
        s.extentOffset = offset;
        s.syncRange();
        return s;
    }

    public long baseOffset() {
        return baseOffset;
    }

    public long extentOffset() {
        return extentOffset;
    }

    private void syncRange() {
        start(Math.min(baseOffset, extentOffset));
        end(Math.max(baseOffset, extentOffset));
    }
}
