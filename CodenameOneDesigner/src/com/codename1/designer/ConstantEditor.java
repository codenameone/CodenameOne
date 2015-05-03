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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * Allows adding constants to the CodenameOne theme
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
        // Generated with sed '/^$/d' ~/temp/test.txt | tr "\t" " " | paste -d " "  - - | sed 's/^\|/        helpMap.put("/'  | sed 's/\ |/", "/' | sed 's/$/");/' | sed 's/ "/"/'
        helpMap.put("alwaysTensileBool", "Enables tensile drag even when there is no scrolling in the container (only for scrollable containers though)");
        helpMap.put("backGestureThresholdInt","The threshold for the back gesture in the SwipeBackSupport class, defaults to 5");
        helpMap.put("backUsesTitleBool","Indicates to the GUI builder that the back command should use the title of the previous form and not just the word \"Back\"");
        helpMap.put("defaultCommandImage", "Image to give a command with no icon");
        helpMap.put("dialogButtonCommandsBool", "Place commands in the dialogs as buttons");
        helpMap.put("dialogPosition", "Place the dialog in an arbitrary border layout position (e.g. North, South, Center etc.)");
        helpMap.put("centeredPopupBool", "Popup of the combo box will appear in the center of the screen");
        helpMap.put("changeTabOnFocusBool","Usefull for feature phones, allows changing the tab when the focus changes immediately without pressing a key");
        helpMap.put("checkBoxCheckDisImage","CheckBox image to use instead of Codename One drawing it on its own");
        helpMap.put("checkBoxCheckedImage", "CheckBox image to use instead of Codename One drawing it on its own");
        helpMap.put("checkBoxOppositeSideBool","Indicates the check box should be drawn on the opposite side to the text and not next to the text");
        helpMap.put("checkBoxUncheckDisImage", "CheckBox image to use instead of Codename One drawing it on its own");
        helpMap.put("checkBoxUncheckedImage", "CheckBox image to use instead of Codename One drawing it on its own");
        helpMap.put("comboImage", "Combo image to use instead of Codename One drawing it on its own");
        helpMap.put("commandBehavior", "Indicates how commands should act, as a touch menu, native menu etc. Possible values: SoftKey, Touch, Bar, Title, Right, Native");
        helpMap.put("ComponentGroupBool", "Enables component group which allows components to be logically grouped together so the UIID's of components would be modified based on their group placement. This allows for some unique styling effects where the first/last elements have different styles from the rest of the elements. Its disabled by default thus leaving its usage up to the designer.");
        helpMap.put("dialogTransitionIn", "Default transition for dialog");
        helpMap.put("dialogTransitionInImage", "Default transition Image for dialog, causes a Timeline transition effect");
        helpMap.put("dialogTransitionOut", "Default transition for dialog");
        helpMap.put("defaultCommandImage","An image to place on a command if none is defined, only applies to touch commands");
        helpMap.put("defaultEmblemImage","The emblem painted on the side of the multibutton, by default this is an arrow on some platforms");
        helpMap.put("dialogTransitionOutImage", "Default transition Image for dialog, causes a Timeline transition effect");
        helpMap.put("disabledColor", "Color to use when disabling entries by default");
        helpMap.put("dlgButtonCommandUIID","The UIID used for dialog button commands");
        helpMap.put("dlgCommandButtonSizeInt", "Minimum size to give to command buttons in the dialog");
        helpMap.put("dlgCommandGridBool", "Places the dialog commands in a grid for uniform sizes");
        helpMap.put("dlgInvisibleButtons","Includes an RRGGBB color for the line separating dialog buttons as is the case with Android 4 and iOS 7 buttons in dialogs");
        helpMap.put("dlgSlideDirection", "Slide hints");
        helpMap.put("dlgSlideInDirBool", "Slide hints");
        helpMap.put("dlgSlideOutDirBool", "Slide hints");
        helpMap.put("drawMapPointerBool","Indicates whether a pointer should appear in the center of the map component");
        helpMap.put("fadeScrollBarBool ", "Boolean indicating if the scrollbar show fade when there is inactivity");
        helpMap.put("fadeScrollEdgeBool", "Places a fade effect at the edges of the screen to indicate that its possible to scroll until we reach the edge (common on Android).");
        helpMap.put("fadeScrollEdgeInt", "Amount of pixels to fade out at the edge");
        helpMap.put("firstCharRTLBool", "Indicates to the GenericListCellRenderer that it should determine RTL status based on the first character in the sentence");
        helpMap.put("noTextModeBool","Indicates that the on-off switch in iOS shouldn't draw text on top of the switch which is the case for iOS 7+ but not for prior versions");
        helpMap.put("fixedSelectionInt", "Number corresponding to the fixed selection constants in List");
        helpMap.put("formTransitionIn", "Default transition for form");
        helpMap.put("formTransitionInImage", "Default transition Image for form, causes a Timeline transition effect");
        helpMap.put("formTransitionOut", "Default transition for form");
        helpMap.put("formTransitionOutImage", "Default transition Image for form, causes a Timeline transition effect");
        helpMap.put("hideBackCommandBool","Hides the back command from the side menu when possible");
        helpMap.put("hideEmptyTitleBool", "Indicates that a title with no content should be hidden even if the border for the title occupies space");
        helpMap.put("hideLeftSideMenuBool","Hides the side menu icon that appears on the left side of the UI");
        helpMap.put("ignorListFocusBool", "Hide the focus component of the list when the list doesn't have focus");
        helpMap.put("infiniteImage","The image used by the infinite progress component, the component will rotate it as needed");
        helpMap.put("includeNativeBool", "True to derive from the platform native theme, false to create a blank theme that only uses the basic defaults.");
        helpMap.put("listItemGapInt ", "Builtin item gap in the list, this defaults to 2 which predated padding/margin in Codename One");
        helpMap.put("listLongPressBool","Defaults to true, indicates whether a list should handle long press events");
        helpMap.put("mapTileLoadingImage","An image to preview while loading the `MapComponent` tile");
        helpMap.put("mapTileLoadingText","The text of the tiles in the `MapComponent` during loading, defaults to \"Loading...\"");
        helpMap.put("mapZoomButtonsBool","Indicates whether buttons should be drawn on the map component");
        helpMap.put("mediaBackImage","Media icon used by the media player class");
        helpMap.put("mediaFwdImage","Media icon used by the media player class");
        helpMap.put("mediaPauseImage","Media icon used by the media player class");
        helpMap.put("mediaPlayImage","Media icon used by the media player class");
        helpMap.put("menuHeightPercent", "Allows positioning and sizing the menu");
        helpMap.put("menuImage","The three dot menu image used in Android and the Toolbar to show additional command entries");
        helpMap.put("menuPrefSizeBool", "Allows positioning and sizing the menu");
        helpMap.put("menuSlideDirection", "Defines menu entrance effect");
        helpMap.put("menuSlideInDirBool", "Defines menu entrance effect");
        helpMap.put("menuSlideOutDirBool", "Defines menu entrance effect");
        helpMap.put("menuTransitionIn", "Defines menu entrance effect");
        helpMap.put("menuTransitionInImage", "Defines menu entrance effect");
        helpMap.put("menuTransitionOut", "Defines menu exit effect");
        helpMap.put("menuTransitionOutImage", "Defines menu entrance effect");
        helpMap.put("menuWidthPercent", "Allows positioning and sizing the menu");
        helpMap.put("minimizeOnBackBool", "Indicates whether the form should minimize the entire application when the physical back button is pressed (if available) and no command is defined as the back command. Defaults to true.");
        helpMap.put("onOffIOSModeBool","Indicates whether the on-off switch should use the iOS or Android mode");
        helpMap.put("otherPopupRendererBool","Indicates that a separate renderer UIID/instance should be used to the list within the combo box popup");
        helpMap.put("PackTouchMenuBool", "Enables preferred sized packing of the touch menu (true by default), when set to false this allows manually determining the touch menu size using percentages");
        helpMap.put("paintsTitleBarBool","Indicates that the StatusBar UIID should be added to the top of the form to space down the title area as is the case on iOS 7+ where the status bar is painted on top of the UI");
        helpMap.put("popupCancelBodyBool", "Indicates that a cancel button should appear within the combo box popup");
        helpMap.put("PopupDialogArrowBool","Indicates whether the popup dialog has an arrow, notice that this constant will change if you change UIID of the popup dialog");
        helpMap.put("PopupDialogArrowBottomImage","Image of the popup dialog arrow, notice that this constant will change if you change UIID of the popup dialog");
        helpMap.put("PopupDialogArrowTopImage","Image of the popup dialog arrow, notice that this constant will change if you change UIID of the popup dialog");
        helpMap.put("PopupDialogArrowLeftImage","Image of the popup dialog arrow, notice that this constant will change if you change UIID of the popup dialog");
        helpMap.put("PopupDialogArrowRightImage","Image of the popup dialog arrow, notice that this constant will change if you change UIID of the popup dialog");
        helpMap.put("popupNoTitleAddPaddingInt","Adds padding to a popup when no title is present");
        helpMap.put("popupTitleBool", "Indicates that a title should appear within the combo box popup");
        helpMap.put("pullToRefreshImage","The arrow image used to draw the `pullToRefresh` animation");
        helpMap.put("pureTouchBool", "Indicates the pure touch mode");
        helpMap.put("radioOppositeSideBool","Indicates the radio button should be drawn on the opposite side to the text and not next to the text");
        helpMap.put("radioSelectedDisImage", "Radio button image");
        helpMap.put("radioSelectedImage", "Radio button image");
        helpMap.put("radioUnselectedDisImage", "Radio button image");
        helpMap.put("radioUnselectedImage", "Radio button image");
        helpMap.put("releaseRadiusInt","Indicates the distance from the button with dragging in which the button should be released, defaults to 0");
        helpMap.put("rendererShowsNumbersBool", "Indicates whether renderers should render the entry number");
        helpMap.put("reverseSoftButtonsBool", "Swaps the softbutton positions");
        helpMap.put("rightSideMenuImage","Same as sideMenuImage only for the right side, optional and defaults to sideMenuImage");
        helpMap.put("rightSideMenuPressImage","Same as sideMenuPressImage only for the right side, optional and defaults to sideMenuPressImage");
        helpMap.put("showBackCommandOnTitleBool","Used by the Toolbar API to indicate whether the back button should appear on the title");
        helpMap.put("shrinkPopupTitleBool","Indicates the title of the popup should be set to 0 if its missing");
        helpMap.put("sideMenuAnimSpeedInt","The speed at which a sidemenu moves defaults to 300 milliseconds");
        helpMap.put("sideMenuFoldedSwipeBool","Indicates the side menu could be opened via swiping");
        helpMap.put("sideMenuImage","The image representing the side menu, three lines (Hamburger menu)");
        helpMap.put("sideMenuPressImage","Optional pressed version of the sideMenuImage");
        helpMap.put("sideMenuShadowBool","Indicates whether the shadow for the side menu should be drawn");
        helpMap.put("sideMenuShadowImage","The image used when drawing the shadow (a default is used if this isn't supplied)");
        helpMap.put("sideMenuSizeTabPortraitInt","The size of the side menu when expanded in a tablet in portrait mode");
        helpMap.put("sideMenuSizePortraitInt","The size of the side menu when expanded in a phone in portrait mode");
        helpMap.put("sideMenuSizeTabLandscapeInt","The size of the side menu when expanded in a tablet in landscape mode");
        helpMap.put("sideMenuSizeLandscapeInt","The size of the side menu when expanded in a phone in landscape mode");
        helpMap.put("sideMenuTensileDragBool","Enables/disables the tensile drag behavior within the opened side menu");
        helpMap.put("sideSwipeActivationInt","Indicates the threshold in the side menu bar at which a swipe should trigger activation, defaults to 15 (percent)");
        helpMap.put("sideSwipeSensitiveInt","Indicates the region of the screen that is sensitive to side swipe in the side menu bar, defaults to 10 (percent)");
        helpMap.put("slideDirection", "Default slide transition settings");
        helpMap.put("slideInDirBool", "Default slide transition settings");
        helpMap.put("slideOutDirBool", "Default slide transition settings");
        helpMap.put("sliderThumbImage","The thumb image that can appear on the sliders");
        helpMap.put("snapGridBool", "Snap to grid toggle");
        helpMap.put("statusBarScrollsUpBool","Indicates that a tap on the status bar should scroll up the UI, only relevant in OS's where paintsTitleBarBool is true");
        helpMap.put("switchButtonPadInt","Indicates the padding in the on-off switch, defaults to 16");
        helpMap.put("switchMaskImage","Indicates the mask image used in iOS mode to draw on top of the switch");
        helpMap.put("switchOnImage","Indicates the on image used in iOS mode to draw the on-off switch");
        helpMap.put("switchOffImage","Indicates the off image used in iOS mode to draw the on-off switch");
        helpMap.put("TabEnableAutoImageBool","Indicates images should be filled by default for tabs");
        helpMap.put("TabSelectedImage","Default unselected image for tabs (if TabEnableAutoImageBool=true)");
        helpMap.put("TabUnselectedImage","Default unselected image for tabs (if TabEnableAutoImageBool=true)");
        helpMap.put("tabPlacementInt", "The placement of the tabs in the Tabs component: TOP = 0, BOTTOM = 2, LEFT = 1, RIGHT = 3");
        helpMap.put("tabsFillRowsBool", "Indicates if the tabs should fill the row using flow layout");
        helpMap.put("tabsGridBool", "Indicates whether tabs should use a grid layout thus forcing all tabs to have identical sizes");
        helpMap.put("tabsOnTopBool","Indicates the tabs should be drawn on top of their content in a layered UI, this allows a tab to intrude into the content of the tabs");
        helpMap.put("textCmpVAlignInt", "The vertical alignment of the text component: TOP = 0, CENTER = 4, BOTTOM = 2");
        helpMap.put("textFieldCursorColorInt", "The color of the cursor as an integer (not hex)");
        helpMap.put("tickerSpeedInt", "The speed of label/button etc. tickering in ms.");
        helpMap.put("tintColor", "The aarrggbb hex color to tint the screen when a dialog is shown");
        helpMap.put("topMenuSizeTabPortraitInt","The size of the side menu when expanded and attached to the top in a tablet in portrait mode");
        helpMap.put("topMenuSizePortraitInt","The size of the side menu when expanded and attached to the top in a phone in portrait mode");
        helpMap.put("topMenuSizeTabLandscapeInt","The size of the side menu when expanded and attached to the top in a tablet in landscape mode");
        helpMap.put("topMenuSizeLandscapeInt","The size of the side menu when expanded and attached to the top in a phone in landscape mode");
        helpMap.put("touchCommandFillBool", "Indicates how the touch menu should layout the commands within");
        helpMap.put("touchCommandFlowBool", "Indicates how the touch menu should layout the commands within");
        helpMap.put("transitionSpeedInt", "Indicates the default speed for transitions");
        helpMap.put("treeFolderImage","Picture of a folder for the Tree class");
        helpMap.put("treeFolderOpenImage","Picture of a folder expanded for the Tree class");
        helpMap.put("treeNodeImage","Picture of a file node for the Tree class");
        helpMap.put("tensileDragBool", "Indicates that tensile drag should be enabled/disabled. This is usually set by platform themes.");

        String[] arr = new String[helpMap.size()];
        helpMap.keySet().toArray(arr);
        Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
        constant.setModel(new javax.swing.DefaultComboBoxModel(arr));
        
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
        constant.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "alwaysTensileBool", "centeredPopupBool", "checkBoxCheckDisImage", "checkBoxCheckedImage", "checkBoxUncheckDisImage", "checkBoxUncheckedImage", "comboImage", "commandBehavior", "ComponentGroupBool", "defaultCommandImage", "dialogButtonCommandsBool", "dialogPosition", "dialogTransitionIn", "dialogTransitionInImage", "dialogTransitionOut", "dialogTransitionOutImage", "disabledColor", "dlgCommandButtonSizeInt", "dlgCommandGridBool", "dlgSlideDirection", "dlgSlideInDirBool", "dlgSlideOutDirBool", "fadeScrollBarBool", "fadeScrollEdgeBool", "fadeScrollEdgeInt", "firstCharRTLBool", "fixedSelectionInt", "formTransitionIn", "formTransitionInImage", "formTransitionOut", "formTransitionOutImage", "hideEmptyTitleBool", "ignorListFocusBool", "includeNativeBool", "listItemGapInt", "menuHeightPercent", "menuPrefSizeBool", "menuSlideDirection", "menuSlideInDirBool", "menuSlideOutDirBool", "menuTransitionIn", "menuTransitionInImage", "menuTransitionOut", "menuTransitionOutImage", "menuWidthPercent", "otherPopupRendererBool", "PackTouchMenuBool", "popupCancelBodyBool", "popupTitleBool", "pureTouchBool", "radioSelectedDisImage", "radioSelectedImage", "radioUnselectedDisImage", "radioUnselectedImage", "rendererShowsNumbersBool", "reverseSoftButtonsBool", "sideMenuImage", "slideDirection", "slideInDirBool", "slideOutDirBool", "snapGridBool", "tabPlacementInt", "tabsFillRowsBool", "tabsGridBool", "textCmpVAlignInt", "textFieldCursorColorInt", "tickerSpeedInt", "tintColor", "touchCommandFillBool", "touchCommandFlowBool", "transitionSpeedInt" }));
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
