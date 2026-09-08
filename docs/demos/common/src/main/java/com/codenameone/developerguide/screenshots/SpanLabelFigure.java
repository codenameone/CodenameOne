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

package com.codenameone.developerguide.screenshots;

import com.codename1.components.SpanLabel;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// The SpanLabel figure for the Components chapter.
///
/// The tagged region below is what the chapter includes, so the listing beside
/// the picture is the code that drew it. That is the point of moving these out
/// of the compile-only snippet fixtures: a fixture and a screenshot are two
/// sources for one example and drift apart silently.
public final class SpanLabelFigure implements GuideFigure {
    @Override
    public String id() {
        return "components-spanlabel";
    }

    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-160[]
        Form hi = new Form("SpanLabel", new BoxLayout(BoxLayout.Y_AXIS));
        Image icon = FontImage.createMaterial(FontImage.MATERIAL_INFO, "Label", 3.0f);
        SpanLabel d = new SpanLabel("Default SpanLabel that can seamlessly line break when the text is really long.");
        d.setIcon(icon);
        SpanLabel l = new SpanLabel("NORTH Positioned Icon SpanLabel that can seamlessly line break when the text is really long.");
        l.setIcon(icon);
        l.setIconPosition(BorderLayout.NORTH);
        SpanLabel r = new SpanLabel("SOUTH Positioned Icon SpanLabel that can seamlessly line break when the text is really long.");
        r.setIcon(icon);
        r.setIconPosition(BorderLayout.SOUTH);
        SpanLabel c = new SpanLabel("EAST Positioned Icon SpanLabel that can seamlessly line break when the text is really long.");
        c.setIcon(icon);
        c.setIconPosition(BorderLayout.EAST);
        hi.add(d).add(l).add(r).add(c);
        hi.show();
        // end::the-components-of-codename-one-java-160[]
        return hi;
    }
}
