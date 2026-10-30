/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.emojipicker;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Tabs;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;

import dart.core.DartList;

/** Builds the tabs, grids and backspace button of an {@link EmojiPicker}. */
final class EmojiGrid {
    private static final int COLUMNS = 8;

    private EmojiGrid() {
    }

    static Container create(final EmojiPicker picker) {
        Tabs tabs = new Tabs();
        DartList<Emoji> recent = EmojiData.recents();
        tabs.addTab("\u23F1", grid(picker, Category.RECENT, recent));
        for (CategoryEmoji c : EmojiData.all()) {
            tabs.addTab(c.emoji().get(0).emoji(), grid(picker, c.category(), c.emoji()));
        }
        if (recent.isEmpty()) {
            tabs.setSelectedIndex(1);
        }
        Button back = new Button("\u232B");
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                picker.backspace();
            }
        });
        Container root = new Container(new BorderLayout());
        root.add(BorderLayout.CENTER, tabs);
        root.add(BorderLayout.SOUTH, BorderLayout.east(back));
        return root;
    }

    private static Container grid(final EmojiPicker picker, final Category cat, DartList<Emoji> list) {
        int rows = Math.max(1, (list.size() + COLUMNS - 1) / COLUMNS);
        Container g = new Container(new GridLayout(rows, COLUMNS));
        g.setScrollableY(true);
        for (final Emoji e : list) {
            Button b = new Button(e.emoji());
            b.setUIID("Label");
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    picker.selected(cat, e);
                }
            });
            g.add(b);
        }
        return g;
    }
}
