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

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;

/**
 * <p>Label that simplifies the usage of scale to fill/fit. This is effectively equivalent to just setting the style image
 * on a label but more convenient for some special circumstances. One major difference is that preferred size
 * equals the image in this case.<br>
 * The default UIID for this component is "{@code Label}".
 * </p>
 * <script src="https://gist.github.com/codenameone/7289bbe5dad9e279eabb.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-scaleimage.png" alt="ScaleImageButton and ScaleImageLabel samples" />

 *
 * @author Shai Almog
 */
public class ScaleImageLabel extends Label {

    /**
     * Default constructor
     */
    public ScaleImageLabel() {
        setUIID("Label");
        setShowEvenIfBlank(true);
        getAllStyles().setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FIT);
        getAllStyles().setBgTransparency(255);
    }
    
    /**
     * Create a version with the given image
     * @param i image
     */
    public ScaleImageLabel(Image i) {
        setUIID("Label");
        setShowEvenIfBlank(true);
        getAllStyles().setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FIT);
        getAllStyles().setBgTransparency(255);
        setIcon(i);
    }
    
    /**
     * Sets the behavior of the background to one of Style.BACKGROUND_IMAGE_SCALED_FIT,
     * Style.BACKGROUND_IMAGE_SCALED_FILL, Style.BACKGROUND_IMAGE_SCALE
     * @param behavior the background behavior
     */
    public void setBackgroundType(byte behavior) {
        getAllStyles().setBackgroundType(behavior);
    }
    
    /**
     * Returns the background type for the component 
     * @return One of Style.BACKGROUND_IMAGE_SCALED_FIT,
     * Style.BACKGROUND_IMAGE_SCALED_FILL or Style.BACKGROUND_IMAGE_SCALE
     */
    public byte getBackgroundType() {
        return getUnselectedStyle().getBackgroundType();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Dimension calcPreferredSize() {
        Image i = getIcon();
        if(i == null) {
            return new Dimension();
        }
        int dw = Display.getInstance().getDisplayWidth();
        int iw = i.getWidth();
        int ih = i.getHeight();
        
        // a huge preferred width might be requested that is bigger than screen size. Normally this isn't a problem but in
        // a scrollable container the vertical height might be granted providing so much space as to make this unrealistic...
        if(iw > dw) {
            float ratio = ((float)iw) / ((float)dw);
            iw = (int) (((float)iw) / ((float)ratio));
            ih = (int) (((float)ih) / ((float)ratio));
        }
        Style s = getStyle();
        return new Dimension(iw + s.getPaddingLeft(false) + s.getPaddingRight(false), ih +
                s.getPaddingTop() + s.getPaddingBottom());
    }

    
    /**
     * Instead of setting the icon sets the background image
     * @param i the image
     */
    public void setIcon(Image i) {
        setShouldCalcPreferredSize(true);
        getAllStyles().setBgImage(i);
        if(i !=null && i.isAnimation()) {
            checkAnimation(i);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Overriden to support animations
     */
    @Override
    protected void initComponent() {
        super.initComponent(); 
        checkAnimation(getIcon());
    }
    
    
    
    void checkAnimation(Image icon) {
        if(icon != null && icon.isAnimation()) {
            Form parent = getComponentForm();
            if(parent != null) {
                // animations are always running so the internal animation isn't
                // good enough. We never want to stop this sort of animation
                parent.registerAnimated(this);
            }
        }
    }
        
    /**
     * Returns the background image
     * @return the bg image
     */
    public Image getIcon() {
        return getUnselectedStyle().getBgImage();
    }
    
    /**
     * Scale image label doesn't support text this method is overriden to do nothing
     */
    @Override
    public void setText(String text) {
    }
    
    /**
     * {@inheritDoc} 
     * Overriden to prevent the setUIID from replacing the code
     */
    @Override
    public void setUIID(String id) {
        byte type = getBackgroundType();
        Image icon = getIcon();
        super.setUIID(id); 
        setIcon(icon);
        getAllStyles().setBackgroundType(type);
        getAllStyles().setBgTransparency(255);
    }

    @Override
    protected void refreshTheme(String id, boolean merge) {
        byte type = getBackgroundType();
        Image icon = getIcon();
        super.refreshTheme(id, merge);
        setIcon(icon);
        getAllStyles().setBackgroundType(type);
        getAllStyles().setBgTransparency(255);
    }
    
    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"backgroundType"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] { Byte.class };
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"Byte"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("backgroundType")) {
            return getBackgroundType();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("backgroundType")) {
            setBackgroundType(((Byte)value).byteValue());
            return null;
        }
        return super.setPropertyValue(name, value);
    }
    
}
