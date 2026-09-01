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
package com.codename1.ui.html;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// `HTMLComponent` inside a window.
///
/// It reached for `getComponentForm()` in eighteen places -- to revalidate, to take
/// focus, to register its animation, to add and remove key listeners. All of them are
/// null inside a window, so a document there never laid out and never took focus.
class HTMLComponentInWindowTest extends UITestBase {

    private static HTMLComponent newDocument() {
        HTMLComponent html = new HTMLComponent();
        html.setBodyText("<html><body><p>Hello</p><a href=\"#x\">link</a></body></html>");
        return html;
    }

    @FormTest
    void anHtmlDocumentInAWindowResolvesThatWindow() {
        implementation.setMultiWindowSupported(true);
        Form main = new Form("main", new BorderLayout());
        main.show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(600, 500);
        HTMLComponent html = newDocument();
        w.add(BorderLayout.CENTER, html);
        w.show();
        DisplayTest.flushEdt();

        assertSame(w, html.getTopLevelContainer(),
                "the document resolves the window it is in");
        assertNull(html.getComponentForm(),
                "while getComponentForm stays null in a window by design, which is "
                        + "exactly why every one of those lookups had to move");

        w.dispose();
        DisplayTest.flushEdt();
    }

    @FormTest
    void anHtmlDocumentOnAFormIsUnchanged() {
        Form main = new Form("main", new BorderLayout());
        HTMLComponent html = newDocument();
        main.add(BorderLayout.CENTER, html);
        main.show();
        DisplayTest.flushEdt();

        assertSame(main, html.getComponentForm(),
                "the single surface path answers exactly as it always did");
        assertSame(main, html.getTopLevelContainer());
        assertNotNull(html.getParent());
    }

    @FormTest
    void anHtmlDocumentSurvivesBeingRemovedFromAWindow() {
        // deinitialize() deregisters the animation and drops key listeners through the
        // same lookups. Two of those sites dereferenced without a null check and would
        // have thrown on a detached component on any platform.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        Window w = new Window("host", new BorderLayout());
        w.setWindowSize(600, 500);
        HTMLComponent html = newDocument();
        w.add(BorderLayout.CENTER, html);
        w.show();
        DisplayTest.flushEdt();

        html.remove();
        w.revalidateWithAnimationSafety();
        DisplayTest.flushEdt();
        assertNull(html.getParent());
        assertNull(html.getTopLevelContainer());

        w.dispose();
        DisplayTest.flushEdt();
    }
}
