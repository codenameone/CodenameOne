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

import javax.swing.SwingUtilities;
import com.codename1.ui.util.EditableResources;
import java.util.Hashtable;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * UI for editing arrays used by the UI Builder. Subclasses of this allow customizing
 * the type of entry within the array to commands etc.
 *
 * @author Shai Almog
 */
public class ArrayEditorDialog extends javax.swing.JDialog {
    private boolean okFlag;

    public boolean isOK() {
        return okFlag;
    }

    /** Creates new form ArrayEditorDialog */
    public ArrayEditorDialog(java.awt.Component parentCmp, final EditableResources res, Object[] array,
            String title, String helpText) {
        super((java.awt.Frame)SwingUtilities.windowForComponent(parentCmp), true);
        initComponents();
        ModifiableJOptionPane.reverseOKCancel(ok, cancel);
        setTitle(title);
        try {
            help.setPage(getClass().getResource(helpText));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        arrayList.setModel(new DefaultListModel());
        arrayList.setCellRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if(value instanceof String[]) {
                    value = Arrays.toString((String[])value);
                } else {
                    if(value instanceof com.codename1.ui.Image) {
                        value = res.findId(value);
                    } else {
                        if(value instanceof com.codename1.ui.Command) {
                            com.codename1.ui.Command c = (com.codename1.ui.Command)value;
                            value = c.getCommandName() + " - "+ c.getId();
                        } else {
                            if(value instanceof Hashtable) {
                                Hashtable h = (Hashtable)value;
                                String result = "";
                                boolean first = true;
                                for(Object key : h.keySet()) {
                                    if(!first) {
                                        result += ", ";
                                    }
                                    Object val = h.get(key);
                                    if(val instanceof com.codename1.ui.Image) {
                                        result += key + "=" + res.findId(val);
                                    } else {
                                        result += key + "=" + val;
                                    }
                                    first = false;
                                }
                                value = result;
                            } else {
                                if(value instanceof String && ((String)value).length() == 0) {
                                    value = "[Empty]";
                                }
                            }
                        }
                    }
                }
                if(value == null) {
                    value = "[null]";
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

        });
        if(array != null) {
            for(Object t : array) {
                ((DefaultListModel)arrayList.getModel()).addElement(t);
            }
        }
        pack();
        setLocationRelativeTo(parentCmp);
        setVisible(true);
    }

    public List getResult() {
        List t = new ArrayList();
        for(int iter = 0 ; iter < arrayList.getModel().getSize() ; iter++) {
            t.add(arrayList.getModel().getElementAt(iter));
        }
        return t;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        arrayList = new javax.swing.JList();
        add = new javax.swing.JButton();
        remove = new javax.swing.JButton();
        edit = new javax.swing.JButton();
        moveUp = new javax.swing.JButton();
        moveDown = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        ok = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();

        FormListener formListener = new FormListener();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        arrayList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        arrayList.setName("arrayList"); // NOI18N
        arrayList.addMouseListener(formListener);
        arrayList.addListSelectionListener(formListener);
        jScrollPane1.setViewportView(arrayList);

        add.setText("Add");
        add.setName("add"); // NOI18N
        add.addActionListener(formListener);

        remove.setText("Remove");
        remove.setEnabled(false);
        remove.setName("remove"); // NOI18N
        remove.addActionListener(formListener);

        edit.setText("Edit");
        edit.setEnabled(false);
        edit.setName("edit"); // NOI18N
        edit.addActionListener(formListener);

        moveUp.setText("Move Up");
        moveUp.setEnabled(false);
        moveUp.setName("moveUp"); // NOI18N
        moveUp.addActionListener(formListener);

        moveDown.setText("Move Down");
        moveDown.setEnabled(false);
        moveDown.setName("moveDown"); // NOI18N
        moveDown.addActionListener(formListener);

        jPanel1.setName("jPanel1"); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.GridLayout(1, 2, 8, 0));

        ok.setText("OK");
        ok.setName("ok"); // NOI18N
        ok.addActionListener(formListener);
        jPanel2.add(ok);

        cancel.setText("Cancel");
        cancel.setName("cancel"); // NOI18N
        cancel.addActionListener(formListener);
        jPanel2.add(cancel);

        jPanel1.add(jPanel2);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setName("help"); // NOI18N
        jScrollPane2.setViewportView(help);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(add)
                            .add(remove)
                            .add(edit)
                            .add(moveUp)
                            .add(moveDown))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 209, Short.MAX_VALUE))
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 507, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {add, edit, moveDown, moveUp, remove}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(add)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(remove)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(edit)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(moveUp)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(moveDown))
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.MouseListener, javax.swing.event.ListSelectionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == add) {
                ArrayEditorDialog.this.addActionPerformed(evt);
            }
            else if (evt.getSource() == remove) {
                ArrayEditorDialog.this.removeActionPerformed(evt);
            }
            else if (evt.getSource() == edit) {
                ArrayEditorDialog.this.editActionPerformed(evt);
            }
            else if (evt.getSource() == moveUp) {
                ArrayEditorDialog.this.moveUpActionPerformed(evt);
            }
            else if (evt.getSource() == moveDown) {
                ArrayEditorDialog.this.moveDownActionPerformed(evt);
            }
            else if (evt.getSource() == cancel) {
                ArrayEditorDialog.this.cancelActionPerformed(evt);
            }
            else if (evt.getSource() == ok) {
                ArrayEditorDialog.this.okActionPerformed(evt);
            }
        }

        public void mouseClicked(java.awt.event.MouseEvent evt) {
            if (evt.getSource() == arrayList) {
                ArrayEditorDialog.this.arrayListMouseClicked(evt);
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

        public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
            if (evt.getSource() == arrayList) {
                ArrayEditorDialog.this.arrayListValueChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void arrayListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_arrayListMouseClicked
        if(evt.getClickCount() == 2) {
            int i = arrayList.getSelectedIndex();
            Object o = edit(((DefaultListModel)arrayList.getModel()).elementAt(i));
            ((DefaultListModel)arrayList.getModel()).setElementAt(o, i);
        }
}//GEN-LAST:event_arrayListMouseClicked

    private void arrayListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_arrayListValueChanged
        boolean s = arrayList.getSelectedIndex() > -1;
        edit.setEnabled(s);
        remove.setEnabled(s);
        int smallest = Math.min(arrayList.getAnchorSelectionIndex(), arrayList.getLeadSelectionIndex());
        int largest = Math.max(arrayList.getAnchorSelectionIndex(), arrayList.getLeadSelectionIndex());
        moveDown.setEnabled(s && largest < arrayList.getModel().getSize() - 1);
        moveUp.setEnabled(s && smallest > 0);
}//GEN-LAST:event_arrayListValueChanged

    private void addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addActionPerformed
        Object o = edit(null);
        if(o != null) {
            ((DefaultListModel)arrayList.getModel()).addElement(o);
        }
}//GEN-LAST:event_addActionPerformed

    private void removeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeActionPerformed
        int[] i = arrayList.getSelectedIndices();
        if(i != null && i.length > 0) {
            if(i.length == 1) {
                ((DefaultListModel)arrayList.getModel()).remove(i[0]);
            } else {
                while(i != null && i.length > 0) {
                    ((DefaultListModel)arrayList.getModel()).remove(i[0]);
                    i = arrayList.getSelectedIndices();
                }
            }
        }
}//GEN-LAST:event_removeActionPerformed

    private void editActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editActionPerformed
        int i = arrayList.getSelectedIndex();
        Object o = edit(((DefaultListModel)arrayList.getModel()).elementAt(i));
        ((DefaultListModel)arrayList.getModel()).setElementAt(o, i);
}//GEN-LAST:event_editActionPerformed

    private void moveUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpActionPerformed
        int[] indices = arrayList.getSelectedIndices();
        for(int i : indices) {
            Object o = ((DefaultListModel)arrayList.getModel()).elementAt(i);
            ((DefaultListModel)arrayList.getModel()).remove(i);
            ((DefaultListModel)arrayList.getModel()).insertElementAt(o, i - 1);
        }
        arrayList.setSelectionInterval(indices[0] - 1, indices[indices.length - 1] - 1);
}//GEN-LAST:event_moveUpActionPerformed

    private void moveDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownActionPerformed
        int[] indices = arrayList.getSelectedIndices();
        for(int iter = indices.length - 1 ; iter >= 0 ; iter--) {
            int i = indices[iter];
            Object o = ((DefaultListModel)arrayList.getModel()).elementAt(i);
            ((DefaultListModel)arrayList.getModel()).remove(i);
            ((DefaultListModel)arrayList.getModel()).insertElementAt(o, i + 1);
        }
        arrayList.setSelectionInterval(indices[0] + 1, indices[indices.length - 1] + 1);
}//GEN-LAST:event_moveDownActionPerformed

    private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
        dispose();
    }//GEN-LAST:event_cancelActionPerformed

    private void okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okActionPerformed
        okFlag = true;
        dispose();
    }//GEN-LAST:event_okActionPerformed

    protected Object edit(Object o) {
        JTextField f = new JTextField(20);
        if(o != null) {
            f.setText((String)o);
        }
        if(showEditDialog(f)) {
            return f.getText();
        }
        return o;
    }

    protected boolean showEditDialog(JComponent cmp) {
        return JOptionPane.showConfirmDialog(this, cmp, "Edit " + getTitle(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton add;
    private javax.swing.JList arrayList;
    private javax.swing.JButton cancel;
    private javax.swing.JButton edit;
    private javax.swing.JTextPane help;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton moveDown;
    private javax.swing.JButton moveUp;
    private javax.swing.JButton ok;
    private javax.swing.JButton remove;
    // End of variables declaration//GEN-END:variables

}
