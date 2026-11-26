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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.devruntime;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.Label;

/// About and help: what the app is, its version and build, and how to use it.
///
/// Written for someone who has never seen it -- an App Review reviewer above
/// all, who has no computer paired and must be able to exercise the whole app
/// from the editor alone. Everything that runs is source the user can read and
/// change in the editor; this screen says so plainly.
public final class AboutForm extends Form {
    private AboutForm(Form previous) {
        super("About", BoxLayout.y());
        setScrollableY(true);
        getToolbar().setBackCommand("", Toolbar.BackCommandPolicy.AS_ARROW, e -> previous.showBack());

        Label name = new Label(Display.getInstance().getProperty("AppName", "Codename One Instant"));
        name.getAllStyles().setFont(com.codename1.ui.Font.createSystemFont(
                com.codename1.ui.Font.FACE_SYSTEM, com.codename1.ui.Font.STYLE_BOLD,
                com.codename1.ui.Font.SIZE_LARGE));
        add(name);
        add(new Label("Version " + version()));

        section("What it is",
                "A Java editor and runtime for learning and prototyping Codename One user interfaces. "
                + "You write Java source here, the app compiles it on the device and runs it in an "
                + "interpreter. Nothing is downloaded: every program that runs is source you can "
                + "read and edit in the editor.");
        section("Try it",
                "1. The Hello project opens on first launch.\n"
                + "2. Tap Run (top right). The program's screen appears.\n"
                + "3. Tap the red Stop button to return to the editor.\n"
                + "4. Change the text in the source, tap Run again.\n"
                + "Compile errors show in the console below the editor; tap one to jump to its line.");
        section("Code completion",
                "Type a dot after a variable or class name to see its methods. Suggestions also "
                + "appear for class names and keywords as you type.");
        section("Running from a computer (optional)",
                "Developers can also send a program from their own computer with the Codename One "
                + "Maven plugin (mvn cn1:push). Tap the computer icon (top left) for the pairing "
                + "screen. A computer must be paired with a code shown on the computer before it "
                + "can send anything, and the source of a received program is shown and saved as a "
                + "project you can edit.");
        section("Privacy",
                "The app collects no data and contacts no server. The network is used only to talk "
                + "to a computer you paired yourself, on your own local network. Your projects stay "
                + "on this device.");
        section("Licenses",
                "Codename One is open source (GPL v2 with Classpath Exception). "
                + "Material Design icons: Apache License 2.0.");
    }

    private void section(String title, String body) {
        Label t = new Label(title);
        t.getAllStyles().setFont(com.codename1.ui.Font.createSystemFont(
                com.codename1.ui.Font.FACE_SYSTEM, com.codename1.ui.Font.STYLE_BOLD,
                com.codename1.ui.Font.SIZE_MEDIUM));
        t.getAllStyles().setMarginTop(3);
        Container c = BoxLayout.encloseY(t, new SpanLabel(body));
        add(c);
    }

    /// The version the user (and a reviewer) can quote in a report, including
    /// the build number App Store Connect shows.
    static String version() {
        Display d = Display.getInstance();
        String v = d.getProperty("AppVersion", "1.0");
        String build = d.getProperty("AppBuild", "");
        return build.length() > 0 && !build.equals(v) ? v + " (" + build + ")" : v;
    }

    /// Shows the screen; back returns to `previous`.
    public static void show(Form previous) {
        new AboutForm(previous).show();
    }
}
