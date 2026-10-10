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
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * A list, a table and a tree over the same planets.
 */
public class DataPanel extends JPanel {

    private final Planet[] planets = Planet.all();
    private final JLabel selection = new JLabel("Nothing selected");

    public DataPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new GridLayout(1, 2, 6, 6));
        top.add(titled("List", planetList()));
        top.add(titled("Tree", planetTree()));
        JPanel center = new JPanel(new GridLayout(2, 1, 6, 6));
        center.add(top);
        center.add(titled("Table", planetTable()));
        add(center, BorderLayout.CENTER);
        add(selection, BorderLayout.SOUTH);
    }

    private JScrollPane planetList() {
        DefaultListModel<Planet> model = new DefaultListModel<>();
        for (Planet p : planets) {
            model.addElement(p);
        }
        JList<Planet> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new PlanetRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                selection.setText("List: " + list.getSelectedValue().getName());
            }
        });
        return new JScrollPane(list);
    }

    private JScrollPane planetTable() {
        PlanetTableModel model = new PlanetTableModel(planets);
        JTable table = new JTable(model);
        table.setRowSorter(new TableRowSorter<>(model));
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(PlanetTableModel.NAME).setPreferredWidth(140);
        table.getColumnModel().getColumn(PlanetTableModel.MASS).setCellRenderer(new MassRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                Object name = model.getValueAt(table.convertRowIndexToModel(row), PlanetTableModel.NAME);
                selection.setText("Table: " + name);
            }
        });
        return new JScrollPane(table);
    }

    private JScrollPane planetTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Solar system");
        DefaultMutableTreeNode rocky = new DefaultMutableTreeNode("Rocky planets");
        DefaultMutableTreeNode giants = new DefaultMutableTreeNode("Giants");
        root.add(rocky);
        root.add(giants);
        for (Planet p : planets) {
            (p.getMass() < 10 ? rocky : giants).add(new DefaultMutableTreeNode(p));
        }
        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.expandRow(1);
        tree.addTreeSelectionListener(e -> {
            TreePath path = e.getNewLeadSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                selection.setText("Tree: " + node.getUserObject() + (node.isLeaf() ? "" : " ("
                        + node.getChildCount() + ")"));
            }
        });
        return new JScrollPane(tree);
    }

    private static JPanel titled(String title, Component content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    /** Shows a planet with its moons, the giants in bold. */
    private static class PlanetRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Planet p = (Planet) value;
            setText(p.getName() + "  -  " + p.getMoons() + (p.getMoons() == 1 ? " moon" : " moons"));
            setFont(getFont().deriveFont(p.getMass() > 10 ? Font.BOLD : Font.PLAIN));
            if (!isSelected && index % 2 == 1) {
                setBackground(new Color(0xf2f6fa));
            }
            return this;
        }
    }

    /** Right-aligned, two decimals, heavy planets in a darker color. */
    private static class MassRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat format = new DecimalFormat("#,##0.000");

        MassRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            double mass = ((Number) value).doubleValue();
            setText(format.format(mass));
            if (!isSelected) {
                setForeground(mass > 10 ? new Color(0x8a3b00) : table.getForeground());
            }
            return this;
        }
    }
}
