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
package com.codenameone.developerguide.desktopinterop.swing;

// tag::desktopInteropSwingApp[]
import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class GreeterApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GreeterApp::showFrame);
    }

    private static void showFrame() {
        JFrame frame = new JFrame("Greeter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextField name = new JTextField(16);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        form.add(new JLabel("Name"));
        form.add(name);

        BadgePanel badge = new BadgePanel();
        JButton greet = new JButton("Greet");
        greet.addActionListener(e -> {
            badge.setInitial(name.getText());
            JOptionPane.showMessageDialog(frame, "Hello, " + name.getText() + "!");
        });

        frame.getContentPane().add(form, BorderLayout.NORTH);
        frame.getContentPane().add(badge, BorderLayout.CENTER);
        frame.getContentPane().add(greet, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
    }
}
// end::desktopInteropSwingApp[]
