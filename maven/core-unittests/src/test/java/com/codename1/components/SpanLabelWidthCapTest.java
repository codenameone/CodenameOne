/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for the "capped dialog card clips its span text" bug:
 * a card whose preferred WIDTH is capped (the dialog width cap / the
 * DialogTheme screenshot mock) must grow its preferred HEIGHT for the
 * re-wrapped text, otherwise the message clips behind the command buttons
 * wherever the cap binds -- exactly what the 375px JavaScript-port screen
 * showed. SpanLabel.setPreferredW documents this contract ("we still want to
 * calculate the preferred height based on this preferred width").
 */
class SpanLabelWidthCapTest extends UITestBase {

    private static final String MESSAGE =
            "Are you sure you want to continue with this action? "
            + "This is a sample of a dialog body with a span label message.";

    @FormTest
    void cappedCardGrowsForWrappedText() {
        implementation.setDisplaySize(375, 667);

        Container dialog = new Container(new BorderLayout());
        Container body = new Container(BoxLayout.y());
        Label title = new Label("Example dialog");
        body.add(title);
        SpanLabel message = new SpanLabel(MESSAGE);
        body.add(message);
        Container commands = new Container(new FlowLayout(Component.RIGHT));
        commands.add(new Button("Cancel")).add(new Button("OK"));
        dialog.add(BorderLayout.CENTER, body).add(BorderLayout.SOUTH, commands);

        // NOTE: capping the dialog CONTAINER via Component.setPreferredW would
        // freeze its preferred HEIGHT at call time (setPreferredSize(w,
        // getPreferredH())) -- the unwrapped height. Cap the SpanLabel instead:
        // its setPreferredW keeps the height dynamic, and the card's width
        // follows its widest child.
        int cap = 375 * 72 / 100;
        message.setPreferredW(cap
                - dialog.getStyle().getHorizontalPadding()
                - body.getStyle().getHorizontalPadding());

        Form form = new Form(BoxLayout.y());
        Container center = new Container(new FlowLayout(Component.CENTER));
        center.add(dialog);
        form.add(center);
        form.show();
        DisplayTest.flushEdt();

        TextArea text = message.getTextComponent();
        // After layout the text wraps to its real (capped) width; the rows it
        // needs must FIT the height the layout actually granted it.
        int rows = text.getLines();
        int fontH = text.getStyle().getFont().getHeight();
        int needed = rows * (fontH + text.getRowsGap())
                + text.getStyle().getVerticalPadding();
        assertTrue(rows >= 2, "the cap must actually bind on a 375px screen (rows=" + rows + ")");
        assertTrue(text.getHeight() >= needed,
                "wrapped text clipped: rows=" + rows + " fontH=" + fontH
                + " needed=" + needed + " granted=" + text.getHeight()
                + " spanH=" + message.getHeight() + " dialogH=" + dialog.getHeight()
                + " dialogPrefH=" + dialog.getPreferredH());
        // And the command row must sit BELOW the text, not on top of it.
        assertTrue(commands.getAbsoluteY() >= text.getAbsoluteY() + needed,
                "commands overlap the wrapped text: commandsY=" + commands.getAbsoluteY()
                + " textY=" + text.getAbsoluteY() + " needed=" + needed);
    }
}
