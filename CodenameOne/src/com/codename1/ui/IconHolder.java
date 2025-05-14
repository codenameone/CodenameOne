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

/**
 * An interface implemented by {@link Component} classes that can display an icon.  E.g. {@link Label}, {@link SpanLabel}, {@link SpanButton}, etc..
 * @author shannah
 * @since 7.0
 */
public interface IconHolder {
    
    /**
     * Returns the labels icon
     * 
     * @return the labels icon
     */
    public Image getIcon();
    
    /**
     * Sets the Label icon, if the icon is unmodified a repaint would not be triggered
     * 
     * @param icon the image that the label presents.
     */
    public void setIcon(Image icon);
    
    /**
     * Set the gap in pixels between the icon/text to the Label boundaries
     * 
     * @param gap the gap in pixels
     */
    public void setGap(int gap);
    
    /**
     * Returns the gap in pixels between the icon/text to the Label boundaries
     * 
     * @return the gap in pixels between the icon/text to the Label boundaries
     */
    public int getGap();
    
    /**
     * Sets the position of the text relative to the icon if exists
     *
     * @param textPosition alignment value (LEFT, RIGHT, BOTTOM or TOP)
     * @see #LEFT
     * @see #RIGHT
     * @see #BOTTOM
     * @see #TOP
     */
    public void setTextPosition(int textPosition);
    
    /**
     * Returns The position of the text relative to the icon
     * 
     * @return The position of the text relative to the icon, one of: LEFT, RIGHT, BOTTOM, TOP
     * @see #LEFT
     * @see #RIGHT
     * @see #BOTTOM
     * @see #TOP
     */
    public int getTextPosition();
    /**
     * Sets a UIID to be used for the material icon style.
     * @param uiid The uiid to use for the material icon style. 
     * @since 7.0
     */
    public void setIconUIID(String uiid);
    
    /**
     * Gets the UIID used for styling material icons on this component.
     * @return 
     */
    public String getIconUIID();
    
    /**
     * Gets the component that should be used for styling material the material icon.  If {@link #setIconUIID(java.lang.String) } has been used
     * to set a custom UIID for the icon, then this will return a component with that UIID.  Otherwise this will just return this component
     * itself.
     * @return The component to use for styling the material icon.
     * @since 7.0
     */
    public Component getIconStyleComponent();

    /**
     * This method is shorthand for {@link com.codename1.ui.FontImage#setMaterialIcon(com.codename1.ui.Label, char, float)}
     * @param c one of the constants from {@link com.codename1.ui.FontImage}
     * @param size the size of the icon in millimeters
     * @since 8.0
     */
    void setMaterialIcon(char c, float size);

    /**
     * This method is shorthand for {@link com.codename1.ui.FontImage#setFontIcon(com.codename1.ui.Label, com.codename1.ui.Font, char, float)}
     * @param c one of the constants from the font
     * @param size the size of the icon in millimeters
     * @since 8.0
     */
    void setFontIcon(Font font, char c, float size);
}
