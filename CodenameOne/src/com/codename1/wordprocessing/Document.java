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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// An immutable word-processing document: a list of `Block`s (paragraphs, tables and pictures),
/// the page they are laid out on and a title.
///
/// A document never changes. Editing produces a new one, which shares every unchanged block
/// with the old, so keeping old versions for undo costs little. `DocumentModel` holds the
/// current version, notifies listeners when it is replaced and keeps the undo history; it is to
/// a document what a `com.codename1.ui.table.TableModel` is to its rows.
///
/// Reading and writing:
///
/// ```java
/// Document d = Document.read(in, Document.Format.MARKDOWN);
/// d.write(out, Document.Format.NATIVE);
/// ```
public final class Document {
    /// The file formats `read` and `write` understand.
    public enum Format {
        /// Codename One's own `.cn1doc`: a ZIP holding `document.xml`. Keeps everything the
        /// model holds. Files written by the Codename One word processor open too; whatever
        /// they hold beyond this model (comments, sections, revisions) is skipped.
        NATIVE("cn1doc"),

        /// Plain UTF-8 text. Formatting is lost; tables become tab separated lines.
        TEXT("txt"),

        /// HTML. Reading accepts common markup, tables (with `colspan`/`rowspan`) and
        /// `data:` URI images; writing produces a complete page.
        HTML("html"),

        /// Markdown with GitHub tables.
        MARKDOWN("md"),

        /// Office Open XML, the format of Word 2007 and later. Reads styles, lists, tables
        /// with merged cells, pictures, fields, notes, comments and tracked changes; headers,
        /// footers, text boxes and charts are skipped.
        DOCX("docx"),

        /// OpenDocument text, the format of LibreOffice Writer. Reads and writes styles,
        /// lists, tables with spans, pictures, notes, annotations and tracked changes.
        ODT("odt");

        private final String extension;

        Format(String extension) {
            this.extension = extension;
        }

        /// The usual file extension, without the dot.
        ///
        /// #### Returns
        ///
        /// the extension
        public String extension() {
            return extension;
        }

        /// The format a file name's extension names.
        ///
        /// #### Parameters
        ///
        /// - `fileName`: a file name or path
        ///
        /// #### Returns
        ///
        /// the format, or null when the extension is not one of these
        public static Format forFileName(String fileName) {
            int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
            if (dot < 0) {
                return null;
            }
            String ext = fileName.substring(dot + 1);
            if ("htm".equalsIgnoreCase(ext)) {
                return HTML;
            }
            if ("docm".equalsIgnoreCase(ext) || "dotx".equalsIgnoreCase(ext)) {
                return DOCX;
            }
            if ("markdown".equalsIgnoreCase(ext)) {
                return MARKDOWN;
            }
            Format[] all = values();
            for (Format f : all) {
                if (f.extension.equalsIgnoreCase(ext)) {
                    return f;
                }
            }
            return null;
        }
    }

    /// The body, in order.
    public final List<Block> blocks;

    /// The page geometry.
    public final PageSpec page;

    /// The title, which `HTML` writes as the page title.
    public final String title;

    /// The next free numeric id; see `newId`.
    public final long nextId;

    /// The review comments, in document order.
    public final List<Comment> comments;

    /// Whether edits made through an editor are recorded as revisions.
    public final boolean trackChanges;

    /// A document with one empty paragraph, on A4.
    public Document() {
        this(single(new Paragraph("p1", RichText.EMPTY)), PageSpec.A4, "Untitled", 2);
    }

    /// Creates a document.
    ///
    /// #### Parameters
    ///
    /// - `blocks`: the body; copied. An empty body gets one empty paragraph, because an editor
    /// always needs somewhere to put the caret.
    ///
    /// - `page`: the page geometry, or null for A4
    ///
    /// - `title`: the title, or null
    ///
    /// - `nextId`: the next free numeric id; it is raised past any `p`/`t`/`i` + digits id in
    /// the body so `newId` never collides
    public Document(List<Block> blocks, PageSpec page, String title, long nextId) {
        this(blocks, page, title, nextId, null, false);
    }

    /// Creates a document with review comments.
    ///
    /// #### Parameters
    ///
    /// - `blocks`: the body; copied. An empty body gets one empty paragraph.
    ///
    /// - `page`: the page geometry, or null for A4
    ///
    /// - `title`: the title, or null
    ///
    /// - `nextId`: the next free numeric id
    ///
    /// - `comments`: the review comments, or null for none; copied
    ///
    /// - `trackChanges`: whether edits are recorded as revisions
    public Document(List<Block> blocks, PageSpec page, String title, long nextId,
            List<Comment> comments, boolean trackChanges) {
        List<Block> b = new ArrayList<Block>(blocks == null ? Collections.<Block>emptyList()
                : blocks);
        long next = Math.max(1, nextId);
        if (b.isEmpty()) {
            b.add(new Paragraph("p" + next, RichText.EMPTY));
            next++;
        }
        for (int i = 0; i < b.size(); i++) {
            if (b.get(i) == null) {
                throw new IllegalArgumentException("Null block at " + i);
            }
            next = Math.max(next, numericId(b.get(i).id()) + 1);
        }
        this.blocks = Collections.unmodifiableList(b);
        this.page = page == null ? PageSpec.A4 : page;
        this.title = title == null ? "" : title;
        List<Comment> c = new ArrayList<Comment>();
        if (comments != null) {
            for (Comment x : comments) {
                if (x != null) {
                    c.add(x);
                    next = Math.max(next, numericId(x.id) + 1);
                }
            }
        }
        this.comments = Collections.unmodifiableList(c);
        this.trackChanges = trackChanges;
        this.nextId = next;
    }

    private static List<Block> single(Block b) {
        List<Block> l = new ArrayList<Block>();
        l.add(b);
        return l;
    }

    static long numericId(String id) {
        if (id.length() < 2 || id.length() > 18) {
            return 0;
        }
        char k = id.charAt(0);
        if (k != 'p' && k != 't' && k != 'i') {
            return 0;
        }
        long v = 0;
        for (int i = 1; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') {
                return 0;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    /// A fresh id of the form `prefix` + number that no block of this document uses. Use the
    /// document `withNextId(nextId + 1)` returns afterwards, or `DocumentModel.newId`, which
    /// does both.
    ///
    /// #### Parameters
    ///
    /// - `prefix`: `p` for a paragraph, `t` for a table, `i` for a picture
    ///
    /// #### Returns
    ///
    /// the id
    public String newId(char prefix) {
        return String.valueOf(prefix) + nextId;
    }

    /// The number of blocks.
    ///
    /// #### Returns
    ///
    /// the block count
    public int size() {
        return blocks.size();
    }

    /// One block.
    ///
    /// #### Parameters
    ///
    /// - `index`: from zero
    ///
    /// #### Returns
    ///
    /// the block
    public Block get(int index) {
        return blocks.get(index);
    }

    /// The index of the top-level block with an id.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// #### Returns
    ///
    /// the index, or -1
    public int indexOf(String id) {
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /// A copy with a different body.
    ///
    /// #### Parameters
    ///
    /// - `value`: the blocks
    ///
    /// #### Returns
    ///
    /// the copy
    public Document withBlocks(List<Block> value) {
        return new Document(value, page, title, nextId, comments, trackChanges);
    }

    /// A copy with one block replaced.
    ///
    /// #### Parameters
    ///
    /// - `index`: the block to replace
    ///
    /// - `block`: its replacement
    ///
    /// #### Returns
    ///
    /// the copy
    public Document withBlock(int index, Block block) {
        List<Block> b = new ArrayList<Block>(blocks);
        b.set(index, block);
        return withBlocks(b);
    }

    /// A copy with a block inserted.
    ///
    /// #### Parameters
    ///
    /// - `index`: where, 0 to `size()`
    ///
    /// - `block`: the new block
    ///
    /// #### Returns
    ///
    /// the copy
    public Document insert(int index, Block block) {
        List<Block> b = new ArrayList<Block>(blocks);
        b.add(index, block);
        return withBlocks(b);
    }

    /// A copy without one block. Removing the last block leaves one empty paragraph.
    ///
    /// #### Parameters
    ///
    /// - `index`: the block to remove
    ///
    /// #### Returns
    ///
    /// the copy
    public Document remove(int index) {
        List<Block> b = new ArrayList<Block>(blocks);
        b.remove(index);
        return withBlocks(b);
    }

    /// A copy on a different page.
    ///
    /// #### Parameters
    ///
    /// - `value`: the page geometry
    ///
    /// #### Returns
    ///
    /// the copy
    public Document withPage(PageSpec value) {
        return new Document(blocks, value, title, nextId, comments, trackChanges);
    }

    /// A copy with a different title.
    ///
    /// #### Parameters
    ///
    /// - `value`: the title
    ///
    /// #### Returns
    ///
    /// the copy
    public Document withTitle(String value) {
        return new Document(blocks, page, value, nextId, comments, trackChanges);
    }

    /// A copy with a different next id.
    ///
    /// #### Parameters
    ///
    /// - `value`: the next free numeric id
    ///
    /// #### Returns
    ///
    /// the copy
    public Document withNextId(long value) {
        return new Document(blocks, page, title, value, comments, trackChanges);
    }

    /// A copy with different review comments.
    ///
    /// #### Parameters
    ///
    /// - `value`: the comments
    ///
    /// #### Returns
    ///
    /// the new document
    public Document withComments(List<Comment> value) {
        return new Document(blocks, page, title, nextId, value, trackChanges);
    }

    /// A copy with change tracking turned on or off.
    ///
    /// #### Parameters
    ///
    /// - `value`: whether edits are tracked
    ///
    /// #### Returns
    ///
    /// the new document
    public Document withTrackChanges(boolean value) {
        return new Document(blocks, page, title, nextId, comments, value);
    }

    /// The comment with an id.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// #### Returns
    ///
    /// the comment, or null
    public Comment comment(String id) {
        for (Comment c : comments) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    /// The whole text without formatting: paragraphs separated by newlines, table cells by tabs.
    ///
    /// #### Returns
    ///
    /// the characters
    public String plainText() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) {
                b.append('\n');
            }
            appendPlainText(b, blocks.get(i));
        }
        return b.toString();
    }

    static void appendPlainText(StringBuilder b, Block block) {
        if (block instanceof Paragraph) {
            appendReadable(b, ((Paragraph) block).text);
        } else if (block instanceof Table) {
            Table t = (Table) block;
            for (int r = 0; r < t.rows(); r++) {
                if (r > 0) {
                    b.append('\n');
                }
                for (int c = 0; c < t.columns(); c++) {
                    if (c > 0) {
                        b.append('\t');
                    }
                    if (!t.covered(r, c)) {
                        b.append(t.cellText(r, c).replace('\n', ' ').replace('\t', ' '));
                    }
                }
            }
        } else if (block instanceof Picture) {
            b.append(((Picture) block).description);
        }
    }

    /// Appends text as a reader sees it: fields show results, deleted revisions are skipped.
    static void appendReadable(StringBuilder b, RichText t) {
        for (int i = 0; i < t.runCount(); i++) {
            TextStyle s = t.runStyle(i);
            if (s.revision != null && s.revision.isDeletion()) {
                continue;
            }
            Inline o = t.runObject(i);
            b.append(o == null ? t.runText(i) : o.displayText());
        }
    }

    /// Reads a document.
    ///
    /// #### Parameters
    ///
    /// - `in`: the stream; it is read to the end but not closed
    ///
    /// - `format`: what the stream holds
    ///
    /// #### Returns
    ///
    /// the document
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the stream fails or does not hold a document of that format
    public static Document read(InputStream in, Format format) throws IOException {
        if (format == Format.NATIVE) {
            return NativeFormat.read(in);
        }
        if (format == Format.DOCX) {
            return DocxReader.read(in);
        }
        if (format == Format.ODT) {
            return OdtReader.read(in);
        }
        String text = new String(readAll(in), "UTF-8");
        if (text.length() > 0 && text.charAt(0) == 0xfeff) {
            text = text.substring(1);
        }
        switch (format) {
            case TEXT:
                return TextFormat.read(text);
            case HTML:
                return HtmlFormat.read(text);
            case MARKDOWN:
                return MarkdownFormat.read(text);
            default:
                throw new IOException("Unsupported format " + format);
        }
    }

    /// Writes this document.
    ///
    /// #### Parameters
    ///
    /// - `out`: the stream; it is flushed but not closed
    ///
    /// - `format`: what to write
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the stream fails
    public void write(OutputStream out, Format format) throws IOException {
        if (format == Format.NATIVE) {
            NativeFormat.write(this, out);
        } else if (format == Format.DOCX) {
            DocxWriter.write(this, out);
        } else if (format == Format.ODT) {
            OdtWriter.write(this, out);
        } else {
            String s;
            switch (format) {
                case TEXT:
                    s = TextFormat.write(this);
                    break;
                case HTML:
                    s = HtmlFormat.write(this);
                    break;
                case MARKDOWN:
                    s = MarkdownFormat.write(this);
                    break;
                default:
                    throw new IOException("Unsupported format " + format);
            }
            out.write(s.getBytes("UTF-8"));
        }
        out.flush();
    }

    /// This document in a text format, for clipboards and previews.
    ///
    /// #### Parameters
    ///
    /// - `format`: `TEXT`, `HTML` or `MARKDOWN`
    ///
    /// #### Returns
    ///
    /// the text
    public String toString(Format format) {
        switch (format) {
            case TEXT:
                return TextFormat.write(this);
            case HTML:
                return HtmlFormat.write(this);
            case MARKDOWN:
                return MarkdownFormat.write(this);
            default:
                throw new IllegalArgumentException(format + " is not a text format");
        }
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /// Two documents are equal when their blocks, page and title are.
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
        if (!(o instanceof Document)) {
            return false;
        }
        Document d = (Document) o;
        return blocks.equals(d.blocks) && page.equals(d.page) && title.equals(d.title)
                && comments.equals(d.comments) && trackChanges == d.trackChanges;
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        return blocks.hashCode() * 31 + title.hashCode();
    }
}
