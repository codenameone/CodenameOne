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
                if (keyCode == 75 || keyCode == 107) { status.setText("K received"); return; }
                super.keyReleased(keyCode);
            }
        };
        if (query.indexOf("review=accessiblelabels") >= 0) {
            TextArea area = new TextArea("Field content"); area.setName("internalFieldName"); area.setHint("Fallback hint");
            area.setAccessibilityText("Configured accessible label");
            Button semantics = new Button("Set semantic label"), associated = new Button("Use associated label"), clear = new Button("Clear accessible label");
            semantics.addActionListener(e -> area.getSemantics().setLabel("Updated semantic label"));
            associated.addActionListener(e -> { area.setAccessibilityText(null); area.setLabelForComponent(new Label("Associated label")); });
            clear.addActionListener(e -> { area.setAccessibilityText(null); area.setLabelForComponent(null); });
            form.addAll(area, semantics, associated, clear);
        } else if (query.indexOf("review=defaultselection") >= 0) {
            Label defaultLabel = new Label("Default selection label");
            Label explicitOn = new Label("Explicit selection on"); explicitOn.setTextSelectionEnabled(true);
            Label explicitOff = new Label("Explicit selection off"); explicitOff.setTextSelectionEnabled(false);
            TextArea area = new TextArea("Default readonly selection"); area.setEditable(false); area.setName("defaultSelectionArea");
            Button enable = new Button("Enable default selection"), disable = new Button("Disable default selection");
            enable.addActionListener(e -> TextSelection.setDefaultSelectable(true));
            disable.addActionListener(e -> TextSelection.setDefaultSelectable(false));
            form.addAll(enable, defaultLabel, explicitOn, explicitOff, area, disable);
        } else if (query.indexOf("review=dynamicdrag") >= 0) {
            Label label = new Label("Dynamic drag label");
            TextArea area = new TextArea("Dynamic drag area"); area.setName("dynamicDragArea");
            Button enable = new Button("Enable dragging"), disable = new Button("Disable dragging");
            enable.addActionListener(e -> { label.setDraggable(true); area.setDraggable(true); });
            disable.addActionListener(e -> { label.setDraggable(false); area.setDraggable(false); });
            form.addAll(enable, label, area, disable);
        } else if (query.indexOf("review=selectedstyles") >= 0) {
            TextArea area = new TextArea("ABBA"); area.setName("selectedStyleArea");
            Runnable applyStyle = () -> {
                if (query.indexOf("style=font") >= 0) {
                    Image glyphs = Image.createImage(24, 12, 0xffffffff);
                    area.getSelectedStyle().setFont(Font.createBitmapFont(glyphs, new int[] {0, 12}, new int[] {12, 12}, "AB"));
                } else if (query.indexOf("style=opacity") >= 0) {
                    area.getSelectedStyle().setOpacity(100);
                } else {
                    area.getSelectedStyle().setTextDecoration(com.codename1.ui.plaf.Style.TEXT_DECORATION_UNDERLINE);
                }
            };
            applyStyle.run();
            Button apply = new Button("Apply selected style"); apply.addActionListener(e -> applyStyle.run());
            Button edit = new Button("Edit selected style"); edit.addActionListener(e -> area.startEditingAsync());
            form.addAll(apply, area, edit);
        } else if (query.indexOf("review=legacysession") >= 0) {
            com.codename1.ui.layouts.LayeredLayout layout = new com.codename1.ui.layouts.LayeredLayout();
            Container layers = new Container(layout); layers.setPreferredH(160);
            TextArea area = new TextArea("Original model", 3, 24); area.setName("legacySessionArea");
            Button cover = new Button("Cover edge");
            layers.addAll(area, cover); layout.setInsets(area, "0"); layout.setInsets(cover, "0 0 auto auto");
            Button edit = new Button("Start legacy"), uncover = new Button("Remove cover"), stop = new Button("Finish legacy");
            edit.addActionListener(e -> area.startEditingAsync());
            uncover.addActionListener(e -> { layers.removeComponent(cover); layers.revalidate(); status.setText("Cover removed"); });
            stop.addActionListener(e -> Display.getInstance().stopEditing(area, () -> status.setText("Committed " + area.getText())));
            form.addAll(edit, layers, uncover, stop, status);
        } else if (query.indexOf("review=readonlykeys") >= 0) {
            TextArea area = new TextArea("Readonly shortcut text"); area.setName("readonlyKeys"); area.setEditable(false);
            if (query.indexOf("disabled=true") >= 0) area.setEnabled(false);
            form.addAll(new Button("Initial focus"), area, status);
        } else if (query.indexOf("review=optingate") >= 0) {
            Label label = new Label("Explicit framework selection"); label.setTextSelectionEnabled(true);
            form.add(label); form.getTextSelection().setEnabled(true);
        } else if (query.indexOf("review=dynamicconstraints") >= 0) {
            TextArea area = new TextArea("Secret value"); area.setSingleLineTextArea(true); area.setName("dynamicArea");
            Button password = new Button("Enable password"), done = new Button("Add done listener"), reset = new Button("Reset constraints");
            password.addActionListener(e -> area.setConstraint(TextArea.PASSWORD));
            done.addActionListener(e -> area.setDoneListener(event -> status.setText("Done received")));
            reset.addActionListener(e -> { area.setConstraint(TextArea.ANY); area.setDoneListener(null); });
            form.addAll(area, password, done, reset, status);
        } else if (query.indexOf("review=ancestorownership") >= 0) {
            Label label = new Label("Ancestor owned label");
            Container parent = BoxLayout.encloseY(label);
            parent.addPointerPressedListener(e -> status.setText("Parent pressed"));
            TextArea area = new TextArea("Ancestor owned area"); area.setName("ancestorOwnedArea"); parent.add(area);
            Button grab = new Button("Grab pointer"), focus = new Button("Focusable parent"), drag = new Button("Draggable parent"), reset = new Button("Release pointer");
            grab.addActionListener(e -> parent.setGrabsPointerEvents(true));
            focus.addActionListener(e -> parent.setFocusable(true));
            drag.addActionListener(e -> parent.setDraggable(true));
            reset.addActionListener(e -> { parent.setGrabsPointerEvents(false); parent.setFocusable(false); parent.setDraggable(false); });
            form.addAll(parent, grab, focus, drag, reset, status);
        } else if (query.indexOf("review=keyboardpadding") >= 0) {
            TextArea area = new TextArea("Keyboard layout"); area.setName("paddingArea");
            Button enable = new Button("Enable keyboard padding"), disable = new Button("Disable keyboard padding");
            enable.addActionListener(e -> form.setFormBottomPaddingEditingMode(true));
            disable.addActionListener(e -> form.setFormBottomPaddingEditingMode(false));
            form.addAll(area, enable, disable);
        } else if (query.indexOf("review=initiatingkeys") >= 0) {
            TextArea area = new TextArea(""); area.setName("initiatingArea"); area.setMaxSize(3);
            Button type = new Button("Type queued keys");
            type.addActionListener(e -> {
                area.keyReleased('A'); area.keyReleased('B'); area.keyReleased('C'); area.keyReleased('D');
                status.setText("Typed " + area.getText());
            });
            form.addAll(type, area, status);
        } else if (query.indexOf("review=elevated") >= 0) {
            com.codename1.ui.layouts.LayeredLayout layout = new com.codename1.ui.layouts.LayeredLayout();
            Container layers = new Container(layout); layers.setPreferredH(160);
            layers.getAllStyles().setSurface(true);
            Container earlier = new Container(new com.codename1.ui.layouts.BorderLayout());
            earlier.getAllStyles().setBgTransparency(0);
            Button cover = new Button("Elevated edge"); cover.getAllStyles().setElevation(5);
            cover.addActionListener(e -> status.setText("Elevated clicked"));
            earlier.add(com.codename1.ui.layouts.BorderLayout.NORTH, cover);
            TextArea area = new TextArea("Covered by elevated descendant", 3, 24); area.setName("elevatedArea");
            layers.addAll(earlier, area);
            layout.setInsets(earlier, "0 0 auto auto"); layout.setInsets(area, "0");
            form.addAll(layers, status);
        } else if (query.indexOf("review=runorder") >= 0) {
            Label first = new Label("Order alpha"), second = new Label("Order beta");
            Container labels = BoxLayout.encloseY(first, second);
            Button reverse = new Button("Reverse labels");
            reverse.addActionListener(e -> {
                Component moved = labels.getComponentAt(0);
                labels.removeComponent(moved); labels.add(moved); labels.revalidate();
            });
            form.addAll(labels, reverse);
        } else if (query.indexOf("review=pointerfocus") >= 0) {
            Label label = new Label("Focusable label"); label.setFocusable(true);
            TextArea area = new TextArea("Focusable readonly"); area.setEditable(false); area.setFocusable(true);
            area.setName("focusReadonly");
            com.codename1.ui.events.FocusListener listener = new com.codename1.ui.events.FocusListener() {
                public void focusGained(Component c) { status.setText(c == label ? "Label focused" : "Readonly focused"); }
                public void focusLost(Component c) { }
            };
            label.addFocusListener(listener); area.addFocusListener(listener);
            form.addAll(new Button("Initial focus"), label, area, status);
        } else if (query.indexOf("review=multialign") >= 0) {
            for (int alignment : new int[] {Component.CENTER, Component.BOTTOM}) {
                TextArea area = new TextArea("First line\nSecond line", 2, 24);
                area.setName(alignment == Component.CENTER ? "multiCenter" : "multiBottom");
                area.setGrowByContent(false); area.setPreferredH(160); area.setEditable(false);
                area.setVerticalAlignment(alignment); area.setRowsGap(7);
                area.getAllStyles().setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
                area.getAllStyles().setPadding(11, 19, 5, 5);
                form.add(area);
            }
        } else if (query.indexOf("review=opacity") >= 0) {
            TextArea area = new TextArea("Fading field"); area.setName("opacityArea");
            Container parent = BoxLayout.encloseY(area);
            Button fade = new Button("Fade field"), ancestor = new Button("Fade ancestor"), reset = new Button("Reset opacity");
            fade.addActionListener(e -> area.getAllStyles().setOpacity(100));
            ancestor.addActionListener(e -> parent.getAllStyles().setOpacity(100));
            reset.addActionListener(e -> { area.getAllStyles().setOpacity(255); parent.getAllStyles().setOpacity(255); });
            form.addAll(parent, fade, ancestor, reset);
        } else if (query.indexOf("review=uniqueeditor") >= 0) {
            TextArea area = new TextArea("Single accessible field"); area.setName("uniqueEditor");
            Button hide = new Button("Canvas fallback"), restore = new Button("Restore editor");
            hide.addActionListener(e -> area.setEndsWith3Points(true));
            restore.addActionListener(e -> area.setEndsWith3Points(false));
            Button disable = new Button("Disable editor"), enable = new Button("Enable editor");
            disable.addActionListener(e -> area.setEnabled(false)); enable.addActionListener(e -> area.setEnabled(true));
            form.addAll(area, hide, restore, disable, enable);
        } else if (query.indexOf("review=traversal") >= 0) {
            TextField first = new TextField("First"); first.setName("tabFirst");
            TextField last = new TextField("Last"); last.setName("tabLast");
            form.addAll(first, new Button("Middle button"), new CheckBox("Middle check"), last);
        } else if (query.indexOf("review=occlusion") >= 0) {
            com.codename1.ui.layouts.LayeredLayout layout = new com.codename1.ui.layouts.LayeredLayout();
            Container layers = new Container(layout);
            layers.setPreferredH(160);
            TextArea area = new TextArea("Partly covered text", 3, 24);
            area.setName("coveredArea");
            Button cover = new Button("Edge button");
            cover.addActionListener(e -> status.setText("Edge button clicked"));
            layers.addAll(area, cover);
            layout.setInsets(area, "0");
            layout.setInsets(cover, "0 0 auto auto");
            form.addAll(layers, status);
        } else if (query.indexOf("review=interactionstate") >= 0) {
            Container context = new Container(BoxLayout.y());
            Container stylus = new Container(BoxLayout.y());
            Container commands = new Container(BoxLayout.y());
            Container[] parents = {context, stylus, commands};
            String[] names = {"dynamicContext", "dynamicStylus", "dynamicCommands"};
            for (int i = 0; i < parents.length; i++) {
                TextArea area = new TextArea(names[i]); area.setName(names[i]);
                parents[i].addAll(new Label(names[i] + " label"), area);
                form.add(parents[i]);
            }
            com.codename1.ui.events.ActionListener handler = e -> { };
            com.codename1.ui.util.UITimer.timer(3000, false, form, () -> {
                context.addContextMenuListener(handler);
                stylus.addStylusListener(handler);
                commands.setContextMenuCommands(new Command("Menu"));
            });
            com.codename1.ui.util.UITimer.timer(6000, false, form, () -> {
                context.removeContextMenuListener(handler);
                stylus.removeStylusListener(handler);
                commands.setContextMenuCommands((Command[]) null);
            });
        } else if (query.indexOf("review=accessiblename") >= 0) {
            TextField field = new TextField("", "Original hint");
            field.setName("Original name");
            com.codename1.ui.util.UITimer.timer(3000, false, form, () -> field.setName("Updated name"));
            com.codename1.ui.util.UITimer.timer(5000, false, form, () -> field.setName(""));
            com.codename1.ui.util.UITimer.timer(7000, false, form, () -> field.setHint("Updated hint"));
            form.add(field);
        } else if (query.indexOf("review=maxsize") >= 0) {
            TextField field = new TextField(""); field.setName("limitedField"); field.setMaxSize(20);
            com.codename1.ui.util.UITimer.timer(3000, false, form, () -> field.setMaxSize(3));
            Button check = new Button("Check maximum");
            check.addActionListener(e -> status.setText("Maximum " + field.getMaxSize() + " value " + field.getText()));
            form.addAll(field, check, status);
        } else if (query.indexOf("review=subclasses") >= 0) {
            TextArea area = new TextArea("Custom area") {
                public void pointerPressed(int x, int y) { status.setText("Custom area pressed"); }
            };
            TextField field = new TextField("Custom field") {
                public void pointerPressed(int x, int y) { status.setText("Custom field pressed"); }
            };
            area.setName("customArea"); field.setName("customField");
            form.addAll(area, field, status);
        } else if (query.indexOf("review=ellipsis") >= 0) {
            TextArea area = new TextArea("First row\nSecond row\nThird row\nFourth row", 2, 24);
            area.setName("ellipsisArea"); area.setEditable(false);
            area.setGrowByContent(true); area.setGrowLimit(2); area.setEndsWith3Points(true);
            form.add(area);
        } else if (query.indexOf("review=ownership") >= 0) {
            TextField field = new TextField("Active session");
            field.setName("ownershipField");
            TextField password = new TextField("", "Password", 20, TextArea.PASSWORD);
            password.setName("legacyPassword");
            Button readonly = new Button("Make readonly");
            readonly.addActionListener(e -> field.setEditable(false));
            Button disable = new Button("Disable field");
            disable.addActionListener(e -> field.setEnabled(false));
            Button editPassword = new Button("Edit password");
            editPassword.addActionListener(e -> password.startEditingAsync());
            form.addAll(field, password, readonly, disable, editPassword);
        } else if (query.indexOf("review=numeric") >= 0) {
            TextField field = new TextField("Not a number", "", 20, TextArea.NUMERIC);
            field.setName("numericModel");
            Button change = new Button("Change numeric model");
            change.addActionListener(e -> field.setText("Still not numeric"));
            form.addAll(field, change);
        } else if (query.indexOf("review=linemode") >= 0) {
            TextArea area = new TextArea("Changing line mode", 2, 24);
            area.setName("changingLineMode");
            com.codename1.ui.util.UITimer.timer(3000, false, form, () -> area.setSingleLineTextArea(true));
            com.codename1.ui.util.UITimer.timer(5000, false, form, () -> area.setSingleLineTextArea(false));
            form.add(area);
        } else if (query.indexOf("review=canvasstyles") >= 0) {
            Image glyphs = Image.createImage(24, 12, 0xffffffff);
            Font bitmap = Font.createBitmapFont(glyphs, new int[] {0, 12}, new int[] {12, 12}, "AB");
            TextArea area = new TextArea("ABBA");
            area.setName("bitmapArea");
            area.getAllStyles().setFont(bitmap);
            area.setEditable(false);
            form.add(area);
            int[] decorations = {1, 2, 4, 7, 8, 16, 32};
            for (int decoration : decorations) {
                TextArea decorated = new TextArea("Decoration " + decoration);
                decorated.setName("decoration" + decoration);
                decorated.setEditable(false);
                form.add(decorated);
            }
            Button apply = new Button("Apply decorations");
            apply.addActionListener(e -> {
                // Apply after startup so deferred theme initialization has completed.
                for (int i = 0; i < decorations.length; i++) {
                    Component decorated = form.getContentPane().getComponentAt(i + 1);
                    decorated.getUnselectedStyle().setTextDecoration(decorations[i]);
                    decorated.getSelectedStyle().setTextDecoration(decorations[i]);
                }
                form.repaint();
            });
            form.add(apply);
        } else if (query.indexOf("review=metadata") >= 0) {
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
            if (query.indexOf("interactive=true") >= 0) form.add(new Button("Initial focus"));
            form.addAll(area, position, reset);
            if (query.indexOf("interactive=true") >= 0) {
                area.setRTL(query.indexOf("rtl=true") >= 0);
                Button configure = new Button("Configure scrollbar");
                configure.addActionListener(e -> {
                    java.util.Hashtable props = new java.util.Hashtable();
                    props.put("@interactiveScrollBool", "true");
                    props.put("DesktopScroll.padding", "0,0,8,8");
                    props.put("DesktopScroll.padUnit", new byte[] {0, 0, 0, 0});
                    com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(props);
                    com.codename1.ui.plaf.UIManager.getInstance().getLookAndFeel().setFadeScrollBar(false);
                    form.refreshTheme(); form.revalidate();
                    com.codename1.ui.util.UITimer.timer(25, true, form, () -> {
                        if (area.isVScrollThumbGrabbed()) status.setText("Thumb grabbed");
                    });
                });
                form.addAll(configure, status);
            }
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
