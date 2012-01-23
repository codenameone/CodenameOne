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
import java.util.HashMap;
import java.util.Map;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * Allows adding constants to the LWUIT theme
 *
 * @author Shai Almog
 */
public class ConstantEditor extends javax.swing.JPanel {
    private Map<String, String> helpMap = new HashMap<String, String>();
    private EditableResources res;

    /** Creates new form ConstantEditor */
    public ConstantEditor(String key, Object valueObject, EditableResources res) {
        this.res = res;
        initComponents();
        String value;
        if(valueObject instanceof com.codename1.ui.Image) {
            value = res.findId(valueObject);
        } else {
            value = (String)valueObject;
        }
        ResourceEditorView.initImagesComboBox(valueCombo, res, true, false);
        helpMap.put("includeNativeBool", "Indicates whether the theme should derive the native OS theme as the basis. This means the current theme will look different in every OS.");
        helpMap.put("centeredPopupBool", "Valid values: true/false. Indicates that a combo box popup should be centered on the screen.");
        helpMap.put("comboImage", "Valid values: image name. Indicates the image of the combo box down arrow");
        helpMap.put("commandBehavior", "Valid values: SoftKey, Touch, Bar, Title, Native. <ol><li>Softkey - indicates the standard default non-touch optimized menus<li>touch - indicates the touch optimized menus<li>bar - indicates a constantly showing bar at the bottom of the screen<li>Title - same as bar, only with a back command embedded in the title<li>Native - indicates a native command when applicable</ol>");
        helpMap.put("checkBoxCheckedImage", "Valid values: image name. Indicates the image of the checked check box");
        helpMap.put("checkBoxUncheckedImage", "Valid values: image name. Indicates the image of the unchecked check box");
        helpMap.put("radioSelectedImage", "Valid values: image name. Indicates the image of the selected radio button");
        helpMap.put("radioUnselectedImage", "Valid values: image name. Indicates the image of the unselected radio button");
        helpMap.put("dialogButtonCommandsBool", "Valid values: true/false. Indicates that commands added to dialogs should appear as buttons, useful for blackberry and touch devices where softbuttons aren't the norm");
        helpMap.put("dialogPosition", "Valid values (<b>case sensitive</b>): North, South, East, West, Center. Indicates the default position for LWUIT dialogs on the screen");
        helpMap.put("dialogTransitionIn", "Valid values: slide, fade. Indicates the default transition when entering a dialog");
        helpMap.put("dialogTransitionOut", "Valid values: slide, fade. Indicates the default transition when leaving a dialog");
        helpMap.put("disabledColor", "Valid values: hexedecimal number e.g. cccccc. Indicates the text color for a disabled component");
        helpMap.put("dlgCommandButtonSizeInt", "Valid values: positive ints. Indicates the minimum size width for a button command in a dialog");
        helpMap.put("formTransitionIn", "Valid values: slide, fade. Indicates the default transition when entering a form");
        helpMap.put("formTransitionOut", "Valid values: slide, fade. Indicates the default transition when leaving a form");
        helpMap.put("hideEmptyTitleBool", "Valid values: true/false. Indicates that titles without text should be hidden from view in a dialog, this is useful for some elaborate dialog designs.");
        helpMap.put("menuHeightPercent", "Valid values: 1-99. Percentage of the screen that should be occupied by the menu dialog when the softbutton Menu option is opened");
        helpMap.put("menuTransitionIn", "Valid values: slide, fade. Indicates the default transition when opening a menu");
        helpMap.put("menuTransitionOut", "Valid values: slide, fade. Indicates the default transition when closing a menu");
        helpMap.put("menuWidthPercent", "Valid values: 1-99. Percentage of the screen that should be occupied by the menu dialog when the softbutton Menu option is opened");
        helpMap.put("otherPopupRendererBool", "Valid values: true/false. Indicates that a separate renderer UIID should be used for the combo box popup using the PopupFocus/PopupItem UIID's.");
        helpMap.put("popupCancelBodyBool", "Valid values: true/false. Indicates that a cancel button should appear within the combo box popup.");
        helpMap.put("popupTitleBool", "Valid values: true/false. Show a title when opening a popup, which displays the labelFor value.");
        helpMap.put("pureTouchBool", "Valid values: true/false. Pure touch indicates that selection isn't painted when the user is using the touch screen and not pressing the display. This is similar to the behavior of Android/iPhone devices, when the keypad is used selection is rendered as usual");
        helpMap.put("rendererShowsNumbersBool", "Valid values: true/false. Indicates that lists (e.g. the menu list) should/should not show numbers next to the text");
        helpMap.put("reverseSoftButtonsBool", "Valid values: true/false. Indicates that the softbuttons should be reversed in order");
        helpMap.put("slideDirection", "Valid values: horizontal/vertical. Indicates the direction for a slide transition defined using one of the transition constants.");
        helpMap.put("tabsFillRowsBool", "Valid values: true/false. Indicates that tabs on top/bottom should fill up available face");
        helpMap.put("tickerSpeedInt", "Valid values: 1-10000. Indicates the speed for the ticker animation in buttons etc.");
        helpMap.put("tintColor", "Valid values: hexedecimal AARRGGBB number e.g. 77FF0000. Indicates the color to paint the form when showing a dialog on top of the form. This can be disabled by setting the value to 0.");
        helpMap.put("touchCommandFlowBool", "Valid values: true/false. Indicates that the touch menu should use a flow layout rather than a grid layout");
        helpMap.put("transitionSpeedInt", "Valid values: 0-20000. The speed for transitions between form, dialog, menu etc. only valid for transitions defined in the constants here.");
        helpMap.put("alwaysTensileBool", "Enables tensile drag even when there is no scrolling in the container (only for scrollable containers though)");
        helpMap.put("ComponentGroupBool", "Enables component group which allows components to be logically grouped together so the UIID's of components would be modified based on their group placement. This allows for some unique styling effects where the first/last elements have different styles from the rest of the elements. Its disabled by default thus leaving its usage up to the designer.");
        helpMap.put("dlgCommandGridBool", "Places the dialog commands in a grid for uniform sizes");
        helpMap.put("textFieldCursorColorInt", "The color of the cursor as an integer (not hex)");
        helpMap.put("textCmpVAlignInt", "The vertical alignment of the text component: TOP = 0, CENTER = 4, BOTTOM = 2");
        helpMap.put("PackTouchMenuBool", "Enables preferred sized packing of the touch menu (true by default), when set to false this allows manually determining the touch menu size using percentages");
        helpMap.put("tabPlacementInt", "The placement of the tabs in the Tabs component: TOP = 0, BOTTOM = 2, LEFT = 1, RIGHT = 3");
        help.setText("<html><body>" + helpMap.get("comboImage") + "</body></html>");
        if(key != null) {
            constant.setSelectedItem(key.substring(1, key.length()));
            this.value.setText(value);
            updateComboValue(key);
        }
        ((JTextComponent)constant.getEditor().getEditorComponent()).getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                update(e);
            }

            private void update(DocumentEvent e) {
                try {
                    updateComboValue(e.getDocument().getText(0, e.getDocument().getLength()));
                } catch(Throwable t) {}
            }

            public void removeUpdate(DocumentEvent e) {
                update(e);
            }

            public void changedUpdate(DocumentEvent e) {
                update(e);
            }
        });
    }

    public String getConstant() {
        return "@" + (String)constant.getSelectedItem();
    }

    public Object getValue() {
        if(((String)constant.getSelectedItem()).endsWith("Image")) {
            return res.getImage(value.getText());
        }
        return value.getText();
    }

    public boolean isValidState() {
        return getConstant().length() > 0;
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
        constant = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        value = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        help = new javax.swing.JEditorPane();
        valueCombo = new javax.swing.JComboBox();
        valueBoolean = new javax.swing.JCheckBox();
        valueSpin = new javax.swing.JSpinner();

        FormListener formListener = new FormListener();

        jLabel1.setText("Constant");
        jLabel1.setName("jLabel1"); // NOI18N

        constant.setEditable(true);
        constant.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "alwaysTensileBool", "centeredPopupBool", "checkBoxCheckDisImage", "checkBoxCheckedImage", "checkBoxUncheckDisImage", "checkBoxUncheckedImage", "comboImage", "commandBehavior", "ComponentGroupBool", "defaultCommandImage", "dialogButtonCommandsBool", "dialogPosition", "dialogTransitionIn", "dialogTransitionInImage", "dialogTransitionOut", "dialogTransitionOutImage", "disabledColor", "dlgCommandButtonSizeInt", "dlgCommandGridBool", "dlgSlideDirection", "dlgSlideInDirBool", "dlgSlideOutDirBool", "fadeScrollBarBool", "fadeScrollEdgeBool", "fadeScrollEdgeInt", "firstCharRTLBool", "fixedSelectionInt", "formTransitionIn", "formTransitionInImage", "formTransitionOut", "formTransitionOutImage", "hideEmptyTitleBool", "ignorListFocusBool", "includeNativeBool", "listItemGapInt", "menuHeightPercent", "menuPrefSizeBool", "menuSlideDirection", "menuSlideInDirBool", "menuSlideOutDirBool", "menuTransitionIn", "menuTransitionInImage", "menuTransitionOut", "menuTransitionOutImage", "menuWidthPercent", "otherPopupRendererBool", "PackTouchMenuBool", "popupCancelBodyBool", "popupTitleBool", "pureTouchBool", "radioSelectedDisImage", "radioSelectedImage", "radioUnselectedDisImage", "radioUnselectedImage", "rendererShowsNumbersBool", "reverseSoftButtonsBool", "slideDirection", "slideInDirBool", "slideOutDirBool", "snapGridBool", "tabPlacementInt", "tabsFillRowsBool", "tabsGridBool", "textCmpVAlignInt", "textFieldCursorColorInt", "tickerSpeedInt", "tintColor", "touchCommandFillBool", "touchCommandFlowBool", "transitionSpeedInt" }));
        constant.setName("constant"); // NOI18N
        constant.addActionListener(formListener);

        jLabel2.setText("Value");
        jLabel2.setName("jLabel2"); // NOI18N

        value.setEnabled(false);
        value.setName("value"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setName("help"); // NOI18N
        jScrollPane1.setViewportView(help);

        valueCombo.setEnabled(false);
        valueCombo.setName("valueCombo"); // NOI18N
        valueCombo.addActionListener(formListener);

        valueBoolean.setName("valueBoolean"); // NOI18N
        valueBoolean.addActionListener(formListener);

        valueSpin.setEnabled(false);
        valueSpin.setName("valueSpin"); // NOI18N
        valueSpin.addChangeListener(formListener);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel1)
                            .add(jLabel2))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(value)
                            .add(constant, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(valueCombo, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(valueBoolean)
                            .add(valueSpin))))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {constant, value}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(constant, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(value, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(valueCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(valueBoolean)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(valueSpin, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == constant) {
                ConstantEditor.this.constantActionPerformed(evt);
            }
            else if (evt.getSource() == valueCombo) {
                ConstantEditor.this.valueComboActionPerformed(evt);
            }
            else if (evt.getSource() == valueBoolean) {
                ConstantEditor.this.valueBooleanActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == valueSpin) {
                ConstantEditor.this.valueSpinStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void valueComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_valueComboActionPerformed
        if(valueCombo.isEnabled()) {
            value.setText((String)valueCombo.getSelectedItem());
        }
    }//GEN-LAST:event_valueComboActionPerformed

    private void valueBooleanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_valueBooleanActionPerformed
        if(valueBoolean.isEnabled()) {
            if(valueBoolean.isSelected()) {
                value.setText("true");
            } else {
                value.setText("false");
            }
        }
    }//GEN-LAST:event_valueBooleanActionPerformed

    private void valueSpinStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_valueSpinStateChanged
        if(valueSpin.isEnabled()) {
            value.setText(valueSpin.getValue().toString());
        }
    }//GEN-LAST:event_valueSpinStateChanged

    private void updateComboValue(String selection) {
        String val = helpMap.get((String)constant.getSelectedItem());
        if(selection.endsWith("Image")) {
            valueCombo.setSelectedItem(value.getText());
            valueCombo.setEnabled(true);
            value.setEnabled(false);
            valueBoolean.setEnabled(false);
            valueSpin.setEnabled(false);
        } else {
            if(selection.endsWith("Bool")) {
                valueBoolean.setSelected("true".equalsIgnoreCase(value.getText()));
                valueCombo.setEnabled(false);
                value.setEnabled(false);
                valueBoolean.setEnabled(true);
                valueSpin.setEnabled(false);
            } else {
                if(selection.endsWith("Int")) {
                    valueCombo.setEnabled(false);
                    value.setEnabled(false);
                    valueBoolean.setEnabled(false);
                    valueSpin.setEnabled(true);
                    int current = 0;
                    try {
                        current = Integer.parseInt(value.getText());
                    } catch(Exception e) {}
                    valueSpin.setModel(new SpinnerNumberModel(current, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
                } else {
                    if(selection.endsWith("Percent")) {
                        valueCombo.setEnabled(false);
                        value.setEnabled(false);
                        valueBoolean.setEnabled(false);
                        valueSpin.setEnabled(true);
                        int current = 0;
                        try {
                            current = Math.max(0, Math.min(Integer.parseInt(value.getText()), 100));
                        } catch(Exception e) {}
                        valueSpin.setModel(new SpinnerNumberModel(current, 0, 100, 1));
                    } else {
                        valueCombo.setEnabled(false);
                        value.setEnabled(true);
                        valueBoolean.setEnabled(false);
                        valueSpin.setEnabled(false);
                    }
                }
            }
        }
        if(val != null) {
            help.setText("<html><body>" + val + "</body></html>");
        } else {
            help.setText("<html><body>...</body></html>");
        }
    }

    private void constantActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_constantActionPerformed
        updateComboValue((String)constant.getSelectedItem());
    }//GEN-LAST:event_constantActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox constant;
    private javax.swing.JEditorPane help;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField value;
    private javax.swing.JCheckBox valueBoolean;
    private javax.swing.JComboBox valueCombo;
    private javax.swing.JSpinner valueSpin;
    // End of variables declaration//GEN-END:variables

}
