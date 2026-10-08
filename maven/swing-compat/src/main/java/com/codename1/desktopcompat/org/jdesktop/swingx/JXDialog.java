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

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dialog;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.Frame;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.javax.swing.AbstractAction;
import com.codename1.desktopcompat.javax.swing.Action;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JDialog;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.KeyStroke;
import com.codename1.desktopcompat.javax.swing.UIManager;

/// A dialog around one content component, with a row of buttons under it
/// taken from the content's action map.
///
/// The action the content keeps under [#EXECUTE_ACTION_COMMAND], if any,
/// becomes the first button and the dialog's default button. The action
/// under [#CLOSE_ACTION_COMMAND] becomes the close button; without one
/// the close button calls [#doClose()]. The escape key presses the close
/// button. The dialog's title is the content's name, and the dialog is
/// packed when built. It is not modal unless made so.
///
/// ## What differs from SwingX
///
///  - There is no extended root pane, so the status bar and the tool bar
///    are absent, and [#getRootPane()] answers a plain root pane.
///  - The button texts come from the `XDialog.Close` and
///    `XDialog.Execute` values of the UI manager, and are `Close` and
///    `Execute` when those are not set; the locale members are absent.
public class JXDialog extends JDialog {

    public static final String EXECUTE_ACTION_COMMAND = "execute";
    public static final String CLOSE_ACTION_COMMAND = "close";
    public static final String UIPREFIX = "XDialog.";

    protected JComponent content;

    public JXDialog(JComponent content) {
        this((Frame) null, content);
    }

    public JXDialog(Frame frame, JComponent content) {
        super(frame);
        cn1Build(content);
    }

    public JXDialog(Dialog dialog, JComponent content) {
        super(dialog);
        cn1Build(content);
    }

    public JXDialog(Window window, JComponent content) {
        super(window);
        cn1Build(content);
    }

    private void cn1Build(JComponent c) {
        if (c == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        content = c;
        super.getContentPane().setLayout(new BorderLayout());
        super.getContentPane().add(c, BorderLayout.CENTER);
        super.getContentPane().add(cn1Buttons(), BorderLayout.SOUTH);
        String name = c.getName();
        if (name != null) {
            super.setTitle(name);
        }
        super.pack();
    }

    /// Sets the title to the content's name, when it has one.
    protected void setTitleFromContent() {
        if (content != null && content.getName() != null) {
            setTitle(content.getName());
        }
    }

    /// Closes the dialog and releases its window.
    public void doClose() {
        dispose();
    }

    /// The row of buttons under the content: the execute button, when the
    /// content has an execute action, and the close button.
    protected JComponent createButtonPanel() {
        return cn1Buttons();
    }

    private JComponent cn1Buttons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Action execute = content.getActionMap().get(EXECUTE_ACTION_COMMAND);
        Action close = content.getActionMap().get(CLOSE_ACTION_COMMAND);
        if (close == null) {
            close = new AbstractAction(cn1Text("Close")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doClose();
                }
            };
        }
        JButton closeButton = new JButton(close);
        if (closeButton.getText() == null || closeButton.getText().length() == 0) {
            closeButton.setText(cn1Text("Close"));
        }
        if (execute != null) {
            JButton executeButton = new JButton(execute);
            if (executeButton.getText() == null || executeButton.getText().length() == 0) {
                executeButton.setText(cn1Text("Execute"));
            }
            panel.add(executeButton);
            super.getRootPane().setDefaultButton(executeButton);
        }
        panel.add(closeButton);
        super.getRootPane().registerKeyboardAction(new Press(closeButton), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        return panel;
    }

    /// Presses a button, if it is enabled.
    private static final class Press implements ActionListener {

        private final JButton button;

        Press(JButton button) {
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (button.isEnabled()) {
                button.doClick();
            }
        }
    }

    private static String cn1Text(String key) {
        String s = UIManager.getString(UIPREFIX + key);
        return s == null ? key : s;
    }

    /// The UI manager's string for `key` under [#UIPREFIX], or null when
    /// there is none.
    protected String getUIString(String key) {
        return UIManager.getString(UIPREFIX + key);
    }
}
