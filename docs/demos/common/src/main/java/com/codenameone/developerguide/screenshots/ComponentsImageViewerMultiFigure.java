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

import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.components.ImageViewer;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.DefaultListModel;

/// An ImageViewer backed by a list, so it can be swiped between images.
class ComponentsImageViewerMultiFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-imageviewer-multi";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-085[]
        Form hi = new Form("ImageViewer", new BorderLayout());

        Image red = Image.createImage(100, 100, 0xffff0000);
        Image green = Image.createImage(100, 100, 0xff00ff00);
        Image blue = Image.createImage(100, 100, 0xff0000ff);
        Image gray = Image.createImage(100, 100, 0xffcccccc);

        ImageViewer iv = new ImageViewer(red);
        iv.setImageList(new DefaultListModel<>(red, green, blue, gray));
        hi.add(BorderLayout.CENTER, iv);
        // end::the-components-of-codename-one-java-085[]
        hi.show();
        return hi;
    }

    @Override
    public boolean fillsViewport() {
        return true;
    }
}
