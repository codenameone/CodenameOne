/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.camera;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;

/** Hosts the CameraView of a {@link CameraPreview}'s session; none before initialize or headless. */
public class CameraPreviewRenderElement extends RenderElement {
    public CameraPreviewRenderElement(CameraPreview widget) {
        super(widget);
    }

    @Override
    protected Component createComponent() {
        CameraController c = ((CameraPreview) widget()).controller;
        if (c == null || c.session() == null) {
            return null;
        }
        return c.session().createView();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Size want = ((CameraPreview) widget()).preferredLayoutSize();
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : Dp.px(want.width());
        double h = constraints.hasBoundedHeight() ? constraints.maxHeight() : Dp.px(want.height());
        return constraints.constrain(new Size(w, h));
    }
}
