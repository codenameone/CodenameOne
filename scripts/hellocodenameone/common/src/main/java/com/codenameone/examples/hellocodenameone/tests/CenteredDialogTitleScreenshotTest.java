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

import com.codename1.components.SpanLabel;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.UIManager;

/**
 * Screenshot coverage for a real {@link Dialog} using the centered-title
 * runtime flag. The dialog is laid out at its preferred packed size and placed
 * in the middle of the host form.
 */
public class CenteredDialogTitleScreenshotTest extends DualAppearanceBaseTest {
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=CenteredDialogTitle status=SKIPPED reason=phone-dialog-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "CenteredDialogTitle";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected boolean useTexturedBackdrop() {
        return true;
    }

    @Override
    protected void populate(Form form, String suffix) {
        Dialog dialog = new Dialog("Delete Conversation?", BoxLayout.y());
        dialog.setTitleCentered(true);
        dialog.placeButtonCommands(new Command[]{
                new Command("Cancel"),
                new Command("Delete")
        });

        SpanLabel message = new SpanLabel(
                "This conversation will be removed from all of your devices.");
        message.setUIID("DialogBody");
        int maxPercent = UIManager.getInstance().getThemeConstant("dialogMaxWidthPercentInt", 72);
        int maxWidth = Display.getInstance().getDisplayWidth() * maxPercent / 100;
        message.setPreferredW(maxWidth
                - dialog.getDialogStyle().getHorizontalPadding()
                - dialog.getContentPane().getStyle().getHorizontalPadding());
        dialog.add(message);

        Container center = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        center.add(dialog);
        form.add(BorderLayout.CENTER, center);
    }
}
