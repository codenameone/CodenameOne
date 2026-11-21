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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String author = "";
    private Revision insertion;
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
        if (document.trackChanges) {
            // one revision per run of typing, so each can be accepted or rejected alone
            if (insertion == null || !("type:" + p.id).equals(typing)) {
                insertion = new Revision(newId('r'), Revision.INSERT, author, null);
            }
            s = s.withRevision(insertion);
        }
        RichText t = p.text.insert(offset, RichText.of(text, s));
        boolean wordEnd = text.indexOf(' ') >= 0 || text.indexOf('\n') >= 0 || text.length() > 1;
        replace(p.withText(t), wordEnd ? null : "type:" + p.id, Anchors.inserted(p.id, offset, text.length()));
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
        TextStyle s = typingStyle(p, offset);
        if (document.trackChanges) {
            s = s.withRevision(new Revision(newId('r'), Revision.INSERT, author, null));
        }
        RichText t = p.text.insert(offset, RichText.of(object, s));
        replace(p.withText(t), null, Anchors.inserted(p.id, offset, 1));
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
        if (document.trackChanges) {
            trackedDelete(ends);
            return new TextPosition(a.id, start);
        }
        if (ends[0].paragraphId.equals(ends[1].paragraphId)) {
            int end = clamp(ends[1].offset, a);
            if (end > start) {
                replace(a.withText(a.text.delete(start, end)), null,
                        Anchors.deleted(a.id, start, a.id, end));
            }
            return new TextPosition(a.id, start);
        }
        final Paragraph b = paragraph(ends[1]);
        final int end = clamp(ends[1].offset, b);
        setDocument(Anchors.apply(withList(a.id, new JoinRange(a, start, b, end)),
                Anchors.deleted(a.id, start, b.id, end), a.id, start));
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
        setDocument(Anchors.apply(withList(p.id, new Split(p, offset, id)).withNextId(document.nextId + 1),
                Anchors.split(p.id, offset, id), p.id, offset));
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
        setDocument(Anchors.apply(document.withBlocks(out),
                Anchors.joined(paragraphId, join.result.paragraphId, join.result.offset),
                join.result.paragraphId, join.result.offset));
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

    // ------------------------------------------------------------------ revisions

    /// The author recorded on tracked changes and new comments.
    ///
    /// #### Parameters
    ///
    /// - `value`: the author's name
    public void setAuthor(String value) {
        author = value == null ? "" : value;
        insertion = null;
    }

    /// The author recorded on tracked changes and new comments.
    ///
    /// #### Returns
    ///
    /// the name, possibly empty
    public String getAuthor() {
        return author;
    }

    /// Turns change tracking on or off. While it is on, inserted text is marked as an
    /// insertion and deleted text stays in the document marked as a deletion, until the
    /// revisions are accepted or rejected.
    ///
    /// #### Parameters
    ///
    /// - `value`: true to track changes
    public void setTrackChanges(boolean value) {
        if (value != document.trackChanges) {
            setDocument(document.withTrackChanges(value));
        }
    }

    private void trackedDelete(TextPosition[] ends) {
        List<String> order = new ArrayList<String>();
        collectIds(document.blocks, order);
        int ia = order.indexOf(ends[0].paragraphId);
        int ib = order.indexOf(ends[1].paragraphId);
        Revision del = new Revision(newId('r'), Revision.DELETE, author, null);
        beginGroup();
        try {
            for (int i = ia; i <= ib && i >= 0; i++) {
                Paragraph p = findParagraph(order.get(i));
                int a = i == ia ? clamp(ends[0].offset, p) : 0;
                int b = i == ib ? clamp(ends[1].offset, p) : p.text.length();
                boolean[] removed = new boolean[p.text.length()];
                RichText t = markDeleted(p.text, a, b, del, removed);
                if (!t.equals(p.text)) {
                    replace(p.withText(t), null, Anchors.removed(p.id, removed));
                }
            }
        } finally {
            endGroup();
        }
        typing = null;
    }

    /// Marks a range deleted: text inserted under tracking simply goes, other text is kept
    /// marked as a deletion.
    private static RichText markDeleted(RichText text, int start, int end, Revision del,
            boolean[] removed) {
        RichText t = text;
        for (int i = end - 1; i >= start; i--) {
            Revision r = t.styleAt(i).revision;
            if (r != null && !r.isDeletion() && r.author.equals(del.author)) {
                // taking back one's own pending insertion leaves no trace, as in Word
                t = t.delete(i, i + 1);
                removed[i] = true;
            } else if (r == null || !r.isDeletion()) {
                // someone else's insertion is marked deleted, so the deletion can be
                // rejected; a character holds one revision, so rejecting it then
                // restores the text as accepted
                t = t.restyle(i, i + 1, new SetRevision(del));
            }
        }
        return t;
    }

    private static final class SetRevision implements RichText.StyleMapper {
        private final Revision revision;

        SetRevision(Revision revision) {
            this.revision = revision;
        }

        @Override
        public TextStyle map(TextStyle s) {
            return s.withRevision(revision);
        }
    }

    /// Accepts tracked changes: insertions become ordinary text and deleted text goes.
    ///
    /// #### Parameters
    ///
    /// - `revisionId`: the revision to accept, or null for all of them
    ///
    /// #### Returns
    ///
    /// the number of characters changed
    public int acceptRevisions(String revisionId) {
        return resolveRevisions(revisionId, true);
    }

    /// Rejects tracked changes: inserted text goes and deletions become ordinary text again.
    ///
    /// #### Parameters
    ///
    /// - `revisionId`: the revision to reject, or null for all of them
    ///
    /// #### Returns
    ///
    /// the number of characters changed
    public int rejectRevisions(String revisionId) {
        return resolveRevisions(revisionId, false);
    }

    private int resolveRevisions(String revisionId, boolean accept) {
        List<String> ids = new ArrayList<String>();
        collectIds(document.blocks, ids);
        int changed = 0;
        beginGroup();
        try {
            for (String id : ids) {
                Paragraph p = findParagraph(id);
                RichText t = p.text;
                boolean[] removed = new boolean[t.length()];
                for (int i = t.length() - 1; i >= 0; i--) {
                    Revision r = t.styleAt(i).revision;
                    if (r == null || revisionId != null && !revisionId.equals(r.id)) {
                        continue;
                    }
                    changed++;
                    if (accept == r.isDeletion()) {
                        t = t.delete(i, i + 1);
                        removed[i] = true;
                    } else {
                        t = t.restyle(i, i + 1, new SetRevision(null));
                    }
                }
                if (!t.equals(p.text)) {
                    replace(p.withText(t), null, Anchors.removed(p.id, removed));
                }
            }
        } finally {
            endGroup();
        }
        typing = null;
        return changed;
    }

    /// The distinct tracked changes in the document, in document order.
    ///
    /// #### Returns
    ///
    /// the revisions
    public List<Revision> getRevisions() {
        List<String> ids = new ArrayList<String>();
        collectIds(document.blocks, ids);
        List<Revision> out = new ArrayList<Revision>();
        for (String id : ids) {
            RichText t = findParagraph(id).text;
            for (int i = 0; i < t.runCount(); i++) {
                Revision r = t.runStyle(i).revision;
                if (r != null && !out.contains(r)) {
                    out.add(r);
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ comments

    /// Adds a review comment over a range, by the current author.
    ///
    /// #### Parameters
    ///
    /// - `from`: one end of the range
    ///
    /// - `to`: the other end
    ///
    /// - `text`: the comment
    ///
    /// #### Returns
    ///
    /// the new comment
    public Comment addComment(TextPosition from, TextPosition to, String text) {
        TextPosition[] ends = order(from, to);
        Comment c = new Comment(newId('c'), author, null, text, false, ends[0].paragraphId,
                ends[0].offset, ends[1].paragraphId, ends[1].offset);
        List<Comment> list = new ArrayList<Comment>(document.comments);
        list.add(c);
        setDocument(document.withComments(list));
        typing = null;
        return c;
    }

    /// Removes a review comment.
    ///
    /// #### Parameters
    ///
    /// - `id`: the comment's id
    ///
    /// #### Returns
    ///
    /// true when there was one
    public boolean removeComment(String id) {
        List<Comment> list = new ArrayList<Comment>(document.comments);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.remove(i);
                setDocument(document.withComments(list));
                typing = null;
                return true;
            }
        }
        return false;
    }

    /// Marks a review comment resolved or open again.
    ///
    /// #### Parameters
    ///
    /// - `id`: the comment's id
    ///
    /// - `resolved`: the new state
    ///
    /// #### Returns
    ///
    /// true when there was such a comment
    public boolean setCommentResolved(String id, boolean resolved) {
        List<Comment> list = new ArrayList<Comment>(document.comments);
        for (int i = 0; i < list.size(); i++) {
            Comment c = list.get(i);
            if (c.id.equals(id)) {
                if (c.resolved != resolved) {
                    list.set(i, c.withResolved(resolved));
                    setDocument(document.withComments(list));
                    typing = null;
                }
                return true;
            }
        }
        return false;
    }

    /// The comments whose range holds a position.
    ///
    /// #### Parameters
    ///
    /// - `at`: the position
    ///
    /// #### Returns
    ///
    /// the comments, in document order
    public List<Comment> commentsAt(TextPosition at) {
        List<Comment> out = new ArrayList<Comment>();
        if (document.comments.isEmpty()) {
            return out;
        }
        List<String> ids = new ArrayList<String>();
        collectIds(document.blocks, ids);
        int p = ids.indexOf(at.paragraphId);
        for (Comment c : document.comments) {
            int[] r = commentRange(c, ids, p, Integer.MAX_VALUE);
            if (r != null && at.offset >= r[0] && at.offset <= r[1]) {
                out.add(c);
            }
        }
        return out;
    }

    /// The part of a comment's range inside the paragraph at an index of the document order.
    static int[] commentRange(Comment c, List<String> ids, int p, int length) {
        int s = ids.indexOf(c.startId);
        int e = ids.indexOf(c.endId);
        if (p < 0 || s < 0 || e < 0 || p < s || p > e) {
            return null;
        }
        return new int[] {p == s ? c.start : 0, p == e ? c.end : length};
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

    private void replace(final Paragraph p, String group, Anchors map) {
        editTyping(Anchors.apply(withList(p.id, new SetAt(p)), map, p.id, 0), group);
    }

    /// Moves comment anchors with an edit, so a comment stays on the words it was made on.
    /// An anchor in a paragraph the edit removed goes to `fallbackId`/`fallbackOffset`.
    abstract static class Anchors {
        /// The new position of an anchor, or null when the edit leaves it where it is.
        abstract TextPosition map(String id, int offset, boolean isStart);

        static Document apply(Document d, Anchors m, String fallbackId, int fallbackOffset) {
            if (d.comments.isEmpty()) {
                return d;
            }
            // one pass over the document, not one per anchor: this runs on every keystroke
            Map<String, Integer> lengths = new HashMap<String, Integer>();
            lengths(d.blocks, lengths);
            List<Comment> out = new ArrayList<Comment>(d.comments.size());
            boolean changed = false;
            for (Comment c : d.comments) {
                // a point comment moves as one point, or typing at it would invert it
                boolean point = c.startId.equals(c.endId) && c.start == c.end;
                TextPosition a = place(lengths, m, c.startId, c.start, !point, fallbackId, fallbackOffset);
                TextPosition b = place(lengths, m, c.endId, c.end, false, fallbackId, fallbackOffset);
                if (a.paragraphId.equals(c.startId) && a.offset == c.start
                        && b.paragraphId.equals(c.endId) && b.offset == c.end) {
                    out.add(c);
                } else {
                    out.add(new Comment(c.id, c.author, c.date, c.text, c.resolved,
                            a.paragraphId, a.offset, b.paragraphId, b.offset));
                    changed = true;
                }
            }
            return changed ? d.withComments(out) : d;
        }

        private static TextPosition place(Map<String, Integer> lengths, Anchors m, String id, int offset,
                boolean isStart, String fallbackId, int fallbackOffset) {
            TextPosition t = m.map(id, offset, isStart);
            if (t == null) {
                t = new TextPosition(id, offset);
            }
            Integer length = lengths.get(t.paragraphId);
            if (length == null) {
                return new TextPosition(fallbackId, fallbackOffset);
            }
            return new TextPosition(t.paragraphId, Math.min(t.offset, length.intValue()));
        }

        private static void lengths(List<Block> blocks, Map<String, Integer> out) {
            for (Block b : blocks) {
                if (b instanceof Paragraph) {
                    Paragraph p = (Paragraph) b;
                    out.put(p.id, Integer.valueOf(p.text.length()));
                    for (int i = 0; i < p.text.length(); i++) {
                        Inline o = p.text.objectAt(i);
                        if (o instanceof Note) {
                            lengths(((Note) o).blocks, out);
                        }
                    }
                } else if (b instanceof Table) {
                    Table t = (Table) b;
                    for (int r = 0; r < t.rows(); r++) {
                        for (int c = 0; c < t.columns(); c++) {
                            lengths(t.cell(r, c), out);
                        }
                    }
                }
            }
        }

        /// Characters removed from `pid` where `removed` is set, by index before the edit.
        static Anchors removed(final String pid, final boolean[] removed) {
            return new Anchors() {
                @Override
                TextPosition map(String id, int offset, boolean isStart) {
                    if (!id.equals(pid)) {
                        return null;
                    }
                    int o = offset;
                    for (int k = 0; k < offset && k < removed.length; k++) {
                        if (removed[k]) {
                            o--;
                        }
                    }
                    return o == offset ? null : new TextPosition(id, o);
                }
            };
        }

        /// Text inserted at `at`: what follows moves on; an anchor right at the insertion
        /// stays before it when it ends a comment and after it when it starts one, so typing
        /// at either edge stays outside.
        static Anchors inserted(final String pid, final int at, final int length) {
            return new Anchors() {
                @Override
                TextPosition map(String id, int offset, boolean isStart) {
                    if (id.equals(pid) && (offset > at || offset == at && isStart)) {
                        return new TextPosition(id, offset + length);
                    }
                    return null;
                }
            };
        }

        /// Text deleted from `aId`/`start` to `bId`/`end`, the rest of `bId` joining `aId`.
        static Anchors deleted(final String aId, final int start, final String bId, final int end) {
            return new Anchors() {
                @Override
                TextPosition map(String id, int offset, boolean isStart) {
                    if (id.equals(bId) && (offset >= start || !aId.equals(bId))) {
                        int o = offset <= end ? start : start + offset - end;
                        return new TextPosition(aId, o);
                    }
                    if (id.equals(aId) && offset > start) {
                        return new TextPosition(aId, start);
                    }
                    return null;
                }
            };
        }

        /// `pid` split at `at`, what follows going to `newId`.
        static Anchors split(final String pid, final int at, final String newId) {
            return new Anchors() {
                @Override
                TextPosition map(String id, int offset, boolean isStart) {
                    if (id.equals(pid) && (offset > at || offset == at && isStart)) {
                        return new TextPosition(newId, offset - at);
                    }
                    return null;
                }
            };
        }

        /// `pid` joined to the end of `intoId`, which was `length` long.
        static Anchors joined(final String pid, final String intoId, final int length) {
            return new Anchors() {
                @Override
                TextPosition map(String id, int offset, boolean isStart) {
                    return id.equals(pid) ? new TextPosition(intoId, length + offset) : null;
                }
            };
        }
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
