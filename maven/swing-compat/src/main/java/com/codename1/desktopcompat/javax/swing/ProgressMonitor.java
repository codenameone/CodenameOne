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

import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.WindowAdapter;
import com.codename1.desktopcompat.java.awt.event.WindowEvent;

/// `javax.swing.ProgressMonitor`: shows a dialog with a progress bar once
/// an operation turns out to take long, and lets the user cancel it.
///
/// The dialog does not block its caller. On a phone it floats over the
/// current form and takes the input while it is showing.
public class ProgressMonitor {

    private final Component parent;
    private final Object message;
    private String note;
    private int min;
    private int max;
    private int millisToDecideToPopup = 500;
    private int millisToPopup = 2000;
    private final long started;
    private boolean canceled;
    private boolean closed;
    private JDialog dialog;
    private JProgressBar bar;
    private JLabel noteLabel;

    public ProgressMonitor(Component parentComponent, Object message, String note, int min, int max) {
        this.parent = parentComponent;
        this.message = message;
        this.note = note;
        this.min = min;
        this.max = max;
        this.started = System.currentTimeMillis();
    }

    public void setProgress(int nv) {
        if (closed) {
            return;
        }
        if (nv >= max) {
            close();
            return;
        }
        if (dialog != null) {
            bar.setValue(nv);
            return;
        }
        long elapsed = System.currentTimeMillis() - started;
        if (elapsed < millisToDecideToPopup) {
            return;
        }
        long predicted = millisToPopup;
        int done = nv - min;
        if (done > 0) {
            predicted = elapsed * (max - min) / done;
        }
        if (predicted >= millisToPopup) {
            open(nv);
        }
    }

    private void open(int nv) {
        Window owner = null;
        if (parent instanceof Window) {
            owner = (Window) parent;
        } else if (parent != null) {
            owner = SwingUtilities.getWindowAncestor(parent);
        }
        String title = UIManager.getString("ProgressMonitor.progressText");
        dialog = new JDialog(owner, title == null ? "Progress..." : title);
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        if (message instanceof Component) {
            body.add((Component) message);
        } else if (message != null) {
            body.add(new JLabel(String.valueOf(message)));
        }
        noteLabel = new JLabel(note == null ? " " : note);
        body.add(noteLabel);
        bar = new JProgressBar(min, max);
        bar.setValue(nv);
        body.add(bar);
        JPanel buttons = new JPanel();
        String cancelText = UIManager.getString("OptionPane.cancelButtonText");
        JButton cancel = new JButton(cancelText == null ? "Cancel" : cancelText);
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canceled = true;
                hide();
            }
        });
        buttons.add(cancel);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(body, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                canceled = true;
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private void hide() {
        JDialog d = dialog;
        dialog = null;
        bar = null;
        noteLabel = null;
        if (d != null) {
            d.setVisible(false);
            d.dispose();
        }
    }

    public void close() {
        closed = true;
        hide();
    }

    public int getMinimum() {
        return min;
    }

    public void setMinimum(int m) {
        min = m;
        if (bar != null) {
            bar.setMinimum(m);
        }
    }

    public int getMaximum() {
        return max;
    }

    public void setMaximum(int m) {
        max = m;
        if (bar != null) {
            bar.setMaximum(m);
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setMillisToDecideToPopup(int millisToDecideToPopup) {
        this.millisToDecideToPopup = millisToDecideToPopup;
    }

    public int getMillisToDecideToPopup() {
        return millisToDecideToPopup;
    }

    public void setMillisToPopup(int millisToPopup) {
        this.millisToPopup = millisToPopup;
    }

    public int getMillisToPopup() {
        return millisToPopup;
    }

    public void setNote(String note) {
        this.note = note;
        if (noteLabel != null) {
            noteLabel.setText(note == null ? " " : note);
        }
    }

    public String getNote() {
        return note;
    }
}
