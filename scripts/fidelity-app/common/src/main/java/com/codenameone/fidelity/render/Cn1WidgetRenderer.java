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
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.Slider;
import com.codename1.ui.Tabs;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.UIManager;
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

    /// Desktop rows that build something of their own rather than reusing a mobile branch.
    private static final java.util.Set<String> SECOND_WAVE =
            new java.util.HashSet<String>(java.util.Arrays.asList(
                    "DesktopScrollBar", "DesktopScrollBarHighlight", "DesktopSeparator",
                    "DesktopGroupBox", "DesktopStepper",
                    "DesktopLinkButton", "DesktopSearchField", "DesktopListRow", "DesktopTabs",
                    "DesktopToolbar", "DesktopDisclosure", "DesktopMenuBar", "DesktopMenuItem",
                    "DesktopTooltip"));

    /// Maps a desktop row id onto the component kind that builds it.
    ///
    /// A desktop Button is still a Button; what makes the row different is its UIID, its tile
    /// geometry and the states it is asked for, none of which live here. Mapping at the top
    /// of the dispatch means every existing branch is reached unchanged, rather than being
    /// duplicated with a "Desktop" prefix.
    private static String desktopToMobileId(String id) {
        if (id == null || !id.startsWith("Desktop")) {
            return id;
        }
        String rest = id.substring("Desktop".length());
        // The second-wave rows keep their own ids: each has a branch of its own below, and
        // stripping the prefix would send several of them somewhere wrong. DesktopTabs would
        // land in the iOS Tabs branch, which builds a Liquid Glass floating pill; DesktopToolbar
        // in the one that builds a navigation bar with a title and a back chevron. Neither is
        // the desktop control.
        if (SECOND_WAVE.contains(id)) {
            return id;
        }
        if ("AccentButton".equals(rest)) {
            return "RaisedButton";
        }
        if ("ComboBox".equals(rest)) {
            // No dedicated ComboBox branch; a popup button reads as a Button with a label,
            // which is what the CN1 side has to offer against a native NSPopUpButton /
            // GtkDropDown / WinUI ComboBox. Tracked as an approximation rather than hidden.
            return "Button";
        }
        return rest;
    }

    /// True when this row is one of the desktop rows, i.e. before desktopToMobileId()
    /// rewrote its id onto the mobile kind that builds it.
    private static boolean isDesktopRow(ComponentSpec spec) {
        return spec != null && spec.getId() != null && spec.getId().startsWith("Desktop");
    }

    /** Returns true when this renderer knows how to build the given component id. */
    public static boolean isSupported(String id) {
        return "Button".equals(id) || "RaisedButton".equals(id) || "FlatButton".equals(id)
                || "TextField".equals(id) || "CheckBox".equals(id) || "RadioButton".equals(id)
                || "Switch".equals(id) || "Slider".equals(id) || "ProgressBar".equals(id)
                || "FloatingActionButton".equals(id) || "Tabs".equals(id) || "Toolbar".equals(id)
                || "Dialog".equals(id) || "Spinner".equals(id)
                || "TabsGeom".equals(id)                       // geometry-isolation: Tabs over a flat backdrop
                || "TabsMorph".equals(id)                      // animation-frame validation: same bar, frozen morph
                || "TabsGlassMotion".equals(id)                // iOS 27 motion frames: same bar, frozen at a time
                || "SwitchMorph".equals(id)                    // animation-frame validation: frozen droplet slide
                || "TabOne".equals(id)                         // minimal: one tab with a transparent icon slot
                || "GlassText".equals(id) || "GlassIcon".equals(id) // ladder rungs: glass + one element
                || (id != null && id.startsWith("GlassPanel")) // glass-blend isolation panels
                // The desktop rows. They build the same components as the mobile ones -- a
                // Button is a Button -- and differ in their UIID, their tile geometry and the
                // states they are asked for. Named separately rather than reusing the mobile
                // ids because one screenshot name has to identify one row, and a desktop
                // Button and a mobile Button are different references.
                || "DesktopButton".equals(id) || "DesktopAccentButton".equals(id)
                || "DesktopTextField".equals(id) || "DesktopCheckBox".equals(id)
                || "DesktopRadioButton".equals(id) || "DesktopSwitch".equals(id)
                || "DesktopSlider".equals(id) || "DesktopProgressBar".equals(id)
                || "DesktopComboBox".equals(id)
                // The second wave: the chrome and the controls the first nine did not reach.
                || "DesktopScrollBar".equals(id) || "DesktopScrollBarHighlight".equals(id)
                || "DesktopSeparator".equals(id)
                || "DesktopGroupBox".equals(id) || "DesktopStepper".equals(id)
                || "DesktopLinkButton".equals(id) || "DesktopSearchField".equals(id)
                || "DesktopListRow".equals(id) || "DesktopTabs".equals(id)
                || "DesktopToolbar".equals(id) || "DesktopDisclosure".equals(id)
                || "DesktopMenuBar".equals(id) || "DesktopMenuItem".equals(id)
                || "DesktopTooltip".equals(id);
    }

    /**
     * Builds the CN1 component, applies UIID + state. The caller is responsible
     * for sizing/placing it in a fixed tile and capturing it.
     */
    public static Component build(ComponentSpec spec, String state) {
        return build(spec, state, "light");
    }

    public static Component build(ComponentSpec spec, String state, String appearance) {
        Component built = buildImpl(spec, state, appearance);
        // Applied here rather than inside each branch. Every widget grew its own state
        // handling, so adding hover to the Button path left CheckBox, RadioButton, Switch and
        // Slider silently ignoring it -- the tiles rendered, looked plausible, and were
        // pixel-identical to their normal state. One place means one behaviour.
        if (built != null) {
            applyDesktopState(built, state);
        }
        return built;
    }

    private static Component buildImpl(ComponentSpec spec, String state, String appearance) {
        String id = desktopToMobileId(spec.getId());
        String uiid = spec.getCn1Uiid();
        boolean dark = "dark".equals(appearance);
        String text = spec.getText() != null ? spec.getText() : "";
        Component c;
        if ("Button".equals(id) || "RaisedButton".equals(id) || "FlatButton".equals(id)) {
            Button b = new Button(text);
            b.setUIID(uiid);
            applyButtonState(b, state);
            // The iOS-modern theme gives buttons a form-like margin (so real apps
            // aren't cramped edge-to-edge), but the fidelity tile pins the widget
            // tight against the native reference -- zero the margin here so the
            // theme's app-facing spacing doesn't shift the comparison. The native
            // reference button has no surrounding margin either.
            b.getAllStyles().setMargin(0, 0, 0, 0);
            // iOS 26 prominentGlass (RaisedButton) is a translucent fill -- the
            // backdrop shows faintly through the blue. Drop the fill alpha a touch
            // so the CN1 raised button reads as glass rather than a flat opaque blue.
            //
            // Keyed on the SPEC's own id, not the mapped kind: DesktopAccentButton maps
            // onto RaisedButton to reuse this branch, but all three desktop themes give
            // their accent button an OPAQUE fill, so applying the glass alpha there made
            // the normal/pressed/disabled tiles artificially translucent -- and, because
            // getAllStyles() excludes hover, left hover opaque, inventing a state
            // difference the native controls do not have.
            if ("RaisedButton".equals(id) && !isDesktopRow(spec)) {
                b.getAllStyles().setBgTransparency(225);
            }
            c = b;
        } else if ("TextField".equals(id)) {
            TextField tf = new TextField(text);
            tf.setUIID(uiid);
            tf.setEditable(false);
            // Zero the theme's form margin for the tight tile: the native field
            // fills its tile edge-to-edge, so the inset margin would leave a strip
            // of grouped backdrop around the CN1 cell and misalign the bbox.
            tf.getAllStyles().setMargin(0, 0, 0, 0);
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
            // The theme gives CheckBox a left margin so it aligns with the label
            // column in a real form; zero it here so the isolated glyph tile still
            // overlays the top-left-pinned native reference 1:1.
            cb.getAllStyles().setMargin(0, 0, 0, 0);
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
            // See CheckBox: zero the theme's alignment margin for the tight tile.
            rb.getAllStyles().setMargin(0, 0, 0, 0);
            if ("selected".equals(state)) {
                rb.setSelected(true);
            } else if ("disabled".equals(state)) {
                rb.setEnabled(false);
            }
            c = rb;
        } else if ("Switch".equals(id) || "SwitchMorph".equals(id)) {
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
            // Every value here mirrors what the matching native reference app sets, and
            // the DESKTOP apps disagree with the mobile ones, so this is scoped by row
            // rather than shared:
            //
            //   mobile  slider 0.5, progress 0.5   (RefWidgets.java, NativeRef.swift)
            //   desktop slider 0.5, progress 0.6   (the three desktop reference apps)
            //
            // Both sides must sit at the same value or the comparison is between two
            // different states. Setting progress to 60 everywhere moved the MOBILE bar off
            // its golden, which was captured at 0.5 -- a regression in the iOS and Android
            // suites introduced while fixing the desktop one.
            boolean desktopRow = spec.getId() != null && spec.getId().startsWith("Desktop");
            s.setProgress("ProgressBar".equals(id) && desktopRow ? 60 : 50);
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
        } else if ("GlassText".equals(id)) {
            // Ladder rung 1: a glass capsule (cn1-pill-border) filling the tile minus
            // 1mm, with one centred text label. Identical authored geometry to the
            // native ios_glass_text, so only the text + glass tint differ.
            Container panel = new Container(new BorderLayout());
            panel.setUIID("GlassText");
            Label l = new Label("Tab");
            l.setUIID("GlassTextLabel");
            panel.add(BorderLayout.CENTER, l);
            c = panel;
        } else if ("GlassIcon".equals(id)) {
            // Ladder rung 2: same glass capsule with one centred icon (SF or Material
            // per iosSFSymbolsBool) so the icon glyph is isolated against the native
            // SF star.fill.
            Container panel = new Container(new BorderLayout());
            panel.setUIID("GlassText");
            int icColor = dark ? 0xffffff : 0x000000;
            com.codename1.ui.plaf.Style icS = new com.codename1.ui.plaf.Style();
            icS.setFgColor(icColor);
            icS.setBgTransparency(0);
            Label l = new Label();
            l.setUIID("GlassTextLabel");
            l.setIcon(FontImage.createSFOrMaterial(FontImage.MATERIAL_STAR, icS, 4.1f));
            panel.add(BorderLayout.CENTER, l);
            c = panel;
        } else if ("TabOne".equals(id)) {
            // UIKit reserves its normal icon slot even when this UITabBarItem has
            // no image.  Supply a transparent icon of the themed slot size so CN1
            // keeps the same title position and overall bar height.
            Tabs tabs = new Tabs(Component.TOP);
            int blankWidth = com.codename1.ui.Display.getInstance().convertToPixels(6.75f);
            int blankHeight = com.codename1.ui.Display.getInstance().convertToPixels(3.75f);
            Image blankIcon = Image.createImage(blankWidth, blankHeight, 0);
            tabs.addTab("Tab", blankIcon, new Container());
            c = tabs;
        } else if ("Tabs".equals(id) || "TabsGeom".equals(id) || "TabsMorph".equals(id)
                || "TabsGlassMotion".equals(id)) {
            // iOS UITabBar: an icon-over-label bar at the TOP, three items
            // (Featured / Search / More) mirroring the native reference's system
            // tab items; the first is selected (blue), the rest grey. NOT a
            // Material pill strip.
            Tabs tabs = new Tabs(Component.TOP);
            // The material icons don't auto-tint to the tab's fg, so build them
            // with explicit colours -- but the colours are PER PLATFORM, not a
            // shared constant. On iOS the vivid blues are part of the lens
            // pipeline (the theme's SelectedTab fg is deliberately dark; the GPU
            // lens drop colours it blue, and the glass pill mutes the tint so
            // the icon starts more vivid than the final look). On Android the
            // tabs must render the THEME's own Material colours (purple
            // selected / onSurfaceVariant unselected) exactly as the theme
            // defines them -- hardcoding the iOS blues here made the Android
            // tile a fake that could never match the honest M3 reference.
            boolean iosTabs = "ios".equals(com.codename1.ui.Display.getInstance().getPlatformName());
            // The iOS 27 bar (tabsMorphPreset ios27) paints every tab as UNSELECTED
            // content outside the selection lens and as SELECTED content inside it
            // -- the accent travels with the glass -- so each tab needs both icon
            // states, in the theme's own TabIcon / TabIcon.pressed colours.
            boolean glass27 = iosTabs && "ios27".equals(com.codename1.ui.plaf.UIManager.getInstance()
                    .getThemeConstant("tabsMorphPreset", ""));
            int selColor;
            int unselColor;
            if (glass27) {
                selColor = com.codename1.ui.plaf.UIManager.getInstance()
                        .getComponentCustomStyle("TabIcon", "press").getFgColor();
                unselColor = com.codename1.ui.plaf.UIManager.getInstance()
                        .getComponentStyle("TabIcon").getFgColor();
            } else if (iosTabs) {
                // Both appearances use the native vivid accent: in light the lens
                // re-tints the deliberately-dark glyph; in dark the lens tint is
                // disabled (it flooded the dark bar) and the glyph carries it.
                selColor = 0x0a84ff;
                unselColor = dark ? 0xebebf5 : 0x3c3c43;
            } else {
                selColor = com.codename1.ui.plaf.UIManager.getInstance()
                        .getComponentStyle("SelectedTab").getFgColor();
                unselColor = com.codename1.ui.plaf.UIManager.getInstance()
                        .getComponentStyle("UnselectedTab").getFgColor();
            }
            com.codename1.ui.plaf.Style selS = new com.codename1.ui.plaf.Style();
            selS.setFgColor(selColor);
            selS.setBgTransparency(0);
            com.codename1.ui.plaf.Style unS = new com.codename1.ui.plaf.Style();
            unS.setFgColor(unselColor);
            unS.setBgTransparency(0);
            // Icon size in mm, theme-tunable (tabIconSizeMm) so SF/Material tab icons
            // can be matched to native without rebuilding. SF symbols render at their
            // natural per-symbol bounds for this point size, so this is the nominal
            // em size, not a forced pixel height.
            float tabIconMm;
            try {
                tabIconMm = Float.parseFloat(com.codename1.ui.plaf.UIManager.getInstance()
                        .getThemeConstant("tabIconSizeMm", "4.1").trim());
            } catch (NumberFormatException nfe) {
                tabIconMm = 4.1f;
            }
            if (glass27) {
                tabs.addTab("Featured", FontImage.createSFOrMaterial(FontImage.MATERIAL_STAR, unS, tabIconMm),
                        FontImage.createSFOrMaterial(FontImage.MATERIAL_STAR, selS, tabIconMm), new Container());
                tabs.addTab("Search", FontImage.createSFOrMaterial(FontImage.MATERIAL_SEARCH, unS, tabIconMm),
                        FontImage.createSFOrMaterial(FontImage.MATERIAL_SEARCH, selS, tabIconMm), new Container());
                tabs.addTab("More", FontImage.createSFOrMaterial(FontImage.MATERIAL_MORE_HORIZ, unS, tabIconMm),
                        FontImage.createSFOrMaterial(FontImage.MATERIAL_MORE_HORIZ, selS, tabIconMm),
                        new Container());
            } else {
                Image star = FontImage.createSFOrMaterial(FontImage.MATERIAL_STAR, selS, tabIconMm);
                Image search = FontImage.createSFOrMaterial(FontImage.MATERIAL_SEARCH, unS, tabIconMm);
                Image more = FontImage.createSFOrMaterial(FontImage.MATERIAL_MORE_HORIZ, unS, tabIconMm);
                tabs.addTab("Featured", star, star, new Container());
                tabs.addTab("Search", search, search, new Container());
                tabs.addTab("More", more, more, new Container());
            }
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
                // The native bar adds NO tint in either mode -- the strip is pure backdrop.
                bar.getAllStyles().setBgTransparency(0);
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
            if (iosDlg) {
                Container dialog = new Container(new BorderLayout());
                dialog.setUIID("Dialog");
                Label title = new Label("Title");
                title.setUIID("DialogTitle");
                title.getAllStyles().setAlignment(Component.CENTER);
                Label body = new Label(text);
                body.setUIID("DialogBody");
                body.getAllStyles().setAlignment(Component.CENTER);
                LayeredLayout contentLayout = new LayeredLayout();
                Container content = new Container(contentLayout);
                content.getAllStyles().setBgTransparency(0);
                content.add(title);
                content.add(body);
                // Match UIAlertController's vertical fields inside its alert
                // surface.  Insets are percentages so this remains stable at
                // every fidelity scale while preserving the reference ratios.
                contentLayout.setInsets(title, "37.75% 0 auto 0");
                contentLayout.setInsets(body, "81.75% 0 auto 0");

                Button cancel = new Button("Cancel");
                cancel.setUIID("DialogButton");
                Button ok = new Button("OK");
                ok.setUIID("DialogButtonDefault");
                Container btns = new Container(new GridLayout(1, 2));
                btns.setUIID("DialogCommandArea");
                String separator = UIManager.getInstance().getThemeConstant(
                        "dark".equals(appearance)
                                ? "dlgInvisibleButtonsDark" : "dlgInvisibleButtons",
                        "dark".equals(appearance) ? "38383a" : "c6c6c8");
                Border divider = Border.createCompoundBorder(null, null, null,
                        Border.createLineBorder(1, Integer.parseInt(separator, 16)));
                cancel.getUnselectedStyle().setBorder(divider);
                cancel.getSelectedStyle().setBorder(divider);
                cancel.getPressedStyle().setBorder(divider);
                btns.add(cancel);
                btns.add(ok);
                dialog.add(BorderLayout.CENTER, content);
                dialog.add(BorderLayout.SOUTH, btns);
                c = dialog;
            } else {
                Container dialog = new Container(new BorderLayout());
                dialog.setUIID("Dialog");
                Label title = new Label("Title");
                title.setUIID("DialogTitle");
                title.getAllStyles().setAlignment(Component.LEFT);
                Label body = new Label(text);
                body.setUIID("DialogBody");
                body.getAllStyles().setAlignment(Component.LEFT);
                Container content = new Container(BoxLayout.y());
                content.getAllStyles().setBgTransparency(0);
                content.add(title);
                content.add(body);
                Button cancel = new Button("Cancel");
                cancel.setUIID("DialogButton");
                Button ok = new Button("OK");
                ok.setUIID("DialogButton");
                Container btns = new Container(new FlowLayout(Component.RIGHT));
                btns.setUIID("DialogCommandArea");
                btns.add(cancel);
                btns.add(ok);
                dialog.add(BorderLayout.CENTER, content);
                dialog.add(BorderLayout.SOUTH, btns);
                c = dialog;
            }
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
        } else if ("DesktopScrollBar".equals(id) || "DesktopScrollBarHighlight".equals(id)) {
            // The bar itself, not a scrolling container. LookAndFeel.drawVerticalScroll takes
            // any component and paints the theme's track and thumb across it, which is exactly
            // the bare NSScroller / GtkScrollbar / WinUI ScrollBar the reference apps build --
            // a scrolling container would put its CONTENT in the comparison too.
            //
            // The hover and drag states are expressed by OVERRIDING the two public methods the
            // look and feel asks, rather than by faking a pointer. Those methods are what
            // drawScroll reads to pick the thumb's selected or pressed style, so this renders
            // the same pixels a real hover does, and it needs no test-only hook in the product.
            c = new ScrollBarProbe("hover".equals(state), "pressed".equals(state));
        } else if ("DesktopSeparator".equals(id)) {
            com.codename1.components.Separator sep = new com.codename1.components.Separator();
            sep.setUIID(uiid);
            c = sep;
        } else if ("DesktopGroupBox".equals(id)) {
            com.codename1.components.GroupBox box =
                    new com.codename1.components.GroupBox(text);
            box.setUIID(uiid);
            // One short row of content, so the frame has something to enclose. A native
            // NSBox / GtkFrame with an empty body collapses to its own insets, which is not a
            // control anyone would recognise -- the same reason the text field is given a
            // width rather than allowed to measure to its placeholder.
            Label body = new Label("Item");
            body.setUIID("Label");
            box.add(body);
            c = box;
        } else if ("DesktopStepper".equals(id)) {
            com.codename1.components.Stepper st = new com.codename1.components.Stepper(1, 0, 10);
            st.setUIID(uiid);
            if ("disabled".equals(state)) {
                st.setEnabled(false);
                st.getField().setEnabled(false);
                st.getDecrementButton().setEnabled(false);
                st.getIncrementButton().setEnabled(false);
            }
            c = st;
        } else if ("DesktopLinkButton".equals(id)) {
            Button link = new Button(text);
            link.setUIID(uiid);
            link.getAllStyles().setMargin(0, 0, 0, 0);
            applyButtonState(link, state);
            c = link;
        } else if ("DesktopSearchField".equals(id)) {
            TextField search = new TextField(text);
            search.setUIID(uiid);
            search.setEditable(false);
            search.getAllStyles().setMargin(0, 0, 0, 0);
            search.setColumns(1);
            search.setGrowByContent(true);
            if ("disabled".equals(state)) {
                search.setEnabled(false);
            }
            c = search;
        } else if ("DesktopListRow".equals(id)) {
            // A row is a Label under the ListRenderer UIID, which is what a CN1 list paints
            // for each entry. Selected and hover are ordinary style states here rather than
            // list-model selection, because the tile is one row with no list around it.
            Label row = new Label(text);
            row.setUIID(uiid);
            row.getAllStyles().setMargin(0, 0, 0, 0);
            if ("selected".equals(state)) {
                row.setFocus(true);
            }
            c = row;
        } else if ("DesktopTabs".equals(id)) {
            Tabs tabs = new Tabs();
            tabs.setUIID(uiid);
            tabs.addTab("One", new Label(""));
            tabs.addTab("Two", new Label(""));
            c = tabs;
        } else if ("DesktopToolbar".equals(id)) {
            // The strip, built directly rather than through Form.setToolbar: the tile has no
            // form chrome around it, and a Toolbar taken off a Form brings its title area's
            // layout with it.
            Container bar = new Container(new BorderLayout());
            bar.setUIID(uiid);
            Label title = new Label("Title");
            title.setUIID("Title");
            bar.add(BorderLayout.CENTER, title);
            c = bar;
        } else if ("DesktopDisclosure".equals(id)) {
            Button disclosure = new Button(text);
            disclosure.setUIID(uiid);
            disclosure.getAllStyles().setMargin(0, 0, 0, 0);
            c = disclosure;
        } else if ("DesktopMenuBar".equals(id)) {
            Container menuBar = new Container(new FlowLayout());
            menuBar.setUIID(uiid);
            Button item = new Button(text);
            item.setUIID("Command");
            menuBar.add(item);
            c = menuBar;
        } else if ("DesktopMenuItem".equals(id)) {
            Button item = new Button(text);
            item.setUIID(uiid);
            item.getAllStyles().setMargin(0, 0, 0, 0);
            applyButtonState(item, state);
            if ("disabled".equals(state)) {
                item.setEnabled(false);
            }
            c = item;
        } else if ("DesktopTooltip".equals(id)) {
            Container tip = new Container(new BorderLayout());
            tip.setUIID("TooltipDialog");
            Label label = new Label(text);
            label.setUIID(uiid);
            tip.add(BorderLayout.CENTER, label);
            c = tip;
        } else {
            return null;
        }
        return c;
    }

    /// Paints the theme's interactive scrollbar and nothing else.
    ///
    /// `LookAndFeel.drawVerticalScroll` takes any component and paints the track and thumb
    /// across it, so this is the bar on its own -- the same thing the reference apps build,
    /// rather than a scrolling container whose content would join the comparison.
    ///
    /// The two state overrides are the whole trick. `drawScroll` asks the component it is
    /// painting for `isVScrollThumbHover()` and `isVScrollThumbGrabbed()` to choose between
    /// the thumb's unselected, selected and pressed styles. Both are public, so answering
    /// them directly renders exactly the pixels a real hover or drag produces, with no
    /// test-only hook added to the framework and no synthetic pointer to get wrong.
    private static final class ScrollBarProbe extends Container {
        private final boolean hover;
        private final boolean grabbed;

        ScrollBarProbe(boolean hover, boolean grabbed) {
            this.hover = hover;
            this.grabbed = grabbed;
            setUIID("Container");
            getAllStyles().setMargin(0, 0, 0, 0);
            getAllStyles().setPadding(0, 0, 0, 0);
            getAllStyles().setBgTransparency(0);
        }

        @Override
        public boolean isVScrollThumbHover() {
            return hover;
        }

        @Override
        public boolean isVScrollThumbGrabbed() {
            return grabbed;
        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            // As wide as the theme's gutter and as tall as it is given. The gutter width is
            // the measurement -- it is DesktopScroll's own padding plus margin -- so it must
            // come from the look and feel rather than from a number written here.
            return new com.codename1.ui.geom.Dimension(
                    getUIManager().getLookAndFeel().getVerticalScrollWidth(), 1);
        }

        @Override
        public void paint(com.codename1.ui.Graphics g) {
            // offsetRatio 0, blockSizeRatio 0.4: a thumb at the top covering about two fifths
            // of the track. Fixed rather than derived, because the reference apps set the same
            // proportion by hand and the two sides have to agree about where the thumb is
            // before anything about its colour or shape can be compared.
            getUIManager().getLookAndFeel().drawVerticalScroll(g, this, 0f, 0.4f);
        }
    }

    private static void applyButtonState(Button b, String state) {
        if ("disabled".equals(state)) {
            b.setEnabled(false);
        } else if ("pressed".equals(state)) {
            // Force the pressed visual state so the pressed style is painted.
            b.pressed();
        }
    }

    /// Applies state that requires an attached component, after the host Form is shown.
    /// Build cannot assign focus: neither runner has attached the widget at that point.
    public static void applyAttachedState(Component c, String state) {
        if ("focus".equals(state)) {
            Form form = c.getComponentForm();
            if (form == null) {
                throw new IllegalStateException("Focus capture requires an attached component");
            }
            form.setFocused(c);
        }
    }

    /// Applies the two states that only exist on the desktop.
    ///
    /// Both are set on the MODEL rather than synthesised as input, which is how every other
    /// state here is driven: the runner never moves a pointer or presses a key, because a
    /// capture that depends on input timing is a capture that differs between runs. Hover is
    /// the state Codename One gained for these themes; focus is the existing selected style,
    /// which is what a focused component renders with.
    private static void applyDesktopState(Component c, String state) {
        if ("hover".equals(state)) {
            // Margin normalisation is NOT done here. The tile runner zeroes margins AFTER
            // build() returns, so anything copied at this point is overwritten a line later;
            // DesktopTileRunner owns it for every component, hover style included.
            c.setHovered(true);
        } else if ("focus".equals(state)) {
            c.setFocusable(true);
            // applyAttachedState assigns the focus owner after attachment and show.
        }
    }
}
