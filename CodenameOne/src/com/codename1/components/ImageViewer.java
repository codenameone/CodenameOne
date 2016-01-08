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
package com.codename1.components;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.Style;

/**
 * An image viewer component that allows zooming into an image and potentially flicking between multiple images
 *
 * @author Shai Almog
 */
public class ImageViewer extends Component {
    private float zoom = 1;
    private float currentZoom = 1;
    private static final int MIN_ZOOM = 1;
    private static final int MAX_ZOOM = 10;
    private Image image;
    private int imageX, imageY, imageDrawWidth, imageDrawHeight;
    private float panPositionX = 0.5f;
    private float panPositionY = 0.5f;
    private int pressX, pressY;
    private ListModel<Image> swipeableImages;
    private DataChangedListener listListener;
    private Image swipePlaceholder;
    private float swipeThreshold = 0.4f;
    private int imageInitialPosition = IMAGE_FIT;

    /**
     * Indicates the initial position of the image in the viewer to FIT to the 
     * component size
     */ 
    public final static int IMAGE_FIT = 0;
    /**
     * Indicates the initial position of the image in the viewer to FILL the 
     * component size.
     * Notice this type might drop edges of the images in order to stretch the image
     * to the full size of the Component.
     */ 
    public final static int IMAGE_FILL = 1;
    
    // return values from image aspect calc
    private int prefX, prefY, prefW, prefH;

    private boolean eagerLock = true;
    private boolean selectLock;
    private boolean cycleLeft = true;
    private boolean cycleRight = true;
    
    /**
     * Default constructor
     */
    public ImageViewer() {
        setFocusable(true);
        setUIID("ImageViewer");
    }

    /**
     * @inheritDoc
     */
    protected void resetFocusable() {
        setFocusable(true);
    }
    
    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"eagerLock", "image", "imageList", "swipePlaceholder"};
    }

    /**
     * @inheritDoc
     */
    protected boolean shouldBlockSideSwipe() {
        return true;
    }
    
    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {Boolean.class, Image.class, 
           com.codename1.impl.CodenameOneImplementation.getImageArrayClass(), Image.class};
    }
    
    /**
     * @inheritDoc
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"Boolean", "Image", "Image[]", "Image"};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("eagerLock")) {
            if(isEagerLock()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("image")) {
            return getImage();
        }
        if(name.equals("imageList")) {
            if(getImageList() == null) {
                return null;
            }
            Image[] a = new Image[getImageList().getSize()];
            int alen = a.length;
            for(int iter = 0 ; iter < alen ; iter++) {
                a[iter] = getImageList().getItemAt(iter);
            }
            return a;
        }
        if(name.equals("swipePlaceholder")) {
            return getSwipePlaceholder();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("eagerLock")) {
            setEagerLock(value != null && ((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("image")) {
            setImage((Image)value);
            return null;
        }
        if(name.equals("imageList")) {
            if(value == null) {
                setImageList(null);
            } else {
                setImageList(new DefaultListModel<Image>((Image[])value));
            }
            return null;
        }
        if(name.equals("swipePlaceholder")) {
            setSwipePlaceholder((Image)value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public void initComponent() {
        super.initComponent();
        if(image == null) {
            // gui builder?
            image = Image.createImage(50, 50, 0);
        } else {
            image.lock();
        }
        if(image.isAnimation()) {
            getComponentForm().registerAnimated(this);
        }
        eagerLock();
    }
    
    private void eagerLock() {
        if(eagerLock) {
            if(swipeableImages != null && swipeableImages.getSize() > 1) {
                Image left = getImageLeft();
                if(swipePlaceholder != null) {
                    left.asyncLock(swipePlaceholder);
                } else {
                    left.lock();
                }            
                if(swipeableImages.getSize() > 2) {
                    Image right = getImageRight();
                    if(swipePlaceholder != null) {
                        right.asyncLock(swipePlaceholder);
                    } else {
                        right.lock();
                    }            
                }
            }
        }        
    }
    
    private void eagerUnlock() {
        if(eagerLock) {
            if(swipeableImages != null && swipeableImages.getSize() > 1) {
                getImageLeft().unlock();
                getImageRight().unlock();
            }
        }        
    }

    /**
     * Returns the x position of the image viewport which can be useful when it is being panned by the user
     * @return x position within the image for the top left corner
     */
    public int getImageX() {
        return imageX;
    }
    
    /**
     * Returns the y position of the image viewport which can be useful when it is being panned by the user
     * @return y position within the image for the top left corner
     */
    public int getImageY() {
        return imageY;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void deinitialize() {
        super.deinitialize();
        image.unlock();
        eagerUnlock();
    }

    /**
     * Initializes the component with an image
     * @param i image to show
     */
    public ImageViewer(Image i) {
        this();
        setImage(i);
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public void keyReleased(int key) {
        if(swipeableImages != null) {
            int gk = Display.getInstance().getGameAction(key);
            if((gk == Display.GAME_LEFT || gk == Display.GAME_UP) && (cycleLeft || swipeableImages.getSelectedIndex() > getImageLeftPos())) {
                new AnimatePanX(-1, getImageLeft(), getImageLeftPos());
                return;
            }
            if(gk == Display.GAME_RIGHT || gk == Display.GAME_RIGHT && (cycleRight || swipeableImages.getSelectedIndex() < getImageRightPos())) {
                new AnimatePanX(2, getImageRight(), getImageRightPos());
                return;
            }
        }
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public void pointerPressed(int x, int y) {
        pressX = x;
        pressY = y;
        currentZoom = zoom;
    }

    private Image getImageRight() {
        return swipeableImages.getItemAt(getImageRightPos());
    }

    private int getImageRightPos() {
        return (swipeableImages.getSelectedIndex() + 1) % swipeableImages.getSize();
    }
    
    private int getImageLeftPos() {
        int pos = swipeableImages.getSelectedIndex() - 1;
        if(pos < 0) {
            return swipeableImages.getSize() - 1;
        }
        return pos;
    }

    private Image getImageLeft() {
        return swipeableImages.getItemAt(getImageLeftPos());
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if(panPositionX > 1) {
            if(panPositionX >= 1 + swipeThreshold && (cycleRight || swipeableImages.getSelectedIndex() < getImageRightPos())) {
                new AnimatePanX(2, getImageRight(), getImageRightPos());
            } else {
                // animate back
                new AnimatePanX(1, null, 0);
            }
            return;
        } 
        if(panPositionX < 0) {
            if(panPositionX <= swipeThreshold * -1 && (cycleLeft || swipeableImages.getSelectedIndex() > getImageLeftPos())) {
                new AnimatePanX(-1, getImageLeft(), getImageLeftPos());
            } else {
                // animate back
                new AnimatePanX(0, null, 0);
            }
            return;
        }
    }
    
    
    /**
     * @inheritDoc
     */
    @Override
    public void pointerDragged(int x, int y) {
        // could be a pan
        float distanceX = ((float)pressX - x) / getZoom();
        float distanceY = ((float)pressY - y) / getZoom();

        // convert to a number between 0 - 1
        distanceX /= ((float)getWidth());
        distanceY /= ((float)getHeight());

        // panning or swiping
        if(getZoom() > 1) {
            if(swipeableImages != null && swipeableImages.getSize() > 1) {
                // this has the potential of being a pan operation... 
                if(panPositionX < 0 || panPositionX == 0 && distanceX < 0) {
                    panPositionX = ((float)pressX - x) / ((float)getWidth());
                    repaint();
                    return;
                } else {
                    if(panPositionX > 1 || panPositionX == 1 && distanceX > 0) {
                        panPositionX = 1 + ((float)pressX - x) / ((float)getWidth());
                        repaint();
                        return;
                    }
                }
            } 
            pressX = x;
            pressY = y;
            panPositionX = panPositionX + distanceX * getZoom();
            panPositionX = Math.min(1, Math.max(0, panPositionX));
            panPositionY = Math.min(1, Math.max(0, panPositionY + distanceY * getZoom()));
            
            updatePositions();
            repaint();
        } else {
            if(swipeableImages != null && swipeableImages.getSize() > 1) {
                panPositionX = distanceX;
                
                // this has the potential of being a pan operation... 
                if(panPositionX < 0) {
                    repaint();
                    return;
                } else {
                    if(panPositionX > 0) {
                        panPositionX += 1;
                        repaint();
                        return;
                    }
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void laidOut() {
        super.laidOut();
        updatePositions();
    }
    
    /**
     * @inheritDoc
     */
    @Override
    protected boolean pinch(float scale) {
        zoom = currentZoom * scale;
        if(zoom < MIN_ZOOM) {
            zoom = MIN_ZOOM;
        } else {
            if(zoom > MAX_ZOOM) {
                zoom = MAX_ZOOM;
            }            
        }
        updatePositions();
        repaint();
        return true;
    }
    
    private void imageAspectCalc(Image img) {
        if(img == null) {
            return;
        }
        int iW = img.getWidth();
        int iH = img.getHeight();
        Style s = getStyle();
        int width = getWidth() - s.getPadding(LEFT) - s.getPadding(RIGHT);
        int height = getHeight() - s.getPadding(TOP) - s.getPadding(BOTTOM);
        float r2; 
        if(imageInitialPosition == IMAGE_FIT){
            r2 = Math.min(((float)width) / ((float)iW), ((float)height) / ((float)iH));
        }else{
            r2 = Math.max(((float)width) / ((float)iW), ((float)height) / ((float)iH));        
        }
        
        // calculate the image position to fit
        prefW = (int)(((float)iW) * r2);
        prefH = (int)(((float)iH) * r2);
        prefX = s.getPadding(LEFT) + (width - prefW) / 2;
        prefY = s.getPadding(TOP) + (height - prefH) / 2;
    }
    
    private void updatePositions() {
        if(zoom == 1) {
            imageAspectCalc(image);
            imageDrawWidth = prefW;
            imageDrawHeight = prefH;
            imageX = prefX;
            imageY = prefY;
            return;
        } 
        int iW = image.getWidth();
        int iH = image.getHeight();
        Style s = getStyle();
        int width = getWidth() - s.getPadding(LEFT) - s.getPadding(RIGHT);
        int height = getHeight() - s.getPadding(TOP) - s.getPadding(BOTTOM);
        float r2;
        if(imageInitialPosition == IMAGE_FIT){
            r2 = Math.min(((float)width) / ((float)iW), ((float)height) / ((float)iH));
        }else{
            r2 = Math.max(((float)width) / ((float)iW), ((float)height) / ((float)iH));        
        }
        imageDrawWidth = (int)(((float)iW) * r2 * zoom);
        imageDrawHeight = (int)(((float)iH) * r2 * zoom);
        imageX = (int)(s.getPadding(LEFT) + (width - imageDrawWidth) * panPositionX);
        imageY = (int)(s.getPadding(TOP) + (height - imageDrawHeight) * panPositionY);
    }
    
    /**
     * @inheritDoc
     */
    @Override    
    protected Dimension calcPreferredSize() {
        if(image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        return new Dimension(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean animate() {
        boolean result = false;
        if(image != null && image.isAnimation()) {
            result = image.animate();
        }
        return super.animate() || result; 
    }
    
    
    
    /**
     * @inheritDoc
     */
    @Override
    public void paint(Graphics g) {
        if(panPositionX < 0) {
            Style s = getStyle();
            int width = getWidth() - s.getPadding(LEFT) - s.getPadding(RIGHT);
            float ratio = ((float)width) * (panPositionX * -1);
            g.drawImage(image, ((int)ratio) + getX() + imageX, getY() + imageY, imageDrawWidth, imageDrawHeight);
            if (cycleLeft || swipeableImages.getSelectedIndex() > getImageLeftPos()) {
                Image left = getImageLeft();
                if(swipePlaceholder != null) {
                    left.asyncLock(swipePlaceholder);
                } else {
                    left.lock();
                }
                ratio = ratio - width;
                imageAspectCalc(left);
                g.drawImage(left, ((int)ratio) + getX() + prefX, getY() + prefY, prefW, prefH);            
            }
            return;
        }
        if(panPositionX > 1) {
            Style s = getStyle();
            int width = getWidth() - s.getPadding(LEFT) - s.getPadding(RIGHT);
            float ratio = ((float)width) * (1 - panPositionX);
            g.drawImage(image, ((int)ratio) + getX() + imageX, getY() + imageY, imageDrawWidth, imageDrawHeight);
            if (cycleRight || swipeableImages.getSelectedIndex() < getImageRightPos()) {
                Image right = getImageRight();
                if(swipePlaceholder != null) {
                    right.asyncLock(swipePlaceholder);
                } else {
                    right.lock();
                }
                ratio = ratio + width;
                imageAspectCalc(right);
                g.drawImage(right, ((int)ratio) + getX() + prefX, getY() + prefY, prefW, prefH);
            }
            return;
        }
        // can happen in the GUI builder
        if(image != null) {
            g.drawImage(image, getX() + imageX, getY() + imageY, imageDrawWidth, imageDrawHeight);
        }
    }
    
    /**
     * @inheritDoc
     */
    @Override
    protected void paintBackground(Graphics g) {
        // disable background painting for performance when zooming
        if(imageDrawWidth < getWidth() || imageDrawHeight < getHeight()) {
            super.paintBackground(g);
        }
    }

    /**
     * Returns the currently showing image
     * @return the image
     */
    public Image getImage() {
        return image;
    }

    /**
     * Sets the currently showing image
     * @param image the image to set
     */
    public void setImage(Image image) {
        if(this.image != image) {
            panPositionX = 0.5f;
            panPositionY = 0.5f;
            zoom = MIN_ZOOM;
            this.image = image;
            updatePositions();
            repaint();
            if(image.isAnimation()) {
                Form f = getComponentForm();
                if(f != null) {
                    f.registerAnimated(this);
                }
            }
        }
    }
    
    /**
     * By providing this optional list of images you can allows swiping between multiple images
     * 
     * @param list a list of images
     */
    public void setImageList(ListModel<Image> model) {
        if(model == null || model.getSize() == 0) {
            return;
        }
        if(image == null) {
            image = model.getItemAt(0);
        }
        if(swipeableImages != null) {
            swipeableImages.removeDataChangedListener(listListener);
            swipeableImages.removeSelectionListener((SelectionListener)listListener);
            model.addDataChangedListener(listListener);
            model.addSelectionListener((SelectionListener)listListener);
        } else {
            class Listener implements SelectionListener, DataChangedListener {
                public void selectionChanged(int oldSelected, int newSelected) {
                    if(selectLock) {
                        return;
                    }
                    if(swipeableImages.getSize() > 0 && newSelected > -1 && newSelected < swipeableImages.getSize()) {
                        setImage(swipeableImages.getItemAt(newSelected));
                    }
                }

                public void dataChanged(int type, int index) {
                    if(swipeableImages.getSize() > 0 && swipeableImages.getSelectedIndex() > -1 && swipeableImages.getSelectedIndex() < swipeableImages.getSize()) {
                        setImage(swipeableImages.getItemAt(swipeableImages.getSelectedIndex()));
                    }
                }                
            }
            listListener = new Listener();
            model.addDataChangedListener(listListener);
            model.addSelectionListener((SelectionListener)listListener);
        }
        this.swipeableImages = model;
    }
    
    /**
     * Returns the list model containing the images in the we can swipe through
     * @return the list model
     */
    public ListModel<Image> getImageList() {
        return swipeableImages;
    }

    /**
     * Manipulate the zoom level of the application
     * @return the zoom
     */
    public float getZoom() {
        return zoom;
    }

    /**
     * Manipulate the zoom level of the application
     * @param zoom the zoom to set
     */
    public void setZoom(float zoom) {
        this.zoom = zoom;
        updatePositions();
        repaint();
    }

    /**
     * This image is shown briefly during swiping while the full size image is loaded
     * @return the swipePlaceholder
     */
    public Image getSwipePlaceholder() {
        return swipePlaceholder;
    }

    /**
     * This image is shown briefly during swiping while the full size image is loaded
     * @param swipePlaceholder the swipePlaceholder to set
     */
    public void setSwipePlaceholder(Image swipePlaceholder) {
        this.swipePlaceholder = swipePlaceholder;
    }

    /**
     * Eager locking effectively locks the right/left images as well as the main image, as a result
     * more heap is taken
     * @return the eagerLock
     */
    public boolean isEagerLock() {
        return eagerLock;
    }

    /**
     * Eager locking effectively locks the right/left images as well as the main image, as a result
     * more heap is taken
     * @param eagerLock the eagerLock to set
     */
    public void setEagerLock(boolean eagerLock) {
        this.eagerLock = eagerLock;
    }

    /**
     * By default the ImageViewer cycles from the beginning to the end of the list
     * when going to the left, setting this to false prevents this behaviour
     * @return true if it should cycle left from beginning
     */
    public boolean isCycleLeft() {
        return cycleLeft;
    }
    
    /**
     * By default the ImageViewer cycles from the beginning to the end of the list
     * when going to the left, setting this to false prevents this behaviour
     * @param cycleLeft the cycle left to set
     */
    public void setCycleLeft(boolean cycleLeft) {
        this.cycleLeft = cycleLeft;
    }
    
    /**
     * By default the ImageViewer cycles from the end to the beginning of the list
     * when going to the right, setting this to false prevents this behaviour
     * @return true if it should cycle right from the end
     */
    public boolean isCycleRight() {
        return cycleRight;
    }

    /**
     * By default the ImageViewer cycles from the end to the beginning of the list
     * when going to the right, setting this to false prevents this behaviour
     * @param cycleRight the cycle right to set
     */
    public void setCycleRight(boolean cycleRight) {
        this.cycleRight = cycleRight;
    }

    /**
     * The swipe threshold is a number between 0 and 1 that indicates the threshold 
     * after which the swiped image moves to the next image. Below that number the image 
     * will bounce back
     * @return the threshold
     */
    public float getSwipeThreshold() {
        return swipeThreshold;
    }

    /**
     * The swipe threshold is a number between 0 and 1 that indicates the threshold 
     * after which the swiped image moves to the next image. Below that number the image 
     * will bounce back
     * @param swipeThreshold the swipeThreshold to set
     */
    public void setSwipeThreshold(float swipeThreshold) {
        this.swipeThreshold = swipeThreshold;
    }
    
    class AnimatePanX implements Animation {
        private Motion motion;
        private Image replaceImage;
        private int updatePos;
        public AnimatePanX(float destPan, Image replaceImage, int updatePos) {
            motion = Motion.createEaseInOutMotion((int)(panPositionX * 10000), (int)(destPan * 10000), 200);
            motion.start();
            this.replaceImage = replaceImage;
            this.updatePos = updatePos;
            Display.getInstance().getCurrent().registerAnimated(this);
        }

        public boolean animate() {
            float v = motion.getValue();
            v /= 10000.0f;
            panPositionX = v;
            if(motion.isFinished()) {
                if(replaceImage != null) {
                    if(!eagerLock) {
                        getImage().unlock();
                        setImage(replaceImage);
                    } else {
                        setImage(replaceImage);
                        Image left = getImageLeft();
                        Image right = getImageRight();
                        if(left != replaceImage) {
                            left.unlock();
                        }
                        if(right != replaceImage) {
                            right.unlock();
                        }
                        selectLock = true;
                        swipeableImages.setSelectedIndex(updatePos);
                        selectLock = false;
                        replaceImage.lock();
                        eagerLock();
                    }
                    selectLock = true;
                    swipeableImages.setSelectedIndex(updatePos);
                    selectLock = false;
                    panPositionX = 0.5f;
                    panPositionY = 0.5f;
                    zoom = MIN_ZOOM;
                } else {
                    // free cached memory
                    if(swipeableImages != null && swipeableImages.getSize() > 1) {
                        getImageLeft().unlock();
                        getImageRight().unlock();
                    }
                }
                Display.getInstance().getCurrent().deregisterAnimated(this);
            }
            repaint();
            return false;
        }

        public void paint(Graphics g) {
        }
        
    }

    /**
     * Sets the viewer initial image position to fill or to fit.
     * @param imageInitialPosition values can be IMAGE_FILL or IMAGE_FIT
     */ 
    public void setImageInitialPosition(int imageInitialPosition) {
        this.imageInitialPosition = imageInitialPosition;
    }
    
    
}
