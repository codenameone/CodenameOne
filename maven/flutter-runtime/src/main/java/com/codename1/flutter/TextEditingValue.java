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
 * The text/selection/composing snapshot a {@code TextInputFormatter} transforms
 * ({@code TextEditingValue} in Flutter). new_gallery's phone-number formatter
 * reads {@link #text()} / {@link #selection()} and returns a new value built with
 * a collapsed selection.
 *
 * <p>Transpiler surface: the named {@code text:}/{@code selection:}/
 * {@code composing:} constructor parameters map to same-named setters; the Dart
 * getters map to the zero-arg accessors.</p>
 */
public class TextEditingValue {

    /** The empty value (Dart's {@code TextEditingValue.empty}). */
    public static final TextEditingValue empty = new TextEditingValue();

    private String text = "";
    private TextSelection selection = TextSelection.collapsed(-1);
    private TextRange composing = TextRange.empty;

    public TextEditingValue() {
    }

    // Named-parameter setters.
    public void text(String v) {
        this.text = v == null ? "" : v;
    }

    public void selection(TextSelection v) {
        if (v != null) {
            this.selection = v;
        }
    }

    public void composing(TextRange v) {
        if (v != null) {
            this.composing = v;
        }
    }

    public String text() {
        return text;
    }

    public TextSelection selection() {
        return selection;
    }

    public TextRange composing() {
        return composing;
    }

    /** Returns a copy with the supplied fields overridden (null keeps current). */
    public TextEditingValue copyWith(String text, TextSelection selection, TextRange composing) {
        TextEditingValue v = new TextEditingValue();
        v.text = text != null ? text : this.text;
        v.selection = selection != null ? selection : this.selection;
        v.composing = composing != null ? composing : this.composing;
        return v;
    }
}
