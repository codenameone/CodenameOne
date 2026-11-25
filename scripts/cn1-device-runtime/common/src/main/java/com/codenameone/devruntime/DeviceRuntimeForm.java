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
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.UITimer;

/**
 * The screen you see when nothing is running.
 *
 * <p>Its job is to answer "what do I do now", so it says what to run on the
 * computer rather than describing its own internals. The address is there
 * because a network can always refuse to cooperate and somebody will need to
 * see it, but nothing here has to be typed for the ordinary case.</p>
 *
 * @author Shai Almog
 */
public class DeviceRuntimeForm extends Form {
    private final Label heading = new Label("");
    private final SpanLabel instruction = new SpanLabel("");
    private final Label command = new Label("");
    private final Label address = new Label("");

    public DeviceRuntimeForm() {
        super("Device Runtime", new BorderLayout());

        heading.getAllStyles().setAlignment(Label.CENTER);
        heading.getAllStyles().setFont(Font.createSystemFont(
                Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        instruction.getTextAllStyles().setAlignment(Label.CENTER);

        // The command is the thing being copied by eye, so it gets a face that
        // does not make l and 1 the same shape.
        command.getAllStyles().setAlignment(Label.CENTER);
        command.getAllStyles().setFont(Font.createSystemFont(
                Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        command.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        command.getAllStyles().setPadding(3, 3, 3, 3);

        address.getAllStyles().setAlignment(Label.CENTER);
        address.getAllStyles().setFont(Font.createSystemFont(
                Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));

        Button find = new Button("Look again");
        find.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                DeviceRuntimeService.setHost(null);
                heading.setText("Looking...");
                revalidate();
            }
        });
        Button forget = new Button("Forget paired computers");
        forget.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                DeviceRuntimePairing.forgetAll();
                heading.setText("Pairings forgotten");
                revalidate();
            }
        });
        // Not decoration. The App Store's allowance for running downloaded code
        // (2.5.2) is conditional on the person holding the device being able to
        // see and edit what runs on it, so this screen is load-bearing for
        // submission as well as useful for debugging.
        Button source = new Button("View source");
        source.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                DeviceRuntimeSourceForm.showIt(DeviceRuntimeForm.this);
            }
        });

        Button stop = new Button("Stop the program");
        stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                DeviceRuntimeService.getInstance().stopProgram();
                refresh();
            }
        });

        Container body = new Container(BoxLayout.y());
        body.add(heading);
        body.add(instruction);
        body.add(command);
        body.add(address);
        body.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
        body.getAllStyles().setMargin(6, 2, 4, 4);

        Container buttons = new Container(BoxLayout.y());
        buttons.add(source).add(stop).add(find).add(forget);
        buttons.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS);
        buttons.getAllStyles().setMargin(2, 3, 4, 4);
        // Otherwise the last button sits under the gesture bar / soft keys and
        // cannot be pressed. Safe area is a property of the container, so the
        // one holding the buttons is what has to opt in.
        buttons.setSafeArea(true);

        Container south = new Container(new BorderLayout());
        south.add(BorderLayout.CENTER, buttons);
        south.setSafeArea(true);

        add(BorderLayout.CENTER, body);
        add(BorderLayout.SOUTH, south);
        refresh();

        // The state it reports is not driven by anything the user does here --
        // a computer appears when a computer appears -- so it refreshes itself.
        UITimer.timer(1500, true, this, new Runnable() {
            public void run() {
                refresh();
            }
        });
    }

    /** Pulls the current state into the view. */
    public void refresh() {
        DeviceRuntimeService svc = DeviceRuntimeService.getInstance();
        String running = svc.getLoadedName();
        if (running != null && running.length() > 0) {
            heading.setText("Running " + running);
            instruction.setText("Push again from your IDE to replace it.");
            command.setText("");
        } else {
            heading.setText("Waiting for your computer");
            instruction.setText("In your project on the same network, run:");
            command.setText("mvn -Ppush-lan package");
        }

        String ip = DeviceRuntimeService.getLocalAddress();
        address.setText(ip == null
                ? "This device has no network address."
                : "This device is " + ip + (svc.isListening() ? "" : " (dial-out only)"));
        revalidate();
    }

    /** Shows the runtime screen, from any thread. */
    public static void showIt() {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Toolbar.setGlobalToolbar(true);
                new DeviceRuntimeForm().show();
            }
        });
    }
}
