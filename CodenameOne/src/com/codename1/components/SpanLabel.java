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
import com.codename1.ui.plaf.Style;

/**
 * A multi line label component that can be easily localized, this is simply based
 * on a text area combined with a label.
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
     * Constructor accepting default text
     */
    public SpanLabel(String txt) {
        setUIID("Label");
        setLayout(new BorderLayout());
        text = new TextArea(getUIManager().localize(txt, txt));
        text.setUIID("Label");
        text.setEditable(false);
        text.setFocusable(false);
        icon = new Label();
        icon.setUIID("icon");
        addComponent(BorderLayout.WEST, icon);
        addComponent(BorderLayout.CENTER, text);
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
     * Returns the image of the icon
     * @return the icon
     */
    public Image getIcon() {
        return icon.getIcon();
    }
    
    /**
     * Sets the icon position based on border layout constraints
     * 
     * @param s position either North/South/East/West
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
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {
            "text", "icon", "iconPosition"
        };
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {
           String.class, // text
           Image.class, // icon
           String.class // iconPosition
       };
    }

    /**
     * @inheritDoc
     */
    public String[] getPropertyTypeNames() {
        return new String[] {"String", "Image", "String"};
    }

    /**
     * @inheritDoc
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
        return null;
    }

    /**
     * @inheritDoc
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
        return super.setPropertyValue(name, value);
    }
}
