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

/**
 * Flutter's elevation-to-shadow mapping, approximated for Codename One's
 * {@code RoundBorder}/{@code RoundRectBorder}, shared by every elevated
 * surface in this runtime that borrows their shadow ({@link
 * CardRenderElement}, {@link ButtonRenderElement}, {@link FabRenderElement}).
 *
 * <p>Flutter composes a real multi-layer {@code BoxShadow} per elevation
 * (see {@code kElevationToShadow} in {@code painting/shadows.dart}); neither
 * border draws more than one band, so this is a best-effort single-band
 * approximation rather than that table -- darker and wider as elevation
 * grows, originally measured against {@link CardRenderElement}'s card
 * shadow and now shared so an elevated button or FAB scales the same way
 * instead of wearing one fixed look at every elevation.</p>
 */
final class ElevationShadow {

    private ElevationShadow() {
    }

    /**
     * {@code shadowOpacity} (0-255) for a Material {@code elevation}. Only ever
     * called for a positive elevation, so this starts visible rather than at 0
     * and reaches full strength by roughly elevation 16.
     */
    static int opacity(double elevation) {
        // Gentle on purpose: the band sits INSIDE the border (see spreadMm), so a
        // dark one reads as a frame around the surface rather than a shadow
        // under it -- 20 + 15/elevation put a near-black ring round Shrine's
        // NEXT (elevation 8) and the gallery's Back pill (elevation 6).
        return Math.min(110, (int) Math.round(30 + elevation * 8));
    }

    /**
     * {@code shadowSpread} in MILLIMETRES -- the overload that stores the value
     * directly, with no re-reading through {@code convertToPixels} (see the
     * comment on {@link ButtonRenderElement#applyRequestedShape} about the
     * {@code int} overload's double pixel conversion, which this sidesteps by
     * handing the mm value straight to the float/boolean-mm setters).
     *
     * <p>Floored at 1mm: {@code convertToPixels} rounds to a whole device
     * pixel, and the shadow is drawn by a ring loop in
     * {@code RoundRectBorder.createTargetImage}/{@code createTargetComponentImage}
     * and {@code RoundBorder.createTargetImage} alike -- {@code for (int iter =
     * shadowSpreadL - 1; iter >= 0; iter--)} -- that paints nothing at all once
     * the spread rounds down small enough. Measured directly against the
     * shipped gallery screenshots: {@link CardRenderElement}'s own un-floored
     * formula gives a Material-default card (elevation 1) a spread of
     * 0.25 + 1 * 0.25 = 0.5mm, and the card in
     * {@code docs/developer-guide/img/flutter-gallery-card.jpg} casts no
     * shadow at all -- a pixel scan of the image shows the card's fill
     * running flat to its own edge and jumping straight to the page's white,
     * with none of the darker band Flutter's copy shows just past that same
     * edge. The Shrine "NEXT" button three rows up in the same chapter
     * (elevation 8, {@code ButtonRenderElement.applyRequestedShape}) used a
     * fixed 1mm and DOES show a faint halo in its own screenshot, which is
     * the floor adopted here.
     *
     * <p>Capped at 1.8mm -- lower than {@code RoundRectBorder}'s own 3mm danger
     * zone (the Shrine NEXT regression this fix must not reintroduce; see
     * {@code ShapedElevatedButtonShadowTest} and the long comment in
     * {@code applyRequestedShape} on the class's squared growth at its
     * un-floored default) because the button role's own vertical capsule
     * padding ({@code ButtonRenderElement.CAPSULE_VPAD_LP}, 10lp on each edge)
     * is the tighter of the two budgets: {@code
     * theShadowsSpreadNeverOutgrowsTheButtonsOwnPadding} holds the spread to
     * UNDER that padding so the shrinking fill this border's shadow technique
     * uses (see that test's own class comment) never eats into the label's own
     * box, and elevation 8's uncapped 2.25mm already crossed it at this
     * suite's density. Opacity, not spread, is what mainly reads as
     * "stronger" in this single-band approximation, so capping spread early
     * costs little.
     */
    static float spreadMm(double elevation) {
        // Fixed at the 1mm floor. Growing it with elevation (up to 1.8mm) was
        // measured in the gallery screenshots, not just in the padding test:
        // NEXT's fill shrank until its label sat on the border's bottom edge,
        // and the Back pill grew a halo on every side. Elevation still darkens
        // the band through opacity().
        return 1f;
    }
}
