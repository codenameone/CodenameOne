/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.designer;

import java.io.IOException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SpinnerNumberModel;

/**
 * The UI used to change the layout of a given component within the UI builder
 *
 * @author Shai Almog
 */
public class LayoutEditor extends javax.swing.JPanel {
    /** Creates new form LayoutEditor */
    public LayoutEditor(com.codename1.ui.Container parent) {
        initComponents();
        try {
            help.setPage(getClass().getResource("/help/layout_flow.html"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if(parent instanceof com.codename1.ui.Form) {
            parent = ((com.codename1.ui.Form)parent).getContentPane();
        }
        rows.setModel(new SpinnerNumberModel(1, 1, 1000, 1));
        columns.setModel(new SpinnerNumberModel(1, 1, 1000, 1));
        if(parent.getComponentCount() > 5) {
            layoutCombo.setModel(new DefaultComboBoxModel(new String[] {
                "Flow Layout",
                "Box Layout X",
                "Box Layout Y",
                "Grid Layout",
                "Table Layout",
                "Layered Layout",
            }));
        } else {
            if(parent.getLayout() instanceof com.codename1.ui.layouts.BorderLayout) {
                layoutCombo.setSelectedIndex(6);
                com.codename1.ui.layouts.BorderLayout b = (com.codename1.ui.layouts.BorderLayout)parent.getLayout();
                initSwapCombo(b, com.codename1.ui.layouts.BorderLayout.NORTH, swapNorth);
                initSwapCombo(b, com.codename1.ui.layouts.BorderLayout.SOUTH, swapSouth);
                initSwapCombo(b, com.codename1.ui.layouts.BorderLayout.EAST, swapEast);
                initSwapCombo(b, com.codename1.ui.layouts.BorderLayout.WEST, swapWest);
                initSwapCombo(b, com.codename1.ui.layouts.BorderLayout.CENTER, swapCenter);
                absoluteCenter.setSelected(b.isAbsoluteCenter());
                return;
            }
        }
        if(parent.getLayout() instanceof com.codename1.ui.layouts.FlowLayout) {
            layoutCombo.setSelectedIndex(0);
            com.codename1.ui.layouts.FlowLayout f = (com.codename1.ui.layouts.FlowLayout)parent.getLayout();
            fillRows.setSelected(f.isFillRows());
            switch(f.getValign()) {
                case com.codename1.ui.Component.TOP:
                    valign.setSelectedIndex(0);
                    break;
                case com.codename1.ui.Component.CENTER:
                    valign.setSelectedIndex(1);
                    break;
                case com.codename1.ui.Component.BOTTOM:
                    valign.setSelectedIndex(2);
                    break;
            }
            switch(f.getAlign()) {
                case com.codename1.ui.Component.LEFT:
                    align.setSelectedIndex(0);
                    break;
                case com.codename1.ui.Component.CENTER:
                    align.setSelectedIndex(1);
                    break;
                case com.codename1.ui.Component.RIGHT:
                    align.setSelectedIndex(2);
                    break;
            }
            return;
        }
        if(parent.getLayout() instanceof com.codename1.ui.layouts.BoxLayout) {
            if(((com.codename1.ui.layouts.BoxLayout)parent.getLayout()).getAxis() == com.codename1.ui.layouts.BoxLayout.Y_AXIS) {
                layoutCombo.setSelectedIndex(2);
            } else {
                layoutCombo.setSelectedIndex(1);
            }
            return;
        }
        if(parent.getLayout() instanceof com.codename1.ui.layouts.GridLayout) {
            layoutCombo.setSelectedIndex(3);
            rows.setValue(((com.codename1.ui.layouts.GridLayout)parent.getLayout()).getRows());
            columns.setValue(((com.codename1.ui.layouts.GridLayout)parent.getLayout()).getColumns());
            rows.setEnabled(true);
            columns.setEnabled(true);
            return;
        }
        if(parent.getLayout() instanceof com.codename1.ui.table.TableLayout) {
            layoutCombo.setSelectedIndex(4);
            rows.setValue(((com.codename1.ui.table.TableLayout)parent.getLayout()).getRows());
            columns.setValue(((com.codename1.ui.table.TableLayout)parent.getLayout()).getColumns());
            rows.setEnabled(true);
            columns.setEnabled(true);
            return;
        }
        if(parent.getLayout() instanceof com.codename1.ui.layouts.LayeredLayout) {
            layoutCombo.setSelectedIndex(5);
            return;
        }
    }

    public com.codename1.ui.layouts.Layout getResult() {
        switch(layoutCombo.getSelectedIndex()) {
            case 0:
                com.codename1.ui.layouts.FlowLayout f = new com.codename1.ui.layouts.FlowLayout();
                f.setFillRows(fillRows.isSelected());
                switch(valign.getSelectedIndex()) {
                    case 0:
                        f.setValign(com.codename1.ui.Component.TOP);
                        break;
                    case 1:
                        f.setValign(com.codename1.ui.Component.CENTER);
                        break;
                    case 2:
                        f.setValign(com.codename1.ui.Component.BOTTOM);
                        break;
                }
                switch(align.getSelectedIndex()) {
                    case 0:
                        f.setAlign(com.codename1.ui.Component.LEFT);
                        break;
                    case 1:
                        f.setAlign(com.codename1.ui.Component.CENTER);
                        break;
                    case 2:
                        f.setAlign(com.codename1.ui.Component.RIGHT);
                        break;
                }
                return f;
            case 1:
                return new com.codename1.ui.layouts.BoxLayout(com.codename1.ui.layouts.BoxLayout.X_AXIS);
            case 2:
                return new com.codename1.ui.layouts.BoxLayout(com.codename1.ui.layouts.BoxLayout.Y_AXIS);
            case 3:
                return new com.codename1.ui.layouts.GridLayout(((Number)rows.getValue()).intValue(), ((Number)columns.getValue()).intValue());
            case 4:
                return new com.codename1.ui.table.TableLayout(((Number)rows.getValue()).intValue(), ((Number)columns.getValue()).intValue());
            case 5:
                return new com.codename1.ui.layouts.LayeredLayout();
            default:
                return createBorderLayout();
        }
    }

    private void defineSwap(com.codename1.ui.layouts.BorderLayout b, String originalPos, JComboBox c) {
        if(c.getSelectedIndex() <= 0) {
            return;
        }
        b.defineLandscapeSwap(originalPos, (String)c.getSelectedItem());
    }

    private void initSwapCombo(com.codename1.ui.layouts.BorderLayout b, String originalPos, JComboBox c) {
        String pos = b.getLandscapeSwap(originalPos);
        if(pos != null) {
            c.setSelectedItem(pos);
        }
    }

    private com.codename1.ui.layouts.BorderLayout createBorderLayout() {
        com.codename1.ui.layouts.BorderLayout b = new com.codename1.ui.layouts.BorderLayout();
        defineSwap(b, com.codename1.ui.layouts.BorderLayout.NORTH, swapNorth);
        defineSwap(b, com.codename1.ui.layouts.BorderLayout.SOUTH, swapSouth);
        defineSwap(b, com.codename1.ui.layouts.BorderLayout.EAST, swapEast);
        defineSwap(b, com.codename1.ui.layouts.BorderLayout.WEST, swapWest);
        defineSwap(b, com.codename1.ui.layouts.BorderLayout.CENTER, swapCenter);
        b.setAbsoluteCenter(absoluteCenter.isSelected());
        return b;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        layoutCombo = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        rows = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        columns = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        valign = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        fillRows = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        swapNorth = new javax.swing.JComboBox();
        swapSouth = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        swapCenter = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        swapEast = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        swapWest = new javax.swing.JComboBox();
        jLabel11 = new javax.swing.JLabel();
        align = new javax.swing.JComboBox();
        jLabel12 = new javax.swing.JLabel();
        absoluteCenter = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();

        FormListener formListener = new FormListener();

        jLabel1.setText("Layout");
        jLabel1.setName("jLabel1"); // NOI18N

        layoutCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Flow Layout", "Box Layout X", "Box Layout Y", "Grid Layout", "Table Layout", "LayeredLayout", "Border Layout" }));
        layoutCombo.setName("layoutCombo"); // NOI18N
        layoutCombo.addActionListener(formListener);

        jLabel2.setText("Rows");
        jLabel2.setName("jLabel2"); // NOI18N

        rows.setEnabled(false);
        rows.setName("rows"); // NOI18N

        jLabel3.setText("Columns");
        jLabel3.setName("jLabel3"); // NOI18N

        columns.setEnabled(false);
        columns.setName("columns"); // NOI18N

        jLabel4.setText("Valign");
        jLabel4.setName("jLabel4"); // NOI18N

        valign.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Top", "Center", "Bottom" }));
        valign.setName("valign"); // NOI18N

        jLabel5.setText("Fill Rows");
        jLabel5.setName("jLabel5"); // NOI18N

        fillRows.setName("fillRows"); // NOI18N

        jLabel6.setText("Landscape North");
        jLabel6.setName("jLabel6"); // NOI18N

        swapNorth.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Don't Swap", "South", "Center", "West", "East" }));
        swapNorth.setEnabled(false);
        swapNorth.setName("swapNorth"); // NOI18N

        swapSouth.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Don't Swap", "North", "Center", "West", "East" }));
        swapSouth.setEnabled(false);
        swapSouth.setName("swapSouth"); // NOI18N

        jLabel7.setText("Landscape South");
        jLabel7.setName("jLabel7"); // NOI18N

        swapCenter.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Don't Swap", "North", "South", "West", "East" }));
        swapCenter.setEnabled(false);
        swapCenter.setName("swapCenter"); // NOI18N

        jLabel8.setText("Landscape Center");
        jLabel8.setName("jLabel8"); // NOI18N

        jLabel9.setText("Landscape East");
        jLabel9.setName("jLabel9"); // NOI18N

        swapEast.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Don't Swap", "North", "Center", "West", "South" }));
        swapEast.setEnabled(false);
        swapEast.setName("swapEast"); // NOI18N

        jLabel10.setText("Landscape West");
        jLabel10.setName("jLabel10"); // NOI18N

        swapWest.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Don't Swap", "North", "Center", "South", "East" }));
        swapWest.setEnabled(false);
        swapWest.setName("swapWest"); // NOI18N

        jLabel11.setText("Align");
        jLabel11.setName("jLabel11"); // NOI18N

        align.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Left", "Center", "Right" }));
        align.setName("align"); // NOI18N

        jLabel12.setText("Absolute Center");
        jLabel12.setName("jLabel12"); // NOI18N

        absoluteCenter.setEnabled(false);
        absoluteCenter.setName("absoluteCenter"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setName("help"); // NOI18N
        jScrollPane1.setViewportView(help);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel1)
                            .add(jLabel2)
                            .add(jLabel3)
                            .add(jLabel11))
                        .add(51, 51, 51)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(rows, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(layoutCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(columns, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(align, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel6)
                            .add(jLabel7)
                            .add(jLabel8)
                            .add(jLabel9)
                            .add(jLabel10)
                            .add(jLabel5)
                            .add(jLabel4)
                            .add(jLabel12))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(layout.createSequentialGroup()
                                .add(absoluteCenter)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 178, Short.MAX_VALUE))
                            .add(valign, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(layout.createSequentialGroup()
                                .add(fillRows)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 178, Short.MAX_VALUE))
                            .add(swapNorth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(swapSouth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(swapCenter, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(swapEast, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(swapWest, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .add(0, 0, 0)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel1)
                            .add(layoutCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(12, 12, 12)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel2)
                            .add(rows, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel3)
                            .add(columns, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel11)
                            .add(align, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(fillRows)
                            .add(layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(valign, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(jLabel4))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel5)))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel6)
                            .add(swapNorth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel7)
                            .add(swapSouth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel8)
                            .add(swapCenter, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel9)
                            .add(swapEast, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel10)
                            .add(swapWest, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel12)
                            .add(absoluteCenter))))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == layoutCombo) {
                LayoutEditor.this.layoutComboActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void layoutComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_layoutComboActionPerformed
        int i = layoutCombo.getSelectedIndex();
        String url = "/help/layout_flow.html";
        switch(i) {
            // Box Layout X
            case 1:
                url = "/help/layout_box_x.html";
                break;

            // Box Layout Y
            case 2:
                url = "/help/layout_box_y.html";
                break;

            // Grid Layout
            case 3:
                url = "/help/layout_grid.html";
                break;

            // Table Layout
            case 4:
                url = "/help/layout_table.html";
                break;

            // LayeredLayout
            case 5:
                url = "/help/layout_layered.html";
                break;

            // Border Layout
            case 6:
                url = "/help/layout_border.html";
                break;
        }
        try {
            help.setPage(getClass().getResource(url));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        boolean v = i == 3 || i == 4;
        rows.setEnabled(v);
        columns.setEnabled(v);
        boolean f = i == 0;
        fillRows.setEnabled(f);
        valign.setEnabled(f);
        align.setEnabled(f);
        boolean c = i == 6;
        swapEast.setEnabled(c);
        swapWest.setEnabled(c);
        swapSouth.setEnabled(c);
        swapNorth.setEnabled(c);
        swapCenter.setEnabled(c);
        absoluteCenter.setEnabled(c);
    }//GEN-LAST:event_layoutComboActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox absoluteCenter;
    private javax.swing.JComboBox align;
    private javax.swing.JSpinner columns;
    private javax.swing.JCheckBox fillRows;
    private javax.swing.JTextPane help;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox layoutCombo;
    private javax.swing.JSpinner rows;
    private javax.swing.JComboBox swapCenter;
    private javax.swing.JComboBox swapEast;
    private javax.swing.JComboBox swapNorth;
    private javax.swing.JComboBox swapSouth;
    private javax.swing.JComboBox swapWest;
    private javax.swing.JComboBox valign;
    // End of variables declaration//GEN-END:variables

}
