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
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dialog;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JDialog;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JScrollPane;
import com.codename1.desktopcompat.javax.swing.JTextArea;
import com.codename1.desktopcompat.javax.swing.SwingUtilities;
import com.codename1.desktopcompat.javax.swing.UIManager;

/// A pane that reports a throwable: its message beside an error icon, a
/// button that opens and closes the stack trace, and a button that closes
/// the window the pane is in.
///
/// [#showDialog(Throwable)] is the usual entry: it shows the pane in a
/// modal dialog titled `Error` and returns once the dialog is closed.
///
/// ## What differs from SwingX
///
///  - The error-info accessors, the reporter accessors and the overloads
///    that take an `ErrorInfo` are absent, because `ErrorInfo` is built on
///    `java.util.logging.Level`, which a device does not have. A pane
///    made with the constructor therefore reports nothing; only
///    [#showDialog(Throwable)] gives it something to show.
///  - There is no report button and no fatal state, so the actions named
///    by [#REPORT_ACTION_KEY] and [#FATAL_ACTION_KEY] are not in the
///    action map.
///  - The frame and internal-frame variants are absent.
///  - The stack trace has the frames the platform records, which on a
///    device may be none; the chain of causes is always listed.
public class JXErrorPane extends JComponent {

    public static final String REPORT_ACTION_KEY = "report-action";
    public static final String FATAL_ACTION_KEY = "fatal-action";
    public static final String uiClassID = "ErrorPaneUI";

    private final JLabel message;
    private final JTextArea details;
    private final JScrollPane detailsScroll;
    private final JButton detailsButton;
    private final JButton closeButton;
    private Icon icon;

    public JXErrorPane() {
        message = new JLabel();
        details = new JTextArea(8, 40);
        details.setEditable(false);
        detailsScroll = new JScrollPane(details);
        detailsScroll.setVisible(false);
        detailsButton = new JButton("Details >>");
        closeButton = new JButton("Close");
        cn1Build();
    }

    private void cn1Build() {
        super.setLayout(new BorderLayout(8, 8));
        icon = UIManager.getIcon("OptionPane.errorIcon");
        message.setIcon(icon);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(closeButton);
        buttons.add(detailsButton);
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(message, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        super.add(top, BorderLayout.NORTH);
        super.add(detailsScroll, BorderLayout.CENTER);
        detailsButton.setEnabled(false);
        detailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cn1ToggleDetails();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Window w = SwingUtilities.getWindowAncestor(JXErrorPane.this);
                if (w != null) {
                    w.setVisible(false);
                    w.dispose();
                }
            }
        });
    }

    private void cn1ToggleDetails() {
        boolean open = !detailsScroll.isVisible();
        detailsScroll.setVisible(open);
        detailsButton.setText(open ? "Details <<" : "Details >>");
        revalidate();
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) {
            w.pack();
        }
    }

    private void cn1Report(Throwable t) {
        String text = t.getMessage();
        if (text == null || text.length() == 0) {
            text = t.toString();
        }
        message.setText(text);
        StringBuilder sb = new StringBuilder();
        Throwable at = t;
        // A chain of causes that loops back on itself ends at 20.
        for (int depth = 0; at != null && depth < 20; depth++) {
            if (depth > 0) {
                sb.append("Caused by: ");
            }
            sb.append(at.toString()).append('\n');
            StackTraceElement[] frames = at.getStackTrace();
            for (int i = 0; i < frames.length; i++) {
                sb.append("    at ").append(String.valueOf(frames[i])).append('\n');
            }
            at = at.getCause();
        }
        details.setText(sb.toString());
        detailsButton.setEnabled(true);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /// Sets the icon shown beside the message.
    public void setIcon(Icon icon) {
        Icon old = this.icon;
        this.icon = icon;
        message.setIcon(icon);
        firePropertyChange("icon", old, icon);
    }

    public Icon getIcon() {
        return icon;
    }

    /// Reports `e` in a modal dialog with no owner, and returns when the
    /// dialog is closed.
    public static void showDialog(Throwable e) {
        JXErrorPane pane = new JXErrorPane();
        if (e != null) {
            pane.cn1Report(e);
        }
        showDialog(null, pane);
    }

    /// Shows `pane` in a modal dialog over the window of `owner`, and
    /// returns when the dialog is closed.
    public static void showDialog(Component owner, JXErrorPane pane) {
        JDialog dialog = createDialog(owner, pane);
        dialog.setVisible(true);
        dialog.dispose();
    }

    /// A modal dialog titled `Error` with `pane` as its content, packed
    /// and placed over `owner`, not yet showing. The close button of the
    /// pane is the dialog's default button.
    public static JDialog createDialog(Component owner, JXErrorPane pane) {
        Window w = null;
        if (owner instanceof Window) {
            w = (Window) owner;
        } else if (owner != null) {
            w = SwingUtilities.getWindowAncestor(owner);
        }
        JDialog dialog = new JDialog(w, "Error", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(pane, BorderLayout.CENTER);
        dialog.getRootPane().setDefaultButton(pane.closeButton);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        return dialog;
    }
}
