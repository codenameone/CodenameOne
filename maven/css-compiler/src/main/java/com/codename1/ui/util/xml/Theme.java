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
package com.codename1.ui.util.xml;


/**
 * Parsed XML data
 *
 * @author Shai Almog
 */
public class Theme {
    private String name;
    
    private Val[] val;

    private Gradient[] gradient;

    private Font[] font;

    private Border[] border;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the val
     */
    public Val[] getVal() {
        return val;
    }

    /**
     * @return the gardient
     */
    public Gradient[] getGradient() {
        return gradient;
    }

    /**
     * @return the font
     */
    public Font[] getFont() {
        return font;
    }

    /**
     * @return the border
     */
    public Border[] getBorder() {
        return border;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVal(Val[] val) {
        this.val = val;
    }

    public void setGradient(Gradient[] gradient) {
        this.gradient = gradient;
    }

    public void setFont(Font[] font) {
        this.font = font;
    }

    public void setBorder(Border[] border) {
        this.border = border;
    }
}
