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

/// Mutable text with spans.
public class SpannableStringBuilder implements CharSequence, GetChars, Spannable, Editable, Appendable {

    private final StringBuilder text;
    private final SpanSet spans;
    private InputFilter[] filters = new InputFilter[0];
    private final java.util.ArrayList<ChangeHook> watchers = new java.util.ArrayList<ChangeHook>();

    /// Runtime use: told before and after every change, with the range
    /// replaced (`start`, `before` characters) and the length of what
    /// replaced it (`after`), so an EditText can notify its TextWatchers.
    public interface ChangeHook {
        /// The text is about to change.
        void beforeChange(SpannableStringBuilder text, int start, int before, int after);

        /// The text has changed.
        void afterChange(SpannableStringBuilder text, int start, int before, int after);
    }

    public SpannableStringBuilder() {
        text = new StringBuilder();
        spans = new SpanSet();
    }

    public SpannableStringBuilder(CharSequence source) {
        this(source, 0, source == null ? 0 : source.length());
    }

    public SpannableStringBuilder(CharSequence source, int start, int end) {
        // Not source.subSequence: for a builder that is this constructor.
        text = new StringBuilder(source == null ? "" : source.toString().substring(start, end));
        if (source instanceof SpannableStringBuilder) {
            spans = ((SpannableStringBuilder) source).spans.slice(start, end);
        } else if (source instanceof SpannableStringInternal) {
            spans = ((SpannableStringInternal) source).spans().slice(start, end);
        } else {
            spans = new SpanSet();
        }
    }

    public static SpannableStringBuilder valueOf(CharSequence source) {
        return source instanceof SpannableStringBuilder ? (SpannableStringBuilder) source
                : new SpannableStringBuilder(source);
    }

    /// Runtime use: called after every change, so an EditText can follow.
    public void addChangeHook(ChangeHook r) {
        watchers.add(r);
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
        return new SpannableStringBuilder(this, start, end);
    }

    @Override
    public void getChars(int start, int end, char[] dest, int destoff) {
        text.getChars(start, end, dest, destoff);
    }

    @Override
    public String toString() {
        return text.toString();
    }

    @Override
    public SpannableStringBuilder replace(int st, int en, CharSequence source, int start, int end) {
        CharSequence src = source.subSequence(start, end);
        for (InputFilter f : filters) {
            CharSequence r = f.filter(src, 0, src.length(), this, st, en);
            if (r != null) {
                src = r;
            }
        }
        int after = src.length();
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).beforeChange(this, st, en - st, after);
        }
        // StringBuilder.replace is not in the Codename One runtime.
        text.delete(st, en);
        text.insert(st, src.toString());
        spans.replaced(st, en, after);
        if (src instanceof Spanned) {
            // The inserted text keeps its own styling, moved to where it
            // landed, as Android's replace copies the source's spans.
            Spanned sp = (Spanned) src;
            Object[] inserted = sp.getSpans(0, after, Object.class);
            for (Object what : inserted) {
                spans.set(what, st + sp.getSpanStart(what), st + sp.getSpanEnd(what), sp.getSpanFlags(what));
            }
        }
        for (int i = 0; i < watchers.size(); i++) {
            watchers.get(i).afterChange(this, st, en - st, after);
        }
        return this;
    }

    @Override
    public SpannableStringBuilder replace(int st, int en, CharSequence text) {
        return replace(st, en, text, 0, text.length());
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence tb, int start, int end) {
        return replace(where, where, tb, start, end);
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence tb) {
        return replace(where, where, tb, 0, tb.length());
    }

    @Override
    public SpannableStringBuilder delete(int start, int end) {
        return replace(start, end, "", 0, 0);
    }

    @Override
    public SpannableStringBuilder append(CharSequence t) {
        if (t == null) {
            t = "null";
        }
        return replace(length(), length(), t, 0, t.length());
    }

    public SpannableStringBuilder append(CharSequence t, Object what, int flags) {
        int start = length();
        append(t);
        setSpan(what, start, length(), flags);
        return this;
    }

    @Override
    public SpannableStringBuilder append(CharSequence t, int start, int end) {
        return replace(length(), length(), t, start, end);
    }

    @Override
    public SpannableStringBuilder append(char t) {
        return append(String.valueOf(t));
    }

    @Override
    public void clear() {
        replace(0, length(), "", 0, 0);
    }

    @Override
    public void clearSpans() {
        spans.entries.clear();
    }

    @Override
    public void setFilters(InputFilter[] filters) {
        this.filters = filters == null ? new InputFilter[0] : filters;
    }

    @Override
    public InputFilter[] getFilters() {
        return filters;
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
        return SpannableStringInternal.spannedEquals(this, o);
    }

    @Override
    public int hashCode() {
        return SpannableStringInternal.spannedHashCode(this);
    }
}
