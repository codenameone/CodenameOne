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

package com.codename1.ui.util.xml.comps;

import com.codename1.ui.util.xml.Val;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 *
 * @author Shai Almog
 */
@XmlRootElement(name="mapItems")
@XmlAccessorType(XmlAccessType.FIELD)
public class MapItems {    
    @XmlElement
    private Val[] imageItem; 

    @XmlElement
    private Val[] actionItem; 
    
    @XmlElement
    private Val[] stringItem; 

    /**
     * @return the imageItem
     */
    public Val[] getImageItem() {
        return imageItem;
    }

    /**
     * @param imageItem the imageItem to set
     */
    public void setImageItem(Val[] imageItem) {
        this.imageItem = imageItem;
    }

    /**
     * @return the actionItem
     */
    public Val[] getActionItem() {
        return actionItem;
    }

    /**
     * @param actionItem the actionItem to set
     */
    public void setActionItem(Val[] actionItem) {
        this.actionItem = actionItem;
    }

    /**
     * @return the stringItem
     */
    public Val[] getStringItem() {
        return stringItem;
    }

    /**
     * @param stringItem the stringItem to set
     */
    public void setStringItem(Val[] stringItem) {
        this.stringItem = stringItem;
    }
}
