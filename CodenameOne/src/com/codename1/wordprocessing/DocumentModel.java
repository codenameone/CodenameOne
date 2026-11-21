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
import java.util.List;

/// The editable state behind a document view: the current immutable `Document`, its listeners
/// and its undo history.
///
/// Every change replaces the whole document, so undo is a stack of earlier documents, and since
/// a new document shares its unchanged blocks with the old one the history stays small. Edits
/// that belong together (the letters of one typed word) are joined with `beginGroup`/`endGroup`
/// into one undo step.
///
/// ```java
/// DocumentModel model = new DocumentModel(Document.read(in, Document.Format.NATIVE));
/// model.addDocumentListener(new DocumentListener() {
///     public void documentChanged(DocumentModel m, Document before, Document after) {
///         markDirty();
///     }
/// });
/// model.insertBlock(model.size(), new Paragraph(model.newId('p'), RichText.of("The end")));
/// model.undo();
/// ```
public class DocumentModel {
    private Document document;
    private final List<DocumentListener> listeners = new ArrayList<DocumentListener>();
    private final List<Document> undo = new ArrayList<Document>();
    private final List<Document> redo = new ArrayList<Document>();
    private int undoLimit = 200;
    private int groupDepth;
    private boolean groupRecorded;
    private String typing;
    private boolean autoRecalculate = true;

    /// A model holding a new empty document.
    public DocumentModel() {
        this(new Document());
    }

    /// A model holding a document.
    ///
    /// #### Parameters
    ///
    /// - `document`: the initial document
    public DocumentModel(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document");
        }
        this.document = document;
    }

    /// The current document.
    ///
    /// #### Returns
    ///
    /// the document
    public Document getDocument() {
        return document;
    }

    /// Replaces the document as one undoable edit and notifies the listeners. Setting an equal
    /// document does nothing.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new document
    public void setDocument(Document value) {
        if (value == null) {
            throw new IllegalArgumentException("document");
        }
        Document next = recalculated(value);
        if (next.equals(document) && next.nextId == document.nextId) {
            return;
        }
        if (groupDepth == 0 || !groupRecorded) {
            push(undo, document);
            groupRecorded = groupDepth > 0;
        }
        redo.clear();
        typing = null;
        replace(next);
    }

    /// Whether table formulas are calculated again after every edit.
    ///
    /// #### Returns
    ///
    /// true by default
    public boolean isAutoRecalculate() {
        return autoRecalculate;
    }

    /// Calculates table formulas again after every edit, so a formula field always shows the
    /// value of the cells it reads; or leaves them as they are until `TableSheets.recalculate`.
    ///
    /// #### Parameters
    ///
    /// - `value`: true to calculate after every edit
    public void setAutoRecalculate(boolean value) {
        autoRecalculate = value;
    }

    private Document recalculated(Document value) {
        return autoRecalculate ? TableSheets.recalculate(value) : value;
    }

    /// Replaces the document and forgets the history, as when a different file is opened.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new document
    public void reset(Document value) {
        if (value == null) {
            throw new IllegalArgumentException("document");
        }
        undo.clear();
        redo.clear();
        typing = null;
        replace(value);
    }

    private void replace(Document value) {
        Document before = document;
        document = value;
        DocumentListener[] l = listeners.toArray(new DocumentListener[listeners.size()]);
        for (DocumentListener listener : l) {
            listener.documentChanged(this, before, value);
        }
    }

    private void push(List<Document> stack, Document d) {
        stack.add(d);
        while (stack.size() > undoLimit) {
            stack.remove(0);
        }
    }

    /// Starts joining edits into one undo step. Groups nest; the step ends with the outermost
    /// `endGroup`.
    public void beginGroup() {
        if (groupDepth == 0) {
            groupRecorded = false;
        }
        groupDepth++;
    }

    /// Ends a group started by `beginGroup`.
    public void endGroup() {
        if (groupDepth == 0) {
            throw new IllegalStateException("endGroup without beginGroup");
        }
        groupDepth--;
    }

    /// Whether `undo` would do anything.
    ///
    /// #### Returns
    ///
    /// true when there is history
    public boolean canUndo() {
        return !undo.isEmpty();
    }

    /// Whether `redo` would do anything.
    ///
    /// #### Returns
    ///
    /// true after an undo with no edit since
    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /// Restores the document before the last edit.
    ///
    /// #### Returns
    ///
    /// false when there was nothing to undo
    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        push(redo, document);
        typing = null;
        replace(undo.remove(undo.size() - 1));
        return true;
    }

    /// Re-applies the last undone edit.
    ///
    /// #### Returns
    ///
    /// false when there was nothing to redo
    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        push(undo, document);
        typing = null;
        replace(redo.remove(redo.size() - 1));
        return true;
    }

    /// Sets how many undo steps are kept.
    ///
    /// #### Parameters
    ///
    /// - `limit`: the step count, at least one
    public void setUndoLimit(int limit) {
        undoLimit = Math.max(1, limit);
        while (undo.size() > undoLimit) {
            undo.remove(0);
        }
    }

    /// Adds a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addDocumentListener(DocumentListener l) {
        listeners.add(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeDocumentListener(DocumentListener l) {
        listeners.remove(l);
    }

    /// A fresh block id, reserved so the next call returns another.
    ///
    /// #### Parameters
    ///
    /// - `prefix`: `p` for a paragraph, `t` for a table, `i` for a picture
    ///
    /// #### Returns
    ///
    /// the id
    public String newId(char prefix) {
        String id = document.newId(prefix);
        // Reserving an id is not an edit: it changes nothing a user could undo.
        document = document.withNextId(document.nextId + 1);
        return id;
    }

    /// The number of top-level blocks.
    ///
    /// #### Returns
    ///
    /// the block count
    public int size() {
        return document.size();
    }

    /// One top-level block.
    ///
    /// #### Parameters
    ///
    /// - `index`: from zero
    ///
    /// #### Returns
    ///
    /// the block
    public Block getBlock(int index) {
        return document.get(index);
    }

    /// Replaces a block, as one undoable edit.
    ///
    /// #### Parameters
    ///
    /// - `index`: the block to replace
    ///
    /// - `block`: its replacement
    public void setBlock(int index, Block block) {
        setDocument(document.withBlock(index, block));
    }

    /// Inserts a block, as one undoable edit.
    ///
    /// #### Parameters
    ///
    /// - `index`: where, 0 to `size()`
    ///
    /// - `block`: the new block
    public void insertBlock(int index, Block block) {
        setDocument(document.insert(index, block));
    }

    /// Removes a block, as one undoable edit.
    ///
    /// #### Parameters
    ///
    /// - `index`: the block to remove
    public void removeBlock(int index) {
        setDocument(document.remove(index));
    }

    // ------------------------------------------------------------------ finding paragraphs

    /// A paragraph anywhere in the document: the body, table cells or notes.
    ///
    /// #### Parameters
    ///
    /// - `id`: the paragraph id
    ///
    /// #### Returns
    ///
    /// the paragraph, or null
    public Paragraph findParagraph(String id) {
        return find(document.blocks, id);
    }

    private static Paragraph find(List<Block> blocks, String id) {
        for (Block b : blocks) {
            if (b instanceof Paragraph) {
                Paragraph p = (Paragraph) b;
                if (p.id.equals(id)) {
                    return p;
                }
                for (int i = 0; i < p.text.length(); i++) {
                    Inline o = p.text.objectAt(i);
                    if (o instanceof Note) {
                        Paragraph q = find(((Note) o).blocks, id);
                        if (q != null) {
                            return q;
                        }
                    }
                }
            } else if (b instanceof Table) {
                Table t = (Table) b;
                for (int r = 0; r < t.rows(); r++) {
                    for (int c = 0; c < t.columns(); c++) {
                        Paragraph q = find(t.cell(r, c), id);
                        if (q != null) {
                            return q;
                        }
                    }
                }
            }
        }
        return null;
    }

    /// The id of the top-level table a paragraph is in, for opening a `TableBinding` on the
    /// table under the caret.
    ///
    /// #### Parameters
    ///
    /// - `paragraphId`: the paragraph
    ///
    /// #### Returns
    ///
    /// the table id, or null when the paragraph is not in a table
    public String tableOf(String paragraphId) {
        for (Block b : document.blocks) {
            if (b instanceof Table) {
                Table t = (Table) b;
                for (int r = 0; r < t.rows(); r++) {
                    for (int c = 0; c < t.columns(); c++) {
                        if (find(t.cell(r, c), paragraphId) != null) {
                            return t.id;
                        }
                    }
                }
            }
        }
        return null;
    }

    /// Changes the block list holding a paragraph.
    interface ListEdit {
        List<Block> apply(List<Block> list, int index);
    }

    /// Applies an edit to the list that holds a paragraph, wherever it is, rebuilding only the
    /// blocks on the way to it.
    static List<Block> editList(List<Block> blocks, String id, ListEdit edit) {
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            if (b instanceof Paragraph) {
                Paragraph p = (Paragraph) b;
                if (p.id.equals(id)) {
                    return edit.apply(new ArrayList<Block>(blocks), i);
                }
                for (int k = 0; k < p.text.length(); k++) {
                    Inline o = p.text.objectAt(k);
                    if (o instanceof Note) {
                        Note n = (Note) o;
                        List<Block> inner = editList(n.blocks, id, edit);
                        if (inner != null) {
                            List<Block> out = new ArrayList<Block>(blocks);
                            out.set(i, p.withText(p.text.withObject(k,
                                    new Note(n.id, n.kind, n.mark, inner))));
                            return out;
                        }
                    }
                }
            } else if (b instanceof Table) {
                Table t = (Table) b;
                for (int r = 0; r < t.rows(); r++) {
                    for (int c = 0; c < t.columns(); c++) {
                        List<Block> inner = editList(t.cell(r, c), id, edit);
                        if (inner != null) {
                            List<Block> out = new ArrayList<Block>(blocks);
                            out.set(i, withCell(t, r, c, inner));
                            return out;
                        }
                    }
                }
            }
        }
        return null;
    }

    static Table withCell(Table t, int row, int column, List<Block> cell) {
        List<List<List<Block>>> rows = new ArrayList<List<List<Block>>>();
        for (int r = 0; r < t.rows(); r++) {
            List<List<Block>> cells = new ArrayList<List<Block>>();
            for (int c = 0; c < t.columns(); c++) {
                cells.add(r == row && c == column ? cell : t.cell(r, c));
            }
            rows.add(cells);
        }
        return new Table(t.id, rows, t.merges, t.headerRows);
    }

    private Document withList(String id, ListEdit e) {
        List<Block> out = editList(document.blocks, id, e);
        if (out == null) {
            throw new IllegalArgumentException("No paragraph " + id);
        }
        return document.withBlocks(out);
    }

    /// Replaces a paragraph, wherever it is, by one with the same id.
    ///
    /// #### Parameters
    ///
    /// - `p`: the new paragraph
    public void replaceParagraph(final Paragraph p) {
        setDocument(withList(p.id, new SetAt(p)));
    }

    // ------------------------------------------------------------------ text edits

    /// Inserts text at a position, as typing does.
    ///
    /// #### Parameters
    ///
    /// - `at`: where
    ///
    /// - `text`: the text; a line break inserts a soft break within the paragraph
    ///
    /// - `style`: its style, or null to continue the style before the position
    ///
    /// #### Returns
    ///
    /// the position after the inserted text
    public TextPosition insertText(TextPosition at, String text, TextStyle style) {
        Paragraph p = paragraph(at);
        int offset = clamp(at.offset, p);
        TextStyle s = style != null ? style : typingStyle(p, offset);
        RichText t = p.text.insert(offset, RichText.of(text, s));
        boolean wordEnd = text.indexOf(' ') >= 0 || text.indexOf('\n') >= 0 || text.length() > 1;
        replace(p.withText(t), wordEnd ? null : "type:" + p.id);
        if (wordEnd) {
            typing = null;
        }
        return new TextPosition(p.id, offset + text.length());
    }

    /// Inserts an inline object: a picture, a component, a field or a note.
    ///
    /// #### Parameters
    ///
    /// - `at`: where
    ///
    /// - `object`: the object
    ///
    /// #### Returns
    ///
    /// the position after it
    public TextPosition insertObject(TextPosition at, Inline object) {
        Paragraph p = paragraph(at);
        int offset = clamp(at.offset, p);
        RichText t = p.text.insert(offset, RichText.of(object, typingStyle(p, offset)));
        replace(p.withText(t), null);
        return new TextPosition(p.id, offset + 1);
    }

    /// The style typing at a position continues: that of the character before it, without a
    /// link or a revision.
    ///
    /// #### Parameters
    ///
    /// - `p`: the paragraph
    ///
    /// - `offset`: the position
    ///
    /// #### Returns
    ///
    /// the style
    public static TextStyle typingStyle(Paragraph p, int offset) {
        if (p.text.isEmpty()) {
            return TextStyle.DEFAULT;
        }
        TextStyle s = p.text.styleAt(Math.max(0, Math.min(p.text.length() - 1, offset - 1)));
        return s.withLink(null).withRevision(null);
    }

    /// Deletes the text between two positions, which may lie in different paragraphs of the
    /// same block list; the paragraphs between them go, and the last one's remaining text joins
    /// the first.
    ///
    /// #### Parameters
    ///
    /// - `from`: one end
    ///
    /// - `to`: the other end
    ///
    /// #### Returns
    ///
    /// the position where the deleted text was
    public TextPosition delete(TextPosition from, TextPosition to) {
        final TextPosition[] ends = order(from, to);
        final Paragraph a = paragraph(ends[0]);
        final int start = clamp(ends[0].offset, a);
        if (ends[0].paragraphId.equals(ends[1].paragraphId)) {
            int end = clamp(ends[1].offset, a);
            if (end > start) {
                replace(a.withText(a.text.delete(start, end)), null);
            }
            return new TextPosition(a.id, start);
        }
        final Paragraph b = paragraph(ends[1]);
        final int end = clamp(ends[1].offset, b);
        setDocument(withList(a.id, new JoinRange(a, start, b, end)));
        typing = null;
        return new TextPosition(a.id, start);
    }

    /// Splits a paragraph in two at a position, as pressing Enter does. The new paragraph keeps
    /// the old one's formatting, except that a heading is followed by a normal paragraph.
    ///
    /// #### Parameters
    ///
    /// - `at`: where
    ///
    /// #### Returns
    ///
    /// the position at the start of the new paragraph
    public TextPosition splitParagraph(TextPosition at) {
        final Paragraph p = paragraph(at);
        final int offset = clamp(at.offset, p);
        final String id = nextId();
        setDocument(withList(p.id, new Split(p, offset, id)).withNextId(document.nextId + 1));
        typing = null;
        return new TextPosition(id, 0);
    }

    /// Joins a paragraph to the one before it in its block list, as Backspace at its start
    /// does.
    ///
    /// #### Parameters
    ///
    /// - `paragraphId`: the paragraph
    ///
    /// #### Returns
    ///
    /// the position at the join, or null when there is no paragraph before it to join
    public TextPosition joinWithPrevious(final String paragraphId) {
        JoinPrevious join = new JoinPrevious();
        List<Block> out = editList(document.blocks, paragraphId, join);
        if (out == null || join.result == null) {
            return null;
        }
        setDocument(document.withBlocks(out));
        typing = null;
        return join.result;
    }

    /// Changes the character style of the text between two positions in one paragraph.
    ///
    /// #### Parameters
    ///
    /// - `from`: one end
    ///
    /// - `to`: the other end, in the same paragraph
    ///
    /// - `mapper`: computes each run's new style
    public void restyle(TextPosition from, TextPosition to, RichText.StyleMapper mapper) {
        TextPosition[] ends = order(from, to);
        if (!ends[0].paragraphId.equals(ends[1].paragraphId)) {
            throw new IllegalArgumentException("Both ends must be in one paragraph");
        }
        Paragraph p = paragraph(ends[0]);
        int a = clamp(ends[0].offset, p);
        int b = clamp(ends[1].offset, p);
        if (b > a) {
            replace(p.withText(p.text.restyle(a, b, mapper)), null);
            typing = null;
        }
    }

    private String nextId() {
        return document.newId('p');
    }

    private void editTyping(Document value, String group) {
        if (group != null && group.equals(typing) && groupDepth == 0 && !undo.isEmpty()) {
            // the next keystroke of the same word: no new undo step
            redo.clear();
            replace(recalculated(value));
        } else {
            setDocument(value);
        }
        typing = group;
    }

    private void replace(final Paragraph p, String group) {
        editTyping(withList(p.id, new SetAt(p)), group);
    }

    private Paragraph paragraph(TextPosition at) {
        Paragraph p = findParagraph(at.paragraphId);
        if (p == null) {
            throw new IllegalArgumentException("No paragraph " + at.paragraphId);
        }
        return p;
    }

    private static int clamp(int offset, Paragraph p) {
        return Math.max(0, Math.min(p.text.length(), offset));
    }

    /// Orders two positions by where they are in the document.
    ///
    /// #### Parameters
    ///
    /// - `a`: one position
    ///
    /// - `b`: the other
    ///
    /// #### Returns
    ///
    /// the two, earlier first
    public TextPosition[] order(TextPosition a, TextPosition b) {
        if (a.paragraphId.equals(b.paragraphId)) {
            return a.offset <= b.offset ? new TextPosition[] {a, b} : new TextPosition[] {b, a};
        }
        List<String> ids = new ArrayList<String>();
        collectIds(document.blocks, ids);
        int ia = ids.indexOf(a.paragraphId);
        int ib = ids.indexOf(b.paragraphId);
        return ia <= ib ? new TextPosition[] {a, b} : new TextPosition[] {b, a};
    }

    /// Every paragraph id in reading order, cells and notes included.
    static void collectIds(List<Block> blocks, List<String> out) {
        for (Block b : blocks) {
            if (b instanceof Paragraph) {
                Paragraph p = (Paragraph) b;
                out.add(p.id);
                for (int i = 0; i < p.text.length(); i++) {
                    Inline o = p.text.objectAt(i);
                    if (o instanceof Note) {
                        collectIds(((Note) o).blocks, out);
                    }
                }
            } else if (b instanceof Table) {
                Table t = (Table) b;
                for (int r = 0; r < t.rows(); r++) {
                    for (int c = 0; c < t.columns(); c++) {
                        collectIds(t.cell(r, c), out);
                    }
                }
            }
        }
    }
    /// Puts a paragraph in place of the one at the index.
    private static final class SetAt implements ListEdit {
        private final Paragraph p;

        SetAt(Paragraph p) {
            this.p = p;
        }

        @Override
        public List<Block> apply(List<Block> list, int index) {
            list.set(index, p);
            return list;
        }
    }

    /// Deletes from an offset of one paragraph to an offset of a later one in the same list.
    private static final class JoinRange implements ListEdit {
        private final Paragraph a;
        private final int start;
        private final Paragraph b;
        private final int end;

        JoinRange(Paragraph a, int start, Paragraph b, int end) {
            this.a = a;
            this.start = start;
            this.b = b;
            this.end = end;
        }

        @Override
        public List<Block> apply(List<Block> list, int index) {
            int last = -1;
            for (int i = index + 1; i < list.size(); i++) {
                if (list.get(i) instanceof Paragraph && ((Paragraph) list.get(i)).id.equals(b.id)) {
                    last = i;
                    break;
                }
            }
            if (last < 0) {
                // not in the same list: delete to the end of the first paragraph only
                list.set(index, a.withText(a.text.delete(start, a.text.length())));
                return list;
            }
            RichText joined = a.text.substring(0, start).append(b.text.substring(end, b.text.length()));
            list.set(index, a.withText(joined));
            for (int i = last; i > index; i--) {
                list.remove(i);
            }
            return list;
        }
    }

    /// Splits a paragraph at an offset into it and a new paragraph.
    private static final class Split implements ListEdit {
        private final Paragraph p;
        private final int offset;
        private final String id;

        Split(Paragraph p, int offset, String id) {
            this.p = p;
            this.offset = offset;
            this.id = id;
        }

        @Override
        public List<Block> apply(List<Block> list, int index) {
            list.set(index, p.withText(p.text.substring(0, offset)));
            Paragraph tail = p.withId(id).withText(p.text.substring(offset, p.text.length()))
                    .withPageBreak(false).withListStart(0);
            if (p.style.startsWith("Heading") || "Title".equals(p.style)) {
                tail = tail.withStyle("Normal");
            }
            list.add(index + 1, tail);
            return list;
        }
    }

    /// Joins a paragraph to the paragraph before it.
    private static final class JoinPrevious implements ListEdit {
        TextPosition result;

        @Override
        public List<Block> apply(List<Block> list, int index) {
            if (index == 0 || !(list.get(index - 1) instanceof Paragraph)) {
                return list;
            }
            Paragraph prev = (Paragraph) list.get(index - 1);
            Paragraph p = (Paragraph) list.get(index);
            result = new TextPosition(prev.id, prev.text.length());
            list.set(index - 1, prev.withText(prev.text.append(p.text)));
            list.remove(index);
            return list;
        }
    }
}
