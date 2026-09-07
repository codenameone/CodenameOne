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

/// The figures this repository generates, and the file each one lands in.
///
/// This list is the manifest. The workflow compares the generated directory
/// against `docs/developer-guide/img` without being told how many files to
/// expect, so adding an entry here and referencing the image from a chapter is
/// the whole of adding a figure.
public final class GuideFigures {
    private GuideFigures() {
    }

    public static FigureVariant[] variants() {
        GuideFigure button = new ButtonFigure();
        GuideFigure appearance = new AppearanceFigure();
        return new FigureVariant[] {
            // The Components chapter shows the platforms side by side.
            new FigureVariant(button, FigureDevice.IOS, false, "components-button-ios.png"),
            new FigureVariant(button, FigureDevice.ANDROID, false, "components-button-android.png"),
            // The native themes chapter shows one platform in both appearances.
            new FigureVariant(appearance, FigureDevice.ANDROID, false, "native-themes-appearance-light.png"),
            new FigureVariant(appearance, FigureDevice.ANDROID, true, "native-themes-appearance-dark.png"),
            // One canonical variant where the component, not the platform, is
            // the subject -- and Android Material for it, because these render
            // through the port's bundled Roboto, which is what Material 3
            // actually specifies. An iOS Modern render is typographically wrong
            // for the same reason: it comes out in Roboto rather than SF, so it
            // is worth having only where the platform difference is the point.
            new FigureVariant(new CheckBoxFigure(), FigureDevice.ANDROID, false,
                    "components-radiobutton-checkbox.png"),
            new FigureVariant(new MultiButtonFigure(), FigureDevice.ANDROID, false,
                    "components-multibutton.png"),
            new FigureVariant(new SpanLabelFigure(), FigureDevice.ANDROID, false,
                    "components-spanlabel.png"),
        };
    }
}
