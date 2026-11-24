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

/// Immutable styled text: a sequence of runs, each a string with one `TextStyle`, and inline
/// objects -- fields, note references, pictures and embedded components -- each of which takes
/// exactly one character, `OBJECT`.
///
/// Adjacent text runs with equal styles are merged and empty runs are dropped, so two texts that
/// read the same and look the same are `equals`. An object run is never merged.
public final class RichText {
    /// The character an inline object occupies, U+FFFC OBJECT REPLACEMENT CHARACTER.
    public static final char OBJECT = 0xfffc;

    private static final String OBJECT_STRING = String.valueOf(OBJECT);

    /// Receives the runs of a text in order. Subclass it and override what you need; methods may
    /// be added here for new kinds of inline content, and they will do nothing unless overridden.
    public abstract static class Visitor {
        /// Called once per text run.
        ///
        /// #### Parameters
        ///
        /// - `offset`: the run's first character within the whole text
        ///
        /// - `text`: the run's characters
        ///
        /// - `style`: the run's style
        public abstract void run(int offset, String text, TextStyle style);

        /// Called once per inline object. Does nothing unless overridden.
        ///
        /// #### Parameters
        ///
        /// - `offset`: the object's character within the whole text
        ///
        /// - `object`: the object
        ///
        /// - `style`: the style the object's character has
        public void inline(int offset, Inline object, TextStyle style) {
        }
    }

    /// The empty text.
    public static final RichText EMPTY = new RichText(new String[0], new TextStyle[0], new Inline[0]);

    private final String[] texts;
    private final TextStyle[] styles;
    private final Inline[] objects;
    private final int length;

    private RichText(String[] texts, TextStyle[] styles, Inline[] objects) {
        this.texts = texts;
        this.styles = styles;
        this.objects = objects;
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
        return new Builder().append(s, style).build();
    }

    /// A text holding one inline object.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object
    ///
    /// - `style`: the style of its character
    ///
    /// #### Returns
    ///
    /// the text
    public static RichText of(Inline object, TextStyle style) {
        return new Builder().append(object, style).build();
    }

    /// Accumulates runs into a `RichText`.
    public static final class Builder {
        private final List<String> texts = new ArrayList<String>();
        private final List<TextStyle> styles = new ArrayList<TextStyle>();
        private final List<Inline> objects = new ArrayList<Inline>();
        private int length;

        /// Appends a run, merging it into the previous one when the styles are equal. An
        /// `OBJECT` character in plain text is replaced by a space: only `append(Inline, ...)`
        /// places objects.
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
            if (text.indexOf(OBJECT) >= 0) {
                text = text.replace(OBJECT, ' ');
            }
            if (style == null) {
                style = TextStyle.DEFAULT;
            }
            int last = texts.size() - 1;
            if (last >= 0 && objects.get(last) == null && styles.get(last).equals(style)) {
                texts.set(last, texts.get(last) + text);
            } else {
                texts.add(text);
                styles.add(style);
                objects.add(null);
            }
            length += text.length();
            return this;
        }

        /// Appends an inline object.
        ///
        /// #### Parameters
        ///
        /// - `object`: the object; null adds nothing
        ///
        /// - `style`: the style of its character
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder append(Inline object, TextStyle style) {
            if (object == null) {
                return this;
            }
            texts.add(OBJECT_STRING);
            styles.add(style == null ? TextStyle.DEFAULT : style);
            objects.add(object);
            length++;
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
                if (text.objects[i] != null) {
                    append(text.objects[i], text.styles[i]);
                } else {
                    append(text.texts[i], text.styles[i]);
                }
            }
            return this;
        }

        /// The number of characters appended so far.
        ///
        /// #### Returns
        ///
        /// the length
        public int length() {
            return length;
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
                    styles.toArray(new TextStyle[styles.size()]),
                    objects.toArray(new Inline[objects.size()]));
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

    /// The number of runs, inline objects included.
    ///
    /// #### Returns
    ///
    /// the run count
    public int runCount() {
        return texts.length;
    }

    /// The characters of one run; `OBJECT` for an inline object.
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

    /// The inline object of one run.
    ///
    /// #### Parameters
    ///
    /// - `index`: the run, from zero
    ///
    /// #### Returns
    ///
    /// the object, or null for a text run
    public Inline runObject(int index) {
        return objects[index];
    }

    /// The inline object at a character offset.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the character offset
    ///
    /// #### Returns
    ///
    /// the object, or null when the character is not an object
    public Inline objectAt(int offset) {
        int start = 0;
        for (int i = 0; i < texts.length; i++) {
            start += texts[i].length();
            if (offset < start) {
                return objects[i];
            }
        }
        return null;
    }

    /// Whether the text holds any inline object.
    ///
    /// #### Returns
    ///
    /// true when it does
    public boolean hasObjects() {
        for (Inline o : objects) {
            if (o != null) {
                return true;
            }
        }
        return false;
    }

    /// The characters without formatting. Every inline object is one `OBJECT` character.
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

    /// The text as a reader sees it: a field shows its result, a note reference its mark, and
    /// other objects nothing.
    ///
    /// #### Returns
    ///
    /// the readable text
    public String displayText() {
        StringBuilder b = new StringBuilder(length);
        for (int i = 0; i < texts.length; i++) {
            if (objects[i] == null) {
                b.append(texts[i]);
            } else {
                b.append(objects[i].displayText());
            }
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
            if (objects[i] != null) {
                visitor.inline(offset, objects[i], styles[i]);
            } else {
                visitor.run(offset, texts[i], styles[i]);
            }
            offset += texts[i].length();
        }
    }

    /// The characters in `[start, end)` with their styles and objects.
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
                if (objects[i] != null) {
                    b.append(objects[i], styles[i]);
                } else {
                    b.append(texts[i].substring(s - pos, e - pos), styles[i]);
                }
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
            if (middle.objects[i] != null) {
                b.append(middle.objects[i], mapper.map(middle.styles[i]));
            } else {
                b.append(middle.texts[i], mapper.map(middle.styles[i]));
            }
        }
        return b.append(substring(end, length)).build();
    }

    /// This text with the object at `offset` replaced.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the object's character
    ///
    /// - `object`: the replacement
    ///
    /// #### Returns
    ///
    /// the new text
    public RichText withObject(int offset, Inline object) {
        if (objectAt(offset) == null) {
            throw new IllegalArgumentException("No object at " + offset);
        }
        return new Builder().append(substring(0, offset)).append(object, styleAt(offset))
                .append(substring(offset + 1, length)).build();
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

    /// Two texts are equal when they have the same runs and objects.
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
            Inline a = objects[i];
            Inline b = r.objects[i];
            if (a == null ? b != null : !a.equals(b)) {
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
            h = h * 31 + (objects[i] == null ? 0 : objects[i].hashCode());
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
