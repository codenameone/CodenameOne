/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;

import dart.runtime.Funcs;

/**
 * Leaf render box for {@link Checkbox}: a CN1 CheckBox (UIID
 * "FlutterCheckbox") with controlled semantics — the user's toggle fires
 * {@code onChanged} and the component is immediately re-snapped to the
 * widget's configured value; only a rebuild with a new value moves it.
 * Material tap target: 48x48lp minimum.
 */
public class CheckboxRenderElement extends RenderElement {

    /** Material's checkbox glyph size in logical pixels. */
    private static final double GLYPH_LP = 18;

    /** Material minimum tap target in logical pixels. */
    public static final double TAP_TARGET_LP = 48;

    private boolean applying;

    public CheckboxRenderElement(Checkbox widget) {
        super(widget);
    }

    private Checkbox checkbox() {
        return (Checkbox) widget();
    }

    /**
     * The value the current widget configuration mandates.
     */
    public boolean configuredValue() {
        return checkbox().getValue();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        // A LABEL painting the Material glyph, not a CN1 CheckBox.
        //
        // Codename One's CheckBox takes its box from the look and feel's image
        // pair, so it has exactly two states and no way to say which glyph to
        // draw. Material has three -- checked, empty and indeterminate -- and
        // the demo shows all three beside a disabled row of the same. Drawing
        // the glyph directly is the only way to say all of that, and it also
        // puts the tick in the theme's colour rather than the look and feel's.
        com.codename1.ui.Label box = new com.codename1.ui.Label("");
        box.setUIID("FlutterCheckbox");
        box.getAllStyles().setPadding(0, 0, 0, 0);
        box.getAllStyles().setMargin(0, 0, 0, 0);
        box.getAllStyles().setBgTransparency(0);
        box.setFocusable(true);
        box.addPointerReleasedListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (applying || !checkbox().isEnabled()) {
                    return;
                }
                userToggled(checkbox().nextValue());
            }
        });
        apply(box);
        return box;
    }

    @Override
    protected void updateComponent(Component c) {
        apply((com.codename1.ui.Label) c);
    }

    private void apply(com.codename1.ui.Label box) {
        applying = true;
        try {
            Checkbox w = checkbox();
            char glyph;
            if (w.isIndeterminate()) {
                glyph = com.codename1.ui.FontImage.MATERIAL_INDETERMINATE_CHECK_BOX;
            } else if (configuredValue()) {
                glyph = com.codename1.ui.FontImage.MATERIAL_CHECK_BOX;
            } else {
                glyph = com.codename1.ui.FontImage.MATERIAL_CHECK_BOX_OUTLINE_BLANK;
            }
            box.getAllStyles().setFgColor(inkRgb(w));
            box.setEnabled(w.isEnabled());
            try {
                com.codename1.ui.FontImage.setMaterialIcon(box, glyph, Dp.mm(GLYPH_LP));
            } catch (Exception noIconFont) {
                // missing icon font: the tap target is still laid out
            }
        } finally {
            applying = false;
        }
    }

    /** Material's checkbox ink: the accent when on, an outline when off, faded when disabled. */
    private int inkRgb(Checkbox w) {
        com.codename1.flutter.Color ink = null;
        com.codename1.flutter.Color behind = null;
        try {
            com.codename1.flutter.material.ColorScheme cs =
                    com.codename1.flutter.material.Theme.of(this).colorScheme();
            if (cs != null) {
                behind = cs.surface();
                boolean on = w.isIndeterminate() || configuredValue();
                ink = on ? cs.primary() : cs.onSurfaceVariant();
                if (!w.isEnabled() && cs.onSurface() != null) {
                    ink = com.codename1.flutter.Color.alphaBlend(
                            new com.codename1.flutter.Color(
                                    (0x61L << 24) | (cs.onSurface().rgb() & 0xFFFFFFL)),
                            new com.codename1.flutter.Color(0xFF000000L
                                    | ((behind == null ? 0xFFFFFF : behind.rgb()) & 0xFFFFFF)));
                }
            }
        } catch (Throwable noTheme) {
            // an unthemed checkbox still has to paint
        }
        return ink == null ? 0x6200EE : ink.rgb();
    }

    /**
     * Controlled toggle entry point (public so headless tests can drive it):
     * fires onChanged with the attempted value, then re-applies the widget's
     * configured value to the component.
     */
    public void userToggled(Boolean attemptedValue) {
        Funcs.VoidFunc1<Boolean> f = checkbox().getOnChanged();
        if (f != null) {
            f.call(attemptedValue);
        }
        Component c = component();
        if (c != null) {
            apply((com.codename1.ui.Label) c);
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double min = Dp.px(TAP_TARGET_LP);
        double w = min;
        double h = min;
        Component c = component();
        if (c != null) {
            Dimension d = c.getPreferredSize();
            w = Math.max(w, d.getWidth());
            h = Math.max(h, d.getHeight());
        }
        return constraints.constrain(new Size(w, h));
    }
}
