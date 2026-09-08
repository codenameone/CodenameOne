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
            new FigureVariant(new L10nManagerFigure(), FigureDevice.ANDROID, false, "l10n-manager.png"),
            new FigureVariant(new CapturePhotoFigure(), FigureDevice.ANDROID, false, "capture-photo.png"),
            new FigureVariant(new EditorsCodeFigure(), FigureDevice.ANDROID, false, "editors-code.png"),
            new FigureVariant(new ComponentsDialogTintFigure(), FigureDevice.ANDROID, false, "components-dialog-tint.png"),
            new FigureVariant(new ComponentsDialogGreenTintFigure(), FigureDevice.ANDROID, false, "components-dialog-green-tint.png"),
            new FigureVariant(new ComponentsDialogBlurFigure(), FigureDevice.ANDROID, false, "components-dialog-blur.png"),
            new FigureVariant(new ComponentsDialogPopupFigure(), FigureDevice.ANDROID, false, "components-dialog-popup.png"),
            new FigureVariant(new ComponentsLabelTextPositionFigure(), FigureDevice.ANDROID, false, "components-label-text-position.png"),
            new FigureVariant(new AutosizeFigure(), FigureDevice.ANDROID, false, "autosize.png"),
            new FigureVariant(new PixelPerfectTextFieldPickerIosFigure(), FigureDevice.ANDROID, false, "pixel-perfect-text-field-picker-ios.png"),
            new FigureVariant(new PixelPerfectTextFieldPickerAndroidFigure(), FigureDevice.ANDROID, false, "pixel-perfect-text-field-picker-android.png"),
            new FigureVariant(new ComponentsLinkButtonFigure(), FigureDevice.ANDROID, false, "components-link-button.png"),
            new FigureVariant(new ComponentsToggleButtonsFigure(), FigureDevice.ANDROID, false, "components-toggle-buttons.png"),
            new FigureVariant(new ComponentsToggleButtonsComponentGroupFigure(), FigureDevice.ANDROID, false, "components-toggle-buttons-component-group.png"),
            new FigureVariant(new ComponentsSpanbuttonFigure(), FigureDevice.ANDROID, false, "components-spanbutton.png"),
            new FigureVariant(new ComponentsTableFigure(), FigureDevice.ANDROID, false, "components-table.png"),
            new FigureVariant(new ComponentsTableWithSpanningFigure(), FigureDevice.ANDROID, false, "components-table-with-spanning.png"),
            new FigureVariant(new ComponentsTablePinstripeFigure(), FigureDevice.ANDROID, false, "components-table-pinstripe.png"),
            new FigureVariant(new ComponentsTablePinstripeEditFigure(), FigureDevice.ANDROID, false, "components-table-pinstripe-edit.png"),
            new FigureVariant(new ComponentsTableMultilinePortraitFigure(), FigureDevice.ANDROID, false, "components-table-multiline-portrait.png"),
            new FigureVariant(new ComponentsTableMultilineLandscapeFigure(), FigureDevice.ANDROID, false, "components-table-multiline-landscape.png"),
            new FigureVariant(new TreeFigure(), FigureDevice.ANDROID, false, "tree.png"),
            new FigureVariant(new ComponentsSharebuttonFigure(), FigureDevice.ANDROID, false, "components-sharebutton.png"),
            new FigureVariant(new ComponentsSharebuttonAndroidFigure(), FigureDevice.ANDROID, false, "components-sharebutton-android.png"),
            new FigureVariant(new ComponentsImageviewerFigure(), FigureDevice.ANDROID, false, "components-imageviewer.png"),
            new FigureVariant(new ComponentsImageviewerZoomedInFigure(), FigureDevice.ANDROID, false, "components-imageviewer-zoomed-in.png"),
            new FigureVariant(new ComponentsImageviewerMultiFigure(), FigureDevice.ANDROID, false, "components-imageviewer-multi.png"),
            new FigureVariant(new ComponentsImageviewerDynamicFigure(), FigureDevice.ANDROID, false, "components-imageviewer-dynamic.png"),
            new FigureVariant(new ComponentsScaleimageFigure(), FigureDevice.ANDROID, false, "components-scaleimage.png"),
            new FigureVariant(new ComponentsToolbarFigure(), FigureDevice.ANDROID, false, "components-toolbar.png"),
            new FigureVariant(new ComponentsToolbarSidemenuFigure(), FigureDevice.ANDROID, false, "components-toolbar-sidemenu.png"),
            new FigureVariant(new ComponentsToolbarOverflowMenuFigure(), FigureDevice.ANDROID, false, "components-toolbar-overflow-menu.png"),
            new FigureVariant(new ComponentsToolbarAnimation1Figure(), FigureDevice.ANDROID, false, "components-toolbar-animation-1.png"),
            new FigureVariant(new ComponentsToolbarAnimation2Figure(), FigureDevice.ANDROID, false, "components-toolbar-animation-2.png"),
            new FigureVariant(new ComponentsToolbarAnimation3Figure(), FigureDevice.ANDROID, false, "components-toolbar-animation-3.png"),
            new FigureVariant(new ComponentsBrowsercomponentFigure(), FigureDevice.ANDROID, false, "components-browsercomponent.png"),
            new FigureVariant(new ComponentsBrowsercomponentCallbackBeforeFigure(), FigureDevice.ANDROID, false, "components-browsercomponent-callback-before.png"),
            new FigureVariant(new ComponentsBrowsercomponentCallbackAfterFigure(), FigureDevice.ANDROID, false, "components-browsercomponent-callback-after.png"),
            new FigureVariant(new ComponentsBrowsercomponentJavascriptFigure(), FigureDevice.ANDROID, false, "components-browsercomponent-javascript.png"),
            new FigureVariant(new ComponentsBrowsercomponentContextFigure(), FigureDevice.ANDROID, false, "components-browsercomponent-context.png"),
            new FigureVariant(new ComponentsAutocompleteFigure(), FigureDevice.ANDROID, false, "components-autocomplete.png"),
            new FigureVariant(new AutoCompleteWithPicturesFigure(), FigureDevice.ANDROID, false, "auto-complete-with-pictures.png"),
            new FigureVariant(new ComponentsPickerFigure(), FigureDevice.ANDROID, false, "components-picker.png"),
            new FigureVariant(new ComponentsPickerDateTimeOnSimulatorFigure(), FigureDevice.ANDROID, false, "components-picker-date-time-on-simulator.png"),
            new FigureVariant(new ComponentsPickerDateAndroidFigure(), FigureDevice.ANDROID, false, "components-picker-date-android.png"),
            new FigureVariant(new ComponentsCalendarFigure(), FigureDevice.ANDROID, false, "components-calendar.png"),
            new FigureVariant(new ComponentsSignature2Figure(), FigureDevice.ANDROID, false, "components-signature2.png"),
            new FigureVariant(new ComponentsAccordionFigure(), FigureDevice.ANDROID, false, "components-accordion.png"),
            new FigureVariant(new ComponentsFloatinghintFigure(), FigureDevice.ANDROID, false, "components-floatinghint.png"),
            new FigureVariant(new BadgeFloatingButtonFigure(), FigureDevice.ANDROID, false, "badge-floating-button.png"),
            new FigureVariant(new JavascriptPwaAddAppBannerFigure(), FigureDevice.ANDROID, false, "javascript-pwa-add-app-banner.png"),
            new FigureVariant(new PixelPerfectTextFieldReasonableOnIosFigure(), FigureDevice.ANDROID, false, "pixel-perfect-text-field-reasonable-on-ios.png"),
            new FigureVariant(new PixelPerfectTextFieldAndroidCodenameoneFontFigure(), FigureDevice.ANDROID, false, "pixel-perfect-text-field-android-codenameone-font.png"),
            new FigureVariant(new GraphicsHiworldFigure(), FigureDevice.ANDROID, false, "graphics-hiworld.png"),
            new FigureVariant(new GraphicsGlasspaneFigure(), FigureDevice.ANDROID, false, "graphics-glasspane.png"),
            new FigureVariant(new GraphicsFontimageFixedFigure(), FigureDevice.ANDROID, false, "graphics-fontimage-fixed.png"),
            new FigureVariant(new GraphicsFontimageStyleFigure(), FigureDevice.ANDROID, false, "graphics-fontimage-style.png"),
            new FigureVariant(new GraphicsFontimageMaterialFigure(), FigureDevice.ANDROID, false, "graphics-fontimage-material.png"),
            new FigureVariant(new FilesystemTreeFigure(), FigureDevice.ANDROID, false, "filesystem-tree.png"),
            new FigureVariant(new SqlTableFigure(), FigureDevice.ANDROID, false, "sql-table.png"),
            new FigureVariant(new SqlEntryFigure(), FigureDevice.ANDROID, false, "sql-entry.png"),
            new FigureVariant(new CsvParsingFigure(), FigureDevice.ANDROID, false, "csv-parsing.png"),
            new FigureVariant(new NetworkSliderbridgeFigure(), FigureDevice.ANDROID, false, "network-sliderbridge.png"),
        };
    }
}
