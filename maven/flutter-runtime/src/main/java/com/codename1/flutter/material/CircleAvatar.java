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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

/**
 * A circular avatar showing an image or a child (initials/icon) — Flutter's
 * {@code CircleAvatar}.
 *
 * <p>It used to render the child and nothing else, so an avatar was a bare
 * character where the design has a filled disc: every row of the lists demo
 * showed a small black number instead of a purple circle with a white one.
 *
 * <p>The default colours approximate Flutter's, which resolve through
 * {@code primaryColorLight} and {@code primaryTextTheme}; the colour scheme's
 * primary pair is the closest thing this runtime models.</p>
 */
public class CircleAvatar extends StatelessWidget {

    private Widget child;
    private Color backgroundColor;
    private Color foregroundColor;
    private ImageProvider backgroundImage;
    private ImageProvider foregroundImage;
    private Double radius;

    public void child(Widget v) {
        this.child = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void foregroundColor(Color v) {
        this.foregroundColor = v;
    }

    public void backgroundImage(ImageProvider v) {
        this.backgroundImage = v;
    }

    public void foregroundImage(ImageProvider v) {
        this.foregroundImage = v;
    }

    public void onBackgroundImageError(Object v) {
    }

    public void radius(double v) {
        this.radius = v;
    }

    public void minRadius(double v) {
    }

    public void maxRadius(double v) {
    }

    /** Flutter's {@code _defaultRadius}. */
    private static final double DEFAULT_RADIUS = 20;

    @Override
    public Widget build(BuildContext context) {
        double r = radius != null ? radius : DEFAULT_RADIUS;

        Color bg = backgroundColor;
        Color fg = foregroundColor;
        if (bg == null || fg == null) {
            try {
                ColorScheme cs = Theme.of(context).colorScheme();
                if (bg == null) {
                    bg = cs.primary();
                }
                if (fg == null) {
                    fg = cs.onPrimary();
                }
            } catch (Throwable t) {
                // no ambient theme; the disc still gets its shape below
            }
        }

        com.codename1.flutter.BoxDecoration decoration =
                new com.codename1.flutter.BoxDecoration();
        decoration.shape(com.codename1.flutter.BoxShape.circle);
        if (bg != null) {
            decoration.color(bg);
        }
        com.codename1.flutter.widgets.Container box =
                new com.codename1.flutter.widgets.Container();
        box.width(r * 2);
        box.height(r * 2);
        box.decoration(decoration);
        box.alignment(com.codename1.flutter.Alignment.center);
        Widget content = imageChild() != null ? imageChild() : child;
        if (content != null && fg != null) {
            com.codename1.flutter.TextStyle style = new com.codename1.flutter.TextStyle();
            style.color(fg);
            content = com.codename1.flutter.widgets.DefaultTextStyle.wrap(style, content);
            content = IconTheme.tint(fg, content);
        }
        if (content != null) {
            // The disc is a circular BACKGROUND; the picture drawn on top of it
            // is a rectangle unless something clips it, which is why every
            // avatar came out square over a round patch of colour.
            com.codename1.flutter.widgets.ClipOval round =
                    new com.codename1.flutter.widgets.ClipOval();
            round.child(content);
            box.child(round);
        }
        return box;
    }

    /** The avatar's picture, when it has one, sized to fill the disc. */
    private Widget imageChild() {
        ImageProvider provider = foregroundImage != null ? foregroundImage : backgroundImage;
        if (provider == null) {
            return null;
        }
        com.codename1.flutter.widgets.Image img = new com.codename1.flutter.widgets.Image();
        img.image(provider);
        img.fit(com.codename1.flutter.BoxFit.cover);
        return img;
    }
}
