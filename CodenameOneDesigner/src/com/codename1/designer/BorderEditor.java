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
        try {
            help.setPage(getClass().getResource("/help/borderHelp.html"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
                    themeColors, imageBorderPreview
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
            };    
        

        arcWidth.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        arcHeight.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        thickness.setModel(new SpinnerNumberModel(1, 1, 100, 1));
        
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

        boolean fourColorBorder = false;
        if(border != null) {
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
        if(border != null) {
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

        jPanel1 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();
        jPanel2 = new javax.swing.JPanel();
        changeHighlightColor = new javax.swing.JButton();
        thickness = new javax.swing.JSpinner();
        jLabel10 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        arcHeight = new javax.swing.JSpinner();
        center = new javax.swing.JComboBox();
        changeSecondaryHighlightColor = new javax.swing.JButton();
        changeSecondaryShadowColor = new javax.swing.JButton();
        right = new javax.swing.JComboBox();
        left = new javax.swing.JComboBox();
        topRight = new javax.swing.JComboBox();
        shadowColor = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        bottomRight = new javax.swing.JComboBox();
        changeLineColor = new javax.swing.JButton();
        borderType = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        secondaryShadowColor = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        bottomLeft = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        highlightColor = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        secondaryHighlightColor = new javax.swing.JTextField();
        top = new javax.swing.JComboBox();
        arcWidth = new javax.swing.JSpinner();
        bottom = new javax.swing.JComboBox();
        imageMode = new javax.swing.JCheckBox();
        raisedBorder = new javax.swing.JCheckBox();
        changeShadowColor = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        lineColor = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        topLeft = new javax.swing.JComboBox();
        themeColors = new javax.swing.JCheckBox();
        imageBorderPreview = new com.codename1.ui.resource.util.CodenameOneComponentWrapper();
        jLabel7 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        FormListener formListener = new FormListener();

        setName("Form"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        jPanel1.setName("jPanel1"); // NOI18N

        okButton.setText("OK");
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(formListener);
        jPanel1.add(okButton);

        cancelButton.setText("Cancel");
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(formListener);
        jPanel1.add(cancelButton);

        add(jPanel1, java.awt.BorderLayout.SOUTH);

        jSplitPane1.setDividerLocation(450);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setName("jSplitPane1"); // NOI18N
        jSplitPane1.setOneTouchExpandable(true);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(650, 500));

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setName("help"); // NOI18N
        jScrollPane1.setViewportView(help);

        jSplitPane1.setRightComponent(jScrollPane1);

        jPanel2.setMinimumSize(new java.awt.Dimension(50, 50));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setOpaque(false);

        changeHighlightColor.setText("...");
        changeHighlightColor.setEnabled(false);
        changeHighlightColor.setName("changeHighlightColor"); // NOI18N

        thickness.setEnabled(false);
        thickness.setName("thickness"); // NOI18N
        thickness.addChangeListener(formListener);

        jLabel10.setText("Images");
        jLabel10.setName("jLabel10"); // NOI18N

        jLabel2.setText("Color");
        jLabel2.setName("jLabel2"); // NOI18N

        arcHeight.setEnabled(false);
        arcHeight.setName("arcHeight"); // NOI18N
        arcHeight.addChangeListener(formListener);

        center.setName("center"); // NOI18N
        center.addActionListener(formListener);

        changeSecondaryHighlightColor.setText("...");
        changeSecondaryHighlightColor.setEnabled(false);
        changeSecondaryHighlightColor.setName("changeSecondaryHighlightColor"); // NOI18N

        changeSecondaryShadowColor.setText("...");
        changeSecondaryShadowColor.setEnabled(false);
        changeSecondaryShadowColor.setName("changeSecondaryShadowColor"); // NOI18N

        right.setName("right"); // NOI18N
        right.addActionListener(formListener);

        left.setName("left"); // NOI18N
        left.addActionListener(formListener);

        topRight.setName("topRight"); // NOI18N
        topRight.addActionListener(formListener);

        shadowColor.setText("000000");
        shadowColor.setEnabled(false);
        shadowColor.setName("shadowColor"); // NOI18N

        jLabel11.setText("Image Border");
        jLabel11.setName("jLabel11"); // NOI18N

        bottomRight.setName("bottomRight"); // NOI18N
        bottomRight.addActionListener(formListener);

        changeLineColor.setText("...");
        changeLineColor.setEnabled(false);
        changeLineColor.setName("changeLineColor"); // NOI18N

        borderType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "[Null]", "[Empty]", "Bevel", "Etched", "Line", "Round", "Image", "Horizontal Image", "Vertical Image" }));
        borderType.setName("borderType"); // NOI18N
        borderType.addActionListener(formListener);

        jLabel3.setText("Highlight Color");
        jLabel3.setName("jLabel3"); // NOI18N

        secondaryShadowColor.setText("000000");
        secondaryShadowColor.setEnabled(false);
        secondaryShadowColor.setName("secondaryShadowColor"); // NOI18N

        jLabel6.setText("Secondary");
        jLabel6.setName("jLabel6"); // NOI18N

        bottomLeft.setName("bottomLeft"); // NOI18N
        bottomLeft.addActionListener(formListener);

        jLabel1.setText("Type");
        jLabel1.setName("jLabel1"); // NOI18N

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

        top.setName("top"); // NOI18N
        top.addActionListener(formListener);

        arcWidth.setEnabled(false);
        arcWidth.setName("arcWidth"); // NOI18N
        arcWidth.addChangeListener(formListener);

        bottom.setName("bottom"); // NOI18N
        bottom.addActionListener(formListener);

        imageMode.setText("3 Image Mode");
        imageMode.setEnabled(false);
        imageMode.setMargin(new java.awt.Insets(0, 0, 0, 0));
        imageMode.setName("imageMode"); // NOI18N
        imageMode.addActionListener(formListener);

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

        topLeft.setName("topLeft"); // NOI18N
        topLeft.addActionListener(formListener);

        themeColors.setText("Theme Colors");
        themeColors.setEnabled(false);
        themeColors.setName("themeColors"); // NOI18N
        themeColors.addActionListener(formListener);

        imageBorderPreview.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        imageBorderPreview.setName("imageBorderPreview"); // NOI18N
        imageBorderPreview.setPreferredSize(new java.awt.Dimension(200, 80));

        jLabel7.setText("Raised");
        jLabel7.setName("jLabel7"); // NOI18N

        jLabel5.setText("Secondary");
        jLabel5.setName("jLabel5"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(imageBorderPreview, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 486, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel1)
                            .add(jLabel2)
                            .add(jLabel12)
                            .add(jLabel8)
                            .add(jLabel3)
                            .add(jLabel4)
                            .add(jLabel7)
                            .add(jLabel10)
                            .add(jLabel11))
                        .add(6, 6, 6)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(left, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(center, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(6, 6, 6)
                                .add(right, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(imageMode)
                                .add(235, 235, 235))
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(raisedBorder)
                                .add(307, 307, 307))
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(shadowColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(changeShadowColor)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel6)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(secondaryShadowColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(changeSecondaryShadowColor)
                                .add(79, 79, 79))
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(arcWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 62, Short.MAX_VALUE)
                                .add(56, 56, 56)
                                .add(jLabel9)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(arcHeight, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 62, Short.MAX_VALUE)
                                .add(130, 130, 130))
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(lineColor)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(changeLineColor)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(themeColors)
                                .add(134, 134, 134))
                            .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                .add(jPanel2Layout.createSequentialGroup()
                                    .add(bottomLeft, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(bottom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(6, 6, 6)
                                    .add(bottomRight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .add(jPanel2Layout.createSequentialGroup()
                                    .add(topLeft, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 104, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                    .add(top, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(6, 6, 6)
                                    .add(topRight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, borderType, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, thickness)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                                        .add(highlightColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(changeHighlightColor)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jLabel5)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(secondaryHighlightColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(changeSecondaryHighlightColor)))
                                .add(79, 79, 79)))
                        .add(296, 296, 296))))
        );

        jPanel2Layout.linkSize(new java.awt.Component[] {arcHeight, arcWidth, highlightColor, lineColor, secondaryHighlightColor, secondaryShadowColor, shadowColor}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel2Layout.linkSize(new java.awt.Component[] {changeHighlightColor, changeLineColor, changeSecondaryHighlightColor, changeSecondaryShadowColor, changeShadowColor}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel2Layout.linkSize(new java.awt.Component[] {bottom, bottomLeft, bottomRight, center, left, right, top, topLeft, topRight}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel1)
                    .add(borderType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel2)
                    .add(lineColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeLineColor)
                    .add(themeColors))
                .add(3, 3, 3)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(jLabel12)
                    .add(thickness, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(arcWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel8)
                    .add(jLabel9)
                    .add(arcHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(highlightColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeHighlightColor)
                    .add(jLabel5)
                    .add(secondaryHighlightColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeSecondaryHighlightColor))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(shadowColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeShadowColor)
                    .add(jLabel6)
                    .add(secondaryShadowColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(changeSecondaryShadowColor))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(raisedBorder)
                    .add(jLabel7))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(imageMode))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(topLeft, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 36, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(top, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(topRight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(bottomLeft, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(bottom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(bottomRight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(left, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(center, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(right, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(jLabel11))
                .add(18, 18, 18)
                .add(imageBorderPreview, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel2Layout.linkSize(new java.awt.Component[] {bottom, bottomLeft, bottomRight, center, left, right, top, topLeft, topRight}, org.jdesktop.layout.GroupLayout.VERTICAL);

        jSplitPane1.setLeftComponent(jPanel2);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == okButton) {
                BorderEditor.this.okButtonActionPerformed(evt);
            }
            else if (evt.getSource() == cancelButton) {
                BorderEditor.this.cancelButtonActionPerformed(evt);
            }
            else if (evt.getSource() == center) {
                BorderEditor.this.centerActionPerformed(evt);
            }
            else if (evt.getSource() == right) {
                BorderEditor.this.rightActionPerformed(evt);
            }
            else if (evt.getSource() == left) {
                BorderEditor.this.leftActionPerformed(evt);
            }
            else if (evt.getSource() == topRight) {
                BorderEditor.this.topRightActionPerformed(evt);
            }
            else if (evt.getSource() == bottomRight) {
                BorderEditor.this.bottomRightActionPerformed(evt);
            }
            else if (evt.getSource() == borderType) {
                BorderEditor.this.borderTypeActionPerformed(evt);
            }
            else if (evt.getSource() == bottomLeft) {
                BorderEditor.this.bottomLeftActionPerformed(evt);
            }
            else if (evt.getSource() == top) {
                BorderEditor.this.topActionPerformed(evt);
            }
            else if (evt.getSource() == bottom) {
                BorderEditor.this.bottomActionPerformed(evt);
            }
            else if (evt.getSource() == imageMode) {
                BorderEditor.this.imageModeActionPerformed(evt);
            }
            else if (evt.getSource() == raisedBorder) {
                BorderEditor.this.raisedBorderActionPerformed(evt);
            }
            else if (evt.getSource() == topLeft) {
                BorderEditor.this.topLeftActionPerformed(evt);
            }
            else if (evt.getSource() == themeColors) {
                BorderEditor.this.themeColorsActionPerformed(evt);
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
        }
    }// </editor-fold>//GEN-END:initComponents

private void borderTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_borderTypeActionPerformed
    updateBorder();
    String entry = "";
    switch(borderType.getSelectedIndex()) {
        case 0:
        case 1:
            entry = "empty";
            break;
        case 2:
            entry = "bevel";
            break;
        case 3:
            entry = "etched";
            break;
        case 4:
            entry = "line";
            break;
        case 5:
            entry = "round";
            break;
        case 6:
        case 7:
        case 8:
            entry = "image";
            break;
    }
    try {
        help.setPage(getClass().getResource("/help/borderHelp.html").toExternalForm() + "#" + entry);
    } catch (IOException ex) {
        ex.printStackTrace();
    }

}//GEN-LAST:event_borderTypeActionPerformed

    private void updateBorder() {
        updateBorder(true);
    }


    private Image getButtonImageBorderIconNotNull(JComboBox b) {
        Image i = (Image)b.getSelectedItem();
        if(i == null) {
            return resources.getImage(resources.getImageResourceNames()[0]);
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
                // round border
                if(themeColors.isSelected()) {
                    currentBorder = Border.createRoundBorder(((Number)arcWidth.getValue()).intValue(), 
                        ((Number)arcHeight.getValue()).intValue());
                } else {
                    currentBorder = Border.createRoundBorder(((Number)arcWidth.getValue()).intValue(), 
                        ((Number)arcHeight.getValue()).intValue(), getColor(lineColor));
                }
                break;
        }
        CodenameOneComponentWrapper w = (CodenameOneComponentWrapper)imageBorderPreview;
        Button b = (Button)w.getCodenameOneComponent();
        b.setPreferredSize(new com.codename1.ui.geom.Dimension(200, 50));
        b.getSelectedStyle().setPadding(20, 20, 20, 20);
        b.getUnselectedStyle().setPadding(20, 20, 20, 20);
        b.getSelectedStyle().setBorder(currentBorder);
        b.getUnselectedStyle().setBorder(currentBorder);
        b.getParent().revalidate();
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

public Border getResult() {
    return currentBorder;
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner arcHeight;
    private javax.swing.JSpinner arcWidth;
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
    private javax.swing.JTextPane help;
    private javax.swing.JTextField highlightColor;
    private javax.swing.JLabel imageBorderPreview;
    private javax.swing.JCheckBox imageMode;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JComboBox left;
    private javax.swing.JTextField lineColor;
    private javax.swing.JButton okButton;
    private javax.swing.JCheckBox raisedBorder;
    private javax.swing.JComboBox right;
    private javax.swing.JTextField secondaryHighlightColor;
    private javax.swing.JTextField secondaryShadowColor;
    private javax.swing.JTextField shadowColor;
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
