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
package com.benchmark.app;

import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Stable Java 8 sample for the translation benchmark's pinned 7.0.214 libraries.
 * Keep this workload independent of the live demo app and its newer API tests.
 */
public class HelloCodenameOne {
    private Form current;

    public void init(Object context) {
    }

    public void start() {
        if (current == null) {
            current = new Form("Hello Codename One", BoxLayout.y());
            current.add(new Label("Translation benchmark"));
            Button button = new Button("Hello");
            button.addActionListener(new ActionListener<ActionEvent>() {
                public void actionPerformed(ActionEvent event) {
                    Dialog.show("Hello", "Hello Codename One", "OK", null);
                }
            });
            current.add(button);
        }
        current.show();
    }

    public void stop() {
    }

    public void destroy() {
    }
}
