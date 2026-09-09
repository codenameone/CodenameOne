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

import com.codename1.flutter.BoxDecoration;
import com.codename1.flutter.BoxShape;
import com.codename1.flutter.Color;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.RoundBorder;

/**
 * Applies a background color and (best-effort) shape from a Flutter
 * {@link BoxDecoration} or plain {@link Color} onto a CN1 component's style.
 * Shared by {@link ColoredBoxRenderElement}, {@link DecoratedBoxRenderElement}
 * and {@link ContainerRenderElement}.
 *
 * <p>Only color and circle shape are honored for this milestone; gradients,
 * borders, border radii, shadows and decoration images are not yet painted.</p>
 */
final class FlutterBoxStyle {

    private FlutterBoxStyle() {
    }

    /**
     * Styles {@code face} from an explicit {@code color} and/or a
     * {@code decoration} (expected to be a {@link BoxDecoration}). The explicit
     * color wins when both are present, matching Flutter (which forbids both).
     */
    static void apply(Component face, Color color, Object decoration) {
        try {
            Color bg = color;
            BoxShape shape = BoxShape.rectangle;
            if (decoration instanceof BoxDecoration) {
                BoxDecoration d = (BoxDecoration) decoration;
                if (bg == null) {
                    bg = d.getColor();
                }
                shape = d.getShape();
            }
            if (shape == BoxShape.circle && bg != null) {
                face.getAllStyles().setBorder(
                        RoundBorder.create().color(bg.rgb()).opacity(bg.alpha()));
                face.getAllStyles().setBgTransparency(0);
                return;
            }
            double radiusLp = cornerRadiusLp(decoration);
            boolean shadowed = decoration instanceof BoxDecoration
                    && ((BoxDecoration) decoration).getBoxShadow() != null;
            if (radiusLp > 0 || shadowed) {
                // Rounded corners and elevation are what make Material look like
                // Material; a flat bgColor drops both.
                com.codename1.ui.plaf.RoundRectBorder border =
                        com.codename1.ui.plaf.RoundRectBorder.create()
                                .useCache(false)
                                .cornerRadius(com.codename1.flutter.rendering.Dp.mm(radiusLp));
                if (shadowed) {
                    border = border.shadowOpacity(40).shadowSpread(0.5f).shadowY(1);
                }
                face.getAllStyles().setBorder(border);
                if (bg != null) {
                    face.getAllStyles().setBgColor(bg.rgb());
                    face.getAllStyles().setBgTransparency(bg.alpha());
                } else {
                    face.getAllStyles().setBgTransparency(0);
                }
                return;
            }
            if (bg != null) {
                face.getAllStyles().setBgColor(bg.rgb());
                face.getAllStyles().setBgTransparency(bg.alpha());
            } else {
                face.getAllStyles().setBgTransparency(0);
            }
        } catch (Exception err) {
            // styling is best-effort; layout must survive regardless
        }
    }

    /**
     * The decoration's corner radius in logical pixels, or 0 when it is square.
     * CN1 draws one radius for all four corners, so a decoration with mixed corners
     * takes its top-left — closer than dropping the rounding altogether.
     */
    private static double cornerRadiusLp(Object decoration) {
        if (!(decoration instanceof BoxDecoration)) {
            return 0;
        }
        Object br = ((BoxDecoration) decoration).getBorderRadius();
        if (br instanceof com.codename1.flutter.BorderRadius) {
            com.codename1.flutter.Radius r = ((com.codename1.flutter.BorderRadius) br).topLeft();
            return r == null ? 0 : r.x();
        }
        return 0;
    }

    /**
     * True when the given color/decoration would paint anything — used to
     * decide whether a face component is worth creating.
     */
    static boolean paints(Color color, Object decoration) {
        if (color != null) {
            return true;
        }
        if (decoration instanceof BoxDecoration) {
            BoxDecoration d = (BoxDecoration) decoration;
            return d.getColor() != null || d.getGradient() != null
                    || d.getBorder() != null || d.getBoxShadow() != null;
        }
        return decoration != null;
    }
}
