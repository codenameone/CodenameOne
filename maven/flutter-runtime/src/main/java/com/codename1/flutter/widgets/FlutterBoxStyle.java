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
                // RoundBorder with no shadow, which Codename One now draws
                // straight onto the Graphics rather than through a
                // component-sized image -- the thing that made an image-backed
                // circle unusable for a coach mark, whose radius animates.
                face.getAllStyles().setBorder(RoundBorder.create()
                        .directPaint(true)
                        .color(bg.rgb())
                        .opacity(bg.alpha()));
                face.getAllStyles().setBgTransparency(0);
                return;
            }
            double radiusLp = cornerRadiusLp(decoration);
            boolean shadowed = decoration instanceof BoxDecoration
                    && ((BoxDecoration) decoration).getBoxShadow() != null;
            if (radiusLp > 0 || shadowed) {
                // Rounded corners and elevation are what make Material look like
                // Material; a flat bgColor drops both.
                // The cache is switched on exactly when there is a shadow, and
                // that is a correctness requirement rather than a tuning knob:
                // with it off, RoundRectBorder renders through
                // createTargetComponentImage, which translates the LIVE Graphics
                // by the shadow's shape offset and never undoes it, so every
                // sibling painted afterwards is displaced -- and displaced
                // again by the next shadowed box, accumulating down the page.
                // With no shadow that offset is zero and the cheaper uncached
                // path is exact. See CardRenderElement, where this cost the
                // cards demo 9 device pixels per card.
                com.codename1.ui.plaf.RoundRectBorder border =
                        com.codename1.ui.plaf.RoundRectBorder.create()
                                .useCache(shadowed)
                                .cornerRadius(com.codename1.flutter.rendering.Dp.mm(radiusLp));
                if (shadowed) {
                    border = border.shadowOpacity(40).shadowSpread(0.5f).shadowY(1);
                }
                border = withOutline(border, decoration);
                face.getAllStyles().setBorder(border);
                if (bg != null) {
                    face.getAllStyles().setBgColor(bg.rgb());
                    face.getAllStyles().setBgTransparency(bg.alpha());
                } else {
                    face.getAllStyles().setBgTransparency(0);
                }
                applyDecorationImage(face, decoration);
                return;
            }
            if (bg != null) {
                face.getAllStyles().setBgColor(bg.rgb());
                face.getAllStyles().setBgTransparency(bg.alpha());
            } else {
                face.getAllStyles().setBgTransparency(0);
            }
            applyDecorationImage(face, decoration);
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
            // getImage() belongs here: a decoration whose ONLY content is an
            // image paints something, and leaving it out of this test meant no
            // component was created for it at all -- so the code that paints
            // the image was never reached and the fix for it looked inert.
            return d.getColor() != null || d.getGradient() != null
                    || d.getBorder() != null || d.getBoxShadow() != null
                    || d.getImage() != null;
        }
        return decoration != null;
    }

    /**
     * Paints a {@code BoxDecoration}'s {@link com.codename1.flutter.DecorationImage}
     * as the component's background.
     *
     * <p>The decoration image was stored and never read, so every
     * {@code BoxDecoration(image:)} in the app painted nothing at all. The cards
     * demo is where it shows worst: {@code Ink.image} is how a Material card puts
     * a photo behind its ink splash, so the cards kept their layout and their
     * captions and lost every photograph.</p>
     *
     * <p>Codename One paints a background image from the style, so the mapping is
     * {@code BoxFit} onto a background type. Only the fits a background can
     * express are mapped; anything else takes {@code cover}, which is both
     * Flutter's common case here and the one that never leaves the box
     * part-empty.</p>
     */
    private static void applyDecorationImage(Component face, Object decoration) {
        if (!(decoration instanceof BoxDecoration)) {
            return;
        }
        Object raw = ((BoxDecoration) decoration).getImage();
        if (!(raw instanceof com.codename1.flutter.DecorationImage)) {
            return;
        }
        com.codename1.flutter.DecorationImage di = (com.codename1.flutter.DecorationImage) raw;
        com.codename1.ui.Image img = load(di.getImage());
        if (img == null) {
            return;
        }
        face.getAllStyles().setBgImage(img);
        face.getAllStyles().setBackgroundType(backgroundType(di.getFit()));
    }

    /** The CN1 background type for a {@code BoxFit}. */
    private static byte backgroundType(com.codename1.flutter.BoxFit fit) {
        if (fit == com.codename1.flutter.BoxFit.contain
                || fit == com.codename1.flutter.BoxFit.scaleDown) {
            return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_SCALED_FIT;
        }
        if (fit == com.codename1.flutter.BoxFit.fill) {
            return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_SCALED;
        }
        return com.codename1.ui.plaf.Style.BACKGROUND_IMAGE_SCALED_FILL;
    }

    /**
     * Decodes an {@link com.codename1.flutter.ImageProvider} that names a bundled
     * asset. A network provider is not resolved here: a background image has no
     * placeholder to show while it arrives.
     */
    private static com.codename1.ui.Image load(com.codename1.flutter.ImageProvider provider) {
        if (!(provider instanceof com.codename1.flutter.AssetImage)) {
            return null;
        }
        String name = ((com.codename1.flutter.AssetImage) provider).resolvedName();
        try {
            com.codename1.flutter.FlutterAssets.Resolved res =
                    com.codename1.flutter.FlutterAssets.open(FlutterBoxStyle.class, name);
            if (res == null) {
                com.codename1.io.Log.p("Flutter runtime: decoration image not found: " + name);
                return null;
            }
            try {
                return com.codename1.ui.EncodedImage.create(res.stream());
            } finally {
                com.codename1.io.Util.cleanup(res.stream());   // the chosen stream is ours to close
            }
        } catch (Exception cannotDecode) {
            com.codename1.io.Log.p("Flutter runtime: could not decode decoration image " + name);
            return null;
        }
    }

    /**
     * Adds a {@code BoxDecoration.border}'s stroke to a rounded border.
     *
     * <p>The border was consulted only to decide whether the decoration paints
     * anything, never drawn. An outlined chip is the case that shows it: the
     * action chip's whole appearance IS its outline, so without this it read as
     * an icon and some text loose on the page.</p>
     */
    private static com.codename1.ui.plaf.RoundRectBorder withOutline(
            com.codename1.ui.plaf.RoundRectBorder border, Object decoration) {
        if (!(decoration instanceof BoxDecoration)) {
            return border;
        }
        Object raw = ((BoxDecoration) decoration).getBorder();
        if (!(raw instanceof com.codename1.flutter.Border)) {
            return border;
        }
        com.codename1.flutter.BorderSide side = ((com.codename1.flutter.Border) raw).top();
        if (side == null || side.color() == null) {
            return border;
        }
        double w = side.width() <= 0 ? 1 : side.width();
        return border
                .strokeColor(side.color().rgb())
                .strokeOpacity(side.color().alpha())
                .stroke((float) com.codename1.flutter.rendering.Dp.px(w), false);
    }
}
