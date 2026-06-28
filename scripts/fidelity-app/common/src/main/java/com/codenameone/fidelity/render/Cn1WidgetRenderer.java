/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codenameone.fidelity.render;

import com.codename1.components.FloatingActionButton;
import com.codename1.components.Switch;
import com.codename1.ui.Button;
import com.codename1.ui.FontImage;
import com.codename1.ui.Image;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.Slider;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codenameone.fidelity.spec.ComponentSpec;

/**
 * Builds the Codename One component for a spec + state, applying the native theme
 * UIID and the requested visual state. Returns null for component kinds not yet
 * supported (containers like Tabs/Toolbar/Dialog are handled separately), so the
 * runner can skip them cleanly.
 */
public final class Cn1WidgetRenderer {
    private Cn1WidgetRenderer() {
    }

    /** Returns true when this renderer knows how to build the given component id. */
    public static boolean isSupported(String id) {
        return "Button".equals(id) || "RaisedButton".equals(id) || "FlatButton".equals(id)
                || "TextField".equals(id) || "CheckBox".equals(id) || "RadioButton".equals(id)
                || "Switch".equals(id) || "Slider".equals(id) || "ProgressBar".equals(id)
                || "FloatingActionButton".equals(id) || "Tabs".equals(id) || "Toolbar".equals(id)
                || "Dialog".equals(id) || "Spinner".equals(id)
                || "TabsGeom".equals(id)                       // geometry-isolation: Tabs over a flat backdrop
                || "TabOne".equals(id)                         // minimal: one text-only tab, flat backdrop
                || (id != null && id.startsWith("GlassPanel")); // glass-blend isolation panels
    }

    /**
     * Builds the CN1 component, applies UIID + state. The caller is responsible
     * for sizing/placing it in a fixed tile and capturing it.
     */
    public static Component build(ComponentSpec spec, String state) {
        return build(spec, state, "light");
    }

    public static Component build(ComponentSpec spec, String state, String appearance) {
        String id = spec.getId();
        String uiid = spec.getCn1Uiid();
        boolean dark = "dark".equals(appearance);
        String text = spec.getText() != null ? spec.getText() : "";
        Component c;
        if ("Button".equals(id) || "RaisedButton".equals(id) || "FlatButton".equals(id)) {
            Button b = new Button(text);
            b.setUIID(uiid);
            applyButtonState(b, state);
            // iOS 26 prominentGlass (RaisedButton) is a translucent fill -- the
            // backdrop shows faintly through the blue. Drop the fill alpha a touch
            // so the CN1 raised button reads as glass rather than a flat opaque blue.
            if ("RaisedButton".equals(id)) {
                b.getAllStyles().setBgTransparency(225);
            }
            c = b;
        } else if ("TextField".equals(id)) {
            TextField tf = new TextField(text);
            tf.setUIID(uiid);
            tf.setEditable(false);
            // Size to the actual text content. getTextAreaSize() reserves
            // columns*widestChar ('m') which overshoots the rendered string and
            // made the field box ~10px wider than the native content-sized field.
            // columns=1 lets stringWidth(text) drive the width so the box matches.
            tf.setColumns(1);
            tf.setGrowByContent(true);
            if ("disabled".equals(state)) {
                tf.setEnabled(false);
            }
            c = tf;
        } else if ("CheckBox".equals(id)) {
            // iOS has no native checkbox; the native reference is a glyph only (no
            // label). Drop the label on iOS so we compare box-against-box rather
            // than penalising CN1 for a label the glyph-only reference omits.
            boolean iosGlyph = "ios".equals(com.codename1.ui.Display.getInstance().getPlatformName());
            CheckBox cb = new CheckBox(iosGlyph ? "" : text);
            cb.setUIID(uiid);
            if ("selected".equals(state)) {
                cb.setSelected(true);
            } else if ("disabled".equals(state)) {
                cb.setEnabled(false);
            }
            c = cb;
        } else if ("RadioButton".equals(id)) {
            boolean iosGlyph = "ios".equals(com.codename1.ui.Display.getInstance().getPlatformName());
            RadioButton rb = new RadioButton(iosGlyph ? "" : text);
            rb.setUIID(uiid);
            if ("selected".equals(state)) {
                rb.setSelected(true);
            } else if ("disabled".equals(state)) {
                rb.setEnabled(false);
            }
            c = rb;
        } else if ("Switch".equals(id)) {
            Switch sw = new Switch();
            sw.setUIID(uiid);
            if ("selected".equals(state)) {
                sw.setValue(true);
            }
            if ("disabled".equals(state)) {
                sw.setEnabled(false);
            }
            c = sw;
        } else if ("Slider".equals(id) || "ProgressBar".equals(id)) {
            Slider s = new Slider();
            s.setUIID(uiid);
            // An editable slider draws a thumb (matching Material's slider); a
            // progress bar has no thumb and is rendered thin by the runner.
            s.setEditable("Slider".equals(id));
            s.setMinValue(0);
            s.setMaxValue(100);
            s.setProgress(50);
            if ("disabled".equals(state)) {
                s.setEnabled(false);
            }
            c = s;
        } else if ("FloatingActionButton".equals(id)) {
            // Material FAB: circular accent button with a "+" glyph.
            FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
            fab.setUIID(uiid);
            if ("disabled".equals(state)) {
                fab.setEnabled(false);
            } else {
                applyButtonState(fab, state);
            }
            // The native FAB golden is anchored at the tile's top-left corner with
            // no app-margin; the FAB's 3mm float-from-edge margin is app layout, not
            // widget fidelity, so zero it here to compare widget-against-widget.
            fab.getAllStyles().setMargin(0, 0, 0, 0);
            // The Android off-screen golden (View rasterized via renderViewOnBitmap)
            // does NOT capture the FAB's elevation shadow, whereas CN1's RoundRectBorder
            // reserves shadow space (shadowSpread + blur) that insets the rounded-square
            // body ~1.5mm from the bounds. To compare the widget body apples-to-apples,
            // give the FAB a flat, shadowless rounded-square border for the test so its
            // body fills the bounds at the corner, matching the shadowless native ref.
            float fabRadius = 2.4f;
            try {
                fabRadius = Float.parseFloat(com.codename1.ui.plaf.UIManager.getInstance()
                        .getThemeConstant("fabCornerRadiusMM", "2.4"));
            } catch (Throwable ignore) {
            }
            com.codename1.ui.plaf.RoundRectBorder flat = com.codename1.ui.plaf.RoundRectBorder.create()
                    .cornerRadius(fabRadius).shadowOpacity(0).shadowSpread(0);
            fab.getUnselectedStyle().setBorder(flat);
            fab.getSelectedStyle().setBorder(flat);
            fab.getPressedStyle().setBorder(flat);
            c = fab;
        } else if (id != null && id.startsWith("GlassPanel")) {
            // Glass-blend isolation: a plain rounded glass panel (no text/items) so
            // only the GlassPanel UIID's translucent tint + backdrop-filter:blur is
            // compared, across four different backdrops (grey/red/gradient/photo).
            // The container fills the tile (minus its 1mm theme/runner margin).
            Container panel = new Container(new BorderLayout());
            panel.setUIID("GlassPanel");
            c = panel;
        } else if ("TabOne".equals(id)) {
            // Minimal isolation case: a tab bar with ONE text-only tab (no icon,
            // no second/unselected item). Strips away the SF-vs-Material icon
            // mismatch and multi-tab spacing so only the glass pill geometry, the
            // single centred text label and the (flat-backdrop) glass tint remain
            // -- the smallest reproduction we can drive to ~100%.
            Tabs tabs = new Tabs(Component.TOP);
            tabs.addTab("Tab", new Container());
            c = tabs;
        } else if ("Tabs".equals(id) || "TabsGeom".equals(id)) {
            // iOS UITabBar: an icon-over-label bar at the TOP, three items
            // (Featured / Search / More) mirroring the native reference's system
            // tab items; the first is selected (blue), the rest grey. NOT a
            // Material pill strip.
            Tabs tabs = new Tabs(Component.TOP);
            // The material icons don't auto-tint to the tab's fg, so build them with
            // explicit colours: the selected item (Featured) is blue, the rest grey,
            // matching the native UITabBar tint.
            // The glass pill mutes the selected tint, so start from a more vivid blue.
            int selColor = dark ? 0x409cff : 0x0a84ff;
            int unselColor = dark ? 0xebebf5 : 0x3c3c43;
            com.codename1.ui.plaf.Style selS = new com.codename1.ui.plaf.Style();
            selS.setFgColor(selColor);
            selS.setBgTransparency(0);
            com.codename1.ui.plaf.Style unS = new com.codename1.ui.plaf.Style();
            unS.setFgColor(unselColor);
            unS.setBgTransparency(0);
            // Bigger icons (the native tab item is icon-dominant); the label font is
            // cut in the theme so the overall item roughly doubles toward native size.
            Image star = FontImage.createSFOrMaterial(FontImage.MATERIAL_STAR, selS, 4.1f);
            Image search = FontImage.createSFOrMaterial(FontImage.MATERIAL_SEARCH, unS, 4.1f);
            Image more = FontImage.createSFOrMaterial(FontImage.MATERIAL_MORE_HORIZ, unS, 4.1f);
            tabs.addTab("Featured", star, star, new Container());
            tabs.addTab("Search", search, search, new Container());
            tabs.addTab("More", more, more, new Container());
            tabs.setTabTextPosition(Component.BOTTOM);
            c = tabs;
        } else if ("Toolbar".equals(id)) {
            // Material small top app bar: title on the bar. The CN1 Toolbar
            // component requires a Form (setToolBar), so for the standalone tile
            // we mirror its appearance with a Toolbar-styled bar + a Title label.
            Container bar = new Container(new BorderLayout());
            bar.setUIID("Toolbar");
            Label title = new Label(text);
            title.setUIID("Title");
            if ("ios".equals(com.codename1.ui.Display.getInstance().getPlatformName())) {
                // A representative iOS navigation bar: a leading back command, a
                // centred title and a trailing action -- the bar button items are a
                // defining part of the look. The native UINavigationBar lays its
                // content row at the TOP of the bar (the bar is taller than the row),
                // so anchor the row NORTH and let the bar background fill the tile.
                title.getAllStyles().setAlignment(Component.CENTER);
                // The iOS 26 glass nav bar is translucent: the backdrop shows
                // through, washed toward the bar's base colour. CN1 cannot blur
                // (CEF-free), but a translucent bar over the shared backdrop
                // approximates it. The light bar washes heavily toward white (232);
                // the dark bar keeps far more of the backdrop's colour (the native
                // dark glass barely lightens it), so it stays much more translucent.
                // iOS 26 glass bar is very translucent -- the colourful backdrop reads
                // through at near-full saturation, especially in dark mode (the dark
                // glass barely darkens it). Wash only lightly.
                // The iOS 26 glass nav bar is VERY translucent -- the colourful backdrop
                // reads through at high saturation; an opaque-ish wash (175) read as a
                // near-white bar that didn't match the native glass at all. Light glass
                // washes only lightly toward white (~90/255); dark glass barely darkens.
                // Native nav bar adds NO light tint (the previous white wash was wrong):
                // light mode is just the blurred backdrop. The native DARK glass darkens
                // the backdrop a touch, so dark keeps a very light black frost; light is
                // fully transparent. The blur hook runs regardless of opacity.
                bar.getAllStyles().setBgTransparency(dark ? 16 : 0);
                Container row = new Container(new BorderLayout());
                row.setUIID("Container");
                row.getAllStyles().setBgTransparency(0);
                // iOS 26 bar items are ICON-ONLY inside circular translucent-glass
                // buttons. The glyph matches the TITLE colour (black in light, white
                // in dark) -- NOT blue.
                int tint = dark ? 0xffffff : 0x000000;
                com.codename1.ui.plaf.Style tintS = new com.codename1.ui.plaf.Style();
                tintS.setFgColor(tint);
                tintS.setBgTransparency(0);
                Button back = new Button("");
                back.setUIID("BackCommand");
                back.setIcon(FontImage.createMaterial(FontImage.MATERIAL_ARROW_BACK_IOS_NEW, tintS, 3.2f));
                Button action = new Button("");
                action.setUIID("TitleCommand");
                action.setIcon(FontImage.createMaterial(FontImage.MATERIAL_ADD, tintS, 3.6f));
                row.add(BorderLayout.WEST, back);
                row.add(BorderLayout.CENTER, title);
                row.add(BorderLayout.EAST, action);
                bar.add(BorderLayout.NORTH, row);
            } else {
                bar.add(BorderLayout.WEST, title);
            }
            c = bar;
        } else if ("Dialog".equals(id)) {
            // iOS alert: a rounded card with a centred title + supporting text in
            // the middle and a hairline-separated row of two equal blue actions
            // pinned to the bottom (Cancel | OK, split by a vertical divider).
            // iOS alerts centre the title/body and split two equal actions with a
            // hairline divider; Android Material dialogs left-align the title/body
            // and right-align a flow of text actions. Pick per platform.
            boolean iosDlg = "ios".equals(com.codename1.ui.Display.getInstance().getPlatformName());
            int dlgAlign = iosDlg ? Component.CENTER : Component.LEFT;
            Container dialog = new Container(new BorderLayout());
            dialog.setUIID("Dialog");
            Label title = new Label("Title");
            title.setUIID("DialogTitle");
            title.getAllStyles().setAlignment(dlgAlign);
            Label body = new Label(text);
            body.setUIID("DialogBody");
            body.getAllStyles().setAlignment(dlgAlign);
            Container content = new Container(BoxLayout.y());
            content.getAllStyles().setBgTransparency(0);
            content.add(title);
            content.add(body);
            Button cancel = new Button("Cancel");
            cancel.setUIID("DialogButton");
            Button ok = new Button("OK");
            ok.setUIID("DialogButton");
            Container btns = iosDlg
                    ? new Container(new GridLayout(1, 2))
                    : new Container(new com.codename1.ui.layouts.FlowLayout(Component.RIGHT));
            btns.setUIID("DialogCommandArea");
            btns.add(cancel);
            btns.add(ok);
            dialog.add(BorderLayout.CENTER, content);
            dialog.add(BorderLayout.SOUTH, btns);
            c = dialog;
        } else if ("Spinner".equals(id)) {
            // iOS picker wheel: a single-column spinner showing several rows with the
            // middle one selected, the curved perspective fade and the glass selection
            // band -- matching a native UIPickerView. The wheel rows/overlay are styled
            // by the SpinnerRenderer / SpinnerOverlay UIIDs in the theme.
            com.codename1.ui.spinner.GenericSpinner spinner = new com.codename1.ui.spinner.GenericSpinner();
            com.codename1.ui.list.DefaultListModel model = new com.codename1.ui.list.DefaultListModel(
                    new Object[]{"Value 1", "Value 2", "Value 3", "Value 4", "Value 5"});
            spinner.setModel(model);
            spinner.setRenderingPrototype("Value 0");
            spinner.setValue("Value 3");
            c = spinner;
        } else {
            return null;
        }
        return c;
    }

    private static void applyButtonState(Button b, String state) {
        if ("disabled".equals(state)) {
            b.setEnabled(false);
        } else if ("pressed".equals(state)) {
            // Force the pressed visual state so the pressed style is painted.
            b.pressed();
        }
    }
}
