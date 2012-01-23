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
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.AbstractBorder;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * Part of the image border wizard in the theme
 *
 * @author Shai Almog
 */
public class ImageBorderWizard extends javax.swing.JPanel {
    private JColorChooser colorChooser;
    private JComponent wiz;
    
    /** Creates new form ImageBorderWizard */
    public ImageBorderWizard() {
        initComponents();
        arcHeight.setModel(new SpinnerNumberModel(10, 1, 50, 1));
        arcWidth.setModel(new SpinnerNumberModel(10, 1, 50, 1));
        com.codename1.ui.Button btn = new com.codename1.ui.Button();
        int bgColor = btn.getStyle().getBgColor();
        int fgColor = btn.getStyle().getFgColor();
        colorA.setText(Integer.toHexString(bgColor));
        colorB.setText(Integer.toHexString(new Color(bgColor).darker().darker().getRGB() & 0xffffff));
        colorC.setText(Integer.toHexString(fgColor));
        colorD.setText(Integer.toHexString(new Color(fgColor).brighter().brighter().getRGB() & 0xffffff));
        bindColorIconToButton(pickColorA, colorA);
        bindColorIconToButton(pickColorB, colorB);
        bindColorIconToButton(pickColorC, colorC);
        bindColorIconToButton(pickColorD, colorD);
        height.setModel(new SpinnerNumberModel(40, 20, 400, 1));
        opacity.setModel(new SpinnerNumberModel(255, 0, 255, 1));
        thickness.setModel(new SpinnerNumberModel(1, 1, 30, 1));
        width.setModel(new SpinnerNumberModel(150, 20, 400, 1));
        trackTextFieldChanges(colorA);
        trackTextFieldChanges(colorB);
        trackTextFieldChanges(colorC);
        trackTextFieldChanges(colorD);
        updateBorderImage();
    }

    void setWiz(JComponent wiz) {
        this.wiz = wiz;
    }

    private void trackTextFieldChanges(JTextComponent c) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateBorderImage();
            }

            public void removeUpdate(DocumentEvent e) {
                updateBorderImage();
            }

            public void changedUpdate(DocumentEvent e) {
                updateBorderImage();
            }
        });
    }

    private void pickColor(final JTextComponent colorText) {
        int color = Integer.decode("0x" + colorText.getText());
        if(colorChooser == null) {
            colorChooser = new JColorChooser();
        }
        colorChooser.setColor(color);

        JDialog dlg = JColorChooser.createDialog(this, "Pick color", true, colorChooser, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = colorChooser.getColor().getRGB() & 0xffffff;
                colorText.setText(Integer.toHexString(i));
            }
        }, null);
        dlg.setLocationByPlatform(true);
        dlg.pack();
        dlg.setVisible(true);
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
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new ImagePanel();
        borderImage = new javax.swing.JLabel();
        useAFile = new javax.swing.JRadioButton();
        generate = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        width = new javax.swing.JSpinner();
        height = new javax.swing.JSpinner();
        borderType = new javax.swing.JComboBox();
        colorA = new javax.swing.JTextField();
        colorB = new javax.swing.JTextField();
        colorC = new javax.swing.JTextField();
        colorD = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        thickness = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        opacity = new javax.swing.JSpinner();
        jLabel10 = new javax.swing.JLabel();
        arcWidth = new javax.swing.JSpinner();
        jLabel11 = new javax.swing.JLabel();
        arcHeight = new javax.swing.JSpinner();
        pickColorA = new javax.swing.JButton();
        pickColorB = new javax.swing.JButton();
        pickColorC = new javax.swing.JButton();
        pickColorD = new javax.swing.JButton();

        FormListener formListener = new FormListener();

        setOpaque(false);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setOpaque(false);

        borderImage.setName("borderImage"); // NOI18N
        jPanel1.add(borderImage);

        jScrollPane1.setViewportView(jPanel1);

        buttonGroup1.add(useAFile);
        useAFile.setText("Use A File");
        useAFile.setName("useAFile"); // NOI18N
        useAFile.addActionListener(formListener);

        buttonGroup1.add(generate);
        generate.setSelected(true);
        generate.setText("Generate");
        generate.setName("generate"); // NOI18N

        jLabel1.setText("Width");
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText("Height");
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel3.setText("Border Type");
        jLabel3.setName("jLabel3"); // NOI18N

        jLabel4.setText("Color A");
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText("Color B");
        jLabel5.setName("jLabel5"); // NOI18N

        jLabel6.setText("Color C");
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel7.setText("Color D");
        jLabel7.setName("jLabel7"); // NOI18N

        width.setName("width"); // NOI18N
        width.addChangeListener(formListener);

        height.setName("height"); // NOI18N
        height.addChangeListener(formListener);

        borderType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Round", "Line", "Raised Etched", "Lowered Etched", "Raised Bevel", "Lowered Bevel" }));
        borderType.setName("borderType"); // NOI18N
        borderType.addActionListener(formListener);

        colorA.setName("colorA"); // NOI18N
        colorA.addActionListener(formListener);

        colorB.setName("colorB"); // NOI18N
        colorB.addActionListener(formListener);

        colorC.setName("colorC"); // NOI18N
        colorC.addActionListener(formListener);

        colorD.setName("colorD"); // NOI18N
        colorD.addActionListener(formListener);

        jLabel8.setText("Thickness");
        jLabel8.setName("jLabel8"); // NOI18N

        thickness.setName("thickness"); // NOI18N
        thickness.addChangeListener(formListener);

        jLabel9.setText("Opacity");
        jLabel9.setName("jLabel9"); // NOI18N

        opacity.setName("opacity"); // NOI18N
        opacity.addChangeListener(formListener);

        jLabel10.setText("Arc Width");
        jLabel10.setName("jLabel10"); // NOI18N

        arcWidth.setName("arcWidth"); // NOI18N
        arcWidth.addChangeListener(formListener);

        jLabel11.setText("Arc Height");
        jLabel11.setName("jLabel11"); // NOI18N

        arcHeight.setName("arcHeight"); // NOI18N
        arcHeight.addChangeListener(formListener);

        pickColorA.setText("...");
        pickColorA.setName("pickColorA"); // NOI18N
        pickColorA.addActionListener(formListener);

        pickColorB.setText("...");
        pickColorB.setName("pickColorB"); // NOI18N
        pickColorB.addActionListener(formListener);

        pickColorC.setText("...");
        pickColorC.setName("pickColorC"); // NOI18N
        pickColorC.addActionListener(formListener);

        pickColorD.setText("...");
        pickColorD.setName("pickColorD"); // NOI18N
        pickColorD.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jLabel1)
                                            .add(jLabel2)
                                            .add(jLabel3)
                                            .add(jLabel8)
                                            .add(jLabel10))
                                        .add(19, 19, 19))
                                    .add(layout.createSequentialGroup()
                                        .add(jLabel11)
                                        .add(28, 28, 28)))
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, arcHeight, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, thickness, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, borderType, 0, 98, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, height, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, width, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, arcWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE))
                                .add(18, 18, 18)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel9)
                                    .add(jLabel7)
                                    .add(jLabel6)
                                    .add(jLabel5)
                                    .add(jLabel4))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(opacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 98, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(layout.createSequentialGroup()
                                        .add(colorD, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(pickColorD))
                                    .add(layout.createSequentialGroup()
                                        .add(colorC, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(pickColorC))
                                    .add(layout.createSequentialGroup()
                                        .add(colorB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(pickColorB))
                                    .add(layout.createSequentialGroup()
                                        .add(colorA, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(pickColorA))))
                            .add(layout.createSequentialGroup()
                                .add(useAFile)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(generate))))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(new java.awt.Component[] {colorA, colorB, colorC, colorD, opacity}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 168, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(useAFile)
                    .add(generate))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel4)
                            .add(colorA, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(pickColorA))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel5)
                            .add(colorB, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(pickColorB))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel6)
                            .add(colorC, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(pickColorC))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel7)
                            .add(colorD, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(pickColorD))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel9)
                            .add(opacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel1)
                            .add(width, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel2)
                            .add(height, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel3)
                            .add(borderType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel8)
                            .add(thickness, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel10)
                            .add(arcWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(arcHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel11))))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == useAFile) {
                ImageBorderWizard.this.useAFileActionPerformed(evt);
            }
            else if (evt.getSource() == borderType) {
                ImageBorderWizard.this.borderTypeActionPerformed(evt);
            }
            else if (evt.getSource() == colorA) {
                ImageBorderWizard.this.colorAActionPerformed(evt);
            }
            else if (evt.getSource() == colorB) {
                ImageBorderWizard.this.colorBActionPerformed(evt);
            }
            else if (evt.getSource() == colorC) {
                ImageBorderWizard.this.colorCActionPerformed(evt);
            }
            else if (evt.getSource() == colorD) {
                ImageBorderWizard.this.colorDActionPerformed(evt);
            }
            else if (evt.getSource() == pickColorA) {
                ImageBorderWizard.this.pickColorAActionPerformed(evt);
            }
            else if (evt.getSource() == pickColorB) {
                ImageBorderWizard.this.pickColorBActionPerformed(evt);
            }
            else if (evt.getSource() == pickColorC) {
                ImageBorderWizard.this.pickColorCActionPerformed(evt);
            }
            else if (evt.getSource() == pickColorD) {
                ImageBorderWizard.this.pickColorDActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == width) {
                ImageBorderWizard.this.widthStateChanged(evt);
            }
            else if (evt.getSource() == height) {
                ImageBorderWizard.this.heightStateChanged(evt);
            }
            else if (evt.getSource() == thickness) {
                ImageBorderWizard.this.thicknessStateChanged(evt);
            }
            else if (evt.getSource() == opacity) {
                ImageBorderWizard.this.opacityStateChanged(evt);
            }
            else if (evt.getSource() == arcWidth) {
                ImageBorderWizard.this.arcWidthStateChanged(evt);
            }
            else if (evt.getSource() == arcHeight) {
                ImageBorderWizard.this.arcHeightStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void widthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_widthStateChanged
        updateBorderImage();
        wiz.revalidate();
        wiz.repaint();
    }//GEN-LAST:event_widthStateChanged

    private void heightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_heightStateChanged
        updateBorderImage();
        wiz.revalidate();
        wiz.repaint();
    }//GEN-LAST:event_heightStateChanged

    private void borderTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_borderTypeActionPerformed
        updateBorderImage();
    }//GEN-LAST:event_borderTypeActionPerformed

    private void thicknessStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_thicknessStateChanged
        updateBorderImage();
    }//GEN-LAST:event_thicknessStateChanged

    private void arcWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arcWidthStateChanged
        updateBorderImage();
    }//GEN-LAST:event_arcWidthStateChanged

    private void arcHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_arcHeightStateChanged
        updateBorderImage();
    }//GEN-LAST:event_arcHeightStateChanged

    private void opacityStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_opacityStateChanged
        updateBorderImage();
    }//GEN-LAST:event_opacityStateChanged

    private void colorAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorAActionPerformed
        updateBorderImage();
    }//GEN-LAST:event_colorAActionPerformed

    private void colorBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorBActionPerformed
        updateBorderImage();
    }//GEN-LAST:event_colorBActionPerformed

    private void colorCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorCActionPerformed
        updateBorderImage();
    }//GEN-LAST:event_colorCActionPerformed

    private void colorDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorDActionPerformed
        updateBorderImage();
    }//GEN-LAST:event_colorDActionPerformed

    private void pickColorAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pickColorAActionPerformed
        pickColor(colorA);
    }//GEN-LAST:event_pickColorAActionPerformed

    private void pickColorBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pickColorBActionPerformed
        pickColor(colorB);
    }//GEN-LAST:event_pickColorBActionPerformed

    private void pickColorCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pickColorCActionPerformed
        pickColor(colorC);
    }//GEN-LAST:event_pickColorCActionPerformed

    private void pickColorDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pickColorDActionPerformed
        pickColor(colorD);
    }//GEN-LAST:event_pickColorDActionPerformed

    private void useAFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useAFileActionPerformed
        File[] files = ResourceEditorView.showOpenFileChooser("Border Image", ".png", ".jpg", ".gif", ".svg");
        if(files != null) {
            try {
                BufferedImage bi;
                if(files[0].getName().toLowerCase().endsWith(".svg")) {
                    try {
                        InputStream input = new FileInputStream(files[0]);
                        org.apache.batik.transcoder.image.PNGTranscoder t = new org.apache.batik.transcoder.image.PNGTranscoder();
                        org.apache.batik.transcoder.TranscoderInput i = new org.apache.batik.transcoder.TranscoderInput(input);
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        org.apache.batik.transcoder.TranscoderOutput o = new org.apache.batik.transcoder.TranscoderOutput(bo);
                        t.transcode(i, o);
                        bo.close();
                        input.close();
                        bi = ImageIO.read(new ByteArrayInputStream(bo.toByteArray()));
                    } catch (org.apache.batik.transcoder.TranscoderException ex) {
                        ex.printStackTrace();
                        throw new IOException(ex);
                    }
                } else {
                    bi = ImageIO.read(files[0]);
                }
                borderImage.setIcon(new ImageIcon(bi));
                borderImage.setBorder(null);
                borderImage.setPreferredSize(new Dimension(bi.getWidth(), bi.getHeight()));
                wiz.revalidate();
                borderImage.revalidate();
                wiz.repaint();
                borderImage.repaint();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "IO Error: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_useAFileActionPerformed

    public BufferedImage getImage() {
        if(generate.isSelected()) {
            BufferedImage b = new BufferedImage(borderImage.getWidth(), borderImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g2d = b.createGraphics();
            borderImage.paint(g2d);
            g2d.dispose();
            return b;
        } else {
            return (BufferedImage)((ImageIcon)borderImage.getIcon()).getImage();
        }
    }

    private Color getColor(JTextField colorField) {
        try {
            return new Color(Integer.parseInt(colorField.getText(), 16));
        } catch(NumberFormatException e) {
            return Color.BLACK;
        }
    }

    private void updateBorderImage() {
        if(useAFile.isSelected()) {
            borderImage.setBorder(null);
            return;
        }
        Border res;
        borderImage.setIcon(null);
        switch(borderType.getSelectedIndex()) {
            case 1: // Line
                res = new LineBorder(getColor(colorA), ((Number)thickness.getValue()).intValue(), true);
                break;
            case 2: // Raised Etched
                res = BorderFactory.createEtchedBorder(EtchedBorder.RAISED, getColor(colorA), getColor(colorB));
                break;
            case 3: // Lowered Etched
                res = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED, getColor(colorA), getColor(colorB));
                break;
            case 4: // Raised Bevel
                res = BorderFactory.createBevelBorder(BevelBorder.RAISED, getColor(colorA), getColor(colorB), getColor(colorC), getColor(colorD));
                break;
            case 5: // Lowered Bevel
                res = BorderFactory.createBevelBorder(BevelBorder.LOWERED, getColor(colorA), getColor(colorB), getColor(colorC), getColor(colorD));
                break;
            default: // Round
                res = new RoundedBorder();
                break;
        }
        Color c = getColor(colorC);
        borderImage.setBackground(new Color(c.getRed(), c.getGreen(), c.getBlue(), ((Number)opacity.getValue()).intValue()));
        borderImage.setPreferredSize(new Dimension(((Number)width.getValue()).intValue(),
                ((Number)height.getValue()).intValue()));
        borderImage.setBorder(res);
        borderImage.revalidate();
        borderImage.repaint();
    }

    private int get(JSpinner s) {
        return ((Number)s.getValue()).intValue();
    }

    class RoundedBorder extends AbstractBorder {

        public void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
            g.setColor(getColor(colorA));
            Graphics2D g2d = (Graphics2D)g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, get(opacity) / 255.0f));

            RoundRectangle2D rr = new RoundRectangle2D.Float(x, y, width - 1, height - 1, get(arcWidth), get(arcHeight));
            g2d.setPaint(new GradientPaint(x + width / 2, y, getColor(colorA), x + width / 2, y + height, getColor(colorB)));
            g2d.fill(rr);
            if(get(thickness) > 0) {
                g2d.setPaint(new GradientPaint(x + width / 2, y, getColor(colorC), x + width / 2, y + height, getColor(colorD)));
                g2d.setStroke(new BasicStroke(get(thickness)));
                g2d.draw(rr);
            }
        }

        public Insets getBorderInsets( Component c ) {
            return new Insets( 10, 10, 10, 10 );
        }

        public Insets getBorderInsets( Component c, Insets insets ) {
            insets.left = insets.top = insets.right = insets.bottom = 10;
            return insets;
        }
    }

    private void bindColorIconToButton(final JButton button, final JTextComponent text) {
        ColorIcon.install(button, text);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner arcHeight;
    private javax.swing.JSpinner arcWidth;
    private javax.swing.JLabel borderImage;
    private javax.swing.JComboBox borderType;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTextField colorA;
    private javax.swing.JTextField colorB;
    private javax.swing.JTextField colorC;
    private javax.swing.JTextField colorD;
    private javax.swing.JRadioButton generate;
    private javax.swing.JSpinner height;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
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
    private javax.swing.JSpinner opacity;
    private javax.swing.JButton pickColorA;
    private javax.swing.JButton pickColorB;
    private javax.swing.JButton pickColorC;
    private javax.swing.JButton pickColorD;
    private javax.swing.JSpinner thickness;
    private javax.swing.JRadioButton useAFile;
    private javax.swing.JSpinner width;
    // End of variables declaration//GEN-END:variables
}
