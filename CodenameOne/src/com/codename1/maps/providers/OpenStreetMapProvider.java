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
package com.codename1.maps.providers;

import com.codename1.ui.geom.Dimension;
import com.codename1.maps.Mercator;

/**
 * This is an OpenStreetMap Provider http://www.openstreetmap.org/
 * 
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
public class OpenStreetMapProvider extends TiledProvider {

    /**
     * Empty Constructor
     */
    public OpenStreetMapProvider() {
        super("http://tile.openstreetmap.org", new Mercator(), new Dimension(256, 256));
    }

    /**
     * @inheritDoc
     */
    public String attribution() {
        return "(c) OpenStreetMap (and) contributors, CC-BY-SA";
    }

    /**
     * @inheritDoc
     */
    public int maxZoomLevel() {
        return 18;
    }
}
