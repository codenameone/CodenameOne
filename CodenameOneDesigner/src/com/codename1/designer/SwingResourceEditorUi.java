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
package com.codename1.designer;

import com.codename1.ui.util.ResourceEditorUi;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/// The resource editor's dialogs, for [com.codename1.ui.util.EditableResources].
///
/// Installed once when the editor starts. Nothing else may install it: the
/// CSS compiler shares `EditableResources` and has to keep running without a
/// display.
final class SwingResourceEditorUi extends ResourceEditorUi {

    private static final boolean IS_MAC =
            System.getProperty("os.name", "").regionMatches(true, 0, "mac", 0, 3);

    private static java.awt.Frame owner() {
        java.awt.Frame[] frames = java.awt.Frame.getFrames();
        return frames.length == 0 ? null : frames[0];
    }

    @Override
    public String promptPassword(String current) {
        JPasswordField password = new JPasswordField();
        if (current != null) {
            password.setText(current);
        }
        int v = JOptionPane.showConfirmDialog(owner(), password, "Enter Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return v == JOptionPane.OK_OPTION ? new String(password.getPassword()) : null;
    }

    @Override
    public void reportError(String title, String message) {
        JOptionPane.showMessageDialog(owner(), message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void modifiedChanged(boolean modified) {
        if (!IS_MAC) {
            return;
        }
        // macOS shows unsaved changes as a dot in the window's close button.
        for (java.awt.Window w : java.awt.Frame.getWindows()) {
            if (w instanceof JFrame) {
                ((JFrame) w).getRootPane().putClientProperty("Window.documentModified", Boolean.valueOf(modified));
            }
        }
    }
}
