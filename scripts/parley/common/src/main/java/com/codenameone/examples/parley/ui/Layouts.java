/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley.ui;

import com.codename1.components.SpanLabel;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.HashMap;
import java.util.Map;

/// How much room there is, and whose look the app wears.
///
/// The app is laid out for a phone. Two things change that, and both are
/// asked here so that no screen works them out for itself:
///
/// - [#wide()]: the window is as wide as a tablet on its side, a desktop
///   window or a browser. The admin console then shows its navigation and a
///   list with its detail side by side; every other screen keeps its column
///   and stops it growing past [#column()].
/// - [#desktopLook()]: the platform's own desktop theme is installed -- macOS
///   Aqua, Windows Fluent or GNOME Adwaita, chosen by the `nativeTheme=native`
///   build hint. The admin console then dresses its controls in that theme's
///   buttons, fields and lists with [#dress(Container)], and keeps the app's
///   own styles only for what a desktop has no control for: the charts and
///   the numbers.
///
/// Both are read when a screen is built. A window that is resized across the
/// line, or a device that is turned, rebuilds the screen: see
/// [#onChange(Form, Runnable)].
public final class Layouts {
    /// A window at least this wide, in millimetres, is laid out side by side.
    /// A phone on its side is narrower; a tablet on its side is not.
    private static final float WIDE_MM = 165f;
    /// The widest a column of text and controls is left to grow.
    private static final float COLUMN_MM = 150f;

    /// The app's own style names, and the name each goes by under a desktop
    /// theme. Those are declared at the end of `theme.css`, each deriving from
    /// the native control it stands for.
    private static final Map<String, String> DESKTOP = new HashMap<String, String>();

    static {
        String[] dressed = {"SvForm", "SvPage", "SvList", "SvPrimary", "SvSecondary", "SvDanger",
            "SvLink", "SvField", "SvFieldHint", "SvRow", "SvRowPicked", "SvRowIcon", "SvRowTitle",
            "SvRowDetail", "SvRowValue", "SvSection", "SvHeading", "SvText", "SvMuted", "SvLabel",
            "SvTitle", "SvError", "SvNotice", "SvPanel", "SvFact", "SvFactLabel", "SvFactValue",
            "SvFactTotal", "SvSegments", "SvSegment", "SvTile", "SvStat", "SvStatValue",
            "SvStatLabel", "SvChartValue", "SvChartAxis", "SvAmount", "SvBannerIcon",
            "SvBannerTitle", "SvBannerText", "SvAlert", "SvSide", "SvSideItem", "SvSidePicked",
            "SvSideLabel", "SvSideName", "SvSideDetail", "SvPaneBar", "SvPaneTitle", "SvMaster",
            "SvBarButton", "SvHomeBar"};
        for (int iter = 0; iter < dressed.length; iter++) {
            DESKTOP.put(dressed[iter], "SvDesk" + dressed[iter].substring(2));
        }
    }

    private Layouts() {
    }

    /// Whether there is room to lay a screen out side by side.
    public static boolean wide() {
        return CN.getDisplayWidth() >= CN.convertToPixels(WIDE_MM);
    }

    /// The widest a single column of content is drawn, in pixels.
    public static int column() {
        return CN.convertToPixels(COLUMN_MM);
    }

    /// How many columns of `millimetres` each fit in `width` pixels: at least
    /// one, and no more than `most`.
    public static int columns(int width, float millimetres, int most) {
        int fit = width / Math.max(1, CN.convertToPixels(millimetres));
        return Math.max(1, Math.min(most, fit));
    }

    /// Whether the platform's desktop theme is the one under the app's own.
    ///
    /// The three desktop themes are the only ones installed on a desktop that
    /// mark themselves as a platform's defaults; a desktop build that kept the
    /// legacy theme, a phone and a tablet all answer false and keep the app's
    /// look everywhere.
    public static boolean desktopLook() {
        return CN.isDesktop()
                && UIManager.getInstance().isThemeConstant("nativeThemeDefaultsBool", false);
    }

    /// Dresses everything under `root` in the desktop theme, when there is one:
    /// each component styled by the app takes the twin of its style that
    /// derives from the platform's control, and whatever scrolls shows the
    /// platform's scrollbar. Does nothing anywhere else.
    ///
    /// It is done to a finished tree rather than where each piece is made, so
    /// that the screens are written once, for a phone, and the console alone
    /// decides what a desktop makes of them.
    public static void dress(Container root) {
        if (!desktopLook()) {
            return;
        }
        dressed(root);
    }

    private static void dressed(Component component) {
        String twin = DESKTOP.get(component.getUIID());
        if (twin != null) {
            component.setUIID(twin);
            if (component instanceof Label) {
                redraw((Label) component);
            }
        }
        if (component instanceof SpanLabel) {
            SpanLabel span = (SpanLabel) component;
            String text = DESKTOP.get(span.getTextUIID());
            if (text != null) {
                span.setTextUIID(text);
            }
            return;
        }
        if (component instanceof Container) {
            Container holder = (Container) component;
            if (holder.isScrollableY()) {
                holder.setScrollVisible(true);
            }
            // A row that is one button is still a container of labels.
            for (int iter = 0; iter < holder.getComponentCount(); iter++) {
                dressed(holder.getComponentAt(iter));
            }
        }
    }

    /// Draws the icon of `label` again in the colours of the style it now has:
    /// an icon is an image, made once from the style the label had then.
    private static void redraw(Label label) {
        if (!(label.getIcon() instanceof FontImage)) {
            return;
        }
        FontImage icon = (FontImage) label.getIcon();
        String glyph = icon.getText();
        float pixels = icon.getFont() == null ? 0 : icon.getFont().getPixelSize();
        if (glyph == null || glyph.length() != 1 || pixels <= 0) {
            return;
        }
        FontImage.setMaterialIcon(label, glyph.charAt(0),
                pixels * 100f / CN.convertToPixels(100f));
    }

    /// Keeps the content of `page` in a column no wider than [#column()], in
    /// the middle of however much room there is: called by the page as it is
    /// laid out, with the padding its style gave it.
    ///
    /// @param margin the side padding of the page's own style, in pixels
    static void centre(Container page, int margin) {
        int side = Math.max(margin, (page.getWidth() - column()) / 2);
        Style style = page.getAllStyles();
        Style now = page.getUnselectedStyle();
        if (now.getPaddingLeftNoRTL() != side || now.getPaddingRightNoRTL() != side) {
            style.setPaddingUnitLeft(Style.UNIT_TYPE_PIXELS);
            style.setPaddingUnitRight(Style.UNIT_TYPE_PIXELS);
            style.setPaddingLeft(side);
            style.setPaddingRight(side);
        }
    }

    /// Calls `rebuild` when `form` stops being wide or starts to be, which is
    /// a window dragged across the line or a tablet turned. The screen was
    /// built for the room there was; it is built again for the room there is.
    public static void onChange(final Form form, final Runnable rebuild) {
        final boolean[] was = {wide()};
        form.addSizeChangedListener(e -> {
            boolean now = wide();
            if (now != was[0]) {
                was[0] = now;
                // After the event: the form is in the middle of being resized.
                CN.callSerially(rebuild);
            }
        });
    }
}
