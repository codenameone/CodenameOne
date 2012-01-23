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
import java.awt.Component;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * Allows us to edit a hashtable of values or a string, similarly to the array editor.
 * This is useful for the ui builder.
 *
 * @author Shai Almog
 */
public class HashtableEditor extends javax.swing.JPanel {
    private EditableResources res;

    /** Creates new form HashtableEditor */
    public HashtableEditor(EditableResources res, Object value, Object lst) {
        this.res = res;
        initComponents();
        keysAndValues.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if(value instanceof com.codename1.ui.Image) {
                    value = HashtableEditor.this.res.findId(value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        keysAndValues.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                removeRow.setEnabled(keysAndValues.getSelectedRow() > -1);
            }
        });
        if(value == null) {
            // add default values for the keys if applicable
            if(lst != null) {
                Object r;
                if(lst instanceof com.codename1.ui.List) {
                    r = ((com.codename1.ui.List)lst).getRenderer();
                } else {
                    r = ((com.codename1.ui.list.ContainerList)lst).getRenderer();
                }
                if(r instanceof com.codename1.ui.list.GenericListCellRenderer) {
                    com.codename1.ui.list.GenericListCellRenderer g = (com.codename1.ui.list.GenericListCellRenderer)r;
                    List<String> names = new ArrayList<String>();
                    findComponentsOfInterest(g.getSelected(), names);
                    for(String current : names) {
                        ((DefaultTableModel)keysAndValues.getModel()).addRow(new Object[] {current, ""});
                    }
                }
            }
            return;
        }
        if(value instanceof String) {
            stringValue.setSelected(true);
            stringTextField.setEnabled(true);
            keysAndValues.setEnabled(false);
            stringTextField.setText((String)value);
            addRow.setEnabled(false);
        } else {
            stringTextField.setEnabled(false);
            keysAndValues.setEnabled(true);
            Hashtable v = (Hashtable)value;
            for(Object key : v.keySet()) {
                ((DefaultTableModel)keysAndValues.getModel()).addRow(new Object[] {key, v.get(key)});
            }
        }
    }

    private void findComponentsOfInterest(com.codename1.ui.Component cmp, List<String> dest) {
        if(cmp instanceof com.codename1.ui.Container) {
            com.codename1.ui.Container c = (com.codename1.ui.Container)cmp;
            int count = c.getComponentCount();
            for(int iter = 0 ; iter < count ; iter++) {
                findComponentsOfInterest(c.getComponentAt(iter), dest);
            }
            return;
        }
        if((cmp instanceof com.codename1.ui.Label || cmp instanceof com.codename1.ui.TextArea) && cmp.getName() != null) {
            dest.add(cmp.getName());
            return;
        }
    }

    public Object getResult() {
        if(keyValue.isSelected()) {
            TableModel t = keysAndValues.getModel();
            Hashtable v = new Hashtable();
            for(int iter = 0 ; iter < t.getRowCount() ; iter++) {
                v.put(t.getValueAt(iter, 0), t.getValueAt(iter, 1));
            }
            return v;
        }
        return stringTextField.getText();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        keyValue = new javax.swing.JRadioButton();
        stringValue = new javax.swing.JRadioButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        keysAndValues = new javax.swing.JTable();
        stringTextField = new javax.swing.JTextField();
        addRow = new javax.swing.JButton();
        removeRow = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();

        FormListener formListener = new FormListener();

        buttonGroup1.add(keyValue);
        keyValue.setSelected(true);
        keyValue.setText("Key/Value");
        keyValue.setName("keyValue"); // NOI18N
        keyValue.addActionListener(formListener);

        buttonGroup1.add(stringValue);
        stringValue.setText("String");
        stringValue.setName("stringValue"); // NOI18N
        stringValue.addActionListener(formListener);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        keysAndValues.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Key", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        keysAndValues.setName("keysAndValues"); // NOI18N
        keysAndValues.addMouseListener(formListener);
        jScrollPane1.setViewportView(keysAndValues);

        stringTextField.setEnabled(false);
        stringTextField.setName("stringTextField"); // NOI18N

        addRow.setText("Add Row");
        addRow.setName("addRow"); // NOI18N
        addRow.addActionListener(formListener);

        removeRow.setText("Remove Row");
        removeRow.setEnabled(false);
        removeRow.setName("removeRow"); // NOI18N
        removeRow.addActionListener(formListener);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      List models can contain arbitrary data, currently the Codename One Designer supports entering either Strings or hash values (key/value pairs). \nThis editor can be used to enter a String (in the text field bellow) or sets of keys and values.\n    </p>\r\n    <p>\n      In order to display the data properly the list needs to have a renderer defined (see the renderer element in the properties) \na String list will normally \"just work\" however a list of Hashtables will need keys to fit the names of the elements within the renderer.\n Normally such keys appear automatically in this editor if you defined the renderer properly.\n    </p>\n    <p>\n    Values for the keys can be boolean (true/false) which are useful for checkboxes, they can be Strings which can appear in \nlabels/buttons in the renderer or they can even be images which can also can be applied to labels etc. <br>\nA value can also be a navigation destination but that isn't represented visually other than by clicking the list element.\n    </p>\n  </body>\r\n</html>\r\n"); // NOI18N
        help.setName("help"); // NOI18N
        jScrollPane2.setViewportView(help);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(addRow)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(removeRow)
                        .add(183, 183, 183))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, stringTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                                .add(keyValue)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(stringValue)))
                        .add(4, 4, 4)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE)
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {addRow, removeRow}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(keyValue)
                    .add(stringValue))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(addRow)
                            .add(removeRow))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(stringTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.MouseListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == keyValue) {
                HashtableEditor.this.keyValueActionPerformed(evt);
            }
            else if (evt.getSource() == stringValue) {
                HashtableEditor.this.stringValueActionPerformed(evt);
            }
            else if (evt.getSource() == addRow) {
                HashtableEditor.this.addRowActionPerformed(evt);
            }
            else if (evt.getSource() == removeRow) {
                HashtableEditor.this.removeRowActionPerformed(evt);
            }
        }

        public void mouseClicked(java.awt.event.MouseEvent evt) {
            if (evt.getSource() == keysAndValues) {
                HashtableEditor.this.keysAndValuesMouseClicked(evt);
            }
        }

        public void mouseEntered(java.awt.event.MouseEvent evt) {
        }

        public void mouseExited(java.awt.event.MouseEvent evt) {
        }

        public void mousePressed(java.awt.event.MouseEvent evt) {
        }

        public void mouseReleased(java.awt.event.MouseEvent evt) {
        }
    }// </editor-fold>//GEN-END:initComponents

    private void keyValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keyValueActionPerformed
        stringTextField.setEnabled(false);
        addRow.setEnabled(true);
        removeRow.setEnabled(keysAndValues.getSelectedRow() > -1);
        keysAndValues.setEnabled(true);
    }//GEN-LAST:event_keyValueActionPerformed

    private void stringValueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stringValueActionPerformed
        stringTextField.setEnabled(true);
        addRow.setEnabled(false);
        removeRow.setEnabled(false);
        keysAndValues.setEnabled(false);
    }//GEN-LAST:event_stringValueActionPerformed

    private void addRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRowActionPerformed
        HashtableKeyValueEditor kv = new HashtableKeyValueEditor(res, "Key", "");
        int result = JOptionPane.showConfirmDialog(this, kv, "Add", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(result == JOptionPane.OK_OPTION) {
            ((DefaultTableModel)keysAndValues.getModel()).addRow(new Object[] {kv.getKey(), kv.getValue()});
        }
    }//GEN-LAST:event_addRowActionPerformed

    private void removeRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeRowActionPerformed
        ((DefaultTableModel)keysAndValues.getModel()).removeRow(keysAndValues.getSelectedRow());
    }//GEN-LAST:event_removeRowActionPerformed

    private void keysAndValuesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_keysAndValuesMouseClicked
        if(evt.getClickCount() == 2) {
            int row = keysAndValues.getSelectedRow();
            if(row > -1) {
                HashtableKeyValueEditor kv = new HashtableKeyValueEditor(res, (String)keysAndValues.getValueAt(row, 0), keysAndValues.getValueAt(row, 1));
                int result = JOptionPane.showConfirmDialog(this, kv, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if(result == JOptionPane.OK_OPTION) {
                    ((DefaultTableModel)keysAndValues.getModel()).setValueAt(kv.getKey(), row, 0);
                    ((DefaultTableModel)keysAndValues.getModel()).setValueAt(kv.getValue(), row, 1);
                }
            }
        }
    }//GEN-LAST:event_keysAndValuesMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addRow;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTextPane help;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JRadioButton keyValue;
    private javax.swing.JTable keysAndValues;
    private javax.swing.JButton removeRow;
    private javax.swing.JTextField stringTextField;
    private javax.swing.JRadioButton stringValue;
    // End of variables declaration//GEN-END:variables

}
