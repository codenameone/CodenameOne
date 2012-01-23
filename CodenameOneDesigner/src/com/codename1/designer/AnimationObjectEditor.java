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
import com.codename1.ui.animations.AnimationAccessor;
import com.codename1.ui.animations.AnimationObject;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.util.EditableResources;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Editor to edit an individual animation entry within the timeline editor
 *
 * @author Shai Almog
 */
public class AnimationObjectEditor extends javax.swing.JPanel {
    private EditableResources res;

    /** Creates new form AnimationObjectEditor */
    public AnimationObjectEditor(EditableResources e, AnimationObject o, int durationValue) {
        initComponents();
        res = e;
        ResourceEditorView.initImagesComboBox(image, res, true, false, true);
        frameDelay.setModel(new SpinnerNumberModel(-1, -1, 100000, 200));
        frameDelay.setValue(-1);
        frameWidth.setModel(new SpinnerNumberModel(30, 2, 2000, 1));
        frameHeight.setModel(new SpinnerNumberModel(30, 2, 2000, 1));
        duration.setModel(new SpinnerNumberModel(durationValue, 1, 1000000, 100));
        if(o != null) {
            startTime.setValue(o.getStartTime());
            duration.setValue(o.getEndTime() - o.getStartTime());
            frameDelay.setValue(AnimationAccessor.getFrameDelay(o));
            frameWidth.setValue(AnimationAccessor.getFrameWidth(o));
            frameHeight.setValue(AnimationAccessor.getFrameHeight(o));

            String[] imgs = res.getImageResourceNames();
            if(AnimationAccessor.getImageName(o) != null) {
                image.setSelectedItem(AnimationAccessor.getImageName(o));
            } else {
                for(int iter = 0 ; iter < imgs.length ; iter++) {
                    if(res.getImage(imgs[iter]) == AnimationAccessor.getImage(o)) {
                        image.setSelectedItem(imgs[iter]);
                        break;
                    }
                }
            }

            initSourceDestMotion(AnimationAccessor.getMotionX(o), startX, xCheck, destX, motionTypeX);
            initSourceDestMotion(AnimationAccessor.getMotionY(o), startY, yCheck, destY, motionTypeY);
            initSourceDestMotion(AnimationAccessor.getWidth(o), startWidth, widthCheck, destWidth, motionTypeWidth);
            initSourceDestMotion(AnimationAccessor.getHeight(o), startHeight, heightCheck, destHeight, motionTypeHeight);
            initSourceDestMotion(AnimationAccessor.getOrientation(o), startOrientation, orientationCheck, destOrientation, motionTypeOrientation);
            initSourceDestMotion(AnimationAccessor.getOpacity(o), startOpacity, opacityCheck, destOpacity, motionTypeOpacity);
        }
    }

    public void setStartTime(int t) {
        startTime.setValue(t);
    }

    public void updatePosition(int x, int y, boolean sourcePoint) {
        if(sourcePoint) {
            startX.setValue(x);
            startY.setValue(y);
        } else {
            destX.setValue(x);
            destY.setValue(y);
        }
        xCheck.setSelected(true);
        yCheck.setSelected(true);
        destX.setEnabled(true);
        destY.setEnabled(true);
    }

    private void initSourceDestMotion(Motion m, JSpinner start, JCheckBox check, JSpinner dest, JComboBox motionType) {
        if(m == null) {
            check.setSelected(false);
            start.setEnabled(false);
            dest.setEnabled(false);
            return;
        }
        start.setValue(m.getSourceValue());
        //if(m.getSourceValue() != m.getDestinationValue()) {
            check.setSelected(true);
            motionType.setEnabled(true);
            dest.setEnabled(true);
            start.setEnabled(true);
        //} else {
        //    check.setSelected(false);
        //}
        motionType.setSelectedIndex(AnimationAccessor.getMotionType(m) - 1);
        dest.setValue(m.getDestinationValue());
    }

    private int val(JSpinner s) {
        return ((Number)s.getValue()).intValue();
    }

    public AnimationObject getAnimationObject() {
        int x = ((Number)startX.getValue()).intValue();
        int y = ((Number)startY.getValue()).intValue();
        AnimationObject anim = AnimationObject.createAnimationImage(res.getImage((String) image.getSelectedItem()),
                x, y);
        anim.setStartTime(val(startTime));
        anim.setEndTime(val(duration) + val(startTime));
        if(xCheck.isSelected()) {
            anim.defineMotionX(motionTypeX.getSelectedIndex() + 1, val(startTime),
                    val(duration), val(startX), val(destX));
        }
        if(yCheck.isSelected()) {
            anim.defineMotionY(motionTypeY.getSelectedIndex() + 1, val(startTime),
                    val(duration), val(startY), val(destY));
        }
        if(widthCheck.isSelected()) {
            anim.defineWidth(motionTypeWidth.getSelectedIndex() + 1, val(startTime),
                    val(duration), val(startWidth), val(destWidth));
        }
        if(heightCheck.isSelected()) {
            anim.defineHeight(motionTypeHeight.getSelectedIndex() + 1, val(startTime),
                    val(duration), val(startHeight), val(destHeight));
        }
        if(opacityCheck.isSelected()) {
            anim.defineOpacity(motionTypeOpacity.getSelectedIndex() + 1, val(startTime),
                    val(duration), val(startOpacity), val(destOpacity));
        }
        if(orientationCheck.isSelected()) {
            anim.defineOrientation(motionTypeOrientation.getSelectedIndex() + 1, val(startTime),
                    val(duration), val(startOrientation), val(destOrientation));
        }
        if(val(frameDelay) > -1) {
            anim.defineFrames(val(frameWidth), val(frameHeight), val(frameDelay));
        }
        return anim;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        opacityLabel = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        startX = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        destX = new javax.swing.JSpinner();
        startY = new javax.swing.JSpinner();
        destY = new javax.swing.JSpinner();
        destWidth = new javax.swing.JSpinner();
        destHeight = new javax.swing.JSpinner();
        startOpacity = new javax.swing.JSpinner();
        destOpacity = new javax.swing.JSpinner();
        startOrientation = new javax.swing.JSpinner();
        destOrientation = new javax.swing.JSpinner();
        motionTypeX = new javax.swing.JComboBox();
        motionTypeY = new javax.swing.JComboBox();
        motionTypeWidth = new javax.swing.JComboBox();
        motionTypeHeight = new javax.swing.JComboBox();
        motionTypeOpacity = new javax.swing.JComboBox();
        motionTypeOrientation = new javax.swing.JComboBox();
        xCheck = new javax.swing.JCheckBox();
        yCheck = new javax.swing.JCheckBox();
        widthCheck = new javax.swing.JCheckBox();
        heightCheck = new javax.swing.JCheckBox();
        opacityCheck = new javax.swing.JCheckBox();
        orientationCheck = new javax.swing.JCheckBox();
        startWidth = new javax.swing.JSpinner();
        startHeight = new javax.swing.JSpinner();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        frameDelay = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        frameWidth = new javax.swing.JSpinner();
        jLabel14 = new javax.swing.JLabel();
        frameHeight = new javax.swing.JSpinner();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        duration = new javax.swing.JSpinner();
        startTime = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        image = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();

        FormListener formListener = new FormListener();

        jLabel6.setText("Start");
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel8.setText("X");
        jLabel8.setName("jLabel8"); // NOI18N

        jLabel9.setText("Y");
        jLabel9.setName("jLabel9"); // NOI18N

        jLabel10.setText("Width");
        jLabel10.setName("jLabel10"); // NOI18N

        jLabel11.setText("Height");
        jLabel11.setName("jLabel11"); // NOI18N

        opacityLabel.setText("Opacity (0-255)");
        opacityLabel.setName("opacityLabel"); // NOI18N

        jLabel13.setText("Orientation");
        jLabel13.setName("jLabel13"); // NOI18N

        startX.setName("startX"); // NOI18N

        jLabel7.setText("Destination");
        jLabel7.setName("jLabel7"); // NOI18N

        destX.setEnabled(false);
        destX.setName("destX"); // NOI18N

        startY.setName("startY"); // NOI18N

        destY.setEnabled(false);
        destY.setName("destY"); // NOI18N

        destWidth.setEnabled(false);
        destWidth.setName("destWidth"); // NOI18N

        destHeight.setEnabled(false);
        destHeight.setName("destHeight"); // NOI18N

        startOpacity.setEnabled(false);
        startOpacity.setName("startOpacity"); // NOI18N

        destOpacity.setEnabled(false);
        destOpacity.setName("destOpacity"); // NOI18N

        startOrientation.setEnabled(false);
        startOrientation.setName("startOrientation"); // NOI18N

        destOrientation.setEnabled(false);
        destOrientation.setName("destOrientation"); // NOI18N

        motionTypeX.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Linear", "Spline" }));
        motionTypeX.setEnabled(false);
        motionTypeX.setName("motionTypeX"); // NOI18N

        motionTypeY.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Linear", "Spline" }));
        motionTypeY.setEnabled(false);
        motionTypeY.setName("motionTypeY"); // NOI18N

        motionTypeWidth.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Linear", "Spline" }));
        motionTypeWidth.setEnabled(false);
        motionTypeWidth.setName("motionTypeWidth"); // NOI18N

        motionTypeHeight.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Linear", "Spline" }));
        motionTypeHeight.setEnabled(false);
        motionTypeHeight.setName("motionTypeHeight"); // NOI18N

        motionTypeOpacity.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Linear", "Spline" }));
        motionTypeOpacity.setEnabled(false);
        motionTypeOpacity.setName("motionTypeOpacity"); // NOI18N

        motionTypeOrientation.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Linear", "Spline" }));
        motionTypeOrientation.setEnabled(false);
        motionTypeOrientation.setName("motionTypeOrientation"); // NOI18N

        xCheck.setName("xCheck"); // NOI18N
        xCheck.addActionListener(formListener);

        yCheck.setName("yCheck"); // NOI18N
        yCheck.addActionListener(formListener);

        widthCheck.setName("widthCheck"); // NOI18N
        widthCheck.addActionListener(formListener);

        heightCheck.setName("heightCheck"); // NOI18N
        heightCheck.addActionListener(formListener);

        opacityCheck.setName("opacityCheck"); // NOI18N
        opacityCheck.addActionListener(formListener);

        orientationCheck.setName("orientationCheck"); // NOI18N
        orientationCheck.addActionListener(formListener);

        startWidth.setEnabled(false);
        startWidth.setName("startWidth"); // NOI18N

        startHeight.setEnabled(false);
        startHeight.setName("startHeight"); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Frames (Sprite Image Frames)"));
        jPanel1.setName("jPanel1"); // NOI18N

        jLabel3.setText("Delay (-1 disable)");
        jLabel3.setName("jLabel3"); // NOI18N

        frameDelay.setName("frameDelay"); // NOI18N
        frameDelay.addChangeListener(formListener);

        jLabel5.setText("Width");
        jLabel5.setName("jLabel5"); // NOI18N

        frameWidth.setEnabled(false);
        frameWidth.setName("frameWidth"); // NOI18N

        jLabel14.setText("Height");
        jLabel14.setName("jLabel14"); // NOI18N

        frameHeight.setEnabled(false);
        frameHeight.setName("frameHeight"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel14)
                    .add(jLabel5)
                    .add(jLabel3))
                .add(18, 18, 18)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(frameHeight, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                    .add(frameWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                    .add(frameDelay, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(frameDelay, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(frameWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel14)
                    .add(frameHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Details"));
        jPanel2.setName("jPanel2"); // NOI18N

        jLabel2.setText("Duration");
        jLabel2.setName("jLabel2"); // NOI18N

        duration.setName("duration"); // NOI18N

        startTime.setName("startTime"); // NOI18N

        jLabel1.setText("Start Time");
        jLabel1.setName("jLabel1"); // NOI18N

        image.setName("image"); // NOI18N

        jLabel4.setText("Image");
        jLabel4.setName("jLabel4"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                        .add(jLabel4)
                        .add(374, 374, 374))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel1)
                            .add(jLabel2))
                        .add(11, 11, 11)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(duration, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                            .add(image, 0, 269, Short.MAX_VALUE)
                            .add(startTime, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE))
                        .add(75, 75, 75)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(startTime, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(duration, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(image, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel13)
                            .add(jLabel8)
                            .add(jLabel9)
                            .add(opacityLabel)
                            .add(jLabel11)
                            .add(jLabel10))
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(11, 11, 11)
                                .add(jLabel6))
                            .add(layout.createSequentialGroup()
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(startOrientation, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                                    .add(startOpacity, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                                    .add(startX, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                                    .add(startWidth, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.TRAILING, startY, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                                    .add(startHeight, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE))))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel7)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                    .add(layout.createSequentialGroup()
                                        .add(xCheck)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(destX, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(layout.createSequentialGroup()
                                        .add(yCheck)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(destY, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(layout.createSequentialGroup()
                                        .add(widthCheck)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(destWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(layout.createSequentialGroup()
                                        .add(heightCheck)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(destHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(layout.createSequentialGroup()
                                        .add(opacityCheck)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(destOpacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(layout.createSequentialGroup()
                                        .add(orientationCheck)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(destOrientation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(motionTypeY, 0, 175, Short.MAX_VALUE)
                                    .add(motionTypeWidth, 0, 175, Short.MAX_VALUE)
                                    .add(motionTypeHeight, 0, 175, Short.MAX_VALUE)
                                    .add(motionTypeOpacity, 0, 175, Short.MAX_VALUE)
                                    .add(motionTypeOrientation, 0, 175, Short.MAX_VALUE)
                                    .add(org.jdesktop.layout.GroupLayout.TRAILING, motionTypeX, 0, 175, Short.MAX_VALUE))))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(7, 7, 7)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(jLabel7))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(jLabel8)
                                .add(destX, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(motionTypeX, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(xCheck))
                        .add(5, 5, 5)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(jLabel9)
                                .add(destY, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(motionTypeY, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(yCheck))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(jLabel10)
                                .add(destWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(motionTypeWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(widthCheck))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(layout.createSequentialGroup()
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(jLabel11)
                                    .add(destHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(motionTypeHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                    .add(opacityLabel)
                                    .add(destOpacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                    .add(motionTypeOpacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                            .add(layout.createSequentialGroup()
                                .add(heightCheck)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(opacityCheck)))
                        .add(3, 3, 3)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(jLabel13)
                                .add(destOrientation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(motionTypeOrientation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(orientationCheck)))
                    .add(layout.createSequentialGroup()
                        .add(startX, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(5, 5, 5)
                        .add(startY, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(startWidth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(startHeight, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(startOpacity, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(3, 3, 3)
                        .add(startOrientation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == xCheck) {
                AnimationObjectEditor.this.xCheckActionPerformed(evt);
            }
            else if (evt.getSource() == yCheck) {
                AnimationObjectEditor.this.yCheckActionPerformed(evt);
            }
            else if (evt.getSource() == widthCheck) {
                AnimationObjectEditor.this.widthCheckActionPerformed(evt);
            }
            else if (evt.getSource() == heightCheck) {
                AnimationObjectEditor.this.heightCheckActionPerformed(evt);
            }
            else if (evt.getSource() == opacityCheck) {
                AnimationObjectEditor.this.opacityCheckActionPerformed(evt);
            }
            else if (evt.getSource() == orientationCheck) {
                AnimationObjectEditor.this.orientationCheckActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == frameDelay) {
                AnimationObjectEditor.this.frameDelayStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void xCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xCheckActionPerformed
        motionTypeX.setEnabled(xCheck.isSelected());
        destX.setEnabled(xCheck.isSelected());
    }//GEN-LAST:event_xCheckActionPerformed

    private void yCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yCheckActionPerformed
        motionTypeY.setEnabled(yCheck.isSelected());
        destY.setEnabled(yCheck.isSelected());
    }//GEN-LAST:event_yCheckActionPerformed

    private void widthCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_widthCheckActionPerformed
        motionTypeWidth.setEnabled(widthCheck.isSelected());
        destWidth.setEnabled(widthCheck.isSelected());
        startWidth.setEnabled(widthCheck.isSelected());
    }//GEN-LAST:event_widthCheckActionPerformed

    private void heightCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_heightCheckActionPerformed
        motionTypeHeight.setEnabled(heightCheck.isSelected());
        destHeight.setEnabled(heightCheck.isSelected());
        startHeight.setEnabled(heightCheck.isSelected());
    }//GEN-LAST:event_heightCheckActionPerformed

    private void opacityCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_opacityCheckActionPerformed
        motionTypeOpacity.setEnabled(opacityCheck.isSelected());
        destOpacity.setEnabled(opacityCheck.isSelected());
        startOpacity.setEnabled(opacityCheck.isSelected());
    }//GEN-LAST:event_opacityCheckActionPerformed

    private void orientationCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_orientationCheckActionPerformed
        motionTypeOrientation.setEnabled(orientationCheck.isSelected());
        destOrientation.setEnabled(orientationCheck.isSelected());
        startOrientation.setEnabled(orientationCheck.isSelected());
    }//GEN-LAST:event_orientationCheckActionPerformed

    private void frameDelayStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_frameDelayStateChanged
        boolean e = (val(frameDelay) > -1);
        frameWidth.setEnabled(e);
        frameHeight.setEnabled(e);
    }//GEN-LAST:event_frameDelayStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner destHeight;
    private javax.swing.JSpinner destOpacity;
    private javax.swing.JSpinner destOrientation;
    private javax.swing.JSpinner destWidth;
    private javax.swing.JSpinner destX;
    private javax.swing.JSpinner destY;
    private javax.swing.JSpinner duration;
    private javax.swing.JSpinner frameDelay;
    private javax.swing.JSpinner frameHeight;
    private javax.swing.JSpinner frameWidth;
    private javax.swing.JCheckBox heightCheck;
    private javax.swing.JComboBox image;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
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
    private javax.swing.JComboBox motionTypeHeight;
    private javax.swing.JComboBox motionTypeOpacity;
    private javax.swing.JComboBox motionTypeOrientation;
    private javax.swing.JComboBox motionTypeWidth;
    private javax.swing.JComboBox motionTypeX;
    private javax.swing.JComboBox motionTypeY;
    private javax.swing.JCheckBox opacityCheck;
    private javax.swing.JLabel opacityLabel;
    private javax.swing.JCheckBox orientationCheck;
    private javax.swing.JSpinner startHeight;
    private javax.swing.JSpinner startOpacity;
    private javax.swing.JSpinner startOrientation;
    private javax.swing.JSpinner startTime;
    private javax.swing.JSpinner startWidth;
    private javax.swing.JSpinner startX;
    private javax.swing.JSpinner startY;
    private javax.swing.JCheckBox widthCheck;
    private javax.swing.JCheckBox xCheck;
    private javax.swing.JCheckBox yCheck;
    // End of variables declaration//GEN-END:variables

}
