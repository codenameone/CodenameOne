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
package com.codename1.desktopcompat.javax.swing.text;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.event.DocumentEvent;
import java.util.ArrayList;
import java.util.Enumeration;

/// Text with attributes.
///
/// The characters are kept in runs, each a stretch with one set of
/// attributes, and every paragraph -- the text up to and including a
/// line break -- has a set of its own. Text that is inserted takes the
/// attributes it is inserted with, none when they are `null`; a paragraph
/// that is split passes its attributes on to both halves, and two that
/// are joined keep those of the first.
///
/// The element tree is the JDK's: a section, its paragraphs, and in each
/// the runs that lie in it. An attribute a run lacks is looked up in its
/// paragraph, and one the paragraph lacks in its logical style, which is
/// the context's default style until another is set. Elements are made
/// when they are asked for and describe the document as it was then; ask
/// again after changing it.
public class DefaultStyledDocument extends AbstractDocument implements StyledDocument {

    public static final int BUFFER_SIZE_DEFAULT = 4096;

    private final StyleContext context;
    private final ArrayList<Run> runs = new ArrayList<Run>();
    private final ArrayList<MutableAttributeSet> paragraphs = new ArrayList<MutableAttributeSet>();
    private final ChangeListener styleListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            styleChanged();
        }
    };
    private final Section section = new Section();

    public DefaultStyledDocument() {
        this(new StyleContext());
    }

    public DefaultStyledDocument(StyleContext styles) {
        context = styles;
        paragraphs.add(newParagraph(null));
        Style def = styles.getStyle(StyleContext.DEFAULT_STYLE);
        if (def != null) {
            def.removeChangeListener(styleListener);
            def.addChangeListener(styleListener);
        }
    }

    private static final class Run {
        int length;
        AttributeSet attributes;

        Run(int length, AttributeSet attributes) {
            this.length = length;
            this.attributes = attributes;
        }
    }

    private MutableAttributeSet newParagraph(AttributeSet like) {
        SimpleAttributeSet p = new SimpleAttributeSet();
        if (like != null) {
            p.addAttributes(like);
        } else {
            Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
            if (def != null) {
                p.setResolveParent(def);
            }
        }
        return p;
    }

    /// A style of this document changed: everything is drawn again.
    private void styleChanged() {
        changed(0, getLength());
    }

    private void changed(int offset, int length) {
        DefaultDocumentEvent e = new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);
        e.end();
        fireChangedUpdate(e);
    }

    // ------------------------------------------------------------ styles

    @Override
    public Style addStyle(String nm, Style parent) {
        Style s = context.addStyle(nm, parent);
        s.addChangeListener(styleListener);
        return s;
    }

    @Override
    public void removeStyle(String nm) {
        Style s = context.getStyle(nm);
        if (s != null) {
            s.removeChangeListener(styleListener);
        }
        context.removeStyle(nm);
    }

    @Override
    public Style getStyle(String nm) {
        return context.getStyle(nm);
    }

    public Enumeration<?> getStyleNames() {
        return context.getStyleNames();
    }

    @Override
    public Color getForeground(AttributeSet attr) {
        return context.getForeground(attr);
    }

    @Override
    public Color getBackground(AttributeSet attr) {
        return context.getBackground(attr);
    }

    @Override
    public Font getFont(AttributeSet attr) {
        return context.getFont(attr);
    }

    // ------------------------------------------------------------ text

    private static int breaks(CharSequence s, int from, int to) {
        int n = 0;
        for (int i = from; i < to; i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    /// Cuts the runs so that one begins at `offset`, and answers its
    /// index; the number of runs when `offset` is the end of the text.
    private int splitAt(int offset) {
        int at = 0;
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (at == offset) {
                return i;
            }
            if (offset < at + r.length) {
                int head = offset - at;
                runs.add(i + 1, new Run(r.length - head, r.attributes));
                r.length = head;
                return i + 1;
            }
            at += r.length;
        }
        return runs.size();
    }

    /// Joins neighbours with equal attributes, and drops empty runs.
    private void tidy() {
        for (int i = runs.size() - 1; i >= 0; i--) {
            Run r = runs.get(i);
            if (r.length <= 0) {
                runs.remove(i);
            } else if (i + 1 < runs.size() && runs.get(i + 1).attributes.isEqual(r.attributes)) {
                r.length += runs.get(i + 1).length;
                runs.remove(i + 1);
            }
        }
    }

    @Override
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        // The text is in already.
        int offs = chng.getOffset();
        int len = chng.getLength();
        CharSequence text = cn1Chars();
        AttributeSet a = attr == null || attr.getAttributeCount() == 0 ? SimpleAttributeSet.EMPTY
                : attr.copyAttributes();
        runs.add(splitAt(offs), new Run(len, a));
        tidy();
        int para = Math.min(breaks(text, 0, offs), paragraphs.size() - 1);
        int added = breaks(text, offs, offs + len);
        for (int i = 0; i < added; i++) {
            paragraphs.add(para + 1, newParagraph(paragraphs.get(para)));
        }
        super.insertUpdate(chng, attr);
    }

    @Override
    protected void removeUpdate(DefaultDocumentEvent chng) {
        // The text is still there.
        int offs = chng.getOffset();
        int len = chng.getLength();
        CharSequence text = cn1Chars();
        int from = splitAt(offs);
        int to = splitAt(offs + len);
        for (int i = to - 1; i >= from; i--) {
            runs.remove(i);
        }
        tidy();
        int para = breaks(text, 0, offs);
        int gone = breaks(text, offs, offs + len);
        for (int i = 0; i < gone && para + 1 < paragraphs.size(); i++) {
            paragraphs.remove(para + 1);
        }
        super.removeUpdate(chng);
    }

    @Override
    public void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace) {
        if (length <= 0 || s == null) {
            return;
        }
        int end = Math.min(getLength(), offset + length);
        int start = Math.max(0, offset);
        if (start >= end) {
            return;
        }
        int from = splitAt(start);
        int to = splitAt(end);
        for (int i = from; i < to; i++) {
            Run r = runs.get(i);
            SimpleAttributeSet now = new SimpleAttributeSet();
            if (!replace) {
                now.addAttributes(r.attributes);
            }
            now.addAttributes(s);
            r.attributes = now;
        }
        tidy();
        changed(start, end - start);
    }

    @Override
    public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace) {
        if (s == null) {
            return;
        }
        int first = paragraphIndex(offset);
        int last = paragraphIndex(offset + (length > 0 ? length - 1 : 0));
        for (int i = first; i <= last; i++) {
            MutableAttributeSet p = paragraphs.get(i);
            if (replace) {
                p.removeAttributes(p);
            }
            p.addAttributes(s);
        }
        int start = paragraphStart(first);
        changed(start, Math.max(0, paragraphEnd(last) - start));
    }

    @Override
    public void setLogicalStyle(int pos, Style s) {
        MutableAttributeSet p = paragraphs.get(paragraphIndex(pos));
        if (s != null) {
            p.setResolveParent(s);
            s.removeChangeListener(styleListener);
            s.addChangeListener(styleListener);
        } else {
            p.removeAttribute(StyleConstants.ResolveAttribute);
        }
        int i = paragraphIndex(pos);
        int start = paragraphStart(i);
        changed(start, Math.max(0, paragraphEnd(i) - start));
    }

    @Override
    public Style getLogicalStyle(int p) {
        AttributeSet a = paragraphs.get(paragraphIndex(p)).getResolveParent();
        return a instanceof Style ? (Style) a : null;
    }

    // ------------------------------------------------------------ structure

    /// The paragraph an offset is in; one past the end is in the last.
    private int paragraphIndex(int pos) {
        CharSequence text = cn1Chars();
        int p = Math.max(0, Math.min(pos, text.length()));
        return Math.min(breaks(text, 0, p), paragraphs.size() - 1);
    }

    private int paragraphStart(int index) {
        CharSequence text = cn1Chars();
        int n = 0;
        for (int i = 0; i < text.length() && n < index; i++) {
            if (text.charAt(i) == '\n') {
                n++;
                if (n == index) {
                    return i + 1;
                }
            }
        }
        return index <= 0 ? 0 : text.length();
    }

    /// One past the paragraph: past its line break, or past the break
    /// that is taken to end the document.
    private int paragraphEnd(int index) {
        CharSequence text = cn1Chars();
        for (int i = paragraphStart(index); i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return text.length() + 1;
    }

    @Override
    public Element getDefaultRootElement() {
        return section;
    }

    @Override
    public Element getParagraphElement(int pos) {
        return new Paragraph(paragraphIndex(pos));
    }

    @Override
    public Element getCharacterElement(int pos) {
        Paragraph p = new Paragraph(paragraphIndex(pos));
        int clamped = Math.max(0, Math.min(pos, getLength()));
        return p.getElement(p.getElementIndex(clamped));
    }

    /// What an element answers for its attributes: its own, and what
    /// they lack looked up in the element above.
    private static final class Attributes implements AttributeSet {

        private final AttributeSet own;
        private final AttributeSet parent;

        Attributes(AttributeSet own, AttributeSet parent) {
            this.own = own;
            this.parent = parent;
        }

        @Override
        public int getAttributeCount() {
            return own.getAttributeCount();
        }

        @Override
        public boolean isDefined(Object attrName) {
            return own.isDefined(attrName);
        }

        @Override
        public boolean isEqual(AttributeSet attr) {
            return attr != null && getAttributeCount() == attr.getAttributeCount() && containsAttributes(attr);
        }

        @Override
        public AttributeSet copyAttributes() {
            return new SimpleAttributeSet(own);
        }

        @Override
        public Object getAttribute(Object key) {
            Object v = own.getAttribute(key);
            if (v == null && parent != null) {
                v = parent.getAttribute(key);
            }
            return v;
        }

        @Override
        public Enumeration<?> getAttributeNames() {
            return own.getAttributeNames();
        }

        @Override
        public boolean containsAttribute(Object name, Object value) {
            return value != null && value.equals(getAttribute(name));
        }

        @Override
        public boolean containsAttributes(AttributeSet attributes) {
            Enumeration<?> names = attributes.getAttributeNames();
            while (names.hasMoreElements()) {
                Object name = names.nextElement();
                Object v = attributes.getAttribute(name);
                if (v == null || !v.equals(getAttribute(name))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public AttributeSet getResolveParent() {
            AttributeSet set = own.getResolveParent();
            return set != null ? set : parent;
        }
    }

    private final class Section implements Element {

        @Override
        public Document getDocument() {
            return DefaultStyledDocument.this;
        }

        @Override
        public Element getParentElement() {
            return null;
        }

        @Override
        public String getName() {
            return SectionElementName;
        }

        @Override
        public AttributeSet getAttributes() {
            return SimpleAttributeSet.EMPTY;
        }

        @Override
        public int getStartOffset() {
            return 0;
        }

        @Override
        public int getEndOffset() {
            return getLength() + 1;
        }

        @Override
        public int getElementIndex(int offset) {
            return paragraphIndex(offset);
        }

        @Override
        public int getElementCount() {
            return paragraphs.size();
        }

        @Override
        public Element getElement(int index) {
            return index < 0 || index >= paragraphs.size() ? null : new Paragraph(index);
        }

        @Override
        public boolean isLeaf() {
            return false;
        }
    }

    private final class Paragraph implements Element {

        private final int index;
        private final int start;
        private final int end;
        private final AttributeSet attributes;
        private final int[] bounds;
        private final AttributeSet[] sets;

        Paragraph(int index) {
            this.index = index;
            start = paragraphStart(index);
            end = paragraphEnd(index);
            attributes = paragraphs.get(index);
            // The runs that lie in the paragraph, cut to it. The break
            // taken to end the document belongs to the last run.
            ArrayList<int[]> cuts = new ArrayList<int[]>();
            ArrayList<AttributeSet> attrs = new ArrayList<AttributeSet>();
            int at = 0;
            for (int i = 0; i < runs.size(); i++) {
                Run r = runs.get(i);
                int a = Math.max(at, start);
                int b = Math.min(at + r.length, end);
                if (a < b) {
                    cuts.add(new int[]{a, b});
                    attrs.add(r.attributes);
                }
                at += r.length;
            }
            if (cuts.isEmpty()) {
                cuts.add(new int[]{start, end});
                attrs.add(SimpleAttributeSet.EMPTY);
            } else {
                cuts.get(cuts.size() - 1)[1] = end;
            }
            bounds = new int[cuts.size() + 1];
            sets = new AttributeSet[cuts.size()];
            for (int i = 0; i < cuts.size(); i++) {
                bounds[i] = cuts.get(i)[0];
                sets[i] = attrs.get(i);
            }
            bounds[cuts.size()] = end;
        }

        @Override
        public Document getDocument() {
            return DefaultStyledDocument.this;
        }

        @Override
        public Element getParentElement() {
            return section;
        }

        @Override
        public String getName() {
            return ParagraphElementName;
        }

        @Override
        public AttributeSet getAttributes() {
            return attributes;
        }

        @Override
        public int getStartOffset() {
            return start;
        }

        @Override
        public int getEndOffset() {
            return end;
        }

        @Override
        public int getElementIndex(int offset) {
            for (int i = 0; i < sets.length; i++) {
                if (offset < bounds[i + 1]) {
                    return i;
                }
            }
            return sets.length - 1;
        }

        @Override
        public int getElementCount() {
            return sets.length;
        }

        @Override
        public Element getElement(int i) {
            if (i < 0 || i >= sets.length) {
                return null;
            }
            return new Content(this, bounds[i], bounds[i + 1], new Attributes(sets[i], attributes));
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public String toString() {
            return "BranchElement(" + getName() + ") " + start + "," + end + " #" + index;
        }
    }

    private final class Content implements Element {

        private final Element parent;
        private final int start;
        private final int end;
        private final AttributeSet attributes;

        Content(Element parent, int start, int end, AttributeSet attributes) {
            this.parent = parent;
            this.start = start;
            this.end = end;
            this.attributes = attributes;
        }

        @Override
        public Document getDocument() {
            return DefaultStyledDocument.this;
        }

        @Override
        public Element getParentElement() {
            return parent;
        }

        @Override
        public String getName() {
            return ContentElementName;
        }

        @Override
        public AttributeSet getAttributes() {
            return attributes;
        }

        @Override
        public int getStartOffset() {
            return start;
        }

        @Override
        public int getEndOffset() {
            return end;
        }

        @Override
        public int getElementIndex(int offset) {
            return -1;
        }

        @Override
        public int getElementCount() {
            return 0;
        }

        @Override
        public Element getElement(int i) {
            return null;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public String toString() {
            return "LeafElement(" + getName() + ") " + start + "," + end;
        }
    }
}
