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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.JList;
import javax.swing.SpinnerNumberModel;
import com.codename1.ui.animations.Timeline;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.animations.AnimationObject;
import com.codename1.tools.resourcebuilder.AnimationImpl;
import com.codename1.ui.util.EditableResources;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Shai
 */
public class TimelineEditor extends BaseForm {
    private CodenameOneImageRenderer renderer;
    private EditableResources res;
    private String name;
    private javax.swing.Timer refreshTimeline;

    /** Creates new form TimelineEditor */
    public TimelineEditor(EditableResources res, String name, ResourceEditorView view) {
        if(res.isOverrideMode() && !res.isOverridenResource(name)) {
            setOverrideMode(true, view.getComponent());
        }
        initComponents();
        animationObjectList.setModel(new AnimationObjectTableModel());
        this.res = res;
        this.name = name;
        animationObjectList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int i = animationObjectList.getSelectedRow();
                removeAnimationObject.setEnabled(i > -1);
                duplicateObject.setEnabled(i > -1);
                int size = animationObjectList.getModel().getRowCount();
                moveUp.setEnabled(i > -1 && i > 0 && size > 1);
                moveDown.setEnabled(i > -1 && i < size - 1 && size > 1);
            }
        });
        animationObjectList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && animationObjectList.getSelectedRow() > -1) {
                    Timeline t = (Timeline)TimelineEditor.this.res.getImage(TimelineEditor.this.name);
                    updatePosition(-1, -1, t.getAnimation(animationObjectList.getSelectedRow()), false);
                }
                renderer.repaint();
            }
        });
        duration.setModel(new SpinnerNumberModel(20000, 1000, 1000000, 1000));
        width.setModel(new SpinnerNumberModel(240, 10, 1000, 10));
        height.setModel(new SpinnerNumberModel(320, 10, 1000, 10));
        com.codename1.ui.animations.Timeline current = (com.codename1.ui.animations.Timeline)res.getImage(name);
        if(current == null) {
            current = Timeline.createTimeline(3000, new AnimationObject[0], new com.codename1.ui.geom.Dimension(100, 100));
        }
        setImage(current);
        current.setPause(true);
    }

    public class AnimationObjectTableModel extends AbstractTableModel {
        private List<com.codename1.ui.animations.AnimationObject> anims = new ArrayList<com.codename1.ui.animations.AnimationObject>();
        private final String[] COLUMNS = {"Start Time", "Duration", "Image", "X", "Y", "Width", "Height", "Orientation", "Opacity"};

        public int indexOf(com.codename1.ui.animations.AnimationObject o) {
            return anims.indexOf(o);
        }

        public void setElementAt(com.codename1.ui.animations.AnimationObject o, int index) {
            anims.set(index, o);
            fireTableRowsUpdated(index, index);
        }

        public void addElement(com.codename1.ui.animations.AnimationObject o) {
            anims.add(o);
            fireTableRowsInserted(anims.size() - 1, anims.size() - 1);
        }

        public void remove(int i) {
            anims.remove(i);
            fireTableRowsDeleted(i, i);
        }

        public void updateTimeline(com.codename1.ui.animations.Timeline t) {
            anims.clear();
            for(int iter = 0 ; iter < t.getAnimationCount() ; iter++) {
                anims.add(t.getAnimation(iter));
            }
            fireTableDataChanged();
        }

        public int getRowCount() {
            return anims.size();
        }

        public com.codename1.ui.animations.AnimationObject getElementAt(int i) {
            return anims.get(i);
        }

        public int getColumnCount() {
            return COLUMNS.length;
        }

        public String getColumnName(int columnIndex) {
            return COLUMNS[columnIndex];
        }

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        private String motionToString(com.codename1.ui.animations.Motion m) {
            if(m == null) {
                return "";
            }
            if(m.getSourceValue() != m.getDestinationValue()) {
                return "" + m.getSourceValue() + " - " + m.getDestinationValue();
            }
            return "" + m.getSourceValue();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            com.codename1.ui.animations.AnimationObject o = anims.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return "" + o.getStartTime();
                case 1:
                    return "" + (o.getEndTime() - o.getStartTime());
                case 2:
                    if(AnimationAccessor.getImageName(o) != null) {
                        return AnimationAccessor.getImageName(o);
                    }
                    return res.findId(AnimationAccessor.getImage(o));
                case 3:
                    return motionToString(AnimationAccessor.getMotionX(o));
                case 4:
                    return motionToString(AnimationAccessor.getMotionY(o));
                case 5:
                    return motionToString(AnimationAccessor.getWidth(o));
                case 6:
                    return motionToString(AnimationAccessor.getHeight(o));
                case 7:
                    return motionToString(AnimationAccessor.getOrientation(o));
                case 8:
                    return motionToString(AnimationAccessor.getOpacity(o));
            }
            return null;
        }
    }

    /**
     * Selects a gif file using a file chooser and converts it to a timeline
     */
    public static void selectFile(ResourceEditorView view, EditableResources res, String timelineName) {
        File[] files = ResourceEditorView.showOpenFileChooser("Images", ".gif");
        if(files != null) {
            File sel = files[0];
            if(timelineName == null) {
                timelineName = sel.getName();
            }
            Preferences.userNodeForPackage(view.getClass()).put("lastDir", sel.getParentFile().getAbsolutePath());
            ImageReader iReader = ImageIO.getImageReadersBySuffix("gif").next();
            try {
                iReader.setInput(ImageIO.createImageInputStream(new FileInputStream(sel)));
                int frames = iReader.getNumImages(true);
                AnimationObject[] anims = new AnimationObject[frames];
                int currentTime = 0;
                for(int frameIter = 0 ; frameIter < frames ; frameIter++) {
                    BufferedImage currentImage = iReader.read(frameIter);

                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    ImageIO.write(currentImage, "png", bo);
                    bo.close();

                    // create a PNG image in the resource file
                    String label = sel.getName() + " frame:" + frameIter;
                    EncodedImage i = EncodedImage.create(bo.toByteArray());
                    res.setImage(label, i);

                    int duration = Math.max(40, AnimationImpl.getFrameTime(iReader, frameIter));
                    Point pos = AnimationImpl.getPixelOffsets(iReader, frameIter);
                    anims[frameIter] = AnimationObject.createAnimationImage(i, pos.x, pos.y);
                    anims[frameIter].setStartTime(currentTime);
                    anims[frameIter].setEndTime(100000000);
                    String disposeMethod = getDisposalMethod(iReader, frameIter);
                    if(disposeMethod != null) {
                        if("restoreToBackgroundColor".equals(disposeMethod)) {
                            if(frameIter + 1 < frames) {
                                int t = Math.max(40, AnimationImpl.getFrameTime(iReader, frameIter + 1));
                                anims[frameIter].setEndTime(currentTime + t);
                            } else {
                                anims[frameIter].setEndTime(currentTime + duration);
                            }
                            //for(int iter = frameIter ; iter >= 0 ; iter--) {
                            //    anims[iter].setEndTime(currentTime);
                            //}
                        }
                        // "none" |
                        // "doNotDispose" | "restoreToBackgroundColor" |
                        // "restoreToPrevious"
                    }
                    currentTime += duration;
                }
                Timeline t = Timeline.createTimeline(currentTime, anims, new com.codename1.ui.geom.Dimension(iReader.getWidth(0), iReader.getHeight(0)));
                res.setImage(timelineName, t);
                view.setSelectedResource(timelineName);
            } catch(IOException err) {
                err.printStackTrace();
                JOptionPane.showMessageDialog(JFrame.getFrames()[0], "Error reading file " + err, "IO Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static String getDisposalMethod(ImageReader reader, int num) throws IOException {
        IIOMetadata meta = reader.getImageMetadata(num);
        Node parent = meta.getAsTree("javax_imageio_gif_image_1.0");
        //printNodeTree(parent);
        NodeList root = parent.getChildNodes();
        for(int iter = 0 ; iter < root.getLength() ; iter++) {
            Node n = root.item(iter);
            if(n.getNodeName().equals("GraphicControlExtension")) {
                return n.getAttributes().getNamedItem("disposalMethod").getNodeValue();
            }
        }
        return null;
    }

    public void updatePosition(int x, int y, AnimationObject o, boolean startingPoint) {
        AnimationObjectTableModel dl = (AnimationObjectTableModel)animationObjectList.getModel();
        int index = -1;
        for(int iter = 0 ; iter < animationObjectList.getModel().getRowCount() ; iter++) {
            if(dl.getElementAt(iter) == o) {
                index = iter;
                break;
            }
        }
        Timeline t = (Timeline)TimelineEditor.this.res.getImage(TimelineEditor.this.name);
        AnimationObjectEditor editor = new AnimationObjectEditor(TimelineEditor.this.res, o, t.getDuration());
        if(x > -1) {
            editor.updatePosition(x, y, startingPoint);
        }
        int ok = JOptionPane.showConfirmDialog(TimelineEditor.this, editor, "Edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(ok == JOptionPane.OK_OPTION) {
            if(index < 0) {
                return;
            }
            dl.setElementAt(editor.getAnimationObject(), index);
            AnimationObject[] animations = new AnimationObject[t.getAnimationCount()];
            for(int iter = 0 ; iter < animations.length ; iter++) {
                animations[iter] = t.getAnimation(iter);
            }
            animations[index] = editor.getAnimationObject();
            boolean paused = t.isPause();
            int oldTime = t.getTime();
            t = Timeline.createTimeline(getValue(duration), animations,
                    new com.codename1.ui.geom.Dimension(getValue(width), getValue(height)));
            t.setPause(paused);
            t.setTime(oldTime);
            setImage(t);
        }
    }

    public void setImage(com.codename1.ui.animations.Timeline image) {
        animationPanel.removeAll();
        AnimationObjectTableModel model = (AnimationObjectTableModel)animationObjectList.getModel();
        model.updateTimeline(image);
        /*for(int iter = 0 ; iter < image.getAnimationCount() ; iter++) {
            model.addElement(image.getAnimation(iter));
        }*/
        renderer = new CodenameOneImageRenderer(image);
        renderer.setAnimationObjectList(animationObjectList, this);
        animationPanel.add(BorderLayout.CENTER, renderer);
        animationPanel.revalidate();
        if(image != res.getImage(name)) {
            res.setImage(name, image);
        }
        timeline.setMaximum(image.getDuration());
        duration.setValue(image.getDuration());
        endTime.setText("" + image.getDuration());
        width.setValue(image.getWidth());
        height.setValue(image.getHeight());
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        animationPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        propertiesTab = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        duration = new javax.swing.JSpinner();
        width = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        height = new javax.swing.JSpinner();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        animationObjectList = new javax.swing.JTable();
        addAnimationObject = new javax.swing.JButton();
        removeAnimationObject = new javax.swing.JButton();
        duplicateObject = new javax.swing.JButton();
        moveUp = new javax.swing.JButton();
        moveDown = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        help = new javax.swing.JTextPane();
        play = new javax.swing.JButton();
        pause = new javax.swing.JButton();
        timeline = new javax.swing.JSlider();
        currentTime = new javax.swing.JLabel();
        endTime = new javax.swing.JLabel();

        FormListener formListener = new FormListener();

        setName("Form"); // NOI18N
        setOpaque(false);

        jSplitPane1.setName("jSplitPane1"); // NOI18N

        animationPanel.setName("animationPanel"); // NOI18N
        animationPanel.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setLeftComponent(animationPanel);

        jPanel3.setName("jPanel3"); // NOI18N

        propertiesTab.setName("propertiesTab"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setOpaque(false);

        jLabel1.setText("Duration");
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText("Width");
        jLabel2.setName("jLabel2"); // NOI18N

        duration.setName("duration"); // NOI18N
        duration.addChangeListener(formListener);

        width.setName("width"); // NOI18N
        width.addChangeListener(formListener);

        jLabel4.setText("Height");
        jLabel4.setName("jLabel4"); // NOI18N

        height.setName("height"); // NOI18N
        height.addChangeListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jLabel2)
                    .add(jLabel4))
                .add(33, 33, 33)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(width, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
                    .add(duration, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
                    .add(height, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(duration, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(width, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(height, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(175, Short.MAX_VALUE))
        );

        propertiesTab.addTab("Timeline", jPanel1);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setOpaque(false);

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        animationObjectList.setName("animationObjectList"); // NOI18N
        jScrollPane2.setViewportView(animationObjectList);

        addAnimationObject.setText("Add");
        addAnimationObject.setName("addAnimationObject"); // NOI18N
        addAnimationObject.addActionListener(formListener);

        removeAnimationObject.setText("Remove");
        removeAnimationObject.setEnabled(false);
        removeAnimationObject.setName("removeAnimationObject"); // NOI18N
        removeAnimationObject.addActionListener(formListener);

        duplicateObject.setText("Duplicate");
        duplicateObject.setEnabled(false);
        duplicateObject.setName("duplicateObject"); // NOI18N
        duplicateObject.addActionListener(formListener);

        moveUp.setText("Move Up");
        moveUp.setEnabled(false);
        moveUp.setName("moveUp"); // NOI18N
        moveUp.addActionListener(formListener);

        moveDown.setText("Move Down");
        moveDown.setEnabled(false);
        moveDown.setName("moveDown"); // NOI18N
        moveDown.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(addAnimationObject)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(removeAnimationObject)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(duplicateObject)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(moveUp)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(moveDown)
                .addContainerGap(68, Short.MAX_VALUE))
            .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 547, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(new java.awt.Component[] {addAnimationObject, duplicateObject, moveDown, moveUp, removeAnimationObject}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(224, Short.MAX_VALUE)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(addAnimationObject)
                    .add(removeAnimationObject)
                    .add(duplicateObject)
                    .add(moveUp)
                    .add(moveDown))
                .addContainerGap())
            .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel2Layout.createSequentialGroup()
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                    .add(44, 44, 44)))
        );

        propertiesTab.addTab("Objects", jPanel2);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        help.setContentType("text/html");
        help.setEditable(false);
        help.setText("<html>\r\n  <head>\r\n\r\n  </head>\r\n  <body>\r\n    <p style=\"margin-top: 0\">\r\n      \rA timeline is a simple repeating animation that allows us to create portable visual effects based \non moving/manipulating images (referenced as objects in timeline terminology). A timeline isn't designed for creation of elaborate effects and is mostly designed for simple splash screen/progress indication animations.\n    </p>\n    <p>\nA timeline has a size and duration, while the duration is fixed the size is used to calculate the ratio \nto image sizes. When a timeline is scaled it will sclae the images within it as well and position them in\na scaled location (e.g. an image positioned in 50,50 for a 100,100 timeline will be in 160x160 when the timeline is scaled to 320x320 resolution).\n    </p>\r\n    <p>\nA timeline is manipuated by adding objects and defining their internal positions/behaviors during a portion of the timeline. \n    </p>\n  </body>\r\n</html>\r\n"); // NOI18N
        help.setName("help"); // NOI18N
        jScrollPane1.setViewportView(help);

        propertiesTab.addTab("Help", jScrollPane1);

        play.setText("Play");
        play.setName("play"); // NOI18N
        play.addActionListener(formListener);

        pause.setText("Pause");
        pause.setEnabled(false);
        pause.setName("pause"); // NOI18N
        pause.addActionListener(formListener);

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(play)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pause)
                .addContainerGap(422, Short.MAX_VALUE))
            .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(propertiesTab, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 552, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(297, Short.MAX_VALUE)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(play)
                    .add(pause))
                .addContainerGap())
            .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel3Layout.createSequentialGroup()
                    .add(propertiesTab, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                    .add(45, 45, 45)))
        );

        jSplitPane1.setRightComponent(jPanel3);

        timeline.setMaximum(20000);
        timeline.setValue(0);
        timeline.setName("timeline"); // NOI18N
        timeline.addChangeListener(formListener);

        currentTime.setText("0");
        currentTime.setName("currentTime"); // NOI18N

        endTime.setText("20000");
        endTime.setName("endTime"); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 559, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(currentTime)
                        .add(18, 18, 18)
                        .add(timeline, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 487, Short.MAX_VALUE)
                        .add(18, 18, 18)
                        .add(endTime)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(currentTime)
                    .add(endTime)
                    .add(timeline, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, javax.swing.event.ChangeListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == addAnimationObject) {
                TimelineEditor.this.addAnimationObjectActionPerformed(evt);
            }
            else if (evt.getSource() == removeAnimationObject) {
                TimelineEditor.this.removeAnimationObjectActionPerformed(evt);
            }
            else if (evt.getSource() == duplicateObject) {
                TimelineEditor.this.duplicateObjectActionPerformed(evt);
            }
            else if (evt.getSource() == moveUp) {
                TimelineEditor.this.moveUpActionPerformed(evt);
            }
            else if (evt.getSource() == moveDown) {
                TimelineEditor.this.moveDownActionPerformed(evt);
            }
            else if (evt.getSource() == play) {
                TimelineEditor.this.playActionPerformed(evt);
            }
            else if (evt.getSource() == pause) {
                TimelineEditor.this.pauseActionPerformed(evt);
            }
        }

        public void stateChanged(javax.swing.event.ChangeEvent evt) {
            if (evt.getSource() == duration) {
                TimelineEditor.this.durationStateChanged(evt);
            }
            else if (evt.getSource() == width) {
                TimelineEditor.this.widthStateChanged(evt);
            }
            else if (evt.getSource() == height) {
                TimelineEditor.this.heightStateChanged(evt);
            }
            else if (evt.getSource() == timeline) {
                TimelineEditor.this.timelineStateChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private Timeline cloneCurrentTimeline() {
        Timeline t = (Timeline)res.getImage(name);
        AnimationObject[] arr = new AnimationObject[t.getAnimationCount()];
        for(int iter = 0 ; iter < arr.length ; iter++) {
            arr[iter] = t.getAnimation(iter);
        }
        Timeline nt = Timeline.createTimeline(getValue(duration), arr,
                new com.codename1.ui.geom.Dimension(getValue(width), getValue(height)));
        nt.setPause(t.isPause());
        nt.setTime(t.getTime());
        return nt;
    }

    private void durationStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_durationStateChanged
        int d = getValue(duration);
        if(d != ((Timeline)renderer.getImage()).getDuration()) {
            com.codename1.ui.animations.Timeline t = cloneCurrentTimeline();
            endTime.setText("" + d);
            timeline.setMaximum(d);
            res.setImage(name, t);
            setImage(t);
        }
    }//GEN-LAST:event_durationStateChanged

    private void widthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_widthStateChanged
        int w = getValue(width);
        if(w != renderer.getImage().getWidth()) {
            com.codename1.ui.animations.Timeline t = cloneCurrentTimeline();
            renderer.getImage().scaled(w, renderer.getImage().getHeight());
            res.setImage(name, t);
            setImage(t);
            renderer.revalidate();
            renderer.repaint();
        }
    }//GEN-LAST:event_widthStateChanged

    private void heightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_heightStateChanged
        int h = getValue(height);
        if(h != renderer.getImage().getHeight()) {
            com.codename1.ui.animations.Timeline t = cloneCurrentTimeline();
            renderer.getImage().scaled(renderer.getImage().getWidth(), h);
            res.setImage(name, t);
            setImage(t);
            renderer.revalidate();
            renderer.repaint();
        }
    }//GEN-LAST:event_heightStateChanged

    private void timelineStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_timelineStateChanged
        currentTime.setText("" + timeline.getValue());
        if(!refreshingTimelineLock) {
            Timeline t = (Timeline)renderer.getImage();
            t.setPause(false);
            t.setTime(timeline.getValue());
            renderer.updateAnimation();
            t.setPause(true);
        }
    }//GEN-LAST:event_timelineStateChanged

    private boolean refreshingTimelineLock;
    private void playActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playActionPerformed
        final Timeline t = (Timeline)renderer.getImage();
        t.setPause(false);
        play.setEnabled(false);
        pause.setEnabled(true);
        refreshTimeline = new javax.swing.Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(t.isPause()) {
                    pause.setEnabled(false);
                    play.setEnabled(true);
                    refreshTimeline.stop();
                    refreshTimeline = null;
                    return;
                }
                refreshingTimelineLock = true;
                timeline.setValue(t.getTime());
                refreshingTimelineLock = false;
            }
        });
        refreshTimeline.setRepeats(true);
        refreshTimeline.start();
    }//GEN-LAST:event_playActionPerformed

    private void pauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseActionPerformed
        Timeline t = (Timeline)renderer.getImage();
        t.setPause(true);
        play.setEnabled(true);
        pause.setEnabled(false);
        if(refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
    }//GEN-LAST:event_pauseActionPerformed

    private int getValue(JSpinner s) {
        return ((Number)s.getValue()).intValue();
    }

    private void addAnimationObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAnimationObjectActionPerformed
        AnimationObjectEditor editor = new AnimationObjectEditor(res, null, getValue(duration));
        editor.setStartTime(timeline.getValue());
        int ok = JOptionPane.showConfirmDialog(this, editor, "Add", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(ok == JOptionPane.OK_OPTION) {
            ((AnimationObjectTableModel)animationObjectList.getModel()).addElement(editor.getAnimationObject());
            Timeline t = (Timeline)res.getImage(name);
            AnimationObject[] animations = new AnimationObject[t.getAnimationCount() + 1];
            for(int iter = 0 ; iter < animations.length - 1 ; iter++) {
                animations[iter] = t.getAnimation(iter);
            }
            animations[animations.length - 1] = editor.getAnimationObject();
            Timeline nt = Timeline.createTimeline(getValue(duration), animations,
                    new com.codename1.ui.geom.Dimension(getValue(width), getValue(height)));
            nt.setPause(t.isPause());
            nt.setTime(t.getTime());
            setImage(nt);
        }
    }//GEN-LAST:event_addAnimationObjectActionPerformed

    private void removeAnimationObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAnimationObjectActionPerformed
        Timeline t = (Timeline)res.getImage(name);
        AnimationObject[] animations = new AnimationObject[t.getAnimationCount() - 1];
        int offset = 0;
        for(int iter = 0 ; iter < t.getAnimationCount() ; iter++) {
            if(iter != animationObjectList.getSelectedRow()) {
                animations[offset] = t.getAnimation(iter);
                offset++;
            }
        }
        ((AnimationObjectTableModel)animationObjectList.getModel()).remove(animationObjectList.getSelectedRow());
        Timeline nt = Timeline.createTimeline(getValue(duration), animations,
                new com.codename1.ui.geom.Dimension(getValue(width), getValue(height)));
        nt.setPause(t.isPause());
        setImage(nt);
        animationObjectList.clearSelection();
        duplicateObject.setEnabled(false);
        moveDown.setEnabled(false);
        moveUp.setEnabled(false);
        removeAnimationObject.setEnabled(false);
    }//GEN-LAST:event_removeAnimationObjectActionPerformed

    private void duplicateObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_duplicateObjectActionPerformed
        Timeline t = cloneCurrentTimeline();
        AnimationObject o = AnimationAccessor.clone((AnimationObject)((AnimationObjectTableModel)animationObjectList.getModel()).getElementAt(animationObjectList.getSelectedRow()));
        ((AnimationObjectTableModel)animationObjectList.getModel()).addElement(o);
        AnimationObject[] animations = new AnimationObject[t.getAnimationCount() + 1];
        for(int iter = 0 ; iter < animations.length - 1 ; iter++) {
            animations[iter] = t.getAnimation(iter);
        }
        animations[animations.length - 1] = o;
        Timeline nt = Timeline.createTimeline(getValue(duration), animations,
                new com.codename1.ui.geom.Dimension(getValue(width), getValue(height)));
        nt.setPause(t.isPause());
        setImage(nt);
        animationObjectList.clearSelection();
        duplicateObject.setEnabled(false);
        moveDown.setEnabled(false);
        moveUp.setEnabled(false);
        removeAnimationObject.setEnabled(false);
    }//GEN-LAST:event_duplicateObjectActionPerformed

    private void moveUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpActionPerformed
        int i = animationObjectList.getSelectedRow();
        Timeline t = cloneCurrentTimeline();
        AnimationObject[] animations = new AnimationObject[t.getAnimationCount()];
        for(int iter = 0 ; iter < animations.length ; iter++) {
            animations[iter] = t.getAnimation(iter);
        }
        AnimationObject o = animations[i - 1];
        animations[i - 1] = animations[i];
        animations[i] = o;
        Timeline nt = Timeline.createTimeline(getValue(duration), animations,
                new com.codename1.ui.geom.Dimension(getValue(width), getValue(height)));
        nt.setPause(t.isPause());
        setImage(nt);
        moveUp.setEnabled(i > 1);
        moveDown.setEnabled(true);
        animationObjectList.getSelectionModel().setSelectionInterval(i - 1, i -1);
    }//GEN-LAST:event_moveUpActionPerformed

    private void moveDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownActionPerformed
        int i = animationObjectList.getSelectedRow();
        Timeline t = cloneCurrentTimeline();
        AnimationObject[] animations = new AnimationObject[t.getAnimationCount()];
        for(int iter = 0 ; iter < animations.length ; iter++) {
            animations[iter] = t.getAnimation(iter);
        }
        AnimationObject o = animations[i + 1];
        animations[i + 1] = animations[i];
        animations[i] = o;
        Timeline nt = Timeline.createTimeline(getValue(duration), animations,
                new com.codename1.ui.geom.Dimension(getValue(width), getValue(height)));
        nt.setPause(t.isPause());
        setImage(nt);
        moveDown.setEnabled(i < animations.length - 1);
        moveUp.setEnabled(true);
        animationObjectList.getSelectionModel().setSelectionInterval(i + 1, i + 1);
    }//GEN-LAST:event_moveDownActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addAnimationObject;
    private javax.swing.JTable animationObjectList;
    private javax.swing.JPanel animationPanel;
    private javax.swing.JLabel currentTime;
    private javax.swing.JButton duplicateObject;
    private javax.swing.JSpinner duration;
    private javax.swing.JLabel endTime;
    private javax.swing.JSpinner height;
    private javax.swing.JTextPane help;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JButton moveDown;
    private javax.swing.JButton moveUp;
    private javax.swing.JButton pause;
    private javax.swing.JButton play;
    private javax.swing.JTabbedPane propertiesTab;
    private javax.swing.JButton removeAnimationObject;
    private javax.swing.JSlider timeline;
    private javax.swing.JSpinner width;
    // End of variables declaration//GEN-END:variables

}

