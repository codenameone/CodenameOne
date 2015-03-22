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
import com.codename1.ui.EncodedImage;
import com.codename1.ui.util.EditableResources;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * UI and logic to add several multi-images in a single batch and scale them all 
 * to the right sizes for all resolutions.
 *
 * @author Shai Almog
 */
public class AddAndScaleMultiImage extends javax.swing.JPanel {
    private float aspect;
    private File selectedFile;
    
    /** Creates new form AddAndScaleMultiImage */
    public AddAndScaleMultiImage() {
        initComponents();
        initSpinner(veryLowWidth, "veryLowWidthSpinner", 28);
        initSpinner(veryLowHeight, "veryLowHeightSpinner", 28);
        initSpinner(lowWidth, "lowWidthSpinner", 36);
        initSpinner(lowHeight, "lowHeightSpinner", 36);
        initSpinner(mediumWidth, "mediumWidthSpinner", 48);
        initSpinner(mediumHeight, "mediumHeightSpinner", 48);
        initSpinner(highWidth, "highWidthSpinner", 72);
        initSpinner(highHeight, "highHeightSpinner", 72);
        initSpinner(veryHighWidth, "veryHighWidthSpinner", 96);
        initSpinner(veryHighHeight, "veryHighHeightSpinner", 96);
        initSpinner(hdWidth, "hdWidthSpinner", 196);
        initSpinner(hdHeight, "hdHeightSpinner", 196);
        percentWidth.setModel(new SpinnerNumberModel(20.0, 1.0, 100.0, 1.0));
        percentHeight.setModel(new SpinnerNumberModel(15.0, 1.0, 100.0, 1.0));
    }

    private float get(JSpinner s) {
        return ((Number)s.getValue()).floatValue();
    }

    private EncodedImage createScale(BufferedImage bi, JSpinner ws, JSpinner hs) throws IOException {
        int w = (int)get(ws);
        int h = (int)get(hs);
        if(w != 0 && h != 0) {
            return EncodedImage.create(scale(bi, w, h));
        }
        return null;
    }

    public void generate(File[] files, EditableResources res, int sourceResolution) {
        for(File f : files) {
            try {
                BufferedImage bi = ImageIO.read(f);
                EditableResources.MultiImage newImage = new EditableResources.MultiImage();
                
                int[] DPIS = new int[] {com.codename1.ui.Display.DENSITY_VERY_LOW,
                    com.codename1.ui.Display.DENSITY_LOW,
                    com.codename1.ui.Display.DENSITY_MEDIUM,
                    com.codename1.ui.Display.DENSITY_HIGH,
                    com.codename1.ui.Display.DENSITY_VERY_HIGH,
                    com.codename1.ui.Display.DENSITY_HD};
                float[] WIDTHS = {
                    176, 240, 360, 480, 640, 1024
                };
                EncodedImage[] images = new EncodedImage[6];
                int imageCount = 6;
                
                for(int iter = 0 ; iter < DPIS.length ; iter++) {
                    if(iter == sourceResolution) {
                        images[iter] = EncodedImage.create(scale(bi, bi.getWidth(), bi.getHeight()));
                    } else {
                        float sourceWidth = WIDTHS[sourceResolution];
                        float destWidth = WIDTHS[iter];
                        int h = (int)(((float)bi.getHeight()) * destWidth / sourceWidth);
                        int w = (int)(((float)bi.getWidth()) * destWidth / sourceWidth);
                        images[iter] = EncodedImage.create(scale(bi, w, h));
                    }
                }

                if(imageCount > 0) {
                    int offset = 0;
                    EncodedImage[] result = new EncodedImage[imageCount];
                    int[] resultDPI = new int[imageCount];
                    for(int iter = 0 ; iter < images.length ; iter++) {
                        if(images[iter] != null) {
                            result[offset] = images[iter];
                            resultDPI[offset] = DPIS[iter];
                            offset++;
                        }
                    }
                    newImage.setDpi(resultDPI);
                    newImage.setInternalImages(result);
                    String destName = f.getName();
                    int count = 1;
                    while(res.containsResource(destName)) {
                        destName = f.getName() + " " + count;
                        count++;
                    }
                    res.setMultiImage(destName, newImage);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error reading file: " + f, "IO Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void generate(File[] files, EditableResources res) {
        for(File f : files) {
            try {
                BufferedImage bi = ImageIO.read(f);
                EditableResources.MultiImage newImage = new EditableResources.MultiImage();
                
                int[] DPIS = new int[] {com.codename1.ui.Display.DENSITY_VERY_LOW,
                    com.codename1.ui.Display.DENSITY_LOW,
                    com.codename1.ui.Display.DENSITY_MEDIUM,
                    com.codename1.ui.Display.DENSITY_HIGH,
                    com.codename1.ui.Display.DENSITY_VERY_HIGH,
                    com.codename1.ui.Display.DENSITY_HD};
                EncodedImage[] images = new EncodedImage[6];
                int imageCount = 0;
                JSpinner[] ws = {veryLowWidth, lowWidth, mediumWidth, highWidth, veryHighWidth, hdWidth};
                JSpinner[] hs = {veryLowHeight, lowHeight, mediumHeight, highHeight, veryHighHeight, hdHeight};
                if(squareImages.isSelected()) {
                    hs = ws;
                }

                for(int iter = 0 ; iter < ws.length ; iter++) {
                    images[iter] = createScale(bi, ws[iter], hs[iter]);
                    if(images[iter] != null) {
                        imageCount++;
                    }
                }

                if(imageCount > 0) {
                    int offset = 0;
                    EncodedImage[] result = new EncodedImage[imageCount];
                    int[] resultDPI = new int[imageCount];
                    for(int iter = 0 ; iter < images.length ; iter++) {
                        if(images[iter] != null) {
                            result[offset] = images[iter];
                            resultDPI[offset] = DPIS[iter];
                            offset++;
                        }
                    }
                    newImage.setDpi(resultDPI);
                    newImage.setInternalImages(result);
                    String destName = f.getName();
                    int count = 1;
                    while(res.containsResource(destName)) {
                        destName = f.getName() + " " + count;
                    }
                    res.setMultiImage(destName, newImage);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error reading file: " + f, "IO Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private byte[] scale(BufferedImage bi, int w, int h) throws IOException {
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(bi, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        g2d.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(scaled, "png", output);
        output.close();
        return output.toByteArray();
    }

    private void initSpinner(final JSpinner s, final String name, int defaultVal) {
        int val = Preferences.userNodeForPackage(ResourceEditorView.class).getInt(name, defaultVal);
        s.setModel(new SpinnerNumberModel(val, 0, 1024, 1));
        s.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int val = ((Number)s.getValue()).intValue();
                if(val >= 0 && val <= 1024) {
                    Preferences.userNodeForPackage(ResourceEditorView.class).putInt(name, val);
                }
            }
        });
    }

    public void selectFiles(JComponent parent, EditableResources res) {
        File[] selection = ResourceEditorView.showOpenFileChooser(true, "Images", ".gif", ".png", ".jpg");
        if (selection == null || selection.length == 0) {
            return;
        }
        selectedFile = selection[0];
        int result = JOptionPane.showConfirmDialog(parent, this, "Select Resolutions", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(result != JOptionPane.OK_OPTION) {
            return;
        }
        generate(selection, res);
    }
    
    public void selectFilesSimpleMode(JComponent parent, EditableResources res) {
        File[] selection = ResourceEditorView.showOpenFileChooser(true, "Images", ".gif", ".png", ".jpg");
        if (selection == null) {
            return;
        }
        JComboBox sourceResolution = new JComboBox(new String[] {"Very Low", "Low", "Medium", "High", "Very High", "HD"});
        sourceResolution.setSelectedIndex(4);
        int result = JOptionPane.showConfirmDialog(parent, sourceResolution, "Select Source Resolutions", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(result != JOptionPane.OK_OPTION) {
            return;
        }
        generate(selection, res, sourceResolution.getSelectedIndex());
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
        jLabel2 = new javax.swing.JLabel();
        veryLowWidth = new javax.swing.JSpinner();
        veryLowHeight = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        lowWidth = new javax.swing.JSpinner();
        lowHeight = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        mediumWidth = new javax.swing.JSpinner();
        mediumHeight = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        highWidth = new javax.swing.JSpinner();
        highHeight = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        veryHighWidth = new javax.swing.JSpinner();
        veryHighHeight = new javax.swing.JSpinner();
        jLabel10 = new javax.swing.JLabel();
        hdWidth = new javax.swing.JSpinner();
        hdHeight = new javax.swing.JSpinner();
        jLabel11 = new javax.swing.JLabel();
        percentWidth = new javax.swing.JSpinner();
        percentHeight = new javax.swing.JSpinner();
        squareImages = new javax.swing.JCheckBox();
        preserveAspectRatio = new javax.swing.JCheckBox();

        FormListener formListener = new FormListener();

        setName("Form"); // NOI18N

        jLabel1.setText("Select The Sizes For Every DPI (select 0 to ignore)");
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText("Very Low");
        jLabel2.setName("jLabel2"); // NOI18N

        veryLowWidth.setName("veryLowWidth"); // NOI18N
        veryLowWidth.addChangeListener(formListener);

        veryLowHeight.setName("veryLowHeight"); // NOI18N
        veryLowHeight.addChangeListener(formListener);

        jLabel3.setText("Size");
        jLabel3.setName("jLabel3"); // NOI18N

        jLabel4.setText("Width");
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText("Height");
        jLabel5.setName("jLabel5"); // NOI18N

        jLabel6.setText("Low");
        jLabel6.setName("jLabel6"); // NOI18N

        lowWidth.setName("lowWidth"); // NOI18N
        lowWidth.addChangeListener(formListener);

        lowHeight.setName("lowHeight"); // NOI18N
        lowHeight.addChangeListener(formListener);

        jLabel7.setText("Medium");
        jLabel7.setName("jLabel7"); // NOI18N

        mediumWidth.setName("mediumWidth"); // NOI18N
        mediumWidth.addChangeListener(formListener);

        mediumHeight.setName("mediumHeight"); // NOI18N
        mediumHeight.addChangeListener(formListener);

        jLabel8.setText("High");
        jLabel8.setName("jLabel8"); // NOI18N

        highWidth.setName("highWidth"); // NOI18N
        highWidth.addChangeListener(formListener);

        highHeight.setName("highHeight"); // NOI18N
        highHeight.addChangeListener(formListener);

        jLabel9.setText("Very High");
        jLabel9.setName("jLabel9"); // NOI18N

        veryHighWidth.setName("veryHighWidth"); // NOI18N
        veryHighWidth.addChangeListener(formListener);

        veryHighHeight.setName("veryHighHeight"); // NOI18N
        veryHighHeight.addChangeListener(formListener);

        jLabel10.setText("HD");
        jLabel10.setName("jLabel10"); // NOI18N

        hdWidth.setName("hdWidth"); // NOI18N
        hdWidth.addChangeListener(formListener);

        hdHeight.setName("hdHeight"); // NOI18N
        hdHeight.addChangeListener(formListener);

        jLabel11.setText("% (will affect all entries)");
        jLabel11.setName("jLabel11"); // NOI18N

        percentWidth.setName("percentWidth"); // NOI18N
        percentWidth.addChangeListener(formListener);

        percentHeight.setName("percentHeight"); // NOI18N
        percentHeight.addChangeListener(formListener);

        squareImages.setText("Square Images");
        squareImages.setName("squareImages"); // NOI18N
        squareImages.addActionListener(formListener);

        preserveAspectRatio.setText("Preserve Aspect Ratio");
        preserveAspectRatio.setName("preserveAspectRatio"); // NOI18N
        preserveAspectRatio.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(6, 6, 6)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel3)
                    .add(jLabel2)
                    .add(jLabel6)
                    .add(jLabel7)
                    .add(jLabel8)
                    .add(jLabel9)
                    .add(jLabel10)
                    .add(jLabel11))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(percentWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
                    .add(hdWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
                    .add(veryHighWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
                    .add(highWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
                    .add(mediumWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
                    .add(lowWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
                    .add(veryLowWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
                    .add(jLabel4))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(percentHeight)
                    .add(hdHeight)
                    .add(veryHighHeight)
                    .add(highHeight)
                    .add(mediumHeight)
                    .add(lowHeight)
                    .add(veryLowHeight)
                    .add(jLabel5))
                .add(125, 125, 125))
            .add(layout.createSequentialGroup()
                .add(10, 10, 10)
                .add(jLabel1)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(squareImages)
                    .add(preserveAspectRatio))
                .add(0, 0, Short.MAX_VALUE))
        );

        layout.linkSize(new java.awt.Component[] {jLabel4, jLabel5}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(jLabel4)
                    .add(jLabel5))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(veryLowWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(veryLowHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(lowWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(lowHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7)
                    .add(mediumWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(mediumHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(highWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(highHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9)
                    .add(veryHighWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(veryHighHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(hdWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(hdHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel11)
                    .add(percentWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(percentHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(squareImages)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(preserveAspectRatio)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == squareImages) {
                AddAndScaleMultiImage.this.squareImagesActionPerformed(evt);
            }
            else if (evt.getSource() == preserveAspectRatio) {
                AddAndScaleMultiImage.this.preserveAspectRatioActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == percentWidth) {
                AddAndScaleMultiImage.this.percentWidthStateChanged(evt);
            }
            else if (evt.getSource() == percentHeight) {
                AddAndScaleMultiImage.this.percentHeightStateChanged(evt);
            }
            else if (evt.getSource() == veryLowWidth) {
                AddAndScaleMultiImage.this.veryLowWidthStateChanged(evt);
            }
            else if (evt.getSource() == lowWidth) {
                AddAndScaleMultiImage.this.lowWidthStateChanged(evt);
            }
            else if (evt.getSource() == mediumWidth) {
                AddAndScaleMultiImage.this.mediumWidthStateChanged(evt);
            }
            else if (evt.getSource() == highWidth) {
                AddAndScaleMultiImage.this.highWidthStateChanged(evt);
            }
            else if (evt.getSource() == veryHighWidth) {
                AddAndScaleMultiImage.this.veryHighWidthStateChanged(evt);
            }
            else if (evt.getSource() == hdWidth) {
                AddAndScaleMultiImage.this.hdWidthStateChanged(evt);
            }
            else if (evt.getSource() == veryLowHeight) {
                AddAndScaleMultiImage.this.veryLowHeightStateChanged(evt);
            }
            else if (evt.getSource() == lowHeight) {
                AddAndScaleMultiImage.this.lowHeightStateChanged(evt);
            }
            else if (evt.getSource() == mediumHeight) {
                AddAndScaleMultiImage.this.mediumHeightStateChanged(evt);
            }
            else if (evt.getSource() == highHeight) {
                AddAndScaleMultiImage.this.highHeightStateChanged(evt);
            }
            else if (evt.getSource() == veryHighHeight) {
                AddAndScaleMultiImage.this.veryHighHeightStateChanged(evt);
            }
            else if (evt.getSource() == hdHeight) {
                AddAndScaleMultiImage.this.hdHeightStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void squareImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_squareImagesActionPerformed
        boolean b = !squareImages.isSelected();
        veryLowHeight.setEnabled(b);
        lowHeight.setEnabled(b);
        mediumHeight.setEnabled(b);
        highHeight.setEnabled(b);
        veryHighHeight.setEnabled(b);
        hdHeight.setEnabled(b);
        percentHeight.setEnabled(b);
    }//GEN-LAST:event_squareImagesActionPerformed

    private void percentWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_percentWidthStateChanged
        float percentRatio = get(percentWidth) / 100.0f;
        veryLowWidth.setValue((int)(174.0f * percentRatio));
        lowWidth.setValue((int)(240.0f * percentRatio));
        mediumWidth.setValue((int)(320.0f * percentRatio));
        highWidth.setValue((int)(480.0f * percentRatio));
        veryHighWidth.setValue((int)(640.0f * percentRatio));
        hdWidth.setValue((int)(1024.0f * percentRatio));
    }//GEN-LAST:event_percentWidthStateChanged

    private void percentHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_percentHeightStateChanged
        float percentRatio = get(percentHeight) / 100.0f;
        veryLowHeight.setValue((int)(220.0f * percentRatio));
        lowHeight.setValue((int)(320.0f * percentRatio));
        mediumHeight.setValue((int)(480.0f * percentRatio));
        highHeight.setValue((int)(854.0f * percentRatio));
        veryHighHeight.setValue((int)(1024.0f * percentRatio));
        hdHeight.setValue((int)(1920.0f * percentRatio));
    }//GEN-LAST:event_percentHeightStateChanged

    private void preserveAspectRatioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_preserveAspectRatioActionPerformed
        if(preserveAspectRatio.isSelected()) {
            try {
                BufferedImage bi = ImageIO.read(selectedFile);
                aspect = ((float)bi.getWidth()) / ((float)bi.getHeight());
                
                veryLowHeight.setValue( (int)(((Integer)veryLowWidth.getValue()).intValue() * aspect) );
                lowHeight.setValue((int)(((Integer)lowWidth.getValue()).intValue() * aspect));
                mediumHeight.setValue((int)(((Integer)mediumWidth.getValue()).intValue() * aspect));
                highHeight.setValue((int)(((Integer)highWidth.getValue()).intValue() * aspect));
                hdHeight.setValue((int)(((Integer)hdWidth.getValue()).intValue() * aspect));
                
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }//GEN-LAST:event_preserveAspectRatioActionPerformed

    private void veryLowWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_veryLowWidthStateChanged
        updateHeight(veryLowWidth, veryLowHeight);
    }//GEN-LAST:event_veryLowWidthStateChanged

    private void lowWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lowWidthStateChanged
        updateHeight(lowWidth, lowHeight);
    }//GEN-LAST:event_lowWidthStateChanged

    private void mediumWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_mediumWidthStateChanged
        updateHeight(mediumWidth, mediumHeight);
    }//GEN-LAST:event_mediumWidthStateChanged

    private void highWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_highWidthStateChanged
        updateHeight(highWidth, highHeight);
    }//GEN-LAST:event_highWidthStateChanged

    private void veryHighWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_veryHighWidthStateChanged
        updateHeight(veryHighWidth, veryHighHeight);
    }//GEN-LAST:event_veryHighWidthStateChanged

    private void hdWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_hdWidthStateChanged
        updateHeight(hdWidth, hdHeight);
    }//GEN-LAST:event_hdWidthStateChanged

    private void veryLowHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_veryLowHeightStateChanged
        updateWidth(veryLowHeight, veryLowWidth);
    }//GEN-LAST:event_veryLowHeightStateChanged

    private void lowHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lowHeightStateChanged
        updateWidth(lowHeight, lowWidth);
    }//GEN-LAST:event_lowHeightStateChanged

    private void mediumHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_mediumHeightStateChanged
        updateWidth(mediumHeight, mediumWidth);
    }//GEN-LAST:event_mediumHeightStateChanged

    private void highHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_highHeightStateChanged
        updateWidth(highHeight, highWidth);
    }//GEN-LAST:event_highHeightStateChanged

    private void veryHighHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_veryHighHeightStateChanged
        updateWidth(veryHighHeight, veryHighWidth);
    }//GEN-LAST:event_veryHighHeightStateChanged

    private void hdHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_hdHeightStateChanged
        updateWidth(hdHeight, hdWidth);
    }//GEN-LAST:event_hdHeightStateChanged

    private boolean lock;
    private void updateHeight(JSpinner source, JSpinner dest) {
        if(preserveAspectRatio.isSelected() && !lock) {
            lock = true;
            float width = get(source);
            float height = width / aspect;
            dest.setValue(new Integer((int)height));
            lock = false;
        }
    }

    private void updateWidth(JSpinner source, JSpinner dest) {
        if(preserveAspectRatio.isSelected() && !lock) {
            lock = true;
            float height = get(source);
            float width = height *  aspect;
            dest.setValue(new Integer((int)width));
            lock = false;
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner hdHeight;
    private javax.swing.JSpinner hdWidth;
    private javax.swing.JSpinner highHeight;
    private javax.swing.JSpinner highWidth;
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
    private javax.swing.JSpinner lowHeight;
    private javax.swing.JSpinner lowWidth;
    private javax.swing.JSpinner mediumHeight;
    private javax.swing.JSpinner mediumWidth;
    private javax.swing.JSpinner percentHeight;
    private javax.swing.JSpinner percentWidth;
    private javax.swing.JCheckBox preserveAspectRatio;
    private javax.swing.JCheckBox squareImages;
    private javax.swing.JSpinner veryHighHeight;
    private javax.swing.JSpinner veryHighWidth;
    private javax.swing.JSpinner veryLowHeight;
    private javax.swing.JSpinner veryLowWidth;
    // End of variables declaration//GEN-END:variables

}
