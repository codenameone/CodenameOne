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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.Layout;

/**
 * <p>The chrome Codename One draws for itself: the menu a right click opens, the tooltip,
 * and a dialog's command area. These are the surfaces the three desktop themes gained
 * rules for in this change, and until now no capture on any platform contained them.</p>
 *
 * <p>Rendered as styled containers rather than by opening the real popups, for the reason
 * {@code DialogThemeScreenshotTest} already gives: a modal popup parks the caller, and the
 * caller here is the harness. {@link com.codename1.ui.ContextMenu#show} takes the modal
 * path deliberately -- it returns the chosen command -- so driving it from a screenshot
 * test would mean either a second thread or a modeless entry point that exists only for
 * the test. Both buy a worse test than this one.</p>
 *
 * <p>What that trade costs is worth stating plainly: this proves the UIIDs
 * ({@code PopupContentPane}, {@code CommandList}, {@code Command}, {@code TooltipDialog},
 * {@code Tooltip}, {@code DialogCommandArea}, {@code DialogButton},
 * {@code DialogButtonDefault}) are defined and legible in both appearances. It does not
 * prove the menu opens. {@code ContextMenuTest} does that, and the two are complementary
 * rather than overlapping -- the bug this PR fixed in the tab strip was a theme naming a
 * UIID the code never writes, which only a pairing like this can catch from both ends.</p>
 *
 * <p>Hover is absent on purpose. {@code Command.hover} cannot be forced without a live
 * pointer, and the desktop fidelity suite already scores hover against the real WinUI,
 * AppKit and GTK references, which is a stronger check than a screenshot of a state this
 * harness would have to fake.</p>
 */
public class DesktopChromeThemeScreenshotTest extends DualAppearanceBaseTest {

    @Override
    protected String baseName() {
        return "DesktopChromeTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected boolean useTexturedBackdrop() {
        // A popup pane and a tooltip are exactly the surfaces that are supposed to be
        // opaque. Over a flat form background a missing background colour is invisible;
        // over the texture it is the first thing a reviewer sees.
        return true;
    }

    @Override
    protected void populate(Form form, String suffix) {
        form.add(new Label("Context menu"));
        Container popup = new Container(new BorderLayout());
        popup.setUIID("PopupContentPane");
        Container items = new Container(BoxLayout.y());
        items.setUIID("CommandList");
        items.add(command("Cut", false));
        items.add(command("Copy", false));
        items.add(command("Paste", false));
        // Disabled is the one command state with a rule of its own in all three themes,
        // and a paste with nothing on the clipboard is where a real menu shows it.
        items.add(command("Paste special", true));
        popup.add(BorderLayout.CENTER, items);
        // Left-aligned and narrow: a menu is as wide as its widest item, and stretching it
        // across the form would hide a Command rule that sets its own text alignment.
        Container popupRow = new Container(new FlowLayout(Component.LEFT));
        popupRow.add(popup);
        form.add(popupRow);

        form.add(new Label("Tooltip"));
        Container tooltip = new Container(new BorderLayout());
        tooltip.setUIID("TooltipDialog");
        Label tip = new Label("Saves the current document");
        tip.setUIID("Tooltip");
        tooltip.add(BorderLayout.CENTER, tip);
        Container tooltipRow = new Container(new FlowLayout(Component.LEFT));
        tooltipRow.add(tooltip);
        form.add(tooltipRow);

        form.add(new Label("Dialog command area"));
        Container commands = new Container(new FlowLayout(Component.RIGHT));
        commands.setUIID("DialogCommandArea");
        Button cancel = new Button("Cancel");
        cancel.setUIID("DialogButton");
        Button ok = new Button("Save");
        ok.setUIID("DialogButtonDefault");
        commands.add(cancel);
        commands.add(ok);
        form.add(commands);
        annotateComponent(commands, "DialogCommandArea: default action distinguished from the rest");
    }

    private static Button command(String text, boolean disabledItem) {
        Button b = new Button(text);
        b.setUIID("Command");
        if (disabledItem) {
            b.setEnabled(false);
        }
        return b;
    }
}
