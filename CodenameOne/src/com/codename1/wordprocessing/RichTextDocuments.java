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

import com.codename1.ui.RichTextArea;
import com.codename1.util.SuccessCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// Moves content between a `RichTextArea` and a `Document`, so the light editor and the paged
/// one can work on the same text.
///
/// ```java
/// final DocumentModel model = new DocumentModel(new Document());
/// model.insertBlock(0, new Paragraph(model.newId('p'), RichText.of("Edit me")));
/// RichTextArea editor = new RichTextArea();
/// RichTextDocuments.show(model.getDocument(), editor);
/// // later, once the user has edited the text
/// RichTextDocuments.read(editor, new SuccessCallback<Document>() {
///     public void onSucess(Document d) {
///         model.setDocument(d);
///     }
/// });
/// ```
///
/// Content goes through HTML, so what HTML cannot say -- page size, tracked changes, comments
/// -- is not carried. Embedded components are: the editor shows each through the image source
/// `EmbeddedComponents` registers, and reading back restores its type, size and state.
public final class RichTextDocuments {
    private RichTextDocuments() {
    }

    /// Shows a document in a rich text editor.
    ///
    /// #### Parameters
    ///
    /// - `d`: the document
    ///
    /// - `editor`: the editor
    public static void show(Document d, RichTextArea editor) {
        editor.setHtml(toHtml(d));
    }

    /// The HTML a rich text editor is given for a document.
    ///
    /// #### Parameters
    ///
    /// - `d`: the document
    ///
    /// #### Returns
    ///
    /// the HTML, embedded components named by `cn1-component:` sources
    public static String toHtml(Document d) {
        return HtmlFormat.write(d, true);
    }

    /// A document made from HTML a rich text editor gave back.
    ///
    /// #### Parameters
    ///
    /// - `html`: the HTML
    ///
    /// #### Returns
    ///
    /// the document
    public static Document fromHtml(String html) {
        return HtmlFormat.read(html == null ? "" : html);
    }

    /// Reads Markdown. Unlike `Document.read`, this reaches no other format's code, so an
    /// application that only exchanges Markdown with a rich text editor carries no DOCX, ODT
    /// or PDF support.
    ///
    /// #### Parameters
    ///
    /// - `markdown`: the source
    ///
    /// #### Returns
    ///
    /// the document
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the source nests too deeply for the thread's stack
    public static Document readMarkdown(String markdown) throws IOException {
        try {
            return MarkdownFormat.read(markdown == null ? "" : markdown);
        } catch (OutOfMemoryError ex) {
            throw ex;
        } catch (VirtualMachineError ex) {
            throw tooDeep(ex);
        }
    }

    /// Writes Markdown.
    ///
    /// #### Parameters
    ///
    /// - `d`: the document
    ///
    /// #### Returns
    ///
    /// the Markdown
    public static String writeMarkdown(Document d) {
        return MarkdownFormat.write(d);
    }

    /// Reads RTF from a string, as a clipboard gives it. Characters above U+00FF, which RTF
    /// spells with escapes, are kept: each becomes the backslash-u control word that says it.
    ///
    /// #### Parameters
    ///
    /// - `rtf`: the source
    ///
    /// #### Returns
    ///
    /// the document
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the source is not RTF or nests too deeply for the thread's stack
    public static Document readRtf(String rtf) throws IOException {
        String value = rtf == null ? "" : rtf;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x100) {
                bytes.write(c);
            } else {
                byte[] escape = Ids.ascii("\\u" + (int) (short) c + "?");
                bytes.write(escape, 0, escape.length);
            }
        }
        try {
            return RtfReader.read(bytes.toByteArray());
        } catch (OutOfMemoryError ex) {
            throw ex;
        } catch (VirtualMachineError ex) {
            throw tooDeep(ex);
        }
    }

    /// Writes RTF.
    ///
    /// #### Parameters
    ///
    /// - `d`: the document
    ///
    /// #### Returns
    ///
    /// the RTF, which is 7-bit
    public static String writeRtf(Document d) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            RtfWriter.write(d, out);
        } catch (IOException ex) {
            // a ByteArrayOutputStream does not fail
            throw new IllegalStateException(ex.toString()); //NOPMD PreserveStackTrace - CLDC has no cause constructor
        }
        // RTF is 7-bit: each byte is one character, so no charset is involved
        byte[] b = out.toByteArray();
        char[] c = new char[b.length];
        for (int i = 0; i < b.length; i++) {
            c[i] = (char) (b[i] & 0xff);
        }
        return new String(c);
    }

    private static IOException tooDeep(VirtualMachineError ex) {
        // a stack overflow: StackOverflowError is not in the CLDC library the core compiles
        // against, and out of memory is rethrown before this
        IOException e = new IOException("The document is nested too deeply: " + ex);
        e.initCause(ex);
        return e;
    }

    /// Reads a rich text editor's content as a document. The editor answers asynchronously.
    ///
    /// #### Parameters
    ///
    /// - `editor`: the editor
    ///
    /// - `callback`: given the document
    public static void read(RichTextArea editor, final SuccessCallback<Document> callback) {
        editor.getHtml(new SuccessCallback<String>() {
            @Override
            public void onSucess(String html) {
                callback.onSucess(fromHtml(html));
            }
        });
    }
}
