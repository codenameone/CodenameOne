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
package com.codename1.jsfixture;

import com.codename1.components.SpanLabel;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;

/// The application scripts/test-javascript-composited-rendering.mjs drives: the Initializr's
/// Hello World screen plus one long SpanLabel, which is exactly the app issues #5910 and #5912
/// were reported against. Kept that small on purpose -- every failure the test looks for
/// showed up on this screen and nothing more is needed to see them.
public class CompositedApp extends Lifecycle {
    private static final String LOREM = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. ";

    @Override
    public void runApp() {
        String query = Display.getInstance().getProperty("browser.window.location.search", "");
        if (query.indexOf("screen=picker") >= 0) {
            showPicker();
            return;
        }
        Form hi = new Form("Hi World", BoxLayout.y());
        Button helloButton = new Button("Hello World");
        hi.add(helloButton);
        helloButton.addActionListener(e -> hello());
        hi.getToolbar().addMaterialCommandToSideMenu("Hello Command",
                FontImage.MATERIAL_CHECK, 4, e -> hello());
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            text.append(LOREM);
        }
        hi.add(new SpanLabel(text.toString().trim()));
        hi.show();
    }

    /// A string Picker left exactly as an application gets it: nothing forces the lightweight
    /// popup, so on this port it takes the native path and puts a `<select>` on the page. Kept
    /// off the default screen, whose layout the scrolling checks measure.
    private void showPicker() {
        Form form = new Form("Picker", BoxLayout.y());
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_STRINGS);
        picker.setStrings("One", "Two", "Three");
        picker.setSelectedString("One");
        Label picked = new Label("Picked nothing");
        picker.addActionListener(e -> {
            picked.setText("Picked " + picker.getSelectedString());
            form.revalidate();
        });
        form.add(picker);
        form.add(picked);
        form.show();
    }

    private void hello() {
        Dialog.show("Hello Codename One", "Welcome to Codename One", "OK", null);
    }
}
