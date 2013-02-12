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

import com.codename1.ui.animations.AnimationAccessor;
import com.codename1.ui.animations.Timeline;
import com.codename1.ui.animations.AnimationObject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * Renders an image from Codename One on a Swing canvas including support for CodenameOne animations
 *
 * @author Shai Almog
 */
public class CodenameOneImageRenderer extends JComponent implements MouseListener, MouseMotionListener {
    private com.codename1.ui.Image image;
    private BufferedImage[] buffer;
    private int currentFrame = 0;
    private static List<WeakReference<CodenameOneImageRenderer>> allAnimations = new ArrayList<WeakReference<CodenameOneImageRenderer>>();
    private JTable animationObjectList;

    // variables related to animations
    private long baseTime = System.currentTimeMillis();
    private int worldTime;
    private int nextFrame = 1;
    private int totalAnimationTime;
    private int[] times;
    private TimelineEditor editor;
    private AnimationObject dragging;
    private BufferedImage draggingImage;
    private int dragX, dragY;
    private boolean drawBorder;
    private double zoom = 1;
    
    // static timer to prevent a memory leak
    static {
        javax.swing.Timer t = new javax.swing.Timer(200, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for(WeakReference<CodenameOneImageRenderer> img : allAnimations) {
                    CodenameOneImageRenderer r = img.get();
                    if(r != null) {
                        r.updateFrameAnimation();
                    } else {
                        // remove empty animations and skip the rest to prevent iteration issues
                        allAnimations.remove(img);
                        return;
                    }
                }
            }
        });
        t.setDelay(200);
        t.start();
    }

    public void updateAnimation() {
        if(!((Timeline)image).isPause()) {
            image.animate();
            buffer[0].setRGB(0, 0, image.getWidth(), image.getHeight(), image.getRGB(), 0, image.getWidth());
            repaint();
        }
    }

    private void updateFrameAnimation() {
        if(image instanceof Timeline) {
            if(!((Timeline)image).isPause()) {
                worldTime = (int)(System.currentTimeMillis() - baseTime);
                if(worldTime > totalAnimationTime) {
                    worldTime = 0;
                    baseTime = System.currentTimeMillis();
                }
                ((Timeline)image).setTime(worldTime);
                image.animate();
                buffer[0].setRGB(0, 0, image.getWidth(), image.getHeight(), image.getRGB(), 0, image.getWidth());
                repaint();
            }
            return;
        }
        if(((nextFrame != 0) && (worldTime >= times[nextFrame])) || ((nextFrame == 0) && (worldTime >= totalAnimationTime))) {
            currentFrame++;
            nextFrame++;
            if(nextFrame >= times.length) {
                nextFrame = 0;
            }
            if(currentFrame >= times.length) {
                currentFrame = 0;
                worldTime = 0;
            }
            repaint();
        }
    }
    
    public CodenameOneImageRenderer(com.codename1.ui.Image image) {
        this.image = image;
        buffer = new BufferedImage[] {new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB)};
        buffer[0].setRGB(0, 0, image.getWidth(), image.getHeight(), image.getRGB(), 0, image.getWidth());
        if(image instanceof Timeline) {
            totalAnimationTime = ((Timeline)image).getDuration();
            allAnimations.add(new WeakReference<CodenameOneImageRenderer>(this));
        } 
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    /**
     * Used to map the timeline component to the animation object list
     */
    public void setAnimationObjectList(JTable animationObjectList, TimelineEditor editor) {
        if(this.animationObjectList == null) {
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        this.animationObjectList = animationObjectList;
        this.editor = editor;
    }

    public void scale(double zoom) {
        this.zoom = zoom;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(new Color(CheckerBoardColorCalibration.getColorA()));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(CheckerBoardColorCalibration.getColorB()));
        int width = getWidth();
        int height = getHeight();
        for (int x = 0; x < width; x += 8) {
            if (x % 16 == 0) {
                for (int y = 0; y < height; y += 16) {
                    g.fillRect(x, y, 8, 8);
                }
            } else {
                for (int y = 8; y < height; y += 16) {
                    g.fillRect(x, y, 8, 8);
                }
            }
        }
        if(drawBorder) {
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, image.getWidth() + 1, image.getHeight() + 1);
        }
        if(zoom != 1) {
            ((Graphics2D)g).scale(zoom, zoom);
        }
        if(animationObjectList != null) {
            if(((Timeline)image).isPause()) {
                int selectedRow = animationObjectList.getSelectedRow();
                AnimationObject sel = null;
                if(selectedRow > -1) {
                    sel = ((TimelineEditor.AnimationObjectTableModel)animationObjectList.getModel()).getElementAt(selectedRow);
                }
                if(dragging != null) {
                    if(draggingImage == null) {
                        draggingImage = new BufferedImage(AnimationAccessor.getWidthInt(dragging),
                                AnimationAccessor.getHeightInt(dragging), BufferedImage.TYPE_INT_ARGB);
                        draggingImage.setRGB(0, 0, draggingImage.getWidth(), draggingImage.getHeight(),
                                AnimationAccessor.getImageMethod(dragging).modifyAlpha((byte)110).getRGB(), 0, draggingImage.getWidth());
                    }
                } else {
                    draggingImage = null;
                }
                if(sel != null) {
                    Graphics2D g2d = (Graphics2D)g.create();
                    g2d.drawImage(buffer[currentFrame], 0, 0, this);
                    g2d.setColor(Color.BLUE);
                    g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawRect(AnimationAccessor.getX(sel), AnimationAccessor.getY(sel),
                            AnimationAccessor.getWidthInt(sel), AnimationAccessor.getHeightInt(sel));
                    if(draggingImage != null) {
                        g2d.drawImage(draggingImage, dragX, dragY, null);
                    }
                    g2d.dispose();
                    return;
                }
            }
        }
        g.drawImage(buffer[currentFrame], 0, 0, this);
        if(draggingImage != null) {
            g.drawImage(draggingImage, dragX, dragY, null);
        }
    }
    
    public static CodenameOneImageRenderer create(File file) throws IOException {
        InputStream i = new FileInputStream(file);
        CodenameOneImageRenderer r = new CodenameOneImageRenderer(com.codename1.ui.EncodedImage.create(i));
        i.close();
        return r;
    }
    
    public com.codename1.ui.Image getImage() {
        return image;
    }

    public void mouseClicked(MouseEvent e) {
        Timeline t = (Timeline)image;
        AnimationObject selection = t.getAnimationAt(e.getX(), e.getY());
        // editing
        if(selection == null) {
            animationObjectList.clearSelection();
        } else {
            if(e.getClickCount() == 2) {
                selectValue(selection);
                MouseListener[] ls = animationObjectList.getListeners(MouseListener.class);
                for(MouseListener l : ls) {
                    l.mouseClicked(e);
                }
            } else {
                // selection
                selectValue(selection);
            }
        }
        repaint();
    }

    private void selectValue(AnimationObject selection) {
        int row = ((TimelineEditor.AnimationObjectTableModel)animationObjectList.getModel()).indexOf(selection);
        animationObjectList.getSelectionModel().setSelectionInterval(row, row);
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        Timeline t = (Timeline)image;
        if(dragging != null && t.isPause()) {
            // if this is a right mouse button update the source position
            editor.updatePosition(e.getX(), e.getY(), dragging, BaseForm.isRightClick(e));
            dragging = null;
        }
        repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        Timeline t = (Timeline)image;
        if(t.isPause()) {
            if(dragging == null) {
                dragging = t.getAnimationAt(e.getX(), e.getY());
                if(dragging == null) {
                    return;
                }
            }
            dragX = e.getX();
            dragY = e.getY();
            repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

    /**
     * @return the drawBorder
     */
    public boolean isDrawBorder() {
        return drawBorder;
    }

    /**
     * @param drawBorder the drawBorder to set
     */
    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
    }
}
