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

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.VirtualKeyboard;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.resource.util.CodenameOneComponentWrapper;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/**
 * Editor for virtual keyboard layout in the l10n section
 *
 * @author Shai Almog
 */
public class VKBEditor extends javax.swing.JDialog {

    private VirtualKeyboard keyboard;

    private String[][] inputMode;
    private String originalValue;
    private boolean canceled;

    /** Creates new form VKBEditor */
    public VKBEditor(java.awt.Component parent, String name, String value) {
        super((java.awt.Frame)SwingUtilities.windowForComponent(parent), true);
        initComponents();
        ModifiableJOptionPane.reverseOKCancel(ok, cancel);
        originalValue = value;
        this.name.setText(name);
        Form blankForm = new Form();
        blankForm.setTransitionInAnimator(CommonTransitions.createEmpty());
        blankForm.setTransitionOutAnimator(CommonTransitions.createEmpty());
        blankForm.show();
        keyboard = new VirtualKeyboard();
        keyboard.setTransitionInAnimator(CommonTransitions.createEmpty());
        keyboard.setTransitionOutAnimator(CommonTransitions.createEmpty());
        setValue(value);
        removeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if(value instanceof int[]) {
                    int[] val = (int[])value;
                    value = inputMode[val[0]][val[1]];
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        rows.setModel(new SpinnerNumberModel(inputMode.length, 1, 20, 1));
        CodenameOneComponentWrapper w = new CodenameOneComponentWrapper(keyboard);
        previewKeyboard.add(java.awt.BorderLayout.CENTER, w);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    public void setValue(String value) {
        keyboard.setInputModeOrder(new String[] {name.getText()});
        inputMode = tokenizeMultiArray(value, '|', '\n');
        if(inputMode.length == 0) {
            inputMode = new String[1][];
            inputMode[0] = new String[0];
        }
        rows.setValue(inputMode.length);
        updateVKB();
    }

    private void updateVKB() {
        int i = ((Number)addToRow.getValue()).intValue();
        int col = ((Number)addToColumn.getValue()).intValue();
        i = Math.min(i, inputMode.length - 1);
        col = Math.max(0, Math.min(col, inputMode[i].length - 1));
        Vector removeVec = new Vector();
        for(int row =0 ; row < inputMode.length ; row++) {
            for(int column = 0 ; column < inputMode[row].length ; column++) {
                removeVec.add(new int[] {row, column});
            }
        }
        int oldIndex = removeCombo.getSelectedIndex();
        removeCombo.setModel(new DefaultComboBoxModel(removeVec));
        if(oldIndex < removeVec.size() && oldIndex > -1) {
            removeCombo.setSelectedIndex(oldIndex);
        }
        addToRow.setModel(new SpinnerNumberModel(i, 0, inputMode.length - 1, 1));
        addToColumn.setModel(new SpinnerNumberModel(col, 0, Math.max(0, inputMode[i].length - 1), 1));
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                keyboard.setTextField(new com.codename1.ui.TextField("Input"));
                keyboard.addInputMode(name.getText(), inputMode);
                VirtualKeyboard.setDefaultInputModeOrder(new String[] {name.getText()});
                VirtualKeyboard.addDefaultInputMode(name.getText(), inputMode);
                keyboard.revalidate();
                previewKeyboard.repaint();
                
                // for some reason refresh doesn't work properly the first time???
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        keyboard.revalidate();
                        previewKeyboard.repaint();
                    }
                });
            }
        });
    }

    public String getValue() {
        if(canceled) {
            return originalValue;
        }
        StringBuilder b = new StringBuilder();
        for(int iter = 0 ; iter < inputMode.length ; iter++) {
            if(iter > 0) {
                b.append('\n');
            }
            if(inputMode[iter] != null && inputMode[iter].length > 0) {
                b.append(inputMode[iter][0]);
                for(int row = 1 ; row < inputMode[iter].length ; row++) {
                    b.append('|');
                    b.append(inputMode[iter][row]);
                }
            }
        }
        return b.toString();
    }

    private String[][] tokenizeMultiArray(String s, char separator, char lineBreak) {
        Vector lines = tokenizeString(s, lineBreak);
        int lineCount = lines.size();
        String[][] result = new String[lineCount][];
        for(int iter = 0 ; iter < lineCount ; iter++) {
            String currentString = (String)lines.elementAt(iter);
            result[iter] = toStringArray(tokenizeString(currentString, separator));
        }
        return result;
    }

    private String[] toStringArray(Vector v) {
        String[] arr = new String[v.size()];
        for(int iter = 0 ; iter < arr.length ; iter++) {
            arr[iter] = (String)v.elementAt(iter);
        }
        return arr;
    }

    private Vector tokenizeString(String s, char separator) {
        Vector tokenized = new Vector();
        int len = s.length();
        boolean lastSeparator = false;
        StringBuffer buf = new StringBuffer();
        for(int iter = 0 ; iter < len ; iter++) {
            char current = s.charAt(iter);
            if(current == separator) {
                if(lastSeparator) {
                    buf.append(separator);
                    lastSeparator = false;
                    continue;
                }
                lastSeparator = true;
                if(buf.length() > 0) {
                    tokenized.addElement(buf.toString());
                    buf = new StringBuffer();
                }
            } else {
                lastSeparator = false;
                buf.append(current);
            }
        }
        if(buf.length() > 0) {
            tokenized.addElement(buf.toString());
        }
        return tokenized;
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
        name = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        addCombo = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        rows = new javax.swing.JSpinner();
        addButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        removeCombo = new javax.swing.JComboBox();
        removeButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        ok = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        previewKeyboard = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        addToRow = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        addToColumn = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        rowToAdd = new javax.swing.JTextField();
        addRow = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();

        FormListener formListener = new FormListener();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Virtual Keyboard");

        jLabel1.setText("Name");
        jLabel1.setName("jLabel1"); // NOI18N

        name.setEditable(false);
        name.setName("name"); // NOI18N

        jLabel2.setText("Add");
        jLabel2.setName("jLabel2"); // NOI18N

        addCombo.setEditable(true);
        addCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "", "$Shift$", "$Delete$", "$Mode$", "$T9$", "$Space$", "$OK$", "" }));
        addCombo.setName("addCombo"); // NOI18N

        jLabel3.setText("Rows");
        jLabel3.setName("jLabel3"); // NOI18N

        rows.setName("rows"); // NOI18N
        rows.addChangeListener(formListener);

        addButton.setText("Add");
        addButton.setName("addButton"); // NOI18N
        addButton.addActionListener(formListener);

        jLabel4.setText("Remove");
        jLabel4.setName("jLabel4"); // NOI18N

        removeCombo.setName("removeCombo"); // NOI18N

        removeButton.setText("Remove");
        removeButton.setName("removeButton"); // NOI18N
        removeButton.addActionListener(formListener);

        jPanel1.setName("jPanel1"); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.GridLayout(1, 2, 14, 0));

        ok.setText("OK");
        ok.setName("ok"); // NOI18N
        ok.addActionListener(formListener);
        jPanel2.add(ok);

        cancel.setText("Cancel");
        cancel.setName("cancel"); // NOI18N
        cancel.addActionListener(formListener);
        jPanel2.add(cancel);

        jPanel1.add(jPanel2);

        previewKeyboard.setName("previewKeyboard"); // NOI18N
        previewKeyboard.setLayout(new java.awt.BorderLayout());

        jLabel5.setText("Row");
        jLabel5.setName("jLabel5"); // NOI18N

        addToRow.setName("addToRow"); // NOI18N
        addToRow.addChangeListener(formListener);

        jLabel6.setText("Column");
        jLabel6.setName("jLabel6"); // NOI18N

        addToColumn.setName("addToColumn"); // NOI18N

        jLabel7.setText("Add Batch");
        jLabel7.setName("jLabel7"); // NOI18N

        rowToAdd.setName("rowToAdd"); // NOI18N

        addRow.setText("Add Batch");
        addRow.setName("addRow"); // NOI18N
        addRow.addActionListener(formListener);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      \rTo add a virtual keyboard to the list of virtual keyboards you need to add an entry to the @vkb\nlist with the name of this virtual keyboard. Notice that the name is the text following the \"@vkb-\" characters.\n    </p>\n   <p>\nYou can set the number of rows in the VKB by increasing the number of rows in the spinner. \nCharacters and special characters can be added to the VKB by typing them in or selecting \nthem within the Add combo box and selecting the row/column where they should be placed. \nTo add multiple characters at once to a given position type them into the Add Batch field and\npress the Add Batch button.\n   </p>\r\n   <p>\nTo remove improperly added entries select them in the remove combo box and press the remove\nbutton.\n   </p>\n  </body>\r\n</html>\r\n"); // NOI18N
        help.setName("help"); // NOI18N
        jScrollPane1.setViewportView(help);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 732, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, previewKeyboard, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel3)
                            .add(jLabel1)
                            .add(jLabel2)
                            .add(jLabel7)
                            .add(jLabel4))
                        .add(44, 44, 44)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(layout.createSequentialGroup()
                                .add(rows, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 257, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 87, Short.MAX_VALUE))
                            .add(layout.createSequentialGroup()
                                .add(name, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 257, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 87, Short.MAX_VALUE))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, rowToAdd, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 105, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(addCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 105, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                        .add(jLabel5)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(addToRow, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 37, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                        .add(jLabel6)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(addToColumn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 37, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(addButton))
                                    .add(org.jdesktop.layout.GroupLayout.TRAILING, addRow)))
                            .add(layout.createSequentialGroup()
                                .add(removeCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 247, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(removeButton)))))
                .add(2, 2, 2)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE))
        );

        layout.linkSize(new java.awt.Component[] {addButton, addRow, removeButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.linkSize(new java.awt.Component[] {name, removeCombo, rows}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.linkSize(new java.awt.Component[] {addCombo, rowToAdd}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel1)
                            .add(name, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel3)
                            .add(rows, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel2)
                            .add(addButton)
                            .add(addCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(addToRow, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(addToColumn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel5)
                            .add(jLabel6))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel7)
                            .add(rowToAdd, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(addRow))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel4)
                            .add(removeCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(removeButton))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(previewKeyboard, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE))
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == addButton) {
                VKBEditor.this.addButtonActionPerformed(evt);
            }
            else if (evt.getSource() == removeButton) {
                VKBEditor.this.removeButtonActionPerformed(evt);
            }
            else if (evt.getSource() == ok) {
                VKBEditor.this.okActionPerformed(evt);
            }
            else if (evt.getSource() == cancel) {
                VKBEditor.this.cancelActionPerformed(evt);
            }
            else if (evt.getSource() == addRow) {
                VKBEditor.this.addRowActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == rows) {
                VKBEditor.this.rowsStateChanged(evt);
            }
            else if (evt.getSource() == addToRow) {
                VKBEditor.this.addToRowStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void rowsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rowsStateChanged
        int rowCount = ((Number)rows.getValue()).intValue();
        if(inputMode != null && inputMode.length != rowCount) {
            String[][] newInputMode = new String[rowCount][];
            System.arraycopy(inputMode, 0, newInputMode, 0, Math.min(rowCount, inputMode.length));
            inputMode = newInputMode;
            for(int iter = 0 ; iter < inputMode.length ; iter++) {
                if(inputMode[iter] == null) {
                    inputMode[iter] = new String[0];
                }
            }
            updateVKB();
        }
    }//GEN-LAST:event_rowsStateChanged

    private void addToRowStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_addToRowStateChanged
        if(inputMode != null) {
            int row = ((Number)addToRow.getValue()).intValue();
            int col = ((Number)addToColumn.getValue()).intValue();
            addToColumn.setModel(new SpinnerNumberModel(Math.min(col, inputMode[row].length), 0, Math.max(0, inputMode[row].length), 1));
        }
    }//GEN-LAST:event_addToRowStateChanged

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        int[] rowColumn = (int[])removeCombo.getSelectedItem();
        List<String> l = new ArrayList<String>();
        l.addAll(Arrays.asList(inputMode[rowColumn[0]]));
        l.remove(rowColumn[1]);
        inputMode[rowColumn[0]] = new String[l.size()];
        l.toArray(inputMode[rowColumn[0]]);
        updateVKB();
    }//GEN-LAST:event_removeButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        String s = (String)addCombo.getSelectedItem();
        if(s.length() == 0) {
            return;
        }
        List<String> l = new ArrayList<String>();
        int row = ((Number)addToRow.getValue()).intValue();
        int col = ((Number)addToColumn.getValue()).intValue();
        l.addAll(Arrays.asList(inputMode[row]));
        l.add(col, s);
        inputMode[row] = new String[l.size()];
        l.toArray(inputMode[row]);
        updateVKB();
    }//GEN-LAST:event_addButtonActionPerformed

    private void okActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okActionPerformed
        dispose();
}//GEN-LAST:event_okActionPerformed

    private void cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelActionPerformed
        canceled = true;
        dispose();
    }//GEN-LAST:event_cancelActionPerformed

    private void addRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRowActionPerformed
        String s = rowToAdd.getText();;
        if(s.length() == 0) {
            return;
        }
        List<String> l = new ArrayList<String>();
        int row = ((Number)addToRow.getValue()).intValue();
        int col = ((Number)addToColumn.getValue()).intValue();
        l.addAll(Arrays.asList(inputMode[row]));
        for(int iter = 0 ; iter < s.length() ; iter++) {
            l.add(col, "" + s.charAt(iter));
        }
        inputMode[row] = new String[l.size()];
        l.toArray(inputMode[row]);
        updateVKB();
    }//GEN-LAST:event_addRowActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JComboBox addCombo;
    private javax.swing.JButton addRow;
    private javax.swing.JSpinner addToColumn;
    private javax.swing.JSpinner addToRow;
    private javax.swing.JButton cancel;
    private javax.swing.JTextPane help;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField name;
    private javax.swing.JButton ok;
    private javax.swing.JPanel previewKeyboard;
    private javax.swing.JButton removeButton;
    private javax.swing.JComboBox removeCombo;
    private javax.swing.JTextField rowToAdd;
    private javax.swing.JSpinner rows;
    // End of variables declaration//GEN-END:variables

}
