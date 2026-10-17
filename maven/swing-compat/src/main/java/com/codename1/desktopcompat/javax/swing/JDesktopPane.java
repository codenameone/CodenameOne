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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import java.util.ArrayList;

/// The desk internal frames lie on: a layered pane that knows its frames
/// and which of them is selected.
///
/// Frames are drawn and moved by [JInternalFrame] itself; there is no
/// desktop manager to replace. A frame is always moved and resized live,
/// whatever the drag mode says.
public class JDesktopPane extends JLayeredPane {

    public static final int LIVE_DRAG_MODE = 0;

    public static final int OUTLINE_DRAG_MODE = 1;

    private JInternalFrame selectedFrame;
    private int dragMode = LIVE_DRAG_MODE;

    public JDesktopPane() {
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /// Kept and answered; frames are always dragged live.
    public void setDragMode(int dragMode) {
        int old = this.dragMode;
        this.dragMode = dragMode;
        firePropertyChange("dragMode", old, dragMode);
    }

    public int getDragMode() {
        return dragMode;
    }

    /// Every internal frame on the desk, the one on top first.
    public JInternalFrame[] getAllFrames() {
        ArrayList<JInternalFrame> found = new ArrayList<JInternalFrame>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JInternalFrame) {
                found.add((JInternalFrame) c);
            }
        }
        return found.toArray(new JInternalFrame[found.size()]);
    }

    public JInternalFrame[] getAllFramesInLayer(int layer) {
        ArrayList<JInternalFrame> found = new ArrayList<JInternalFrame>();
        for (int i = 0; i < getComponentCount(); i++) {
            Component c = getComponent(i);
            if (c instanceof JInternalFrame && getLayer(c) == layer) {
                found.add((JInternalFrame) c);
            }
        }
        return found.toArray(new JInternalFrame[found.size()]);
    }

    public JInternalFrame getSelectedFrame() {
        return selectedFrame;
    }

    /// Records which frame is the selected one. It does not select it:
    /// that is `JInternalFrame.setSelected`.
    public void setSelectedFrame(JInternalFrame f) {
        selectedFrame = f;
    }

    @Override
    public void remove(int index) {
        Component c = getComponent(index);
        super.remove(index);
        if (c == selectedFrame) {
            selectedFrame = null;
        }
    }

    @Override
    public void removeAll() {
        super.removeAll();
        selectedFrame = null;
    }

    /// Nothing, unless a minimum size was set: as on the desktop, a
    /// desktop pane can be any size and its frames stay where they are.
    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return new Dimension(0, 0);
    }
}
