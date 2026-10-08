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

/// The text and span bookkeeping [SpannableString] and [SpannedString]
/// share. As on Android, the two are siblings rather than one extending the
/// other: a [SpannedString] must not be a [Spannable], or code that branches
/// on `instanceof Spannable` could edit spans the type promises are fixed.
abstract class SpannableStringInternal implements CharSequence, GetChars, Spanned {

    private final String text;
    private final SpanSet spans;

    SpannableStringInternal(CharSequence source) {
        text = source == null ? "" : source.toString();
        if (source instanceof SpannableStringInternal) {
            spans = ((SpannableStringInternal) source).spans.copy();
        } else if (source instanceof Spanned) {
            spans = new SpanSet();
            Spanned sp = (Spanned) source;
            Object[] all = sp.getSpans(0, source.length(), Object.class);
            for (Object o : all) {
                spans.set(o, sp.getSpanStart(o), sp.getSpanEnd(o), sp.getSpanFlags(o));
            }
        } else {
            spans = new SpanSet();
        }
    }

    SpannableStringInternal(String text, SpanSet spans) {
        this.text = text;
        this.spans = spans;
    }

    final SpanSet spans() {
        return spans;
    }

    final void setSpanInternal(Object what, int start, int end, int flags) {
        spans.set(what, start, end, flags);
    }

    final void removeSpanInternal(Object what) {
        spans.remove(what);
    }

    @Override
    public final int length() {
        return text.length();
    }

    @Override
    public final char charAt(int index) {
        return text.charAt(index);
    }

    @Override
    public final void getChars(int start, int end, char[] dest, int destoff) {
        text.getChars(start, end, dest, destoff);
    }

    @Override
    public final String toString() {
        return text;
    }

    @Override
    public final <T> T[] getSpans(int start, int end, Class<T> type) {
        return spans.get(start, end, type);
    }

    @Override
    public final int getSpanStart(Object tag) {
        SpanSet.Entry e = spans.find(tag);
        return e == null ? -1 : e.start;
    }

    @Override
    public final int getSpanEnd(Object tag) {
        SpanSet.Entry e = spans.find(tag);
        return e == null ? -1 : e.end;
    }

    @Override
    public final int getSpanFlags(Object tag) {
        SpanSet.Entry e = spans.find(tag);
        return e == null ? 0 : e.flags;
    }

    @Override
    public final int nextSpanTransition(int start, int limit, Class type) {
        return spans.nextTransition(start, limit, type);
    }

    /// Equal to another [Spanned] with the same characters and the same
    /// spans, ranges and flags in the same order, as on Android. Never equal
    /// to a plain String: `String.equals` rejects this object, and equality
    /// has to be symmetric.
    @Override
    public final boolean equals(Object o) {
        return spannedEquals(this, o);
    }

    @Override
    public final int hashCode() {
        return spannedHashCode(this);
    }

    /// The span-aware equality [SpannableStringBuilder] shares, so every
    /// spanned type answers the same question.
    static boolean spannedEquals(Spanned self, Object o) {
        if (o == self) {
            return true;
        }
        if (!(o instanceof Spanned) || !self.toString().equals(o.toString())) {
            return false;
        }
        Spanned other = (Spanned) o;
        Object[] mine = self.getSpans(0, self.length(), Object.class);
        Object[] theirs = other.getSpans(0, other.length(), Object.class);
        if (mine.length != theirs.length) {
            return false;
        }
        for (int i = 0; i < mine.length; i++) {
            Object a = mine[i];
            Object b = theirs[i];
            // A text set as its own span would recurse forever through equals.
            boolean same = a == self ? b == other : a.equals(b);
            if (!same || self.getSpanStart(a) != other.getSpanStart(b)
                    || self.getSpanEnd(a) != other.getSpanEnd(b)
                    || self.getSpanFlags(a) != other.getSpanFlags(b)) {
                return false;
            }
        }
        return true;
    }

    static int spannedHashCode(Spanned self) {
        int hash = self.toString().hashCode();
        Object[] all = self.getSpans(0, self.length(), Object.class);
        for (Object s : all) {
            if (s != self) {
                hash = hash * 31 + s.hashCode();
            }
            hash = hash * 31 + self.getSpanStart(s);
            hash = hash * 31 + self.getSpanEnd(s);
            hash = hash * 31 + self.getSpanFlags(s);
        }
        return hash;
    }
}
