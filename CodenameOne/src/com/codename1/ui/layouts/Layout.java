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
package com.codename1.ui.layouts;

import com.codename1.ui.geom.Dimension;
import com.codename1.ui.*;

/**
 * Abstract class that can be used to arrange components in a container using
 * a predefined algorithm. This class may be implemented externally and is similar
 * in spirit to the AWT/Swing layout managers.
 *
 * @author Chen Fishbein
 */
public abstract class Layout {
    
    /**
     * Layout the given parent container children
     * 
     * @param parent the given parent container
     */
    public abstract void layoutContainer(Container parent);
    
    
    /**
     * Returns the container preferred size
     * 
     * @param parent the parent container
     * @return the container preferred size
     */
    public abstract Dimension getPreferredSize(Container parent);

    /**
     * Some layouts can optionally track the addition of elements with meta-data 
     * that allows the user to "hint" on object positioning.
     * 
     * @param value optional meta data information, like alignment orientation
     * @param comp the added component to the layout
     * @param c the parent container
     */
    public void addLayoutComponent(Object value, Component comp, Container c) {
        if(value != null) {
            throw new IllegalStateException("Layout doesn't support adding with arguments: " + getClass().getName());
        }
    }

    /**
     * Removes the component from the layout this operation is only useful if the 
     * layout maintains references to components within it
     * 
     * @param comp the removed component from layout
     */
    public void removeLayoutComponent(Component comp) {}
    
    /**
     * Returns the optional component constraint
     * 
     * @param comp the component whose constraint should be returned
     * @return the optional component constraint
     */
    public Object getComponentConstraint(Component comp) {
        return null;
    }
    
    /**
     * This method returns true if the Layout allows Components to
     * Overlap.
     * 
     * @return true if Components may intersect in this layout
     */
    public boolean isOverlapSupported(){
        return false;
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass();
    }

    /**
     * @inheritDoc
     */
    public int hashCode() {
        return getClass().getName().hashCode();
    }
}
