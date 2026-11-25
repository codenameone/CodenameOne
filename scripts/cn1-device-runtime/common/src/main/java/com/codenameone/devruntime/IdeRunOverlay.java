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
package com.codenameone.devruntime;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;

/// A Stop button floated over every form a running program shows, whether
/// the editor started it or a computer pushed it.
///
/// A program shows its own forms and the runtime's toolbar goes with them, so
/// without this there is no way out short of killing the app. It is put into
/// each form's layered pane as the form is shown, and the button stops the
/// program, which returns to where it was started from.
public final class IdeRunOverlay {
    private static ActionListener listener;

    private IdeRunOverlay() {
    }

    /// Starts adding the button to forms; also adds it to the current one.
    /// Callable from any thread.
    public static void install() {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(IdeRunOverlay::install);
            return;
        }
        if (listener != null) {
            return;
        }
        listener = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Form f = Display.getInstance().getCurrent();
                if (isProgramForm(f)) {
                    attach(f);
                }
            }
        };
        Form f = Display.getInstance().getCurrent();
        if (isProgramForm(f)) {
            attach(f);
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                poll();
            }
        });
    }

    /// Stops adding it.
    public static void uninstall() {
        listener = null;
    }

    // There is no global "form shown" event, so the overlay checks the current
    // form on the next frames until the program is stopped. Cheap: one
    // getCurrent and a client property lookup.
    private static void poll() {
        if (listener == null || !DeviceRuntimeService.getInstance().isProgramRunning()) {
            listener = null;
            return;
        }
        listener.actionPerformed(null);
        com.codename1.ui.util.UITimer.timer(500, false, Display.getInstance().getCurrent(),
                new Runnable() {
                    public void run() {
                        poll();
                    }
                });
    }

    /// Not the runtime's own screens: those have their own way back.
    private static boolean isProgramForm(Form f) {
        return f != null && !(f instanceof IdeForm) && !(f instanceof DeviceRuntimeForm);
    }

    static void attach(Form f) {
        if (f.getClientProperty("cn1ide.stop") != null) {
            return;
        }
        f.putClientProperty("cn1ide.stop", Boolean.TRUE);
        Container layer = f.getLayeredPane(IdeRunOverlay.class, true);
        layer.setLayout(new FlowLayout(Component_RIGHT, Component_BOTTOM));
        Button stop = new Button("Stop");
        // Its own style, not the button's: an icon drawn from the button style
        // paints the background colour into its square too.
        Style iconStyle = new Style();
        iconStyle.setFgColor(0xffffff);
        iconStyle.setBgTransparency(0);
        stop.setIcon(FontImage.createMaterial(FontImage.MATERIAL_STOP, iconStyle, 3.5f));
        Style s = stop.getAllStyles();
        s.setBgColor(0xc62828);
        s.setFgColor(0xffffff);
        s.setBgTransparency(235);
        // The theme's button border paints its own fill over the background
        // colour, which is how this came out in the accent colour, not red.
        s.setBorder(com.codename1.ui.plaf.Border.createRoundBorder(40, 40, 0xc62828, false));
        s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        s.setPadding(2, 2, 4, 4);
        s.setMarginUnit(Style.UNIT_TYPE_DIPS);
        s.setMargin(0, 6, 0, 3);
        stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                listener = null;
                DeviceRuntimeService.getInstance().stopProgram();
            }
        });
        layer.add(stop);
        f.revalidate();
    }

    private static final int Component_RIGHT = com.codename1.ui.Component.RIGHT;
    private static final int Component_BOTTOM = com.codename1.ui.Component.BOTTOM;
}
