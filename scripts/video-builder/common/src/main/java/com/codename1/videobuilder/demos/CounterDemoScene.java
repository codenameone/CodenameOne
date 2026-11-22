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
package com.codename1.videobuilder.demos;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.videobuilder.DemoContext;
import com.codename1.videobuilder.DemoScene;
import java.util.Map;

/** Small compiled example that demonstrates real event and layout animation code. */
public final class CounterDemoScene implements DemoScene {
    private Container root;
    private Label count;
    private int value;

    @Override public Component create(DemoContext context) {
        count = new Label("0");
        count.getAllStyles().setAlignment(Component.CENTER);
        count.getAllStyles().setFont(com.codename1.ui.Font.createSystemFont(
                com.codename1.ui.Font.FACE_SYSTEM, com.codename1.ui.Font.STYLE_BOLD,
                com.codename1.ui.Font.SIZE_LARGE));
        Button increment = new Button("Increment");
        increment.addActionListener(event -> setValue(value + 1));
        Container controls = BoxLayout.encloseYCenter(count, increment);
        root = new Container(new BorderLayout());
        root.add(BorderLayout.CENTER, controls);
        return root;
    }

    @Override public void onAction(String name, Map<String, Object> arguments) {
        if ("increment".equals(name)) {
            Object requested = arguments.get("count");
            setValue(requested instanceof Number ? ((Number) requested).intValue() : value + 1);
        } else if ("reset".equals(name)) {
            reset();
        } else {
            throw new IllegalArgumentException("Unknown counter demo action: " + name);
        }
    }

    private void setValue(int newValue) {
        value = newValue;
        count.setText(String.valueOf(value));
        root.animateLayout(450);
    }

    @Override public void reset() {
        value = 0;
        if (count != null) count.setText("0");
    }

    @Override public void dispose() {
        root = null;
        count = null;
    }
}
