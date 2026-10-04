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
public class SpannableString implements CharSequence, GetChars, Spannable {

    private final String text;
    private final SpanSet spans;

    public SpannableString(CharSequence source) {
        text = source == null ? "" : source.toString();
        if (source instanceof SpannableString) {
            spans = ((SpannableString) source).spans.copy();
        } else if (source instanceof SpannableStringBuilder) {
            spans = new SpanSet();
            Object[] all = ((SpannableStringBuilder) source).getSpans(0, source.length(), Object.class);
            for (Object o : all) {
                SpannableStringBuilder b = (SpannableStringBuilder) source;
                spans.set(o, b.getSpanStart(o), b.getSpanEnd(o), b.getSpanFlags(o));
            }
        } else {
            spans = new SpanSet();
        }
    }

    private SpannableString(String text, SpanSet spans) {
        this.text = text;
        this.spans = spans;
    }

    public static SpannableString valueOf(CharSequence source) {
        return source instanceof SpannableString ? (SpannableString) source : new SpannableString(source);
    }

    SpanSet spans() {
        return spans;
    }

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public char charAt(int index) {
        return text.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new SpannableString(text.substring(start, end), spans.slice(start, end));
    }

    @Override
    public void getChars(int start, int end, char[] dest, int destoff) {
        text.getChars(start, end, dest, destoff);
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        spans.set(what, start, end, flags);
    }

    @Override
    public void removeSpan(Object what) {
        spans.remove(what);
    }

    @Override
    public <T> T[] getSpans(int start, int end, Class<T> type) {
        return spans.get(start, end, type);
    }

    @Override
    public int getSpanStart(Object tag) {
        SpanSet.Entry e = spans.find(tag);
        return e == null ? -1 : e.start;
    }

    @Override
    public int getSpanEnd(Object tag) {
        SpanSet.Entry e = spans.find(tag);
        return e == null ? -1 : e.end;
    }

    @Override
    public int getSpanFlags(Object tag) {
        SpanSet.Entry e = spans.find(tag);
        return e == null ? 0 : e.flags;
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class type) {
        return spans.nextTransition(start, limit, type);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CharSequence && o.toString().equals(text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }
}
