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

import com.codename1.ui.util.EditableResources;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;

/**
 * The ui for changing the renderer of a list within the gui builder
 *
 * @author Shai Almog
 */
public class ListRendererEditor extends javax.swing.JPanel {
    private com.codename1.ui.Component parentList;
    private EditableResources res;

    /** Creates new form ListRendererEditor */
    public ListRendererEditor(EditableResources res, com.codename1.ui.Component parentList, String currentUI) {
        initComponents();
        try {
            help.setPage(getClass().getResource("/help/renderer.html"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.parentList = parentList;
        this.res = res;
        Vector names = new Vector();
        for(String uiName : res.getUIResourceNames()) {
            if(uiName.equals(currentUI)) {
                continue;
            }
            com.codename1.ui.util.UIBuilderOverride b = new com.codename1.ui.util.UIBuilderOverride();
            if(!(b.createContainer(res, uiName) instanceof com.codename1.ui.Form)) {
                names.addElement(uiName);
            }
        }
        if(names.size() == 0) {
            errorMessage.setText("<html><body><b>You must create Container objects to use as renderers</b></body></html>");
            unselectedEven.setEnabled(false);
            selectedEven.setEnabled(false);
            unselected.setEnabled(false);
            selected.setEnabled(false);
            type.setEnabled(false);
            return;
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        selected.setModel(new DefaultComboBoxModel(names));
        unselected.setModel(new DefaultComboBoxModel(names));
        selectedEven.setModel(new DefaultComboBoxModel(names));
        unselectedEven.setModel(new DefaultComboBoxModel(names));
        Object r;
        if(parentList instanceof com.codename1.ui.List) {
            r = (com.codename1.ui.list.CellRenderer)((com.codename1.ui.List)parentList).getRenderer();
        } else {
            r = ((com.codename1.ui.list.ContainerList)parentList).getRenderer();
        }
        if(r instanceof com.codename1.ui.list.GenericListCellRenderer) {
            com.codename1.ui.list.GenericListCellRenderer g = (com.codename1.ui.list.GenericListCellRenderer)r;
            String selectedRendererName = g.getSelected().getName();
            String unselectedRendererName = g.getUnselected().getName();
            selected.setSelectedItem(selectedRendererName);
            unselected.setSelectedItem(unselectedRendererName);
            if(g.getSelectedEven() != null && g.getUnselectedEven() != null) {
                selectedEven.setSelectedItem(g.getSelectedEven().getName());
                unselectedEven.setSelectedItem(g.getUnselectedEven().getName());
                type.setSelectedIndex(2);
            } else {
                unselectedEven.setEnabled(false);
                selectedEven.setEnabled(false);
                if(!selectedRendererName.equals(unselectedRendererName)) {
                    type.setSelectedIndex(1);
                } else {
                    unselected.setEnabled(false);
                }
            }
        } else {
            unselectedEven.setEnabled(false);
            selectedEven.setEnabled(false);
            unselected.setEnabled(false);
        }
    }

    public com.codename1.ui.list.CellRenderer getResult() {
        if(selected.getModel().getSize() == 0) {
            if(parentList instanceof com.codename1.ui.List) {
                return (com.codename1.ui.list.CellRenderer)((com.codename1.ui.List)parentList).getRenderer();
            } else {
                return ((com.codename1.ui.list.ContainerList)parentList).getRenderer();
            }
        }
        com.codename1.ui.util.UIBuilderOverride b = new com.codename1.ui.util.UIBuilderOverride();
        com.codename1.ui.Container selectedContainer = b.createContainer(res, (String)selected.getSelectedItem());
        switch(type.getSelectedIndex()) {
            case 0:
                return new com.codename1.ui.list.GenericListCellRenderer(selectedContainer, b.createContainer(res, (String)selected.getSelectedItem()));
            case 1:
                return new com.codename1.ui.list.GenericListCellRenderer(selectedContainer, b.createContainer(res, (String)unselected.getSelectedItem()));
            default:
                com.codename1.ui.Container selectedContainerEven = b.createContainer(res, (String)selectedEven.getSelectedItem());
                com.codename1.ui.Container unselectedContainerEven = b.createContainer(res, (String)unselectedEven.getSelectedItem());
                return new com.codename1.ui.list.GenericListCellRenderer(selectedContainer, b.createContainer(res, (String)unselected.getSelectedItem()),
                        selectedContainerEven, unselectedContainerEven);
        }
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
        selected = new javax.swing.JComboBox();
        unselected = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        selectedEven = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        unselectedEven = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        type = new javax.swing.JComboBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();
        errorMessage = new javax.swing.JLabel();

        FormListener formListener = new FormListener();

        jLabel1.setText("Selected Renderer");
        jLabel1.setName("jLabel1"); // NOI18N

        selected.setName("selected"); // NOI18N

        unselected.setName("unselected"); // NOI18N

        jLabel2.setText("Unselected Renderer");
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel3.setText("Selected Even");
        jLabel3.setName("jLabel3"); // NOI18N

        selectedEven.setName("selectedEven"); // NOI18N

        jLabel4.setText("Unselected Even");
        jLabel4.setName("jLabel4"); // NOI18N

        unselectedEven.setName("unselectedEven"); // NOI18N

        jLabel5.setText("Renderer Type");
        jLabel5.setName("jLabel5"); // NOI18N

        type.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Standard", "Fisheye", "Pinstripe" }));
        type.setName("type"); // NOI18N
        type.addActionListener(formListener);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setName("help"); // NOI18N
        jScrollPane1.setViewportView(help);

        errorMessage.setText("Pick a renderer component");
        errorMessage.setName("errorMessage"); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel2)
                            .add(jLabel1)
                            .add(jLabel3)
                            .add(jLabel4)
                            .add(jLabel5))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(unselectedEven, 0, 297, Short.MAX_VALUE)
                            .add(selectedEven, 0, 297, Short.MAX_VALUE)
                            .add(selected, 0, 297, Short.MAX_VALUE)
                            .add(unselected, 0, 297, Short.MAX_VALUE)
                            .add(type, 0, 297, Short.MAX_VALUE)))
                    .add(layout.createSequentialGroup()
                        .add(errorMessage)
                        .add(280, 280, 280)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 479, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(errorMessage)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel5)
                            .add(type, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel1)
                            .add(selected, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel2)
                            .add(unselected, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel3)
                            .add(selectedEven, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel4)
                            .add(unselectedEven, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == type) {
                ListRendererEditor.this.typeActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void typeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeActionPerformed
        switch(type.getSelectedIndex()) {
            case 0:
                unselectedEven.setEnabled(false);
                selectedEven.setEnabled(false);
                unselected.setEnabled(false);
                break;
            case 1:
                unselectedEven.setEnabled(false);
                selectedEven.setEnabled(false);
                unselected.setEnabled(true);
                break;
            case 2:
                unselectedEven.setEnabled(true);
                selectedEven.setEnabled(true);
                unselected.setEnabled(true);
                break;
        }
    }//GEN-LAST:event_typeActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel errorMessage;
    private javax.swing.JTextPane help;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox selected;
    private javax.swing.JComboBox selectedEven;
    private javax.swing.JComboBox type;
    private javax.swing.JComboBox unselected;
    private javax.swing.JComboBox unselectedEven;
    // End of variables declaration//GEN-END:variables

}
