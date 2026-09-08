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
            new FigureVariant(new L10nBasicFigure(), FigureDevice.ANDROID, false, "l10n-basic.png"),
            new FigureVariant(new ComponentsLabelTextPositionFigure(), FigureDevice.ANDROID, false, "components-label-text-position.png"),
            new FigureVariant(new ComponentsLinkButtonFigure(), FigureDevice.ANDROID, false, "components-link-button.png"),
            new FigureVariant(new ComponentsToggleButtonsFigure(), FigureDevice.ANDROID, false, "components-toggle-buttons.png"),
            // ComponentGroup does nothing unless the theme sets ComponentGroupBool, which
            // the iOS theme does by default and Android Material does not -- rendered on
            // Android the sample comes out as plain separate rows, which is the opposite
            // of what the section demonstrates.
            new FigureVariant(new ComponentsToggleButtonsComponentGroupFigure(), FigureDevice.IOS, false, "components-toggle-buttons-component-group.png"),
            new FigureVariant(new ComponentsSpanbuttonFigure(), FigureDevice.ANDROID, false, "components-spanbutton.png"),
            new FigureVariant(new ComponentsTableFigure(), FigureDevice.ANDROID, false, "components-table.png"),
            new FigureVariant(new ComponentsTableWithSpanningFigure(), FigureDevice.ANDROID, false, "components-table-with-spanning.png"),
            new FigureVariant(new ComponentsTablePinstripeFigure(), FigureDevice.ANDROID, false, "components-table-pinstripe.png"),
            new FigureVariant(new ComponentsSharebuttonFigure(), FigureDevice.ANDROID, false, "components-sharebutton.png"),
            new FigureVariant(new ComponentsToolbarFigure(), FigureDevice.ANDROID, false, "components-toolbar.png"),
            new FigureVariant(new ComponentsSignature2Figure(), FigureDevice.ANDROID, false, "components-signature2.png"),
            new FigureVariant(new ComponentsFloatinghintFigure(), FigureDevice.ANDROID, false, "components-floatinghint.png"),
            new FigureVariant(new BadgeFloatingButtonFigure(), FigureDevice.ANDROID, false, "badge-floating-button.png"),
            new FigureVariant(new GraphicsHiworldFigure(), FigureDevice.IOS, false, "graphics-hiworld.png"),
            new FigureVariant(new GraphicsGlasspaneFigure(), FigureDevice.ANDROID, false, "graphics-glasspane.png"),
            new FigureVariant(new GraphicsFontimageFixedFigure(), FigureDevice.ANDROID, false, "graphics-fontimage-fixed.png"),
            new FigureVariant(new GraphicsFontimageStyleFigure(), FigureDevice.ANDROID, false, "graphics-fontimage-style.png"),
            new FigureVariant(new GraphicsFontimageMaterialFigure(), FigureDevice.ANDROID, false, "graphics-fontimage-material.png"),
            new FigureVariant(new CsvParsingFigure(), FigureDevice.ANDROID, false, "csv-parsing.png"),
        };
    }
}
