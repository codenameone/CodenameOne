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

import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.components.ImageViewer;
import com.codename1.ui.layouts.BorderLayout;

/// The same ImageViewer after a pinch has zoomed it in.
class ComponentsImageViewerZoomedFigure implements GuideFigure {

    private ImageViewer viewer;

    @Override
    public String id() {
        return "components-imageviewer-zoomed-in";
    }

    @Override
    public Form build() {
        Image duke = FontImage.createMaterial(FontImage.MATERIAL_INFO, "Label", 3.0f);
        Form hi = new Form("ImageViewer", new BorderLayout());
        viewer = new ImageViewer(duke);
        hi.add(BorderLayout.CENTER, viewer);
        hi.show();
        return hi;
    }

    /// A viewer works out its own zoom from the size it was given, so asking
    /// before it has one is asking nothing. This is the state a pinch would
    /// leave behind, which a still cannot show happening.
    @Override
    public void afterShow(Form form) {
        // setZoom animates by default: it starts a Motion and registers for
        // animation rather than moving anything now, so asking and then taking
        // the picture produced a frame identical to the unzoomed one. A still
        // cannot show the animation, so the figure asks for the end state.
        viewer.setAnimateZoom(false);
        viewer.setZoom(3.0f);
    }

    @Override
    public boolean fillsViewport() {
        return true;
    }
}
