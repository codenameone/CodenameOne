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

/// One figure rendered on one device in one appearance, and the file it lands in.
///
/// Variants are declared rather than generated combinatorially, because every
/// generated image has to be referenced by a chapter -- `find_unused_images.py`
/// fails the build for one that is not -- so producing a whole matrix would
/// break the build for the pictures no chapter had asked for yet.
public final class FigureVariant {
    private final GuideFigure figure;
    private final FigureDevice device;
    private final boolean darkMode;
    private final String fileName;

    public FigureVariant(GuideFigure figure, FigureDevice device, boolean darkMode, String fileName) {
        this.figure = figure;
        this.device = device;
        this.darkMode = darkMode;
        this.fileName = fileName;
    }

    public GuideFigure figure() {
        return figure;
    }

    public FigureDevice device() {
        return device;
    }

    public boolean darkMode() {
        return darkMode;
    }

    /// File name written into the output directory, including the extension.
    public String fileName() {
        return fileName;
    }
}
