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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Cursor;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JRootPane;
import com.codename1.desktopcompat.javax.swing.KeyStroke;

/// A frame with a cancel button the escape key presses, a place to start
/// at, and a waiting state that swaps in a wait pane and a wait cursor.
///
/// ## What differs from SwingX
///
///  - There is no extended root pane, so the status bar, the tool bar and
///    `getRootPaneExt` are absent. Add a status bar or a tool bar to the
///    content pane instead.
///  - Key preview is absent.
///  - The idle state is what [#setIdle(boolean)] was last given; nothing
///    watches the input for it, and the threshold is only kept.
///  - The constructors that take a graphics configuration are absent.
public class JXFrame extends JFrame {

    /// Where the frame is put when it is first shown.
    public enum StartPosition {
        CenterInScreen,
        CenterInParent,
        Manual
    }

    private JButton cancelButton;
    private boolean cancelBound;
    private StartPosition startPosition;
    private boolean placed;
    private boolean waitCursorVisible;
    private Cursor realCursor;
    private Component waitPane;
    private Component glassPaneBefore;
    private boolean waitPaneVisible;
    private boolean waiting;
    private boolean idle;
    private long idleThreshold;

    public JXFrame() {
        this(null, false);
    }

    public JXFrame(String title) {
        this(title, false);
    }

    /// A frame that ends the application when closed, if `exitOnClose`.
    public JXFrame(String title, boolean exitOnClose) {
        super(title);
        if (exitOnClose) {
            super.setDefaultCloseOperation(EXIT_ON_CLOSE);
        }
    }

    /// Sets the button the escape key presses while this frame has the
    /// focus, or null for none.
    public void setCancelButton(JButton button) {
        JButton old = cancelButton;
        cancelButton = button;
        JRootPane root = getRootPane();
        if (!cancelBound && root != null) {
            cancelBound = true;
            root.registerKeyboardAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JButton b = cancelButton;
                    if (b != null && b.isEnabled()) {
                        b.doClick();
                    }
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        firePropertyChange("cancelButton", old, button);
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    /// Sets the root pane's default button.
    public void setDefaultButton(JButton button) {
        JButton old = getDefaultButton();
        getRootPane().setDefaultButton(button);
        firePropertyChange("defaultButton", old, button);
    }

    public JButton getDefaultButton() {
        return getRootPane().getDefaultButton();
    }

    /// Sets where the frame is put the first time it is shown.
    public void setStartPosition(StartPosition position) {
        StartPosition old = getStartPosition();
        startPosition = position;
        firePropertyChange("startPosition", old, getStartPosition());
    }

    /// Where the frame is put the first time it is shown, `Manual` unless
    /// set.
    public StartPosition getStartPosition() {
        return startPosition == null ? StartPosition.Manual : startPosition;
    }

    /// Shows or hides the frame, placing it by the start position the
    /// first time it is shown.
    @Override
    public void setVisible(boolean visible) {
        if (visible && !placed) {
            placed = true;
            StartPosition p = getStartPosition();
            if (p == StartPosition.CenterInScreen) {
                setLocationRelativeTo(null);
            } else if (p == StartPosition.CenterInParent) {
                setLocationRelativeTo(getOwner());
            }
        }
        super.setVisible(visible);
    }

    /// Shows the wait cursor over the frame, or goes back to the cursor
    /// last set with [#setCursor(Cursor)].
    public void setWaitCursorVisible(boolean flag) {
        boolean old = waitCursorVisible;
        if (flag != old) {
            if (flag) {
                realCursor = isCursorSet() ? getCursor() : null;
                super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                waitCursorVisible = true;
            } else {
                waitCursorVisible = false;
                super.setCursor(realCursor);
            }
            firePropertyChange("waitCursorVisible", old, flag);
        }
    }

    public boolean isWaitCursorVisible() {
        return waitCursorVisible;
    }

    /// Sets the frame's cursor; while the wait cursor shows, the cursor
    /// is kept and takes effect when the wait cursor goes.
    @Override
    public void setCursor(Cursor cursor) {
        if (waitCursorVisible) {
            realCursor = cursor;
        } else {
            super.setCursor(cursor);
        }
    }

    /// Sets the component that takes the place of the glass pane while
    /// the wait pane is visible.
    public void setWaitPane(Component c) {
        Component old = waitPane;
        boolean shown = waitPaneVisible;
        if (shown) {
            setWaitPaneVisible(false);
        }
        waitPane = c;
        if (shown) {
            setWaitPaneVisible(true);
        }
        firePropertyChange("waitPane", old, c);
    }

    public Component getWaitPane() {
        return waitPane;
    }

    /// Puts the wait pane in as the glass pane and shows it, or hides it
    /// and puts the glass pane back. Without a wait pane only the flag
    /// changes.
    public void setWaitPaneVisible(boolean flag) {
        boolean old = waitPaneVisible;
        if (flag != old) {
            waitPaneVisible = flag;
            Component wp = waitPane;
            if (wp != null) {
                if (flag) {
                    glassPaneBefore = getGlassPane();
                    setGlassPane(wp);
                    wp.setVisible(true);
                } else {
                    wp.setVisible(false);
                    if (glassPaneBefore != null) {
                        setGlassPane(glassPaneBefore);
                    }
                    glassPaneBefore = null;
                }
            }
            firePropertyChange("waitPaneVisible", old, flag);
        }
    }

    public boolean isWaitPaneVisible() {
        return waitPaneVisible;
    }

    /// Shows or hides both the wait cursor and the wait pane.
    public void setWaiting(boolean waiting) {
        boolean old = this.waiting;
        this.waiting = waiting;
        firePropertyChange("waiting", old, waiting);
        setWaitPaneVisible(waiting);
        setWaitCursorVisible(waiting);
    }

    public boolean isWaiting() {
        return waiting;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        boolean old = this.idle;
        this.idle = idle;
        firePropertyChange("idle", old, idle);
    }

    public void setIdleThreshold(long threshold) {
        long old = idleThreshold;
        idleThreshold = threshold;
        firePropertyChange("idleThreshold", Long.valueOf(old), Long.valueOf(threshold));
    }

    public long getIdleThreshold() {
        return idleThreshold;
    }
}
