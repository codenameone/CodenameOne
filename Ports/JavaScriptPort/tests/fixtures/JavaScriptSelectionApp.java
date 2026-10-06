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

import com.codename1.impl.html5.JavaScriptPortBootstrap;
import com.codename1.system.Lifecycle;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;

/** Browser regression fixture for issues 5943, 5944 and 5946. */
public class JavaScriptSelectionApp extends Lifecycle {
    public static void main(String[] args) {
        JavaScriptPortBootstrap.bootstrap(new JavaScriptSelectionApp());
    }

    @Override
    public void init(Object context) {
        String query = Display.getInstance().getProperty("browser.window.location.search", "");
        Display.getInstance().setProperty("javascript.textSelection", query.indexOf("selection=off") >= 0 ? "false" : "true");
        // The initial default Font wraps a null native handle. Android theme
        // initialization measures this before replacing the default (#5943).
        Font font = Font.getDefaultFont();
        if (font.getHeight() <= 0 || font.stringWidth("Default font") <= 0
                || font.charWidth('M') <= 0 || font.charsWidth(new char[] {'M'}, 0, 1) <= 0) {
            throw new IllegalStateException("Default font metrics failed");
        }
        super.init(context);
    }

    @Override
    public void runApp() {
        String query = Display.getInstance().getProperty("browser.window.location.search", "");
        if (query.indexOf("review=") >= 0) {
            showReviewFixture(query);
            return;
        }
        Form form = new Form("Selection regressions", BoxLayout.y());
        TextField title = new TextField("Title");
        title.setName("selectionTitle");
        TextArea notes = new TextArea("Caffè, perché, città, più, però.\nSecond paragraph with words.", 4, 24);
        notes.setName("selectionNotes");
        notes.setMaxSize(4096);
        notes.setGrowByContent(true);
        TextArea readOnly = new TextArea("First paragraph wraps across several lines so copying must preserve spaces.\nSecond paragraph remains separate.", 4, 24);
        readOnly.setEditable(false);
        readOnly.setName("selectionReadOnly");
        Label status = new Label("Ready");
        Button action = new Button("Run action");
        action.addActionListener(e -> status.setText("Action fired"));
        Button dialog = new Button("Open dialog");
        dialog.addActionListener(e -> Dialog.show("Selection dialog", "Modal text", "OK", null));
        form.addAll(title, notes, readOnly, action, dialog, status);
        TextArea appOwned = new TextArea("Application-owned interaction");
        appOwned.setName("selectionAppOwned");
        appOwned.addActionListener(e -> status.setText("Custom action fired"));
        form.add(appOwned);
        TextArea notSelectable = new TextArea("Selection explicitly disabled");
        notSelectable.setName("selectionDisabled");
        notSelectable.setEditable(false);
        notSelectable.setTextSelectionEnabled(false);
        form.add(notSelectable);
        for (int i = 0; i < 18; i++) form.add(new Label("Scrollable row " + i));
        form.show();
    }

    private void showReviewFixture(String query) {
        final Label status = new Label("Shortcut ready");
        Form form = new Form("Review regressions", BoxLayout.y()) {
            public void keyReleased(int keyCode) {
                // Application-level shortcuts can consume the key before it is
                // interpreted as input by the focused TextField.
                if (keyCode == 27) { status.setText("Escape received"); return; }
                if (keyCode == 113) { status.setText("F2 received"); return; }
                super.keyReleased(keyCode);
            }
        };
        if (query.indexOf("review=metadata") >= 0) {
            TextField field = new TextField("123", "", 20, TextArea.EMAILADDR);
            field.setName("reviewField");
            field.setPreferredH(200);
            field.setVerticalAlignment(Component.BOTTOM);
            field.getAllStyles().setFgColor(0x123456);
            field.getAllStyles().setFgAlpha(96);
            Button change = new Button("Change constraints");
            change.addActionListener(e -> {
                field.setConstraint(TextArea.NUMERIC | TextArea.SENSITIVE);
                field.repaint();
            });
            Button reset = new Button("Reset constraints");
            reset.addActionListener(e -> {
                field.setConstraint(TextArea.ANY | TextArea.INITIAL_CAPS_WORD);
                field.putClientProperty("cn1$autocomplete", "nickname");
                field.repaint();
            });
            form.addAll(field, change, reset, status);
        } else if (query.indexOf("review=scroll") >= 0) {
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i < 30; i++) lines.append("Text row ").append(i).append("\n");
            TextArea area = new TextArea(lines.toString(), 3, 24);
            area.setName("reviewScroll");
            area.setGrowByContent(false);
            area.setPreferredH(110);
            area.setSmoothScrolling(false);
            Label position = new Label("Scroll Y 0");
            area.addScrollListener((x, y, oldX, oldY) -> position.setText("Scroll Y " + y));
            Button reset = new Button("Reset scroll");
            reset.addActionListener(e -> com.codename1.ui.Accessor.setNativeTextScrollY(area, 0));
            form.addAll(area, position, reset);
        } else {
            Label normal = new Label("Plain selectable label");
            Label custom = new Label("Custom pointer label") {
                public void pointerReleased(int x, int y) { setText("Custom pointer fired"); }
            };
            custom.setFocusable(true);
            Label draggable = new Label("Draggable label");
            draggable.setDraggable(true);
            Slider slider = new Slider();
            slider.setEditable(true);
            slider.setRenderPercentageOnTop(true);
            slider.setProgress(50);
            slider.setName("reviewSlider");
            Container horizontal = new Container(BoxLayout.x());
            horizontal.setScrollableX(true);
            horizontal.setScrollableY(false);
            for (int i = 0; i < 10; i++) horizontal.add(new Label("Horizontal item " + i));
            form.addAll(normal, custom, draggable, slider, horizontal);
        }
        form.show();
    }
}
