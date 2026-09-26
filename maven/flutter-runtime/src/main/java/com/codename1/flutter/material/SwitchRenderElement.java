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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;

import dart.runtime.Funcs;

/**
 * Leaf render box for {@link Switch}: a CN1
 * {@link com.codename1.components.Switch} constructed with the
 * "FlutterSwitch" UIID, controlled like {@link CheckboxRenderElement}. The
 * action listener (user interaction only — programmatic setValue fires
 * change, not action events) reports the flip and snaps back.
 * Intrinsic size: the M3 switch track 52x32lp minimum.
 */
public class SwitchRenderElement extends RenderElement {

    /** M3 switch track width in logical pixels. */
    public static final double TRACK_WIDTH_LP = 52;
    /** M3 switch track height in logical pixels. */
    public static final double TRACK_HEIGHT_LP = 32;

    private boolean applying;

    public SwitchRenderElement(Switch widget) {
        super(widget);
    }

    private Switch switchWidget() {
        return (Switch) widget();
    }

    /**
     * The value the current widget configuration mandates.
     */
    public boolean configuredValue() {
        return switchWidget().getValue();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        com.codename1.components.Switch sw = new com.codename1.components.Switch("FlutterSwitch");
        sw.addActionListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (applying) {
                    return;
                }
                userToggled(((com.codename1.components.Switch) component()).isValue());
            }
        });
        apply(sw);
        return sw;
    }

    @Override
    protected void updateComponent(Component c) {
        apply((com.codename1.components.Switch) c);
    }

    private void apply(com.codename1.components.Switch sw) {
        applying = true;
        try {
            sw.setValue(configuredValue());
            // A null onChanged is Flutter's disabled switch; the checkbox and button
            // renderers already mirror that, and the switch kept enabled styling,
            // focus and pointer behaviour.
            sw.setEnabled(switchWidget().getOnChanged() != null);
        } finally {
            applying = false;
        }
    }

    /**
     * Controlled toggle entry point (public so headless tests can drive it):
     * fires onChanged with the attempted value, then re-applies the widget's
     * configured value.
     */
    public void userToggled(boolean attemptedValue) {
        Funcs.VoidFunc1<Boolean> f = switchWidget().getOnChanged();
        if (f != null) {
            f.call(attemptedValue);
        }
        Component c = component();
        if (c != null) {
            apply((com.codename1.components.Switch) c);
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double w = Dp.px(TRACK_WIDTH_LP);
        double h = Dp.px(TRACK_HEIGHT_LP);
        Component c = component();
        if (c != null) {
            Dimension d = c.getPreferredSize();
            w = Math.max(w, d.getWidth());
            h = Math.max(h, d.getHeight());
        }
        return constraints.constrain(new Size(w, h));
    }
}
