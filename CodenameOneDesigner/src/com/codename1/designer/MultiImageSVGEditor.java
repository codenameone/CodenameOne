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
import java.awt.BorderLayout;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * Editor for editing SVG files in the resource file
 *
 * @author Shai Almog
 */
public class MultiImageSVGEditor extends javax.swing.JPanel {
    private EditableResources res;
    private String name;
    private LWUITImageRenderer renderer;

    /** Creates new form MultiImageSVGEditor */
    public MultiImageSVGEditor(EditableResources res, String name) {
        initComponents();
        this.res = res;
        this.name = name;
        imageName.setText(name);
        zoom.setModel(new SpinnerNumberModel(1, 0.5, 20, 0.5));
        Vector users = new Vector();
        ImageRGBEditor.findImageUse(name, users, res);
        DefaultListModel d = new DefaultListModel();
        for(Object o : users) {
            d.addElement(o);
        }
        componentList.setModel(d);
    }

    private void updateSVGImage() {
        com.codename1.ui.Image img = renderer.getImage();
        int count = 0;
        for(int iter = 0 ; iter < dpiTable.getRowCount() ; iter++) {
            Boolean b = (Boolean)dpiTable.getValueAt(iter, 1);
            if(b != null && b.booleanValue()) {
                if(dpiTable.getValueAt(iter, 3) != null && dpiTable.getValueAt(iter, 2) != null &&
                        ((Number)dpiTable.getValueAt(iter, 3)).intValue() > 0 &&
                        ((Number)dpiTable.getValueAt(iter, 2)).intValue() > 0) {
                    count++;
                }
            }
        }
        int[] dpis = new int[count];
        int[] widths = new int[count];
        int[] heights = new int[count];
        count = 0;
        for(int iter = 0 ; iter < dpiTable.getRowCount() ; iter++) {
            Boolean b = (Boolean)dpiTable.getValueAt(iter, 1);
            if(b != null && b.booleanValue()) {
                if(dpiTable.getValueAt(iter, 3) != null && dpiTable.getValueAt(iter, 2) != null &&
                        ((Number)dpiTable.getValueAt(iter, 3)).intValue() > 0 &&
                        ((Number)dpiTable.getValueAt(iter, 2)).intValue() > 0) {
                    dpis[count] = getRowDPI(iter);
                    if(dpiTable.getValueAt(iter, 2) != null && dpiTable.getValueAt(iter, 3) != null) {
                        widths[count] = ((Number)dpiTable.getValueAt(iter, 2)).intValue();
                        heights[count] = ((Number)dpiTable.getValueAt(iter, 3)).intValue();
                        if(widths[count] > 0 && heights[count] > 0) {
                            count++;
                        }
                    }
                }
            }
        }
        res.setSVGDPIs(name, dpis, widths, heights);
    }

    public com.codename1.ui.Image getImage() {
        com.codename1.ui.Image img = renderer.getImage();
        int count = 0;
        for(int iter = 0 ; iter < dpiTable.getRowCount() ; iter++) {
            Boolean b = (Boolean)dpiTable.getValueAt(iter, 1);
            if(b != null && b.booleanValue()) {
                if(dpiTable.getValueAt(iter, 3) != null && dpiTable.getValueAt(iter, 2) != null &&
                        ((Number)dpiTable.getValueAt(iter, 3)).intValue() > 0 &&
                        ((Number)dpiTable.getValueAt(iter, 2)).intValue() > 0) {
                    count++;
                }
            }
        }
        int[] dpis = new int[count];
        int[] widths = new int[count];
        int[] heights = new int[count];
        count = 0;
        for(int iter = 0 ; iter < dpiTable.getRowCount() ; iter++) {
            Boolean b = (Boolean)dpiTable.getValueAt(iter, 1);
            if(b != null && b.booleanValue()) {
                dpis[count] = getRowDPI(iter);
                if(dpiTable.getValueAt(iter, 2) != null && dpiTable.getValueAt(iter, 3) != null) {
                    widths[count] = ((Number)dpiTable.getValueAt(iter, 2)).intValue();
                    heights[count] = ((Number)dpiTable.getValueAt(iter, 3)).intValue();
                    if(widths[count] > 0 && heights[count] > 0) {
                        count++;
                    }
                }
            }
        }
        com.codename1.impl.javase.SVG s = (com.codename1.impl.javase.SVG)img.getSVGDocument();
        s.setDpis(dpis);
        s.setWidthForDPI(widths);
        s.setHeightForDPI(heights);

        return img;
    }

    private int getRowDPI(int row) {
        switch(row) {
            case 0:
                return com.codename1.ui.Display.DENSITY_VERY_LOW;
            case 1:
                return com.codename1.ui.Display.DENSITY_LOW;
            case 2:
                return com.codename1.ui.Display.DENSITY_MEDIUM;
            case 3:
                return com.codename1.ui.Display.DENSITY_HIGH;
            case 4:
                return com.codename1.ui.Display.DENSITY_VERY_HIGH;
            default:
                return com.codename1.ui.Display.DENSITY_HD;
        }
    }

    private int getDPIRow(int dpi) {
        switch(dpi) {
            case com.codename1.ui.Display.DENSITY_VERY_LOW:
                return 0;
            case com.codename1.ui.Display.DENSITY_LOW:
                return 1;
            case com.codename1.ui.Display.DENSITY_MEDIUM:
                return 2;
            case com.codename1.ui.Display.DENSITY_HIGH:
                return 3;
            case com.codename1.ui.Display.DENSITY_VERY_HIGH:
                return 4;
            default:
                return 5;
        }
    }

    public void setImage(com.codename1.ui.Image img) {
        com.codename1.impl.javase.SVG s = (com.codename1.impl.javase.SVG)img.getSVGDocument();
        if(s.getDpis() != null) {
            for(int iter = 0 ; iter < dpiTable.getRowCount() ; iter++) {
                dpiTable.setValueAt(Boolean.FALSE, iter, 1);
            }
            for(int iter = 0 ; iter < s.getDpis().length ; iter++) {
                int row = getDPIRow(s.getDpis()[iter]);
                dpiTable.setValueAt(Boolean.TRUE, row, 1);
                dpiTable.setValueAt(s.getWidthForDPI()[iter], row, 2);
                dpiTable.setValueAt(s.getHeightForDPI()[iter], row, 3);
            }
        }
        renderer = new LWUITImageRenderer(img);
        preview.removeAll();
        preview.add(BorderLayout.CENTER, renderer);
        preview.revalidate();
        dpiTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                updateSVGImage();
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pickSVG = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        preview = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        dpiTable = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        imageName = new javax.swing.JLabel();
        zoom = new javax.swing.JSpinner();
        jScrollPane2 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        componentList = new javax.swing.JList();

        FormListener formListener = new FormListener();

        pickSVG.setText("...");
        pickSVG.setName("pickSVG"); // NOI18N
        pickSVG.addActionListener(formListener);

        jLabel1.setText("SVG");
        jLabel1.setName("jLabel1"); // NOI18N

        preview.setName("preview"); // NOI18N
        preview.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        dpiTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Very Low (176x220)", new Boolean(false), null, null},
                {"Low (240x320)", new Boolean(false), null, null},
                {"Medium (320x480)", new Boolean(false), null, null},
                {"High (480x854)", new Boolean(false), null, null},
                {"Very High (1080x720)", null, null, null},
                {"HD (1920x1080)", null, null, null}
            },
            new String [] {
                "DPI Mode", "Generate", "Width Pixels", "Height Pixels"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Boolean.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        dpiTable.setName("dpiTable"); // NOI18N
        jScrollPane1.setViewportView(dpiTable);

        jLabel2.setText("Fallback Image Sizes");
        jLabel2.setName("jLabel2"); // NOI18N

        imageName.setText("jLabel3");
        imageName.setName("imageName"); // NOI18N

        zoom.setToolTipText("Zoom");
        zoom.setName("zoom"); // NOI18N
        zoom.addChangeListener(formListener);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      \rSVG's work for supported devices as expected, however many mobile devices do not support SVG properly or support it poorly causing \nissues with device fragmentation. To work around this issue the Codename One Designer can generae fallback images for the given device resolutions \nwhich will be used seamlessly by Codename One when SVG isn't supported by the platform.\n    </p>\n    <p>\n     The image generation is seamless once you mark a resolution and type in the desired image size e.g. for an SVG icon you might \nwant a Low resolution image to be 24x24 and a Medium resolution to be 32x32. You achieve this by checking the boxes next to Low \nand medium resolution and filling in 24 and 32 in the respective columns. On platforms where SVG isn't supported PNG's would load, \nthese PNG's will not have any animation properties that the original SVG's might have had.\n    </p>\r\n  </body>\r\n</html>\r\n"); // NOI18N
        help.setName("help"); // NOI18N
        jScrollPane2.setViewportView(help);

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        componentList.setName("componentList"); // NOI18N
        jScrollPane3.setViewportView(componentList);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 768, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(preview, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED))
                            .add(layout.createSequentialGroup()
                                .add(pickSVG)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jLabel1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(imageName)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(zoom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(64, 64, 64)))
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                                .add(jLabel2)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 364, Short.MAX_VALUE))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
                            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(pickSVG)
                    .add(jLabel1)
                    .add(jLabel2)
                    .add(imageName)
                    .add(zoom, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(preview, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 126, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 162, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == pickSVG) {
                MultiImageSVGEditor.this.pickSVGActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == zoom) {
                MultiImageSVGEditor.this.zoomStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void zoomStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zoomStateChanged
        renderer.scale(((Number)zoom.getValue()).doubleValue());
    }//GEN-LAST:event_zoomStateChanged

    private void pickSVGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pickSVGActionPerformed
        selectFile();
    }//GEN-LAST:event_pickSVGActionPerformed

    protected com.codename1.ui.Image createImage(byte[] data) throws IOException {
        return com.codename1.ui.Image.createSVG(null, false, data);
    }

    protected com.codename1.ui.Image createImage(File selection) throws IOException {
        byte[] data = new byte[(int) selection.length()];
        DataInputStream di = new DataInputStream(new FileInputStream(selection));
        di.readFully(data);
        di.close();
        com.codename1.ui.Image i = createImage(data);
        di.close();
        return i;
    }

    protected File[] createChooser() {
        return ResourceEditorView.showOpenFileChooser("SVG", ".svg");
    }

    public void selectFiles() {
        try {
            File[] selection = ResourceEditorView.showOpenFileChooser(true, "SVG", ".svg");
            if (selection == null) {
                return;
            }
            for(File s : selection) {
                res.setImage(s.getName(), createImage(s));
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "An error occured while trying to load the file:\n" + ex,
                "IO Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void selectFile() {
        try {
            File[] c = createChooser();
            if (c == null) {
                return;
            }
            File selection = c[0];
            if(renderer != null) {
                preview.removeAll();
            }
            renderer = new LWUITImageRenderer(createImage(selection));
            preview.add(BorderLayout.CENTER, renderer);
            res.setImage(name, renderer.getImage());
            preview.revalidate();
            preview.repaint();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "An error occured while trying to load the file:\n" + ex,
                "IO Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editImageActionPerformed(java.awt.event.ActionEvent evt) {
        selectFile();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList componentList;
    private javax.swing.JTable dpiTable;
    private javax.swing.JTextPane help;
    private javax.swing.JLabel imageName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JButton pickSVG;
    private javax.swing.JPanel preview;
    private javax.swing.JSpinner zoom;
    // End of variables declaration//GEN-END:variables

}
