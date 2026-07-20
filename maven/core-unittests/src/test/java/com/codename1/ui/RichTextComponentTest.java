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

package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestUtils;
import com.codename1.ui.editor.RichBlocks;
import com.codename1.ui.editor.RichRunPainter;
import com.codename1.ui.editor.TextStyle;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional and layout tests for {@link RichTextComponent}: content parsing, the builder API,
 * height-for-width wrapping, heading scale, link dispatch and a rendered-state screenshot.
 */
class RichTextComponentTest extends UITestBase {

    @FormTest
    void emptyByDefault() {
        RichTextComponent r = new RichTextComponent();
        assertEquals("", r.getText());
    }

    @FormTest
    void plainTextRoundTrips() {
        RichTextComponent r = new RichTextComponent();
        r.setText("Hello world");
        assertEquals("Hello world", r.getText());
    }

    @FormTest
    void plainTextConstructor() {
        assertEquals("Hi there", new RichTextComponent("Hi there").getText());
    }

    @FormTest
    void htmlStripsMarkupToPlainText() {
        RichTextComponent r = new RichTextComponent();
        r.setHtml("<p>Hello <b>bold</b> and <i>italic</i></p>");
        assertEquals("Hello bold and italic", r.getText());
    }

    @FormTest
    void markdownParsesToText() {
        RichTextComponent r = new RichTextComponent();
        r.setMarkdown("# Title\n\nSome **bold** text");
        String t = r.getText();
        assertTrue(t.contains("Title"), t);
        assertTrue(t.contains("bold"), t);
    }

    @FormTest
    void appendBuildsContent() {
        RichTextComponent r = new RichTextComponent();
        r.append("Hello ", TextStyle.DEFAULT).append("bold", TextStyle.DEFAULT.withBold(true));
        assertEquals("Hello bold", r.getText());
    }

    @FormTest
    void clearEmptiesContent() {
        RichTextComponent r = new RichTextComponent();
        r.setText("stuff");
        r.clear();
        assertEquals("", r.getText());
    }

    @FormTest
    void narrowerWidthWrapsToGreaterHeight() {
        RichTextComponent r = new RichTextComponent();
        r.setText("The quick brown fox jumps over the lazy dog again and again and again");
        int wide = r.preferredSizeForWidth(4000).getHeight();
        int narrow = r.preferredSizeForWidth(120).getHeight();
        assertTrue(narrow > wide,
                "wrapping at a narrow width must increase height: narrow=" + narrow + " wide=" + wide);
    }

    @FormTest
    void preferredWidthTracksContentLength() {
        RichTextComponent r = new RichTextComponent();
        r.setText("x");
        int small = r.preferredSizeForWidth(4000).getWidth();
        r.setText("a much longer single line of text without any breaks");
        int big = r.preferredSizeForWidth(4000).getWidth();
        assertTrue(big > small, "longer content must be wider: big=" + big + " small=" + small);
    }

    @FormTest
    void headingScaleAndAbsolutePixelSizeDriveRunSize() {
        // Deterministic run-size math (no font derivation, so it is stable headlessly):
        // an h1 run is twice the base size, and an absolute pixel size overrides both the
        // base size and the relative level.
        RichRunPainter p = new RichRunPainter();
        p.setBaseSizePx(20);
        assertEquals(40, p.runPx(RichBlocks.H1, TextStyle.DEFAULT), "h1 scales the base size by 2x");
        assertEquals(20, p.runPx(RichBlocks.PARAGRAPH, TextStyle.DEFAULT), "a paragraph keeps the base size");
        assertEquals(30, p.runPx(RichBlocks.PARAGRAPH, TextStyle.DEFAULT.withFontSizePx(30)),
                "an absolute pixel size wins over the base size");
        assertEquals(60, p.runPx(RichBlocks.H1, TextStyle.DEFAULT.withFontSizePx(30)),
                "heading scale still applies on top of an absolute pixel size");
    }

    @FormTest
    void multipleParagraphsAreTallerThanOne() {
        RichTextComponent one = new RichTextComponent();
        one.setText("line one");
        RichTextComponent three = new RichTextComponent();
        three.setText("line one\nline two\nline three");
        assertTrue(three.preferredSizeForWidth(4000).getHeight() > one.preferredSizeForWidth(4000).getHeight());
    }

    @FormTest
    void sizeModeDefaultsToShrink() {
        assertEquals(RichTextComponent.SizeMode.SHRINK, new RichTextComponent().getSizeMode());
    }

    @FormTest
    void linkListenerFiresWithHref() {
        final AtomicReference<String> fired = new AtomicReference<String>();
        RichTextComponent r = new RichTextComponent();
        r.getAllStyles().setPadding(0, 0, 0, 0);
        r.append("Link", TextStyle.DEFAULT, "http://codenameone.com");
        r.addLinkListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.set((String) evt.getSource());
            }
        });
        Form form = new Form("links", BoxLayout.y());
        form.add(r);
        form.show();
        flushSerialCalls();
        // tap near the top-left where the single "Link" word is laid out
        r.pointerReleased(r.getAbsoluteX() + 3, r.getAbsoluteY() + r.getHeight() / 2);
        assertEquals("http://codenameone.com", fired.get());
    }

    @FormTest
    void tapOutsideAnyLinkDoesNotFire() {
        final AtomicReference<String> fired = new AtomicReference<String>();
        RichTextComponent r = new RichTextComponent();
        r.getAllStyles().setPadding(0, 0, 0, 0);
        r.append("no link here", TextStyle.DEFAULT);
        r.addLinkListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fired.set("fired");
            }
        });
        Form form = new Form("links", BoxLayout.y());
        form.add(r);
        form.show();
        flushSerialCalls();
        r.pointerReleased(r.getAbsoluteX() + 3, r.getAbsoluteY() + r.getHeight() / 2);
        assertFalse(fired.get() != null, "no link listener should fire over plain text");
    }

    @FormTest
    void renderedStatesScreenshot() {
        Form form = new Form("RichText", BoxLayout.y());
        form.getStyle().setPadding(4, 4, 4, 4);

        RichTextComponent md = new RichTextComponent();
        md.setMarkdown("# Heading\n\nA paragraph with **bold**, *italic* and `code`, then a "
                + "[link](https://codenameone.com) that should wrap onto multiple lines when the "
                + "component is narrow enough to force word wrapping.");

        RichTextComponent list = new RichTextComponent();
        list.setMarkdown("Shopping:\n\n- apples\n- oranges\n- pears\n\n1. first\n2. second\n3. third");

        RichTextComponent html = new RichTextComponent();
        html.setHtml("<blockquote>A quoted block</blockquote>"
                + "<p style=\"text-align:center\">centered paragraph</p>"
                + "<pre>preformatted  text</pre>");

        form.addAll(md, list, html);
        form.show();
        flushSerialCalls();

        assertTrue(TestUtils.screenshotTest("RichTextComponentStates"));
    }

    @FormTest
    void getPreferredSizeIsPositiveWhenShown() {
        RichTextComponent r = new RichTextComponent();
        r.setText("some content here");
        Form form = new Form("pref", new BorderLayout());
        form.add(BorderLayout.CENTER, r);
        form.show();
        flushSerialCalls();
        Dimension d = r.getPreferredSize();
        assertTrue(d.getWidth() > 0 && d.getHeight() > 0);
    }
}
