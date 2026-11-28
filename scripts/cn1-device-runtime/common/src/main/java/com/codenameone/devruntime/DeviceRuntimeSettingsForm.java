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

import com.codename1.components.SpanLabel;
import com.codename1.io.Preferences;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;

/// The app's settings: where the "Slow interpreted mode" label sits over a
/// running program, and whether it shows at all. Stored in [Preferences] and
/// read each time a program's form is shown, so a change applies to the next
/// program that runs.
public final class DeviceRuntimeSettingsForm extends Form {
    private DeviceRuntimeSettingsForm(Form previous) {
        super("Settings", BoxLayout.y());
        setScrollableY(true);
        getToolbar().setBackCommand("", Toolbar.BackCommandPolicy.AS_ARROW, e -> previous.showBack());

        add(new Label("Interpreted mode label"));
        final CheckBox show = new CheckBox("Show over running programs");
        show.setSelected(!Preferences.get(IdeRunOverlay.PREF_HIDDEN, false));
        show.addActionListener(e -> Preferences.set(IdeRunOverlay.PREF_HIDDEN, !show.isSelected()));
        add(show);

        add(new Label("Position"));
        ButtonGroup group = new ButtonGroup();
        int current = Preferences.get(IdeRunOverlay.PREF_CORNER, 0);
        for (int i = 0; i < IdeRunOverlay.CORNERS.length; i++) {
            final int corner = i;
            RadioButton r = new RadioButton(IdeRunOverlay.CORNERS[i]);
            group.add(r);
            r.setSelected(i == current);
            r.addActionListener(e -> Preferences.set(IdeRunOverlay.PREF_CORNER, corner));
            add(r);
        }
        add(new SpanLabel("Long-press the label to stop a running program. With the label "
                + "hidden, stop it from the pairing screen (the computer icon) or from the "
                + "computer that sent it."));
    }

    /// Shows the screen; back returns to `previous`.
    public static void show(Form previous) {
        new DeviceRuntimeSettingsForm(previous).show();
    }
}
