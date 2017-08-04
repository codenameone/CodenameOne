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

import com.codename1.ui.resource.util.CodenameOneComponentWrapper;
import com.codename1.ui.Button;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Accessor;
import com.codename1.designer.ResourceEditorView;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.util.EditableResources;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Edits the border entry type from the "add theme entry form"
 *
 * @author  Shai Almog
 */
public class BorderEditor extends javax.swing.JPanel {
    private JColorChooser colorChooser;
    private Border currentBorder;
    private Border originalBorder;
    private EditableResources resources;
    private JComponent[][] comboSelectionEnabled;
    private List<JComponent> colorComponents = new ArrayList<JComponent>();
    
    /** Creates new form BorderEditor */
    public BorderEditor(Border border, EditableResources resources) {
        initComponents();
        originalBorder = border;
        this.resources = resources;

        // image border must have images available
        if(resources.getImageResourceNames() == null || resources.getImageResourceNames().length < 1) {
            borderType.removeItem("Image");
            borderType.removeItem("Horizontal Image");
            borderType.removeItem("Vertical Image");
        } else {
            ResourceEditorView.initImagesComboBox(topLeft, resources, false, false, true);
            ResourceEditorView.initImagesComboBox(top, resources, false, false, true);
            ResourceEditorView.initImagesComboBox(topRight, resources, false, false, true);
            ResourceEditorView.initImagesComboBox(left, resources, false, false, true);
            ResourceEditorView.initImagesComboBox(right, resources, false, false, true);
            ResourceEditorView.initImagesComboBox(bottomRight, resources, false, false, true);
            ResourceEditorView.initImagesComboBox(bottomLeft, resources, false, false, true);
            ResourceEditorView.initImagesComboBox(bottom, resources, false, false, true);
            ResourceEditorView.initImagesComboBox(center, resources, false, true, true);
        }
        JComponent[] all = new JComponent[] {
                    arcHeight, arcWidth, bottom, bottomLeft, bottomRight, center,
                    changeHighlightColor, changeLineColor, changeSecondaryHighlightColor,
                    changeSecondaryShadowColor, changeShadowColor, highlightColor, imageMode,
                    left, lineColor, thickness, raisedBorder, right, secondaryHighlightColor,
                    secondaryShadowColor, shadowColor, top, topLeft, topRight,
                    themeColors, imageBorderPreview, roundBorderSettings
                };
        colorComponents.add(changeHighlightColor);
        colorComponents.add(changeLineColor);
        colorComponents.add(changeSecondaryHighlightColor);
        colorComponents.add(changeSecondaryShadowColor);
        colorComponents.add(changeShadowColor);
        colorComponents.add(highlightColor);
        colorComponents.add(lineColor);
        colorComponents.add(secondaryHighlightColor);
        colorComponents.add(secondaryShadowColor);
        colorComponents.add(shadowColor);
        colorComponents.add(bottom);
        colorComponents.add(bottomLeft);
        colorComponents.add(bottomRight);
        colorComponents.add(left);
        colorComponents.add(right);
        colorComponents.add(topRight);
        
        comboSelectionEnabled = new JComponent[][] {
                // [Null], [Empty]
                all, all,
                // Bevel
                {
                    changeHighlightColor, changeSecondaryHighlightColor,
                    changeSecondaryShadowColor, changeShadowColor, highlightColor, raisedBorder, 
                    secondaryHighlightColor, secondaryShadowColor, shadowColor, themeColors
                },
                // Etched
                {
                    changeHighlightColor, changeShadowColor, highlightColor, raisedBorder, 
                    shadowColor, themeColors
                },
                // Line
                {
                    lineColor, changeLineColor, themeColors, thickness
                },
                // Round
                {
                    arcHeight, arcWidth, lineColor, changeLineColor, themeColors
                },
                // Image
                {
                    bottom, bottomLeft, bottomRight, center,
                    imageMode, left, right, top, topLeft, topRight, imageBorderPreview
                },
                // Image Horizontal
                {
                    center, left, right
                },
                // Image Vertical
                {
                    bottom, center, top
                },
                // round
                {
                    lineColor, roundBorderSettings
                },
                // round rect
                {
                    lineColor, roundBorderSettings
                }
            };    
        

        arcWidth.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        arcHeight.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        thickness.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        opacity.setModel(new SpinnerNumberModel(255, 0, 255, 1));
        shadowBlur.setModel(new SpinnerNumberModelThatWorks(10.0, 1, 100, 1));
        shadowOpacity.setModel(new SpinnerNumberModel(0, 0, 255, 1));
        shadowSpread.setModel(new SpinnerNumberModel(10, 0, 255, 1));
        shadowX.setModel(new SpinnerNumberModelThatWorks(0.5, 0, 1, 0.01));
        shadowY.setModel(new SpinnerNumberModelThatWorks(0.5, 0, 1, 0.01));
        strokeOpacity.setModel(new SpinnerNumberModel(255, 0, 255, 1));
        strokeThickness.setModel(new SpinnerNumberModelThatWorks(0.0, 0, 30, 0.5));

        rrRadius.setModel(new SpinnerNumberModelThatWorks(2.0, 0.1, 100, 0.1));
        rrShadowBlur.setModel(new SpinnerNumberModelThatWorks(10.0, 1, 100, 0.1));
        rrShadowOpacity.setModel(new SpinnerNumberModel(0, 0, 255, 1));
        rrShadowSpread.setModel(new SpinnerNumberModelThatWorks(10, 0, 255, 0.5));
        rrShadowX.setModel(new SpinnerNumberModelThatWorks(0.5, 0, 1, 0.01));
        rrShadowY.setModel(new SpinnerNumberModelThatWorks(0.5, 0, 1, 0.01));
        rrStrokeOpacity.setModel(new SpinnerNumberModel(255, 0, 255, 1));
        rrStrokeThickness.setModel(new SpinnerNumberModelThatWorks(0.0, 0, 30, 0.5));
        
        okButton.setPreferredSize(cancelButton.getPreferredSize());
        ((AbstractDocument)highlightColor.getDocument()).setDocumentFilter(new ColorDocmentFilter());
        new ButtonColorIcon(highlightColor, changeHighlightColor);
        ((AbstractDocument)lineColor.getDocument()).setDocumentFilter(new ColorDocmentFilter());
        new ButtonColorIcon(lineColor, changeLineColor);
        ((AbstractDocument)shadowColor.getDocument()).setDocumentFilter(new ColorDocmentFilter());
        new ButtonColorIcon(shadowColor, changeShadowColor);
        ((AbstractDocument)secondaryHighlightColor.getDocument()).setDocumentFilter(new ColorDocmentFilter());
        new ButtonColorIcon(secondaryHighlightColor, changeSecondaryHighlightColor);
        ((AbstractDocument)secondaryShadowColor.getDocument()).setDocumentFilter(new ColorDocmentFilter());
        new ButtonColorIcon(secondaryShadowColor, changeSecondaryShadowColor);
        new ButtonColorIcon(strokeColor, strokeColorPicker);
        new ButtonColorIcon(backgroundColor, backgroundColorPicker);
        ((AbstractDocument)strokeColor.getDocument()).setDocumentFilter(new ColorDocmentFilter());
        ((AbstractDocument)backgroundColor.getDocument()).setDocumentFilter(new ColorDocmentFilter());
        new ButtonColorIcon(rrStrokeColor, rrStrokeColorPicker);
        ((AbstractDocument)rrStrokeColor.getDocument()).setDocumentFilter(new ColorDocmentFilter());

        boolean fourColorBorder = false;
        if(border != null) {
            if(border instanceof RoundBorder) {
                borderType.setSelectedIndex(borderType.getItemCount() - 2);
                jTabbedPane1.setSelectedIndex(2);
            } else {
                if(border instanceof RoundRectBorder) {
                    borderType.setSelectedIndex(borderType.getItemCount() - 1);
                    jTabbedPane1.setSelectedIndex(3);
                } else {
                    jTabbedPane1.setEnabledAt(2, false);
                    switch(Accessor.getType(border)) {
                        case Accessor.TYPE_EMPTY:
                            borderType.setSelectedIndex(1);
                            break;
                        case Accessor.TYPE_LINE:
                            borderType.setSelectedIndex(4);
                            break;
                        case Accessor.TYPE_ROUNDED:
                        case Accessor.TYPE_ROUNDED_PRESSED:
                            borderType.setSelectedIndex(5);
                            break;
                        case Accessor.TYPE_ETCHED_RAISED:
                            raisedBorder.setSelected(true);
                        case Accessor.TYPE_ETCHED_LOWERED:
                            borderType.setSelectedIndex(3);
                            break;
                        case Accessor.TYPE_BEVEL_RAISED:
                            raisedBorder.setSelected(true);
                        case Accessor.TYPE_BEVEL_LOWERED:
                            borderType.setSelectedIndex(2);
                            fourColorBorder = true;
                            break;
                        case Accessor.TYPE_IMAGE:
                            borderType.setSelectedIndex(6);
                            break;
                        case Accessor.TYPE_IMAGE_HORIZONTAL:
                            borderType.setSelectedIndex(7);
                            break;
                        case Accessor.TYPE_IMAGE_VERTICAL:
                            borderType.setSelectedIndex(8);
                            break;
                    }
                }
            }
        }
        if(border != null) {
            if(border instanceof RoundBorder) {
                RoundBorder rb = (RoundBorder)border;
                backgroundColor.setText(Integer.toHexString(rb.getColor()));                
                opacity.setValue(rb.getOpacity());
                isRectangle.setSelected(rb.isRectangle());
                shadowBlur.setValue(rb.getShadowBlur());
                shadowOpacity.setValue(rb.getShadowOpacity());
                shadowSpread.setValue(rb.getShadowSpread());
                shadowX.setValue(rb.getShadowX());
                shadowY.setValue(rb.getShadowY());
                strokeColor.setText(Integer.toHexString(rb.getStrokeColor()));                
                strokeOpacity.setValue(rb.getStrokeOpacity());
                strokeThickness.setValue(rb.getStrokeThickness());
                strokeMillimeter.setSelected(rb.isStrokeMM());
            } else {
                if(border instanceof RoundRectBorder) {
                    RoundRectBorder rb = (RoundRectBorder)border;
                    rrShadowBlur.setValue(rb.getShadowBlur());
                    rrShadowOpacity.setValue(rb.getShadowOpacity());
                    rrShadowSpread.setValue(rb.getShadowSpread());
                    rrShadowX.setValue(rb.getShadowX());
                    rrShadowY.setValue(rb.getShadowY());
                    rrStrokeColor.setText(Integer.toHexString(rb.getStrokeColor()));                
                    rrStrokeOpacity.setValue(rb.getStrokeOpacity());
                    rrStrokeThickness.setValue(rb.getStrokeThickness());
                    rrBezier.setSelected(rb.isBezierCorners());
                    if(rb.isTopOnlyMode()) {
                        rrMode.setSelectedIndex(1);
                    } else {
                        if(rb.isBottomOnlyMode()) {
                            rrMode.setSelectedIndex(2);
                        }
                    }
                    rrStrokeMillimeter.setSelected(rb.isStrokeMM());
                    rrRadius.setValue(rb.getCornerRadius());
                } else {
                    arcHeight.setValue(new Integer(Math.max(1, Accessor.getArcHeight(border))));
                    arcWidth.setValue(new Integer(Math.max(1, Accessor.getArcWidth(border))));
                    highlightColor.setText(Integer.toHexString(Accessor.getColorA(border)));
                    lineColor.setText(Integer.toHexString(Accessor.getColorA(border)));
                    thickness.setValue(new Integer(Math.max(1, Accessor.getThickness(border))));
                    secondaryShadowColor.setText(Integer.toHexString(Accessor.getColorD(border)));
                    if(fourColorBorder) {
                        secondaryHighlightColor.setText(Integer.toHexString(Accessor.getColorB(border)));
                        shadowColor.setText(Integer.toHexString(Accessor.getColorC(border)));
                    } else {
                        secondaryHighlightColor.setText(Integer.toHexString(Accessor.getColorC(border)));
                        shadowColor.setText(Integer.toHexString(Accessor.getColorB(border)));
                    }
                    themeColors.setSelected(Accessor.isThemeColors(border));

                    Image[] images = Accessor.getImages(border);
                    if(images != null) {
                        if(images.length == 9) {
                            String[] imageNames = new String[9];
                            for(int iter = 0 ; iter < 9 ; iter++) {
                                imageNames[iter] = findImageName(images[iter]);
                                if(imageNames[iter] == null && iter < 8) {
                                    // ok this means that this is probably a 3 image mode border...
                                    imageMode.setSelected(true);
                                    topLeft.setSelectedItem(images[4]);
                                    top.setSelectedItem(images[0]);
                                    center.setSelectedItem(images[8]);
                                    updateBorder();
                                    return;
                                }
                            }

                            // top, bottom, left, right, topLeft, topRight, bottomLeft,
                            // bottomRight, background
                            imageMode.setSelected(false);
                            top.setSelectedItem(images[0]);
                            bottom.setSelectedItem(images[1]);
                            left.setSelectedItem(images[2]);
                            right.setSelectedItem(images[3]);
                            topLeft.setSelectedItem(images[4]);
                            topRight.setSelectedItem(images[5]);
                            bottomLeft.setSelectedItem(images[6]);
                            bottomRight.setSelectedItem(images[7]);
                            center.setSelectedItem(images[8]);
                        } else {
                            String[] imageNames = new String[3];
                            for(int iter = 0 ; iter < 3 ; iter++) {
                                imageNames[iter] = findImageName(images[iter]);
                            }

                            if(Accessor.getType(border) == Accessor.TYPE_IMAGE_HORIZONTAL) {
                                left.setSelectedItem(images[0]);
                                right.setSelectedItem(images[1]);
                                center.setSelectedItem(images[2]);
                            } else {
                                top.setSelectedItem(images[0]);
                                bottom.setSelectedItem(images[1]);
                                center.setSelectedItem(images[2]);
                            }
                        }
                    }
                }
            }
        }
        updateBorder();
    }

    /**
     * Finds the image name in the resource file or returns null
     */
    private String findImageName(Image i) {
        for(String name : resources.getImageResourceNames()) {
            if(resources.getImage(name) == i) {
                return name;
            }
        }
        return null;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel6 = new javax.swing.JPanel();
        borderType = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        imageBorderPreview = new com.codename1.ui.resource.util.CodenameOneComponentWrapper();
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        generalSettings = new javax.swing.JPanel();
        changeHighlightColor = new javax.swing.JButton();
        thickness = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        arcHeight = new javax.swing.JSpinner();
        changeSecondaryHighlightColor = new javax.swing.JButton();
        changeSecondaryShadowColor = new javax.swing.JButton();
        shadowColor = new javax.swing.JTextField();
        changeLineColor = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        secondaryShadowColor = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        highlightColor = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        secondaryHighlightColor = new javax.swing.JTextField();
        arcWidth = new javax.swing.JSpinner();
        raisedBorder = new javax.swing.JCheckBox();
        changeShadowColor = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        lineColor = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        themeColors = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        imageBorderSettings = new javax.swing.JPanel();
        imageMode = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        topLeft = new javax.swing.JComboBox();
        top = new javax.swing.JComboBox();
        topRight = new javax.swing.JComboBox();
        left = new javax.swing.JComboBox();
        center = new javax.swing.JComboBox();
        right = new javax.swing.JComboBox();
        bottomLeft = new javax.swing.JComboBox();
        bottom = new javax.swing.JComboBox();
        bottomRight = new javax.swing.JComboBox();
        roundBorderSettings = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        strokeMillimeter = new javax.swing.JCheckBox();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        strokeThickness = new javax.swing.JSpinner();
        opacity = new javax.swing.JSpinner();
        strokeColor = new javax.swing.JTextField();
        strokeColorPicker = new javax.swing.JButton();
        strokeOpacity = new javax.swing.JSpinner();
        shadowOpacity = new javax.swing.JSpinner();
        shadowSpread = new javax.swing.JSpinner();
        shadowX = new javax.swing.JSpinner();
        shadowY = new javax.swing.JSpinner();
        shadowBlur = new javax.swing.JSpinner();
        isRectangle = new javax.swing.JCheckBox();
        jLabel21 = new javax.swing.JLabel();
        backgroundColor = new javax.swing.JTextField();
        backgroundColorPicker = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        rrStrokeColor = new javax.swing.JTextField();
        rrStrokeColorPicker = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        rrStrokeOpacity = new javax.swing.JSpinner();
        jLabel24 = new javax.swing.JLabel();
        rrStrokeThickness = new javax.swing.JSpinner();
        rrStrokeMillimeter = new javax.swing.JCheckBox();
        jLabel25 = new javax.swing.JLabel();
        rrShadowOpacity = new javax.swing.JSpinner();
        jLabel26 = new javax.swing.JLabel();
        rrShadowSpread = new javax.swing.JSpinner();
        jLabel27 = new javax.swing.JLabel();
        rrShadowX = new javax.swing.JSpinner();
        jLabel28 = new javax.swing.JLabel();
        rrShadowY = new javax.swing.JSpinner();
        jLabel29 = new javax.swing.JLabel();
        rrShadowBlur = new javax.swing.JSpinner();
        jLabel30 = new javax.swing.JLabel();
        rrRadius = new javax.swing.JSpinner();
        jLabel31 = new javax.swing.JLabel();
        rrBezier = new javax.swing.JCheckBox();
        jLabel32 = new javax.swing.JLabel();
        rrMode = new javax.swing.JComboBox();

        FormListener formListener = new FormListener();

        setName("Form"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.BorderLayout());

        borderType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "[Null]", "[Empty]", "Bevel", "Etched", "Line", "Rounded (Deprecated)", "Image", "Horizontal Image", "Vertical Image", "Round (circle or square whose corners are completely round)", "Rounded Rectangle" }));
        borderType.setName("borderType"); // NOI18N
        borderType.addActionListener(formListener);
        jPanel6.add(borderType, java.awt.BorderLayout.CENTER);

        jLabel1.setText("Type");
        jLabel1.setName("jLabel1"); // NOI18N
        jPanel6.add(jLabel1, java.awt.BorderLayout.LINE_START);

        add(jPanel6, java.awt.BorderLayout.NORTH);

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.BorderLayout());

        imageBorderPreview.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        imageBorderPreview.setName("imageBorderPreview"); // NOI18N
        imageBorderPreview.setPreferredSize(new java.awt.Dimension(200, 80));
        jPanel5.add(imageBorderPreview, java.awt.BorderLayout.CENTER);

        jPanel1.setName("jPanel1"); // NOI18N

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.GridLayout(1, 2));

        okButton.setText("OK");
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(formListener);
        jPanel3.add(okButton);

        cancelButton.setText("Cancel");
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(formListener);
        jPanel3.add(cancelButton);

        jPanel1.add(jPanel3);

        jPanel5.add(jPanel1, java.awt.BorderLayout.PAGE_END);

        add(jPanel5, java.awt.BorderLayout.SOUTH);

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        generalSettings.setMinimumSize(new java.awt.Dimension(50, 50));
        generalSettings.setName("generalSettings"); // NOI18N
        generalSettings.setOpaque(false);

        changeHighlightColor.setText("...");
        changeHighlightColor.setEnabled(false);
        changeHighlightColor.setName("changeHighlightColor"); // NOI18N

        thickness.setEnabled(false);
        thickness.setName("thickness"); // NOI18N
        thickness.addChangeListener(formListener);

        jLabel2.setText("Color");
        jLabel2.setName("jLabel2"); // NOI18N

        arcHeight.setEnabled(false);
        arcHeight.setName("arcHeight"); // NOI18N
        arcHeight.addChangeListener(formListener);

        changeSecondaryHighlightColor.setText("...");
        changeSecondaryHighlightColor.setEnabled(false);
        changeSecondaryHighlightColor.setName("changeSecondaryHighlightColor"); // NOI18N

        changeSecondaryShadowColor.setText("...");
        changeSecondaryShadowColor.setEnabled(false);
        changeSecondaryShadowColor.setName("changeSecondaryShadowColor"); // NOI18N

        shadowColor.setText("000000");
        shadowColor.setEnabled(false);
        shadowColor.setName("shadowColor"); // NOI18N

        changeLineColor.setText("...");
        changeLineColor.setEnabled(false);
        changeLineColor.setName("changeLineColor"); // NOI18N

        jLabel3.setText("Highlight Color");
        jLabel3.setName("jLabel3"); // NOI18N

        secondaryShadowColor.setText("000000");
        secondaryShadowColor.setEnabled(false);
        secondaryShadowColor.setName("secondaryShadowColor"); // NOI18N

        jLabel6.setText("Secondary");
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel4.setText("Shadow Color");
        jLabel4.setName("jLabel4"); // NOI18N

        highlightColor.setText("000000");
        highlightColor.setEnabled(false);
        highlightColor.setName("highlightColor"); // NOI18N

        jLabel12.setText("Thickness");
        jLabel12.setName("jLabel12"); // NOI18N

        secondaryHighlightColor.setText("000000");
        secondaryHighlightColor.setEnabled(false);
        secondaryHighlightColor.setName("secondaryHighlightColor"); // NOI18N

        arcWidth.setEnabled(false);
        arcWidth.setName("arcWidth"); // NOI18N
        arcWidth.addChangeListener(formListener);

        raisedBorder.setEnabled(false);
        raisedBorder.setMargin(new java.awt.Insets(0, 0, 0, 0));
        raisedBorder.setName("raisedBorder"); // NOI18N
        raisedBorder.addActionListener(formListener);

        changeShadowColor.setText("...");
        changeShadowColor.setEnabled(false);
        changeShadowColor.setName("changeShadowColor"); // NOI18N

        jLabel8.setText("Arc Width");
        jLabel8.setName("jLabel8"); // NOI18N

        lineColor.setText("000000");
        lineColor.setEnabled(false);
        lineColor.setName("lineColor"); // NOI18N

        jLabel9.setText("Arc Height");
        jLabel9.setName("jLabel9"); // NOI18N

        themeColors.setText("Theme Colors");
        themeColors.setEnabled(false);
        themeColors.setName("themeColors"); // NOI18N
        themeColors.addActionListener(formListener);

        jLabel7.setText("Raised");
        jLabel7.setName("jLabel7"); // NOI18N

        jLabel5.setText("Secondary");
        jLabel5.setName("jLabel5"); // NOI18N

        org.jdesktop.layout.GroupLayout generalSettingsLayout = new org.jdesktop.layout.GroupLayout(generalSettings);
        generalSettings.setLayout(generalSettingsLayout);
        generalSettingsLayout.setHorizontalGroup(
            generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(generalSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(jLabel12)
                    .add(jLabel8)
                    .add(jLabel3)
                    .add(jLabel4)
                    .add(jLabel7))
                .add(6, 6, 6)
                .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(generalSettingsLayout.createSequentialGroup()
                        .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(thickness, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
                            .add(generalSettingsLayout.createSequentialGroup()
                                .add(highlightColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(changeHighlightColor)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel5)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(secondaryHighlightColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(changeSecondaryHighlightColor))
                            .add(generalSettingsLayout.createSequentialGroup()
                                .add(shadowColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(changeShadowColor)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel6)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(secondaryShadowColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(changeSecondaryShadowColor)))
                        .add(14, 14, 14))
                    .add(generalSettingsLayout.createSequentialGroup()
                        .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(raisedBorder)
                            .add(generalSettingsLayout.createSequentialGroup()
                                .add(arcWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 62, Short.MAX_VALUE)
                                .add(56, 56, 56)
                                .add(jLabel9)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(arcHeight, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 62, Short.MAX_VALUE))
                            .add(generalSettingsLayout.createSequentialGroup()
                                .add(lineColor)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(changeLineColor)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(themeColors)))
                        .add(6, 6, 6))))
        );

        generalSettingsLayout.linkSize(new java.awt.Component[] {arcHeight, arcWidth, highlightColor, lineColor, secondaryHighlightColor, secondaryShadowColor, shadowColor}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        generalSettingsLayout.linkSize(new java.awt.Component[] {changeHighlightColor, changeLineColor, changeSecondaryHighlightColor, changeSecondaryShadowColor, changeShadowColor}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        generalSettingsLayout.setVerticalGroup(
            generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(generalSettingsLayout.createSequentialGroup()
                .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel2)
                    .add(lineColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeLineColor)
                    .add(themeColors))
                .add(3, 3, 3)
                .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel12)
                    .add(thickness, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(arcWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel8)
                    .add(jLabel9)
                    .add(arcHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(highlightColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeHighlightColor)
                    .add(jLabel5)
                    .add(secondaryHighlightColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeSecondaryHighlightColor))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(shadowColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeShadowColor)
                    .add(jLabel6)
                    .add(secondaryShadowColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeSecondaryShadowColor))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(generalSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(raisedBorder)
                    .add(jLabel7))
                .add(0, 187, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("General", generalSettings);

        imageBorderSettings.setName("imageBorderSettings"); // NOI18N

        imageMode.setText("3 Image Mode");
        imageMode.setEnabled(false);
        imageMode.setMargin(new java.awt.Insets(0, 0, 0, 0));
        imageMode.setName("imageMode"); // NOI18N
        imageMode.addActionListener(formListener);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setOpaque(false);
        jPanel4.setLayout(new java.awt.GridLayout(3, 3));

        topLeft.setName("topLeft"); // NOI18N
        topLeft.addActionListener(formListener);
        jPanel4.add(topLeft);

        top.setName("top"); // NOI18N
        top.addActionListener(formListener);
        jPanel4.add(top);

        topRight.setName("topRight"); // NOI18N
        topRight.addActionListener(formListener);
        jPanel4.add(topRight);

        left.setName("left"); // NOI18N
        left.addActionListener(formListener);
        jPanel4.add(left);

        center.setName("center"); // NOI18N
        center.addActionListener(formListener);
        jPanel4.add(center);

        right.setName("right"); // NOI18N
        right.addActionListener(formListener);
        jPanel4.add(right);

        bottomLeft.setName("bottomLeft"); // NOI18N
        bottomLeft.addActionListener(formListener);
        jPanel4.add(bottomLeft);

        bottom.setName("bottom"); // NOI18N
        bottom.addActionListener(formListener);
        jPanel4.add(bottom);

        bottomRight.setName("bottomRight"); // NOI18N
        bottomRight.addActionListener(formListener);
        jPanel4.add(bottomRight);

        org.jdesktop.layout.GroupLayout imageBorderSettingsLayout = new org.jdesktop.layout.GroupLayout(imageBorderSettings);
        imageBorderSettings.setLayout(imageBorderSettingsLayout);
        imageBorderSettingsLayout.setHorizontalGroup(
            imageBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(imageBorderSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .add(imageBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(imageBorderSettingsLayout.createSequentialGroup()
                        .add(imageMode)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE))
                .addContainerGap())
        );
        imageBorderSettingsLayout.setVerticalGroup(
            imageBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, imageBorderSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .add(imageMode)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 127, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(214, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Image", imageBorderSettings);

        roundBorderSettings.setName("roundBorderSettings"); // NOI18N

        jLabel10.setText("Opacity");
        jLabel10.setName("jLabel10"); // NOI18N

        jLabel11.setText("Stroke Color");
        jLabel11.setName("jLabel11"); // NOI18N

        jLabel13.setText("Stroke Opacity");
        jLabel13.setName("jLabel13"); // NOI18N

        jLabel14.setText("Stroke Thickness");
        jLabel14.setName("jLabel14"); // NOI18N

        jLabel15.setText("Shadow Opacity");
        jLabel15.setName("jLabel15"); // NOI18N

        strokeMillimeter.setText("In Millimeters");
        strokeMillimeter.setToolTipText("Is the thickness in millimeters or pixels");
        strokeMillimeter.setName("strokeMillimeter"); // NOI18N
        strokeMillimeter.addActionListener(formListener);

        jLabel16.setText("Shadow Spread");
        jLabel16.setName("jLabel16"); // NOI18N

        jLabel17.setText("Shadow X");
        jLabel17.setName("jLabel17"); // NOI18N

        jLabel18.setText("Shadow Y");
        jLabel18.setName("jLabel18"); // NOI18N

        jLabel19.setText("Shadow Blur");
        jLabel19.setName("jLabel19"); // NOI18N

        jLabel20.setText("Rectangle");
        jLabel20.setName("jLabel20"); // NOI18N

        strokeThickness.setName("strokeThickness"); // NOI18N
        strokeThickness.addChangeListener(formListener);

        opacity.setName("opacity"); // NOI18N
        opacity.addChangeListener(formListener);

        strokeColor.setText("0");
        strokeColor.setName("strokeColor"); // NOI18N
        strokeColor.addActionListener(formListener);

        strokeColorPicker.setText("...");
        strokeColorPicker.setName("strokeColorPicker"); // NOI18N

        strokeOpacity.setName("strokeOpacity"); // NOI18N
        strokeOpacity.addChangeListener(formListener);

        shadowOpacity.setName("shadowOpacity"); // NOI18N
        shadowOpacity.addChangeListener(formListener);

        shadowSpread.setName("shadowSpread"); // NOI18N
        shadowSpread.addChangeListener(formListener);

        shadowX.setName("shadowX"); // NOI18N
        shadowX.addChangeListener(formListener);

        shadowY.setName("shadowY"); // NOI18N
        shadowY.addChangeListener(formListener);

        shadowBlur.setName("shadowBlur"); // NOI18N
        shadowBlur.addChangeListener(formListener);

        isRectangle.setName("isRectangle"); // NOI18N
        isRectangle.addActionListener(formListener);

        jLabel21.setText("Background Color");
        jLabel21.setName("jLabel21"); // NOI18N

        backgroundColor.setText("d32f2f");
        backgroundColor.setName("backgroundColor"); // NOI18N
        backgroundColor.addActionListener(formListener);

        backgroundColorPicker.setText("...");
        backgroundColorPicker.setName("backgroundColorPicker"); // NOI18N

        org.jdesktop.layout.GroupLayout roundBorderSettingsLayout = new org.jdesktop.layout.GroupLayout(roundBorderSettings);
        roundBorderSettings.setLayout(roundBorderSettingsLayout);
        roundBorderSettingsLayout.setHorizontalGroup(
            roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(roundBorderSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel14)
                    .add(jLabel10)
                    .add(jLabel11)
                    .add(jLabel13)
                    .add(jLabel15)
                    .add(jLabel16)
                    .add(jLabel17)
                    .add(jLabel18)
                    .add(jLabel19)
                    .add(jLabel20)
                    .add(jLabel21))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, roundBorderSettingsLayout.createSequentialGroup()
                        .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, shadowX)
                            .add(shadowSpread)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, shadowY)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, shadowBlur))
                        .add(130, 130, 130))
                    .add(roundBorderSettingsLayout.createSequentialGroup()
                        .add(isRectangle)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, roundBorderSettingsLayout.createSequentialGroup()
                        .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(roundBorderSettingsLayout.createSequentialGroup()
                                .add(backgroundColor)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(backgroundColorPicker))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, roundBorderSettingsLayout.createSequentialGroup()
                                .add(strokeColor, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(strokeColorPicker))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, strokeThickness)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, opacity)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, strokeOpacity)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, shadowOpacity))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(strokeMillimeter)
                        .addContainerGap())))
        );
        roundBorderSettingsLayout.setVerticalGroup(
            roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, roundBorderSettingsLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel21)
                    .add(backgroundColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(backgroundColorPicker))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(opacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel11)
                    .add(strokeColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(strokeColorPicker))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel13)
                    .add(strokeOpacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel14)
                    .add(strokeMillimeter)
                    .add(strokeThickness, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel15)
                    .add(shadowOpacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel16)
                    .add(shadowSpread, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel17)
                    .add(shadowX, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel18)
                    .add(shadowY, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel19)
                    .add(shadowBlur, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(roundBorderSettingsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel20)
                    .add(isRectangle))
                .add(15, 15, 15))
        );

        jTabbedPane1.addTab("Round", roundBorderSettings);

        jPanel2.setName("jPanel2"); // NOI18N

        jLabel22.setText("Stroke Color");
        jLabel22.setName("jLabel22"); // NOI18N

        rrStrokeColor.setText("0");
        rrStrokeColor.setName("rrStrokeColor"); // NOI18N
        rrStrokeColor.addActionListener(formListener);

        rrStrokeColorPicker.setText("...");
        rrStrokeColorPicker.setName("rrStrokeColorPicker"); // NOI18N
        rrStrokeColorPicker.addActionListener(formListener);

        jLabel23.setText("Stroke Opacity");
        jLabel23.setName("jLabel23"); // NOI18N

        rrStrokeOpacity.setName("rrStrokeOpacity"); // NOI18N
        rrStrokeOpacity.addChangeListener(formListener);

        jLabel24.setText("Stroke Thickness");
        jLabel24.setName("jLabel24"); // NOI18N

        rrStrokeThickness.setName("rrStrokeThickness"); // NOI18N
        rrStrokeThickness.addChangeListener(formListener);

        rrStrokeMillimeter.setText("In Millimeters");
        rrStrokeMillimeter.setToolTipText("Is the thickness in millimeters or pixels");
        rrStrokeMillimeter.setName("rrStrokeMillimeter"); // NOI18N
        rrStrokeMillimeter.addActionListener(formListener);

        jLabel25.setText("Shadow Opacity");
        jLabel25.setName("jLabel25"); // NOI18N

        rrShadowOpacity.setName("rrShadowOpacity"); // NOI18N
        rrShadowOpacity.addChangeListener(formListener);

        jLabel26.setText("Shadow Spread");
        jLabel26.setName("jLabel26"); // NOI18N

        rrShadowSpread.setName("rrShadowSpread"); // NOI18N
        rrShadowSpread.addChangeListener(formListener);

        jLabel27.setText("Shadow X");
        jLabel27.setName("jLabel27"); // NOI18N

        rrShadowX.setName("rrShadowX"); // NOI18N
        rrShadowX.addChangeListener(formListener);

        jLabel28.setText("Shadow Y");
        jLabel28.setName("jLabel28"); // NOI18N

        rrShadowY.setName("rrShadowY"); // NOI18N
        rrShadowY.addChangeListener(formListener);

        jLabel29.setText("Shadow Blur");
        jLabel29.setName("jLabel29"); // NOI18N

        rrShadowBlur.setName("rrShadowBlur"); // NOI18N
        rrShadowBlur.addChangeListener(formListener);

        jLabel30.setText("Radius Millimeters");
        jLabel30.setName("jLabel30"); // NOI18N

        rrRadius.setName("rrRadius"); // NOI18N
        rrRadius.addChangeListener(formListener);

        jLabel31.setText("Bezier Corners");
        jLabel31.setName("jLabel31"); // NOI18N

        rrBezier.setName("rrBezier"); // NOI18N
        rrBezier.addActionListener(formListener);

        jLabel32.setText("Mode");
        jLabel32.setName("jLabel32"); // NOI18N

        rrMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Standard", "Top Only", "Bottom Only" }));
        rrMode.setName("rrMode"); // NOI18N
        rrMode.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel24)
                    .add(jLabel22)
                    .add(jLabel23)
                    .add(jLabel25)
                    .add(jLabel26)
                    .add(jLabel27)
                    .add(jLabel28)
                    .add(jLabel29)
                    .add(jLabel30)
                    .add(jLabel31)
                    .add(jLabel32))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(rrStrokeThickness)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(rrStrokeMillimeter))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(rrBezier)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, rrMode, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, rrStrokeOpacity)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, rrShadowOpacity)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, rrShadowSpread)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, rrShadowX)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, rrShadowY)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, rrShadowBlur)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, rrRadius)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                                .add(rrStrokeColor, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(rrStrokeColorPicker)))
                        .add(124, 124, 124)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel30)
                    .add(rrRadius, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel22)
                    .add(rrStrokeColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(rrStrokeColorPicker))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel23)
                    .add(rrStrokeOpacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel24)
                    .add(rrStrokeMillimeter)
                    .add(rrStrokeThickness, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel25)
                    .add(rrShadowOpacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel26)
                    .add(rrShadowSpread, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel27)
                    .add(rrShadowX, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel28)
                    .add(rrShadowY, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel29)
                    .add(rrShadowBlur, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel31)
                    .add(rrBezier))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel32)
                    .add(rrMode, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(44, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Round Rect", jPanel2);

        add(jTabbedPane1, java.awt.BorderLayout.CENTER);
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == borderType) {
                BorderEditor.this.borderTypeActionPerformed(evt);
            }
            else if (evt.getSource() == okButton) {
                BorderEditor.this.okButtonActionPerformed(evt);
            }
            else if (evt.getSource() == cancelButton) {
                BorderEditor.this.cancelButtonActionPerformed(evt);
            }
            else if (evt.getSource() == raisedBorder) {
                BorderEditor.this.raisedBorderActionPerformed(evt);
            }
            else if (evt.getSource() == themeColors) {
                BorderEditor.this.themeColorsActionPerformed(evt);
            }
            else if (evt.getSource() == imageMode) {
                BorderEditor.this.imageModeActionPerformed(evt);
            }
            else if (evt.getSource() == topLeft) {
                BorderEditor.this.topLeftActionPerformed(evt);
            }
            else if (evt.getSource() == top) {
                BorderEditor.this.topActionPerformed(evt);
            }
            else if (evt.getSource() == topRight) {
                BorderEditor.this.topRightActionPerformed(evt);
            }
            else if (evt.getSource() == left) {
                BorderEditor.this.leftActionPerformed(evt);
            }
            else if (evt.getSource() == center) {
                BorderEditor.this.centerActionPerformed(evt);
            }
            else if (evt.getSource() == right) {
                BorderEditor.this.rightActionPerformed(evt);
            }
            else if (evt.getSource() == bottomLeft) {
                BorderEditor.this.bottomLeftActionPerformed(evt);
            }
            else if (evt.getSource() == bottom) {
                BorderEditor.this.bottomActionPerformed(evt);
            }
            else if (evt.getSource() == bottomRight) {
                BorderEditor.this.bottomRightActionPerformed(evt);
            }
            else if (evt.getSource() == strokeMillimeter) {
                BorderEditor.this.strokeMillimeterActionPerformed(evt);
            }
            else if (evt.getSource() == strokeColor) {
                BorderEditor.this.strokeColorActionPerformed(evt);
            }
            else if (evt.getSource() == isRectangle) {
                BorderEditor.this.isRectangleActionPerformed(evt);
            }
            else if (evt.getSource() == backgroundColor) {
                BorderEditor.this.backgroundColorActionPerformed(evt);
            }
            else if (evt.getSource() == rrStrokeColor) {
                BorderEditor.this.rrStrokeColorActionPerformed(evt);
            }
            else if (evt.getSource() == rrStrokeMillimeter) {
                BorderEditor.this.rrStrokeMillimeterActionPerformed(evt);
            }
            else if (evt.getSource() == rrStrokeColorPicker) {
                BorderEditor.this.rrStrokeColorPickerActionPerformed(evt);
            }
            else if (evt.getSource() == rrBezier) {
                BorderEditor.this.rrBezierActionPerformed(evt);
            }
            else if (evt.getSource() == rrMode) {
                BorderEditor.this.rrModeActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == thickness) {
                BorderEditor.this.thicknessStateChanged(evt);
            }
            else if (evt.getSource() == arcHeight) {
                BorderEditor.this.arcHeightStateChanged(evt);
            }
            else if (evt.getSource() == arcWidth) {
                BorderEditor.this.arcWidthStateChanged(evt);
            }
            else if (evt.getSource() == strokeThickness) {
                BorderEditor.this.strokeThicknessStateChanged(evt);
            }
            else if (evt.getSource() == opacity) {
                BorderEditor.this.opacityStateChanged(evt);
            }
            else if (evt.getSource() == strokeOpacity) {
                BorderEditor.this.strokeOpacityStateChanged(evt);
            }
            else if (evt.getSource() == shadowOpacity) {
                BorderEditor.this.shadowOpacityStateChanged(evt);
            }
            else if (evt.getSource() == shadowSpread) {
                BorderEditor.this.shadowSpreadStateChanged(evt);
            }
            else if (evt.getSource() == shadowX) {
                BorderEditor.this.shadowXStateChanged(evt);
            }
            else if (evt.getSource() == shadowY) {
                BorderEditor.this.shadowYStateChanged(evt);
            }
            else if (evt.getSource() == shadowBlur) {
                BorderEditor.this.shadowBlurStateChanged(evt);
            }
            else if (evt.getSource() == rrStrokeOpacity) {
                BorderEditor.this.rrStrokeOpacityStateChanged(evt);
            }
            else if (evt.getSource() == rrStrokeThickness) {
                BorderEditor.this.rrStrokeThicknessStateChanged(evt);
            }
            else if (evt.getSource() == rrShadowOpacity) {
                BorderEditor.this.rrShadowOpacityStateChanged(evt);
            }
            else if (evt.getSource() == rrShadowSpread) {
                BorderEditor.this.rrShadowSpreadStateChanged(evt);
            }
            else if (evt.getSource() == rrShadowX) {
                BorderEditor.this.rrShadowXStateChanged(evt);
            }
            else if (evt.getSource() == rrShadowY) {
                BorderEditor.this.rrShadowYStateChanged(evt);
            }
            else if (evt.getSource() == rrShadowBlur) {
                BorderEditor.this.rrShadowBlurStateChanged(evt);
            }
            else if (evt.getSource() == rrRadius) {
                BorderEditor.this.rrRadiusStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

private void borderTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_borderTypeActionPerformed
    updateBorder();
    if(borderType.getSelectedIndex() == borderType.getItemCount() - 2) {
        jTabbedPane1.setEnabledAt(2, true);
        jTabbedPane1.setSelectedIndex(2);
    } else {
        if(borderType.getSelectedIndex() == borderType.getItemCount() - 1) {
            jTabbedPane1.setEnabledAt(3, true);
            jTabbedPane1.setSelectedIndex(3);
        } else {
            jTabbedPane1.setEnabledAt(2, false);
            if(jTabbedPane1.getSelectedIndex() == 2) {
                jTabbedPane1.setSelectedIndex(0);
            }
        }
    }
}//GEN-LAST:event_borderTypeActionPerformed

    private void updateBorder() {
        updateBorder(true);
    }


    private Image getButtonImageBorderIconNotNull(JComboBox b) {
        Image i = (Image)b.getSelectedItem();
        if(i == null) {
            if(resources.getImageResourceNames().length > 0) {
                return resources.getImage(resources.getImageResourceNames()[0]);
            }
        }
        return i;
    }

    private Image getButtonImageBorderIcon(JComboBox b) {
        return  (Image)b.getSelectedItem();
    }

    private void updateBorder(boolean updateEnabled) {
        if(updateEnabled) {
            okButton.setEnabled(true);
            for(JComponent c : comboSelectionEnabled[0]) {
                c.setEnabled(false);
            }
            if(borderType.getSelectedIndex() > 1) {
                List<JComponent> colorElements = colorComponents;
                if(borderType.getSelectedIndex() == 6 && !imageMode.isSelected()) {
                    colorElements = new ArrayList<JComponent>();
                } else {
                    if(borderType.getSelectedIndex() < 6 && !themeColors.isSelected()) {
                        colorElements = new ArrayList<JComponent>();
                    } else {
                        if(borderType.getSelectedIndex() > 6) {
                            colorElements = new ArrayList<JComponent>();
                        }
                    }
                }
                for(JComponent c : comboSelectionEnabled[borderType.getSelectedIndex()]) {
                    // if colors arrive from the theme then don't enable any color related element
                    c.setEnabled(!colorElements.contains(c));
                }
            }
        }
        if(borderType.getSelectedIndex() == borderType.getItemCount() - 2) {
            // we need to use a special case because a theme with no images will have a different offset for the border 
            currentBorder = RoundBorder.create().
                    color(getColor(backgroundColor)).
                    opacity(((Number)opacity.getValue()).intValue()).
                    rectangle(isRectangle.isSelected()).
                    shadowBlur(((Number)shadowBlur.getValue()).floatValue()).
                    shadowOpacity(((Number)shadowOpacity.getValue()).intValue()).
                    shadowSpread(((Number)shadowSpread.getValue()).intValue()).
                    shadowX(((Number)shadowX.getValue()).floatValue()).
                    shadowY(((Number)shadowY.getValue()).floatValue()).
                    stroke(((Number)strokeThickness.getValue()).floatValue(), strokeMillimeter.isSelected()).
                    strokeColor(getColor(strokeColor)).
                    strokeOpacity(((Number)strokeOpacity.getValue()).intValue());
        } else {
            if(borderType.getSelectedIndex() == borderType.getItemCount() - 1) {
                // we need to use a special case because a theme with no images will have a different offset for the border 
                currentBorder = RoundRectBorder.create().
                        shadowBlur(((Number)rrShadowBlur.getValue()).floatValue()).
                        shadowOpacity(((Number)rrShadowOpacity.getValue()).intValue()).
                        shadowSpread(((Number)rrShadowSpread.getValue()).floatValue()).
                        shadowX(((Number)rrShadowX.getValue()).floatValue()).
                        shadowY(((Number)rrShadowY.getValue()).floatValue()).
                        stroke(((Number)rrStrokeThickness.getValue()).floatValue(), rrStrokeMillimeter.isSelected()).
                        strokeColor(getColor(rrStrokeColor)).
                        strokeOpacity(((Number)rrStrokeOpacity.getValue()).intValue()).
                        bezierCorners(rrBezier.isSelected()).
                        cornerRadius(((Number)rrRadius.getValue()).floatValue()).
                        bottomOnlyMode(rrMode.getSelectedIndex() == 1).
                        topOnlyMode(rrMode.getSelectedIndex() == 2);
            } else {
                switch(borderType.getSelectedIndex()) {
                    case 0:
                        // null border
                        currentBorder = null;
                        break;
                    case 1:
                        // empty border
                        currentBorder = Border.getEmpty();
                        break;
                    case 2: 
                        // bevel border
                        if(themeColors.isSelected()) {
                            if(raisedBorder.isSelected()) {
                                currentBorder = Border.createBevelRaised();
                            } else {
                                currentBorder = Border.createBevelLowered();
                            }
                        } else {
                            if(raisedBorder.isSelected()) {
                                currentBorder = Border.createBevelRaised(getColor(highlightColor), getColor(secondaryHighlightColor),
                                        getColor(shadowColor), getColor(secondaryShadowColor));
                            } else {
                                currentBorder = Border.createBevelLowered(getColor(highlightColor), getColor(secondaryHighlightColor),
                                        getColor(shadowColor), getColor(secondaryShadowColor));
                            }
                        }
                        break;
                    case 3: 
                        // etched border
                        if(themeColors.isSelected()) {
                            if(raisedBorder.isSelected()) {
                                currentBorder = Border.createEtchedRaised();
                            } else {
                                currentBorder = Border.createEtchedLowered();
                            }
                        } else {
                            if(raisedBorder.isSelected()) {
                                currentBorder = Border.createEtchedRaised(getColor(highlightColor), getColor(shadowColor));
                            } else {
                                currentBorder = Border.createEtchedLowered(getColor(highlightColor), getColor(shadowColor));
                            }
                        }
                        break;
                    case 6: {
                        // this is a theme with no images
                        if(borderType.getItemCount() < 8) {
                            break;
                        }
                        // image border
                        Image c = getButtonImageBorderIcon(this.center);

                        if(imageMode.isSelected()) {                    
                            currentBorder = Border.createImageBorder(getButtonImageBorderIconNotNull(top),
                                    getButtonImageBorderIconNotNull(topLeft), c);
                        } else {
                            currentBorder = Border.createImageBorder(
                                    getButtonImageBorderIconNotNull(top),
                                    getButtonImageBorderIconNotNull(bottom),
                                    getButtonImageBorderIconNotNull(left),
                                    getButtonImageBorderIconNotNull(right),
                                    getButtonImageBorderIconNotNull(topLeft),
                                    getButtonImageBorderIconNotNull(topRight),
                                    getButtonImageBorderIconNotNull(bottomLeft),
                                    getButtonImageBorderIconNotNull(bottomRight),
                                c);
                        }
                        break;
                    }
                    case 7: {
                        Image c = getButtonImageBorderIcon(this.center);

                        currentBorder = Border.createHorizonalImageBorder(
                                getButtonImageBorderIconNotNull(left),
                                getButtonImageBorderIconNotNull(right),
                            c);
                        break;
                    }
                    case 8: {
                        Image c = getButtonImageBorderIcon(this.center);

                        currentBorder = Border.createVerticalImageBorder(
                                getButtonImageBorderIconNotNull(top),
                                getButtonImageBorderIconNotNull(bottom),
                            c);
                        break;
                    }
                    case 4:
                        // line border
                        if(themeColors.isSelected()) {
                            currentBorder = Border.createLineBorder(((Number)thickness.getValue()).intValue());
                        } else {
                            currentBorder = Border.createLineBorder(((Number)thickness.getValue()).intValue(), getColor(lineColor));
                        }
                        break;
                    case 5:
                        // rounded border
                        if(themeColors.isSelected()) {
                            currentBorder = Border.createRoundBorder(((Number)arcWidth.getValue()).intValue(), 
                                ((Number)arcHeight.getValue()).intValue());
                        } else {
                            currentBorder = Border.createRoundBorder(((Number)arcWidth.getValue()).intValue(), 
                                ((Number)arcHeight.getValue()).intValue(), getColor(lineColor));
                        }
                        break;
                }
            }
        }
        final CodenameOneComponentWrapper w = (CodenameOneComponentWrapper)imageBorderPreview;
        final Border finalBorder = currentBorder;
        final Button b = (Button)w.getCodenameOneComponent();
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                b.clearClientProperties();
                b.setPreferredSize(new com.codename1.ui.geom.Dimension(200, 100));
                b.getAllStyles().setPadding(20, 20, 20, 20);
                b.getAllStyles().setBorder(finalBorder);
                b.getParent().revalidate();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        w.revalidate();
                    }
                });
            }
        });
    }

    private int getColor(JTextField f) {
        return Integer.decode("0x" + f.getText()).intValue();
    }
        
private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
    ((JDialog)SwingUtilities.windowForComponent(this)).dispose();
}//GEN-LAST:event_okButtonActionPerformed

private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
    currentBorder = originalBorder;
    ((JDialog)SwingUtilities.windowForComponent(this)).dispose();
}//GEN-LAST:event_cancelButtonActionPerformed

private void themeColorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_themeColorsActionPerformed
    updateBorder();
}//GEN-LAST:event_themeColorsActionPerformed

private void arcWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arcWidthStateChanged
    updateBorder();
}//GEN-LAST:event_arcWidthStateChanged

private void arcHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arcHeightStateChanged
    updateBorder();
}//GEN-LAST:event_arcHeightStateChanged

private void raisedBorderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_raisedBorderActionPerformed
    updateBorder();
}//GEN-LAST:event_raisedBorderActionPerformed

private void imageModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageModeActionPerformed
    updateBorder();
}//GEN-LAST:event_imageModeActionPerformed


private void thicknessStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_thicknessStateChanged
    updateBorder();
}//GEN-LAST:event_thicknessStateChanged

private void topLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_topLeftActionPerformed
    updateBorder();
}//GEN-LAST:event_topLeftActionPerformed

private void topActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_topActionPerformed
    updateBorder();
}//GEN-LAST:event_topActionPerformed

private void topRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_topRightActionPerformed
    updateBorder();
}//GEN-LAST:event_topRightActionPerformed

private void leftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_leftActionPerformed
    updateBorder();
}//GEN-LAST:event_leftActionPerformed

private void centerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_centerActionPerformed
    updateBorder();
}//GEN-LAST:event_centerActionPerformed

private void rightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rightActionPerformed
    updateBorder();
}//GEN-LAST:event_rightActionPerformed

private void bottomLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bottomLeftActionPerformed
    updateBorder();
}//GEN-LAST:event_bottomLeftActionPerformed

private void bottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bottomActionPerformed
    updateBorder();
}//GEN-LAST:event_bottomActionPerformed

private void bottomRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bottomRightActionPerformed
    updateBorder();
}//GEN-LAST:event_bottomRightActionPerformed

    private void opacityStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_opacityStateChanged
        updateBorder();
    }//GEN-LAST:event_opacityStateChanged

    private void strokeColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_strokeColorActionPerformed
        updateBorder();
    }//GEN-LAST:event_strokeColorActionPerformed

    private void strokeOpacityStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_strokeOpacityStateChanged
        updateBorder();
    }//GEN-LAST:event_strokeOpacityStateChanged

    private void strokeThicknessStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_strokeThicknessStateChanged
        updateBorder();
    }//GEN-LAST:event_strokeThicknessStateChanged

    private void strokeMillimeterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_strokeMillimeterActionPerformed
        updateBorder();
    }//GEN-LAST:event_strokeMillimeterActionPerformed

    private void shadowOpacityStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_shadowOpacityStateChanged
        updateBorder();
    }//GEN-LAST:event_shadowOpacityStateChanged

    private void shadowSpreadStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_shadowSpreadStateChanged
        updateBorder();
    }//GEN-LAST:event_shadowSpreadStateChanged

    private void shadowXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_shadowXStateChanged
        updateBorder();
    }//GEN-LAST:event_shadowXStateChanged

    private void shadowYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_shadowYStateChanged
        updateBorder();
    }//GEN-LAST:event_shadowYStateChanged

    private void shadowBlurStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_shadowBlurStateChanged
        updateBorder();
    }//GEN-LAST:event_shadowBlurStateChanged

    private void isRectangleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_isRectangleActionPerformed
        updateBorder();
    }//GEN-LAST:event_isRectangleActionPerformed

    private void backgroundColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backgroundColorActionPerformed
        updateBorder();
    }//GEN-LAST:event_backgroundColorActionPerformed

    private void rrStrokeColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rrStrokeColorActionPerformed
        updateBorder();
    }//GEN-LAST:event_rrStrokeColorActionPerformed

    private void rrStrokeOpacityStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rrStrokeOpacityStateChanged
            updateBorder();
    }//GEN-LAST:event_rrStrokeOpacityStateChanged

    private void rrStrokeThicknessStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rrStrokeThicknessStateChanged
        updateBorder();
    }//GEN-LAST:event_rrStrokeThicknessStateChanged

    private void rrStrokeMillimeterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rrStrokeMillimeterActionPerformed
        updateBorder();
    }//GEN-LAST:event_rrStrokeMillimeterActionPerformed

    private void rrShadowOpacityStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rrShadowOpacityStateChanged
        updateBorder();
    }//GEN-LAST:event_rrShadowOpacityStateChanged

    private void rrShadowSpreadStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rrShadowSpreadStateChanged
        updateBorder();
    }//GEN-LAST:event_rrShadowSpreadStateChanged

    private void rrShadowXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rrShadowXStateChanged
        updateBorder();
    }//GEN-LAST:event_rrShadowXStateChanged

    private void rrShadowYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rrShadowYStateChanged
        updateBorder();
    }//GEN-LAST:event_rrShadowYStateChanged

    private void rrShadowBlurStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rrShadowBlurStateChanged
        updateBorder();
    }//GEN-LAST:event_rrShadowBlurStateChanged

    private void rrRadiusStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rrRadiusStateChanged
        updateBorder();
    }//GEN-LAST:event_rrRadiusStateChanged

    private void rrStrokeColorPickerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rrStrokeColorPickerActionPerformed
        updateBorder();
    }//GEN-LAST:event_rrStrokeColorPickerActionPerformed

    private void rrBezierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rrBezierActionPerformed
        updateBorder();
    }//GEN-LAST:event_rrBezierActionPerformed

    private void rrModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rrModeActionPerformed
        updateBorder();
    }//GEN-LAST:event_rrModeActionPerformed

public Border getResult() {
    return currentBorder;
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner arcHeight;
    private javax.swing.JSpinner arcWidth;
    private javax.swing.JTextField backgroundColor;
    private javax.swing.JButton backgroundColorPicker;
    private javax.swing.JComboBox borderType;
    private javax.swing.JComboBox bottom;
    private javax.swing.JComboBox bottomLeft;
    private javax.swing.JComboBox bottomRight;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox center;
    private javax.swing.JButton changeHighlightColor;
    private javax.swing.JButton changeLineColor;
    private javax.swing.JButton changeSecondaryHighlightColor;
    private javax.swing.JButton changeSecondaryShadowColor;
    private javax.swing.JButton changeShadowColor;
    private javax.swing.JPanel generalSettings;
    private javax.swing.JTextField highlightColor;
    private javax.swing.JLabel imageBorderPreview;
    private javax.swing.JPanel imageBorderSettings;
    private javax.swing.JCheckBox imageMode;
    private javax.swing.JCheckBox isRectangle;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox left;
    private javax.swing.JTextField lineColor;
    private javax.swing.JButton okButton;
    private javax.swing.JSpinner opacity;
    private javax.swing.JCheckBox raisedBorder;
    private javax.swing.JComboBox right;
    private javax.swing.JPanel roundBorderSettings;
    private javax.swing.JCheckBox rrBezier;
    private javax.swing.JComboBox rrMode;
    private javax.swing.JSpinner rrRadius;
    private javax.swing.JSpinner rrShadowBlur;
    private javax.swing.JSpinner rrShadowOpacity;
    private javax.swing.JSpinner rrShadowSpread;
    private javax.swing.JSpinner rrShadowX;
    private javax.swing.JSpinner rrShadowY;
    private javax.swing.JTextField rrStrokeColor;
    private javax.swing.JButton rrStrokeColorPicker;
    private javax.swing.JCheckBox rrStrokeMillimeter;
    private javax.swing.JSpinner rrStrokeOpacity;
    private javax.swing.JSpinner rrStrokeThickness;
    private javax.swing.JTextField secondaryHighlightColor;
    private javax.swing.JTextField secondaryShadowColor;
    private javax.swing.JSpinner shadowBlur;
    private javax.swing.JTextField shadowColor;
    private javax.swing.JSpinner shadowOpacity;
    private javax.swing.JSpinner shadowSpread;
    private javax.swing.JSpinner shadowX;
    private javax.swing.JSpinner shadowY;
    private javax.swing.JTextField strokeColor;
    private javax.swing.JButton strokeColorPicker;
    private javax.swing.JCheckBox strokeMillimeter;
    private javax.swing.JSpinner strokeOpacity;
    private javax.swing.JSpinner strokeThickness;
    private javax.swing.JCheckBox themeColors;
    private javax.swing.JSpinner thickness;
    private javax.swing.JComboBox top;
    private javax.swing.JComboBox topLeft;
    private javax.swing.JComboBox topRight;
    // End of variables declaration//GEN-END:variables

    class ColorDocmentFilter extends DocumentFilter {
        public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws
                           BadLocationException {
            if(fb.getDocument().getLength() > length) {
                fb.remove(offset, length);
            }
            updateBorder(false);
        }

        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string,
                                 AttributeSet attr) throws BadLocationException {
            if(fb.getDocument().getLength() + string.length() > 6) {
                return;
            }
            for(int iter = 0 ; iter < string.length() ; iter++) {
                char c = string.charAt(iter);
                if(!(Character.isDigit(c) || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                    return;
                }
            }
            fb.insertString(offset, string, attr);
            updateBorder(false);
        }

        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text,
                            AttributeSet attrs) throws BadLocationException {
            if(fb.getDocument().getLength() - length + text.length() > 6) {
                return;
            }
            for(int iter = 0 ; iter < text.length() ; iter++) {
                char c = text.charAt(iter);
                if(!(Character.isDigit(c) || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                    return;
                }
            }
            fb.replace(offset, length, text, attrs);
            updateBorder(false);
        }
    }

    class ButtonColorIcon extends ColorIcon implements DocumentListener, ActionListener {
        private JTextField t;
        private JButton btn;
        public ButtonColorIcon(JTextField t, JButton btn) {
            super(null);
            this.t = t;
            this.btn = btn;
            btn.setIcon(this);
            btn.addActionListener(this);
            t.getDocument().addDocumentListener(this);
        }
        
        protected String getColorString(java.awt.Component c) {
            return t.getText();
        }

        public void insertUpdate(DocumentEvent e) {
            btn.repaint();
        }

        public void removeUpdate(DocumentEvent e) {
            btn.repaint();
        }

        public void changedUpdate(DocumentEvent e) {
            btn.repaint();            
        }

        public void actionPerformed(ActionEvent e) {
            int color = Integer.decode("0x" + t.getText());
            if(colorChooser == null) {
                colorChooser = new JColorChooser();
            }
            colorChooser.setColor(color);

            JDialog dlg = JColorChooser.createDialog(btn, "Pick color", true, colorChooser, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int i = colorChooser.getColor().getRGB() & 0xffffff;
                    t.setText(Integer.toHexString(i));
                }
            }, null);
            dlg.setLocationByPlatform(true);
            dlg.pack();
            dlg.setVisible(true);
            updateBorder();
        }
    }
}
