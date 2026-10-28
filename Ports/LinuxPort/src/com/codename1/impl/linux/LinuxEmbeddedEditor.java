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
package com.codename1.impl.linux;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;

/// Text editing on the embedded Linux target, where there is no native text
/// widget and usually no physical keyboard. A field being edited opens a
/// dialog holding a lightweight copy of it and an on-screen keyboard; a
/// physical keyboard types into the same field, so a panel with one works the
/// same way. Done delivers the text through `Display.onEditingComplete`,
/// Cancel restores the original.
final class LinuxEmbeddedEditor {
    private static final String[][] LETTERS = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l", "'"},
        {"z", "x", "c", "v", "b", "n", "m", ",", ".", "-"}
    };
    private static final String[][] SYMBOLS = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"@", "#", "$", "%", "&", "*", "(", ")", "/", ":"},
        {"!", "?", ";", "\"", "+", "=", "_", "<", ">", "~"}
    };

    private LinuxEmbeddedEditor() {
    }

    static void edit(final Component cmp, int maxSize, int constraint, final String text) {
        final boolean multiLine = cmp instanceof TextArea && !((TextArea) cmp).isSingleLineTextArea();
        final TextField field = new TextField(text == null ? "" : text);
        field.setConstraint(constraint);
        if (maxSize > 0) {
            field.setMaxSize(maxSize);
        }
        if (multiLine) {
            field.setSingleLineTextArea(false);
            field.setRows(3);
        }
        final Dialog dlg = new Dialog(new BorderLayout());
        final boolean[] done = new boolean[1];
        final Container keys = new Container(BoxLayout.y());
        final boolean[] shift = new boolean[1];
        final boolean[] symbols = new boolean[1];
        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = new Runnable() {
            @Override
            public void run() {
                keys.removeAll();
                String[][] rows = symbols[0] ? SYMBOLS : LETTERS;
                for (int r = 0; r < rows.length; r++) {
                    Container row = new Container(new GridLayout(1, rows[r].length));
                    for (int k = 0; k < rows[r].length; k++) {
                        String label = rows[r][k];
                        if (shift[0] && !symbols[0]) {
                            label = upper(label);
                        }
                        row.add(key(label, field, null));
                    }
                    keys.add(row);
                }
                Container last = new Container(new GridLayout(1, 5));
                last.add(key(shift[0] ? "SHIFT*" : "Shift", field, new Runnable() {
                    @Override
                    public void run() {
                        shift[0] = !shift[0];
                        rebuild[0].run();
                    }
                }));
                last.add(key(symbols[0] ? "abc" : "?123", field, new Runnable() {
                    @Override
                    public void run() {
                        symbols[0] = !symbols[0];
                        rebuild[0].run();
                    }
                }));
                last.add(key("Space", field, null));
                last.add(key("Del", field, null));
                last.add(key(multiLine ? "Enter" : "Done", field, multiLine ? null : new Runnable() {
                    @Override
                    public void run() {
                        done[0] = true;
                        dlg.dispose();
                    }
                }));
                keys.add(last);
                keys.revalidate();
            }
        };
        rebuild[0].run();
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.CENTER, field);
        Button ok = new Button("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                done[0] = true;
                dlg.dispose();
            }
        });
        Button cancel = new Button("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                dlg.dispose();
            }
        });
        Container buttons = new Container(new GridLayout(1, 2));
        buttons.add(cancel);
        buttons.add(ok);
        top.add(BorderLayout.EAST, buttons);
        dlg.add(BorderLayout.NORTH, top);
        dlg.add(BorderLayout.CENTER, keys);
        field.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (!multiLine) {
                    done[0] = true;
                    dlg.dispose();
                }
            }
        });
        dlg.setDisposeWhenPointerOutOfBounds(false);
        dlg.setScrollable(false);
        dlg.getContentPane().setPreferredH(Display.getInstance().getDisplayHeight() / 2);
        // Modal: the port is in synchronous edit mode, so this blocks the caller
        // (through invokeAndBlock, the EDT keeps running) until the dialog closes.
        dlg.showPacked(BorderLayout.SOUTH, true);
        Display.getInstance().onEditingComplete(cmp, done[0] ? field.getText() : text);
    }

    private static Button key(final String label, final TextField field, final Runnable action) {
        Button b = new Button(label);
        b.setUIID("VKBKey");
        b.setFocusable(false);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (action != null) {
                    action.run();
                    return;
                }
                String t = field.getText();
                int caret = field.getCursorPosition();
                if (caret < 0 || caret > t.length()) {
                    caret = t.length();
                }
                String insert;
                if ("Del".equals(label)) {
                    if (caret == 0) {
                        return;
                    }
                    field.setText(t.substring(0, caret - 1) + t.substring(caret));
                    field.setCursorPosition(caret - 1);
                    return;
                } else if ("Space".equals(label)) {
                    insert = " ";
                } else if ("Enter".equals(label)) {
                    insert = "\n";
                } else {
                    insert = label;
                }
                if (field.getMaxSize() > 0 && t.length() + insert.length() > field.getMaxSize()) {
                    return;
                }
                field.setText(t.substring(0, caret) + insert + t.substring(caret));
                field.setCursorPosition(caret + insert.length());
            }
        });
        return b;
    }

    private static String upper(String s) {
        char c = s.charAt(0);
        if (c >= 'a' && c <= 'z') {
            return String.valueOf((char) (c - 'a' + 'A'));
        }
        return s;
    }
}
