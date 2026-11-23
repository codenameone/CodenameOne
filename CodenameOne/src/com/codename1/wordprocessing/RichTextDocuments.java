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
