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

import com.codename1.ui.EncodedImage;
import com.codename1.designer.ResourceEditorView;
import com.codename1.ui.resource.util.ImageTools;
import com.codename1.ui.util.EditableResources;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;

/**
 * UI for editing a multi-image type image
 *
 * @author Shai Almog
 */
public class ImageMultiEditor extends BaseForm {
    private EditableResources res;
    private String name;
    private static final int DPIS[] = {
            com.codename1.ui.Display.DENSITY_VERY_LOW,
            com.codename1.ui.Display.DENSITY_LOW,
            com.codename1.ui.Display.DENSITY_MEDIUM,
            com.codename1.ui.Display.DENSITY_HIGH,
            com.codename1.ui.Display.DENSITY_VERY_HIGH,
            com.codename1.ui.Display.DENSITY_HD
    };
    private EditableResources.MultiImage multi;
    private CodenameOneImageRenderer renderer;
    
    /** Creates new form ImageMultiEditor */
    public ImageMultiEditor(EditableResources res, String name, ResourceEditorView view) {
        if(res.isOverrideMode() && !res.isOverridenResource(name)) {
            setOverrideMode(true, view.getComponent());
        }
        initComponents();
        dpi.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if(index > -1) {
                    com.codename1.ui.Image i = getByDPI(multi, DPIS[index]);
                    if(i != null) {
                        value = "<html><body><b>" + ((String)value);
                    } else {
                        value = "<html><body><i>" + ((String)value);
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        this.res = res;
        this.name = name;
        imageName.setText(name);
        zoom.setModel(new SpinnerNumberModel(1, 0.5, 20, 0.5));
        if(res.getResourceObject(name) != null) {
            multi = (EditableResources.MultiImage)res.getResourceObject(name);

            Vector users = new Vector();
            ImageRGBEditor.findImageUse(name, users, res);
            DefaultListModel d = new DefaultListModel();
            for(Object o : users) {
                d.addElement(o);
            }
            componentList.setModel(d);

            for(int iter = 0 ; iter < DPIS.length ; iter++) {
                if(getByDPI(multi, DPIS[iter]) != null) {
                    dpi.setSelectedIndex(iter);
                    return;
                }
            }
        } else {
            multi = new EditableResources.MultiImage();
            multi.setDpi(new int[0]);
            multi.setInternalImages(new com.codename1.ui.EncodedImage[0]);
            res.setMultiImage(name, multi);
        }
    }

    private com.codename1.ui.EncodedImage getByDPI(EditableResources.MultiImage m, int dpi) {
        for(int iter = 0 ; iter < m.getDpi().length ; iter++) {
            if(m.getDpi()[iter] == dpi) {
                return m.getInternalImages()[iter];
            }
        }
        return null;
    }

    public void setImage(EditableResources.MultiImage m) {
        multi = m;
        com.codename1.ui.Image img = getByDPI(m, DPIS[dpi.getSelectedIndex()]);
        if(img == null) {
            preview.removeAll();
            preview.revalidate();
            preview.repaint();
            return;
        }
        renderer = new CodenameOneImageRenderer(img);
        renderer.scale(((Number)zoom.getValue()).doubleValue());
        if(img instanceof com.codename1.ui.EncodedImage) {
            int s = ((com.codename1.ui.EncodedImage)img).getImageData().length;
            imageSize.setText(img.getWidth() + "x" + img.getHeight() + " " + (s / 1024) + "kb (" + s + " bytes)");
        } else {
            imageSize.setText(img.getWidth() + "x" + img.getHeight());
        }
        preview.removeAll();
        preview.add(java.awt.BorderLayout.CENTER, renderer);
        preview.revalidate();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dpi = new javax.swing.JComboBox();
        jScrollPane2 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();
        jLabel1 = new javax.swing.JLabel();
        editImage = new javax.swing.JButton();
        type = new javax.swing.JLabel();
        imageName = new javax.swing.JLabel();
        zoom = new javax.swing.JSpinner();
        imageSize = new javax.swing.JLabel();
        preview = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        componentList = new javax.swing.JList();
        delete = new javax.swing.JButton();
        scale = new javax.swing.JButton();
        toJpeg = new javax.swing.JButton();
        editExternal = new javax.swing.JButton();

        FormListener formListener = new FormListener();

        dpi.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Very Low Density 176x220 And Smaller", "Low Density Up To 240x320", "Medium Density Up To (360x480/3.5 inch)", "Hi Density Up To (480x854/3.5-4inch)", "Very Hi Density Up To (1440x720/3.5-4.5inch)", "HD Up To 1920x1080" }));
        dpi.setName("dpi"); // NOI18N
        dpi.addActionListener(formListener);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      \rMulti images allow complex elements such as icons to be specified on a per DPI (dots per inch) basis with classifications to several general DPI\n groups. Normally DPI isn't specified in reolution but since some devices don't provide the means to query the actual DPI it is \n\"faked\" on those devices using the resolution.\n    </p>\r\n    <p>\n      Notice that multi-images aren't intended for use as a replacement for scaling/tiling, or anything of that kind since that would become prohibitively \nexpensive as the resource grows and would also be ineffective since phones tend to change their resolution during runtime e.g. \nby rotation or poping up some types of virtual keyboards.\n    </p>\n    <p>\n     The multi-image is very similar in spirit to CodenameOne's SVG support and is mostly targeted at designers who have raster images rather \nthan vector resources or those who would normally target devices without SVG. \n    </p>\n    <p>\n     Working with the multi-image is seamless and CodenameOne will replace the images on loading during runtime (this is 100% seamless to developers). \nUse the combo box at the top to select the proper DPI/resolution and create an icon that would look decent in that resolution. Keep in mind that \nyou do not need to support all resolutions and that CodenameOne will pick the \"best\" image it can if none is defined for a specific DPI. \nE.g. if an image isn't defined for the very low resolution CodenameOne will look to higher resolution resources until it finds an image. \n    </p>\n  </body>\r\n</html>\r\n"); // NOI18N
        help.setName("help"); // NOI18N
        jScrollPane2.setViewportView(help);

        jLabel1.setText("DPI");
        jLabel1.setName("jLabel1"); // NOI18N

        editImage.setText("...");
        editImage.setToolTipText("Pick Image");
        editImage.setName("editImage"); // NOI18N
        editImage.addActionListener(formListener);

        type.setText("Multi-Image");
        type.setName("type"); // NOI18N

        imageName.setText("Image Name");
        imageName.setName("imageName"); // NOI18N

        zoom.setToolTipText("Zoom");
        zoom.setName("zoom"); // NOI18N
        zoom.addChangeListener(formListener);

        imageSize.setText("width X height");
        imageSize.setName("imageSize"); // NOI18N

        preview.setName("preview"); // NOI18N
        preview.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        componentList.setName("componentList"); // NOI18N
        jScrollPane1.setViewportView(componentList);

        delete.setText("Delete");
        delete.setToolTipText("Delete this resolution");
        delete.setName("delete"); // NOI18N
        delete.addActionListener(formListener);

        scale.setText("Scale");
        scale.setName("scale"); // NOI18N
        scale.addActionListener(formListener);

        toJpeg.setText("To JPEG");
        toJpeg.setToolTipText("Convert Multi-Image to JPEG");
        toJpeg.setName("toJpeg"); // NOI18N
        toJpeg.addActionListener(formListener);

        editExternal.setText("Edit");
        editExternal.setToolTipText("Edit the image with an external tool");
        editExternal.setName("editExternal"); // NOI18N
        editExternal.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(dpi, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(editImage)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(editExternal)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(delete)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(scale)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(toJpeg)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(type)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(imageName)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(zoom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(imageSize))
                    .add(layout.createSequentialGroup()
                        .add(preview, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 518, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 604, Short.MAX_VALUE)))
                .addContainerGap())
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1176, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(dpi, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1)
                    .add(editImage)
                    .add(delete)
                    .add(type)
                    .add(imageName)
                    .add(zoom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(imageSize)
                    .add(scale)
                    .add(toJpeg)
                    .add(editExternal))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jScrollPane1)
                    .add(preview, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 310, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE))
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == dpi) {
                ImageMultiEditor.this.dpiActionPerformed(evt);
            }
            else if (evt.getSource() == editImage) {
                ImageMultiEditor.this.editImageActionPerformed(evt);
            }
            else if (evt.getSource() == delete) {
                ImageMultiEditor.this.deleteActionPerformed(evt);
            }
            else if (evt.getSource() == scale) {
                ImageMultiEditor.this.scaleActionPerformed(evt);
            }
            else if (evt.getSource() == toJpeg) {
                ImageMultiEditor.this.toJpegActionPerformed(evt);
            }
            else if (evt.getSource() == editExternal) {
                ImageMultiEditor.this.editExternalActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == zoom) {
                ImageMultiEditor.this.zoomStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void editImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editImageActionPerformed
        pickFile();
}//GEN-LAST:event_editImageActionPerformed

    private void zoomStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zoomStateChanged
        renderer.scale(((Number)zoom.getValue()).doubleValue());
}//GEN-LAST:event_zoomStateChanged

    private void deleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteActionPerformed
        remove();
    }//GEN-LAST:event_deleteActionPerformed

    private void dpiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dpiActionPerformed
        setImage(multi);
    }//GEN-LAST:event_dpiActionPerformed

    private void scaleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scaleActionPerformed
        if (multi.getDpi().length == 0 || getByDPI(multi, DPIS[dpi.getSelectedIndex()]) == null) {
            return;
        }
        ScaleMultiImage scaleMulti = new ScaleMultiImage(multi, DPIS[dpi.getSelectedIndex()]);
        int result = JOptionPane.showConfirmDialog(this, scaleMulti, "Scale", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        int toDPI = scaleMulti.getToDPI();
        int scaledWidth = scaleMulti.getDestWidth();
        int scaledHeight = scaleMulti.getDestHeight();
        int fromDPI = DPIS[dpi.getSelectedIndex()];

        EditableResources.MultiImage newImage = scaleMultiImage(fromDPI, toDPI, scaledWidth, scaledHeight, multi);
        if(newImage == null) {
            return;
        }
        res.setMultiImage(name, newImage);
        setImage(newImage);
    }//GEN-LAST:event_scaleActionPerformed

private void toJpegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toJpegActionPerformed
        int[] dpis = multi.getDpi();
        com.codename1.ui.EncodedImage[] images = new com.codename1.ui.EncodedImage[multi.getInternalImages().length];
        for(int iter = 0 ; iter < images.length ; iter++) {
            try {
                com.codename1.ui.Image sourceImage = multi.getInternalImages()[iter];
                BufferedImage buffer = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                buffer.setRGB(0, 0, sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getRGB(), 0, sourceImage.getWidth());
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ImageIO.write(buffer, "jpeg", bo);
                bo.close();
                images[iter] = EncodedImage.create(bo.toByteArray());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        EditableResources.MultiImage newImage = new EditableResources.MultiImage();
        newImage.setDpi(dpis);
        newImage.setInternalImages(images);
        res.setMultiImage(name, newImage);
        setImage(newImage);
}//GEN-LAST:event_toJpegActionPerformed

private void editExternalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editExternalActionPerformed
        try {
            final File tempImage = File.createTempFile("Image", ".png");
            tempImage.deleteOnExit();
            com.codename1.ui.EncodedImage img = getByDPI(multi, DPIS[dpi.getSelectedIndex()]);
            
            FileOutputStream os = new FileOutputStream(tempImage);
            os.write(img.getImageData());
            os.close();
            Desktop.getDesktop().edit(tempImage);
            new Thread() {
                public void run() {
                    long tstamp = tempImage.lastModified();
                    File f = ResourceEditorView.getLoadedFile();
                    while(f == ResourceEditorView.getLoadedFile() && tempImage.exists()) {
                        if(tstamp != tempImage.lastModified()) {
                            tstamp = tempImage.lastModified();
                            pickFile(tempImage);
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }.start();
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occured working with the file: " + ex,
                    "Error", JOptionPane.ERROR_MESSAGE);            
        }
}//GEN-LAST:event_editExternalActionPerformed

    public static EditableResources.MultiImage scaleMultiImage(int fromDPI, int toDPI, int scaledWidth, int scaledHeight, EditableResources.MultiImage multi) {
        try {
            int[] dpis = multi.getDpi();
            com.codename1.ui.EncodedImage[] imgs = multi.getInternalImages();
            int fromOffset = -1;
            int toOffset = -1;
            for (int iter = 0; iter < dpis.length; iter++) {
                if (dpis[iter] == fromDPI) {
                    fromOffset = iter;
                }
                if (dpis[iter] == toDPI) {
                    toOffset = iter;
                }
            }
            if (fromOffset == -1) {
                return null;
            }
            EditableResources.MultiImage newImage = new EditableResources.MultiImage();
            if (toOffset == -1) {
                com.codename1.ui.EncodedImage[] newImages = new com.codename1.ui.EncodedImage[imgs.length + 1];
                System.arraycopy(imgs, 0, newImages, 0, imgs.length);
                toOffset = imgs.length;
                int[] newDpis = new int[dpis.length + 1];
                System.arraycopy(dpis, 0, newDpis, 0, dpis.length);
                newDpis[toOffset] = toDPI;
                newImage.setDpi(newDpis);
                newImage.setInternalImages(newImages);
            } else {
                com.codename1.ui.EncodedImage[] newImages = new com.codename1.ui.EncodedImage[imgs.length];
                System.arraycopy(multi.getInternalImages(), 0, newImages, 0, imgs.length);
                newImage.setDpi(dpis);
                newImage.setInternalImages(newImages);
            }
            com.codename1.ui.Image sourceImage = newImage.getInternalImages()[fromOffset];
            BufferedImage buffer = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            buffer.setRGB(0, 0, sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getRGB(), 0, sourceImage.getWidth());
            sourceImage.getRGB();
            sourceImage.getWidth();
            BufferedImage scaled = ImageTools.getScaledInstance(buffer, scaledWidth, scaledHeight);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(scaled, "png", output);
            output.close();
            byte[] bytes = output.toByteArray();
            com.codename1.ui.EncodedImage encoded = com.codename1.ui.EncodedImage.create(bytes);
            newImage.getInternalImages()[toOffset] = encoded;
            return newImage;
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(JFrame.getFrames()[0], "An error occured working with the file: " + ex,
                    "Error", JOptionPane.ERROR_MESSAGE);            
        }
        return null;
    }

    private void remove() {
        preview.removeAll();
        preview.revalidate();
        preview.repaint();
        int dpi = DPIS[this.dpi.getSelectedIndex()];
        int[] dpis = multi.getDpi();
        com.codename1.ui.EncodedImage[] imgs = multi.getInternalImages();
        for(int iter = 0 ; iter < dpis.length ; iter++) {
            if(dpis[iter] == dpi) {
                com.codename1.ui.EncodedImage[] newImages = new com.codename1.ui.EncodedImage[imgs.length - 1];
                int[] newDpis = new int[dpis.length - 1];
                int originalOffset = 0;
                for(int x = 0 ; x < newImages.length ; x++) {
                    if(originalOffset == iter) {
                        originalOffset++;
                    }
                    newImages[x] = imgs[originalOffset];
                    newDpis[x] = dpis[originalOffset];
                    originalOffset++;
                }

                multi = new EditableResources.MultiImage();
                multi.setDpi(newDpis);
                multi.setInternalImages(newImages);
                res.setMultiImage(name, multi);
                return;
            }
        }
    }

    private void pickFile(File selection) {
        try {
            byte[] data = new byte[(int) selection.length()];
            DataInputStream di = new DataInputStream(new FileInputStream(selection));
            di.readFully(data);
            di.close();
            com.codename1.ui.EncodedImage i = com.codename1.ui.EncodedImage.create(data);
            int[] dpis = multi.getDpi();
            int dpi = DPIS[this.dpi.getSelectedIndex()];
            com.codename1.ui.EncodedImage[] imgs = multi.getInternalImages();
            for(int iter = 0 ; iter < dpis.length ; iter++) {
                if(dpis[iter] == dpi) {
                    com.codename1.ui.EncodedImage[] newImages = new com.codename1.ui.EncodedImage[imgs.length];
                    System.arraycopy(imgs, 0, newImages, 0, imgs.length);
                    newImages[iter] = i;
                    multi = new EditableResources.MultiImage();
                    multi.setDpi(dpis);
                    multi.setInternalImages(newImages);
                    res.setMultiImage(name, multi);
                    setImage(multi);
                    return;
                }
            }
            com.codename1.ui.EncodedImage[] newImages = new com.codename1.ui.EncodedImage[imgs.length + 1];
            System.arraycopy(imgs, 0, newImages, 0, imgs.length);
            newImages[imgs.length] = i;
            int[] newDpis = new int[dpis.length + 1];
            System.arraycopy(dpis, 0, newDpis, 0, dpis.length);
            newDpis[dpis.length] = dpi;
            multi = new EditableResources.MultiImage();
            multi.setDpi(newDpis);
            multi.setInternalImages(newImages);
            res.setMultiImage(name, multi);
            setImage(multi);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "An error occured while trying to load the file:\n" + ex,
                "IO Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void pickFile() {
        File[] files = ResourceEditorView.showOpenFileChooser("Images", ".gif", ".png", ".jpg");
        if (files == null) {
            return;
        }
        File selection = files[0];
        pickFile(selection);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList componentList;
    private javax.swing.JButton delete;
    private javax.swing.JComboBox dpi;
    private javax.swing.JButton editExternal;
    private javax.swing.JButton editImage;
    private javax.swing.JTextPane help;
    private javax.swing.JLabel imageName;
    private javax.swing.JLabel imageSize;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel preview;
    private javax.swing.JButton scale;
    private javax.swing.JButton toJpeg;
    private javax.swing.JLabel type;
    private javax.swing.JSpinner zoom;
    // End of variables declaration//GEN-END:variables

}
