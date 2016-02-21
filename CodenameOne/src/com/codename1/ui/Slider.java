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
package com.codename1.ui;


import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;

/**
 * <p>The slider component serves both as a slider widget to allow users to select
 * a value on a scale via touch/arrows and also to indicate progress. The slider
 * defaults to percentage display but can represent any positive set of values.</p>
 * <img src="https://www.codenameone.com/img/developer-guide/slider.png" alt="Sample Slider" />
 *
 * @author Shai Almog
 */
public class Slider extends Label {
    private int value;
    private int maxValue = 100;
    private int minValue = 0;
    private boolean vertical;
    private boolean editable;
    private EventDispatcher listeners = new EventDispatcher();
    private EventDispatcher actionListeners = new EventDispatcher();
    private int increments = 4;
    private int previousX = -1, previousY = -1;
    private Style sliderFull;
    private Style sliderFullSelected;
    private boolean paintingFull;
    private boolean renderPercentageOnTop;
    private boolean renderValueOnTop;

    private boolean infinite = false;
    private float infiniteDirection = 0.03f;
    private Image thumbImage;
    private String fullUIID = "Slider";


    /**
     * The default constructor uses internal rendering to draw its state
     */
    public Slider() {
        this("Slider", "Slider");
    }

    private Slider(String uiid, String fullUIID) {
        setFocusable(false);
        setUIID(uiid);
        this.fullUIID = fullUIID;
        setAlignment(CENTER);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setUIID(String id) {
        super.setUIID(id);
        initStyles(id);
    }

    private void initStyles(String id){
        sliderFull = getUIManager().getComponentStyle(id + "Full");
        sliderFullSelected = getUIManager().getComponentSelectedStyle(id + "Full");
        initCustomStyle(sliderFull);
        initCustomStyle(sliderFullSelected);    
    }
    
    /**
     * {@inheritDoc}
     */
    protected boolean isStickyDrag() {
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    public void initComponent() {
        if(infinite) {
            getComponentForm().registerAnimatedInternal(this);
            if(thumbImage == null) {
                thumbImage = UIManager.getInstance().getThemeImageConstant("sliderThumbImage");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deinitialize() {
        if(infinite) {
            Form f = getComponentForm();
            if(f != null) {
                f.deregisterAnimatedInternal(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean animate() {
        if(infinite) {
            super.animate();
            float f = (infiniteDirection * ((float)maxValue));
            if(((int)f) == 0) {
                if(f < 0) {
                    f = -1;
                } else {
                    f = 1;
                }
            }
            value += ((int)f);
            if(value >= maxValue) {
                value = maxValue;
                infiniteDirection *= (-1);
            }
            if(value <= 0) {
                value = (int)0;
                infiniteDirection *= (-1);
            }
            return true;
        }
        return super.animate();
    }
    /**
     * The infinite slider functionality is used to animate
     * progress for which there is no defined value.
     *
     * @return true for infinite progress
     */
    public boolean isInfinite() {
        return infinite;
    }

    /**
     * Activates/disables the infinite slider functionality used to animate 
     * progress for which there is no defined value.
     * 
     * @param i true for infinite progress
     */
    public void setInfinite(boolean i) {
        if(infinite != i) {
            infinite = i;
            if(isInitialized()) {
                if(i) {
                    getComponentForm().registerAnimatedInternal(this);
                } else {
                    getComponentForm().deregisterAnimatedInternal(this);
                }
            }
        }
    }

    /**
     * Creates an infinite progress slider
     *
     * @return a slider instance that has no end value
     */
    public static Slider createInfinite() {
        Slider s = new Slider();
        s.infinite = true;
        return s;
    }


    /**
     * {@inheritDoc}
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        if(sliderFull != null) {
            deinitializeCustomStyle(sliderFull);
            deinitializeCustomStyle(sliderFullSelected);
            initStyles("Slider");
        }
    }

    /**
     * Indicates the value of progress made
     *
     * @return the progress on the slider
     */
    public int getProgress() {
        return value;
    }

    /**
     * Indicates the value of progress made, this method is thread safe and
     * can be invoked from any thread although discretion should still be kept
     * so one thread doesn't regress progress made by another thread...
     *
     * @param value new value for progress
     */
    public void setProgress(int value) {
        if(this.value != value){
            fireDataChanged(DataChangedListener.CHANGED, value);
        }
        setProgressInternal(value);
    }
    
    private void setProgressInternal(int value) {
        this.value = value;
        if(renderValueOnTop || renderPercentageOnTop) {
            super.setText(formattedValue(value));
        } else {
            if(isInitialized()) {
                repaint();
            }
        }
    }

    /**
     * Allows formatting the appearance of the progress when text is drawn on top
     * 
     * @param value the value of the slider
     * @return a string formatted version
     */
    protected String formattedValue(int value) {
        if(renderValueOnTop) {
            return("" + value);
        }
        if(renderPercentageOnTop) {
            return(value + "%");
        }
        return("");
    }

    /**
     * Returns the {@link com.codename1.ui.plaf.Style} used to paint the slider when its full
     * @return the Style object that shows a completely full style.
     */
    public Style getSliderFullUnselectedStyle() {
        return sliderFull;
    }

    /**
     * Returns the {@link com.codename1.ui.plaf.Style} used to paint the slider when its full and selected
     * @return the Style object that shows a completely full style.
     */
    public Style getSliderFullSelectedStyle() {
        return sliderFull;
    }

    /**
     * Returns the {@link com.codename1.ui.plaf.Style} used to paint the slider when its full
     * @return the Style object that shows a completely full style.
     */
    public Style getSliderEmptyUnselectedStyle() {
        return super.getUnselectedStyle();
    }

    /**
     * Returns the {@link com.codename1.ui.plaf.Style} used to paint the slider when its full and selected
     * @return the Style object that shows a completely full style.
     */
    public Style getSliderEmptySelectedStyle() {
        return super.getSelectedStyle();
    }

    /**
     * {@inheritDoc}
     */
    public Style getStyle() {
        if(paintingFull) {
            if(sliderFull == null) {
                initStyles(fullUIID);
            }
            if(hasFocus()) {
                return sliderFullSelected;
            }
            return sliderFull;
        }
        return super.getStyle();
    }

    /**
     * Return the size we would generally like for the component
     */
    protected Dimension calcPreferredSize() {
        Style style = getStyle();
        int prefW = 0, prefH = 0;
        if(style.getBorder() != null) {
            prefW = Math.max(style.getBorder().getMinimumWidth(), prefW);
            prefH = Math.max(style.getBorder().getMinimumHeight(), prefH);
        }
        if(thumbImage != null) {
            prefW = Math.max(thumbImage.getWidth(), prefW);
            prefH = Math.max(thumbImage.getHeight(), prefH);
        }
        // we don't really need to be in the font height but this provides
        // a generally good indication for size expectations
        if(Display.getInstance().isTouchScreenDevice() && isEditable()) {
            if(vertical) {
                prefW = Math.max(prefW, Font.getDefaultFont().charWidth('X') * 2);
                prefH = Math.max(prefH, Display.getInstance().getDisplayHeight() / 2);
            } else {
                prefW = Math.max(prefW, Display.getInstance().getDisplayWidth() / 2);
                prefH = Math.max(prefH, Font.getDefaultFont().getHeight() * 2);
            }
        } else {
            if(vertical) {
                prefW = Math.max(prefW, Font.getDefaultFont().charWidth('X'));
                prefH = Math.max(prefH, Display.getInstance().getDisplayHeight() / 2);
            } else {
                prefW = Math.max(prefW, Display.getInstance().getDisplayWidth() / 2);
                prefH = Math.max(prefH, Font.getDefaultFont().getHeight());
            }
        }
        if (prefH != 0) {
            prefH += (style.getPadding(false, Component.TOP) + style.getPadding(false, Component.BOTTOM));
        }
        if (prefW != 0) {
            prefW += (style.getPadding(isRTL(), Component.RIGHT) + style.getPadding(isRTL(), Component.LEFT));
        }
        return new Dimension(prefW, prefH);
    }

    /**
     * Paint the progress indicator
     */
    public void paintComponentBackground(Graphics g) {
        super.paintComponentBackground(g);
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        //g.pushClip();
        int width = getWidth();
        int height = getHeight();
        
        int y = getY();
        if(infinite) {
            int blockSize = getWidth() / 5;
            int x = getX() + (int) ((((float) value) / ((float)maxValue - minValue)) * (getWidth() - blockSize));
            g.clipRect(x, y, blockSize, height - 1);
        } else {
            if(vertical) {
                int actualHeight = (int) ((((float) value) / ((float)maxValue - minValue)) * getHeight());
                y += height - actualHeight;
            } else {
                width = (int) ((((float) value) / ((float)maxValue - minValue)) * getWidth());
            }

            g.clipRect(getX(), y, width, height);
        }

        // paint the selected style
        paintingFull = true;
        super.paintComponentBackground(g);
        paintingFull = false;

        g.setClip(clipX, clipY, clipW, clipH);
        //g.popClip();
        if(thumbImage != null && !infinite) {
            if(!vertical) {
                int xPos = getX() + width - thumbImage.getWidth() / 2;
                xPos = Math.max(getX(), xPos);
                xPos = Math.min(getX() + getWidth() - thumbImage.getWidth(), xPos);
                g.drawImage(thumbImage, xPos,
                        y + height / 2 - thumbImage.getHeight() / 2);
            } else {
                int yPos = y;// + height - thumbImage.getHeight() / 2;
                //yPos = Math.max(getY(), yPos);
                //yPos = Math.min(getY() + getHeight() - thumbImage.getHeight(), yPos);
                g.drawImage(thumbImage, getX() + width / 2 - thumbImage.getWidth() / 2,
                        yPos);
            }
        }
    }

    /**
     * Indicates the slider is vertical
     * @return true if the slider is vertical
     */
    public boolean isVertical() {
        return vertical;
    }

    /**
     * Indicates the slider is vertical
     * @param vertical true if the slider is vertical
     */
    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    /**
     * Indicates the slider is modifyable
     * @return true if the slider is editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Indicates the slider is modifyable
     * @param editable  true if the slider is editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        setFocusable(editable);
    }    
   
    public void pointerPressed(int x, int y) {
        if(!editable) {
            return;
        }
        if(vertical) {
            // turn the coordinate to a local coordinate and invert it
            y = Math.abs(getHeight() - (y - getAbsoluteY()));
            setProgressInternal((int)(Math.min(maxValue, ((float)y) / ((float)getHeight()) * maxValue)));
        } else {
            x = Math.abs(x - getAbsoluteX());
            setProgressInternal((int)(Math.min(maxValue, ((float)x) / ((float)getWidth()) * maxValue)));
        }
        value = Math.max(value, minValue);
        
        if(vertical) {
            if(previousY < y){
                fireDataChanged(DataChangedListener.ADDED, value);
            }else{
                fireDataChanged(DataChangedListener.REMOVED, value);
            }
            previousY = y;
        }else{
            if(previousX < x){
                fireDataChanged(DataChangedListener.ADDED, value);
            }else{
                fireDataChanged(DataChangedListener.REMOVED, value);
            }
            previousX = x;
        
        }
    }

    /**
     * {@inheritDoc}
     */
    public void pointerDragged(int x, int y) {
        if(!editable) {
            return;
        }
        if(vertical && previousY == -1){
            previousY = y;
            return;
        }
        if(!vertical && previousX == -1){
            previousX = x;
            return;
        }
        int per = 0;
        if(vertical) {
            // turn the coordinate to a local coordinate and invert it
            y = Math.max(getHeight() - (y - getAbsoluteY()), 0);
            per = (int)(Math.min(maxValue, ((float)y) / ((float)getHeight()) * maxValue));
        } else {
            x = Math.max(x - getAbsoluteX(), 0);
            per = (int)(Math.min(maxValue, ((float)x) / ((float)getWidth()) * maxValue));
        }
        per = Math.max(per, minValue);
        if(per != getProgress()) {
            setProgressInternal(per);

            if(vertical) {
                if(previousY < y){
                    fireDataChanged(DataChangedListener.ADDED, value);
                }else{
                    fireDataChanged(DataChangedListener.REMOVED, value);
                }
                previousY = y;
            }else{
                if(previousX < x){
                    fireDataChanged(DataChangedListener.ADDED, value);
                }else{
                    fireDataChanged(DataChangedListener.REMOVED, value);
                }
                previousX = x;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void fireClicked() {
        setHandlesInput(!handlesInput());
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isSelectableInteraction() {
        return editable;
    }

    /**
     * {@inheritDoc}
     */
    public void pointerReleased(int x, int y) {
        if(!editable) {
            return;
        }
        fireActionEventImpl();
        previousX = -1;
        previousY = -1;
    }

    /**
     * {@inheritDoc}
     */
    public void keyReleased(int code) {
        super.keyReleased(code);
        fireActionEventImpl();
    }
    
    /**
     * {@inheritDoc}
     */
    public void keyPressed(int code) {
        if(editable && handlesInput()) {
            int game = Display.getInstance().getGameAction(code);
            switch(game) {
                case Display.GAME_UP:
                    if(vertical) {
                        setProgressInternal((int)(Math.min(maxValue, value + increments)));
                        fireDataChanged(DataChangedListener.ADDED, value);
                    } else {
                        setHandlesInput(false);
                    }
                    break;
                case Display.GAME_DOWN:
                    if(vertical) {
                        setProgressInternal((int)(Math.max(minValue, value - increments)));
                        fireDataChanged(DataChangedListener.REMOVED, value);
                    } else {
                        setHandlesInput(false);
                    }
                    break;
                case Display.GAME_LEFT:
                    if(!vertical) {
                        setProgressInternal((int)(Math.max(minValue, value - increments)));
                        fireDataChanged(DataChangedListener.REMOVED, value);
                    } else {
                        setHandlesInput(false);
                    }
                    break;
                case Display.GAME_RIGHT:
                    if(!vertical) {
                        setProgressInternal((int)(Math.min(maxValue, value + increments)));
                        fireDataChanged(DataChangedListener.ADDED, value);
                    } else {
                        setHandlesInput(false);
                    }
                    break;
                case Display.GAME_FIRE:
                    if(!Display.getInstance().isThirdSoftButton()) {
                        fireClicked();
                    }
                    break;
            }
        } else {
            if(!Display.getInstance().isThirdSoftButton() &&
                    Display.getInstance().getGameAction(code) == Display.GAME_FIRE) {
                fireClicked();
            }
        }
        super.keyPressed(code);
    }

    /**
     * The increments when the user presses a key to the left/right/up/down etc.
     *
     * @return increment value
     */
    public int getIncrements() {
        return increments;
    }

    /**
     * The increments when the user presses a key to the left/right/up/down etc.
     *
     * @param increments increment value
     */
    public void setIncrements(int increments) {
        this.increments = increments;
    }
   
    private void fireDataChanged(int event, int val){
        listeners.fireDataChangeEvent(val, event);
    }
    
    private void fireActionEventImpl() {
        actionListeners.fireActionEvent(new ActionEvent(this,ActionEvent.Type.PointerPressed));
    }

    /**
     * Adds a listener to data changed events, notice that the status argument to the data change listener
     * shouldn't be relied upon. 
     *
     * @param l new listener
     */
    public void addDataChangedListener(DataChangedListener l){
        listeners.addListener(l);
    }

    /**
     * Removes a listener from data changed events, notice that the status argument to the data change listener
     * shouldn't be relied upon. 
     *
     * @param l listener to remove
     */
    public void removeDataChangedListener(DataChangedListener l){
        listeners.removeListener(l);
    }
    
    /**
     * Action listeners give a more coarse event only when the user lifts the finger from the slider
     * @param l the listener
     */
    public void addActionListener(ActionListener l) {
        actionListeners.addListener(l);
    }

    /**
     * Action listeners give a more coarse event only when the user lifts the finger from the slider
     * @param l the listener
     */
    public void removeActionListener(ActionListener l) {
        actionListeners.removeListener(l);
    }
    
    /**
     * Indicates that the value of the slider should be rendered with a percentage sign
     * on top of the slider.
     *
     * @return true if so
     */
    public boolean isRenderPercentageOnTop() {
        return renderPercentageOnTop;
    }

    /**
     * Indicates that the value of the slider should be rendered with a percentage sign
     * on top of the slider.
     *
     * @param renderPercentageOnTop true to render percentages
     */
    public void setRenderPercentageOnTop(boolean renderPercentageOnTop) {
        this.renderPercentageOnTop = renderPercentageOnTop;
    }

    /**
     * @return the renderValueOnTop
     */
    public boolean isRenderValueOnTop() {
        return renderValueOnTop;
    }

    /**
     * @param renderValueOnTop the renderValueOnTop to set
     */
    public void setRenderValueOnTop(boolean renderValueOnTop) {
        this.renderValueOnTop = renderValueOnTop;
    }

    /**
     * @return the maxValue
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * @param maxValue the maxValue to set
     */
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * @return the minValue
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * @param minValue the minValue to set
     */
    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    /**
     * The thumb image is drawn on top of the current progress
     *
     * @return the thumbImage
     */
    public Image getThumbImage() {
        return thumbImage;
    }

    /**
     * The thumb image is drawn on top of the current progress
     * 
     * @param thumbImage the thumbImage to set
     */
    public void setThumbImage(Image thumbImage) {
        this.thumbImage = thumbImage;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean shouldBlockSideSwipe() {
        return editable && !vertical;
    }
}
