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
package com.codename1.maps;

import com.codename1.maps.providers.MapProvider;
import com.codename1.ui.Graphics;
import com.codename1.ui.util.WeakHashMap;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 *
 * @author Roman Kamyk <roman.kamyk@itiner.pl>
 */
class CacheProviderProxy extends MapProvider {

    private final MapProvider provider;
    private WeakHashMap _cache;
    private int _time;
    private int _maxSize;

    CacheProviderProxy(MapProvider provider) {
        super(provider.projection(), provider.tileSize());
        this.provider = provider;
        _cache = new WeakHashMap();
        _time = 0;
        _maxSize = 100;
    }
    
    public int maxZoomLevel() {
        return provider.maxZoomLevel();
    }

    public Coord scale(int zoomLevel) {
        return provider.scale(zoomLevel);
    }

    public Tile tileFor(BoundingBox bbox) {
        Tile tile = get(bbox);
        if (tile == null) {
            put(bbox, tile = provider.tileFor(bbox));
        }
        return tile;
    }

    public BoundingBox bboxFor(Coord position, int zoomLevel) {
        return provider.bboxFor(position, zoomLevel);
    }

    public String attribution() {
        return provider.attribution();
    }

    public MapProvider originalProvider() {
        return provider;
    }

    protected Tile get(BoundingBox bbox) {
        _time += 1;
        Object o = _cache.get(bbox);
        if (o == null) {
            return null;
        }
        AgeableTile tile = (AgeableTile) o;
        tile.age = _time;
        return tile;
    }

    protected void put(BoundingBox bbox, Tile tile) {
        _cache.put(bbox, new AgeableTile(tile, _time));
    }

    public void clearCache() {
        _maxSize = 6;
        _cache.clear();
    }

    class AgeableTile extends Tile {

        private Tile tile;
        public int age;

        public AgeableTile(Tile tile, int time) {
            super(tile.dimension(), tile.getBoundingBox(), null);
            this.tile = tile;
            this.age = time;
        }

        public boolean paint(Graphics g) {
            return tile.paint(g);
        }
    }
}