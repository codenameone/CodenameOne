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
package android.text;

/// Immutable text with mutable spans.
public class SpannableString extends SpannableStringInternal implements Spannable {

    public SpannableString(CharSequence source) {
        super(source);
    }

    private SpannableString(String text, SpanSet spans) {
        super(text, spans);
    }

    public static SpannableString valueOf(CharSequence source) {
        return source instanceof SpannableString ? (SpannableString) source : new SpannableString(source);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new SpannableString(toString().substring(start, end), spans().slice(start, end));
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        setSpanInternal(what, start, end, flags);
    }

    @Override
    public void removeSpan(Object what) {
        removeSpanInternal(what);
    }
}
