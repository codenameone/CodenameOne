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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.DocumentView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// The same rich content read from different formats: an HTML page with headings, styled
/// runs, nested lists, a quote and a table, shown after a round trip through DOCX and ODT,
/// above a Markdown file of its own.
public class DocumentFormatsScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=DocumentFormats status=SKIPPED reason=document-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "DocumentFormats";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    static final String HTML = "<html><body><h1>Release Notes</h1>"
            + "<p>Version <b>8.0</b> adds <i>documents</i>, <u>spell checking</u> and "
            + "<span style=\"color:#c0392b\">tracked changes</span>. "
            + "See <a href=\"https://www.codenameone.com/\">the site</a>.</p>"
            + "<h2>Highlights</h2><ul><li>Paged layout<ul><li>Headers repeat on new pages</li></ul></li>"
            + "<li>PDF export</li></ul><ol><li>Read DOCX</li><li>Write ODT</li></ol>"
            + "<blockquote>Documents now open on every platform.</blockquote>"
            + "<table><tr><th>Format</th><th>Read</th><th>Write</th></tr>"
            + "<tr><td>DOCX</td><td>yes</td><td>yes</td></tr>"
            + "<tr><td>DOC</td><td>yes</td><td>no</td></tr>"
            + "<tr><td>ODT</td><td>yes</td><td>yes</td></tr></table></body></html>";

    static final String MARKDOWN = "# Meeting Notes\n\n"
            + "Attendees: **Ana**, *Ben* and `ops`.\n\n"
            + "## Decisions\n\n"
            + "1. Ship on **Monday**\n2. Freeze the API\n\n"
            + "- Update the [guide](https://www.codenameone.com/)\n- Tag the release\n\n"
            + "> Next meeting in two weeks.\n";

    static Document through(Document d, Document.Format f) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        d.write(out, f);
        return Document.read(new ByteArrayInputStream(out.toByteArray()), f);
    }

    static DocumentView view(Document d) {
        DocumentView v = new DocumentView(d);
        v.setEditable(false);
        v.setFitWidth(true);
        return v;
    }

    @Override
    protected void populate(Form form, String suffix) {
        Container c = new Container(new GridLayout(2, 1));
        try {
            Document html = Document.read(new ByteArrayInputStream(HTML.getBytes("UTF-8")),
                    Document.Format.HTML);
            c.add(view(through(through(html, Document.Format.DOCX), Document.Format.ODT)));
            c.add(view(Document.read(new ByteArrayInputStream(MARKDOWN.getBytes("UTF-8")),
                    Document.Format.MARKDOWN)));
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }
        form.add(BorderLayout.CENTER, c);
    }
}
