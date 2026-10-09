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
package com.codename1.desktopcompat.org.fife.ui.rtextarea;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.javax.swing.JScrollPane;

/// A scroll pane for a [RTextArea] with a [Gutter] of line numbers at its
/// left, which scrolls with the text.
public class RTextScrollPane extends JScrollPane {

    private Gutter gutter;

    public RTextScrollPane() {
        this(null, true);
    }

    public RTextScrollPane(RTextArea textArea) {
        this(textArea, true);
    }

    public RTextScrollPane(Component comp) {
        this(comp, true);
    }

    public RTextScrollPane(RTextArea textArea, boolean lineNumbers) {
        this(textArea, lineNumbers, null);
    }

    public RTextScrollPane(Component comp, boolean lineNumbers) {
        this(comp, lineNumbers, null);
    }

    /// #### Parameters
    ///
    /// - `comp`: the text area, or a component that is not one, which
    ///   then gets no line numbers
    ///
    /// - `lineNumbers`: whether line numbers are shown
    ///
    /// - `lineNumberColor`: their color, or null for the color of
    ///   disabled text
    public RTextScrollPane(Component comp, boolean lineNumbers, Color lineNumberColor) {
        super();
        gutter = new Gutter(comp instanceof RTextArea ? (RTextArea) comp : null);
        gutter.setLineNumbersEnabled(lineNumbers);
        if (lineNumberColor != null) {
            gutter.setLineNumberColor(lineNumberColor);
        }
        if (comp != null) {
            setViewportView(comp);
        }
        cn1Header();
    }

    private void cn1Header() {
        boolean wanted = gutter.getLineNumbersEnabled();
        boolean shown = getRowHeader() != null && getRowHeader().getView() != null;
        if (wanted != shown) {
            setRowHeaderView(wanted ? gutter : null);
        }
    }

    public Gutter getGutter() {
        return gutter;
    }

    public boolean getLineNumbersEnabled() {
        return gutter.getLineNumbersEnabled();
    }

    public void setLineNumbersEnabled(boolean enabled) {
        gutter.setLineNumbersEnabled(enabled);
        cn1Header();
        revalidate();
        repaint();
    }

    /// The text area in the viewport, or null when the view is not one.
    public RTextArea getTextArea() {
        Component view = getViewport() == null ? null : getViewport().getView();
        return view instanceof RTextArea ? (RTextArea) view : null;
    }

    public boolean isFoldIndicatorEnabled() {
        return gutter.isFoldIndicatorEnabled();
    }

    /// Recorded only: there is no code folding.
    public void setFoldIndicatorEnabled(boolean enabled) {
        gutter.setFoldIndicatorEnabled(enabled);
    }

    public boolean isIconRowHeaderEnabled() {
        return gutter.isIconRowHeaderEnabled();
    }

    /// Recorded only: there are no gutter icons.
    public void setIconRowHeaderEnabled(boolean enabled) {
        gutter.setIconRowHeaderEnabled(enabled);
    }

    @Override
    public void setViewportView(Component view) {
        super.setViewportView(view);
        if (gutter != null) {
            gutter.setTextArea(view instanceof RTextArea ? (RTextArea) view : null);
        }
    }
}
