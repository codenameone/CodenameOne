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

import com.codename1.ui.Form;
import com.codename1.ui.RichTextArea;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Screenshot coverage for {@link RichTextArea}. Renders a non-editable (caret-free, for a
 * deterministic capture) rich text document.
 *
 * The test forces the pure Codename One text engine (see {@link ScreenshotPureEditors}), which paints
 * through the regular component pipeline on every port, so the capture simply waits for the editor's
 * ready event -- no web view, settle timers or retry machinery is involved.
 */
public class RichTextAreaScreenshotTest extends BaseTest {
    private RichTextArea editor;
    private FirstPaintGate gate;

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm("Rich Text", new BorderLayout(), "RichTextArea");
        editor = new ScreenshotPureEditors.Rich();
        editor.setEditable(false);
        editor.setHtml("<h2>Trip itinerary</h2>"
                + "<p>Meet at the <b>main lobby</b> by <i>9:00 AM</i>. Bring a "
                + "<span style=\"color:#d93025\">valid passport</span> and your "
                + "<a href=\"#\">boarding pass</a>.</p>"
                + "<ul><li>Day 1 &mdash; city tour</li><li>Day 2 &mdash; museum &amp; harbor</li>"
                + "<li>Day 3 &mdash; free time</li></ul>");
        gate = new FirstPaintGate(editor);
        form.add(BorderLayout.CENTER, gate);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(final Form parent, final Runnable run) {
        // capture after the backend reports ready AND the editor's content actually painted;
        // ready alone races the first paint flush on the slower ports. Then wait out any
        // animation still in flight (e.g. the form show transition on a slow CI emulator,
        // which the capture would otherwise catch mid-fade).
        editor.onReady(new Runnable() {
            public void run() {
                gate.runAfterNextPaint(new Runnable() {
                    public void run() {
                        parent.getAnimationManager().flushAnimation(run);
                    }
                });
            }
        });
    }
}
