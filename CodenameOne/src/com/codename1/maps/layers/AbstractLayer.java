/*
 * Copyright (c) 2010, 2011 Itiner.pl. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Itiner designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Itiner in the LICENSE.txt file that accompanied this code.
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
 */
package com.codename1.maps.layers;

import com.codename1.maps.BoundingBox;
import com.codename1.maps.Projection;
import com.codename1.maps.providers.MapProvider;

/**
 * This class represents an abstract layer on the map.
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public abstract class AbstractLayer implements Layer{

    protected final String name;
    protected Projection projection;

    /**
     * Creates an abstract layer.
     * 
     * @param p the projection system of this Layer
     * @param name the name of this Layer
     */
    public AbstractLayer(Projection p, String name) {
        this.name = name;
        projection = p;
    }

    /**
     * Gets the name of this Layer
     * @return the name of this Layer
     */
    public String getName() {
        return name;
    }

    /**
     * The projection of this Layer
     * @return the projection of this Layer
     */
    public Projection getProjection() {
        return projection;
    }
    
    /**
     * The bounding box of this Layer
     * @return the Layer bounding box
     */
    public abstract BoundingBox boundingBox();
    
}
