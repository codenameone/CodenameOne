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

import com.codename1.designer.ResourceEditorView;
import com.codename1.ui.util.EditableResources;
import java.awt.Component;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;

/**
 * Used by the UI builder to edit commands within the UI
 *
 * @author Shai Almog
 */
public class CommandEditor extends javax.swing.JPanel {
    private Properties projectGeneratorSettings;
    private String uiName;

    /** Creates new form CommandEditor */
    public CommandEditor(ActionCommand cmd, EditableResources res, String uiName, List<com.codename1.ui.Command> commands, Properties projectGeneratorSettings) {
        this.projectGeneratorSettings = projectGeneratorSettings;
        this.uiName = uiName;
        initComponents();
        goToSource.setEnabled(projectGeneratorSettings != null);
        com.codename1.ui.Command[] existing = new com.codename1.ui.Command[commands.size() + 1];
        existing[0] = null;
        for(int iter = 1 ; iter < existing.length ; iter++) {
            existing[iter] = commands.get(iter - 1);
        }

        Vector postActions = new Vector();
        postActions.addElement("None");
        Vector actions = new Vector();
        actions.addElement("None");
        actions.addElement("Minimize");
        actions.addElement("Exit");
        actions.addElement("Execute");
        actions.addElement("Back");
        backCommand.setSelected(cmd.isBackCommand());

        String[] uiEntries = new String[res.getUIResourceNames().length];
        System.arraycopy(res.getUIResourceNames(), 0, uiEntries, 0, uiEntries.length);
        Arrays.sort(uiEntries);

        // prevent the current form from appearing in the navigation combo box
        for(String uis : uiEntries) {
            if(!uiName.equals(uis)) {
                actions.addElement(uis);
                postActions.addElement(uis);
            }
        }
        action.setModel(new DefaultComboBoxModel(actions));
        postAction.setModel(new DefaultComboBoxModel(postActions));
        String a = cmd.getAction();
        if(a != null) {
            if(a.startsWith("@")) {
                a = a.substring(1);
                asynchronous.setSelected(true);
            } else {
                if(a.startsWith("!")) {
                    a = a.substring(1);
                    String[] arr = a.split(";");
                    action.setSelectedItem(arr[0]);
                    postAction.setSelectedItem(arr[1]);
                } else {
                    if(a.startsWith("$")) {
                        a = a.substring(1);
                    }
                }
            }
        }
        action.setSelectedItem(a);
        name.setText(cmd.getCommandName());
        id.setModel(new SpinnerNumberModel(cmd.getId(), -10000, Integer.MAX_VALUE, 1));
        ResourceEditorView.initImagesComboBox(icon, res, false, true);
        icon.setSelectedItem(cmd.getIcon());
        ResourceEditorView.initImagesComboBox(rollover, res, false, true);
        rollover.setSelectedItem(cmd.getRolloverIcon());
        ResourceEditorView.initImagesComboBox(pressed, res, false, true);
        pressed.setSelectedItem(cmd.getPressedIcon());
        ResourceEditorView.initImagesComboBox(disabled, res, false, true);
        disabled.setSelectedItem(cmd.getDisabledIcon());
    }

    private String getCommandAction() {
        if(action.getSelectedIndex() == 0 || action.getSelectedItem() == null) {
            return "";
        }

        if(action.getSelectedIndex() < 5) {
            return "$" + action.getSelectedItem();
        }

        if(postAction.getSelectedIndex() > 0 && action.getSelectedIndex() >= 5) {
            return "!" + action.getSelectedItem() + ";" + postAction.getSelectedItem();
        }

        if(asynchronous.isSelected()) {
            return "@" + action.getSelectedItem();
        }
        return (String)action.getSelectedItem();
    }

    public ActionCommand getResult() {
        ActionCommand ac = new ActionCommand(name.getText(), (com.codename1.ui.Image)icon.getSelectedItem(),
                ((Number)id.getValue()).intValue(), getCommandAction(), backCommand.isSelected(), commandArgument.getText());
        ac.setRolloverIcon((com.codename1.ui.Image)rollover.getSelectedItem());
        ac.setPressedIcon((com.codename1.ui.Image)pressed.getSelectedItem());
        ac.setDisabledIcon((com.codename1.ui.Image)disabled.getSelectedItem());
        return ac;
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
        jTextPane1 = new javax.swing.JTextPane();
        jPanel1 = new javax.swing.JPanel();
        pressed = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        asynchronous = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        action = new javax.swing.JComboBox();
        commandArgument = new javax.swing.JTextField();
        id = new javax.swing.JSpinner();
        name = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        disabled = new javax.swing.JComboBox();
        rollover = new javax.swing.JComboBox();
        postAction = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        icon = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        backCommand = new javax.swing.JCheckBox();
        goToSource = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();

        FormListener formListener = new FormListener();

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jTextPane1.setContentType("text/html");
        jTextPane1.setEditable(false);
        jTextPane1.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      Commands are the working horse of a Codename One applications, they allow most of the common actions within the application and are \nthe basic building blocks for menus. A command is identified in Codename One based on its name and id although the name can be left \nblank for a command which is only identified by an icon. \r\n    </p>\r\n    <p>\n      The id can be modified to anything the user sees fit in order to identify the command in the application code although \nthe Codename One designer makes a \"best effort\" to create a unique id to ease developers work. While behavior for \ncommands can be defined entirely in the code common actions can be defined in the combo boxes here including navigating to \na specific form, exiting, minimizing etc.\n    </p>\n    <p>\n       A command can be marked as a back command to indcate that it should be executed by the back button it also hints\nthat when the command navigation occurs the transition is run in reverse to indciate going back.  A command may be marked\nas asynchronous to indicate that running it will require a background process.<br>\nFor a command that may perform a heavy operation asynchronously a developer can specify a destination that should\nbe navigated to after the background operation completed.\n    </p>\n  </body>\r\n</html>\r\n"); // NOI18N
        jTextPane1.setName("jTextPane1"); // NOI18N
        jScrollPane1.setViewportView(jTextPane1);

        jPanel1.setName("jPanel1"); // NOI18N

        pressed.setName("pressed"); // NOI18N

        jLabel4.setText("Action");
        jLabel4.setName("jLabel4"); // NOI18N

        asynchronous.setName("asynchronous"); // NOI18N

        jLabel3.setText("Icon");
        jLabel3.setName("jLabel3"); // NOI18N

        jLabel2.setText("Id");
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel12.setText("Disabled Icon");
        jLabel12.setName("jLabel12"); // NOI18N

        jLabel1.setText("Name");
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel9.setText("Rollover Icon");
        jLabel9.setName("jLabel9"); // NOI18N

        action.setName("action"); // NOI18N
        action.addActionListener(formListener);

        commandArgument.setEnabled(false);
        commandArgument.setName("commandArgument"); // NOI18N

        id.setName("id"); // NOI18N

        name.setColumns(15);
        name.setName("name"); // NOI18N

        jLabel10.setText("Argument");
        jLabel10.setName("jLabel10"); // NOI18N

        jLabel11.setText("Pressed Icon");
        jLabel11.setName("jLabel11"); // NOI18N

        disabled.setName("disabled"); // NOI18N

        rollover.setName("rollover"); // NOI18N

        postAction.setName("postAction"); // NOI18N

        jLabel6.setText("Asynchronous");
        jLabel6.setName("jLabel6"); // NOI18N

        icon.setName("icon"); // NOI18N

        jLabel8.setText("Post Action Go To");
        jLabel8.setName("jLabel8"); // NOI18N

        backCommand.setName("backCommand"); // NOI18N

        goToSource.setText("Go To Source");
        goToSource.setName("goToSource"); // NOI18N
        goToSource.addActionListener(formListener);

        jLabel5.setText("Back Command");
        jLabel5.setName("jLabel5"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel1)
                            .add(jLabel2)
                            .add(jLabel3)
                            .add(jLabel9)
                            .add(jLabel11)
                            .add(jLabel12)
                            .add(jLabel4)
                            .add(jLabel8)
                            .add(jLabel10)
                            .add(jLabel5)
                            .add(jLabel6))
                        .add(11, 11, 11)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel1Layout.createSequentialGroup()
                                .add(asynchronous)
                                .add(165, 165, 165))
                            .add(jPanel1Layout.createSequentialGroup()
                                .add(backCommand)
                                .add(165, 165, 165))
                            .add(commandArgument, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                            .add(postAction, 0, 179, Short.MAX_VALUE)
                            .add(action, 0, 179, Short.MAX_VALUE)
                            .add(disabled, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 186, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(pressed, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 186, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(rollover, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 186, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(icon, 0, 179, Short.MAX_VALUE)
                            .add(id, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                            .add(name, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(89, 89, 89)
                        .add(goToSource)))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(new java.awt.Component[] {action, commandArgument, disabled, icon, id, name, postAction, pressed, rollover}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(name, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(id, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel3)
                    .add(icon, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9)
                    .add(rollover, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel11)
                    .add(pressed, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel12)
                    .add(disabled, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(action, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(postAction, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(commandArgument, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel5)
                    .add(backCommand))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(asynchronous))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(goToSource)
                .addContainerGap(48, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(6, 6, 6)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == action) {
                CommandEditor.this.actionActionPerformed(evt);
            }
            else if (evt.getSource() == goToSource) {
                CommandEditor.this.goToSourceActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void goToSourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goToSourceActionPerformed
        try {
            File destFile = new File(projectGeneratorSettings.getProperty("userClassAbs"));
            if(!destFile.exists()) {
                JOptionPane.showMessageDialog(this, "File not fount:\n" + destFile.getAbsolutePath(), "File Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DataInputStream input = new DataInputStream(new FileInputStream(destFile));
            byte[] data = new byte[(int)destFile.length()];
            input.readFully(data);
            input.close();
            String fileContent = new String(data);
            int line = -1;
            String commandName = name.getText();
            if(commandName.length() == 0) {
                commandName = "Command" + ((Number)id.getValue()).intValue();
            }
            String methodName = "on" + ResourceEditorView.normalizeFormName(uiName) +
                     ResourceEditorView.normalizeFormName(commandName);
            int pos = fileContent.indexOf("boolean " + methodName + "(");
            if(pos > -1) {
                line = UserInterfaceEditor.charIndexToFileLine(pos, fileContent);
            } else {
                // assuming one class per file...
                pos = fileContent.lastIndexOf('}');

                line = UserInterfaceEditor.charIndexToFileLine(pos, fileContent) + 4;
                fileContent = fileContent.substring(0, pos) +
                        "\n    protected boolean " + methodName + "() {\n" +
                        "        // If the resource file changes the names of components this call will break notifying you that you should fix the code\n" +
                        "        boolean val = super." + methodName +"();\n" +
                        "        \n" +
                        "        return val;\n" +
                        "    }\n" +
                        fileContent.substring(pos);
                Writer output = new FileWriter(destFile);
                output.write(fileContent);
                output.close();
            }
            ResourceEditorView.openInIDE(destFile, line);
        } catch(IOException err) {
            err.printStackTrace();
            JOptionPane.showMessageDialog(this, "An IO exception occured: " + err, "IO Exception", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_goToSourceActionPerformed

    private void actionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_actionActionPerformed
        commandArgument.setEnabled(action.getSelectedIndex() == 3);
    }//GEN-LAST:event_actionActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox action;
    private javax.swing.JCheckBox asynchronous;
    private javax.swing.JCheckBox backCommand;
    private javax.swing.JTextField commandArgument;
    private javax.swing.JComboBox disabled;
    private javax.swing.JButton goToSource;
    private javax.swing.JComboBox icon;
    private javax.swing.JSpinner id;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextField name;
    private javax.swing.JComboBox postAction;
    private javax.swing.JComboBox pressed;
    private javax.swing.JComboBox rollover;
    // End of variables declaration//GEN-END:variables

}
