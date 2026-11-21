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
package com.codename1.wordprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Immutable styled text: a sequence of runs, each a string with one `TextStyle`.
///
/// Adjacent runs with equal styles are merged and empty runs are dropped, so two texts that read
/// the same and look the same are `equals`.
public final class RichText {
    /// Receives the runs of a text in order. Subclass it and override what you need; methods may
    /// be added here for new kinds of inline content, and they will do nothing unless overridden.
    public abstract static class Visitor {
        /// Called once per run.
        ///
        /// #### Parameters
        ///
        /// - `offset`: the run's first character within the whole text
        ///
        /// - `text`: the run's characters
        ///
        /// - `style`: the run's style
        public abstract void run(int offset, String text, TextStyle style);
    }

    /// The empty text.
    public static final RichText EMPTY = new RichText(new String[0], new TextStyle[0]);

    private final String[] texts;
    private final TextStyle[] styles;
    private final int length;

    private RichText(String[] texts, TextStyle[] styles) {
        this.texts = texts;
        this.styles = styles;
        int n = 0;
        for (String t : texts) {
            n += t.length();
        }
        this.length = n;
    }

    /// Plain text in the default style.
    ///
    /// #### Parameters
    ///
    /// - `s`: the characters
    ///
    /// #### Returns
    ///
    /// the text
    public static RichText of(String s) {
        return of(s, TextStyle.DEFAULT);
    }

    /// Text in a single style.
    ///
    /// #### Parameters
    ///
    /// - `s`: the characters
    ///
    /// - `style`: their style
    ///
    /// #### Returns
    ///
    /// the text
    public static RichText of(String s, TextStyle style) {
        if (s == null || s.length() == 0) {
            return EMPTY;
        }
        return new RichText(new String[] {s}, new TextStyle[] {style});
    }

    /// Accumulates runs into a `RichText`.
    public static final class Builder {
        private final List<String> texts = new ArrayList<String>();
        private final List<TextStyle> styles = new ArrayList<TextStyle>();

        /// Appends a run, merging it into the previous one when the styles are equal.
        ///
        /// #### Parameters
        ///
        /// - `text`: the characters; null or empty adds nothing
        ///
        /// - `style`: their style
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder append(String text, TextStyle style) {
            if (text == null || text.length() == 0) {
                return this;
            }
            if (style == null) {
                style = TextStyle.DEFAULT;
            }
            int last = texts.size() - 1;
            if (last >= 0 && styles.get(last).equals(style)) {
                texts.set(last, texts.get(last) + text);
            } else {
                texts.add(text);
                styles.add(style);
            }
            return this;
        }

        /// Appends every run of a text.
        ///
        /// #### Parameters
        ///
        /// - `text`: the text to append
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder append(RichText text) {
            for (int i = 0; i < text.texts.length; i++) {
                append(text.texts[i], text.styles[i]);
            }
            return this;
        }

        /// The number of characters appended so far.
        ///
        /// #### Returns
        ///
        /// the length
        public int length() {
            int n = 0;
            for (String t : texts) {
                n += t.length();
            }
            return n;
        }

        /// The text built so far.
        ///
        /// #### Returns
        ///
        /// an immutable text
        public RichText build() {
            if (texts.isEmpty()) {
                return EMPTY;
            }
            return new RichText(texts.toArray(new String[texts.size()]),
                    styles.toArray(new TextStyle[styles.size()]));
        }
    }

    /// The number of characters.
    ///
    /// #### Returns
    ///
    /// the length
    public int length() {
        return length;
    }

    /// Whether the text has no characters.
    ///
    /// #### Returns
    ///
    /// true when empty
    public boolean isEmpty() {
        return length == 0;
    }

    /// The number of runs.
    ///
    /// #### Returns
    ///
    /// the run count
    public int runCount() {
        return texts.length;
    }

    /// The characters of one run.
    ///
    /// #### Parameters
    ///
    /// - `index`: the run, from zero
    ///
    /// #### Returns
    ///
    /// the run's text
    public String runText(int index) {
        return texts[index];
    }

    /// The style of one run.
    ///
    /// #### Parameters
    ///
    /// - `index`: the run, from zero
    ///
    /// #### Returns
    ///
    /// the run's style
    public TextStyle runStyle(int index) {
        return styles[index];
    }

    /// The characters without formatting.
    ///
    /// #### Returns
    ///
    /// the plain text
    public String plainText() {
        if (texts.length == 1) {
            return texts[0];
        }
        StringBuilder b = new StringBuilder(length);
        for (String t : texts) {
            b.append(t);
        }
        return b.toString();
    }

    /// The style at a character offset. At the end of the text, and for an empty text, this is
    /// the style of the last run, so text typed there continues it.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the character offset
    ///
    /// #### Returns
    ///
    /// the style in effect
    public TextStyle styleAt(int offset) {
        int start = 0;
        for (int i = 0; i < texts.length; i++) {
            start += texts[i].length();
            if (offset < start) {
                return styles[i];
            }
        }
        return texts.length == 0 ? TextStyle.DEFAULT : styles[texts.length - 1];
    }

    /// Calls the visitor once per run, in order.
    ///
    /// #### Parameters
    ///
    /// - `visitor`: the receiver
    public void visit(Visitor visitor) {
        int offset = 0;
        for (int i = 0; i < texts.length; i++) {
            visitor.run(offset, texts[i], styles[i]);
            offset += texts[i].length();
        }
    }

    /// The characters in `[start, end)` with their styles.
    ///
    /// #### Parameters
    ///
    /// - `start`: the first offset, inclusive
    ///
    /// - `end`: the last offset, exclusive
    ///
    /// #### Returns
    ///
    /// the slice
    public RichText substring(int start, int end) {
        if (start < 0 || end > length || start > end) {
            throw new IndexOutOfBoundsException(start + ".." + end + " of " + length);
        }
        Builder b = new Builder();
        int pos = 0;
        for (int i = 0; i < texts.length; i++) {
            int runEnd = pos + texts[i].length();
            int s = Math.max(start, pos);
            int e = Math.min(end, runEnd);
            if (s < e) {
                b.append(texts[i].substring(s - pos, e - pos), styles[i]);
            }
            pos = runEnd;
        }
        return b.build();
    }

    /// This text with `insert` placed at an offset.
    ///
    /// #### Parameters
    ///
    /// - `offset`: where to insert
    ///
    /// - `insert`: the text to insert
    ///
    /// #### Returns
    ///
    /// the new text
    public RichText insert(int offset, RichText insert) {
        return new Builder().append(substring(0, offset)).append(insert)
                .append(substring(offset, length)).build();
    }

    /// This text without the characters in `[start, end)`.
    ///
    /// #### Parameters
    ///
    /// - `start`: the first offset, inclusive
    ///
    /// - `end`: the last offset, exclusive
    ///
    /// #### Returns
    ///
    /// the new text
    public RichText delete(int start, int end) {
        return new Builder().append(substring(0, start)).append(substring(end, length)).build();
    }

    /// This text followed by another.
    ///
    /// #### Parameters
    ///
    /// - `other`: the text to append
    ///
    /// #### Returns
    ///
    /// the new text
    public RichText append(RichText other) {
        return new Builder().append(this).append(other).build();
    }

    /// Restyles the characters in `[start, end)`.
    ///
    /// #### Parameters
    ///
    /// - `start`: the first offset, inclusive
    ///
    /// - `end`: the last offset, exclusive
    ///
    /// - `mapper`: computes each run's new style from its old one
    ///
    /// #### Returns
    ///
    /// the new text
    public RichText restyle(int start, int end, StyleMapper mapper) {
        Builder b = new Builder().append(substring(0, start));
        RichText middle = substring(start, end);
        for (int i = 0; i < middle.texts.length; i++) {
            b.append(middle.texts[i], mapper.map(middle.styles[i]));
        }
        return b.append(substring(end, length)).build();
    }

    /// Computes a new style from an old one, for `restyle`.
    public interface StyleMapper {
        /// Maps one style.
        ///
        /// #### Parameters
        ///
        /// - `style`: the style a run has
        ///
        /// #### Returns
        ///
        /// the style it should have
        TextStyle map(TextStyle style);
    }

    /// The distinct styles used, in order of first use.
    ///
    /// #### Returns
    ///
    /// an unmodifiable list
    public List<TextStyle> styles() {
        List<TextStyle> out = new ArrayList<TextStyle>();
        for (TextStyle s : styles) {
            if (!out.contains(s)) {
                out.add(s);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /// Two texts are equal when they have the same runs.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other object
    ///
    /// #### Returns
    ///
    /// true when equal
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RichText)) {
            return false;
        }
        RichText r = (RichText) o;
        if (r.texts.length != texts.length) {
            return false;
        }
        for (int i = 0; i < texts.length; i++) {
            if (!texts[i].equals(r.texts[i]) || !styles[i].equals(r.styles[i])) {
                return false;
            }
        }
        return true;
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        int h = 1;
        for (int i = 0; i < texts.length; i++) {
            h = h * 31 + texts[i].hashCode();
            h = h * 31 + styles[i].hashCode();
        }
        return h;
    }

    /// The plain text, for debugging.
    ///
    /// #### Returns
    ///
    /// the characters
    @Override
    public String toString() {
        return plainText();
    }
}
