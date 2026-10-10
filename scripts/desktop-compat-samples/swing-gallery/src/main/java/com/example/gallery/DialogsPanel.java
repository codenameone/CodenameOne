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
package com.example.gallery;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The standard dialogs, a modal dialog of the application's own, and the file chooser.
 */
public class DialogsPanel extends JPanel {

    private final JLabel result = new JLabel("No dialog shown yet");

    public DialogsPanel() {
        super(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttons.add(button("Message", this::showMessage));
        buttons.add(button("Confirm", this::showConfirm));
        buttons.add(button("Input", this::showInput));
        buttons.add(button("Custom dialog", this::showCustom));
        buttons.add(button("Open file", this::showOpen));
        add(buttons, BorderLayout.NORTH);
        result.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(result, BorderLayout.CENTER);
    }

    private static JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    private void showMessage() {
        JOptionPane.showMessageDialog(this, "The operation completed.", "Message", JOptionPane.INFORMATION_MESSAGE);
        result.setText("Message dismissed");
    }

    private void showConfirm() {
        int answer = JOptionPane.showConfirmDialog(this, "Delete the selected item?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        result.setText(answer == JOptionPane.YES_OPTION ? "Confirmed" : "Declined");
    }

    private void showInput() {
        String name = JOptionPane.showInputDialog(this, "What is your name?", "Input", JOptionPane.QUESTION_MESSAGE);
        result.setText(name == null ? "Input cancelled" : "Hello, " + name);
    }

    private void showCustom() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Rename", true);
        JTextField field = new JTextField("untitled", 18);
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        boolean[] accepted = new boolean[1];
        ok.addActionListener(e -> {
            accepted[0] = true;
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());

        JPanel form = new JPanel(new FlowLayout());
        form.add(new JLabel("New name:"));
        form.add(field);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(ok);
        dialog.getContentPane().add(form, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        result.setText(accepted[0] ? "Renamed to " + field.getText() : "Rename cancelled");
    }

    private void showOpen() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt", "md"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            result.setText("Chose " + file.getName() + " (" + file.length() + " bytes)");
        } else {
            result.setText("No file chosen");
        }
    }
}
