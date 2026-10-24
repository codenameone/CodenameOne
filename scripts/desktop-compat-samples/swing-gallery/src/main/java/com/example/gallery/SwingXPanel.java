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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.decorator.HighlighterFactory;

/**
 * A few SwingX components: a striped table, task panes, a date picker and a busy label.
 */
public class SwingXPanel extends JPanel {

    private final JLabel picked = new JLabel("No date picked");

    public SwingXPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(new JScrollPane(tasks()), BorderLayout.WEST);
        add(new JScrollPane(table()), BorderLayout.CENTER);
        add(pickers(), BorderLayout.SOUTH);
    }

    private JXTable table() {
        JXTable table = new JXTable(new PlanetTableModel(Planet.all()));
        table.setHighlighters(HighlighterFactory.createSimpleStriping());
        table.setColumnControlVisible(true);
        table.setVisibleRowCount(8);
        table.packAll();
        return table;
    }

    private JXTaskPaneContainer tasks() {
        JXTaskPaneContainer container = new JXTaskPaneContainer();

        JXTaskPane files = new JXTaskPane();
        files.setTitle("File tasks");
        files.add(new JLabel("Rename this file"));
        files.add(new JLabel("Move this file"));
        container.add(files);

        JXTaskPane details = new JXTaskPane();
        details.setTitle("Details");
        details.setCollapsed(true);
        details.add(new JLabel("8 planets"));
        container.add(details);
        return container;
    }

    private JPanel pickers() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JXDatePicker date = new JXDatePicker(new Date());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        date.addActionListener(e -> picked.setText(date.getDate() == null ? "No date picked"
                : "Picked " + format.format(date.getDate())));
        p.add(date);
        p.add(picked);

        JXBusyLabel busy = new JXBusyLabel();
        busy.setText("Idle");
        JButton toggle = new JButton("Toggle busy");
        toggle.addActionListener(e -> {
            busy.setBusy(!busy.isBusy());
            busy.setText(busy.isBusy() ? "Working..." : "Idle");
        });
        p.add(toggle);
        p.add(busy);
        return p;
    }
}
