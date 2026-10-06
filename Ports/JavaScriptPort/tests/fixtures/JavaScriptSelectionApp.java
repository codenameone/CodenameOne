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
        } else if (query.indexOf("review=selectionstate") >= 0) {
            Label selectable = new Label("Toggle selection label");
            TextArea readOnly = new TextArea("Toggle readonly selection");
            readOnly.setName("toggleReadOnly");
            readOnly.setEditable(false);
            // Only the flag setters repaint: no buttons, focus changes or status updates.
            com.codename1.ui.util.UITimer.timer(3000, false, form, () -> selectable.setTextSelectionEnabled(false));
            com.codename1.ui.util.UITimer.timer(5000, false, form, () -> selectable.setTextSelectionEnabled(true));
            com.codename1.ui.util.UITimer.timer(7000, false, form, () -> readOnly.setTextSelectionEnabled(false));
            com.codename1.ui.util.UITimer.timer(9000, false, form, () -> readOnly.setTextSelectionEnabled(true));
            com.codename1.ui.util.UITimer.timer(11000, false, form, () -> form.getTextSelection().setEnabled(false));
            com.codename1.ui.util.UITimer.timer(13000, false, form, () -> form.getTextSelection().setEnabled(true));
            form.addAll(selectable, readOnly);
        } else if (query.indexOf("review=editable") >= 0) {
            TextArea area = new TextArea("Static editability");
            area.setName("toggleEditable");
            com.codename1.ui.util.UITimer.timer(3000, false, form, () -> area.setEditable(false));
            com.codename1.ui.util.UITimer.timer(5000, false, form, () -> area.setEditable(true));
            form.add(area);
        } else if (query.indexOf("review=focus") >= 0) {
            TextArea area = new TextArea("Readonly focus");
            area.setName("focusReadOnly");
            area.setEditable(false);
            TextArea disabled = new TextArea("Disabled focus");
            disabled.setName("focusDisabled");
            disabled.setEnabled(false);
            Button target = new Button("Focus destination");
            Button move = new Button("Move focus");
            move.addActionListener(e -> target.requestFocus());
            form.addAll(area, disabled, target, move);
        } else if (query.indexOf("review=formpointer") >= 0) {
            Label label = new Label("Form pointer label");
            TextArea area = new TextArea("Form pointer area");
            area.setEditable(false);
            area.setName("formPointerArea");
            if (query.indexOf("handler=release") >= 0) {
                form.addPointerReleasedListener(e -> status.setText("Form release received"));
            } else if (query.indexOf("handler=drag") >= 0) {
                form.addPointerDraggedListener(e -> status.setText("Form drag received"));
            } else if (query.indexOf("handler=long") >= 0) {
                form.addLongPressListener(e -> status.setText("Form long received"));
            } else {
                form.addPointerPressedListener(e -> status.setText("Form press received"));
            }
            form.addAll(label, area, status);
        } else if (query.indexOf("review=snapshot") >= 0) {
            TextField field = new TextField("Snapshot field value");
            field.setName("snapshotField");
            TextArea area = new TextArea("Snapshot area value", 2, 24);
            area.setName("snapshotArea");
            Button capture = new Button("Capture text images");
            capture.addActionListener(e -> {
                boolean fieldText = snapshotContainsText(field);
                boolean areaText = snapshotContainsText(area);
                status.setText(fieldText && areaText ? "Snapshots contain text" : "Snapshot text missing");
            });
            form.addAll(field, area, capture, status);
        } else if (query.indexOf("review=stylus") >= 0) {
            Label direct = new Label("Stylus listener label");
            direct.addStylusListener(e -> status.setText("Stylus received"));
            Container parent = new Container(BoxLayout.y());
            parent.addStylusListener(e -> status.setText("Inherited stylus received"));
            parent.add(new Label("Inherited stylus label"));
            TextArea area = new TextArea("Stylus listener area");
            area.setName("stylusArea");
            area.addStylusListener(e -> status.setText("Area stylus received"));
            form.addAll(direct, parent, area, status);
        } else if (query.indexOf("review=rendering") >= 0) {
            TextField hint = new TextField("", "Styled empty hint");
            hint.setName("reviewHint");
            hint.getHintLabel().getAllStyles().setFgColor(0x654321);
            TextArea multiline = new TextArea("", 3, 24);
            multiline.setName("reviewMultilineHint");
            multiline.setHint("Multiline empty hint");
            Button clear = new Button("Clear hint field");
            clear.addActionListener(e -> hint.setText(""));
            form.addAll(hint, multiline, clear);
            for (int align : new int[] {Component.LEFT, Component.RIGHT, Component.CENTER}) {
                TextField rtl = new TextField("שלום");
                rtl.setName("rtl" + align);
                rtl.setRTL(true);
                rtl.getAllStyles().setAlignment(align);
                form.add(rtl);
            }
        } else if (query.indexOf("review=exclusions") >= 0) {
            TextField.setUseNativeTextInput(false);
            TextField lightweight = new TextField("Lightweight value");
            lightweight.setName("reviewLightweight");
            Label menu = new Label("Context listener label");
            menu.addContextMenuListener(e -> { status.setText("Context received"); e.consume(); });
            TextArea commands = new TextArea("Context command area");
            commands.setName("reviewContextCommands");
            commands.setContextMenuCommands(new Command("Application command"));
            Container inherited = new Container(BoxLayout.y());
            inherited.addContextMenuListener(e -> { status.setText("Inherited context received"); e.consume(); });
            inherited.add(new Label("Inherited context label"));
            form.addAll(lightweight, menu, commands, inherited, status);
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
            // Keep the scrollbar from overlapping the glyphs: overlapping canvas
            // decoration correctly demotes those runs out of the native text layer.
            horizontal.setScrollVisible(false);
            TextArea horizontalText = new TextArea("Horizontal selectable text", 2, 30);
            horizontalText.setName("horizontalText");
            horizontalText.setEditable(false);
            horizontalText.setPreferredW(Display.getInstance().getDisplayWidth() * 2);
            horizontalText.setPreferredH(CN.convertToPixels(12));
            horizontal.add(horizontalText);
            form.addAll(normal, custom, draggable, slider, horizontal);
        }
        form.show();
    }
    private boolean snapshotContainsText(TextArea area) {
        String value = area.getText();
        int[] withText = area.toImage().getRGB();
        area.setText("");
        int[] withoutText = area.toImage().getRGB();
        area.setText(value);
        int changed = 0;
        for (int i = 0; i < withText.length; i++) {
            if (withText[i] != withoutText[i]) changed++;
        }
        return changed > 20;
    }

}
