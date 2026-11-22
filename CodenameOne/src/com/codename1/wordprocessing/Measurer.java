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
package com.codename1.wordprocessing;

/// Measures text for `DocumentLayout`, in points.
///
/// Pagination depends on every width, so the screen and a PDF only show the same pages when
/// both are laid out with the same measurer. `DocumentView` uses `ScreenMeasurer` by default
/// and the PDF export `PdfMeasurer`.
public interface Measurer {
    /// The advance width of text drawn in a style.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text, never containing a line break
    ///
    /// - `style`: the style
    ///
    /// #### Returns
    ///
    /// the width in points
    float width(String text, TextStyle style);

    /// The distance from the top of a line of this style to its baseline.
    ///
    /// #### Parameters
    ///
    /// - `style`: the style
    ///
    /// #### Returns
    ///
    /// the ascent in points
    float ascent(TextStyle style);

    /// The distance from the baseline to the bottom of a line of this style.
    ///
    /// #### Parameters
    ///
    /// - `style`: the style
    ///
    /// #### Returns
    ///
    /// the descent in points, positive
    float descent(TextStyle style);
}
