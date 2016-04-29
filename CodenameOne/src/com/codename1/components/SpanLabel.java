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

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;

/**
 * <p>A multi line label component that can be easily localized, this is simply based
 * on a text area combined with a label.</p>
 * <script src="https://gist.github.com/codenameone/55b73c621fea0263638a.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-spanlabel.png" alt="SpanLabel Sample" />
 *
 * @author Shai Almog
 */
public class SpanLabel extends Container {
    private Label icon;
    private TextArea text;
    
    /**
     * Default constructor will be useful when adding this to the GUI builder
     */
    public SpanLabel() {
        this("");
    }
    
    /**
     * Constructor accepting default text and uiid for the text
     * @param txt the text
     * @param textUiid the new text UIID
     */
    public SpanLabel(String txt, String textUiid) {
        this(txt);
        text.setUIID(textUiid);
    }
    
    /**
     * Constructor accepting default text
     */
    public SpanLabel(String txt) {
        setUIID("Container");
        setLayout(new BorderLayout());
        text = new TextArea(getUIManager().localize(txt, txt));
        text.setActAsLabel(true);
        text.setColumns(text.getText().length() + 1);
        text.setUIID("Label");
        text.setEditable(false);
        text.setFocusable(false);
        icon = new Label();
        icon.setUIID("icon");
        addComponent(BorderLayout.WEST, icon);
        addComponent(BorderLayout.CENTER, text);
    }
    
    /**
     * Sets the UIID for the actual text
     * @param uiid the uiid
     */
    public void setTextUIID(String uiid) {
        text.setUIID(uiid);
    }
    
    /**
     * Returns the uiid of the actual text
     * @return the uiid
     */
    public String getTextUIID() {
        return text.getUIID();
    }
    
    /**
     * Returns the text elements style object
     * @return the style object
     */
    public Style getTextUnselectedStyle() {
        return text.getUnselectedStyle();
    }
    
    /**
     * The text elements style object
     * @param t the style object
     */
    public void setTextUnselectedStyle(Style t) {
        text.setUnselectedStyle(t);
    }
    
    /**
     * Returns the text elements style object
     * @return the style object
     */
    public Style getTextSelectedStyle() {
        return text.getSelectedStyle();
    }
    
    /**
     * The text elements style object
     * @param t the style object
     */
    public void setTextSelectedStyle(Style t) {
        text.setSelectedStyle(t);
    }
    
    /**
     * Sets the uiid for the icon if present
     * @param uiid the uiid for the icon
     */
    public void setIconUIID(String uiid) {
        icon.setUIID(uiid);
    }
    
    /**
     * Returns the UIID for the icon
     * @return the uiid
     */
    public String getIconUIID() {
        return icon.getUIID();
    }
    
    /**
     * Set the text of the label
     * @param t text of the label
     */
    public void setText(String t) {
        text.setText(getUIManager().localize(t, t));
    }

    /**
     * Sets the icon for the label
     * @param i the icon
     */
    public void setIcon(Image i) {
        icon.setIcon(i);
    }
    
    /**
     * Returns the text of the label
     * @return the text
     */
    public String getText() {
        return text.getText();
    }
    
    /**
     * Indicates the alignment of the whole text block, this is different from setting the alignment of the text within
     * the block since the UIID might have a border or other design element that won't be affected by such alignment.
     * The default is none (-1) which means no alignment takes place and the text block takes the whole width.
     * @param align valid values are Component.LEFT, Component.RIGHT, Component.CENTER. Anything else will
     * stretch the text block
     */
    public void setTextBlockAlign(int align) {
        switch(align) {
            case LEFT:
            case RIGHT:
            case CENTER:
                wrapText(align);
                return;
            default:
                if(text.getParent() != this) {
                    removeComponent(text.getParent());
                    text.getParent().removeAll();
                    addComponent(BorderLayout.CENTER, text);
                }
        }
    }
    
    /**
     * Returns the alignment of the whole text block and not the text within it!
     * 
     * @return -1 for unaligned otherwise one of Component.LEFT/RIGHT/CENTER
     */
    public int getTextBlockAlign() {
        if(text.getParent() == this) {
            return -1;
        }
        return ((FlowLayout)text.getParent().getLayout()).getAlign();
    }
    
    private void wrapText(int alignment) {
        Container parent = text.getParent();
        if(parent == this) {
            parent.removeComponent(text);
            parent = new Container(new FlowLayout(alignment));
            parent.addComponent(text);
            addComponent(BorderLayout.CENTER, parent);
        } else {
            ((FlowLayout)parent.getLayout()).setAlign(alignment);
        }
    }
    
    /**
     * Returns the image of the icon
     * @return the icon
     */
    public Image getIcon() {
        return icon.getIcon();
    }
    
    /**
     * Sets the icon position based on border layout constraints
     * 
     * @param t position either North/South/East/West
     */
    public void setIconPosition(String t) {
        removeComponent(icon);
        addComponent(t, icon);
        revalidate();
    }
    
    /**
     * Returns the icon position based on border layout constraints
     * 
     * @return position either North/South/East/West
     */
    public String getIconPosition() {
        return (String)getLayout().getComponentConstraint(icon);
    }
    

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {
            "text", "icon", "iconPosition", "textUiid", "iconUiid"
        };
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {
           String.class, // text
           Image.class, // icon
           String.class, // iconPosition
           String.class,
           String.class
       };
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String", "Image", "String", "String", "String"};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("text")) {
            return getText();
        }
        if(name.equals("icon")) {
            return getIcon();
        }
        if(name.equals("iconPosition")) {
            return getIconPosition();
        }
        if(name.equals("textUiid")) {
            return getTextUIID();
        }
        if(name.equals("iconUiid")) {
            return getIconUIID();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("text")) {
            setText((String)value);
            return null;
        }
        if(name.equals("icon")) {
            setIcon((Image)value);
            return null;
        }
        if(name.equals("iconPosition")) {
            setIconPosition((String)value);
            return null;
        }
        if(name.equals("textUiid")) {
            setTextUIID((String)value);
            return null;
        }
        if(name.equals("iconUiid")) {
            setIconUIID((String)value);
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}
