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
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
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
                || "Dialog".equals(id);
    }

    /**
     * Builds the CN1 component, applies UIID + state. The caller is responsible
     * for sizing/placing it in a fixed tile and capturing it.
     */
    public static Component build(ComponentSpec spec, String state) {
        String id = spec.getId();
        String uiid = spec.getCn1Uiid();
        String text = spec.getText() != null ? spec.getText() : "";
        Component c;
        if ("Button".equals(id) || "RaisedButton".equals(id) || "FlatButton".equals(id)) {
            Button b = new Button(text);
            b.setUIID(uiid);
            applyButtonState(b, state);
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
            CheckBox cb = new CheckBox(text);
            cb.setUIID(uiid);
            if ("selected".equals(state)) {
                cb.setSelected(true);
            } else if ("disabled".equals(state)) {
                cb.setEnabled(false);
            }
            c = cb;
        } else if ("RadioButton".equals(id)) {
            RadioButton rb = new RadioButton(text);
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
        } else if ("Tabs".equals(id)) {
            // Material tab strip: 3 tabs, first selected. Content panes are empty
            // (the 16mm tile crops to the strip, which is what the native TabLayout
            // golden shows).
            Tabs tabs = new Tabs();
            tabs.addTab("Tab 1", new Container());
            tabs.addTab("Tab 2", new Container());
            tabs.addTab("Tab 3", new Container());
            c = tabs;
        } else if ("Toolbar".equals(id)) {
            // Material small top app bar: title on the bar. The CN1 Toolbar
            // component requires a Form (setToolBar), so for the standalone tile
            // we mirror its appearance with a Toolbar-styled bar + a Title label.
            Container bar = new Container(new BorderLayout());
            bar.setUIID("Toolbar");
            Label title = new Label(text);
            title.setUIID("Title");
            bar.add(BorderLayout.WEST, title);
            c = bar;
        } else if ("Dialog".equals(id)) {
            // Material 3 alert dialog content: a rounded surface card with a
            // headline, supporting text and two trailing text action buttons.
            Container dialog = new Container(BoxLayout.y());
            dialog.setUIID("Dialog");
            Label title = new Label("Title");
            title.setUIID("DialogTitle");
            Label body = new Label(text);
            body.setUIID("DialogBody");
            Container btns = new Container(new FlowLayout(Component.RIGHT));
            btns.setUIID("DialogCommandArea");
            Button cancel = new Button("Cancel");
            cancel.setUIID("DialogButton");
            Button ok = new Button("OK");
            ok.setUIID("DialogButton");
            btns.add(cancel);
            btns.add(ok);
            dialog.add(title);
            dialog.add(body);
            dialog.add(btns);
            c = dialog;
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
