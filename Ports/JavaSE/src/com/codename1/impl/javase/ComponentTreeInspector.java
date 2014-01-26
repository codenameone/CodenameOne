/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.List;
import com.codename1.ui.list.ContainerList;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * A visual view of the component hierarchy within the current Codename One form
 * this UI monitors the tree of components and gives us some information of its
 * structure which can be useful for debugging applications.
 *
 * @author Shai Almog
 */
public class ComponentTreeInspector extends javax.swing.JFrame {
    
    /** Creates new form ComponentTreeInspector */
    public ComponentTreeInspector() {
        initComponents();
        refreshComponentTree();
        componentTree.setCellRenderer(new DefaultTreeCellRenderer() {

            @Override
            public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                Component t = (Component)value;
                String newVal = t.getClass().getSimpleName();
                if(t.getName() != null) {
                    newVal += "[" + t.getName() + "]";
                } else {
                    newVal += "[Unnamed]";
                }
                newVal += ", " + t.getUIID();
                return super.getTreeCellRendererComponent(tree, newVal, sel, expanded, leaf, row, hasFocus);
            }
            
        });
        componentTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                if(e.getPath() != null) {
                    Component c = (Component)e.getPath().getLastPathComponent();
                    if(c != null) {
                        componentClass.setText(c.getClass().getName());
                        componentName.setText("" + c.getName());
                        componentUIID.setText("" + c.getUIID());
                        componentSelected.setSelected(c.hasFocus());
                        coordinates.setText("x: " + c.getX() + " y: " + c.getY() + " absX: " + c.getAbsoluteX()+ " absY: " + c.getAbsoluteY() + 
                                " Width: " + c.getWidth() + " Height: " + c.getHeight());
                        preferredSize.setText(c.getPreferredW() + ", " + c.getPreferredH());
                        padding.setText("Top: " + c.getStyle().getPadding(Component.TOP) + " Bottom: " + c.getStyle().getPadding(Component.BOTTOM)
                                 + " Left: " + c.getStyle().getPadding(Component.LEFT) + " Right: " + c.getStyle().getPadding(Component.RIGHT));
                        margin.setText("Top: " + c.getStyle().getMargin(Component.TOP) + " Bottom: " + c.getStyle().getMargin(Component.BOTTOM)
                                 + " Left: " + c.getStyle().getMargin(Component.LEFT) + " Right: " + c.getStyle().getMargin(Component.RIGHT));
                        if(c instanceof com.codename1.ui.Container) {
                            layout.setText(((com.codename1.ui.Container)c).getLayout().getClass().getSimpleName());
                        } else {
                            layout.setText("");
                        }
                        com.codename1.ui.Container parent = c.getParent();
                        constraint.setText("");
                        if(parent != null) {
                            Object o = parent.getLayout().getComponentConstraint(c);
                            if(o != null) {
                                constraint.setText(o.toString());
                            }
                        } 
                    }
                }
            }
        });
        pack();
        setLocationByPlatform(true);
        setVisible(true);
    }

    private void refreshComponentTree() {
        componentTree.setModel(new ComponentTreeModel(Display.getInstance().getCurrent()));
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        componentTree = new javax.swing.JTree();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        componentClass = new javax.swing.JTextField();
        componentName = new javax.swing.JTextField();
        componentUIID = new javax.swing.JTextField();
        componentSelected = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        layout = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        constraint = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        coordinates = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        preferredSize = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        padding = new javax.swing.JTextField();
        margin = new javax.swing.JTextField();
        jToolBar1 = new javax.swing.JToolBar();
        refreshTree = new javax.swing.JButton();

        FormListener formListener = new FormListener();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Component Tree Inspector");

        jScrollPane1.setViewportView(componentTree);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jLabel1.setText("Class");

        jLabel2.setText("Name");

        jLabel3.setText("UIID");

        jLabel4.setText("Selected");

        componentClass.setEditable(false);

        componentName.setEditable(false);

        componentUIID.setEditable(false);

        componentSelected.setEnabled(false);

        jLabel5.setText("Layout");

        layout.setEditable(false);

        jLabel6.setText("Constraint");
        jLabel6.setToolTipText("Layout Constraint (for border layout)");

        constraint.setEditable(false);
        constraint.setToolTipText("Layout Constraint (for border layout)");

        jLabel7.setText("Coordinates");

        coordinates.setEditable(false);

        jLabel8.setText("Preferred Size");

        preferredSize.setEditable(false);

        jLabel9.setText("Padding");

        jLabel10.setText("Margin");

        padding.setEditable(false);

        margin.setEditable(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(componentSelected)
                            .addComponent(componentClass, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                            .addComponent(componentName, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                            .addComponent(componentUIID, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                            .addComponent(layout, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                            .addComponent(constraint, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                            .addComponent(coordinates, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                            .addComponent(preferredSize, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                            .addComponent(padding, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                            .addComponent(margin, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(componentClass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(componentName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(componentUIID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(componentSelected))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(layout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(constraint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(coordinates, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(preferredSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(padding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(margin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jSplitPane1.setRightComponent(jPanel1);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jToolBar1.setRollover(true);

        refreshTree.setText("Refresh");
        refreshTree.setFocusable(false);
        refreshTree.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        refreshTree.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        refreshTree.addActionListener(formListener);
        jToolBar1.add(refreshTree);

        getContentPane().add(jToolBar1, java.awt.BorderLayout.PAGE_START);

        pack();
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == refreshTree) {
                ComponentTreeInspector.this.refreshTreeActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

private void refreshTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshTreeActionPerformed
    refreshComponentTree();
}//GEN-LAST:event_refreshTreeActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField componentClass;
    private javax.swing.JTextField componentName;
    private javax.swing.JCheckBox componentSelected;
    private javax.swing.JTree componentTree;
    private javax.swing.JTextField componentUIID;
    private javax.swing.JTextField constraint;
    private javax.swing.JTextField coordinates;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTextField layout;
    private javax.swing.JTextField margin;
    private javax.swing.JTextField padding;
    private javax.swing.JTextField preferredSize;
    private javax.swing.JButton refreshTree;
    // End of variables declaration//GEN-END:variables
}
