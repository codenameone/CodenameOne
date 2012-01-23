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
import com.codename1.designer.ResourceEditorView;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.animations.AnimationObject;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.animations.Timeline;
import com.codename1.ui.util.EditableResources;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Shai
 */
public class PulsateEditor extends javax.swing.JPanel {
    private com.codename1.ui.Image currentImage;
    private com.codename1.ui.EncodedImage[] internalImages;
    private BufferedImage sourceImage;
    private com.codename1.ui.Label previewLabel = new com.codename1.ui.Label();
    private javax.swing.Timer timer;

    /** Creates new form PulsateEditor */
    public PulsateEditor() {
        initComponents();
        duration.setModel(new SpinnerNumberModel(1000, 100, 20000, 100));
        smallSize.setModel(new SpinnerNumberModel(50, 5, 200, 1));
        largeSize.setModel(new SpinnerNumberModel(150, 5, 300, 1));
        frames.setModel(new SpinnerNumberModel(6, 1, 20, 1));
        preview.add(java.awt.BorderLayout.CENTER, new CodenameOneComponentWrapper(previewLabel));
    }

    private int get(JSpinner s) {
        return ((Number)s.getValue()).intValue();
    }

    private void updateTimeline() {
        AnimationObject[] anim = new AnimationObject[get(frames) * 2 - 1];
        internalImages = new EncodedImage[get(frames)];
        int small = Math.min(get(smallSize), get(largeSize));
        int large = Math.max(get(smallSize), get(largeSize));
        int dur = get(duration);
        int segment = dur / anim.length;
        Motion calculator = Motion.createSplineMotion(small, large, dur / 2);
        float ratioOfLargest = ((float)large) / 100.0f;
        int timelineWidth = (int)(ratioOfLargest * ((float)sourceImage.getWidth()));
        int timelineHeight = (int)(ratioOfLargest * ((float)sourceImage.getHeight()));
        for(int iter = 0 ; iter < internalImages.length ; iter++) {
            calculator.setCurrentMotionTime(segment * iter);
            int percentAtTime = calculator.getValue();
            float ratioAtTime = ((float)percentAtTime) / 100.0f;
            int currentWidth = (int)(sourceImage.getWidth() * ratioAtTime);
            int currentHeight = (int)(sourceImage.getHeight() * ratioAtTime);
            internalImages[iter] = EncodedImage.create(scale(sourceImage, currentWidth, currentHeight));
            anim[iter] = AnimationObject.createAnimationImage(internalImages[iter], (timelineWidth - currentWidth) / 2,
                    (timelineHeight - currentHeight) / 2);
            anim[iter].setStartTime(segment * iter);
            anim[iter].setEndTime(segment * iter + segment);

            // peek at the next frame to calculate the diff
            calculator.setCurrentMotionTime(segment * iter + segment);
            int percentAtNextFrame = calculator.getValue();
            float ratioAtNextFrame = ((float)percentAtNextFrame) / 100.0f;
            int nextWidth = (int)(sourceImage.getWidth() * ratioAtNextFrame);
            int nextHeight = (int)(sourceImage.getHeight() * ratioAtNextFrame);
            anim[iter].defineHeight(AnimationObject.MOTION_TYPE_SPLINE, segment * iter, segment, currentHeight, nextHeight);
            anim[iter].defineWidth(AnimationObject.MOTION_TYPE_SPLINE, segment * iter, segment, currentWidth, nextWidth);
            anim[iter].defineMotionX(AnimationObject.MOTION_TYPE_SPLINE, segment * iter, segment,
                    (timelineWidth - currentWidth) / 2, (timelineWidth - nextWidth) / 2);
            anim[iter].defineMotionY(AnimationObject.MOTION_TYPE_SPLINE, segment * iter, segment,
                    (timelineHeight - currentHeight) / 2, (timelineHeight - nextHeight) / 2);

            // create the "inverse" shrinking animation object
            if(iter > 0) {
                int nextOffset = anim.length - iter;
                anim[nextOffset] = AnimationObject.createAnimationImage(internalImages[iter], (timelineWidth - currentWidth) / 2,
                        (timelineHeight - currentHeight) / 2);
                anim[nextOffset].setStartTime(segment * nextOffset);
                if(iter == 1) {
                    // this resolves any rounding errors that might have occured in the creation of the frames
                    anim[nextOffset].setEndTime(dur);
                } else {
                    anim[nextOffset].setEndTime(segment * nextOffset + segment);
                }

                // peek at the previous frame to calculate the diff
                calculator.setCurrentMotionTime(segment * iter - segment);
                int percentAtPreviousFrame = calculator.getValue();
                float ratioAtPreviousFrame = ((float)percentAtPreviousFrame) / 100.0f;
                int previousWidth = (int)(sourceImage.getWidth() * ratioAtPreviousFrame);
                int previousHeight = (int)(sourceImage.getHeight() * ratioAtPreviousFrame);
                anim[nextOffset].defineHeight(AnimationObject.MOTION_TYPE_SPLINE, segment * nextOffset, segment, currentHeight, previousHeight);
                anim[nextOffset].defineWidth(AnimationObject.MOTION_TYPE_SPLINE, segment * nextOffset, segment, currentWidth, previousWidth);
                anim[nextOffset].defineMotionX(AnimationObject.MOTION_TYPE_SPLINE, segment * nextOffset, segment,
                        (timelineWidth - currentWidth) / 2, (timelineWidth - previousWidth) / 2);
                anim[nextOffset].defineMotionY(AnimationObject.MOTION_TYPE_SPLINE, segment * nextOffset, segment,
                        (timelineHeight - currentHeight) / 2, (timelineHeight - previousHeight) / 2);
            }
        }
        currentImage = Timeline.createTimeline(dur, anim, new com.codename1.ui.geom.Dimension(timelineWidth, timelineHeight));
        previewLabel.setIcon(currentImage);
        previewLabel.repaint();
        preview.repaint();
    }

    private byte[] scale(BufferedImage bi, int w, int h) {
        try {
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(bi, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
            g2d.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", output);
            output.close();
            return output.toByteArray();
        } catch (IOException ex) {
            // will never happen since this is a byte array output stream
            ex.printStackTrace();
            return null;
        }
    }

    public void pulsateWizard(EditableResources res, JComponent parent) {
        File[] f = ResourceEditorView.showOpenFileChooser("Image", ".png", ".jpg", ".jpeg", ".gif");
        if(f != null && f.length > 0)  {
            try {
                timelineName.setText(f[0].getName());
                sourceImage = ImageIO.read(f[0]);
                updateTimeline();
                timer = new javax.swing.Timer(130, new java.awt.event.ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if(previewLabel.animate()) {
                            previewLabel.repaint();
                            preview.repaint();
                        }
                    }
                });
                timer.setRepeats(true);
                timer.setCoalesce(true);
                timer.start();
                int val = JOptionPane.showConfirmDialog(parent, this, "Edit Effect", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                timer.stop();
                if (val == JOptionPane.OK_OPTION) {
                    store(res, currentImage, timelineName.getText() + ": TL ");
                    for(EncodedImage img : internalImages) {
                        store(res, img, timelineName.getText() + ": Fr ");
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(parent, "Error in reading image file", "File Read Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void store(EditableResources res, com.codename1.ui.Image img, String name) {
        int it = 1;
        String n = name;
        while(res.containsResource(n)) {
            n = name + " " + it;
            it++;
        }
        res.setImage(n, img);
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
        duration = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        smallSize = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        largeSize = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        timelineName = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        frames = new javax.swing.JSpinner();
        jScrollPane1 = new javax.swing.JScrollPane();
        preview = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();

        FormListener formListener = new FormListener();

        jLabel1.setText("Duration (ms)");
        jLabel1.setName("jLabel1"); // NOI18N

        duration.setName("duration"); // NOI18N
        duration.addChangeListener(formListener);

        jLabel2.setText("Small Size %");
        jLabel2.setName("jLabel2"); // NOI18N

        smallSize.setName("smallSize"); // NOI18N
        smallSize.addChangeListener(formListener);

        jLabel3.setText("Large Size %");
        jLabel3.setName("jLabel3"); // NOI18N

        largeSize.setName("largeSize"); // NOI18N
        largeSize.addChangeListener(formListener);

        jLabel4.setText("Name");
        jLabel4.setName("jLabel4"); // NOI18N

        timelineName.setName("timelineName"); // NOI18N

        jLabel5.setText("Frames");
        jLabel5.setName("jLabel5"); // NOI18N

        frames.setName("frames"); // NOI18N
        frames.addChangeListener(formListener);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        preview.setName("preview"); // NOI18N
        preview.setLayout(new java.awt.BorderLayout());
        jScrollPane1.setViewportView(preview);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTextPane1.setContentType("text/html");
        jTextPane1.setEditable(false);
        jTextPane1.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      \rThis wizard allows generating a visual \"pulsating\" effect in which an image grows and shrinks in an animation. \n     This is very useful for splash screens, loading screens or to attrack the users attention to an application occurence.\n    </p>\n    <p>\n      An effect like pulsate can be very effective, however if software scaling alone is used to produce this effect the\nresults can be heavily and noticeably pixelated due to on device scaling deterioration. This wizard will perform scaling \nin the range defined on the PC and create multiple images (based on the number of frames defined) and switch between\nthem within the generated timeline animation. When more frames are added the resource file will be larger and memory\noverhead might increase, however the animation will be smoother. This is a tradeoff one must take when using this \nwizard.\n    </p>\r\n  </body>\r\n</html>\r\n"); // NOI18N
        jTextPane1.setName("jTextPane1"); // NOI18N
        jScrollPane2.setViewportView(jTextPane1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 387, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(jLabel4)
                        .add(42, 42, 42)
                        .add(timelineName, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel1)
                            .add(jLabel2)
                            .add(jLabel3)
                            .add(jLabel5))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(duration, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                            .add(smallSize, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                            .add(largeSize, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                            .add(frames, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE))))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 295, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 558, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel1)
                            .add(duration, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel2)
                            .add(smallSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel3)
                            .add(largeSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel5)
                            .add(frames, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(timelineName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel4))))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements javax.swing.event.ChangeListener {
        FormListener() {}
        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == duration) {
                PulsateEditor.this.durationStateChanged(evt);
            }
            else if (evt.getSource() == smallSize) {
                PulsateEditor.this.smallSizeStateChanged(evt);
            }
            else if (evt.getSource() == largeSize) {
                PulsateEditor.this.largeSizeStateChanged(evt);
            }
            else if (evt.getSource() == frames) {
                PulsateEditor.this.framesStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void durationStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_durationStateChanged
        updateTimeline();
    }//GEN-LAST:event_durationStateChanged

    private void smallSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_smallSizeStateChanged
        updateTimeline();
    }//GEN-LAST:event_smallSizeStateChanged

    private void largeSizeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_largeSizeStateChanged
        updateTimeline();
    }//GEN-LAST:event_largeSizeStateChanged

    private void framesStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_framesStateChanged
        updateTimeline();
    }//GEN-LAST:event_framesStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner duration;
    private javax.swing.JSpinner frames;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JSpinner largeSize;
    private javax.swing.JPanel preview;
    private javax.swing.JSpinner smallSize;
    private javax.swing.JTextField timelineName;
    // End of variables declaration//GEN-END:variables

}
