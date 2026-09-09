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

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * An icon rendered from an {@link ImageProvider} rather than an icon font —
 * Flutter's {@code ImageIcon}. Captures the image, size and tint; this pass
 * reserves the icon's box via a {@link SizedBox}, with the actual image decode
 * and tinting deferred to the image layer.
 */
public class ImageIcon extends StatelessWidget {

    private final ImageProvider image;
    private Double size;
    private Color color;

    public ImageIcon(ImageProvider image) {
        this.image = image;
    }

    public void size(double v) {
        this.size = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public void semanticLabel(String v) {
    }

    public ImageProvider getImage() {
        return image;
    }

    @Override
    public Widget build(BuildContext context) {
        double side = size != null ? size.doubleValue() : iconThemeSize(context);
        if (image == null) {
            SizedBox box = new SizedBox();
            box.width(side);
            box.height(side);
            return box;
        }
        // This used to return the empty SizedBox above and nothing else, so an ImageIcon
        // reserved its space and drew nothing -- the mail study's logo is one, and its
        // bottom bar simply had a gap where the mark belongs.
        Image img = new Image();
        img.image(image);
        img.width(Double.valueOf(side));
        img.height(Double.valueOf(side));
        // An icon is CONTAINED in its box: Flutter sizes an ImageIcon by the icon theme
        // and expects the artwork to fit inside that square whatever its aspect.
        img.fit(BoxFit.contain);
        return img;
    }

    /// {@code IconTheme.of(context).size}, or Material's 24 when there is none -- the
    /// same fallback an Icon uses.
    private static double iconThemeSize(BuildContext context) {
        try {
            com.codename1.flutter.material.IconThemeData theme =
                    com.codename1.flutter.material.IconTheme.of(context);
            if (theme != null && theme.size() != null) {
                return theme.size().doubleValue();
            }
        } catch (Throwable noTheme) {
            // fall through to Material's default
        }
        return 24.0;
    }
}
