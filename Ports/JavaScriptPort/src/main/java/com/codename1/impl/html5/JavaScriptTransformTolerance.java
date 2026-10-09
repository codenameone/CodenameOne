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
package com.codename1.impl.html5;

/// Decides whether a 2D affine transform is the identity for the purpose of
/// drawing text as DOM nodes above the canvas, which the port does only under
/// an identity transform: a DOM rectangle cannot represent any other.
///
/// The test cannot be exact. Application and framework code routinely turns
/// the graphics for one thing and turns it back -- `rotate(a)`, draw,
/// `rotate(-a)` -- and that round trip is not exact arithmetic: the angle is
/// a `float`, its sine and cosine are rounded, and what is left differs from
/// the identity in the fifteenth digit. Compared with `===` that residue was
/// "a transform", so every string painted after it moved from the DOM layer
/// to the canvas, whose font metrics differ by a few pixels. One rotated
/// label in a chart moved the title of the form it was on, and nothing in
/// the application said why.
///
/// So a transform this close to the identity is taken for it. The bounds are
/// set by what could be seen, not by what the residue happens to be:
///
/// - The four linear entries may be off by `LINEAR_EPSILON`. A point is moved
///   by that times its coordinate, so at 100,000 pixels from the origin the
///   error is a ten-thousandth of a pixel. The residue of a round trip is five
///   orders of magnitude smaller still.
/// - The two translation entries ARE pixels, and may be off by
///   `TRANSLATION_EPSILON` of one. A deliberate sub-pixel translation is a
///   real transform the canvas honours and the DOM layer would not, so this
///   stays far below anything a caller could mean, and far above the residue
///   of rotating about a pivot (the linear residue times the pivot).
///
/// A value that is not a number is never the identity.
///
/// This class has no natives and no dependencies, so that it compiles and is
/// tested alone under plain JUnit (`JavaScriptTransformToleranceTest`).
public final class JavaScriptTransformTolerance {
    /// How far a scale or shear entry may be from the identity's.
    public static final double LINEAR_EPSILON = 1e-9;
    /// How far a translation entry may be from zero, in pixels.
    public static final double TRANSLATION_EPSILON = 1e-6;

    private JavaScriptTransformTolerance() {
    }

    /// Whether the matrix `[m00 m01 m02; m10 m11 m12]` draws everything where
    /// the identity would, to within what a display can show.
    public static boolean isNearIdentity(double m00, double m10, double m01, double m11, double m02, double m12) {
        return within(m00 - 1.0, LINEAR_EPSILON) && within(m11 - 1.0, LINEAR_EPSILON)
                && within(m10, LINEAR_EPSILON) && within(m01, LINEAR_EPSILON)
                && within(m02, TRANSLATION_EPSILON) && within(m12, TRANSLATION_EPSILON);
    }

    // Written so that NaN fails: every comparison with NaN is false.
    private static boolean within(double difference, double epsilon) {
        return difference <= epsilon && difference >= -epsilon;
    }
}
