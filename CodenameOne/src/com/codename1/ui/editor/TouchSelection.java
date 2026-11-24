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

package com.codename1.ui.editor;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;

/// The touch selection chrome a text editor shows after a long press: a draggable handle at
/// each end of the selection and a context toolbar with Copy, Cut, Paste and Select All.
///
/// The editor keeps the selection; this class only paints the chrome and says what a press
/// landed on. `EditorView` and the word processing `DocumentView` share it, so both editors
/// look and behave the same under a finger.
///
/// The host shows the chrome with `setShown(true)` after a long press selects a word, calls
/// `paint` after painting its content while a selection exists, and hands every press to
/// `press` before handling it itself.
public final class TouchSelection {
    /// `press` hit nothing; the chrome was dismissed.
    public static final int MISSED = 0;

    /// `press` hit a toolbar button, whose action the host has already run.
    public static final int TOOLBAR = 1;

    /// `press` grabbed the handle at the start of the selection.
    public static final int START_HANDLE = 2;

    /// `press` grabbed the handle at the end of the selection.
    public static final int END_HANDLE = 3;

    /// Toolbar action: copy the selection.
    public static final int COPY = 0;

    /// Toolbar action: cut the selection.
    public static final int CUT = 1;

    /// Toolbar action: paste over the selection.
    public static final int PASTE = 2;

    /// Toolbar action: select everything.
    public static final int SELECT_ALL = 3;

    private static final String[] LABELS = {"Copy", "Cut", "Paste", "Select All"};
    private static final int COLOR = 0x1a73e8;

    /// What the chrome needs from the editor it decorates.
    public interface Host {
        /// Where an end of the selection is, in coordinates relative to the editor's own
        /// origin.
        ///
        /// #### Parameters
        ///
        /// - `start`: true for the start of the selection, false for its end
        ///
        /// #### Returns
        ///
        /// `{x, top, height}` of the caret at that end
        int[] edgeRect(boolean start);

        /// Whether Cut and Paste are offered.
        ///
        /// #### Returns
        ///
        /// true when the content can be changed
        boolean canEdit();

        /// Runs a toolbar action.
        ///
        /// #### Parameters
        ///
        /// - `action`: `COPY`, `CUT`, `PASTE` or `SELECT_ALL`
        void toolbarAction(int action);

        /// The font the toolbar labels are drawn in and the handles are sized from.
        ///
        /// #### Returns
        ///
        /// the font
        Font chromeFont();
    }

    private final Host host;
    private boolean shown;
    private int[] startHandle;
    private int[] endHandle;
    private int[][] buttons;
    private int[] actions;

    /// Creates the chrome for an editor.
    ///
    /// #### Parameters
    ///
    /// - `host`: the editor
    public TouchSelection(Host host) {
        this.host = host;
    }

    /// Whether the chrome is showing.
    ///
    /// #### Returns
    ///
    /// true while shown
    public boolean isShown() {
        return shown;
    }

    /// Shows or dismisses the chrome.
    ///
    /// #### Parameters
    ///
    /// - `value`: true to show it
    public void setShown(boolean value) {
        shown = value;
        if (!value) {
            startHandle = null;
            endHandle = null;
            buttons = null;
        }
    }

    private int radius() {
        return Math.max(6, host.chromeFont().getHeight() / 3);
    }

    /// Paints the handles and the toolbar when shown. Call after painting the content, and only
    /// while there is a selection.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics
    ///
    /// - `owner`: the editor, whose position the coordinates are relative to
    public void paint(Graphics g, Component owner) {
        if (!shown) {
            return;
        }
        int r = radius();
        int[] s = host.edgeRect(true);
        int[] e = host.edgeRect(false);
        int x = owner.getX();
        int y = owner.getY();
        g.setColor(COLOR);
        g.fillRect(x + s[0] - 1, y + s[1], 2, s[2]);
        g.fillArc(x + s[0] - r, y + s[1] - r * 2, r * 2, r * 2, 0, 360);
        startHandle = new int[] {s[0], s[1] - r};
        g.fillRect(x + e[0] - 1, y + e[1], 2, e[2]);
        g.fillArc(x + e[0] - r, y + e[1] + e[2], r * 2, r * 2, 0, 360);
        endHandle = new int[] {e[0], e[1] + e[2] + r};
        paintToolbar(g, owner, s, r);
    }

    private void paintToolbar(Graphics g, Component owner, int[] anchor, int r) {
        Font f = host.chromeFont();
        boolean edit = host.canEdit();
        int count = edit ? 4 : 2;
        actions = edit ? new int[] {COPY, CUT, PASTE, SELECT_ALL} : new int[] {COPY, SELECT_ALL};
        int pad = f.charWidth('m');
        int bh = f.getHeight() + pad;
        int[] w = new int[count];
        int total = 0;
        for (int i = 0; i < count; i++) {
            w[i] = f.stringWidth(LABELS[actions[i]]) + pad * 2;
            total += w[i];
        }
        int tx = Math.max(0, Math.min(anchor[0] - total / 2, owner.getWidth() - total));
        int ty = anchor[1] - bh - r * 2;
        if (ty < 0) {
            ty = anchor[1] + anchor[2] + r * 2;
        }
        int x = owner.getX();
        int y = owner.getY();
        g.setColor(0x222222);
        g.fillRect(x + tx, y + ty, total, bh);
        buttons = new int[count][];
        int cx = tx;
        g.setFont(f);
        for (int i = 0; i < count; i++) {
            buttons[i] = new int[] {cx, ty, w[i], bh};
            g.setColor(0xf0f0f0);
            g.drawString(LABELS[actions[i]], x + cx + pad, y + ty + pad / 2);
            cx += w[i];
            if (i < count - 1) {
                g.setColor(0x555555);
                g.drawLine(x + cx, y + ty + 4, x + cx, y + ty + bh - 4);
            }
        }
    }

    /// Hit tests a press. A toolbar press runs its action; a press on neither handle nor
    /// toolbar dismisses the chrome.
    ///
    /// #### Parameters
    ///
    /// - `owner`: the editor
    ///
    /// - `x`: the absolute x
    ///
    /// - `y`: the absolute y
    ///
    /// #### Returns
    ///
    /// `MISSED`, `TOOLBAR`, `START_HANDLE` or `END_HANDLE`
    public int press(Component owner, int x, int y) {
        if (!shown) {
            return MISSED;
        }
        int lx = x - owner.getAbsoluteX();
        int ly = y - owner.getAbsoluteY();
        if (buttons != null) {
            for (int i = 0; i < buttons.length; i++) {
                int[] b = buttons[i];
                if (lx >= b[0] && lx <= b[0] + b[2] && ly >= b[1] && ly <= b[1] + b[3]) {
                    int action = actions[i];
                    host.toolbarAction(action);
                    setShown(action == SELECT_ALL);
                    owner.repaint();
                    return TOOLBAR;
                }
            }
        }
        if (near(startHandle, lx, ly)) {
            return START_HANDLE;
        }
        if (near(endHandle, lx, ly)) {
            return END_HANDLE;
        }
        setShown(false);
        return MISSED;
    }

    /// The last painted bounds of a toolbar button, relative to the editor; for tests.
    int[] buttonBounds(int index) {
        return buttons == null || index >= buttons.length ? null : buttons[index];
    }

    private boolean near(int[] handle, int lx, int ly) {
        if (handle == null) {
            return false;
        }
        int r = radius() * 3;
        int dx = lx - handle[0];
        int dy = ly - handle[1];
        return dx * dx + dy * dy <= r * r;
    }
}
