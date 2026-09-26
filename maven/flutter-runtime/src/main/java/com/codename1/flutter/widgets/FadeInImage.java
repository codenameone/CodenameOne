/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.core.Duration;

/**
 * Shows a {@code placeholder} image while the target {@code image} loads, then
 * cross-fades to it — Flutter's {@code FadeInImage}. This pass reserves the
 * box (when width/height are given) and holds both providers; the decode and
 * fade animation are deferred to the image layer.
 */
public class FadeInImage extends StatelessWidget {

    private ImageProvider placeholder;
    private ImageProvider image;
    private Duration fadeInDuration;
    private Duration fadeOutDuration;
    private Double width;
    private Double height;
    private Object fit;

    public void placeholder(ImageProvider v) {
        this.placeholder = v;
    }

    public void image(ImageProvider v) {
        this.image = v;
    }

    public void fadeInDuration(Duration v) {
        this.fadeInDuration = v;
    }

    public void fadeOutDuration(Duration v) {
        this.fadeOutDuration = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void fit(Object v) {
        this.fit = v;
    }

    public void alignment(Object v) {
    }

    public void repeat(Object v) {
    }

    public void placeholderFit(Object v) {
    }

    /** Whether the image is hidden from semantics — Flutter's {@code excludeFromSemantics}. */
    public void excludeFromSemantics(boolean v) {
    }

    public ImageProvider getImage() {
        return image;
    }

    @Override
    public Widget build(BuildContext context) {
        // Render the target image directly (the cross-fade from the placeholder is
        // deferred). Falls back to the placeholder, then an empty box, when absent.
        ImageProvider shown = image != null ? image : placeholder;
        if (shown != null) {
            Image img = new Image();
            img.image(shown);
            if (width != null) {
                img.width(width);
            }
            if (height != null) {
                img.height(height);
            }
            if (fit != null) {
                img.fit(fit);
            }
            return img;
        }
        SizedBox box = new SizedBox();
        if (width != null) {
            box.width(width);
        }
        if (height != null) {
            box.height(height);
        }
        return box;
    }
}
