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

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.codename1.desktopcompat.SwingInterop;
import com.codename1.ui.Form;

/// A Codename One screen that shows a Swing panel, and Swing windows opened
/// from a Codename One application.
public class EmbeddedReport {

    public void show() {
        // tag::desktopInteropEmbedSwing[]
        JPanel report = new JPanel(new BorderLayout());
        report.add(new JLabel("Orders this week"), BorderLayout.NORTH);
        report.add(new JScrollPane(new JTable(new Object[][] {{"Mon", 12}, {"Tue", 9}},
                new Object[] {"Day", "Orders"})), BorderLayout.CENTER);

        Form form = new Form("Report", new com.codename1.ui.layouts.BorderLayout());
        form.add(com.codename1.ui.layouts.BorderLayout.CENTER, SwingInterop.asComponent(report));
        form.show();
        // end::desktopInteropEmbedSwing[]
    }

    public boolean openEditor(JPanel editor) {
        // tag::desktopInteropSwingWindows[]
        JFrame frame = new JFrame("Edit order");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(editor);
        frame.setVisible(true);   // closing it returns to the form that was showing

        int answer = JOptionPane.showConfirmDialog(frame, "Save the order?", "Save",
                JOptionPane.YES_NO_OPTION);
        // end::desktopInteropSwingWindows[]
        return answer == JOptionPane.YES_OPTION;
    }
}
