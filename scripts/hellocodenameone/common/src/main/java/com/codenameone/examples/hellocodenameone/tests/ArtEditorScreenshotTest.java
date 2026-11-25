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

import com.codename1.art.ArtDocument;
import com.codename1.art.ArtEditor;
import com.codename1.art.ArtModel;
import com.codename1.art.SvgReader;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/// The `com.codename1.art` editor showing a run-time SVG document: gradients, a blend mode,
/// a filter drop shadow and text on a path, all drawn by the software renderer, so every
/// port should produce the same canvas.
public class ArtEditorScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=ArtEditor status=SKIPPED reason=editor-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "ArtEditor";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    static final String SVG = "<svg xmlns='http://www.w3.org/2000/svg' width='240' height='180'>"
            + "<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>"
            + "<stop offset='0' stop-color='#1e88e5'/><stop offset='1' stop-color='#8e24aa'/></linearGradient>"
            + "<filter id='s'><feDropShadow dx='3' dy='4' stdDeviation='3' flood-color='#000' flood-opacity='0.5'/>"
            + "</filter><path id='arc' d='M30 150 Q120 60 210 150'/></defs>"
            + "<rect x='10' y='10' width='220' height='160' rx='14' fill='url(#g)'/>"
            + "<circle cx='90' cy='80' r='45' fill='#ffca28' filter='url(#s)'/>"
            + "<circle cx='150' cy='80' r='45' fill='#26a69a' style='mix-blend-mode:multiply'/>"
            + "<text font-size='16' fill='#fff'><textPath href='#arc' startOffset='50%' text-anchor='middle'>"
            + "Codename One</textPath></text></svg>";

    @Override
    protected void populate(Form form, String suffix) {
        ArtDocument doc;
        try {
            doc = SvgReader.read(new ByteArrayInputStream(SVG.getBytes("UTF-8")));
        } catch (IOException e) {
            throw new IllegalStateException(e.toString());
        }
        ArtEditor editor = new ArtEditor(new ArtModel(doc));
        form.add(BorderLayout.CENTER, editor);
    }
}
