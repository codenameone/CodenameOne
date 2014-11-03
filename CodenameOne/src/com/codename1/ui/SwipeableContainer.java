/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
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
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.ui;

import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import java.util.ArrayList;

/**
 * This Container enables to place Components below a Component and to swipe the
 * top Component to the right or to the left to expose the Components below.
 * 
 * @author Chen
 */
public class SwipeableContainer extends Container {

    private Container bottomLeftWrapper;
    private Container bottomRightWrapper;
    private Container topWrapper;

    private boolean open = false;
    private boolean openedToRight = false;
    private boolean openedToLeft = false;

    private Motion openCloseMotion;
    private boolean swipeActivated = true;
    private SwipeListener press, drag, release;
    private int initialX = -1;
    private int topX = -1;
    private boolean waitForRelease;

    /**
     * Simple Constructor
     * 
     * @param bottomLeft the Component below the top, this Component is exposed 
     * when dragging the top to the right
     * @param top the component on top.
     */
    public SwipeableContainer(Component bottomLeft, Component top) {
        this(bottomLeft, null, top);
    }

    /**
     * Simple Constructor
     * 
     * @param bottomLeft the Component below the top, this Component is exposed 
     * when dragging the top to the right
     * @param bottomRight the Component below the top, this Component is exposed 
     * when dragging the top to the Left
     * @param top the component on top.
     */ 
    public SwipeableContainer(Component bottomLeft, Component bottomRight, Component top) {
        setLayout(new LayeredLayout());
        bottomLeftWrapper = new Container(new BorderLayout());
        if(bottomLeft != null){
            bottomLeftWrapper.addComponent(BorderLayout.WEST, bottomLeft);
        }

        bottomRightWrapper = new Container(new BorderLayout());
        if(bottomRight != null){
            bottomRightWrapper.addComponent(BorderLayout.EAST, bottomRight);
        }

        topWrapper = new Container(new BorderLayout());
        topWrapper.addComponent(BorderLayout.CENTER, top);
        addComponent(bottomRightWrapper);
        addComponent(bottomLeftWrapper);
        addComponent(topWrapper);

        press = new SwipeListener(SwipeListener.PRESS);
        drag = new SwipeListener(SwipeListener.DRAG);
        release = new SwipeListener(SwipeListener.RELEASE);
    }

    /**
     * @inheritDoc
     */
    protected void deinitialize() {
        Form form = this.getComponentForm();
        if (form != null) {
            form.removePointerPressedListener(press);
            form.removePointerReleasedListener(release);
            form.removePointerDraggedListener(drag);
        }
        super.deinitialize();
    }

    /**
     * @inheritDoc
     */
    protected void initComponent() {
        super.initComponent();
        Form form = this.getComponentForm();
        if (form != null && swipeActivated) {
            form.addPointerPressedListener(press);
            form.addPointerReleasedListener(release);
            form.addPointerDraggedListener(drag);
        }
    }

    /**
     * This method will open the top Component to the right if there is a Component
     * to expose on the left.
     */ 
    public void openToRight() {
        if (open) {
            return;
        }
        if(bottomLeftWrapper.getComponentCount() == 0){
            return;
        }
        Component bottom = bottomLeftWrapper.getComponentAt(0);
        
        if(bottomRightWrapper.getComponentCount() > 0){
            bottomRightWrapper.setVisible(false);
        }
        
        int topX = topWrapper.getX();
        openCloseMotion = Motion.createSplineMotion(topX, bottom.getWidth(), 300);
        getComponentForm().registerAnimated(this);
        openCloseMotion.start();
        openedToRight = true;
        open = true;
    }

    /**
     * This method will open the top Component to the left if there is a Component
     * to expose on the right.
     */ 
    public void openToLeft() {
        if (open) {
            return;
        }
        if(bottomRightWrapper.getComponentCount() == 0){
            return;
        }
        Component bottom = bottomRightWrapper.getComponentAt(0);
        
        if(bottomLeftWrapper.getComponentCount() > 0){
            bottomLeftWrapper.setVisible(false);
        }
        
        int topX = topWrapper.getX();
        openCloseMotion = Motion.createSplineMotion(-topX, bottom.getWidth(), 300);
        getComponentForm().registerAnimated(this);
        openCloseMotion.start();
        openedToLeft = true;
        open = true;
    }

    /**
     * Close the top component if it is currently opened.
     */ 
    public void close() {
        if (!open) {
            return;
        }
        if (openedToRight) {
            int topX = topWrapper.getX();
            openCloseMotion = Motion.createSplineMotion(topX, 0, 300);
        } else {
            int topX = topWrapper.getX();
            openCloseMotion = Motion.createSplineMotion(-topX, 0, 300);
        }
        getComponentForm().registerAnimated(this);
        openCloseMotion.start();
        open = false;
    }

    @Override
    public Component getComponentAt(int x, int y) {
        if (!open) {
            return topWrapper.getComponentAt(x, y);
        } else {
            return super.getComponentAt(x, y);
        }
    }

    @Override
    public boolean animate() {
        if (openCloseMotion != null) {
            int val = openCloseMotion.getValue();
            if (openedToRight) {
                topWrapper.setX(val);
            } else {
                topWrapper.setX(-val);
            }

            repaint();
            boolean finished = openCloseMotion.isFinished();
            if (finished) {
                getComponentForm().deregisterAnimated(this);
                openCloseMotion = null;
                if(!open){
                    bottomRightWrapper.setVisible(true);
                    bottomLeftWrapper.setVisible(true);
                    openedToLeft = false;
                    openedToRight = false;
                }
            }
            return !finished;
        }
        return false;
    }

    /**
     * disable/enable dragging of the top Component
     */ 
    public void setSwipeActivated(boolean swipeActivated) {
        this.swipeActivated = swipeActivated;
    }

    /**
     * Returns true if swipe is activated
     */ 
    public boolean isSwipeActivated() {
        return swipeActivated;
    }

    /**
     * Returns true if the top Component is currently opened
     */ 
    public boolean isOpen() {
        return open && (openedToRight || openedToLeft);
    }

    class SwipeListener implements ActionListener {

        private final static int PRESS = 0;
        private final static int DRAG = 1;
        private final static int RELEASE = 2;
        private final int type;

        public SwipeListener(int type) {
            this.type = type;
        }

        public void actionPerformed(ActionEvent evt) {

            if (getComponentCount() == 0 || !swipeActivated || animate()) {
                return;
            }
            Component bottomL;
            int bottomLeftW = 0;
            if(bottomLeftWrapper.getComponentCount() > 0){
                bottomL = bottomLeftWrapper.getComponentAt(0);
                bottomLeftW = bottomL.getWidth();
            }

            Component bottomR;
            int bottomRightW = 0;
            int bottomRightX = topWrapper.getWidth();
            if(bottomRightWrapper.getComponentCount() > 0){
                bottomR = bottomRightWrapper.getComponentAt(0);
                bottomRightW = bottomR.getWidth();
                bottomRightX = bottomR.getX();            
            }

            final int x = evt.getX();
            final int y = evt.getY();
            switch (type) {
                case PRESS: {
                    if (!topWrapper.contains(x, y)) {
                        return;
                    }
                    topX = topWrapper.getX();
                    initialX = x;
                    waitForRelease = true;
                    break;
                }
                case DRAG: {
                    if (!topWrapper.contains(x, y)) {
                        return;
                    }
                    if (initialX != -1) {
                        int diff = x - initialX;
                        int val = 0;
                        if(!isOpen()){
                            val = Math.min(diff, bottomLeftW);
                            val = Math.max(val, -bottomRightW);
                        }
                        if(openedToRight){
                            val = Math.min(diff, 0);
                            val = Math.max(val, -bottomLeftW+1);
                        }else if(openedToLeft){
                            val = Math.max(diff, 0);
                            val = Math.min(val, bottomRightW-1);
                        }
                        
                        topWrapper.setX(topX + val);
                        if(topWrapper.getX()>0){
                            bottomRightWrapper.setVisible(false);
                            bottomLeftWrapper.setVisible(true);                            
                        }else{
                            bottomRightWrapper.setVisible(true);
                            bottomLeftWrapper.setVisible(false);                                                    
                        }
                        repaint();
                    }
                    break;
                }
                case RELEASE: {
                    if (waitForRelease) {
                        initialX = -1;
                        //if (!isOpen()) {
                            int topX = topWrapper.getX();
                            //it's opened to the right
                            if (topX > 0) {
                                if (topX > bottomLeftW / 2) {
                                    open = false;
                                    openToRight();
                                } else {
                                    open = true;
                                    close();
                                }
                            } else {
                                if (topX + topWrapper.getWidth() < bottomRightX + bottomRightW / 2) {
                                    open = false;
                                    openToLeft();
                                } else {
                                    open = true;
                                    close();
                                }

                            }
                        //}
                        waitForRelease = false;
                    }
                    break;
                }
            }
        }
    }

}
