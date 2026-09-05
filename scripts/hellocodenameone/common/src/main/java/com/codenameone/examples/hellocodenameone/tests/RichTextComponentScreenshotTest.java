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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.RichTextComponent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Screenshot coverage for the read-only {@link RichTextComponent}. Renders a mix of Markdown and HTML
 * content exercising headings, inline styles, lists, a block quote, a centered paragraph, a
 * preformatted block, a hyperlink, and multi-line word wrapping.
 *
 * <p>The component paints synchronously through the regular component pipeline on every port, so the
 * capture only needs to wait for the content's first real paint (via {@link FirstPaintGate}); no web
 * view, ready event or settle-timer machinery is involved.</p>
 */
public class RichTextComponentScreenshotTest extends BaseTest {
    private FirstPaintGate gate;

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm("Rich Text View", new BorderLayout(), "RichTextComponent");

        Container content = new Container(BoxLayout.y());
        content.setScrollableY(true);
        content.getAllStyles().setPadding(4, 4, 4, 4);

        RichTextComponent md = new RichTextComponent();
        md.setMarkdown("# Trip summary\n\n"
                + "Departs **09:40**, arrives *11:15*, gate `B12`. Bring a passport and your "
                + "[boarding pass](app://pass). This paragraph is intentionally long so it wraps onto "
                + "several lines at a typical phone width.\n\n"
                + "- Window seat\n"
                + "- Carry-on only\n"
                + "- Vegetarian meal\n\n"
                + "1. Check in\n"
                + "2. Security\n"
                + "3. Board");

        RichTextComponent html = new RichTextComponent();
        html.setHtml("<blockquote>Travel light; a packed bag is a happy bag.</blockquote>"
                + "<p style=\"text-align:center\">Have a great trip!</p>"
                + "<pre>PNR   ABC123\nSeat  14A</pre>");

        content.addAll(md, html);
        gate = new FirstPaintGate(content);
        form.add(BorderLayout.CENTER, gate);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(final Form parent, final Runnable run) {
        // capture after the content actually painted, then wait out any form-show animation still in
        // flight so the capture is not caught mid-fade on a slow CI emulator.
        gate.runAfterNextPaint(new Runnable() {
            public void run() {
                parent.getAnimationManager().flushAnimation(run);
            }
        });
    }
}
