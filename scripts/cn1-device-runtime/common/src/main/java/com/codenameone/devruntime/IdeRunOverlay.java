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
import com.codename1.components.ToastBar;
import com.codename1.io.Preferences;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;

/// The "Slow interpreted mode" badge floated over every form a running
/// program shows, whether the editor started it or a computer pushed it.
///
/// It says two things a pushed program cannot: that it runs in an
/// interpreter, so its speed says nothing about the same code compiled, and
/// how to leave. A long press stops the program and returns to where it was
/// started from; a short tap explains that. A long press rather than a tap
/// because the badge sits over the program's own UI, where a stray touch must
/// not end it.
///
/// Its corner and whether it shows at all are settings
/// ([DeviceRuntimeSettingsForm]). Hidden, the program is still stopped from
/// the pairing screen or from the computer.
public final class IdeRunOverlay {
    /// Preference keys, shared with the settings screen.
    static final String PREF_HIDDEN = "instant.overlay.hidden";
    static final String PREF_CORNER = "instant.overlay.corner";

    /// Corners, in the order the settings screen lists them.
    static final String[] CORNERS = {"Bottom right", "Bottom left", "Top right", "Top left"};

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
        if (Preferences.get(PREF_HIDDEN, false)) {
            return;
        }
        int corner = Preferences.get(PREF_CORNER, 0);
        if (corner < 0 || corner >= CORNERS.length) {
            corner = 0;
        }
        boolean left = corner == 1 || corner == 3;
        boolean top = corner >= 2;
        Container layer = f.getLayeredPane(IdeRunOverlay.class, true);
        layer.setLayout(new FlowLayout(left ? Component_LEFT : Component_RIGHT,
                top ? Component_TOP : Component_BOTTOM));
        // Clear of the notch and the home indicator in every corner.
        layer.setSafeArea(true);
        final boolean[] longPressed = new boolean[1];
        final Button badge = new Button("Slow interpreted mode");
        Style s = badge.getAllStyles();
        s.setBgColor(0x37474f);
        s.setFgColor(0xffffff);
        s.setBgTransparency(215);
        s.setBorder(com.codename1.ui.plaf.Border.createRoundBorder(30, 30, 0x37474f, false));
        s.setFont(com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM,
                com.codename1.ui.Font.STYLE_PLAIN, com.codename1.ui.Font.SIZE_SMALL));
        s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        s.setPadding(1, 1, 3, 3);
        s.setMarginUnit(Style.UNIT_TYPE_DIPS);
        s.setMargin(top ? 2 : 0, top ? 0 : 4, 3, 3);
        badge.addLongPressListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                longPressed[0] = true;
                listener = null;
                DeviceRuntimeService.getInstance().stopProgram();
            }
        });
        badge.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                if (longPressed[0]) {
                    // The release that ends a long press is not a tap.
                    return;
                }
                ToastBar.showInfoMessage("This program runs in an interpreter, much slower "
                        + "than when built. Long-press this label to stop it.");
            }
        });
        layer.add(badge);
        f.revalidate();
    }

    private static final int Component_LEFT = com.codename1.ui.Component.LEFT;
    private static final int Component_TOP = com.codename1.ui.Component.TOP;
    private static final int Component_RIGHT = com.codename1.ui.Component.RIGHT;
    private static final int Component_BOTTOM = com.codename1.ui.Component.BOTTOM;
}
