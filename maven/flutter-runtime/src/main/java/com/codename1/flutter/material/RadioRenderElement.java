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
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.RadioButton;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;

import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

/**
 * Leaf render box for {@link Radio}: a CN1 RadioButton (UIID "FlutterRadio")
 * deliberately NOT placed in a CN1 ButtonGroup — the selected state is
 * derived from Dart-equality of {@code value} vs {@code groupValue}, and the
 * component is snapped back to that derived state after every user press
 * (controlled semantics). Material tap target: 48x48lp minimum.
 */
public class RadioRenderElement extends RenderElement {

    /** Material minimum tap target in logical pixels. */
    public static final double TAP_TARGET_LP = 48;

    private boolean applying;

    public RadioRenderElement(Radio widget) {
        super(widget);
    }

    private Radio radio() {
        return (Radio) widget();
    }

    /**
     * Whether the current configuration renders this radio selected:
     * Dart equality of value vs groupValue.
     */
    public boolean selected() {
        return DartRuntime.eq(radio().getValue(), radio().getGroupValue());
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        RadioButton rb = new RadioButton();
        rb.setUIID("FlutterRadio");
        rb.addActionListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (applying) {
                    return;
                }
                userSelected();
            }
        });
        apply(rb);
        return rb;
    }

    @Override
    protected void updateComponent(Component c) {
        apply((RadioButton) c);
    }

    private void apply(RadioButton rb) {
        applying = true;
        try {
            rb.setSelected(selected());
            // A null onChanged is Flutter's disabled radio, as the checkbox, switch and
            // slider renderers already mirror; the radio stayed enabled and focusable.
            rb.setEnabled(radio().getOnChanged() != null);
        } finally {
            applying = false;
        }
    }

    /**
     * Controlled select entry point (public so headless tests can drive it):
     * fires onChanged with this radio's value, then re-applies the state
     * derived from the CURRENT groupValue.
     */
    public void userSelected() {
        Funcs.VoidFunc1<Object> f = radio().getOnChanged();
        if (f != null) {
            f.call(radio().getValue());
        }
        Component c = component();
        if (c != null) {
            apply((RadioButton) c);
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
